package ee.webmedia.alfresco.common.bootstrap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.common.service.GeneralService;

public abstract class AbstractNodeUpdater extends AbstractModuleComponent implements InitializingBean {
    protected final Log log = LogFactory.getLog(getClass());

    protected static final int DEFAULT_BATCH_SIZE = 50;
    protected static final char CSV_SEPARATOR = ';';
    protected static Charset CSV_CHARSET = Charset.forName("UTF-8");

    protected NodeService nodeService;
    protected SearchService searchService;
    protected GeneralService generalService;
    protected BehaviourFilter behaviourFilter;

    protected final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    protected final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
    {
        dateFormat.setLenient(false);
        dateTimeFormat.setLenient(false);
    }

    protected int batchSize = DEFAULT_BATCH_SIZE;
    private boolean enabled = true;
    private File inputFolder;

    private Set<NodeRef> nodes = new HashSet<NodeRef>();
    private Set<NodeRef> completedNodes = new HashSet<NodeRef>();
    private File nodesFile;
    private File completedNodesFile;

    @Override
    public void afterPropertiesSet() throws Exception {
        nodeService = serviceRegistry.getNodeService();
        searchService = serviceRegistry.getSearchService();
        generalService = (GeneralService) serviceRegistry.getService(QName.createQName(null, GeneralService.BEAN_NAME));
        behaviourFilter = (BehaviourFilter) serviceRegistry.getService(QName.createQName(null, "policyBehaviourFilter"));
    }

    private final AtomicBoolean updaterRunning = new AtomicBoolean(false);
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private final AtomicInteger sleepTime = new AtomicInteger(0);

    public boolean isUpdaterRunning() {
        return updaterRunning.get();
    }

    public boolean isUpdaterStopping() {
        return isUpdaterRunning() && stopFlag.get();
    }

    public int getSleepTime() {
        return sleepTime.get();
    }

    public void setSleepTime(int sleepTime) {
        if (this.sleepTime.getAndSet(sleepTime) != sleepTime) {
            log.info("Set sleepTime to " + sleepTime + " ms");
        }
    }

    /** @param event */
    public synchronized void stopUpdater(ActionEvent event) {
        stopUpdater();
    }

    public void stopUpdater() {
        stopFlag.set(true);
        log.info("Stop requested. Stopping after current batch.");
    }

    /** @param event */
    public void updateSleepTime(ActionEvent event) {
        // sleepTime is actually updated in setSleepTime method
    }

    /** @param event */
    public synchronized void executeUpdaterInBackground(ActionEvent event) {
        executeUpdaterInBackground();
    }

    public synchronized void executeUpdaterInBackground() {
        if (!isUpdaterRunning()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        updaterRunning.set(true);
                        stopFlag.set(false);
                        AuthenticationUtil.runAs(new RunAsWork<Void>() {
                            @Override
                            public Void doWork() throws Exception {
                                while (true) {
                                    try {
                                        executeUpdater();
                                        return null;
                                    } catch (Exception e) {
                                        log.error("Background updater error", e);
                                        if (stopFlag.get()) {
                                            return null;
                                        }
                                        Thread.sleep(5000);
                                    }
                                }
                            }
                        }, AuthenticationUtil.getSystemUserName());
                    } finally {
                        updaterRunning.set(false);
                    }
                }
            }, getBaseFileName() + "Thread").start();
        } else {
            log.warn("Updater is already running");
        }
    }

    @Override
    protected void executeInternal() throws Throwable {
        if (!enabled) {
            log.info("Skipping node updater, because it is disabled" + (isExecuteOnceOnly() ? ". It will not be executed again, because executeOnceOnly=true" : ""));
            return;
        }
        executeUpdater();
    }

    protected void executeUpdater() throws Exception {
        log.info("Starting node updater");
        nodesFile = new File(inputFolder, getNodesCsvFileName());
        nodes = loadNodesFromFile(nodesFile, false);
        if (nodes == null) {
            nodes = loadNodesFromRepo();
            if (nodes == null) {
                log.info("Cancelling node update");
                return;
            }
            writeNodesToFile(nodesFile, nodes);
        }
        completedNodesFile = new File(inputFolder, getCompletedNodesCsvFileName());
        completedNodes = loadNodesFromFile(completedNodesFile, true);
        if (completedNodes != null) {
            nodes.removeAll(completedNodes);
            log.info("Removed " + completedNodes.size() + " completed nodes from nodes list, " + nodes.size() + " nodes remained");
        } else {
            completedNodes = new HashSet<NodeRef>();
        }
        log.info("Starting to update " + nodes.size() + " nodes");
        if (nodes.size() > 0) {
            UpdateNodesBatchProgress batchProgress = new UpdateNodesBatchProgress();
            try {
                batchProgress.run();
            } finally {
                log.info("Completed nodes have been written to file " + completedNodesFile.getAbsolutePath());
            }
        }
        log.info("Completed nodes updater");
    }

    final protected String getBaseFileName() {
        return getName();
    }

    private String getNodesCsvFileName() {
        return getBaseFileName() + ".csv";
    }

    private String getCompletedNodesCsvFileName() {
        return getBaseFileName() + "Completed.csv";
    }

    private String getRollbackNodesCsvFileName() {
        return getBaseFileName() + "Rollback-" + dateTimeFormat.format(new Date()) + ".csv";
    }

    private Set<NodeRef> loadNodesFromFile(File file, boolean readHeaders) throws Exception {
        if (!file.exists()) {
            log.info("Skipping loading nodes, file does not exist: " + file.getAbsolutePath());
            return null;
        }

        log.info("Loading nodes from file " + file.getAbsolutePath());

        Set<NodeRef> loadedNodes = new HashSet<NodeRef>();
        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(file)), CSV_SEPARATOR, CSV_CHARSET);
        try {
            if (readHeaders) {
                reader.readHeaders();
            }
            while (reader.readRecord()) {
                NodeRef nodeRef = new NodeRef(reader.get(0));
                loadedNodes.add(nodeRef);
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + loadedNodes.size() + " nodes from file " + file.getAbsolutePath());
        return loadedNodes;
    }

    private void writeNodesToFile(File file, Set<NodeRef> nodesToWrite) throws Exception {
        log.info("Writing " + nodesToWrite.size() + " nodes to file " + file.getAbsolutePath());
        try {
            CsvWriter writer = new CsvWriter(new FileOutputStream(file), CSV_SEPARATOR, CSV_CHARSET);
            try {
                for (NodeRef nodeRef : nodesToWrite) {
                    writer.writeRecord(new String[] { nodeRef.toString() });
                }
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing CSV file '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
        }
    }

    private Set<NodeRef> loadNodesFromRepo() throws Exception {
        log.info("Searching nodes from repository");
        List<ResultSet> resultSets = getNodeLoadingResultSet();
        if (resultSets == null || resultSets.size() == 0) {
            return null;
        }
        Set<NodeRef> nodeSet = new HashSet<NodeRef>();
        try {
            for (ResultSet resultSet : resultSets) {
                log.info("Found " + resultSet.length() + " nodes from repository store "
                        + resultSet.getResultSetMetaData().getSearchParameters().getStores().get(0).getIdentifier()
                        + ", loading...");
                nodeSet.addAll(resultSet.getNodeRefs());
            }
            if (nodeSet.size() == 0) {
                return null;
            }
            return nodeSet;
        } finally {
            for (ResultSet resultSet : resultSets) {
                resultSet.close();
            }
            log.info("Loaded total " + nodeSet.size() + " nodes from repository");
        }
    }

    /**
     * @return the result set of the nodes that need updating
     */
    protected abstract List<ResultSet> getNodeLoadingResultSet() throws Exception;

    private class UpdateNodesBatchProgress extends BatchProgress<NodeRef> {

        public UpdateNodesBatchProgress() {
            origin = nodes;
            completedSize = completedNodes.size();
            processName = "Nodes updating";
        }

        @Override
        void executeBatch() throws Exception {
            updateNodesBatch(batchList);
        }
    }

    private void updateNodesBatch(final List<NodeRef> batchList) throws Exception {
        final List<String[]> batchInfos = new ArrayList<String[]>(batchSize);
        for (NodeRef nodeRef : batchList) {
            if (!nodeService.exists(nodeRef)) {
                batchInfos.add(new String[] { nodeRef.toString(), "nodeDoesNotExist" });
                continue;
            }
            try {
                String[] info = updateNode(nodeRef);
                String[] batchInfo = (String[]) ArrayUtils.add(info, 0, nodeRef.toString());
                batchInfos.add(batchInfo);
            } catch (Exception e) {
                throw new Exception("Error updating node " + nodeRef + ": " + e.getMessage(), e);
            }
            int sleepTime2 = getSleepTime();
            if (sleepTime2 > 0) {
                Thread.sleep(sleepTime2);
            }
        }
        File rollbackNodesFile = new File(inputFolder, getRollbackNodesCsvFileName());
        bindCsvWriteAfterCommit(completedNodesFile, rollbackNodesFile, new CsvWriterClosure() {

            @Override
            public void execute(CsvWriter writer) throws IOException {
                for (String[] info : batchInfos) {
                    writer.writeRecord(info);
                }
            }

            @Override
            public String[] getHeaders() {
                return AbstractNodeUpdater.this.getCsvFileHeaders();
            }
        });
    }

    /**
     * @param nodeRef nodeRef to be updated; only nodes that currently exist are passed here, this check is performed by {@link AbstractNodeUpdater}.
     * @return array of strings to be written into the completed nodes csv file, can be {@code null}; nodref is automatically insterted as the first element by
     *         {@link AbstractNodeUpdater}.
     * @throws Exception
     */
    protected abstract String[] updateNode(NodeRef nodeRef) throws Exception;

    protected String[] getCsvFileHeaders() {
        return new String[] { "nodeRef" };
    }

    protected boolean isRequiresNewTransaction() {
        return true;
    }

    private abstract class BatchProgress<E> {
        Collection<E> origin;
        List<E> batchList;
        @SuppressWarnings("hiding")
        int batchSize = AbstractNodeUpdater.this.batchSize;
        int totalSize;
        int i;
        int completedSize;
        int thisRunCompletedSize;
        long thisRunStartTime;
        long startTime;
        String processName;
        File stopFile;

        private void init() {
            totalSize = completedSize + origin.size();
            thisRunStartTime = System.currentTimeMillis();
            startTime = thisRunStartTime;
            i = 0;
            batchList = new ArrayList<E>(batchSize);
            stopFile = new File(inputFolder, "stop.file");
        }

        private boolean isStopRequested() {
            boolean f = stopFlag.get() || stopFile.exists();
            if (f) {
                log.info("Stop requested. Stopping.");
                stopFlag.set(true);
            }
            return f;
        }

        abstract void executeBatch() throws Exception;

        void executeInTransaction() {
            getTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>() {
                @Override
                public Object execute() throws Throwable {
                    doAfterTransactionBegin();
                    executeBatch();
                    return null;
                }
            }, false, isRequiresNewTransaction());
        }

        private void step() {
            executeInTransaction();
            completedSize += batchList.size();
            thisRunCompletedSize += batchList.size();
            batchList = new ArrayList<E>(batchSize);
            long endTime = System.currentTimeMillis();
            double completedPercent = (completedSize) * 100L / ((double) totalSize);
            double lastDocsPerSec = (i) * 1000L / ((double) (endTime - startTime));
            long thisRunTotalTime = endTime - thisRunStartTime;
            double totalDocsPerSec = (thisRunCompletedSize) * 1000L / ((double) thisRunTotalTime);
            long remainingSize = ((long) totalSize) - ((long) completedSize);
            long divisor = (thisRunCompletedSize) * 60000L;
            int etaMinutes = ((int) (remainingSize * thisRunTotalTime / divisor)) + 1;
            int etaHours = 0;
            if (etaMinutes > 59) {
                etaHours = etaMinutes / 60;
                etaMinutes = etaMinutes % 60;
            }
            String eta = etaMinutes + "m";
            if (etaHours > 0) {
                eta = etaHours + "h " + eta;
            }
            i = 0;
            String info = "%s: %6.2f%% completed - %7d of %7d, %5.1f docs per second (last), %5.1f (total), ETA %s";
            int sleepTime2 = getSleepTime();
            if (sleepTime2 > 0) {
                info += ", sleep n * " + sleepTime2 + " ms";
            }
            log.info(String.format(info, processName,
                    completedPercent, completedSize, totalSize, lastDocsPerSec, totalDocsPerSec, eta));
            startTime = endTime;
        }

        public void run() {
            init();
            if (isStopRequested()) {
                return;
            }
            for (E e : origin) {
                batchList.add(e);
                i++;
                if (i >= batchSize) {
                    step();
                    if (isStopRequested()) {
                        return;
                    }
                }
            }
            step();
        }

    }

    protected void doAfterTransactionBegin() {
        // can be overridden, but it is not necessary to do so
    }

    private static interface CsvWriterClosure {
        void execute(CsvWriter writer) throws IOException;

        String[] getHeaders();
    }

    private static void bindCsvWriteAfterCommit(final File completedFile, final File rollbackFile, final CsvWriterClosure closure) {
        AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
            @Override
            public void afterCommit() {
                try {
                    // Write created documents
                    boolean exists = completedFile.exists();
                    if (!exists) {
                        OutputStream outputStream = new FileOutputStream(completedFile);
                        try {
                            // the Unicode value for UTF-8 BOM, is needed so that Excel would recognise the file in correct encoding
                            outputStream.write("\ufeff".getBytes("UTF-8"));
                        } finally {
                            outputStream.close();
                        }
                    }
                    CsvWriter writer = new CsvWriter(new FileWriter(completedFile, true), CSV_SEPARATOR);
                    try {
                        if (!exists) {
                            writer.writeRecord(closure.getHeaders());
                        }
                        closure.execute(writer);
                    } finally {
                        writer.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error writing file '" + completedFile + "': " + e.getMessage(), e);
                }
            }

            @Override
            public void afterRollback() {
                try {
                    // Write created documents
                    if (rollbackFile.exists()) {
                        throw new RuntimeException("File already exists: " + rollbackFile.getAbsolutePath());
                    }
                    OutputStream outputStream = new FileOutputStream(rollbackFile);
                    try {
                        // the Unicode value for UTF-8 BOM, is needed so that Excel would recognise the file in correct encoding
                        outputStream.write("\ufeff".getBytes("UTF-8"));
                    } finally {
                        outputStream.close();
                    }
                    CsvWriter writer = new CsvWriter(new FileWriter(rollbackFile, true), CSV_SEPARATOR);
                    try {
                        writer.writeRecord(closure.getHeaders());
                        closure.execute(writer);
                    } finally {
                        writer.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error writing file '" + rollbackFile + "': " + e.getMessage(), e);
                }
            }
        });
    }

    // HELPER METHODS
    /** RetryingTransactionHelper that only tries to do things once. */
    private RetryingTransactionHelper getTransactionHelper() {
        RetryingTransactionHelper helper = new RetryingTransactionHelper();
        helper.setMaxRetries(3);
        helper.setTransactionService(serviceRegistry.getTransactionService());
        return helper;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setInputFolderPath(String inputFolderPath) {
        inputFolder = new File(inputFolderPath);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDisabled(boolean disabled) {
        enabled = !disabled;
    }

}
