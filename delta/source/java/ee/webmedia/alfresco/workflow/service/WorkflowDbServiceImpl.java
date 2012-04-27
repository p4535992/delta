package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.utils.RepoUtil.isSaved;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UserUtil;
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

    private static final String HAS_FILES = "has_files";
    private static final String INDEX_IN_WORKFLOW_FIELD = "index_in_workflow";
    private static final String TASK_ID_FIELD = "task_id";
    private static final String TASK_TYPE_FIELD = "task_type";
    private static final String DUE_DATE_HISTORY_FIELD = "has_due_date_history";
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(WorkflowDbServiceImpl.class);
    private static final List<QName> NOT_SENT_TO_REPO_PROPS = Arrays.asList(QName.createQName(WorkflowCommonModel.URI, "creatorId"),
            QName.createQName(WorkflowCommonModel.URI, "creatorEmail"), MoveTaskFileToChildAssoc.TASK_FILE_PROP_QNAME);
    private static final String SEPARATOR = "_";

    private SimpleJdbcTemplate jdbcTemplate;
    private DictionaryService dictionaryService;
    private NodeService nodeService;

    @Override
    public void createTaskEntry(Task task) {
        createTaskEntry(task, null);
    }

    @Override
    public void updateTaskEntry(Task task, Map<QName, Serializable> changedProps) {
        if (task == null) {
            return;
        }
        verifyTask(task, null);
        Pair<List<String>, List<Object>> fieldNamesAndArguments = getFieldNamesAndArguments(task, null, changedProps);
        List<Object> arguments = fieldNamesAndArguments.getSecond();
        if (arguments.isEmpty()) {
            // no properties were changed, no update needed
            return;
        }
        arguments.add(task.getNodeRef().getId());
        updateTaskEntry(fieldNamesAndArguments.getFirst(), arguments, task.getNodeRef());
        task.setOriginalHasDueDateHistory(task.getHasDueDateHistory());
        task.setOriginalHasFiles(task.getHasFiles());
    }

    @Override
    public void createTaskEntry(Task task, NodeRef workflowfRef) {
        if (task == null) {
            return;
        }
        verifyTask(task, workflowfRef);
        Assert.isTrue(task.getTaskIndexInWorkflow() >= 0);
        Pair<List<String>, List<Object>> fieldNamesAndArguments = getFieldNamesAndArguments(task, workflowfRef, RepoUtil.toQNameProperties(task.getNode().getProperties()));

        List<String> fields = fieldNamesAndArguments.getFirst();
        fields.add("is_searchable");
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

    private void updateTaskEntry(List<String> fieldNames, List<Object> arguments, NodeRef taskRef) {
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
        int rowsUpdated = jdbcTemplate.update(sqlQuery, arguments.toArray());
        if (rowsUpdated != 1) {
            throw new RuntimeException("Update failed: updated " + rowsUpdated + " rows for task nodeRef=" + taskRef + ", sql='" + sqlQuery + "', arguments=" + arguments);
        }
    }

    private void addTaskEntry(List<String> fieldNames, List<Object> arguments) {
        String questionMarks = getArgumentQuestionMarks(fieldNames);
        jdbcTemplate.update(
                "INSERT INTO delta_task (" + TextUtil.joinNonBlankStringsWithComma(fieldNames) + ") VALUES (" + questionMarks + ")", arguments.toArray());

    }

    private String getArgumentQuestionMarks(List<String> fieldNames) {
        String questionMarks = StringUtils.repeat("?, ", fieldNames.size());
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
        boolean addTaskIndexInWorkflow = task.getTaskIndexInWorkflow() >= 0;
        fieldNames.addAll(Arrays.asList(TASK_ID_FIELD, "workflow_id", TASK_TYPE_FIELD));
        String workflow_id = task.getParent() != null ? task.getParent().getNodeRef().getId() : workflowfRef.getId();
        arguments.addAll(Arrays.asList(task.getNodeRef().getId(), workflow_id, task.getType().getLocalName()));
        if (addTaskIndexInWorkflow) {
            fieldNames.add(INDEX_IN_WORKFLOW_FIELD);
            arguments.add(task.getTaskIndexInWorkflow());
        }
        if (!ObjectUtils.equals(task.getHasDueDateHistory(), task.getOriginalHasDueDateHistory())) {
            fieldNames.add(DUE_DATE_HISTORY_FIELD);
            arguments.add(task.getHasDueDateHistory());
        }
        if (!ObjectUtils.equals(task.getHasFiles(), task.getOriginalHasFiles())) {
            fieldNames.add(HAS_FILES);
            arguments.add(task.getHasFiles());
        }
        getPropFieldNamesAndArguments(fieldNames, arguments, changedProps);
        return new Pair<List<String>, List<Object>>(fieldNames, arguments);
    }

    private void getPropFieldNamesAndArguments(List<String> fieldNames, List<Object> arguments, Map<QName, Serializable> taskProps) {
        for (Map.Entry<QName, Serializable> entry : taskProps.entrySet()) {
            QName propName = entry.getKey();
            if (!WorkflowCommonModel.URI.equals(propName.getNamespaceURI()) && !WorkflowSpecificModel.URI.equals(propName.getNamespaceURI())) {
                continue;
            }
            if (NOT_SENT_TO_REPO_PROPS.contains(propName)) {
                continue;
            }
            Serializable value = entry.getValue();
            if (WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.equals(propName)) {
                value = UserUtil.getDisplayUnit((Iterable<String>) value);
            }
            fieldNames.add(getDbFieldNameFromPropQName(propName));
            arguments.add(value);
        }
    }

    private String getDbFieldNameFromPropQName(QName propName) {
        String prefix = WorkflowCommonModel.URI.equals(propName.getNamespaceURI()) ? getPrefix(WorkflowCommonModel.PREFIX) : (WorkflowSpecificModel.URI.equals(propName
                .getNamespaceURI())
                ? getPrefix(WorkflowSpecificModel.PREFIX) : "");
        Assert.isTrue(StringUtils.isNotBlank(prefix));
        String localName = propName.getLocalName();
        Assert.isTrue(StringUtils.isNotBlank(localName));
        StringBuilder sb = new StringBuilder(prefix + SEPARATOR);
        for (int i = 0; i < localName.length(); i++) {
            char c = localName.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(SEPARATOR + Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private QName getPropQNameFromDbFieldName(String fieldName) {
        String namespaceUri = null;
        String prefix = null;
        if (fieldName.startsWith(prefix = getPrefix(WorkflowCommonModel.PREFIX) + SEPARATOR)) {
            namespaceUri = WorkflowCommonModel.URI;
        } else if (fieldName.startsWith(prefix = getPrefix(WorkflowSpecificModel.PREFIX) + SEPARATOR)) {
            namespaceUri = WorkflowSpecificModel.URI;
        } else {
            return null;
        }
        StringTokenizer st = new StringTokenizer(fieldName.substring(prefix.length()), SEPARATOR);
        StringBuilder sb = new StringBuilder();
        boolean firstToken = true;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            char firstChar = firstToken ? token.charAt(0) : Character.toUpperCase(token.charAt(0));
            sb.append(firstChar + token.substring(1));
            firstToken = false;
        }
        return QName.createQName(namespaceUri, sb.toString());
    }

    private String getPrefix(String prefix) {
        return (new StringTokenizer(prefix, ":")).nextToken();
    }

    @Override
    public List<Task> getWorkflowTasks(NodeRef originalParentRef, Collection<QName> taskDataTypeDefaultAspects, List<QName> taskDataTypeDefaultProps,
            Map<QName, QName> taskPrefixedQNames, WorkflowType workflowType, Workflow workflow, boolean copy) {
        return jdbcTemplate.query("SELECT * FROM delta_task where workflow_id=? ORDER BY index_in_workflow",
                new TaskRowMapper(originalParentRef, taskDataTypeDefaultAspects, taskDataTypeDefaultProps, taskPrefixedQNames, workflowType, workflow, copy),
                new Object[] { originalParentRef.getId() });
    }

    private class TaskRowMapper implements ParameterizedRowMapper<Task> {
        private final Collection<QName> taskDataTypeDefaultAspects;
        private final List<QName> taskDataTypeDefaultProps;
        private Map<String, QName> rsColumnQNames;
        private final Map<QName, QName> taskPrefixedQNames;
        private final NodeRef originalParentRef;
        private final WorkflowType workflowType;
        private final Workflow workflow;
        private final boolean copy;

        public TaskRowMapper(NodeRef originalParentRef, Collection<QName> taskDataTypeDefaultAspects, List<QName> taskDataTypeDefaultProps, Map<QName, QName> taskPrefixedQNames,
                             WorkflowType workflowType, Workflow workflow, boolean copy) {
            this.taskDataTypeDefaultAspects = taskDataTypeDefaultAspects;
            this.taskDataTypeDefaultProps = taskDataTypeDefaultProps;
            this.taskPrefixedQNames = taskPrefixedQNames;
            this.originalParentRef = originalParentRef;
            this.workflowType = workflowType;
            this.workflow = workflow;
            this.copy = copy;
        }

        @Override
        public Task mapRow(ResultSet rs, int i) throws SQLException {
            checkTaskDataTypeDefinition(rs);
            loadRsColumnQNames(rs);
            WmNode taskNode = getNode(rs, workflowType.getTaskType());
            Task task = Task.create(workflowType.getTaskClass(), taskNode, workflow, workflowType.getTaskOutcomes());
            boolean originalHasDueDateHistory = rs.getBoolean(DUE_DATE_HISTORY_FIELD);
            task.setHasDueDateHistory(originalHasDueDateHistory);
            task.setOriginalHasDueDateHistory(originalHasDueDateHistory);
            boolean originalHasFiles = rs.getBoolean(HAS_FILES);
            task.setHasFiles(originalHasFiles);
            task.setOriginalHasFiles(originalHasFiles);
            task.setTaskIndexInWorkflow(rs.getInt(INDEX_IN_WORKFLOW_FIELD));
            return task;
        }

        private void loadRsColumnQNames(ResultSet rs) throws SQLException {
            if (rsColumnQNames == null) {
                rsColumnQNames = new HashMap<String, QName>();
                ResultSetMetaData metaData = rs.getMetaData();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String columnLabel = metaData.getColumnName(i);
                    QName propName = getPropQNameFromDbFieldName(columnLabel);
                    if (propName != null) {
                        rsColumnQNames.put(columnLabel, propName);
                    }
                }
            }
        }

        private void checkTaskDataTypeDefinition(ResultSet rs) throws SQLException {
            String typeStr = rs.getString(TASK_TYPE_FIELD);
            QName typeQName = QName.createQName(WorkflowSpecificModel.URI, typeStr);
            QName workflowTaskType = workflowType.getTaskType();
            if (!workflowTaskType.equals(typeQName)) {
                throw new RuntimeException("Workflow task type '" + workflowTaskType + "' doesn't match task type '" + typeQName + "' in db!");
            }
        }

        private WmNode getNode(ResultSet rs, QName taskType) throws SQLException {
            Set<QName> currentTaskAspects = new HashSet<QName>(taskDataTypeDefaultAspects);
            Set<QName> currentTaskProps = new HashSet<QName>(taskDataTypeDefaultProps);
            Map<QName, Serializable> taskProps = new HashMap<QName, Serializable>();
            if (rs.getObject("wfs_active") != null) {
                currentTaskAspects.add(WorkflowSpecificModel.Aspects.RESPONSIBLE);
                currentTaskProps.add(WorkflowSpecificModel.Props.ACTIVE);
            }
            for (Map.Entry<String, QName> entry : rsColumnQNames.entrySet()) {
                QName propName = entry.getValue();
                Object value = rs.getObject(entry.getKey());
                if (currentTaskProps.contains(propName)) {
                    if (WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.equals(propName)) {
                        value = Collections.singletonList(value);
                    }
                    taskProps.put(propName, (Serializable) value);
                }
            }
            return new WmNode(copy ? null : new NodeRef(originalParentRef.getStoreRef(), rs.getString(TASK_ID_FIELD)), taskPrefixedQNames.get(taskType), currentTaskAspects,
                    taskProps);
        }
    }

    private void deleteWorkflowTasks(NodeRef removedWorkflowNodeRef) {
        jdbcTemplate.update("DELETE FROM delta_task WHERE workflow_id=?", new Object[] { removedWorkflowNodeRef.getId() });
    }

    private void deleteTask(NodeRef removedTaskNodeRef) {
        jdbcTemplate.update("DELETE FROM delta_task WHERE task_id=?", new Object[] { removedTaskNodeRef.getId() });
    }

    @Override
    public void deleteTasksCascading(NodeRef nodeRef, QName nodeTypeQName) {
        if (dictionaryService.isSubClass(nodeTypeQName, WorkflowCommonModel.Types.TASK)) {
            deleteTask(nodeRef);
        } else if (WorkflowCommonModel.Types.WORKFLOW.equals(nodeTypeQName)) {
            deleteWorkflowTasks(nodeRef);
        } else if (WorkflowCommonModel.Types.COMPOUND_WORKFLOW.equals(nodeTypeQName)) {
            deleteCompoundWorkflowTasks(nodeRef);
        } else if (DocumentCommonModel.Types.DOCUMENT.equals(nodeTypeQName)) {
            deleteDocumentTasks(nodeRef);
        } else if (CaseModel.Types.CASE.equals(nodeTypeQName) || VolumeModel.Types.VOLUME.equals(nodeTypeQName)) {
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
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, Collections.singleton(VolumeModel.Types.VOLUME));
        for (ChildAssociationRef childAssoc : childAssocs) {
            deleteCaseOrVolumeTasks(childAssoc.getChildRef());
        }
    }

    private void deleteCaseOrVolumeTasks(NodeRef nodeRef) {
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
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, Collections.singleton(WorkflowCommonModel.Types.WORKFLOW));
        for (ChildAssociationRef childAssoc : childAssocs) {
            deleteWorkflowTasks(childAssoc.getChildRef());
        }
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

}
