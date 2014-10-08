package ee.webmedia.alfresco.sharepoint;

import static ee.webmedia.alfresco.common.web.BeanHelper.getCaseFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPersonService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSearchService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowDbService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.sharepoint.DocumentImporter.CSV_NAME_COMPLETED_DOCS;
import static ee.webmedia.alfresco.sharepoint.ImportUtil.docsError;
<<<<<<< HEAD
=======
import static ee.webmedia.alfresco.sharepoint.ImportUtil.getString;
>>>>>>> develop-5.1
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import de.schlichtherle.io.FileInputStream;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.casefile.service.CaseFileService;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.sharepoint.entity.Association;
import ee.webmedia.alfresco.sharepoint.entity.CaseFile;
import ee.webmedia.alfresco.sharepoint.entity.CompoundWorkflow;
import ee.webmedia.alfresco.sharepoint.entity.Task;
import ee.webmedia.alfresco.sharepoint.entity.Workflow;
import ee.webmedia.alfresco.sharepoint.entity.mapper.IntegerMapper;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ProgressTracker;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * Class contains CompundWorkflow and related data import logic in general. More exact input data processing is done in database. The scripts for creating database tables and
 * functions and for dropping them later must be placed in <em>dataFolder</em>/sql/ folder: these files are looked for explicitly imp_create_imp_tables.sql and
 * imp_drop_objects.sql, other SQL files are processed in order found.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> develop-5.1
 */
public class WorkflowImporter {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(WorkflowImporter.class);

    private static final String CSV_NAME_FAILED_IMPORT = "failed_import.csv";
    private static final String CSV_NAME_COMPLETED_PROCS = "completed_procedures.csv";
    private static final String CSV_NAME_FAILED_PROCS = "failed_procedures.csv";

    private static final String SQL_FILE_CREATE_TABLES = "imp_create_imp_tables.sql";
    private static final String SQL_FILE_DROP_ALL = "imp_drop_objects.sql";
    private static final String NO_REF = "-";

    // IMPORT SETTINGS
    private final ImportSettings settings;
    private final ImportStatus status;

    // IMPORT STATE DATA
    private final File logFile;
    private final File errorFile;
    private final File failedFile;
    private final File scriptDir;
    private final String baseCompoundWorkflowUrl;
    private final NodeRef compoundWorkflowsParentRef;
    private final LogEntry logEntry = new LogEntry();
<<<<<<< HEAD
=======
    private final Map<String, NodeRef> importedCaseFiles;
>>>>>>> develop-5.1
    private ProgressTracker progressTracker;

    private final SimpleJdbcTemplate jdbcTemplate;
    private final NodeService nodeService = getNodeService();
    private final UserService userService = getUserService();
    private final LogService logService = getLogService();
    private final WorkflowService workflowService = getWorkflowService();
    private final WorkflowDbService workflowDbService = getWorkflowDbService();
    private final CaseFileService caseFileService = getCaseFileService();
    private final SearchService searchService = getSearchService();
    private final GeneralService generalService = getGeneralService();

    public WorkflowImporter(ImportSettings settings, ImportStatus status, SimpleJdbcTemplate jdbcTemplate) {
        this.settings = settings;
        this.status = status;
        this.jdbcTemplate = jdbcTemplate;

        logFile = settings.getWorkFolderFile(CSV_NAME_COMPLETED_PROCS);
        failedFile = settings.getWorkFolderFile(CSV_NAME_FAILED_PROCS);
        errorFile = settings.getWorkFolderFile(CSV_NAME_FAILED_IMPORT);
        scriptDir = settings.getDataFolderFile("sql");

        File volumeLogFile = settings.getWorkFolderFile(StructureImporter.COMPLETED_FILENAME);

        if (errorFile.exists()) {
            throw new RuntimeException("Structure or document import has not completed successfully. Skipping workflow import.");
        }

        if (failedFile.exists()) {
            failedFile.delete();
        }

        if (!scriptDir.exists()) {
            docsError(errorFile, "SQL files directory not found: " + scriptDir);
            throw new RuntimeException("SQL files directory not found");
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
<<<<<<< HEAD
=======
        importedCaseFiles = loadCompletedCaseFiles(volumeLogFile);
>>>>>>> develop-5.1

        baseCompoundWorkflowUrl = BeanHelper.getDocumentTemplateService().getServerUrl() + "/n/" + ExternalAccessPhaseListener.OUTCOME_COMPOUND_WORKFLOW_PROCEDURE_ID + "/";
        compoundWorkflowsParentRef = generalService.getNodeRef(WorkflowCommonModel.Repo.INDEPENDENT_WORKFLOWS_SPACE);

        logEntry.setComputerIp("127.0.0.1");
        logEntry.setComputerName("localhost");
        logEntry.setEventDescription("Terviktöövoo importimine");
        logEntry.setLevel(LogObject.WORKFLOW.getLevel());
        logEntry.setObjectName(LogObject.WORKFLOW.getObjectName());
        logEntry.setCreatorId("IMPORT");
        logEntry.setCreatorName("IMPORT");
    }

    public boolean init() {
        return (Boolean) jdbcTemplate.getJdbcOperations().execute(new ConnectionCallback() {

            @Override
            public Object doInConnection(Connection con) throws SQLException, DataAccessException {
                try {
                    con.setAutoCommit(true);

                    LOG.info("Creating tables...");
                    con.setAutoCommit(true);

                    executeScript(new File(scriptDir, SQL_FILE_CREATE_TABLES), con, true);
                    for (String file : scriptDir.list()) {
                        if (FilenameUtils.isExtension(file, "sql") && !SQL_FILE_CREATE_TABLES.equals(file) && !SQL_FILE_DROP_ALL.equals(file)) {
                            executeScript(new File(scriptDir, file), con, false);
                        }
                    }

                    LOG.info("Writing input data to tables...");
                    writeMenetlused(con);
                    writeDokMenetlused(con);
                    writeIsikMenetlused(con);
                    writeKommentaarid(con);
                    writeLisavaljavaartus(con);
                    writeResolutsioonid(con);
                    writeKasutOstr(con);
                    writeKasutAdsi(con);
                    writeCompletedDocs(con);
                    writeSystemUsers(con);
                    writeCompletedLogFromCsv(con);

                    LOG.info("Executing input data processing logic...");
                    PreparedStatement ps = null;
                    try {
                        ps = con.prepareStatement("SELECT imp_proc(?)");
                        ps.setString(1, settings.getDefaultOwnerId());
                        ps.execute();
                    } finally {
                        closeStmt(ps);
                    }

                    long totalSize = jdbcTemplate.queryForLong("SELECT COUNT(*) FROM imp_completed_procedures");
                    long completedSize = jdbcTemplate.queryForLong("SELECT COUNT(*) FROM imp_completed_procedures WHERE node_ref IS NOT NULL");
                    progressTracker = new ProgressTracker(totalSize, completedSize);

                    LOG.info("Initialization completed.");

                } catch (Exception e) {
                    LOG.warn("An error was detected during workflow import initialization/data reading.", e);
                    ImportUtil.workflowError(errorFile, e);
                    return false;

                } finally {
                    con.setAutoCommit(false);
                }

                return true;
            }
        });
    }

    public boolean doBatch() {
        List<Integer> procIds = jdbcTemplate.query("SELECT procedure_id FROM imp_completed_procedures WHERE node_ref IS NULL LIMIT ?", IntegerMapper.INSTANCE,
                settings.getBatchSize());

        if (procIds.isEmpty()) {
            String info = progressTracker.step(procIds.size());
            if (info != null) {
                LOG.info("Workflows import: " + info);
            }
            return false;
        }

        try {
            outer: for (Integer procId : procIds) {
                status.incrCount();

                List<CompoundWorkflow> compoundWorkflows = jdbcTemplate.query("SELECT * FROM imp_compound_workflow WHERE procedure_id=?", CompoundWorkflow.MAPPER, procId);
                List<Workflow> workflows = jdbcTemplate.query("SELECT * FROM imp_workflow WHERE procedure_id=?", Workflow.MAPPER, procId);
                List<Task> tasks = jdbcTemplate.query("SELECT * FROM imp_task WHERE procedure_id=?", Task.MAPPER, procId);
                List<Association> associations = jdbcTemplate.query("SELECT * FROM imp_association WHERE procedure_id=?", Association.MAPPER, procId);
                List<CaseFile> caseFiles = jdbcTemplate.query("SELECT * FROM imp_casefile WHERE procedure_id=?", CaseFile.MAPPER, procId);

                NodeRef parentRef = null;

                if (!caseFiles.isEmpty()) {
                    final CaseFile caseFile = caseFiles.get(0);

                    String searchCaseFileVolumeMark = StringUtils.stripToEmpty(caseFile.getVolumeMark());
                    if (StringUtils.isNotBlank(searchCaseFileVolumeMark)) {
<<<<<<< HEAD
                        List<String> query = new ArrayList<String>(2);
                        query.add(SearchUtil.generateTypeQuery(CaseFileModel.Types.CASE_FILE));
                        query.add(SearchUtil.generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
                        query.add(SearchUtil.generateStringExactQuery(searchCaseFileVolumeMark, VolumeModel.Props.VOLUME_MARK));
                        String q = SearchUtil.joinQueryPartsAnd(query, false);

                        org.alfresco.service.cmr.search.ResultSet result = searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, q);
                        for (NodeRef nodeRef : result.getNodeRefs()) {
                            if (nodeService.exists(nodeRef)
                                    && searchCaseFileVolumeMark.equalsIgnoreCase(StringUtils.stripToEmpty((String) nodeService.getProperty(nodeRef, VolumeModel.Props.VOLUME_MARK)))) {
                                parentRef = nodeRef;
                                break;
                            }
                        }
                        result.close();
                        if (parentRef == null) {
                            result = searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, q);
                            for (NodeRef nodeRef : result.getNodeRefs()) {
                                if (nodeService.exists(nodeRef)
                                        && searchCaseFileVolumeMark
                                                .equalsIgnoreCase(StringUtils.stripToEmpty((String) nodeService.getProperty(nodeRef, VolumeModel.Props.VOLUME_MARK)))) {
                                    parentRef = nodeRef;
                                    break;
                                }
                            }
                            result.close();
=======
                        parentRef = importedCaseFiles.get(searchCaseFileVolumeMark);
                        if (parentRef == null) {
                            parentRef = importedCaseFiles.get("0" + searchCaseFileVolumeMark);
>>>>>>> develop-5.1
                        }
                    }

                    ee.webmedia.alfresco.casefile.service.CaseFile cf;
                    if (parentRef == null) {

                        if (isNotBlank(settings.getSeriesIdentifierForProcessToCaseFile()) && settings.getCaseFileTypeVersionForProcessToCaseFile() != null) {
                            StoreRef seriesSearchStore;
                            if (caseFile.getValidTo() == null || !caseFile.getValidTo().before(settings.getDocListArchivalsSeparatingDate())) {
                                seriesSearchStore = generalService.getStore();
                            } else {
                                seriesSearchStore = generalService.getArchivalsStoreRef();
                            }

                            String searchSeriesIdentifier = StringUtils.stripToEmpty(settings.getSeriesIdentifierForProcessToCaseFile());
                            NodeRef series = null;
                            List<String> query = new ArrayList<String>(2);
                            query.add(SearchUtil.generateTypeQuery(SeriesModel.Types.SERIES));
                            query.add(SearchUtil.generateStringExactQuery(searchSeriesIdentifier, SeriesModel.Props.SERIES_IDENTIFIER));
                            String q = SearchUtil.joinQueryPartsAnd(query, false);
                            org.alfresco.service.cmr.search.ResultSet result = searchService.query(seriesSearchStore, SearchService.LANGUAGE_LUCENE, q);
                            for (NodeRef nodeRef : result.getNodeRefs()) {
                                if (nodeService.exists(nodeRef) && searchSeriesIdentifier.equalsIgnoreCase(
                                        StringUtils.stripToEmpty((String) nodeService.getProperty(nodeRef, SeriesModel.Props.SERIES_IDENTIFIER)))) {
                                    series = nodeRef;
                                    break;
                                }
                            }
                            result.close();

                            if (series == null) {
<<<<<<< HEAD
                                markCompletedProcedureFailed(procId, 45,
=======
                                markCompletedProcedureFailed(procId, 48,
>>>>>>> develop-5.1
                                        "Asjatoimiku loomiseks ei leita sisendparameetri seriesIdentifierForProcessToCaseFile poolt määratud sarja");
                                continue;
                            }

<<<<<<< HEAD

=======
>>>>>>> develop-5.1
                            if (isBlank(caseFile.getVolumeMark())) {
                                caseFile.setVolumeMark(procId.toString());
                            }
                            if (isBlank(caseFile.getTitle()) || isBlank(caseFile.getVolumeMark()) || isBlank(caseFile.getStatus()) || caseFile.getValidFrom() == null) {
<<<<<<< HEAD
                                markCompletedProcedureFailed(procId, 46, "Asjatoimiku loomiseks vajalikud kohustuslikud andmed on puudulikud");
=======
                                markCompletedProcedureFailed(procId, 49, "Asjatoimiku loomiseks vajalikud kohustuslikud andmed on puudulikud");
>>>>>>> develop-5.1
                                continue;
                            }
                            cf = caseFileService.createNewCaseFile(settings.getCaseFileTypeVersionForProcessToCaseFile(), series, false).getFirst();
                            parentRef = cf.getNodeRef();
                            cf.setProp(DocumentCommonModel.Props.FUNCTION, nodeService.getPrimaryParent(series).getParentRef());
                            cf.setProp(DocumentCommonModel.Props.SERIES, series);
                            cf.setProp(VolumeModel.Props.MARK, caseFile.getVolumeMark());
                            cf.setProp(VolumeModel.Props.VOLUME_TYPE, VolumeType.CASE_FILE.name());
                            cf.setProp(VolumeModel.Props.CONTAINS_CASES, Boolean.FALSE);

                            String ownerId = settings.getDefaultOwnerId();
                            Map<QName, Serializable> userProps = userService.getUserProperties(ownerId);
                            cf.setProp(DocumentCommonModel.Props.OWNER_ID, ownerId);
                            cf.setProp(DocumentCommonModel.Props.OWNER_NAME, UserUtil.getPersonFullName1(userProps));
                            cf.setProp(DocumentCommonModel.Props.OWNER_EMAIL, userProps.get(ContentModel.PROP_EMAIL));
                            cf.setProp(DocumentCommonModel.Props.OWNER_PHONE, userProps.get(ContentModel.PROP_TELEPHONE));
                            cf.setProp(DocumentCommonModel.Props.OWNER_JOB_TITLE, userProps.get(ContentModel.PROP_JOBTITLE));
                            cf.setProp(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT, userProps.get(ContentModel.PROP_ORGANIZATION_PATH));
                            cf.setProp(DocumentDynamicModel.Props.OWNER_SERVICE_RANK, userProps.get(ContentModel.PROP_SERVICE_RANK));
                            cf.setProp(DocumentDynamicModel.Props.OWNER_WORK_ADDRESS, userProps.get(ContentModel.PROP_STREET_HOUSE));

                        } else {
<<<<<<< HEAD
                            markCompletedProcedureFailed(procId, 44, "Asjatoimik ei ole leitav");
=======
                            markCompletedProcedureFailed(procId, 47, "Asjatoimik ei ole leitav");
>>>>>>> develop-5.1
                            continue;
                        }

                    } else {
                        cf = caseFileService.getCaseFile(parentRef);
                    }

                    caseFile.updateProps(cf);

                    if (!compoundWorkflows.isEmpty()) {
                        CompoundWorkflow compoundFlow = compoundWorkflows.get(0);
                        compoundFlow.writeCaseFileOwnerProps(cf, userService);

                        if (!compoundFlow.isStatusWorkInProgress()) {
                            for (ChildAssociationRef assoc : nodeService
                                    .getChildAssocs(cf.getNodeRef(), CaseModel.Associations.CASE_DOCUMENT, CaseModel.Associations.CASE_DOCUMENT)) {
                                nodeService.setProperty(assoc.getChildRef(), DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.FINISHED.getValueName());
                                nodeService.setProperty(assoc.getChildRef(), DocumentCommonModel.Props.DOCUMENT_IS_IMPORTED, Boolean.TRUE);
                            }
                        }
                    } else {
                        LOG.warn("No compound workflow found for case file: " + caseFile.getVolumeMark());
                    }

                    caseFileService.update(cf, null);
                } else {
                    parentRef = compoundWorkflowsParentRef;
                }

                NodeRef compoundFlowRef = null;
                Map<QName, Serializable> props = new HashMap<QName, Serializable>();

                for (CompoundWorkflow compoundFlow : compoundWorkflows) {
                    compoundFlow.writePropsTo(props);

                    compoundFlowRef = nodeService.createNode(parentRef, WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                            WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW, WorkflowCommonModel.Types.COMPOUND_WORKFLOW, props).getChildRef();
                    Map<String, Object> taskSearchableProps = RepoUtil.toStringProperties(WorkflowUtil.getTaskSearchableProps(props));
                    Collections.sort(workflows);
                    for (Workflow workflow : workflows) {
                        workflow.writePropsTo(props);
                        NodeRef flowRef = nodeService.createNode(compoundFlowRef, WorkflowCommonModel.Assocs.WORKFLOW,
                                WorkflowCommonModel.Assocs.WORKFLOW, workflow.getNodeType(), props).getChildRef();

                        Collections.sort(tasks);
                        int taskIndex = 0;

                        WorkflowType workflowType = BeanHelper.getWorkflowService().getWorkflowTypes().get(workflow.getNodeType());
                        for (Task task : tasks) {
                            if ((task.isAssignment() && workflow.isAssignment() || task.isConfirmation() && workflow.isConfirmation())
                                    && ObjectUtils.equals(task.getWorkflowOrder(), workflow.getOrderNo())) {

                                task.writePropsTo(props);

                                ee.webmedia.alfresco.workflow.service.Task taskNode = BeanHelper.getWorkflowService().createTaskInMemory(flowRef, workflowType, props);
                                Set<QName> aspects = taskNode.getNode().getAspects();
                                aspects.add(WorkflowSpecificModel.Aspects.SEARCHABLE);
                                Map<String, Object> deltaTaskProps = taskNode.getNode().getProperties();
                                deltaTaskProps.putAll(taskSearchableProps);

                                // if assignmentWorkflow, then 1 task must have responsible active=true, (and 0-few can have responsible active=false)
                                if (task.isResponsible()) {
                                    deltaTaskProps.put(WorkflowSpecificModel.Props.ACTIVE.toString(), Boolean.TRUE);
                                    aspects.add(WorkflowSpecificModel.Aspects.RESPONSIBLE);
                                }
                                taskNode.setTaskIndexInWorkflow(taskIndex++);
                                workflowDbService.createTaskEntry(taskNode, flowRef);
                            }
                        }
                    }

                    // Validate CWF-FW-TASK statuses
                    try {
                        WorkflowUtil.checkCompoundWorkflow(workflowService.getCompoundWorkflow(compoundFlowRef));
                    } catch (WorkflowChangedException e) {
                        LOG.warn("Failed to validate compoundWorkflow statuses for procedure_id " + procId, e);
                        // Only one compoundWorkflow can be created from one procedure_id
                        nodeService.deleteNode(compoundFlowRef);
<<<<<<< HEAD
                        markCompletedProcedureFailed(procId, 47, "Terviktöövoo, töövoogude ja tööülesannete staatused ei ole omavahel kooskõlas");
=======
                        markCompletedProcedureFailed(procId, 50, "Terviktöövoo, töövoogude ja tööülesannete staatused ei ole omavahel kooskõlas");
>>>>>>> develop-5.1
                        continue outer;
                    }

                    boolean maindDocSet = false;

                    if (!compoundFlow.isCaseFileWorkflow()) {
                        for (Association assoc : associations) {
                            if (assoc.isDocument() && assoc.getFromNode() != null) {
                                final NodeRef fromNode = new NodeRef(assoc.getFromNode());
                                if (!nodeService.exists(fromNode)) {
                                    nodeService.deleteNode(compoundFlowRef);
<<<<<<< HEAD
                                    markCompletedProcedureFailed(procId, 48, "Seotud dokument ei eksisteeri: " + fromNode);
=======
                                    markCompletedProcedureFailed(procId, 51, "Seotud dokument ei eksisteeri: " + fromNode);
>>>>>>> develop-5.1
                                    continue outer;
                                }
                                nodeService.createAssociation(fromNode, compoundFlowRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
                                nodeService.setProperty(fromNode, DocumentCommonModel.Props.SEARCHABLE_HAS_STARTED_COMPOUND_WORKFLOWS, true);

                                if (compoundFlow.isStatusWorkInProgress()) {
                                    nodeService.setProperty(fromNode, DocumentCommonModel.Props.DOC_STATUS, DocumentStatus.WORKING.getValueName());
                                    nodeService.setProperty(fromNode, DocumentCommonModel.Props.DOCUMENT_IS_IMPORTED, Boolean.FALSE);
                                }

                                if (compoundFlow.isStatusWorkInProgress()) {
                                    nodeService.setProperty(fromNode, DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS, false);
                                } else if (nodeService.getProperty(fromNode, DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS) == null) {
                                    nodeService.setProperty(fromNode, DocumentCommonModel.Props.SEARCHABLE_HAS_ALL_FINISHED_COMPOUND_WORKFLOWS, true);
                                }

                                if (assoc.isMainDocument()) {
                                    if (maindDocSet) {
                                        throw new RuntimeException("Main document is already set on given compound workflow.");
                                    }

                                    nodeService.setProperty(compoundFlowRef, WorkflowCommonModel.Props.MAIN_DOCUMENT, fromNode);
                                    maindDocSet = true;
                                }
                            } else if (!compoundFlow.isCaseFileWorkflow() && (assoc.isWorkflow() || assoc.isRelatedUrl())) {
                                props.clear();

                                if (assoc.isRelatedUrl()) {
                                    props.put(WorkflowCommonModel.Props.URL, assoc.getFromNode());
                                    props.put(WorkflowCommonModel.Props.URL_COMMENT, "Seotud URL");
                                } else if (assoc.isWorkflow()) {
                                    props.put(WorkflowCommonModel.Props.URL, baseCompoundWorkflowUrl + assoc.getFromNode());
                                    props.put(WorkflowCommonModel.Props.URL_COMMENT, "Seotud menetlus");
                                } else {
                                    throw new RuntimeException("Unexpected association type: " + assoc.getType());
                                }

                                props.put(WorkflowCommonModel.Props.URL_CREATOR_NAME, assoc.getCreator());
                                props.put(WorkflowCommonModel.Props.URL_MODIFIER_NAME, assoc.getCreator());
                                props.put(WorkflowCommonModel.Props.CREATED, assoc.getCreatedDateTime());
                                props.put(WorkflowCommonModel.Props.MODIFIED, assoc.getCreatedDateTime());
                                nodeService.createNode(compoundFlowRef, WorkflowCommonModel.Assocs.RELATED_URL, WorkflowCommonModel.Assocs.RELATED_URL,
                                        WorkflowCommonModel.Types.RELATED_URL, props);
                            } else {
                                LOG.warn("Skipping association: " + assoc.getType() + " from: " + assoc.getFromNode());
                            }
                        }

                        // This block must be done first!
                        if (compoundFlow.getParentId() != null) {
                            // L.1.1
                            createRelatedUrl(compoundFlowRef, compoundFlow.getParentId(), "Ülemmenetlus");

                            // L.1.2
                            NodeRef nodeRef = null;
                            try {
                                nodeRef = new NodeRef(
                                        jdbcTemplate.queryForObject(
                                                "SELECT node_ref FROM imp_completed_procedures WHERE procedure_id = ? AND node_ref IS NOT NULL AND length(node_ref) > 1",
                                                String.class, compoundFlow.getParentId()));
                            } catch (IncorrectResultSizeDataAccessException e) {
                                // Finding zero rows is expected
                            }
                            if (nodeRef != null) {
                                createRelatedUrl(nodeRef, compoundFlow.getProcedureId(), "Alammenetlus");

                                // D.1 kopeeri parent'i küljest seotud dokumendid enda külge
                                copyDocAssocs(nodeRef, compoundFlowRef);
                            }
                        }

                        // L.3
                        List<Map<String, Object>> rows = jdbcTemplate
                                .queryForList(
                                        "SELECT procedure_id, node_ref FROM imp_completed_procedures WHERE parent_id = ?",
                                        compoundFlow.getProcedureId());
                        for (Map<String, Object> row : rows) {
                            Integer procedureId = (Integer) row.get("procedure_id");
                            createRelatedUrl(compoundFlowRef, procedureId, "Alammenetlus");

                            // D.2 kopeeri enda küljest seotud dokumendid neile, kes viitavad mulle kui parent'ile
                            String nodeRefString = (String) row.get("node_ref");
                            if (nodeRefString != null) {
                                NodeRef nodeRef = new NodeRef(nodeRefString);
                                copyDocAssocs(compoundFlowRef, nodeRef);
                            }
                        }

                        if (compoundFlow.getParentId() != null && !compoundFlow.getParentId().equals(compoundFlow.getOriginalProcedureId())) {
                            // L.2.1
                            createRelatedUrl(compoundFlowRef, compoundFlow.getOriginalProcedureId(), "Ülem-ülemmenetlus");

                            // L.2.2
                            NodeRef nodeRef = null;
                            try {
                                nodeRef = new NodeRef(
                                        jdbcTemplate.queryForObject(
                                                "SELECT node_ref FROM imp_completed_procedures WHERE procedure_id = ? AND node_ref IS NOT NULL AND length(node_ref) > 1",
                                                String.class, compoundFlow.getOriginalProcedureId()));
                            } catch (IncorrectResultSizeDataAccessException e) {
                                // Finding zero rows is expected
                            }
                            if (nodeRef != null) {
                                createRelatedUrl(nodeRef, compoundFlow.getProcedureId(), "Alam-alammenetlus");
                            }
                        }

                        // L.4
                        rows = jdbcTemplate
                                .queryForList(
                                        "SELECT procedure_id, node_ref FROM imp_completed_procedures WHERE parent_id IS NOT NULL AND original_procedure_id = ? AND original_procedure_id != parent_id",
                                            compoundFlow.getProcedureId());
                        for (Map<String, Object> row : rows) {
                            Integer procedureId = (Integer) row.get("procedure_id");
                            createRelatedUrl(compoundFlowRef, procedureId, "Alam-alammenetlus");

                            // D.3 kopeeri enda küljest seotud dokumendid neile, kes viitavad mulle kui grandparent'ile
                            String nodeRefString = (String) row.get("node_ref");
                            if (nodeRefString != null) {
                                NodeRef nodeRef = new NodeRef(nodeRefString);
                                copyDocAssocs(compoundFlowRef, nodeRef);
                            }
                        }
<<<<<<< HEAD
=======
                    } else {
                        for (Association assoc : associations) {
                            if (assoc.isDocument() && assoc.getFromNode() != null) {
                                final NodeRef fromNode = new NodeRef(assoc.getFromNode());
                                if (!nodeService.exists(fromNode)) {
                                    nodeService.deleteNode(compoundFlowRef);
                                    markCompletedProcedureFailed(procId, 51, "Seotud dokument ei eksisteeri: " + fromNode);
                                    continue outer;
                                }

                                // If document (fromNode) is not a child of our caseFile (parentRef), only then create association
                                if (!nodeService.getPrimaryParent(fromNode).getParentRef().equals(parentRef)) {
                                    boolean assocExists = false;
                                    List<AssociationRef> existingAssocs = nodeService.getSourceAssocs(fromNode, CaseFileModel.Assocs.CASE_FILE_DOCUMENT);
                                    for (AssociationRef existingAssoc : existingAssocs) {
                                        if (existingAssoc.getSourceRef().equals(parentRef)) {
                                            assocExists = true;
                                            break;
                                        }
                                    }
                                    if (!assocExists) {
                                        nodeService.createAssociation(parentRef, fromNode, CaseFileModel.Assocs.CASE_FILE_DOCUMENT);
                                    }
                                }
                            }
                        }
>>>>>>> develop-5.1
                    }

                    logEntry.setObjectId(compoundFlowRef.toString());
                    logService.addLogEntry(logEntry);

                    updateProcessLine(procId, compoundFlowRef.toString());
                }

                if (compoundFlowRef == null) {
                    updateProcessLine(procId, NO_REF);
                }
            }
        } catch (Exception e) {
            status.incrFailed();
            throw (RuntimeException) e;
        } finally {
            String info = progressTracker.step(procIds.size());
            if (info != null) {
                LOG.info("Workflows import: " + info);
            }
        }

        return true;
    }

    private void copyDocAssocs(NodeRef fromCompoundWorkflowRef, NodeRef toCompoundWorkflowRef) {
        List<AssociationRef> toExistingAssocs = nodeService.getSourceAssocs(toCompoundWorkflowRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
        for (AssociationRef docAssoc : nodeService.getSourceAssocs(fromCompoundWorkflowRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT)) {
            Assert.isTrue(docAssoc.getTargetRef().equals(fromCompoundWorkflowRef));
            boolean exists = false;
            for (AssociationRef toExistingDocAssoc : toExistingAssocs) {
                Assert.isTrue(toExistingDocAssoc.getTargetRef().equals(toCompoundWorkflowRef));
                if (toExistingDocAssoc.getSourceRef().equals(docAssoc.getSourceRef())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                nodeService.createAssociation(docAssoc.getSourceRef(), toCompoundWorkflowRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT);
            }
        }
    }

    private void createRelatedUrl(NodeRef compoundFlowRef, Integer procedureId, String comment) {
        String url = baseCompoundWorkflowUrl + procedureId;
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(compoundFlowRef, WorkflowCommonModel.Assocs.RELATED_URL, WorkflowCommonModel.Assocs.RELATED_URL);
        for (ChildAssociationRef ref : childAssocs) {
            Map<QName, Serializable> props = nodeService.getProperties(ref.getChildRef());
            if (url.equals(props.get(WorkflowCommonModel.Props.URL))
                    && comment.equals(props.get(WorkflowCommonModel.Props.URL_COMMENT))
                    && "IMPORT".equals(props.get(WorkflowCommonModel.Props.URL_CREATOR_NAME))
                    && "IMPORT".equals(props.get(WorkflowCommonModel.Props.URL_MODIFIER_NAME))) {
                return;
            }
        }
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(WorkflowCommonModel.Props.URL, url);
        props.put(WorkflowCommonModel.Props.URL_COMMENT, comment);
        props.put(WorkflowCommonModel.Props.URL_CREATOR_NAME, "IMPORT");
        props.put(WorkflowCommonModel.Props.URL_MODIFIER_NAME, "IMPORT");
        props.put(WorkflowCommonModel.Props.CREATED, new Date(AlfrescoTransactionSupport.getTransactionStartTime()));
        props.put(WorkflowCommonModel.Props.MODIFIED, new Date(AlfrescoTransactionSupport.getTransactionStartTime()));
        nodeService.createNode(compoundFlowRef, WorkflowCommonModel.Assocs.RELATED_URL, WorkflowCommonModel.Assocs.RELATED_URL,
                WorkflowCommonModel.Types.RELATED_URL, props);
    }

    private void markCompletedProcedureFailed(Integer procId, int errorCode, String errorDesc) {
        int count = jdbcTemplate.update("INSERT INTO imp_failed_procedures(procedure_id,started_date_time,finished_date_time,error_code,error_desc) " +
                "SELECT procedure_id,started_date_time,finished_date_time,?,? FROM imp_completed_procedures WHERE procedure_id=?", errorCode, errorDesc, procId);
        if (count != 1) {
            throw new RuntimeException("Failed to insert line to 'imp_failed_procedures' table: expected 1 row to be inserted, actually " + count
                    + " rows were inserted for procedure_id=" + procId);
        }
        jdbcTemplate.update("DELETE FROM imp_completed_procedures WHERE procedure_id=?", procId);
        status.incrFailed();
    }

    public void cleanup() {
        LOG.info("Writing to log files.");
        jdbcTemplate.getJdbcOperations().execute(new ConnectionCallback() {

            @Override
            public Object doInConnection(Connection con) throws SQLException, DataAccessException {
                try {
                    con.setAutoCommit(true);

                    try {
                        writeCompletedLog(con);
                    } catch (Exception e) {
                        LOG.warn("Could not update completed procedures log file", e);
                    }

                    try {
                        writeFailedLog(con);
                    } catch (Exception e) {
                        LOG.warn("Could not update failed procedures log file", e);
                    }

                    LOG.info("Performing cleanup...");
                    executeScript(new File(scriptDir, SQL_FILE_DROP_ALL), con, true);

                } catch (Exception e) {
                    LOG.warn("An error was detected during workflow import cleanup phase.", e);
                    ImportUtil.workflowError(errorFile, e);

                } finally {
                    con.setAutoCommit(false);
                }

                return null;
            }
        });
    }

<<<<<<< HEAD
=======
    private static Map<String, NodeRef> loadCompletedCaseFiles(File completed) {
        LOG.info("Loading previously completed caseFiles from file " + completed);

        Map<String, NodeRef> caseFiles = new HashMap<String, NodeRef>();
        CsvReader reader = null;

        try {
            reader = ImportUtil.createLogReader(completed);

            if (reader.readHeaders()) {
                while (reader.readRecord()) {
                    String type = getString(reader, 1);
                    if (!"caseFile".equals(type)) {
                        continue;
                    }
                    String volumeMark = getString(reader, 2);
                    if (caseFiles.containsKey(volumeMark)) {
                        continue;
                    }
                    caseFiles.put(volumeMark, new NodeRef(getString(reader, 5)));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        LOG.info("Loaded " + caseFiles.size() + " previously imported caseFiles (by non-duplicate volumeMark).");

        return caseFiles;
    }

>>>>>>> develop-5.1
    private Set<Integer> getCompletedProcedureIds() {
        File completedFile = settings.getWorkFolderFile(CSV_NAME_COMPLETED_PROCS);
        if (!completedFile.exists()) {
            return Collections.emptySet();
        }

        Set<Integer> procIds = new HashSet<Integer>();
        CsvReader csv = null;

        try {
            csv = ImportUtil.createLogReader(completedFile);
            if (csv.readHeaders()) {
                while (csv.readRecord()) {
                    procIds.add(ImportUtil.getInteger(csv, 1));
                }
            }
        } catch (IOException e) {
            // Should be ignored. Nothing to do with the exception as the log file will be overwritten.
            LOG.info("Unexpected exception when trying to read '" + CSV_NAME_COMPLETED_PROCS + "'.", e);
        } finally {
            if (csv != null) {
                csv.close();
            }
        }

        return procIds;
    }

    private void writeMenetlused(Connection con) {
        final Set<Integer> completedIds = getCompletedProcedureIds();
        final AtomicInteger newProcCount = new AtomicInteger();

        if (!completedIds.isEmpty()) {
            LOG.info("Completed procedures (" + completedIds.size() + ") will be skipped.");
        }

        writeCsvToTable(settings.getDataFolderFile("d_menetlus.csv"), true,
                "INSERT INTO imp_d_menetlus (id, prioriteet, olek, menetluseliik, alguskp, loppkp, tahtaegkp, algataja_id, kirjeldus, looja, " +
                        "loomisekp, labibkantselei, trykk, menetlusy_id, menetlusa_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?, ?, ?)", con, new CsvBasedParamsSetter() {

                    @Override
                    public boolean setCsvValues(PreparedStatement ps, CsvReader csv) throws SQLException, IOException {
                        Integer procId = ImportUtil.getInteger(csv, 1);
                        boolean notUsed = !completedIds.contains(procId);
                        if (notUsed) {
                            setInt(ps, 1, procId);
                            ps.setString(2, ImportUtil.getString(csv, 4));
                            ps.setString(3, ImportUtil.getString(csv, 5));
                            ps.setString(4, ImportUtil.getString(csv, 6));
                            ps.setTimestamp(5, ImportUtil.getTimestamp(csv, 7));
                            ps.setTimestamp(6, ImportUtil.getTimestamp(csv, 8));
                            ps.setTimestamp(7, ImportUtil.getTimestamp(csv, 9));
                            setInt(ps, 8, ImportUtil.getInteger(csv, 10));
                            ps.setString(9, ImportUtil.getString(csv, 14));
                            ps.setString(10, ImportUtil.getString(csv, 15));
                            ps.setTimestamp(11, ImportUtil.getTimestamp(csv, 16));
                            ps.setBoolean(12, "1".equals(ImportUtil.getString(csv, 21)));
                            ps.setBoolean(13, "1".equals(ImportUtil.getString(csv, 22)));
                            setInt(ps, 14, ImportUtil.getInteger(csv, 3));
                            setInt(ps, 15, ImportUtil.getInteger(csv, 2));
                            newProcCount.incrementAndGet();
                        }
                        return notUsed;
                    }
                });

        LOG.info("Not imported procedures count: " + newProcCount.intValue());
    }

    private void writeDokMenetlused(Connection con) {
        writeCsvToTable(settings.getDataFolderFile("d_dok_men.csv"), false,
                "INSERT INTO imp_d_dok_men (menetlus_id, id, dokument_id, urldokument, liik, looja, loomisekp) VALUES (?,?,?,?,?,?,?)", con,
                new CsvBasedParamsSetter() {

                    @Override
                    public boolean setCsvValues(PreparedStatement ps, CsvReader csv) throws SQLException, IOException {
                        setInt(ps, 1, ImportUtil.getInteger(csv, 1));
                        setInt(ps, 2, ImportUtil.getInteger(csv, 2));
                        setInt(ps, 3, ImportUtil.getInteger(csv, 4));
                        ps.setString(4, ImportUtil.getString(csv, 5));
                        ps.setString(5, ImportUtil.getString(csv, 7));
                        ps.setString(6, ImportUtil.getString(csv, 9));
                        ps.setTimestamp(7, ImportUtil.getTimestamp(csv, 10));
                        return true;
                    }
                });
    }

    private void writeIsikMenetlused(Connection con) {
        writeCsvToTable(settings.getDataFolderFile("d_isik_men.csv"), true,
                "INSERT INTO imp_d_isik_men (menetlus_id,id,ylem_id,staatus,kinnitatud,liik,d_org_strukt_id,alguskp,tahtaegkp,loppkp,looja,loomisekp) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", con, new CsvBasedParamsSetter() {

                    @Override
                    public boolean setCsvValues(PreparedStatement ps, CsvReader csv) throws SQLException, IOException {
                        setInt(ps, 1, ImportUtil.getInteger(csv, 1));
                        setInt(ps, 2, ImportUtil.getInteger(csv, 2));
                        setInt(ps, 3, ImportUtil.getInteger(csv, 3));
                        ps.setString(4, ImportUtil.getString(csv, 4));
                        ps.setString(5, ImportUtil.getString(csv, 6));
                        ps.setString(6, ImportUtil.getString(csv, 7));
                        setInt(ps, 7, ImportUtil.getInteger(csv, 9));
                        ps.setTimestamp(8, ImportUtil.getTimestamp(csv, 11));
                        ps.setTimestamp(9, ImportUtil.getTimestamp(csv, 12));
                        ps.setTimestamp(10, ImportUtil.getTimestamp(csv, 13));
                        ps.setString(11, ImportUtil.getString(csv, 16));
                        ps.setTimestamp(12, ImportUtil.getTimestamp(csv, 17));
                        return true;
                    }
                });
    }

    private void writeKommentaarid(Connection con) {
        writeCsvToTable(settings.getDataFolderFile("d_kommentaar.csv"), false, "INSERT INTO imp_d_kommentaar (menetlus_id, id, kommentaar, looja, loomisekp) VALUES (?,?,?,?,?)",
                con, new CsvBasedParamsSetter() {

                    @Override
                    public boolean setCsvValues(PreparedStatement ps, CsvReader csv) throws SQLException, IOException {
                        setInt(ps, 1, ImportUtil.getInteger(csv, 1));
                        setInt(ps, 2, ImportUtil.getInteger(csv, 2));
                        ps.setString(3, ImportUtil.getString(csv, 3));
                        ps.setString(4, ImportUtil.getString(csv, 4));
                        ps.setTimestamp(5, ImportUtil.getTimestamp(csv, 5));
                        return true;
                    }
                });
    }

    private void writeLisavaljavaartus(Connection con) {
        writeCsvToTable(settings.getDataFolderFile("d_lisavaljaVaartus.csv"), false,
                "INSERT INTO imp_d_lisavaljavaartus (menetlus_id, id, menetluseliik, valjatyyp, valjanimi, combo_vaartus, " +
                        "string_vaartus, date_vaartus, int_vaartus, is_selected) VALUES (?,?,?,?,?,?,?,?,?,?)", con, new CsvBasedParamsSetter() {

                    @Override
                    public boolean setCsvValues(PreparedStatement ps, CsvReader csv) throws SQLException, IOException {
                        setInt(ps, 1, ImportUtil.getInteger(csv, 1));
                        setInt(ps, 2, ImportUtil.getInteger(csv, 2));
                        ps.setString(3, ImportUtil.getString(csv, 3));
                        ps.setString(4, ImportUtil.getString(csv, 4));
                        ps.setString(5, ImportUtil.getString(csv, 5));
                        ps.setString(6, ImportUtil.getString(csv, 6));
                        ps.setString(7, ImportUtil.getString(csv, 7));
                        ps.setDate(8, ImportUtil.getSqlDateTS(csv, 8));
                        setInt(ps, 9, ImportUtil.getInteger(csv, 9));
                        ps.setBoolean(10, "1".equals(ImportUtil.getString(csv, 10)));
                        return true;
                    }
                });
    }

    private void writeResolutsioonid(Connection con) {
        writeCsvToTable(settings.getDataFolderFile("resolutsioonid.csv"), false, "INSERT INTO imp_resolutsioonid (id, isik_men_id, resolutsioon) VALUES (?,?,?)", con,
                new CsvBasedParamsSetter() {

                    @Override
                    public boolean setCsvValues(PreparedStatement ps, CsvReader csv) throws SQLException, IOException {
                        setInt(ps, 1, ImportUtil.getInteger(csv, 1));
                        setInt(ps, 2, ImportUtil.getInteger(csv, 2));
                        ps.setString(3, ImportUtil.getString(csv, 8));
                        return true;
                    }
                });
    }

    private void writeKasutOstr(Connection con) {
        writeCsvToTable(settings.getDataFolderFile("d_kasut_ostr.csv"), true, "INSERT INTO imp_d_kasut_ostr (id,d_org_strukt_id,kasutaja,alguskp,loppkp) VALUES (?,?,?,?,?)", con,
                new CsvBasedParamsSetter() {

                    @Override
                    public boolean setCsvValues(PreparedStatement ps, CsvReader csv) throws SQLException, IOException {
                        setInt(ps, 1, ImportUtil.getInteger(csv, 1));
                        setInt(ps, 2, ImportUtil.getInteger(csv, 2));
                        ps.setString(3, ImportUtil.getString(csv, 3));
                        ps.setTimestamp(4, ImportUtil.getTimestamp(csv, 4));
                        ps.setTimestamp(5, ImportUtil.getTimestamp(csv, 5));
                        return true;
                    }
                });
    }

    private void writeKasutAdsi(Connection con) {
        writeCsvToTable(settings.getDataFolderFile(DocumentImporter.CSV_NAME_KASUT_ADSI), true, "INSERT INTO imp_d_kasut_adsi (displayname, samaccountname) VALUES (?,?)", con,
                new CsvBasedParamsSetter() {

                    @Override
                    public boolean setCsvValues(PreparedStatement ps, CsvReader csv) throws SQLException, IOException {
                        ps.setString(1, ImportUtil.getString(csv, 1));
                        ps.setString(2, ImportUtil.getString(csv, 6));
                        return true;
                    }
                });
    }

    private void writeCompletedDocs(Connection con) {
        final Set<String> usedRefs = new HashSet<String>(); // Documents with versions may contain many lines in completed_docs file, each one with same document NodeRef.

        writeCsvToTable(settings.getWorkFolderFile(CSV_NAME_COMPLETED_DOCS), false, "INSERT INTO imp_completed_docs (document_id,node_ref,originallocation) VALUES (?,?,?)", con,
                new CsvBasedParamsSetter() {

                    @Override
                    public boolean setCsvValues(PreparedStatement ps, CsvReader csv) throws SQLException, IOException {
                        final String docNodeRef = ImportUtil.getString(csv, 2);
                        boolean notUsed = !usedRefs.contains(docNodeRef);

                        if (notUsed) {
                            ps.setString(1, ImportUtil.getString(csv, 1));
                            ps.setString(2, docNodeRef);
                            ps.setString(3, ImportUtil.getString(csv, 3));
                            usedRefs.add(docNodeRef);
                        }
                        return notUsed;
                    }
                });
    }

    private void writeSystemUsers(Connection con) {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO imp_delta_users (name, username, email) VALUES (?,?,?)");

            for (NodeRef personRef : getPersonService().getAllPeople()) {
                String firstName = (String) nodeService.getProperty(personRef, ContentModel.PROP_FIRSTNAME);
                String lastName = (String) nodeService.getProperty(personRef, ContentModel.PROP_LASTNAME);
                String name = StringUtils.defaultString(firstName) + " " + StringUtils.defaultString(lastName);

                ps.setString(1, name.trim());
                ps.setString(2, (String) nodeService.getProperty(personRef, ContentModel.PROP_USERNAME));
                ps.setString(3, (String) nodeService.getProperty(personRef, ContentModel.PROP_EMAIL));
                ps.addBatch();
            }

            ps.executeBatch();

        } catch (SQLException e) {
            throw new RuntimeException("Exception while writing Delta users to a table.", e);
        } finally {
            closeStmt(ps);
        }
    }

    private void writeCompletedLogFromCsv(Connection con) {
        writeCsvToTable(logFile, false,
                "INSERT INTO imp_completed_procedures (procedure_id,started_date_time,finished_date_time,comment,node_ref,parent_id,original_procedure_id) VALUES (?,?,?,?,?,?,?)",
                con, new CsvBasedParamsSetter() {

                    @Override
                    public boolean setCsvValues(PreparedStatement ps, CsvReader csv) throws SQLException, IOException {
                        ps.setInt(1, ImportUtil.getInteger(csv, 1));
                        ps.setDate(2, ImportUtil.getSqlDate(csv, 2));
                        ps.setDate(3, ImportUtil.getSqlDate(csv, 3));
                        ps.setString(4, ImportUtil.getString(csv, 4));
                        ps.setString(5, ImportUtil.getString(csv, 5));

                        Integer col6 = ImportUtil.getInteger(csv, 6);
                        if (col6 == null) {
                            ps.setNull(6, Types.INTEGER);
                        } else {
                            ps.setInt(6, col6);
                        }

                        Integer col7 = ImportUtil.getInteger(csv, 7);
                        if (col7 == null) {
                            ps.setNull(7, Types.INTEGER);
                        } else {
                            ps.setInt(7, col7);
                        }

                        return true;
                    }

                });
    }

    private void writeCompletedLog(Connection con) {
        if (logFile.exists()) {
            File renameFile = settings.getWorkFolderFile("completed_procedures-" + DateFormatUtils.format(new Date(), "yyyy-MM-dd-HH-mm-ss-SSSZ") + ".csv");
            Assert.isTrue(!renameFile.exists(), "File exists: " + renameFile);
            Assert.isTrue(logFile.renameTo(renameFile), "Renaming '" + logFile + "' to '" + renameFile + "' failed");
            LOG.info("Renamed '" + logFile + "' to '" + renameFile + "' failed");
        }
        writeTableToCsv(logFile, false, "SELECT * FROM imp_completed_procedures WHERE node_ref IS NOT NULL", con, new CsvBasedTableReader() {

            @Override
            public void writeToCsv(ResultSet rs, CsvWriter csv) throws SQLException, IOException {
                csv.write(Integer.toString(rs.getInt(1)));
                csv.write(ImportUtil.formatDate(rs.getDate(2)));
                csv.write(ImportUtil.formatDate(rs.getDate(3)));
                csv.write(rs.getString(4));

                String nodeRef = rs.getString(5);
                csv.write(NO_REF.equals(nodeRef) ? null : nodeRef);

                csv.write(rs.getObject(6) == null ? null : Integer.toString(rs.getInt(6)));
                csv.write(rs.getObject(7) == null ? null : Integer.toString(rs.getInt(7)));
                csv.endRecord();
            }
        });
    }

    private void writeFailedLog(Connection con) {
        writeTableToCsv(failedFile, true, "SELECT * FROM imp_failed_procedures", con, new CsvBasedTableReader() {

            @Override
            public void writeToCsv(ResultSet rs, CsvWriter csv) throws SQLException, IOException {
                csv.write(Integer.toString(rs.getInt(1)));
                csv.write(ImportUtil.formatDate(rs.getDate(2)));
                csv.write(ImportUtil.formatDate(rs.getDate(3)));
                csv.write(rs.getString(4));
                csv.write(rs.getString(5));
                csv.write(rs.getString(6));
                csv.endRecord();
            }
        });
    }

    private void writeCsvToTable(File file, boolean require, String sqlInsert, Connection con, CsvBasedParamsSetter setter) {
        if (!file.exists()) {
            if (require) {
                throw new RuntimeException("A required input file does not exist: " + file);
            }
            return;
        }

        LOG.info("Writing content from file '" + file + "' to a table...");

        CsvReader csv = null;
        PreparedStatement ps = null;
        try {
            csv = DocumentImporter.CSV_NAME_KASUT_ADSI.equals(file.getName()) ? ImportUtil.createDataReader(file) : ImportUtil.createLogReader(file);
            ps = con.prepareStatement(sqlInsert);

            if (csv.readHeaders()) {
                int lines = 0;
                while (csv.readRecord()) {
                    if (setter.setCsvValues(ps, csv)) {
                        ps.addBatch();
                        lines++;

                        if (lines > 10000) {
                            ps.executeBatch();
                            lines = 0;
                        }
                    }
                }
            }
            ps.executeBatch();
        } catch (SQLException e) {
            String lastMsg = e.getMessage();
            SQLException inner = e.getNextException();
            while (inner != null) {
                lastMsg = inner.getMessage();
                LOG.warn("SQL exception: " + lastMsg, e);
                inner = inner.getNextException();
            }
            throw new RuntimeException("SQL exception while writing CSV data from '" + file.getName() + "' (line: " + (csv == null ? 0 : (csv.getCurrentRecord() + 2))
                    + ") to a table: " + lastMsg, e);
        } catch (Exception e) {
            throw new RuntimeException("Exception while writing CSV data from '" + file.getName() + "' (line: " + (csv == null ? 0 : (csv.getCurrentRecord() + 2))
                    + ") to a table: " + e, e);
        } finally {
            closeStmt(ps);
            if (csv != null) {
                csv.close();
            }
        }
    }

    private void updateProcessLine(Integer procId, String nodeRefVal) {
        int count = jdbcTemplate.update("UPDATE imp_completed_procedures SET node_ref=? WHERE node_ref IS NULL AND procedure_id=?", nodeRefVal, procId);
        if (count != 1) {
            throw new RuntimeException("Failed to update 'imp_completed_procedures' table: expected 1 row to be updated, actually " + count
                    + " rows were updated for procedure_id=" + procId);
        }
    }

    private void writeTableToCsv(File file, boolean isFailedFile, String sqlSelect, Connection con, CsvBasedTableReader reader) {
        CsvWriter csv = null;
        Statement stmt = null;
        ResultSet rs = null;

        LOG.info("Writing content from a table to file '" + file + "', " + (isFailedFile ? "appending" : "overwriting") + "...");

        try {

            csv = initLogWriter(file, isFailedFile);
            stmt = con.createStatement();

            if (stmt.execute(sqlSelect)) {
                rs = stmt.getResultSet();
                while (rs.next()) {
                    reader.writeToCsv(rs, csv);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected exception when reading log tables.", e);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception when writing to log file.", e);
        } finally {
            closeResultSet(rs);
            closeStmt(stmt);
            if (csv != null) {
                csv.close();
            }
        }
    }

    private static void executeScript(File file, Connection con, boolean multipleCmds) throws IOException, SQLException {
        if (!file.exists()) {
            throw new RuntimeException("Missing required file: " + file);
        }

        LOG.info("Executing SQL script from file: " + file);

        BufferedReader reader = null;
        Statement stmt = null;

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), ImportUtil.CHARSET_ISO));
            stmt = con.createStatement();

            StringBuilder sb = new StringBuilder();
            String line = null;
            String prevLine = null;

            do {
                line = reader.readLine();

                if (line == null && sb.length() > 0) {
                    executeSql(sb, stmt);
                } else if (line != null) {
                    line = line.trim();

                    if (!line.isEmpty() && !line.startsWith("--")) {
                        if (multipleCmds && prevLine != null && prevLine.endsWith(";")) {
                            executeSql(sb, stmt);
                        }
                        sb.append(line).append("\n");
                        prevLine = line;
                    }
                }
            } while (line != null);

        } finally {
            IOUtils.closeQuietly(reader);
            closeStmt(stmt);
        }
    }

    private static void executeSql(final StringBuilder sb, Statement stmt) throws SQLException {
        sb.setLength(sb.length() - 2); // Remove semicolon and new-line
        stmt.executeUpdate(sb.toString());
        sb.setLength(0);
    }

    private static void closeStmt(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // Ignore.
            }
        }
    }

    private static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // Ignore.
            }
        }
    }

    private static final void setInt(PreparedStatement ps, int pos, Integer integer) throws SQLException {
        if (integer != null) {
            ps.setInt(pos, integer);
        } else {
            ps.setNull(pos, Types.INTEGER);
        }
    }

    private static final CsvWriter initLogWriter(File file, boolean failedFile) throws IOException {
        boolean notExists = !file.exists();
        CsvWriter csv = ImportUtil.createLogWriter(file, failedFile);
        if (notExists) {
            if (failedFile) {
                csv.writeRecord(new String[] { "procedure_id", "started_date_time", "finished_date_time", "task_user", "error_code", "error_desc" });
            } else {
                csv.writeRecord(new String[] { "procedure_id", "started_date_time", "finished_date_time", "comment", "node_ref", "parent_id", "original_procedure_id" });
            }
        }
        return csv;
    }

    private interface CsvBasedParamsSetter {

        boolean setCsvValues(PreparedStatement ps, CsvReader csv) throws SQLException, IOException;
    }

    private interface CsvBasedTableReader {

        void writeToCsv(ResultSet rs, CsvWriter csv) throws SQLException, IOException;
    }
}
