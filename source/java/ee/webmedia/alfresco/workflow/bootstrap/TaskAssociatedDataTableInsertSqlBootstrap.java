package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.logging.Log;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.BulkLoadNodeServiceImpl;
import ee.webmedia.alfresco.common.service.CreateObjectCallback;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.ProgressTracker;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Insert tasks' due date extension data, due date extension history, files, multivalued properties etc
 * into corresponding tables.
 */
public class TaskAssociatedDataTableInsertSqlBootstrap extends AbstractModuleComponent {

    private WorkflowDbService workflowDbService;
    private WorkflowService workflowService;
    private SimpleJdbcTemplate jdbcTemplate;
    private static final Set<QName> FILE_PROPS_TO_LOAD = new HashSet<QName>();
    private static final Set<QName> OWNER_ORGANIZATION_NAME_PROP = new HashSet<QName>();
    private boolean enabled;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DueDateExtensionTaskSqlUpdater.class);

    static {
        FILE_PROPS_TO_LOAD.add(ContentModel.PROP_NAME);
        OWNER_ORGANIZATION_NAME_PROP.add(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void executeInternal() throws Throwable {
        if (!enabled) {
            LOG.info("TaskAssociatedDataTableInsertSqlBootstrap is disabled, skipping");
            return;
        }
        LOG.info("Executing TaskAssociatedDataTableInsertSqlBootstrap.");

        BulkLoadNodeService bulkLoadNodeService = BeanHelper.getSpringBean(BulkLoadNodeService.class, BulkLoadNodeService.BEAN_NAME);
        NodeService nodeService = BeanHelper.getNodeService();
        String sqlQuery = "SELECT task_id, store_id, task_type FROM delta_task ";
        List<String> taskRefs = jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString("task_id");
            }

        });
        int taskCount = taskRefs.size();
        LOG.info("Processing " + taskCount + " tasks.");
        ProgressTracker progress = new ProgressTracker(taskCount, 0);
        int batchSize = 100;
        List<List<String>> slicedTasks = BulkLoadNodeServiceImpl.sliceList(taskRefs, batchSize);
        List<Pair<NodeRef, Pair<String, Date>>> dueDateHistories = new ArrayList<Pair<NodeRef, Pair<String, Date>>>();
        List<Pair<NodeRef, NodeRef>> dueDateExtensionTasks = new ArrayList<Pair<NodeRef, NodeRef>>();
        Map<NodeRef, List<Pair<NodeRef, String>>> taskFiles = new HashMap<NodeRef, List<Pair<NodeRef, String>>>();
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (List<String> taskUuidSlice : slicedTasks) {
            List<NodeRef> taskRefSlice = bulkLoadNodeService.loadNodeRefByUuid(taskUuidSlice);
            Map<NodeRef, List<Pair<String, Date>>> dueDateHistoriesSlice = bulkLoadNodeService.loadChildNodes(taskRefSlice, null,
                    WorkflowCommonModel.Types.DUE_DATE_HISTORY,
                    propertyTypes,
                    new CreateObjectCallback<Pair<String, Date>>() {

                        @Override
                        public Pair<String, Date> create(NodeRef nodeRef, Map<QName, Serializable> properties) {
                            return new Pair<String, Date>((String) properties.get(WorkflowCommonModel.Props.CHANGE_REASON), (Date) properties
                                    .get(WorkflowCommonModel.Props.PREVIOUS_DUE_DATE));
                        }
                    });
            for (Entry<NodeRef, List<Pair<String, Date>>> entry : dueDateHistoriesSlice.entrySet()) {
                List<Pair<String, Date>> value = entry.getValue();
                if (value != null && !value.isEmpty()) {
                    for (Pair<String, Date> historyRecord : value) {
                        dueDateHistories.add(new Pair(entry.getKey(), historyRecord));
                    }
                }
            }
            if (dueDateHistories.size() > batchSize) {
                workflowDbService.createTaskDueDateHistoryEntries(dueDateHistories);
                dueDateHistories.clear();
            }
            Map<NodeRef, List<NodeRef>> dueDateExtensionTaskSlice = bulkLoadNodeService.getSourceAssocs(taskRefSlice, WorkflowSpecificModel.Assocs.TASK_DUE_DATE_EXTENSION);
            for (Map.Entry<NodeRef, List<NodeRef>> entry : dueDateExtensionTaskSlice.entrySet()) {
                List<NodeRef> value = entry.getValue();
                if (value != null && !value.isEmpty()) {
                    for (NodeRef otherTaskRef : value) {
                        dueDateExtensionTasks.add(Pair.newInstance(otherTaskRef, entry.getKey()));
                        // if there are accidentally more than one related task, only the first one is taken in account
                        break;
                    }
                }
            }
            if (dueDateExtensionTasks.size() > batchSize) {
                workflowDbService.createTaskDueDateExtensionAssocEntries(dueDateExtensionTasks);
                dueDateExtensionTasks.clear();
            }
            Map<NodeRef, List<Pair<NodeRef, String>>> taskFilesSlice = bulkLoadNodeService.loadChildNodes(taskRefSlice, FILE_PROPS_TO_LOAD, ContentModel.TYPE_CONTENT,
                    propertyTypes, new CreateObjectCallback<Pair<NodeRef, String>>() {

                        @Override
                        public Pair<NodeRef, String> create(NodeRef nodeRef, Map<QName, Serializable> properties) {
                            return new Pair(nodeRef, properties.get(ContentModel.PROP_NAME));
                        }
                    });
            taskFiles.putAll(taskFilesSlice);
            if (taskFiles.size() > batchSize) {
                processTaskFiles(taskFiles, bulkLoadNodeService, nodeService);
                taskFiles.clear();
            }
            Map<NodeRef, Node> tasks = bulkLoadNodeService.loadNodes(taskRefSlice, OWNER_ORGANIZATION_NAME_PROP);
            workflowDbService.updateTaskOwnerOrgNameAndStoreRef(new ArrayList<Node>(tasks.values()));
            String info = progress.step(taskUuidSlice.size());
            if (info != null) {
                LOG.info("Tasks updating: " + info);
            }
        }
        if (!dueDateHistories.isEmpty()) {
            workflowDbService.createTaskDueDateHistoryEntries(dueDateHistories);
        }
        if (!dueDateExtensionTasks.isEmpty()) {
            workflowDbService.createTaskDueDateExtensionAssocEntries(dueDateExtensionTasks);
        }
        if (!taskFiles.isEmpty()) {
            processTaskFiles(taskFiles, bulkLoadNodeService, nodeService);
        }

    }

    private void processTaskFiles(Map<NodeRef, List<Pair<NodeRef, String>>> taskFiles, BulkLoadNodeService bulkLoadNodeService, NodeService nodeService) {
        Map<NodeRef, NodeRef> taskToWorkflow = bulkLoadNodeService.getPrimaryParentRefs(taskFiles.keySet(), null);
        List<Pair<NodeRef, NodeRef>> taskToFile = new ArrayList<Pair<NodeRef, NodeRef>>();
        for (Map.Entry<NodeRef, List<Pair<NodeRef, String>>> entry : taskFiles.entrySet()) {
            NodeRef taskRef = entry.getKey();
            NodeRef workflowRef = taskToWorkflow.get(taskRef);
            for (Pair<NodeRef, String> fileRefAndName : entry.getValue()) {
                NodeRef fileRef = fileRefAndName.getFirst();
                taskToFile.add(new Pair(taskRef, fileRef));
                nodeService.moveNode(fileRef, workflowRef, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(fileRefAndName.getSecond())));
            }
        }
        workflowDbService.createTaskFileEntriesFromNodeRefs(taskToFile);
    }

    public static String[] checkTaskExists(final NodeRef nodeRef, Log log, NodeService nodeService, WorkflowService workflowService, WorkflowDbService workflowDbService) {
        if (!workflowDbService.taskExists(nodeRef)) {
            WmNode taskNode = new WmNode(nodeRef, nodeService.getType(nodeRef));
            String taskStr = taskNode.toString();
            log.error("Found task from repo that doesn't exist in delta_task table, node:\n" + taskStr);
            return new String[] { "Task not present in delta_task table", taskStr };
        }
        return null;
    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

}
