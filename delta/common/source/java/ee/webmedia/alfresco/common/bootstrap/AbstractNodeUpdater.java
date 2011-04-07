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
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.apache.commons.lang.ArrayUtils;
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

    protected NodeService nodeService;

    private File inputFolder;
    private Set<NodeRef> nodes = new HashSet<NodeRef>();
    private Set<NodeRef> completedNodes = new HashSet<NodeRef>();
    private File nodesFile;
    private File completedNodesFile;

    public void setInputFolderPath(String inputFolderPath) {
        inputFolder = new File(inputFolderPath);
    }

    @Override
    protected void executeInternal() throws Throwable {
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

    protected String getNodesCsvFileName() {
        return getClass().getSimpleName() + ".csv";
    }

    protected String getCompletedNodesCsvFileName() {
        return getClass().getSimpleName() + "Completed.csv";
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

    protected void updateNodesBatch(final List<NodeRef> batchList) throws Exception {
        final List<String[]> batchInfos = new ArrayList<String[]>(BATCH_SIZE);
        for (NodeRef nodeRef : batchList) {
            if (!nodeService.exists(nodeRef)) {
                batchInfos.add(new String[] { nodeRef.toString(), "nodeDoesNotExist" });
                continue;
            }
            String[] info = updateNode(nodeRef);
            if (info != null) {
                String[] batchInfo = (String[]) ArrayUtils.add(info, 0, nodeRef.toString());
                batchInfos.add(batchInfo);
            }
        }
        bindCsvWriteAfterCommit(completedNodesFile, new CsvWriterClosure() {

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
            double completedPercent = ((long) completedSize) * 100L / ((double) totalSize);
            double lastDocsPerSec = ((long) i) * 1000L / ((double) (endTime - startTime));
            long thisRunTotalTime = endTime - thisRunStartTime;
            double totalDocsPerSec = ((long) thisRunCompletedSize) * 1000L / ((double) thisRunTotalTime);
            long remainingSize = ((long) totalSize) - ((long) completedSize);
            long divisor = ((long) thisRunCompletedSize) * 60000L;
            int etaMinutes = ((int) (remainingSize  *  thisRunTotalTime / divisor)) + 1;
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
            log.info(String.format("%s: %6.2f%% completed - %7d of %7d, %5.1f docs per second (last), %5.1f (total), ETA %s", processName,
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
        helper.setTransactionService(serviceRegistry.getTransactionService());
        return helper;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
