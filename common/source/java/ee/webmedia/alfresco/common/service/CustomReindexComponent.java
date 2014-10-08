package ee.webmedia.alfresco.common.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.domain.Transaction;
import org.alfresco.repo.node.index.AbstractReindexComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.utils.ProgressTracker;

public class CustomReindexComponent extends AbstractReindexComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(CustomReindexComponent.class);

    private static final String MSG_RECOVERY_TERMINATED = "index.recovery.terminated";
    private static final String MSG_RECOVERY_ERROR = "index.recovery.error";

    boolean enabled = true;
    private int maxRecordSetSize;
    private int maxTransactionsPerLuceneCommit;
    private SimpleJdbcTemplate jdbcTemplate;
    private long lookBackMinutes = 1440; // 24 hours
    private String customChangeTxnIds = "";

    @Override
    protected boolean requireTransaction() {
        return false;
    }

    @Override
    protected void reindexImpl() {
        List<Long> txns;
        boolean force = false;
        if (StringUtils.isNotBlank(customChangeTxnIds)) {
            force = true;
            txns = getTransactions(customChangeTxnIds);
        } else {
            txns = getTransactions();
        }
        if (txns.isEmpty()) {
            return;
        }

        searchHolesAndIndex(txns, force);
    }

    private List<Long> getTransactions(final String changeTxnIds) {
        return transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<List<Long>>() {
            @Override
            public List<Long> execute() throws Throwable {
                final List<Long> allTxns = new ArrayList<Long>();
                for (String changeTxnId : StringUtils.split(changeTxnIds, null)) {
                    long txnId = jdbcTemplate.queryForLong("SELECT id FROM alf_transaction WHERE change_txn_id = ?", changeTxnId);
                    allTxns.add(txnId);
                }
                return allTxns;
            }
        }, true);
    }

    private List<Long> getTransactions() {
        final long endTimeMs = System.currentTimeMillis();
        final AtomicLong startTimeMs = new AtomicLong(endTimeMs - (lookBackMinutes * 60000));
        LOG.info("Finding transactions from " + ISO8601DateFormat.format(new Date(startTimeMs.get())) + " to " + ISO8601DateFormat.format(new Date(endTimeMs)) + "...");
        final Pair<String, String> firstAndLastTxnInfo = new Pair<String, String>(null, null);
        final List<Long> excludeTxnIds = new ArrayList<Long>();
        final List<Long> allTxns = new ArrayList<Long>();
        boolean hasResults;
        do {
            hasResults = transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Boolean>() {
                @Override
                public Boolean execute() throws Throwable {
                    if (LOG.isDebugEnabled()) {
                        LOG.info("Query transactions from " + ISO8601DateFormat.format(new Date(startTimeMs.get())) + " to " + ISO8601DateFormat.format(new Date(endTimeMs))
                                + " (max "
                                + maxRecordSetSize + ", exclude " + excludeTxnIds + ")");
                    }
                    List<Transaction> txns = nodeDaoService.getTxnsByCommitTimeAscending(startTimeMs.get(), endTimeMs, maxRecordSetSize, excludeTxnIds, false);
                    if (LOG.isDebugEnabled()) {
                        LOG.info("Found " + txns.size() + " transactions");
                    }
                    if (txns.isEmpty()) {
                        return false;
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.info("First: " + getTransactionInfo(txns.get(0)));
                        LOG.info("Last : " + getTransactionInfo(txns.get(txns.size() - 1)));
                    }

                    for (Transaction txn : txns) {
                        allTxns.add(txn.getId());
                    }

                    startTimeMs.set(txns.get(txns.size() - 1).getCommitTimeMs());
                    excludeTxnIds.clear();
                    for (int i = txns.size() - 1; i >= 0; i--) {
                        Transaction txn = txns.get(i);
                        if (txn.getCommitTimeMs() < startTimeMs.get()) {
                            break;
                        }
                        excludeTxnIds.add(txn.getId());
                    }
                    if (firstAndLastTxnInfo.getFirst() == null) {
                        firstAndLastTxnInfo.setFirst(getTransactionInfo(txns.get(0)));
                    }
                    firstAndLastTxnInfo.setSecond(getTransactionInfo(txns.get(txns.size() - 1)));
                    return true;
                }
            }, true);
        } while (hasResults);

        LOG.info("Found total " + allTxns.size() + " transactions during that time period");
        if (allTxns.isEmpty()) {
            return allTxns;
        }
        LOG.info("First: " + firstAndLastTxnInfo.getFirst());
        LOG.info("Last : " + firstAndLastTxnInfo.getSecond());
        return allTxns;
    }

    public static String getTransactionInfo(Transaction txn) {
        return ISO8601DateFormat.format(new Date(txn.getCommitTimeMs())) + " " + txn.getId() + " " + txn.getVersion() + " " + txn.getServer();
    }

    private void searchHolesAndIndex(List<Long> txns, final boolean force) {
        LOG.info("Searching for holes and starting background indexing...");
        final AtomicReference<Transaction> sliceStartTxn = new AtomicReference<Transaction>(null);
        final AtomicReference<Transaction> lastTxn = new AtomicReference<Transaction>(null);
        final AtomicReference<Boolean> sliceCommited = new AtomicReference<Boolean>(null);
        final AtomicInteger sliceItemCount = new AtomicInteger(0);
        final Map<String, Integer> sliceServerCount = new HashMap<String, Integer>();
        final ProgressTracker progress = new ProgressTracker(txns.size(), 0);
        final AtomicInteger i = new AtomicInteger(0);
        final AtomicInteger txnsToReindex = new AtomicInteger(0);
        final AtomicInteger holes = new AtomicInteger(0);
        final List<Long> txnIdBuffer = new ArrayList<Long>(maxTransactionsPerLuceneCommit);
        final Iterator<Long> txnIterator = txns.iterator();
        while (txnIterator.hasNext()) {
            transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    Transaction txn = nodeDaoService.getTxnById(txnIterator.next());
                    boolean commited = force ? false : isTxnPresentInIndex(txn) == InIndex.YES;
                    if (sliceStartTxn.get() == null || sliceCommited.get() == null) {
                        sliceStartTxn.set(txn);
                        sliceCommited.set(commited);
                    }
                    if (commited != sliceCommited.get()) {
                        if (!sliceCommited.get()) {
                            StringBuilder s = new StringBuilder();
                            s.append("Found hole, contains ").append(sliceItemCount.get()).append(" sequential transactions (");
                            boolean first = true;
                            for (Entry<String, Integer> entry : sliceServerCount.entrySet()) {
                                if (first) {
                                    first = false;
                                } else {
                                    s.append(", ");
                                }
                                s.append(entry.getKey()).append(" * ").append(entry.getValue());
                            }
                            s.append(")\n  First: ").append(ISO8601DateFormat.format(new Date(sliceStartTxn.get().getCommitTimeMs()))).append(" ")
                                    .append(sliceStartTxn.get().getId())
                                    .append(" ")
                                    .append(sliceStartTxn.get().getVersion()).append(" ").append(sliceStartTxn.get().getServer());
                            s.append("\n  Last : ").append(ISO8601DateFormat.format(new Date(lastTxn.get().getCommitTimeMs()))).append(" ").append(lastTxn.get().getId())
                                    .append(" ")
                                    .append(lastTxn.get().getVersion()).append(" " + lastTxn.get().getServer());
                            LOG.info(s.toString());
                            holes.incrementAndGet();
                        }
                        sliceCommited.set(commited);
                        sliceStartTxn.set(txn);
                        sliceItemCount.set(0);
                        sliceServerCount.clear();
                    }
                    if (!commited) {
                        sliceItemCount.incrementAndGet();
                        String server = txn.getServer().getId() + " " + txn.getServer().getIpAddress();
                        Integer serverCount = sliceServerCount.get(server);
                        if (serverCount == null) {
                            serverCount = 1;
                        } else {
                            serverCount = serverCount + 1;
                        }
                        sliceServerCount.put(server, serverCount);

                        // Add the transaction ID to the buffer
                        txnIdBuffer.add(txn.getId());
                        txnsToReindex.incrementAndGet();
                    }
                    // Reindex if the buffer is full or if there are no more transactions
                    if (!txnIterator.hasNext() || txnIdBuffer.size() >= maxTransactionsPerLuceneCommit) {
                        try {
                            reindexTransactionAsynchronously(txnIdBuffer);
                        } catch (Throwable e) {
                            String msgError = I18NUtil.getMessage(MSG_RECOVERY_ERROR, txn.getId(), e.getMessage());
                            LOG.error(msgError, e);
                        }
                        // Clear the buffer
                        txnIdBuffer.clear();
                    }
                    i.incrementAndGet();
                    if (!txnIterator.hasNext() || i.get() >= 100) {
                        String progressInfo = progress.step(i.get());
                        if (progressInfo != null) {
                            LOG.info("Holes searching: " + progressInfo);
                        }
                        i.set(0);
                    }
                    lastTxn.set(txn);
                    return null;
                }
            }, true);
            // check if we have to terminate
            if (isShuttingDown()) {
                String msgTerminated = I18NUtil.getMessage(MSG_RECOVERY_TERMINATED);
                LOG.warn(msgTerminated);
                return;
            }
        }
        double percent = txnsToReindex.get() * 100 / ((double) txns.size());
        LOG.info(String.format("Found %d holes - %d of %d transactions (%.1f%%) were missing from index and were reindexed", holes.get(), txnsToReindex.get(), txns.size(), percent));
        LOG.info("Waiting for background indexing to complete...");
        waitForAsynchronousReindexing();
        LOG.info("Background indexing completed");
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setMaxRecordSetSize(int maxRecordSetSize) {
        this.maxRecordSetSize = maxRecordSetSize;
    }

    public void setMaxTransactionsPerLuceneCommit(int maxTransactionsPerLuceneCommit) {
        this.maxTransactionsPerLuceneCommit = maxTransactionsPerLuceneCommit;
    }

    public void setLookBackMinutes(long lookBackMinutes) {
        this.lookBackMinutes = lookBackMinutes;
    }

    public long getLookBackMinutes() {
        return lookBackMinutes;
    }

    public void setCustomChangeTxnIds(String customChangeTxnIds) {
        this.customChangeTxnIds = customChangeTxnIds;
    }

    public String getCustomChangeTxnIds() {
        return customChangeTxnIds;
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

}
