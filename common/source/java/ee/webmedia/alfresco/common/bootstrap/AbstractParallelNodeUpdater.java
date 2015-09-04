package ee.webmedia.alfresco.common.bootstrap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.CollectionUtils;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.utils.ProgressTracker;

/**
 * Same as {@link AbstractNodeUpdater} but updates nodes in multiple threads.
 * By default creates (CPU core count - 1) threads or a single thread if CPU only has one core.
 * <b>Can only be used if updatable node set can be split into independent subsets.</b>
 */
public abstract class AbstractParallelNodeUpdater extends AbstractNodeUpdater {

    {
        threadCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    }

    @Override
    protected void initializeBeforeUpdating() throws Exception {
        prepareForUpdating();
        super.initializeBeforeUpdating();
        int nodesSize = nodes == null ? 0 : nodes.size();
        if (nodesSize <= threadCount) {
            threadCount = Math.max(1, nodesSize);
        }
    }

    /**
     * Method for fulfilling prerequisites (in a single thread) to be able to update nodes in parallel.
     * For example, creating new QNames in database, etc.</br>
     * By default does nothing, so subclasses must override to achieve desired behaviour.
     */
    protected void prepareForUpdating() {
        // subclass must override
    }

    @Override
    protected void executeUpdater() throws Exception {
        initializeBeforeUpdating();
        if (CollectionUtils.isEmpty(nodes)) {
            log.info("Did not find any nodes to update");
            return;
        }
        String infoText = "Starting to update " + nodes.size() + " nodes";
        if (isMultithreaded()) {
            infoText += String.format(" in %d threads", threadCount);
        }
        log.info(infoText);

        if (nodes.size() > 0) {
            ProgressTracker progress = new ProgressTracker(nodes.size(), 0);
            List<NodeRef> nodeList = new ArrayList<>(nodes);
            int subListSize = nodeList.size() / threadCount;
            int remainingItems = nodeList.size() % threadCount;

            CountDownLatch latch = new CountDownLatch(threadCount);

            int startedThreadCount = 0;

            int start = 0;
            int end = 0;
            Map<String, Exception> exceptions = new ConcurrentHashMap<>();
            for (int i = 0; i < threadCount; i++) {
                start = end;
                if (start >= nodeList.size()) {
                    break;
                }
                boolean addExtra = i < remainingItems;
                end = Math.min((i + 1) * subListSize + (addExtra ? 1 : 0), nodeList.size());
                List<NodeRef> subList = nodeList.subList(start, end);
                Set<NodeRef> threadNodes = new HashSet<>(subList);
                File completedNodesFile = completedNodesFiles.get(i);
                File failedNodesFile = failedNodesFiles.get(i);
                Thread updater = new UpdaterThread(getName() + THREAD_SUFFIX + i, threadNodes, progress, completedNodesFile, failedNodesFile, latch, exceptions);

                try {
                    updater.start();
                    startedThreadCount++;
                } catch (RuntimeException e) {
                    log.error("Failed to submit updater job", e);
                    throw e;
                }
            }

            while (startedThreadCount < threadCount) {
                latch.countDown();
                threadCount--;
            }
            latch.await();
            if (!exceptions.isEmpty()) {
                for (Map.Entry<String, Exception> entry : exceptions.entrySet()) {
                    log.error("Updater " + getName() + " thread " + entry.getKey() + " throw unhandled exception " + entry.getValue());
                }
                throw new RuntimeException("Updater failed to execute, because one or more updater threads failed to perform update, see previous log for detailed error messages.");
            }
            log.info("Merging separate thread files together");
            mergeCsvFiles(getCompletedNodesCsvFileName());
            mergeCsvFiles(getFailedNodesCsvFileName());
        }
        log.info("Completed nodes updater");
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    private boolean isMultithreaded() {
        return threadCount > 1;
    }

    private void mergeCsvFiles(String fileName) {
        List<File> inputFiles = getFiles(FilenameUtils.removeExtension(fileName));
        if (CollectionUtils.isEmpty(inputFiles)) {
            return;
        }
        File resultFile = new File(inputFolder, fileName);
        log.info("Creating " + resultFile.getAbsolutePath());
        boolean safeToDeleteThreadFiles = true;
        CsvWriter writer = null;
        try {
            writer = new CsvWriter(new FileOutputStream(resultFile), CSV_SEPARATOR, CSV_CHARSET);
            for (File inputFile : inputFiles) {
                if (!inputFile.exists()) {
                    continue;
                }
                CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(inputFile)), CSV_SEPARATOR, CSV_CHARSET);
                try {
                    reader.readHeaders();
                    while (reader.readRecord()) {
                        writer.writeRecord(reader.getValues());
                    }
                } catch (IOException e) {
                    safeToDeleteThreadFiles = false;
                    log.error("Unable to read file " + inputFile.getAbsolutePath());
                    continue;
                } finally {
                    reader.close();
                }
            }
        } catch (FileNotFoundException e) {
            log.error("Unable to write to file " + fileName, e);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
        if (safeToDeleteThreadFiles) {
            deleteFiles(inputFiles);
        }
    }

    private class UpdateNodesParallelBatchProgress extends UpdateNodesBatchProgress {

        private final File completedNodesFile;
        private final File failedNodesFile;

        public UpdateNodesParallelBatchProgress(Set<NodeRef> nodes, ProgressTracker progress, File completedNodesFile, File failedNodesFiles) {
            origin = nodes;
            processName = "Nodes updating";
            this.progress = progress;
            this.completedNodesFile = completedNodesFile;
            failedNodesFile = failedNodesFiles;
        }

        @Override
        void executeBatch() throws Exception {
            updateNodesBatch(batchList, completedNodesFile, failedNodesFile);
        }

    }

    private class UpdaterThread extends Thread {

        private final Set<NodeRef> nodes;
        private final ProgressTracker progress;
        private final File completedNodesFile;
        private final File failedNodesFile;
        private final CountDownLatch latch;
        private final Map<String, Exception> exceptions;

        private UpdaterThread(String name, Set<NodeRef> nodes, ProgressTracker progress, File completedNodesFile, File failedNodesFile, CountDownLatch latch,
                              Map<String, Exception> exceptions) {
            setName(name);
            this.nodes = nodes;
            this.progress = progress;
            this.completedNodesFile = completedNodesFile;
            this.failedNodesFile = failedNodesFile;
            this.latch = latch;
            this.exceptions = exceptions;
        }

        @Override
        public void run() {
            try {
                log.info(String.format("Starting thread %s with %d nodes", getName(), nodes.size()));
                UpdateNodesParallelBatchProgress batchProgress = new UpdateNodesParallelBatchProgress(nodes, progress, completedNodesFile, failedNodesFile);
                batchProgress.run();
                log.info("Finishing " + getName());
            } catch (Exception e) {
                exceptions.put(getName(), e);
                throw e;
            } finally {
                latch.countDown();
            }
        }

    }

}
