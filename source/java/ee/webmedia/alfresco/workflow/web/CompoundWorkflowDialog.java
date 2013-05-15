package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getCompoundWorkflowFavoritesService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowBlockBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.parameters.model.Parameters.MAX_ATTACHED_FILE_SIZE;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.isAdminOrDocmanagerWithPermission;
import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.TASK_INDEX;
import static ee.webmedia.alfresco.workflow.web.TaskListGenerator.WF_INDEX;
import static org.apache.commons.lang.time.DateUtils.isSameDay;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIPanel;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.dialog.DialogState;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.casefile.web.CaseFileDialog;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerWithDueDateGenerator;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent.ModalLayerSubmitEvent;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.Confirmable;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.search.web.AbstractSearchBlockBean;
import ee.webmedia.alfresco.document.search.web.BlockBeanProviderProvider;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.web.FavoritesModalComponent;
import ee.webmedia.alfresco.document.web.FavoritesModalComponent.AddToFavoritesEvent;
import ee.webmedia.alfresco.dvk.service.ReviewTaskException;
import ee.webmedia.alfresco.dvk.service.ReviewTaskException.ExceptionType;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;
import ee.webmedia.alfresco.notification.exception.EmailAttachmentSizeLimitException;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.user.model.UserModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException.ErrorCause;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
import ee.webmedia.alfresco.workflow.service.OrderAssignmentWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Task.Action;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowServiceImpl;
import ee.webmedia.alfresco.workflow.service.WorkflowServiceImpl.DialogAction;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.web.evaluator.WorkflowNewEvaluator;

/**
 * Dialog bean for working with one compound workflow instance which is tied to a document.
 * 
 * @author Erko Hansar
 */
public class CompoundWorkflowDialog extends CompoundWorkflowDefinitionDialog implements Confirmable, BlockBeanProviderProvider {

    private static final String CONTINUE_VALIDATED_WORKFLOW = "CompoundWorkflowDialog.continueValidatedWorkflow";
    private static final String START_VALIDATED_WORKFLOW = "CompoundWorkflowDialog.startValidatedWorkflow";
    private static final String STOP_VALIDATED_WORKFLOW = "CompoundWorkflowDialog.stopValidatedWorkflow";
    private static final String SAVE_VALIDATED_WORKFLOW = "CompoundWorkflowDialog.saveValidatedWorkflow";
    private static final String DELETE = "deleteWorkflow";
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CompoundWorkflowDialog";

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(CompoundWorkflowDialog.class);

    private transient DocumentService documentService;
    private transient DocumentLogService documentLogService;
    private transient ParametersService parametersService;
    private String existingUserCompoundWorkflowDefinition;
    private String newUserCompoundWorkflowDefinition;
    private boolean showEmptyWorkflowMessage = true;
    private boolean disableDocumentUpdate;
    private boolean finishImplConfirmed;

    private static final List<QName> knownWorkflowTypes = Arrays.asList(//
            WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW
            , WorkflowSpecificModel.Types.OPINION_WORKFLOW
            , WorkflowSpecificModel.Types.REVIEW_WORKFLOW
            , WorkflowSpecificModel.Types.INFORMATION_WORKFLOW
            , WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW
            , WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW
            , WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_WORKFLOW
            );
    public static final String MODAL_KEY_ENTRY_COMMENT = "popup_comment";
    private String renderedModal;
    private transient UIPanel modalContainer;

    /**
     * @param propSheet
     * @return true if "{temp}workflowTasks" property should be shown on given propertySheet
     */
    public boolean showAssignmentWorkflowWorkflowTasks(UIPropertySheet propSheet) {
        final int index = (Integer) propSheet.getAttributes().get(TaskListGenerator.ATTR_WORKFLOW_INDEX);
        final Workflow workflow2 = getWorkflow().getWorkflows().get(index);
        final List<Task> tasks = workflow2.getTasks();
        for (Task task : tasks) {
            if (WorkflowUtil.isActiveResponsible(task)) {
                return true; // this workflow has at least one active responsibility task
            }
        }
        return false; // this workflow has no active responsibility tasks
    }

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        resetPathForCompoundWorkflow();
    }

    @Override
    public void restored() {
        initBlocks(true, true);
    }

    @Override
    public boolean canRestore() {
        return compoundWorkflow != null && (RepoUtil.isUnsaved(compoundWorkflow.getNodeRef()) || getNodeService().exists(compoundWorkflow.getNodeRef()));
    }

    private void initBlocks() {
        initBlocks(false, true);
    }

    private void initBlocks(boolean fromRestore, boolean initWorkflowBlockBean) {
        if (initWorkflowBlockBean) {
            initWorkflowBlockBean();
        }
        if (!fromRestore) {
            BeanHelper.getCompoundWorkflowAssocListDialog().setup(compoundWorkflow);
            if (initWorkflowBlockBean) {
                getWorkflowBlockBean().resetSigningData();
            }
        }
        getSearch().initSearch(compoundWorkflow != null ? compoundWorkflow.getNodeRef() : null, "#{CompoundWorkflowDialog.showAssocSearchObjectType}");
        getLog().init(compoundWorkflow);
        if (!fromRestore || compoundWorkflow.getNode().isSaved()) {
            BeanHelper.getRelatedUrlListBlock().setup(compoundWorkflow);
        }
        DialogDataProvider dataProvider = BeanHelper.getDocumentDialogHelperBean().getDataProvider();
        if (dataProvider instanceof CaseFileDialog && dataProvider.getCaseFile() != null && ((CaseFileDialog) dataProvider).canRestore()) {
            dataProvider.switchMode(false);
        }
    }

    public void initWorkflowBlockBean() {
        getWorkflowBlockBean().initIndependentWorkflow(compoundWorkflow, this);
    }

    public boolean isShowAssocSearchObjectType() {
        return false;
    }

    private void resetPathForCompoundWorkflow() {
        // CompoundWorkflowDialog doesn't support multiple instances of same dialog in view stack,
        // so drop previous path items if they refer to the same dialog
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        @SuppressWarnings("unchecked")
        Stack<Object> viewStack = (Stack<Object>) sessionMap.get(UIMenuComponent.VIEW_STACK);
        int pathItemsDropped = 0;
        String compoundWorkflowDialogName = "compoundWorkflowDialog";
        Object pathItem = viewStack.peek();
        while (pathItem instanceof DialogState && compoundWorkflowDialogName.equals(((DialogState) pathItem).getConfig().getName())) {
            viewStack.pop();
            pathItemsDropped++;
            if (viewStack.isEmpty()) {
                break;
            }
            pathItem = viewStack.peek();
        }
        BeanHelper.getMenuBean().removeBreadcrumbItems(pathItemsDropped);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        return saveWorkflow(context, outcome);
    }

    protected String saveWorkflow(FacesContext context, String outcome) {
        return saveWorkflow(context, null, null, outcome);
    }

    protected String saveWorkflow(FacesContext context, String workflowBlockCallback, List<Pair<String, Object>> params, String outcome) {
        boolean isInProgress = WorkflowUtil.isStatus(compoundWorkflow, Status.IN_PROGRESS);
        preprocessWorkflow();
        boolean checkConfirmations = !finishImplConfirmed;
        finishImplConfirmed = false;
        if (isInProgress && hasOwnerWithNoEmail("workflow_compound_save_failed_owner_without_email")) {
            return null;
        }
        boolean hasWorkflowBlockCallback = workflowBlockCallback != null;
        if (validate(context, isInProgress, false, hasWorkflowBlockCallback)) {
            List<String> confirmationMessages = checkConfirmations ? getConfirmationMessages(false) : null;

            if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                updatePanelGroup(confirmationMessages, hasWorkflowBlockCallback ? workflowBlockCallback : SAVE_VALIDATED_WORKFLOW, true, true, params, !hasWorkflowBlockCallback);
                return null;
            }
            return saveOrConfirmValidatedWorkflow(!isDocumentWorkflow() ? null : outcome, hasWorkflowBlockCallback);
        }
        return null;
    }

    public void saveValidatedWorkflow(ActionEvent event) {
        finishImplConfirmed = true;
        String outcome = finish();
        if (StringUtils.isNotBlank(outcome)) {
            WebUtil.navigateTo(outcome);
        }
    }

    protected String saveOrConfirmValidatedWorkflow(String originalOutcome, boolean finishingTask) {
        String confirmationOutcome = askConfirmIfHasSameTask(MessageUtil.getMessage("workflow_compound_save"), DialogAction.SAVING, false);
        if (confirmationOutcome == null) {
            confirmationOutcome = originalOutcome;
            boolean saveSucceeded = saveCompWorkflow();
            if (!saveSucceeded) {
                confirmationOutcome = null;
            }
            updatePanelGroup(null, null, true, true, null, !finishingTask);
            initBlocks(false, !finishingTask);
            if (finishingTask) {
                return "SAVED";
            }
        }
        return confirmationOutcome;
    }

    @Override
    protected void resetState(boolean resetPanelGroup) {
        super.resetState(resetPanelGroup);
        existingUserCompoundWorkflowDefinition = null;
        newUserCompoundWorkflowDefinition = null;
        showEmptyWorkflowMessage = true;
        disableDocumentUpdate = false;
        resetModals();
    }

    private void resetModals() {
        renderedModal = null;
        // Add favorite modal component
        FavoritesModalComponent favoritesModal = new FavoritesModalComponent();
        CompoundWorkflowLinkGeneratorModalComponent linkModal = new CompoundWorkflowLinkGeneratorModalComponent();
        final FacesContext context = FacesContext.getCurrentInstance();
        final Application application = context.getApplication();
        favoritesModal.setActionListener(application.createMethodBinding("#{CompoundWorkflowDialog.addCompoundWorkflowToFavorites}", UIActions.ACTION_CLASS_ARGS));
        favoritesModal.setId("favorite-popup-" + context.getViewRoot().createUniqueId());
        linkModal.setId("link-popup-" + context.getViewRoot().createUniqueId());
        List<UIComponent> children = ComponentUtil.getChildren(getModalContainer());
        children.clear();
        children.add(favoritesModal);
        children.add(linkModal);
    }

    private boolean hasOwnerWithNoEmail(String messageKey) {
        List<String> ownersWithNoEmail = WorkflowUtil.getOwnersWithNoEmailForNotFinishedTasks(compoundWorkflow);
        if (!ownersWithNoEmail.isEmpty()) {
            for (String owner : ownersWithNoEmail) {
                MessageUtil.addErrorMessage(messageKey, owner);
            }
            MessageUtil.addErrorMessage("workflow_compound_contact_administrator");
            return true;
        }
        return false;
    }

    private String askConfirmIfHasSameTask(String title, DialogAction requiredAction, boolean navigate) {
        if (!compoundWorkflow.isDocumentWorkflow()) {
            return null;
        }
        Set<Pair<String, QName>> hasSameTask = WorkflowUtil.haveSameTask(compoundWorkflow, getWorkflowService().getOtherCompoundWorkflows(compoundWorkflow));
        if (!hasSameTask.isEmpty()) {
            ArrayList<MessageData> messageDataList = new ArrayList<MessageData>();
            String msgKey = "workflow_compound_confirm_same_task";
            for (Pair<String, QName> ownerNameTypePair : hasSameTask) {
                MessageData msgData = new MessageDataImpl(MessageSeverity.WARN, msgKey, ownerNameTypePair.getFirst(), MessageUtil.getTypeName(ownerNameTypePair.getSecond()));
                messageDataList.add(msgData);
            }
            messageDataList.add(new MessageDataImpl(MessageSeverity.WARN, "workflow_compound_confirm_continue"));
            BeanHelper.getConfirmDialog().setupConfirmDialog(this, messageDataList, title, requiredAction);
            isFinished = false;
            String navigationOutcome = "dialog:confirmDialog";
            if (navigate) {
                WebUtil.navigateTo(navigationOutcome, null);
            }
            return navigationOutcome;
        }
        return null;
    }

    private boolean saveCompWorkflow() {
        try {
            preprocessWorkflow();
            compoundWorkflow = getWorkflowService().saveCompoundWorkflow(compoundWorkflow);
            isUnsavedWorkFlow = false;
            setReviewTaskDvkInfoMessages();
            MessageUtil.addInfoMessage("save_success");
            return true;
        } catch (NodeLockedException e) {
            log.debug("Compound workflow action failed: document locked!", e);
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("workflow_compound_save_failed_docLocked", e.getNodeRef());
        } catch (Exception e) {
            handleException(e, null);
        }
        return false;
    }

    private void setReviewTaskDvkInfoMessages() {
        for (Pair<String, Object[]> message : compoundWorkflow.getReviewTaskDvkInfoMessages()) {
            MessageUtil.addInfoMessage(message.getFirst(), message.getSecond());
        }
        compoundWorkflow.getReviewTaskDvkInfoMessages().clear();
    }

    /**
     * Action listener for JSP.
     */
    @Override
    public void setupWorkflow(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        setupWorkflow(nodeRef);
    }

    private void setupWorkflow(NodeRef nodeRef) {
        resetState(false);
        if (!checkExists(nodeRef)) {
            navigateCancel();
            return;
        }
        CompoundWorkflow tmpCompoundWorkflow = getWorkflowService().getCompoundWorkflow(nodeRef);
        if (!checkPermissions(tmpCompoundWorkflow)) {
            navigateCancel();
            return;
        }
        compoundWorkflow = tmpCompoundWorkflow;
        BeanHelper.getLogService().addLogEntry(
                LogEntry.create(LogObject.COMPOUND_WORKFLOW, BeanHelper.getUserService(), compoundWorkflow.getNodeRef(), "applog_compoundWorkflow_view"));
        addLargeWorkflowWarning();
        updateFullAccess();
        initExpandedStatuses();
        initBlocks();
        if (panelGroup != null) {
            updatePanelGroup(null, null, true, false, null, false);
        }
        disableDocumentUpdate = false;
    }

    private void navigateCancel() {
        WebUtil.navigateTo(getDefaultCancelOutcome(), FacesContext.getCurrentInstance());
    }

    private boolean checkPermissions(CompoundWorkflow cmpWorkflow) {
        if (!cmpWorkflow.isIndependentWorkflow()) {
            return true;
        }
        boolean hasPermissions = isOwnerOrDocManager(cmpWorkflow) || hasNotNewTask(cmpWorkflow) || hasDocumentPermissions(cmpWorkflow);
        if (!hasPermissions) {
            MessageUtil.addErrorMessage("workflow_compound_edit_error_no_permissions");
        }

        return hasPermissions;
    }

    private boolean hasDocumentPermissions(CompoundWorkflow cmpWorkflow) {
        List<Document> documents = getWorkflowService().getCompoundWorkflowDocuments(cmpWorkflow.getNodeRef());
        if (documents != null) {
            for (Document document : documents) {
                if (document.hasPermission(DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA) && document.hasPermission(DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasNotNewTask(CompoundWorkflow cmpWorkflow) {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        for (Workflow workflow : cmpWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                if (!task.isStatus(Status.NEW) && StringUtils.equals(task.getOwnerId(), runAsUser)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkExists(NodeRef nodeRef) {
        boolean nodeExists = getNodeService().exists(nodeRef);
        if (!nodeExists) {
            MessageUtil.addErrorMessage("workflow_compound_edit_error_docDeleted");
        }
        return nodeExists;
    }

    protected void reload(NodeRef nodeRef, boolean resetExpandedData, boolean initWorkflowBlock) {
        compoundWorkflow = getWorkflowService().getCompoundWorkflow(nodeRef);
        if (resetExpandedData) {
            initExpandedStatuses();
        }
        updatePanelGroup(!resetExpandedData);
        initBlocks(false, initWorkflowBlock);
    }

    public void setupWorkflowFromList(ActionEvent event) {
        setupWorkflow(event);
        disableDocumentUpdate = true;
    }

    public void setupWorkflowFromList(NodeRef compoundWorkflowRef) {
        setupWorkflow(compoundWorkflowRef);
        disableDocumentUpdate = true;
    }

    /**
     * Action listener for JSP.
     */
    @Override
    public void setupNewWorkflow(ActionEvent event) {
        resetState();
        NodeRef compoundWorkflowDefinition = new NodeRef(ActionUtil.getParam(event, "compoundWorkflowDefinitionNodeRef"));
        NodeRef parentRef = new NodeRef(ActionUtil.getParam(event, "parentNodeRef"));
        try {
            compoundWorkflow = getWorkflowService().getNewCompoundWorkflow(compoundWorkflowDefinition, parentRef);
            if (CaseFileModel.Types.CASE_FILE.equals(BeanHelper.getNodeService().getType(parentRef))) {
                compoundWorkflow.setTitle((String) BeanHelper.getNodeService().getProperty(parentRef, DocumentDynamicModel.Props.DOC_TITLE));
            }
            addLargeWorkflowWarning();
            Workflow costManagerWorkflow = getCostManagerForkflow();
            if (costManagerWorkflow != null) {
                addCostManagerTasks(costManagerWorkflow);
            }
            updateFullAccess();
            initExpandedStatuses();
            initBlocks();
            setSignatureTaskOwnerProps();
            isUnsavedWorkFlow = true;
        } catch (InvalidNodeRefException e) {
            log.warn("Failed to create a new compound workflow instance because someone has probably deleted the compound workflow definition.");
        }
    }

    public void setupNewIndependentWorkflowFromDocument(ActionEvent event) {
        setupNewWorkflow(event);
        if (compoundWorkflow != null) {
            NodeRef documentRef = new NodeRef(ActionUtil.getParam(event, "docAssocNodeRef"));
            compoundWorkflow.setTitle((String) BeanHelper.getNodeService().getProperty(documentRef, DocumentCommonModel.Props.DOC_NAME));
            BeanHelper.getCompoundWorkflowAssocListDialog().restored();
            BeanHelper.getCompoundWorkflowAssocListDialog().setResetNewAssocs(false);
            BeanHelper.getCompoundWorkflowAssocSearchBlock().addAssocDocHandler(documentRef);
            List<Document> documents = BeanHelper.getCompoundWorkflowAssocListDialog().getDocuments();
            if (documents != null && !documents.isEmpty()) {
                documents.get(0).setMainDocument(Boolean.TRUE);
            }
        }
    }

    private void setSignatureTaskOwnerProps() {
        Map<String, Object> signatureTaskOwnerProps = loadSignatureTaskOwnerProps(compoundWorkflow.getParent());
        if (signatureTaskOwnerProps == null) {
            return;
        }
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (workflow.isType(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW) && !workflow.getTasks().isEmpty()) {
                workflow.getTasks().get(0).getNode().getProperties().putAll(signatureTaskOwnerProps);
            }
        }
    }

    private Map<String, Object> loadSignatureTaskOwnerProps(NodeRef docRef) {
        Map<String, Object> signatureTaskOwnerProps = null;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (workflow.isType(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)) {
                signatureTaskOwnerProps = TaskListGenerator.loadSignatureTaskOwnerProps(docRef);
                break;
            }
        }
        return signatureTaskOwnerProps;
    }

    private Workflow getCostManagerForkflow() {
        NodeRef docRef = compoundWorkflow.getParent();
        if (docRef == null || !DocumentSubtypeModel.Types.INVOICE.equals(BeanHelper.getNodeService().getType(docRef))) {
            return null;
        }
        Long costManagerWfIndex = BeanHelper.getParametersService().getLongParameter(Parameters.REVIEW_WORKFLOW_COST_MANAGER_WORKFLOW_NUMBER);
        if (costManagerWfIndex == null) {
            return null;
        }
        int reviewWfIndex = 0;
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (workflow.isType(WorkflowSpecificModel.Types.REVIEW_WORKFLOW)) {
                // parameter workflow index is 1-based (not 0-based)
                if (reviewWfIndex == costManagerWfIndex - 1) {
                    return workflow;
                }
                reviewWfIndex++;
            }
        }
        return null;
    }

    public void startWorkflow() {
        log.debug("startWorkflow");
        preprocessWorkflow();
        if (hasOwnerWithNoEmail("workflow_compound_start_failed_owner_without_email")) {
            return;
        }
        if (validate(FacesContext.getCurrentInstance(), true, true, false)) {
            List<String> confirmationMessages = getConfirmationMessages(true);
            if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                updatePanelGroup(confirmationMessages, START_VALIDATED_WORKFLOW, true, true, null, true);
                return;
            }
            if (askConfirmIfHasSameTask(MessageUtil.getMessage("workflow_compound_starting"), DialogAction.STARTING, true) == null) {
                startValidatedWorkflow(null);
            }
        }
    }

    /**
     * This method assumes that workflows has been validated
     */
    public void startValidatedWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        boolean succeeded = false;
        try {
            // clear panelGroup to avoid memory issues when working with large worflows
            resetPanelGroup(true);
            preprocessWorkflow();
            compoundWorkflow = getWorkflowService().startCompoundWorkflow(compoundWorkflow);
            isUnsavedWorkFlow = false;
            setReviewTaskDvkInfoMessages();
            MessageUtil.addInfoMessage("workflow_compound_start_success");
            succeeded = true;
        } catch (Exception e) {
            handleException(e, "workflow_compound_start_workflow_failed", START_VALIDATED_WORKFLOW);
        }
        initBlocks();
        swithModeIfDocumentWorkflow();
        if (succeeded && isDocumentWorkflow()) {
            WebUtil.navigateTo(getDefaultFinishOutcome());
        } else {
            // update only if we stay on same page
            updatePanelGroup(false);
        }
    }

    private boolean isDocumentWorkflow() {
        return compoundWorkflow == null || compoundWorkflow.isDocumentWorkflow();
    }

    private List<String> getConfirmationMessages(boolean checkDocumentDueDate) {
        List<String> messages = new ArrayList<String>();
        NodeService nodeService = BeanHelper.getNodeService();
        NodeRef docRef = compoundWorkflow.getParent();
        Date invoiceDueDate = null;
        Date notInvoiceDueDate = null;
        List<Document> independentCompWorkflowDocs = null;
        boolean documentWorkflow = compoundWorkflow.isDocumentWorkflow();
        boolean independentWorkflow = compoundWorkflow.isIndependentWorkflow();
        if (checkDocumentDueDate) {
            if (documentWorkflow) {
                if (SystematicDocumentType.INVOICE.isSameType((String) nodeService.getProperty(docRef, DocumentAdminModel.Props.OBJECT_TYPE_ID))) {
                    invoiceDueDate = (Date) nodeService.getProperty(docRef, DocumentSpecificModel.Props.INVOICE_DUE_DATE);
                } else {
                    notInvoiceDueDate = (Date) nodeService.getProperty(docRef, DocumentSpecificModel.Props.DUE_DATE);
                }
            } else if (independentWorkflow) {
                independentCompWorkflowDocs = BeanHelper.getCompoundWorkflowAssocListDialog().getDocuments();
            }
        }
        boolean addedDueDateInPastMsg = false;
        Date now = new Date(System.currentTimeMillis());
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                Date taskDueDate = task.getDueDate();
                if (taskDueDate != null) {
                    if (checkDocumentDueDate) {
                        if (documentWorkflow) {
                            if (invoiceDueDate != null) {
                                Date invoiceDueDateMinus3Days = DateUtils.addDays(invoiceDueDate, -3);
                                if (!isSameDay(invoiceDueDateMinus3Days, taskDueDate) && taskDueDate.after(invoiceDueDateMinus3Days)) {
                                    getAndAddMessage(messages, workflow, taskDueDate, "task_confirm_invoice_task_due_date", invoiceDueDate);
                                }
                            }
                            if (notInvoiceDueDate != null) {
                                if (!isSameDay(notInvoiceDueDate, taskDueDate) && taskDueDate.after(notInvoiceDueDate)) {
                                    getAndAddMessage(messages, workflow, taskDueDate, "task_confirm_not_invoice_task_due_date", notInvoiceDueDate);
                                }
                            }
                        } else if (independentWorkflow && independentCompWorkflowDocs != null && task.isStatus(Status.NEW)) {
                            for (Document document : independentCompWorkflowDocs) {
                                if (SystematicDocumentType.INVOICE.isSameType((String) document.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID))) {
                                    continue;
                                }
                                Date docDueDate = document.getDueDate();
                                if (docDueDate != null && !isSameDay(docDueDate, taskDueDate) && taskDueDate.after(docDueDate)) {
                                    DateFormat dateFormat = Utils.getDateFormat(FacesContext.getCurrentInstance());
                                    String invoiceTaskDueDateConfirmationMsg = MessageUtil.getMessage("task_confirm_independent_workflow_task_due_date",
                                            MessageUtil.getMessage(workflow.getType().getLocalName()),
                                            dateFormat.format(taskDueDate), document.getDocName(), dateFormat.format(docDueDate));
                                    messages.add(invoiceTaskDueDateConfirmationMsg);
                                }
                            }
                        }
                    }
                    if (!addedDueDateInPastMsg && task.isStatus(Status.NEW) && taskDueDate.before(now)) {
                        messages.add(MessageUtil.getMessage("task_confirm_due_date_in_past"));
                        addedDueDateInPastMsg = true;
                    }
                }
            }
        }
        return messages;
    }

    private void getAndAddMessage(List<String> messages, Workflow workflow, Date taskDueDate, String msgKey, Date date) {
        FacesContext fc = FacesContext.getCurrentInstance();
        DateFormat dateFormat = Utils.getDateFormat(fc);
        String invoiceTaskDueDateConfirmationMsg = MessageUtil.getMessage(msgKey,
                MessageUtil.getMessage(workflow.getType().getLocalName()),
                dateFormat.format(taskDueDate), dateFormat.format(date));
        messages.add(invoiceTaskDueDateConfirmationMsg);
    }

    /**
     * Action listener for JSP.
     */
    public void stopWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("stopWorkflow");
        try {
            preprocessWorkflow();
            if (validate(FacesContext.getCurrentInstance(), false, false, false)) {
                List<String> confirmationMessages = getConfirmationMessages(false);
                if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                    updatePanelGroup(confirmationMessages, STOP_VALIDATED_WORKFLOW, true, true, null, true);
                    return;
                }
                // clear panelGroup to avoid memory issues when working with large worflows
                resetPanelGroup(true);
                compoundWorkflow = getWorkflowService().stopCompoundWorkflow(compoundWorkflow);
                setReviewTaskDvkInfoMessages();
                MessageUtil.addInfoMessage("workflow_compound_stop_success");
            }
        } catch (Exception e) {
            handleException(e, "workflow_compound_stop_workflow_failed");
        }
        updatePanelGroup(false);
        initBlocks();
    }

    public void stopValidatedWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("stopValidatedWorkflow");
        try {
            // clear panelGroup to avoid memory issues when working with large worflows
            resetPanelGroup(true);
            preprocessWorkflow();
            compoundWorkflow = getWorkflowService().stopCompoundWorkflow(compoundWorkflow);
            setReviewTaskDvkInfoMessages();
            MessageUtil.addInfoMessage("workflow_compound_stop_success");
        } catch (Exception e) {
            handleException(e, "workflow_compound_stop_workflow_failed");
        }
        updatePanelGroup(false);
    }

    /**
     * Action listener for JSP.
     */
    public void continueWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("continueWorkflow");
        try {
            preprocessWorkflow();
            if (hasOwnerWithNoEmail("workflow_compound_continue_failed_owner_without_email")) {
                return;
            }
            if (validate(FacesContext.getCurrentInstance(), true, true, false)) {
                List<String> confirmationMessages = getConfirmationMessages(true);
                if (confirmationMessages != null && !confirmationMessages.isEmpty()) {
                    updatePanelGroup(confirmationMessages, CONTINUE_VALIDATED_WORKFLOW, true, true, null, true);
                    return;
                }
                if (askConfirmIfHasSameTask(MessageUtil.getMessage("workflow_compound_continuing"), DialogAction.CONTINUING, true) == null) {
                    continueValidatedWorkflow();
                }

            }
        } catch (Exception e) {
            handleException(e, "workflow_compound_continue_workflow_failed", CONTINUE_VALIDATED_WORKFLOW);
        }
    }

    /**
     * This method assumes that compound workflow has been validated
     */
    public void continueValidatedWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        continueValidatedWorkflow();
    }

    private void continueValidatedWorkflow() {
        try {
            // clear panelGroup to avoid memory issues when working with large worflows
            resetPanelGroup(true);
            preprocessWorkflow();
            compoundWorkflow = getWorkflowService().continueCompoundWorkflow(compoundWorkflow);
            setReviewTaskDvkInfoMessages();
            MessageUtil.addInfoMessage("workflow_compound_continue_success");
        } catch (Exception e) {
            // let calling method handle error
            handleException(e, "workflow_compound_continue_workflow_failed", CONTINUE_VALIDATED_WORKFLOW);
        }
        updatePanelGroup(false);
        initBlocks();
        swithModeIfDocumentWorkflow();
    }

    private void swithModeIfDocumentWorkflow() {
        if (compoundWorkflow.isDocumentWorkflow() && !disableDocumentUpdate) {
            BeanHelper.getDocumentDynamicDialog().switchMode(false); // document metadata might have changed (for example owner)
        }
    }

    /**
     * Action listener for JSP.
     */
    public void finishWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("finishWorkflow");
        try {
            preprocessWorkflow();
            if (validate(FacesContext.getCurrentInstance(), false, true, false)) {
                // clear panelGroup to avoid memory issues when working with large worflows
                resetPanelGroup(true);
                compoundWorkflow = getWorkflowService().finishCompoundWorkflow(compoundWorkflow);
                setReviewTaskDvkInfoMessages();
                MessageUtil.addInfoMessage("workflow_compound_finish_success");
            }
        } catch (Exception e) {
            handleException(e, "workflow_compound_finish_workflow_failed");
        }
        updatePanelGroup(false);
        initBlocks();
    }

    public void reopenWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("finishWorkflow");
        try {
            preprocessWorkflow();
            if (validate(FacesContext.getCurrentInstance(), false, true, false)) {
                compoundWorkflow = getWorkflowService().reopenCompoundWorkflow(compoundWorkflow);
                setReviewTaskDvkInfoMessages();
                MessageUtil.addInfoMessage("workflow_compound_reopen_success");
            }
        } catch (Exception e) {
            handleException(e, "workflow_compound_reopen_workflow_failed");
        }
        updatePanelGroup();
        initBlocks();
    }

    /**
     * Copy saved version of current compound workflow. Not saved changes of current compound workflow are lost.
     * Action listener for JSP.
     */
    public void copyWorkflow(@SuppressWarnings("unused") ActionEvent event) {
        log.debug("copyWorkflow");
        try {
            compoundWorkflow = getWorkflowService().copyAndResetCompoundWorkflow(compoundWorkflow.getNodeRef());
        } catch (Exception e) {
            handleException(e, "workflow_compound_copy_workflow_failed");
        }
        updatePanelGroup();
        initBlocks();
    }

    /**
     * Action for JSP.
     */
    public String deleteWorkflow() {
        log.debug("deleteWorkflow");
        try {
            preprocessWorkflow();
            getWorkflowService().deleteCompoundWorkflow(compoundWorkflow.getNodeRef(), true);
            resetState();
            MessageUtil.addInfoMessage("workflow_compound_delete_compound_success");
            // stay on same screen, no additional message is needed
            showEmptyWorkflowMessage = false;

        } catch (Exception e) {
            handleException(e, "workflow_compound_delete_workflow_failed", DELETE);
            return null;
        }
        return null;
    }

    /**
     * Callback method for workflow owner Search component.
     */
    public void setWorkfowOwner(String username) {
        compoundWorkflow.setOwnerId(username);
        compoundWorkflow.setOwnerName(getUserService().getUserFullName(username));
        Node user = getUserService().getUser(username);
        Map<String, Object> userProps = user.getProperties();
        compoundWorkflow.setProp(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME,
                (Serializable) BeanHelper.getUserService().getUserOrgPathOrOrgName(RepoUtil.toQNameProperties(userProps)));
        compoundWorkflow.setProp(WorkflowCommonModel.Props.OWNER_JOB_TITLE, (Serializable) userProps.get(ContentModel.PROP_JOBTITLE));
    }

    @Override
    public Object getActionsContext() {
        return getWorkflow();
    }

    /**
     * Action listener for JSP.
     */
    public void cancelWorkflowTask(ActionEvent event) {
        int wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);
        int taskIndex = ActionUtil.getParam(event, TASK_INDEX, Integer.class);
        log.debug("cancelWorkflowTask: " + wfIndex + ", " + taskIndex);
        Workflow block = compoundWorkflow.getWorkflows().get(wfIndex);
        Task task = block.getTasks().get(taskIndex);
        task.setAction(Action.UNFINISH);
        updatePanelGroup();
        initBlocks();
    }

    /**
     * Action listener for JSP.
     */
    public void finishWorkflowTask(ActionEvent event) {
        ModalLayerSubmitEvent commentEvent = (ModalLayerSubmitEvent) event;
        int index = (Integer) event.getComponent().getAttributes().get(TaskListGenerator.ATTR_WORKFLOW_INDEX);
        int taskIndex = commentEvent.getActionIndex();
        String comment = (String) commentEvent.getSubmittedValue(MODAL_KEY_ENTRY_COMMENT);
        log.debug("finishWorkflowTask: " + index + ", " + taskIndex + ", " + comment);
        if (StringUtils.isBlank(comment)) {
            return;
        }

        Workflow block = compoundWorkflow.getWorkflows().get(index);
        Task task = block.getTasks().get(taskIndex);
        task.setAction(Action.FINISH);
        task.setComment(comment);
        updatePanelGroup();
        initBlocks();
    }

    /** @param event */
    public void saveasCompoundWorkflowDefinition(ActionEvent event) {
        String userId = AuthenticationUtil.getRunAsUser();
        if (validateSaveasData()) {
            if (StringUtils.isNotBlank(newUserCompoundWorkflowDefinition)) {
                getWorkflowService().createCompoundWorkflowDefinition(compoundWorkflow, userId, newUserCompoundWorkflowDefinition);
            } else {
                getWorkflowService().overwriteExistingCompoundWorkflowDefinition(compoundWorkflow, userId, existingUserCompoundWorkflowDefinition);
            }
            BeanHelper.getMenuService().menuUpdated();
        }
        updatePanelGroup();
    }

    private boolean validateSaveasData() {
        if (StringUtils.isBlank(newUserCompoundWorkflowDefinition) && StringUtils.isBlank(existingUserCompoundWorkflowDefinition)) {
            MessageUtil.addErrorMessage("compoundWorkflow_definition_saveas_error_fields_empty");
            return false;
        }
        if (StringUtils.isNotBlank(newUserCompoundWorkflowDefinition) && StringUtils.isNotBlank(existingUserCompoundWorkflowDefinition)) {
            MessageUtil.addErrorMessage("compoundWorkflow_definition_saveas_error_both_fields_filled");
            return false;
        }
        if (StringUtils.isNotBlank(newUserCompoundWorkflowDefinition)) {
            if (getWorkflowService().getCompoundWorkflowDefinitionByName(newUserCompoundWorkflowDefinition, AuthenticationUtil.getRunAsUser(), true) != null) {
                MessageUtil.addErrorMessage("compoundWorkflow_definition_saveas_error_definition_exists");
                return false;
            }
        }
        return true;
    }

    public void deleteCompoundWorkflowDefinition(ActionEvent event) {
        if (StringUtils.isBlank(existingUserCompoundWorkflowDefinition)) {
            MessageUtil.addErrorMessage("compoundWorkflow_definition_delete_error_definition_not_selected");
            return;
        }
        getWorkflowService().deleteCompoundWorkflowDefinition(existingUserCompoundWorkflowDefinition, AuthenticationUtil.getRunAsUser());
        updatePanelGroup();
    }

    @SuppressWarnings("unchecked")
    public List<SelectItem> getUserCompoundWorkflowDefinitions(FacesContext context, UIInput component) {
        List<SelectItem> userCompoundWorkflowDefinitions = new ArrayList<SelectItem>();
        for (CompoundWorkflowDefinition compoundWorkflowDefinition : getWorkflowService().getUserCompoundWorkflowDefinitions(AuthenticationUtil.getRunAsUser())) {
            userCompoundWorkflowDefinitions.add(new SelectItem(compoundWorkflowDefinition.getName()));
        }
        Collections.sort(userCompoundWorkflowDefinitions, new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((SelectItem) input).getValue();
            }
        }, new NullComparator()));
        userCompoundWorkflowDefinitions.add(0, new SelectItem("", MessageUtil.getMessage("workflow_choose")));
        return userCompoundWorkflowDefinitions;
    }

    public void addNotification() {
        BeanHelper.getNotificationService().addNotificationAssocForCurrentUser(compoundWorkflow.getNodeRef(), getNotificationAssocType(), getNotificationAspect());
    }

    public void removeNotification() {
        BeanHelper.getNotificationService().removeNotificationAssocForCurrentUser(compoundWorkflow.getNodeRef(), getNotificationAssocType(), getNotificationAspect());
    }

    private QName getNotificationAspect() {
        return compoundWorkflow.isIndependentWorkflow() ? UserModel.Aspects.INDEPENDENT_WORKFLOW_NOTIFICATIONS : UserModel.Aspects.CASE_FILE_WORKFLOW_NOTIFICATIONS;
    }

    private QName getNotificationAssocType() {
        return compoundWorkflow.isIndependentWorkflow() ? UserModel.Assocs.INDEPENDENT_WORKFLOW_NOTIFICATION : UserModel.Assocs.CASE_FILE_WORKFLOW_NOTIFICATION;
    }

    public String getExistingUserCompoundWorkflowDefinition() {
        return existingUserCompoundWorkflowDefinition;
    }

    public void setExistingUserCompoundWorkflowDefinition(String existingUserCompoundWorkflowDefinition) {
        this.existingUserCompoundWorkflowDefinition = existingUserCompoundWorkflowDefinition;
    }

    public String getNewUserCompoundWorkflowDefinition() {
        return newUserCompoundWorkflowDefinition;
    }

    public void setNewUserCompoundWorkflowDefinition(String newUserCompoundWorkflowDefinition) {
        this.newUserCompoundWorkflowDefinition = newUserCompoundWorkflowDefinition;
    }

    public void calculateDueDate(ActionEvent event) {
        int wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);
        int taskIndex = ActionUtil.getParam(event, TASK_INDEX, Integer.class);
        Workflow block = compoundWorkflow.getWorkflows().get(wfIndex);
        Task task = block.getTasks().get(taskIndex);
        Integer dueDateDays = task.getDueDateDays();
        if (dueDateDays != null) {
            task.setDueDate(getNewDueDate(task.getPropBoolean(WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS), dueDateDays, task.getDueDate()));
        }
    }

    public void calculateTaskGroupDueDate(ActionEvent event) {
        String selectorId = ActionUtil.getParam(event, "selector");
        int wfIndex = ActionUtil.getParam(event, WF_INDEX, Integer.class);

        UIComponent selector = ComponentUtil.findComponentById(FacesContext.getCurrentInstance(), event.getComponent().getParent(), selectorId);
        List value = (List) ((HtmlSelectOneMenu) selector).getValue();
        if (value == null) {
            return;
        }

        TaskGroup taskGroup = findTaskGroup(event);
        Date existingDueDate = taskGroup.getDueDate();
        taskGroup.setDueDate(getNewDueDate((Boolean) value.get(1), (Integer) value.get(0), existingDueDate));

        // Set the due dates according to the group
        WorkflowUtil.setGroupTasksDueDates(taskGroup, getWorkflow().getWorkflows().get(wfIndex).getTasks());
    }

    private Date getNewDueDate(Boolean isWorkingDays, Integer dueDateDays, Date existingDueDate) {
        LocalDate newDueDate = DatePickerWithDueDateGenerator.calculateDueDate(isWorkingDays, dueDateDays);
        LocalTime newTime;
        if (existingDueDate != null) {
            newTime = new LocalTime(existingDueDate.getHours(), existingDueDate.getMinutes());
        } else {
            newTime = new LocalTime(23, 59);
        }

        return newDueDate.toDateTime(newTime).toDate();
    }

    @Override
    protected void preprocessWorkflow() {
        super.preprocessWorkflow();
        removeImproperDueDateDays();
        setNewTaskDueDateFromGroup();
        addNewAssocs();
        addNewRelatedUrls();
        addAssociatedDocumentsData();
    }

    private void addNewAssocs() {
        if (compoundWorkflow != null && !compoundWorkflow.isSaved() && compoundWorkflow.isIndependentWorkflow()) {
            compoundWorkflow.getNewAssocs().clear();
            compoundWorkflow.getNewAssocs().addAll(BeanHelper.getCompoundWorkflowAssocListDialog().getNewAssocs());
        }
    }

    private void addNewRelatedUrls() {
        if (compoundWorkflow != null && !compoundWorkflow.isSaved()) {
            compoundWorkflow.getNewRelatedUrls().clear();
            compoundWorkflow.getNewRelatedUrls().addAll(BeanHelper.getRelatedUrlListBlock().getRelatedUrls());
        }
    }

    private void addAssociatedDocumentsData() {
        if (!compoundWorkflow.isIndependentWorkflow()) {
            return;
        }
        List<NodeRef> documentsToSign = compoundWorkflow.getDocumentsToSign();
        documentsToSign.clear();
        compoundWorkflow.setMainDocument(null);
        for (Document document : BeanHelper.getCompoundWorkflowAssocListDialog().getDocuments()) {
            NodeRef docRef = document.getNodeRef();
            if (document.getMainDocument()) {
                compoundWorkflow.setMainDocument(docRef);
            }
            if (document.getDocumentToSign()) {
                documentsToSign.add(docRef);
            }
        }
    }

    private void removeImproperDueDateDays() {
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                if (task.isStatus(Status.NEW) && task.getDueDate() != null && task.getDueDateDays() != null) {
                    if (!DateUtils.isSameDay(task.getDueDate(),
                            DatePickerWithDueDateGenerator.calculateDueDate(task.getPropBoolean(WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS), task.getDueDateDays())
                                    .toDateMidnight().toDate())) {
                        task.setProp(WorkflowSpecificModel.Props.DUE_DATE_DAYS, null);
                        task.setProp(WorkflowSpecificModel.Props.IS_DUE_DATE_WORKING_DAYS, Boolean.FALSE); // reset to default value
                    }
                }
            }
        }
    }

    private void setNewTaskDueDateFromGroup() {
        FacesContext context = FacesContext.getCurrentInstance();
        Application app = context.getApplication();
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                if (task.isStatus(Status.NEW) && task.getDueDate() == null && task.getGroupDueDateVbString() != null) {
                    task.setDueDate((Date) app.createValueBinding(task.getGroupDueDateVbString()).getValue(context));
                }
            }
        }
    }

    public void addDateForAllTasks(ActionEvent event) {
        Workflow block = compoundWorkflow.getWorkflows().get(ActionUtil.getParam(event, WF_INDEX, Integer.class));
        if (block.isStatus(Status.NEW)) {
            Task selectedTask = block.getTasks().get(ActionUtil.getParam(event, TASK_INDEX, Integer.class));
            Date originDate = selectedTask.getDueDate();
            for (Task task : block.getTasks()) {
                if (task != selectedTask) {
                    task.setDueDate(originDate);
                }
            }
        }
    }

    public void addCompoundWorkflowToFavorites(ActionEvent event) {
        if (!(event instanceof AddToFavoritesEvent)) {
            renderedModal = FavoritesModalComponent.ADD_TO_FAVORITES_MODAL_ID;
            return;
        }
        getCompoundWorkflowFavoritesService().addFavorite(compoundWorkflow.getNodeRef(), ((AddToFavoritesEvent) event).getFavoriteDirectoryName(), true);
        renderedModal = null;
    }

    public void showCompoundWorkflowLink(ActionEvent event) {
        renderedModal = CompoundWorkflowLinkGeneratorModalComponent.COMPOUND_WORKFLOW_LINK_MODAL_ID;
    }

    public void removeFavorite(@SuppressWarnings("unused") ActionEvent event) {
        getCompoundWorkflowFavoritesService().removeFavorite(compoundWorkflow.getNodeRef());
    }

    public String getRenderedModal() {
        try {
            return renderedModal;
        } finally {
            renderedModal = null;
        }
    }

    public boolean isModalRendered() {
        return StringUtils.isNotBlank(renderedModal);
    }

    public UIPanel getModalContainer() {
        if (modalContainer == null) {
            modalContainer = new UIPanel();
        }
        return modalContainer;
    }

    public void setModalContainer(UIPanel modalContainer) {
        this.modalContainer = modalContainer;
    }

    // /// PROTECTED & PRIVATE METHODS /////

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        if (new WorkflowNewEvaluator().evaluate(compoundWorkflow)
                && !compoundWorkflow.getWorkflows().isEmpty()
                && (!compoundWorkflow.isIndependentWorkflow()
                || isOwnerOrDocManager())) {
            return Arrays.asList(new DialogButtonConfig("compound_workflow_start", null, "workflow_compound_start",
                    "#{CompoundWorkflowDialog.startWorkflow}", "false", null));

        }
        return Collections.<DialogButtonConfig> emptyList();
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return compoundWorkflow != null && (!compoundWorkflow.isIndependentWorkflow() || isOwnerOrDocManagerOrHasInProgressTask());
    }

    @Override
    protected Map<String, QName> getSortedTypes() {
        if (sortedTypes == null) {
            Map<String, QName> sortedTypes = new TreeMap<String, QName>();
            if (compoundWorkflow != null) {
                Document doc = getParentDocument();
                String docStatus = doc != null ? doc.getDocStatus() : null;
                boolean isDocStatusWorking = docStatus != null ? DocumentStatus.WORKING.getValueName().equals(docStatus) : false;
                boolean isDocumentWorkflow = compoundWorkflow.isDocumentWorkflow();
                boolean isIndependentWorkflow = compoundWorkflow.isIndependentWorkflow();
                boolean isUserWithAddRights = isIndependentWorkflow ? isOwnerOrDocManagerOrHasInProgressTask() : false;
                if (!isIndependentWorkflow || isUserWithAddRights) {
                    for (QName wfType : super.getSortedTypes().values()) {
                        boolean isAllowedType = true;
                        if (isDocumentWorkflow) {
                            isAllowedType = checkDocumentWorkflowType(wfType, doc, isDocStatusWorking);
                        } else if (isIndependentWorkflow) {
                            isAllowedType = checkIndependentWorkflowType(wfType);
                        } else {
                            isAllowedType = checkCaseFileWorkflowType(wfType);
                        }
                        if (isAllowedType) {
                            String tmpName = MessageUtil.getMessage(wfType.getLocalName());
                            sortedTypes.put(tmpName, wfType);
                        }
                    }
                }
            }
            this.sortedTypes = sortedTypes;
        }
        return sortedTypes;
    }

    public boolean isOwnerOrDocManager() {
        return isOwnerOrDocManager(compoundWorkflow);
    }

    private boolean isOwnerOrDocManager(CompoundWorkflow cmpWorkflow) {
        return getUserService().isDocumentManager() || isCompoundWorkflowOwner(cmpWorkflow);
    }

    private boolean isCompoundWorkflowOwner(CompoundWorkflow cmpWorkflow) {
        return cmpWorkflow != null && StringUtils.equals(cmpWorkflow.getOwnerId(), AuthenticationUtil.getRunAsUser());
    }

    public boolean isOwnerOrDocManagerOrHasInProgressTask() {
        return isOwnerOrDocManager() || hasInProgressTask();
    }

    public boolean isShowAssocActions() {
        return compoundWorkflow != null && compoundWorkflow.isIndependentWorkflow() && !compoundWorkflow.isStatus(Status.FINISHED)
                && isOwnerOrDocManagerOrHasInProgressTask();
    }

    private boolean hasInProgressTask() {
        return WorkflowUtil.isOwnerOfInProgressTask(compoundWorkflow, AuthenticationUtil.getRunAsUser());
    }

    private boolean checkIndependentWorkflowType(QName wfType) {
        if (wfType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)) {
            return false;
        }
        if (wfType.equals(WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW)) {
            return getUserService().isDocumentManager();
        }
        return true;
    }

    private boolean checkCaseFileWorkflowType(QName wfType) {
        if (wfType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW) || wfType.equals(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)) {
            return false;
        }
        return true;
    }

    private boolean checkDocumentWorkflowType(QName wfType, Document doc, boolean isDocStatusWorking) {
        if (!isDocStatusWorking
                && ((wfType.equals(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)
                        && !isAdminOrDocmanagerWithPermission(doc, Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA))
                        || wfType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)
                        || wfType.equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW)
                        || wfType.equals(WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_WORKFLOW))) {
            return false;
        }
        if ((wfType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)
                || wfType.equals(WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW)
                || wfType.equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW)
                || wfType.equals(WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_WORKFLOW))
                && !BaseDialogBean.hasPermission(doc.getNodeRef(), DocumentCommonModel.Privileges.EDIT_DOCUMENT)) {
            return false;
        }
        if (wfType.equals(WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW)
                && !getUserService().isAdministrator()) {
            return false;
        }
        return true;
    }

    @Override
    protected String getConfigArea() {
        return null;
    }

    protected ParametersService getParametersService() {
        if (parametersService == null) {
            parametersService = (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    ParametersService.BEAN_NAME);
        }
        return parametersService;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }

    protected DocumentLogService getDocumentLogService() {
        if (documentLogService == null) {
            documentLogService = (DocumentLogService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentLogService.BEAN_NAME);
        }
        return documentLogService;
    }

    @Override
    protected void updateFullAccess() {
        if (compoundWorkflow != null && !compoundWorkflow.isDocumentWorkflow()) {
            fullAccess = true;
            return;
        }
        fullAccess = false;

        if (getUserService().isDocumentManager()) {
            fullAccess = true;
        } else if (getDocumentDynamicService().isOwner(compoundWorkflow.getParent(), AuthenticationUtil.getRunAsUser())) {
            fullAccess = true;
        } else if (StringUtils.equals(compoundWorkflow.getOwnerId(), AuthenticationUtil.getRunAsUser())) {
            fullAccess = true;
        } else if (StringUtils.equals(compoundWorkflow.getOwnerId(), AuthenticationUtil.getFullyAuthenticatedUser())) {
            // user is probably substituting someone else (but workFlow owner is still user that logged in)
            fullAccess = true;
        } else if (hasTask(AuthenticationUtil.getRunAsUser(), true)) {
            fullAccess = true;
        } else if (hasTask(AuthenticationUtil.getRunAsUser(), false)) {
            fullAccess = false;
        } else {
            throw new RuntimeException("Unknown user rights! Please check the condition rules in code!");
        }
    }

    private boolean hasTask(String user, boolean responsible) {
        for (Workflow block : compoundWorkflow.getWorkflows()) {
            for (Task task : block.getTasks()) {
                if (responsible && task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)
                        && (Boolean) task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)) {
                    if (StringUtils.equals(task.getOwnerId(), user)) {
                        return true;
                    }
                }
                if (!responsible && !task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)) {
                    if (WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(task.getNode().getType())
                            && StringUtils.equals(task.getOwnerId(), user)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void handleException(Exception e, String failMsg) {
        handleException(e, failMsg, null);
    }

    public static void handleException(Exception e, String failMsg, String handledAction) {
        FacesContext context = FacesContext.getCurrentInstance();
        boolean isWorkflowChangedException = e instanceof WorkflowChangedException;
        if (isWorkflowChangedException || e.getCause() instanceof WorkflowChangedException) {
            WorkflowChangedException exception = (WorkflowChangedException) (isWorkflowChangedException ? e : e.getCause());
            ErrorCause errorCause = exception.getErrorCause();
            String debugMessage;
            String displayMessageKey;
            if (errorCause != null) {
                debugMessage = "Compound workflow action failed: no documents found!";
                boolean isRegistrationError = errorCause == ErrorCause.INDEPENDENT_WORKFLOW_REGISTRATION_NO_DOCUMENTS;
                if (START_VALIDATED_WORKFLOW.equals(handledAction)) {
                    displayMessageKey = isRegistrationError ? "workflow_compound_start_failed_registration_no_documents" : "workflow_compound_start_failed_signature_no_documents";
                } else if (CONTINUE_VALIDATED_WORKFLOW.equals(handledAction)) {
                    displayMessageKey = isRegistrationError ? "workflow_compound_continue_failed_registration_no_documents" : "workflow_compound_continue_failed_signature_no_documents";
                } else {
                    displayMessageKey = e.getMessage();
                }
            } else {
                debugMessage = "Compound workflow action failed: data changed!";
                displayMessageKey = "workflow_compound_save_failed";
            }
            handleWorkflowChangedException(exception, debugMessage, displayMessageKey, log);
        } else if (e instanceof WorkflowActiveResponsibleTaskException) {
            log.debug("Compound workflow action failed: more than one active responsible task!", e);
            MessageUtil.addErrorMessage(context, "workflow_compound_save_failed_responsible");
        } else if (e instanceof EmailAttachmentSizeLimitException) {
            log.debug("Compound workflow action failed: email attachment exceeded size limit set in parameter!", e);
            MessageUtil.addErrorMessage(context, "notification_zip_size_too_large", BeanHelper.getParametersService().getLongParameter(MAX_ATTACHED_FILE_SIZE));
        } else if (e instanceof NodeLockedException) {
            log.debug("Compound workflow action failed: document is locked!", e);
            String[] lockedBy = new String[] { BeanHelper.getUserService().getUserFullName(
                    StringUtils.substringBefore((String) BeanHelper.getNodeService().getProperty(((NodeLockedException) e).getNodeRef(), ContentModel.PROP_LOCK_OWNER), "_")) };
            @SuppressWarnings("unchecked")
            Pair<String, Object[]>[] messages = new Pair[] { new Pair<String, Object[]>(failMsg, null),
                    new Pair<String, Object[]>("document_error_docLocked", lockedBy) };
            MessageUtil.addErrorMessage(context, messages);
        } else if (e instanceof InvalidNodeRefException) {
            MessageUtil.addErrorMessage(context, "workflow_task_save_failed_docDeleted");
            WebUtil.navigateTo(AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME, context);
        } else if (e instanceof UnableToPerformException) {
            MessageUtil.addStatusMessage(context, (UnableToPerformException) e);
        } else if (e instanceof ReviewTaskException) {
            ReviewTaskException externalReviewException = (ReviewTaskException) e;
            ExceptionType exceptionType = externalReviewException.getExceptionType();
            if (exceptionType.equals(ReviewTaskException.ExceptionType.EXTERNAL_REVIEW_DVK_CAPABILITY_ERROR)) {
                log.debug("Compound workflow action failed: external review task owner not dvk capable. ", e);
                MessageUtil.addErrorMessage(context, "workflow_compound_save_failed_external_review_owner_not_task_capable");
            } else if (exceptionType.equals(ReviewTaskException.ExceptionType.REVIEW_DVK_CAPABILITY_ERROR)) {
                log.debug("Compound workflow action failed: review task owner not dvk capable. ", e);
                if (DELETE.equals(handledAction)) {
                    MessageUtil.addErrorMessage(context, "workflow_compound_delete_failed_review_owner_not_task_capable");
                } else {
                    MessageUtil.addErrorMessage(context, "workflow_compound_save_failed_review_owner_not_task_capable");
                }
            } else {
                log.debug("Compound workflow action failed: external review workflow error of type: " + externalReviewException.getExceptionType(), e);
                MessageUtil.addErrorMessage(context, "workflow_compound_save_failed_external_review_error");
            }
        } else {
            log.error("Compound workflow action failed!", e);
            MessageUtil.addErrorMessage(context, failMsg);
        }
    }

    public static void handleWorkflowChangedException(WorkflowChangedException workflowChangedException, String logMessage, String displayMessageKey, Log log) {
        if (!log.isDebugEnabled()) {
            log.error(logMessage + workflowChangedException.getShortMessage());
        } else {
            log.debug(logMessage, workflowChangedException);
        }
        MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), displayMessageKey);
    }    
    
    private boolean validate(FacesContext context, boolean checkFinished, boolean checkInvoice, boolean finishingTask) {
        boolean valid = true;
        boolean activeResponsibleAssignTaskInSomeWorkFlow = false;
        // true if some orderAssignmentWorkflow in status NEW has no active responible task (but has some co-responsible tasks)
        boolean checkOrderAssignmentResponsibleTask = false;
        boolean missingOwnerAssignment = false;
        Set<String> missingOwnerMessageKeys = null;
        boolean hasForbiddenFlowsForFinished = false;
        boolean isCategoryEnabled = BeanHelper.getWorkflowService().getOrderAssignmentCategoryEnabled();
        boolean isDocumentWorkflow = compoundWorkflow.isDocumentWorkflow();
        checkFinished = checkFinished && isDocumentWorkflow;
        boolean registeringNotAllowed = false;
        boolean hasUnallowedRegisteringWorkflows = false;
        Document doc = getParentDocument();
        if (isDocumentWorkflow) {
            String docTypeId = (String) doc.getProperties().get(Props.OBJECT_TYPE_ID);
            registeringNotAllowed = !getDocumentAdminService().getDocumentTypeProperty(docTypeId, DocumentAdminModel.Props.REGISTRATION_ENABLED, Boolean.class);
        }
        boolean adminOrDocmanagerWithPermission = isAdminOrDocmanagerWithPermission(doc, Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA);
        for (Workflow block : compoundWorkflow.getWorkflows()) {
            boolean foundOwner = false;
            QName blockType = block.getNode().getType();
            boolean activeResponsibleAssigneeNeeded = blockType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)
                    && !activeResponsibleAssignTaskInSomeWorkFlow && !isActiveResponsibleAssignedForDocument(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, true);
            boolean activeResponsibleAssigneeAssigned = !activeResponsibleAssigneeNeeded;

            if (checkFinished && !hasForbiddenFlowsForFinished) {
                if ((WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(blockType)
                        && !adminOrDocmanagerWithPermission)
                        || WorkflowSpecificModel.Types.REVIEW_WORKFLOW.equals(blockType)
                        || WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW.equals(blockType)
                        || WorkflowSpecificModel.Types.OPINION_WORKFLOW.equals(blockType)) {
                    if (WorkflowUtil.isStatus(block, Status.NEW, Status.STOPPED)) {
                        hasForbiddenFlowsForFinished = true;
                    }
                }
            }
            if (block.isType(WorkflowSpecificModel.Types.REVIEW_WORKFLOW)
                    && block.getNode().getProperties().get(WorkflowCommonModel.Props.PARALLEL_TASKS) == null) {
                valid = false;
                MessageUtil.addErrorMessage(context, "workflow_save_error_missingParallelOrNot");
            }

            if (block.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW) && isCategoryEnabled && StringUtils.isBlank(((OrderAssignmentWorkflow) block).getCategory())) {
                MessageUtil.addErrorMessage(context, "task_category_empty");
                valid = false;
            }

            if (block.isType(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW) && isShowSigningType() && block.getSigningType() == null) {
                valid = false;
                MessageUtil.addErrorMessage(context, "workflow_save_error_missingSigningType");
            }

            if (registeringNotAllowed && block.isType(WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW) && block.isStatus(Status.NEW)) {
                hasUnallowedRegisteringWorkflows = true;
            }

            boolean hasOrderAssignmentActiveResponsible = !(block.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW) && block.isStatus(Status.NEW));
            for (Task task : block.getTasks()) {
                final boolean activeResponsible = WorkflowUtil.isActiveResponsible(task) && !task.isStatus(Status.UNFINISHED);
                if (activeResponsibleAssigneeNeeded && StringUtils.isNotBlank(task.getOwnerName()) && activeResponsible) {
                    activeResponsibleAssignTaskInSomeWorkFlow = true;
                    activeResponsibleAssigneeAssigned = true;
                    missingOwnerAssignment = false;
                }
                hasOrderAssignmentActiveResponsible |= activeResponsible;
                foundOwner |= StringUtils.isNotBlank(task.getOwnerName());
                valid = validateTaskOwnerAndDueDate(context, block, task);
                if (!valid) {
                    break;
                }
            }
            checkOrderAssignmentResponsibleTask |= !hasOrderAssignmentActiveResponsible;
            if (activeResponsibleAssigneeNeeded && !activeResponsibleAssigneeAssigned) {
                missingOwnerAssignment = true;
                if (!foundOwner) {
                    valid = false;
                    final String missingOwnerMessageKey = getMissingOwnerMessageKey(blockType);
                    if (missingOwnerMessageKeys == null) {
                        missingOwnerMessageKeys = new HashSet<String>(2);
                    }
                    missingOwnerMessageKeys.add(missingOwnerMessageKey);
                }
                continue;
            }
            if (!foundOwner) {
                String missingOwnerMsgKey = getMissingOwnerMessageKey(blockType);
                if (missingOwnerMsgKey != null) {
                    MessageUtil.addErrorMessage(context, missingOwnerMsgKey);
                    valid = false;
                }
            }
        }

        valid &= checkTaskDueDateRegression(compoundWorkflow.getWorkflows());

        if (missingOwnerAssignment) {
            valid = false;
            MessageUtil.addErrorMessage(context, "workflow_save_error_missingOwner_assignmentWorkflow1");
        } else if (missingOwnerMessageKeys != null) {
            for (String msgKey : missingOwnerMessageKeys) {
                MessageUtil.addErrorMessage(context, msgKey);
            }
        }

        if (checkOrderAssignmentResponsibleTask) {
            valid = false;
            MessageUtil.addErrorMessage("workflow_save_error_missing_orderAssigmnentResponsibleTask");
        }

        if (checkFinished && hasForbiddenFlowsForFinished && DocumentStatus.FINISHED.getValueName().equals(doc.getDocStatus())) {
            valid = false;
            MessageUtil.addErrorMessage(context, adminOrDocmanagerWithPermission ? "workflow_start_failed_docFinished_admin" : "workflow_start_failed_docFinished");
        }
        if (hasUnallowedRegisteringWorkflows) {
            valid = false;
            MessageUtil.addErrorMessage(context, "workflow_save_error_unallowed_regitering_workflows");
        }
        if (getWorkflowService().isWorkflowTitleEnabled() && StringUtils.isBlank(compoundWorkflow.getTitle()) && !isTitleReadonly()) {
            valid = false;
            MessageUtil.addErrorMessage(context, "workflow_save_error_title_required");
        }
        if (checkInvoice) {
            valid = valid && validateInvoice();
        }
        if (!valid) {
            updatePanelGroup(null, null, true, true, null, !finishingTask);
        }
        return valid;
    }

    private boolean validateTaskOwnerAndDueDate(FacesContext context, Workflow block, Task task) {
        QName taskType = task.getNode().getType();
        String taskOwnerMsg = getTaskOwnerMessage(block, taskType, task.isResponsible());
        if (taskType.equals(WorkflowSpecificModel.Types.INFORMATION_TASK)) {
            if (StringUtils.isBlank(task.getOwnerName())) {
                MessageUtil.addErrorMessage(context, "task_name_required", taskOwnerMsg);
                return false;
            }
        }
        else if (taskType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
            if (StringUtils.isBlank(task.getInstitutionName()) || task.getDueDate() == null) {
                MessageUtil.addErrorMessage(context, "task_name_and_due_required", taskOwnerMsg);
                return false;
            }
        }
        else if (StringUtils.isBlank(task.getOwnerName()) || task.getDueDate() == null) {
            MessageUtil.addErrorMessage(context, "task_name_and_due_required", taskOwnerMsg);
            return false;
        }
        return true;
    }

    private boolean isResponsibleAllowed(QName taskType) {
        return taskType.equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK) || taskType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK);
    }

    private boolean hasNoOwnerOrDueDate(Task task) {
        return StringUtils.isBlank(task.getOwnerName()) != (task.getDueDate() == null && task.getDueDateDays() == null);
    }

    public boolean isShowComment() {
        return compoundWorkflow.isIndependentWorkflow() || compoundWorkflow.isCaseFileWorkflow();
    }

    public boolean isCommentReadonly() {
        return compoundWorkflow.isStatus(Status.FINISHED) || !isOwnerOrDocManagerOrHasInProgressTask();
    }

    public boolean isOwnerNameReadonly() {
        return (compoundWorkflow.isDocumentWorkflow() && (!fullAccess || compoundWorkflow.isStatus(Status.IN_PROGRESS)))
                || (compoundWorkflow.isIndependentWorkflow() && (!isCompoundWorkflowOwner(compoundWorkflow) || compoundWorkflow.isStatus(Status.FINISHED)))
                || (compoundWorkflow.isCaseFileWorkflow() && compoundWorkflow.isStatus(Status.FINISHED) && !(isCompoundWorkflowOwner(compoundWorkflow) || PrivilegeUtil
                        .isAdminOrDocmanagerWithPermission(compoundWorkflow.getParent(), Privileges.VIEW_CASE_FILE)));
    }

    public boolean isShowEmptyWorkflowMessage() {
        return showEmptyWorkflowMessage && compoundWorkflow == null;
    }
    
    public boolean isShowTitle() {
        return BeanHelper.getWorkflowService().isWorkflowTitleEnabled();
    }
    
    public boolean isTitleReadonly() {
        return !(compoundWorkflow.isStatus(Status.NEW) && (StringUtils.equals(AuthenticationUtil.getRunAsUser(), compoundWorkflow.getOwnerId()) || BeanHelper.getUserService()
                .isDocumentManager()));
    }

    private boolean checkTaskDueDateRegression(List<Workflow> workflows) {
        List<List<Workflow>> parallelWorkflowBlocks = collectParallelWorkflowBlocks(workflows);
        Date minAllowedDueDate = null;
        if (parallelWorkflowBlocks.size() == 1 && !parallelWorkflowBlocks.get(0).isEmpty() && parallelWorkflowBlocks.get(0).get(0).isParallelTasks()) {
            // one block with parallel tasks needs no additional check
            return true;
        }
        // these blocks must have strict due date order between each other:
        // minimum due date of block must be >= maximum due date in preceeding blocks
        // maximum due date of block must be <= minimum due date in succeeding blocks
        for (List<Workflow> block : parallelWorkflowBlocks) {
            if (block.isEmpty()) {
                // shouldn't normally happen, but this check is performed elsewhere
                continue;
            }
            Workflow blockFirstWorkflow = block.get(0);
            boolean checkTasksInsideBlock = !blockFirstWorkflow.isParallelTasks();
            Assert.isTrue(!checkTasksInsideBlock || block.size() == 1, "Not parallel tasks inside parallel block are not allowed.");
            Pair<Date, Date> minMaxDateInBlock = getCheckAndGetMaxDueDate(block, minAllowedDueDate, checkTasksInsideBlock);
            if (minMaxDateInBlock == null) {
                return false;
            }
            Date minDueDateInBlock = minMaxDateInBlock.getFirst();
            Date maxDueDateInBlock = minMaxDateInBlock.getSecond();
            // date mandatory check is not performed here, null values are ignored
            if (minDueDateInBlock != null && minAllowedDueDate != null && minDueDateInBlock.before(minAllowedDueDate)) {
                MessageUtil.addErrorMessage("workflow_save_error_dueDate_decreaseNotAllowed");
                return false;
            }
            if (minAllowedDueDate == null || maxDueDateInBlock != null) {
                minAllowedDueDate = maxDueDateInBlock;
            }
        }
        return true;
    }

    /** Return null indicating that regression inside the workflow block was wrong */
    private Pair<Date, Date> getCheckAndGetMaxDueDate(List<Workflow> block, Date minAllowedDueDate, boolean checkTasksInsideBlock) {
        Date minDueDate = null;
        Date maxDueDate = null;
        Date previousDueDate = null;
        for (Workflow workflow : block) {
            for (Task task : workflow.getTasks()) {
                if (!task.isStatus(Status.NEW)) {
                    continue;
                }
                Date taskDueDate = task.getDueDate();
                if (taskDueDate == null) {
                    continue;
                }
                if (checkTasksInsideBlock) {
                    if (previousDueDate != null && taskDueDate.before(previousDueDate)) {
                        MessageUtil.addErrorMessage("workflow_save_error_dueDate_decreaseNotAllowed");
                        return null;
                    }
                    previousDueDate = taskDueDate;
                }
                if (minDueDate == null || minDueDate.after(taskDueDate)) {
                    minDueDate = taskDueDate;
                }
                if (maxDueDate == null || taskDueDate.after(maxDueDate)) {
                    maxDueDate = taskDueDate;
                }
            }
        }
        return new Pair<Date, Date>(minDueDate, maxDueDate);
    }

    /**
     * Collect workflow blocks that don't need due date comparing between each others tasks inside one block,
     * but need comparing between different blocks
     * and may need comparing within single workflow depending on the workflow type
     */
    private List<List<Workflow>> collectParallelWorkflowBlocks(List<Workflow> workflows) {
        List<List<Workflow>> parallelWorkflowBlocks = new ArrayList<List<Workflow>>();
        List<Workflow> currentBlock = new ArrayList<Workflow>();
        for (Workflow currentWorkflow : workflows) {
            if (!currentBlock.isEmpty()) {
                Workflow previousWorkflow = currentBlock.get(currentBlock.size() - 1);
                if (previousWorkflow.isType(WorkflowSpecificModel.CAN_START_PARALLEL) && currentWorkflow.isType(WorkflowSpecificModel.CAN_START_PARALLEL)) {
                    currentBlock.add(currentWorkflow);
                } else {
                    parallelWorkflowBlocks.add(currentBlock);
                    currentBlock = new ArrayList<Workflow>();
                    currentBlock.add(currentWorkflow);
                }
            } else {
                currentBlock.add(currentWorkflow);
            }
        }
        if (!currentBlock.isEmpty()) {
            parallelWorkflowBlocks.add(currentBlock);
        }
        return parallelWorkflowBlocks;
    }

    private String getTaskOwnerMessage(Workflow block, QName taskType, boolean isResponsible) {
        String suffix = "";
        if (isResponsibleAllowed(taskType) && !isResponsible) {
            suffix = "_co";
        }
        String taskOwnerMsg = MessageUtil.getMessage(block.getNode().getType().getLocalName() + "_tasks" + suffix);
        return taskOwnerMsg;
    }

    private boolean validateInvoice() {
        NodeRef docRef = compoundWorkflow.getParent();
        if (!compoundWorkflow.isDocumentWorkflow() || docRef == null || !DocumentSubtypeModel.Types.INVOICE.equals(BeanHelper.getNodeService().getType(docRef))) {
            return true;
        }
        Map<QName, Serializable> docProps = BeanHelper.getNodeService().getProperties(docRef);
        List<Transaction> transactions = BeanHelper.getEInvoiceService().getInvoiceTransactions(docRef);
        if (transactions.isEmpty()) {
            return true;
        }
        List<String> mandatoryForOwner = BeanHelper.getEInvoiceService().getOwnerMandatoryFields();
        if (mandatoryForOwner.isEmpty()) {
            return true;
        }
        boolean valid = true;
        List<Pair<String, String>> errorMessages = new ArrayList<Pair<String, String>>();
        List<String> addedErrorKeys = new ArrayList<String>();
        for (Transaction transaction : transactions) {
            Map<String, Object> props = transaction.getNode().getProperties();
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                EInvoiceUtil.checkTransactionMandatoryFields(mandatoryForOwner, errorMessages, addedErrorKeys, transaction);
            }
        }

        if (!errorMessages.isEmpty()) {
            valid = false;
            for (Pair<String, String> validationMsg : errorMessages) {
                // override validation message, use only object value
                MessageUtil.addErrorMessage("workflow_start_failed_transaction_mandatory_not_filled", validationMsg.getSecond());
            }
        }
        List<String> errorMessageKeys = new ArrayList<String>();
        Double totalSum = (Double) docProps.get(DocumentSpecificModel.Props.INVOICE_SUM);
        EInvoiceUtil.checkTotalSum(errorMessageKeys, "workflow_start_failed_", totalSum, transactions, null, false);
        if (!errorMessageKeys.isEmpty()) {
            valid = false;
            for (String validationMsg : errorMessageKeys) {
                MessageUtil.addErrorMessage(validationMsg);
            }
        }
        return valid;
    }

    private String getMissingOwnerMessageKey(QName blockType) {
        String missingOwnerMsgKey = null;
        if (knownWorkflowTypes.contains(blockType)) {
            missingOwnerMsgKey = "workflow_save_error_missingOwner_" + blockType.getLocalName();
        }
        return missingOwnerMsgKey;
    }

    @Override
    public void afterConfirmationAction(Object action) {
        switch ((WorkflowServiceImpl.DialogAction) action) {
        case SAVING:
            if (saveCompWorkflow()) {
                resetState();
                initBlocks();
                WebUtil.navigateTo(getDefaultFinishOutcome());
            }
            break;
        case STARTING:
            startValidatedWorkflow(null);
            break;
        case CONTINUING:
            continueValidatedWorkflow();
        }
    }

    @Override
    protected Document getParentDocument() {
        return compoundWorkflow != null && compoundWorkflow.isDocumentWorkflow() ? new Document(compoundWorkflow.getParent()) : null;
    }

    @Override
    protected boolean isAddLinkForWorkflow(Document doc, QName workflowType) {
        if (compoundWorkflow.isIndependentWorkflow()) {
            return !workflowType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW);
        } else if (compoundWorkflow.isCaseFileWorkflow()) {
            return !(workflowType.equals(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)
                    || workflowType.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)
                    || workflowType.equals(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW) || workflowType.equals(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW));
        }
        boolean addLinkForThisWorkflow = false;
        if (WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && doc.hasPermission(Privileges.EDIT_DOCUMENT)) {
                addLinkForThisWorkflow = true;
            } else if (doc.isDocStatus(DocumentStatus.FINISHED) && isAdminOrDocmanagerWithPermission(doc, Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.OPINION_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && doc.hasPermission(Privileges.EDIT_DOCUMENT)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && doc.hasPermission(Privileges.EDIT_DOCUMENT)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.REVIEW_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && doc.hasPermission(Privileges.EDIT_DOCUMENT)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW.equals(workflowType)) {
            if (doc.hasPermissions(Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && getUserService().isAdministrator()) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.INFORMATION_WORKFLOW.equals(workflowType)) {
            if (doc.hasPermissions(Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW.equals(workflowType)) {
            if (doc.hasPermissions(Privileges.VIEW_DOCUMENT_FILES, Privileges.VIEW_DOCUMENT_META_DATA)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && doc.hasPermission(Privileges.EDIT_DOCUMENT)) {
                addLinkForThisWorkflow = true;
            }
        } else if (WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_WORKFLOW.equals(workflowType)) {
            if (doc.isDocStatus(DocumentStatus.WORKING) && doc.hasPermission(Privileges.EDIT_DOCUMENT)) {
                addLinkForThisWorkflow = true;
            }
        } else {
            throw new UnableToPerformException("unknown workflow type " + workflowType.getLocalName()
                    + " - not sure if it should be displayed or not when configuring compound workflow bound to document");
        }
        return addLinkForThisWorkflow;
    }

    public void updateCompoundWorkflowMainDoc(NodeRef mainDocRef, NodeRef removedDocumentToSign) {
        compoundWorkflow.setMainDocument(mainDocRef);
        if (removedDocumentToSign != null) {
            for (Iterator<NodeRef> i = compoundWorkflow.getDocumentsToSign().iterator(); i.hasNext();) {
                NodeRef docRef = i.next();
                if (docRef.getId().equals(removedDocumentToSign.getId())) {
                    i.remove();
                }
            }
        }
        updateAssocRelatedBlocks();
    }

    public void updateAssocRelatedBlocks() {
        BeanHelper.getCompoundWorkflowAssocListDialog().restored(false);
        getLog().restore();
    }

    @Override
    public AbstractSearchBlockBean getSearch() {
        return BeanHelper.getCompoundWorkflowAssocSearchBlock();
    }

    @Override
    public CompoundWorkflowLogBlockBean getLog() {
        return BeanHelper.getCompoundWorkflowLogBlockBean();
    }

}
