package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_NAME;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator.ATTR_DESCRIPTION_AS_LABEL;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.MultiClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_SPECIFIERS;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.MultiClassificatorSelectorGenerator.ATTR_CLASSIFICATOR_SPECIFIER_LABELS;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.MultiClassificatorSelectorGenerator.ATTR_FILTER_NUMERIC;
import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.MultiClassificatorSelectorGenerator.CLASSIFICATOR_NAME_SEPARATOR;
import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;
import static ee.webmedia.alfresco.utils.ComponentUtil.getChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.TASK_INDEX;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getActionId;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getDialogId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
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

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.LabelAndValueSelectorRenderer;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.MultiClassificatorSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ValueBindingsWrapper;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DateTimePicker;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DateTimePickerGenerator;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DateTimePickerRenderer;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.search.SearchRenderer;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Task.Action;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.xtee.client.dhl.DhlXTeeService.SendStatus;

/**
 * Generator for compound workflow tasks setup block.
 * 
 * @author Erko Hansar
 */
public class TaskListGenerator extends BaseComponentGenerator {

    // private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(TaskListGenerator.class);

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
        @SuppressWarnings("unchecked")
        Map<String, Object> propSheetAttrs = propertySheet.getAttributes();
        int wfIndex = (Integer) propSheetAttrs.get(ATTR_WORKFLOW_INDEX);
        Workflow workflow = ((CompoundWorkflowDefinitionDialog) dialogManager.getBean()).getWorkflow().getWorkflows().get(wfIndex);

        TaskOwnerSearchType searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_DEFAULT;
        boolean responsible = new Boolean(getCustomAttributes().get(ATTR_RESPONSIBLE));

        QName blockType = workflow.getNode().getType();
        if (workflow.getType().equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW)) {
            searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_REVIEW;
        } else if (workflow.isType(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) && responsible) {
            searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_RESPONSIBLE;
        } else if (workflow.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_WORKFLOW)) {
            searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_DUE_DATE_EXTENSION;
        } else if (blockType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
            searchType = TaskOwnerSearchType.TASK_OWNER_SEARCH_EXTERNAL_REVIEW;
        }
        boolean fullAccess = propertySheet.inEditMode()
                || ((blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) || blockType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)) && !responsible);

        final HtmlPanelGroup result = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        result.setId("task-panel-" + listId);

        addChildren(item, result);

        final HtmlPanelGrid taskGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        taskGrid.setId("task-grid-" + listId);

        final String customStyleClass = StringUtils.trimToEmpty(getCustomAttributes().get("styleClass"));
        taskGrid.setStyleClass("recipient tasks" + " " + customStyleClass);
        final List<UIComponent> resultChildren = addChildren(result, taskGrid);

        List<Task> tasks = workflow.getTasks();
        List<Integer> visibleTasks = filterTasks(tasks, responsible);
        boolean sendDateDisplayed = false;
        if (!visibleTasks.isEmpty()) {
            HtmlPanelGroup pickerPanel = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            pickerPanel.setId("task-picker-panel-" + listId);
            pickerPanel.setRendererType(TaskListPickerRenderer.class.getCanonicalName());
            resultChildren.add(pickerPanel);

            UIGenericPicker picker = createOwnerPickerComponent(application, listId, wfIndex, searchType);
            addChildren(pickerPanel, picker);

            // This disables doing AJAX submit when picker finish button is pressed
            // Currently, picker finish reconstructs entire panelgroup, which is some levels above propertysheet
            // If AJAX submit is desired, something needs to be reworked
            putAttribute(pickerPanel, Search.AJAX_PARENT_LEVEL_KEY, Integer.valueOf(100));

            ModalLayerComponent commentPopup = null;
            if (blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW) || blockType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)
                    || blockType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)) {
                commentPopup = createCommentPopup(application, listId);
                putAttribute(commentPopup, TaskListGenerator.ATTR_WORKFLOW_INDEX, wfIndex);
                commentPopup.setActionListener(application.createMethodBinding("#{DialogManager.bean.finishWorkflowTask}", UIActions.ACTION_CLASS_ARGS));
                resultChildren.add(commentPopup);
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

            // create table rows for each task
            NodeService nodeService = BeanHelper.getNodeService();
            for (int counter = 0; counter < tasks.size(); counter++) {
                if (visibleTasks.contains(counter)) {
                    Task task = tasks.get(counter);
                    String taskStatus = task.getStatus();

                    HtmlInputText nameInput = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
                    String taskRowId = listId + "-" + counter;
                    nameInput.setId("task-name-" + taskRowId);
                    nameInput.setReadonly(true);
                    putAttribute(nameInput, "styleClass", "ownerName medium");
                    String nameValueBinding = null;
                    if (task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
                        nameValueBinding = createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.INSTITUTION_NAME);
                    } else if (task.isType(WorkflowSpecificModel.Types.SIGNATURE_TASK) && counter == 0 && task.getOwnerId() == null) {
                        NodeRef docRef = task.getParent().getParent().getParent();
                        if (docRef != null) {
                            String signerName = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.SIGNER_NAME);
                            String signerId = (String) nodeService.getProperty(docRef, DocumentDynamicModel.Props.SIGNER_ID);
                            if (StringUtils.isNotBlank(signerId)) {
                                task.setOwnerName(signerName);
                                task.setOwnerId(signerId);
                            }
                        }
                        nameValueBinding = createPropValueBinding(wfIndex, counter, WorkflowCommonModel.Props.OWNER_NAME);
                    } else {
                        nameValueBinding = createPropValueBinding(wfIndex, counter, WorkflowCommonModel.Props.OWNER_NAME);
                    }
                    nameInput.setValueBinding("value", application.createValueBinding(nameValueBinding));

                    String ownerId = task.getOwnerId();
                    boolean hideExtraInfo = new Boolean(getCustomAttributes().get("hideExtraInfo"));
                    if (ownerId != null && !hideExtraInfo) {
                        String info = UserUtil.getSubstitute(ownerId);
                        if (StringUtils.isNotBlank(info)) {
                            final HtmlPanelGroup userPanelGroup = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
                            userPanelGroup.setId("task-name-group-" + listId + "-" + counter);
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
                            addResolutionInput(context, application, wfIndex, taskGridChildren, counter, taskRowId, task);
                        }

                        DateTimePickerGenerator dateTimePickerGenerator = new DateTimePickerGenerator();
                        final DateTimePicker dueDateTimeInput = (DateTimePicker) dateTimePickerGenerator.generate(context, "task-duedate-" + taskRowId);
                        taskGridChildren.add(dueDateTimeInput);
                        String dueDateValueBinding = createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.DUE_DATE);
                        dueDateTimeInput.setValueBinding("value", application.createValueBinding(dueDateValueBinding));
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> dueDateTimeAttributes = dueDateTimeInput.getAttributes();
                        if (isTaskRowEditable(responsible, fullAccess, task, taskStatus)) {
                            dateTimePickerGenerator.getCustomAttributes().put(DateTimePickerGenerator.DATE_FIELD_LABEL, MessageUtil.getMessage("task_property_due_date"));
                            dateTimePickerGenerator.setupValidDateConstraint(context, propertySheet, null, dueDateTimeInput);
                        } else {
                            dateTimePickerGenerator.setReadonly(dueDateTimeInput, true);
                        }
                        dueDateTimeAttributes.put("styleClass", "margin-left-4");
                        dueDateTimeAttributes.put(DateTimePickerRenderer.DATE_STYLE_CLASS_ATTR, "task-due-date-date");
                        dueDateTimeAttributes.put(DateTimePickerRenderer.TIME_STYLE_CLASS_ATTR, "task-due-date-time");

                        if (!isDueDateExtension) {
                            MultiClassificatorSelectorGenerator classificatorSelectorGenerator = new MultiClassificatorSelectorGenerator();
                            UIComponent classificatorSelector = classificatorSelectorGenerator.generateSelectComponent(context, null, false);
                            classificatorSelector.setId("task-dueDateDays-" + listId + "-" + counter);
                            Map<String, String> selectorGeneratorAttributes = classificatorSelectorGenerator.getCustomAttributes();

                            selectorGeneratorAttributes.put(ATTR_FILTER_NUMERIC, "true");
                            selectorGeneratorAttributes.put(ATTR_DESCRIPTION_AS_LABEL, "true");
                            selectorGeneratorAttributes.put(ATTR_CLASSIFICATOR_NAME, "dueDateCalendarDays" + CLASSIFICATOR_NAME_SEPARATOR + "dueDateWorkDays");
                            selectorGeneratorAttributes.put(ATTR_CLASSIFICATOR_SPECIFIER_LABELS,
                                    MessageUtil.getMessage(CALENDAR_DAYS) + CLASSIFICATOR_NAME_SEPARATOR + MessageUtil.getMessage(WORKING_DAYS));
                            selectorGeneratorAttributes.put(ATTR_CLASSIFICATOR_SPECIFIERS, "false" + CLASSIFICATOR_NAME_SEPARATOR + "true");
                            ComponentUtil.createAndSetConverter(context, DueDateDaysConverter.CONVERTER_ID, classificatorSelector);
                            List<ValueBinding> valueBindings = new ArrayList<ValueBinding>();
                            valueBindings.add(application.createValueBinding(createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.DUE_DATE_DAYS)));
                            valueBindings.add(application.createValueBinding(createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS)));
                            classificatorSelector.setValueBinding("value", new ValueBindingsWrapper(valueBindings));
                            classificatorSelectorGenerator.setupSelectComponent(context, null, null, null, classificatorSelector, false);
                            classificatorSelector.setRendererType(LabelAndValueSelectorRenderer.LABEL_AND_VALUE_SELECTOR_RENDERER_TYPE);

                            ComponentUtil.putAttribute(classificatorSelector, CustomAttributeNames.STYLE_CLASS, "width120 task-due-date-days");
                            boolean isTaskRowEditable = isTaskRowEditable(responsible, fullAccess, task, taskStatus);
                            if (!isTaskRowEditable) {
                                ComponentUtil.putAttribute(classificatorSelector, "readonly", true);
                            }
                            putAttribute(classificatorSelector, "displayMandatoryMark", true);
                            ComponentUtil.putAttribute(classificatorSelector, "styleClass", "task-due-date-days margin-left-4 width130");
                            if (!isTaskRowEditable) {
                                taskGridChildren.add(classificatorSelector);
                            } else {
                                final HtmlPanelGroup panel = (HtmlPanelGroup) context.getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
                                panel.setId("task-dueDateDays-panel" + listId + "-" + counter);
                                addChildren(panel, classificatorSelector);
                                ComponentUtil.addOnchangeJavascript(classificatorSelector);
                                ComponentUtil.addOnchangeClickLink(application, getChildren(panel), "#{CompoundWorkflowDialog.calculateDueDate}",
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

                        if (task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
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
                                    ComponentUtil.createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, sendDateInput);
                                    ComponentUtil.putAttribute(sendDateInput, "styleClass", "margin-left-4 disabled-date");
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
                        if (task.isType(WorkflowSpecificModel.Types.CONFIRMATION_TASK)) {
                            addResolutionInput(context, application, wfIndex, taskGridChildren, counter, taskRowId, task);
                        }
                    }

                    final HtmlPanelGroup columnActions = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
                    columnActions.setId("column-actions-" + taskRowId);
                    taskGridChildren.add(columnActions);

                    Action taskAction = task.getAction();
                    final List<UIComponent> actionChildren = addChildren(columnActions);
                    if (fullAccess
                            && (taskAction == Action.NONE)
                            && (Status.NEW.equals(taskStatus)
                                    || ((task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE) && task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK))))
                                            && (!responsible || Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)))) {
                        UIActionLink taskSearchLink = createOwnerSearchLink(context, application, listId, picker, counter);
                        actionChildren.add(taskSearchLink);
                    }
                    if (isTaskRowEditable(responsible, fullAccess, task, taskStatus)) {

                        final UIActionLink taskDeleteLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                        taskDeleteLink.setId("task-remove-link-" + taskRowId);
                        taskDeleteLink.setValue("");
                        taskDeleteLink.setTooltip(MessageUtil.getMessage("delete"));
                        taskDeleteLink.setActionListener(application.createMethodBinding("#{DialogManager.bean.removeWorkflowTask}",
                                UIActions.ACTION_CLASS_ARGS));
                        taskDeleteLink.setShowLink(false);
                        putAttribute(taskDeleteLink, "styleClass", "icon-link margin-left-4 delete");
                        addChildren(taskDeleteLink, createWfIndexPraram(wfIndex, application), createTaskIndexParam(counter, application));
                        actionChildren.add(taskDeleteLink);
                    }
                    if (taskAction == Action.NONE) {
                        if (fullAccess && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED.equals(taskStatus))
                                && !WorkflowUtil.isInactiveResponsible(task)) {

                            final UIActionLink taskCancelLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                            taskCancelLink.setId("task-cancel-link-" + taskRowId);
                            taskCancelLink.setValue("");
                            taskCancelLink.setTooltip(MessageUtil.getMessage("task_cancel"));
                            taskCancelLink.setActionListener(application.createMethodBinding("#{DialogManager.bean.cancelWorkflowTask}",
                                    UIActions.ACTION_CLASS_ARGS));
                            taskCancelLink.setShowLink(false);
                            putAttribute(taskCancelLink, "styleClass", "icon-link margin-left-4 cancel-task");
                            addChildren(taskCancelLink, createWfIndexPraram(wfIndex, application), createTaskIndexParam(counter, application));
                            actionChildren.add(taskCancelLink);
                        }
                        if (fullAccess && (Status.IN_PROGRESS.equals(taskStatus) || Status.STOPPED.equals(taskStatus) || Status.UNFINISHED.equals(taskStatus))
                                && !WorkflowUtil.isInactiveResponsible(task)) {
                            QName taskType = task.getNode().getType();
                            if (taskType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) || taskType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK)
                                    || taskType.equals(WorkflowSpecificModel.Types.OPINION_TASK)) {
                                UIActionLink taskFinishLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                                taskFinishLink.setId("task-finish-link-" + taskRowId);
                                taskFinishLink.setValue("");
                                taskFinishLink.setTooltip(MessageUtil.getMessage("task_finish"));
                                taskFinishLink.setShowLink(false);
                                putAttribute(taskFinishLink, "styleClass", "icon-link margin-left-4 finish-task");
                                String onclick = ComponentUtil.generateFieldSetter(context, commentPopup, WorkflowUtil.getActionId(context, commentPopup),
                                        ModalLayerComponent.ACTION_INDEX + ";" + counter);
                                onclick += "return showModal('" + WorkflowUtil.getDialogId(context, commentPopup) + "');";
                                taskFinishLink.setOnclick(onclick);
                                actionChildren.add(taskFinishLink);
                            }
                        }
                        if (mustCreateAddTaskLink(workflow, responsible, fullAccess)) {
                            createAddTaskLink(application, listId, blockType, counter, columnActions, wfIndex, counter + 1, false, responsible);
                        }
                    } else {
                        HtmlOutputText actionTxt = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
                        actionTxt.setId("task-action-txt-" + taskRowId);
                        String actionMsg = MessageUtil.getMessage(taskAction == Action.UNFINISH ? "task_cancel_marked" : "task_finish_marked");
                        actionTxt.setValue(actionMsg);
                        actionChildren.add(actionTxt);
                        if (mustCreateAddTaskLink(workflow, responsible, fullAccess)) {
                            createAddTaskLink(application, listId, blockType, counter, columnActions, wfIndex, counter + 1, false, responsible);
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
                                UIActionLink taskSearchLink = createOwnerSearchLink(context, application, listId, picker, counter);
                                actionChildren.add(taskSearchLink);
                            }
                        }
                    }
                }
            }
        } else {
            if (mustCreateAddTaskLink(workflow, responsible, fullAccess)) {
                createAddTaskLink(application, listId, blockType, 0, result, wfIndex, 0, true, responsible);
            }
        }
        return result;
    }

    private void addResolutionHeading(Application application, final List<UIComponent> taskGridChildren) {
        addHeading(application, taskGridChildren, "task_property_resolution_assignmentTask");
    }

    private void addResolutionInput(FacesContext context, Application application, int wfIndex, final List<UIComponent> taskGridChildren, int counter, String taskRowId, Task task) {
        TextAreaGenerator textAreaGenerator = new TextAreaGenerator();
        UIComponent resolutionInput = textAreaGenerator.generate(context, "task-resolution-" + taskRowId);
        String reolutionValueBinding = createPropValueBinding(wfIndex, counter, WorkflowSpecificModel.Props.RESOLUTION);
        resolutionInput.setValueBinding("value", application.createValueBinding(reolutionValueBinding));
        putAttribute(resolutionInput, "styleClass", "expand19-200 width190");
        if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK) || !task.isStatus(Status.NEW)) {
            ComponentUtil.setReadonlyAttributeRecursively(resolutionInput);
        }
        taskGridChildren.add(resolutionInput);
    }

    private void addHeading(Application application, final List<UIComponent> taskGridChildren, String headingKey) {
        UIOutput dueDateDaysHeading = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        putAttribute(dueDateDaysHeading, "styleClass", "th");
        dueDateDaysHeading.setValue(MessageUtil.getMessage(headingKey));
        taskGridChildren.add(dueDateDaysHeading);
    }

    private ModalLayerComponent createCommentPopup(Application application, String listId) {
        ModalLayerComponent commentPopup = (ModalLayerComponent) application.createComponent(ModalLayerComponent.class.getCanonicalName());
        commentPopup.setId("task-comment-popup-" + listId);
        ComponentUtil.getAttributes(commentPopup).put(ModalLayerComponent.ATTR_HEADER_KEY, "task_finish_popup");
        UIInput commentInput = (UIInput) application.createComponent(HtmlInputText.COMPONENT_TYPE);
        commentInput.setId(CompoundWorkflowDialog.MODAL_KEY_ENTRY_COMMENT);
        Map<String, Object> attributes = ComponentUtil.getAttributes(commentInput);
        attributes.put(ModalLayerComponent.ATTR_LABEL_KEY, "task_finish_comment");
        attributes.put(ModalLayerComponent.ATTR_MANDATORY, Boolean.TRUE);
        attributes.put("styleClass", "expand19-200");
        attributes.put("style", "height: 50px;");
        ComponentUtil.getChildren(commentPopup).add(commentInput);
        return commentPopup;
    }

    private boolean isTaskRowEditable(boolean responsible, boolean fullAccess, Task task, String taskStatus) {
        return fullAccess && Status.NEW.equals(taskStatus)
                && (!responsible || Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)));
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

    private void createAddTaskLink(Application application, String listId, QName blockType, int counter, HtmlPanelGroup parentComponent, int wfIndex,
            int taskIndexValue, boolean setValue, boolean responsible) {
        UIActionLink taskAddLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        taskAddLink.setId("task-add-link-" + listId + "-" + counter);
        String addLinkText = MessageUtil.getMessage("workflow_compound_add_" + blockType.getLocalName() + "_user"
                + ((blockType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW) && !responsible) ? "_co" : ""));
        taskAddLink.setValue(setValue ? addLinkText : "");
        taskAddLink.setTooltip(addLinkText);
        taskAddLink.setActionListener(createAddTaskMethodBinding(application));
        taskAddLink.setShowLink(false);
        ComponentUtil.putAttribute(taskAddLink, "styleClass", "icon-link add-person");
        addChildren(taskAddLink, createWfIndexPraram(wfIndex, application), createTaskIndexParam(taskIndexValue, application),
                createUIParam(ATTR_RESPONSIBLE, responsible, application));
        addChildren(parentComponent, taskAddLink);
    }

    private MethodBinding createAddTaskMethodBinding(Application application) {
        // DialogManager.bean is either CompoundWorkflowDialog or CompoundWorkflowDefinitionDialog
        return application.createMethodBinding("#{DialogManager.bean.addWorkflowTask}", UIActions.ACTION_CLASS_ARGS);
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

    private UIGenericPicker createOwnerPickerComponent(Application application, String listId, int workflowIndex, TaskOwnerSearchType searchType) {
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
        ComponentUtil.putAttribute(picker, TaskListGenerator.ATTR_WORKFLOW_INDEX, workflowIndex);
        if (!searchType.equals(TaskOwnerSearchType.TASK_OWNER_SEARCH_DUE_DATE_EXTENSION)) {
            picker.setShowFilter(true);
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
