package ee.smit.tera;

import ee.smit.tera.model.TeraFilesEntry;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.sharepoint.ImportStatus;
import ee.webmedia.alfresco.utils.FilenameUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static ee.webmedia.alfresco.common.web.BeanHelper.*;
import static ee.webmedia.alfresco.document.file.model.FileModel.Props.DISPLAY_NAME;

public class DataFixProcess {
    private static final Log log = LogFactory.getLog(DataFixProcess.class);
    private ProcessSettings processSettings;

    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private NodeRef rootNodeRef;
    private static final String CREATOR_MODIFIER = "DELTA";
    private final LogEntry logEntry = new LogEntry();
    private final LogService logService = getLogService();

    public DataFixProcess(ProcessSettings processSettings) {
        this.processSettings = processSettings;
        log.info("Start finding files...");

        log.debug("GET NODE SERVICE....");
        this.nodeService = BeanHelper.getNodeService();

        log.debug("GET FILE-FOLDER SERVICE...");
        this.fileFolderService = BeanHelper.getFileFolderService();

        int filesCount = getTeraService().countAllFiles();
        log.info("Files to check... " + filesCount);

        logEntry.setComputerIp("127.0.0.1");
        logEntry.setComputerName("localhost");
        logEntry.setEventDescription("SHA-1 ületembeldamine - datafix");
        logEntry.setLevel(LogObject.DOCUMENT.getLevel());
        logEntry.setObjectName(LogObject.DOCUMENT.getObjectName());
        logEntry.setCreatorId(CREATOR_MODIFIER);
        logEntry.setCreatorName(CREATOR_MODIFIER);

        // DO SOMETHINGS....

        // DONE!


    }

    public boolean doBatch() {
        List<String> processedFiles = new ArrayList<>();


        final List<TeraFilesEntry> entries = getTeraService().getTeraFilesEntrys(processSettings.getBatchSize(), 0);

        try {
            processedFiles = getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<List<String>>() {
                @Override
                public List<String> execute() throws Throwable {
                    return processFiles(entries);
                }
            });
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        } finally {
            updateTeraFilesListStatus(processedFiles);
        }
        return processedFiles == null || !processedFiles.isEmpty();
    }

    private void updateTeraFilesListStatus(List<String> processedFiles) {
        for (String nodeRef : processedFiles) {
            log.info("UPDATED NODE_REF: " + nodeRef);
        }

    }

    private List<String> processFiles(List<TeraFilesEntry> entries) throws IOException {
        if (entries.isEmpty()) {
            log.info("FILES TO PROCESS... 0");
            return Collections.emptyList();
        }

        log.info("FILES TO PROCESS... " + entries.size());

        final List<String> filesProcessed = new ArrayList<>();
        final List<Callable<String>> tasks = new ArrayList<>();


        int threadCount = Math.min(
                Runtime.getRuntime().availableProcessors(),
                entries.size()
        );

        if (getDigiSignService().getMaxThreads() > 0) {
            threadCount = Math.min(getDigiSignService().getMaxThreads(), entries.size());
        }

        log.info("THREAD COUNT: " + threadCount);

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            for (final TeraFilesEntry entry : entries) {
                tasks.add(
                        new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                return checkFileEntry(entry);
                            }
                        }
                );
            }

            for (final Future<String> f : executor.invokeAll(tasks/*, timeout? */)) {
                try {
                    // If you added a timeout, you can check f.isCancelled().
                    // If timed out, get() will throw a CancellationException.
                    if (f != null) {
                        filesProcessed.add(f.get());
                    } else {

                    }
                } catch (final ExecutionException e) {
                    // This request failed.  Handle it however you like.
                    log.error(e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            executor.shutdown();
        }


        return filesProcessed;
    }

    private String checkFileEntry(TeraFilesEntry entry) {
        String nodeRefString = entry.getNodeRef();
        NodeRef fileNodeRef = new NodeRef(nodeRefString);
        FileInfo fileInfo = fileFolderService.getFileInfo(fileNodeRef);

        String contentPath = fileInfo.getContentData().getContentUrl();
        String fileName = getTeraService().getAndFixFilename(fileInfo);

        String fileExt = FilenameUtil.getDigiDocExt(fileName);

        Map<QName, Serializable> properties = fileInfo.getProperties();
        String displayName = (String) properties.get(DISPLAY_NAME);

        log.info("File nodeRef: " + fileNodeRef + ", filename: " + fileName + ", displayName: " + displayName);

        String statusInfo = "";
        if (!checkFileContent(fileInfo)) {
            statusInfo = "File check failed! NO DATA FOUND!";
            log.warn(statusInfo);
            getTeraService().updateTeraFilesRow(fileNodeRef.toString(), fileName, fileExt, "", statusInfo, true, false);
            return null;
        }

        return nodeRefString;
    }

    private boolean checkFileContent(FileInfo fileInfo) {
        if (BeanHelper.getDigiSignSearches().getContent(fileInfo.getNodeRef()) == null) {
            log.error("FILE CONTENTDATA IS NULL!...");
            return false;
        }
        return true;
    }


    private void getNodeProperties(NodeRef nodeRef) {
        FileInfo fileInfo = fileFolderService.getFileInfo(nodeRef);
        log.debug("GET NODE PROPERTIES... " + nodeRef.toString());
        Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
        getTeraService().fixModifier(nodeService, fileFolderService, nodeRef, CREATOR_MODIFIER);
    }

}
