package ee.webmedia.alfresco.common.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.domain.Transaction;
import org.alfresco.repo.node.index.AbstractReindexComponent;
import org.alfresco.util.ISO8601DateFormat;

import ee.webmedia.alfresco.utils.ProgressTracker;

/**
 * @author Alar Kvell
 */
public class CustomReindexComponent extends AbstractReindexComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(CustomReindexComponent.class);

    private static final String MSG_RECOVERY_TERMINATED = "index.recovery.terminated";
    private static final String MSG_RECOVERY_ERROR = "index.recovery.error";

    boolean enabled = true;
    private int maxRecordSetSize;
    private int maxTransactionsPerLuceneCommit;
    private long lookBackMinutes = 1440; // 24 hours

    @Override
    protected void reindexImpl() {
        List<Transaction> txns = getTransactions();
        if (txns.isEmpty()) {
            return;
        }

        searchHolesAndIndex(txns);
    }

    private List<Transaction> getTransactions() {
        long endTimeMs = System.currentTimeMillis();
        long startTimeMs = endTimeMs - (lookBackMinutes * 60000);
        LOG.info("Finding transactions from " + ISO8601DateFormat.format(new Date(startTimeMs)) + " to " + ISO8601DateFormat.format(new Date(endTimeMs)) + "...");
        List<Long> excludeTxnIds = new ArrayList<Long>();
        List<Transaction> allTxns = new ArrayList<Transaction>();
        List<Transaction> txns;
        do {
            if (LOG.isDebugEnabled()) {
                LOG.info("Query transactions from " + ISO8601DateFormat.format(new Date(startTimeMs)) + " to " + ISO8601DateFormat.format(new Date(endTimeMs)) + " (max "
                        + maxRecordSetSize + ", exclude " + excludeTxnIds + ")");
            }
            txns = nodeDaoService.getTxnsByCommitTimeAscending(startTimeMs, endTimeMs, maxRecordSetSize, excludeTxnIds, false);
            if (LOG.isDebugEnabled()) {
                LOG.info("Found " + txns.size() + " transactions");
            }
            if (txns.isEmpty()) {
                break;
            }
            if (LOG.isDebugEnabled()) {
                LOG.info("First: " + getTransactionInfo(txns.get(0)));
                LOG.info("Last : " + getTransactionInfo(txns.get(txns.size() - 1)));
            }

            allTxns.addAll(txns);
            if (!txns.isEmpty()) {
                startTimeMs = txns.get(txns.size() - 1).getCommitTimeMs();
                excludeTxnIds = new ArrayList<Long>();
                for (int i = txns.size() - 1; i >= 0; i--) {
                    Transaction txn = txns.get(i);
                    if (txn.getCommitTimeMs() < startTimeMs) {
                        break;
                    }
                    excludeTxnIds.add(txn.getId());
                }
            }
        } while (!txns.isEmpty());

        LOG.info("Found total " + allTxns.size() + " transactions during that time period");
        if (allTxns.isEmpty()) {
            return allTxns;
        }
        Transaction firstTxn = allTxns.get(0);
        LOG.info("First: " + getTransactionInfo(firstTxn));
        Transaction lastTxn = allTxns.get(allTxns.size() - 1);
        LOG.info("Last : " + getTransactionInfo(lastTxn));
        return allTxns;
    }

    private String getTransactionInfo(Transaction txn) {
        return ISO8601DateFormat.format(new Date(txn.getCommitTimeMs())) + " " + txn.getId() + " " + txn.getVersion() + " " + txn.getServer();
    }

    private void searchHolesAndIndex(List<Transaction> txns) {
        LOG.info("Searching for holes and starting background indexing...");
        Transaction sliceStartTxn = null;
        Transaction lastTxn = null;
        Boolean sliceCommited = null;
        int sliceItemCount = 0;
        Map<String, Integer> sliceServerCount = new HashMap<String, Integer>();
        ProgressTracker progress = new ProgressTracker(txns.size(), 0);
        int i = 0;
        int txnsToReindex = 0;
        int holes = 0;
        List<Long> txnIdBuffer = new ArrayList<Long>(maxTransactionsPerLuceneCommit);
        Iterator<Transaction> txnIterator = txns.iterator();
        while (txnIterator.hasNext()) {
            Transaction txn = txnIterator.next();
            boolean commited = isTxnPresentInIndex(txn) == InIndex.YES;
            if (sliceStartTxn == null || sliceCommited == null) {
                sliceStartTxn = txn;
                sliceCommited = commited;
            }
            if (commited != sliceCommited) {
                if (!sliceCommited) {
                    StringBuilder s = new StringBuilder();
                    s.append("Found hole, contains ").append(sliceItemCount).append(" sequential transactions (");
                    boolean first = true;
                    for (Entry<String, Integer> entry : sliceServerCount.entrySet()) {
                        if (first) {
                            first = false;
                        } else {
                            s.append(", ");
                        }
                        s.append(entry.getKey()).append(" * ").append(entry.getValue());
                    }
                    s.append(")\n  First: ").append(ISO8601DateFormat.format(new Date(sliceStartTxn.getCommitTimeMs()))).append(" ").append(sliceStartTxn.getId()).append(" ")
                            .append(sliceStartTxn.getVersion()).append(" ").append(sliceStartTxn.getServer());
                    s.append("\n  Last : ").append(ISO8601DateFormat.format(new Date(lastTxn.getCommitTimeMs()))).append(" ").append(lastTxn.getId()).append(" ")
                            .append(lastTxn.getVersion()).append(" " + lastTxn.getServer());
                    LOG.info(s.toString());
                    holes++;
                }
                sliceCommited = commited;
                sliceStartTxn = txn;
                sliceItemCount = 0;
                sliceServerCount = new HashMap<String, Integer>();
            }
            if (!commited) {
                sliceItemCount++;
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
                txnsToReindex++;
            }
            // check if we have to terminate
            if (isShuttingDown()) {
                String msgTerminated = I18NUtil.getMessage(MSG_RECOVERY_TERMINATED);
                LOG.warn(msgTerminated);
                return;
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
                txnIdBuffer = new ArrayList<Long>(maxTransactionsPerLuceneCommit);
            }
            i++;
            if (!txnIterator.hasNext() || i >= 100) {
                String progressInfo = progress.step(i);
                if (progressInfo != null) {
                    LOG.info("Holes searching: " + progressInfo);
                }
                i = 0;
            }
            lastTxn = txn;
        }
        double percent = txnsToReindex * 100 / ((double) txns.size());
        LOG.info(String.format("Found %d holes - %d of %d transactions (%.1f%%) were missing from index and were reindexed", holes, txnsToReindex, txns.size(), percent));
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

}
