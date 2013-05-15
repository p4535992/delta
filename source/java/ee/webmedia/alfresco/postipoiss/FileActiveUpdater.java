package ee.webmedia.alfresco.postipoiss;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.sharepoint.ImportUtil;

/**
 * @author Alar Kvell
 */
public class FileActiveUpdater extends AbstractNodeUpdater {

    private FileService fileService;

    private String completedDocsCsvPath;
    private String filesCsvPath;

    private Map<String /* docId */, NodeRef /* docRef */> docRefsByDocId;
    private Map<NodeRef /* docRef */, String /* docId */> docIdsByDocRef;
    private Map<NodeRef /* docRef */, Set<String /* displayName */>> inactiveFileDisplayNamesByDocRef;
    private File completedFile;
    private File failedFile;

    @Override
    protected void executeUpdater() throws Exception {
        Assert.hasText(completedDocsCsvPath, "Path to completed_docs.csv must not be blank");
        Assert.hasText(filesCsvPath, "Path to files.csv must not be blank");
        super.executeUpdater();
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return null;
    }

    @Override
    protected boolean usePreviousState() {
        return false;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        completedFile = new File(inputFolder, getBaseFileName() + "FilesCompleted.csv");
        if (completedFile.exists()) {
            log.info("Completed files file exists, deleting: " + completedFile.getAbsolutePath());
            Assert.isTrue(completedFile.delete());
        }
        failedFile = new File(inputFolder, getBaseFileName() + "FilesFailed.csv");
        if (failedFile.exists()) {
            log.info("Failed files file exists, deleting: " + failedFile.getAbsolutePath());
            Assert.isTrue(failedFile.delete());
        }
        List<String[]> failedList = new ArrayList<String[]>();

        docRefsByDocId = new HashMap<String, NodeRef>();
        docIdsByDocRef = new HashMap<NodeRef, String>();
        log.info("Loading completed document nodeRefs from '" + completedDocsCsvPath + "'");
        CsvReader reader = ImportUtil.createLogReader(new File(completedDocsCsvPath));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                try {
                    String docId = reader.get(0);
                    Assert.hasText(docId, "docId cannot be blank");
                    if (docRefsByDocId.containsKey(docId)) {
                        continue;
                    }
                    NodeRef docRef = new NodeRef(reader.get(1));
                    Assert.isTrue(!docIdsByDocRef.containsKey(docRef), "Duplicate document nodeRef " + docRef);
                    docRefsByDocId.put(docId, docRef);
                    docIdsByDocRef.put(docRef, docId);
                } catch (Exception e) {
                    throw new RuntimeException("Error in '" + completedDocsCsvPath + "' line " + reader.getCurrentRecord() + ": " + e.getMessage(), e);
                }
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + docRefsByDocId.size() + " completed document nodeRefs");

        inactiveFileDisplayNamesByDocRef = new HashMap<NodeRef, Set<String>>();
        int inactiveFilesCount = 0;
        log.info("Loading files from '" + filesCsvPath + "'");
        reader = ImportUtil.createLogReader(new File(filesCsvPath));
        try {
            while (reader.readRecord()) {
                try {
                    if (!"Ei".equals(StringUtils.strip(reader.get(5)))) {
                        continue;
                    }
                    String docId = reader.get(0);
                    NodeRef docRef = docRefsByDocId.get(docId);
                    if (docRef == null) {
                        failedList.add(new String[] { "documentDoesNotExist", "", docId, "", "", "", "" });
                        continue;
                    }
                    String displayName = reader.get(4);
                    int i = displayName.lastIndexOf('.');
                    if (i >= 0) {
                        displayName = displayName.substring(0, i);
                    }
                    Set<String> displayNames = inactiveFileDisplayNamesByDocRef.get(docRef);
                    if (displayNames == null) {
                        displayNames = new HashSet<String>();
                        inactiveFileDisplayNamesByDocRef.put(docRef, displayNames);
                    }
                    if (!displayNames.contains(displayName)) {
                        displayNames.add(displayName);
                        inactiveFilesCount++;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error in '" + filesCsvPath + "' line " + reader.getCurrentRecord() + ": " + e.getMessage(), e);
                }
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + inactiveFilesCount + " inactive file displayNames for " + inactiveFileDisplayNamesByDocRef.size() + " documents");

        executeAfterCommit(failedFile, new FilesCsvWriterClosure(failedList));
        return inactiveFileDisplayNamesByDocRef.keySet();
    }

    private static class FilesCsvWriterClosure implements CsvWriterClosure {

        private final List<String[]> rows;

        public FilesCsvWriterClosure(List<String[]> rows) {
            this.rows = rows;
        }

        @Override
        public String[] getHeaders() {
            return new String[] { "documentNodeRef", "action", "documentId", "fileNodeRef", "fileDisplayName", "documentRegNumber", "documentDocName" };
        }

        @Override
        public void execute(CsvWriter writer) throws IOException {
            for (String[] row : rows) {
                writer.writeRecord(row);
            }
        }
    }

    @Override
    protected boolean processOnlyExistingNodeRefs() {
        return false;
    }

    @Override
    protected String[] updateNode(NodeRef docRef) throws Exception {
        List<String[]> completedList = new ArrayList<String[]>();
        List<String[]> failedList = new ArrayList<String[]>();
        bindCsvWriteAfterCommit(completedFile, null, new FilesCsvWriterClosure(completedList));
        bindCsvWriteAfterCommit(failedFile, null, new FilesCsvWriterClosure(failedList));

        int filesNotFoundCount = 0;
        int filesFoundAndChangedActiveToFalse = 0;
        int filesFoundAndActiveWasAlreadyFalse = 0;
        String docId = docIdsByDocRef.get(docRef);
        Set<String> inactiveFileDisplayNames = inactiveFileDisplayNamesByDocRef.get(docRef);

        String regNumber = "";
        String docName = "";
        String docAction;
        if (nodeService.exists(docRef)) {
            docAction = "documentExists";
            regNumber = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.REG_NUMBER);
            docName = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.DOC_NAME);

            List<ee.webmedia.alfresco.document.file.model.File> files = fileService.getAllFilesExcludingDigidocSubitems(docRef);
            for (String displayName : inactiveFileDisplayNames) {
                boolean found = false;
                for (ee.webmedia.alfresco.document.file.model.File file : files) {
                    if (displayName.equals(file.getDisplayName())) {
                        found = true;
                        String action;
                        if (file.isActive()) {
                            nodeService.setProperty(file.getNodeRef(), FileModel.Props.ACTIVE, Boolean.FALSE);
                            action = "changedActiveToFalse";
                            filesFoundAndChangedActiveToFalse++;
                        } else {
                            action = "activeWasAlreadyFalse";
                            filesFoundAndActiveWasAlreadyFalse++;
                        }
                        completedList.add(new String[] { action, docRef.toString(), docId, file.getNodeRef().toString(), displayName, regNumber, docName });
                        break;
                    }
                }
                if (!found) {
                    failedList.add(new String[] { "fileDoesNotExist", docRef.toString(), docId, "", displayName, regNumber, docName });
                    filesNotFoundCount++;
                }
            }
        } else {
            docAction = "documentDoesNotExist";
            for (String displayName : inactiveFileDisplayNames) {
                failedList.add(new String[] { "documentDoesNotExist", docRef.toString(), docId, "", displayName, regNumber, docName });
                filesNotFoundCount++;
            }
        }
        return new String[] { docAction, docId, Integer.toString(filesNotFoundCount), Integer.toString(filesFoundAndChangedActiveToFalse),
                Integer.toString(filesFoundAndActiveWasAlreadyFalse),
                regNumber, docName };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] {
                "documentNodeRef",
                "action",
                "documentId",
                "filesNotFoundCount",
                "filesFoundAndChangedActiveToFalse",
                "filesFoundAndActiveWasAlreadyFalse",
                "documentDocName",
                "documentRegNumber" };
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public String getCompletedDocsCsvPath() {
        return completedDocsCsvPath;
    }

    public void setCompletedDocsCsvPath(String completedDocsCsvPath) {
        this.completedDocsCsvPath = completedDocsCsvPath;
    }

    public String getFilesCsvPath() {
        return filesCsvPath;
    }

    public void setFilesCsvPath(String filesCsvPath) {
        this.filesCsvPath = filesCsvPath;
    }

}
