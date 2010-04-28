package ee.webmedia.alfresco.workflow.web;

import java.util.ArrayList;
import java.util.List;

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

    //private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(TaskListGenerator.class);
    
    public static final String ATTR_WORKFLOW_INDEX = "workflow_index";

    private DialogManager dialogManager;
    
    @Override
    public UIComponent generate(FacesContext context, String id) {
        throw new RuntimeException("This is never called!");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item) {
        Application application = context.getApplication();
        String listId = context.getViewRoot().createUniqueId();
        int index = (Integer)propertySheet.getAttributes().get(ATTR_WORKFLOW_INDEX);
        Workflow block = ((CompoundWorkflowDefinitionDialog)dialogManager.getBean()).getWorkflow().getWorkflows().get(index);
        boolean responsible = new Boolean(getCustomAttributes().get("responsible")); 
        QName blockType = block.getNode().getType();
        boolean fullAccess = propertySheet.inEditMode() || (blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) && !responsible);
        
        HtmlPanelGroup result = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        result.setId("task-panel-" + listId);
        
        item.getChildren().add(result);
        
        HtmlPanelGrid taskGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        taskGrid.setId("task-grid-" + listId);
        taskGrid.setColumns(4);
        final String customStyleClass = StringUtils.trimToEmpty(getCustomAttributes().get("styleClass"));
        taskGrid.setStyleClass("recipient tasks" + " " + customStyleClass);
        result.getChildren().add(taskGrid);        

        List<Integer> visibleTasks = filterTasks(block.getTasks(), responsible);
        
        if (!visibleTasks.isEmpty()) {
            HtmlPanelGroup pickerPanel = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            pickerPanel.setId("task-picker-panel-" + listId);
            pickerPanel.setRendererType(TaskListPickerRenderer.class.getCanonicalName());
            result.getChildren().add(pickerPanel);
            
            UIGenericPicker picker = (UIGenericPicker) application.createComponent("org.alfresco.faces.GenericPicker");
            picker.setId("task-picker-" + listId);
            picker.setShowFilter(false);
            picker.setWidth(400);
            picker.setMultiSelect(!responsible);
            MethodBinding pickerB = null;
            if (responsible) {
                pickerB = application.createMethodBinding("#{DialogManager.bean.executeResponsibleOwnerSearch}", new Class[] { int.class, String.class });
            }
            else {
                pickerB = application.createMethodBinding("#{DialogManager.bean.executeOwnerSearch}", new Class[] { int.class, String.class });
            }
            picker.setQueryCallback(pickerB);
            picker.setActionListener(application.createMethodBinding("#{DialogManager.bean.processOwnerSearchResults}", UIActions.ACTION_CLASS_ARGS));
            picker.getAttributes().put(TaskListGenerator.ATTR_WORKFLOW_INDEX, index);
            picker.setShowFilter(true);
            ValueBinding pickerV = null;
            if (responsible) {
                pickerV = application.createValueBinding("#{DialogManager.bean.responsibleOwnerSearchFilters}");
            }
            else {
                pickerV = application.createValueBinding("#{DialogManager.bean.ownerSearchFilters}");
            }
            picker.setValueBinding("filters", pickerV);
            pickerPanel.getChildren().add(picker);
            
            TaskListCommentComponent commentPopup = null;
            if (blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) || blockType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)) {
                commentPopup = (TaskListCommentComponent) application.createComponent(TaskListCommentComponent.class.getCanonicalName());
                commentPopup.setId("task-comment-popup-" + listId);
                commentPopup.getAttributes().put(TaskListGenerator.ATTR_WORKFLOW_INDEX, index);
                commentPopup.setActionListener(application.createMethodBinding("#{DialogManager.bean.finishWorkflowTask}", UIActions.ACTION_CLASS_ARGS));
                result.getChildren().add(commentPopup);
            }

            HtmlOutputText header = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
            header.setId("task-grid-name-" + listId);
            header.setEscape(false);
            if (dialogManager.getBean() instanceof CompoundWorkflowDialog) {
                UIOutput ownerNameHeading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                ownerNameHeading.getAttributes().put("styleClass", "th");
                ownerNameHeading.setValue(MessageUtil.getMessage("workflow_task_owner_name"));
                taskGrid.getChildren().add(ownerNameHeading);
                
                UIOutput dueDateHeading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                dueDateHeading.getAttributes().put("styleClass", "th");
                dueDateHeading.setValue(MessageUtil.getMessage("task_property_due_date"));
                taskGrid.getChildren().add(dueDateHeading);
                
                UIOutput wfStatusHeading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                wfStatusHeading.getAttributes().put("styleClass", "th");
                wfStatusHeading.setValue(MessageUtil.getMessage("workflow_status"));
                taskGrid.getChildren().add(wfStatusHeading);
                
                UIOutput actionsHeading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                actionsHeading.getAttributes().put("styleClass", "th");
                actionsHeading.setValue("");
                taskGrid.getChildren().add(actionsHeading);
            }
            else {
                header.setValue(MessageUtil.getMessage("workflow_task_owner_name"));
                taskGrid.getFacets().put("header", header);
            }

            for (int counter = 0; counter < block.getTasks().size(); counter++) {
                if (visibleTasks.contains(counter)) {
                    Task task = block.getTasks().get(counter);
                    String taskStatus = task.getStatus();
    
                    HtmlInputText nameInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                    nameInput.setId("task-name-" + listId + "-" + counter);
                    nameInput.setReadonly(true);
                    nameInput.getAttributes().put("styleClass", "ownerName medium");
                    String nameValueBinding = "#{DialogManager.bean.workflow.workflows[" + index + "].tasks[" + counter + "].node.properties[\"" + WorkflowCommonModel.Props.OWNER_NAME + "\"]}";
                    nameInput.setValueBinding("value", application.createValueBinding(nameValueBinding));
                    taskGrid.getChildren().add(nameInput);
                    
                    if (dialogManager.getBean() instanceof CompoundWorkflowDialog) {
                        HtmlInputText dueDateInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                        taskGrid.getChildren().add(dueDateInput);
                        dueDateInput.setId("task-duedate-" + listId + "-" + counter);
                        String dueDateValueBinding = "#{DialogManager.bean.workflow.workflows[" + index + "].tasks[" + counter + "].node.properties[\"" + WorkflowSpecificModel.Props.DUE_DATE + "\"]}";
                        dueDateInput.setValueBinding("value", application.createValueBinding(dueDateValueBinding));
                        ComponentUtil.createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, dueDateInput);
                        if (fullAccess && Status.NEW.equals(taskStatus)
                                && (!responsible || Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)))) {
                            dueDateInput.getAttributes().put("styleClass", "margin-left-4 date");
                            dueDateInput.getAttributes().put("onchange", "processButtonState();");
                            // add client side validation
                            List<String> params = new ArrayList<String>(2);
                            params.add("document.getElementById('" + dueDateInput.getClientId(context) + "')");
                            String invalidMsg = MessageUtil.getMessage(context, "validation_date_failed", MessageUtil.getMessage("task_property_due_date"));
                            addStringConstraintParam(params, invalidMsg);
                            propertySheet.addClientValidation(new ClientValidation("validateDate", params, true));
                        }
                        else {
                            dueDateInput.getAttributes().put("styleClass", "margin-left-4 disabled-date");
                            dueDateInput.setReadonly(true);
                        }
                        
                        HtmlInputText statusInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                        statusInput.setId("task-status-" + listId + "-" + counter);
                        statusInput.setReadonly(true);
                        String statusValueBinding = "#{DialogManager.bean.workflow.workflows[" + index + "].tasks[" + counter + "].node.properties[\"" + WorkflowCommonModel.Props.STATUS + "\"]}";
                        statusInput.setValueBinding("value", application.createValueBinding(statusValueBinding));
                        statusInput.getAttributes().put("styleClass", "margin-left-4 medium");
                        taskGrid.getChildren().add(statusInput);
                    }

                    HtmlPanelGroup columnActions = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
                    columnActions.setId("column-actions-" + listId + "-" + counter);
                    taskGrid.getChildren().add(columnActions);

                    Action taskAction = task.getAction();
                    if (fullAccess && (taskAction == Action.NONE) && (Status.NEW.equals(taskStatus) 
                            || (task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE) && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED.equals(taskStatus))))
                            && (!responsible || Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)))) {
                        UIActionLink taskSearchLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                        taskSearchLink.setId("task-search-link-" + listId + "-" + counter);
                        taskSearchLink.setValue("");
                        taskSearchLink.setTooltip(MessageUtil.getMessage("search"));
                        taskSearchLink.setShowLink(false);
                        taskSearchLink.getAttributes().put("styleClass", "icon-link margin-left-4 search");
                        String onclick = ComponentUtil.generateFieldSetter(context, picker, getActionId(context, picker), SearchRenderer.OPEN_DIALOG_ACTION + ";" + counter);
                        onclick += "return showModal('" + getDialogId(context, picker) + "');";
                        taskSearchLink.setOnclick(onclick);
                        columnActions.getChildren().add(taskSearchLink);
                    }
                    if (fullAccess && Status.NEW.equals(taskStatus)
                            && (!responsible || Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)))) {
                        UIParameter blockIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
                        blockIndex.setName("index");
                        blockIndex.setValue(index);
                        
                        UIParameter taskIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
                        taskIndex.setName("taskIndex");
                        taskIndex.setValue(counter);
                        
                        UIActionLink taskDeleteLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                        taskDeleteLink.setId("task-remove-link-" + listId + "-" + counter);
                        taskDeleteLink.setValue("");
                        taskDeleteLink.setTooltip(MessageUtil.getMessage("delete"));
                        taskDeleteLink.setActionListener(application.createMethodBinding("#{DialogManager.bean.removeWorkflowTask}", UIActions.ACTION_CLASS_ARGS));
                        taskDeleteLink.setShowLink(false);
                        taskDeleteLink.getAttributes().put("styleClass", "icon-link margin-left-4 delete");
                        taskDeleteLink.getChildren().add(blockIndex);
                        taskDeleteLink.getChildren().add(taskIndex);
                        columnActions.getChildren().add(taskDeleteLink);
                    }
                    if (taskAction == Action.NONE) {
                        if (fullAccess && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED.equals(taskStatus)) && !WorkflowUtil.isInactiveResponsible(task)) {
                            UIParameter blockIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
                            blockIndex.setName("index");
                            blockIndex.setValue(index);
                            
                            UIParameter taskIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
                            taskIndex.setName("taskIndex");
                            taskIndex.setValue(counter);
                            
                            UIActionLink taskCancelLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                            taskCancelLink.setId("task-cancel-link-" + listId + "-" + counter);
                            taskCancelLink.setValue("");
                            taskCancelLink.setTooltip(MessageUtil.getMessage("task_cancel"));
                            taskCancelLink.setActionListener(application.createMethodBinding("#{DialogManager.bean.cancelWorkflowTask}", UIActions.ACTION_CLASS_ARGS));
                            taskCancelLink.setShowLink(false);
                            taskCancelLink.getAttributes().put("styleClass", "icon-link margin-left-4 cancel-task");
                            taskCancelLink.getChildren().add(blockIndex);
                            taskCancelLink.getChildren().add(taskIndex);
                            columnActions.getChildren().add(taskCancelLink);
                        }
                        if (fullAccess && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED.equals(taskStatus) || Status.UNFINISHED.equals(taskStatus)) && !WorkflowUtil.isInactiveResponsible(task)) {
                            QName taskType = task.getNode().getType();
                            if (taskType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) || taskType.equals(WorkflowSpecificModel.Types.OPINION_TASK)) {
                                UIActionLink taskFinishLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                                taskFinishLink.setId("task-finish-link-" + listId + "-" + counter);
                                taskFinishLink.setValue("");
                                taskFinishLink.setTooltip(MessageUtil.getMessage("task_finish"));
                                taskFinishLink.setShowLink(false);
                                taskFinishLink.getAttributes().put("styleClass", "icon-link margin-left-4 finish-task");
                                String onclick = ComponentUtil.generateFieldSetter(context, commentPopup, getActionId(context, commentPopup), TaskListCommentComponent.TASK_INDEX + ";" + counter);
                                onclick += "return showModal('" + getDialogId(context, commentPopup) + "');";
                                taskFinishLink.setOnclick(onclick);
                                columnActions.getChildren().add(taskFinishLink);
                            }
                        }
                    }
                    else {
                        HtmlOutputText actionTxt = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
                        actionTxt.setId("task-action-txt-" + listId + "-" + counter);
                        String actionMsg = MessageUtil.getMessage(taskAction == Action.UNFINISH ? "task_cancel_marked" : "task_finish_marked");
                        actionTxt.setValue(actionMsg);
                        columnActions.getChildren().add(actionTxt);
                    }
                }
            }
        }
        
        String blockStatus = block.getStatus();
        if (fullAccess && !responsible && !Status.FINISHED.equals(blockStatus)) {
            UIParameter blockIndex = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
            blockIndex.setName("index");
            blockIndex.setValue(index);
            
            UIActionLink taskAddLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
            taskAddLink.setId("task-add-link-" + listId);
            taskAddLink.setValue(MessageUtil.getMessage("workflow_compound_add_"+ blockType.getLocalName() +"_user"));
            taskAddLink.setActionListener(application.createMethodBinding("#{DialogManager.bean.addWorkflowTask}", UIActions.ACTION_CLASS_ARGS));
            taskAddLink.getAttributes().put("styleClass", "icon-link add-person");
            taskAddLink.getChildren().add(blockIndex);
            taskGrid.getFacets().put("footer", taskAddLink);
        }

        return result;
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef, UIComponent component) {
        // overwrite parent and do nothing.
    }

    public static String getDialogId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + "_popup";
    }

    public static String getActionId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + "_action";
    }
    
    public void setDialogManager(DialogManager dialogManager) {
        this.dialogManager = dialogManager;
    }

    @Override
    protected String getValidateMandatoryJsFunctionName() {
        // if needed, we could implement JS function, that performs client-side validation
        return null;
    }

    ///// PRIVATE METHODS /////
    
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
