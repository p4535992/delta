package ee.webmedia.alfresco.sharepoint;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileFolderService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPersonService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getTransactionService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVersionsService;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.docsError;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.getDate;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.getString;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.PublishToAdr;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.sharepoint.mapping.DocumentMetadata;
import ee.webmedia.alfresco.sharepoint.mapping.ImportFile;
import ee.webmedia.alfresco.sharepoint.mapping.MappedDocument;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.ProgressTracker;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.versions.model.VersionsModel;
import ee.webmedia.alfresco.versions.service.VersionsService;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Imports documents and files from Sharepoint.
 * 
 * @author Martti Tamm
 */
public class DocumentImporter {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentImporter.class);

    public static final String[] SYSTEM_OWNERS = { "JUSTMIN\\admin.mihkel", "JUSTMIN\\kristjan12", "JUSTMIN\\sps.pumpaja" };
    private static final String[] NON_DIGITAL_EXT = { "htm", "html" };

    private static final Pattern DOC_XML_WITH_VERSION_PATTERN = Pattern.compile(".*\\.v(\\d+).xml$");
    private static final Pattern REG_NUMBER_PATTERN = Pattern.compile(".*-\\d+$");

    public static final String CSV_NAME_KASUT_ADSI = "d_kasut_adsi.csv";
    public static final String CSV_NAME_COMPLETED_DOCS = "completed_docs.csv";
    private static final String CSV_NAME_FAILED_DOCS = "failed_docs.csv";
    private static final String CSV_NAME_FAILED_IMPORT = "failed_import.csv";
    private static final String DIR_FILES = "files";
    private static final String DIR_DOCS = "documents";
    public static final String USER_INFO_DUPL = "[duplicate user info here]";

    // IMPORT SETTINGS
    private final ImportSettings settings;
    private final ImportStatus status;
    private final ProgressTracker progressTracker;

    // IMPORT STATE DATA
    private final File logFile;
    private final File errorFile;
    private final File failedFile;
    private final File filesDir;

    private final Map<String, Set<VolumeCase>> importedVolumesCases;
    private final Map<String, String> externalUsersById;
    private final Map<String, String> systemUsersByFullName;
    private Map<String, NodeRef> importedDocXmls;
    private Map<String, NodeRef> importedDocXmlsCommited;
    private final List<Map<Integer, File>> notImportedDocXmls;
    private final Map<NodeRef, Map<QName, NodeRef>> parentsCache = new HashMap<NodeRef, Map<QName, NodeRef>>(500);

    private String lastKnownDocDir;

    // SPRING BEANS
    private final GeneralService generalService = getGeneralService();
    private final FileFolderService fileFolderService = getFileFolderService();
    private final NodeService nodeService = getNodeService();
    private final UserService userService = getUserService();
    private final DocumentService documentService = getDocumentService();
    private final DocumentDynamicService documentDynamicService = getDocumentDynamicService();
    private final VersionsService versionsService = getVersionsService();
    private final LogService logService = getLogService();
    private final SimpleJdbcTemplate jdbcTemplate;

    private final SharepointMapping mapper;

    public DocumentImporter(ImportSettings settings, ImportStatus status, SimpleJdbcTemplate jdbcTemplate) {
        this.settings = settings;
        this.status = status;
        this.jdbcTemplate = jdbcTemplate;

        logFile = settings.getWorkFolderFile(CSV_NAME_COMPLETED_DOCS);
        failedFile = settings.getWorkFolderFile(CSV_NAME_FAILED_DOCS);
        errorFile = settings.getWorkFolderFile(CSV_NAME_FAILED_IMPORT);
        filesDir = settings.getDataFolderFile(DIR_FILES);

        File volumeLogFile = settings.getWorkFolderFile(StructureImporter.COMPLETED_FILENAME);

        if (errorFile.exists()) {
            throw new RuntimeException("Structure import has not completed successfully. Skipping document import.");
        }

        if (errorFile.exists()) {
            errorFile.delete();
        }
        if (failedFile.exists()) {
            failedFile.delete();
        }

        if (!filesDir.exists()) {
            docsError(errorFile, "Document files directory not found: " + filesDir);
            throw new RuntimeException("Document files directory not found");
        }

        try {
            if (!volumeLogFile.exists()) {
                throw new ImportValidationException("File '" + StructureImporter.COMPLETED_FILENAME
                        + "' cannot be found. Do structure import first and keep the log file as it is created!");
            } else if (userService.getPerson(settings.getDefaultOwnerId()) == null) {
                throw new ImportValidationException("Default user with id = " + settings.getDefaultOwnerId() + " does not exist");
            }
        } catch (ImportValidationException e) {
            docsError(errorFile, e);
            throw new RuntimeException(e);
        }

        try {
            mapper = new SharepointMapping(settings.getMappingsFile(), settings.isAmphoraOrigin());
        } catch (Exception e) {
            docsError(errorFile, e);
            throw new RuntimeException(e);
        }

        systemUsersByFullName = loadSystemUsers();
        externalUsersById = loadExternalUsers(settings.getDataFolderFile(CSV_NAME_KASUT_ADSI));
        importedDocXmls = loadCompletedDocuments(logFile, settings.isDocsWithVersions());
        importedDocXmlsCommited = new HashMap<String, NodeRef>(importedDocXmls);
        importedVolumesCases = loadCompletedVolumesCases(volumeLogFile);
        Pair<Integer, List<Map<Integer, File>>> result = settings.isDocsWithVersions() ? getAllDocumentXmlVersionFiles() : getAllDocumentXmlSimpleFiles();
        notImportedDocXmls = result.getSecond();
        progressTracker = new ProgressTracker(result.getFirst() + importedDocXmls.size(), importedDocXmls.size());
    }

    /**
     * Imports a batch of documents. Since settings were provided to the constructor, this method just needs to do its work. This method returns <code>true</code> when all
     * documents in batch were successfully imported. Value <code>false</code> is returned when the batch failed for some reason or no new documents were found. This method should
     * not throw an exception. The caller should not continue when <code>false</code> is returned.
     * 
     * @return A Boolean that is <code>true</code> when a batch was imported successfully, or <code>false</code> when an error occurred or new documents were found.
     * @throws IOException
     */
    public boolean doBatch() {
        CsvWriter logWriter = initLogWriter(logFile);
        CsvWriter failedWriter = initLogWriter(failedFile);
        List<DocumentImportResult> importedDocs = new ArrayList<DocumentImportResult>(settings.getBatchSize());
        final List<Map<Integer, File>> nextDocumentXmlFiles = getNextDocumentXmlFiles();

        try {

            importedDocs = getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<List<DocumentImportResult>>() {
                @Override
                public List<DocumentImportResult> execute() throws Throwable {
                    AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
                        @Override
                        public void afterCommit() {
                            importedDocXmlsCommited = new HashMap<String, NodeRef>(importedDocXmls);
                        }

                        @Override
                        public void afterRollback() {
                            importedDocXmls = new HashMap<String, NodeRef>(importedDocXmlsCommited);
                        }
                    });
                    return settings.isDocsWithVersions() ? importVersions(nextDocumentXmlFiles) : importSimple(nextDocumentXmlFiles);
                }
            });

            // We need to log only when the entire batch of documents was imported successfully: the log file must not contain documents where import failed and the document must
            // be imported on next try. The log files must contain entries of only those document XML files not to be imported again.
            if (importedDocs != null) {
                for (DocumentImportResult importedDoc : importedDocs) {
                    importedDoc.logResult(logWriter, failedWriter);
                }
            }

        } catch (Exception e) {
            docsError(errorFile, e);
            LOG.warn("Unexpected error", e);
            importedDocs = null;
        } finally {
            if (logWriter != null) {
                logWriter.close();
            }
            if (failedWriter != null) {
                failedWriter.close();
            }
            logService.setThreadLocalOverride(null, null, null);
            if (nextDocumentXmlFiles != null) {
                int realBatchSize = 0;
                for (Map<Integer, File> map : nextDocumentXmlFiles) {
                    if (map != null) {
                        realBatchSize += map.values().size();
                    }
                }
                String info = progressTracker.step(realBatchSize);
                if (info != null) {
                    LOG.info(settings.getStructAndDocsOrigin() + " documents import: " + info);
                }
            }
        }

        return importedDocs == null || !importedDocs.isEmpty();
    }

    private List<DocumentImportResult> importSimple(List<Map<Integer, File>> nextDocumentXmlFiles) {
        List<DocumentImportResult> results = new ArrayList<DocumentImportResult>(settings.getBatchSize());
        DocumentImportResult result;

        for (Map<Integer, File> map : nextDocumentXmlFiles) {
            for (File docXmlFile : map.values()) {

                result = null;

                try {
                    status.incrCount();

                    result = handleDocument(docXmlFile, null, null);

                    if (result.isSuccessful()) {
                        addDocumentLastLog(result);
                    } else {
                        status.incrFailed();
                    }

                    results.add(result);
                } catch (Exception e) {
                    status.incrFailed();
                    LOG.warn("Caught exception during document import.", e);
                    docsError(errorFile, docXmlFile, e);
                    results = null;
                    break;
                } finally {
                    importedDocXmls.put(FilenameUtils.getBaseName(docXmlFile.getName()), result != null ? result.getDocumentRef() : null);
                }
            }
        }

        return results;
    }

    /**
     * Imports documents with versions. Single document snapshot import part relies on common document (XML) import functionality. When a document version import fails,
     * the document at hand will be deleted (when necessary) and logged, and next document with versions is taken. (Stopping migration due to a single document should be rarely
     * used.)
     * 
     * @return <code>null</code> when entire batch import failed, otherwise result objects for imported document (per version). The caller can write these results to a log file.
     */
    private List<DocumentImportResult> importVersions(List<Map<Integer, File>> nextDocumentXmlFiles) {
        List<DocumentImportResult> results = new ArrayList<DocumentImportResult>(settings.getBatchSize() * 5);

        batchLoop: for (Map<Integer, File> docXmlFiles : nextDocumentXmlFiles) {
            List<DocumentImportResult> docResults = new ArrayList<DocumentImportResult>(docXmlFiles.size());
            DocumentMetadata meta = null;

            // In case of versions, the meta data from newest (last) document must be used (e.g. document original location may not exist anymore).
            for (Iterator<File> i = docXmlFiles.values().iterator(); i.hasNext();) {
                File file = i.next();

                if (!i.hasNext()) {
                    try {
                        meta = getDocumentMeta(file);
                    } catch (ImportValidationException e) {
                        results.add(new DocumentImportResult(null, file, e.getMessage()));
                        continue;
                    }
                }
            }

            DocumentImportResult result;

            for (File docXmlFile : docXmlFiles.values()) {
                result = null;
                try {
                    status.incrCount();

                    result = handleDocument(docXmlFile, docResults.isEmpty() ? null : docResults.get(0).getDocumentRef(), meta);
                    docResults.add(result);

                    if (!result.isSuccessful()) {
                        // Remove document if previously saved:
                        if (docResults.size() > 1) {
                            final NodeRef documentRef = docResults.get(0).getDocumentRef();
                            if (documentRef != null) {
                                if (nodeService.exists(documentRef)) {
                                    nodeService.deleteNode(documentRef);
                                }
                                jdbcTemplate.update("DELETE FROM delta_log WHERE object_id=?", documentRef.toString());
                            }
                        }

                        status.incrFailed();

                        // Keep only the last result entry about failed document import:
                        results.add(result);
                        continue batchLoop;
                    }

                } catch (Exception e) {
                    status.incrFailed();
                    LOG.warn("Caught exception during document import.", e);
                    docsError(errorFile, docXmlFile, e);
                    results = null;
                    break batchLoop;
                } finally {
                    importedDocXmls.put(FilenameUtils.getBaseName(FilenameUtils.getBaseName(docXmlFile.getName())), result != null ? result.getDocumentRef() : null);
                }
            }

            if (!docResults.isEmpty()) {
                results.addAll(docResults);
                addDocumentLastLog(docResults.get(0));
            }
        }

        return results;
    }

    private CsvWriter initLogWriter(File log) {
        boolean fileNew = !log.exists();
        CsvWriter logWriter = null;
        try {
            logWriter = ImportUtil.createLogWriter(log, true);

            if (fileNew) {
                logWriter.writeRecord(new String[] {
                        "documentId",
                        "nodeRef",
                        "originalLocation",
                        "originalLocationName",
                        "tempName",
                        "created",
                        "regNumber",
                        "regDateTime",
                        "docName",
                        "ownerId",
                        "ownerName",
                        "accessRestriction",
                        "contentType",
                        "receivedSent",
                        "documentSubspecies",
                        "objectTypeId",
                        "faultDescription"
                });
            }
            return logWriter;
        } catch (IOException e) {
            if (logWriter != null) {
                logWriter.close();
            }
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Set<VolumeCase>> loadCompletedVolumesCases(File completed) {
        LOG.info("Loading previously completed volumes/cases from file " + completed);

        Map<String, Set<VolumeCase>> volumesCases = new HashMap<String, Set<VolumeCase>>();
        CsvReader reader = null;

        try {
            reader = ImportUtil.createLogReader(completed);

            if (reader.readHeaders()) {
                while (reader.readRecord()) {
                    String url = getString(reader, 4);

                    if (volumesCases.containsKey(url)) {
                        volumesCases.get(url).add(new VolumeCase(reader));
                    } else {
                        Set<VolumeCase> parentRefs = new HashSet<VolumeCase>(1);
                        parentRefs.add(new VolumeCase(reader));
                        volumesCases.put(url, parentRefs);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        LOG.info("Loaded " + volumesCases.size() + " previously imported volumes/cases (by URL).");

        return volumesCases;
    }

    public static Map<String, String> loadExternalUsers(File usersFile) {
        LOG.info("Loading previous system users from file " + usersFile);

        Map<String, String> users = new HashMap<String, String>();

        if (!usersFile.exists()) {
            LOG.info("The users file does not exist; not loading users; continuing.");
            return users;
        }

        CsvReader reader = null;

        try {
            reader = ImportUtil.createDataReader(usersFile);

            if (reader.readHeaders()) {
                while (reader.readRecord()) {
                    String id = getString(reader, 6);

                    if (users.containsKey(id)) {
                        users.put(id, USER_INFO_DUPL);
                    } else {
                        users.put(id, getString(reader, 1));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        LOG.info("Loaded " + users.size() + " users.");

        return users;
    }

    public static Map<String, String> loadSystemUsers() {
        Map<String, String> usersByFullName = new HashMap<String, String>();
        NodeService nodeService = getNodeService();

        String firstName, lastName, name, username;
        for (NodeRef personRef : getPersonService().getAllPeople()) {
            firstName = (String) nodeService.getProperty(personRef, ContentModel.PROP_FIRSTNAME);
            lastName = (String) nodeService.getProperty(personRef, ContentModel.PROP_LASTNAME);
            name = StringUtils.defaultString(firstName).toLowerCase() + " " + StringUtils.defaultString(lastName).toLowerCase();
            username = (String) nodeService.getProperty(personRef, ContentModel.PROP_USERNAME);

            usersByFullName.put(name.trim(), username);
        }

        return usersByFullName;
    }

    private static Map<String, NodeRef> loadCompletedDocuments(File logFile, boolean versions) {
        Map<String, NodeRef> result = new HashMap<String, NodeRef>();
        CsvReader reader = null;

        if (!logFile.exists()) {
            LOG.info("Skipping loading previously completed documents, file does not exist: " + logFile);
            return result;
        }

        LOG.info("Loading previously completed documentId-s from file " + logFile);

        try {
            reader = ImportUtil.createLogReader(logFile);
            if (reader.readHeaders()) {
                while (reader.readRecord()) {
                    String xmlName = getString(reader, 1);
                    String nodeRef = getString(reader, 2);
                    result.put(versions ? FilenameUtils.getBaseName(xmlName) : xmlName, nodeRef != null ? new NodeRef(nodeRef) : null);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        LOG.info("Found " + result.size() + " previously completed document XML files.");
        return result;
    }

    private Pair<Integer, List<Map<Integer, File>>> getAllDocumentXmlSimpleFiles() {
        final boolean amphora = settings.isAmphoraOrigin();
        final List<Map<Integer, File>> xmlFiles = new ArrayList<Map<Integer, File>>();

        LOG.info("Getting all unprocessed document XML entries (without versions) from " + settings.getDataFolder());

        final File target;
        if (amphora) {
            target = new File(settings.getDataFolder(), DIR_DOCS);
        } else {
            target = new File(settings.getDataFolder());
        }
        target.listFiles(new XmlFilesFilter(xmlFiles, amphora ? 2 : 0));

        return Pair.newInstance(xmlFiles.size(), xmlFiles);
    }

    private final AtomicInteger xmlCount = new AtomicInteger();

    private Pair<Integer, List<Map<Integer, File>>> getAllDocumentXmlVersionFiles() {
        final Map<String, Map<Integer, File>> xmlFiles = new HashMap<String, Map<Integer, File>>();

        LOG.info("Getting all unprocessed document XML (with versions) entries from " + settings.getDataFolder());

        new File(settings.getDataFolder()).listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                // The base name without ".vN.xml" from the end of file name:
                String baseName = FilenameUtils.getBaseName(FilenameUtils.getBaseName(name));
                Matcher matcher = DOC_XML_WITH_VERSION_PATTERN.matcher(name);

                // Requirements:
                // 1) must be XML
                // 2) must contain version number
                // 3) must not already be imported (file base name with version number)
                // 4) XML files quota is not full unless this file is another version of a collected document

                if (!FilenameUtils.isExtension(name, "xml")
                        || !matcher.matches()
                        || importedDocXmls.containsKey(baseName)) {
                    return false;
                }

                Map<Integer, File> map = xmlFiles.get(baseName);

                if (map == null) {
                    map = new TreeMap<Integer, File>();
                    xmlFiles.put(baseName, map);
                }

                String num = matcher.group(1);
                map.put(Integer.valueOf(num), new File(dir, name));
                xmlCount.incrementAndGet();
                return true;
            }
        });

        return Pair.newInstance(xmlCount.get(), (List<Map<Integer, File>>) new ArrayList<Map<Integer, File>>(xmlFiles.values()));
    }

    private List<Map<Integer, File>> getNextDocumentXmlFiles() {
        final int batchSize = settings.getBatchSize();
        List<Map<Integer, File>> xmlFiles = new ArrayList<Map<Integer, File>>(batchSize);
        for (Iterator<Map<Integer, File>> i = notImportedDocXmls.iterator(); i.hasNext();) {
            xmlFiles.add(i.next());
            i.remove();
            if (xmlFiles.size() >= batchSize) {
                break;
            }
        }
        return xmlFiles;
    }

    /**
     * Imports given document XML. When provided XML is the next version of an imported document, reference to the imported document must be given, too.
     * 
     * @param documentXml Document XML for importing document.
     * @param documentRef Reference to the previously imported document version.
     * @param lastMeta Optional meta data from last version (defaults to current document meta data when <code>null</code>).
     * @return Document import result (can be used for logging).
     */
    private DocumentImportResult handleDocument(File documentXml, final NodeRef documentRef, DocumentMetadata lastMeta) {
        DocumentMetadata currentMeta = null;
        DocumentDynamic doc = null;

        try {
            // 1. Read most important meta-data from XML (may be provided in case of versions: the meta-data of the last document is used)
            currentMeta = getDocumentMeta(documentXml);
            if (lastMeta == null) {
                lastMeta = currentMeta;
            }

            // 2. Check if referred document file exists
            for (ImportFile file : currentMeta.getFiles()) {
                file.validate();
            }

            // 3. Determine parent volume/case by document URL from document XML
            NodeRef volumeRef = null;
            if (documentRef != null) {
                volumeRef = nodeService.getPrimaryParent(documentRef).getParentRef();
            } else {
                volumeRef = lastMeta.resolveVolumeRef(importedVolumesCases);
            }

            // 4. Read document properties from document XML according to resolved mappings file (may fail with an exception)
            MappedDocument mappedDoc = mapper.createMappedDocument(new SAXReader().read(documentXml).getRootElement(), lastMeta);

            if (mappedDoc == null) {
                throw new ImportValidationException("Could not resolve document type");
            }

            logService.setThreadLocalOverride(currentMeta.getModified(), currentMeta.getModifier(), getUserName(currentMeta.getModifier()));

            Map<QName, Serializable> props = mappedDoc.getPropertyValues();

            // 5. Apply custom property checks (by the specification)
            Date restrictionBeginSuggestion = settings.isSharepointOrigin() ? currentMeta.getCreated() : (Date) nodeService.getProperty(volumeRef, VolumeModel.Props.VALID_FROM);
            postProcessRestrictions(props, restrictionBeginSuggestion, volumeRef);
            postProcessOther(props, currentMeta, volumeRef);

            if (!settings.isSharepointOrigin() && !currentMeta.getFiles().isEmpty()) {
                boolean html = FilenameUtils.isExtension(currentMeta.getFiles().get(0).getTitle(), NON_DIGITAL_EXT);
                props.put(DocumentCommonModel.Props.STORAGE_TYPE, html ? "Paber" : "Digitaalne");
            }

            // 6. Create document and files
            if (documentRef == null) {
                doc = documentDynamicService.createNewDocument(mappedDoc.getDocumentTypeVersion(), volumeRef, false).getFirst();
                nodeService.addAspect(doc.getNodeRef(), DocumentCommonModel.Aspects.SEARCHABLE, null);

                // Store parents information on document:
                Map<QName, NodeRef> parentRefs = parentsCache.get(volumeRef);
                if (parentRefs == null) {
                    parentRefs = documentService.getDocumentParents(doc.getNodeRef());
                    parentsCache.put(volumeRef, parentRefs);
                }
                props.putAll(parentRefs);
                doc.getNode().getProperties().putAll(RepoUtil.toStringProperties(props));
                doc.getNode().getProperties().put(DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED, Boolean.TRUE);
                doc.getNode().getProperties().put(DocumentCommonModel.Props.DOCUMENT_IS_IMPORTED.toString(), Boolean.TRUE);

                if (currentMeta.getHistory() != null) {
                    for (DocumentHistory history : currentMeta.getHistory()) {
                        addDocumentLog(doc.getNodeRef(), history.getDatetime(), history.getCreator(), history.getEvent());
                    }
                    logService.setThreadLocalOverride(currentMeta.getModified(), currentMeta.getModifier(), getUserName(currentMeta.getModifier()));
                }
            } else {
                doc = documentDynamicService.getDocument(documentRef);
                doc.getNode().getProperties().putAll(RepoUtil.toStringProperties(props));
            }

            addChildren(mappedDoc, doc.getNode(), mappedDoc.getDocumentTypeVersion());

            documentDynamicService.updateDocument(doc, Arrays.asList(DocumentImportListener.BEAN_NAME), false, true);

            addFile(doc.getNodeRef(), currentMeta);
            documentService.updateSearchableFiles(doc.getNodeRef());

            updateDocumentLog(doc.getNodeRef(), currentMeta.getModified(), currentMeta.getModifier());

            if (currentMeta.getAssocs() != null && currentMeta.getAssocType() != null) {
                QName assocType = currentMeta.getAssocType();
                boolean assocSource = currentMeta.isDocumentAssocSource();

                for (String assoc : currentMeta.getAssocs()) {
                    NodeRef targetRef = importedDocXmls.get(assoc);

                    if (targetRef == null) {
                        LOG.warn("Could not resolve document association target for '" + assoc + "'.");
                    } else if (assocSource) {
                        nodeService.createAssociation(doc.getNodeRef(), targetRef, assocType);
                    } else {
                        nodeService.createAssociation(targetRef, doc.getNodeRef(), assocType);
                    }
                }
            }

            return new DocumentImportResult(currentMeta, documentXml, doc.getNodeRef(), props);

        } catch (ImportValidationException e) {
            if (doc != null && nodeService.exists(doc.getNodeRef())) {
                nodeService.deleteNode(doc.getNodeRef());
            }
            return new DocumentImportResult(currentMeta, documentXml, e.getMessage(), e.isReportAsSuccess());
        } catch (Exception e) {
            if (doc != null && nodeService.exists(doc.getNodeRef())) {
                nodeService.deleteNode(doc.getNodeRef());
            }
            LOG.warn("Caught unexpected exception during document XML import.", e);
            return new DocumentImportResult(currentMeta, documentXml, e.toString());
        }
    }

    private DocumentMetadata getDocumentMeta(File documentXml) throws ImportValidationException {
        try {
            Element docRoot = new SAXReader().read(documentXml).getRootElement();
            return mapper.getMetadata(docRoot, filesDir, settings);
        } catch (DocumentException e) {
            throw new ImportValidationException("Could not parse XML file: " + documentXml);
        }
    }

    private void postProcessRestrictions(Map<QName, Serializable> props, Date created, NodeRef volumeRef) {
        String accessRestriction = (String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);

        if ("Avalik, piiranguga".equalsIgnoreCase(StringUtils.strip(accessRestriction))
                || "Avalik, väljastatakse teabenõude korras".equalsIgnoreCase(StringUtils.strip(accessRestriction))) {
            props.put(DocumentDynamicModel.Props.PUBLISH_TO_ADR, PublishToAdr.REQUEST_FOR_INFORMATION.getValueName());
        }

        // a.
        if (accessRestriction != null && accessRestriction.indexOf('§') >= 0) {
            props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, accessRestriction);
            accessRestriction = AccessRestriction.AK.getValueName();
        }

        // b.
        accessRestriction = mapper.getRestrictionValue(accessRestriction);

        AccessRestriction restriction = AccessRestriction.valueNameOf(accessRestriction);

        if (restriction == null) {
            throw new RuntimeException("Invalid accessRestriction value '" + accessRestriction + "'");
        }

        props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION, accessRestriction);

        // Further checks are for restricted properties:
        if (restriction != AccessRestriction.AK) {
            return;
        }

        // c.
        if (props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE) == null) {
            props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, created);
        }

        // d.
        if (props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE) == null
                && Arrays.binarySearch(SYSTEM_OWNERS, props.get(DocumentCommonModel.Props.OWNER_NAME)) >= 0) {
            props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, nodeService.getProperty(volumeRef, VolumeModel.Props.VALID_FROM));
        }

        // e.
        Serializable reason = props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON);
        if (reason == null || reason.equals("-")) {
            props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, "§ 35 l 2 p 3");
        }

        if (props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE) == null) {
            Date dispositionDate = (Date) nodeService.getProperty(volumeRef, EventPlanModel.Props.RETAIN_UNTIL_DATE);

            if (dispositionDate != null) {
                // f.
                props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE, DateUtils.addYears(dispositionDate, 1));
            } else {
                // g.
                props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC, "Väljastatakse teabenõude korras");
            }
        }

        // Non-specified checks:

        if (props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE) == null) {
            throw new RuntimeException("accessRestrictionBeginDate is null, but accessRestriction = " + accessRestriction);
        }

        if (props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE) == null && props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC) == null) {
            throw new RuntimeException("accessRestrictionEndDate is null and accessRestrictionEndDesc is blank, but accessRestriction = " + accessRestriction);
        }
    }

    private void postProcessOther(Map<QName, Serializable> props, DocumentMetadata meta, NodeRef volumeRef) {
        String publishToAdr = (String) props.get(DocumentDynamicModel.Props.PUBLISH_TO_ADR);

        // i.
        if (settings.isSharepointOrigin()) {
            if (publishToAdr == null) {
                Date dispositionDate = (Date) nodeService.getProperty(volumeRef, EventPlanModel.Props.RETAIN_UNTIL_DATE);
                if (dispositionDate != null && dispositionDate.before(new Date())) {
                    publishToAdr = PublishToAdr.NOT_TO_ADR.getValueName();
                }
            }
            if (publishToAdr == null && settings.isNotPublishToAdr(meta.getCreated())) {
                publishToAdr = PublishToAdr.NOT_TO_ADR.getValueName();
            }
            if (publishToAdr == null && meta.getCreated() != null
                    && meta.getCreated().before(settings.getPublishToAdrWithFilesStartingFromDate())) {

                final String restriction = (String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
                if (AccessRestriction.OPEN.getValueName().equals(restriction)) {
                    publishToAdr = PublishToAdr.REQUEST_FOR_INFORMATION.getValueName();
                } else if (AccessRestriction.AK.getValueName().equals(restriction)) {
                    publishToAdr = PublishToAdr.TO_ADR.getValueName();
                }
            }
        }

        // i.(5-6) and j.
        if (publishToAdr == null && meta.isMakePublic() != null) {
            publishToAdr = meta.isMakePublic() ? PublishToAdr.TO_ADR.getValueName() : PublishToAdr.NOT_TO_ADR.getValueName();
        }

        props.put(DocumentDynamicModel.Props.PUBLISH_TO_ADR, publishToAdr);

        postProcessOwner(props, meta);

        String regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
        String shortRegNumber = (String) props.get(DocumentCommonModel.Props.SHORT_REG_NUMBER);
        String individualNumber = (String) props.get(DocumentCommonModel.Props.INDIVIDUAL_NUMBER);

        // m., n. & o.
        if (regNumber != null) {

            if (shortRegNumber == null) {
                shortRegNumber = regNumber.contains("/") ? StringUtils.substringAfterLast(regNumber, "/") : regNumber;

                if (REG_NUMBER_PATTERN.matcher(shortRegNumber).matches()) {
                    shortRegNumber = StringUtils.substringBeforeLast(shortRegNumber, "-");
                }
            }

            if (individualNumber == null && REG_NUMBER_PATTERN.matcher(regNumber).matches()) {
                individualNumber = StringUtils.substringAfterLast(regNumber, "-");
            }
        }

        props.put(DocumentCommonModel.Props.REG_NUMBER, regNumber);
        props.put(DocumentCommonModel.Props.SHORT_REG_NUMBER, shortRegNumber);
        props.put(DocumentCommonModel.Props.INDIVIDUAL_NUMBER, individualNumber);
    }

    private void postProcessOwner(Map<QName, Serializable> props, DocumentMetadata meta) {
        final String ownerName = (String) props.get(DocumentCommonModel.Props.OWNER_NAME);
        String ownerId = (String) props.get(DocumentCommonModel.Props.OWNER_ID);

        if (ownerId == null && settings.isSharepointOrigin()) {

            if (ownerName != null && Arrays.binarySearch(SYSTEM_OWNERS, ownerName) >= 0) {
                ownerId = settings.getDefaultOwnerId();
            } else if (ownerName != null) {
                int separatorIndex = ownerName.indexOf('\\');
                String userName = separatorIndex >= 0 ? ownerName.substring(separatorIndex + 1) : ownerName;
                String userFullName = externalUsersById.get(userName);

                if (userFullName != null && !USER_INFO_DUPL.equals(userFullName)) {
                    ownerId = systemUsersByFullName.get(userFullName.trim().toLowerCase());
                }
            }

            if (ownerId != null) {
                addCommentLine(props, "Dokumendi algne vastutaja: " + ownerName);
            }

        } else if (ownerId == null && settings.isAmphoraOrigin()) {
            if (ownerName != null) {
                ownerId = systemUsersByFullName.get(ownerName.trim().toLowerCase());
            }

            if (ownerId == null && meta.getCreator() != null) {
                ownerId = systemUsersByFullName.get(meta.getCreator().trim().toLowerCase());
            }

        } else if (ownerId == null && settings.isRiigikohusOrigin()) {
            if (ownerName != null) {
                ownerId = systemUsersByFullName.get(ownerName.trim().toLowerCase());
            }
        }

        if (ownerId == null) {
            ownerId = settings.getDefaultOwnerId();
            if (ownerName != null) {
                addCommentLine(props, "Dokumendi algne vastutaja: " + ownerName);
            }
        }

        if (ownerId != null) {
            Map<QName, Serializable> userProps = userService.getUserProperties(ownerId);
            props.put(DocumentCommonModel.Props.OWNER_ID, ownerId);
            if (userProps != null) {
                props.put(DocumentCommonModel.Props.OWNER_NAME, UserUtil.getPersonFullName1(userProps));
                props.put(DocumentCommonModel.Props.OWNER_EMAIL, userProps.get(ContentModel.PROP_EMAIL));
                props.put(DocumentCommonModel.Props.OWNER_PHONE, userProps.get(ContentModel.PROP_TELEPHONE));
                props.put(DocumentCommonModel.Props.OWNER_JOB_TITLE, userProps.get(ContentModel.PROP_JOBTITLE));
                props.put(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT, userProps.get(ContentModel.PROP_ORGANIZATION_PATH));
                props.put(DocumentDynamicModel.Props.OWNER_SERVICE_RANK, userProps.get(ContentModel.PROP_SERVICE_RANK));
                props.put(DocumentDynamicModel.Props.OWNER_WORK_ADDRESS, userProps.get(ContentModel.PROP_STREET_HOUSE));
            }
        }
    }

    private void addFile(NodeRef documentRef, DocumentMetadata meta) {
        if (meta.getFiles().isEmpty()) {
            return;
        }

        NodeRef currentFileRef;
        final List<FileInfo> currentFiles = fileFolderService.listFiles(documentRef);

        if (settings.isDocsWithVersions() && !currentFiles.isEmpty()) {
            ImportFile file = meta.getFiles().get(0);
            currentFileRef = currentFiles.get(0).getNodeRef();
            versionsService.addVersionLockableAspect(currentFileRef);
            versionsService.setVersionLockableAspect(currentFileRef, false);
            versionsService.updateVersion(currentFileRef, file.getTitle(), false);
            // Unlock the node again, since previous method locked it
            versionsService.setVersionLockableAspect(currentFileRef, false);

            String fileName = StringUtils.defaultIfEmpty(file.getTitle(), file.getFilename());
            String repoFileName = generalService.getUniqueFileName(documentRef, FilenameUtil.makeSafeFilename(fileName));

            ContentWriter writer = fileFolderService.getWriter(currentFileRef);
            generalService.writeFile(writer, file.getFile(), fileName, null);

            updateFileInfo(currentFileRef, file);
            nodeService.setProperty(currentFileRef, ContentModel.PROP_NAME, repoFileName); // Also handles childAssocName change
        } else {
            for (ImportFile file : meta.getFiles()) {
                String fileName = StringUtils.defaultIfEmpty(file.getTitle(), file.getFilename());
                String repoFileName = generalService.getUniqueFileName(documentRef, FilenameUtil.makeSafeFilename(fileName));

                final FileInfo fileInfo = fileFolderService.create(documentRef, repoFileName, ContentModel.TYPE_CONTENT);
                final ContentWriter writer = fileFolderService.getWriter(fileInfo.getNodeRef());

                generalService.writeFile(writer, file.getFile(), fileName, null);
                currentFileRef = fileInfo.getNodeRef();
                updateFileInfo(currentFileRef, file);
            }
        }

    }

    private void updateFileInfo(NodeRef fileRef, ImportFile file) {
        String userName = getUserName(file.getModifier());

        Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>(3);
        aspectProperties.put(VersionsModel.Props.VersionModified.MODIFIED, file.getModified());
        aspectProperties.put(VersionsModel.Props.VersionModified.FIRSTNAME, StringUtils.substringBeforeLast(userName, " "));
        aspectProperties.put(VersionsModel.Props.VersionModified.LASTNAME, StringUtils.substringAfterLast(userName, " "));

        if (nodeService.hasAspect(fileRef, VersionsModel.Aspects.VERSION_MODIFIED)) {
            nodeService.addProperties(fileRef, aspectProperties);
        } else {
            nodeService.addAspect(fileRef, VersionsModel.Aspects.VERSION_MODIFIED, aspectProperties);
        }

        nodeService.setProperty(fileRef, FileModel.Props.DISPLAY_NAME, file.getTitle());
        nodeService.setProperty(fileRef, FileModel.Props.ACTIVE, file.isActive());
        nodeService.setProperty(fileRef, ContentModel.PROP_NAME, file.getTitle()); // Should be set to same value as found in original XML
        nodeService.setProperty(fileRef, ContentModel.PROP_CREATED, file.getCreated());
        nodeService.setProperty(fileRef, ContentModel.PROP_MODIFIED, file.getModified());
        nodeService.setProperty(fileRef, ContentModel.PROP_CREATOR, file.getCreator());
        nodeService.setProperty(fileRef, ContentModel.PROP_MODIFIER, file.getModifier());
    }

    private void addChildren(MappedDocument document, Node parent, DocumentTypeVersion docVersion, QName... hierarchy) throws ImportValidationException {
        Map<QName, Integer> childNodesProcessed = new HashMap<QName, Integer>();

        for (Entry<QName, MappedDocument> entry : document.getChildren().entrySet()) {
            List<Node> childNodes = parent.getAllChildAssociations(entry.getKey());

            if (childNodes == null || childNodes.isEmpty()) {
                throw new ImportValidationException("No child nodes exist of type " + entry.getKey().toPrefixString(getNamespaceService())
                        + " on node " + parent.getType().toPrefixString(getNamespaceService())
                        + ", hierarchy=" + Arrays.asList(hierarchy)
                        + ", objectTypeIdAndVersion=" + DocAdminUtil.getDocTypeIdAndVersionNr(parent));
            }

            Integer index = childNodesProcessed.get(entry.getKey());
            index = index == null ? 0 : index;
            childNodesProcessed.put(entry.getKey(), index + 1);

            if (index >= childNodes.size()) {
                getDocumentDynamicService().createChildNodesHierarchyAndSetDefaultPropertyValues(parent, hierarchy, docVersion);
            }

            Node childNode = childNodes.get(index);
            childNode.getProperties().putAll(RepoUtil.toStringProperties(entry.getValue().getPropertyValues()));

            addChildren(entry.getValue(), childNode, docVersion, (QName[]) ArrayUtils.add(hierarchy, entry.getKey()));
        }
    }

    private void updateDocumentLog(NodeRef documentRef, Date modified, final String modifier) {
        final String userName = getUserName(modifier);

        for (ChildAssociationRef assoc : nodeService.getChildAssocs(documentRef, ContentModel.TYPE_CONTENT, ContentModel.ASSOC_CONTAINS)) {
            NodeRef logRef = assoc.getChildRef();
            Date created = (Date) nodeService.getProperty(logRef, VersionsModel.Props.VersionModified.MODIFIED);

            if (modified.before(created)) {
                continue;
            }

            nodeService.setProperty(logRef, VersionsModel.Props.VersionModified.MODIFIED, modified);
            nodeService.setProperty(logRef, VersionsModel.Props.VersionModified.FIRSTNAME, StringUtils.substringBeforeLast(userName, " "));
            nodeService.setProperty(logRef, VersionsModel.Props.VersionModified.LASTNAME, StringUtils.substringAfterLast(userName, " "));
        }
    }

    private void addDocumentLastLog(DocumentImportResult result) {
        if (result.getDocumentRef() != null) {
            addDocumentLog(result.getDocumentRef(), null, "IMPORT", "Dokumendi importimine");
        }
    }

    private void addDocumentLog(NodeRef documentRef, Date created, String creatorId, String event) {
        logService.setThreadLocalOverride(created, creatorId, getUserName(creatorId));

        LogEntry logEntry = new LogEntry();
        logEntry.setComputerIp("127.0.0.1");
        logEntry.setComputerName("localhost");
        logEntry.setLevel(LogObject.DOCUMENT.getLevel());
        logEntry.setObjectName(LogObject.DOCUMENT.getObjectName());
        logEntry.setCreatorId(creatorId);
        logEntry.setCreatorName(getUserName(creatorId));
        logEntry.setEventDescription(event);
        logEntry.setObjectId(documentRef.toString());
        logService.addLogEntry(logEntry);
    }

    private String getUserName(String userId) {
        String userName = externalUsersById.get(userId);
        return userName != null && !USER_INFO_DUPL.equals(userName) ? userName : userId;
    }

    private static void addCommentLine(Map<QName, Serializable> props, String comment) {
        String currentComment = (String) props.get(DocumentCommonModel.Props.COMMENT);

        if (currentComment == null) {
            currentComment = comment;
        } else {
            currentComment += "\n" + comment;
        }

        props.put(DocumentCommonModel.Props.COMMENT, currentComment);
    }

    private class XmlFilesFilter implements FilenameFilter {
        private final List<Map<Integer, File>> xmlFiles;
        private final int subdirsAllowed;

        private XmlFilesFilter(List<Map<Integer, File>> xmlFiles, int subdirsAllowed) {
            this.xmlFiles = xmlFiles;
            this.subdirsAllowed = subdirsAllowed;
        }

        @Override
        public boolean accept(File dir, String name) {
            File f = new File(dir, name);
            if (f.isDirectory()) {
                if (subdirsAllowed > 0) {
                    f.list(new XmlFilesFilter(xmlFiles, subdirsAllowed - 1));
                }
                return false;
            } else if (!f.isFile() || !FilenameUtils.isExtension(name, "xml") || name.equals(settings.getMappingsFileName())
                    || importedDocXmls.containsKey(FilenameUtils.getBaseName(name))) {
                return false;
            }

            xmlFiles.add(Collections.singletonMap(0, f));
            return false;
        }
    }

    public static class VolumeCase {

        private final NodeRef nodeRef;
        private final Date validFrom;
        private final Date validTo;
        private final String function;
        private final String series;

        public VolumeCase(CsvReader reader) throws IOException {
            nodeRef = new NodeRef(getString(reader, 5));
            validFrom = getDate(reader, 6);
            validTo = getDate(reader, 7);
            function = getString(reader, 8);
            series = getString(reader, 9);
        }

        public NodeRef getNodeRef() {
            return nodeRef;
        }

        public boolean isInVolumePeriod(Date date) {
            return date != null && !validFrom.after(date) && (validTo == null || !validTo.before(date) || DateUtils.isSameDay(validTo, date));
        }

        public boolean isLocation(String docFunction, String docSeries, String docVolume) {
            if (docFunction == null || !docFunction.equals(function) || docSeries == null || !docSeries.equals(series) || docVolume == null) {
                return false;
            }

            if (docVolume.length() == 4 && StringUtils.isNumeric(docVolume)) {
                int year = Integer.parseInt(docVolume);
                Calendar cal = Calendar.getInstance();
                cal.setTime(validFrom);
                return year == cal.get(Calendar.YEAR);
            } else if (docVolume.length() == 10) {
                return isInVolumePeriod(ImportUtil.getDate(docVolume));
            }

            return false;
        }
    }

    public static class DocumentHistory implements Comparable<DocumentHistory> {

        private final Date datetime;
        private final String creator;
        private final String event;

        public DocumentHistory(Date datetime, String creator, String event) {
            this.datetime = datetime;
            this.creator = creator;
            this.event = event;
        }

        public Date getDatetime() {
            return datetime;
        }

        public String getCreator() {
            return creator;
        }

        public String getEvent() {
            return event;
        }

        @Override
        public int compareTo(DocumentHistory o) {
            return datetime.compareTo(o.datetime);
        }
    }
}
