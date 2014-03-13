package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerWithDueDateGenerator.createDueDateDaysSelector;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.dialog.DialogManager;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.tag.GenericPickerTag;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ValueBindingsWrapper;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DateTimePicker;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DateTimePickerGenerator;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DateTimePickerRenderer;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ValidatingModalLayerComponent;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.search.SearchRenderer;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Task.Action;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

/**
 * Generator for compound workflow tasks setup block.
 */
public class TaskListGenerator extends BaseComponentGenerator {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(TaskListGenerator.class);

    public static final String ATTR_RESPONSIBLE = "responsible";
    private static final String CALENDAR_DAYS = "calendarDays";
    public static final String WORKING_DAYS = "workingDays";
    /** Workflow index - same meaning as {@link #WF_INDEX}, but used in different places */
    public static final String ATTR_WORKFLOW_INDEX = "workflow_index";
    /** Workflow index - same meaning as {@link #ATTR_WORKFLOW_INDEX}, but used in different places */
    public static final String WF_INDEX = "index";

    protected DialogManager dialogManager;

    @Override
    public UIComponent generate(FacesContext context, String id) {
        throw new RuntimeException("This is never called!");
    }

    enum TaskOwnerSearchType {
        TASK_OWNER_SEARCH_RESPONSIBLE,
        TASK_OWNER_SEARCH_EXTERNAL_REVIEW,
        TASK_OWNER_SEARCH_DEFAULT,
        TASK_OWNER_SEARCH_REVIEW,
        TASK_OWNER_SEARCH_DUE_DATE_EXTENSION
    }

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, final PropertySheetItem item) {
        Application application = context.getApplication();
        String listId = context.getViewRoot().createUniqueId();
        Map<String, Object> propSheetAttrs = propertySheet.getAttributes();
        int wfIndex = (Integer) propSheetAttrs.get(ATTR_WORKFLOW_INDEX);
        CompoundWorkflowDefinitionDialog compoundWorkflowDefinitionDialog = (CompoundWorkflowDefinitionDialog) dialogManager.getBean();
        CompoundWorkflow compoundWorkflow = compoundWorkflowDefinitionDialog.getWorkflow();
        Workflow workflow = compoundWorkflow.getWorkflows().get(wfIndex);

        boolean responsible = new Boolean(getCustomAttributes().get(ATTR_RESPONSIBLE));

        QName blockType = workflow.getNode().getType();
        boolean isExternalReviewWorkflow = blockType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW);
        TaskOwnerSearchType searchType = getSearchType(workflow, responsible, isExternalReviewWorkflow);
        boolean fullAccess = propertySheet.inEditMode()
                || ((blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) || blockType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)) && !responsible);

        final HtmlPanelGroup result = createHtmlPanelGroupWithId(application, "task-panel-" + listId);

        addChildren(item, result);

        final HtmlPanelGrid taskGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        taskGrid.setId("task-grid-" + listId);

        final String customStyleClass = StringUtils.trimToEmpty(getCustomAttributes().get("styleClass"));
        taskGrid.setStyleClass("recipient tasks" + " " + customStyleClass);
        final List<UIComponent> resultChildren = addChildren(result, taskGrid);

        List<Task> tasks = workflow.getTasks();
        List<Integer> visibleTasks = filterTasks(tasks, responsible);
        Integer maxTaskNr = visibleTasks.isEmpty() ? 0 : Collections.max(visibleTasks);

        boolean sendDateDisplayed = false;

        boolean mustCreateAddTaskLink = mustCreateAddTaskLink(workflow, responsible, fullAccess);
        String addLinkText = MessageUtil.getMessage("workflow_compound_add_" + blockType.getLocalName() + "_user"
                + ((blockType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW) && !responsible) ? "_co" : ""));
        MethodBinding addTaskMB = application.createMethodBinding("#{DialogManager.bean.addWorkflowTask}", UIActions.ACTION_CLASS_ARGS);

        if (!visibleTasks.isEmpty()) {
            HtmlPanelGroup pickerPanel = createHtmlPanelGroupWithId(application, "task-picker-panel-" + listId);
            pickerPanel.setRendererType(TaskListPickerRenderer.class.getCanonicalName());
            resultChildren.add(pickerPanel);

            UIGenericPicker picker = createOwnerPickerComponent(application, listId, wfIndex, searchType, compoundWorkflow.isIndependentWorkflow());
            addChildren(pickerPanel, picker);
            String pickerActionId = getActionId(context, picker);
            String pickerModalOnclickJsCall = "return showModal('" + getDialogId(context, picker) + "');";

            putAttribute(pickerPanel, Search.AJAX_PARENT_LEVEL_KEY, Integer.valueOf(1));

            ValidatingModalLayerComponent commentPopup = null;
            String commentPopupActionId = null;
            String commentPopupModalJsCall = null;
            boolean addCommentPopup = blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) || blockType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)
                    || blockType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW);
            if (addCommentPopup) {
                commentPopup = createCommentPopup(application, wfIndex);
                putAttribute(commentPopup, TaskListGenerator.ATTR_WORKFLOW_INDEX, wfIndex);
                commentPopup.setActionListener(application.createMethodBinding("#{DialogManager.bean.finishWorkflowTask}", UIActions.ACTION_CLASS_ARGS));
                resultChildren.add(commentPopup);
                commentPopupActionId = getActionId(context, commentPopup);
                commentPopupModalJsCall = "return showModal('" + WorkflowUtil.getDialogId(context, commentPopup) + "');";
            }

            // create table header
            taskGrid.setColumns(2);
            final List<UIComponent> taskGridChildren = addChildren(taskGrid);
            addHeading(application, taskGridChildren, "workflow_task_owner_name");
            boolean isDueDateExtension = workflow.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW);
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

                if (workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                    taskGrid.setColumns(6);
                    addHeading(application, taskGridChildren, "task_property_sendStatus_externalReviewTask");
                    for (int counter = 0; counter < tasks.size(); counter++) {
                        if (visibleTasks.contains(counter)) {
                            Task task = tasks.get(counter);
                            if (SendStatus.SENT.toString().equals(task.getSendStatus())) {
                                taskGrid.setColumns(7);
                                addHeading(application, taskGridChildren, "task_property_sendDateTime_externalReviewTask");
                                sendDateDisplayed = true;
                                break;
                            }
                        }
                    }
                }

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

            // All possible constant are moved out of task row component generation cycle
            // due to performance issues
            boolean isSignatureWorkflow = blockType.equals(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW);
            boolean isConfirmationWorkflow = blockType.equals(WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW);
            Map<String, Object> signatureTaskOwnerProps = null;
            boolean assignSigner = false;
            if (isSignatureWorkflow && workflow.getParent().isDocumentWorkflow()) {
                signatureTaskOwnerProps = loadSignatureTaskOwnerProps(workflow.getParent().getParent());
                assignSigner = signatureTaskOwnerProps != null;
            }

            Map<String, Object> dueDateTimeAttr = new HashMap<String, Object>();
            dueDateTimeAttr.put("styleClass", "margin-left-4");
            dueDateTimeAttr.put(DateTimePickerRenderer.DATE_STYLE_CLASS_ATTR, "task-due-date-date");
            dueDateTimeAttr.put(DateTimePickerRenderer.TIME_STYLE_CLASS_ATTR, "task-due-date-time");

            Map<String, Object> groupDueDateTimeAttr = new HashMap<String, Object>(dueDateTimeAttr);
            groupDueDateTimeAttr.put("styleClass", StringUtils.join(Arrays.asList((String) dueDateTimeAttr.get("styleClass"), "groupRowDate"), ' '));

            String taskPropertyDueDateLabel = MessageUtil.getMessage("task_property_due_date");
            String deleteLinkTooltipMsg = MessageUtil.getMessage("delete");
            MethodBinding deleteLinkActionListenerMB = application.createMethodBinding("#{DialogManager.bean.removeWorkflowTask}",
                    UIActions.ACTION_CLASS_ARGS);
            String cancelLinkTooltipMsg = MessageUtil.getMessage("task_cancel");
            MethodBinding cancelLinkActionListenerMB = application.createMethodBinding("#{DialogManager.bean.cancelWorkflowTask}",
                    UIActions.ACTION_CLASS_ARGS);
            String finishLinkTooltipMsg = MessageUtil.getMessage("task_finish");
            String taskCancelMarkedMsg = MessageUtil.getMessage("task_cancel_marked");
            String taskFinishMarkedMsg = MessageUtil.getMessage("task_finish_marked");

            TextAreaGenerator textAreaGenerator = new TextAreaGenerator();

            // END constants used in cycle

            boolean showAddDateLink = ((dialogManager.getBean() instanceof CompoundWorkflowDialog) && workflow.isStatus(Status.NEW));
            boolean isAssignmentWorkflow = workflow.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW);

            List<Map<String, List<TaskGroup>>> taskGroupsByWf = compoundWorkflowDefinitionDialog.getTaskGroups();
            if (taskGroupsByWf.size() <= wfIndex) {
                ((ArrayList<Map<String, List<TaskGroup>>>) taskGroupsByWf).ensureCapacity(wfIndex + 1);
            }

            Map<String, List<TaskGroup>> taskGroups = null;
            try {
                taskGroups = taskGroupsByWf.get(wfIndex);
            } catch (IndexOutOfBoundsException e) {
                // Let the following code create the task group if it isn't added to the list yet.
            }
            if (taskGroups == null) {
                taskGroups = new HashMap<String, List<TaskGroup>>();
                taskGroupsByWf.add(taskGroups);
            }
            Set<TaskGroup> generatedGroups = new HashSet<TaskGroup>();
            String dueDateVbString = null;
            for (Integer counter : visibleTasks) {
                Task task = tasks.get(counter);
                boolean taskInGroup = false;

                // Check if we can group the tasks
                String ownerGroup = task.getOwnerGroup();
                Pair<Integer, TaskGroup> adjacentTaskGroup = getAdjacentTaskGroup(taskGroups.get(ownerGroup), counter);
                Integer order = adjacentTaskGroup.getFirst();
                TaskGroup taskGroup = adjacentTaskGroup.getSecond();
                if (StringUtils.isNotBlank(ownerGroup) && taskGroup != null) {
                    if (!generatedGroups.contains(taskGroup)) {
                        dueDateVbString = generateGroupRow(context, application, wfIndex, workflow.isStatus(Status.NEW), taskGroup, taskGrid, deleteLinkTooltipMsg, blockType,
                                maxTaskNr, responsible, addLinkText, addTaskMB, groupDueDateTimeAttr, order, isConfirmationWorkflow || workflow.hasTaskResolution());
                        generatedGroups.add(taskGroup);
                    }

                    List<Integer> taskIds = taskGroup.getTaskIds();
                    if (!taskIds.contains(counter)) {
                        taskIds.add(counter);
                    }
                    taskInGroup = true;
                    task.setGroupDueDateVbString(dueDateVbString);
                    if (!taskGroup.isExpanded()) {
                        continue; // Skip processing this task further, since it is already included in a group
                    }
                } else if (StringUtils.isNotBlank(ownerGroup) && taskGroup == null) {
                    taskGroup = new TaskGroup(ownerGroup, counter, responsible, fullAccess);
                    if (task.getDueDate() != null) { // If we add a group under a task that has a due date set, we must also copy it to the group
                        taskGroup.setDueDate(task.getDueDate());
                    }
                    List<TaskGroup> groups = taskGroups.get(ownerGroup);
                    if (groups == null) {
                        groups = new ArrayList<TaskGroup>();
                    }
                    groups.add(taskGroup);
                    taskGroups.put(ownerGroup, groups);
                    // Generate the group row
                    dueDateVbString = generateGroupRow(context, application, wfIndex, workflow.isStatus(Status.NEW), taskGroup, taskGrid, deleteLinkTooltipMsg, blockType,
                            maxTaskNr, responsible, addLinkText, addTaskMB, groupDueDateTimeAttr, order, isConfirmationWorkflow || workflow.hasTaskResolution());
                    generatedGroups.add(taskGroup);
                    task.setGroupDueDateVbString(dueDateVbString);
                    continue;
                } else {
                    dueDateVbString = null;
                }

                // Create components for individual tasks
                String taskStatus = task.getStatus();
                boolean isTaskRowEditable = isTaskRowEditable(responsible, fullAccess, task, taskStatus);
                String taskRowId = listId + "-" + counter;

                HtmlInputText nameInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                nameInput.setId("task-name-" + taskRowId);
                nameInput.setReadonly(true);
                putAttribute(nameInput, "styleClass", "ownerName medium");
                String nameValueBinding = null;
                if (isExternalReviewWorkflow) {
                    nameValueBinding = createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.INSTITUTION_NAME);
                } else {
                    nameValueBinding = createPropValueBinding(wfIndex, counter, WorkflowCommonModel.Props.OWNER_NAME);
                    if (isSignatureWorkflow && counter == 0 && task.getOwnerId() == null && assignSigner) {
                        task.getNode().getProperties().putAll(signatureTaskOwnerProps);
                    }
                }
                nameInput.setValueBinding("value", application.createValueBinding(nameValueBinding));

                String ownerId = task.getOwnerId();
                boolean hideExtraInfo = new Boolean(getCustomAttributes().get("hideExtraInfo"));
                if (ownerId != null && !hideExtraInfo) {
                    String info = UserUtil.getSubstitute(ownerId);
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

                if (dialogManager.getBean() instanceof CompoundWorkflowDialog) {
                    if (workflow.hasTaskResolution()) {
                        addResolutionInput(context, application, wfIndex, taskGridChildren, counter, taskRowId, task, textAreaGenerator);
                    }

                    DateTimePickerGenerator dateTimePickerGenerator = new DateTimePickerGenerator();
                    final DateTimePicker dueDateTimeInput = (DateTimePicker) dateTimePickerGenerator.generate(context, "task-duedate-" + taskRowId);
                    taskGridChildren.add(dueDateTimeInput);
                    dueDateTimeInput.setValueBinding("value", application.createValueBinding(createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.DUE_DATE)));
                    if (isTaskRowEditable) {
                        dateTimePickerGenerator.getCustomAttributes().put(DateTimePickerGenerator.DATE_FIELD_LABEL, taskPropertyDueDateLabel);
                        dateTimePickerGenerator.setupValidDateConstraint(context, propertySheet, null, dueDateTimeInput);
                    } else {
                        dateTimePickerGenerator.setReadonly(dueDateTimeInput, true);
                    }
                    Map<String, Object> componentAttributes = new HashMap<String, Object>(dueDateTimeAttr);
                    if (taskInGroup) {
                        componentAttributes.put("styleClass", StringUtils.join(Arrays.asList((String) componentAttributes.get("styleClass"), "clearGroupRowDate"), ' '));
                    }
                    addAttributes(dueDateTimeInput, componentAttributes);

                    if (!isDueDateExtension) {
                        ValueBindingsWrapper vb = new ValueBindingsWrapper(Arrays.asList(
                                application.createValueBinding(createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.DUE_DATE_DAYS))
                                , application.createValueBinding(createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS)))
                                );
                        UIComponent classificatorSelector = createDueDateDaysSelector(context, taskRowId + order, isTaskRowEditable, vb);

                        if (!isTaskRowEditable) {
                            taskGridChildren.add(classificatorSelector);
                        } else {
                            final HtmlPanelGroup panel = createHtmlPanelGroupWithId(application, "task-dueDateDays-panel" + taskRowId);
                            addChildren(panel, classificatorSelector);
                            addOnchangeJavascript(classificatorSelector);
                            addOnchangeClickLink(application, getChildren(panel), "#{CompoundWorkflowDialog.calculateDueDate}",
                                    "task-dueDateDays-onclick" + listId + "-" + counter, createWfIndexPraram(wfIndex, application), createTaskIndexParam(counter, application));
                            taskGridChildren.add(panel);
                        }
                    }
                    HtmlInputText statusInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                    statusInput.setId("task-status-" + taskRowId);
                    statusInput.setReadonly(true);
                    String statusValueBinding = createPropValueBinding(wfIndex, counter, WorkflowCommonModel.Props.STATUS);
                    statusInput.setValueBinding("value", application.createValueBinding(statusValueBinding));
                    putAttribute(statusInput, "styleClass", "margin-left-4 small");
                    taskGridChildren.add(statusInput);

                    if (isExternalReviewWorkflow) {
                        HtmlInputText sendStatusInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                        sendStatusInput.setId("task-sendStatus-" + taskRowId);
                        sendStatusInput.setReadonly(true);
                        String sendStatusValueBinding = createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.SEND_STATUS);
                        sendStatusInput.setValueBinding("value", application.createValueBinding(sendStatusValueBinding));
                        putAttribute(sendStatusInput, "styleClass", "margin-left-4 small");
                        taskGridChildren.add(sendStatusInput);

                        if (sendDateDisplayed) {
                            if (SendStatus.SENT.toString().equals(task.getSendStatus())) {
                                final HtmlInputText sendDateInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                                taskGridChildren.add(sendDateInput);
                                sendDateInput.setId("task-senddate-" + taskRowId);
                                String sendDateValueBinding = createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.DUE_DATE);
                                sendDateInput.setValueBinding("value", application.createValueBinding(sendDateValueBinding));
                                createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, sendDateInput);
                                putAttribute(sendDateInput, "styleClass", "margin-left-4 disabled-date");
                                sendDateInput.setReadonly(true);
                            } else {
                                UIOutput dummyOutput = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                                putAttribute(dummyOutput, "styleClass", "margin-left-4");
                                dummyOutput.setValue("");
                                taskGridChildren.add(dummyOutput);
                            }
                        }
                    }
                } else {
                    if (isConfirmationWorkflow) {
                        addResolutionInput(context, application, wfIndex, taskGridChildren, counter, taskRowId, task, textAreaGenerator);
                    }
                }

                final HtmlPanelGroup columnActions = createHtmlPanelGroupWithId(application, "column-actions-" + taskRowId);
                taskGridChildren.add(columnActions);

                Action taskAction = task.getAction();
                final List<UIComponent> actionChildren = addChildren(columnActions);
                if (fullAccess
                        && (taskAction == Action.NONE)
                        && (Status.NEW.equals(taskStatus)
                        || ((task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE) && task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK))))
                        && (!responsible || Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)))) {
                    UIActionLink taskSearchLink = createOwnerSearchLink(context, application, listId, picker, counter, pickerActionId, pickerModalOnclickJsCall);
                    actionChildren.add(taskSearchLink);
                }

                if (isTaskRowEditable) {
                    UIActionLink taskDeleteLink = createUIActionLink(application, deleteLinkTooltipMsg, deleteLinkActionListenerMB, "task-remove-link-" + taskRowId);
                    putAttribute(taskDeleteLink, "styleClass", "icon-link margin-left-4 delete");
                    addChildren(taskDeleteLink, createWfIndexPraram(wfIndex, application), createTaskIndexParam(counter, application));
                    actionChildren.add(taskDeleteLink);
                }

                if (taskAction == Action.NONE) {
                    if (fullAccess && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED.equals(taskStatus))
                            && !WorkflowUtil.isInactiveResponsible(task)) {

                        if (renderCancelLink(task, compoundWorkflow)) {
                            final UIActionLink taskCancelLink = createUIActionLink(application, cancelLinkTooltipMsg, cancelLinkActionListenerMB, "task-cancel-link-" + taskRowId);
                            putAttribute(taskCancelLink, "styleClass", "icon-link margin-left-4 cancel-task");
                            addChildren(taskCancelLink, createWfIndexPraram(wfIndex, application), createTaskIndexParam(counter, application));
                            actionChildren.add(taskCancelLink);
                        }
                    }

                    if (fullAccess && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED.equals(taskStatus) || Status.UNFINISHED.equals(taskStatus))
                            && !WorkflowUtil.isInactiveResponsible(task)) {
                        if (addCommentPopup) {
                            UIActionLink taskFinishLink = createUIActionLink(application, finishLinkTooltipMsg, null, "task-finsih-link-" + taskRowId);
                            putAttribute(taskFinishLink, "styleClass", "icon-link margin-left-4 finish-task");
                            String onclick = generateFieldSetter(context, commentPopup, commentPopupActionId,
                                    ModalLayerComponent.ACTION_INDEX + ";" + counter) + commentPopupModalJsCall;
                            taskFinishLink.setOnclick(onclick);
                            actionChildren.add(taskFinishLink);
                        }
                    }

                    if (mustCreateAddTaskLink) {
                        createAddTaskLink(application, taskRowId, blockType, columnActions, wfIndex, counter + 1, false, responsible, addLinkText, addTaskMB);
                    }

                } else {
                    HtmlOutputText actionTxt = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
                    actionTxt.setId("task-action-txt-" + taskRowId);
                    String actionMsg = taskAction == Action.UNFINISH ? taskCancelMarkedMsg : taskFinishMarkedMsg;
                    actionTxt.setValue(actionMsg);
                    actionChildren.add(actionTxt);
                    if (mustCreateAddTaskLink) {
                        createAddTaskLink(application, taskRowId, blockType, columnActions, wfIndex, counter + 1, false, responsible, addLinkText, addTaskMB);
                    }
                    if (blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
                        final boolean mustCreateOwnerSearchLink;
                        if (!responsible && Status.NEW.equals(workflow.getStatus())) {
                            mustCreateOwnerSearchLink = true;
                        } else if (responsible && !Status.FINISHED.equals(workflow.getStatus()) && WorkflowUtil.isActiveResponsible(task)) {
                            mustCreateOwnerSearchLink = true;
                        } else {
                            mustCreateOwnerSearchLink = false;
                        }
                        if (mustCreateOwnerSearchLink) {
                            UIActionLink taskSearchLink = createOwnerSearchLink(context, application, listId, picker, counter, pickerActionId, pickerModalOnclickJsCall);
                            actionChildren.add(taskSearchLink);
                        }
                    }
                }
                if (showAddDateLink(showAddDateLink, isAssignmentWorkflow, responsible)) {
                    actionChildren.add(createAddDateLink(application, taskRowId, wfIndex, counter));
                    showAddDateLink = false;
                }
            }
        } else {
            if (mustCreateAddTaskLink) {
                createAddTaskLink(application, listId + "-0", blockType, result, wfIndex, 0, true, responsible, addLinkText, addTaskMB);
            }
        }
        ComponentUtil.setAjaxEnabledOnActionLinksRecursive(result, 1);
        return result;
    }

    private boolean renderCancelLink(Task task, CompoundWorkflow compoundWorkflow) {
        if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && task.isResponsible()) {
            Set<CompoundWorkflow> compoundWorkflows = new HashSet<CompoundWorkflow>();
            compoundWorkflows.add(compoundWorkflow);
            if (compoundWorkflow.isDocumentWorkflow()) {
                NodeRef docRef = compoundWorkflow.getParent();
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
                        if (t.isStatus(Status.NEW, Status.STOPPED, Status.IN_PROGRESS) && !Action.UNFINISH.equals(t.getAction())) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private HtmlPanelGroup createHtmlPanelGroupWithId(Application application, String Id) {
        HtmlPanelGroup result = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        result.setId(Id);
        return result;
    }

    private UIActionLink createUIActionLink(Application application, String linkTooltipMsg, MethodBinding linkActionListenerMB, String taskRowIdentifier) {
        UIActionLink uIActionLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        uIActionLink.setId(taskRowIdentifier);
        uIActionLink.setValue("");
        uIActionLink.setTooltip(linkTooltipMsg);
        if (linkActionListenerMB != null) {
            uIActionLink.setActionListener(linkActionListenerMB);
        }
        uIActionLink.setShowLink(false);
        return uIActionLink;
    }

    private TaskOwnerSearchType getSearchType(Workflow workflow, boolean responsible, boolean isExternalReviewWorkflow) {
        TaskOwnerSearchType searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_DEFAULT;
        if (workflow.getType().equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW)) {
            searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_REVIEW;
        } else if (workflow.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) && responsible) {
            searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_RESPONSIBLE;
        } else if (workflow.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW)) {
            searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_DUE_DATE_EXTENSION;
        } else if (isExternalReviewWorkflow) {
            searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_EXTERNAL_REVIEW;
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

            Map<String, Object> signatureTaskOwnerProps = new HashMap<String, Object>();
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

    private Pair<Integer, TaskGroup> getAdjacentTaskGroup(List<TaskGroup> taskGroupsByGroupName, Integer counter) {
        int groupNr = 0;
        if (taskGroupsByGroupName == null) {
            return new Pair<Integer, TaskGroup>(groupNr, null);
        }

        for (groupNr = 0; groupNr < taskGroupsByGroupName.size(); groupNr++) {
            TaskGroup taskGroup = taskGroupsByGroupName.get(groupNr);
            for (Integer taskId : taskGroup.getTaskIds()) {
                if (taskId.equals(counter) || Math.abs(counter - taskId) == 1) {
                    return new Pair<Integer, TaskGroup>(groupNr, taskGroup);
                }
            }
        }

        return new Pair<Integer, TaskGroup>(groupNr, null);
    }

    private String generateGroupRow(FacesContext context, Application application, int wfIndex, boolean isNewWorkflow, TaskGroup group, HtmlPanelGrid taskGrid,
            String deleteLinkTooltipMsg, QName blockType, Integer maxTaskNr, boolean responsible, String addLinkText, MethodBinding addTaskMB, Map<String, Object> dueDateTimeAttr,
            Integer order, boolean hasResolutionColumn) {
        HtmlPanelGroup iconAndName = (HtmlPanelGroup) context.getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        String dueDateVbString = null;

        String toggleTooltipMessage = MessageUtil.getMessage((group.isExpanded() ? "hide_details" : "show_details"));
        String rowId = makeLegalId(group.getGroupId());

        UIActionLink toggle = createUIActionLink(application, toggleTooltipMessage,
                application.createMethodBinding("#{TaskListGenerator.toggleGroup}", UIActions.ACTION_CLASS_ARGS), "task-group-toggle-" + rowId + order);
        toggle.setImage("/images/icons/" + (group.isExpanded() ? "minus" : "plus") + ".gif");
        addChildren(toggle, createUIParam("groupName", group.getGroupName(), application), createUIParam("groupId", group.getGroupId(), application),
                createWfIndexPraram(wfIndex, application));

        String groupName = group.getGroupName();
        // Name
        UIComponent groupNameLabel = application.createComponent(UIOutput.COMPONENT_TYPE);
        groupNameLabel.setValueBinding("value", application.createValueBinding("#{DialogManager.bean.taskGroups[" + wfIndex + "]['" + groupName + "'][" + order + "].groupName}"));
        addChildren(iconAndName, toggle, groupNameLabel);
        addChildren(taskGrid, iconAndName);

        // Generate the spacer for resolution column if needed
        boolean resolutionDisabled = (WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW.equals(blockType) || WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW.equals(blockType))
                && !(dialogManager.getBean() instanceof CompoundWorkflowDialog);
        if (hasResolutionColumn && !resolutionDisabled) {
            UIOutput spacer = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
            spacer.setValue("");
            addChildren(taskGrid, spacer);
        }

        if (dialogManager.getBean() instanceof CompoundWorkflowDialog) {
            // Date
            final HtmlPanelGroup dateTimePickerPanel = createHtmlPanelGroupWithId(application, "task-dueDateDaysAndTime-panel" + group.getGroupId());
            final DateTimePickerGenerator dateTimePickerGenerator = new DateTimePickerGenerator();

            DateTimePicker dueDateTimeInput = (DateTimePicker) dateTimePickerGenerator.generate(context, "group-duedate-" + rowId + order);
            dueDateVbString = "#{DialogManager.bean.taskGroups[" + wfIndex + "]['" + groupName + "'][" + order + "].dueDate}";
            dueDateTimeInput.setValueBinding("value", application.createValueBinding(dueDateVbString));

            addAttributes(dueDateTimeInput, dueDateTimeAttr);
            addOnChangeJavascript(dueDateTimeInput, "processTaskDueDateDateInput(jQuery('#' + escapeId4JQ(currElId)));");

            addChildren(dateTimePickerPanel, dueDateTimeInput);
            addOnchangeClickLink(application, getChildren(dateTimePickerPanel), "#{CompoundWorkflowDialog.calculateTaskGroupDueDate}",
                    "task-dueDateDaysAndTime-onclick" + rowId, createUIParam("datepicker", dueDateTimeInput.getClientId(context), application),
                    createUIParam("groupName", groupName, application), createUIParam("groupId", group.getGroupId(), application), createWfIndexPraram(wfIndex, application));
            addChildren(taskGrid, dateTimePickerPanel);

            // Selector
            final HtmlPanelGroup selectorPanel = createHtmlPanelGroupWithId(application, "task-dueDateDays-panel" + group.getGroupId());
            UIComponent selector = createDueDateDaysSelector(context, rowId, true, null);
            addOnchangeJavascript(selector);
            addChildren(selectorPanel, selector);
            addOnchangeClickLink(application, getChildren(selectorPanel), "#{CompoundWorkflowDialog.calculateTaskGroupDueDate}",
                    "task-dueDateDays-onclick" + rowId, createUIParam("selector", selector.getClientId(context), application),
                    createUIParam("groupName", groupName, application), createUIParam("groupId", group.getGroupId(), application), createWfIndexPraram(wfIndex, application));
            addChildren(taskGrid, selectorPanel);

            // Status spacer
            UIOutput spacer = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
            spacer.setValue("");
            addChildren(taskGrid, spacer);

            if (WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW.equals(blockType)) {
                // Sending status spacer
                UIOutput sendingStatus = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
                sendingStatus.setValue("");
                addChildren(taskGrid, sendingStatus);
            }
        }

        // Icons
        final HtmlPanelGroup iconsPanel = (HtmlPanelGroup) context.getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        if (isNewWorkflow) {
            UIActionLink deleteLink = createUIActionLink(application, deleteLinkTooltipMsg,
                    application.createMethodBinding("#{TaskListGenerator.deleteGroup}", UIActions.ACTION_CLASS_ARGS), "delete-" + rowId);
            putAttribute(deleteLink, "styleClass", "icon-link margin-left-4 delete");
            addChildren(deleteLink, createUIParam("groupName", groupName, application), createUIParam("groupId", group.getGroupId(), application),
                    createWfIndexPraram(wfIndex, application));
            addChildren(iconsPanel, deleteLink);
        }
        createAddTaskLink(application, rowId, blockType, iconsPanel, wfIndex, ++maxTaskNr, false, responsible, addLinkText, addTaskMB);
        addChildren(taskGrid, iconsPanel);

        return dueDateVbString;
    }

    public void toggleGroup(ActionEvent event) {
        CompoundWorkflowDefinitionDialog compoundWorkflowDefinitionDialog = (CompoundWorkflowDefinitionDialog) dialogManager.getBean();
        TaskGroup group = compoundWorkflowDefinitionDialog.findTaskGroup(event);
        if (group == null) {
            return;
        }

        if (!group.isExpanded()) {
            List<Task> tasks = compoundWorkflowDefinitionDialog.getWorkflow().getWorkflows().get(ActionUtil.getParam(event, WF_INDEX, Integer.class)).getTasks();
            WorkflowUtil.setGroupTasksDueDates(group, tasks);
        }

        group.setExpanded(!group.isExpanded());
        compoundWorkflowDefinitionDialog.updatePanelGroupWithoutWorkflowBlockUpdate();
    }

    public void deleteGroup(ActionEvent event) {
        CompoundWorkflowDefinitionDialog compoundWorkflowDefinitionDialog = (CompoundWorkflowDefinitionDialog) dialogManager.getBean();
        TaskGroup group = compoundWorkflowDefinitionDialog.findTaskGroup(event);
        if (group == null) {
            return;
        }

        Integer wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);

        // Delete the tasks one by one
        List<Integer> taskIds = new ArrayList<Integer>(group.getTaskIds()); // Avoid concurrent modification from iteration
        Collections.sort(taskIds, Collections.reverseOrder()); // Start from the largest taskId so we don't need to sync the original list
        for (Integer taskId : taskIds) {
            compoundWorkflowDefinitionDialog.removeWorkflowTask(wfIndex, taskId, false);
        }

        // And remove the group itself
        compoundWorkflowDefinitionDialog.getTaskGroups().get(wfIndex).get(group.getGroupName()).remove(group);
        compoundWorkflowDefinitionDialog.updatePanelGroupWithoutWorkflowBlockUpdate();
    }

    private boolean showAddDateLink(boolean showAddDateLink, boolean isAssignmentWorkflow, boolean responsible) {
        return (showAddDateLink && ((isAssignmentWorkflow && responsible) || !isAssignmentWorkflow));
    }

    private UIActionLink createAddDateLink(Application application, String taskRowId, int wfIndex, int counter) {
        UIActionLink taskSetDateLink = createUIActionLink(application, MessageUtil.getMessage("add_date_for_all"),
                application.createMethodBinding("#{DialogManager.bean.addDateForAllTasks}", UIActions.ACTION_CLASS_ARGS), "task-add-date-link-" + taskRowId);
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
        String reolutionValueBinding = createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.RESOLUTION);
        resolutionInput.setValueBinding("value", application.createValueBinding(reolutionValueBinding));
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

    private String createPropValueBinding(int wfIndex, int taskIndex, QName propName) {
        return "#{DialogManager.bean.workflow.workflows[" + wfIndex + "].tasks[" + taskIndex + "].node.properties[\"" + propName + "\"]}";
    }

    private boolean mustCreateAddTaskLink(Workflow block, boolean responsible, boolean fullAccess) {
        if (fullAccess
                && !block.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW)
                && (!responsible || block.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW))
                && !Status.FINISHED.equals(block.getStatus())) {
            return true;
        }
        return false;
    }

    private void createAddTaskLink(Application application, String linkId, QName blockType, HtmlPanelGroup parentComponent, int wfIndex,
            int taskIndexValue, boolean setValue, boolean responsible, String addLinkText, MethodBinding addTaskMB) {
        UIActionLink taskAddLink = createUIActionLink(application, addLinkText, addTaskMB, "task-add-link-" + linkId);
        taskAddLink.setValue(setValue ? addLinkText : "");
        putAttribute(taskAddLink, "styleClass", "icon-link add-person");
        addChildren(taskAddLink, createWfIndexPraram(wfIndex, application), createTaskIndexParam(taskIndexValue, application),
                createUIParam(ATTR_RESPONSIBLE, responsible, application));
        addChildren(parentComponent, taskAddLink);
    }

    public static UIActionLink createOwnerSearchLink(FacesContext context, Application application, String listId, UIGenericPicker picker, int taskNumber, String pickerActionId,
            String pickerModalOnclickJsCall) {
        UIActionLink taskSearchLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        taskSearchLink.setId("task-search-link-" + listId + "-" + taskNumber);
        taskSearchLink.setValue("");
        taskSearchLink.setTooltip(MessageUtil.getMessage("search"));
        taskSearchLink.setShowLink(false);
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = taskSearchLink.getAttributes();
        attributes.put("styleClass", "icon-link margin-left-4 search");
        String onclick = generateFieldSetter(context, picker, pickerActionId, SearchRenderer.OPEN_DIALOG_ACTION + ";" + taskNumber) + pickerModalOnclickJsCall;
        taskSearchLink.setOnclick(onclick);
        return taskSearchLink;
    }

    private UIGenericPicker createOwnerPickerComponent(Application application, String listId, int workflowIndex, TaskOwnerSearchType searchType,
            boolean isIndependentCompoundWorkflow) {
        UIGenericPicker picker = (UIGenericPicker) application.createComponent("org.alfresco.faces.GenericPicker");
        picker.setId("task-picker-" + listId);
        picker.setShowFilter(false);
        picker.setWidth(400);
        if (TaskOwnerSearchType.TASK_OWNER_SEARCH_RESPONSIBLE.equals(searchType) || TaskOwnerSearchType.TASK_OWNER_SEARCH_DUE_DATE_EXTENSION.equals(searchType)) {
            picker.setMultiSelect(false);
        } else {
            picker.setMultiSelect(true);
        }
        setPickerActionListenerAndQueryCallback(picker, searchType, application);
        putAttribute(picker, TaskListGenerator.ATTR_WORKFLOW_INDEX, workflowIndex);
        if (!searchType.equals(TaskOwnerSearchType.TASK_OWNER_SEARCH_DUE_DATE_EXTENSION)) {
            picker.setShowFilter(true);
            picker.setShowSelectButton(true);
            picker.setFilterByTaskOwnerStructUnit(!searchType.equals(TaskOwnerSearchType.TASK_OWNER_SEARCH_REVIEW) || !BeanHelper.getWorkflowService().isReviewToOtherOrgEnabled()
                    || !isIndependentCompoundWorkflow);
            picker.setValueBinding("filters", createPickerValueBinding(application, searchType));
        }
        return picker;
    }

    private static void setPickerActionListenerAndQueryCallback(UIGenericPicker picker, TaskOwnerSearchType searchType, Application application) {
        String callbackB;
        String searchProcessingB;
        if (TaskOwnerSearchType.TASK_OWNER_SEARCH_RESPONSIBLE.equals(searchType)) {
            callbackB = "#{DialogManager.bean.executeResponsibleOwnerSearch}";
            searchProcessingB = "#{DialogManager.bean.processResponsibleOwnerSearchResults}";
        } else if (TaskOwnerSearchType.TASK_OWNER_SEARCH_EXTERNAL_REVIEW.equals(searchType)) {
            callbackB = "#{DialogManager.bean.executeExternalReviewOwnerSearch}";
            searchProcessingB = "#{DialogManager.bean.processExternalReviewOwnerSearchResults}";
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

    protected ValueBinding createPickerValueBinding(Application application, TaskOwnerSearchType searchType) {
        if (TaskOwnerSearchType.TASK_OWNER_SEARCH_RESPONSIBLE.equals(searchType)) {
            return application.createValueBinding("#{OwnerSearchBean.responsibleOwnerSearchFilters}");
        } else if (TaskOwnerSearchType.TASK_OWNER_SEARCH_EXTERNAL_REVIEW.equals(searchType)) {
            return application.createValueBinding("#{DialogManager.bean.externalReviewOwnerSearchFilters}");
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