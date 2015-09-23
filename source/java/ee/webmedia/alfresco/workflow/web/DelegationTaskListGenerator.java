package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDelegationBean;
import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.addOnChangeJavascript;
import static ee.webmedia.alfresco.utils.ComponentUtil.addOnchangeClickLink;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;
import static ee.webmedia.alfresco.utils.ComponentUtil.getChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getActionId;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getDialogId;
import static org.alfresco.web.app.servlet.FacesHelper.makeLegalId;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.ActionEvent;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.tag.GenericPickerTag;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.workflow.DelegationTaskListContainer;
import ee.webmedia.alfresco.common.propertysheet.workflow.TaskListContainer;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Depending on "taskType" attribute it generates taskList for delegating assignment task as
 * new ASSIGNMENT_RESPONSIBLE, ASSIGNMENT_NOT_RESPONSIBLE, INFORMATION or OPINION task
 */
public class DelegationTaskListGenerator extends TaskListGenerator {
    /**
     * Values for the "show-property" element attribute "taskType"
     */
    public enum DelegatableTaskType {
        ASSIGNMENT_RESPONSIBLE(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.ASSIGNMENT_TASK)
        , ASSIGNMENT_NOT_RESPONSIBLE(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.ASSIGNMENT_TASK)
        , ORDER_ASSIGNMENT_RESPONSIBLE(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK)
        , ORDER_ASSIGNMENT_NOT_RESPONSIBLE(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK)
        , INFORMATION(WorkflowSpecificModel.Types.INFORMATION_WORKFLOW, WorkflowSpecificModel.Types.INFORMATION_TASK)
        , OPINION(WorkflowSpecificModel.Types.OPINION_WORKFLOW, WorkflowSpecificModel.Types.OPINION_TASK)
        , REVIEW(WorkflowSpecificModel.Types.REVIEW_WORKFLOW, WorkflowSpecificModel.Types.REVIEW_TASK);

        private final QName workflowTypeQName;
        private final QName taskTypeQName;

        DelegatableTaskType(QName workflowTypeQName, QName taskTypeQName) {
            this.workflowTypeQName = workflowTypeQName;
            this.taskTypeQName = taskTypeQName;
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

        public QName getTaskTypeQName() {
            return taskTypeQName;
        }

        public static DelegatableTaskType getTypeByTask(Task task) {
            if (task.isType(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
                return INFORMATION;
            } else if (task.isType(WorkflowSpecificModel.Types.OPINION_TASK)) {
                return OPINION;
            } else if (task.isType(WorkflowSpecificModel.Types.REVIEW_TASK)) {
                return REVIEW;
            } else if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
                if (task.isResponsible()) {
                    return ASSIGNMENT_RESPONSIBLE;
                }
                return ASSIGNMENT_NOT_RESPONSIBLE;
            } else if (task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK)) {
                if (task.isResponsible()) {
                    return ORDER_ASSIGNMENT_RESPONSIBLE;
                }
                return ORDER_ASSIGNMENT_NOT_RESPONSIBLE;
            } else {
                throw new RuntimeException("No DelegatableTaskType defined for task type " + task.getType());
            }
        }

    }

    public static final String ATTRIB_DELEGATE_TASK_TYPE = "delegateTaskType";
    public static final String ATTRIB_GROUP_DATE_PICKER_ID = "groupDatepickerId";

    private static final String ATTR_DISPLAY_RESOLUTION_FIELD = "displayResolution";
    private static final String DELETE_TOOLTIP = MessageUtil.getMessage("delete");

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, final PropertySheetItem item) {
        int delegatableTaskIndex = (Integer) propertySheet.getAttributes().get(DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX);
        DelegatableTaskType dTaskType = DelegatableTaskType.valueOf(getCustomAttributes().get("taskType"));
        boolean isSingleTaskTypeDelegation = BooleanUtils.toBoolean(getCustomAttributes().get("delegate-single-task-type"));

        TaskListInfo listInfo = new TaskListInfo(context);
        listInfo.responsible = false;
        listInfo.fullAccess = true;

        DelegationBean delegationBean = getDelegationBean();
        List<Task> tasks = delegationBean.getTasks(delegatableTaskIndex, dTaskType, isSingleTaskTypeDelegation);
        listInfo.workflowIndex = tasks.isEmpty() ? 0 : tasks.get(0).getWorkflowIndex();
        Set<TaskListRow> visibleTasks = filterTasks(dTaskType, tasks, listInfo, delegationBean.getTaskGroups());

        DelegationTaskListContainer taskListContainer = createTaskListContainer(propertySheet, listInfo, context, item);
        addChildren(item, taskListContainer);

        boolean displayResolutionField = dTaskType.isOrderAssignmentOrAssignmentWorkflow() || isSingleTaskTypeDelegation;
        listInfo.getExtraAttributes().put(ATTR_DISPLAY_RESOLUTION_FIELD, displayResolutionField);

        GenerationContext genContext = new GenerationContext(context, listInfo, tasks, visibleTasks, taskListContainer,
                displayResolutionField, isSingleTaskTypeDelegation, dTaskType, delegatableTaskIndex);

        if (!visibleTasks.isEmpty()) {
            genContext.taskGrid = createTaskGrid(genContext.application, listInfo);
            genContext.taskGroups = delegationBean.getTaskGroups();

            if (!displayResolutionField) {
                createSingleResolutionRow(genContext, taskListContainer);
            }
            createUserPicker(genContext, taskListContainer);
            createTaskListHeadings(genContext);
            int rows = createTaskListRows(genContext);
            createTaskListFooter(context, item, listInfo, genContext.taskGrid);
            ComponentUtil.setAjaxEnabledOnActionLinksRecursive(taskListContainer, 1);
            taskListContainer.setRowCount(rows);
        } else {
            if (!dTaskType.equals(DelegatableTaskType.ASSIGNMENT_RESPONSIBLE)) {
                UIActionLink taskAddLink = createAddTaskLink(genContext, 0, 0, true, false);
                addChildren(taskListContainer, taskAddLink);
            }
        }
        return taskListContainer;
    }

    @Override
    protected String getPageSizeDropDownChangeBinding() {
        return "#{DelegationBean.updateTaskListPageSize}";
    }

    private void createSingleResolutionRow(GenerationContext genContext, TaskListContainer taskListContainer) {
        Application app = genContext.application;
        UIOutput resoLable = (UIOutput) app.createComponent(UIOutput.COMPONENT_TYPE);
        resoLable.setValue(StringUtils.uncapitalize(MessageUtil.getMessage("task_property_resolution")));
        putAttribute(resoLable, "styleClass", "bold");
        ValueBinding wfVB = createWorkflowPropValueBinding(genContext.dTaskType, genContext.delegatableTaskIndex, WorkflowSpecificModel.Props.RESOLUTION, app);
        UIComponent resolutionInput = createResolutionInput(app, genContext.listInfo.listId, null, wfVB);
        putAttribute(resolutionInput, "styleClass", "delegationReso expand19-200");
        addChildren(taskListContainer, resoLable, resolutionInput);
    }

    private void createUserPicker(GenerationContext genContext, TaskListContainer taskListContainer) {
        List<UIComponent> resultChildren = addChildren(taskListContainer, genContext.taskGrid);
        HtmlPanelGroup pickerPanel = (HtmlPanelGroup) genContext.application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        pickerPanel.setId("task-picker-panel-" + genContext.listInfo.listId);
        pickerPanel.setRendererType(TaskListPickerRenderer.class.getCanonicalName());
        resultChildren.add(pickerPanel);

        UIGenericPicker picker = createOwnerPickerComponent(genContext);
        addChildren(pickerPanel, picker);
        genContext.picker = picker;
        putAttribute(pickerPanel, Search.AJAX_PARENT_LEVEL_KEY, Integer.valueOf(100));
    }

    private UIGenericPicker createOwnerPickerComponent(GenerationContext genContext) {
        UIGenericPicker picker = (UIGenericPicker) genContext.application.createComponent("org.alfresco.faces.GenericPicker");
        picker.setId("task-picker-" + genContext.listInfo.listId);
        picker.setShowFilter(false);
        picker.setMultiSelect(false);
        picker.setShowFilter(true);
        picker.setShowSelectButton(true);
        picker.setFilterByTaskOwnerStructUnit(true);
        picker.setWidth(400);
        setPickerBindings(picker, genContext.dTaskType, genContext.application);
        ComponentUtil.putAttribute(picker, ATTRIB_DELEGATE_TASK_TYPE, genContext.dTaskType);
        ComponentUtil.putAttribute(picker, DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, genContext.delegatableTaskIndex);
        ComponentUtil.putAttribute(picker, DelegationBean.ATTRIB_SINGLE_TASK_TYPE_DELEGATION, genContext.isSingleTaskTypeDelegation);
        return picker;
    }

    private void createTaskListHeadings(GenerationContext genContext) {
        Application app = genContext.application;
        final List<UIComponent> taskGridChildren = addChildren(genContext.taskGrid, createColumnHeading("workflow_task_owner_name", app));
        if (genContext.displayResolutionField) {
            taskGridChildren.add(createColumnHeading("task_property_resolution", app));
        }
        taskGridChildren.add(createColumnHeading("task_property_due_date", app));
        taskGridChildren.add(createColumnHeading(null, app));
    }

    private int createTaskListRows(GenerationContext genContext) {
        @SuppressWarnings("unchecked")
        List<UIComponent> taskGridChildren = genContext.taskGrid.getChildren();

        FacesContext context = genContext.context;
        genContext.pickerActionId = getActionId(context, genContext.picker);
        genContext.pickerModalOnclikcJsCall = "return showModal('" + getDialogId(context, genContext.picker) + "');";

        int rowNumber = 0;
        for (TaskListRow row : genContext.visibleTasks) {

            TaskListContainer taskListContainer = genContext.taskListContainer;
            if (row.isSingleTaskRow()) {
                if (taskListContainer.isRowOnCurrentPage(rowNumber)) {
                    if (row.partOfGroup) { // other group member have been deleted by user
                        TaskGroup group = getTaskGroup(genContext.listInfo, genContext.tasks.get(row.getTaskIndex()), row.getTaskIndex(), genContext.taskGroups);
                        reorderGroupTaskIds(group, row.getTaskIndex(), Collections.<Integer> emptyList());
                    }
                    createTaskRow(genContext, taskGridChildren, row.getTaskIndex());
                }
                rowNumber++;
                continue;
            }

            TaskGroup taskGroup = getTaskGroup(genContext, rowNumber, row);

            if (!row.isGroupExpanded()) {
                if (taskListContainer.isRowOnCurrentPage(rowNumber)) {
                    generateGroupRowComponents(genContext, taskGroup, row.getTaskIndex());
                }
                rowNumber++;
                continue;
            }

            // Expanded group row
            if (taskListContainer.isRowOnCurrentPage(rowNumber)) {
                generateGroupRowComponents(genContext, taskGroup, row.getTaskIndex());
            }
            rowNumber++;

            // Primary task in group
            if (taskListContainer.isRowOnCurrentPage(rowNumber)) {
                createTaskRow(genContext, taskGridChildren, row.getTaskIndex());
            }
            rowNumber++;

            if (taskListContainer.isRowRangeOnCurrentPage(rowNumber, (rowNumber + row.getGroupedTasksCount()))) {
                int startIndex = getTaskGroupStartIndex(taskListContainer, rowNumber);
                List<Integer> groupedTaskIndices = row.getGroupedTaskIndices();
                for (int i = startIndex, actualRow = rowNumber + startIndex; (i < row.getGroupedTasksCount() && taskListContainer.isRowOnCurrentPage(actualRow)); i++, actualRow++) {
                    createTaskRow(genContext, taskGridChildren, groupedTaskIndices.get(i));
                }
            }
            rowNumber += row.getGroupedTasksCount();

        }

        return rowNumber;
    }

    @Override
    public void toggleGroup(ActionEvent event) {
        String groupName = ActionUtil.getParam(event, "groupName");
        String groupId = ActionUtil.getParam(event, "groupId");
        Integer wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);
        TaskGroup group = getDelegationBean().getTaskGroups().getTaskGroup(wfIndex, groupName, groupId);
        if (group == null) {
            return;
        }
        group.setExpanded(!group.isExpanded());
        getDelegationBean().updatePanelGroup();
    }

    private TaskGroup getTaskGroup(GenerationContext genContext, int rowNumber, TaskListRow row) {
        return genContext.taskListContainer.isRowOnCurrentPage(rowNumber)
                ? createGroupRow(genContext.listInfo, genContext.tasks.get(row.getTaskIndex()), row.getTaskIndex(), row.getGroupedTaskIndices(), genContext.taskGroups) : null;
    }

    private void createTaskRow(GenerationContext genContext, List<UIComponent> taskGridChildren, int taskIndex) {
        createInputFields(genContext, taskGridChildren, taskIndex);
        HtmlPanelGroup actions = createSearchAndDeleteActions(genContext, taskIndex);
        taskGridChildren.add(actions);
        if (!genContext.dTaskType.equals(DelegatableTaskType.ASSIGNMENT_RESPONSIBLE)) {
            UIActionLink taskAddLink = createAddTaskLink(genContext, taskIndex, taskIndex + 1, false, false);
            addChildren(actions, taskAddLink);
        }
    }

    private void createInputFields(GenerationContext genContext, List<UIComponent> taskGridChildren, Integer taskIndex) {
        Application application = genContext.application;

        DelegatableTaskType dTaskType = genContext.dTaskType;
        boolean isSingleTaskTypeDelegation = genContext.isSingleTaskTypeDelegation;
        int delegatableTaskIndex = genContext.delegatableTaskIndex;

        HtmlInputText nameInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
        nameInput.setId("task-name-" + genContext.listInfo.listId + "-" + taskIndex);
        nameInput.setReadonly(true);
        putAttribute(nameInput, "styleClass", "ownerName width120");
        ValueBinding nameValueBinding = createTaskPropValueBinding(dTaskType, delegatableTaskIndex, taskIndex, WorkflowCommonModel.Props.OWNER_NAME,
                application, isSingleTaskTypeDelegation);
        nameInput.setValueBinding("value", nameValueBinding);
        taskGridChildren.add(nameInput);

        if (genContext.displayResolutionField) {
            ValueBinding resolutionVB = createTaskPropValueBinding(dTaskType, delegatableTaskIndex, taskIndex,
                    WorkflowSpecificModel.Props.RESOLUTION, application, isSingleTaskTypeDelegation);
            taskGridChildren.add(createResolutionInput(application, genContext.listInfo.listId, taskIndex, resolutionVB));
        }

        final HtmlInputText dueDateInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
        taskGridChildren.add(dueDateInput);
        dueDateInput.setId("task-duedate-" + genContext.listInfo.listId + "-" + taskIndex);
        dueDateInput.setValueBinding("value"
                , createTaskPropValueBinding(dTaskType, delegatableTaskIndex, taskIndex, WorkflowSpecificModel.Props.DUE_DATE, application, isSingleTaskTypeDelegation));
        ComponentUtil.createAndSetConverter(genContext.context, DatePickerConverter.CONVERTER_ID, dueDateInput);
        ComponentUtil.putAttribute(dueDateInput, "styleClass", "margin-left-4 date");
    }

    private HtmlPanelGroup createSearchAndDeleteActions(GenerationContext genContext, Integer taskIndex) {
        TaskListInfo listInfo = genContext.listInfo;
        Application application = genContext.application;
        DelegatableTaskType dTaskType = genContext.dTaskType;

        final HtmlPanelGroup columnActions = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        columnActions.setId("column-actions-" + listInfo.listId + "-" + taskIndex);

        final List<UIComponent> actionChildren = addChildren(columnActions);
        UIActionLink taskSearchLink = createOwnerSearchLink(genContext.context, genContext.listInfo.listId, genContext.picker, taskIndex,
                genContext.pickerActionId, genContext.pickerModalOnclikcJsCall);
        actionChildren.add(taskSearchLink);

        final UIActionLink taskDeleteLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        taskDeleteLink.setValue("");
        taskDeleteLink.setShowLink(false);
        taskDeleteLink.setId("task-remove-link-" + listInfo.listId + "-" + taskIndex);
        if (dTaskType.equals(DelegatableTaskType.ASSIGNMENT_RESPONSIBLE)) {
            taskDeleteLink.setTooltip(MessageUtil.getMessage("clear_fields"));
            taskDeleteLink.setActionListener(application.createMethodBinding("#{DelegationBean.resetDelegationTask}", UIActions.ACTION_CLASS_ARGS));
            putAttribute(taskDeleteLink, "styleClass", "icon-link margin-left-4 reset");
        } else {
            taskDeleteLink.setTooltip(DELETE_TOOLTIP);
            taskDeleteLink.setActionListener(application.createMethodBinding("#{DelegationBean.removeDelegationTask}", UIActions.ACTION_CLASS_ARGS));
            putAttribute(taskDeleteLink, "styleClass", "icon-link margin-left-4 delete");
        }
        addChildren(taskDeleteLink
                , createUIParam(ATTRIB_DELEGATE_TASK_TYPE, dTaskType, application)
                , createUIParam(DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, genContext.delegatableTaskIndex, application)
                , createTaskIndexParam(taskIndex, application)
                , createUIParam(DelegationBean.ATTRIB_SINGLE_TASK_TYPE_DELEGATION, genContext.isSingleTaskTypeDelegation, application));
        actionChildren.add(taskDeleteLink);
        return columnActions;
    }

    private UIActionLink createAddTaskLink(GenerationContext genContext, int rowNumber, int taskIndexValue, boolean setValue, boolean toGroup) {
        Application app = genContext.application;
        UIActionLink taskAddLink = (UIActionLink) app.createComponent("org.alfresco.faces.ActionLink");
        taskAddLink.setId("task-add-link-" + (toGroup ? "" : "group-") + genContext.listInfo.listId + "-" + rowNumber);
        String workflowType = genContext.dTaskType.getWorkflowTypeQName().getLocalName();
        String addUserText = MessageUtil.getMessage("workflow_compound_add_" + workflowType + "_user"
                + (genContext.dTaskType.equals(DelegatableTaskType.ORDER_ASSIGNMENT_NOT_RESPONSIBLE) ? "_co" : ""));
        taskAddLink.setValue(setValue ? addUserText : "");
        taskAddLink.setTooltip(addUserText);
        taskAddLink.setActionListener(app.createMethodBinding("#{DelegationBean.addDelegationTask}", UIActions.ACTION_CLASS_ARGS));
        taskAddLink.setShowLink(false);
        ComponentUtil.putAttribute(taskAddLink, "styleClass", "icon-link add-person");
        addChildren(taskAddLink
                , createUIParam(DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, genContext.delegatableTaskIndex, app)
                , createTaskIndexParam(taskIndexValue, app)
                , createUIParam(DelegationBean.ATTRIB_WF_INDEX, genContext.listInfo.workflowIndex, app)
                , createUIParam(ATTRIB_DELEGATE_TASK_TYPE, genContext.dTaskType, app)
                , createUIParam(DelegationBean.ATTRIB_SINGLE_TASK_TYPE_DELEGATION, genContext.isSingleTaskTypeDelegation, app));
        return taskAddLink;
    }

    private void generateGroupRowComponents(GenerationContext genContext, TaskGroup group, int rowIndex) {
        String taskGroupBinding = "#{" + DelegationBean.BEAN_NAME + ".taskGroups.byGroupId['" + group.getGroupId() + "']";
        Application app = genContext.application;
        TaskListInfo list = genContext.listInfo;
        String rowId = makeLegalId(group.getGroupId());

        HtmlPanelGroup iconAndName = (HtmlPanelGroup) app.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        UIComponent groupNameLabel = app.createComponent(UIOutput.COMPONENT_TYPE);
        groupNameLabel.setValueBinding("value", app.createValueBinding(taskGroupBinding + ".groupName}"));
        addChildren(iconAndName, createToggleGroupActionLink(app, group, list.workflowIndex, rowId, "DelegationTaskListGenerator"), groupNameLabel);
        addChildren(genContext.taskGrid, iconAndName);

        if (genContext.displayResolutionField) {
            addChildren(genContext.taskGrid, ComponentUtil.createSpacer(app));
        }
        String dueDateVbStr = taskGroupBinding + ".dueDate}";
        addChildren(genContext.taskGrid, createTaskGroupDatePicker(genContext, group, rowId, dueDateVbStr));

        HtmlPanelGroup iconsPanel = (HtmlPanelGroup) app.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        addChildren(iconsPanel, createDeleteGroupLink(genContext, group, rowIndex));
        int maxIndex = genContext.listInfo.maximumVisibleTaskIndex + 1;
        addChildren(iconsPanel, createAddTaskLink(genContext, rowIndex, maxIndex, false, true));
        addChildren(genContext.taskGrid, iconsPanel);
    }

    private HtmlPanelGroup createTaskGroupDatePicker(GenerationContext genContext, TaskGroup group, String rowId, String dueDateVbString) {
        Application app = genContext.application;
        HtmlInputText dueDateInput = (HtmlInputText) app.createComponent(HtmlInputText.COMPONENT_TYPE);
        dueDateInput.setId("task-dueDateDaysAndTime-input" + group.getGroupId());
        ComponentUtil.createAndSetConverter(genContext.context, DatePickerConverter.CONVERTER_ID, dueDateInput);
        ComponentUtil.putAttribute(dueDateInput, "styleClass", "margin-left-4 date");
        dueDateInput.setValueBinding("value", app.createValueBinding(dueDateVbString));
        addOnChangeJavascript(dueDateInput, "processTaskDueDateDateInput(jQuery('#' + escapeId4JQ(currElId)));");

        HtmlPanelGroup dueDatePanel = createHtmlPanelGroupWithId(app, "task-dueDateDaysAndTime-panel" + group.getGroupId());
        addChildren(dueDatePanel, dueDateInput);

        addOnchangeClickLink(app, getChildren(dueDatePanel), "#{DelegationBean.calculateTaskGroupDueDate}", "task-dueDateDaysAndTime-onclick" + rowId,
                createUIParam(ATTRIB_GROUP_DATE_PICKER_ID, dueDateInput.getClientId(genContext.context), app),
                createUIParam(ATTRIB_DELEGATE_TASK_TYPE, genContext.dTaskType, app),
                createUIParam(DelegationBean.ATTRIB_GROUP_ID, group.getGroupId(), app),
                createUIParam(DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, genContext.delegatableTaskIndex, app),
                createUIParam(DelegationBean.ATTRIB_WF_INDEX, genContext.listInfo.workflowIndex, app),
                createUIParam(DelegationBean.ATTRIB_SINGLE_TASK_TYPE_DELEGATION, genContext.isSingleTaskTypeDelegation, app));

        return dueDatePanel;
    }

    private UIActionLink createDeleteGroupLink(GenerationContext genContext, TaskGroup group, int rowNumber) {
        Application app = genContext.application;
        final UIActionLink taskGroupLink = (UIActionLink) app.createComponent("org.alfresco.faces.ActionLink");
        taskGroupLink.setValue("");
        taskGroupLink.setShowLink(false);
        taskGroupLink.setId("group-remove-link-" + genContext.listInfo.listId + "-" + rowNumber);

        taskGroupLink.setTooltip(DELETE_TOOLTIP);
        taskGroupLink.setActionListener(app.createMethodBinding("#{DelegationBean.removeDelegationGroup}",
                UIActions.ACTION_CLASS_ARGS));
        putAttribute(taskGroupLink, "styleClass", "icon-link margin-left-4 delete");

        addChildren(taskGroupLink
                , createUIParam(ATTRIB_DELEGATE_TASK_TYPE, genContext.dTaskType, app)
                , createUIParam(DelegationBean.ATTRIB_GROUP_ID, group.getGroupId(), app)
                , createUIParam(DelegationBean.ATTRIB_WF_INDEX, genContext.listInfo.workflowIndex, app)
                , createUIParam(DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, genContext.delegatableTaskIndex, app)
                , createTaskIndexParam(rowNumber, app)
                , createUIParam(DelegationBean.ATTRIB_SINGLE_TASK_TYPE_DELEGATION, genContext.isSingleTaskTypeDelegation, app));

        return taskGroupLink;
    }

    @Override
    protected HtmlPanelGrid createTaskGrid(Application application, TaskListInfo list) {
        HtmlPanelGrid taskGrid = super.createTaskGrid(application, list);
        boolean displayResolutionFiled = (boolean) list.getExtraAttributes().get(ATTR_DISPLAY_RESOLUTION_FIELD);
        taskGrid.setColumns(displayResolutionFiled ? 4 : 3);
        return taskGrid;
    }

    @Override
    protected DelegationTaskListContainer createTaskListContainer(UIComponent parent, TaskListInfo list, FacesContext context, PropertySheetItem item) {
        String currentPageAttributeKey = getCurrentPageAttributeKey(context, item);
        DelegationTaskListContainer container = new DelegationTaskListContainer(parent, list.workflowIndex, currentPageAttributeKey);
        container.setId("task-panel-" + list.listId);
        putAttribute(container, "styleClass", "delegationWrapper");
        return container;
    }

    private UIComponent createResolutionInput(Application application, String listId, Integer rowNumber, ValueBinding resolutionVB) {
        UIComponent resolutionInput = application.createComponent(ComponentConstants.JAVAX_FACES_INPUT);
        resolutionInput.setRendererType(ComponentConstants.JAVAX_FACES_TEXTAREA);
        resolutionInput.setId("task-resolution-" + listId + "-" + rowNumber);
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

    private Set<TaskListRow> filterTasks(DelegatableTaskType taskType, List<Task> tasks, TaskListInfo list, TaskGroupHolder taskGroups) {
        Set<TaskListRow> result = new TreeSet<>();
        int visibleTaskCount = 0;
        String previousGroupName = "";
        int maximumVisibleIndex = 0;
        TaskListRow previousGroupRow = null;
        int taskIndex = -1;

        for (Task task : tasks) {
            taskIndex++;
            if (taskType.getTaskTypeQName().equals(task.getNode().getType()) && WorkflowUtil.isGeneratedByDelegation(task)) {

                String ownerGroup = task.getOwnerGroup();
                if (StringUtils.isNotBlank(ownerGroup) && previousGroupName.equals(ownerGroup)) {
                    previousGroupRow.addGroupedTask(taskIndex);
                } else {
                    if (!taskType.isOrderAssignmentOrAssignmentWorkflow() || taskType.isResponsibleTask() == task.isResponsible()) {
                        previousGroupRow = addVisibleTask(result, taskGroups, ownerGroup, list.workflowIndex, taskIndex);
                        boolean hasGroup = StringUtils.isNotBlank(ownerGroup);
                        previousGroupRow.partOfGroup = hasGroup;
                        previousGroupName = hasGroup ? ownerGroup : "";
                    }
                }
                maximumVisibleIndex = visibleTaskCount;
                visibleTaskCount++;
            }
        }
        list.maximumVisibleTaskIndex = maximumVisibleIndex;
        return result;
    }

    private static void setPickerBindings(UIGenericPicker picker, DelegatableTaskType dTaskType, Application application) {
        String getOwnerSearchFiltersB; // from what groups owners can be searched
        String executeSearchCallbackB; // search from selected group
        String selectedSearchProcessingB; // processes selected results
        if (DelegatableTaskType.ASSIGNMENT_RESPONSIBLE.equals(dTaskType)) {
            getOwnerSearchFiltersB = "#{OwnerSearchBean.responsibleOwnerSearchFilters}";
            executeSearchCallbackB = "#{CompoundWorkflowDialog.executeResponsibleOwnerSearch}";
            selectedSearchProcessingB = "#{DelegationBean.processResponsibleOwnerSearchResults}";
        } else {
            getOwnerSearchFiltersB = "#{OwnerSearchBean.ownerSearchFilters}";
            executeSearchCallbackB = "#{CompoundWorkflowDialog.executeTaskOwnerSearch}";
            selectedSearchProcessingB = "#{DelegationBean.processOwnerSearchResults}";
        }
        picker.setValueBinding("filters", application.createValueBinding(getOwnerSearchFiltersB));
        picker.setQueryCallback(application.createMethodBinding(executeSearchCallbackB, GenericPickerTag.QUERYCALLBACK_CLASS_ARGS));
        picker.setActionListener(application.createMethodBinding(selectedSearchProcessingB, UIActions.ACTION_CLASS_ARGS));
    }

    private ValueBinding createTaskPropValueBinding(DelegatableTaskType dTaskType
            , int delegatableTaskIndex, int taskIndex, QName propName, Application application, boolean isSingleTaskTypeDelegation) {
        String tasksListVB;
        if (dTaskType.isOrderAssignmentOrAssignmentWorkflow() || isSingleTaskTypeDelegation) {
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

    private static class GenerationContext {

        private GenerationContext(FacesContext context, TaskListInfo listInfo, List<Task> tasks, Set<TaskListRow> visibleTasks, TaskListContainer taskListContainer,
                boolean displayResolutionField, boolean isSingleTaskTypeDelegation, DelegatableTaskType dTaskType, int delegatableTaskIndex) {
            this.context = context;
            application = context.getApplication();
            this.listInfo = listInfo;
            this.tasks = tasks;
            this.visibleTasks = visibleTasks;
            this.taskListContainer = taskListContainer;
            this.displayResolutionField = displayResolutionField;
            this.isSingleTaskTypeDelegation = isSingleTaskTypeDelegation;
            this.dTaskType = dTaskType;
            this.delegatableTaskIndex = delegatableTaskIndex;
        }

        private final FacesContext context;
        private final Application application;
        private final TaskListInfo listInfo;
        private final List<Task> tasks;
        private final Set<TaskListRow> visibleTasks;
        private final TaskListContainer taskListContainer;
        private final boolean displayResolutionField;
        private final boolean isSingleTaskTypeDelegation;
        private final DelegatableTaskType dTaskType;
        private final int delegatableTaskIndex;

        private TaskGroupHolder taskGroups;
        private HtmlPanelGrid taskGrid;
        private UIGenericPicker picker;
        private String pickerActionId;
        private String pickerModalOnclikcJsCall;
    }
}
