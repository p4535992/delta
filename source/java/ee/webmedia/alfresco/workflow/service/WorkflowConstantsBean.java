package ee.webmedia.alfresco.workflow.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.ui.repo.component.UIActions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * Bean for holding application workflow-specific configuration data that is not going to change during uptime.
 */
public class WorkflowConstantsBean implements InitializingBean {

    public static final String BEAN_NAME = "workflowConstantsBean";

    private boolean orderAssignmentCategoryEnabled;
    private boolean orderAssignmentWorkflowEnabled;
    private boolean confirmationWorkflowEnabled;
    private boolean groupAssignmentWorkflowEnabled;
    private boolean independentWorkflowEnabled;
    private boolean documentWorkflowEnabled;
    private boolean workflowTitleEnabled;
    private boolean reviewToOtherOrgEnabled;
    private boolean finishDocumentsWhenWorkflowFinishes;
    private boolean externalReviewWorkflowEnabled;
    private final Map<QName, WorkflowType> workflowTypesByWorkflow = new HashMap<QName, WorkflowType>();
    private Map<QName, WorkflowType> unmodifiableWorkflowTypesByWorkflow = Collections.unmodifiableMap(new HashMap<QName, WorkflowType>());
    private final Map<QName, WorkflowType> workflowTypesByTask = new HashMap<QName, WorkflowType>();
    private Set<QName> allWorkflowTypes = new HashSet();
    private final Map<QName, Collection<QName>> taskDataTypeDefaultAspects = new HashMap<QName, Collection<QName>>();
    private final Map<QName, List<QName>> taskDataTypeDefaultProps = new HashMap<QName, List<QName>>();
    private final Map<QName, QName> taskPrefixedQNames = new HashMap<QName, QName>();
    private List<QName> taskDataTypeSearchableProps;
    private final Map<String, String> workflowTypeMessages = new HashMap<String, String>();
    private final Map<String, String> taskTypeMessages = new HashMap<String, String>();
    private final Map<String, String> taskToWorkflowTypeMapping = new HashMap<>();
    private TaskListConstants taskListConstants;

    private String assignmentWorkflowCoOwnerMessage;
    private String emptyTaskValueMessage;
    private Map<CompoundWorkflowType, String> compoundWorkflowTypeMessages;

    private ApplicationConstantsBean applicationConstantsBean;
    private DictionaryService dictionaryService;
    private GeneralService generalService;
    private NamespaceService namespaceService;

    @Override
    public void afterPropertiesSet() throws Exception {
        assignmentWorkflowCoOwnerMessage = MessageUtil.getMessage("assignmentWorkflow_coOwner");
        emptyTaskValueMessage = MessageUtil.getMessage("task_empty_value");
        Map<CompoundWorkflowType, String> tmp = new HashMap<>();
        for (CompoundWorkflowType type : CompoundWorkflowType.values()) {
            tmp.put(type, MessageUtil.getMessage(type));
        }
        compoundWorkflowTypeMessages = Collections.unmodifiableMap(tmp);
    }

    public TaskListConstants getTaskListConstants() {
        if (taskListConstants == null) {
            taskListConstants = TaskListConstants.newInstance();
        }
        return taskListConstants;
    }

    public boolean isWorkflowEnabled() {
        return applicationConstantsBean.isCaseVolumeEnabled() || independentWorkflowEnabled || documentWorkflowEnabled;
    }

    public boolean getOrderAssignmentCategoryEnabled() {
        return orderAssignmentCategoryEnabled;
    }

    public void setOrderAssignmentCategoryEnabled(boolean orderAssignmentCategoryEnabled) {
        this.orderAssignmentCategoryEnabled = orderAssignmentCategoryEnabled;
    }

    public boolean isOrderAssignmentWorkflowEnabled() {
        return orderAssignmentWorkflowEnabled;
    }

    public void setOrderAssignmentWorkflowEnabled(boolean enabled) {
        orderAssignmentWorkflowEnabled = enabled;
    }

    public boolean isGroupAssignmentWorkflowEnabled() {
        return groupAssignmentWorkflowEnabled;
    }

    public void setGroupAssignmentWorkflowEnabled(boolean enabled) {
        groupAssignmentWorkflowEnabled = enabled;
    }

    public void setConfirmationWorkflowEnabled(boolean enabled) {
        confirmationWorkflowEnabled = enabled;
    }

    public boolean isConfirmationWorkflowEnabled() {
        return confirmationWorkflowEnabled;
    }

    public void setIndependentWorkflowEnabled(boolean independentWorkflowEnabled) {
        this.independentWorkflowEnabled = independentWorkflowEnabled;
    }

    public boolean isIndependentWorkflowEnabled() {
        return independentWorkflowEnabled;
    }

    public boolean isDocumentWorkflowEnabled() {
        return documentWorkflowEnabled;
    }

    public void setDocumentWorkflowEnabled(boolean documentWorkflowEnabled) {
        this.documentWorkflowEnabled = documentWorkflowEnabled;
    }

    public void setWorkflowTitleEnabled(boolean workflowTitleEnabled) {
        this.workflowTitleEnabled = workflowTitleEnabled;
    }

    public boolean isWorkflowTitleEnabled() {
        return workflowTitleEnabled;
    }

    public boolean isReviewToOtherOrgEnabled() {
        return reviewToOtherOrgEnabled;
    }

    public void setReviewToOtherOrgEnabled(boolean reviewToOtherOrgEnabled) {
        this.reviewToOtherOrgEnabled = reviewToOtherOrgEnabled;
    }

    public boolean isFinishDocumentsWhenWorkflowFinishes() {
        return finishDocumentsWhenWorkflowFinishes;
    }

    public void setFinishDocumentsWhenWorkflowFinishes(boolean finishDocumentsWhenWorkflowFinishes) {
        this.finishDocumentsWhenWorkflowFinishes = finishDocumentsWhenWorkflowFinishes;
    }

    public void setApplicationConstantsBean(ApplicationConstantsBean applicationConstantsBean) {
        this.applicationConstantsBean = applicationConstantsBean;
    }

    public boolean isExternalReviewWorkflowEnabled() {
        return externalReviewWorkflowEnabled;
    }

    public void setExternalReviewWorkflowEnabled(boolean externalReviewWorkflowEnabled) {
        this.externalReviewWorkflowEnabled = externalReviewWorkflowEnabled;
    }

    public void registerWorkflowType(WorkflowType workflowType) {
        Assert.notNull(workflowType);

        Assert.isTrue(!workflowTypesByWorkflow.containsKey(workflowType.getWorkflowType()));
        boolean isIndependentTaskType = workflowType.isIndependentTaskType();
        if (!isIndependentTaskType) {
            QName workflowTypeQName = workflowType.getWorkflowType();
            Assert.isTrue(dictionaryService.isSubClass(workflowTypeQName, WorkflowCommonModel.Types.WORKFLOW));
            workflowTypesByWorkflow.put(workflowTypeQName, workflowType);
            String workflowTypeLocalName = workflowTypeQName.getLocalName();
            workflowTypeMessages.put(workflowTypeLocalName, MessageUtil.getMessage(workflowTypeLocalName));
        }

        QName taskTypeQName = workflowType.getTaskType();
        if (taskTypeQName != null) {
            Assert.notNull(workflowType.getTaskClass());
            Assert.isTrue(!workflowTypesByTask.containsKey(taskTypeQName));
            Assert.isTrue(dictionaryService.isSubClass(taskTypeQName, WorkflowCommonModel.Types.TASK));
            workflowTypesByTask.put(taskTypeQName, workflowType);
            Collection<QName> aspects = RepoUtil.getAspectsIgnoringSystem(generalService.getDefaultAspects(taskTypeQName));
            taskDataTypeDefaultAspects.put(taskTypeQName, aspects);
            List<QName> taskDefaultProps = new ArrayList<QName>();
            taskDataTypeDefaultProps.put(taskTypeQName, taskDefaultProps);
            for (QName aspect : aspects) {
                addPropertyDefs(taskDefaultProps, dictionaryService.getPropertyDefs(aspect));
            }
            addPropertyDefs(taskDefaultProps, dictionaryService.getPropertyDefs(taskTypeQName));
            taskPrefixedQNames.put(taskTypeQName, taskTypeQName.getPrefixedQName(namespaceService));
            String taskTypeLocalName = taskTypeQName.getLocalName();
            taskTypeMessages.put(taskTypeLocalName, MessageUtil.getMessage(taskTypeLocalName));
            addTaskToWorkflowMapping(taskTypeLocalName, workflowType.getWorkflowType());
        }
        unmodifiableWorkflowTypesByWorkflow = Collections.unmodifiableMap(workflowTypesByWorkflow);
        allWorkflowTypes = Collections.unmodifiableSet((workflowTypesByWorkflow.keySet()));
    }

    private void addTaskToWorkflowMapping(String taskTypeLocalName, QName workflowType) {
        if (workflowType == null) {
            return;
        }

        taskToWorkflowTypeMapping.put(taskTypeLocalName, MessageUtil.getMessage(workflowType.getLocalName()));
    }

    private void addPropertyDefs(List<QName> taskDefaultProps, Map<QName, PropertyDefinition> propertyDefs) {
        for (Map.Entry<QName, PropertyDefinition> entry : propertyDefs.entrySet()) {
            PropertyDefinition propDef = entry.getValue();
            QName prop = propDef.getName();
            if ((WorkflowCommonModel.URI.equals(prop.getNamespaceURI()) || WorkflowSpecificModel.URI.equals(prop.getNamespaceURI()))) {
                taskDefaultProps.add(entry.getKey());
            }
        }
    }

    public Map<QName, Collection<QName>> getTaskDataTypeDefaultAspects() {
        return taskDataTypeDefaultAspects;
    }

    public Map<QName, List<QName>> getTaskDataTypeDefaultProps() {
        return taskDataTypeDefaultProps;
    }

    public Map<QName, QName> getTaskPrefixedQNames() {
        return taskPrefixedQNames;
    }

    public Map<QName, WorkflowType> getWorkflowTypes() {
        return unmodifiableWorkflowTypesByWorkflow;
    }

    public Set<QName> getAllWorkflowTypes() {
        return allWorkflowTypes;
    }

    public Map<QName, WorkflowType> getWorkflowTypesByTask() {
        return workflowTypesByTask;
    }

    public Map<QName, WorkflowType> getWorkflowTypesByWorkflow() {
        return workflowTypesByWorkflow;
    }

    public List<QName> getTaskDataTypeSearchableProps() {
        if (taskDataTypeSearchableProps == null) {
            taskDataTypeSearchableProps = new ArrayList<QName>();
            Collection<QName> aspects = RepoUtil.getAspectsIgnoringSystem(generalService.getDefaultAspects(WorkflowSpecificModel.Aspects.SEARCHABLE));
            for (QName aspect : aspects) {
                addPropertyDefs(taskDataTypeSearchableProps, dictionaryService.getPropertyDefs(aspect));
            }
        }
        return taskDataTypeSearchableProps;
    }

    public String getWorkflowTypeName(QName workflowType) {
        return workflowTypeMessages.get(workflowType.getLocalName());
    }

    public String getWorkflowTypeNameByTaskType(QName taskType) {
        WorkflowType workflowType = workflowTypesByTask.get(taskType);
        return workflowType != null ? workflowTypeMessages.get(workflowType.getWorkflowType().getLocalName()) : null;
    }

    public String getTaskTypeName(QName taskType) {
        return taskTypeMessages.get(taskType.getLocalName());
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public String getWorkflowTypeNameByTask(String taskType) {
        return taskToWorkflowTypeMapping.get(taskType);
    }

    public String getAssignmentWorkflowCoOwnerMessage() {
        return assignmentWorkflowCoOwnerMessage;
    }

    public String getEmptyTaskValueMessage() {
        return emptyTaskValueMessage;
    }

    public String getCompoundWorkflowTypeMessage(CompoundWorkflowType type) {
        return compoundWorkflowTypeMessages.get(type);
    }

    public static class TaskListConstants {

        private String deleteTaskLinkText;
        private String cancelTaskLinkText;
        private String taskPropertyDueDateMessage;
        private String finishTaskLinkText;
        private String cancelMarkedTasksText;
        private String finishMarkedTasksText;
        private String expandTaskGroupText;
        private String collapseTaskGroupText;

        private TaskListConstants() {
        }


        public String getDeleteTaskLinkText() {
            return deleteTaskLinkText;
        }

        public String getCancelTaskLinkText() {
            return cancelTaskLinkText;
        }

        public String getTaskPropertyDueDateMessage() {
            return taskPropertyDueDateMessage;
        }

        public String getFinishTaskLinkText() {
            return finishTaskLinkText;
        }

        public String getCancelMarkedTasksText() {
            return cancelMarkedTasksText;
        }

        public String getFinishMarkedTasksText() {
            return finishMarkedTasksText;
        }

        public String getExpandTaskGroupText() {
            return expandTaskGroupText;
        }

        public String getCollapseTaskGroupText() {
            return collapseTaskGroupText;
        }

        private static TaskListConstants newInstance() {
            TaskListConstants constants = new TaskListConstants();
            constants.deleteTaskLinkText = MessageUtil.getMessage("delete");
            constants.cancelTaskLinkText = MessageUtil.getMessage("task_cancel");
            constants.taskPropertyDueDateMessage = MessageUtil.getMessage("task_property_due_date");
            constants.finishTaskLinkText = MessageUtil.getMessage("task_finish");
            constants.cancelMarkedTasksText = MessageUtil.getMessage("task_cancel_marked");
            constants.finishMarkedTasksText = MessageUtil.getMessage("task_finish_marked");
            constants.expandTaskGroupText = MessageUtil.getMessage("show_details");
            constants.collapseTaskGroupText = MessageUtil.getMessage("hide_details");

            return constants;
        }

    }
}
