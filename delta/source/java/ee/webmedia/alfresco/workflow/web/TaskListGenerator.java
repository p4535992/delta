package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;
import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.DialogManager;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet.ClientValidation;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.search.SearchRenderer;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.Task.Action;

/**
 * Generator for compound workflow tasks setup block.
 * 
 * @author Erko Hansar
 */
public class TaskListGenerator extends BaseComponentGenerator {

    // private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(TaskListGenerator.class);

    public static final String ATTR_WORKFLOW_INDEX = "workflow_index";

    private DialogManager dialogManager;

    @Override
    public UIComponent generate(FacesContext context, String id) {
        throw new RuntimeException("This is never called!");
    }

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, final PropertySheetItem item) {
        Application application = context.getApplication();
        String listId = context.getViewRoot().createUniqueId();
        int index = (Integer) propertySheet.getAttributes().get(ATTR_WORKFLOW_INDEX);
        Workflow block = ((CompoundWorkflowDefinitionDialog) dialogManager.getBean()).getWorkflow().getWorkflows().get(index);
        boolean responsible = new Boolean(getCustomAttributes().get("responsible"));
        QName blockType = block.getNode().getType();
        boolean fullAccess = propertySheet.inEditMode() || (blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) && !responsible);

        final HtmlPanelGroup result = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        result.setId("task-panel-" + listId);

        addChildren(item, result);

        final HtmlPanelGrid taskGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        taskGrid.setId("task-grid-" + listId);
        taskGrid.setColumns(4);
        final String customStyleClass = StringUtils.trimToEmpty(getCustomAttributes().get("styleClass"));
        taskGrid.setStyleClass("recipient tasks" + " " + customStyleClass);
        final List<UIComponent> resultChildren = addChildren(result, taskGrid);

        List<Integer> visibleTasks = filterTasks(block.getTasks(), responsible);

        if (!visibleTasks.isEmpty()) {
            HtmlPanelGroup pickerPanel = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            pickerPanel.setId("task-picker-panel-" + listId);
            pickerPanel.setRendererType(TaskListPickerRenderer.class.getCanonicalName());
            resultChildren.add(pickerPanel);

            UIGenericPicker picker = createOwnerPickerComponent(application, listId, index, responsible);
            addChildren(pickerPanel, picker);

            // This disables doing AJAX submit when picker finish button is pressed
            // Currently, picker finish reconstructs entire panelgroup, which is some levels above propertysheet
            // If AJAX submit is desired, something needs to be reworked
            putAttribute(pickerPanel, Search.AJAX_PARENT_LEVEL_KEY, Integer.valueOf(100));

            TaskListCommentComponent commentPopup = null;
            if (blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) || blockType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)) {
                commentPopup = (TaskListCommentComponent) application.createComponent(TaskListCommentComponent.class.getCanonicalName());
                commentPopup.setId("task-comment-popup-" + listId);
                putAttribute(commentPopup, TaskListGenerator.ATTR_WORKFLOW_INDEX, index);
                commentPopup.setActionListener(application.createMethodBinding("#{DialogManager.bean.finishWorkflowTask}", UIActions.ACTION_CLASS_ARGS));
                resultChildren.add(commentPopup);
            }

            HtmlOutputText header = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
            header.setId("task-grid-name-" + listId);
            header.setEscape(false);
            final List<UIComponent> taskGridChildren = addChildren(taskGrid);
            if (dialogManager.getBean() instanceof CompoundWorkflowDialog) {
                UIOutput ownerNameHeading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                putAttribute(ownerNameHeading, "styleClass", "th");
                ownerNameHeading.setValue(MessageUtil.getMessage("workflow_task_owner_name"));
                taskGridChildren.add(ownerNameHeading);

                UIOutput dueDateHeading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                putAttribute(dueDateHeading, "styleClass", "th");
                dueDateHeading.setValue(MessageUtil.getMessage("task_property_due_date"));
                taskGridChildren.add(dueDateHeading);

                UIOutput wfStatusHeading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                putAttribute(wfStatusHeading, "styleClass", "th");
                wfStatusHeading.setValue(MessageUtil.getMessage("workflow_status"));
                taskGridChildren.add(wfStatusHeading);

                UIOutput actionsHeading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                putAttribute(actionsHeading, "styleClass", "th");
                actionsHeading.setValue("");
                taskGridChildren.add(actionsHeading);
            } else {
                header.setValue(MessageUtil.getMessage("workflow_task_owner_name"));
                @SuppressWarnings("unchecked")
                final Map<String, UIComponent> facets = taskGrid.getFacets();
                facets.put("header", header);
            }

            for (int counter = 0; counter < block.getTasks().size(); counter++) {
                if (visibleTasks.contains(counter)) {
                    Task task = block.getTasks().get(counter);
                    String taskStatus = task.getStatus();

                    HtmlInputText nameInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                    nameInput.setId("task-name-" + listId + "-" + counter);
                    nameInput.setReadonly(true);
                    putAttribute(nameInput, "styleClass", "ownerName medium");
                    String nameValueBinding = "#{DialogManager.bean.workflow.workflows[" + index + "].tasks[" + counter + "].node.properties[\""
                            + WorkflowCommonModel.Props.OWNER_NAME + "\"]}";
                    nameInput.setValueBinding("value", application.createValueBinding(nameValueBinding));
                    taskGridChildren.add(nameInput);

                    if (dialogManager.getBean() instanceof CompoundWorkflowDialog) {
                        final HtmlInputText dueDateInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                        taskGridChildren.add(dueDateInput);
                        dueDateInput.setId("task-duedate-" + listId + "-" + counter);
                        String dueDateValueBinding = "#{DialogManager.bean.workflow.workflows[" + index + "].tasks[" + counter + "].node.properties[\""
                                + WorkflowSpecificModel.Props.DUE_DATE + "\"]}";
                        dueDateInput.setValueBinding("value", application.createValueBinding(dueDateValueBinding));
                        ComponentUtil.createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, dueDateInput);
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> dueDateAttributes = dueDateInput.getAttributes();
                        if (fullAccess && Status.NEW.equals(taskStatus)
                                && (!responsible || Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)))) {
                            dueDateAttributes.put("styleClass", "margin-left-4 date");
                            dueDateAttributes.put("onchange", "processButtonState();");
                            // add client side validation
                            List<String> params = new ArrayList<String>(2);
                            params.add("document.getElementById('" + dueDateInput.getClientId(context) + "')");
                            String invalidMsg = MessageUtil.getMessage(context, "validation_date_failed", MessageUtil.getMessage("task_property_due_date"));
                            addStringConstraintParam(params, invalidMsg);
                            propertySheet.addClientValidation(new ClientValidation("validateDate", params, true));
                        } else {
                            dueDateAttributes.put("styleClass", "margin-left-4 disabled-date");
                            dueDateInput.setReadonly(true);
                        }

                        HtmlInputText statusInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                        statusInput.setId("task-status-" + listId + "-" + counter);
                        statusInput.setReadonly(true);
                        String statusValueBinding = "#{DialogManager.bean.workflow.workflows[" + index + "].tasks[" + counter + "].node.properties[\""
                                + WorkflowCommonModel.Props.STATUS + "\"]}";
                        statusInput.setValueBinding("value", application.createValueBinding(statusValueBinding));
                        putAttribute(statusInput, "styleClass", "margin-left-4 small");
                        taskGridChildren.add(statusInput);
                    }

                    final HtmlPanelGroup columnActions = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
                    columnActions.setId("column-actions-" + listId + "-" + counter);
                    taskGridChildren.add(columnActions);

                    Action taskAction = task.getAction();
                    final List<UIComponent> actionChildren = addChildren(columnActions);
                    if (fullAccess
                            && (taskAction == Action.NONE)
                            && (Status.NEW.equals(taskStatus)
                            || (task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE) && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED
                                    .equals(taskStatus))))
                            && (!responsible || Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)))) {
                        UIActionLink taskSearchLink = createOwnerSearchLink(context, application, listId, picker, counter);
                        actionChildren.add(taskSearchLink);
                    }
                    if (fullAccess && Status.NEW.equals(taskStatus)
                            && (!responsible || Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)))) {
                        UIParameter blockIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
                        blockIndex.setName("index");
                        blockIndex.setValue(index);

                        UIParameter taskIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
                        taskIndex.setName("taskIndex");
                        taskIndex.setValue(counter);

                        final UIActionLink taskDeleteLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                        taskDeleteLink.setId("task-remove-link-" + listId + "-" + counter);
                        taskDeleteLink.setValue("");
                        taskDeleteLink.setTooltip(MessageUtil.getMessage("delete"));
                        taskDeleteLink.setActionListener(application.createMethodBinding("#{DialogManager.bean.removeWorkflowTask}",
                                UIActions.ACTION_CLASS_ARGS));
                        taskDeleteLink.setShowLink(false);
                        putAttribute(taskDeleteLink, "styleClass", "icon-link margin-left-4 delete");
                        addChildren(taskDeleteLink, blockIndex, taskIndex);
                        actionChildren.add(taskDeleteLink);
                    }
                    if (taskAction == Action.NONE) {
                        if (fullAccess && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED.equals(taskStatus))
                                && !WorkflowUtil.isInactiveResponsible(task)) {
                            UIParameter blockIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
                            blockIndex.setName("index");
                            blockIndex.setValue(index);

                            UIParameter taskIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
                            taskIndex.setName("taskIndex");
                            taskIndex.setValue(counter);

                            final UIActionLink taskCancelLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                            taskCancelLink.setId("task-cancel-link-" + listId + "-" + counter);
                            taskCancelLink.setValue("");
                            taskCancelLink.setTooltip(MessageUtil.getMessage("task_cancel"));
                            taskCancelLink.setActionListener(application.createMethodBinding("#{DialogManager.bean.cancelWorkflowTask}",
                                    UIActions.ACTION_CLASS_ARGS));
                            taskCancelLink.setShowLink(false);
                            putAttribute(taskCancelLink, "styleClass", "icon-link margin-left-4 cancel-task");
                            addChildren(taskCancelLink, blockIndex, taskIndex);
                            actionChildren.add(taskCancelLink);
                        }
                        if (fullAccess && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED.equals(taskStatus) || Status.UNFINISHED.equals(taskStatus))
                                && !WorkflowUtil.isInactiveResponsible(task)) {
                            QName taskType = task.getNode().getType();
                            if (taskType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) || taskType.equals(WorkflowSpecificModel.Types.OPINION_TASK)) {
                                UIActionLink taskFinishLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                                taskFinishLink.setId("task-finish-link-" + listId + "-" + counter);
                                taskFinishLink.setValue("");
                                taskFinishLink.setTooltip(MessageUtil.getMessage("task_finish"));
                                taskFinishLink.setShowLink(false);
                                putAttribute(taskFinishLink, "styleClass", "icon-link margin-left-4 finish-task");
                                String onclick = ComponentUtil.generateFieldSetter(context, commentPopup, getActionId(context, commentPopup),
                                        TaskListCommentComponent.TASK_INDEX + ";" + counter);
                                onclick += "return showModal('" + getDialogId(context, commentPopup) + "');";
                                taskFinishLink.setOnclick(onclick);
                                actionChildren.add(taskFinishLink);
                            }
                        }
                        if (mustCreateAddTaskLink(block, responsible, fullAccess)) {
                            createAddTaskLink(application, listId, blockType, counter, columnActions, index, counter + 1, false);
                        }
                    } else {
                        HtmlOutputText actionTxt = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
                        actionTxt.setId("task-action-txt-" + listId + "-" + counter);
                        String actionMsg = MessageUtil.getMessage(taskAction == Action.UNFINISH ? "task_cancel_marked" : "task_finish_marked");
                        actionTxt.setValue(actionMsg);
                        actionChildren.add(actionTxt);
                        if (mustCreateAddTaskLink(block, responsible, fullAccess)) {
                            createAddTaskLink(application, listId, blockType, counter, columnActions, index, counter + 1, false);
                        }
                        if (blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
                            final boolean mustCreateOwnerSearchLink;
                            if (!responsible && Status.NEW.equals(block.getStatus())) {
                                mustCreateOwnerSearchLink = true;
                            } else if (responsible && !Status.FINISHED.equals(block.getStatus()) && WorkflowUtil.isActiveResponsible(task)) {
                                mustCreateOwnerSearchLink = true;
                            } else {
                                mustCreateOwnerSearchLink = false;
                            }
                            if (mustCreateOwnerSearchLink) {
                                UIActionLink taskSearchLink = createOwnerSearchLink(context, application, listId, picker, counter);
                                actionChildren.add(taskSearchLink);
                            }
                        }
                    }
                }
            }
        } else {
            if (mustCreateAddTaskLink(block, responsible, fullAccess)) {
                createAddTaskLink(application, listId, blockType, 0, result, index, 0, true);
            }
        }
        return result;
    }

    private boolean mustCreateAddTaskLink(Workflow block, boolean responsible, boolean fullAccess) {
        if (fullAccess && !responsible && !Status.FINISHED.equals(block.getStatus())) {
            return true;
        }
        return false;
    }

    private void createAddTaskLink(Application application, String listId, QName blockType, int counter, HtmlPanelGroup parentComponent, int index,
            int taskIndexValue, boolean setValue) {
        UIParameter blockIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
        blockIndex.setName("index");
        blockIndex.setValue(index);

        UIParameter taskIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
        taskIndex.setName("taskIndex");
        taskIndex.setValue(taskIndexValue); // Add after current

        UIActionLink taskAddLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        taskAddLink.setId("task-add-link-" + listId + "-" + counter);
        taskAddLink.setValue(setValue ? MessageUtil.getMessage("workflow_compound_add_" + blockType.getLocalName() + "_user") : "");
        taskAddLink.setTooltip(MessageUtil.getMessage("workflow_compound_add_" + blockType.getLocalName() + "_user"));
        taskAddLink.setActionListener(application.createMethodBinding("#{DialogManager.bean.addWorkflowTask}", UIActions.ACTION_CLASS_ARGS));
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = taskAddLink.getAttributes();
        attributes.put("styleClass", "icon-link add-person");
        taskAddLink.setShowLink(false);
        @SuppressWarnings("unchecked")
        final List<UIComponent> children = taskAddLink.getChildren();
        children.add(blockIndex);
        children.add(taskIndex);
        @SuppressWarnings("unchecked")
        final List<UIComponent> parentChildren = parentComponent.getChildren();
        parentChildren.add(taskAddLink);
    }

    public static UIActionLink createOwnerSearchLink(FacesContext context, Application application, String listId, UIGenericPicker picker, int taskNumber) {
        UIActionLink taskSearchLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        taskSearchLink.setId("task-search-link-" + listId + "-" + taskNumber);
        taskSearchLink.setValue("");
        taskSearchLink.setTooltip(MessageUtil.getMessage("search"));
        taskSearchLink.setShowLink(false);
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = taskSearchLink.getAttributes();
        attributes.put("styleClass", "icon-link margin-left-4 search");
        String onclick = ComponentUtil.generateFieldSetter(context, picker, getActionId(context, picker), SearchRenderer.OPEN_DIALOG_ACTION + ";" + taskNumber);
        onclick += "return showModal('" + getDialogId(context, picker) + "');";
        taskSearchLink.setOnclick(onclick);
        return taskSearchLink;
    }

    private static UIGenericPicker createOwnerPickerComponent(Application application, String listId, int workflowIndex, boolean isResponsibleTask) {
        UIGenericPicker picker = (UIGenericPicker) application.createComponent("org.alfresco.faces.GenericPicker");
        picker.setId("task-picker-" + listId);
        picker.setShowFilter(false);
        picker.setWidth(400);
        picker.setMultiSelect(!isResponsibleTask);
        MethodBinding pickerB = null;
        if (isResponsibleTask) {
            pickerB = application.createMethodBinding("#{DialogManager.bean.executeResponsibleOwnerSearch}", new Class[] { int.class, String.class });
        } else {
            pickerB = application.createMethodBinding("#{DialogManager.bean.executeOwnerSearch}", new Class[] { int.class, String.class });
        }
        picker.setQueryCallback(pickerB);
        picker.setActionListener(application.createMethodBinding("#{DialogManager.bean.processOwnerSearchResults}", UIActions.ACTION_CLASS_ARGS));
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = picker.getAttributes();
        attributes.put(TaskListGenerator.ATTR_WORKFLOW_INDEX, workflowIndex);
        picker.setShowFilter(true);
        ValueBinding pickerV = null;
        if (isResponsibleTask) {
            pickerV = application.createValueBinding("#{DialogManager.bean.responsibleOwnerSearchFilters}");
        } else {
            pickerV = application.createValueBinding("#{DialogManager.bean.ownerSearchFilters}");
        }
        picker.setValueBinding("filters", pickerV);
        return picker;
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        // overwrite parent and do nothing.
    }

    public static String getDialogId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + "_popup";
    }

    public static String getActionId(FacesContext context, UIComponent component) {
        return component.getParent().getClientId(context) + "_action";
    }

    public void setDialogManager(DialogManager dialogManager) {
        this.dialogManager = dialogManager;
    }

    @Override
    protected String getValidateMandatoryJsFunctionName() {
        // if needed, we could implement JS function, that performs client-side validation
        return null;
    }

    // /// PRIVATE METHODS /////

    private List<Integer> filterTasks(List<Task> tasks, boolean responsible) {
        List<Integer> result = new ArrayList<Integer>();
        int index = 0;
        for (Task task : tasks) {
            boolean hasResponsible = task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE);
            if (responsible == hasResponsible) {
                result.add(index);
            }
            index++;
        }
        return result;
    }

}
