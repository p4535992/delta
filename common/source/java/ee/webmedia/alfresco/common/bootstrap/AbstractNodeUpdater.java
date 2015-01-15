package ee.webmedia.alfresco.common.bootstrap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.ProgressTracker;

public abstract class AbstractNodeUpdater extends AbstractModuleComponent implements InitializingBean {

    protected final Log log = LogFactory.getLog(getClass());

    protected static final int DEFAULT_BATCH_SIZE = 50;
    protected static final char CSV_SEPARATOR = ';';
    protected static final Charset CSV_CHARSET = Charset.forName("UTF-8");

    protected NodeService nodeService;
    protected SearchService searchService;
    protected GeneralService generalService;
    protected BehaviourFilter behaviourFilter;

    protected final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
    protected final FastDateFormat dateTimeFormat = FastDateFormat.getInstance("yyyy-MM-dd-HH-mm-ss-SSS");

    protected int batchSize = DEFAULT_BATCH_SIZE;
    private boolean enabled = true;

    private int transactionHelperMinRetryWaits = -1;
    protected File inputFolder;

    protected Set<NodeRef> nodes = Collections.newSetFromMap(new ConcurrentHashMap<NodeRef, Boolean>());
    private Set<NodeRef> completedNodes = Collections.newSetFromMap(new ConcurrentHashMap<NodeRef, Boolean>());

    protected List<File> failedNodesFiles;
    protected List<File> completedNodesFiles;

    private boolean errorExecutingUpdaterInBackground;

    protected int threadCount = 1;

    private static final String CSV_EXTENSION = ".csv";
    protected static final String THREAD_SUFFIX = "_thread-";

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
                                        errorExecutingUpdaterInBackground = true;
                                        if (!isRetryUpdaterInBackground() || stopFlag.get()) {
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

    protected boolean isRetryUpdaterInBackground() {
        return false;
    }

    @Override
    protected void executeInternal() throws Throwable {
        if (!enabled) {
            log.info("Skipping node updater, because it is disabled" + (isExecuteOnceOnly() ? ". It will not be executed again, because executeOnceOnly=true" : ""));
            return;
        }
        executeUpdater();
    }

    protected boolean usePreviousState() {
        return true;
    }

    protected boolean usePreviousInputState() {
        return usePreviousState();
    }

    protected boolean usePreviousCompletedState() {
        return usePreviousState();
    }

    protected void initializeBeforeUpdating() throws Exception {
        log.info("Initializing values");
        try {
            failedNodesFiles = new CopyOnWriteArrayList<>();
            completedNodesFiles = new CopyOnWriteArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                File tempFailedNodesFile = new File(inputFolder, getFailedNodesCsvFileName(i));
                File tempCompletedNodesFile = new File(inputFolder, getCompletedNodesCsvFileName(i));
                failedNodesFiles.add(tempFailedNodesFile);
                completedNodesFiles.add(tempCompletedNodesFile);
            }

            File nodesFile = new File(inputFolder, getNodesCsvFileName());
            nodes = null;
            if (usePreviousInputState()) {
                nodes = loadNodesFromFile(nodesFile, false);
            }
            if (nodes == null) {
                nodes = loadNodesFromRepo();
                if (nodes == null) {
                    log.info("Cancelling node update");
                    return;
                }
                writeNodesToFile(nodesFile, nodes);
            }
            completedNodes = null;
            if (usePreviousCompletedState()) {
                completedNodes = loadNodesFromFile(FilenameUtils.removeExtension(getCompletedNodesCsvFileName()), true);
            } else {
                List<File> completedFiles = getFiles(FilenameUtils.removeExtension(getCompletedNodesCsvFileName()));
                log.info("Found " + completedFiles.size() + " completed nodes files");
                deleteFiles(completedFiles);

                List<File> failedNodesFiles = getFiles(FilenameUtils.removeExtension(getFailedNodesCsvFileName()));
                log.info("Found " + failedNodesFiles.size() + " failed nodes files");
                deleteFiles(failedNodesFiles);
            }
            if (completedNodes != null) {
                nodes.removeAll(completedNodes);
                log.info("Removed " + completedNodes.size() + " completed nodes from nodes list, " + nodes.size() + " nodes remained");
            } else {
                completedNodes = Collections.newSetFromMap(new ConcurrentHashMap<NodeRef, Boolean>());
            }
        } catch (Exception e) {
            stopFlag.set(true);
            throw e;
        }
    }

    protected void deleteFiles(List<File> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        for (File file : files) {
            if (file.exists()) {
                log.info("Deleting: " + file.getAbsolutePath());
                file.delete();
            }
        }
    }

    protected void executeUpdater() throws Exception {
        initializeBeforeUpdating();
        if (CollectionUtils.isEmpty(nodes)) {
            log.info("Did not find any nodes to update");
            return;
        }
        log.info("Starting to update " + nodes.size() + " nodes");
        UpdateNodesBatchProgress batchProgress = new UpdateNodesBatchProgress();
        try {
            batchProgress.run();
        } finally {
            log.info("Completed nodes have been written to file " + completedNodesFiles.get(0).getAbsolutePath());
        }
        log.info("Completed nodes updater");
    }

    protected int getThreadCount() {
        return threadCount;
    }

    final protected String getBaseFileName() {
        return getName();
    }

    private String getNodesCsvFileName() {
        return getBaseFileName() + CSV_EXTENSION;
    }

    private String getFailedNodesCsvFileName(int thread) {
        return getBaseFileName() + "Failed" + THREAD_SUFFIX + thread + CSV_EXTENSION;
    }

    protected String getFailedNodesCsvFileName() {
        return getBaseFileName() + "Failed.csv";
    }

    protected String getCompletedNodesCsvFileName() {
        return getBaseFileName() + "Completed.csv";
    }

    private String getCompletedNodesCsvFileName(int thread) {
        return getBaseFileName() + "Completed" + THREAD_SUFFIX + thread + CSV_EXTENSION;
    }

    private String getRollbackNodesCsvFileName() {
        return getBaseFileName() + "Rollback-" + dateTimeFormat.format(new Date()) + Thread.currentThread().getName() + CSV_EXTENSION;
    }

    protected List<File> getFiles(final String filenameWithoutExtension) {
        File folder = inputFolder;
        File mainFile = new File(folder, filenameWithoutExtension + CSV_EXTENSION);

        File[] threadFiles = folder.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(filenameWithoutExtension + THREAD_SUFFIX);
            }
        });

        List<File> files = new ArrayList<>();
        if (mainFile.exists()) {
            log.info("Found file: " + mainFile.getAbsolutePath());
            files.add(mainFile);
        }
        if (threadFiles != null && threadFiles.length > 0) {
            log.info(String.format("Found %d files with prefix %s", threadFiles.length, filenameWithoutExtension + THREAD_SUFFIX));
            files.addAll(Arrays.asList(threadFiles));
        }
        return files;
    }

    protected Set<NodeRef> loadNodesFromFile(final String filenameWithoutExtension, boolean readHeaders) throws Exception {
        Set<NodeRef> loadedNodes = null;
        List<File> files = getFiles(filenameWithoutExtension);
        if (CollectionUtils.isNotEmpty(files)) {
            loadedNodes = Collections.newSetFromMap(new ConcurrentHashMap<NodeRef, Boolean>());
            for (File file : files) {
                log.info("Processing file: " + file.getAbsolutePath());
                Set<NodeRef> loaded = loadNodesFromFile(file, readHeaders);
                if (loaded != null) {
                    loadedNodes.addAll(loaded);
                }
            }
        }
        return CollectionUtils.isNotEmpty(loadedNodes) ? loadedNodes : null;
    }

    protected Set<NodeRef> loadNodesFromFile(File file, boolean readHeaders) throws Exception {
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
        List<String[]> records = new ArrayList<>(nodesToWrite.size());
        for (NodeRef nodeRef : nodesToWrite) {
            records.add(new String[] { nodeRef.toString() });
        }
        writeRecordsToCsvFile(file, records);
    }

    public static void writeRecordsToCsvFile(File file, List<String[]> records) {
        try {
            CsvWriter writer = new CsvWriter(new FileOutputStream(file), CSV_SEPARATOR, CSV_CHARSET);
            try {
                for (String[] record : records) {
                    writer.writeRecord(record);
                }
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing CSV file '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
        }
    }

    public static void writeRecordsToCsvFile(File file, Collection<String> records) {
        try {
            CsvWriter writer = new CsvWriter(new FileOutputStream(file), CSV_SEPARATOR, CSV_CHARSET);
            try {
                for (String record : records) {
                    writer.writeRecord(new String[] { record });
                }
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing CSV file '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
        }
    }

    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        log.info("Searching nodes from repository");
        List<ResultSet> resultSets = getNodeLoadingResultSet();
        if (resultSets == null || resultSets.size() == 0) {
            return null;
        }
        Set<NodeRef> nodeSet = Collections.newSetFromMap(new ConcurrentHashMap<NodeRef, Boolean>());
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

    protected class UpdateNodesBatchProgress extends BatchProgress<NodeRef> {

        public UpdateNodesBatchProgress() {
            origin = nodes;
            processName = "Nodes updating";
            progress = new ProgressTracker(completedNodes.size() + origin.size(), completedNodes.size());
        }

        @Override
        void executeBatch() throws Exception {
            updateNodesBatch(batchList, completedNodesFiles.get(0), failedNodesFiles.get(0));
        }
    }

    protected boolean processOnlyExistingNodeRefs() {
        return true;
    }

    protected void updateNodesBatch(final List<NodeRef> batchList, File completedNodesFile, File failedNodesFile) throws Exception {
        final List<String[]> batchInfos = processNodes(batchList, failedNodesFile);
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

    protected List<String[]> processNodes(final List<NodeRef> batchList, File failedNodesFile) throws Exception, InterruptedException {
        final List<String[]> batchInfos = new ArrayList<>(batchList.size());
        for (NodeRef nodeRef : batchList) {
            if (processOnlyExistingNodeRefs() && !nodeService.exists(nodeRef)) {
                batchInfos.add(new String[] { nodeRef.toString(), "nodeDoesNotExist" });
                continue;
            }
            try {
                String[] info = updateNode(nodeRef);
                String[] batchInfo = (String[]) ArrayUtils.add(info, 0, nodeRef.toString());
                batchInfos.add(batchInfo);
            } catch (Exception e) {
                handleNodeProcessingError(Collections.singletonList(nodeRef), e, failedNodesFile);
            }
            int sleepTime2 = getSleepTime();
            if (sleepTime2 > 0) {
                Thread.sleep(sleepTime2);
            }
        }
        return batchInfos;
    }

    protected void handleNodeProcessingError(List<NodeRef> nodeRefs, Exception e, File failedNodesFile) throws Exception {
        try {
            boolean exists = failedNodesFile.exists();
            if (!exists) {
                OutputStream outputStream = new FileOutputStream(failedNodesFile);
                try {
                    // the Unicode value for UTF-8 BOM, is needed so that Excel would recognise the file in correct encoding
                    outputStream.write("\ufeff".getBytes("UTF-8"));
                } finally {
                    outputStream.close();
                }
            }
            CsvWriter writer = new CsvWriter(new FileWriter(failedNodesFile, true), CSV_SEPARATOR);
            try {
                if (!exists) {
                    writer.writeRecord(new String[] { "nodeRef", "error" });
                }
                for (NodeRef nodeRef : nodeRefs) {
                    writer.writeRecord(new String[] { nodeRef.toString(), e.toString() });
                }
            } finally {
                writer.close();
            }
        } catch (IOException e1) {
            log.error("Error writing file '" + failedNodesFile + "': " + e.getMessage(), e);
        }
        throw new Exception("Error updating nodes " + (nodeRefs.size() == 1 ? nodeRefs.get(0) : nodeRefs) + ": " + e.getMessage(), e);
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

    @Override
    public boolean isRequiresNewTransaction() {
        return false;
    }

    public boolean isTransactionReadOnly() {
        return false;
    }

    public boolean isContinueWithNextBatchAfterError() {
        return false;
    }

    private abstract class BatchProgress<E> {
        Collection<E> origin;
        List<E> batchList;
        int batchSize = AbstractNodeUpdater.this.batchSize;
        String processName;
        File stopFile;
        ProgressTracker progress;

        private void init() {
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
            try {
                getTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>() {
                    @Override
                    public Object execute() throws Throwable {
                        doAfterTransactionBegin();
                        executeBatch();
                        return null;
                    }
                }, isTransactionReadOnly(), true);
            } catch (RuntimeException e) {
                if (isContinueWithNextBatchAfterError()) {
                    log.error("Error updating node; continuing updating next batch.", e);
                    int sleepTime2 = getSleepTime();
                    if (sleepTime2 > 0) {
                        try {
                            Thread.sleep(sleepTime2);
                        } catch (InterruptedException e2) {
                            throw new RuntimeException(e2);
                        }
                    }
                } else {
                    throw e;
                }
            }
        }

        private void step() {
            executeInTransaction();
            String info = progress.step(batchList.size());
            if (info != null) {
                info = processName + ": " + info;
                int sleepTime2 = getSleepTime();
                if (sleepTime2 > 0) {
                    info += ", sleep n * " + sleepTime2 + " ms";
                }
                log.info(info);
            }
            batchList = new ArrayList<E>(batchSize);
        }

        public void run() {
            init();
            if (isStopRequested()) {
                return;
            }
            for (E e : origin) {
                batchList.add(e);
                if (batchList.size() >= batchSize) {
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

    protected static interface CsvWriterClosure {
        void execute(CsvWriter writer) throws IOException;

        String[] getHeaders();
    }

    protected void bindCsvWriteAfterCommit(final File completedFile, final File rollbackFile, final CsvWriterClosure closure) {
        AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
            @Override
            public void afterCommit() {
                executeAfterCommit(completedFile, closure);
            }

            @Override
            public void afterRollback() {
                if (rollbackFile == null) {
                    return;
                }
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

    protected void executeAfterCommit(final File completedFile, final CsvWriterClosure closure) {
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

    // HELPER METHODS
    /** RetryingTransactionHelper that only tries to do things once. */
    private RetryingTransactionHelper getTransactionHelper() {
        RetryingTransactionHelper helper = new RetryingTransactionHelper();
        helper.setMaxRetries(3);
        helper.setTransactionService(serviceRegistry.getTransactionService());
        if (transactionHelperMinRetryWaits > 0) {
            helper.setMinRetryWaitMs(transactionHelperMinRetryWaits);
        }
        return helper;
    }

    public int getBatchSize() {
        return batchSize;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setDisabled(boolean disabled) {
        enabled = !disabled;
    }

    public boolean isErrorExecutingUpdaterInBackground() {
        return errorExecutingUpdaterInBackground;
    }

    public void setTransactionHelperMinRetryWaits(int transactionHelperMinRetryWaits) {
        this.transactionHelperMinRetryWaits = transactionHelperMinRetryWaits;
    }

}
