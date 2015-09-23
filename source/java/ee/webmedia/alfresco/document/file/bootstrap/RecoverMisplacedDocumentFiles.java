package ee.webmedia.alfresco.document.file.bootstrap;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.TextUtil;

/**
 * This updater tries to recover files that had property {@code FileModel.Props.PREVIOUS_FILE_PARENT} and
 * were moved to the location pointed by that property after the documents store was changed (arvhived or deleted). <br>
 * Note: <br>
 * Some of the log/.csv file entries created by this updater will be bogus because not all actions on
 * files are logged causing this updater to report that document has missing files although it actually does not.
 */
public class RecoverMisplacedDocumentFiles extends AbstractNodeUpdater {

    private JdbcTemplate jdbcTemplate;
    private FileService fileService;

    private Map<String, List<String>> docToFileNames;
    private Set<StoreRef> storeRefs;
    private Set<NodeRef> visitedNodes;

    private static final String[] SEARCH_ARRAY = { "õ", "ä", "ö", "ü", "š", "ž", "Õ", "Ä", "Ö", "Ü", "Š", "Ž" };
    private static final String[] REPLACE_ARRAY = { "o", "a", "o", "u", "s", "z", "O", "A", "O", "U", "S", "Z" };

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        String placeHolderPattern = "\\{\\d+\\}";
        String fileAdded = MessageUtil.getMessage("document_log_status_fileAdded").replaceAll(placeHolderPattern, "(.*)");
        String fileDeleted = MessageUtil.getMessage("document_log_status_fileDeleted").replaceAll(placeHolderPattern, "(.*)");
        String fileNameChanged = MessageUtil.getMessage("document_log_status_fileNameChanged").replaceAll(placeHolderPattern, "(.*)");
        final Matcher fileAddedMatcher = Pattern.compile(fileAdded).matcher("");
        final Matcher fileDeletedMatcher = Pattern.compile(fileDeleted).matcher("");
        final Matcher fileNameChangedMatcher = Pattern.compile(fileNameChanged).matcher("");

        String sql = "SELECT object_id node_ref, description FROM delta_log"
                + " WHERE ("
                + " description ~* '" + fileAdded + "'"
                + " OR description ~* '" + fileDeleted + "'"
                + " OR description ~* '" + fileNameChanged + "'"
                + " )"
                + " AND created_date_time >= '2014-01-15'" // date this bug was commited
                + " ORDER BY created_date_time";

        final Set<NodeRef> nodeRefs = new HashSet<>();
        jdbcTemplate.query(sql, new RowMapper<Void>() {

            @Override
            public Void mapRow(java.sql.ResultSet rs, int rowNum) throws SQLException {
                Object objectId = rs.getObject("node_ref");
                NodeRef nodeRef = new NodeRef((String) objectId);
                nodeRefs.add(nodeRef);
                String nodeId = nodeRef.getId();
                String description = rs.getString("description");

                List<String> fileNames = docToFileNames.get(nodeId);
                if (fileNames == null) {
                    fileNames = new ArrayList<>();
                    docToFileNames.put(nodeId, fileNames);
                }
                if (fileDeletedMatcher.reset(description).find()) {
                    String deletedFileName = replaceEstChars(fileDeletedMatcher.group(1));
                    fileNames.remove(deletedFileName);
                } else if (fileAddedMatcher.reset(description).find()) {
                    String fileName = replaceEstChars(fileAddedMatcher.group(1));
                    fileNames.add(fileName);
                } else if (fileNameChangedMatcher.reset(description).find()) {
                    String oldFileName = replaceEstChars(fileNameChangedMatcher.group(1));
                    String newFileName = replaceEstChars(fileNameChangedMatcher.group(2));
                    fileNames.remove(oldFileName);
                    fileNames.add(newFileName);
                }
                return null;
            }

        });
        return nodeRefs;
    }

    private String replaceEstChars(String text) {
        return StringUtils.replaceEach(text, SEARCH_ARRAY, REPLACE_ARRAY);
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return null; // not called
    }

    @Override
    protected void executeUpdater() throws Exception {
        initFileds();
        super.executeUpdater();
        resetFileds();
    }

    private void initFileds() {
        docToFileNames = new HashMap<>();
        storeRefs = generalService.getAllStoreRefsWithTrashCan();
        visitedNodes = new HashSet<>();
    }

    private void resetFileds() {
        docToFileNames = null;
        storeRefs = null;
        visitedNodes = null;
    }

    @Override
    protected boolean processOnlyExistingNodeRefs() {
        return false;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        NodeRef realNodeRef = nodeService.exists(nodeRef) ? nodeRef : findExistingNodeRef(nodeRef); // nodeRef might point to wrong store
        List<String> fileNamesFromLog = docToFileNames.get(nodeRef.getId());
        if (CollectionUtils.isEmpty(fileNamesFromLog)) {
            return new String[] { "No files" };
        }
        if (realNodeRef == null) {
            String files = TextUtil.joinNonBlankStrings(fileNamesFromLog, "\n");
            log.warn("NodeRef is deleted: " + nodeRef + ". Files according to log:\n" + files);
            return (String[]) ArrayUtils.addAll(new String[] { "NODE DELETED" }, fileNamesFromLog.toArray());
        }
        if (visitedNodes.contains(realNodeRef)) {
            return new String[] { "Already visited", realNodeRef.toString() };
        }
        visitedNodes.add(realNodeRef);

        List<File> docFiles = fileService.getAllFilesExcludingDigidocSubitems(realNodeRef);
        List<String> currentDocFileNames = new ArrayList<>(docFiles.size());
        for (File f : docFiles) {
            currentDocFileNames.add(f.getName());
        }
        fileNamesFromLog.removeAll(currentDocFileNames);

        for (String s : currentDocFileNames) {
            if (CollectionUtils.isEmpty(fileNamesFromLog)) {
                break;
            }
            String extension = FilenameUtils.getExtension(s);
            String extensionStr = StringUtils.isNotBlank(extension) ? "." + extension : "";
            final String name = s.replaceFirst(" \\(\\d+\\)" + extensionStr, extensionStr);
            CollectionUtils.filter(fileNamesFromLog, new Predicate<String>() {
                @Override
                public boolean evaluate(String object) {
                    if (StringUtils.equals(name, object)) {
                        return false;
                    }
                    return true;
                }
            });
        }

        if (CollectionUtils.isEmpty(fileNamesFromLog)) {
            return new String[] { "Nothing to recover" };
        }

        Set<NodeRef> fileRefs = searchFiles(fileNamesFromLog);
        if (CollectionUtils.isEmpty(fileRefs)) {
            return (String[]) ArrayUtils.addAll(new String[] { "Did not find file(s)" }, fileNamesFromLog.toArray());
        }

        Map<NodeRef, String> extractedfileRefs = extractCorrectFileRefs(fileRefs, fileNamesFromLog, realNodeRef);
        Set<String> recoveredFileNames = new HashSet<>();
        for (Map.Entry<NodeRef, String> entry : extractedfileRefs.entrySet()) {
            NodeRef fileRef = entry.getKey();
            fileRef = nodeService.moveNode(fileRef, realNodeRef, ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS).getChildRef();
            recoveredFileNames.add(entry.getValue());

            Map<QName, Serializable> props = new HashMap<>();
            props.put(FileModel.Props.ACTIVE, true);
            String extension = FilenameUtils.getExtension(entry.getValue());
            if (StringUtils.isNotBlank(extension)) {
                props.put(FileModel.Props.CONVERT_TO_PDF_IF_SIGNED, fileService.isTransformableToPdf(extension));
            }
            nodeService.addProperties(fileRef, props);
        }

        if (!recoveredFileNames.isEmpty()) {
            fileNamesFromLog.removeAll(recoveredFileNames);
            fileService.reorderFiles(realNodeRef);
            BeanHelper.getDocumentService().updateSearchableFiles(realNodeRef);

            if (fileNamesFromLog.isEmpty()) {
                log.info("Recovered all files for node " + realNodeRef + ". Recovered files:\n" + TextUtil.joinNonBlankStrings(recoveredFileNames, "\n"));
                return new String[] { "Recovered all files, count: " + recoveredFileNames.size() };
            }
            log.info("Partially recovered files for node " + realNodeRef + ".\nRecovered files:\n"
                    + TextUtil.joinNonBlankStrings(recoveredFileNames, "\nNot recovered files:\n" + TextUtil.joinNonBlankStrings(fileNamesFromLog, "\n")));
            return (String[]) ArrayUtils.addAll(new String[] { "Partially recovered files, recovered: " + recoveredFileNames.size() +
                    " not recovered: " + fileNamesFromLog.size() }, fileNamesFromLog.toArray());
        }

        log.warn("Unable to recover file(s) for " + realNodeRef + ":\n" + TextUtil.joinNonBlankStrings(fileNamesFromLog, "\n"));
        return (String[]) ArrayUtils.addAll(new String[] { "Unable to recover file(s)" }, fileNamesFromLog.toArray());
    }

    private Set<NodeRef> searchFiles(List<String> docFileNames) {
        String query = SearchUtil.joinQueryPartsAnd(SearchUtil.generateTypeQuery(ContentModel.TYPE_CONTENT),
                SearchUtil.generatePropertyExactQuery(ContentModel.PROP_NAME, docFileNames));
        Set<ResultSet> searchResult = new HashSet<>(2);
        searchResult.add(searchService.query(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, query));
        searchResult.add(searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, query));

        Set<NodeRef> fileRefs = new HashSet<>();
        try {
            for (ResultSet set : searchResult) {
                fileRefs.addAll(set.getNodeRefs());
            }
        } finally {
            for (ResultSet set : searchResult) {
                set.close();
            }
        }
        return fileRefs;
    }

    private Map<NodeRef, String> extractCorrectFileRefs(Set<NodeRef> fileRefs, List<String> fileNamesFromLog, NodeRef docRef) {
        List<String> fileNamesFromLogCopy = new ArrayList<>(fileNamesFromLog);
        Map<NodeRef, Node> files = BeanHelper.getBulkLoadNodeService().loadNodes(fileRefs, Collections.singleton(ContentModel.PROP_NAME));
        List<String> repoFileNames = new ArrayList<>();
        for (Node fileNode : files.values()) {
            String name = (String) fileNode.getProperties().get(ContentModel.PROP_NAME);
            repoFileNames.add(name);
        }
        Map<NodeRef, String> filesToRecheck = new HashMap<>();
        Map<NodeRef, String> fileRefToName = new HashMap<>();
        for (NodeRef fileRef : fileRefs) {
            Node fileNode = files.get(fileRef);
            if (fileNode == null) {
                log.warn("Did not find file: " + fileRef);
                continue;
            }
            final String repoFileName = (String) fileNode.getProperties().get(ContentModel.PROP_NAME);

            if (!fileNamesFromLogCopy.contains(repoFileName)) { // must be exact match
                continue;
            }

            NodeRef parentRef = nodeService.getPrimaryParent(fileRef).getParentRef();
            QName parentType = nodeService.getType(parentRef);
            if (DocumentCommonModel.Types.DOCUMENT.equals(parentType)) {
                repoFileNames.remove(repoFileName);
                continue;
            }

            int repoCount = countOccurences(repoFileNames, repoFileName);
            if (repoCount > 1) { // Cannot decide
                filesToRecheck.put(fileRef, repoFileName);
                continue;
            }

            fileRefToName.put(fileRef, repoFileName);
        }

        if (!filesToRecheck.isEmpty()) {
            Set<String> checkedFileNames = new HashSet<>();
            for (NodeRef fileRef : fileRefs) {
                String repoFileName = filesToRecheck.get(fileRef);
                if (repoFileName == null || checkedFileNames.contains(repoFileName)) {
                    continue;
                }
                checkedFileNames.add(repoFileName);
                int repoCount = countOccurences(repoFileNames, repoFileName);
                if (repoCount > 1) {
                    log.warn(String.format("Found %d possible matches for '%s' (docRef=%s). Cannot choose correct file.", repoCount, repoFileName, docRef.toString()));
                } else if (repoCount == 1) {
                    fileRefToName.put(fileRef, repoFileName);
                }
            }
        }
        return fileRefToName;
    }

    private int countOccurences(List<String> fileNames, final String fileName) {
        return CollectionUtils.countMatches(fileNames, new Predicate<String>() {
            @Override
            public boolean evaluate(String name) {
                return StringUtils.equals(fileName, name);
            }
        });
    }

    private NodeRef findExistingNodeRef(NodeRef nodeRef) {
        String uuid = nodeRef.getId();
        StoreRef storeRef = nodeRef.getStoreRef();
        NodeRef result = null;
        for (StoreRef sr : storeRefs) {
            if (sr.equals(storeRef)) {
                continue;
            }
            NodeRef refToTest = new NodeRef(sr, uuid);
            if (nodeService.exists(refToTest)) {
                result = refToTest;
                break;
            }
        }
        return result;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

}
