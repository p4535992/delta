package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.common.search.DbSearchUtil.TASK_TYPE_FIELD;
import static ee.webmedia.alfresco.common.search.DbSearchUtil.getDbFieldNameFromPropQName;
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

import javax.sql.DataSource;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.search.DbSearchUtil;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.bootstrap.MoveTaskFileToChildAssoc;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * Main implementation of {@link WorkflowDbService}. This class does not rely on Alfresco, and exchanges data with the database using JDBC(Template) directly.
 * 
 * @author Riina Tens
 */
public class WorkflowDbServiceImpl implements WorkflowDbService {

    private static final String IS_SEARCHABLE_FIELD = "is_searchable";
    private static final String INDEX_IN_WORKFLOW_FIELD = "index_in_workflow";
    private static final String TASK_ID_FIELD = "task_id";
    private static final String DUE_DATE_HISTORY_FIELD = "has_due_date_history";
    private static final String WORKFLOW_ID_KEY = "workflow_id";
    private static final String STORE_ID_FIELD = "store_id";
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(WorkflowDbServiceImpl.class);
    private static final List<QName> NOT_SENT_TO_REPO_PROPS = Arrays.asList(QName.createQName(WorkflowCommonModel.URI, "creatorId"),
            QName.createQName(WorkflowCommonModel.URI, "creatorEmail"), MoveTaskFileToChildAssoc.TASK_FILE_PROP_QNAME);

    private static final Map<String, QName> TRANSIENT_PROPS = new HashMap<String, QName>();

    private SimpleJdbcTemplate jdbcTemplate;
    private DataSource dataSource;
    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private GeneralService generalService;

    static {
        TRANSIENT_PROPS.put("initiating_compound_workflow_id", INITIATING_COMPOUND_WORKFLOW_REF);
        TRANSIENT_PROPS.put("initiating_compound_workflow_title", INITIATING_COMPOUND_WORKFLOW_TITLE);
        TRANSIENT_PROPS.put("initiating_compound_workflow_store_id", null);
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
        Pair<List<String>, List<Object>> fieldNamesAndArguments = getFieldNamesAndArguments(task, null, changedProps);
        List<Object> arguments = fieldNamesAndArguments.getSecond();
        if (arguments.isEmpty()) {
            // no fields were changed, no update needed
            return;
        }
        arguments.add(task.getNodeRef().getId());
        updateTaskEntry(fieldNamesAndArguments.getFirst(), arguments, task.getNodeRef());
        task.setOriginalHasDueDateHistory(task.getHasDueDateHistory());
    }

    @Override
    public void createTaskEntry(Task task, NodeRef workflowfRef) {
        createTaskEntry(task, workflowfRef, false);
    }

    @Override
    public void createTaskEntry(Task task, NodeRef workflowfRef, boolean isIndependentTask) {
        if (task == null) {
            return;
        }
        verifyTask(task, workflowfRef);
        Integer taskIndexInWorkflow = task.getTaskIndexInWorkflow();
        Assert.isTrue(taskIndexInWorkflow == null || taskIndexInWorkflow >= 0);
        Pair<List<String>, List<Object>> fieldNamesAndArguments = getFieldNamesAndArguments(task, isIndependentTask ? null : workflowfRef,
                RepoUtil.toQNameProperties(task.getNode().getProperties()));

        List<String> fields = fieldNamesAndArguments.getFirst();
        fields.add(IS_SEARCHABLE_FIELD);
        List<Object> arguments = fieldNamesAndArguments.getSecond();
        arguments.add(task.getNode().getAspects().contains(WorkflowSpecificModel.Aspects.SEARCHABLE));
        addTaskEntry(fields, arguments);
    }

    @Override
    public void updateTaskProperties(NodeRef taskRef, Map<QName, Serializable> props) {
        if (taskRef == null || RepoUtil.isUnsaved(taskRef)) {
            throw new RuntimeException("Task is not saved, unable to update! taskRef=" + taskRef);
        }
        if (props == null || props.isEmpty()) {
            return;
        }
        List<String> fieldNames = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        getPropFieldNamesAndArguments(fieldNames, arguments, props);
        arguments.add(taskRef.getId());
        updateTaskEntry(fieldNames, arguments, taskRef);
    }

    @Override
    public int updateTaskPropertiesAndStorRef(NodeRef taskRef, Map<QName, Serializable> props) {
        List<String> fieldNames = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        fieldNames.add(STORE_ID_FIELD);
        arguments.add(taskRef.getStoreRef().toString());
        getPropFieldNamesAndArguments(fieldNames, arguments, props);
        arguments.add(taskRef.getId());
        return updateTaskEntry(fieldNames, arguments, taskRef);
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
        Map<QName, QName> taskPrefixedQNames = BeanHelper.getWorkflowService().getTaskPrefixedQNames();
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
        final String fieldName = getDbFieldNameFromPropQName(qname);
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

    private int updateTaskEntry(List<String> fieldNames, List<Object> arguments, NodeRef taskRef) {
        return updateTaskEntry(fieldNames, arguments, taskRef, true);
    }

    private int updateTaskEntry(List<String> fieldNames, List<Object> arguments, NodeRef taskRef, boolean throwIfNotSingleUpdate) {
        StringBuffer sb = new StringBuffer();
        boolean isNotFirst = false;
        for (String fieldName : fieldNames) {
            if (isNotFirst) {
                sb.append(", ");
            }
            isNotFirst = true;
            sb.append(fieldName + "=?");
        }
        String sqlQuery = "UPDATE delta_task SET " + sb.toString() + " WHERE task_id=?";
        Object[] argumentsArray = arguments.toArray();
        int rowsUpdated = jdbcTemplate.update(sqlQuery, argumentsArray);
        explainQuery(sqlQuery, argumentsArray);
        if (rowsUpdated != 1 && throwIfNotSingleUpdate) {
            throw new RuntimeException("Update failed: updated " + rowsUpdated + " rows for task nodeRef=" + taskRef + ", sql='" + sqlQuery + "', arguments=" + arguments);
        }
        return rowsUpdated;
    }

    private void addTaskEntry(List<String> fieldNames, List<Object> arguments) {
        String questionMarks = getArgumentQuestionMarks(fieldNames);
        jdbcTemplate.update(
                "INSERT INTO delta_task (" + TextUtil.joinNonBlankStringsWithComma(fieldNames) + ") VALUES (" + questionMarks + ")", arguments.toArray());

    }

    private String getArgumentQuestionMarks(List<String> fieldNames) {
        return getQuestionMarks(fieldNames.size());
    }

    private String getQuestionMarks(int size) {
        String questionMarks = StringUtils.repeat("?, ", size);
        questionMarks = questionMarks.substring(0, questionMarks.length() - 2);
        return questionMarks;
    }

    private void verifyTask(Task task, NodeRef workflowRef) {
        Assert.notNull(task);
        Workflow workflow = task.getParent();
        Assert.isTrue(workflow != null || workflowRef != null);
        NodeRef taskRef = task.getNodeRef();
        NodeRef wfRef = workflow != null ? workflow.getNodeRef() : workflowRef;
        Assert.isTrue(isSaved(taskRef) && isSaved(wfRef) && task.getType() != null);
    }

    @SuppressWarnings("unchecked")
    private Pair<List<String>, List<Object>> getFieldNamesAndArguments(Task task, NodeRef workflowfRef, Map<QName, Serializable> changedProps) {
        List<String> fieldNames = new ArrayList<String>();
        List<Object> arguments = new ArrayList<Object>();
        Integer taskIndexInWorkflow = task.getTaskIndexInWorkflow();
        boolean addTaskIndexInWorkflow = taskIndexInWorkflow != null && taskIndexInWorkflow >= 0;

        fieldNames.addAll(Arrays.asList(TASK_ID_FIELD, WORKFLOW_ID_KEY, TASK_TYPE_FIELD, "store_id"));
        String workflowId = task.getParent() != null ? task.getParent().getNodeRef().getId() : (workflowfRef != null ? workflowfRef.getId() : null);
        arguments.addAll(Arrays.asList(task.getNodeRef().getId(), workflowId, task.getType().getLocalName(), task.getNodeRef().getStoreRef().toString()));

        if (addTaskIndexInWorkflow) {
            fieldNames.add(INDEX_IN_WORKFLOW_FIELD);
            arguments.add(taskIndexInWorkflow);
        }
        if (!ObjectUtils.equals(task.getHasDueDateHistory(), task.getOriginalHasDueDateHistory())) {
            fieldNames.add(DUE_DATE_HISTORY_FIELD);
            arguments.add(task.getHasDueDateHistory());
        }
        getPropFieldNamesAndArguments(fieldNames, arguments, changedProps);
        return new Pair<List<String>, List<Object>>(fieldNames, arguments);
    }

    private void getPropFieldNamesAndArguments(List<String> fieldNames, List<Object> arguments, @SuppressWarnings("rawtypes") Map taskProps) {
        if (taskProps == null) {
            return;
        }
        for (Object entryObj : taskProps.entrySet()) {
            @SuppressWarnings("unchecked")
            Map.Entry<QName, Serializable> entry = (Map.Entry<QName, Serializable>) entryObj;
            @SuppressWarnings({ "cast" })
            QName propName = (QName) entry.getKey();
            if (!WorkflowCommonModel.URI.equals(propName.getNamespaceURI()) && !WorkflowSpecificModel.URI.equals(propName.getNamespaceURI())) {
                continue;
            }
            if (NOT_SENT_TO_REPO_PROPS.contains(propName)) {
                continue;
            }
            Object value = entry.getValue();
            if ((WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.equals(propName)
                    || WorkflowSpecificModel.Props.SEARCHABLE_COMPOUND_WORKFLOW_OWNER_ORGANIZATION_NAME.equals(propName)) && value != null) {
                Connection connection = null;
                try {
                    // TODO: get "text" constant value from connection? It is database-specific
                    connection = dataSource.getConnection();
                    value = connection.createArrayOf("text", (((List<String>) value).toArray()));
                } catch (SQLException e) {
                    LOG.error("Error creating owner organization name input", e);
                    throw new RuntimeException(e);
                } finally {
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (SQLException e) {
                            LOG.error("Error closing connection", e);
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            fieldNames.add(getDbFieldNameFromPropQName(propName));
            arguments.add(value);
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
    public Pair<List<Task>, Boolean> searchTasksMainStore(String queryCondition, List<Object> arguments, int limit) {
        return searchTasks(queryCondition, arguments, limit, BeanHelper.getGeneralService().getStore());
    }

    @Override
    public Pair<List<Task>, Boolean> searchTasksAllStores(String queryCondition, List<Object> arguments, int limit) {
        return searchTasks(queryCondition, arguments, limit, null);
    }

    private Pair<List<Task>, Boolean> searchTasks(String queryCondition, List<Object> arguments, int limit, StoreRef storeRef) {
        if (StringUtils.isBlank(queryCondition)) {
            return new Pair<List<Task>, Boolean>(new ArrayList<Task>(), Boolean.FALSE);
        }
        arguments.add(0, Boolean.TRUE);
        boolean hasStoreRef = storeRef != null;
        Set<StoreRef> storeRefs = new LinkedHashSet<StoreRef>();
        if (hasStoreRef) {
            storeRefs.add(storeRef);
        } else {
            storeRefs.addAll(BeanHelper.getGeneralService().getAllWithArchivalsStoreRefs());
        }
        for (StoreRef store : storeRefs) {
            arguments.add(1, store.toString());
        }
        boolean limited = limit > -1;
        if (limited) {
            arguments.add(limit + 1);
        }
        TaskRowMapper taskRowMapper = new TaskRowMapper(null, null, null, BeanHelper.getWorkflowService().getTaskPrefixedQNames(), null, null, false, limited);
        String sqlQuery = "SELECT delta_task.* "
                + (limited ? ", count(*) OVER() AS full_count " : "") + " FROM delta_task WHERE "
                + SearchUtil.joinQueryPartsAnd(" is_searchable=? ", "store_id IN (" + getQuestionMarks(storeRefs.size()) + ")", queryCondition)
                + (limited ? " LIMIT ? " : "");
        Object[] argumentsArray = arguments.toArray();
        List<Task> tasks = jdbcTemplate.query(sqlQuery, taskRowMapper, argumentsArray);
        explainQuery(sqlQuery, argumentsArray);
        return new Pair<List<Task>, Boolean>(tasks, !limited || taskRowMapper.getTaskCountBeforeLimit() > tasks.size());
    }

    @Override
    public List<NodeRef> searchTaskNodeRefs(String queryCondition, List<Object> arguments) {
        arguments.add(0, Boolean.TRUE);
        String sqlQuery = "SELECT task_id, store_id FROM delta_task WHERE " + SearchUtil.joinQueryPartsAnd(" is_searchable=? ", queryCondition);
        Object[] argumentsArray = arguments.toArray();
        List<NodeRef> taskRefs = jdbcTemplate.query(sqlQuery, new TaskNodeRefRowMapper(), argumentsArray);
        explainQuery(sqlQuery, argumentsArray);
        return taskRefs;
    }

    @Override
    public int countTasks(String queryCondition, List<Object> arguments) {
        arguments.add(0, Boolean.TRUE);
        Set<StoreRef> storeRefs = BeanHelper.getGeneralService().getAllWithArchivalsStoreRefs();
        for (StoreRef store : storeRefs) {
            arguments.add(1, store.toString());
        }
        String sqlQuery = "SELECT COUNT(1) FROM delta_task WHERE "
                + SearchUtil.joinQueryPartsAnd(" is_searchable=? ", "store_id IN (" + getQuestionMarks(storeRefs.size()) + ")", queryCondition);
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
        arguments.add(0, Boolean.TRUE);
        String sqlQuery = "SELECT task_id, store_id, wfs_sent_dvk_id, wfs_institution_code FROM delta_task WHERE "
                + SearchUtil.joinQueryPartsAnd(" is_searchable=? ", queryCondition);
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
    public Task getTask(NodeRef nodeRef, Map<QName, QName> taskPrefixedQNames, Workflow workflow, boolean copy) {
        String sqlQuery = "SELECT * FROM delta_task where task_id=?";
        String nodeRefId = nodeRef.getId();
        List<Task> tasks = jdbcTemplate.query(sqlQuery,
                new TaskRowMapper(null, null, null, taskPrefixedQNames, null, workflow, copy, false),
                nodeRefId);
        explainQuery(sqlQuery, nodeRefId);
        int taskCount = tasks.size();
        if (taskCount != 1) {
            throw new RuntimeException("Expected to find one task, found " + taskCount + " for nodeRef=" + nodeRef);
        }
        return tasks.get(0);
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
        jdbcTemplate.getJdbcOperations().batchUpdate("INSERT INTO delta_task_due_date_history (task_id, previous_date, change_reason, extension_task_id) VALUES (?, ?, ?, ?)",
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
    public Collection<DueDateHistoryRecord> getDueDateHistoryRecords(NodeRef taskRef) {
        String sqlQuery = "SELECT history.previous_date, history.change_reason, history.task_id, history.extension_task_id, task.wfs_compound_workflow_id, task.store_id " +
                " FROM delta_task_due_date_history history " +
                " join delta_task task on task.task_id = history.extension_task_id " +
                " WHERE history.task_id=? ORDER BY history.task_due_date_history_id";
        String taskRefId = taskRef.getId();
        List<DueDateHistoryRecord> dueDateHistoryRecords = jdbcTemplate.query(sqlQuery, new TaskDueDateHistoryMapper(), taskRefId);
        explainQuery(sqlQuery, taskRefId);
        // null value shouldn't actually be in the list, but just in case...
        dueDateHistoryRecords.remove(null);
        return dueDateHistoryRecords;
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
        jdbcTemplate.getJdbcOperations().batchUpdate("INSERT INTO delta_task_file (task_id, file_id) VALUES (?, ?)",
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
            LOG.info("No file input provided, skipping insert");
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
    public Map<NodeRef, List<NodeRef>> getCompoundWorkflowsTaskFiles(List<CompoundWorkflow> compoundWorkflows) {
        final Map<NodeRef, List<NodeRef>> result = new HashMap<NodeRef, List<NodeRef>>();
        List<String> workflowNodeRefs = new ArrayList<String>();
        if (compoundWorkflows == null) {
            return result;
        }
        StoreRef storeRef = null;
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (workflow.isSaved()) {
                    NodeRef nodeRef = workflow.getNodeRef();
                    workflowNodeRefs.add(nodeRef.getId());
                    storeRef = nodeRef.getStoreRef();
                }
            }
        }
        if (workflowNodeRefs.isEmpty()) {
            return result;
        }
        String sqlQuery = "SELECT delta_task.task_id as task_id, delta_task_file.file_id as file_id FROM delta_task, delta_task_file " +
                "     WHERE delta_task.task_id=delta_task_file.task_id " +
                "      AND delta_task.workflow_id IN (" + getQuestionMarks(workflowNodeRefs.size()) + ") ORDER BY delta_task_file.task_file_id";
        Object[] workflowRefArray = workflowNodeRefs.toArray();
        final StoreRef storeRefFinal = storeRef;
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

        }, workflowRefArray);
        explainQuery(sqlQuery, workflowRefArray);
        return result;
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
        List<NodeRef> workflows = jdbcTemplate.query(sqlQuery,
                new ParameterizedRowMapper<NodeRef>() {

                    @Override
                    public NodeRef mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return nodeRefFromRs(rs, WORKFLOW_ID_KEY);
                    }

                });
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
                valueList.add(resultSet.getString(1));
            }
            return (Serializable) valueList;
        }
        return (Serializable) value;
    }

    private class TaskDueDateHistoryMapper implements ParameterizedRowMapper<DueDateHistoryRecord> {

        @Override
        public DueDateHistoryRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            Date previousDate = (Date) rs.getObject("previous_date");
            String changeReason = (String) rs.getObject("change_reason");
            String compoundWorkflowId = (String) rs.getObject("wfs_compound_workflow_id");
            String taskId = (String) rs.getObject("task_id");
            String extensionTaskId = (String) rs.getObject("extension_task_id");
            String storeId = (String) rs.getObject("store_id");
            StoreRef storeRef = StringUtils.isNotBlank(storeId) ? new StoreRef(storeId) : null;
            NodeRef compoundWorkflowRef = storeRef != null && StringUtils.isNotBlank(compoundWorkflowId) ? new NodeRef(storeRef, compoundWorkflowId) : null;
            return new DueDateHistoryRecord(taskId, changeReason, previousDate, extensionTaskId, compoundWorkflowRef);
        }
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

    private class TaskRowMapper implements ParameterizedRowMapper<Task> {
        private static final String INITIATING_COMPOUND_WORKFLOW_ID_KEY = "initiating_compound_workflow_id";
        private static final String INITIATING_COMPOUND_WORKFLOW_STORE_ID_KEY = "initiating_compound_workflow_store_id";
        private final Collection<QName> taskDataTypeDefaultAspects;
        private final List<QName> taskDataTypeDefaultProps;
        private final List<QName> taskDataTypeSearchableProps;
        private Map<String, QName> rsColumnQNames;
        private final Map<QName, QName> taskPrefixedQNames;
        private final NodeRef originalParentRef;
        private final WorkflowType workflowType;
        private final Workflow workflow;
        private final boolean copy;
        private final boolean limited;
        private int taskCountBeforeLimit = -1;

        public TaskRowMapper(NodeRef originalParentRef, Collection<QName> taskDataTypeDefaultAspects, List<QName> taskDataTypeDefaultProps, Map<QName, QName> taskPrefixedQNames,
                             WorkflowType workflowType, Workflow workflow, boolean copy, boolean limited) {
            Assert.isTrue(workflowType == null || workflowType.getTaskType() == null || (taskDataTypeDefaultAspects != null && taskDataTypeDefaultProps != null));
            this.taskDataTypeDefaultAspects = taskDataTypeDefaultAspects;
            this.taskDataTypeDefaultProps = taskDataTypeDefaultProps;
            taskDataTypeSearchableProps = BeanHelper.getWorkflowService().getTaskDataTypeSearchableProps();
            this.taskPrefixedQNames = taskPrefixedQNames;
            this.originalParentRef = originalParentRef;
            this.workflowType = workflowType;
            this.workflow = workflow;
            this.copy = copy;
            this.limited = limited;
        }

        @Override
        public Task mapRow(ResultSet rs, int i) throws SQLException {
            checkTaskDataTypeDefinition(rs);
            loadRsColumnQNames(rs);
            WmNode taskNode = getNode(rs);
            WorkflowType workflowType = this.workflowType != null ? this.workflowType : BeanHelper.getWorkflowService().getWorkflowTypesByTask().get(taskNode.getType());
            Task task = Task.create(workflowType.getTaskClass(), taskNode, workflow, workflowType.getTaskOutcomes());
            boolean originalHasDueDateHistory = rs.getBoolean(DUE_DATE_HISTORY_FIELD);
            task.setHasDueDateHistory(originalHasDueDateHistory);
            task.setOriginalHasDueDateHistory(originalHasDueDateHistory);
            // use rs.getObject instead of rs.getInt to get null values also
            task.setTaskIndexInWorkflow((Integer) rs.getObject(INDEX_IN_WORKFLOW_FIELD));
            task.setWorkflowNodeRefId(rs.getString(WORKFLOW_ID_KEY));
            task.setStoreRef(rs.getString(STORE_ID_FIELD));
            if (limited && taskCountBeforeLimit < 0) {
                taskCountBeforeLimit = rs.getInt("full_count");
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
                    QName propName = DbSearchUtil.getPropQNameFromDbFieldName(columnLabel);
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

        private QName getTaskTypeFromRs(ResultSet rs) throws SQLException {
            String typeStr = rs.getString(TASK_TYPE_FIELD);
            return QName.createQName(WorkflowSpecificModel.URI, typeStr);
        }

        // if task type and/or parent is given, get task with the given properties.
        // Otherwise determine type and parent from rs
        private WmNode getNode(ResultSet rs) throws SQLException {
            QName taskType = workflowType != null ? workflowType.getTaskType() : getTaskTypeFromRs(rs);
            WorkflowService workflowService = BeanHelper.getWorkflowService();
            Set<QName> currentTaskAspects = new HashSet<QName>(taskDataTypeDefaultAspects != null ? taskDataTypeDefaultAspects : workflowService.getTaskDataTypeDefaultAspects()
                    .get(taskType));
            Set<QName> currentTaskProps = new HashSet<QName>(taskDataTypeDefaultProps != null ? taskDataTypeDefaultProps : workflowService.getTaskDataTypeDefaultProps()
                    .get(taskType));
            Map<QName, Serializable> taskProps = new HashMap<QName, Serializable>();
            if (rs.getObject(DbSearchUtil.ACTIVE_FIELD) != null) {
                currentTaskAspects.add(WorkflowSpecificModel.Aspects.RESPONSIBLE);
                currentTaskProps.add(WorkflowSpecificModel.Props.ACTIVE);
            }

            if (Boolean.TRUE.equals(rs.getObject(IS_SEARCHABLE_FIELD))) {
                currentTaskAspects.add(WorkflowSpecificModel.Aspects.SEARCHABLE);
                currentTaskProps.addAll(taskDataTypeSearchableProps);
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

        private int getTaskCountBeforeLimit() {
            return taskCountBeforeLimit;
        }

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
    public void deleteTasksCascading(NodeRef nodeRef, QName nodeTypeQName) {
        if (dictionaryService.isSubClass(nodeTypeQName, WorkflowCommonModel.Types.TASK)) {
            deleteTask(nodeRef);
        } else if (isWorkflow(nodeTypeQName)) {
            deleteWorkflowTasks(nodeRef);
        } else if (WorkflowCommonModel.Types.COMPOUND_WORKFLOW.equals(nodeTypeQName)) {
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

    private boolean isWorkflow(QName type) {
        return dictionaryService.isSubClass(type, WorkflowCommonModel.Types.WORKFLOW);
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
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

}