package ee.webmedia.alfresco.common.bootstrap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

public abstract class AbstractNodeUpdater extends AbstractModuleComponent {
    protected final Log log = LogFactory.getLog(getClass());

    protected static final int BATCH_SIZE = 50;
    protected static final char CSV_SEPARATOR = ';';
    protected static Charset CSV_CHARSET = Charset.forName("UTF-8");
    protected static FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");

    private TransactionService transactionService;

    private File inputFolder;
    private Set<NodeRef> nodes = new HashSet<NodeRef>();
    private Set<NodeRef> completedNodes = new HashSet<NodeRef>();
    private File nodesFile;
    private File completedNodesFile;

    public void setInputFolderPath(String inputFolderPath) {
        this.inputFolder = new File(inputFolderPath);
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    public TransactionService getTransactionService() {
        if(transactionService == null) transactionService = serviceRegistry.getTransactionService();
        return transactionService;
    }

    @Override
    protected void executeInternal() throws Throwable {
        log.info("Starting node updater");
        nodesFile = new File(inputFolder, getNodesCsvFileName());
        nodes = loadNodesFromFile(nodesFile);
        if (nodes == null) {
            nodes = loadNodesFromRepo();
            if (nodes == null) {
                log.info("Cancelling node update");
                return;
            }
            writeNodesToFile(nodesFile, nodes);
        }
        completedNodesFile = new File(inputFolder, getCompletedNodesCsvFileName());
        completedNodes = loadNodesFromFile(completedNodesFile);
        if (completedNodes != null) {
            nodes.removeAll(completedNodes);
            log.info("Removed " + completedNodes.size() + " completed nodes from nodes list, " + nodes.size() + " nodes remained");
        } else {
            completedNodes = new HashSet<NodeRef>();
        }
        log.info("Updating properties of " + nodes.size() + " nodes");
        if (nodes.size() > 0) {
            UpdateNodesBatchProgress batchProgress = new UpdateNodesBatchProgress();
            batchProgress.run();
        }
        log.info("Completed document properties updater");
    }

    protected String getNodesCsvFileName() {
        return getClass().getSimpleName() + ".csv";
    }

    protected String getCompletedNodesCsvFileName() {
        return getClass().getSimpleName() + "Completed.csv";
    }

    protected Set<NodeRef> loadNodesFromFile(File file) throws Exception {
        if (!file.exists()) {
            log.info("Skipping loading nodes, file does not exist: " + file.getAbsolutePath());
            return null;
        }

        log.info("Loading nodes from file " + file.getAbsolutePath());

        Set<NodeRef> loadedNodes = new HashSet<NodeRef>();
        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(file)), CSV_SEPARATOR, CSV_CHARSET);
        try {
            reader.readHeaders();
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

    protected void writeNodesToFile(File file, Set<NodeRef> nodes) throws Exception {
        log.info("Writing " + nodes.size() + " nodes to file " + file.getAbsolutePath());
        try {
            CsvWriter writer = new CsvWriter(new FileOutputStream(file), CSV_SEPARATOR, CSV_CHARSET);
            try {
                for (NodeRef nodeRef : nodes) {
                    writer.writeRecord(new String[] { nodeRef.toString() });
                }
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing CSV file '" + file.getAbsolutePath() + "': " + e.getMessage(), e);
        }
    }

    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        log.info("Loading nodes from repository");
        List<ResultSet> resultSets = getNodeLoadingResultSet();
        if (resultSets == null || resultSets.size() == 0) {
            return null;
        }
        try {
            HashSet<NodeRef> nodeSet = new HashSet<NodeRef>();
            for(ResultSet resultSet : resultSets){
                log.info("Found " + resultSet.length() + " nodes from repository store " 
                        + resultSet.getResultSetMetaData().getSearchParameters().getStores().get(0).getIdentifier() 
                        + ", loading...");                
                nodeSet.addAll(resultSet.getNodeRefs());
            }
            if(nodeSet.size() == 0){
                return null;
            }
            return nodeSet;
        } finally {
            for(ResultSet resultSet : resultSets){
                resultSet.close();
            }
            log.info("Loaded nodes from repository");
        }
    }

    /**
     * @return the result set of the nodes that need updating
     */
    protected abstract List<ResultSet> getNodeLoadingResultSet() throws Exception;

    private class UpdateNodesBatchProgress extends BatchProgress<NodeRef> {

        public UpdateNodesBatchProgress() {
            this.origin = nodes;
            this.completedSize = completedNodes.size();
            processName = "Nodes updating";
        }

        @Override
        void executeBatch() throws Exception {
            updateNodesBatch(batchList);
        }
    }

    protected void updateNodesBatch(final List<NodeRef> batchList) throws Exception {
        final List<String[]> batchInfo = new ArrayList<String[]>(BATCH_SIZE);
        for (NodeRef nodeRef : batchList) {
            String[] info = updateNode(nodeRef);
            if(info != null){
                batchInfo.add(info);
            }
        }
        bindCsvWriteAfterCommit(completedNodesFile, new CsvWriterClosure() {

            @Override
            public void execute(CsvWriter writer) throws IOException {
                for (String[] info : batchInfo) {
                    writer.writeRecord(info);
                    // fixedDocuments.add(documentId);
                }
            }

            @Override
            public String[] getHeaders() {
                return new String[] { "nodeRef" };
            }
        });
    }

    /**
     * 
     * @param nodeRef nodeRef to be updated
     * @return array of strings to be written into the completed nodes file
     * @throws Exception
     * 
     * NB! first check if the node exists
     */
    protected abstract String[] updateNode(NodeRef nodeRef) throws Exception;

    private abstract class BatchProgress<E> {
        Collection<E> origin;
        List<E> batchList;
        int batchSize = BATCH_SIZE;
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
            boolean f = stopFile.exists();
            if (f) {
                log.info("Stop requested. Stopping.");
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
            }, false, true);
        }

        private void step() {
            executeInTransaction();
            completedSize += batchList.size();
            thisRunCompletedSize += batchList.size();
            batchList = new ArrayList<E>(batchSize);
            long endTime = System.currentTimeMillis();
            double completedPercent = completedSize * 100 / ((double) totalSize);
            double lastDocsPerSec = i * 1000 / ((double) (endTime - startTime));
            double totalDocsPerSec = thisRunCompletedSize * 1000 / ((double) (endTime - thisRunStartTime));
            int etaMinutes = (int) (((long)(totalSize - completedSize)) * (endTime - thisRunStartTime) / (long)(thisRunCompletedSize * 60000));
            i = 0;
            log.info(String.format("%s: %6.2f%% completed - %7d of %7d, %5.1f docs per second (last), %5.1f (total), ETA %d min", processName,
                    completedPercent, completedSize, totalSize, lastDocsPerSec, totalDocsPerSec, etaMinutes));
            startTime = endTime;
        }

        public void run() {
            init();
            if (isStopRequested())
                return;
            for (E e : origin) {
                batchList.add(e);
                i++;
                if (i >= batchSize) {
                    step();
                    if (isStopRequested())
                        return;
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

    private static void bindCsvWriteAfterCommit(final File file, final CsvWriterClosure closure) {
        AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
            @Override
            public void afterCommit() {
                try {
                    // Write created documents
                    boolean exists = file.exists();
                    if (!exists) {
                        OutputStream outputStream = new FileOutputStream(file);
                        try {
                            // the Unicode value for UTF-8 BOM, is needed so that Excel would recognise the file in correct encoding
                            outputStream.write("\ufeff".getBytes("UTF-8"));
                        } finally {
                            outputStream.close();
                        }
                    }
                    CsvWriter writer = new CsvWriter(new FileWriter(file, true), CSV_SEPARATOR);
                    try {
                        if (!exists) {
                            writer.writeRecord(closure.getHeaders());
                        }
                        closure.execute(writer);
                    } finally {
                        writer.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error writing file '" + file + "': " + e.getMessage(), e);
                }
            }
        });
    }

    // HELPER METHODS
    /** RetryingTransactionHelper that only tries to do things once. */
    private RetryingTransactionHelper getTransactionHelper() {
        RetryingTransactionHelper helper = new RetryingTransactionHelper();
        helper.setMaxRetries(1);
        helper.setTransactionService(getTransactionService());
        return helper;
    }

}
