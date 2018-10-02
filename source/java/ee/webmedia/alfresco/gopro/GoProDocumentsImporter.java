package ee.webmedia.alfresco.gopro;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentListService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPrivilegeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.addressbook.model.AddressbookModel.Props;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.PublishToAdr;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.DocumentService.AssocType;
import ee.webmedia.alfresco.gopro.GoProDocumentsMapper.DocumentValue;
import ee.webmedia.alfresco.gopro.GoProDocumentsMapper.Mapping;
import ee.webmedia.alfresco.gopro.GoProDocumentsMapper.PropMapping;
import ee.webmedia.alfresco.gopro.GoProDocumentsMapper.PropertyValue;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.orgstructure.model.OrganizationStructure;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * Imports documents and files from GoPro.
 */
public class GoProDocumentsImporter {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(GoProDocumentsImporter.class);
    
    private static final String GP_MAIN_ELEMENT_FIELD = "field";
    private static final String GP_MAIN_ATTRIBUTE_NAME = "name";
    // doc elements
    private static final String GP_ELEMENT_CREATION_DATE = "CreationDate";
    private static final String GP_ELEMENT_DOC_ACCESS_TYPE = "DocAccessType";
    private static final String GP_ELEMENT_DOC_CAT = "DocCat";
    private static final String GP_ELEMENT_DATE_SET = "DateSet";
    private static final String GP_ELEMENT_MAIN_ATTACHMANET = "mainAttachment";
    private static final String GP_ELEMENT_ADDITIONAL_ATTACHMENT = "additionalAttachment";
    private static final String GP_ATTRIBUTE_ATTACHMENT_NAME = "attachmentname";
    private static final String GP_ELEMENT_HTML_BODY = "Body";
    private static final String GP_ELEMENT_DOC_COMMENTS_LIST = "document_comments_list";
    private static final String GP_ELEMENT_LINK = "link";
    private static final String GP_ELEMENT_PARENT_DOCUMENT_ID = "parentdocumentid";
    private static final String GP_ELEMENT_CASE_LINK = "caseLink";
    private static final String GP_ELEMENT_CASE_NUMBER = "CaseNumber";
    // wf elements
    private static final String GP_ELEMENT_WF_APPROVAL_DESCRIPTION = "ApprovalDescription";
    private static final String GP_ELEMENT_WF_APPSTART = "APPSTART";
    private static final String GP_ELEMENT_WF_PROCESSED_REVIEWERS = "processedreviewers";
    private static final String GP_ELEMENT_WF_APPROVAL_HISTORY = "approvalhistory";
    
    // series mapping elements
    private static final String GP_ELEMENT_SERIES_MAPPING_PROP = "prop";
    private static final String GP_ELEMENT_SERIES_MAPPING_FROM = "from";
    private static final String GP_ELEMENT_SERIES_MAPPING_TO = "to";
    
    
    
    /**
     * 
     */
    private static final String MAPPINGS_PROP_TO_REG_NUMBER_WITHOUT_INDIVIDUAL = "_regNumberWithoutIndividual";
    private static final String MAPPINGS_PROP_TO_DOCUMENT_TYPE_FROM = "_documentTypeFrom";
    
    private static final String DUMMY_USER_ID = "99999999999";
    private static final String HTML_FILE_META_INFO = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">";

    /*
     * TODO lisada kellaaeg csv'sse
     * TODO kirjutada failide mimetype ja encoding csv'sse
     * TODO ETA arvutamist tegin korda - võtsin AbstraceNodeUpdater'ist uuema koodi; ainus väike asi, et kui viimane batch tuleb täpselt täis, siis viimast completed info rida
     * trükib 2 korda
     */
    


    protected static final char CSV_SEPARATOR = ';';
    private static final String CREATOR_MODIFIER = "DHS";
    private static final FastDateFormat staticDateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm:ss");

    private final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    private final DateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private final DateFormat dateTimeFormatddMMyy = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
    private final DateFormat dateTimeFormatdMMyy = new SimpleDateFormat("d.MM.yy HH:mm:ss");

    protected SAXReader xmlReader = new SAXReader();

    private File dataFolder;
    private File workFolder;
    private int batchSize;
    private String institutionCode;
    private String defaultOwnerId;
    private String defaultOwnerName;
    private String defaultOwnerEmail;
    private Collection<StoreRef> storeRefs = new LinkedHashSet<StoreRef>();
    
    // [SPRING BEANS
    private TransactionService transactionService;
    private DocumentService documentService;
    private GeneralService generalService;
    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private GoProDocumentsMapper goProDocumentsMapper;
    private BehaviourFilter behaviourFilter;
    private GoProImporter goProImporter;
    

    public GoProDocumentsImporter(GoProImporter goProImporter) {
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
        goProDocumentsMapper = new GoProDocumentsMapper();
        goProDocumentsMapper.setNamespaceService(getNamespaceService());
        goProDocumentsMapper.setDictionaryService(BeanHelper.getDictionaryService());
        goProDocumentsMapper.setGeneralService(BeanHelper.getGeneralService());
        setBehaviourFilter(BeanHelper.getPolicyBehaviourFilter());
        setGoProImporter(goProImporter);

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

    public void setGoProDocumentsMapper(GoProDocumentsMapper goProDocumentsMapper) {
        this.goProDocumentsMapper = goProDocumentsMapper;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
        this.behaviourFilter = behaviourFilter;
    }

    public void setGoProImporter(GoProImporter goProImporter) {
        this.goProImporter = goProImporter;
    }

    /**
     * Runs documents import process
     */
    public void runImport(File dataFolder, File workFolder, File mappingsFile, int batchSize, String defaultOwnerId, String institutionCode)
                    throws Exception {
        this.dataFolder = dataFolder;
        this.workFolder = workFolder;
        this.batchSize = batchSize;
        this.institutionCode = institutionCode;
        this.defaultOwnerId = defaultOwnerId;
        Map<QName, Serializable> defaultUserProps = getUserService().getUserProperties(defaultOwnerId);
        this.defaultOwnerName = UserUtil.getPersonFullName1(defaultUserProps);
        this.defaultOwnerEmail = (String)defaultUserProps.get(ContentModel.PROP_EMAIL);
        init();
        // Doc import
        try {
        	File completedStructureFile = new File(workFolder, StructureImporter.COMPLETED_FILENAME);
        	if (!completedStructureFile.exists()) {
                log.info("Skipping documents import, struucture was not imported yet, file does not exist: " + completedStructureFile);
                return;
            }
        	storeRefs.add(generalService.getStore());
        	storeRefs.add(generalService.getArchivalsStoreRef());
        	
            mappings = goProDocumentsMapper.loadMetadataMappings(mappingsFile);
            loadSeriesMappings();
            loadCompletedCaseFilesVolumes();
            loadDocuments();
            loadCompletedDocuments();
            loadPostponedAssocs();
            createDocuments();
        } finally {
            writePostponedAssocs();
            documentsMap = null;
            mappings = null;
            postponedAssocs = null;
            completedCaseFiles = null;
            completedVolumes = null;
        }


        // Files indexing
        loadIndexedFiles();
        indexFiles();
    }

    private void init() {
        documentsMap = new TreeMap<String, File>();
        completedDocumentsMap = new TreeMap<String, NodeRef>();
        postponedAssocs = new TreeMap<String, List<PostponedAssoc>>();
        postponedAssocsCommited = new TreeMap<String, List<PostponedAssoc>>();
        completedCaseFiles = new HashMap<String, GoProVolume>();
        completedVolumes = new HashMap<String, List<GoProVolume>>();
        mappings = null;
        seriesMappings = new HashMap<String, String>();
        indexedFiles = null;
        filesToIndex = null;
        filesToProceed = null;
    }

    protected NavigableMap<String /* documentId */, File> documentsMap;
    protected NavigableMap<String /* documentId */, NodeRef> completedDocumentsMap;
    protected NavigableMap<String /* documentId */, NodeRef> readyToIndexDocumentsMap;
    protected NavigableMap<String /* documentId */, List<PostponedAssoc>> postponedAssocs;
    protected NavigableMap<String /* documentId */, List<PostponedAssoc>> postponedAssocsCommited;
    private final Map<String, Pair<NodeRef, Map<QName, Serializable>>> userDataByUserName = new HashMap<String, Pair<NodeRef, Map<QName, Serializable>>>();
    private Map<String, GoProVolume> completedCaseFiles;
    private Map<String, List<GoProVolume>> completedVolumes;


    protected NavigableSet<Integer> filesToProceed;
    protected File completedDocumentsFile;
    protected File failedDocumentsFile;
    protected File postponedAssocsFile;
    

    private Map<String, Mapping> mappings;
    private Map<String, String> seriesMappings = new HashMap<String, String>();
    
    
    private void loadCompletedCaseFilesVolumes() throws IOException {
    	completedCaseFiles = new HashMap<String, GoProVolume>();
        completedVolumes = new HashMap<String, List<GoProVolume>>();
        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(new File(workFolder, "completed_volumes_cases.csv"))), CSV_SEPARATOR,
                Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                String volumeType = reader.get(0);
                String mark = reader.get(1);
                NodeRef nodeRef = new NodeRef(reader.get(4));
                Date validFrom = null;
                try {
                	validFrom = dateFormat.parse(reader.get(5));
                } catch (Throwable t) {}
                Date validTo = null;
                try {
                	validTo = dateFormat.parse(reader.get(6));
                } catch (Throwable e) {}
                String seriesIdentifier = reader.get(8);
                String function = reader.get(7);
                GoProVolume volume = new GoProVolume();
                volume.nodeRef = nodeRef;
                volume.mark = mark;
                volume.seriesIdentifier = seriesIdentifier;
                volume.function = function;
                volume.validFrom = validFrom;
                volume.validTo = validTo;
                if ("caseFile".equals(volumeType)) {
                	completedCaseFiles.put(mark, volume);
                } else {
                	List<GoProVolume> volumes = completedVolumes.get(seriesIdentifier);
                	if (volumes == null) {
                		volumes = new ArrayList<GoProVolume>();
                		completedVolumes.put(seriesIdentifier, volumes);
                	}
                	volumes.add(volume);
                	
                }
            }
        } finally {
            reader.close();
        }
    }

    protected void loadDocuments() {
        documentsMap = new TreeMap<String, File>();
        log.info("Getting xml entries of " + dataFolder);
        File[] files = dataFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return StringUtils.isNotBlank(name) && name.endsWith(".xml") && !name.toLowerCase().contains("mapping");
            }
        });

        log.info("Directory listing contains " + files.length + " xml entries, parsing them to documents");
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".xml") && !name.contains("mappings")) {
                // Document
            	String documentId = StringUtils.substringBefore(name, ".xml");
                log.debug("Found documentId=" + documentId);
                documentsMap.put(documentId, file);
                
            }
        }
        log.info("Completed parsing directory listing, got " + documentsMap.size() + " documents");
    }

    
    
    private File indexedFilesFile;
    private Set<String> indexedFiles;
    private NavigableSet<String> filesToIndex;

    protected void loadIndexedFiles() throws Exception {
        indexedFilesFile = new File(workFolder, "indexed_files.csv");
        indexedFiles = new HashSet<String>();

        if (!indexedFilesFile.exists()) {
            log.info("Skipping loading previously indexed files, file does not exist: " + indexedFilesFile);
        } else {

            log.info("Loading previously indexed files from file " + indexedFilesFile);

            CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(indexedFilesFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
            try {
                reader.readHeaders();
                while (reader.readRecord()) {
                    String documentId = reader.get(0);
                    indexedFiles.add(documentId);
                }
            } finally {
                reader.close();
            }
            log.info("Loaded " + indexedFiles.size() + " documents with previously indexed files");
        }
        
        readyToIndexDocumentsMap = new TreeMap<String, NodeRef>();

        completedDocumentsFile = new File(workFolder, "completed_docs.csv");
        if (!completedDocumentsFile.exists()) {
            log.info("Skipping loading previously completed documentId-s, file does not exist: " + completedDocumentsFile);
            return;
        }

        log.info("Loading previously completed documentId-s- (for indexing) from file " + completedDocumentsFile);

        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(completedDocumentsFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                String documentId = reader.get(0);
                String nodeRefString = reader.get(1);
                if (StringUtils.isNotBlank(nodeRefString)) {
                    NodeRef documentRef = new NodeRef(nodeRefString);
                    readyToIndexDocumentsMap.put(documentId, documentRef);
                }
            }
        } finally {
            reader.close();
        }
        log.info("Loaded " + readyToIndexDocumentsMap.size() + " previously completed documentId-s (for indexing)");


        filesToIndex = new TreeSet<String>(readyToIndexDocumentsMap.keySet());
        filesToIndex.removeAll(indexedFiles);

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

    private class FilesIndexBatchProgress extends BatchProgress<String> {

        public FilesIndexBatchProgress(Set<String> origin) {
            this.origin = origin;
            processName = "Files indexing";
        }

        @Override
        void executeBatch() {
            createFilesIndexBatch(batchList);
        }
    }

    protected void createFilesIndexBatch(final List<String> batchList) {
        final Set<String> batchCompletedFiles = new HashSet<String>(batchSize);
        for (String documentId : batchList) {
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
                for (String documentId : batchCompletedFiles) {
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

    private boolean findUserNode(String goProUserName, Map<QName, Serializable> props, QName idQname, QName nameQname, QName emailQname) {
    	Node userNode = null;
    	if (StringUtils.isNotBlank(goProUserName)) {
    		String userFullname = parseUserFullName(goProUserName);
    		
    		List<Node> userNodes = getUserService().searchUsers(userFullname, false, -1);
    		if (userNodes != null && userNodes.size() == 1) {
    			userNode = userNodes.get(0);
    		}
    	}
    	if (userNode != null) {
    		String userId = (String)userNode.getProperties().get(ContentModel.PROP_USERNAME);
    		String userName = (String)userNode.getProperties().get(ContentModel.PROP_FIRSTNAME) + " " + (String)userNode.getProperties().get(ContentModel.PROP_LASTNAME);
    		String email = (String)userNode.getProperties().get(ContentModel.PROP_EMAIL);
    		if (idQname != null) {
    			props.put(idQname, userId);
    		}
    		if (nameQname != null) {
    			props.put(nameQname, (StringUtils.isNotBlank(userName)?userName:userId));
    		}
    		if (emailQname != null && StringUtils.isNotBlank(email)) {
    			props.put(emailQname, email);
    		}
    		return true;
    	} else {
    		return false;
    	}
	}
    
    private String parseUserFullName(String goProUserName) {
    	String userFullname = goProUserName;
		if (goProUserName.contains("/")) {
			userFullname = StringUtils.substringBefore(goProUserName, "/").trim();
		}
		if (goProUserName.contains("(")) {
			userFullname = StringUtils.substringBefore(goProUserName, "(").trim();
		}
		
		return userFullname;
    }

    private void setOwnerProperties(Map<QName, Serializable> props, Element root, Mapping mapping, NodeRef caseFileRef) {
        String gpOwnerName = StringUtils.stripToNull((String) props.get(DocumentCommonModel.Props.OWNER_NAME));
        String gpSignerName = StringUtils.stripToNull((String) props.get(DocumentCommonModel.Props.SIGNER_NAME));
        String comment = (String) props.get(DocumentCommonModel.Props.COMMENT);
        
        boolean ownerFound = false;
        boolean signerFound = false;
        if (StringUtils.isNotBlank(gpOwnerName)) {
        	ownerFound = findUserNode(gpOwnerName, props, OWNER_ID, DocumentCommonModel.Props.OWNER_NAME, DocumentCommonModel.Props.OWNER_EMAIL);
        	if (!ownerFound) {
       			comment = StringUtils.isNotBlank(comment)?comment + " " + DocumentCommonModel.Props.OWNER_NAME.getLocalName() + ":" + gpOwnerName :DocumentCommonModel.Props.OWNER_NAME.getLocalName() + ":" + gpOwnerName;
       		}
        }
        
        if (!ownerFound) {
        	String ownerId = defaultOwnerId;
        	String ownerName = defaultOwnerName;
        	String volumeType = (String)getNodeService().getProperty(caseFileRef, VolumeModel.Props.VOLUME_TYPE);
        	if (VolumeType.CASE_FILE.name().equals(volumeType)) {
        		ownerId = (String)getNodeService().getProperty(caseFileRef, DocumentCommonModel.Props.OWNER_ID);
        		ownerName = (String)getNodeService().getProperty(caseFileRef, DocumentCommonModel.Props.OWNER_NAME);
        	}
        	props.put(DocumentCommonModel.Props.OWNER_ID, ownerId);
        	props.put(DocumentCommonModel.Props.OWNER_NAME, ownerName);
        }
        
        if (StringUtils.isNotBlank(gpSignerName)) {
       		signerFound = findUserNode(gpSignerName, props, null, DocumentCommonModel.Props.SIGNER_NAME, null);
       		if (!signerFound) {
       			comment = StringUtils.isNotBlank(comment)?comment + " " + DocumentCommonModel.Props.SIGNER_NAME.getLocalName() + ":" + gpSignerName :DocumentCommonModel.Props.SIGNER_NAME.getLocalName() + ":" + gpSignerName;
       		}
        }
        
        if (!signerFound) {
        	props.remove(DocumentCommonModel.Props.SIGNER_NAME);
        }
        
        props.put(DocumentCommonModel.Props.COMMENT, comment);

        
    }


    private void setAccessRestriction(Map<QName, Serializable> props, Element root, Mapping mapping) {
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
                props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, "AvTS § 35 lg 1 p 12");
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
            if (props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE) == null) {
                throw new RuntimeException("accessRestrictionEndDate is null, but accessRestriction = " + accessRestriction);
            }
        }
        if (AccessRestriction.OPEN.equals(accessRestriction) || AccessRestriction.INTERNAL.equals(accessRestriction)) {
            props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON, null);
            props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE, null);
            props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE, null);
            props.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC, null);
        }

        if (mapping.typeInfo.propDefs.containsKey(DocumentDynamicModel.Props.PUBLISH_TO_ADR.getLocalName())) {
        	setPublishToAdr(props, root);
        }

    }
    
    private void setPublishToAdr(Map<QName, Serializable> props, Element root) {
    	try {
    		Date notToAdrDate = dateFormat.parse("04.01.2010");
    		Date accessRestrictoinBeginDate = (Date)props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE);
    		String creationDateStr = getElementTextByAttribudeName(root, GP_ELEMENT_CREATION_DATE);
    		Date creationDate = (StringUtils.isNotBlank(creationDateStr))?dateFormat.parse(root.elementText(GP_ELEMENT_CREATION_DATE)):null;
    		if (creationDate != null && creationDate.before(notToAdrDate) && accessRestrictoinBeginDate != null && accessRestrictoinBeginDate.before(notToAdrDate)) {
    			props.put(DocumentDynamicModel.Props.PUBLISH_TO_ADR, PublishToAdr.NOT_TO_ADR.getValueName());
    			return;
    		}
    	} catch (Exception e) {
    		// do nothing, just check next conditions
    	}
    	
    	String docCat = getElementTextByAttribudeName(root, GP_ELEMENT_DOC_CAT);
    	if (StringUtils.isNotBlank(docCat) && docCat.contains("5.1-2")) {
    		props.put(DocumentDynamicModel.Props.PUBLISH_TO_ADR, PublishToAdr.NOT_TO_ADR.getValueName());
			return;
    	}
    	
    	String accessRestriction = (String) props.get(ACCESS_RESTRICTION);
    	if ("A".equals(getElementTextByAttribudeName(root, GP_ELEMENT_DOC_ACCESS_TYPE)) || AccessRestriction.AK.getValueName().equals(accessRestriction)) {
    		props.put(DocumentDynamicModel.Props.PUBLISH_TO_ADR, PublishToAdr.TO_ADR.getValueName());
    	} else if (AccessRestriction.OPEN.getValueName().equals(accessRestriction)) {
    		props.put(DocumentDynamicModel.Props.PUBLISH_TO_ADR, PublishToAdr.REQUEST_FOR_INFORMATION.getValueName());
    	}
    	
    }

    private void checkStop() {
        goProImporter.checkStop();
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
    
    protected void loadSeriesMappings() throws Exception {
    	File seriesMappingsXml = new File(dataFolder, "seriesMappings.xml");
        if (!seriesMappingsXml.exists()) {
            log.info("Skipping loading series mappings, file does not exist: " + seriesMappingsXml);
            return;
        }
        Element root = xmlReader.read(seriesMappingsXml).getRootElement();
        for (Object prop: root.elements(GP_ELEMENT_SERIES_MAPPING_PROP)) {
        	String from = ((Element)prop).elementText(GP_ELEMENT_SERIES_MAPPING_FROM);
        	String to = ((Element)prop).elementText(GP_ELEMENT_SERIES_MAPPING_TO);
        	if (from != null) {
        		from = from.trim();
        	}
        	if (to != null) {
        		to = to.trim();
        	}
        	seriesMappings.put(from, to);
        }
        
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

        completedDocumentsMap = new TreeMap<String, NodeRef>();

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
                String documentId = reader.get(0);
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

    private class DocumensBatchProgress extends BatchProgress<Entry<String, File>> {
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
            getDocumentListService().updateDocCounters();
        }
        log.info("Documents IMPORT COMPLETE :)");
    }

    static class ImportedDocument {
        String documentId;
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
        String docType;
        String suffixType;
        String documentTypeId;
        String fileNames;

        public ImportedDocument(String documentId, NodeRef nodeRef, String toimik, String registreerimisNr, String docType, String suffixType, String documentTypeId, String fileNames,
                Map<QName, Serializable> props) {
            this.documentId = documentId;
            this.nodeRef = nodeRef;
            this.toimik = toimik;
            this.registreerimisNr = registreerimisNr;
            this.docType = docType;
            this.suffixType = suffixType;
            this.documentTypeId = documentTypeId;
            this.fileNames = fileNames;
            
            if (props != null) {
                regNumber = (String) props.get(DocumentCommonModel.Props.REG_NUMBER);
                regDateTime = ((Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME) != null)?staticDateTimeFormat.format((Date) props.get(DocumentCommonModel.Props.REG_DATE_TIME)):"";
                docName = (String) props.get(DocumentCommonModel.Props.DOC_NAME);
                ownerId = (String) props.get(DocumentCommonModel.Props.OWNER_ID);
                ownerName = (String) props.get(DocumentCommonModel.Props.OWNER_NAME);
                accessRestriction = (String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
                accessRestrictionReason = (String) props.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON);
            }
        }
    }

    protected void createDocumentsBatch(final List<Entry<String, File>> batchList) throws Exception {
        final Map<String, ImportedDocument> batchCompletedDocumentsMap = new TreeMap<String, ImportedDocument>();
        AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
            @Override
            public void afterCommit() {
                postponedAssocsCommited = new TreeMap<String, List<PostponedAssoc>>(postponedAssocs);
            }

            @Override
            public void afterRollback() {
                postponedAssocs = new TreeMap<String, List<PostponedAssoc>>(postponedAssocsCommited);
                completedDocumentsMap.keySet().removeAll(batchCompletedDocumentsMap.keySet());
            }
        });
        for (Entry<String, File> entry : batchList) {
            String documentId = entry.getKey();
            if (log.isTraceEnabled()) {
                log.trace("Processing documentId=" + documentId);
            }
            File file = entry.getValue();
            try {
                ImportedDocument doc = createDocument(documentId, file);
                if (doc.nodeRef != null) {
                	batchCompletedDocumentsMap.put(documentId, doc);
                	completedDocumentsMap.put(documentId, doc.nodeRef); // Add immediately to completedDocumentsMap, because other code wants to access it
                } else {
	                CsvWriter writer = new CsvWriter(new FileWriter(failedDocumentsFile, true), CSV_SEPARATOR);
	                try {
	                    writer.writeRecord(new String[] { file.getName(), doc.documentTypeId });
	                } finally {
	                    writer.close();
	                }
                }
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
                        "documentTypeId",
                        "fileNames"
                };
            }

            @Override
            public void execute(CsvWriter writer) throws IOException {
                for (Entry<String, ImportedDocument> entry : batchCompletedDocumentsMap.entrySet()) {
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
                                                                                                            doc.docType == null ? "" : doc.docType,
                                                                                                                    doc.suffixType == null ? "" : doc.suffixType,
                                                                                                                            doc.documentTypeId == null ? "" : doc.documentTypeId,
                                                                                                                            		doc.fileNames == null ? "" : doc.fileNames
                    });
                }
            }
        });
    }

    private ImportedDocument createDocument(String documentId, File file) throws DocumentException, ParseException {
        return importDoc(file, mappings, documentId);
    }
    
    private String addFiles(String documentId, NodeRef importDocRef, Element root) {
    	StringBuilder fileNames = new StringBuilder();
    	List activeFilesList = getElementsByAttribudeName(root, GP_ELEMENT_MAIN_ATTACHMANET);
        if (activeFilesList != null) {
            for (Object file : activeFilesList) {
                String fileName = ((Element)file).attributeValue(GP_ATTRIBUTE_ATTACHMENT_NAME);
                String contentStr = ((Element)file).getText();
                
                NodeRef fileRef = addFile(fileName, contentStr, importDocRef, null, true, true);
                if (fileNames.length() > 1) {
                	fileNames.append("| ");
                }
                fileNames.append(fileName + ":" + fileRef.toString());
            }
        }
        List inactiveFilesList = getElementsByAttribudeName(root, GP_ELEMENT_ADDITIONAL_ATTACHMENT);
        if (inactiveFilesList != null) {
            for (Object file : inactiveFilesList) {
                String fileName = ((Element)file).attributeValue(GP_ATTRIBUTE_ATTACHMENT_NAME);
                String contentStr = ((Element)file).getText();
                NodeRef fileRef = addFile(fileName, contentStr, importDocRef, null, false, true);
                if (fileNames.length() > 1) {
                	fileNames.append("| ");
                }
                fileNames.append(fileName + ":" + fileRef.toString());
            }
        }
        
        Element htmlFile = getElementByAttribudeName(root, GP_ELEMENT_HTML_BODY);
        if (htmlFile != null) {
        	String fileName = "Sisu.html";
        	NodeRef fileRef = addFile(fileName, HTML_FILE_META_INFO + htmlFile.getText(), importDocRef, null, true, false);
        	if (fileNames.length() > 1) {
            	fileNames.append("| ");
            }
            fileNames.append(fileName + ":" + fileRef.toString());
        }
        
        return fileNames.toString();
    }
    
    private NodeRef addFile(String displayName, String content, NodeRef parentNodeRef, String mimeType, boolean active, boolean isBase64) {
        String fileName = FilenameUtil.makeSafeFilename(displayName);
        fileName = generalService.getUniqueFileName(parentNodeRef, fileName);
        final FileInfo fileInfo = fileFolderService.create(parentNodeRef, fileName, ContentModel.TYPE_CONTENT);
        final NodeRef fileRef = fileInfo.getNodeRef();
        
        OutputStream out = null;
        try {
        	String mimetype = BeanHelper.getMimetypeService().guessMimetype(fileName);
	        final ContentWriter writer = fileFolderService.getWriter(fileRef);
	        writer.setMimetype(mimetype);
	        writer.setEncoding("UTF-8");
	        if (isBase64) {
		        out = writer.getContentOutputStream();
		        byte[] decodedContent = Base64.decode(content);
		        out.write(decodedContent);
		        out.flush();
	        } else {
	        	writer.putContent(content);
	        }
        } catch (IOException e) {
            final String msg = "Outputstream exception while writing file: " + fileName;
            throw new RuntimeException(msg, e);
        } catch (Base64DecodingException e) {
        	final String msg = "Outputstream exception while exporting consolidated docList row to CSV-stream";
            throw new RuntimeException(msg, e);
        } finally {
        	IOUtils.closeQuietly(out);
        }
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(ContentModel.PROP_CREATOR, CREATOR_MODIFIER);
        props.put(ContentModel.PROP_MODIFIER, CREATOR_MODIFIER);
        props.put(FileModel.Props.DISPLAY_NAME, displayName);
        props.put(FileModel.Props.ACTIVE, Boolean.valueOf(active));
        nodeService.addProperties(fileRef, props);
        return fileRef;
    }

    private Map<QName, Serializable> mapProperties(Element root, Mapping mapping, int index) {
        DocumentValue docValue = new DocumentValue(mapping.typeInfo);

        for (PropMapping pm : mapping.props) {
            try {
                if (pm.to.startsWith("_")) {
                    continue;
                }
                
                if (pm.from.endsWith("#")) {
                	String from = pm.from;
                	if (index > 0) {
                		String value = getElementTextByAttribudeName(root, from.replace("#", String.valueOf(index)));
	                	if (StringUtils.isBlank(value)) {
	                		value = getElementTextByAttribudeName(root, from.replace("#", "_" + index));
	                	}
	                	if (value != null) {
		                    docValue.put(value, pm.to, pm.prefix);
		                }
                	} else {
	                	List<String> values = new ArrayList<String>();
	                	int i = 1;
	                	String itemValue = getElementTextByAttribudeName(root, from.replace("#", String.valueOf(i)));
	                	if (StringUtils.isBlank(itemValue)) {
	                		itemValue = getElementTextByAttribudeName(root, from.replace("#", "_" + i));
	                	}
	                	
	                	while (StringUtils.isNotBlank(itemValue)) {
	                		values.add(itemValue);
	                		i++;
	                		itemValue = getElementTextByAttribudeName(root, from.replace("#", String.valueOf(i)));
	                		if (StringUtils.isBlank(itemValue)) {
	                    		itemValue = getElementTextByAttribudeName(root, from.replace("#", "_" + i));
	                    	}
	                	}
	                	
	                	if (!values.isEmpty()) {
	                		docValue.putObject(values, pm.to);
	                	}
                	}
                } else {
                	String value = null;
	                Element el = getElementByAttribudeName(root, pm.from);
	                if (el != null) {
	                    value = el.getStringValue();
	                }
	                if (value != null) {
	                    docValue.put(value, pm.to, pm.prefix);
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

    private ImportedDocument importDoc(File xml, Map<String, Mapping> mappings, String documentId) throws DocumentException,
    ParseException {

        Element root = xmlReader.read(xml).getRootElement();

        Mapping generalMapping = mappings.get("general");
        String gpDocumentTypeFrom = generalMapping.requirePropMappingTo(MAPPINGS_PROP_TO_DOCUMENT_TYPE_FROM).from;
        String gpRegNumberWithoutIndividual = generalMapping.requirePropMappingTo(MAPPINGS_PROP_TO_REG_NUMBER_WITHOUT_INDIVIDUAL).from;

        String suffixType = getElementTextByAttribudeName(root, gpDocumentTypeFrom);
        String type = root.attributeValue("form");
        Mapping mapping = null;
        
        if (StringUtils.isNotBlank(type) && StringUtils.isNotBlank(suffixType)) {
            mapping = mappings.get(type.trim() + " " + suffixType.trim());
        }
        if (mapping == null) {
            mapping = mappings.get(type);
        }
        if (mapping == null) {
            // Skip document for which mapping doesn't exist
            return new ImportedDocument(documentId, null, getElementTextByAttribudeName(root, gpRegNumberWithoutIndividual),
                    null,
                    type,
                    suffixType, "documentType mapping not found", null, null);
        }
        
        Assert.notNull(getElementTextByAttribudeName(root, GP_ELEMENT_CREATION_DATE), "CreationDate must not be null");
        
        NodeRef parentRef = findCaseFile(root);
        updateSeriesDocumentTypes(parentRef, mapping.typeInfo.name);   
        
        String shortRegNumber = null;
        String regNumber = getElementTextByAttribudeName(root, gpRegNumberWithoutIndividual);
        String caseNumber = getElementTextByAttribudeName(root, GP_ELEMENT_CASE_NUMBER);
        if (StringUtils.isNotBlank(regNumber) && regNumber.contains("-")) {
        	shortRegNumber = StringUtils.substringAfterLast(regNumber, "-");
        }else if (StringUtils.isNotBlank(regNumber) && regNumber.contains("/")) {
        	shortRegNumber = StringUtils.substringAfterLast(regNumber, "/");
        }

        
        Map<QName, Serializable> propsMap = setProps(root, mapping, regNumber, shortRegNumber, parentRef);

        DocumentDynamic doc = getDocumentDynamicService().createNewDocument(mapping.typeInfo.docVer, parentRef, false).getFirst();
        NodeRef documentRef = doc.getNodeRef();
        nodeService.addAspect(documentRef, DocumentCommonModel.Aspects.SEARCHABLE, null);

        Map<NodeRef, Map<QName, NodeRef>> parentRefsByVolumeRef = new HashMap<NodeRef, Map<QName, NodeRef>>();
        Map<QName, NodeRef> parentRefs  = documentService.getDocumentParents(documentRef);
        parentRefsByVolumeRef.put(parentRef, parentRefs);
        
        propsMap.putAll(parentRefs);

        checkProps(propsMap, null, mapping.typeInfo.propDefs);
        
        Pair<DynamicPropertyDefinition, Field> docNameAdrPropDefinition = mapping.typeInfo.propDefs.get(DocumentDynamicModel.Props.DOC_NAME_ADR.getLocalName());
        if (docNameAdrPropDefinition != null && docNameAdrPropDefinition.getSecond() != null) {
        	String docNameAdrDefualt = docNameAdrPropDefinition.getSecond().getClassificatorDefaultValue();
        	if (StringUtils.isNotBlank(docNameAdrDefualt)) {
        		propsMap.put(DocumentDynamicModel.Props.DOC_NAME_ADR, docNameAdrDefualt);
        	}
        }

        mapChildren(root, mapping, doc.getNode(), new QName[] {}, mapping.typeInfo.propDefs);

        addDocumentLogs(documentRef, root, documentId);
        
        addWorkflowItems(documentRef, root, propsMap, mapping);
        
        propsMap.put(DocumentCommonModel.Props.DOCUMENT_IS_IMPORTED,
                Boolean.valueOf(DocumentStatus.FINISHED.getValueName().equals(propsMap.get(DocumentCommonModel.Props.DOC_STATUS))));

        doc.getNode().getProperties().putAll(RepoUtil.toStringProperties(propsMap));
        doc.getNode().getProperties().put(DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED.toString(), Boolean.TRUE);
        getDocumentDynamicService().updateDocument(doc, Arrays.asList("goproImporter"), false, true);

        addAssociations(documentRef, documentId, root);
        

        LogEntry logEntry = new LogEntry();
        logEntry.setComputerIp("127.0.0.1");
        logEntry.setComputerName("localhost");
        logEntry.setLevel(LogObject.DOCUMENT.getLevel());
        logEntry.setObjectName(LogObject.DOCUMENT.getObjectName());
        logEntry.setCreatorId("IMPORT");
        logEntry.setCreatorName("IMPORT");
        logEntry.setEventDescription("Dokumendi importimine");
        logEntry.setObjectId(documentRef.toString());
        BeanHelper.getLogService().addLogEntry(logEntry);
        
        String fileNames = addFiles(documentId, documentRef, root);
        
        return new ImportedDocument(documentId, documentRef, regNumber,
                shortRegNumber, type,
                suffixType, mapping.typeInfo.docVer.getParent().getId(), fileNames, nodeService.getProperties(documentRef));
    }
    
    private void updateSeriesDocumentTypes(NodeRef caseFileRef, String docTypeName) {
    	NodeRef seriesRef = (NodeRef)getNodeService().getProperty(caseFileRef, DocumentCommonModel.Props.SERIES);
    	List<String> docTypes = (List<String>) getNodeService().getProperty(seriesRef, SeriesModel.Props.DOC_TYPE);
    	if (!docTypes.contains(docTypeName)) {
    		docTypes.add(docTypeName);
    		getNodeService().setProperty(seriesRef, SeriesModel.Props.DOC_TYPE, (Serializable)docTypes);
    	}
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
                if (!("additionalRecipientName".equals(propDef.getName().getLocalName()) || "partyName".equals(propDef.getName().getLocalName()) || "partyContactPerson".equals(propDef.getName().getLocalName())) && !propDef.isMultiValued()) {
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
            if ("contractParty".equals(childAssocType.getLocalName())) {
            	List <Map<QName, Serializable>> contractPartiesProps = new ArrayList<Map<QName, Serializable>>();
            	int partyIndex = 1;
            	boolean notEmptyProps = true;
            	while (notEmptyProps) {
            		Map<QName, Serializable> contractPartyPropsMap = mapProperties(root, subMapping, partyIndex);
            		notEmptyProps = isNotEmptyProps(contractPartyPropsMap);
            		if (notEmptyProps) {
            			partyIndex++;
            			contractPartiesProps.add(contractPartyPropsMap);
            		}
            	}
            	for (int i = 0; i < contractPartiesProps.size() - childNodes.size(); i++) {
            		getDocumentDynamicService().createChildNodesHierarchyAndSetDefaultPropertyValues(node, hierarchy, mapping.typeInfo.docVer);
            	}
            	if (contractPartiesProps.size() > 0) {
	            	childNodes = node.getAllChildAssociations(childAssocType);
	            	Assert.isTrue(contractPartiesProps.size() == childNodes.size());
	            	for (int i = 0; i < contractPartiesProps.size(); i++) {
	            		Map<QName, Serializable> contractPartyPropsMap = contractPartiesProps.get(i);
	            		Node childNode = childNodes.get(i);
	            		checkProps(contractPartyPropsMap, hierarchy, propDefs);
	            		childNode.getProperties().putAll(RepoUtil.toStringProperties(contractPartyPropsMap));
	            	}
            	}
            } else {
	            if (index >= childNodes.size()) {
	                getDocumentDynamicService().createChildNodesHierarchyAndSetDefaultPropertyValues(node, hierarchy, mapping.typeInfo.docVer);
	            }
	            Node childNode = childNodes.get(index);
	            childNodesProcessed.put(childAssocType, index + 1);
	
	            Map<QName, Serializable> propsMap = mapProperties(root, subMapping, 0);
	            checkProps(propsMap, hierarchy, propDefs);
	
	            childNode.getProperties().putAll(RepoUtil.toStringProperties(propsMap));
	
	            mapChildren(root, subMapping, childNode, hierarchy, propDefs);
            }
        }
    }
    
    private boolean isNotEmptyProps(Map<QName, Serializable> propsMap) {
    	String partyNames = (String)propsMap.get(DocumentSpecificModel.Props.PARTY_NAME);
    	String partyContactPersons = (String)propsMap.get(DocumentSpecificModel.Props.PARTY_CONTACT_PERSON);
    	if (StringUtils.isNotBlank(partyNames) || StringUtils.isNotBlank(partyContactPersons)) {
    		return true;
    	}
    	return false;
    }

    private Map<QName, Serializable> setProps(Element root, Mapping mapping, String regNumber, String shortRegNumber, NodeRef caseFileRef) {
        Map<QName, Serializable> propsMap = mapProperties(root, mapping, 0);

        if (StringUtils.isNotBlank(shortRegNumber)) {
            propsMap.put(DocumentCommonModel.Props.REG_NUMBER, regNumber);
            propsMap.put(DocumentCommonModel.Props.SHORT_REG_NUMBER, shortRegNumber);
            Assert.notNull(propsMap.get(DocumentCommonModel.Props.REG_DATE_TIME), "regDateTime must not be null");
            Assert.hasText((String) propsMap.get(DocumentCommonModel.Props.REG_NUMBER), "regNumber must not be empty");
        } else {
            propsMap.remove(DocumentCommonModel.Props.REG_NUMBER);
            propsMap.remove(DocumentCommonModel.Props.SHORT_REG_NUMBER);
            propsMap.remove(DocumentCommonModel.Props.REG_DATE_TIME);
        }

        setDocumentStatus(root, propsMap);
        
        setAccessRestriction(propsMap, root, mapping);

        setOwnerProperties(propsMap, root, mapping, caseFileRef);

        Assert.hasText((String) propsMap.get(DocumentCommonModel.Props.DOC_NAME), "docName cannot be blank");

        setStorageType(root, propsMap);

        return propsMap;
    }

    private void setDocumentStatus(Element root, Map<QName, Serializable> propsMap) {
    	String docStatus = (String) propsMap.get(DocumentCommonModel.Props.DOC_STATUS);
        Mapping docStatusMapping = mappings.get("docStatusValues");
        if (docStatusMapping != null) {
            for (PropMapping propMapping : docStatusMapping.props) {
                if (propMapping.from.equals(docStatus)) {
                	docStatus = propMapping.to;
                    break;
                }
            }
        }
        propsMap.put(DocumentCommonModel.Props.DOC_STATUS, docStatus);
    }
    
    private void setStorageType(Element root, Map<QName, Serializable> propsMap) {
    	String storageType = (String) propsMap.get(DocumentCommonModel.Props.STORAGE_TYPE);
        Mapping storageTypeMapping = mappings.get("storageTypeValues");
        if (storageTypeMapping != null) {
            for (PropMapping propMapping : storageTypeMapping.props) {
                if (propMapping.from.equals(storageType)) {
                	storageType = propMapping.to;
                    break;
                }
            }
        }
        propsMap.put(DocumentCommonModel.Props.STORAGE_TYPE, storageType);
    }

    private NodeRef findCompletedDoc(String id) {
        NodeRef res = completedDocumentsMap.get(id);
        return res;
    }
    
    private NodeRef findCaseFile(Element root) {
    	NodeRef caseFileNodeRef = null;
    	Element caseNumber = getElementByAttribudeName(root, GP_ELEMENT_CASE_NUMBER);
    	if (caseNumber != null) {
    		String caseMark = caseNumber.getText().trim();
    		GoProVolume goProVolume = completedCaseFiles.get(caseMark);
    		if (goProVolume != null) {
    			caseFileNodeRef = goProVolume.getNodeRef();
    		}
    		if (caseFileNodeRef == null) {
	    		List<NodeRef> caseFileRefs = findCaseFiles(storeRefs, caseMark);
	    		if (caseFileRefs.isEmpty() || caseFileRefs.size() > 1) {
	    			throw new RuntimeException("Could not find volume for document, searched based on caseNumber=" + caseNumber.getStringValue());
	    		} else {
	    			caseFileNodeRef = caseFileRefs.get(0);
	    		}
    		}
    	} else {
    		Element docCat = getElementByAttribudeName(root, GP_ELEMENT_DOC_CAT);
    		Element dateSet = getElementByAttribudeName(root, GP_ELEMENT_CREATION_DATE);
    		if (docCat == null || StringUtils.isBlank(docCat.getText()) || dateSet == null || StringUtils.isBlank(dateSet.getText())) {
    			throw new RuntimeException("Could not find volume for document, both caseNumber and (docCat and/or dateSet) are missing");
    		} else {
    			String seriesIdentifier = StringUtils.substringBefore(docCat.getText(), " ").trim();
    			if (seriesMappings.containsKey(seriesIdentifier)) {
    				seriesIdentifier = seriesMappings.get(seriesIdentifier);
    			}
    			Date dateSetDate = null;
    			try {
    				dateSetDate = dateFormat.parse(dateSet.getText());
    			} catch (Throwable t) {
    				throw new RuntimeException("Could not find volume for document, invalid dateSet=" + dateSet.getText());
    			}
    			
    			List<GoProVolume> volumes = completedVolumes.get(seriesIdentifier);
    			if (volumes != null) {
    				for (GoProVolume volume: volumes) {
    					if ((dateSetDate.after(volume.validFrom) || dateSetDate.equals(volume.validFrom))  && 
    							(volume.validTo == null || dateSetDate.before(volume.validTo) || dateSetDate.equals(volume.validTo))) {
    						caseFileNodeRef = volume.getNodeRef();
    						break;
    					}
    				}	
    			}
    			if (caseFileNodeRef == null) {
	    			List<NodeRef> seriesRefs = findSeries(storeRefs, seriesIdentifier);
	    			if (seriesRefs.isEmpty()) {
	    				throw new RuntimeException("Could not find volume for document, searched based on seriesIdentifier = " + seriesIdentifier);
	    			} else {
	    				List<NodeRef> caseFileRefs = findAnnualFiles(storeRefs, seriesRefs, dateSetDate);
	    				if (caseFileRefs.isEmpty() || caseFileRefs.size() > 1) {
	    					throw new RuntimeException("Could not find volume for document, searched based on docCat = " + docCat.getText() + ", and creationDate = " + dateSet.getText());
	    	    		} else {
	    	    			caseFileNodeRef = caseFileRefs.get(0);
	    	    		}
	    			}
    			}
    		}
    	}
    	return caseFileNodeRef;
    }
    
    private List<NodeRef> findSeries(Collection<StoreRef> storeRefs, String seriesIdentifier) {
    	String query = SearchUtil.generateTypeQuery(SeriesModel.Types.SERIES) + " AND " +SearchUtil.generatePropertyExactQuery(SeriesModel.Props.SERIES_IDENTIFIER, seriesIdentifier);
    	List<NodeRef> seriesRefs = BeanHelper.getDocumentSearchService().searchByQuery(storeRefs, query, "goProImportedSeries");
    	return seriesRefs;
    }
    
    private List<NodeRef> findCaseFiles(Collection<StoreRef> storeRefs, String volumeMark) {
    	String query = SearchUtil.generateTypeQuery(CaseFileModel.Types.CASE_FILE) + " AND "
    			+ SearchUtil.generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE) + " AND "
    			+ SearchUtil.generatePropertyExactQuery(VolumeModel.Props.VOLUME_MARK, volumeMark);
    	List<NodeRef> caseFileRefs = BeanHelper.getDocumentSearchService().searchByQuery(storeRefs, query, "goProCaseFiles");
    	return caseFileRefs;
    }
    
    private List<NodeRef> findAnnualFiles(Collection<StoreRef> storeRefs, List<NodeRef> seriesRefs, Date dateSet) {
    	String query = SearchUtil.generateTypeQuery(VolumeModel.Types.VOLUME) + " AND "
    			+ generateSearchSeriesNodeRefs(seriesRefs) + " AND "
    			+ generateDateSetPart(dateSet);
    	List<NodeRef> annualFilesRefs = BeanHelper.getDocumentSearchService().searchByQuery(storeRefs, query, "goProAnnualFiles");
    	return annualFilesRefs;
    }

    private String generateSearchSeriesNodeRefs(List<NodeRef> seriesRefs) {
    	String seriesPart = "(";
    	for (NodeRef seriesRef: seriesRefs) {
    		if (seriesPart.length() > 2) {
    			seriesPart += " OR " + "@\\{http\\://alfresco.webmedia.ee/model/document/dynamic/1.0\\}series:\"" + seriesRef.toString() + "\"";
    		} else {
    			seriesPart += "@\\{http\\://alfresco.webmedia.ee/model/document/dynamic/1.0\\}series:\"" + seriesRef.toString() + "\"";
    		}
    	}
    	seriesPart += ")";
    	
    	return seriesPart;
    }
    
    private String generateDateSetPart(Date dateSet) {
    	String queryPart = "("
		+ SearchUtil.generateDatePropertyRangeQuery(null, dateSet, VolumeModel.Props.VALID_FROM) + " AND ("
		+ SearchUtil.generateDatePropertyRangeQuery(dateSet, null, VolumeModel.Props.VALID_TO) + " OR "
		+ SearchUtil.generatePropertyNullQuery(VolumeModel.Props.VALID_TO)
    	+ "))";
    	
    	return queryPart;
    }
    
    
    protected void addDocumentLogs(NodeRef docRef, Element root, String goProDocId) {
    	Element docCommentsList = getElementByAttribudeName(root, GP_ELEMENT_DOC_COMMENTS_LIST);
        if (docCommentsList == null || StringUtils.isBlank(docCommentsList.getText())) {
            return;
        }
        String [] docComments = docCommentsList.getText().split(";");
        for (String docComment: docComments) {
        	Date dateCreated = null;
        	String dateStr = StringUtils.substringBefore(docComment, "-");
        	try {
        		dateCreated = dateTimeFormat.parse(dateStr);
        	} catch (Throwable t) {
        		log.info("Could not get document log created date from docCommnet = " + docComment + " for goPro document = " + goProDocId);
        		continue;
        	}
        	String secondPart = StringUtils.substringAfter(docComment, "-");
        	String creatorName = StringUtils.substringBefore(secondPart, "-");
        	String description = StringUtils.substringAfter(secondPart, "-");
        	
        	if (StringUtils.isBlank(creatorName) || StringUtils.isBlank(description) || dateCreated == null) {
        		log.info("document log entry is not complete: created date = " + dateStr + ", creatorName = " + creatorName + ", description = " + description + " from docCommnet = " + docComment + " for goPro document = " + goProDocId);
        		continue;
        	}
	    	LogEntry logEntry = new LogEntry();
	        logEntry.setComputerIp("127.0.0.1");
	        logEntry.setComputerName("localhost");
	        logEntry.setLevel(LogObject.DOCUMENT.getLevel());
	        logEntry.setObjectName(LogObject.DOCUMENT.getObjectName());
	        logEntry.setCreatorId("IMPORT");
	        logEntry.setCreatorName(creatorName.trim());
	        logEntry.setEventDescription(description.trim());
	        logEntry.setObjectId(docRef.toString());
	        BeanHelper.getLogService().addImportedLogEntry(logEntry, dateCreated);
        }
    }
    
    

    
    // [ASSOCS

    protected void loadPostponedAssocs() throws Exception {
        postponedAssocsFile = new File(workFolder, "postponed_assocs.csv");

        postponedAssocs = new TreeMap<String, List<PostponedAssoc>>();

        if (!postponedAssocsFile.exists()) {
            log.info("Skipping loading postponed assocs, file does not exist: " + postponedAssocsFile);
            return;
        }

        log.info("Loading postponed assocs from file " + postponedAssocsFile);

        CsvReader reader = new CsvReader(new BufferedInputStream(new FileInputStream(postponedAssocsFile)), CSV_SEPARATOR, Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                String sourceDocId = reader.get(0);
                String targetDocId = reader.get(1);
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
        postponedAssocsCommited = new TreeMap<String, List<PostponedAssoc>>(postponedAssocs);
    }

    static class PostponedAssoc {
        String sourceDocId;
        String targetDocId;
        AssocType assocType;

        public PostponedAssoc(String sourceDocId, String targetDocId, AssocType assocType) {
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

    protected void addAssociations(NodeRef documentRef, String documentId, Element root) {
        List docLinks = getElementsByAttribudeName(root, GP_ELEMENT_LINK);
        List docParentLinks = getElementsByAttribudeName(root, GP_ELEMENT_PARENT_DOCUMENT_ID);
        if (docLinks != null && docParentLinks != null) {
        	docLinks.addAll(docParentLinks);
        } else if (docLinks == null && docParentLinks != null) {
        	docLinks = docParentLinks;
        }
        if (docLinks != null && !docLinks.isEmpty()) {
            for (Object object : docLinks) {
            	String targetDocumentId = ((Element) object).getText();
                if (StringUtils.isBlank(targetDocumentId)) {
                    continue;
                }
                NodeRef targetDocumentRef = findCompletedDoc(targetDocumentId);
                if (targetDocumentRef == null) {
                    if (documentsMap.get(targetDocumentId) != null || completedDocumentsMap.get(targetDocumentId) != null) {
                        log.warn("Association from documentId=" + documentId + " to non-existent documentId=" + targetDocumentId + " assocType=" + AssocType.DEFAULT.getValueName());
                    }
                    putPostponedAssoc(documentId, targetDocumentId, AssocType.DEFAULT);
                } else {
                    log.debug("Creating assoc " + documentId + " [" + documentRef + "] -> " + targetDocumentId + " [" + targetDocumentRef + "], type=" + AssocType.DEFAULT);
                    createAssoc(documentRef, targetDocumentRef, documentId, targetDocumentId, false);
                }
            }
        }
        
        List caseLinks = getElementsByAttribudeName(root, GP_ELEMENT_CASE_LINK);
        if (caseLinks != null && !caseLinks.isEmpty()) {
            for (Object object : caseLinks) {
            	NodeRef caseFileRef = null;
            	String volumeMark = ((Element) object).getText();
                if (StringUtils.isBlank(volumeMark)) {
                    continue;
                }
                GoProVolume goProVolume = completedCaseFiles.get(volumeMark);
        		if (goProVolume != null) {
        			caseFileRef = goProVolume.getNodeRef();
        		}
        		if (caseFileRef == null) {
		            List<NodeRef> caseFileRefs = findCaseFiles(storeRefs, volumeMark);
		    		if (caseFileRefs.isEmpty() || caseFileRefs.size() > 1) {
		    			throw new RuntimeException("Could not find volume for document, searched based on caseLink=" + volumeMark);
		    		} else {
		    			caseFileRef = caseFileRefs.get(0);
		    		}
        		}
        		if (caseFileRef != null) {
        			log.debug("Creating assoc " + volumeMark + " [" + caseFileRef + "] -> " + documentId + " [" + documentRef + "], type=" + AssocType.DEFAULT);
                    createAssoc(caseFileRef, documentRef, volumeMark, documentId, true);
        		}
            }
        }

        
        List<PostponedAssoc> list = postponedAssocs.get(documentId);
        if (list != null) {
            for (PostponedAssoc postponedAssoc : list) {
                NodeRef sourceDocRef = findCompletedDoc(postponedAssoc.sourceDocId);
                log.debug("Creating assoc " + postponedAssoc.sourceDocId + " [" + sourceDocRef + "] -> " + documentId + " [" + documentRef + "], type=" + postponedAssoc.assocType);
                createAssoc(sourceDocRef, documentRef, postponedAssoc.sourceDocId, documentId, false);
            }
            postponedAssocs.remove(documentId);
        }
    }

    private void createAssoc(NodeRef sourceDocRef, NodeRef targetDocRef, String sourceDocId, String targetDocId, boolean isCaseFileAssoc) {
        QName assocTypeQName;
        if (isCaseFileAssoc) {
        	assocTypeQName = CaseFileModel.Assocs.CASE_FILE_DOCUMENT;
        } else {
            assocTypeQName = DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT;
        }
        if (sourceDocRef == null || !nodeService.exists(sourceDocRef)) {
            log.error("Skipping creating assoc, source does not exist, sourceDocumentId=" + sourceDocId + " targetDocumentId="
                    + targetDocId + " assocType=tavaline sourceDocRef=" + sourceDocRef + " targetDocRef=" + targetDocRef);
            return;
        }
        if (targetDocRef == null || !nodeService.exists(targetDocRef)) {
            log.error("Skipping creating assoc, target does not exist, sourceDocumentId=" + sourceDocId + " targetDocumentId="
                    + targetDocId + " assocType=tavaline sourceDocRef=" + sourceDocRef + " targetDocRef=" + targetDocRef);
            return;
        }
        boolean skip = false;

        // Check that reverse-direction associations are not previously defined, otherwise it is a business rule failure
        List<AssociationRef> targetAssocs = nodeService.getSourceAssocs(sourceDocRef, assocTypeQName);
        for (AssociationRef assocRef : targetAssocs) {
            Assert.isTrue(assocRef.getTargetRef().equals(sourceDocRef), "targetDocRef=" + targetDocRef + ", sourceDocRef=" + sourceDocRef + ", assocRef=" + assocRef);
            // if (sourceDocRef.equals(assocRef.getSourceRef())
            log.debug("Existing target-assoc [" + assocRef.getSourceRef() + "] -> [" + assocRef.getTargetRef() + "], type=" + assocRef.getTypeQName());
            if ((sourceDocRef.equals(assocRef.getSourceRef()) && targetDocRef.equals(assocRef.getTargetRef())) ||
                    (targetDocRef.equals(assocRef.getSourceRef()) && sourceDocRef.equals(assocRef.getTargetRef()))) {
                    log.debug("Skipping this assoc creation");
                    skip = true;
            }
            
        }
        if (!skip) {
	        List<AssociationRef> sourceAssocs = nodeService.getTargetAssocs(targetDocRef, assocTypeQName);
	        for (AssociationRef assocRef : sourceAssocs) {
	            Assert.isTrue(assocRef.getSourceRef().equals(targetDocRef), "targetDocRef=" + targetDocRef + ", sourceDocRef=" + sourceDocRef + ", assocRef=" + assocRef);
	            log.debug("Existing source-assoc [" + assocRef.getSourceRef() + "] -> [" + assocRef.getTargetRef() + "], type=" + assocRef.getTypeQName());
	            if ((sourceDocRef.equals(assocRef.getSourceRef()) && targetDocRef.equals(assocRef.getTargetRef())) ||
	                    (targetDocRef.equals(assocRef.getSourceRef()) && sourceDocRef.equals(assocRef.getTargetRef()))) {
	                    log.debug("Skipping this assoc creation");
	                    skip = true;
	            }
	        }
        }
        
        if (!skip) {
	        // Check that same-direction associations are not previously defined, otherwise nodeService.createAssociation throws AssociationExistsException
	        targetAssocs = nodeService.getSourceAssocs(targetDocRef, assocTypeQName);
	        for (AssociationRef assocRef : targetAssocs) {
	            Assert.isTrue(assocRef.getTargetRef().equals(targetDocRef), "targetDocRef=" + targetDocRef + ", sourceDocRef=" + sourceDocRef + ", assocRef=" + assocRef);
	            log.debug("Existing target-assoc [" + assocRef.getSourceRef() + "] -> [" + assocRef.getTargetRef() + "], type=" + assocRef.getTypeQName());
	            if ((sourceDocRef.equals(assocRef.getSourceRef()) && targetDocRef.equals(assocRef.getTargetRef())) ||
	                    (targetDocRef.equals(assocRef.getSourceRef()) && sourceDocRef.equals(assocRef.getTargetRef()))) {
	                    log.debug("Skipping this assoc creation");
	                    skip = true;
	            }
	        }
        }
        if (!skip) {
        	List<AssociationRef> sourceAssocs = nodeService.getTargetAssocs(sourceDocRef, assocTypeQName);
	        for (AssociationRef assocRef : sourceAssocs) {
	            Assert.isTrue(assocRef.getSourceRef().equals(sourceDocRef), "targetDocRef=" + targetDocRef + ", sourceDocRef=" + sourceDocRef + ", assocRef=" + assocRef);
	            log.debug("Existing source-assoc [" + assocRef.getSourceRef() + "] -> [" + assocRef.getTargetRef() + "], type=" + assocRef.getTypeQName());
	            if ((sourceDocRef.equals(assocRef.getSourceRef()) && targetDocRef.equals(assocRef.getTargetRef())) ||
	                    (targetDocRef.equals(assocRef.getSourceRef()) && sourceDocRef.equals(assocRef.getTargetRef()))) {
	                    log.debug("Skipping this assoc creation");
	                    skip = true;
	            }
	        }
        }

        if (!skip) {
            nodeService.createAssociation(sourceDocRef, targetDocRef, assocTypeQName);
        }
    }

    private void putPostponedAssoc(String sourceDocId, String targetDocId, AssocType assocType) {
        List<PostponedAssoc> list = postponedAssocs.get(targetDocId);
        if (list == null) {
            list = new ArrayList<PostponedAssoc>();
            postponedAssocs.put(targetDocId, list);
        }
        list.add(new PostponedAssoc(sourceDocId, targetDocId, assocType));
    }

    // ASSOCS]
    
    

    // HELPER METHODS
    /** RetryingTransactionHelper that only tries to do things once. */
    private RetryingTransactionHelper getTransactionHelper() {
        RetryingTransactionHelper helper = new RetryingTransactionHelper();
        helper.setMaxRetries(1);
        helper.setTransactionService(transactionService);
        return helper;
    }
    
    private String getElementTextByAttribudeName(Element root, String name) {
    	List fields = root.elements(GP_MAIN_ELEMENT_FIELD);
    	for (Object field: fields) {
    		String fieldName = ((Element)field).attributeValue(GP_MAIN_ATTRIBUTE_NAME);
    		if (name.equalsIgnoreCase(fieldName)) {
    			return ((Element)field).getStringValue();
    		}
    	}
    	return null;
    }
    
    private Element getElementByAttribudeName(Element root, String name) {
    	List fields = root.elements(GP_MAIN_ELEMENT_FIELD);
    	for (Object field: fields) {
    		String fieldName = ((Element)field).attributeValue(GP_MAIN_ATTRIBUTE_NAME);
    		if (name.equalsIgnoreCase(fieldName)) {
    			return (Element)field;
    		}
    	}
    	return null;
    }
    
    private List<Element> getElementsByAttribudeName(Element root, String name) {
    	List fields = root.elements(GP_MAIN_ELEMENT_FIELD);
    	List<Element> foundFields = new ArrayList<Element>();
    	for (Object field: fields) {
    		String fieldName = ((Element)field).attributeValue(GP_MAIN_ATTRIBUTE_NAME);
    		if (name.equalsIgnoreCase(fieldName)) {
    			foundFields.add((Element)field);
    		}
    	}
    	return foundFields;
    }
    
    protected void addWorkflowItems(NodeRef docRef, Element root, Map<QName, Serializable> docProps, Mapping mapping) throws ParseException {
        String approvalDescription = getElementTextByAttribudeName(root, GP_ELEMENT_WF_APPROVAL_DESCRIPTION);
        if (StringUtils.isBlank(approvalDescription)) {
            return;
        }
        String approvalHistory = getElementTextByAttribudeName(root, GP_ELEMENT_WF_APPROVAL_HISTORY);
        String processedReviewers = getElementTextByAttribudeName(root, GP_ELEMENT_WF_PROCESSED_REVIEWERS);
        String appStart = getElementTextByAttribudeName(root, GP_ELEMENT_WF_APPSTART);
        
        Date startedDateTime = null;
        Date finishedDueDateDateTime = null;
        Date inProgressDueDateDateTime = DateUtils.addDays(new Date(), 1);
        try {
        	startedDateTime = (appStart.length() == 7)?dateTimeFormatdMMyy.parse(appStart + " 00:00:00"):dateTimeFormatddMMyy.parse(appStart + " 00:00:00");
        	finishedDueDateDateTime = (appStart.length() == 7)?dateTimeFormatdMMyy.parse(appStart + " 23:59:59"):dateTimeFormatddMMyy.parse(appStart + " 23:59:59");
        } catch (ParseException e) {
            throw new RuntimeException("Unable to parse APPSTART: " + appStart + ", error: " + e.getMessage(), e);
        }
        
        LogEntry logEntry = new LogEntry();
        logEntry.setComputerIp("127.0.0.1");
        logEntry.setComputerName("localhost");
        logEntry.setLevel(LogObject.DOCUMENT.getLevel());
        logEntry.setObjectName(LogObject.DOCUMENT.getObjectName());
        logEntry.setCreatorId("IMPORT");
        logEntry.setCreatorName((String)docProps.get(DocumentCommonModel.Props.OWNER_NAME));
        logEntry.setEventDescription(approvalHistory);
        logEntry.setObjectId(docRef.toString());
        BeanHelper.getLogService().addImportedLogEntry(logEntry, startedDateTime);
        
        
        Map<String, ReviewTask> reviewTasks = parseReviewTasks(approvalHistory);
        Set<String> reviewers = parseReviewers(processedReviewers);
        
        boolean finishedWf = isFinishedWf(reviewers, reviewTasks.keySet());
        Date wfFinishDateTime = (finishedWf)?getMaxTaskFinishDate(reviewTasks):null;
        
        Pair<NodeRef, Map<QName,Serializable>> wfPair = doWorkflow(docRef, docProps, startedDateTime, approvalDescription, wfFinishDateTime);
        NodeRef wfRef = wfPair.getFirst();
        int taskIndex = 0;
        
        Map<QName, Serializable> taskSearchableProps = wfPair.getSecond();
        
        boolean responsibleActiveSet = false;
        NodeRef firstTaskRef = null;
        String firstTaskOwnerId = null;

        boolean reviewToOtherOrgEnabled = BeanHelper.getWorkflowConstantsBean().isReviewToOtherOrgEnabled();
        String creatorInstitutionCode = institutionCode;
        String creatorInstitutionName = BeanHelper.getParametersService().getStringParameter(Parameters.TASK_OWNER_STRUCT_UNIT);
        
    	Map<QName, Serializable> props = new HashMap<QName, Serializable>();
    	

        
        WorkflowType workflowType = BeanHelper.getWorkflowConstantsBean().getWorkflowTypes().get(WorkflowSpecificModel.Types.REVIEW_WORKFLOW);
        for (String reviewer : reviewers) {
            ReviewTask reviewTaskInfo = reviewTasks.get(reviewer);
        	if (reviewTaskInfo == null) {
        		reviewTaskInfo = new ReviewTask();
        	}
            
        	String userId = defaultOwnerId;
    		String userName = defaultOwnerName;
    		String email = defaultOwnerEmail;
        	
            String institutionName = null;
            
            
    		
        	Node userNode = null;
    		List<Node> userNodes = getUserService().searchUsers(reviewer, false, -1);
    		if (userNodes != null && userNodes.size() == 1) {
    			userNode = userNodes.get(0);
    		}
    		
        	if (userNode != null) {
        		userId = (String)userNode.getProperties().get(ContentModel.PROP_USERNAME);
        		userName = reviewer;
        		email = (String)userNode.getProperties().get(ContentModel.PROP_EMAIL);
        	} else if (reviewTaskInfo.getCompletedDateTime() != null) {
        		userId = DUMMY_USER_ID;
        		userName = reviewer;
        	}
        	
        	if (!DUMMY_USER_ID.equals(userId)) {
        		OrganizationStructure organizationStructure = getOwnerInstitution(userId);
        		institutionName = organizationStructure != null ? organizationStructure.getName() : null;
        	}
        	
            props = new HashMap<QName, Serializable>();
            props.put(WorkflowSpecificModel.Props.CREATOR_EMAIL, docProps.get(DocumentCommonModel.Props.OWNER_EMAIL));
            props.put(WorkflowSpecificModel.Props.CREATOR_ID, docProps.get(DocumentCommonModel.Props.OWNER_ID));
            props.put(WorkflowCommonModel.Props.CREATOR_NAME, docProps.get(DocumentCommonModel.Props.OWNER_NAME));
            props.put(WorkflowCommonModel.Props.DOCUMENT_TYPE, docProps.get(DocumentAdminModel.Props.OBJECT_TYPE_ID));
            props.put(WorkflowCommonModel.Props.OWNER_EMAIL, email);
            props.put(WorkflowCommonModel.Props.OWNER_ID, userId);
            props.put(WorkflowCommonModel.Props.OWNER_NAME, userName);            
            
            if (reviewToOtherOrgEnabled) {
            	props.put(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_CODE, creatorInstitutionCode);
            	props.put(WorkflowSpecificModel.Props.CREATOR_INSTITUTION_NAME, creatorInstitutionName);
            	props.put(WorkflowSpecificModel.Props.INSTITUTION_NAME, institutionName);
            
            }
            
            props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, startedDateTime);
            props.put(WorkflowCommonModel.Props.STOPPED_DATE_TIME, null);
            props.put(WorkflowCommonModel.Props.STATUS, (reviewTaskInfo.getCompletedDateTime() != null)?Status.FINISHED.getName():Status.IN_PROGRESS.getName());
            props.put(WorkflowSpecificModel.Props.DUE_DATE, (reviewTaskInfo.getCompletedDateTime() != null)?finishedDueDateDateTime:inProgressDueDateDateTime);
            props.put(WorkflowSpecificModel.Props.DUE_DATE_DAYS, null);
            props.put(WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS, null);
            props.put(WorkflowSpecificModel.Props.RESOLUTION, approvalDescription);
            props.put(WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION, approvalDescription);
            props.put(WorkflowCommonModel.Props.OUTCOME, reviewTaskInfo.getOutcome());
            props.put(WorkflowCommonModel.Props.COMPLETED_DATE_TIME, reviewTaskInfo.completedDateTime);
            props.put(WorkflowSpecificModel.Props.COMMENT, (StringUtils.isBlank(reviewTaskInfo.comment))?"":reviewTaskInfo.getComment());

            props.putAll(taskSearchableProps);
            Task task = BeanHelper.getWorkflowService().createTaskInMemory(wfRef, workflowType, props);
            Set<QName> aspects = task.getNode().getAspects();
            aspects.add(WorkflowSpecificModel.Aspects.SEARCHABLE);

            if (firstTaskRef == null && reviewTaskInfo.getCompletedDateTime() == null) {
                firstTaskRef = task.getNodeRef();
                firstTaskOwnerId = userId;
            }
            if (!responsibleActiveSet && reviewTaskInfo.getCompletedDateTime() == null && userId.equals(docProps.get(OWNER_ID))) {
                task.getNode().getProperties().put(WorkflowSpecificModel.Props.ACTIVE.toString(), Boolean.TRUE);
                aspects.add(WorkflowSpecificModel.Aspects.RESPONSIBLE);
                responsibleActiveSet = true;
                getPrivilegeService().setPermissions(docRef, userId, Privilege.EDIT_DOCUMENT);
            } else {
                getPrivilegeService().setPermissions(docRef, userId, Privilege.VIEW_DOCUMENT_FILES);
            }
            task.setTaskIndexInWorkflow(taskIndex++);
            BeanHelper.getWorkflowDbService().createTaskEntry(task, wfRef);
            
            if (finishedWf) {
            	docProps.put(DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS, Boolean.TRUE);
            } else {
            	docProps.put(DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, Boolean.TRUE);
            }
            //docProps.put(DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName());
        }
    
        if (!responsibleActiveSet && firstTaskRef != null) {
            props = new HashMap<QName, Serializable>();
            props.put(WorkflowSpecificModel.Props.ACTIVE, Boolean.TRUE);
            getPrivilegeService().setPermissions(docRef, firstTaskOwnerId, Privilege.EDIT_DOCUMENT);
            BeanHelper.getWorkflowDbService().updateTaskProperties(firstTaskRef, props);
        }
	}
    
    private boolean isFinishedWf(Set<String> reviewers, Set<String> finishedReviewers) {
    	if (reviewers.size() > finishedReviewers.size()) {
    		return false;
    	}
    	for (String reviewer: reviewers) {
    		if (!finishedReviewers.contains(reviewer)) {
    			return false;
    		}
    	}
    	return true;
    }
    
    private Date getMaxTaskFinishDate(Map<String, ReviewTask> reviewTasks) {
    	Date maxDate = null;
    	for (String reviewer: reviewTasks.keySet()) {
    		ReviewTask task = reviewTasks.get(reviewer);
    		if (maxDate == null) {
    			maxDate = task.getCompletedDateTime();
    		} else if (maxDate.before(task.getCompletedDateTime())){
    			maxDate = task.getCompletedDateTime();
    		}
    	}
    	return maxDate;
    }
    
    private Pair<NodeRef, Map<QName, Serializable>>  doWorkflow(NodeRef docRef, Map<QName, Serializable> docProps, Date startDateTime, String resolution, Date finishedDateTime) {
    	NodeRef wfRef = null;
        int taskIndex = 0;
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        Map<QName, Serializable> taskSearchableProps = null;

        WorkflowType workflowType = BeanHelper.getWorkflowConstantsBean().getWorkflowTypes().get(WorkflowSpecificModel.Types.REVIEW_WORKFLOW);
        
        props = new HashMap<QName, Serializable>();
        props.put(WorkflowCommonModel.Props.CREATOR_NAME, docProps.get(DocumentCommonModel.Props.OWNER_NAME));
        props.put(WorkflowCommonModel.Props.OWNER_ID, docProps.get(DocumentCommonModel.Props.OWNER_ID));
        props.put(WorkflowCommonModel.Props.OWNER_NAME, docProps.get(DocumentCommonModel.Props.OWNER_NAME));
        props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, startDateTime);
        props.put(WorkflowCommonModel.Props.STATUS, (finishedDateTime != null)?Status.FINISHED.getName():Status.IN_PROGRESS.getName());
        props.put(WorkflowCommonModel.Props.STOPPED_DATE_TIME, null);
        if (finishedDateTime != null) {
        	props.put(WorkflowCommonModel.Props.FINISHED_DATE_TIME, finishedDateTime);
        }
        props.put(WorkflowCommonModel.Props.TYPE, CompoundWorkflowType.INDEPENDENT_WORKFLOW.name());
        props.put(WorkflowCommonModel.Props.TITLE, docProps.get(DocumentCommonModel.Props.DOC_NAME));
        NodeRef cwfRef = getNodeService().createNode(
                BeanHelper.getConstantNodeRefsBean().getIndependentWorkflowsRoot(),
                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                WorkflowCommonModel.Types.COMPOUND_WORKFLOW,
                props
                ).getChildRef();
        getNodeService().createAssociation(docRef, cwfRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
        getWorkflowService().updateMainDocument(cwfRef, docRef);

        props = new HashMap<QName, Serializable>();
        props.put(WorkflowCommonModel.Props.CREATOR_NAME, docProps.get(DocumentCommonModel.Props.OWNER_NAME));
        props.put(WorkflowCommonModel.Props.MANDATORY, Boolean.FALSE);
        props.put(WorkflowCommonModel.Props.PARALLEL_TASKS, Boolean.TRUE);
        props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, startDateTime);
        props.put(WorkflowCommonModel.Props.STOPPED_DATE_TIME, null);
        props.put(WorkflowCommonModel.Props.STATUS, (finishedDateTime != null)?Status.FINISHED.getName():Status.IN_PROGRESS.getName());
        props.put(WorkflowCommonModel.Props.STOP_ON_FINISH, Boolean.FALSE);
        props.put(WorkflowSpecificModel.Props.RESOLUTION, resolution);
        wfRef = getNodeService().createNode(
                cwfRef,
                WorkflowCommonModel.Assocs.WORKFLOW,
                WorkflowCommonModel.Assocs.WORKFLOW,
                WorkflowSpecificModel.Types.REVIEW_WORKFLOW,
                props
                ).getChildRef();
        taskSearchableProps = WorkflowUtil.getTaskSearchableProps(props);
        taskSearchableProps.put(WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_ID, cwfRef.getId());

        return new Pair<NodeRef, Map<QName,Serializable>>(wfRef, taskSearchableProps);
    }
    
    private Map<String, ReviewTask> parseReviewTasks(String tasksInfo) {
    	Map<String, ReviewTask> reviewTasks = new HashMap<String, ReviewTask>();
    	if (StringUtils.isNotBlank(tasksInfo)) {
        	String [] reviewerTasksArr = tasksInfo.split(";");
        	for (int i = 0; i < reviewerTasksArr.length; i++) {
        		String taskInfo = reviewerTasksArr[i];
        		String dateTimeStr = StringUtils.substringBefore(taskInfo, " - ").trim();
        		Date completedDateTime = null;
        		try {
        			if (StringUtils.isNotBlank(dateTimeStr) && dateTimeStr.length() == 10) {
        				completedDateTime = dateFormat.parse(dateTimeStr);
        			} else {
        				completedDateTime = dateTimeFormat.parse(dateTimeStr);
        			}
                } catch (ParseException e) {
                	throw new RuntimeException("Unable to parse approval date: " + dateTimeStr + ", taskInfo: " + taskInfo + ", error: " + e.getMessage(), e);
                }
        		String taskInfoSecond = StringUtils.substringAfter(taskInfo, " - ");
        		String name = StringUtils.substringBefore(taskInfoSecond, " - ").trim();
        		String outcomeInfo = StringUtils.substringAfter(taskInfoSecond, " - ");
        		String comment = (outcomeInfo.contains(":"))?StringUtils.substringAfter(outcomeInfo, ":").trim():null;
        		String outcome = (outcomeInfo.toLowerCase().contains("dokument tagasi lükatud:"))?"Kooskõlastamata":"Kooskõlastatud";
        		
        		ReviewTask rTask = new ReviewTask();
        		rTask.setUserName(name);
        		rTask.setCompletedDateTime(completedDateTime);
        		rTask.setOutcome(outcome);
        		rTask.setComment(comment);
        		reviewTasks.put(name, rTask);
        	}
        }
    	return reviewTasks;
        
    }
    
    private Set<String> parseReviewers(String reviewersInfo) {
        Set<String> reviewers = new HashSet<String>();
        if (StringUtils.isNotBlank(reviewersInfo)) {
        	String [] reviewersArr = reviewersInfo.split(";");
        	for (int i = 0; i < reviewersArr.length; i++) {
        		reviewers.add(parseUserFullName(reviewersArr[i]));
        	}
        }
    	return reviewers;
    }
    
    private class ReviewTask {
    	private String userName;
		private Date completedDateTime;
    	private String comment;
    	private String outcome;
    	
    	public String getOutcome() {
			return outcome;
		}
		public void setOutcome(String outcome) {
			this.outcome = outcome;
		}
		public String getUserName() {
			return userName;
		}
		public void setUserName(String userName) {
			this.userName = userName;
		}
		public Date getCompletedDateTime() {
			return completedDateTime;
		}
		public void setCompletedDateTime(Date completedDateTime) {
			this.completedDateTime = completedDateTime;
		}
		public String getComment() {
			return comment;
		}
		public void setComment(String comment) {
			this.comment = comment;
		}
    	
    	
    }
    
    private class GoProVolume {
    	private NodeRef nodeRef;
		private Date validFrom;
		private Date validTo;
    	private String mark;
    	private String seriesIdentifier;
    	private String function;
    	
    	public NodeRef getNodeRef() {
			return nodeRef;
		}
		public void setNodeRef(NodeRef nodeRef) {
			this.nodeRef = nodeRef;
		}
		public Date getValidFrom() {
			return validFrom;
		}
		public void setValidFrom(Date validFrom) {
			this.validFrom = validFrom;
		}
		public Date getValidTo() {
			return validTo;
		}
		public void setValidTo(Date validTo) {
			this.validTo = validTo;
		}
		public String getMark() {
			return mark;
		}
		public void setMark(String mark) {
			this.mark = mark;
		}
		public String getSeriesIdentifier() {
			return seriesIdentifier;
		}
		public void setSeriesIdentifier(String seriesIdentifier) {
			this.seriesIdentifier = seriesIdentifier;
		}
		public String getFunction() {
			return function;
		}
		public void setFunction(String function) {
			this.function = function;
		}    	
    	
    	
    }
    
    private OrganizationStructure getOwnerInstitution(String userId) {
    	Node user = BeanHelper.getUserService().getUser(userId);
        if (user == null) {
            return null;
        }
        String organizationId = (String) user.getProperties().get(ContentModel.PROP_ORGID);
        if (StringUtils.isBlank(organizationId)) {
            return null;
        }
        OrganizationStructure organizationStructure = BeanHelper.getOrganizationStructureService().getOrganizationStructure(organizationId);
        while (organizationStructure != null) {
            String institutionRegCode = organizationStructure.getInstitutionRegCode();
            if (StringUtils.isNotBlank(institutionRegCode)) {
                List<Node> institutions = BeanHelper.getAddressbookService().getContactsByRegNumber(institutionRegCode);
                if (!institutions.isEmpty()) {
                    Map<String, Object> orgProps = institutions.get(0).getProperties();
                    if (Boolean.TRUE.equals(orgProps.get(Props.DVK_CAPABLE))) {
                        // dvk capable organization contact found
                        return organizationStructure;
                    }
                    
                    return null;
                }
                return null;
            }
            organizationStructure = BeanHelper.getOrganizationStructureService().getOrganizationStructure(organizationStructure.getSuperUnitId());
        }
        // organization with non-empty reg code is not found
        return null;
    }

}
