package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.tag.GenericPickerTag;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet.ClientValidation;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Depending on "taskType" attribute it generates taskList for delegating assignment task as
 * new ASSIGNMENT_RESPONSIBLE, ASSIGNMENT_NOT_RESPONSIBLE, INFORMATION or OPINION task
 * 
 * @author Ats Uiboupin
 */
public class DelegationTaskListGenerator extends TaskListGenerator {
    /**
     * Values for the "show-property" element attribute "taskType"
     */
    enum DelegatableTaskType {
        ASSIGNMENT_RESPONSIBLE(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)
        , ASSIGNMENT_NOT_RESPONSIBLE(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)
        , ORDER_ASSIGNMENT_RESPONSIBLE(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)
        , ORDER_ASSIGNMENT_NOT_RESPONSIBLE(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)
        , INFORMATION(WorkflowSpecificModel.Types.INFORMATION_WORKFLOW)
        , OPINION(WorkflowSpecificModel.Types.OPINION_WORKFLOW);

        private final QName workflowTypeQName;

        DelegatableTaskType(QName workflowTypeQName) {
            this.workflowTypeQName = workflowTypeQName;
        }

        public boolean isOrderAssignmentOrAssignmentWorkflow() {
            return WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW.equals(workflowTypeQName) || WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW.equals(workflowTypeQName);
        }

        public boolean isResponsibleTask() {
            return ASSIGNMENT_RESPONSIBLE.equals(this) || ORDER_ASSIGNMENT_RESPONSIBLE.equals(this);
        }

        public QName getWorkflowTypeQName() {
            return workflowTypeQName;
        }

        public static DelegatableTaskType getTypeByTask(Task task) {
            if (task.isType(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
                return INFORMATION;
            } else if (task.isType(WorkflowSpecificModel.Types.OPINION_TASK)) {
                return OPINION;
            } else if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
                if (task.isResponsible()) {
                    return ASSIGNMENT_RESPONSIBLE;
                } else {
                    return ASSIGNMENT_NOT_RESPONSIBLE;
                }
            } else {
                throw new RuntimeException("No DelegatableTaskType defined for task type " + task.getType());
            }
        }

        public QName getTaskTypeQName() {
            if (WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW.equals(workflowTypeQName)) {
                return WorkflowSpecificModel.Types.ASSIGNMENT_TASK;
            } else if (WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW.equals(workflowTypeQName)) {
                return WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK;
            } else if (INFORMATION.equals(this)) {
                return WorkflowSpecificModel.Types.INFORMATION_TASK;
            } else if (OPINION.equals(this)) {
                return WorkflowSpecificModel.Types.OPINION_TASK;
            } else {
                throw new RuntimeException("Unknown constant " + this);
            }
        }
    }

    public static final String ATTRIB_DELEGATE_TASK_TYPE = "delegateTaskType";

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, final PropertySheetItem item) {
        @SuppressWarnings("unchecked")
        Map<String, Object> propSheetAttrs = propertySheet.getAttributes();

        DelegationBean delegationBean = (DelegationBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), DelegationBean.BEAN_NAME);
        int delegatableTaskIndex = (Integer) propSheetAttrs.get(DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX);
        DelegatableTaskType dTaskType = DelegatableTaskType.valueOf(getCustomAttributes().get("taskType"));

        List<Task> tasks = delegationBean.getTasks(delegatableTaskIndex, dTaskType);
        List<Integer> visibleTasks = filterTasks(dTaskType, tasks);

        Application application = context.getApplication();
        String listId = context.getViewRoot().createUniqueId();

        final HtmlPanelGroup result = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        result.setId("task-panel-" + listId);
        putAttribute(result, "styleClass", "delegationWrapper");

        addChildren(item, result);

        final HtmlPanelGrid taskGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        taskGrid.setId("task-grid-" + listId);
        boolean isOrderAssignmentOrAssignment = dTaskType.isOrderAssignmentOrAssignmentWorkflow();
        taskGrid.setColumns(isOrderAssignmentOrAssignment ? 4 : 3);
        final String customStyleClass = StringUtils.trimToEmpty(getCustomAttributes().get("styleClass"));
        taskGrid.setStyleClass("recipient tasks" + " " + customStyleClass);

        if (!visibleTasks.isEmpty()) {
            if (!isOrderAssignmentOrAssignment) {
                UIOutput resoLable = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                resoLable.setValue(StringUtils.uncapitalize(MessageUtil.getMessage("task_property_resolution")));
                putAttribute(resoLable, "styleClass", "bold");
                ValueBinding wfVB = createWorkflowPropValueBinding(dTaskType, delegatableTaskIndex, WorkflowSpecificModel.Props.RESOLUTION, application);
                UIComponent resolutionInput = createResolutionInput(context, listId, null, wfVB);
                putAttribute(resolutionInput, "styleClass", "delegationReso expand19-200");
                addChildren(result, resoLable, resolutionInput);
            }

            final List<UIComponent> resultChildren = addChildren(result, taskGrid);
            HtmlPanelGroup pickerPanel = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            pickerPanel.setId("task-picker-panel-" + listId);
            pickerPanel.setRendererType(TaskListPickerRenderer.class.getCanonicalName());
            resultChildren.add(pickerPanel);

            UIGenericPicker picker = createOwnerPickerComponent(application, listId, dTaskType, delegatableTaskIndex);
            addChildren(pickerPanel, picker);

            // This disables doing AJAX submit when picker finish button is pressed
            // Currently, picker finish reconstructs entire panelgroup, which is some levels above propertysheet
            // If AJAX submit is desired, something needs to be reworked
            putAttribute(pickerPanel, Search.AJAX_PARENT_LEVEL_KEY, Integer.valueOf(100));

            final List<UIComponent> taskGridChildren = addChildren(taskGrid, createColumnHeading("workflow_task_owner_name", application));
            if (isOrderAssignmentOrAssignment) {
                taskGridChildren.add(createColumnHeading("task_property_resolution", application));
            }
            taskGridChildren.add(createColumnHeading("task_property_due_date", application));
            taskGridChildren.add(createColumnHeading(null, application));

            // create table rows for each task
            for (int counter = 0; counter < tasks.size(); counter++) {
                if (visibleTasks.contains(counter)) {
                    HtmlInputText nameInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                    nameInput.setId("task-name-" + listId + "-" + counter);
                    nameInput.setReadonly(true);
                    putAttribute(nameInput, "styleClass", "ownerName width120");
                    ValueBinding nameValueBinding = createTaskPropValueBinding(dTaskType, delegatableTaskIndex, counter, WorkflowCommonModel.Props.OWNER_NAME,
                            application);
                    nameInput.setValueBinding("value", nameValueBinding);
                    taskGridChildren.add(nameInput);

                    if (isOrderAssignmentOrAssignment) {
                        ValueBinding resolutionVB = createTaskPropValueBinding(dTaskType, delegatableTaskIndex, counter,
                                WorkflowSpecificModel.Props.RESOLUTION,
                                application);
                        taskGridChildren.add(createResolutionInput(context, listId, counter, resolutionVB));
                    }

                    final HtmlInputText dueDateInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                    taskGridChildren.add(dueDateInput);
                    dueDateInput.setId("task-duedate-" + listId + "-" + counter);
                    dueDateInput.setValueBinding("value"
                            , createTaskPropValueBinding(dTaskType, delegatableTaskIndex, counter, WorkflowSpecificModel.Props.DUE_DATE, application));
                    ComponentUtil.createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, dueDateInput);
                    Map<String, Object> dueDateAttributes = ComponentUtil.putAttribute(dueDateInput, "styleClass", "margin-left-4 date");
                    if (DelegatableTaskType.ASSIGNMENT_RESPONSIBLE.equals(dTaskType) || DelegatableTaskType.ORDER_ASSIGNMENT_RESPONSIBLE.equals(dTaskType)) { // add client side
                                                                                                                                                              // validation
                        List<String> params = new ArrayList<String>(2);
                        params.add("document.getElementById('" + dueDateInput.getClientId(context) + "')");
                        String invalidMsg = MessageUtil.getMessage(context, "validation_date_failed", MessageUtil.getMessage("task_property_due_date"));
                        addStringConstraintParam(params, invalidMsg);
                        propertySheet.addClientValidation(new ClientValidation("validateMandatory", params, true));
                        dueDateAttributes.put("onchange", "processButtonState();");
                    }

                    final HtmlPanelGroup columnActions = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
                    columnActions.setId("column-actions-" + listId + "-" + counter);
                    taskGridChildren.add(columnActions);

                    final List<UIComponent> actionChildren = addChildren(columnActions);
                    UIActionLink taskSearchLink = createOwnerSearchLink(context, application, listId, picker, counter);
                    actionChildren.add(taskSearchLink);
                    { // taskDeleteLink // taskResetLink
                        final UIActionLink taskDeleteLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                        taskDeleteLink.setValue("");
                        taskDeleteLink.setShowLink(false);
                        taskDeleteLink.setId("task-remove-link-" + listId + "-" + counter);
                        if (dTaskType.equals(DelegatableTaskType.ASSIGNMENT_RESPONSIBLE)) {
                            taskDeleteLink.setTooltip(MessageUtil.getMessage("clear_fields"));
                            taskDeleteLink.setActionListener(application.createMethodBinding("#{DelegationBean.resetDelegationTask}",
                                    UIActions.ACTION_CLASS_ARGS));
                            putAttribute(taskDeleteLink, "styleClass", "icon-link margin-left-4 reset");
                        } else {
                            taskDeleteLink.setTooltip(MessageUtil.getMessage("delete"));
                            taskDeleteLink.setActionListener(application.createMethodBinding("#{DelegationBean.removeDelegationTask}",
                                    UIActions.ACTION_CLASS_ARGS));
                            putAttribute(taskDeleteLink, "styleClass", "icon-link margin-left-4 delete");
                        }
                        addChildren(taskDeleteLink
                                , createUIParam(ATTRIB_DELEGATE_TASK_TYPE, dTaskType, application)
                                , createUIParam(DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, delegatableTaskIndex, application)
                                , createTaskIndexParam(counter, application)//
                        );
                        actionChildren.add(taskDeleteLink);
                    }
                    if (!dTaskType.equals(DelegatableTaskType.ASSIGNMENT_RESPONSIBLE)) {
                        createAddTaskLink(application, listId, delegatableTaskIndex, dTaskType, counter, columnActions, counter + 1, false);
                    }
                }
            }
        } else {
            if (!dTaskType.equals(DelegatableTaskType.ASSIGNMENT_RESPONSIBLE)) {
                createAddTaskLink(application, listId, delegatableTaskIndex, dTaskType, 0, result, 0, true);
            }
        }
        return result;
    }

    private UIComponent createResolutionInput(FacesContext context, String listId, Integer counter, ValueBinding resolutionVB) {
        UIComponent resolutionInput = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
        resolutionInput.setRendererType(ComponentConstants.JAVAX_FACES_TEXTAREA);
        resolutionInput.setId("task-resolution-" + listId + "-" + counter);
        resolutionInput.setValueBinding("value", resolutionVB);
        putAttribute(resolutionInput, "styleClass", "margin-left-4 medium expand19-200");
        return resolutionInput;
    }

    private UIOutput createColumnHeading(String columnLabel, Application application) {
        UIOutput heading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        putAttribute(heading, "styleClass", "th");
        heading.setValue(columnLabel != null ? MessageUtil.getMessage(columnLabel) : "");
        return heading;
    }

    private MethodBinding createAddTaskMethodBinding(Application application) {
        return application.createMethodBinding("#{DelegationBean.addDelegationTask}", UIActions.ACTION_CLASS_ARGS);
    }

    private void createAddTaskLink(Application application, String listId, int delegatableTaskIndex, DelegatableTaskType dTaskType, int counter,
            HtmlPanelGroup parentComponent,
            int taskIndexValue, boolean setValue) {
        UIActionLink taskAddLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        taskAddLink.setId("task-add-link-" + listId + "-" + counter);
        String workflowType = dTaskType.getWorkflowTypeQName().getLocalName();
        String addUserText = MessageUtil.getMessage("workflow_compound_add_" + workflowType + "_user"
                + (dTaskType.equals(DelegatableTaskType.ORDER_ASSIGNMENT_NOT_RESPONSIBLE) ? "_co" : ""));
        taskAddLink.setValue(setValue ? addUserText : "");
        taskAddLink.setTooltip(addUserText);
        taskAddLink.setActionListener(createAddTaskMethodBinding(application));
        taskAddLink.setShowLink(false);
        ComponentUtil.putAttribute(taskAddLink, "styleClass", "icon-link add-person");
        addChildren(taskAddLink
                , createUIParam(DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, delegatableTaskIndex, application)
                , createTaskIndexParam(taskIndexValue, application)
                , createUIParam(ATTRIB_DELEGATE_TASK_TYPE, dTaskType, application));
        addChildren(parentComponent, taskAddLink);
    }

    private List<Integer> filterTasks(DelegatableTaskType taskType, List<Task> tasks) {
        List<Integer> result = new ArrayList<Integer>();
        int index = 0;
        for (Task task : tasks) {
            if (taskType.getTaskTypeQName().equals(task.getNode().getType()) && WorkflowUtil.isGeneratedByDelegation(task)) {
                if (!taskType.isOrderAssignmentOrAssignmentWorkflow()) {
                    result.add(index);
                } else {
                    if (taskType.isResponsibleTask() == task.isResponsible()) {
                        result.add(index);
                    }
                }
            }
            index++;
        }
        return result;
    }

    private UIGenericPicker createOwnerPickerComponent(Application application, String listId, DelegatableTaskType dTaskType, int delegatableTaskIndex) {
        UIGenericPicker picker = (UIGenericPicker) application.createComponent("org.alfresco.faces.GenericPicker");
        picker.setId("task-picker-" + listId);
        picker.setShowFilter(false);
        picker.setMultiSelect(false);
        picker.setShowFilter(true);
        picker.setWidth(400);
        setPickerBindings(picker, dTaskType, application);
        ComponentUtil.putAttribute(picker, ATTRIB_DELEGATE_TASK_TYPE, dTaskType);
        ComponentUtil.putAttribute(picker, DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, delegatableTaskIndex);
        return picker;
    }

    private static void setPickerBindings(UIGenericPicker picker, DelegatableTaskType dTaskType, Application application) {
        String getOwnerSearchFiltersB; // from what groups owners can be searched
        String executeSearchCallbackB; // search from selected group
        String selectedSearchProcessingB; // processes selected results
        if (DelegatableTaskType.ASSIGNMENT_RESPONSIBLE.equals(dTaskType)) {
            getOwnerSearchFiltersB = "#{OwnerSearchBean.responsibleOwnerSearchFilters}";
            executeSearchCallbackB = "#{CompoundWorkflowDefinitionDialog.executeResponsibleOwnerSearch}";
            selectedSearchProcessingB = "#{DelegationBean.processResponsibleOwnerSearchResults}";
        } else {
            getOwnerSearchFiltersB = "#{OwnerSearchBean.ownerSearchFilters}";
            executeSearchCallbackB = "#{CompoundWorkflowDefinitionDialog.executeTaskOwnerSearch}";
            selectedSearchProcessingB = "#{DelegationBean.processOwnerSearchResults}";
        }
        picker.setValueBinding("filters", application.createValueBinding(getOwnerSearchFiltersB));
        picker.setQueryCallback(application.createMethodBinding(executeSearchCallbackB, GenericPickerTag.QUERYCALLBACK_CLASS_ARGS));
        picker.setActionListener(application.createMethodBinding(selectedSearchProcessingB, UIActions.ACTION_CLASS_ARGS));
    }

    private ValueBinding createTaskPropValueBinding(DelegatableTaskType dTaskType
            , int delegatableTaskIndex, int taskIndex, QName propName, Application application) {
        String tasksListVB;
        if (dTaskType.isOrderAssignmentOrAssignmentWorkflow()) {
            tasksListVB = "#{DelegationBean.delegatableTasks[" + delegatableTaskIndex + "].parent.tasks";
        } else {
            tasksListVB = "#{DelegationBean.newWorkflowTasksFetchers[" + delegatableTaskIndex + "].nonAssignmentTasksByType[\"" + dTaskType.name() + "\"]";
        }
        return application.createValueBinding(tasksListVB + "[" + taskIndex + "].node.properties[\"" + propName + "\"]}");
    }

    private ValueBinding createWorkflowPropValueBinding(DelegatableTaskType dTaskType, int delegatableTaskIndex, QName propName, Application application) {
        String workflowVB;
        if (!dTaskType.isOrderAssignmentOrAssignmentWorkflow()) {
            workflowVB = "#{DelegationBean.newWorkflowTasksFetchers[" + delegatableTaskIndex + "].nonAssignmentWorkflowsByType[\"" + dTaskType.name()
                    + "\"]";
        } else {
            // if isAssignmentWorkflow then equivalent tasksListVB is probably "#{DelegationBean.delegatableTasks[" + delegatableTaskIndex + "].parent"
            throw new RuntimeException("Unimplemented workflow property binding for " + dTaskType);
        }
        return application.createValueBinding(workflowVB + ".node.properties[\"" + propName + "\"]}");
    }
}
