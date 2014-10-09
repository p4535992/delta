package ee.webmedia.alfresco.workflow.bootstrap;

import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.service.BulkLoadNodeServiceImpl;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.WorkflowDbService;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

public class DueDateExtensionTaskSqlUpdater extends AbstractModuleComponent {

    private WorkflowDbService workflowDbService;
    private WorkflowService workflowService;
    private SimpleJdbcTemplate jdbcTemplate;
    private final Map<String, Map<QName, Serializable>> allUserProps = new HashMap<String, Map<QName, Serializable>>();
    private boolean enabled;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DueDateExtensionTaskSqlUpdater.class);

    @Override
    protected void executeInternal() throws Throwable {
        if (!enabled) {
            LOG.info("DueDateExtensionTaskSqlUpdater is disabled, skipping.");
            return;
        }
        String sqlQuery = "SELECT initiating_task.wfs_creator_id as initiating_task_creator_id,"
                + " extension_task.task_id as extension_task_id, extension_task.store_id as store_id "
                + " FROM delta_task extension_task"
                + " join delta_task_due_date_extension_assoc extension_assoc on extension_task.task_id = extension_assoc.extension_task_id "
                + " join delta_task initiating_task on initiating_task.task_id = extension_assoc.task_id ";
        List<Pair<NodeRef, String>> taskRefsToRequiredOwner = jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<Pair<NodeRef, String>>() {

            @Override
            public Pair<NodeRef, String> mapRow(java.sql.ResultSet rs, int i) throws SQLException {
                String nodeUuid = rs.getString("extension_task_id");
                String storeId = rs.getString("store_id");
                String creator = rs.getString("initiating_task_creator_id");
                return new Pair(new NodeRef(new StoreRef(storeId), nodeUuid), creator);
            }
        });

        int batchSize = 100;
        List<List<Pair<NodeRef, String>>> slicedTasks = BulkLoadNodeServiceImpl.sliceList(taskRefsToRequiredOwner, batchSize);
        for (List<Pair<NodeRef, String>> taskSlice : slicedTasks) {
            List<Pair<NodeRef, Map<QName, Serializable>>> tasksNewProps = new ArrayList<Pair<NodeRef, Map<QName, Serializable>>>();
            for (Pair<NodeRef, String> taskAndRequiredOwner : taskSlice) {
                String requiredOwnerId = taskAndRequiredOwner.getSecond();
                Map<QName, Serializable> newTaskProps = new HashMap<QName, Serializable>();
                Map<QName, Serializable> userProps = allUserProps.get(requiredOwnerId);
                if (userProps == null && StringUtils.isNotBlank(requiredOwnerId) && !allUserProps.containsKey(requiredOwnerId)) {
                    userProps = getUserService().getUserProperties(requiredOwnerId);
                    allUserProps.put(requiredOwnerId, userProps);
                }
                if (userProps != null) {
                    newTaskProps.put(WorkflowCommonModel.Props.OWNER_EMAIL, userProps.get(ContentModel.PROP_EMAIL));
                    newTaskProps.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, userProps.get(ContentModel.PROP_JOBTITLE));
                    Serializable orgName = (Serializable) getOrganizationStructureService().getOrganizationStructurePaths((String) userProps.get(ContentModel.PROP_ORGID));
                    newTaskProps.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, orgName);
                } else {
                    newTaskProps.put(WorkflowCommonModel.Props.OWNER_EMAIL, null);
                    newTaskProps.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, null);
                    newTaskProps.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, null);
                }
            }
            workflowDbService.updateTaskOwnerProps(tasksNewProps);
        }

    }

    public void setWorkflowDbService(WorkflowDbService workflowDbService) {
        this.workflowDbService = workflowDbService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
