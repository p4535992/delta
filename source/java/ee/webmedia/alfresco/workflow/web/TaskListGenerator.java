package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerWithDueDateGenerator.createDueDateDaysSelector;
import static ee.webmedia.alfresco.common.web.BeanHelper.getBrowseBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getOrganizationStructureService;
import static ee.webmedia.alfresco.utils.ComponentUtil.addAttributes;
import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.addOnChangeJavascript;
import static ee.webmedia.alfresco.utils.ComponentUtil.addOnchangeClickLink;
import static ee.webmedia.alfresco.utils.ComponentUtil.addOnchangeJavascript;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;
import static ee.webmedia.alfresco.utils.ComponentUtil.generateFieldSetter;
import static ee.webmedia.alfresco.utils.ComponentUtil.getAttributes;
import static ee.webmedia.alfresco.utils.ComponentUtil.getChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.TASK_INDEX;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getActionId;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getDialogId;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.isTaskRowEditable;
import static org.alfresco.web.app.servlet.FacesHelper.makeLegalId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.DialogManager;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.common.component.data.UIDataPager;
import org.alfresco.web.ui.common.tag.GenericPickerTag;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ValueBindingsWrapper;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DateTimePicker;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DateTimePickerGenerator;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DateTimePickerRenderer;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ValidatingModalLayerComponent;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.search.SearchRenderer;
import ee.webmedia.alfresco.common.propertysheet.workflow.TaskListContainer;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Task.Action;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * Generator for compound workflow tasks setup block.
 */
public class TaskListGenerator extends BaseComponentGenerator {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(TaskListGenerator.class);

    public static final String ATTR_RESPONSIBLE = "responsible";
    private static final String CALENDAR_DAYS = "calendarDays";
    public static final String WORKING_DAYS = "workingDays";
    /**
     * Workflow index - same meaning as {@link #WF_INDEX}, but used in different places
     */
    public static final String ATTR_WORKFLOW_INDEX = "workflow_index";
    /**
     * Workflow index - same meaning as {@link #ATTR_WORKFLOW_INDEX}, but used in different places
     */
    public static final String WF_INDEX = "index";

    protected DialogManager dialogManager;
    protected WorkflowConstantsBean.TaskListConstants constants = BeanHelper.getWorkflowConstantsBean().getTaskListConstants();

    private static final Map<String, Object> DUE_DATE_TIME_ATTRIBUTES = new HashMap<>();
    private static final Map<String, Object> GROUP_DUE_DATE_TIME_ATTRIBUTES = new HashMap<>();

    static {
        DUE_DATE_TIME_ATTRIBUTES.put("styleClass", "margin-left-4");
        DUE_DATE_TIME_ATTRIBUTES.put(DateTimePickerRenderer.DATE_STYLE_CLASS_ATTR, "task-due-date-date");
        DUE_DATE_TIME_ATTRIBUTES.put(DateTimePickerRenderer.TIME_STYLE_CLASS_ATTR, "task-due-date-time");

        GROUP_DUE_DATE_TIME_ATTRIBUTES.putAll(DUE_DATE_TIME_ATTRIBUTES);
        GROUP_DUE_DATE_TIME_ATTRIBUTES.put("styleClass", (DUE_DATE_TIME_ATTRIBUTES.get("styleClass") + " groupRowDate"));
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        throw new RuntimeException("This is never called!");
    }

    private enum TaskOwnerSearchType {
        TASK_OWNER_SEARCH_RESPONSIBLE,
        TASK_OWNER_SEARCH_EXTERNAL_REVIEW,
        TASK_OWNER_SEARCH_DEFAULT,
        TASK_OWNER_SEARCH_REVIEW,
        TASK_OWNER_SEARCH_DUE_DATE_EXTENSION
    }

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, final PropertySheetItem item) {
        Application application = context.getApplication();
        CompoundWorkflowDefinitionDialog compoundWorkflowDefinitionDialog = (CompoundWorkflowDefinitionDialog) dialogManager.getBean();
        CompoundWorkflow compoundWorkflow = compoundWorkflowDefinitionDialog.getWorkflow();

        TaskListInfo taskListInfo = new TaskListInfo(context);
        Workflow workflow = setupTaskListInfo(taskListInfo, propertySheet, compoundWorkflow);
        List<Task> tasks = workflow.getTasks();

        final TaskListContainer result = createTaskListContainer(propertySheet, taskListInfo, context, item);
        addChildren(item, result);

        final HtmlPanelGrid taskGrid = createTaskGrid(application, taskListInfo);
        final List<UIComponent> resultChildren = addChildren(result, taskGrid);

        Set<TaskListRow> visibleTasks = filterVisibleTasks(compoundWorkflowDefinitionDialog.getTaskGroups(), taskListInfo.workflowIndex, tasks, taskListInfo);

        if (visibleTasks.isEmpty()) {
            return createEmptyTaskList(application, taskListInfo, result);
        }

        UIGenericPicker picker = createAndAddTaskOwnerPicker(context, taskListInfo, resultChildren);
        initAndAddCommentPopupIfNeeded(context, resultChildren, taskListInfo);
        createTaskListHeader(application, workflow, taskGrid);
        int taskRows = createTaskListRows(context, propertySheet, compoundWorkflowDefinitionDialog.getTaskGroups(), compoundWorkflow, taskListInfo, workflow, tasks, taskGrid,
                visibleTasks, picker, result);
        createTaskListFooter(context, item, taskListInfo, taskGrid);
        ComponentUtil.setAjaxEnabledOnActionLinksRecursive(result, 1);

        result.setRowCount(taskRows);
        return result;
    }

    protected void createTaskListFooter(FacesContext context, PropertySheetItem item, TaskListInfo list, HtmlPanelGrid taskGrid) {
        if (list.responsible) {
            return;
        }

        HtmlPanelGroup footerComponents = createHtmlPanelGroupWithId(context.getApplication(), "task-list-footer-" + item.getId() + "-" + list.workflowIndex);
        taskGrid.getFacets().put("footer", footerComponents);

        createPageSizeDropDown(context, footerComponents);
        createTaskListPager(footerComponents, item.getId());
    }

    protected String getCurrentPageAttributeKey(FacesContext context, PropertySheetItem item) {
        String clientId = item.getClientId(context);
        String tempPropNamespace = "prop_x007b_tempx007d_";
        int beginIndex = clientId.indexOf(tempPropNamespace) + tempPropNamespace.length();
        return clientId.substring(beginIndex);
    }

    @SuppressWarnings("unchecked")
    private void createTaskListPager(UIComponent parentComponent, String id) {
        UIDataPager pager = new UIDataPager();
        addChildren(parentComponent, pager);
        pager.getAttributes().put("styleClass", "task-list-pager");
        pager.setId(id + "-pager");
    }

    private void createPageSizeDropDown(FacesContext context, UIComponent parentComponent) {
        UIPanel container = (UIPanel) context.getApplication().createComponent("org.alfresco.faces.Panel");
        container.setId("page-controls");
        putAttribute(container, "styleClass", "page-controls with-pager right static");
        addChildren(parentComponent, container);

        UIOutput label = ComponentUtil.createUnescapedOutputText(context, "contentSize");
        label.setValue(MessageUtil.getMessage(context, "items_per_page"));
        addChildren(container, label);

        HtmlSelectOneMenu dropDownMenu = (HtmlSelectOneMenu) context.getApplication().createComponent(HtmlSelectOneMenu.COMPONENT_TYPE);
        dropDownMenu.setId("page-size-select");
        addChildren(container, dropDownMenu);
        dropDownMenu.setValue(Integer.toString(getBrowseBean().getPageSizeContent()));
        dropDownMenu.setValueChangeListener(context.getApplication().createMethodBinding(getPageSizeDropDownChangeBinding(), new Class[] { ValueChangeEvent.class }));
        dropDownMenu.setOnchange(ComponentUtil.generateAjaxFormSubmit(context, dropDownMenu, null, null, 1));

        for (String pageSize : Arrays.asList("10", "20", "50", "75", "100")) {
            UISelectItem menuItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            menuItem.setItemValue(pageSize);
            menuItem.setItemLabel(pageSize);
            addChildren(dropDownMenu, menuItem);
        }
    }

    protected String getPageSizeDropDownChangeBinding() {
        return "#{DialogManager.bean.updateTaskListPageSize}";
    }

    private UIComponent createEmptyTaskList(Application application, TaskListInfo list, HtmlPanelGroup result) {
        if (list.createAddTaskLink) {
            UIActionLink addTaskLink = createAddTaskLink(application, list, list.listId + "-0", 0);
            addTaskLink.setValue(list.addTaskLinkText);
            addChildren(result, addTaskLink);
        }
        ComponentUtil.setAjaxEnabledOnActionLinksRecursive(result, 1);
        return result;
    }

    private UIGenericPicker createAndAddTaskOwnerPicker(FacesContext context, TaskListInfo list, List<UIComponent> resultChildren) {
        Application application = context.getApplication();
        HtmlPanelGroup pickerPanel = createHtmlPanelGroupWithId(application, "task-picker-panel-" + list.listId);
        pickerPanel.setRendererType(TaskListPickerRenderer.class.getCanonicalName());
        resultChildren.add(pickerPanel);

        UIGenericPicker picker = createOwnerPickerComponent(context, list);
        addChildren(pickerPanel, picker);
        list.taskOwnerPickerActionId = getActionId(context, picker);
        list.taskOwnerPickerModalOnclickJsCall = "return showModal('" + getDialogId(context, picker) + "');";
        putAttribute(pickerPanel, Search.AJAX_PARENT_LEVEL_KEY, Integer.valueOf(1));
        return picker;
    }

    private int createTaskListRows(FacesContext context, UIPropertySheet propertySheet, TaskGroupHolder taskGroupHolder,
            CompoundWorkflow compoundWorkflow, TaskListInfo taskListInfo, Workflow workflow, List<Task> tasks, HtmlPanelGrid taskGrid, Set<TaskListRow> visibleTasks,
            UIGenericPicker picker, TaskListContainer taskListPage) {

        TextAreaGenerator textAreaGenerator = new TextAreaGenerator();
        List<UIComponent> taskGridChildren = taskGrid.getChildren();
        boolean showAddDateLink = workflow.isStatus(Status.NEW) && taskListInfo.compoundWorkflowDialog;
        Map<String, Object> signatureTaskOwnerProps = getSignatureTaskOwnerProps(workflow, taskListInfo);

        int rowNumber = 0;
        for (TaskListRow row : visibleTasks) {

            if (row.isSingleTaskRow()) {
                if (taskListPage.isRowOnCurrentPage(rowNumber)) {
                    if (row.partOfGroup) { // other group member have been deleted by user
                        TaskGroup group = getTaskGroup(taskListInfo, tasks.get(row.getTaskIndex()), row.getTaskIndex(), taskGroupHolder);
                        reorderGroupTaskIds(group, row.getTaskIndex(), Collections.<Integer> emptyList());
                    }
                    showAddDateLink = createTaskRow(context, propertySheet, compoundWorkflow, taskListInfo, workflow, tasks, picker, textAreaGenerator, taskGridChildren,
                            showAddDateLink, signatureTaskOwnerProps, row.getTaskIndex(), null);
                }
                rowNumber++;
                continue;
            }

            TaskGroup taskGroup = taskListPage.isRowOnCurrentPage(rowNumber)
                    ? createGroupRow(taskListInfo, tasks.get(row.getTaskIndex()), row.getTaskIndex(), row.getGroupedTaskIndices(), taskGroupHolder)
                            : null;

                    if (!row.isGroupExpanded()) {
                        if (taskListPage.isRowOnCurrentPage(rowNumber)) {
                            generateGroupRowComponents(context, taskListInfo, workflow.hasTaskResolution(), workflow.isStatus(Status.NEW), taskGroup, taskGrid);
                        }
                        rowNumber++;
                        continue;
                    }

                    // Expanded group row
                    if (taskListPage.isRowOnCurrentPage(rowNumber)) {
                        generateGroupRowComponents(context, taskListInfo, workflow.hasTaskResolution(), workflow.isStatus(Status.NEW), taskGroup, taskGrid);
                    }
                    rowNumber++;

                    // Primary task in group
                    if (taskListPage.isRowOnCurrentPage(rowNumber)) {
                        showAddDateLink = createTaskRow(context, propertySheet, compoundWorkflow, taskListInfo, workflow, tasks, picker, textAreaGenerator, taskGridChildren,
                                showAddDateLink, signatureTaskOwnerProps, row.getTaskIndex(), taskGroup);
                    }
                    rowNumber++;

                    if (taskListPage.isRowRangeOnCurrentPage(rowNumber, (rowNumber + row.getGroupedTasksCount()))) {
                        int startIndex = getTaskGroupStartIndex(taskListPage, rowNumber);
                        List<Integer> groupedTaskIndices = row.getGroupedTaskIndices();
                        for (int i = startIndex, actualRow = rowNumber + startIndex; (i < row.getGroupedTasksCount() && taskListPage.isRowOnCurrentPage(actualRow)); i++, actualRow++) {
                            showAddDateLink = createTaskRow(context, propertySheet, compoundWorkflow, taskListInfo, workflow, tasks, picker, textAreaGenerator, taskGridChildren,
                                    showAddDateLink, signatureTaskOwnerProps, groupedTaskIndices.get(i), taskGroup);
                        }
                    }
                    rowNumber += row.getGroupedTasksCount();

        }

        return rowNumber;
    }

    protected int getTaskGroupStartIndex(TaskListContainer taskListPage, int rowNumber) {
        int startIndex = -1;
        int firstPageRowNumber = taskListPage.getFirstPageRowNumber();
        if (rowNumber < firstPageRowNumber) {
            startIndex = firstPageRowNumber - rowNumber;
        } else if (rowNumber >= firstPageRowNumber) {
            startIndex = 0;
        }
        return startIndex;
    }

    protected TaskGroup getTaskGroup(TaskListInfo list, Task task, Integer taskIndex, TaskGroupHolder taskGroupHolder) {
        String ownerGroup = task.getOwnerGroup();
        return taskGroupHolder.getAdjacentTaskGroup(list.workflowIndex, ownerGroup, taskIndex);
    }

    protected void reorderGroupTaskIds(TaskGroup taskGroup, Integer taskIndex, List<Integer> secondaryGroupTaskIndices) {
        if (taskGroup == null) {
            return;
        }
        Set<Integer> groupTaskIds = taskGroup.getTaskIds();
        groupTaskIds.clear();
        groupTaskIds.add(taskIndex);
        groupTaskIds.addAll(secondaryGroupTaskIndices);
    }

    protected TaskGroup createGroupRow(TaskListInfo list, Task task, Integer taskIndex, List<Integer> secondaryGroupTaskIndices, TaskGroupHolder taskGroupHolder) {
        TaskGroup taskGroup = getTaskGroup(list, task, taskIndex, taskGroupHolder);
        if (taskGroup == null) {
            taskGroup = taskGroupHolder.addNewTaskGroup(list.workflowIndex, taskIndex, task.getOwnerGroup(), list.responsible, list.fullAccess);
            if (task.getDueDate() != null) {
                taskGroup.setDueDate(task.getDueDate());
            }
        }
        reorderGroupTaskIds(taskGroup, taskIndex, secondaryGroupTaskIndices);
        task.setGroupDueDateVbString("#{DialogManager.bean.taskGroups.byGroupId['" + taskGroup.getGroupId() + "'].dueDate}");
        return taskGroup;
    }

    private boolean createTaskRow(FacesContext context, UIPropertySheet propertySheet, CompoundWorkflow compoundWorkflow, TaskListInfo list, Workflow workflow,
            List<Task> tasks, UIGenericPicker picker, TextAreaGenerator textAreaGenerator, List<UIComponent> taskGridChildren,
            boolean showAddDateLink, Map<String, Object> signatureTaskOwnerProps, Integer taskIndex, TaskGroup taskGroup) {
        Application application = context.getApplication();
        Task task = tasks.get(taskIndex);

        String taskStatus = task.getStatus();
        boolean isTaskRowEditable = isTaskRowEditable(list.responsible, list.fullAccess, task, taskStatus);
        String taskRowId = list.listId + "-" + taskIndex;

        HtmlInputText nameInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
        nameInput.setId("task-name-" + taskRowId);
        nameInput.setReadonly(false);
        nameInput.getAttributes().put("autocomplete", "off");
        putAttribute(nameInput, "styleClass", "ownerName medium");
        nameInput.setValueChangeListener(context.getApplication().createMethodBinding("#{DialogManager.bean.ownerNameChanged}",
                new Class[] { ValueChangeEvent.class }));
        if (list.signatureWorkflow && taskIndex == 0 && task.getOwnerId() == null && signatureTaskOwnerProps != null) {
            task.getNode().getProperties().putAll(signatureTaskOwnerProps);
        }
        nameInput.setValueBinding("value", createPropValueBinding(application, list.workflowIndex, taskIndex, WorkflowCommonModel.Props.OWNER_NAME));

        String ownerId = task.getOwnerId();

        if (ownerId != null && !list.hideExtraInfo) {
            String info = BeanHelper.getSubstituteService().getSubstituteLabel(ownerId);
            if (StringUtils.isNotBlank(info)) {
                final HtmlPanelGroup userPanelGroup = createHtmlPanelGroupWithId(application, "task-name-group-" + taskRowId);
                HtmlOutputText substitutionInfoOutput = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
                substitutionInfoOutput.setValue(info);
                putAttribute(substitutionInfoOutput, "styleClass", "fieldExtraInfo");
                taskGridChildren.add(userPanelGroup);
                addChildren(userPanelGroup, nameInput, substitutionInfoOutput);
            } else {
                taskGridChildren.add(nameInput);
            }
        } else {
            taskGridChildren.add(nameInput);
        }

        if (list.compoundWorkflowDialog) {
            if (workflow.hasTaskResolution()) {
                addResolutionInput(context, application, list.workflowIndex, taskGridChildren, taskIndex, taskRowId, task, textAreaGenerator);
            }

            DateTimePickerGenerator dateTimePickerGenerator = new DateTimePickerGenerator();
            final DateTimePicker dueDateTimeInput = (DateTimePicker) dateTimePickerGenerator.generate(context, "task-duedate-" + taskRowId);
            taskGridChildren.add(dueDateTimeInput);
            dueDateTimeInput.setValueBinding("value", createPropValueBinding(application, list.workflowIndex, taskIndex, WorkflowSpecificModel.Props.DUE_DATE));
            if (isTaskRowEditable) {
                dateTimePickerGenerator.getCustomAttributes().put(DateTimePickerGenerator.DATE_FIELD_LABEL, constants.getTaskPropertyDueDateMessage());
                dateTimePickerGenerator.setupValidDateConstraint(context, propertySheet, null, dueDateTimeInput);
            } else {
                dateTimePickerGenerator.setReadonly(dueDateTimeInput, true);
                nameInput.setReadonly(true);
            }
            Map<String, Object> componentAttributes = new HashMap<>(DUE_DATE_TIME_ATTRIBUTES);
            if (taskGroup != null) {
                componentAttributes.put("styleClass", (componentAttributes.get("styleClass") + " clearGroupRowDate"));
            }
            addAttributes(dueDateTimeInput, componentAttributes);

            if (!workflow.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW)) {
                ValueBindingsWrapper vb = new ValueBindingsWrapper(createPropValueBinding(application, list.workflowIndex, taskIndex,
                        WorkflowSpecificModel.Props.DUE_DATE_DAYS),
                        createPropValueBinding(application, list.workflowIndex, taskIndex, WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS));
                UIComponent classificatorSelector = createDueDateDaysSelector(context, taskRowId + (taskGroup == null ? "" : taskGroup.getGroupId()), isTaskRowEditable, vb);

                if (!isTaskRowEditable) {
                    taskGridChildren.add(classificatorSelector);
                } else {
                    final HtmlPanelGroup panel = createHtmlPanelGroupWithId(application, "task-dueDateDays-panel" + taskRowId);
                    addChildren(panel, classificatorSelector);
                    addOnchangeJavascript(classificatorSelector);
                    addOnchangeClickLink(application, getChildren(panel), "#{CompoundWorkflowDialog.calculateDueDate}",
                            "task-dueDateDays-onclick" + list.listId + "-" + taskIndex, createWfIndexPraram(list.workflowIndex, application),
                            createTaskIndexParam(taskIndex, application));
                    taskGridChildren.add(panel);
                }
            }
            HtmlInputText statusInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
            statusInput.setId("task-status-" + taskRowId);
            statusInput.setReadonly(true);
            statusInput.setValueBinding("value", createPropValueBinding(application, list.workflowIndex, taskIndex, WorkflowCommonModel.Props.STATUS));
            putAttribute(statusInput, "styleClass", "margin-left-4 small");
            taskGridChildren.add(statusInput);
        } else {
            if (list.confirmationWorkflow) {
                addResolutionInput(context, application, list.workflowIndex, taskGridChildren, taskIndex, taskRowId, task, textAreaGenerator);
            }
        }

        final HtmlPanelGroup columnActions = createHtmlPanelGroupWithId(application, "column-actions-" + taskRowId);
        taskGridChildren.add(columnActions);

        Action taskAction = task.getAction();
        final List<UIComponent> actionChildren = addChildren(columnActions);
        if (list.fullAccess && (taskAction == Action.NONE)
                && (Status.NEW.equals(taskStatus) || WorkflowUtil.isResponsibleAssignmentTask(task))
                && (!list.responsible || WorkflowUtil.isActiveResponsible(task))) {
            UIActionLink taskSearchLink = createOwnerSearchLinkWithoutOnClick(context, list.listId, taskIndex);
            actionChildren.add(taskSearchLink);
            String actionId = getActionId(context, picker);
            String onclick = generateFieldSetter(context, picker, actionId, SearchRenderer.OPEN_DIALOG_ACTION + ";" + taskIndex) + list.taskOwnerPickerModalOnclickJsCall;
            taskSearchLink.setOnclick(onclick);
        }

        if (isTaskRowEditable) {
            actionChildren.add(createDeleteTaskLink(application, taskRowId, list.workflowIndex, taskIndex));
            String callBack = BeanHelper.getUserListDialog().getCallbackByPickerFilterValues(picker.getFilterOptions());
            SearchUtil.addSimpleSearchSuggest(nameInput, callBack, picker.getClientId(context), true);
        }

        if (taskAction == Action.NONE) {
            if (list.fullAccess && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED.equals(taskStatus)) && !WorkflowUtil.isInactiveResponsible(task)) {
                if (renderCancelLink(task, compoundWorkflow)) {
                    actionChildren.add(createCancelTaskLink(application, taskRowId, list.workflowIndex, taskIndex));
                }
            }

            if (list.fullAccess && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED.equals(taskStatus) || Status.UNFINISHED.equals(taskStatus))
                    && !WorkflowUtil.isInactiveResponsible(task)) {
                if (list.commentPopupRequired) {
                    String onClickEvent = generateFieldSetter(context, list.commentPopup, list.commentPopupActionId, ModalLayerComponent.ACTION_INDEX + ";" + taskIndex)
                            + list.commentPopupModalJsCall;
                    actionChildren.add(createFinishTaskLink(application, taskRowId, onClickEvent));
                }
            }

            if (list.createAddTaskLink) {
                addChildren(columnActions, createAddTaskLink(application, list, taskRowId, taskIndex + 1));
            }
        } else {
            actionChildren.add(createCancelOrFinishTaskLink(application, taskRowId, taskAction));
            if (list.createAddTaskLink) {
                addChildren(columnActions, createAddTaskLink(application, list, taskRowId, taskIndex + 1));
            }

            if (list.assignmentWorkflow && isAssignmentTaskOwnerSearchRequired(workflow, task, list)) {
                UIActionLink taskSearchLink = createOwnerSearchLink(context, list.listId, picker, taskIndex, list.taskOwnerPickerActionId,
                        list.taskOwnerPickerModalOnclickJsCall); // FIXME Verify if working
                actionChildren.add(taskSearchLink);
            }

        }
        if (showAddDateLink(list, showAddDateLink)) {
            actionChildren.add(createAddDateLink(application, taskRowId, list.workflowIndex, taskIndex));
            showAddDateLink = false;
        }

        return showAddDateLink;
    }

    private HtmlOutputText createCancelOrFinishTaskLink(Application application, String taskRowId, Action taskAction) {
        HtmlOutputText actionTxt = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
        actionTxt.setId("task-action-txt-" + taskRowId);
        String actionMsg = taskAction == Action.UNFINISH ? constants.getCancelMarkedTasksText() : constants.getFinishMarkedTasksText();
        actionTxt.setValue(actionMsg);
        return actionTxt;
    }

    private boolean isAssignmentTaskOwnerSearchRequired(Workflow workflow, Task task, TaskListInfo list) {
        boolean mustCreateOwnerSearchLink;
        if (!list.responsible && Status.NEW.equals(workflow.getStatus())) {
            mustCreateOwnerSearchLink = true;
        } else if (list.responsible && !Status.FINISHED.equals(workflow.getStatus()) && WorkflowUtil.isActiveResponsible(task)) {
            mustCreateOwnerSearchLink = true;
        } else {
            mustCreateOwnerSearchLink = false;
        }
        return mustCreateOwnerSearchLink;
    }

    private Map<String, Object> getSignatureTaskOwnerProps(Workflow workflow, TaskListInfo list) {
        if (list.signatureWorkflow && workflow.getParent().isDocumentWorkflow()) {
            return loadSignatureTaskOwnerProps(workflow.getParent().getParent());
        }
        return null;
    }

    protected HtmlPanelGrid createTaskGrid(Application application, TaskListInfo list) {
        final HtmlPanelGrid taskGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        taskGrid.setId("task-grid-" + list.listId);
        taskGrid.setStyleClass(getTaskGridStyleClass());
        return taskGrid;
    }

    private void initAndAddCommentPopupIfNeeded(FacesContext context, List<UIComponent> resultChildren, TaskListInfo list) {
        if (!list.commentPopupRequired) {
            return;
        }

        ValidatingModalLayerComponent commentPopup = createCommentPopup(context.getApplication(), list.workflowIndex);
        resultChildren.add(commentPopup);

        list.commentPopup = commentPopup;
        list.commentPopupActionId = getActionId(context, list.commentPopup);
        list.commentPopupModalJsCall = "return showModal('" + WorkflowUtil.getDialogId(context, list.commentPopup) + "');";
    }

    private void createTaskListHeader(Application application, Workflow workflow, HtmlPanelGrid taskGrid) {
        boolean isDueDateExtension = workflow.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW);
        List<UIComponent> taskGridChildren = taskGrid.getChildren();

        taskGrid.setColumns(2);
        addHeading(application, taskGridChildren, "workflow_task_owner_name");
        if (dialogManager.getBean() instanceof CompoundWorkflowDialog) {
            if (isDueDateExtension) {
                taskGrid.setColumns(4);
            } else {
                taskGrid.setColumns(5);
                if (workflow.hasTaskResolution()) {
                    taskGrid.setColumns(6);
                    addResolutionHeading(application, taskGridChildren);
                }
            }
            addHeading(application, taskGridChildren, "task_property_due_date");
            if (!isDueDateExtension) {
                addHeading(application, taskGridChildren, "task_property_due_date_days");
            }
            addHeading(application, taskGridChildren, "workflow_status");
        } else {
            if (workflow.isType(WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW)) {
                taskGrid.setColumns(3);
                addResolutionHeading(application, taskGridChildren);
            }
        }
        // dummy header for actions column
        UIOutput actionsHeading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        putAttribute(actionsHeading, "styleClass", "th");
        actionsHeading.setValue("");
        taskGridChildren.add(actionsHeading);
    }

    private Workflow setupTaskListInfo(TaskListInfo listInfo, UIPropertySheet propertySheet, CompoundWorkflow compoundWorkflow) {
        int workflowIndex = (Integer) propertySheet.getAttributes().get(ATTR_WORKFLOW_INDEX);
        boolean isCompoundWorkflowDialog = dialogManager.getBean() instanceof CompoundWorkflowDialog;
        Workflow workflow = compoundWorkflow.getWorkflows().get(workflowIndex);
        QName workflowBlockType = workflow.getNode().getType();

        listInfo.workflowIndex = workflowIndex;
        listInfo.hideExtraInfo = Boolean.parseBoolean(getCustomAttributes().get("hideExtraInfo"));
        listInfo.responsible = new Boolean(getCustomAttributes().get(ATTR_RESPONSIBLE));
        listInfo.workflowBlockType = workflowBlockType;
        listInfo.taskOwnerSearchType = getSearchType(workflow, listInfo);
        listInfo.fullAccess = propertySheet.inEditMode() || ((workflowBlockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)
                || workflowBlockType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)) && !listInfo.responsible);

        listInfo.commentPopupRequired = workflowBlockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)
                || workflowBlockType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)
                || workflowBlockType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW);

        listInfo.signatureWorkflow = workflowBlockType.equals(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW);
        listInfo.confirmationWorkflow = workflowBlockType.equals(WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW);
        listInfo.compoundWorkflowDialog = isCompoundWorkflowDialog;
        listInfo.assignmentWorkflow = workflow.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW);

        listInfo.createAddTaskLink = mustCreateAddTaskLink(workflow, listInfo);
        listInfo.addTaskLinkText = getAddTaskLinkText(listInfo);

        listInfo.independentWorkflow = compoundWorkflow.isIndependentWorkflow();

        return workflow;
    }

    private String getAddTaskLinkText(TaskListInfo c) {
        String coResponsibleSuffix = (c.workflowBlockType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW) && !c.responsible) ? "_co" : "";
        String messageId = "workflow_compound_add_" + c.workflowBlockType.getLocalName() + "_user" + coResponsibleSuffix;
        return MessageUtil.getMessage(messageId);
    }

    private String getTaskGridStyleClass() {
        String styleClass = "recipient tasks";
        String customStyleClass = StringUtils.trimToEmpty(getCustomAttributes().get("styleClass"));

        if (StringUtils.isNotBlank(customStyleClass)) {
            styleClass += (" " + customStyleClass);
        }
        return styleClass;
    }

    private boolean renderCancelLink(Task task, CompoundWorkflow compoundWorkflow) {
        if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && task.isResponsible()) {
            Set<CompoundWorkflow> compoundWorkflows = new HashSet<>();
            compoundWorkflows.add(compoundWorkflow);
            if (compoundWorkflow.isDocumentWorkflow()) {
                NodeRef docRef = compoundWorkflow.getParent();
                // FIXME Query just the childAssocs as these are saved and then query delta_tasks table
                List<CompoundWorkflow> otherDocCompoundWorkflows = BeanHelper.getWorkflowService().getCompoundWorkflows(docRef, compoundWorkflow.getNodeRef());
                compoundWorkflows.addAll(otherDocCompoundWorkflows);
            }
            for (CompoundWorkflow cwf : compoundWorkflows) {
                for (Workflow wf : cwf.getWorkflows()) {
                    if (!wf.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
                        continue;
                    }
                    for (Task t : wf.getTasks()) {
                        if (task.equals(t)) {
                            continue;
                        }
                        if (t.isStatus(Status.NEW, Status.STOPPED, Status.IN_PROGRESS) && !Action.UNFINISH.equals(t.getAction())) { // FIXME this can firts be iterated on the cfw
                            // that is in memory
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    protected TaskListContainer createTaskListContainer(UIComponent parent, TaskListInfo list, FacesContext context, PropertySheetItem item) {
        String currentPageAttributeKey = getCurrentPageAttributeKey(context, item);
        TaskListContainer container = new TaskListContainer(parent, list.workflowIndex, currentPageAttributeKey);
        container.setId("task-panel-" + list.listId);
        return container;
    }

    protected HtmlPanelGroup createHtmlPanelGroupWithId(Application application, String Id) {
        HtmlPanelGroup result = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        result.setId(Id);
        return result;
    }

    private UIActionLink createUIActionLink(Application application, String rowIdentifier) {
        UIActionLink uIActionLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        uIActionLink.setId(rowIdentifier);
        uIActionLink.setValue("");
        uIActionLink.setShowLink(false);
        return uIActionLink;
    }

    private UIActionLink createAddTaskLink(Application application, TaskListInfo info, String taskRowId, int taskIndexValue) {
        UIActionLink addTaskLink = createUIActionLink(application, "task-add-link-" + taskRowId);
        ComponentUtil.setActionLinkTooltipAndActionListener(addTaskLink, info.addTaskLinkText, createMethodBinding(application, "#{DialogManager.bean.addWorkflowTask}"));
        putAttribute(addTaskLink, "styleClass", "icon-link add-person");
        addChildren(addTaskLink, createWfIndexPraram(info.workflowIndex, application), createTaskIndexParam(taskIndexValue, application),
                createUIParam(ATTR_RESPONSIBLE, info.responsible, application));

        return addTaskLink;
    }

    private UIActionLink createDeleteTaskLink(Application application, String taskRowId, int wfIndex, int taskIndex) {
        UIActionLink deleteTaskLink = createUIActionLink(application, "task-remove-link-" + taskRowId);
        ComponentUtil.setActionLinkTooltipAndActionListener(deleteTaskLink, constants.getDeleteTaskLinkText(),
                createMethodBinding(application, "#{DialogManager.bean.removeWorkflowTask}"));
        putAttribute(deleteTaskLink, "styleClass", "icon-link margin-left-4 delete");
        addChildren(deleteTaskLink, createWfIndexPraram(wfIndex, application), createTaskIndexParam(taskIndex, application));

        return deleteTaskLink;
    }

    private UIActionLink createDeleteGroupLink(Application application, TaskGroup group, String rowId, int wfIndex) {
        UIActionLink deleteGroupLink = createUIActionLink(application, "delete-" + rowId);
        ComponentUtil.setActionLinkTooltipAndActionListener(deleteGroupLink, constants.getDeleteTaskLinkText(),
                createMethodBinding(application, "#{DialogManager.bean.deleteGroup}"));
        putAttribute(deleteGroupLink, "styleClass", "icon-link margin-left-4 delete");
        addChildren(deleteGroupLink, createUIParam("groupName", group.getGroupName(), application), createUIParam("groupId", group.getGroupId(), application),
                createWfIndexPraram(wfIndex, application));

        return deleteGroupLink;
    }

    private UIActionLink createFinishTaskLink(Application application, String taskRowId, String onClickEvent) {
        UIActionLink taskFinishLink = createUIActionLink(application, "task-finsih-link-" + taskRowId);
        taskFinishLink.setTooltip(constants.getFinishTaskLinkText());
        putAttribute(taskFinishLink, "styleClass", "icon-link margin-left-4 finish-task");
        taskFinishLink.setOnclick(onClickEvent);

        return taskFinishLink;
    }

    private UIActionLink createCancelTaskLink(Application application, String taskRowId, int wfIndex, int counter) {
        final UIActionLink taskCancelLink = createUIActionLink(application, "task-cancel-link-" + taskRowId);
        ComponentUtil.setActionLinkTooltipAndActionListener(taskCancelLink, constants.getCancelTaskLinkText(),
                createMethodBinding(application, "#{DialogManager.bean.cancelWorkflowTask}"));
        putAttribute(taskCancelLink, "styleClass", "icon-link margin-left-4 cancel-task");
        addChildren(taskCancelLink, createWfIndexPraram(wfIndex, application), createTaskIndexParam(counter, application));

        return taskCancelLink;
    }

    private TaskOwnerSearchType getSearchType(Workflow workflow, TaskListInfo info) {
        TaskOwnerSearchType searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_DEFAULT;
        if (workflow.getType().equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW)) {
            searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_REVIEW;
        } else if (workflow.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) && info.responsible) {
            searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_RESPONSIBLE;
        } else if (workflow.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW)) {
            searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_DUE_DATE_EXTENSION;
        }
        return searchType;
    }

    public static Map<String, Object> loadSignatureTaskOwnerProps(NodeRef docRef) {
        if (docRef == null) {
            return null;
        }
        NodeService nodeService = BeanHelper.getNodeService();
        Map<QName, Serializable> docProps = nodeService.getProperties(docRef);
        String signerId = (String) docProps.get(DocumentDynamicModel.Props.SIGNER_ID);
        String signerName = (String) docProps.get(DocumentCommonModel.Props.SIGNER_NAME);
        boolean assignSigner = StringUtils.isNotBlank(signerId) && StringUtils.isNotBlank(signerName);
        if (assignSigner) {
            @SuppressWarnings("unchecked")
            List<String> orgStructUnit = (List<String>) docProps.get(DocumentCommonModel.Props.SIGNER_ORG_STRUCT_UNIT);

            Map<String, Object> signatureTaskOwnerProps = new HashMap<>();
            signatureTaskOwnerProps.put(WorkflowCommonModel.Props.OWNER_ID.toString(), signerId);
            signatureTaskOwnerProps.put(WorkflowCommonModel.Props.OWNER_NAME.toString(), signerName);
            if (orgStructUnit != null && !orgStructUnit.isEmpty()) {
                signatureTaskOwnerProps.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.toString(), orgStructUnit);
            }
            String signerEmail = (String) docProps.get(DocumentDynamicModel.Props.SIGNER_EMAIL);
            Map<QName, Serializable> userProps = BeanHelper.getUserService().getUserProperties(signerId);
            if (userProps != null) {
                if (StringUtils.isBlank(signerEmail)) {
                    signerEmail = (String) userProps.get(ContentModel.PROP_EMAIL);
                }
                signatureTaskOwnerProps.put(WorkflowCommonModel.Props.OWNER_EMAIL.toString(), signerEmail);
                signatureTaskOwnerProps.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE.toString(), userProps.get(ContentModel.PROP_JOBTITLE));
                List<String> organizationStructurePaths = getOrganizationStructureService().getOrganizationStructurePaths((String) userProps.get(ContentModel.PROP_ORGID));
                // Don't reset if user has manually assigned structure unit in document properties
                if (organizationStructurePaths != null && !organizationStructurePaths.isEmpty()) {
                    signatureTaskOwnerProps.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME.toString(), organizationStructurePaths);
                }
                return signatureTaskOwnerProps;
            }
        }
        return null;
    }

    private String generateGroupRowComponents(FacesContext context, TaskListInfo list, boolean hasTaskResolution, boolean createDeleteLink, TaskGroup group,
            HtmlPanelGrid taskGrid) {
        String taskGroupBinding = "#{DialogManager.bean.taskGroups.byGroupId['" + group.getGroupId() + "']";
        Application application = context.getApplication();
        String rowId = makeLegalId(group.getGroupId());
        HtmlPanelGroup iconAndName = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);

        // Name
        UIComponent groupNameLabel = application.createComponent(UIOutput.COMPONENT_TYPE);
        groupNameLabel.setValueBinding("value", application.createValueBinding(taskGroupBinding + ".groupName}"));
        addChildren(iconAndName, createToggleGroupActionLink(application, group, list.workflowIndex, rowId, "TaskListGenerator"), groupNameLabel);
        addChildren(taskGrid, iconAndName);

        // Generate the spacer for resolution column if needed
        QName blockType = list.workflowBlockType;
        boolean hasResolutionColumn = WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW.equals(blockType)
                || (hasTaskResolution && dialogManager.getBean() instanceof CompoundWorkflowDialog);
        if (hasResolutionColumn) {
            addChildren(taskGrid, ComponentUtil.createSpacer(application));
        }

        if (dialogManager.getBean() instanceof CompoundWorkflowDialog) {
            String dueDateVbString = taskGroupBinding + ".dueDate}";
            addChildren(taskGrid, createTaskGroupDatePicker(context, group, list.workflowIndex, rowId, dueDateVbString));
            addChildren(taskGrid, createTaskGroupDueDateDaysSelector(context, group, list.workflowIndex, rowId));
            addChildren(taskGrid, ComponentUtil.createSpacer(application));
        }

        // Icons
        final HtmlPanelGroup iconsPanel = (HtmlPanelGroup) context.getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        if (createDeleteLink) {
            addChildren(iconsPanel, createDeleteGroupLink(application, group, rowId, list.workflowIndex));
        }
        addChildren(iconsPanel, createAddTaskLink(application, list, rowId, list.maximumVisibleTaskIndex + 1));
        addChildren(taskGrid, iconsPanel);

        return group.getGroupId();
    }

    private HtmlPanelGroup createTaskGroupDueDateDaysSelector(FacesContext context, TaskGroup group, int wfIndex, String rowId) {
        Application application = context.getApplication();
        final HtmlPanelGroup selectorPanel = createHtmlPanelGroupWithId(application, "task-dueDateDays-panel" + group.getGroupId());
        UIComponent selector = createDueDateDaysSelector(context, rowId, true, null);
        addOnchangeJavascript(selector);
        addChildren(selectorPanel, selector);
        addOnchangeClickLink(application, getChildren(selectorPanel), "#{CompoundWorkflowDialog.calculateTaskGroupDueDate}",
                "task-dueDateDays-onclick" + rowId, createUIParam("selector", selector.getClientId(context), application),
                createUIParam("groupName", group.getGroupName(), application), createUIParam("groupId", group.getGroupId(), application), createWfIndexPraram(wfIndex, application));
        return selectorPanel;
    }

    private HtmlPanelGroup createTaskGroupDatePicker(FacesContext context, TaskGroup group, int wfIndex, String rowId, String dueDateVbString) {
        Application application = context.getApplication();

        final HtmlPanelGroup dateTimePickerPanel = createHtmlPanelGroupWithId(application, "task-dueDateDaysAndTime-panel" + group.getGroupId());
        final DateTimePickerGenerator dateTimePickerGenerator = new DateTimePickerGenerator();

        DateTimePicker dueDateTimeInput = (DateTimePicker) dateTimePickerGenerator.generate(context, "group-duedate-" + rowId + group.getGroupId());
        dueDateTimeInput.setValueBinding("value", application.createValueBinding(dueDateVbString));

        addAttributes(dueDateTimeInput, DUE_DATE_TIME_ATTRIBUTES);
        addOnChangeJavascript(dueDateTimeInput, "processTaskDueDateDateInput(jQuery('#' + escapeId4JQ(currElId)));");

        addChildren(dateTimePickerPanel, dueDateTimeInput);
        addOnchangeClickLink(application, getChildren(dateTimePickerPanel), "#{CompoundWorkflowDialog.calculateTaskGroupDueDate}",
                "task-dueDateDaysAndTime-onclick" + rowId, createUIParam("datepicker", dueDateTimeInput.getClientId(context), application),
                createUIParam("groupName", group.getGroupName(), application), createUIParam("groupId", group.getGroupId(), application), createWfIndexPraram(wfIndex, application));

        return dateTimePickerPanel;
    }

    protected UIActionLink createToggleGroupActionLink(Application application, TaskGroup group, int wfIndex, String rowId, String generator) {
        UIActionLink toggleGroupLink = createUIActionLink(application, "task-group-toggle-" + rowId + group.getGroupId());
        String tooltip = group.isExpanded() ? constants.getCollapseTaskGroupText() : constants.getExpandTaskGroupText();
        ComponentUtil.setActionLinkTooltipAndActionListener(toggleGroupLink, tooltip,
                application.createMethodBinding("#{" + generator + ".toggleGroup}", UIActions.ACTION_CLASS_ARGS));

        toggleGroupLink.setImage("/images/icons/" + (group.isExpanded() ? "minus" : "plus") + ".gif");
        addChildren(toggleGroupLink, createUIParam("groupName", group.getGroupName(), application), createUIParam("groupId", group.getGroupId(), application),
                createWfIndexPraram(wfIndex, application));

        return toggleGroupLink;
    }

    public void toggleGroup(ActionEvent event) {
        CompoundWorkflowDefinitionDialog compoundWorkflowDefinitionDialog = (CompoundWorkflowDefinitionDialog) dialogManager.getBean();
        TaskGroup group = compoundWorkflowDefinitionDialog.findTaskGroup(event);
        if (group == null) {
            return;
        }

        group.setExpanded(!group.isExpanded());
        compoundWorkflowDefinitionDialog.updatePanelGroupWithoutWorkflowBlockUpdate();
    }

    public void pageChanged(ActionEvent event) {
        CompoundWorkflowDefinitionDialog compoundWorkflowDefinitionDialog = (CompoundWorkflowDefinitionDialog) dialogManager.getBean();
        compoundWorkflowDefinitionDialog.updatePanelGroupWithoutWorkflowBlockUpdate();
    }

    private boolean showAddDateLink(TaskListInfo listInfo, boolean showAddDateLink) {
        return (showAddDateLink && ((listInfo.assignmentWorkflow && listInfo.responsible) || !listInfo.assignmentWorkflow));
    }

    private UIActionLink createAddDateLink(Application application, String taskRowId, int wfIndex, int counter) {
        UIActionLink taskSetDateLink = createUIActionLink(application, "task-add-date-link-" + taskRowId);
        ComponentUtil.setActionLinkTooltipAndActionListener(taskSetDateLink, MessageUtil.getMessage("add_date_for_all"),
                application.createMethodBinding("#{DialogManager.bean.addDateForAllTasks}", UIActions.ACTION_CLASS_ARGS));
        putAttribute(taskSetDateLink, "styleClass", "icon-link margin-left-4 add_date");
        addChildren(taskSetDateLink, createWfIndexPraram(wfIndex, application), createTaskIndexParam(counter, application));

        return taskSetDateLink;
    }

    private void addResolutionHeading(Application application, final List<UIComponent> taskGridChildren) {
        addHeading(application, taskGridChildren, "task_property_resolution_assignmentTask");
    }

    private void addResolutionInput(FacesContext context, Application application, int wfIndex, final List<UIComponent> taskGridChildren, int counter, String taskRowId, Task task,
            TextAreaGenerator textAreaGenerator) {
        UIComponent resolutionInput;
        if (task.isStatus(Status.NEW, Status.STOPPED)) {
            resolutionInput = textAreaGenerator.generate(context, "task-resolution-" + taskRowId);
            putAttribute(resolutionInput, "styleClass", "expand19-200 width190");
        } else {
            resolutionInput = application.createComponent(UIOutput.COMPONENT_TYPE);
            putAttribute(resolutionInput, "styleClass", "condence50");
        }
        resolutionInput.setValueBinding("value", createPropValueBinding(application, wfIndex, counter, WorkflowSpecificModel.Props.RESOLUTION));
        taskGridChildren.add(resolutionInput);
    }

    private void addHeading(Application application, final List<UIComponent> taskGridChildren, String headingKey) {
        UIOutput dueDateDaysHeading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        putAttribute(dueDateDaysHeading, "styleClass", "th");
        dueDateDaysHeading.setValue(MessageUtil.getMessage(headingKey));
        taskGridChildren.add(dueDateDaysHeading);
    }

    private ValidatingModalLayerComponent createCommentPopup(Application application, int wfIndex) {
        ValidatingModalLayerComponent commentPopup = (ValidatingModalLayerComponent) application.createComponent(ValidatingModalLayerComponent.class.getCanonicalName());
        commentPopup.setId("task-comment-popup-" + wfIndex);
        Map<String, Object> popupAttributes = getAttributes(commentPopup);
        popupAttributes.put(ModalLayerComponent.ATTR_HEADER_KEY, "task_finish_popup");
        popupAttributes.put(ValidatingModalLayerComponent.ATTR_AJAX_ENABLED, Boolean.TRUE);
        popupAttributes.put(TaskListGenerator.ATTR_WORKFLOW_INDEX, wfIndex);
        commentPopup.setActionListener(application.createMethodBinding("#{DialogManager.bean.finishWorkflowTask}", UIActions.ACTION_CLASS_ARGS));

        TextAreaGenerator textAreaGenerator = new TextAreaGenerator();
        UIInput commentInput = (UIInput) textAreaGenerator.generate(FacesContext.getCurrentInstance(), CompoundWorkflowDialog.MODAL_KEY_ENTRY_COMMENT);
        commentInput.setId(CompoundWorkflowDialog.MODAL_KEY_ENTRY_COMMENT);
        Map<String, Object> attributes = getAttributes(commentInput);
        attributes.put(ValidatingModalLayerComponent.ATTR_LABEL_KEY, "task_finish_comment");
        attributes.put(ValidatingModalLayerComponent.ATTR_MANDATORY, Boolean.TRUE);
        attributes.put("styleClass", "expand19-200");
        getChildren(commentPopup).add(commentInput);

        return commentPopup;
    }

    private UIParameter createWfIndexPraram(int wfIndex, Application application) {
        return createUIParam(WF_INDEX, wfIndex, application);
    }

    protected UIParameter createTaskIndexParam(int taskIndexCounter, Application application) {
        return createUIParam(TASK_INDEX, taskIndexCounter, application);
    }

    private ValueBinding createPropValueBinding(Application application, int wfIndex, int taskIndex, QName propName) {
        return application.createValueBinding("#{DialogManager.bean.workflow.workflows[" + wfIndex + "].tasks[" + taskIndex + "].node.properties[\"" + propName + "\"]}");
    }

    private MethodBinding createMethodBinding(Application application, String methodBinding) {
        return application.createMethodBinding(methodBinding, UIActions.ACTION_CLASS_ARGS);
    }

    private boolean mustCreateAddTaskLink(Workflow block, TaskListInfo constants) {
        if (constants.fullAccess
                && !block.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW)
                && (!constants.responsible || block.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW))
                && !Status.FINISHED.equals(block.getStatus())) {
            return true;
        }
        return false;
    }

    public static UIActionLink createOwnerSearchLink(FacesContext context, String listId, UIGenericPicker picker, int taskNumber, String pickerActionId,
            String pickerModalOnclickJsCall) {
        UIActionLink taskSearchLink = createOwnerSearchLinkWithoutOnClick(context, listId, taskNumber);
        String onclick = generateFieldSetter(context, picker, pickerActionId, SearchRenderer.OPEN_DIALOG_ACTION + ";" + taskNumber) + pickerModalOnclickJsCall;
        taskSearchLink.setOnclick(onclick);

        return taskSearchLink;
    }

    private static UIActionLink createOwnerSearchLinkWithoutOnClick(FacesContext context, String listId, int taskNumber) {
        UIActionLink taskSearchLink = (UIActionLink) context.getApplication().createComponent("org.alfresco.faces.ActionLink");
        taskSearchLink.setId("task-search-link-" + listId + "-" + taskNumber);
        taskSearchLink.setValue("");
        taskSearchLink.setTooltip(MessageUtil.getMessage("search"));
        taskSearchLink.setShowLink(false);
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = taskSearchLink.getAttributes();
        attributes.put("styleClass", "icon-link margin-left-4 search");

        return taskSearchLink;
    }

    private UIGenericPicker createOwnerPickerComponent(FacesContext context, TaskListInfo info) {
        Application application = context.getApplication();
        
        UIGenericPicker picker = (UIGenericPicker) application.createComponent("org.alfresco.faces.GenericPicker");
        picker.setId("task-picker-" + info.listId);
        picker.setShowFilter(false);
        picker.setWidth(400);
        TaskOwnerSearchType searchType = info.taskOwnerSearchType;
        if (TaskOwnerSearchType.TASK_OWNER_SEARCH_RESPONSIBLE.equals(searchType) || TaskOwnerSearchType.TASK_OWNER_SEARCH_DUE_DATE_EXTENSION.equals(searchType)) {
            picker.setMultiSelect(false);
        } else {
            picker.setMultiSelect(true);
        }
        setPickerActionListenerAndQueryCallback(picker, searchType, application);
        putAttribute(picker, TaskListGenerator.ATTR_WORKFLOW_INDEX, info.workflowIndex);
        if (!searchType.equals(TaskOwnerSearchType.TASK_OWNER_SEARCH_DUE_DATE_EXTENSION)) {
            picker.setShowFilter(true);
            picker.setShowSelectButton(true);
            picker.setFilterByTaskOwnerStructUnit(!searchType.equals(TaskOwnerSearchType.TASK_OWNER_SEARCH_REVIEW)
                    || !BeanHelper.getWorkflowConstantsBean().isReviewToOtherOrgEnabled()
                    || !info.independentWorkflow);

            boolean responsible = info.responsible != null && info.responsible.booleanValue();
            SelectItem[] filters = getFilters(info.workflowBlockType, searchType, responsible);
            if (filters == null) {
              picker.setValueBinding("filters", createPickerValueBinding(application, searchType, info));
            } else {
            	picker.setFilters(filters);
            }
        }
        return picker;
    }

    private static void setPickerActionListenerAndQueryCallback(UIGenericPicker picker, TaskOwnerSearchType searchType, Application application) {
        String callbackB;
        String searchProcessingB;
        if (TaskOwnerSearchType.TASK_OWNER_SEARCH_RESPONSIBLE.equals(searchType)) {
            callbackB = "#{DialogManager.bean.executeResponsibleOwnerSearch}";
            searchProcessingB = "#{DialogManager.bean.processResponsibleOwnerSearchResults}";
        } else if (TaskOwnerSearchType.TASK_OWNER_SEARCH_DUE_DATE_EXTENSION.equals(searchType)) {
            callbackB = "#{DialogManager.bean.executeDueDateExtensionOwnerSearch}";
            searchProcessingB = "#{DialogManager.bean.processOwnerSearchResults}";
        } else {
            callbackB = "#{DialogManager.bean.executeTaskOwnerSearch}";
            searchProcessingB = "#{DialogManager.bean.processOwnerSearchResults}";
        }
        picker.setQueryCallback(application.createMethodBinding(callbackB, GenericPickerTag.QUERYCALLBACK_CLASS_ARGS));
        picker.setActionListener(application.createMethodBinding(searchProcessingB, UIActions.ACTION_CLASS_ARGS));
    }

    protected ValueBinding createPickerValueBinding(Application application, TaskOwnerSearchType searchType, TaskListInfo info) {
        if (TaskOwnerSearchType.TASK_OWNER_SEARCH_RESPONSIBLE.equals(searchType)) {
            return application.createValueBinding("#{OwnerSearchBean.responsibleOwnerSearchFilters}");
        } else if (TaskOwnerSearchType.TASK_OWNER_SEARCH_REVIEW.equals(searchType)) {
            return application.createValueBinding("#{OwnerSearchBean.reviewOwnerSearchFilters}");
        } else {

            return application.createValueBinding("#{DialogManager.bean.ownerSearchFilters}");
        }
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        // overwrite parent and do nothing.
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

    private Set<TaskListRow> filterVisibleTasks(TaskGroupHolder taskGroups, int workflowIndex, List<Task> tasks, TaskListInfo list) {
        boolean responsible = list.responsible;
        Set<TaskListRow> result = new TreeSet<>();
        String previousGroupName = "";
        TaskListRow previousGroupRow = null;
        int maximumVisibleIndex = 0;
        int taskIndex = 0;

        for (Task task : tasks) {
            boolean hasResponsible = task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE);
            if (responsible == hasResponsible) {
                String ownerGroup = task.getOwnerGroup();
                if (StringUtils.isNotBlank(ownerGroup) && previousGroupName.equals(ownerGroup)) {
                    previousGroupRow.addGroupedTask(taskIndex);
                } else {
                    previousGroupRow = addVisibleTask(result, taskGroups, ownerGroup, workflowIndex, taskIndex);
                    boolean hasGroup = StringUtils.isNotBlank(ownerGroup);
                    previousGroupRow.partOfGroup = hasGroup;
                    previousGroupName = hasGroup ? ownerGroup : "";
                }
                maximumVisibleIndex = taskIndex;
            }
            taskIndex++;
        }

        list.maximumVisibleTaskIndex = maximumVisibleIndex;
        return result;
    }

    protected TaskListRow addVisibleTask(Set<TaskListRow> result, TaskGroupHolder taskGroups, String ownerGroup, int workflowIndex, int taskIndex) {
        TaskListRow row = new TaskListRow(taskIndex);
        if (StringUtils.isNotBlank(ownerGroup)) {
            TaskGroup adjacentTaskGroup = taskGroups.getAdjacentTaskGroup(workflowIndex, ownerGroup, taskIndex);
            boolean taskGroupExpanded = adjacentTaskGroup == null ? false : adjacentTaskGroup.isExpanded();
            if (taskGroupExpanded) {
                row.setGroupExpanded(taskGroupExpanded);
            }
        }

        result.add(row);
        return row;
    }

	private SelectItem[] getFilters(QName workflowBlockType, TaskOwnerSearchType taskOwnerSearchType, boolean responsible) {
		if (workflowBlockType.isMatch(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW) || workflowBlockType.isMatch(WorkflowSpecificModel.Types.SIGNATURE_TASK)) {
			return getFilters("signatureWorkflowRecipient");
		}

		if (workflowBlockType.isMatch(WorkflowSpecificModel.Types.OPINION_WORKFLOW) || workflowBlockType.isMatch(WorkflowSpecificModel.Types.OPINION_TASK)) {
			return getFilters("opinionWorkflowRecipient");
		}

		if (workflowBlockType.isMatch(WorkflowSpecificModel.Types.REVIEW_WORKFLOW) || workflowBlockType.isMatch(WorkflowSpecificModel.Types.REVIEW_TASK)) {
			return getFilters("reviewWorkflowRecipient");
		}

		if (workflowBlockType.isMatch(WorkflowSpecificModel.Types.INFORMATION_WORKFLOW) || workflowBlockType.isMatch(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
			return getFilters("informationWorkflowRecipient");
		}

		if (workflowBlockType.isMatch(WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW) || workflowBlockType.isMatch(WorkflowSpecificModel.Types.CONFIRMATION_TASK)) {
			return getFilters("confirmationWorkflowRecipient");
		}

		if (workflowBlockType.isMatch(WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_WORKFLOW) || workflowBlockType.isMatch(WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_WORKFLOW)) {
			return getFilters("groupAssignmentWorkflowRecipient");
		}

		if (workflowBlockType.isMatch(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
			return getFilters(taskOwnerSearchType == TaskOwnerSearchType.TASK_OWNER_SEARCH_RESPONSIBLE ? "assignmentWorkflowRespRecipient" : "assignmentWorkflowRecipient");
		}

		if (workflowBlockType.isMatch(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)) {
			return getFilters(taskOwnerSearchType == TaskOwnerSearchType.TASK_OWNER_SEARCH_RESPONSIBLE || responsible ? "orderAssignmentWorkflowRespRecipient" : "orderAssignmentWorkflowRecipient");
		}
		return null;
	}

	protected SelectItem[] getFilters(String classificatorValue) {
		ClassificatorService classificatorService = BeanHelper.getClassificatorService();
		List<ClassificatorValue> values = new ArrayList<ClassificatorValue>();

		for (ClassificatorValue value : classificatorService.getAllClassificatorValues(classificatorValue)) {
			if (value.isActive()) {
				values.add(value);
			}
		}

		Collections.sort(values, new Comparator<ClassificatorValue>() {

			@Override
			public int compare(ClassificatorValue o1, ClassificatorValue o2) {
				return o1.isByDefault() ? -1 : o1.compareTo(o2);
			}
		});

		
		
		List<SelectItem> result = new ArrayList<SelectItem>();
		for (ClassificatorValue value : values) {
			result.add(generateItem(value.getValueName()));
		}
		return result.toArray(new SelectItem[] {});
	}

	private SelectItem generateItem(String classificatorValue) {
		if (classificatorValue.equalsIgnoreCase("kasutajad")) {
			return new SelectItem(UserContactGroupSearchBean.USERS_FILTER, MessageUtil.getMessage("task_owner_users"));
		}
		if (classificatorValue.equalsIgnoreCase("kasutajagrupid")) {
			return new SelectItem(UserContactGroupSearchBean.USER_GROUPS_FILTER, MessageUtil.getMessage("task_owner_usergroups"));
		}
		if (classificatorValue.equalsIgnoreCase("kontaktid")) {
			return new SelectItem(UserContactGroupSearchBean.CONTACTS_FILTER, MessageUtil.getMessage("task_owner_contacts"));
		}
		if (classificatorValue.equalsIgnoreCase("kontaktgrupid")) {
			return new SelectItem(UserContactGroupSearchBean.CONTACT_GROUPS_FILTER, MessageUtil.getMessage("task_owner_contactgroups"));
		}
		throw new RuntimeException("Unable to generate SelectItem based on "+classificatorValue);
	}
		
		
	
    protected static class TaskListRow implements Serializable, Comparable<TaskListRow> {
        private static final long serialVersionUID = 1L;
        private final Integer taskIndex;
        private List<Integer> groupedTaskIndices;
        private boolean groupExpanded = false;
        protected boolean partOfGroup;

        public TaskListRow(int taskIndex) {
            this.taskIndex = taskIndex;
        }

        public int getGroupedTasksCount() {
            return groupedTaskIndices == null ? 0 : groupedTaskIndices.size();
        }

        public void addGroupedTask(int taskIndex) {
            if (groupedTaskIndices == null) {
                groupedTaskIndices = new ArrayList<>();
            }
            groupedTaskIndices.add(taskIndex);
        }

        public boolean isSingleTaskRow() {
            return groupedTaskIndices == null || groupedTaskIndices.isEmpty();
        }

        public Integer getTaskIndex() {
            return taskIndex;
        }

        public List<Integer> getGroupedTaskIndices() {
            if (groupedTaskIndices == null) {
                return Collections.emptyList();
            }
            return groupedTaskIndices;
        }

        public boolean isGroupExpanded() {
            return groupExpanded;
        }

        @Override
        public int compareTo(TaskListRow o) {
            return taskIndex.compareTo(o.getTaskIndex());
        }

        public void setGroupExpanded(boolean expanded) {
            groupExpanded = expanded;
        }

    }

    protected static class TaskListInfo {
        
        protected TaskListInfo(FacesContext context) {
            listId = context.getViewRoot().createUniqueId();
        }

        protected final String listId;
        protected boolean createAddTaskLink;
        protected String addTaskLinkText;
        protected QName workflowBlockType;
        protected Boolean responsible;
        protected TaskOwnerSearchType taskOwnerSearchType;
        protected boolean fullAccess;
        protected boolean commentPopupRequired;
        protected boolean hideExtraInfo;
        protected int workflowIndex;
        protected UIComponent commentPopup;
        protected String commentPopupActionId;
        protected String commentPopupModalJsCall;
        protected boolean confirmationWorkflow;
        protected boolean signatureWorkflow;
        protected int maximumVisibleTaskIndex;
        protected String taskOwnerPickerModalOnclickJsCall;
        protected String taskOwnerPickerActionId;
        protected boolean assignmentWorkflow;
        protected boolean compoundWorkflowDialog;
        protected boolean independentWorkflow;
        private Map<String, Object> extraAttributes;

        public Map<String, Object> getExtraAttributes() {
            if (extraAttributes == null) {
                extraAttributes = new HashMap<>();
            }
            return extraAttributes;
        }
    }

}
