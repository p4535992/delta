package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getPermissionService;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIPropertySheet;
import ee.webmedia.alfresco.common.propertysheet.renderkit.PropertySheetGridRenderer;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.document.einvoice.web.TransactionsBlockBean;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.signature.web.SignatureAppletModalComponent;
import ee.webmedia.alfresco.signature.web.SignatureBlockBean;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowBlockItem;
import ee.webmedia.alfresco.workflow.model.WorkflowBlockItemGroup;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
import ee.webmedia.alfresco.workflow.service.SignatureTask;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

public class WorkflowBlockBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(WorkflowBlockBean.class);
    public static final String BEAN_NAME = "WorkflowBlockBean";

    private static final String WORKFLOW_METHOD_BINDING_NAME = "#{DocumentDialog.workflow.findCompoundWorkflowDefinitions}";
    private static final String DROPDOWN_MENU_ITEM_ICON = "/images/icons/versioned_properties.gif";
    private static final String MSG_WORKFLOW_ACTION_GROUP = "workflow_compound_start_workflow";
    private static final String ATTRIB_OUTCOME_INDEX = "outcomeIndex";

    /** task index attribute name */
    private static final String ATTRIB_INDEX = "index";
    private FileBlockBean fileBlockBean;
    private MetadataBlockBean metadataBlockBean;
    private DelegationBean delegationBean;
    private TransactionsBlockBean transactionsBlockBean;

    private transient DocumentService documentService;
    private transient WorkflowService workflowService;
    private transient UserService userService;
    private transient FileService fileService;
    private transient SignatureService signatureService;
    private transient DvkService dvkService;

    private transient HtmlPanelGroup dataTableGroup;

    private NodeRef docRef;
    private Node document;
    private NodeRef taskPanelControlDocument;
    private List<CompoundWorkflow> compoundWorkflows;
    private List<Task> myTasks;
    private List<Task> finishedReviewTasks;
    private List<Task> finishedOpinionTasks;
    private SignatureTask signatureTask;

    public void init(Node document) {
        this.document = document;
        docRef = document.getNodeRef();
        delegationBean.setWorkflowBlockBean(this);
        restore();
    }

    public void reset() {
        document = null;
        compoundWorkflows = null;
        myTasks = null;
        finishedReviewTasks = null;
        finishedOpinionTasks = null;
        signatureTask = null;
        dataTableGroup = null;
        delegationBean.reset();
    }

    public void restore() {
        compoundWorkflows = getWorkflowService().getCompoundWorkflows(docRef);
        myTasks = getWorkflowService().getMyTasksInProgress(compoundWorkflows);
        finishedReviewTasks = WorkflowUtil.getFinishedTasks(compoundWorkflows, WorkflowSpecificModel.Types.REVIEW_TASK);
        finishedOpinionTasks = WorkflowUtil.getFinishedTasks(compoundWorkflows, WorkflowSpecificModel.Types.OPINION_TASK);
        signatureTask = null;
        delegationBean.reset();
        // rebuild the whole task panel
        constructTaskPanelGroup();
    }

    public boolean isCompoundWorkflowOwner() {
        return getWorkflowService().isOwner(getCompoundWorkflows());
    }

    public List<ActionDefinition> findCompoundWorkflowDefinitions(String documentTypeQName) {
        return findCompoundWorkflowDefinitions(documentTypeQName, DocumentStatus.WORKING.getValueName());
    }

    public List<ActionDefinition> findCompoundWorkflowDefinitions(String documentTypeQName, String documentStatus) {
        WorkflowService workflowService = getWorkflowService();
        boolean showCWorkflowDefsWith1Workflow = false;
        for (CompoundWorkflow cWorkflow : compoundWorkflows) {
            if (cWorkflow.isStatus(Status.IN_PROGRESS, Status.STOPPED) && cWorkflow.getWorkflows().size() > 1) {
                showCWorkflowDefsWith1Workflow = true;
            }
        }

        QName documentType = QName.createQName(documentTypeQName);
        List<CompoundWorkflowDefinition> workflowDefs = workflowService.getCompoundWorkflowDefinitions(documentType, documentStatus);
        List<ActionDefinition> actionDefinitions = new ArrayList<ActionDefinition>(workflowDefs.size());
        String userId = AuthenticationUtil.getRunAsUser();
        for (CompoundWorkflowDefinition compoundWorkflowDefinition : workflowDefs) {
            String cWFUserId = compoundWorkflowDefinition.getUserId();
            if (cWFUserId != null && StringUtils.equals(cWFUserId, userId) // defined by other user for private use
                    || (showCWorkflowDefsWith1Workflow && compoundWorkflowDefinition.getWorkflows().size() > 1)) {
                continue;
            }
            if (workflowService.externalReviewWorkflowEnabled() || !containsExternalReviewWorkflows(compoundWorkflowDefinition)) {
                ActionDefinition actionDefinition = new ActionDefinition("compoundWorkflowDefinitionAction");
                actionDefinition.Image = DROPDOWN_MENU_ITEM_ICON;
                actionDefinition.Label = compoundWorkflowDefinition.getName();
                actionDefinition.Action = "#{DocumentDialog.workflow.getCompoundWorkflowDialog}";
                actionDefinition.ActionListener = "#{CompoundWorkflowDialog.setupNewWorkflow}";
                actionDefinition.addParam("compoundWorkflowDefinitionNodeRef", compoundWorkflowDefinition.getNode().getNodeRef().toString());

                actionDefinitions.add(actionDefinition);
            }
        }

        return actionDefinitions;
    }

    private boolean containsExternalReviewWorkflows(CompoundWorkflowDefinition compoundWorkflowDefinition) {
        for (Workflow workflow : compoundWorkflowDefinition.getWorkflows()) {
            if (workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                return true;
            }
        }
        return false;
    }

    public String getCompoundWorkflowDialog() {
        DocumentDialog dialog = (DocumentDialog) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), DocumentDialog.BEAN_NAME);
        final NodeService nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
        if (!nodeService.exists(dialog.getNode().getNodeRef())) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "workflow_compound_start_workflow_error_docDeleted");
            return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
        }
        return "dialog:compoundWorkflowDialog";
    }

    public String getWorkflowMethodBindingName() {
        // Check if at least one condition is true, if not return null (don't show the button)
        // the logged in user is an admin or doc.manager
        // or user's id is document 'ownerId'
        // or user's id is 'taskOwnerId' and 'taskStatus' = IN_PROGRESS of some document's task

        if (!getMyTasks().isEmpty() || getPermissionService().hasPermission(docRef, DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA) == AccessStatus.ALLOWED) {
            return WORKFLOW_METHOD_BINDING_NAME;
        }
        return null;
    }

    public void saveTask(ActionEvent event) {
        Integer index = (Integer) event.getComponent().getAttributes().get(ATTRIB_INDEX);
        try {
            getWorkflowService().saveInProgressTask(getMyTasks().get(index));
            MessageUtil.addInfoMessage("save_success");
        } catch (WorkflowChangedException e) {
            log.debug("Saving task failed", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_task_save_failed");
        }
        restore();
    }

    public void finishTask(ActionEvent event) {
        Integer index = (Integer) event.getComponent().getAttributes().get(ATTRIB_INDEX);
        Integer outcomeIndex = (Integer) event.getComponent().getAttributes().get(ATTRIB_OUTCOME_INDEX);
        Task task = getMyTasks().get(index);
        QName taskType = task.getNode().getType();

        if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)
                || WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
            outcomeIndex = (Integer) task.getNode().getProperties().get(WorkflowSpecificModel.Props.TEMP_OUTCOME.toString());
        } else if (WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)) {
            if (outcomeIndex == 1) {

                // signing requires that at least 1 active file exists within this document
                long step0 = System.currentTimeMillis();
                List<File> activeFiles = getFileService().getAllActiveFiles(docRef);
                if (activeFiles == null || activeFiles.isEmpty()) {
                    MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "task_files_required");
                    return;
                }

                signatureTask = (SignatureTask) task;
                try {
                    long step1 = System.currentTimeMillis();
                    getDocumentService().prepareDocumentSigning(docRef);
                    long step2 = System.currentTimeMillis();
                    fileBlockBean.restore();
                    showModal();
                    long step3 = System.currentTimeMillis();
                    if (log.isInfoEnabled()) {
                        log.info("prepareDocumentSigning took total time " + (step3 - step0) + " ms\n    load file list - " + (step1 - step0)
                                + " ms\n    service call - " + (step2 - step1) + " ms\n    reload file list - " + (step3 - step2) + " ms");
                    }
                } catch (UnableToPerformException e) {
                    if (MessageUtil.addStatusMessage(e)) {
                        return;
                    }
                }
                MessageUtil.addInfoMessage("task_finish_success_defaultMsg");
                return;
            }
        }

        List<Pair<String, String>> validationMsgs = null;
        if ((validationMsgs = validate(task, outcomeIndex)) != null) {
            for (Pair<String, String> validationMsg : validationMsgs) {
                if (validationMsg.getSecond() == null) {
                    MessageUtil.addErrorMessage(validationMsg.getFirst());
                } else {
                    MessageUtil.addErrorMessage(validationMsg.getFirst(), validationMsg.getSecond());
                }

            }
            return;
        }

        // finish the task
        try {
            getWorkflowService().finishInProgressTask(task, outcomeIndex);
            MessageUtil.addInfoMessage("task_finish_success_defaultMsg");
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "task_finish_error_docDeleted");
            context.getApplication().getNavigationHandler().handleNavigation(context, null, AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME);
            return;
        } catch (WorkflowChangedException e) {
            log.debug("Finishing task failed", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_task_save_failed");
        }
        restore();
        metadataBlockBean.viewDocument(getDocumentService().getDocument(metadataBlockBean.getDocument().getNodeRef()));
    }

    @SuppressWarnings("unchecked")
    private List<Pair<String, String>> validate(Task task, Integer outcomeIndex) {
        QName taskType = task.getNode().getType();
        if (WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)) {
            if (outcomeIndex == 0 && StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_signatureTask_comment", null));
            }
        } else if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)) {
            if ((outcomeIndex == 1 || outcomeIndex == 2) && StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_reviewTask_comment", null));
            }
            if (DocumentSubtypeModel.Types.INVOICE.equals(document.getType())) {
                return checkTransactionCostManagers();
            }
        } else if (WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
            if (outcomeIndex == 1 && StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_externalReviewTask_comment", null));
            }
        } else if (WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(taskType)) {
            if (StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_assignmentTask_comment", null));
            }
        } else if (WorkflowSpecificModel.Types.OPINION_TASK.equals(taskType)) {
            if (StringUtils.isBlank(task.getComment()) && task.getProp(WorkflowSpecificModel.Props.FILE) == null) {
                return Arrays.asList(new Pair<String, String>("task_validation_opinionTask_comment", null));
            }
        }
        return null;
    }

    private List<Pair<String, String>> checkTransactionCostManagers() {
        List<Transaction> transactions = transactionsBlockBean.getTransactions();
        if (transactions == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<String> relatedFundsCenters = (List<String>) BeanHelper.getNodeService().getProperty(userService.getCurrentUser(), ContentModel.PROP_RELATED_FUNDS_CENTER);
        if (relatedFundsCenters == null || relatedFundsCenters.isEmpty()) {
            return null;
        }
        List<String> mandatoryForCostManager = BeanHelper.getEInvoiceService().getCostManagerMandatoryFields();
        if (mandatoryForCostManager.isEmpty()) {
            return null;
        }
        List<Pair<String, String>> errorMessages = new ArrayList<Pair<String, String>>();
        List<String> addedErrorKeys = new ArrayList<String>();
        for (Transaction transaction : transactions) {
            String costCenter = transaction.getFundsCenter();
            for (String relatedFundsCenter : relatedFundsCenters) {
                if (costCenter != null && costCenter.equalsIgnoreCase(relatedFundsCenter)) {
                    EInvoiceUtil.checkTransactionMandatoryFields(mandatoryForCostManager, errorMessages, addedErrorKeys, transaction);
                }
            }
        }
        if (errorMessages.isEmpty()) {
            return null;
        }
        return errorMessages;
    }

    public List<CompoundWorkflow> getCompoundWorkflows() {
        if (compoundWorkflows == null) {
            restore();
        }
        return compoundWorkflows;
    }

    public List<Task> getMyTasks() {
        if (myTasks == null) {
            restore();
        }
        return myTasks;
    }

    public List<Task> getFinishedReviewTasks() {
        if (finishedReviewTasks == null) {
            restore();
        }
        return finishedReviewTasks;
    }

    public List<Task> getFinishedOpinionTasks() {
        if (finishedOpinionTasks == null) {
            restore();
        }
        return finishedOpinionTasks;
    }

    public boolean getReviewNoteBlockRendered() {
        return getFinishedReviewTasks().size() != 0;
    }

    public boolean getOpinionNoteBlockRendered() {
        return getFinishedOpinionTasks().size() != 0;
    }

    public String getWorkflowMenuLabel() {
        return MessageUtil.getMessage(MSG_WORKFLOW_ACTION_GROUP);
    }

    public void processCert() {
        @SuppressWarnings("unchecked")
        Map<String, String> requestParameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String certHex = requestParameterMap.get("certHex");
        String certId = requestParameterMap.get("certId");
        try {
            long step0 = System.currentTimeMillis();
            SignatureDigest signatureDigest = getDocumentService().prepareDocumentDigest(docRef, certHex);
            long step1 = System.currentTimeMillis();
            showModal(signatureDigest.getDigestHex(), certId);
            signatureTask.setSignatureDigest(signatureDigest);
            if (log.isInfoEnabled()) {
                log.info("prepareDocumentDigest took total time " + (step1 - step0) + " ms\n    service call - " + (step1 - step0) + " ms");
            }
        } catch (SignatureException e) {
            SignatureBlockBean.addSignatureError(e);
            closeModal();
            signatureTask = null;
        }
    }

    public void signDocument() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String signatureHex = (String) facesContext.getExternalContext().getRequestParameterMap().get("signatureHex");

        try {
            long step0 = System.currentTimeMillis();
            getDocumentService().finishDocumentSigning(signatureTask, signatureHex);
            long step1 = System.currentTimeMillis();
            fileBlockBean.restore();
            long step2 = System.currentTimeMillis();
            metadataBlockBean.viewDocument(getDocumentService().getDocument(metadataBlockBean.getDocument().getNodeRef()));
            long step3 = System.currentTimeMillis();
            restore();
            long step4 = System.currentTimeMillis();
            if (log.isInfoEnabled()) {
                log.info("finishDocumentSigning took total time " + (step4 - step0) + " ms\n    service call - " + (step1 - step0)
                        + " ms\n    reload file list - " + (step2 - step1) + " ms\n    reload document - " + (step3 - step2) + " ms\n    reload workflows - "
                        + (step4 - step3) + " ms");
            }
            MessageUtil.addInfoMessage("task_finish_success_defaultMsg");
        } catch (WorkflowChangedException e) {
            log.debug("Finishing signature task failed", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_task_save_failed");
        } catch (SignatureRuntimeException e) {
            SignatureBlockBean.addSignatureError(e);
        } catch (FileExistsException e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to create ddoc, file with same name already exists, parentRef = " + docRef, e);
            }
            Utils.addErrorMessage(MessageUtil.getMessage("ddoc_file_exists"));
        } finally {
            closeModal();
            signatureTask = null;
        }
    }

    public void cancelSign() {
        metadataBlockBean.reloadDoc();
        closeModal();
        signatureTask = null;
    }

    /**
     * Callback to generate the drop down of outcomes for reviewTask.
     */
    public List<SelectItem> getReviewTaskOutcomes(FacesContext context, UIInput selectComponent) {
        int outcomes = getWorkflowService().getWorkflowTypes().get(WorkflowSpecificModel.Types.REVIEW_WORKFLOW).getTaskOutcomes();
        List<SelectItem> selectItems = new ArrayList<SelectItem>(outcomes);

        for (int i = 0; i < outcomes; i++) {
            String label = MessageUtil.getMessage("task_outcome_reviewTask" + i);
            selectItems.add(new SelectItem(i, label));
        }
        return selectItems;
    }

    public List<SelectItem> getExternalReviewTaskOutcomes(FacesContext context, UIInput selectComponent) {
        int outcomes = getWorkflowService().getWorkflowTypes().get(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW).getTaskOutcomes();
        List<SelectItem> selectItems = new ArrayList<SelectItem>(outcomes);

        for (int i = 0; i < outcomes; i++) {
            String label = MessageUtil.getMessage("task_outcome_externalReviewTask" + i);
            selectItems.add(new SelectItem(i, label));
        }
        return selectItems;
    }

    public void constructTaskPanelGroup() {
        constructTaskPanelGroup(getDataTableGroupInner());
    }

    /**
     * Manually generate a panel group with everything.
     */
    @SuppressWarnings("unchecked")
    private void constructTaskPanelGroup(HtmlPanelGroup panelGroup) {
        panelGroup.getChildren().clear();
        Application app = FacesContext.getCurrentInstance().getApplication();

        // add 2 hidden links and a modal applet so signing
        panelGroup.getChildren().add(new SignatureAppletModalComponent());
        panelGroup.getChildren().add(generateLinkWithParam(app, "processCert", "#{" + BEAN_NAME + ".processCert}", "cert"));
        panelGroup.getChildren().add(generateLinkWithParam(app, "signDocument", "#{" + BEAN_NAME + ".signDocument}", "signature"));
        panelGroup.getChildren().add(generateLinkWithParam(app, "cancelSign", "#{" + BEAN_NAME + ".cancelSign}", null));

        for (int index = 0; index < getMyTasks().size(); index++) {
            Task myTask = getMyTasks().get(index);
            Node node = myTask.getNode();
            QName taskType = node.getType();
            UIPropertySheet sheet = new WMUIPropertySheet();
            if (WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(taskType)) {
                // must use a copy of tasks workflow, as there might be at the same time 2 tasks of the same workflow for delegation
                Task myTaskCopy = WorkflowUtil.createTaskCopy(myTask);
                Pair<Integer, Task> delegatableTask = delegationBean.initDelegatableTask(myTaskCopy);
                int delegatableTaskIndex = delegatableTask.getFirst();
                putAttribute(sheet, DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, delegatableTaskIndex);
                myTask = delegatableTask.getSecond();// first copy of myTask - stored in delegationBean and used in propertySheet
                getMyTasks().set(index, myTask);
                node = myTask.getNode();
            }

            // the main block panel
            UIPanel panel = new UIPanel();
            panel.setId("workflow-task-block-panel-" + node.getId());
            panel.setLabel(MessageUtil.getMessage("task_title_main") + MessageUtil.getMessage("task_title_" + taskType.getLocalName()));
            panel.setProgressive(true);
            panel.getAttributes().put("styleClass", "panel-100 workflow-task-block");

            // the properties
            sheet.setId("task-sheet-" + node.getId());
            sheet.setNode(node);
            // this ensures we can use more than 1 property sheet on the page
            sheet.setVar("taskNode" + index);
            sheet.getAttributes().put("externalConfig", Boolean.TRUE);
            sheet.getAttributes().put("labelStyleClass", "propertiesLabel");
            sheet.getAttributes().put("columns", 1);
            sheet.setRendererType(PropertySheetGridRenderer.class.getCanonicalName());
            panel.getChildren().add(sheet);

            HtmlPanelGroup panelGrid = new HtmlPanelGroup();
            panelGrid.setStyleClass("task-sheet-buttons");
            // panel grid with a column for every button

            // save button used only for some task types
            if (WorkflowSpecificModel.Types.OPINION_TASK.equals(taskType) ||
                    WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType) ||
                    WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {

                // the save button
                HtmlCommandButton saveButton = new HtmlCommandButton();
                saveButton.setId("save-id-" + node.getId());
                saveButton.setActionListener(app.createMethodBinding("#{DocumentDialog.workflow.saveTask}", new Class[] { ActionEvent.class }));
                saveButton.setValue(MessageUtil.getMessage("task_save_" + taskType.getLocalName()));
                saveButton.setStyleClass("taskOutcome");
                saveButton.getAttributes().put(ATTRIB_INDEX, index);

                panelGrid.getChildren().add(saveButton);
            }

            // the outcome buttons
            String label = "task_outcome_" + node.getType().getLocalName();
            for (int outcomeIndex = 0; outcomeIndex < myTask.getOutcomes(); outcomeIndex++) {

                HtmlCommandButton outcomeButton = new HtmlCommandButton();
                outcomeButton.setId("outcome-id-" + index + "-" + outcomeIndex);
                outcomeButton.setActionListener(app.createMethodBinding("#{DocumentDialog.workflow.finishTask}", new Class[] { ActionEvent.class }));
                outcomeButton.setValue(MessageUtil.getMessage(label + outcomeIndex));
                Map<String, Object> outcomeBtnAttributes = ComponentUtil.putAttribute(outcomeButton, "styleClass", "taskOutcome");
                outcomeBtnAttributes.put(ATTRIB_INDEX, index);
                outcomeBtnAttributes.put(ATTRIB_OUTCOME_INDEX, outcomeIndex);

                panelGrid.getChildren().add(outcomeButton);

                // the review and external review task has only 1 button and the outcomes come from TEMP_OUTCOME property
                if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)
                        || WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
                    // node.getProperties().put(WorkflowSpecificModel.Props.TEMP_OUTCOME.toString(), 0);
                    outcomeButton.setValue(MessageUtil.getMessage(label));
                    break;
                }
            }
            panel.getChildren().add(panelGrid);
            panelGroup.getChildren().add(panel);
        }
    }

    private HtmlCommandLink generateLinkWithParam(Application app, String linkId, String methodBinding, String paramName) {
        HtmlCommandLink link = new HtmlCommandLink();
        link.setId(linkId);
        link.setAction(app.createMethodBinding(methodBinding, new Class[] {}));
        link.setStyle("display: none");

        if (paramName != null) {
            UIParameter param = new UIParameter();
            param.setId(linkId + "-param");
            param.setName(paramName);
            param.setValue("");
            @SuppressWarnings("unchecked")
            List<UIComponent> children = link.getChildren();
            children.add(param);
        }

        return link;
    }

    private void showModal() {
        getModalApplet().showModal();
    }

    private void showModal(String digestHex, String certId) {
        getModalApplet().showModal(digestHex, certId);
    }

    private void closeModal() {
        getModalApplet().closeModal();
    }

    private SignatureAppletModalComponent getModalApplet() {
        return (SignatureAppletModalComponent) getDataTableGroupInner().getChildren().get(0);
    }

    private boolean checkRights(Workflow workflow) {
        boolean localRights = getUserService().isDocumentManager()
                || getDocumentService().isDocumentOwner(docRef, AuthenticationUtil.getRunAsUser())
                || getWorkflowService().isOwner(workflow.getParent())
                || getWorkflowService().isOwnerOfInProgressAssignmentTask(workflow.getParent());
        boolean externalReviewRights = !workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)
                || !Boolean.TRUE.equals(document.getProperties().get(DocumentSpecificModel.Props.NOT_EDITABLE))
                || !hasCurrentInstitutionTask(workflow);
        return localRights && externalReviewRights;
    }

    private boolean hasCurrentInstitutionTask(Workflow wrkflw) {
        for (Workflow workflow : wrkflw.getParent().getWorkflows()) {
            if (workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
                for (Task task : workflow.getTasks()) {
                    if (task.getInstitutionCode().equals(getDvkService().getInstitutionCode())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // START: getters / setters
    public void setFileBlockBean(FileBlockBean fileBlockBean) {
        this.fileBlockBean = fileBlockBean;
    }

    public void setMetadataBlockBean(MetadataBlockBean metadataBlockBean) {
        this.metadataBlockBean = metadataBlockBean;
    }

    public void setTransactionsBlockBean(TransactionsBlockBean transactionsBlockBean) {
        this.transactionsBlockBean = transactionsBlockBean;
    }

    public void setDelegationBean(DelegationBean delegationBean) {
        this.delegationBean = delegationBean;
    }

    // NB! Don't call this method from java code; this is meant ONLY for workflow-block.jsp binding
    public HtmlPanelGroup getDataTableGroup() {
        if (dataTableGroup == null) {
            dataTableGroup = new HtmlPanelGroup();
        }
        taskPanelControlDocument = docRef;
        return dataTableGroup;
    }

    private HtmlPanelGroup getDataTableGroupInner() {
        // This will be called once in the first RESTORE VIEW phase.
        if (dataTableGroup == null) {
            dataTableGroup = new HtmlPanelGroup();
        }
        return dataTableGroup;
    }

    public void setDataTableGroup(HtmlPanelGroup dataTableGroup) {
        if (taskPanelControlDocument != null && !taskPanelControlDocument.equals(docRef)) {
            constructTaskPanelGroup(dataTableGroup);
            taskPanelControlDocument = docRef;
        }
        this.dataTableGroup = dataTableGroup;
    }

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(UserService.BEAN_NAME);
        }
        return userService;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    protected WorkflowService getWorkflowService() {
        if (workflowService == null) {
            workflowService = (WorkflowService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    WorkflowService.BEAN_NAME);
        }
        return workflowService;
    }

    protected FileService getFileService() {
        if (fileService == null) {
            fileService = (FileService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(FileService.BEAN_NAME);
        }
        return fileService;
    }

    protected SignatureService getSignatureService() {
        if (signatureService == null) {
            signatureService = (SignatureService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    SignatureService.BEAN_NAME);
        }
        return signatureService;
    }

    protected DvkService getDvkService() {
        if (dvkService == null) {
            dvkService = (DvkService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    DvkService.BEAN_NAME);
        }
        return dvkService;
    }

    public List<WorkflowBlockItem> getWorkflowBlockItems() {
        List<WorkflowBlockItemGroup> workflows = new ArrayList<WorkflowBlockItemGroup>();
        for (CompoundWorkflow cWf : getCompoundWorkflows()) {
            List<WorkflowBlockItem> items = new ArrayList<WorkflowBlockItem>();
            final List<Workflow> wfs = cWf.getWorkflows();
            for (Workflow wf : wfs) {
                if (WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW.equals(wf.getNode().getType())
                        || WorkflowUtil.isGeneratedByDelegation(wf)) {
                    continue; // Don't display registration workflows
                    // nor workflows that have been temporarily created when assignment task is shown that can potentially be delegated
                    // using those workflows for information,opinion tasks
                }
                boolean raisedRights = checkRights(wf);
                for (Task task : wf.getTasks()) {
                    items.add(new WorkflowBlockItem(task, raisedRights));
                }
            }
            Collections.sort(items, WorkflowBlockItem.COMPARATOR);
            workflows.add(new WorkflowBlockItemGroup(items, wfs.size()));
        }

        if (workflows.isEmpty()) {
            return Collections.<WorkflowBlockItem> emptyList();
        }

        // Sort by workflows
        Collections.sort(workflows, WorkflowBlockItemGroup.COMPARATOR);

        // Flatten the structure.
        List<WorkflowBlockItem> items = new ArrayList<WorkflowBlockItem>();
        for (WorkflowBlockItemGroup workflowBlockItemGroup : workflows) {
            items.addAll(workflowBlockItemGroup.getItems());
            items.add(new WorkflowBlockItem(true));
            items.add(new WorkflowBlockItem(false));
        }
        items.remove(items.size() - 1); // remove two last ones
        items.remove(items.size() - 1);
        return items;
    }

    // END: getters / setters

}
