package ee.webmedia.alfresco.postipoiss;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentListService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPrivilegeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.ACCESS_RESTRICTION;
import static ee.webmedia.alfresco.document.model.DocumentCommonModel.Props.OWNER_ID;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.util.Assert;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.document.register.model.RegNrHolder;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentService.AssocType;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.ConvertException;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.DocumentValue;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.Mapping;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.Pair;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.PropMapping;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper.PropertyValue;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * Imports documents and files from Postipoiss.
 * 
 * @author Aleksei Lissitsin
 * @author Alar Kvell
 */
public class PostipoissDocumentsImporter {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PostipoissDocumentsImporter.class);

    private static final String PP_ELEMENT_DOK_NR = "dok_nr";
    private static final String PP_ELEMENT_ALUS_DOK_NR = "alus_dok_nr";
    /**
     * SIM "toimik_sari", MV "toimik", PPA "volume", now comes from mappings.xml {@code <prop from="..." to="_regNumberWithoutIndividual"/>}.
     * For example
     * 
     * <pre>
     * &lt;volume&gt;10.1-01/11/33070&lt;/volume&gt;
     * &lt;regNumber&gt;33070&lt;/regNumber&gt;
     * &lt;jrknr&gt;2&lt;/jrknr&gt;
     * 
     * docdyn:regNumber = 10.1-01/11/33070-2
     * docdyn:shortRegNumber = 33070
     * docdyn:individualNumber = 2
     * </pre>
     */
    private static final String MAPPINGS_PROP_TO_REG_NUMBER_WITHOUT_INDIVIDUAL = "_regNumberWithoutIndividual";
    /**
     * SIM "reg_nr", MV "registreerimis_nr", PPA "regNumber", now comes from mappings.xml {@code <prop from="..." to="_shortRegNumber" />}
     */
    private static final String MAPPINGS_PROP_TO_SHORT_REG_NUMBER = "_shortRegNumber";
    private static final String PP_ELEMENT_PARITOLU = "paritolu"; // SIM "paritolu"
    private static final String PP_ELEMENT_PARITOLU2 = "parituolu";
    private static final String PP_VALUE_PARITOLU_ALGATUSDOKUMENT = "Algatusdokument";
    private static final String PP_VALUE_PARITOLU_VASTUSDOKUMENT = "Vastusdokument";
    private static final String PP_VALUE_PARITOLU_JARG = "Järg";
    private static final String PP_VALUE_PARITOLU_JARELEPARIMINE = "Järelepärimine";
    private static final String PP_ELEMENT_FAIL = "fail"; // SIM "fail", MV "viide"
    private static final String PP_ELEMENT_SEOSED = "seosed";
    private static final String PP_ELEMENT_SUUND = "suund";
    /**
     * MV "dok_liik", SIM, PPA "dokliik", now comes from mappings.xml {@code <prop from="..." to="_documentTypeFrom" />}
     */
    private static final String MAPPINGS_PROP_TO_DOCUMENT_TYPE_FROM = "_documentTypeFrom";
    /**
     * MV, SIM, PPA "jrknr", now comes from mappings.xml {@code <prop from="..." to="_individualNumber" />}
     */
    private static final String MAPPINGS_PROP_TO_INDIVIDUAL_NUMBER = "_individualNumber";

    /*
     * TODO lisada kellaaeg csv'sse
     * TODO kirjutada failide mimetype ja encoding csv'sse
     * TODO ETA arvutamist tegin korda - võtsin AbstraceNodeUpdater'ist uuema koodi; ainus väike asi, et kui viimane batch tuleb täpselt täis, siis viimast completed info rida
     * trükib 2 korda
     */

    protected static final int LAST_VOLUME_YEAR = 11;

    protected static final char CSV_SEPARATOR = ';';
    private static final String CREATOR_MODIFIER = "DELTA";
    private static final FastDateFormat staticDateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm:ss");

    private final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private final DateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    protected SAXReader xmlReader = new SAXReader();

    private File dataFolder;
    private File workFolder;
    private NodeRef archivalsRoot;
    private int batchSize;
    private String defaultOwnerId;

    // [SPRING BEANS
    private TransactionService transactionService;
    private DocumentService documentService;
    private GeneralService generalService;
    private FileFolderService fileFolderService;
    private SendOutService sendOutService;
    private NodeService nodeService;
    private PersonService personService;
    private PostipoissDocumentsMapper postipoissDocumentsMapper;
    private BehaviourFilter behaviourFilter;
    private PostipoissImporter postipoissImporter;

    public PostipoissDocumentsImporter(PostipoissImporter postipoissImporter) {
        // <bean id="postipoissDocumentsImporter" class="ee.webmedia.alfresco.postipoiss.PostipoissDocumentsImporter">
        // <property name="documentService" ref="DocumentService" />
        // <property name="transactionService" ref="TransactionService" />
        // <property name="generalService" ref="GeneralService" />
        // <property name="fileFolderService" ref="FileFolderService" />
        // <property name="nodeService" ref="NodeService" />
        // <property name="personService" ref="PersonService" />
        // <property name="sendOutService" ref="SendOutService" />
        // <property name="postipoissDocumentsMapper" ref="postipoissDocumentsMapper" />
        // <property name="behaviourFilter" ref="policyBehaviourFilter" />
        // </bean>
        //
        // <bean id="postipoissDocumentsMapper" class="ee.webmedia.alfresco.postipoiss.PostipoissDocumentsMapper">
        // <property name="namespaceService" ref="NamespaceService" />
        // <property name="dictionaryService" ref="DictionaryService" />
        // <property name="generalService" ref="GeneralService" />
        // </bean>
        setDocumentService(BeanHelper.getDocumentService());
        setTransactionService(BeanHelper.getTransactionService());
        setGeneralService(BeanHelper.getGeneralService());
        setFileFolderService(BeanHelper.getFileFolderService());
        setNodeService(BeanHelper.getNodeService());
        setPersonService(BeanHelper.getPersonService());
        setSendOutService(BeanHelper.getSendOutService());
        postipoissDocumentsMapper = new PostipoissDocumentsMapper();
        postipoissDocumentsMapper.setNamespaceService(getNamespaceService());
        postipoissDocumentsMapper.setDictionaryService(BeanHelper.getDictionaryService());
        postipoissDocumentsMapper.setGeneralService(BeanHelper.getGeneralService());
        setBehaviourFilter(BeanHelper.getPolicyBehaviourFilter());
        setPostipoissImporter(postipoissImporter);

        dateFormat.setLenient(false);
        dateTimeFormat.setLenient(false);
    }

    // INJECTORS
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setPostipoissDocumentsMapper(PostipoissDocumentsMapper postipoissDocumentsMapper) {
        this.postipoissDocumentsMapper = postipoissDocumentsMapper;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setSendOutService(SendOutService sendOutService) {
        this.sendOutService = sendOutService;
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
        this.behaviourFilter = behaviourFilter;
    }

    public void setPostipoissImporter(PostipoissImporter postipoissImporter) {
        this.postipoissImporter = postipoissImporter;
    }

    /**
     * Runs documents import process
     */
    public void runImport(File dataFolder, File workFolder, NodeRef archivalsRoot, File mappingsFile, int batchSize, String defaultOwnerId) throws Exception {
        this.dataFolder = dataFolder;
        this.workFolder = workFolder;
        this.archivalsRoot = archivalsRoot;
        this.batchSize = batchSize;
        this.defaultOwnerId = defaultOwnerId;
        init();
        // Doc import
        try {
            mappings = postipoissDocumentsMapper.loadMetadataMappings(mappingsFile);
            loadDocuments();
            loadCompletedDocuments();
            loadToimiks();
            loadPostponedAssocs();
            loadUsers();
            createDocuments();
        } finally {
            writePostponedAssocs();
            writeUsersFound();
            documentsMap = null;
            toimikud = null;
            normedToimikud = null;
            mappings = null;
            postponedAssocs = null;
        }

        // Files import
        try {
            loadFiles();
            loadCompletedFiles();
            importFiles();
        } finally {
            filesMap = null;
        }

        // Files indexing
        loadIndexedFiles();
        indexFiles();
    }

    private void init() {
        documentsMap = new TreeMap<Integer, File>();
        filesMap = new TreeMap<Integer, List<File>>();
        completedDocumentsMap = new TreeMap<Integer, NodeRef>();
        completedFiles = new HashSet<Integer>();
        postponedAssocs = new TreeMap<Integer, List<PostponedAssoc>>();
        postponedAssocsCommited = new TreeMap<Integer, List<PostponedAssoc>>();
        mappings = null;
        toimikud = null;
        normedToimikud = null;
        indexedFiles = null;
        filesToIndex = null;
        filesToProceed = null;
    }

    protected NavigableMap<Integer /* documentId */, File> documentsMap;
    protected NavigableMap<Integer /* documentId */, List<File>> filesMap;
    protected NavigableMap<Integer /* documentId */, NodeRef> completedDocumentsMap;
    protected Set<Integer> completedFiles;
    protected NavigableMap<Integer /* documentId */, List<PostponedAssoc>> postponedAssocs;
    protected NavigableMap<Integer /* documentId */, List<PostponedAssoc>> postponedAssocsCommited;
    private final Map<String, org.alfresco.util.Pair<NodeRef, Map<QName, Serializable>>> userDataByUserName = new HashMap<String, org.alfresco.util.Pair<NodeRef, Map<QName, Serializable>>>();

    // protected Map<String /* ownerNameCleaned */, String /* ownerId */> allUsersByOwnerNameCleaned;
    // protected Map<String /* ownerId */, String /* ownerName */> allUsersByOwnerId;
    protected Map<String /* ownerName */, Integer /* count */> usersFound;
    protected Map<String /* ownerName */, Integer /* count */> usersFoundCommited;
    protected Map<String /* ownerName */, Integer /* count */> usersNotFound;
    protected Map<String /* ownerName */, Integer /* count */> usersNotFoundCommited;

    protected NavigableSet<Integer> filesToProceed;
    protected File completedDocumentsFile;
    protected File failedDocumentsFile;
    protected File completedFilesFile;
    protected File postponedAssocsFile;
    protected File usersFoundFile;
    protected File usersNotFoundFile;

    private Map<String, Mapping> mappings;

    protected void loadDocuments() {
        documentsMap = new TreeMap<Integer, File>();
        log.info("Getting xml entries of " + dataFolder);
        File[] files = dataFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return StringUtils.isNotBlank(name) && name.endsWith(".xml");
            }
        });

        log.info("Directory listing contains " + files.length + " xml entries, parsing them to documents");
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".xml")) {
                // Document
                int i = name.lastIndexOf("_");
                if (i == -1) {
                    continue;
                }
                String documentType = name.substring(0, i);
                if (StringUtils.isBlank(documentType)) {
                    continue;
                }
                try {
                    Integer documentId = new Integer(name.substring(i + 1, name.length() - 4));
                    log.debug("Found documentId=" + documentId + " documentType='" + documentType + "'");
                    documentsMap.put(documentId, file);
                } catch (NumberFormatException e) {
                    // Ignore because there are other XML files also in this folder
                    log.info("Ignoring " + name);
                }
            }
        }
        log.info("Completed parsing directory listing, got " + documentsMap.size() + " documents");
    }

    protected void loadFiles() {
        filesMap = new TreeMap<Integer, List<File>>();
        log.info("Getting non-xml entries of " + dataFolder);
        File[] files = dataFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File arg0, String name) {
                return StringUtils.isNotBlank(name) && !name.endsWith(".xml");
            }
        });

        if (files == null) {
            throw new RuntimeException("Data folder " + dataFolder + " not found!");
        }
        log.info("There are " + files.length + " non-xml entries, parsing them to files");
        int count = 0;
        for (File file : files) {
            String name = file.getName();
            int i = name.lastIndexOf(".");
            if (i == -1) {
                continue;
            }
            String filename = name.substring(0, i);
            if (StringUtils.isBlank(filename)) {
                continue;
            }
            try {
                Integer documentId = new Integer(name.substring(i + 1));
                List<File> fileList = filesMap.get(documentId);
                if (fileList == null) {
                    fileList = new ArrayList<File>();
                    filesMap.put(documentId, fileList);
                }
                fileList.add(file);
                count++;
            } catch (NumberFormatException e) {
                // Ignore because there are CSV files also in this folder
                log.info("Ignoring " + name);
            }
        }
        log.info("Completed parsing directory listing, got " + count + " files");
    }

    protected void loadCompletedFiles() throws Exception {
        completedFilesFile = new File(workFolder, "completed_files.csv");
        completedFiles = new HashSet<Integer>();

        if (!completedFilesFile.exists()) {
            log.info("Skipping loading previously completed files, file does not exist: " + completedFilesFile);
        } else {

            log.info("Loading previously completed files from file " + completedFilesFile);

            CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(completedFilesFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
            try {
                reader.readHeaders();
                while (reader.readRecord()) {
                    Integer documentId = new Integer(reader.get(0));
                    completedFiles.add(documentId);
                }
            } finally {
                reader.close();
            }
            log.info("Loaded " + completedFiles.size() + " documents with previously completed files");

            int removedFilesCount = 0;
            for (Integer documentId : completedFiles) {
                List<File> filesList = filesMap.remove(documentId);
                if (filesList != null) {
                    removedFilesCount += filesList.size();
                }
            }
            log.info("Removed those files from current file list (total " + removedFilesCount + " files)");
        }

        filesToProceed = new TreeSet<Integer>(filesMap.keySet());
        filesToProceed.retainAll(completedDocumentsMap.keySet());

        log.info("Total documents with files to proceed: " + filesToProceed.size());
    }

    private File indexedFilesFile;
    private Set<Integer> indexedFiles;
    private NavigableSet<Integer> filesToIndex;

    protected void loadIndexedFiles() throws Exception {
        indexedFilesFile = new File(workFolder, "indexed_files.csv");
        indexedFiles = new HashSet<Integer>();

        if (!indexedFilesFile.exists()) {
            log.info("Skipping loading previously indexed files, file does not exist: " + indexedFilesFile);
        } else {

            log.info("Loading previously indexed files from file " + indexedFilesFile);

            CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(indexedFilesFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
            try {
                reader.readHeaders();
                while (reader.readRecord()) {
                    Integer documentId = new Integer(reader.get(0));
                    indexedFiles.add(documentId);
                }
            } finally {
                reader.close();
            }
            log.info("Loaded " + indexedFiles.size() + " documents with previously indexed files");
        }

        filesToIndex = new TreeSet<Integer>(completedFiles);
        filesToIndex.removeAll(indexedFiles);
        filesToIndex.retainAll(completedDocumentsMap.keySet());

        log.info("Total documents with files to index: " + filesToIndex.size());
    }

    private void indexFiles() {
        if (filesToIndex.isEmpty()) {
            log.info("Skipping files indexing, no files found");
            return;
        }
        log.info("Starting files indexing, total = " + filesToIndex.size());
        FilesIndexBatchProgress batchProgress = new FilesIndexBatchProgress(filesToIndex);
        batchProgress.run();
        log.info("Files INDEXING COMPLETE :)");
    }

    private class FilesIndexBatchProgress extends BatchProgress<Integer> {

        public FilesIndexBatchProgress(Set<Integer> origin) {
            this.origin = origin;
            processName = "Files indexing";
        }

        @Override
        void executeBatch() {
            createFilesIndexBatch(batchList);
        }
    }

    protected void createFilesIndexBatch(final List<Integer> batchList) {
        final Set<Integer> batchCompletedFiles = new HashSet<Integer>(batchSize);
        for (Integer documentId : batchList) {
            if (log.isTraceEnabled()) {
                log.trace("Processing files with docId = " + documentId);
            }
            NodeRef documentRef = completedDocumentsMap.get(documentId);
            if (documentRef == null) {
                continue;
            }
            if (!nodeService.exists(documentRef)) {
                log.error("Skipping indexing files for documentId=" + documentId + ", node does not exist: " + documentRef);
                continue;
            }
            try {
                Map<QName, Serializable> origProps = nodeService.getProperties(documentRef);
                Map<QName, Serializable> setProps = new HashMap<QName, Serializable>();
                setProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
                setProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
                documentService.updateSearchableFiles(documentRef, setProps);
            } catch (Exception e) {
                throw new RuntimeException("Error indexing files for document id=" + documentId + ", nodeRef=" + documentRef + ": " + e.getMessage(), e);
            }
            batchCompletedFiles.add(documentId);
        }
        bindCsvWriteAfterCommit(indexedFilesFile, new CsvWriterClosure() {

            @Override
            public void execute(CsvWriter writer) throws IOException {
                for (Integer documentId : batchCompletedFiles) {
                    writer.writeRecord(new String[] {
                            documentId.toString()
                    });
                    indexedFiles.add(documentId);
                }
            }

            @Override
            public String[] getHeaders() {
                return new String[] { "documentId" };
            }
        });
    }

    protected void loadUsers() throws Exception {
        // @formatter:off
        /*
        log.info("Loading all users");
        Set<NodeRef> userRefs = personService.getAllPeople();
        allUsersByOwnerNameCleaned = new HashMap<String, String>(userRefs.size() * 2);
        allUsersByOwnerId = new HashMap<String, String>(userRefs.size());
        for (NodeRef nodeRef : userRefs) {
            Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
            String ownerId = (String) props.get(ContentModel.PROP_USERNAME);
            Assert.isTrue(StringUtils.isNotBlank(ownerId), nodeRef.toString());
            String firstName = cleanUserFullName((String) props.get(ContentModel.PROP_FIRSTNAME));
            String lastName = cleanUserFullName((String) props.get(ContentModel.PROP_LASTNAME));

            String combination1 = firstName + lastName;
            Assert.isTrue(!allUsersByOwnerNameCleaned.containsKey(combination1), combination1);

            String combination2 = lastName + firstName;
            Assert.isTrue(!allUsersByOwnerNameCleaned.containsKey(combination2), combination2);

            allUsersByOwnerNameCleaned.put(combination1, ownerId);
            allUsersByOwnerNameCleaned.put(combination2, ownerId);
            allUsersByOwnerId.put(ownerId, UserUtil.getPersonFullName1(props));
        }
        log.info("Loaded " + allUsersByOwnerId.size() + " users");
        */
        // @formatter:on

        usersFoundFile = new File(workFolder, "users_found.csv");
        usersFound = new HashMap<String, Integer>();
        if (usersFoundFile.exists()) {
            log.info("Loading found users from file " + usersFoundFile);
            CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(usersFoundFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
            try {
                reader.readHeaders();
                while (reader.readRecord()) {
                    usersFound.put(reader.get(0), new Integer(reader.get(1)));
                }
            } finally {
                reader.close();
            }
        } else {
            log.info("Skipping loading found users, file does not exist: " + usersFoundFile);
        }
        usersFoundCommited = new HashMap<String, Integer>(usersFound);

        usersNotFoundFile = new File(workFolder, "users_not_found.csv");
        usersNotFound = new HashMap<String, Integer>();
        if (usersNotFoundFile.exists()) {
            log.info("Loading not-found users from file " + usersNotFoundFile);
            CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(usersNotFoundFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
            try {
                reader.readHeaders();
                while (reader.readRecord()) {
                    usersNotFound.put(reader.get(0), new Integer(reader.get(1)));
                }
            } finally {
                reader.close();
            }
        } else {
            log.info("Skipping loading not-found users, file does not exist: " + usersNotFoundFile);
        }
        usersNotFoundCommited = new HashMap<String, Integer>(usersNotFound);
    }

    private void writeUsersFound() {
        if (usersFoundFile != null) {
            try {
                CsvWriter writer = new CsvWriter(new FileWriter(usersFoundFile, false), CSV_SEPARATOR);
                writer.writeRecord(new String[] {
                        "userFullName",
                        "count"
                });
                try {
                    for (Entry<String, Integer> entry : usersFoundCommited.entrySet()) {
                        writer.writeRecord(new String[] { entry.getKey(), entry.getValue().toString() });
                    }
                } finally {
                    writer.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("Error writing CSV file '" + usersFoundFile + "': " + e.getMessage(), e);
            }
        }
        if (usersNotFoundFile != null) {
            try {
                CsvWriter writer = new CsvWriter(new FileWriter(usersNotFoundFile, false), CSV_SEPARATOR);
                writer.writeRecord(new String[] {
                        "userFullName",
                        "count"
                });
                try {
                    for (Entry<String, Integer> entry : usersNotFoundCommited.entrySet()) {
                        writer.writeRecord(new String[] { entry.getKey(), entry.getValue().toString() });
                    }
                } finally {
                    writer.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("Error writing CSV file '" + usersNotFoundFile + "': " + e.getMessage(), e);
            }
        }
    }

    private void setOwnerProperties(Map<QName, Serializable> props, Mapping mapping) {
        String ownerId = StringUtils.stripToNull((String) props.get(OWNER_ID));
        if (StringUtils.isNotBlank(ownerId)) {
            // find user by ownerId
            // if found, then overwrite all owner fields
            if (!setOwnerIfExists(props, ownerId, mapping)) {
                // if not found, then preserve values that came from mappings
                Integer count = usersNotFound.get(ownerId);
                if (count == null) {
                    count = 0;
                }
                count++;
                usersNotFound.put(ownerId, count);
            }
        } else {
            // find user by ownerId
            // if found, then overwrite all owner fields
            if (!setOwnerIfExists(props, defaultOwnerId, mapping)) {
                // if not found, throw exception
                throw new RuntimeException("User with default ownerId not found: " + defaultOwnerId);
            }
        }

        // @formatter:off
        /*
        String ownerName = (String) props.get(OWNER_NAME);
        String ownerId = null;
        String ownerJobTitle = null;

        // ownerName = processOwnerNameSim(ownerName); // SIM had extra parsing of ownerName

        ownerName = StringUtils.trim(StringUtils.strip(ownerName));
        if (StringUtils.isNotBlank(ownerName)) {
            while (ownerName.indexOf("  ") >= 0) {
                ownerName = StringUtils.replace(ownerName, "  ", " ");
            }
            ownerId = allUsersByOwnerNameCleaned.get(cleanUserFullName(ownerName)); // if not found then null
            if (StringUtils.isBlank(ownerId)) {
                Integer count = usersNotFound.get(ownerName);
                if (count == null) {
                    count = 0;
                }
                count++;
                usersNotFound.put(ownerName, count);
            } else {
                ownerName = allUsersByOwnerId.get(ownerId);
                Assert.isTrue(StringUtils.isNotBlank(ownerName), ownerId);
                Integer count = usersFound.get(ownerName);
                if (count == null) {
                    count = 0;
                }
                count++;
                usersFound.put(ownerName, count);
            }
        } else {
            ownerName = IMPORTER_NAME;
            ownerId = IMPORTER_ID_CODE;
        }
        props.put(OWNER_ID, ownerId);
        props.put(OWNER_NAME, ownerName);
        if (StringUtils.isNotBlank(ownerJobTitle)) {
            props.put(OWNER_JOB_TITLE, ownerJobTitle); // kirjutab üle kui PP xml'is oli juba olemas
        }
        // props.put(OWNER_JOB_TITLE, props.get(OWNER_JOB_TITLE));
        // props.put(OWNER_ORG_STRUCT_UNIT, props.get(OWNER_ORG_STRUCT_UNIT));
        // props.put(OWNER_EMAIL, props.get(OWNER_EMAIL));
        // props.put(OWNER_PHONE, props.get(OWNER_PHONE));
         */
        // @formatter:on
    }

    private boolean setOwnerIfExists(Map<QName, Serializable> props, String ownerId, Mapping mapping) {
        if (personService.personExists(ownerId)) {
            getDocumentDynamicService().setOwner(props, ownerId, false, mapping.typeInfo.propDefs);

            Integer count = usersFound.get(ownerId);
            if (count == null) {
                count = 0;
            }
            count++;
            usersFound.put(ownerId, count);
            return true;
        }
        return false;
    }

    // @formatter:off
    /*
    private org.alfresco.util.Pair<String, String> processOwnerNameSim(String ppOwnerName) {
        String ownerName = StringUtils.strip(ppOwnerName);
        String ownerJobTitle = null;

        ownerName = StringUtils.replace(ownerName, " Upuhkab ", " (puhkab ");
        if (ownerName.endsWith(")")) {
            int i = ownerName.lastIndexOf("(");
            if (i < 0) { // Padrik Ene puhkab 02.07.-15.08. asendab Priidu Ristkok)
                i = ownerName.indexOf("puhkab") - 1;
            }
            Assert.isTrue(i >= 0, ppOwnerName);
            boolean cont = true;
            if (i - 7 >= 2 && ownerName.substring(i - 7, i).equals("puhkab ")) {
                ownerName = ownerName.substring(0, i - 7);

                if (ownerName.endsWith(")")) {
                    i = ownerName.lastIndexOf("(");
                    Assert.isTrue(i >= 0, ppOwnerName);
                } else {
                    cont = false;
                }
            }

            if (cont) {
                ownerJobTitle = StringUtils.trim(StringUtils.strip(ownerName.substring(i + 1, ownerName.length() - 1)));
                ownerName = StringUtils.trim(StringUtils.strip(ownerName.substring(0, i)));
                if (ownerJobTitle.startsWith("puhkab") || ownerJobTitle.indexOf("asendab ") >= 0) {
                    ownerJobTitle = "";
                } else {
                    while (ownerJobTitle.indexOf("  ") >= 0) {
                        ownerJobTitle = StringUtils.replace(ownerJobTitle, "  ", " ");
                    }
                }
            }
        }

        int i = ownerName.lastIndexOf(")");
        if (i >= 0) {
            Assert.isTrue(i >= 4 && i <= ownerName.length() - 3, ppOwnerName);
            int j = ownerName.lastIndexOf("(");
            Assert.isTrue(j < i - 1 && j >= 2, ppOwnerName);
            String removeString = ownerName.substring(j + 1, i).toLowerCase();
            if (removeString.indexOf("puhkab") >= 0 || removeString.indexOf("puhkusel") >= 0 || removeString.indexOf("asendab") >= 0
                    || removeString.equals("haiguslehel") || (removeString.indexOf("lapsehooldus") >= 0 && removeString.indexOf("puhkus") >= 0)
                    || removeString.equals("ei aktiveeri seda kontot, teha uus isik") || removeString.startsWith("05.07-")) {
                ownerName = StringUtils.trim(StringUtils.strip(ownerName.substring(0, j))) + " "
                        + StringUtils.trim(StringUtils.strip(ownerName.substring(i + 1)));
            }
            // else
            // Natalja Zinovjeva (peaspetsialist), Ene Padrik
            // Terje Enula (peaspetsialist), Enel Pungas
            // Peeter Küüts (nõunik), Mart Riisenberg
        }
        // Assert.isTrue(ownerName.indexOf("(") == -1 && ownerName.indexOf(")") == -1, ppOwnerName); // nende kolme ülemise näite puhul jääb ainult sisse sulud
        return new org.alfresco.util.Pair<String, String>(ownerName, ownerJobTitle);
    }
     */
    // @formatter:on

    private void setAccessRestriction(Map<QName, Serializable> props) {
        String accessRestriction = (String) props.get(ACCESS_RESTRICTION);
        Mapping accessRestrictionMapping = mappings.get("accessRestrictionValues");
        if (accessRestrictionMapping != null) {
            for (PropMapping propMapping : accessRestrictionMapping.props) {
                if (propMapping.from.equals(accessRestriction)) {
                    accessRestriction = propMapping.to;
                    break;
                }
            }
        }
        if (AccessRestriction.OPEN.getValueName().equals(accessRestriction) || AccessRestriction.AK.getValueName().equals(accessRestriction)
                || AccessRestriction.INTERNAL.getValueName().equals(accessRestriction) || AccessRestriction.LIMITED.getValueName().equals(accessRestriction)) {
            // OK
        } else {
            throw new RuntimeException("Invalid accessRestriction value '" + accessRestriction + "'");
        }
        props.put(ACCESS_RESTRICTION, accessRestriction);

        if (AccessRestriction.AK.getValueName().equals(accessRestriction) || AccessRestriction.LIMITED.getValueName().equals(accessRestriction)) {
            if (StringUtils.isBlank((String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON))) {
                props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, "AvTS § 35 lg 1 p 9");
            }
            if (props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE) == null) {
                props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, props.get(DocumentCommonModel.Props.REG_DATE_TIME));
            }
            if (props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE) == null) {
                throw new RuntimeException("accessRestrictionBeginDate is null, but accessRestriction = " + accessRestriction);
            }
            if (props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE) == null
                    && StringUtils.isBlank((String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC))
                    && props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE) != null) {
                Date beginDate = (Date) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE);
                Calendar cal = Calendar.getInstance();
                cal.setTime(beginDate);
                cal.add(Calendar.YEAR, 5);
                props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE, cal.getTime());
            }
            if (props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE) == null
                    && StringUtils.isBlank((String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC))) {
                throw new RuntimeException("accessRestrictionEndDate is null and accessRestrictionEndDesc is blank, but accessRestriction = " + accessRestriction);
            }
        }
    }

    private static String cleanUserFullName(String strippedOwnerName) {
        if (strippedOwnerName == null) {
            return "";
        }
        strippedOwnerName = StringUtils.deleteWhitespace(strippedOwnerName.toLowerCase());
        for (char c : new char[] { '-', '/', '.', ',', ';', '"', '&', '@', '<', '>' }) {
            strippedOwnerName = StringUtils.remove(strippedOwnerName, c);
        }
        return StringUtils.trim(strippedOwnerName);
    }

    private void checkStop() {
        postipoissImporter.checkStop();
    }

    private abstract class BatchProgress<E> {
        Collection<E> origin;
        List<E> batchList;
        int totalSize;
        int i;
        int completedSize;
        int thisRunCompletedSize;
        long thisRunStartTime;
        long startTime;
        String processName;

        private void init() {
            totalSize = origin.size();
            thisRunStartTime = System.currentTimeMillis();
            startTime = thisRunStartTime;
            i = 0;
            completedSize = 0;
            batchList = new ArrayList<E>(batchSize);
        }

        abstract void executeBatch() throws Exception;

        void executeInTransaction() {
            try {
                getTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>() {
                    @Override
                    public Object execute() throws Throwable {
                        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
                        executeBatch();
                        return null;
                    }
                });
            } catch (Exception e) {
                log.error("Import batch of documents failed, transaction rolled back, continuing with next batch", e);
            }
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
            String info = "%s: %6.2f%% completed - %7d of %7d, %5.1f items per second (last), %5.1f (total), ETA %s";
            log.info(String.format(info, processName, completedPercent, completedSize, totalSize, lastDocsPerSec, totalDocsPerSec, eta));
            startTime = endTime;
        }

        public void run() {
            init();
            checkStop();
            for (E e : origin) {
                batchList.add(e);
                i++;
                if (i >= batchSize) {
                    step();
                    checkStop();
                }
            }
            step();
        }

    }

    private class FilesBatchProgress extends BatchProgress<Integer> {

        public FilesBatchProgress(Set<Integer> origin) {
            this.origin = origin;
            processName = "Files import";
        }

        @Override
        void executeBatch() {
            createFilesBatch(batchList);
        }
    }

    private void importFiles() {
        if (filesToProceed.isEmpty()) {
            log.info("Skipping files import, no files found");
            return;
        }
        log.info("Starting files import. Total = " + filesToProceed.size());
        FilesBatchProgress batchProgress = new FilesBatchProgress(filesToProceed.descendingSet());
        batchProgress.run();
        log.info("Files IMPORT COMPLETE :)");
    }

    protected void createFilesBatch(final List<Integer> batchList) {
        final Set<Integer> batchCompletedFiles = new HashSet<Integer>(batchSize);
        for (Integer documentId : batchList) {
            if (log.isTraceEnabled()) {
                log.trace("Processing files with docId = " + documentId);
            }
            NodeRef documentRef = completedDocumentsMap.get(documentId);
            if (documentRef == null) {
                continue;
            }
            if (!nodeService.exists(documentRef)) {
                log.error("Skipping creating files for documentId=" + documentId + ", node does not exist: " + documentRef);
                continue;
            }
            try {
                addFiles(documentId, documentRef);
            } catch (Exception e) {
                throw new RuntimeException("Error adding files for document id=" + documentId + ", nodeRef=" + documentRef + ": " + e.getMessage(), e);
            }
            batchCompletedFiles.add(documentId);
        }
        bindCsvWriteAfterCommit(completedFilesFile, new CsvWriterClosure() {

            @Override
            public void execute(CsvWriter writer) throws IOException {
                for (Integer documentId : batchCompletedFiles) {
                    writer.writeRecord(new String[] {
                            documentId.toString()
                    });
                    completedFiles.add(documentId);
                }
            }

            @Override
            public String[] getHeaders() {
                return new String[] { "documentId" };
            }
        });
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

    protected void loadCompletedDocuments() throws Exception {
        failedDocumentsFile = new File(workFolder, "failed_docs.csv");
        OutputStream outputStream = new FileOutputStream(failedDocumentsFile);
        try {
            // the Unicode value for UTF-8 BOM, is needed so that Excel would recognise the file in correct encoding
            outputStream.write("\ufeff".getBytes("UTF-8"));
        } finally {
            outputStream.close();
        }
        CsvWriter writer = new CsvWriter(new FileWriter(failedDocumentsFile, true), CSV_SEPARATOR);
        try {
            writer.writeRecord(new String[] {
                    "documentFileName",
                    "errorMsg"
            });
        } finally {
            writer.close();
        }

        completedDocumentsMap = new TreeMap<Integer, NodeRef>();

        completedDocumentsFile = new File(workFolder, "completed_docs.csv");
        if (!completedDocumentsFile.exists()) {
            log.info("Skipping loading previously completed documentId-s, file does not exist: " + completedDocumentsFile);
            return;
        }

        log.info("Loading previously completed documentId-s from file " + completedDocumentsFile);

        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(completedDocumentsFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                Integer documentId = new Integer(reader.get(0));
                String nodeRefString = reader.get(1);
                if (StringUtils.isNotBlank(nodeRefString)) {
                    NodeRef documentRef = new NodeRef(nodeRefString);
                    completedDocumentsMap.put(documentId, documentRef);
                }
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + completedDocumentsMap.size() + " previously completed documentId-s");

        int previousSize = documentsMap.size();
        documentsMap.keySet().removeAll(completedDocumentsMap.keySet());
        /*
         * Assert.isTrue(previousSize - documentsMap.size() == completedDocumentsMap.size(), previousSize + " " + documentsMap.size() + " "
         * + completedDocumentsMap.size());
         */
        log.info("Removed previously completed documentId-s from current document list: " + previousSize + " -> " + documentsMap.size());
    }

    private static class Toimik {
        String normedMark;
        NodeRef nodeRef;
        int year;

        @Override
        public String toString() {
            return "Toimik [nodeRef=" + nodeRef + ", normedMark=" + normedMark + ", year=" + year + "]";
        }
    }

    private Map<Integer, Map<String, Toimik>> toimikud;
    private Map<Integer, Map<String, Toimik>> normedToimikud;

    private Toimik getToimik(int year, String mark, boolean normed) {
        Map<Integer, Map<String, Toimik>> bmap = normed ? normedToimikud : toimikud;
        Map<String, Toimik> map = bmap.get(year);
        if (map == null) {
            return null;
        }
        return map.get(mark);
    }

    private void putToimik(String mark, Toimik t, boolean normed) {
        Map<Integer, Map<String, Toimik>> bmap = normed ? normedToimikud : toimikud;
        Map<String, Toimik> map = bmap.get(t.year);
        if (map == null) {
            map = new HashMap<String, Toimik>();
            bmap.put(t.year, map);
        }
        map.put(mark, t);
    }

    private void loadToimiks() throws IOException {
        toimikud = new HashMap<Integer, Map<String, Toimik>>();
        normedToimikud = new HashMap<Integer, Map<String, Toimik>>();
        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(new File(workFolder, "completed_toimikud.csv"))), CSV_SEPARATOR,
                Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                int year = Integer.parseInt(reader.get(0));
                String normedMark = reader.get(1);
                String mark = reader.get(2);
                Toimik toimik = getToimik(year, normedMark, true);
                if (toimik == null) {
                    toimik = new Toimik();
                    toimik.year = year;
                    toimik.normedMark = normedMark;
                    toimik.nodeRef = new NodeRef(reader.get(3));
                    putToimik(normedMark, toimik, true);
                }
                putToimik(mark, toimik, false);
            }
        } finally {
            reader.close();
        }
    }

    private class DocumensBatchProgress extends BatchProgress<Entry<Integer, File>> {
        {
            origin = documentsMap.entrySet();
            processName = "Documents import";
        }

        @Override
        void executeBatch() throws Exception {
            createDocumentsBatch(batchList);
        }
    }

    private void createDocuments() throws Exception {
        if (documentsMap.isEmpty()) {
            log.info("Skipping documents import, no documents found");
            return;
        }
        log.info("Starting documents import. Total = " + documentsMap.size());
        DocumensBatchProgress batchProgress = new DocumensBatchProgress();
        try {
            batchProgress.run();
        } finally {
            getDocumentListService().updateDocCounters(archivalsRoot);
        }
        log.info("Documents IMPORT COMPLETE :)");
    }

    static class ImportedDocument {
        Integer documentId;
        NodeRef nodeRef;
        String toimik;
        String registreerimisNr;
        String regNumber;
        String regDateTime;
        String docName;
        String ownerId;
        String ownerName;
        String accessRestriction;
        String accessRestrictionReason;
        String dokLiik;
        String suund;
        String documentTypeId;

        public ImportedDocument(Integer documentId, NodeRef nodeRef, String toimik, String registreerimisNr, String dokLiik, String suund, String documentTypeId,
                                Map<QName, Serializable> props) {
            this.documentId = documentId;
            this.nodeRef = nodeRef;
            this.toimik = toimik;
            this.registreerimisNr = registreerimisNr;
            this.dokLiik = dokLiik;
            this.suund = suund;
            this.documentTypeId = documentTypeId;
            if (props != null) {
                regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
                regDateTime = staticDateTimeFormat.format((Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME));
                docName = (String) props.get(DocumentCommonModel.Props.DOC_NAME);
                ownerId = (String) props.get(DocumentCommonModel.Props.OWNER_ID);
                ownerName = (String) props.get(DocumentCommonModel.Props.OWNER_NAME);
                accessRestriction = (String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
                accessRestrictionReason = (String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON);
            }
        }
    }

    protected void createDocumentsBatch(final List<Entry<Integer, File>> batchList) throws Exception {
        final Map<Integer, ImportedDocument> batchCompletedDocumentsMap = new TreeMap<Integer, ImportedDocument>();
        // FOR ASSOCS
        // this.batchCompletedDocumentsMap = batchCompletedDocumentsMap;
        for (Entry<Integer, File> entry : batchList) {
            Integer documentId = entry.getKey();
            if (log.isTraceEnabled()) {
                log.trace("Processing documentId=" + documentId);
            }
            File file = entry.getValue();
            try {
                ImportedDocument doc = createDocument(documentId, file);
                batchCompletedDocumentsMap.put(documentId, doc);
                completedDocumentsMap.put(documentId, doc.nodeRef); // Add immediately to completedDocumentsMap, because other code wants to access it
            } catch (Exception e) {
                CsvWriter writer = new CsvWriter(new FileWriter(failedDocumentsFile, true), CSV_SEPARATOR);
                try {
                    writer.writeRecord(new String[] { file.getName(), e.getMessage() });
                } finally {
                    writer.close();
                }
                throw new RuntimeException("Error importing document " + file.getName() + ": " + e.getMessage(), e);
            }
        }
        AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
            @Override
            public void afterCommit() {
                postponedAssocsCommited = new TreeMap<Integer, List<PostponedAssoc>>(postponedAssocs);
                usersFoundCommited = new HashMap<String, Integer>(usersFound);
                usersNotFoundCommited = new HashMap<String, Integer>(usersNotFound);
            }

            @Override
            public void afterRollback() {
                postponedAssocs = new TreeMap<Integer, List<PostponedAssoc>>(postponedAssocsCommited);
                usersFound = new HashMap<String, Integer>(usersFoundCommited);
                usersNotFound = new HashMap<String, Integer>(usersNotFoundCommited);
                completedDocumentsMap.keySet().removeAll(batchCompletedDocumentsMap.keySet());
            }
        });
        bindCsvWriteAfterCommit(completedDocumentsFile, new CsvWriterClosure() {

            @Override
            public String[] getHeaders() {
                return new String[] {
                        "documentId",
                        "nodeRef",
                        "ppToimik",
                        "ppRegistreerimisNr",
                        "regNumber",
                        "regDateTime",
                        "docName",
                        "ownerId",
                        "ownerName",
                        "accessRestriction",
                        "accessRestrictionReason",
                        "ppDokLiik",
                        "ppSuund",
                        "documentTypeId"
                };
            }

            @Override
            public void execute(CsvWriter writer) throws IOException {
                for (Entry<Integer, ImportedDocument> entry : batchCompletedDocumentsMap.entrySet()) {
                    ImportedDocument doc = entry.getValue();
                    writer.writeRecord(new String[] {
                            doc.documentId.toString(),
                            doc.nodeRef == null ? "" : doc.nodeRef.toString(),
                            doc.toimik == null ? "" : doc.toimik,
                            doc.registreerimisNr == null ? "" : doc.registreerimisNr,
                            doc.regNumber == null ? "" : doc.regNumber,
                            doc.regDateTime == null ? "" : doc.regDateTime,
                            doc.docName == null ? "" : doc.docName,
                            doc.ownerId == null ? "" : doc.ownerId,
                            doc.ownerName == null ? "" : doc.ownerName,
                            doc.accessRestriction == null ? "" : doc.accessRestriction,
                            doc.accessRestrictionReason == null ? "" : doc.accessRestrictionReason,
                            doc.dokLiik == null ? "" : doc.dokLiik,
                            doc.suund == null ? "" : doc.suund,
                            doc.documentTypeId == null ? "" : doc.documentTypeId
                    });
                }
            }
        });
    }

    private ImportedDocument createDocument(Integer documentId, File file) throws DocumentException, ParseException {
        // Node importDoc = ;
        return importDoc(file, mappings, documentId); // new ImportedDocument(documentId, importDoc);
    }

    private void addFiles(Integer documentId, NodeRef importDocRef) {
        List<File> fileList = filesMap.get(documentId);
        if (fileList != null) {
            for (File file : fileList) {
                String fileName = file.getName();
                int i = fileName.lastIndexOf('.');
                fileName = fileName.substring(0, i);
                addFile(fileName, file, importDocRef, null);
            }
        }
    }

    private Map<QName, Serializable> mapProperties(Element root, Mapping mapping) {
        DocumentValue docValue = new DocumentValue(mapping.typeInfo);

        for (PropMapping pm : mapping.props) {
            try {
                if (pm.to.startsWith("_")) {
                    continue;
                }
                String value = null;
                if (pm.expression == null) {
                    Element el = root.element(pm.from);
                    if (el != null) {
                        value = el.getStringValue();
                    }
                } else {
                    if (pm.expression.equals("xpath")) {
                        value = PostipoissUtil.findAnyValue(root, pm.from);
                    } else if (pm.expression.equals("const")) {
                        value = pm.from;
                    } else {
                        throw new RuntimeException("Bad expression type " + pm.expression);
                    }
                }
                if (value != null) {
                    if (pm.splitter == null) {
                        docValue.put(value, pm.to, pm.prefix);
                    } else {
                        try {
                            Pair pair = pm.splitter.split(value);
                            docValue.putObject(pair.first, pm.toFirst);
                            docValue.putObject(pair.second, pm.toSecond);
                        } catch (ConvertException e) {
                            if (pm.to != null) {
                                docValue.put(value, pm.to, pm.prefix);
                            }
                        }
                    }
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("PropMapping" + pm + ": " + e.getMessage(), e);
            }
        }

        HashMap<QName, Serializable> propsMap = new HashMap<QName, Serializable>();

        for (PropertyValue propertyValue : docValue.props.values()) {
            propsMap.put(propertyValue.qname, propertyValue.get());

        }

        return propsMap;
    }

    private static class VolumeIndex {
        String mark;
        String regNumber;
        int year;

        @Override
        public String toString() {
            return "VolumeIndex [mark=" + mark + ", regNumber=" + regNumber + ", year=" + year + "]";
        }
    }

    private VolumeIndex inferVolumeIndex(Element root, Mapping m) {
        VolumeIndex vi = new VolumeIndex();
        String toimikSari = root.elementText(m.requirePropMappingTo(MAPPINGS_PROP_TO_REG_NUMBER_WITHOUT_INDIVIDUAL).from);
        vi.regNumber = root.elementText(m.requirePropMappingTo(MAPPINGS_PROP_TO_SHORT_REG_NUMBER).from);

        if (toimikSari != null) {
            toimikSari = toimikSari.trim();
        }

        if (StringUtils.isBlank(toimikSari)) {
            if (m.defaultVolume != null) {
                vi.mark = m.defaultVolume;
                vi.year = 10;
            } else {
                return null;
            }
        } else {
            int i = toimikSari.lastIndexOf("/");
            if (i == -1) {
                return null;
            }
            String rn2 = toimikSari.substring(i + 1);
            if (vi.regNumber == null) {
                vi.regNumber = rn2;
            }
            if (StringUtils.isBlank(vi.regNumber)) {
                return null;
            }

            String rest = toimikSari.substring(0, i);
            if (rest.length() > 4 && rest.matches(".*/\\d\\d")) {
                vi.year = Integer.parseInt(rest.substring(rest.length() - 2));
                rest = rest.substring(0, rest.length() - 3);
            } else {
                String regKpvElementName = m.requirePropMappingTo(DocumentCommonModel.Props.REG_DATE_TIME.getLocalName()).from;
                String regKpv = root.elementText(regKpvElementName);
                vi.year = inferYearFromRegKpv(regKpv);
            }

            if (StringUtils.isBlank(rest)) {
                return null;
            }
            vi.mark = rest.trim();
        }
        return vi;
    }

    private int inferYearFromRegKpv(String regKpv) {
        if (regKpv == null) {
            return LAST_VOLUME_YEAR;
        }
        try {
            Date regDate = dateFormat.parse(regKpv);
            int year = PostipoissUtil.getYear(regDate) - 2000;
            if (year < 0 || year > LAST_VOLUME_YEAR) {
                return LAST_VOLUME_YEAR;
            }
            return year;
        } catch (ParseException e) {
            return LAST_VOLUME_YEAR;
        }
    }

    // @formatter:off
    /*
    private boolean isSissetulevKiriOpen(String regKpv) {
        if (regKpv != null) {
            try {
                Date date = dateFormat.parse(regKpv);
                if (!date.before(openDocsDate)) {
                    return true;
                }
            } catch (ParseException e) {
            }
        }
        return false;
    }
     */
    // @formatter:on

    private ImportedDocument importDoc(File xml, Map<String, Mapping> mappings, Integer documentId) throws DocumentException,
            ParseException {

        Element root = xmlReader.read(xml).getRootElement();

        Mapping generalMapping = mappings.get("general");
        String ppDocumentTypeFrom = generalMapping.requirePropMappingTo(MAPPINGS_PROP_TO_DOCUMENT_TYPE_FROM).from;
        String ppRegNumberWithoutIndividual = generalMapping.requirePropMappingTo(MAPPINGS_PROP_TO_REG_NUMBER_WITHOUT_INDIVIDUAL).from;
        String ppShortRegNumber = generalMapping.requirePropMappingTo(MAPPINGS_PROP_TO_SHORT_REG_NUMBER).from;

        String type = root.elementText(ppDocumentTypeFrom);
        String suund = root.elementText(PP_ELEMENT_SUUND);
        String toimikSari = root.elementText(ppRegNumberWithoutIndividual);
        Mapping mapping = null;
        if (StringUtils.isNotBlank(suund)) {
            mapping = mappings.get(type + "-" + suund);
        }
        if (mapping == null && StringUtils.isNotBlank(toimikSari)) {
            int i = toimikSari.indexOf('/');
            if (i >= 0) {
                toimikSari = toimikSari.substring(0, i);
            }
            mapping = mappings.get(type + "-" + toimikSari);
        }
        if (mapping == null) {
            mapping = mappings.get(type);
        }
        if (mapping == null) {
            // Skip document for which mapping doesn't exist
            return new ImportedDocument(documentId, null, root.elementText(ppRegNumberWithoutIndividual),
                    root.elementText(ppShortRegNumber),
                    root.elementText(ppDocumentTypeFrom),
                    root.elementText(PP_ELEMENT_SUUND), "documentType mapping not found", null);
        }
        VolumeIndex volumeIndex = inferVolumeIndex(root, mapping);
        if (volumeIndex == null) {
            throw new RuntimeException("Could not parse volume for document, volume='"
                    + root.elementText(ppRegNumberWithoutIndividual) + "'");
        }
        String volumeIndexMarkOrig = volumeIndex.mark;

        Toimik t = getToimik(volumeIndex.year, volumeIndex.mark, false);
        if (t == null) {
            log.debug("Could not load toimik by mark for " + volumeIndex);
            t = getToimik(volumeIndex.year, volumeIndex.mark, true);
        }
        if (t == null) {
            log.debug("Could not load toimik by normed mark for " + volumeIndex);
            volumeIndex.mark = StringUtils.deleteWhitespace(volumeIndex.mark);
            log.debug("Cleaned mark to " + volumeIndex.mark);
            t = getToimik(volumeIndex.year, volumeIndex.mark, false);
        }
        if (t == null) {
            log.debug("Could not load toimik by mark for " + volumeIndex);
            t = getToimik(volumeIndex.year, volumeIndex.mark, true);
        }
        if (t == null) {
            throw new RuntimeException("Could not find volume for document, searched based on calculated year=" + volumeIndex.year + " and mark='" + volumeIndexMarkOrig + "'");
        }

        Map<QName, Serializable> propsMap = setProps(root, mapping, volumeIndex, t);

        NodeRef parentRef = t.nodeRef;
        DocumentDynamic doc = getDocumentDynamicService().createNewDocument(mapping.typeInfo.docVer, parentRef, false).getFirst();
        NodeRef documentRef = doc.getNodeRef();
        nodeService.addAspect(documentRef, DocumentCommonModel.Aspects.SEARCHABLE, null);

        Map<NodeRef, Map<QName, NodeRef>> parentRefsByVolumeRef = new HashMap<NodeRef, Map<QName, NodeRef>>();
        Map<QName, NodeRef> parentRefs = parentRefsByVolumeRef.get(parentRef);
        if (parentRefs == null) {
            parentRefs = documentService.getDocumentParents(documentRef);
            parentRefsByVolumeRef.put(parentRef, parentRefs);
        }
        propsMap.putAll(parentRefs);

        String recipient = getRecipient(type, propsMap); // call before checkProps, because checkProps transforms multi-valued values to Lists-s

        checkProps(propsMap, null, mapping.typeInfo.propDefs);

        mapChildren(root, mapping, doc.getNode(), new QName[] {}, mapping.typeInfo.propDefs);

        addHistoryItems(documentRef, root, propsMap, mapping);

        doc.getNode().getProperties().putAll(RepoUtil.toStringProperties(propsMap));
        doc.getNode().getProperties().put(DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED.toString(), Boolean.TRUE);
        getDocumentDynamicService().updateDocument(doc, Arrays.asList("postipoissImporter"), false);

        // Add sendInfo
        if (recipient != null) {
            Map<QName, Serializable> sendInfoProps = mapProperties(root, mappings.get("sendInfo"));
            sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_RECIPIENT, recipient);
            String sendMode = (String) sendInfoProps.get(DocumentCommonModel.Props.SEND_INFO_SEND_MODE);
            if (StringUtils.isBlank(sendMode)) {
                sendMode = "määramata";
            } else if (sendMode.equals("email")) {
                sendMode = SendMode.EMAIL.getValueName();
            } else if (sendMode.equals("Elektroonselt (DVK)")) {
                sendMode = SendMode.DVK.getValueName();
            } else if (sendMode.equals("kullerpost")) {
                sendMode = SendMode.REGISTERED_MAIL.getValueName();
            }
            // else: "post", "faks" - left as is
            sendInfoProps.put(DocumentCommonModel.Props.SEND_INFO_SEND_MODE, sendMode);
            sendOutService.addSendinfo(documentRef, sendInfoProps);
        }

        addAssociations(documentRef, documentId, root);

        return new ImportedDocument(documentId, documentRef, root.elementText(ppRegNumberWithoutIndividual),
                root.elementText(ppShortRegNumber), root.elementText(ppDocumentTypeFrom),
                root.elementText(PP_ELEMENT_SUUND), mapping.typeInfo.docVer.getParent().getId(), nodeService.getProperties(documentRef));
    }

    private void checkProps(Map<QName, Serializable> propsMap, QName[] hierarchy, Map<String, org.alfresco.util.Pair<DynamicPropertyDefinition, Field>> propDefs) {
        List<String> errors = new ArrayList<String>();
        for (Entry<QName, Serializable> entry : propsMap.entrySet()) {
            String propNameString = entry.getKey().toPrefixString(getNamespaceService());
            if (!DocumentDynamicModel.URI.equals(entry.getKey().getNamespaceURI())) {
                errors.add("Property " + propNameString + " has wrong namespace");
                continue;
            }
            org.alfresco.util.Pair<DynamicPropertyDefinition, Field> pair = propDefs.get(entry.getKey().getLocalName());
            if (pair == null) {
                errors.add("Property " + propNameString + " is missing propertyDefinition");
                continue;
            }

            if (hierarchy == null) {
                hierarchy = new QName[] {};
            }
            DynamicPropertyDefinition propDef = pair.getFirst();
            QName[] propHierarchy = propDef.getChildAssocTypeQNameHierarchy();
            if (propHierarchy == null) {
                propHierarchy = new QName[] {};
            }
            if (!Arrays.equals(hierarchy, propHierarchy)) {
                errors.add("Property " + propNameString + " should be on hierarchy " + Arrays.asList(propHierarchy) + " but is on hierarchy " + Arrays.asList(hierarchy));
                continue;
            }

            // PostipoissDocumentsMapper doesn't know about single- and multi-valued properties, so handle it here
            Serializable value = entry.getValue();
            if (propDef.isMultiValued() && value != null && !(value instanceof List)) {
                ArrayList<Serializable> list = new ArrayList<Serializable>();
                list.add(value);
                value = list;
                entry.setValue(value);
            }

            if (value == null) {
                continue;
            }
            String requiredClassName = propDef.getDataType().getJavaClassName();
            if (value instanceof List<?>) {
                if (!propDef.isMultiValued()) {
                    errors.add("Property " + propNameString + " should not be a List, but is a List");
                    continue;
                }
                List<Serializable> list = (List<Serializable>) value;
                int i = 0;
                for (Serializable listElement : list) {
                    if (listElement == null) {
                        i++;
                        continue;
                    }
                    String realClassName = listElement.getClass().getName();
                    if (!realClassName.equals(requiredClassName)) {
                        errors.add("Property " + propNameString + " value at index " + i + " should be of type " + requiredClassName + " but is of type " + realClassName + " : "
                                + listElement);
                        continue;
                    }
                    i++;
                }
            } else {
                if (propDef.isMultiValued()) {
                    errors.add("Property " + propNameString + " should be a List, but is not a List");
                    continue;
                }
                String realClassName = value.getClass().getName();
                if (!realClassName.equals(requiredClassName)) {
                    errors.add("Property " + propNameString + " value should be of type " + requiredClassName + " but is of type " + realClassName + " : " + value);
                    continue;
                }
            }
        }
        if (!errors.isEmpty()) {
            log.info("props=" + WmNode.toString(propsMap, getNamespaceService()));
            throw new RuntimeException("Found " + errors.size() + " errors:\n  * " + StringUtils.join(errors, "\n  * "));
        }
    }

    private void mapChildren(Element root, Mapping mapping, Node node, QName[] parentHierarchy,
            Map<String, org.alfresco.util.Pair<DynamicPropertyDefinition, Field>> propDefs) {
        Map<QName, Integer> childNodesProcessed = new HashMap<QName, Integer>();
        for (Mapping subMapping : mapping.subMappings) {
            QName childAssocType = subMapping.to;
            List<Node> childNodes = node.getAllChildAssociations(childAssocType);
            QName[] hierarchy = (QName[]) ArrayUtils.add(parentHierarchy, childAssocType);
            if (childNodes == null || childNodes.isEmpty()) {
                throw new RuntimeException("No child nodes exist of type " + childAssocType.toPrefixString(getNamespaceService())
                        + " on node " + node.getType().toPrefixString(getNamespaceService())
                        + ", hierarchy=" + Arrays.asList(hierarchy)
                        + ", objectTypeIdAndVersion=" + DocAdminUtil.getDocTypeIdAndVersionNr(node));
            }
            Integer index = childNodesProcessed.get(childAssocType);
            if (index == null) {
                index = 0;
            }
            if (index >= childNodes.size()) {
                getDocumentDynamicService().createChildNodesHierarchyAndSetDefaultPropertyValues(node, hierarchy, mapping.typeInfo.docVer);
            }
            Node childNode = childNodes.get(index);
            childNodesProcessed.put(childAssocType, index + 1);

            Map<QName, Serializable> propsMap = mapProperties(root, subMapping);
            checkProps(propsMap, hierarchy, propDefs);

            childNode.getProperties().putAll(RepoUtil.toStringProperties(propsMap));

            mapChildren(root, subMapping, childNode, hierarchy, propDefs);
        }
    }

    private Map<QName, Serializable> setProps(Element root, Mapping mapping, VolumeIndex volumeIndex, Toimik t) {
        // SIM, MV constructed regNumber like this: String regNumber = t.normedMark + "/" + volumeIndex.regNumber;
        // PPA needs regNumber to be original, unmodified
        String regNumber = root.elementText(mapping.requirePropMappingTo(MAPPINGS_PROP_TO_REG_NUMBER_WITHOUT_INDIVIDUAL).from);
        String individualNr = root.elementText(mapping.requirePropMappingTo(MAPPINGS_PROP_TO_INDIVIDUAL_NUMBER).from);
        if (StringUtils.isNotBlank(individualNr)) {
            regNumber += "-" + individualNr;
        }

        Map<QName, Serializable> propsMap = mapProperties(root, mapping);

        boolean open;
        if (StringUtils.isNotBlank(root.elementText(mapping.requirePropMappingTo(MAPPINGS_PROP_TO_SHORT_REG_NUMBER).from))) {
            propsMap.put(DocumentCommonModel.Props.REG_NUMBER, regNumber);
            RegNrHolder regNrHolder = new RegNrHolder(regNumber);
            propsMap.put(DocumentCommonModel.Props.SHORT_REG_NUMBER, regNrHolder.getShortRegNrWithoutIndividualizingNr());
            propsMap.put(DocumentCommonModel.Props.INDIVIDUAL_NUMBER, regNrHolder.getIndividualizingNr() == null ? null : regNrHolder.getIndividualizingNr().toString());
            Assert.notNull(propsMap.get(DocumentCommonModel.Props.REG_DATE_TIME), "regDateTime must not be null");
            Assert.hasText((String) propsMap.get(DocumentCommonModel.Props.REG_NUMBER), "regNumber must not be empty");
            open = false;
        } else {
            propsMap.remove(DocumentCommonModel.Props.REG_NUMBER);
            propsMap.remove(DocumentCommonModel.Props.SHORT_REG_NUMBER);
            propsMap.remove(DocumentCommonModel.Props.INDIVIDUAL_NUMBER);
            propsMap.remove(DocumentCommonModel.Props.REG_DATE_TIME);
            open = true;
        }

        propsMap.put(DocumentCommonModel.Props.DOC_STATUS, open ? DocumentStatus.WORKING.getValueName() : DocumentStatus.FINISHED.getValueName());

        setAccessRestriction(propsMap);

        // log.info("ppOwnerName=" + PostipoissUtil.findAnyValue(root, "/dokument/tegevused/tegevus[tegevus_liik=1]/kes"));
        // log.info("ownerName=" + (String) propsMap.get(OWNER_NAME));
        // log.info("ownerId=" + (String) propsMap.get(OWNER_NAME));
        // String ownerName = (String) propsMap.get(DocumentCommonModel.Props.OWNER_NAME);
        // if (StringUtils.isBlank(ownerName)) {
        // SIM and MV used additional rule:
        // ownerName = PostipoissUtil.findAnyValue(root, "/dokument/tegevused/tegevus[tegevus_liik=1]/kes"); // SIM used document and kellelt_tekst
        // SIM used additional rule:
        // if (StringUtils.isBlank(ownerName)) {
        // ownerName = (String) propsMap.get(DocumentCommonModel.Props.SIGNER_NAME);
        // }
        // if (StringUtils.isBlank(ownerName)) {
        // ownerName = IMPORTER_NAME;
        // }
        // propsMap.put(DocumentCommonModel.Props.OWNER_NAME, ownerName);
        // }
        setOwnerProperties(propsMap, mapping);
        // log.info("ownerName=" + (String) propsMap.get(OWNER_NAME));
        // log.info("ownerId=" + (String) propsMap.get(OWNER_NAME));

        if (!propsMap.containsKey(DocumentCommonModel.Props.DOC_NAME)) {
            String docName = root.elementText(mapping.requirePropMappingTo(MAPPINGS_PROP_TO_DOCUMENT_TYPE_FROM).from);
            if (StringUtils.isNotBlank(docName)) {
                propsMap.put(DocumentCommonModel.Props.DOC_NAME, docName);
            }
        }

        Assert.hasText((String) propsMap.get(DocumentCommonModel.Props.DOC_NAME), "docName cannot be blank");

        setStorageType(root, propsMap);

        return propsMap;
    }

    private void setStorageType(Element root, Map<QName, Serializable> propsMap) {
        String storageType = StringUtils.stripToEmpty((String) propsMap.get(DocumentCommonModel.Props.STORAGE_TYPE));
        if ("Digitaaldokument".equalsIgnoreCase(storageType)) {
            propsMap.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.DIGITAL.getValueName());
        } else if ("Paberdokument".equalsIgnoreCase(storageType)) {
            propsMap.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.PAPER.getValueName());
        } else if (StringUtils.isBlank(storageType)) {
            StorageType storageTypeEnum = StorageType.PAPER;
            Element el = root.element(PP_ELEMENT_FAIL);
            if (el != null && StringUtils.isNotBlank(el.getStringValue())) {
                storageTypeEnum = StorageType.DIGITAL;
            }
            propsMap.put(DocumentCommonModel.Props.STORAGE_TYPE, storageTypeEnum.getValueName());
        }
    }

    private String getRecipient(String type, Map<QName, Serializable> propsMap) {
        if (propsMap != null) {
            // if ("Kiri-sissetulev".equals(type))
            // return null;
            String recipientName = (String) propsMap.get(DocumentCommonModel.Props.RECIPIENT_NAME);
            String additionalRecipientName = (String) propsMap.get(DocumentCommonModel.Props.ADDITIONAL_RECIPIENT_NAME);
            if (StringUtils.isBlank(recipientName) && StringUtils.isBlank(additionalRecipientName)) {
                return null;
            }
            return PostipoissDocumentsMapper.join(", ", recipientName, additionalRecipientName);
        }
        return null;
    }

    private NodeRef findCompletedDoc(Integer id) {
        NodeRef res = completedDocumentsMap.get(id);

        // if (res == null) {
        // ImportedDocument importedDocument = batchCompletedDocumentsMap.get(id);
        // if (importedDocument != null) {
        // res = importedDocument.nodeRef;
        // }
        // }
        return res;
    }

    // private Map<Integer, ImportedDocument> batchCompletedDocumentsMap;

    protected void addHistoryItems(NodeRef docRef, Element root, Map<QName, Serializable> docProps, Mapping mapping) throws ParseException {
        Element tegevused = root.element("tegevused");
        if (tegevused == null) {
            return;
        }
        boolean responsibleActiveSet = false;
        NodeRef firstTaskRef = null;

        for (Object o : root.element("tegevused").elements()) {
            Element tegevus = (Element) o;
            Date kpv = null;
            String kuupaev = tegevus.elementText("kuupaev");
            if (StringUtils.isBlank(kuupaev)) {
                kpv = (Date) docProps.get(DocumentCommonModel.Props.REG_DATE_TIME);
                Assert.notNull(kpv, "tegevus kuupaev is null and document regDateTime is also null");
            } else {
                String kellaaeg = tegevus.elementText("kellaaeg");
                String dateString = String.format("%s %s", kuupaev, kellaaeg);
                try {
                    kpv = dateTimeFormat.parse(dateString);
                } catch (ParseException e) {
                    throw new RuntimeException("Unable to parse kuupaev kellaaeg: " + e.getMessage(), e);
                }
            }
            String nimetus = tegevus.elementText("nimetus");
            String resolutsioon = tegevus.elementText("resolutsioon");
            if (nimetus.startsWith("Dokumendi eksportimine") && resolutsioon.startsWith("Auto.eksport")) {
                // PPA needs to retain "Dokumendi eksportimine" log messages, which are NOT "Auto.eksport"
                continue;
            }
            String kes = tegevus.elementText("kes");
            if (StringUtils.isBlank(kes)) {
                kes = tegevus.elementText("kellelt_tekst");
            }
            String kelleleTekst = tegevus.elementText("kellele_tekst");
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(DocumentCommonModel.Props.CREATED_DATETIME, kpv);
            props.put(DocumentCommonModel.Props.CREATOR_NAME, kes);
            props.put(DocumentCommonModel.Props.EVENT_DESCRIPTION, PostipoissDocumentsMapper.join("; ", nimetus, kelleleTekst, resolutsioon));
            nodeService.createNode(docRef, DocumentCommonModel.Types.DOCUMENT_LOG, DocumentCommonModel.Types.DOCUMENT_LOG,
                            DocumentCommonModel.Types.DOCUMENT_LOG, props);

            if (!"edastamine täitmiseks".equals(nimetus)) {
                continue;
            }
            Element kellelt = tegevus.element("kellelt");
            String kelleltEnimi = kellelt.elementText("enimi");
            String kelleltPnimi = kellelt.elementText("pnimi");
            String kelleltIkood = kellelt.elementText("ikood");
            String kelleltEmail = kellelt.elementText("email");
            if (StringUtils.isBlank(kelleltEnimi) || StringUtils.isBlank(kelleltPnimi) || StringUtils.isBlank(kelleltIkood) || StringUtils.isBlank(kelleltEmail)) {
                continue;
            }

            Date dateTime = StringUtils.isBlank(kuupaev) ? new Date() : kpv;
            NodeRef wfRef = null;

            for (Element kellele : (List<Element>) tegevus.elements("kellele")) {
                String kelleleEnimi = kellele.elementText("enimi");
                String kellelePnimi = kellele.elementText("pnimi");
                String kelleleIkood = kellele.elementText("ikood");
                String kelleleEmail = kellele.elementText("email");
                String kelleleTahtaeg = kellele.elementText("tahtaeg");
                String kelleleTehtud = kellele.elementText("tehtud");

                if (StringUtils.isBlank(kelleleEnimi) || StringUtils.isBlank(kellelePnimi) || StringUtils.isBlank(kelleleIkood) || StringUtils.isBlank(kelleleEmail)
                            || StringUtils.isBlank(kelleleTahtaeg) || StringUtils.isNotBlank(kelleleTehtud)) {
                    continue;
                }
                if (wfRef == null) {
                    props = new HashMap<QName, Serializable>();
                    props.put(WorkflowCommonModel.Props.CREATOR_NAME, kelleltEnimi + " " + kelleltPnimi);
                    props.put(WorkflowCommonModel.Props.OWNER_ID, kelleltIkood);
                    props.put(WorkflowCommonModel.Props.OWNER_NAME, kelleltEnimi + " " + kelleltPnimi);
                    props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, dateTime);
                    props.put(WorkflowCommonModel.Props.STATUS, Status.IN_PROGRESS.getName());
                    props.put(WorkflowCommonModel.Props.STOPPED_DATE_TIME, null);
                    NodeRef cwfRef = getNodeService().createNode(
                                docRef,
                                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                                WorkflowCommonModel.Types.COMPOUND_WORKFLOW,
                                props
                                ).getChildRef();

                    props = new HashMap<QName, Serializable>();
                    props.put(WorkflowCommonModel.Props.CREATOR_NAME, kelleltEnimi + " " + kelleltPnimi);
                    props.put(WorkflowCommonModel.Props.MANDATORY, Boolean.FALSE);
                    props.put(WorkflowCommonModel.Props.PARALLEL_TASKS, Boolean.TRUE);
                    props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, dateTime);
                    props.put(WorkflowCommonModel.Props.STOPPED_DATE_TIME, null);
                    props.put(WorkflowCommonModel.Props.STATUS, Status.IN_PROGRESS.getName());
                    props.put(WorkflowCommonModel.Props.STOP_ON_FINISH, Boolean.FALSE);
                    props.put(WorkflowSpecificModel.Props.RESOLUTION, resolutsioon);
                    wfRef = getNodeService().createNode(
                                cwfRef,
                                WorkflowCommonModel.Assocs.WORKFLOW,
                                WorkflowCommonModel.Assocs.WORKFLOW,
                                WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW,
                                props
                                ).getChildRef();
                }

                props = new HashMap<QName, Serializable>();
                props.put(WorkflowSpecificModel.Props.CREATOR_EMAIL, kelleltEmail);
                props.put(WorkflowSpecificModel.Props.CREATOR_ID, kelleltIkood);
                props.put(WorkflowCommonModel.Props.CREATOR_NAME, kelleltEnimi + " " + kelleltPnimi);
                props.put(WorkflowCommonModel.Props.DOCUMENT_TYPE, mapping.typeInfo.docVer.getParent().getId());
                props.put(WorkflowCommonModel.Props.OWNER_EMAIL, kelleleEmail);
                props.put(WorkflowCommonModel.Props.OWNER_ID, kelleleIkood);
                props.put(WorkflowCommonModel.Props.OWNER_NAME, kelleleEnimi + " " + kellelePnimi);
                org.alfresco.util.Pair<NodeRef, Map<QName, Serializable>> kelleleUser = getUserData(kelleleIkood);
                Serializable taskOwnerJobTitle = null;
                Serializable taskOwnerStructUnit = null;
                if (kelleleUser != null) {
                    taskOwnerJobTitle = kelleleUser.getSecond().get(ContentModel.PROP_JOBTITLE);
                    taskOwnerStructUnit = kelleleUser.getSecond().get(ContentModel.PROP_ORGANIZATION_PATH);
                }
                props.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, taskOwnerJobTitle);
                props.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, taskOwnerStructUnit);
                props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, dateTime);
                props.put(WorkflowCommonModel.Props.STOPPED_DATE_TIME, null);
                props.put(WorkflowCommonModel.Props.STATUS, Status.IN_PROGRESS.getName());
                Date dueDate;
                try {
                    dueDate = dateTimeFormat.parse(kelleleTahtaeg + " 23:59:59");
                } catch (ParseException e) {
                    throw new RuntimeException("Unable to parse kelleleTahtaeg: " + e.getMessage(), e);
                }
                props.put(WorkflowSpecificModel.Props.DUE_DATE, dueDate);
                props.put(WorkflowSpecificModel.Props.DUE_DATE_DAYS, null);
                props.put(WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS, null);
                props.put(WorkflowSpecificModel.Props.RESOLUTION, resolutsioon);
                props.put(WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION, resolutsioon);
                props.put(WorkflowCommonModel.Props.OUTCOME, null);
                props.put(WorkflowCommonModel.Props.COMPLETED_DATE_TIME, null);
                props.put(WorkflowSpecificModel.Props.COMMENT, "");

                NodeRef taskRef = getNodeService().createNode(
                            wfRef,
                            WorkflowCommonModel.Assocs.TASK,
                            WorkflowCommonModel.Assocs.TASK,
                            WorkflowSpecificModel.Types.ASSIGNMENT_TASK,
                            props
                            ).getChildRef();
                getNodeService().addAspect(taskRef, WorkflowSpecificModel.Aspects.SEARCHABLE, null);
                if (firstTaskRef == null) {
                    firstTaskRef = taskRef;
                }
                if (!responsibleActiveSet && kelleleIkood.equals(docProps.get(OWNER_ID))) {
                    props = new HashMap<QName, Serializable>();
                    props.put(WorkflowSpecificModel.Props.ACTIVE, Boolean.TRUE);
                    getNodeService().addAspect(taskRef, WorkflowSpecificModel.Aspects.RESPONSIBLE, props);
                    responsibleActiveSet = true;
                    getPrivilegeService().setPermissions(docRef, kelleleIkood, Collections.singleton(Privileges.EDIT_DOCUMENT));
                } else {
                    getPrivilegeService().setPermissions(docRef, kelleleIkood, Collections.singleton(Privileges.VIEW_DOCUMENT_FILES));
                }

                docProps.put(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, Boolean.TRUE);
            }
        }
        if (!responsibleActiveSet && firstTaskRef != null) {
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(WorkflowSpecificModel.Props.ACTIVE, Boolean.TRUE);
            getNodeService().addAspect(firstTaskRef, WorkflowSpecificModel.Aspects.RESPONSIBLE, props);
            String ownerId = (String) getNodeService().getProperty(firstTaskRef, WorkflowCommonModel.Props.OWNER_ID);
            getPrivilegeService().setPermissions(docRef, ownerId, Collections.singleton(Privileges.EDIT_DOCUMENT));
        }
    }

    // [ASSOCS

    protected void loadPostponedAssocs() throws Exception {
        postponedAssocsFile = new File(workFolder, "postponed_assocs.csv");

        postponedAssocs = new TreeMap<Integer, List<PostponedAssoc>>();

        if (!postponedAssocsFile.exists()) {
            log.info("Skipping loading postponed assocs, file does not exist: " + postponedAssocsFile);
            return;
        }

        log.info("Loading postponed assocs from file " + postponedAssocsFile);

        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(postponedAssocsFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                Integer sourceDocId = new Integer(reader.get(0));
                Integer targetDocId = new Integer(reader.get(1));
                String assocTypeString = reader.get(2);
                AssocType assocType;
                if (AssocType.DEFAULT.getValueName().equals(assocTypeString)) {
                    assocType = AssocType.DEFAULT;
                } else if (AssocType.FOLLOWUP.getValueName().equals(assocTypeString)) {
                    assocType = AssocType.FOLLOWUP;
                } else if (AssocType.REPLY.getValueName().equals(assocTypeString)) {
                    assocType = AssocType.REPLY;
                } else {
                    throw new RuntimeException("Unknown assocType value: " + assocTypeString);
                }
                putPostponedAssoc(sourceDocId, targetDocId, assocType);
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + postponedAssocs.size() + " postponed assocs.");
        postponedAssocsCommited = new TreeMap<Integer, List<PostponedAssoc>>(postponedAssocs);
    }

    static class PostponedAssoc {
        Integer sourceDocId;
        Integer targetDocId;
        AssocType assocType;

        public PostponedAssoc(Integer sourceDocId, Integer targetDocId, AssocType assocType) {
            this.sourceDocId = sourceDocId;
            this.targetDocId = targetDocId;
            this.assocType = assocType;
        }
    }

    private void writePostponedAssocs() {
        try {
            // Write created documents
            if (postponedAssocsFile == null) {
                return;
            }
            boolean exists = postponedAssocsFile.exists();
            if (exists) {
                postponedAssocsFile.delete();
            }
            CsvWriter writer = new CsvWriter(new FileWriter(postponedAssocsFile, false), CSV_SEPARATOR);
            try {
                writer.writeRecord(new String[] {
                            "sourceDocumentId",
                            "targetDocumentId",
                            "assocType"
                    });
                for (List<PostponedAssoc> list : postponedAssocsCommited.values()) {
                    for (PostponedAssoc postponedAssoc : list) {
                        writer.writeRecord(new String[] {
                                postponedAssoc.sourceDocId.toString(),
                                postponedAssoc.targetDocId.toString(),
                                postponedAssoc.assocType.getValueName()
                        });
                    }
                }
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing file '" + postponedAssocsFile + "': " + e.getMessage(), e);
        }
    }

    protected void addAssociations(NodeRef documentRef, Integer documentId, Element root) {
        Element seosed = root.element(PP_ELEMENT_SEOSED);
        if (seosed != null) {
            for (Object object : seosed.elements()) {
                Element element = (Element) object;
                String elementText = element.elementText(PP_ELEMENT_DOK_NR);
                if (StringUtils.isBlank(elementText)) {
                    continue;
                }
                Integer targetDocumentId = new Integer(elementText);
                NodeRef targetDocumentRef = findCompletedDoc(targetDocumentId);
                if (targetDocumentRef == null) {
                    // Assert.isTrue(documentsMap.get(targetDocumentId) != null || completedDocumentsMap.get(targetDocumentId) != null
                    // , "Association to non-existent documentId=" + targetDocumentId);
                    if (documentsMap.get(targetDocumentId) != null || completedDocumentsMap.get(targetDocumentId) != null) {
                        log.warn("Association from documentId=" + documentId + " to non-existent documentId=" + targetDocumentId + " assocType=" + AssocType.DEFAULT.getValueName());
                    }
                    putPostponedAssoc(documentId, targetDocumentId, AssocType.DEFAULT);
                    // log.warn("Association to non-existent documentId=" + targetDocumentId + " in " + documentId + ".xml");
                    // continue;
                } else {
                    // for doc2doc assoc, it doesn't matter which is source and which is target
                    log.debug("Creating assoc " + documentId + " [" + documentRef + "] -> " + targetDocumentId + " [" + targetDocumentRef + "], type=" + AssocType.DEFAULT);
                    createAssoc(documentRef, targetDocumentRef, documentId, targetDocumentId, AssocType.DEFAULT);
                }
            }
        }

        // for reply/followup, initial document is source of assoc
        String elementText = root.elementText(PP_ELEMENT_ALUS_DOK_NR);
        Integer initialDocumentId = null;
        // if (elementText != null) {
        initialDocumentId = new Integer(elementText);
        // }
        String origin = root.elementText(PP_ELEMENT_PARITOLU);
        if (StringUtils.isBlank(origin)) {
            origin = root.elementText(PP_ELEMENT_PARITOLU2);
        }
        if (initialDocumentId != null && !documentId.equals(initialDocumentId)) {
            AssocType assocType;
            if (PP_VALUE_PARITOLU_VASTUSDOKUMENT.equals(origin)) {
                assocType = AssocType.REPLY;
            } else if (PP_VALUE_PARITOLU_JARG.equals(origin) || PP_VALUE_PARITOLU_JARELEPARIMINE.equals(origin)) {
                // SIM ignored Järg assocs, MV does not ignore
                assocType = AssocType.FOLLOWUP;
            } else {
                throw new RuntimeException("Unknown value of " + PP_ELEMENT_PARITOLU + " '" + origin + "'");
            }
            NodeRef initialDocumentRef = findCompletedDoc(initialDocumentId);
            if (initialDocumentRef == null) {
                // Assert.isTrue(documentsMap.get(initialDocumentId) != null || completedDocumentsMap.get(initialDocumentId) != null
                // , "Association to non-existent documentId=" + initialDocumentId);
                if (documentsMap.get(initialDocumentId) != null || completedDocumentsMap.get(initialDocumentId) != null) {
                    log.warn("Association from documentId=" + documentId + " to non-existent documentId=" + initialDocumentId + " assocType=" + assocType.getValueName());
                }
                putPostponedAssoc(documentId, initialDocumentId, assocType);
            } else {
                log.debug("Creating assoc " + documentId + " [" + documentRef + "] -> " + initialDocumentId + " [" + initialDocumentRef + "], type=" + assocType);
                createAssoc(documentRef, initialDocumentRef, documentId, initialDocumentId, assocType);
            }
        } else {
            // Few <paritolu> or <parituolu> values are blank
            if (StringUtils.isNotBlank(origin) && !PP_VALUE_PARITOLU_ALGATUSDOKUMENT.equals(origin)) {
                throw new RuntimeException("Unknown value of " + PP_ELEMENT_PARITOLU + " '" + origin + "'");
            }
        }
        List<PostponedAssoc> list = postponedAssocs.get(documentId);
        if (list != null) {
            for (PostponedAssoc postponedAssoc : list) {
                NodeRef sourceDocRef = findCompletedDoc(postponedAssoc.sourceDocId);
                log.debug("Creating assoc " + postponedAssoc.sourceDocId + " [" + sourceDocRef + "] -> " + documentId + " [" + documentRef + "], type=" + postponedAssoc.assocType);
                createAssoc(sourceDocRef, documentRef, postponedAssoc.sourceDocId, documentId, postponedAssoc.assocType);
            }
            postponedAssocs.remove(documentId);
        }
    }

    private void createAssoc(NodeRef sourceDocRef, NodeRef targetDocRef, Integer sourceDocId, Integer targetDocId, AssocType assocType) {
        QName assocTypeQName;
        switch (assocType) {
        case REPLY:
            assocTypeQName = DocumentCommonModel.Assocs.DOCUMENT_REPLY;
            break;
        case FOLLOWUP:
            assocTypeQName = DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP;
            break;
        case DEFAULT:
            assocTypeQName = DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT;
            break;
        default:
            throw new RuntimeException("Unsupported assocType: " + assocType);
        }
        if (sourceDocRef == null || !nodeService.exists(sourceDocRef)) {
            log.error("Skipping creating assoc, source does not exist, sourceDocumentId=" + sourceDocId + " targetDocumentId="
                    + targetDocId + " assocType=" + assocType.getValueName() + " sourceDocRef=" + sourceDocRef + " targetDocRef=" + targetDocRef);
            return;
        }
        if (targetDocRef == null || !nodeService.exists(targetDocRef)) {
            log.error("Skipping creating assoc, target does not exist, sourceDocumentId=" + sourceDocId + " targetDocumentId="
                    + targetDocId + " assocType=" + assocType.getValueName() + " sourceDocRef=" + sourceDocRef + " targetDocRef=" + targetDocRef);
            return;
        }
        boolean skip = false;
        List<AssociationRef> targetAssocs = nodeService.getSourceAssocs(sourceDocRef, assocTypeQName);
        for (AssociationRef assocRef : targetAssocs) {
            Assert.isTrue(assocRef.getTargetRef().equals(sourceDocRef), "targetDocRef=" + targetDocRef + ", sourceDocRef=" + sourceDocRef + ", assocRef=" + assocRef);
            // if (sourceDocRef.equals(assocRef.getSourceRef())
            log.debug("Existing target-assoc [" + assocRef.getSourceRef() + "] -> [" + assocRef.getTargetRef() + "], type=" + assocRef.getTypeQName());
            if ((sourceDocRef.equals(assocRef.getSourceRef()) && targetDocRef.equals(assocRef.getTargetRef())) ||
                    (targetDocRef.equals(assocRef.getSourceRef()) && sourceDocRef.equals(assocRef.getTargetRef()))) {
                if (assocType == AssocType.DEFAULT) {
                    log.debug("Skipping this assoc creation");
                    skip = true;
                } else {
                    throw new RuntimeException("Non-default assoc cannot previously exist - existing target-assoc ["
                            + assocRef.getSourceRef() + "] -> [" + assocRef.getTargetRef() + "], type=" + assocRef.getTypeQName());
                }
            }
            // if (assocRef.getSourceRef().equals(targetDocRef)) {
            // throw new RuntimeException("Assoc already exists: " + assocRef);
            // }
        }
        List<AssociationRef> sourceAssocs = nodeService.getTargetAssocs(targetDocRef, assocTypeQName);
        for (AssociationRef assocRef : sourceAssocs) {
            Assert.isTrue(assocRef.getSourceRef().equals(targetDocRef), "targetDocRef=" + targetDocRef + ", sourceDocRef=" + sourceDocRef + ", assocRef=" + assocRef);
            log.debug("Existing source-assoc [" + assocRef.getSourceRef() + "] -> [" + assocRef.getTargetRef() + "], type=" + assocRef.getTypeQName());
            if ((sourceDocRef.equals(assocRef.getSourceRef()) && targetDocRef.equals(assocRef.getTargetRef())) ||
                    (targetDocRef.equals(assocRef.getSourceRef()) && sourceDocRef.equals(assocRef.getTargetRef()))) {
                if (assocType == AssocType.DEFAULT) {
                    log.debug("Skipping this assoc creation");
                    skip = true;
                } else {
                    throw new RuntimeException("Non-default assoc cannot previously exist - existing source-assoc ["
                            + assocRef.getSourceRef() + "] -> [" + assocRef.getTargetRef() + "], type=" + assocRef.getTypeQName());
                }
            }
        }
        // if (targetAssocs == null || targetAssocs.isEmpty()) {
        if (!skip) {
            nodeService.createAssociation(sourceDocRef, targetDocRef, assocTypeQName);
        }
        // }
    }

    private void putPostponedAssoc(Integer sourceDocId, Integer targetDocId, AssocType assocType) {
        List<PostponedAssoc> list = postponedAssocs.get(targetDocId);
        if (list == null) {
            list = new ArrayList<PostponedAssoc>();
            postponedAssocs.put(targetDocId, list);
        }
        list.add(new PostponedAssoc(sourceDocId, targetDocId, assocType));
    }

    // ASSOCS]
    public NodeRef addFile(String displayName, File file, NodeRef parentNodeRef, String mimeType) {
        String fileName = FilenameUtil.makeSafeFilename(displayName);
        fileName = generalService.getUniqueFileName(parentNodeRef, fileName);
        final FileInfo fileInfo = fileFolderService.create(parentNodeRef, fileName, ContentModel.TYPE_CONTENT);
        final NodeRef fileRef = fileInfo.getNodeRef();
        final ContentWriter writer = fileFolderService.getWriter(fileRef);
        generalService.writeFile(writer, file, fileName, mimeType);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        // props.put(VersionsModel.Props.VersionModified.FIRSTNAME, "");
        // props.put(VersionsModel.Props.VersionModified.LASTNAME, CREATOR_MODIFIER);
        // props.put(VersionsModel.Props.VersionModified.MODIFIED, new Date());
        props.put(ContentModel.PROP_CREATOR, CREATOR_MODIFIER);
        props.put(ContentModel.PROP_MODIFIER, CREATOR_MODIFIER);
        props.put(FileModel.Props.DISPLAY_NAME, displayName);
        props.put(FileModel.Props.ACTIVE, Boolean.TRUE);
        nodeService.addProperties(fileRef, props);
        return fileRef;
    }

    // HELPER METHODS
    /** RetryingTransactionHelper that only tries to do things once. */
    private RetryingTransactionHelper getTransactionHelper() {
        RetryingTransactionHelper helper = new RetryingTransactionHelper();
        helper.setMaxRetries(1);
        helper.setTransactionService(transactionService);
        return helper;
    }

    private org.alfresco.util.Pair<NodeRef, Map<QName, Serializable>> getUserData(String userName) {
        org.alfresco.util.Pair<NodeRef, Map<QName, Serializable>> userData = userDataByUserName.get(userName);
        if (userData == null) {
            NodeRef userRef = getUserService().getPerson(userName);
            if (userRef != null) {
                Map<QName, Serializable> userProps = getNodeService().getProperties(userRef);
                userData = org.alfresco.util.Pair.newInstance(userRef, userProps);
            }
            userDataByUserName.put(userName, userData);
        }
        return userData;
    }

}
