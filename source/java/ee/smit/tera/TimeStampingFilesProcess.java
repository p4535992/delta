package ee.smit.tera;

import ee.smit.tera.model.TeraFilesEntry;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.sharepoint.ImportStatus;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.SignatureItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static ee.webmedia.alfresco.common.web.BeanHelper.*;
import static ee.webmedia.alfresco.document.file.model.FileModel.Props.DISPLAY_NAME;

public class TimeStampingFilesProcess {
    private static final Log log = LogFactory.getLog(TimeStampingFilesProcess.class);
    private final ImportStatus status;
    private FileFolderService fileFolderService;
    private ProcessSettings processSettings;
    private NodeService nodeService;
    private final LogEntry logEntry = new LogEntry();
    private final LogService logService = getLogService();


    public TimeStampingFilesProcess(ProcessSettings processSettings, ImportStatus status) {
        this.status = status;
        this.processSettings = processSettings;

        log.debug("GET FILE-FOLDER SERVICE...");
        this.fileFolderService = BeanHelper.getFileFolderService();

        log.debug("GET NODE SERVICE....");
        this.nodeService = BeanHelper.getNodeService();

        logEntry.setComputerIp("127.0.0.1");
        logEntry.setComputerName("localhost");
        logEntry.setEventDescription("SHA-1 ületembeldamine");
        logEntry.setLevel(LogObject.DOCUMENT.getLevel());
        logEntry.setObjectName(LogObject.DOCUMENT.getObjectName());
        logEntry.setCreatorId("IMPORT");
        logEntry.setCreatorName("IMPORT");
    }

    public boolean doBatch() {
        List<String> processedFiles = new ArrayList<>();


        final List<TeraFilesEntry> entries = getTeraService().getTeraFilesEntrys(processSettings.getBatchSize(), 0);

        try {
            processedFiles = getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<List<String>>() {
                @Override
                public List<String> execute() throws Throwable {
                    return processFiles(status, entries);
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
            //getTeraService().updateTeraFilesProcessStatus(nodeRef,"SHA-1", false);
        }

    }


    private List<String> processFiles(ImportStatus status, List<TeraFilesEntry> entries) throws IOException {
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

        log.info("File nodeRef: "+fileNodeRef+", filename: "+fileName+", displayName: "+ displayName);

        String statusInfo = "";
        if (!checkFileContent(fileInfo)) {
            statusInfo = "File check failed! NO DATA FOUND!";
            log.warn(statusInfo);
            getTeraService().updateTeraFilesRow(fileNodeRef.toString(), fileName, fileExt, "", statusInfo, true, false);
            return null;
        }

        String processedNodeRef = fileInfo.getNodeRef().toString();

        if (fileExt.equals("DDOC")) {
            processedNodeRef = makeAsics(fileInfo, fileName, fileExt);
            logEntry.setObjectId(processedNodeRef);
            logService.addLogEntry(logEntry);
        } else if (fileExt.equals("BDOC")) {
            //processedNodeRef = checkDigiDocFileAndMakeAsics(fileInfo, fileName, fileExt);
            SignatureItemsAndDataItems items = getDigidocDataItems(fileNodeRef);
            if (items == null) {
                statusInfo = "Digidoc file parse failed! No ITEMS found!";
                log.warn(statusInfo);
                getTeraService().updateTeraFilesRow(fileNodeRef.toString(), fileName, fileExt, "", statusInfo, true, false);
                return null;
            }
            boolean needsOverstamping = DigidocFileContainsSHA1(items);

            if (needsOverstamping) {
                processedNodeRef = makeAsics(fileInfo, fileName, fileExt);
                logEntry.setObjectId(processedNodeRef);
                logService.addLogEntry(logEntry);
            } else {
                getTeraService().updateTeraFilesRow(fileNodeRef.toString(), fileName, fileExt, "", "No SHA-1 found!", true, false);
            }

        } else if (fileExt.equals("ASICS")) {
            getTeraService().updateTeraFilesRow(fileNodeRef.toString(), fileName, fileExt, "", "ASICS already done!", true, true);
            logEntry.setObjectId(processedNodeRef);
            logService.addLogEntry(logEntry);
        } else {
            getTeraService().updateTeraFilesRow(fileNodeRef.toString(), fileName, fileExt, "", "Overstamping not needed!", true, false);
        }

        return processedNodeRef;
    }

    /*
    private String checkDigiDocFileAndMakeAsics(FileInfo fileInfo, String fileName, String fileExt) {
        log.info("CONTAINER CONTAINS SHA-1 ENCRYPTION AND NEEDS TIMESTAMPING (ASIC-S CONTAINER)!");
        if (getAsicsAndSave(fileInfo, fileName, fileExt)) {
            getTeraService().updateTeraFilesRow(fileInfo.getNodeRef().toString(), fileName, fileExt, "SHA-1", "", true, true);
        } else {
            log.warn("File not processed to timestamping!");
            return null;
        }
        return fileInfo.getNodeRef().toString();
    }
*/
    private String makeAsics(FileInfo fileInfo, String fileName, String fileExt) {
        log.info("CONTAINER CONTAINS SHA-1 ENCRYPTION AND NEEDS TIMESTAMPING (ASIC-S CONTAINER)!");
        if (getAsicsAndSave(fileInfo, fileName, fileExt)) {
            getTeraService().updateTeraFilesRow(fileInfo.getNodeRef().toString(), fileName, fileExt, "SHA-1", "", true, true);
        } else {
            log.warn("File not processed to timestamping!");
            return null;
        }
        return fileInfo.getNodeRef().toString();
    }


    private boolean getAsicsAndSave(FileInfo fileInfo, String filename, String fileType) {
        try {

            log.debug("Make ASIC-S container from file: " + filename);
            byte[] asicSContainer = getAsicSContainer(filename, fileInfo.getNodeRef(), fileType);
            if (asicSContainer == null) {
                log.error("MAKING ASIC-S FAILED! NO FILEDATA! continue...");
                return false;
            }

            String fileBaseName = FilenameUtils.getBaseName(filename);
            String fileExt = ".asics";

            log.debug("UPDATING fileinfo ...");

            String CURRENT_USER = "DELTA";

            Map<QName, Serializable> properties = getTeraService().renameFile(nodeService, fileInfo, fileBaseName, fileExt, fileType, CURRENT_USER);
            if (properties == null) {
                log.error("FILE RENAME FAILED!");
            } else {
                log.trace("File properties updated!");
            }

            try {
                writeSignedContainer(fileInfo.getNodeRef(), asicSContainer, CURRENT_USER);
            } catch (Exception e) {
                log.error(e.getMessage());
                getTeraService().updateTeraFilesRow(fileInfo.getNodeRef().toString(), filename, fileExt, "SHA-1", "", true, false);
                return false;
            }

            getTeraService().fixModifier(nodeService, fileFolderService, fileInfo.getNodeRef(), CURRENT_USER);

            log.debug("File is updated to ASIC-S....");
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    private byte[] getAsicSContainer(String filename, NodeRef nodeRef, String fileExt) {
        byte[] fileBytes = BeanHelper.getDigiSignSearches().getContent(nodeRef);

        if(fileBytes == null || fileBytes.length == 0){
            String statusInfo = "NO FILE DATA FOUND! NodeRef: " + nodeRef.toString() + ", Filename: " + filename;
            log.info(statusInfo);
            getTeraService().updateTeraFilesRow(nodeRef.toString(), filename, fileExt, "", statusInfo, true, false);
            return null;
        } else {
            log.info("DATA FOUND! NodeRef: " + nodeRef.toString() + ", Filename: " + filename + ", filesize: " + fileBytes.length + " bytes");
        }

        try {
            log.debug("Make ASIC-S....");
            byte[] out = BeanHelper.getDigiSignSearches().makeAsicS(filename, fileBytes);
            if(out == null) {
                log.debug("Response is NULL!");
            } else {
                log.debug("RESPONSE file size: " + out.length + " bytes");
            }
            return out;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private String getContentPath(NodeRef nodeRef){
        try{
            FileInfo fileInfo = fileFolderService.getFileInfo(nodeRef);
            String contentPath = fileInfo.getContentData().getContentUrl();
            return contentPath;
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return null;
    }


    private byte[] parseJsonObject(JSONObject json) {
        try {
            //String filename = (String) json.get("filename");
            //log.debug("ASIC-S filename: " + filename);
            int statusCode = (int) json.get("statusCode");
            log.trace("REQUEST statusCore: " + statusCode);

            if (statusCode == 400) {
                String status = (String) json.get("status");
                log.error("REQUEST status: " + status);
                return null;
            }
            String filedata = (String) json.get("filedata");
            if (filedata == null || filedata.isEmpty()) {

                log.error("MAKING ASIC-S CONTAINER FAILED! NO RESPONSE FILEDATA!!");

                return null;
            }
            return Base64.decodeBase64(filedata);

        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private byte[] parseJsonMap(Object asicSFileObj) {
        Map<String, Object> json = (Map<String, Object>) asicSFileObj;
        String filename = (String) json.get("filename");
        log.debug("ASIC-S filename: " + filename);
        String filedata = (String) json.get("filedata");
        if (filedata == null || filedata.isEmpty()) {
            log.error("MAKING ASIC-S CONTAINER FAILED! NO RESPONSE FILEDATA!!");
            return null;
        }
        return Base64.decodeBase64(filedata);
    }




    private boolean DigidocFileContainsSHA1(SignatureItemsAndDataItems items) {
        List<SignatureItem> signatureItems = items.getSignatureItems();

        for (SignatureItem signatureItem : signatureItems) {
            String encrytionType = signatureItem.getEncrytionType();
            log.debug("Signature encryption type: " + encrytionType);
            if (encrytionType.startsWith("SHA-1")) {
                return true;
            }
        }

        return false;
    }

    private SignatureItemsAndDataItems getDigidocDataItems(NodeRef fileNodeRef) {
        try {
            SignatureItemsAndDataItems items = BeanHelper.getDigiDoc4JSignatureService().getDataItemsAndSignatureItems(fileNodeRef, true);
            return items;
        } catch (SignatureException ex) {
            log.error("getDigidocDataItems file nodeRef: " + fileNodeRef + " Exception error: " + ex.getMessage(), ex);
        }
        return null;
    }

    private void writeSignedContainer(NodeRef nodeRef, byte[] filedata, String CURRENT_USER) throws IOException {
        log.trace("writeSignedContainer()...fileFolderService.getWriter(): CURRENT_USER: " + CURRENT_USER);
        ContentWriter writer = fileFolderService.getWriter(nodeRef, CURRENT_USER);
        log.trace("writeSignedContainer(): writer.setMimetype()...");
        writer.setMimetype(SignatureService.DIGIDOC_MIMETYPE);
        log.trace("writeSignedContainer(): writer.setEncoding()...");
        writer.setEncoding(AppConstants.CHARSET);
        log.trace("writeSignedContainer(): writer.putContent()...");
        InputStream is = new ByteArrayInputStream(filedata);
        writer.putContent(is);
        IOUtils.closeQuietly(is);
        log.trace("writeSignedContainer()...DONE!");
    }

    private boolean checkFileContent(FileInfo fileInfo) {
        byte[] fileBytes = BeanHelper.getDigiSignSearches().getContent(fileInfo.getNodeRef());
        if (fileBytes == null ) {
            log.error("FILE CONTENTDATA IS NULL!...");
            return false;
        }

        if(fileBytes.length == 0)
        {
            log.error("FILE CONTENTDATA IS 0!...");
            return false;
        }
        //log.debug("FILE contentUrl: " + fileContentData.getContentUrl());
        //log.debug("FILE encoding: " + fileContentData.getEncoding());
        //log.debug("FILE mimetype: " + fileContentData.getMimetype());
        //log.debug("FILE size: " + fileContentData.getSize());

        return true;
    }


}
