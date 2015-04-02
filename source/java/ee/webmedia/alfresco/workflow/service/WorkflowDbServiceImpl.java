package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.common.search.DbSearchUtil.TASK_TYPE_FIELD;
import static ee.webmedia.alfresco.common.search.DbSearchUtil.getDbFieldNameFromPropQName;
import static ee.webmedia.alfresco.common.search.DbSearchUtil.getQuestionMarks;
import static ee.webmedia.alfresco.utils.RepoUtil.isSaved;
import static ee.webmedia.alfresco.workflow.service.Task.INITIATING_COMPOUND_WORKFLOW_REF;
import static ee.webmedia.alfresco.workflow.service.Task.INITIATING_COMPOUND_WORKFLOW_TITLE;

import java.io.Serializable;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.search.DbSearchUtil;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.Transformer;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.bootstrap.MoveTaskFileToChildAssoc;
import ee.webmedia.alfresco.workflow.model.Comment;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowBlockItem;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel.Types;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;
import ee.webmedia.alfresco.workflow.web.PrintTableServlet;

/**
 * Main implementation of {@link WorkflowDbService}. This class does not rely on Alfresco, and exchanges data with the database using JDBC(Template) directly.
 */
public class WorkflowDbServiceImpl implements WorkflowDbService {

    private static final String IS_SEARCHABLE_FIELD = "is_searchable";
    private static final String INDEX_IN_WORKFLOW_FIELD = "index_in_workflow";
    private static final String TASK_ID_FIELD = "task_id";
    private static final String WORKFLOW_ID_KEY = "workflow_id";
    private static final String COMPOUND_WORKFLOW_ID_KEY = "wfs_compound_workflow_id";
    private static final String STORE_ID_FIELD = "store_id";
    private static final String INITIATING_COMPOUND_WORKFLOW_ID_KEY = "initiating_compound_workflow_id";
    private static final String INITIATING_COMPOUND_WORKFLOW_STORE_ID_KEY = "initiating_compound_workflow_store_id";
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(WorkflowDbServiceImpl.class);
    private static final List<QName> NOT_SENT_TO_REPO_PROPS = Arrays.asList(QName.createQName(WorkflowCommonModel.URI, "creatorId"),
            QName.createQName(WorkflowCommonModel.URI, "creatorEmail"), MoveTaskFileToChildAssoc.TASK_FILE_PROP_QNAME);
    public static final Map<String, QName> ALL_TASK_TYPES = new HashMap<String, QName>();
    static {
        addTaskTypes(Types.ASSIGNMENT_TASK, Types.CONFIRMATION_TASK, Types.DUE_DATE_EXTENSION_TASK, Types.EXTERNAL_REVIEW_TASK, Types.GROUP_ASSIGNMENT_TASK,
                Types.INFORMATION_TASK, Types.LINKED_REVIEW_TASK, Types.OPINION_TASK, Types.ORDER_ASSIGNMENT_TASK, Types.REVIEW_TASK, Types.SIGNATURE_TASK);
    }

    private static final Map<String, QName> TRANSIENT_PROPS = new HashMap<>();
    private static final Map<String, QName> FIELD_NAME_TO_TASK_PROP = new ConcurrentHashMap<>();
    private static final Map<QName, String> TASK_PROP_TO_FIELD_NAME = new ConcurrentHashMap<>();

    private JdbcTemplate jdbcTemplate;
    private DataSource dataSource;
    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private GeneralService generalService;
    private WorkflowConstantsBean workflowConstantsBean;

    static {
        TRANSIENT_PROPS.put(INITIATING_COMPOUND_WORKFLOW_ID_KEY, INITIATING_COMPOUND_WORKFLOW_REF);
        TRANSIENT_PROPS.put("initiating_compound_workflow_title", INITIATING_COMPOUND_WORKFLOW_TITLE);
        TRANSIENT_PROPS.put(INITIATING_COMPOUND_WORKFLOW_STORE_ID_KEY, null);
    }

    private static void addTaskTypes(QName... taskTypes) {
        for (QName taskType : taskTypes) {
            ALL_TASK_TYPES.put(taskType.getLocalName(), taskType);
        }
    }

    @Override
    public List<WorkflowBlockItem> getWorkflowBlockItemGroup(WorkflowBlockItem firstItemInGroup) {
        return getWorkflowBlockItemGroup(firstItemInGroup.getWorkflowNodeRef().getId(), firstItemInGroup.getIndexInWorkflow(), firstItemInGroup.getNumberOfTasksInGroup());
    }

    @Override
    public List<WorkflowBlockItem> getWorkflowBlockItemGroup(String workflowNodeRefId, Integer offset, Integer limit) {
        Assert.hasText(workflowNodeRefId);
        Assert.notNull(offset);

        List<Object> arguments = new ArrayList<>();
        String sql = "select "
                + "row_number() over (order by wfs_searchable_compound_workflow_started_date_time, workflow_id, wfc_started_date_time, index_in_workflow, (case when (task_type = 'assignmentTask' and (wfs_active is null or wfs_active = false)) then 1 else 0 end)) as num"
                + ", store_id, task_id, wfs_compound_workflow_id, workflow_id, wfc_owner_name, wfc_started_date_time, wfc_completed_date_time, wfs_due_date, wfc_creator_name, task_type, wfs_active, wfs_proposed_due_date, wfs_resolution, wfs_workflow_resolution, "
                + "wfc_outcome, wfs_comment, wfc_owner_substitute_name, wfc_owner_group, wfc_status, lag(wfc_owner_group, 1) over w as prev_group, lead(wfc_owner_group, 1) over w as next_group, index_in_workflow, false as is_group "
                + "from delta_task WHERE workflow_id=? "
                + "window w as (partition by workflow_id order by workflow_id, index_in_workflow) "
                + "order by num "
                + "offset ?";

        arguments.add(workflowNodeRefId);
        arguments.add(offset);
        if (limit != null) {
            sql += " limit ?";
            arguments.add(limit);
        }

        final List<WorkflowBlockItem> items = jdbcTemplate.query(sql, new WorkflowBlockItemMapper(null, null), arguments.toArray());
        Set<String> taskIds = new HashSet<>();
        for (WorkflowBlockItem item : items) {
            taskIds.add(item.getTaskNodeRef().getId());
        }
        Map<String, List<DueDateHistoryRecord>> records = getDueDateHistoryRecords(taskIds);
        for (WorkflowBlockItem item : items) {
            item.setDueDateHistoryRecords(records.get(item.getTaskNodeRef().getId()));
        }
        return items;
    }

    @Override
    public List<Integer> getWorkflowBlockItemRowNumbers(List<NodeRef> compoundWorkflows) {
        return getWorkflowBlockItems(compoundWorkflows, Collections.<Integer> emptyList(), new RowMapper<Integer>() {
            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt("num");
            }
        });
    }

    @Override
    public List<WorkflowBlockItem> getWorkflowBlockItems(List<NodeRef> compoundWorkflows, final Map<NodeRef, Boolean> checkWorkflowRights, final String workflowGroupTasksUrl) {
        return getWorkflowBlockItems(compoundWorkflows, checkWorkflowRights, workflowGroupTasksUrl, Collections.<Integer> emptyList());
    }

    @Override
    public List<WorkflowBlockItem> getWorkflowBlockItems(List<NodeRef> compoundWorkflows, final Map<NodeRef, Boolean> checkWorkflowRights, final String workflowGroupTasksUrl,
            List<Integer> rowsToLoad) {
        WorkflowBlockItemMapper rowMapper = new WorkflowBlockItemMapper(checkWorkflowRights, workflowGroupTasksUrl);
        return getWorkflowBlockItems(compoundWorkflows, rowsToLoad, rowMapper);
    }

    private <T> List<T> getWorkflowBlockItems(List<NodeRef> compoundWorkflows, List<Integer> rowsToLoad, RowMapper<T> rowMapper) {
        if (compoundWorkflows == null || compoundWorkflows.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List<NodeRef> savedCompoundWorkflows = filterUnsavedCompoundWorkflows(compoundWorkflows);

        if (savedCompoundWorkflows.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        boolean rowsLimited = rowsToLoad != null && !rowsToLoad.isEmpty();

        String taskQuery = "SELECT *, lag(wfc_owner_group, 1) over w AS prev_group, lead(wfc_owner_group, 1) over w AS next_group"
                + " FROM "
                + " (SELECT row_number() over ("
                + "                             ORDER BY wfs_searchable_compound_workflow_started_date_time, wfc_started_date_time, alf_child_assoc.assoc_index, alf_child_assoc.id, "
                + "                             (CASE WHEN (task_type = 'assignmentTask'"
                + "                                     AND (wfs_active IS NULL"
                + "                                             OR wfs_active = FALSE)) THEN 1 ELSE 0 END), index_in_workflow) AS num,"
                + "             alf_node.id AS workflow_node_id, delta_task.store_id, task_id, wfs_compound_workflow_id, workflow_id, wfc_owner_name, wfc_started_date_time, "
                + "             wfc_completed_date_time, wfs_due_date, wfc_creator_name, task_type, wfs_active, wfs_proposed_due_date, wfs_resolution, wfs_workflow_resolution,"
                + "             wfc_outcome, wfs_comment, wfc_owner_substitute_name, wfc_owner_group, wfc_status, index_in_workflow"
                + "             FROM delta_task"
                + "             LEFT JOIN alf_node ON (delta_task.workflow_id = alf_node.uuid"
                + "                     AND delta_task.wfs_compound_workflow_store_id = alf_node.store_id)"
                + "             LEFT JOIN alf_child_assoc ON (alf_node.id = alf_child_assoc.child_node_id)"
                + "             WHERE wfs_compound_workflow_id IN (" + DbSearchUtil.getQuestionMarks(savedCompoundWorkflows.size()) + ")"
                + "             ORDER BY num ) AS task_order window w AS (partition BY workflow_id"
                + "     ORDER BY num)"
                + " ORDER BY num";

        String sql = "SELECT *, lead(num, 1) over results AS next_num"
                + " FROM (WITH tasks AS ( " + taskQuery + " )"
                + " SELECT *, TRUE AS is_group FROM tasks WHERE " + getGroupCondition(rowsLimited, rowsToLoad)
                + " UNION ALL"
                + " SELECT *, FALSE AS is_group FROM tasks WHERE " + getItemCondition(rowsLimited, rowsToLoad)
                + " ) AS q window results AS (partition BY workflow_id ORDER BY num)"
                + " ORDER BY num";

        List<Object> arguments = DbSearchUtil.appendNodeRefIdQueryArguments(savedCompoundWorkflows);
        if (rowsLimited) {
            addLimitedRowArguments(rowsToLoad, arguments);
        }

        return jdbcTemplate.query(sql, rowMapper, arguments.toArray());
    }

    private String getItemCondition(boolean rowsLimited, List<Integer> rowsToLoad) {
        return "(wfc_owner_group IS NULL"
                + " OR ((next_group IS NULL"
                + "      OR wfc_owner_group <> next_group)"
                + "     AND (prev_group IS NULL"
                + "          OR prev_group IS NOT NULL"
                + "          AND wfc_owner_group <> prev_group))) "
                + getLimitedRowsCondition(rowsLimited, rowsToLoad);
    }

    private String getGroupCondition(boolean rowsLimited, List<Integer> rowsToLoad) {
        return "(wfc_owner_group = prev_group"
                + " OR wfc_owner_group = next_group)"
                + "AND (wfc_owner_group <> prev_group"
                + "     OR wfc_owner_group IS NOT NULL"
                + "     AND prev_group IS NULL) "
                + getLimitedRowsCondition(rowsLimited, rowsToLoad);
    }

    private String getLimitedRowsCondition(boolean rowsLimited, List<Integer> rowsToLoad) {
        return rowsLimited ? "AND num IN (" + DbSearchUtil.getQuestionMarks(rowsToLoad.size()) + ") " : "";
    }

    private void addLimitedRowArguments(List<Integer> rowsToLoad, List<Object> arguments) {
        // Add arguments for both single items and grouped items
        arguments.addAll(rowsToLoad);
        arguments.addAll(rowsToLoad);
    }

    private List<NodeRef> filterUnsavedCompoundWorkflows(List<NodeRef> compoundWorkflows) {
        List<NodeRef> compoundWfArguments = new ArrayList<>(compoundWorkflows);
        CollectionUtils.filter(compoundWfArguments, new Predicate<NodeRef>() {

            @Override
            public boolean eval(NodeRef compoundWorkflowNodeRef) {
                return RepoUtil.isSaved(compoundWorkflowNodeRef);
            }
        });

        return compoundWfArguments;
    }

    @Override
    public void createTaskEntry(Task task) {
        createTaskEntry(task, null);
    }

    @Override
    public void updateTaskEntry(Task task, Map<QName, Serializable> changedProps) {
        updateTaskEntry(task, changedProps, null);
    }

    @Override
    public void updateTaskEntry(Task task, Map<QName, Serializable> changedProps, NodeRef parentRef) {
        if (task == null) {
            return;
        }
        verifyTask(task, parentRef);
        TaskUpdateInfo updateInfo = getTaskUpdateInfo(task, parentRef, changedProps, null);
        if (!updateInfo.isUpdateNeeded()) {
            return;
        }
        updateTaskEntry(updateInfo);
    }

    @Override
    public void updateTaskEntryIgnoringParent(Task task, Map<QName, Serializable> changedProps) {
        TaskUpdateInfo updateInfo = getTaskUpdateInfo(task, null, changedProps, null);
        updateInfo.remove(WORKFLOW_ID_KEY);
        updateTaskEntry(updateInfo);
    }

    @Override
    public void updateTaskSingleProperty(Task task, QName key, Serializable value, NodeRef workflowRef) {
        Map<QName, Serializable> changedProps = new HashMap<>();
        changedProps.put(key, value);
        updateTaskEntry(task, changedProps, workflowRef);
    }

    @Override
    public void createTaskEntry(Task task, NodeRef workflowfRef) {
        createTaskEntry(task, workflowfRef, false);
    }

    @Override
    public void createTaskEntry(Task task, NodeRef workflowfRef, boolean isIndependentTask) {
        TaskUpdateInfo updateInfo = verifyTaskAndGetUpdateInfoOnCreate(task, (isIndependentTask ? null : workflowfRef), null);
        if (updateInfo == null) {
            return;
        }
        addTaskEntry(updateInfo);
    }

    @Override
    public void createTaskEntries(final List<TaskUpdateInfo> taskUpdateInfos, final Set<String> usedFieldNames) {
        String commaSeparatedUsedFieldNames = TextUtil.joinNonBlankStringsWithComma(usedFieldNames);
        final StringBuilder sql = new StringBuilder("INSERT INTO delta_task (" + commaSeparatedUsedFieldNames + ") VALUES ");
        String placeHolder = "(" + getQuestionMarks(usedFieldNames.size()) + ")";

        int finalIndex = taskUpdateInfos.size() - 1;
        for (int i = 0; i < taskUpdateInfos.size(); i++) {
            sql.append(placeHolder).append((i == finalIndex) ? "" : ",");
        }

        jdbcTemplate.update(new PreparedStatementCreator() {

            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement ps = con.prepareStatement(sql.toString());
                int fieldIndex = 1;
                for (TaskUpdateInfo info : taskUpdateInfos) {
                    for (String fieldName : usedFieldNames) {
                        Object value = info.getFieldValue(fieldName, FIELD_NAME_TO_TASK_PROP);
                        if (value instanceof List) {
                            value = getArrayValueForDb(value, con);
                        }
                        DbSearchUtil.setParameterValue(ps, fieldIndex, value);
                        fieldIndex++;
                    }
                }
                return ps;
            }
        });
    }

    @Override
    public void updateTaskEntries(List<TaskUpdateInfo> taskUpdateInfos, Set<String> usedFieldNames) {
        String sql = "UPDATE delta_task SET " + DbSearchUtil.createCommaSeparatedUpdateString(usedFieldNames) + " WHERE task_id=?";
        batchUpdate(taskUpdateInfos, usedFieldNames, sql, true);
    }

    private void batchUpdate(final List<TaskUpdateInfo> taskUpdateInfos, final Set<String> usedFieldNames, String sql, final boolean addTaskId) {
        try (Connection conn = dataSource.getConnection()) {

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    TaskUpdateInfo info = taskUpdateInfos.get(i);
                    int fieldIndex = 1;
                    for (String fieldName : usedFieldNames) {
                        Object value = info.getFieldValue(fieldName, FIELD_NAME_TO_TASK_PROP);
                        if (value instanceof List) {
                            value = getArrayValueForDb(value, conn);
                        }
                        DbSearchUtil.setParameterValue(ps, fieldIndex, value);
                        fieldIndex++;
                    }
                    if (addTaskId) {
                        ps.setObject(fieldIndex, info.getTaskId());
                    }
                }

                @Override
                public int getBatchSize() {
                    return taskUpdateInfos.size();
                }
            });
        } catch (SQLException e) {
            LOG.error("Unable to update tasks", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public TaskUpdateInfo verifyTaskAndGetUpdateInfoOnCreate(Task task, NodeRef workflowRef, Connection connection) {
        return verifyTaskAndGetUpdateInfo(task, workflowRef, true, RepoUtil.toQNameProperties(task.getNode().getProperties()), connection);
    }

    @Override
    public TaskUpdateInfo verifyTaskAndGetUpdateInfoOnUpdate(Task task, NodeRef workflowRef, Map<QName, Serializable> propsToSave, Connection connection) {
        return verifyTaskAndGetUpdateInfo(task, workflowRef, false, propsToSave, connection);
    }

    private TaskUpdateInfo verifyTaskAndGetUpdateInfo(Task task, NodeRef workflowfRef, boolean isNewTask, Map<QName, Serializable> props, Connection connection) {
        if (task == null) {
            return null;
        }

        verifyTask(task, workflowfRef);
        if (isNewTask) {
            Integer taskIndexInWorkflow = task.getTaskIndexInWorkflow();
            Assert.isTrue(taskIndexInWorkflow == null || taskIndexInWorkflow >= 0);
        }
        TaskUpdateInfo updateInfo = getTaskUpdateInfo(task, workflowfRef, props, connection);
        if (isNewTask) {
            updateInfo.add(IS_SEARCHABLE_FIELD, task.getNode().getAspects().contains(WorkflowSpecificModel.Aspects.SEARCHABLE));
        }
        return updateInfo;
    }

    @Override
    public void updateTaskProperties(NodeRef taskRef, Map<QName, Serializable> props) {
        if (taskRef == null || RepoUtil.isUnsaved(taskRef)) {
            throw new RuntimeException("Task is not saved, unable to update! taskRef=" + taskRef);
        }
        if (props == null || props.isEmpty()) {
            return;
        }
        TaskUpdateInfo updateInfo = new TaskUpdateInfo(taskRef);
        populateTaskUpdateInfo(updateInfo, props, null);
        updateTaskEntry(updateInfo);
    }

    @Override
    public int[] updateCompoundWorkflowTaskSearchableProperties(final List<Pair<String, Map<QName, Serializable>>> compoundWorkflowtaskSearchableProps,
            final List<QName> compoundWorkflowTaskSearchableProperties, String compoundWorkflowTaskUpdateString) {

        String sql = "UPDATE delta_task SET " + compoundWorkflowTaskUpdateString + " WHERE wfs_compound_workflow_id=?";
        Connection connection = null;
        try {
            // TODO: get "text" constant value from connection? It is database-specific
            connection = dataSource.getConnection();
            final Connection finalConnection = connection;
            return jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Pair<String, Map<QName, Serializable>> pair = compoundWorkflowtaskSearchableProps.get(i);
                    int fieldCounter = 1;
                    Map<QName, Serializable> props = pair.getSecond();
                    for (QName prop : compoundWorkflowTaskSearchableProperties) {
                        Object value = props.get(prop);
                        if ((WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.equals(prop)
                                || WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_ORGANIZATION_NAME.equals(prop)) && value != null) {
                            value = getArrayValueForDb(value, finalConnection);
                        }
                        DbSearchUtil.setParameterValue(ps, fieldCounter++, value);
                    }
                    ps.setObject(fieldCounter, pair.getFirst());
                }

                @Override
                public int getBatchSize() {
                    return compoundWorkflowtaskSearchableProps.size();
                }
            });
        } catch (SQLException e) {
            LOG.error("Error creating owner organization name input", e);
            throw new RuntimeException(e);
        } finally {
            WorkflowUtil.closeConnection(connection);
        }
    }

    @Override
    public int updateTaskPropertiesAndStorRef(NodeRef taskRef, Map<QName, Serializable> props) {
        TaskUpdateInfo info = new TaskUpdateInfo(taskRef);
        info.add(STORE_ID_FIELD, taskRef.getStoreRef().toString());
        populateTaskUpdateInfo(info, props, null);
        return updateTaskEntry(info);
    }

    @Override
    public void updateWorkflowTasksStore(NodeRef workflowRef, StoreRef newStoreRef) {
        boolean hasWorkflowRef = workflowRef != null;
        String sqlQuery = "UPDATE delta_task SET store_id=? WHERE workflow_id" + (hasWorkflowRef ? "=?" : " IS NULL");
        Object[] args;
        if (hasWorkflowRef) {
            args = new Object[] { newStoreRef.toString(), workflowRef.getId() };
        } else {
            args = new Object[] { newStoreRef.toString() };
        }
        jdbcTemplate.update(sqlQuery, args);
        explainQuery(sqlQuery, args);
    }

    @Override
    public void updateWorkflowTaskProperties(NodeRef workflowRef, Map<QName, Serializable> newProps) {
        String sqlQuery = "SELECT task_id FROM delta_task where workflow_id=?";
        String parentId = workflowRef.getId();
        List<String> tasks = jdbcTemplate.query(sqlQuery,
                new ParameterizedRowMapper<String>() {

                    @Override
                    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return rs.getString(1);
                    }
                },
                parentId);
        explainQuery(sqlQuery, parentId);
        for (String taskId : tasks) {
            updateTaskProperties(new NodeRef(workflowRef.getStoreRef(), taskId), newProps);
        }
    }

    private void explainQuery(String sqlQuery, Object... args) {
        generalService.explainQuery(sqlQuery, LOG, args);
    }

    @Override
    public NodeRef getTaskParentNodeRef(NodeRef nodeRef) {
        String sqlQuery = "SELECT store_id, workflow_id FROM delta_task where task_id=?";
        String nodeRefId = nodeRef.getId();
        List<NodeRef> parentRefs = jdbcTemplate.query(sqlQuery, new TaskParentNodeRefMapper(), nodeRefId);
        explainQuery(sqlQuery, nodeRefId);
        return parentRefs == null || parentRefs.isEmpty() ? null : parentRefs.get(0);
    }

    @Override
    public QName getTaskType(NodeRef nodeRef) {
        Map<QName, QName> taskPrefixedQNames = workflowConstantsBean.getTaskPrefixedQNames();
        String sqlQuery = "SELECT task_type FROM delta_task where task_id=?";
        String nodeRefId = nodeRef.getId();
        String taskType = jdbcTemplate.queryForObject(sqlQuery, String.class, nodeRefId);
        explainQuery(sqlQuery, nodeRefId);
        for (Map.Entry<QName, QName> entry : taskPrefixedQNames.entrySet()) {
            if (entry.getKey().getLocalName().equals(taskType)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public boolean taskExists(NodeRef taskRef) {
        String sqlQuery = "SELECT COUNT(1) FROM delta_task WHERE task_id=?";
        String nodeRefId = taskRef.getId();
        int rowCount = jdbcTemplate.queryForInt(sqlQuery, nodeRefId);
        explainQuery(sqlQuery, nodeRefId);
        return rowCount > 0;
    }

    @Override
    public Serializable getTaskProperty(NodeRef taskRef, final QName qname) {
        final String fieldName = getDbFieldName(qname);
        String taskId = taskRef.getId();
        String sqlQuery = "SELECT " + fieldName + " FROM delta_task WHERE task_id=?";
        List<Serializable> results = jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<Serializable>() {

            @Override
            public Serializable mapRow(ResultSet rs, int rowNum) throws SQLException {
                Object value = rs.getObject(fieldName);
                return getConvertedValue(rs, qname, fieldName, value);
            }

        }, taskId);
        explainQuery(sqlQuery, taskId);
        if (results.isEmpty()) {
            throw new RuntimeException("Task with id=" + taskId + " does not exist.");
        }
        return results.get(0);
    }

    private String getDbFieldName(QName qname) {
        String fieldName = TASK_PROP_TO_FIELD_NAME.get(qname);

        if (fieldName == null) {
            fieldName = getDbFieldNameFromPropQName(qname);
            TASK_PROP_TO_FIELD_NAME.put(qname, fieldName);
            QName previous = FIELD_NAME_TO_TASK_PROP.put(fieldName, qname);
            if (previous != null && previous.equals(qname)) {
                throw new RuntimeException("Field name and task prop conversions should produce uniform results.");
            }
        }

        return fieldName;
    }

    @Override
    public boolean hasInProgressOtherUserOrderAssignmentTasks(String userName, List<NodeRef> compoundWorkflowRefs) {
        List<Object> arguments = new ArrayList<>();
        String sql = "SELECT EXISTS (SELECT 1 FROM delta_task WHERE task_type = 'assignmentTask' "
                + " AND wfs_compound_workflow_id IN (" + DbSearchUtil.getQuestionMarks(compoundWorkflowRefs.size()) + ")"
                + " AND wfc_status = '" + Status.IN_PROGRESS.getName() + "' AND wfc_owner_id = ?)";
        arguments.addAll(RepoUtil.getNodeRefIds(compoundWorkflowRefs));
        arguments.add(userName);
        return jdbcTemplate.queryForObject(sql, Boolean.class, arguments.toArray());
    }

    @Override
    public boolean containsTaskOfType(List<NodeRef> compoundWorkflowRefs, QName... taskTypes) {
        if (compoundWorkflowRefs == null || compoundWorkflowRefs.isEmpty()) {
            return false;
        }
        List<Object> arguments = new ArrayList<>();
        String sql = "SELECT EXISTS (SELECT 1 FROM delta_task WHERE wfs_compound_workflow_id IN (" + DbSearchUtil.getQuestionMarks(compoundWorkflowRefs.size()) + ") "
                + " AND task_type IN (" + DbSearchUtil.getQuestionMarks(taskTypes.length) + ") )";
        arguments.addAll(RepoUtil.getNodeRefIds(compoundWorkflowRefs));
        for (QName taskType : taskTypes) {
            arguments.add(taskType.getLocalName());
        }
        return jdbcTemplate.queryForObject(sql, Boolean.class, arguments.toArray());
    }

    private int updateTaskEntry(TaskUpdateInfo taskInfo) {
        return updateTaskEntry(taskInfo, true);
    }

    private int updateTaskEntry(TaskUpdateInfo taskInfo, boolean throwIfNotSingleUpdate) {
        String sqlQuery = "UPDATE delta_task SET " + taskInfo.getParameterizedUpdateString() + " WHERE task_id=?";
        Object[] arguments = taskInfo.getArgumentArrayWithTaskId();
        int rowsUpdated = jdbcTemplate.update(sqlQuery, arguments);
        explainQuery(sqlQuery, arguments);
        if (rowsUpdated != 1 && throwIfNotSingleUpdate) {
            throw new RuntimeException("Update failed: updated " + rowsUpdated + " rows for task nodeRef=" + taskInfo.getTaskNodeRef() + ", sql='" + sqlQuery + "', arguments="
                    + arguments);
        }
        return rowsUpdated;
    }

    private void addTaskEntry(TaskUpdateInfo updateInfo) {
        jdbcTemplate.update("INSERT INTO delta_task (" + updateInfo.getFieldNameListing() + ") VALUES (" + updateInfo.getArgumentQuestionMarks() + ")"
                , updateInfo.getArgumentArray());
    }

    private void verifyTask(Task task, NodeRef workflowRef) {
        Assert.notNull(task);
        Workflow workflow = task.getParent();
        Assert.isTrue(workflow != null || workflowRef != null);
        NodeRef taskRef = task.getNodeRef();
        NodeRef wfRef = workflow != null ? workflow.getNodeRef() : workflowRef;
        Assert.isTrue(isSaved(taskRef) && isSaved(wfRef) && task.getType() != null);
    }

    private TaskUpdateInfo getTaskUpdateInfo(Task task, NodeRef workflowfRef, Map<QName, Serializable> changedProps, Connection connection) {
        TaskUpdateInfo updateInfo = new TaskUpdateInfo(task);

        Integer taskIndexInWorkflow = task.getTaskIndexInWorkflow();
        boolean addTaskIndexInWorkflow = taskIndexInWorkflow != null && taskIndexInWorkflow >= 0;
        String workflowId = task.getParent() != null ? task.getParent().getNodeRef().getId() : (workflowfRef != null ? workflowfRef.getId() : null);

        updateInfo.add(TASK_ID_FIELD, task.getNodeRef().getId());
        updateInfo.add(WORKFLOW_ID_KEY, workflowId);
        updateInfo.add(TASK_TYPE_FIELD, task.getType().getLocalName());
        updateInfo.add(STORE_ID_FIELD, task.getNodeRef().getStoreRef().toString());

        if (addTaskIndexInWorkflow) {
            updateInfo.add(INDEX_IN_WORKFLOW_FIELD, taskIndexInWorkflow);
        }
        populateTaskUpdateInfo(updateInfo, changedProps, connection);
        return updateInfo;
    }

    private void populateTaskUpdateInfo(TaskUpdateInfo updateInfo, @SuppressWarnings("rawtypes") Map taskProps, Connection connection) {
        if (taskProps == null) {
            return;
        }
        for (Object entryObj : taskProps.entrySet()) {
            @SuppressWarnings("unchecked")
            Map.Entry<QName, Serializable> entry = (Map.Entry<QName, Serializable>) entryObj;
            QName propName = entry.getKey();
            if (!WorkflowCommonModel.URI.equals(propName.getNamespaceURI()) && !WorkflowSpecificModel.URI.equals(propName.getNamespaceURI())) {
                continue;
            }
            if (NOT_SENT_TO_REPO_PROPS.contains(propName)) {
                continue;
            }
            Object value = entry.getValue();
            if ((WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.equals(propName)
                    || WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_ORGANIZATION_NAME.equals(propName)) && value != null) {
                value = getArrayValueForDb(value, connection);
            }
            updateInfo.add(getDbFieldName(propName), value);
        }
    }

    /**
     * @param connection - if not null, use provided connection for retrieving value. Connection is not closed. If connection is null, new connection is fetched and also closed
     *            after retrieving data.
     * @return
     */
    private Object getArrayValueForDb(Object value, Connection connection) {
        Object[] valueArray = ((List<String>) value).toArray();
        if (connection != null) {
            try {
                return connection.createArrayOf("text", valueArray);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            connection = dataSource.getConnection();
            return connection.createArrayOf("text", valueArray);
        } catch (SQLException e) {
            LOG.error("Error creating owner organization name input", e);
            throw new RuntimeException(e);
        } finally {
            WorkflowUtil.closeConnection(connection);
        }
    }

    @Override
    public List<Task> getWorkflowTasks(NodeRef originalParentRef, Collection<QName> taskDataTypeDefaultAspects, List<QName> taskDataTypeDefaultProps,
            Map<QName, QName> taskPrefixedQNames, WorkflowType workflowType, Workflow workflow, boolean copy) {
        String sqlQuery = "SELECT delta_task.*, initiating_task.wfs_compound_workflow_id as initiating_compound_workflow_id, " +
                " initiating_task.wfs_compound_workflow_title as initiating_compound_workflow_title, " +
                " initiating_task.store_id as initiating_compound_workflow_store_id " +
                " FROM delta_task " +
                " left join delta_task_due_date_extension_assoc ext_assoc on ext_assoc.extension_task_id = delta_task.task_id " +
                " left join delta_task initiating_task on ext_assoc.task_id = initiating_task.task_id " +
                " where delta_task.workflow_id=? ORDER BY index_in_workflow";
        String parentId = originalParentRef.getId();
        List<Task> tasks = jdbcTemplate.query(sqlQuery,
                new TaskRowMapper(originalParentRef, taskDataTypeDefaultAspects, taskDataTypeDefaultProps, taskPrefixedQNames, workflowType, workflow, copy, false),
                parentId);
        explainQuery(sqlQuery, parentId);
        return tasks;
    }

    @Override
    public List<NodeRef> getWorkflowTaskNodeRefs(NodeRef workflowRef) {
        String sqlQuery = "SELECT task_id, store_id FROM delta_task where workflow_id=? ORDER BY index_in_workflow";
        String parentId = workflowRef.getId();
        List<NodeRef> taskRefs = jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<NodeRef>() {

            @Override
            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                return nodeRefFromRs(rs, TASK_ID_FIELD);
            }
        }, parentId);
        explainQuery(sqlQuery, parentId);
        return taskRefs;
    }

    @Override
    public Pair<List<Task>, Boolean> searchTasksMainStore(String queryCondition, List<Object> arguments, int limit) {
        return searchTasks(queryCondition, arguments, limit, BeanHelper.getGeneralService().getStore(), null);
    }

    @Override
    public Pair<List<Task>, Boolean> searchTasksAllStores(String queryCondition, List<Object> arguments, int limit) {
        return searchTasks(queryCondition, arguments, limit, null, null);
    }

    @Override
    public <T extends Object> Pair<List<T>, Boolean> searchTasksAllStores(String queryCondition, List<Object> arguments, int limit, RowMapper<T> rowMapper) {
        return searchTasks(queryCondition, arguments, limit, null, rowMapper);
    }

    private <T extends Object> Pair<List<T>, Boolean> searchTasks(String queryCondition, List<Object> arguments, int limit, StoreRef storeRef, RowMapper<T> rowMapper) {
        if (StringUtils.isBlank(queryCondition)) {
            return new Pair<List<T>, Boolean>(new ArrayList<T>(), Boolean.FALSE);
        }
        boolean hasStoreRef = storeRef != null;
        Set<StoreRef> storeRefs = new LinkedHashSet<StoreRef>();
        if (hasStoreRef) {
            storeRefs.add(storeRef);
        } else {
            storeRefs.addAll(BeanHelper.getGeneralService().getAllWithArchivalsStoreRefs());
        }
        for (StoreRef store : storeRefs) {
            arguments.add(0, store.toString());
        }
        boolean limited = limit > -1;
        RowMapper<T> taskRowMapper;
        if (rowMapper != null) {
            taskRowMapper = rowMapper;
        } else {
            taskRowMapper = new TaskRowMapper(null, null, null, workflowConstantsBean.getTaskPrefixedQNames(), null, null, false, false);
        }
        String sqlQuery = "SELECT * FROM delta_task WHERE "
                + SearchUtil.joinQueryPartsAnd(" is_searchable = true ", "store_id IN (" + getQuestionMarks(storeRefs.size()) + ")", queryCondition)
                + (limited ? (" LIMIT " + (limit + 1)) : "");
        Object[] argumentsArray = arguments.toArray();
        List<T> tasks = jdbcTemplate.query(sqlQuery, taskRowMapper, argumentsArray);
        explainQuery(sqlQuery, argumentsArray);
        boolean limitedResult = limit > 0 && limit <= tasks.size();
        if (limitedResult) {
            tasks.remove(tasks.size() - 1);
        }

        return new Pair<List<T>, Boolean>(tasks, limitedResult);
    }

    @Override
    public Pair<List<NodeRef>, Boolean> searchTaskNodeRefs(String queryCondition, List<Object> arguments, int limit) {
        boolean useLimit = limit > -1;
        String sqlQuery = "SELECT task_id, store_id FROM delta_task WHERE "
                + SearchUtil.joinQueryPartsAnd(getSearchableAndNotInArchiveSpacesstoreCondition(), queryCondition) + (useLimit ? " LIMIT " + (limit + 1) : "");
        Object[] argumentsArray = arguments.toArray();
        List<NodeRef> taskRefs = jdbcTemplate.query(sqlQuery, new TaskNodeRefRowMapper(), argumentsArray);
        explainQuery(sqlQuery, argumentsArray);
        boolean limitedResult = false;
        if (useLimit && taskRefs.size() > limit) {
            limitedResult = true;
            taskRefs.remove(taskRefs.size() - 1);
        }
        return Pair.newInstance(taskRefs, limitedResult);
    }

    @Override
    public Pair<List<Pair<NodeRef, QName>>, Boolean> searchTaskNodeRefAndType(String queryCondition, String orderClause, List<Object> arguments, int limit) {
        boolean useLimit = limit > -1;
        String sqlQuery = "SELECT task_id, store_id, task_type FROM delta_task WHERE "
                + SearchUtil.joinQueryPartsAnd(getSearchableAndNotInArchiveSpacesstoreCondition(), queryCondition) + (useLimit ? " LIMIT " + (limit + 1) : "");
        if (StringUtils.isNotBlank(orderClause)) {
            sqlQuery += orderClause;
        }
        Object[] argumentsArray = arguments.toArray();
        List<Pair<NodeRef, QName>> taskRefs = jdbcTemplate.query(sqlQuery, new TaskNodeRefAndTypeRowMapper(), argumentsArray);
        explainQuery(sqlQuery, argumentsArray);
        boolean limitedResult = false;
        if (useLimit && taskRefs.size() > limit) {
            limitedResult = true;
            taskRefs.remove(taskRefs.size() - 1);
        }
        return Pair.newInstance(taskRefs, limitedResult);
    }

    @Override
    public Pair<List<NodeRef>, Boolean> searchTaskNodeRefsCheckLimitedSeries(String queryCondition, final String userId, List<Object> arguments, final int limit) {
        DocumentSearchService documentSearchService = BeanHelper.getDocumentSearchService();
        boolean isAdministrator = BeanHelper.getUserService().isAdministrator(userId);
        final List<NodeRef> restrictedSeries = new ArrayList<>();
        if (!isAdministrator) {
            restrictedSeries.addAll(documentSearchService.searchRestrictedSeries(documentSearchService.getAllStoresWithArchivalStoreVOs()));
        }
        if (restrictedSeries.isEmpty() || BeanHelper.getUserService().isAdministrator()) {
            return searchTaskNodeRefs(queryCondition, arguments, limit);
        }
        final BulkLoadNodeService bulkLoadNodeService = BeanHelper.getBulkLoadNodeService();
        // 1) get all tasks that match search criteria without checking permissions for restricted series.
        // Retrieve document and series nodeRef to perform additional check later if needed
        String sqlQuery = "SELECT task_id, task.store_id, series_prop.string_value as series_ref, owner_prop.string_value as owner_id, "
                + " document.store_id as document_store_id, document.uuid as document_uuid FROM delta_task task "
                + " left join alf_node compound_workflow on "
                + "         (task.wfs_compound_workflow_store_id = compound_workflow.store_id and task.wfs_compound_workflow_id = compound_workflow.uuid "
                + "         and compound_workflow.type_qname_id = "
                + bulkLoadNodeService.getQNameDbId(WorkflowCommonModel.Types.COMPOUND_WORKFLOW)
                + ")"
                + " left join alf_child_assoc doc_assoc on doc_assoc.child_node_id = compound_workflow.id "
                + " left join alf_node document on document.id = doc_assoc.parent_node_id and document.type_qname_id = "
                + bulkLoadNodeService.getQNameDbId(DocumentCommonModel.Types.DOCUMENT)
                + " left join alf_node_properties series_prop on document.id = series_prop.node_id and series_prop.qname_id = "
                + bulkLoadNodeService.getQNameDbId(DocumentCommonModel.Props.SERIES)
                + " left join alf_node_properties owner_prop on document.id = owner_prop.node_id and owner_prop.qname_id = "
                + bulkLoadNodeService.getQNameDbId(DocumentCommonModel.Props.OWNER_ID);

        sqlQuery += " WHERE " + SearchUtil.joinQueryPartsAnd(getSearchableAndNotInArchiveSpacesstoreCondition("task"), queryCondition);

        Object[] argumentsArray = arguments.toArray();
        final boolean useLimit = limit > -1;
        final List<NodeRef> userAvailableTasks = new ArrayList<>();
        final Map<NodeRef, NodeRef> restrictedTaskDocuments = new HashMap<>();
        jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<Void>() {
            private boolean enoughTasksRetrieved = false;

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                if (enoughTasksRetrieved) {
                    return null;
                }
                NodeRef taskRef = nodeRefFromRs(rs, TASK_ID_FIELD);
                String seriesRefStr = rs.getString("series_ref");
                NodeRef seriesRef = StringUtils.isNotBlank(seriesRefStr) ? new NodeRef(seriesRefStr) : null;
                if (seriesRef != null && restrictedSeries.contains(seriesRef)) {
                    String ownerId = rs.getString("owner_id");
                    if (!userId.equals(ownerId)) {
                        restrictedTaskDocuments.put(taskRef,
                                new NodeRef(bulkLoadNodeService.getStoreRefByDbId(rs.getLong("document_store_id")), rs.getString("document_uuid")));
                    }
                } else {
                    userAvailableTasks.add(taskRef);
                    if (useLimit && userAvailableTasks.size() > limit) {
                        enoughTasksRetrieved = true;
                    }
                }
                return null;
            }
        }, argumentsArray);

        int retrievedtaskCount = userAvailableTasks.size();
        if (useLimit && retrievedtaskCount >= limit) {
            boolean limited = false;
            if (retrievedtaskCount > limit) {
                userAvailableTasks.remove(retrievedtaskCount - 1);
                limited = true;
            }
            return Pair.newInstance(userAvailableTasks, limited);
        }

        // 2) not enough unrestricted tasks were found, execute additional query to verify restricted series tasks' permissions
        Set<NodeRef> restrictedDocumentsSet = new HashSet<>(restrictedTaskDocuments.values());
        List<List<NodeRef>> slicedRestrictedDocuments = RepoUtil.sliceList(new ArrayList<>(restrictedDocumentsSet), 10000);
        List<String> authorities = new ArrayList<>();
        authorities.add(userId);
        authorities.addAll(BeanHelper.getAuthorityService().getContainingAuthorities(AuthorityType.GROUP, userId, false));

        OUTER: for (List<NodeRef> restrictedTasksSlice : slicedRestrictedDocuments) {
            Set<NodeRef> allowedDocRefs = BeanHelper.getPrivilegeService().getNodeRefWithSetViewPrivilege(restrictedTasksSlice, authorities);
            for (Map.Entry<NodeRef, NodeRef> entry : restrictedTaskDocuments.entrySet()) {
                if (allowedDocRefs.contains(entry.getValue())) {
                    userAvailableTasks.add(entry.getKey());
                    if (userAvailableTasks.size() > limit) {
                        break OUTER;
                    }
                }
            }
        }
        boolean limited = false;
        retrievedtaskCount = userAvailableTasks.size();
        if (retrievedtaskCount > limit) {
            userAvailableTasks.remove(retrievedtaskCount - 1);
            limited = true;
        }
        return Pair.newInstance(userAvailableTasks, limited);
    }

    private String getSearchableAndNotInArchiveSpacesstoreCondition() {
        return getSearchableAndNotInArchiveSpacesstoreCondition("");
    }

    private String getSearchableAndNotInArchiveSpacesstoreCondition(String tableName) {
        Set<StoreRef> stores = generalService.getAllWithArchivalsStoreRefs();
        List<StoreRef> storeList = new ArrayList<>(stores);
        String storesCondition = "( ";
        int lastIndex = storeList.size() - 1;
        for (int i = 0; i < lastIndex; i++) {
            storesCondition += "'" + storeList.get(i) + "', ";
        }
        storesCondition += "'" + storeList.get(lastIndex) + "' )";
        tableName = StringUtils.isNotBlank(tableName) ? (tableName + ".") : "";
        return tableName + "is_searchable = true AND " + tableName + "store_id in " + storesCondition;
    }

    @Override
    public Map<String, Integer> countAllCurrentUserTasks() {
        final Map<String, Integer> counts = new HashMap<>();
        String userName = AuthenticationUtil.getRunAsUser();
        String sql = "SELECT task_type, count(*) FROM delta_task" +
                " WHERE wfc_owner_id= ?" +
                " AND wfc_status='" + Status.IN_PROGRESS.getName() + "'" +
                " AND " + getSearchableAndNotInArchiveSpacesstoreCondition() +
                " GROUP BY task_type;";
        jdbcTemplate.query(sql, new RowMapper<Void>() {
            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                counts.put(rs.getString(TaskSearchUtil.TASK_TYPE_FIELD), rs.getInt("count"));
                return null;
            }
        }, userName);
        return counts;
    }

    @Override
    public Map<QName, Integer> countTasksByType(String queryCondition, List<Object> arguments, final QName... taskType) {
        final Map<QName, Integer> counts = new HashMap<QName, Integer>();
        if (taskType == null || taskType.length == 0) {
            return counts;
        }

        Set<StoreRef> storeRefs = BeanHelper.getGeneralService().getAllWithArchivalsStoreRefs();
        for (StoreRef store : storeRefs) {
            arguments.add(0, store.toString());
        }

        // Duplicate arguments for count queries. TODO Could we substitute the '?' only once?
        List<Object> arg = new ArrayList<Object>();
        for (int i = 0; i <= taskType.length; i++) {
            arg.addAll(arguments);
        }
        Object[] argumentsArray = arg.toArray();

        String restriction = SearchUtil.joinQueryPartsAnd(" is_searchable = true ", "store_id IN (" + getQuestionMarks(storeRefs.size()) + ")", queryCondition);

        StringBuilder sql = new StringBuilder("SELECT");
        boolean first = true;
        for (QName type : taskType) {
            if (!first) {
                sql.append(", ");
            }
            first = false;

            sql.append(" (SELECT COUNT(*) FROM delta_task WHERE task_type = '").append(type.getLocalName()).append("' AND ")
                    .append(restriction)
                    .append(") AS ").append(type.getLocalName());
        }
        sql.append(" FROM delta_task WHERE")
                .append(restriction)
                .append(" LIMIT 1;");
        String sqlQuery = sql.toString();

        // NB! There will be only one row!
        jdbcTemplate.query(sqlQuery, new RowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                // Populate results map
                for (QName type : taskType) {
                    counts.put(type, rs.getInt(type.getLocalName().toLowerCase()));
                }

                return null;
            }

        }, argumentsArray);
        explainQuery(sqlQuery, argumentsArray);

        return counts;
    }

    @Override
    public int countTasks(String queryCondition, List<Object> arguments) {
        Set<StoreRef> storeRefs = BeanHelper.getGeneralService().getAllWithArchivalsStoreRefs();
        for (StoreRef store : storeRefs) {
            arguments.add(0, store.toString());
        }
        String sqlQuery = "SELECT COUNT(1) FROM delta_task WHERE "
                + SearchUtil.joinQueryPartsAnd(" is_searchable = true ", "store_id IN (" + getQuestionMarks(storeRefs.size()) + ")", queryCondition);
        Object[] argumentsArray = arguments.toArray();
        int taskCount = jdbcTemplate.queryForInt(sqlQuery, argumentsArray);
        explainQuery(sqlQuery, argumentsArray);
        return taskCount;
    }

    @Override
    public Map<NodeRef, Pair<String, String>> searchTaskSendStatusInfo(String queryCondition, List<Object> arguments) {
        if (StringUtils.isBlank(queryCondition)) {
            return new HashMap<NodeRef, Pair<String, String>>();
        }
        String sqlQuery = "SELECT task_id, store_id, wfs_sent_dvk_id, wfs_institution_code FROM delta_task WHERE "
                + SearchUtil.joinQueryPartsAnd(" is_searchable = true ", queryCondition);
        Object[] argumentsArray = arguments.toArray();
        List<Pair<NodeRef, Pair<String, String>>> taskSendInfos = jdbcTemplate.query(sqlQuery,
                new ParameterizedRowMapper<Pair<NodeRef, Pair<String, String>>>() {

                    @Override
                    public Pair<NodeRef, Pair<String, String>> mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new Pair<NodeRef, Pair<String, String>>(nodeRefFromRs(rs, TASK_ID_FIELD), new Pair<String, String>(rs.getString("wfs_sent_dvk_id"), rs
                                .getString("wfs_institution_code")));
                    }

                }, argumentsArray);
        explainQuery(sqlQuery, argumentsArray);
        Map<NodeRef, Pair<String, String>> sendInfosMap = new HashMap<NodeRef, Pair<String, String>>();
        for (Pair<NodeRef, Pair<String, String>> sendInfo : taskSendInfos) {
            sendInfosMap.put(sendInfo.getFirst(), sendInfo.getSecond());
        }
        return sendInfosMap;
    }

    private NodeRef nodeRefFromRs(ResultSet rs, String nodeRefIdField) throws SQLException {
        String nodeRefId = rs.getString(nodeRefIdField);
        if (StringUtils.isNotBlank(nodeRefId)) {
            return new NodeRef(new StoreRef(rs.getString(STORE_ID_FIELD)), nodeRefId);
        }
        return null;
    }

    @Override
    public Task getTask(NodeRef nodeRef, Workflow workflow, boolean copy) {
        Map<NodeRef, Task> tasks = getTasks(Arrays.asList(nodeRef), workflow, copy, null);
        int taskCount = tasks.size();
        if (taskCount != 1) {
            throw new RuntimeException("Expected to find one task, found " + taskCount + " for nodeRef=" + nodeRef);
        }
        return tasks.get(nodeRef);
    }

    @Override
    public Map<NodeRef, Task> getTasks(List<NodeRef> taskRefs) {
        return getTasks(taskRefs, null, false, null);
    }

    @Override
    public Map<NodeRef, Task> getTasksWithCompoundWorkflowRef(List<NodeRef> taskRefs) {
        return getTasks(taskRefs, null, false, null, true);
    }

    @Override
    public Map<NodeRef, Task> getTasks(List<NodeRef> taskRefs, Workflow workflow, boolean copy, Set<QName> propsToLoad) {
        return getTasks(taskRefs, workflow, copy, propsToLoad, false);
    }

    private Map<NodeRef, Task> getTasks(List<NodeRef> taskRefs, Workflow workflow, boolean copy, Set<QName> propsToLoad, boolean loadCompoundWorkflowRef) {
        if (taskRefs == null || taskRefs.isEmpty()) {
            return new HashMap<>();
        }
        String sqlQuery = "SELECT " + createSelectFieldListing(propsToLoad) + " FROM delta_task where task_id IN (" + getQuestionMarks(taskRefs.size()) + ")";
        List<String> params = new ArrayList<String>();
        for (NodeRef taskRef : taskRefs) {
            params.add(taskRef.getId());
        }
        Object[] paramArray = params.toArray();
        List<Task> tasks = jdbcTemplate
                .query(sqlQuery,
                        new TaskRowMapper(null, null, null, getWorkflowConstantsBean().getTaskPrefixedQNames(), null, workflow, copy, propsToLoad != null
                                && !propsToLoad.isEmpty(), loadCompoundWorkflowRef),
                        paramArray);
        explainQuery(sqlQuery, paramArray);
        Map<NodeRef, Task> result = new HashMap<NodeRef, Task>();
        for (Task task : tasks) {
            result.put(task.getNodeRef(), task);
        }
        return result;
    }

    private String createSelectFieldListing(Set<QName> propsToLoad) {
        if (propsToLoad == null || propsToLoad.isEmpty()) {
            return " * ";
        }
        List<String> fieldNames = DbSearchUtil.getDbFieldNamesFromPropQNames(propsToLoad.toArray(new QName[propsToLoad.size()]));
        return STORE_ID_FIELD + ", " + TASK_ID_FIELD + ", " + TextUtil.joinNonBlankStringsWithComma(fieldNames);
    }

    @Override
    public List<Task> getDueDateExtensionInitiatingTask(NodeRef nodeRef, Map<QName, QName> taskPrefixedQNames) {
        String sqlQuery = "SELECT * FROM delta_task where task_id in (SELECT task_id FROM delta_task_due_date_extension_assoc where extension_task_id=?)";
        String nodeRefId = nodeRef.getId();
        List<Task> tasks = jdbcTemplate.query(sqlQuery,
                new TaskRowMapper(null, null, null, taskPrefixedQNames, null, null, false, false),
                nodeRefId);
        explainQuery(sqlQuery, nodeRefId);
        return tasks;
    }

    @Override
    public void createTaskDueDateExtensionAssocEntry(NodeRef initiatingTaskRef, NodeRef nodeRef) {
        int rowsInserted = jdbcTemplate.update(
                "INSERT INTO delta_task_due_date_extension_assoc (task_id, extension_task_id) VALUES (?, ?)",
                new Object[] { initiatingTaskRef.getId(), nodeRef.getId() });
        if (rowsInserted != 1) {
            throw new RuntimeException("Insert failed: inserted " + rowsInserted + " rows for initiatingNodeRef=" + initiatingTaskRef + ", targetNodeRef=" + nodeRef);
        }
    }

    @Override
    public void createTaskDueDateHistoryEntries(NodeRef taskRef, final List<DueDateHistoryRecord> historyRecords) {
        Assert.notNull(taskRef);
        if (historyRecords == null || historyRecords.isEmpty()) {
            LOG.info("No history record input provided, skipping insert");
            return;
        }
        final String taskRefId = taskRef.getId();
        jdbcTemplate.batchUpdate("INSERT INTO delta_task_due_date_history (task_id, previous_date, change_reason, extension_task_id) VALUES (?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, taskRefId);
                        DueDateHistoryRecord record = historyRecords.get(i);
                        Date previousDate = record.getPreviousDate();
                        ps.setTimestamp(2, previousDate != null ? new Timestamp(previousDate.getTime()) : null);
                        ps.setString(3, record.getChangeReason());
                        ps.setString(4, record.getExtensionTaskId());
                    }

                    @Override
                    public int getBatchSize() {
                        return historyRecords.size();
                    }
                });
    }

    @Override
    public Map<String, List<DueDateHistoryRecord>> getDueDateHistoryRecords(Set<String> taskIds) {
        final Map<String, List<DueDateHistoryRecord>> result = new HashMap<>();
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(taskIds)) {
            return result;
        }
        String query = "SELECT history.previous_date, history.change_reason, history.task_id, history.extension_task_id, task.wfs_compound_workflow_id, task.store_id " +
                " FROM delta_task_due_date_history history " +
                " LEFT JOIN delta_task task ON task.task_id = history.extension_task_id " +
                " WHERE history.task_id IN (" + DbSearchUtil.getQuestionMarks(taskIds.size()) + ") ORDER BY history.task_due_date_history_id";

        jdbcTemplate.query(query, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                Date previousDate = (Date) rs.getObject("previous_date");
                String changeReason = (String) rs.getObject("change_reason");
                String compoundWorkflowId = (String) rs.getObject("wfs_compound_workflow_id");
                String taskId = (String) rs.getObject("task_id");
                String extensionTaskId = (String) rs.getObject("extension_task_id");
                String storeId = (String) rs.getObject("store_id");
                StoreRef storeRef = StringUtils.isNotBlank(storeId) ? new StoreRef(storeId) : null;
                NodeRef compoundWorkflowRef = storeRef != null && StringUtils.isNotBlank(compoundWorkflowId) ? new NodeRef(storeRef, compoundWorkflowId) : null;
                DueDateHistoryRecord record = new DueDateHistoryRecord(taskId, changeReason, previousDate, extensionTaskId, compoundWorkflowRef);

                List<DueDateHistoryRecord> taskRecord = result.get(taskId);
                if (taskRecord == null) {
                    taskRecord = new ArrayList<>();
                    result.put(taskId, taskRecord);
                }
                taskRecord.add(record);
                return null;
            }
        }, taskIds.toArray());

        return result;
    }

    @Override
    public void createTaskFileEntries(NodeRef taskRef, final List<File> files) {
        Assert.notNull(taskRef);
        if (isEmptyInsert(files)) {
            return;
        }
        final List<NodeRef> fileNodeRefs = new ArrayList<NodeRef>();
        for (File file : files) {
            Assert.isTrue(file != null && file.getNodeRef() != null);
            fileNodeRefs.add(file.getNodeRef());
        }
        createTaskFileEntriesFromNodeRefs(taskRef, fileNodeRefs);
    }

    @Override
    public void createTaskFileEntriesFromNodeRefs(NodeRef taskRef, final List<NodeRef> fileNodeRefs) {
        Assert.notNull(taskRef);
        if (isEmptyInsert(fileNodeRefs)) {
            return;
        }
        Assert.isTrue(!fileNodeRefs.contains(null));
        final String taskRefId = taskRef.getId();
        jdbcTemplate.batchUpdate("INSERT INTO delta_task_file (task_id, file_id) VALUES (?, ?)",
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, taskRefId);
                        ps.setString(2, fileNodeRefs.get(i).getId());
                    }

                    @Override
                    public int getBatchSize() {
                        return fileNodeRefs.size();
                    }
                });
    }

    private boolean isEmptyInsert(@SuppressWarnings("rawtypes") final List files) {
        if (files == null || files.isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    public void removeTaskFiles(NodeRef nodeRef, List<NodeRef> removedFileRefs) {
        if (removedFileRefs == null || removedFileRefs.isEmpty()) {
            return;
        }
        List<String> arguments = new ArrayList<String>();
        arguments.add(nodeRef.getId());
        for (NodeRef fileRef : removedFileRefs) {
            arguments.add(fileRef.getId());
        }
        int numFilesToRemove = removedFileRefs.size();
        String sqlQuery = "DELETE FROM delta_task_file WHERE task_id=? AND file_id IN (" + getQuestionMarks(numFilesToRemove) + ")";
        Object[] argumentsArray = arguments.toArray();
        int rowsDeleted = jdbcTemplate.update(sqlQuery, argumentsArray);
        explainQuery(sqlQuery, argumentsArray);
        if (rowsDeleted != numFilesToRemove) {
            throw new RuntimeException("Delete failed: requeired to delete " + numFilesToRemove + ", actually deleted " + rowsDeleted + " rows for taskRef=" + nodeRef
                    + ", fileNodeRefs=" + removedFileRefs);
        }
    }

    @Override
    public List<NodeRef> getTaskFileNodeRefs(NodeRef taskNodeRef) {
        String sqlQuery = "SELECT delta_task.store_id as store_id, delta_task_file.file_id as file_id FROM delta_task, delta_task_file " +
                "     WHERE delta_task.task_id=delta_task_file.task_id " +
                "      AND delta_task_file.task_id=? ORDER BY delta_task_file.task_file_id";
        String taskRefId = taskNodeRef.getId();
        List<NodeRef> fileRefs = jdbcTemplate
                .query(sqlQuery,
                        new ParameterizedRowMapper<NodeRef>() {

                            @Override
                            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                                return nodeRefFromRs(rs, "file_id");
                            }

                        }, taskRefId);
        explainQuery(sqlQuery, taskRefId);
        // null value shouldn't actually be in the list, but just in case...
        fileRefs.remove(null);
        return fileRefs;
    }

    @Override
    public Map<NodeRef, List<NodeRef>> getTaskFileNodeRefs(List<NodeRef> taskNodeRefs) {
        if (taskNodeRefs == null || taskNodeRefs.isEmpty()) {
            return new HashMap<>();
        }
        String sqlQuery = "SELECT delta_task.store_id as store_id, delta_task.task_id as task_id, delta_task_file.file_id as file_id FROM delta_task " +
                "LEFT JOIN delta_task_file ON delta_task.task_id = delta_task_file.task_id " +
                "WHERE delta_task.task_id IN (" + getQuestionMarks(taskNodeRefs.size()) + ") ORDER BY delta_task.index_in_workflow";

        Object[] taskRefArray = CollectionUtils.collect(taskNodeRefs, new Transformer<NodeRef, String>() {

            @Override
            public String tr(NodeRef input) {
                return input.getId();
            }
        }).toArray();
        final HashMap<NodeRef, List<NodeRef>> taskRefsWithFileRefs = new HashMap<>();
        jdbcTemplate.query(sqlQuery,
                new RowMapper<Void>() {

                    @Override
                    public Void mapRow(ResultSet rs, int rowNum) throws SQLException {

                        final NodeRef task_id = nodeRefFromRs(rs, "task_id");
                        final NodeRef file_id = nodeRefFromRs(rs, "file_id");
                        List<NodeRef> files = taskRefsWithFileRefs.get(task_id);
                        if (files == null) {
                            files = new ArrayList<>();
                        }
                        if (file_id != null) {
                            files.add(file_id);
                        }
                        taskRefsWithFileRefs.put(task_id, files);

                        return null;
                    }

                }, taskRefArray);
        explainQuery(sqlQuery, taskRefArray);
        return taskRefsWithFileRefs;
    }

    @Override
    public Map<NodeRef, List<NodeRef>> getCompoundWorkflowsTaskFiles(List<NodeRef> compoundWorkflows) {
        final Map<NodeRef, List<NodeRef>> result = new HashMap<>();
        if (compoundWorkflows == null || compoundWorkflows.isEmpty()) {
            return result;
        }

        String sqlQuery = "SELECT delta_task.task_id as task_id, delta_task_file.file_id as file_id FROM delta_task, delta_task_file " +
                "     WHERE delta_task.task_id=delta_task_file.task_id " +
                "      AND delta_task.wfs_compound_workflow_id IN (" + getQuestionMarks(compoundWorkflows.size()) + ") ORDER BY delta_task_file.task_file_id";

        final StoreRef storeRefFinal = compoundWorkflows.get(0).getStoreRef();
        final Object[] nodeRefIdQueryArguments = DbSearchUtil.appendNodeRefIdQueryArguments(compoundWorkflows).toArray();
        jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<NodeRef>() {

            @Override
            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                NodeRef taskRef = new NodeRef(storeRefFinal, rs.getString(TASK_ID_FIELD));
                NodeRef fileRef = new NodeRef(storeRefFinal, rs.getString("file_id"));
                if (!result.containsKey(taskRef)) {
                    result.put(taskRef, new ArrayList<NodeRef>());
                }
                result.get(taskRef).add(fileRef);
                return null;
            }

        }, nodeRefIdQueryArguments);
        explainQuery(sqlQuery, nodeRefIdQueryArguments);
        return result;
    }

    @Override
    public List<NodeRef> getCompoundWorkflowsFinishedTasks(List<NodeRef> compoundWorkflows, QName taskType) {
        return getCompoundWorkflowsFinishedTasks(compoundWorkflows, taskType, null, false);
    }

    @Override
    public List<NodeRef> getCompoundWorkflowsFinishedTasks(List<NodeRef> compoundWorkflows, QName taskType, QName sortByProperty, boolean descending) {
        final List<NodeRef> result = new ArrayList<>();
        if (compoundWorkflows == null || compoundWorkflows.isEmpty()) {
            return result;
        }

        String sqlQuery = "SELECT delta_task.task_id as task_id FROM delta_task " +
                "     WHERE delta_task.wfc_status=? " +
                "     AND delta_task.task_type=?" +
                "     AND delta_task.wfs_compound_workflow_id IN (" + getQuestionMarks(compoundWorkflows.size()) + ") ORDER BY ";
        if (sortByProperty != null) {
            sqlQuery += (getDbFieldName(sortByProperty) + (descending ? " DESC" : " ASC"));
        }
        if (!WorkflowCommonModel.Props.COMPLETED_DATE_TIME.equals(sortByProperty)) {
            if (sortByProperty != null) {
                sqlQuery += ", ";
            }
            sqlQuery += "delta_task.wfc_completed_date_time";
        }

        final StoreRef storeRefFinal = compoundWorkflows.get(0).getStoreRef();
        Object[] args = DbSearchUtil.appendNodeRefIdQueryArguments(compoundWorkflows, Status.FINISHED.getName(), taskType.getLocalName()).toArray();
        jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<NodeRef>() {

            @Override
            public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                result.add(new NodeRef(storeRefFinal, rs.getString(TASK_ID_FIELD)));
                return null;
            }

        }, args);
        explainQuery(sqlQuery, args);
        return result;
    }

    @Override
    public List<Task> loadCompoundWorkflowTasks(List<NodeRef> compoundWorkflowRefs, Set<QName> taskTypes, Set<Status> taskStatuses) {
        if (compoundWorkflowRefs == null || compoundWorkflowRefs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> statusNames = new HashSet<>();
        for (Status s : taskStatuses) {
            statusNames.add(s.getName());
        }
        String sqlQuery = getTaskQuery(compoundWorkflowRefs, taskTypes, statusNames, false);
        Object[] queryArgs = DbSearchUtil.appendNodeRefIdQueryArguments(compoundWorkflowRefs, statusNames.toArray()).toArray();
        List<Task> tasks = executeTaskQuery(taskTypes, sqlQuery, queryArgs);
        return tasks;
    }

    @Override
    public List<Task> getInProgressTasks(List<NodeRef> compoundWorkflows, String ownerId) {
        return getInProgressTasks(compoundWorkflows, ownerId, null);
    }

    @Override
    public List<Task> getInProgressTasks(List<NodeRef> compoundWorkflows, String ownerId, Set<QName> taskTypes) {
        Assert.hasText(ownerId, "Must specify an owner to check!");
        if (compoundWorkflows == null || compoundWorkflows.isEmpty()) {
            return Collections.emptyList();
        }

        String sqlQuery = getTaskQuery(compoundWorkflows, taskTypes, Collections.singleton(Status.IN_PROGRESS.getName()), true);

        Object[] queryArgs = DbSearchUtil.appendNodeRefIdQueryArguments(compoundWorkflows, Status.IN_PROGRESS.getName(), ownerId).toArray();
        List<Task> tasks = executeTaskQuery(taskTypes, sqlQuery, queryArgs);
        return tasks;
    }

    private List<Task> executeTaskQuery(Set<QName> taskTypes, String sqlQuery, Object[] queryArgs) {
        if (taskTypes != null) {
            Set<Object> types = new HashSet<>(taskTypes.size());
            for (QName type : taskTypes) {
                types.add(type.getLocalName());
            }
            queryArgs = ArrayUtils.addAll(queryArgs, types.toArray());
        }
        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<Task> tasks = jdbcTemplate.query(sqlQuery, new TaskRowMapper(null, null, null, workflowConstantsBean.getTaskPrefixedQNames(), null, null, false, false), queryArgs);
        explainQuery(sqlQuery, queryArgs);
        return tasks;
    }

    private String getTaskQuery(List<NodeRef> compoundWorkflows, Set<QName> taskTypes, Set<String> statusNames, boolean filterByOwner) {
        String typeQuery = "";
        if (taskTypes != null && !taskTypes.isEmpty()) {
            typeQuery = " AND delta_task.task_type IN (" + getQuestionMarks(taskTypes.size()) + ") ";
        }
        String sqlQuery = "SELECT delta_task.*, initiating_task.wfs_compound_workflow_id as initiating_compound_workflow_id, " +
                " initiating_task.wfs_compound_workflow_title as initiating_compound_workflow_title, " +
                " initiating_task.store_id as initiating_compound_workflow_store_id " +
                " FROM delta_task " +
                " left join delta_task_due_date_extension_assoc ext_assoc on ext_assoc.extension_task_id = delta_task.task_id " +
                " left join delta_task initiating_task on ext_assoc.task_id = initiating_task.task_id "
                + "WHERE delta_task.wfc_status IN (" + getQuestionMarks(statusNames.size()) + ")"
                + (filterByOwner ? "AND delta_task.wfc_owner_id=? " : "")
                + "AND delta_task.wfs_compound_workflow_id IN (" + getQuestionMarks(compoundWorkflows.size())
                + ") "
                + typeQuery
                + " ORDER BY index_in_workflow";
        return sqlQuery;
    }

    @Override
    public boolean hasNoInProgressOrOnlyActiveResponsibleAssignmentTasks(List<NodeRef> compoundWorkflows) {
        Assert.notNull(compoundWorkflows);

        if (compoundWorkflows.isEmpty()) {
            return true;
        }

        return !isOwnerOfInProgressTask(compoundWorkflows, null, false) || isOwnerOfInProgressTask(compoundWorkflows, WorkflowSpecificModel.Types.ASSIGNMENT_TASK, true);
    }

    @Override
    public Map<NodeRef, Task> loadTasksWithFiles(List<NodeRef> taskNodeRefs, Set<QName> propsToLoad) {
        WorkflowService workflowService = BeanHelper.getWorkflowService();
        Map<NodeRef, Task> result = getTasks(taskNodeRefs, null, false, propsToLoad);
        Map<NodeRef, List<NodeRef>> taskFileNodeRefs = getTaskFileNodeRefs(taskNodeRefs);

        for (Map.Entry<NodeRef, Task> entry : result.entrySet()) {
            Task task = entry.getValue();
            List<NodeRef> fileNodeRefs = taskFileNodeRefs.get(entry.getKey());
            if (fileNodeRefs != null && !fileNodeRefs.isEmpty()) {
                workflowService.loadTaskFiles(task, fileNodeRefs);
            }
            result.put(entry.getKey(), task);
        }

        return result;
    }

    @Override
    public boolean hasInProgressTasks(List<NodeRef> compoundWorkflows, String taskOwner) {
        Assert.notEmpty(compoundWorkflows, "Must specify at least one compound workflow!");
        Assert.hasText(taskOwner, "Must specify an owner to check!");

        boolean hasInProgressTasks = false;

        // if (task.isStatus(Status.IN_PROGRESS) && StringUtils.equals(task.getOwnerId(), runAsUser)) {
        String sqlQuery = "SELECT COUNT(1) FROM delta_task " +
                "     WHERE delta_task.wfc_status=? " +
                "     AND delta_task.wfc_owner_id=?" +
                "     AND delta_task.wfs_compound_workflow_id IN (" + getQuestionMarks(compoundWorkflows.size()) + ")";

        Object[] args = DbSearchUtil.appendNodeRefIdQueryArguments(compoundWorkflows, Status.IN_PROGRESS.getName(), taskOwner).toArray();
        int rowCount = jdbcTemplate.queryForInt(sqlQuery, args);
        hasInProgressTasks = rowCount > 0;
        explainQuery(sqlQuery, args);

        return hasInProgressTasks;
    }

    @Override
    public boolean isOwnerOfInProgressTask(List<NodeRef> compoundWorkflowNodeRef, QName taskType, boolean requireActiveResponsible) {
        Assert.notNull(compoundWorkflowNodeRef);

        if (compoundWorkflowNodeRef.isEmpty()) {
            return false;
        }

        String sqlQuery = "SELECT COUNT(1) FROM delta_task WHERE delta_task.wfc_owner_id=? AND delta_task.wfc_status=?";
        String[] baseArgs;
        if (taskType != null) {
            sqlQuery += " AND delta_task.task_type=?";
            baseArgs = new String[] { AuthenticationUtil.getRunAsUser(), Status.IN_PROGRESS.getName(), taskType.getLocalName() };
        } else {
            baseArgs = new String[] { AuthenticationUtil.getRunAsUser(), Status.IN_PROGRESS.getName() };
        }
        if (requireActiveResponsible) {
            sqlQuery += " AND delta_task.wfs_active";
        }
        sqlQuery += " AND delta_task.wfs_compound_workflow_id IN (" + getQuestionMarks(compoundWorkflowNodeRef.size()) + ")";
        sqlQuery += " LIMIT 1";

        Object[] args = DbSearchUtil.appendNodeRefIdQueryArguments(compoundWorkflowNodeRef, baseArgs).toArray();
        int rowCount = jdbcTemplate.queryForInt(sqlQuery, args);
        boolean hasInProgressTasks = rowCount > 0;
        explainQuery(sqlQuery, args);

        return hasInProgressTasks;
    }

    @Override
    public Map<NodeRef, String> getInProgressTaskOwners(Collection<NodeRef> compoundWorkflows) {
        if (compoundWorkflows == null || compoundWorkflows.isEmpty()) {
            return new HashMap<NodeRef, String>();
        }
        String sqlQuery = "SELECT wfs_compound_workflow_id, task_type, string_agg(wfc_owner_name, ', ') AS owner_names FROM delta_task "
                + "     WHERE delta_task.wfc_status = '" + Status.IN_PROGRESS.getName() + "'"
                + "     AND delta_task.wfs_compound_workflow_id IN (" + getQuestionMarks(compoundWorkflows.size()) + ")"
                + " GROUP BY wfs_compound_workflow_id, workflow_id, task_type"
                + " ORDER BY wfs_compound_workflow_id, workflow_id";

        Object[] args = DbSearchUtil.appendNodeRefIdQueryArguments(compoundWorkflows).toArray();
        final Map<String, List<Pair<String, String>>> owners = new HashMap<>();
        Map<String, String> workflowTypes = new HashMap<>();
        jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<Void>() {

            @Override
            public Void mapRow(ResultSet rs, int rowNum) throws SQLException {
                String compoundWorkflowId = rs.getString("wfs_compound_workflow_id");
                List<Pair<String, String>> compoundWorkflowOwners = owners.get(compoundWorkflowId);
                if (compoundWorkflowOwners == null) {
                    compoundWorkflowOwners = new ArrayList<>();
                    owners.put(compoundWorkflowId, compoundWorkflowOwners);
                }
                compoundWorkflowOwners.add(Pair.newInstance(rs.getString("task_type"), rs.getString("owner_names")));
                return null;
            }
        }, args);
        Map<NodeRef, String> taskOwners = new HashMap<NodeRef, String>();
        for (NodeRef nodeRef : compoundWorkflows) {
            List<Pair<String, String>> typesAndOwners = owners.get(nodeRef.getId());

            if (typesAndOwners == null) {
                continue; // no in progress tasks
            }

            StringBuilder workflowsAndTaskOwners = new StringBuilder();
            for (Pair<String, String> typeAndOwners : typesAndOwners) {
                if (workflowsAndTaskOwners.length() > 0) {
                    workflowsAndTaskOwners.append("; ");
                }
                workflowsAndTaskOwners.append(workflowConstantsBean.getWorkflowTypeNameByTask(typeAndOwners.getFirst()))
                        .append(" (" + typeAndOwners.getSecond() + ")");
            }
            taskOwners.put(nodeRef, workflowsAndTaskOwners.toString());
        }
        return taskOwners;
    }

    @Override
    public List<List<String>> deleteNotExistingTasks() {
        String sqlQuery = "SELECT * from delta_task where store_id is null";
        final List<String> columnNames = new ArrayList<String>();
        final List<String> taskIds = new ArrayList<String>();
        List<List<String>> taskData = jdbcTemplate.query(sqlQuery,
                new ParameterizedRowMapper<List<String>>() {

                    @Override
                    public List<String> mapRow(ResultSet rs, int rowNum) throws SQLException {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        if (rowNum == 0) {
                            for (int i = 1; i <= columnCount; i++) {
                                columnNames.add(metaData.getColumnName(i));
                            }
                        }
                        List<String> columnValues = new ArrayList<String>();
                        for (int i = 1; i <= columnCount; i++) {
                            Object object = rs.getObject(i);
                            columnValues.add(object != null ? object.toString() : null);
                        }
                        taskIds.add(rs.getString(TASK_ID_FIELD));
                        return columnValues;
                    }

                });
        explainQuery(sqlQuery);
        taskData.add(0, columnNames);

        int numTasks = taskIds.size();
        if (numTasks > 0) {
            String createTableAsQuery = "CREATE TABLE delta_task_no_store_id_tmp AS (" + sqlQuery + ")";
            jdbcTemplate.update(createTableAsQuery);

            String deleteQuery = "DELETE FROM delta_task WHERE store_id is null";
            jdbcTemplate.update(deleteQuery);
            explainQuery(deleteQuery);
        }

        return taskData;
    }

    @Override
    public int replaceTaskOutcomes(String oldOutcome, String newOutcome, String taskType) {
        boolean hasType = StringUtils.isNotBlank(taskType);
        String query = "UPDATE delta_task SET wfc_outcome=? WHERE wfc_outcome=?" + (hasType ? " AND task_type=?" : "");
        int tasksUpdated = 0;
        if (hasType) {
            jdbcTemplate.update(query, newOutcome, oldOutcome, taskType);
            explainQuery(query, newOutcome, oldOutcome, taskType);
        } else {
            jdbcTemplate.update(query, newOutcome, oldOutcome);
            explainQuery(query, newOutcome, oldOutcome);
        }
        return tasksUpdated;
    }

    @Override
    public Set<NodeRef> getAllWorflowNodeRefs() {
        String sqlQuery = "SELECT workflow_id, store_id FROM delta_task WHERE workflow_id IS NOT NULL AND store_id IS NOT NULL GROUP BY workflow_id, store_id";
        List<NodeRef> workflows = queryWorkflowNodeRefs(sqlQuery);
        explainQuery(sqlQuery);
        return new HashSet<NodeRef>(workflows);
    }

    private Serializable getConvertedValue(ResultSet rs, QName propName, String columnLabel, Object value) throws SQLException {
        if (WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.equals(propName) && value != null) {
            Array array = rs.getArray(columnLabel);
            // TODO: is there a better method to do this?
            List<String> valueList = new ArrayList<String>();
            ResultSet resultSet = array.getResultSet();
            while (resultSet.next()) {
                valueList.add(resultSet.getString(2));
            }
            return (Serializable) valueList;
        }
        return (Serializable) value;
    }

    private class TaskParentNodeRefMapper implements ParameterizedRowMapper<NodeRef> {

        @Override
        public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
            return nodeRefFromRs(rs, WORKFLOW_ID_KEY);
        }

    }

    private class TaskNodeRefRowMapper implements ParameterizedRowMapper<NodeRef> {

        @Override
        public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
            return nodeRefFromRs(rs, TASK_ID_FIELD);
        }
    }

    private class TaskNodeRefAndTypeRowMapper implements ParameterizedRowMapper<Pair<NodeRef, QName>> {

        @Override
        public Pair<NodeRef, QName> mapRow(ResultSet rs, int rowNum) throws SQLException {
            NodeRef nodeRef = nodeRefFromRs(rs, TASK_ID_FIELD);
            QName type = getTaskTypeFromRs(rs);
            return new Pair(nodeRef, type);
        }
    }

    private class TaskRowMapper<T> implements ParameterizedRowMapper<Task> {
        private final Collection<QName> taskDataTypeDefaultAspects;
        private final List<QName> taskDataTypeDefaultProps;
        private final List<QName> taskDataTypeSearchableProps;
        private Map<String, QName> rsColumnQNames;
        private final Map<QName, QName> taskPrefixedQNames;
        private final NodeRef originalParentRef;
        private final WorkflowType workflowType;
        private final Workflow workflow;
        private final boolean copy;
        private final boolean restrictedProps;
        private final boolean loadCompoundWorkflowRef;

        public TaskRowMapper(NodeRef originalParentRef, Collection<QName> taskDataTypeDefaultAspects, List<QName> taskDataTypeDefaultProps, Map<QName, QName> taskPrefixedQNames,
                             WorkflowType workflowType, Workflow workflow, boolean copy, boolean restrictedProps) {
            this(originalParentRef, taskDataTypeDefaultAspects, taskDataTypeDefaultProps, taskPrefixedQNames, workflowType, workflow, copy, restrictedProps, false);
        }

        public TaskRowMapper(NodeRef originalParentRef, Collection<QName> taskDataTypeDefaultAspects, List<QName> taskDataTypeDefaultProps, Map<QName, QName> taskPrefixedQNames,
                             WorkflowType workflowType, Workflow workflow, boolean copy, boolean restrictedProps, boolean loadCompoundWorkflowRef) {
            Assert.isTrue(workflowType == null || workflowType.getTaskType() == null || (taskDataTypeDefaultAspects != null && taskDataTypeDefaultProps != null));
            this.taskDataTypeDefaultAspects = taskDataTypeDefaultAspects;
            this.taskDataTypeDefaultProps = taskDataTypeDefaultProps;
            taskDataTypeSearchableProps = workflowConstantsBean.getTaskDataTypeSearchableProps();
            this.taskPrefixedQNames = taskPrefixedQNames;
            this.originalParentRef = originalParentRef;
            this.workflowType = workflowType;
            this.workflow = workflow;
            this.copy = copy;
            this.restrictedProps = restrictedProps;
            this.loadCompoundWorkflowRef = loadCompoundWorkflowRef;
        }

        @Override
        public Task mapRow(ResultSet rs, int i) throws SQLException {
            checkTaskDataTypeDefinition(rs);
            loadRsColumnQNames(rs);
            WmNode taskNode = getNode(rs);
            WorkflowType workflowType = this.workflowType != null ? this.workflowType : workflowConstantsBean.getWorkflowTypesByTask().get(taskNode.getType());
            Task task = Task.create(workflowType.getTaskClass(), taskNode, workflow, workflowType.getTaskOutcomes());
            // use rs.getObject instead of rs.getInt to get null values also
            if (!restrictedProps) {
                task.setTaskIndexInWorkflow((Integer) rs.getObject(INDEX_IN_WORKFLOW_FIELD));
            }
            if (!copy) {
                task.setWorkflowNodeRefId(rs.getString(WORKFLOW_ID_KEY));
                String storeRefStr = rs.getString(STORE_ID_FIELD);
                task.setStoreRef(storeRefStr);
                if (loadCompoundWorkflowRef) {
                    task.setCompoundWorkflowNodeRef(new NodeRef(new StoreRef(storeRefStr), rs.getString(COMPOUND_WORKFLOW_ID_KEY)));
                }
            } else if (workflow != null && workflow.getNodeRef() != null) {
                NodeRef workflowRef = workflow.getNodeRef();
                task.setWorkflowNodeRefId(workflowRef.getId());
                task.setStoreRef(workflowRef.getStoreRef().toString());
            }

            return task;
        }

        // during querying result task data type was not known, so determine type-specific properties from result

        private void loadRsColumnQNames(ResultSet rs) throws SQLException {
            if (rsColumnQNames == null) {
                rsColumnQNames = new HashMap<String, QName>();
                ResultSetMetaData metaData = rs.getMetaData();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String columnLabel = metaData.getColumnName(i);
                    if (TRANSIENT_PROPS.containsKey(columnLabel)) {
                        rsColumnQNames.put(columnLabel, TRANSIENT_PROPS.get(columnLabel));
                        continue;
                    }
                    QName propName = getPropQNameFromDbFieldName(columnLabel);
                    if (propName != null) {
                        rsColumnQNames.put(columnLabel, propName);
                    }
                }
            }
        }

        private void checkTaskDataTypeDefinition(ResultSet rs) throws SQLException {
            if (workflowType == null) {
                // no assumptions is made about returned task type
                return;
            }
            QName typeQName = getTaskTypeFromRs(rs);
            QName workflowTaskType = workflowType.getTaskType();
            if (!workflowTaskType.equals(typeQName)) {
                throw new RuntimeException("Workflow task type '" + workflowTaskType + "' doesn't match task type '" + typeQName + "' in db!");
            }
        }

        // if task type and/or parent is given, get task with the given properties.
        // Otherwise determine type and parent from rs
        private WmNode getNode(ResultSet rs) throws SQLException {
            QName taskType = workflowType != null ? workflowType.getTaskType() : getTaskTypeFromRs(rs);
            Set<QName> currentTaskAspects = new HashSet<QName>(taskDataTypeDefaultAspects != null ? taskDataTypeDefaultAspects : workflowConstantsBean
                    .getTaskDataTypeDefaultAspects()
                    .get(taskType));
            Set<QName> currentTaskProps = new HashSet<QName>(taskDataTypeDefaultProps != null ? taskDataTypeDefaultProps : workflowConstantsBean.getTaskDataTypeDefaultProps()
                    .get(taskType));
            Map<QName, Serializable> taskProps = new HashMap<QName, Serializable>();
            if (!restrictedProps) {
                if (rs.getObject(DbSearchUtil.ACTIVE_FIELD) != null) {
                    currentTaskAspects.add(WorkflowSpecificModel.Aspects.RESPONSIBLE);
                    currentTaskProps.add(WorkflowSpecificModel.Props.ACTIVE);
                }

                if (Boolean.TRUE.equals(rs.getObject(IS_SEARCHABLE_FIELD))) {
                    currentTaskAspects.add(WorkflowSpecificModel.Aspects.SEARCHABLE);
                    currentTaskProps.addAll(taskDataTypeSearchableProps);
                }
            }

            for (Map.Entry<String, QName> entry : rsColumnQNames.entrySet()) {
                QName propName = entry.getValue();
                String columnLabel = entry.getKey();
                Object value = rs.getObject(columnLabel);
                if (INITIATING_COMPOUND_WORKFLOW_ID_KEY.equals(columnLabel) && rsColumnQNames.containsKey(INITIATING_COMPOUND_WORKFLOW_STORE_ID_KEY)) {
                    String storeId = (String) rs.getObject(INITIATING_COMPOUND_WORKFLOW_STORE_ID_KEY);
                    String compoundWorkflowId = (String) value;
                    if (StringUtils.isNotBlank(compoundWorkflowId) && StringUtils.isNotBlank(storeId)) {
                        taskProps.put(propName, new NodeRef(new StoreRef(storeId), compoundWorkflowId));
                    }
                } else if (currentTaskProps.contains(propName) || TRANSIENT_PROPS.containsKey(propName)) {
                    value = getConvertedValue(rs, propName, columnLabel, value);
                    taskProps.put(propName, (Serializable) value);
                }
            }
            StoreRef storeRef = originalParentRef != null ? originalParentRef.getStoreRef() : new StoreRef(rs.getString(STORE_ID_FIELD));
            return new WmNode(copy ? null : new NodeRef(storeRef, rs.getString(TASK_ID_FIELD)), taskPrefixedQNames.get(taskType), currentTaskAspects,
                    taskProps);
        }

    }

    private class WorkflowBlockItemMapper implements ParameterizedRowMapper<WorkflowBlockItem> {

        private final Map<NodeRef, Boolean> checkWorkflowRights;
        private final String workflowGroupTasksUrl;
        private NodeRef previousCompoundWorkflow;

        public WorkflowBlockItemMapper(Map<NodeRef, Boolean> checkWorkflowRights, String workflowGroupTasksUrl) {
            this.checkWorkflowRights = checkWorkflowRights;
            this.workflowGroupTasksUrl = workflowGroupTasksUrl;
        }

        @Override
        public WorkflowBlockItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            final NodeRef workflowNodeRef = nodeRefFromRs(rs, WORKFLOW_ID_KEY);
            final NodeRef compoundWorkflowNodeRef = nodeRefFromRs(rs, "wfs_compound_workflow_id");
            final NodeRef taskNodeRef = nodeRefFromRs(rs, TASK_ID_FIELD);

            final WorkflowBlockItem item = createItem(rs, workflowNodeRef, compoundWorkflowNodeRef, taskNodeRef);
            setItemProperties(rs, compoundWorkflowNodeRef, item);
            if (item.isGroupBlockItem()) {
                setNumberOfTasksInGroup(rs, item);
                createTaskGroupUrl(item, workflowNodeRef);
            }
            rememberPreviousCompoundWorkflow(compoundWorkflowNodeRef);

            return item;
        }

        private WorkflowBlockItem createItem(ResultSet rs, NodeRef workflowNodeRef, NodeRef compoundWorkflowNodeRef, NodeRef taskNodeRef) throws SQLException {
            boolean raisedRights = checkWorkflowRights != null && checkWorkflowRights.containsKey(workflowNodeRef) && checkWorkflowRights.get(workflowNodeRef);
            return new WorkflowBlockItem(compoundWorkflowNodeRef, workflowNodeRef, taskNodeRef, rs.getString("wfc_owner_name"), rs.getBoolean("is_group"), raisedRights);
        }

        private void setItemProperties(ResultSet rs, NodeRef compoundWorkflowNodeRef, WorkflowBlockItem item) throws SQLException {
            item.setStartedDateTime(rs.getTimestamp("wfc_started_date_time"));
            item.setCompletedDateTime(rs.getTimestamp("wfc_completed_date_time"));
            item.setDueDate(rs.getTimestamp("wfs_due_date"));
            item.setCreatorName(rs.getString("wfc_creator_name"));
            item.setTaskType(rs.getString("task_type"));
            item.setResponsible(rs.getBoolean("wfs_active"));
            item.setProposedDueDate(rs.getTimestamp("wfs_proposed_due_date"));
            item.setTaskResolution(rs.getString("wfs_resolution"));
            item.setWorkflowResolution(rs.getString("wfs_workflow_resolution"));
            item.setTaskOutcome(rs.getString("wfc_outcome"));
            item.setTaskComment(rs.getString("wfs_comment"));
            item.setOwnerSubstituteName(rs.getString("wfc_owner_substitute_name"));
            item.setTaskOwnerGroup(rs.getString("wfc_owner_group"));
            item.setTaskStatus(rs.getString("wfc_status"));
            item.setIndexInWorkflow(rs.getInt("index_in_workflow"));
            item.setSeparator(previousCompoundWorkflow != null && !compoundWorkflowNodeRef.equals(previousCompoundWorkflow));
            item.setRowNumber(rs.getInt("num"));
        }

        private void createTaskGroupUrl(WorkflowBlockItem item, NodeRef workflowNodeRef) {
            if (workflowGroupTasksUrl == null) {
                return;
            }
            StringBuffer url = new StringBuffer(workflowGroupTasksUrl).append(workflowNodeRef.getId())
                    .append("&amp;").append(PrintTableServlet.TASK_INDEX).append("=").append(item.getIndexInWorkflow());

            final Integer numberOfTasksInGroup = item.getNumberOfTasksInGroup();
            if (numberOfTasksInGroup != null) {
                url.append("&amp;").append(PrintTableServlet.TASK_LIMIT).append("=").append(numberOfTasksInGroup);
            }

            item.setWorkflowGroupTasksUrl(url.toString());
        }

        private void rememberPreviousCompoundWorkflow(NodeRef compoundWorkflowNodeRef) {
            previousCompoundWorkflow = compoundWorkflowNodeRef;
        }

        private void setNumberOfTasksInGroup(ResultSet rs, WorkflowBlockItem item) throws SQLException {
            int nextNum = rs.getInt("next_num");
            Integer groupTaskQueryLimit = null;
            if (nextNum != 0) {
                final int num = rs.getInt("num");
                groupTaskQueryLimit = nextNum - num;
            }
            item.setNumberOfTasksInGroup(groupTaskQueryLimit);
        }
    }

    public static QName getTaskTypeFromRs(ResultSet rs) throws SQLException {
        String typeStr = rs.getString(TASK_TYPE_FIELD);
        return ALL_TASK_TYPES.get(typeStr);
    }

    @Override
    public void deleteWorkflowTasks(NodeRef removedWorkflowNodeRef) {
        String sqlQuery = "DELETE FROM delta_task WHERE workflow_id=?";
        String workflowRefId = removedWorkflowNodeRef.getId();
        jdbcTemplate.update(sqlQuery, workflowRefId);
        explainQuery(sqlQuery, workflowRefId);
    }

    @Override
    public void deleteTask(NodeRef removedTaskNodeRef) {
        String sqlQuery = "DELETE FROM delta_task WHERE task_id=?";
        String taskRefId = removedTaskNodeRef.getId();
        jdbcTemplate.update(sqlQuery, taskRefId);
        explainQuery(sqlQuery, taskRefId);
    }

    @Override
    public void deleteTasks(List<NodeRef> removedTaskNodeRefs) {
        if (removedTaskNodeRefs == null || removedTaskNodeRefs.isEmpty()) {
            return;
        }

        String sqlQuery = "DELETE FROM delta_task WHERE task_id IN (" + DbSearchUtil.getQuestionMarks(removedTaskNodeRefs.size()) + ")";
        Object[] nodeRefIds = RepoUtil.getNodeRefIds(removedTaskNodeRefs).toArray();
        jdbcTemplate.update(sqlQuery, nodeRefIds);
        explainQuery(sqlQuery, nodeRefIds);
    }

    @Override
    public void deleteTasksCascading(NodeRef nodeRef, QName nodeTypeQName) {
        if (dictionaryService.isSubClass(nodeTypeQName, WorkflowCommonModel.Types.TASK)) {
            deleteTask(nodeRef);
        } else if (isWorkflow(nodeTypeQName)) {
            deleteWorkflowTasks(nodeRef);
        } else if (WorkflowCommonModel.Types.COMPOUND_WORKFLOW.equals(nodeTypeQName) || WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION.equals(nodeTypeQName)) {
            deleteCompoundWorkflowTasks(nodeRef);
        } else if (DocumentCommonModel.Types.DOCUMENT.equals(nodeTypeQName)) {
            deleteDocumentTasks(nodeRef);
        } else if (CaseModel.Types.CASE.equals(nodeTypeQName) || VolumeModel.Types.VOLUME.equals(nodeTypeQName) || CaseFileModel.Types.CASE_FILE.equals(nodeTypeQName)) {
            deleteCaseOrVolumeTasks(nodeRef);
        } else if (SeriesModel.Types.SERIES.equals(nodeTypeQName)) {
            deleteSeriesTasks(nodeRef);
        } else if (FunctionsModel.Types.FUNCTION.equals(nodeTypeQName)) {
            deleteFunctionTasks(nodeRef);
        }

    }

    private void deleteFunctionTasks(NodeRef nodeRef) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, Collections.singleton(SeriesModel.Types.SERIES));
        for (ChildAssociationRef childAssoc : childAssocs) {
            deleteSeriesTasks(childAssoc.getChildRef());
        }
    }

    private void deleteSeriesTasks(NodeRef nodeRef) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, new HashSet<QName>(Arrays.asList(VolumeModel.Types.VOLUME, CaseFileModel.Types.CASE_FILE)));
        for (ChildAssociationRef childAssoc : childAssocs) {
            deleteCaseOrVolumeTasks(childAssoc.getChildRef());
        }
    }

    private void deleteCaseOrVolumeTasks(NodeRef nodeRef) {
        // case file compound workflows
        deleteDocumentTasks(nodeRef);
        // case file documents' compound workflows
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, Collections.singleton(DocumentCommonModel.Types.DOCUMENT));
        for (ChildAssociationRef childAssoc : childAssocs) {
            deleteDocumentTasks(childAssoc.getChildRef());
        }
        List<ChildAssociationRef> caseChildAssocs = nodeService.getChildAssocs(nodeRef, Collections.singleton(CaseModel.Types.CASE));
        for (ChildAssociationRef childAssoc : caseChildAssocs) {
            deleteCaseOrVolumeTasks(childAssoc.getChildRef());
        }
    }

    private void deleteDocumentTasks(NodeRef nodeRef) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, Collections.singleton(WorkflowCommonModel.Types.COMPOUND_WORKFLOW));
        for (ChildAssociationRef childAssoc : childAssocs) {
            deleteCompoundWorkflowTasks(childAssoc.getChildRef());
        }
    }

    private void deleteCompoundWorkflowTasks(NodeRef nodeRef) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef childRef = childAssoc.getChildRef();
            if (isWorkflow(nodeService.getType(childRef))) {
                deleteWorkflowTasks(childRef);
            }
        }
    }

    @Override
    public Set<NodeRef> getAllWorkflowsWithEmptyTasks() {
        String sqlQuery = "SELECT task_id, workflow_id, store_id FROM delta_task " +
                "WHERE workflow_id IS NOT NULL AND store_id IS NOT NULL AND wfc_owner_name IS NULL AND wfs_due_date IS NULL";
        List<NodeRef> workflows = queryWorkflowNodeRefs(sqlQuery);
        explainQuery(sqlQuery);
        return new HashSet<NodeRef>(workflows);
    }

    @Override
    public Set<NodeRef> getWorkflowsWithWrongTaskOrder() {
        String sqlQuery = "select workflow_id, store_id from (" +
                " select workflow_id, sum(max_index - min_index + 1) as task_ranged_count, sum(task_count) as task_count, max(task_type)  as task_type, " +
                "max(store_id) as store_id from" +
                " (select workflow_id, grouped_status, max(index_in_workflow) as max_index, min(index_in_workflow) as min_index, max(task_type) as task_type, " +
                " max(store_id) as store_id, count(*) as task_count from " +
                " (select *, case when wfc_status in ('teostamata', 'lõpetatud') then 'lõpetatud_grupp' else wfc_status end as grouped_status from delta_task) as tmp0" +
                " where task_type NOT IN ('opinionTask', 'informationTask', 'assignmentTask', 'orderAssignmentTask') group by workflow_id, grouped_status" +
                " order by workflow_id, max_index) as tmp group by workflow_id) as tmp1" +
                " where task_ranged_count <> task_count";
        List<NodeRef> workflows = queryWorkflowNodeRefs(sqlQuery);
        explainQuery(sqlQuery);
        return new HashSet<NodeRef>(workflows);
    }

    @Override
    public List<Comment> getCompoundWorkflowComments(String compoundWorkflowId) {
        String sqlQuery = "SELECT * FROM delta_compound_workflow_comment where compound_workflow_id=? order by created DESC";
        List<Comment> comments = jdbcTemplate.query(sqlQuery, new ParameterizedRowMapper<Comment>() {

            @Override
            public Comment mapRow(ResultSet rs, int rowNum) throws SQLException {
                String compWorkflowId = (String) rs.getObject("compound_workflow_id");
                Date created = (Date) rs.getObject("created");
                String creatorId = (String) rs.getObject("creator_id");
                String creatorName = (String) rs.getObject("creator_name");
                String commentText = (String) rs.getObject("comment_text");
                Comment comment = new Comment(compWorkflowId, created, creatorId, creatorName, commentText);
                comment.setCommentId((Long) rs.getObject("comment_id"));
                return comment;
            }
        }, compoundWorkflowId);
        explainQuery(sqlQuery, compoundWorkflowId);
        return comments;
    }

    @Override
    public void addCompoundWorkfowComment(Comment comment) {
        int rowsInserted = jdbcTemplate.update(
                "INSERT INTO delta_compound_workflow_comment (compound_workflow_id, created, creator_id, creator_name, comment_text) VALUES (?, ?, ?, ?, ?)",
                new Object[] { comment.getCompoundWorkflowId(), comment.getCreated(), comment.getCreatorId(), comment.getCreatorName(), comment.getCommentText() });
        if (rowsInserted != 1) {
            throw new RuntimeException("Insert failed: inserted " + rowsInserted + " rows for compoundWorkflowId=" + comment.getCompoundWorkflowId());
        }
    }

    @Override
    public void editCompoundWorkflowComment(Long commentId, String commentText) {
        String sqlQuery = "UPDATE delta_compound_workflow_comment SET comment_text=? WHERE comment_id=?";
        Object[] args = new Object[] { commentText, commentId };
        jdbcTemplate.update(sqlQuery, args);
        explainQuery(sqlQuery, args);
    }

    private List<NodeRef> queryWorkflowNodeRefs(String sqlQuery, Object... args) {
        return jdbcTemplate.query(sqlQuery, new TaskParentNodeRefMapper(), args);
    }

    private boolean isWorkflow(QName type) {
        return dictionaryService.isSubClass(type, WorkflowCommonModel.Types.WORKFLOW);
    }

    private static QName getPropQNameFromDbFieldName(String fieldName) {
        QName propName = FIELD_NAME_TO_TASK_PROP.get(fieldName);
        if (propName == null) {
            String namespaceUri;
            String prefix;
            if (fieldName.startsWith(prefix = DbSearchUtil.getPrefix(WorkflowCommonModel.PREFIX) + DbSearchUtil.SEPARATOR)) {
                namespaceUri = WorkflowCommonModel.URI;
            } else if (fieldName.startsWith(prefix = DbSearchUtil.getPrefix(WorkflowSpecificModel.PREFIX) + DbSearchUtil.SEPARATOR)) {
                namespaceUri = WorkflowSpecificModel.URI;
            } else {
                return null;
            }
            StringTokenizer st = new StringTokenizer(fieldName.substring(prefix.length()), DbSearchUtil.SEPARATOR);
            StringBuilder sb = new StringBuilder();
            boolean firstToken = true;
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                char firstChar = firstToken ? token.charAt(0) : Character.toUpperCase(token.charAt(0));
                sb.append(firstChar + token.substring(1));
                firstToken = false;
            }
            propName = QName.createQName(namespaceUri, sb.toString());
            FIELD_NAME_TO_TASK_PROP.put(fieldName, propName);
            String previous = TASK_PROP_TO_FIELD_NAME.put(propName, fieldName);
            if (previous != null && previous.equals(fieldName)) {
                throw new RuntimeException("Field name and task prop conversions should produce uniform results.");
            }
        }
        return propName;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public WorkflowConstantsBean getWorkflowConstantsBean() {
        return workflowConstantsBean;
    }

    public void setWorkflowConstantsBean(WorkflowConstantsBean workflowConstantsBean) {
        this.workflowConstantsBean = workflowConstantsBean;
    }

}
