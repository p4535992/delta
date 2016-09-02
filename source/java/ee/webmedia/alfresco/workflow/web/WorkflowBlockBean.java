package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocLockService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getJsfBindingHelper;
import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDigiDoc4JSignatureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowConstantsBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowDbService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.isAdminOrDocmanagerWithPermission;
import static ee.webmedia.alfresco.utils.ComponentUtil.getAttributes;
import static ee.webmedia.alfresco.utils.ComponentUtil.getChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;
import static ee.webmedia.alfresco.workflow.web.CompoundWorkflowDialog.handleWorkflowChangedException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.UISelectBoolean;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpSession;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.ajax.InvokeCommand.ResponseMimetype;
import org.alfresco.web.bean.FileUploadBean;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.common.component.data.UIColumn;
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.digidoc4j.Container;
import org.digidoc4j.DataToSign;
import org.joda.time.LocalDate;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIPropertySheet;
import ee.webmedia.alfresco.common.propertysheet.customchildrencontainer.CustomChildrenCreator;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerConverter;
import ee.webmedia.alfresco.common.propertysheet.datepicker.DatePickerGenerator;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent.ModalLayerSubmitEvent;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ValidatingModalLayerComponent;
import ee.webmedia.alfresco.common.propertysheet.renderer.HtmlButtonRenderer;
import ee.webmedia.alfresco.common.propertysheet.renderkit.PropertySheetGridRenderer;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.search.UserSearchGenerator;
import ee.webmedia.alfresco.common.propertysheet.upload.UploadFileInput;
import ee.webmedia.alfresco.common.richlist.PageLoadCallback;
import ee.webmedia.alfresco.common.service.RequestCacheBean;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.web.evaluator.DocumentNotInDraftsFunctionActionEvaluator;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.web.Digidoc4jSignatureModalComponent;
import ee.webmedia.alfresco.signature.web.SignatureAppletModalComponent;
import ee.webmedia.alfresco.signature.web.SignatureBlockBean;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.SigningType;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowBlockItem;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel.SignatureTaskOutcome;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
import ee.webmedia.alfresco.workflow.service.DueDateHistoryRecord;
import ee.webmedia.alfresco.workflow.service.SignatureTask;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.type.DueDateExtensionWorkflowType;
import ee.webmedia.alfresco.workflow.web.PrintTableServlet.TableMode;


public class WorkflowBlockBean implements DocumentDynamicBlock {

    private static final long serialVersionUID = 1L;
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(WorkflowBlockBean.class);
    public static final String BEAN_NAME = "WorkflowBlockBean";

    private static final String WORKFLOW_METHOD_BINDING_NAME = "#{WorkflowBlockBean.findCompoundWorkflowDefinitions}";
    private static final String WORKFLOW_METHOD_BINDING_NAME_CACHE_KEY = "WorkflowMethodBindingName";
    private static final String INDEPENDENT_WORKFLOW_METHOD_BINDING_NAME = "#{WorkflowBlockBean.findIndependentCompoundWorkflowDefinitions}";
    private static final String INDEPENDENT_WORKFLOW_METHOD_BINDING_NAME_CACHE_KEY = "IndependentWorkflowMethodBindingName";
    private static final String DROPDOWN_MENU_ITEM_ICON = "/images/icons/versioned_properties.gif";
    private static final String ARROW_RIGHT_ICON = "/images/icons/arrow-right.png";
    private static final String MSG_WORKFLOW_ACTION_GROUP = "workflow_compound_start_workflow";
    private static final String TASK_DUE_DATE_EXTENSION_ID = "task-due-date-extension";
    private static final String ATTRIB_OUTCOME_INDEX = "outcomeIndex";
    private static final String SAVE_TASK = "WorkflowBlockBean.saveTask";
    public static final String ATTRIB_FINISH_VALIDATED = "finishValidated";
    public static final String FINISH_TASK = "WorkflowBlockBean.finishTask";
    public static final String SEND_TASK_DUE_DATE_EXTENSION_REQUEST = "WorkflowBlockBean.sendTaskDueDateExtensionRequest";
    public static final String PARAM_COMPOUND_WORKFLOF_DEFINITION_NODEREF = "compoundWorkflowDefinitionNodeRef";
    public static final String PARAM_ASSOC_NODEREF = "docAssocNodeRef";

    private static final String NOTIFY_DIALOGS_BINDING = "#{" + BEAN_NAME + ".notifyDialogsIfNeeded}";
    private static final String SIGN_OWNER_DOC_BINDING_NAME = "#{" + BEAN_NAME + ".createSignOwnerDocMenu}";
    private static final String SIGN_OWNER_DOC_ACTION_LISTENER = "#{" + BEAN_NAME + ".signOwnerDocument}";
    private static final String DOC_REF_PARAM = "docRef";
    private static final String SIGNING_METHOD = "signingMethod";
    private static final Map<String, String> OUTCOME_TO_LABEL;
    static {
        Map<String, String> tempMap = new HashMap<>();
        tempMap.put(SignatureTaskOutcome.SIGNED_IDCARD.name(), MessageUtil.getMessage("document_sign_id_card"));
        tempMap.put(SignatureTaskOutcome.SIGNED_MOBILEID.name(), MessageUtil.getMessage("document_sign_mobile_id"));
        OUTCOME_TO_LABEL = Collections.unmodifiableMap(tempMap);
    }
    private static final Set<QName> DELEGABLE_TASKS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(WorkflowSpecificModel.Types.INFORMATION_TASK)));

    /** task index attribute name */
    public static final String ATTRIB_INDEX = "index";
    private static final String MODAL_KEY_REASON = "reason";
    private static final String MODAL_KEY_EXTENDER = "extender";
    private static final String MODAL_KEY_DUE_DATE = "dueDate";
    private static final String MODAL_KEY_PROPOSED_DUE_DATE = "proposedDueDate";
    private static final String MODAL_KEY_EXTENDER_FULL_NAME = "extenderFullName";
    private DelegationBean delegationBean;
    private CompoundWorkflowDialog compoundWorkflowDialog;

    private NodeRef containerRef;
    private Node container;
    private NodeRef taskPanelControlDocument;
    private List<NodeRef> compoundWorkflows;
    // in case of independent workflow, use only given workflow
    private CompoundWorkflow compoundWorkflow;
    private List<Task> myTasks;
    private TaskDataProvider finishedReviewTasks;
    private TaskDataProvider finishedOpinionTasks;
    private TaskDataProvider finishedOrderAssignmentTasks;
    private List<WorkflowBlockItem> groupedWorkflowBlockItems;
    private WorkflowBlockItemDataProvider workflowBlockItemDataProvider;
    private List<File> removedFiles;
    private SigningFlowContainer signingFlow;
    private String challengeId;

    private String dueDateExtenderUsername;
    private String dueDateExtenderUserFullname;
    

    private RequestCacheBean requestCacheBean;
    
    private NodeRef lockedCompoundWorkflowNodeRef;

    @Override
    public void resetOrInit(DialogDataProvider provider) {
        if (provider == null) {
            reset();
        } else {
            compoundWorkflow = null;
            compoundWorkflowDialog = null;
            init(provider.getNode());
        }
    }

    public void init(Node container) {
        this.container = container;
        containerRef = container.getNodeRef();
        delegationBean.setWorkflowBlockBean(this);
        restore("init");
    }

    public void initIndependentWorkflow(CompoundWorkflow compoundWorkflow, CompoundWorkflowDialog compoundWorkflowDialog) {
        // use copy of compound workflow because otherwise both
        // CompoundWorkflowDialog and WorkflowBlockBean display inputs are bind to same task object
        // resulting in undetermined task data
        this.compoundWorkflow = getWorkflowService().copyCompoundWorkflowInMemory(compoundWorkflow);
        this.compoundWorkflowDialog = compoundWorkflowDialog;
        init(new Node(BeanHelper.getConstantNodeRefsBean().getIndependentWorkflowsRoot()));
    }

    public void reset() {
        BeanHelper.getDelegationBean().reset();
        container = null;
        containerRef = null;
        compoundWorkflows = null;
        compoundWorkflow = null;
        myTasks = null;
        finishedReviewTasks = null;
        finishedOpinionTasks = null;
        finishedOrderAssignmentTasks = null;
        groupedWorkflowBlockItems = null;
        workflowBlockItemDataProvider = null;
        removedFiles = null;
        delegationBean.reset();
        signingFlow = null;
        dueDateExtenderUsername = null;
        dueDateExtenderUserFullname = null;
    }

    @Override
    public void clean() {
        reset();
    }

    void refreshCompoundWorkflowAndRestore(CompoundWorkflow compoundWorkflow, String action) {
        this.compoundWorkflow = compoundWorkflow;
        restore(action);
    }

    public void restore(String action) {
        // TODO When are all the active task actually needed? Currently they are loaded in a plethora of places.
        getRestoredMyTasks(getRestoredCompoundWorkflows());
        WorkflowService wfService = getWorkflowService();
        wfService.loadTaskFilesFromCompoundWorkflows(myTasks, compoundWorkflows);

        removedFiles = null;

        // Reset data providers
        finishedReviewTasks = null;
        finishedOpinionTasks = null;
        finishedOrderAssignmentTasks = null;

        groupedWorkflowBlockItems = null;
        workflowBlockItemDataProvider = null;
        delegationBean.reset();
        // rebuild the whole task panel
        constructTaskPanelGroup(action);

        if (!myTasks.isEmpty()) {
            List<QName> taskTypes = new ArrayList<>(5);

            for (Task task : myTasks) {
                QName type = task.getType();
                if (WorkflowSpecificModel.Types.OPINION_TASK.equals(type)) {
                    wfService.loadTaskFiles(task);
                }

                if (!taskTypes.contains(type)) {
                    getLogService().addLogEntry(LogEntry.create(LogObject.WORKFLOW, getUserService(), containerRef, "applog_task_view", MessageUtil.getTypeName(task.getType())));
                    taskTypes.add(type);
                }
            }
        }
        clearFileUploadBean();
    }

    private List<Task> getRestoredMyTasks(List<NodeRef> taskCompoundWorkflows) {
        myTasks = getWorkflowService().getMyTasksInProgress(taskCompoundWorkflows);
        return myTasks;
    }

    private List<NodeRef> getRestoredCompoundWorkflows() {
        if (compoundWorkflow != null && compoundWorkflow.isIndependentWorkflow() && compoundWorkflow.getNodeRef() != null) {
            compoundWorkflows = Arrays.asList(compoundWorkflow.getNodeRef());
        } else if (BeanHelper.getConstantNodeRefsBean().getIndependentWorkflowsRoot().equals(containerRef)) {
            compoundWorkflows = Collections.emptyList();
        } else if (containerRef != null) {
            compoundWorkflows = getWorkflowService().getCompoundWorkflowNodeRefs(containerRef);
        } else {
            compoundWorkflows = new ArrayList<NodeRef>();
        }

        return compoundWorkflows;
    }

    private void clearFileUploadBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        FileUploadBean fileBean = (FileUploadBean) context.getExternalContext().getSessionMap().
                get(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
        if (fileBean != null) {
            context.getExternalContext().getSessionMap().remove(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
        }
    }

    public boolean isCompoundWorkflowOwner() {
        return getWorkflowService().isCompoundWorkflowOwner(getCompoundWorkflows());
    }

    public boolean isWorkflowSummaryBlockExpanded() {
        return compoundWorkflow != null && compoundWorkflow.isIndependentWorkflow() && !compoundWorkflow.isStatus(Status.NEW);
    }

    public boolean getIsShowDocumentWorkflowSummaryBlock() {
        return getWorkflowConstantsBean().isDocumentWorkflowEnabled();
    }

    // This method (UIAction's "value" methodBinding) is called on every render, so caching results is useful
    public List<ActionDefinition> findCompoundWorkflowDefinitions(@SuppressWarnings("unused") String nodeTypeId) {
        if (container == null) {
            return Collections.emptyList();
        }
        return getCompoundWorkflowDefinitionsByType(getCompoundWorkflowType());
    }

    public List<ActionDefinition> findIndependentCompoundWorkflowDefinitions(@SuppressWarnings("unused") String nodeTypeId) {
        if (container == null) {
            return Collections.emptyList();
        }
        return getCompoundWorkflowDefinitionsByType(CompoundWorkflowType.INDEPENDENT_WORKFLOW);
    }

    @SuppressWarnings("unchecked")
    public List<ActionDefinition> getCompoundWorkflowDefinitionsByType(CompoundWorkflowType compoundWorkflowType) {
        WorkflowService wfService = getWorkflowService();
        String userId = AuthenticationUtil.getRunAsUser();
        List<CompoundWorkflowDefinition> workflowDefs = wfService.getCompoundWorkflowDefinitionsByType(userId, compoundWorkflowType, true);

        if (!getWorkflowConstantsBean().isExternalReviewWorkflowEnabled()) {
            for (Iterator<CompoundWorkflowDefinition> it = workflowDefs.iterator(); it.hasNext();) {
                CompoundWorkflowDefinition compoundWorkflowDefinition = it.next();
                if (containsExternalReviewWorkflows(compoundWorkflowDefinition)) {
                    it.remove();
                }
            }
        }
        List<ActionDefinition> actionDefinitions = new ArrayList<>(workflowDefs.size());
        if (CompoundWorkflowType.DOCUMENT_WORKFLOW.equals(compoundWorkflowType)) {
            boolean showCWorkflowDefsWith1Workflow = false;
            Map<NodeRef, List<NodeRef>> childWorkflowNodeRefsByCompoundWorkflow = getWorkflowService().getChildWorkflowNodeRefsByCompoundWorkflow(compoundWorkflows);
            for (Map.Entry<NodeRef, List<NodeRef>> cWorkflow : childWorkflowNodeRefsByCompoundWorkflow.entrySet()) {
                if (cWorkflow.getValue().size() > 1) {
                    Status status = Status.of((String) getNodeService().getProperty(cWorkflow.getKey(), WorkflowCommonModel.Props.STATUS));
                    if (Status.IN_PROGRESS.equals(status) || Status.STOPPED.equals(status)) {
                        showCWorkflowDefsWith1Workflow = true;
                    }
                }
            }

            String documentTypeId = (String) container.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
            String documentStatus = (String) container.getProperties().get(DocumentCommonModel.Props.DOC_STATUS);

            // remove CompoundWorkflowDefinitions that shouldn't be visible the user viewing this document regardless permissions
            for (Iterator<CompoundWorkflowDefinition> it = workflowDefs.iterator(); it.hasNext();) {
                CompoundWorkflowDefinition compoundWorkflowDefinition = it.next();
                if (!compoundWorkflowDefinition.getDocumentTypes().contains(documentTypeId)) {
                    it.remove(); // not for same DocType
                } else if (showCWorkflowDefsWith1Workflow && compoundWorkflowDefinition.getWorkflows().size() > 1) {
                    it.remove(); // already have active cWorkflow with multiple workflows - allowed only one at the time
                }
            }

            boolean isWorking = DocumentStatus.WORKING.getValueName().equals(documentStatus);
            boolean isFinished = DocumentStatus.FINISHED.getValueName().equals(documentStatus);
            boolean hasPrivEditDoc = container.hasPermission(Privilege.EDIT_DOCUMENT);
            boolean hasViewPrivs = container.hasPermission(Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES);
            boolean hasViewPrivsWithoutEdit = !hasPrivEditDoc && hasViewPrivs;
            Boolean adminOrDocmanagerWithPermission = null;
            for (CompoundWorkflowDefinition cWorkflowDef : workflowDefs) {
                if (isWorking && hasPrivEditDoc) {
                    actionDefinitions.add(createActionDef(cWorkflowDef));
                    continue;
                } else if (isWorking && hasViewPrivsWithoutEdit) {
                    if (!hasOtherWFs(cWorkflowDef, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW
                            , WorkflowSpecificModel.Types.INFORMATION_WORKFLOW, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)) {
                        actionDefinitions.add(createActionDef(cWorkflowDef));
                        continue;
                    }
                } else if (isFinished && hasViewPrivs) {
                    if (!hasOtherWFs(cWorkflowDef, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW
                            , WorkflowSpecificModel.Types.INFORMATION_WORKFLOW, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW)) {
                        actionDefinitions.add(createActionDef(cWorkflowDef));
                        continue;
                    }
                }
                if (isFinished) {
                    if (adminOrDocmanagerWithPermission == null) {
                        adminOrDocmanagerWithPermission = isAdminOrDocmanagerWithPermission(container, Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES);
                    }
                    if (adminOrDocmanagerWithPermission
                            && !hasOtherWFs(cWorkflowDef, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.INFORMATION_WORKFLOW,
                                    WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)) {
                        actionDefinitions.add(createActionDef(cWorkflowDef));
                    }
                }
            }
        } else {
            boolean isCaseFileWorkflow = CompoundWorkflowType.CASE_FILE_WORKFLOW.equals(compoundWorkflowType);
            for (CompoundWorkflowDefinition cWorkflowDef : workflowDefs) {
                actionDefinitions.add(isCaseFileWorkflow ? createActionDef(cWorkflowDef) : createIndependentWorkflowActionDef(cWorkflowDef));
            }
        }
        TransformingComparator transformingComparator = new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((ActionDefinition) input).Label;
            }
        }, AppConstants.getNewCollatorInstance());
        Collections.sort(actionDefinitions, transformingComparator);
        return actionDefinitions;
    }

    private CompoundWorkflowType getCompoundWorkflowType() {
        QName parentType = container.getType();
        CompoundWorkflowType compoundWorkflowType;
        if (isDocumentWorkflow(parentType)) {
            compoundWorkflowType = CompoundWorkflowType.DOCUMENT_WORKFLOW;
        } else if (isCaseWorkflow(parentType)) {
            compoundWorkflowType = CompoundWorkflowType.CASE_FILE_WORKFLOW;
        } else {
            throw new UnableToPerformException("Unallowed parent type for compound workflow: " + parentType);
        }
        return compoundWorkflowType;
    }

    private ActionDefinition createActionDef(CompoundWorkflowDefinition compoundWorkflowDefinition) {
        ActionDefinition actionDefinition = new ActionDefinition("compoundWorkflowDefinitionAction");
        actionDefinition.Image = DROPDOWN_MENU_ITEM_ICON;
        actionDefinition.Label = compoundWorkflowDefinition.getName();
        actionDefinition.Action = "#{WorkflowBlockBean.getCompoundWorkflowDialog}";
        actionDefinition.ActionListener = "#{CompoundWorkflowDialog.setupNewWorkflow}";
        actionDefinition.addParam(ActionUtil.PARAM_PARENT_NODEREF, container.getNodeRefAsString());
        actionDefinition.addParam(PARAM_COMPOUND_WORKFLOF_DEFINITION_NODEREF, compoundWorkflowDefinition.getNodeRef().toString());
        return actionDefinition;
    }

    private ActionDefinition createIndependentWorkflowActionDef(CompoundWorkflowDefinition compoundWorkflowDefinition) {
        ActionDefinition actionDefinition = createActionDef(compoundWorkflowDefinition);
        // override parent node set in createActionDef
        actionDefinition.addParam(ActionUtil.PARAM_PARENT_NODEREF, BeanHelper.getConstantNodeRefsBean().getIndependentWorkflowsRoot().toString());
        actionDefinition.ActionListener = "#{CompoundWorkflowDialog.setupNewIndependentWorkflowFromDocument}";
        actionDefinition.addParam(PARAM_ASSOC_NODEREF, container.getNodeRefAsString());
        return actionDefinition;
    }

    private boolean hasOtherWFs(CompoundWorkflowDefinition cWorkflowDef, QName... expectedWFTypes) {
        for (Workflow workflow : cWorkflowDef.getWorkflows()) {
            if (!workflow.isType(expectedWFTypes)) {
                return true;
            }
        }
        return false;
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
        final NodeService nodeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getNodeService();
        if (!nodeService.exists(getDocumentDialogHelperBean().getNodeRef())) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "workflow_compound_start_workflow_error_docDeleted");
            return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
        }
        return CompoundWorkflowDialog.DIALOG_NAME;
    }

    public String getWorkflowMethodBindingName() {
        try {
            // independent compound workflow
            if (compoundWorkflow != null) {
                return null;
            }
            Boolean returnBinding = (Boolean) requestCacheBean.getResult(WORKFLOW_METHOD_BINDING_NAME_CACHE_KEY);
            if (returnBinding != null) {
                return returnBinding ? WORKFLOW_METHOD_BINDING_NAME : null;
            }
            returnBinding = evaluateShowWorkflowMethodBinding();
            requestCacheBean.setResult(WORKFLOW_METHOD_BINDING_NAME_CACHE_KEY, returnBinding);
            return returnBinding ? WORKFLOW_METHOD_BINDING_NAME : null;
        } catch (InvalidNodeRefException ne) {
            log.warn("Node " + containerRef + " in invalid!");
            return null;
        } catch (RuntimeException e) {
            // Log error here, because JSF EL evaluator does not log detailed error cause
            log.error("Error getting workflowMethodBindingName", e);
            throw e;
        }
    }

    private boolean evaluateShowWorkflowMethodBinding() {
        QName parentType = null;
        if (containerRef != null && getNodeService().exists(containerRef)) {
            parentType = getNodeService().getType(containerRef);
        }
        return (isDocumentWorkflow(parentType) && getWorkflowConstantsBean().isDocumentWorkflowEnabled()
                && container.hasPermission(Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES)
                && (container == null || new DocumentNotInDraftsFunctionActionEvaluator().evaluate(container)))
                || isCaseWorkflow(parentType) && BeanHelper.getWorkflowService().hasNoStoppedOrInprogressCompoundWorkflows(containerRef);
    }

    public String getIndependentWorkflowMethodBindingName() {
        try {
            Boolean returnBinding = (Boolean) requestCacheBean.getResult(INDEPENDENT_WORKFLOW_METHOD_BINDING_NAME_CACHE_KEY);
            if (returnBinding != null) {
                return returnBinding ? INDEPENDENT_WORKFLOW_METHOD_BINDING_NAME : null;
            }
            returnBinding = evaluateShowIndependentWorkflowMethodBinding();
            requestCacheBean.setResult(INDEPENDENT_WORKFLOW_METHOD_BINDING_NAME_CACHE_KEY, returnBinding);
            return returnBinding ? INDEPENDENT_WORKFLOW_METHOD_BINDING_NAME : null;
        } catch (InvalidNodeRefException ne) {
            log.warn("Node " + containerRef + " in invalid!");
            return null;
        } catch (RuntimeException e) {
            // Log error here, because JSF EL evaluator does not log detailed error cause
            log.error("Error getting independentWorkflowMethodBindingName", e);
            throw e;
        }
    }

    private boolean evaluateShowIndependentWorkflowMethodBinding() {
        return compoundWorkflow == null && getWorkflowConstantsBean().isIndependentWorkflowEnabled()
                && BeanHelper.getPrivilegeService()
                        .hasPermission(containerRef, AuthenticationUtil.getRunAsUser(), Privilege.VIEW_DOCUMENT_META_DATA, Privilege.VIEW_DOCUMENT_FILES)
                && (containerRef == null || !isDocumentWorkflow(BeanHelper.getNodeService().getType(containerRef))
                || new DocumentNotInDraftsFunctionActionEvaluator().evaluate(new Node(containerRef)));
    }

    public void clearRequestCache() {
        requestCacheBean.clear();
    }

    private boolean isCaseWorkflow(QName parentType) {
        return CaseFileModel.Types.CASE_FILE.equals(parentType);
    }

    private boolean isDocumentWorkflow(QName parentType) {
        return DocumentCommonModel.Types.DOCUMENT.equals(parentType);
    }

    public boolean isInWorkspace() {
        return containerRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE);
    }

    public void saveTask(ActionEvent event) {
        Integer index = ActionUtil.hasParam(event, ATTRIB_INDEX) ? ActionUtil.getParam(event, ATTRIB_INDEX, Integer.class) : (Integer) event.getComponent().getAttributes()
                .get(ATTRIB_INDEX);
        
        NodeRef compoundWfNodeRef = null;
    	try {
    		if (compoundWorkflow != null && compoundWorkflow.getNodeRef() != null) {
    			compoundWfNodeRef = compoundWorkflow.getNodeRef();
    		} else {
    			compoundWfNodeRef = new NodeRef(getMyTasks().get(index).getStoreRef() + "/" + getMyTasks().get(index).getCompoundWorkflowId());
    		}
    	} catch (Throwable t) {
    		log.error("Error getting compoundWfNodeRef: " + t.getMessage());
    	}
    	boolean locked = (compoundWfNodeRef != null)?setLock(FacesContext.getCurrentInstance(), compoundWfNodeRef, "workflow_compond_locked_for_change"):false;
        if (compoundWfNodeRef == null || locked) {
        	try {
		        List<Pair<String, Object>> params = new ArrayList<Pair<String, Object>>();
		        params.add(new Pair<String, Object>(ATTRIB_INDEX, index));
		        // Save all changes to independent workflow before updating task.
		        if (!saveIfIndependentWorkflow(params, SAVE_TASK, event)) {
		            return;
		        }
		
		        try {
		            Task task = reloadWorkflow(index);
		            addRemovedFiles(task);
		            boolean opinionFilesUploaded = WorkflowSpecificModel.Types.OPINION_TASK.equals(task.getType()) && hasUploadedFiles(task);
		            getWorkflowService().saveInProgressTask(task);
		            // as service operates on copy of task, we need to clear files lists here also
		            // force reloading files
		            task.clearFiles();
		            // clear removed files
		            task.getRemovedFiles().clear();
		            MessageUtil.addInfoMessage("save_success");
		            if (opinionFilesUploaded) {
		                MessageUtil.addInfoMessage("task_save_opinionTask_files_uploaded");
		            }
		        } catch (WorkflowChangedException e) {
		            handleWorkflowChangedException(e, "Saving task failed", "workflow_task_save_failed", log);
		        }
        	} finally {
        		if (compoundWfNodeRef != null) {
        			getDocLockService().unlockIfOwner(compoundWfNodeRef);
        		}
        	}
        	notifyDialogsIfNeeded();
        }
        
    }

    protected boolean saveIfIndependentWorkflow(List<Pair<String, Object>> params, String workflowBlockCallback, ActionEvent event) {
        if (compoundWorkflow != null && compoundWorkflowDialog != null && compoundWorkflow.isIndependentWorkflow()) {
            boolean confirmationsProcessed = ActionUtil.hasParam(event, ATTRIB_FINISH_VALIDATED) && ActionUtil.getParam(event, ATTRIB_FINISH_VALIDATED, Boolean.class);
            String response = null;
            if (confirmationsProcessed) {
                response = compoundWorkflowDialog.saveOrConfirmValidatedWorkflow(null, true);
            } else {
                params.add(new Pair<String, Object>(ATTRIB_FINISH_VALIDATED, Boolean.TRUE));
                response = compoundWorkflowDialog.saveWorkflow(FacesContext.getCurrentInstance(), workflowBlockCallback, params, null, false);
            }
            return StringUtils.isNotBlank(response);
        }
        return true; // CaseFile and Document workflows

    }
    
    /**
     * Sets compound workflow lock
     * @return
     */
    private boolean setLock(final FacesContext context, final NodeRef compoundWfNodeRef, final String lockMsgKey) {
    	RetryingTransactionHelper txnHelper = Repository.getRetryingTransactionHelper(context);
        RetryingTransactionCallback<Boolean> callback = new RetryingTransactionCallback<Boolean>()
        {
           public Boolean execute() throws Throwable
           {
        	   
        	   LockStatus lockStatus = getDocLockService().setLockIfFree(compoundWfNodeRef);
               boolean result;
               
	           	if (lockStatus == LockStatus.LOCK_OWNER) {
	           		result = true;
	            } else {
	            	String lockOwner = StringUtils.substringBefore(getDocLockService().getLockOwnerIfLocked(compoundWfNodeRef), "_");
	                String lockOwnerName = getUserService().getUserFullNameAndId(lockOwner);
	               	MessageUtil.addErrorMessage(context, lockMsgKey, lockOwnerName);
	               	result = false;
                }
                return result;
           }
        };
        
        return txnHelper.doInTransaction(callback, false, true);
    	
    }
    
    public void finishTask(ActionEvent event) throws Exception {
    	String lockMsgKey = "workflow_compond_locked_for_change";
    	Integer index = ActionUtil.getEventParamOrAttirbuteValue(event, ATTRIB_INDEX, Integer.class);
    	Integer delegableTaskIndex = ActionUtil.getEventParamOrAttirbuteValue(event, DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, Integer.class);
		
        if (delegableTaskIndex != null && delegationBean.hasTasksForDelegation(getMyTasks().get(index).getNodeRef())) {
        	lockMsgKey = "workflow_compond_locked_for_delegate";
        }

    	NodeRef compoundWfNodeRef = null;
    	try {
    		if (compoundWorkflow != null && compoundWorkflow.getNodeRef() != null) {
    			compoundWfNodeRef = compoundWorkflow.getNodeRef();
    		} else {
    			compoundWfNodeRef = new NodeRef(getMyTasks().get(index).getStoreRef() + "/" + getMyTasks().get(index).getCompoundWorkflowId());
    		}
    	} catch (Throwable t) {
    		log.error("Error getting compoundWfNodeRef: " + t.getMessage());
    	}
    	
    	boolean locked = (compoundWfNodeRef != null)?setLock(FacesContext.getCurrentInstance(), compoundWfNodeRef, lockMsgKey):false;
        if (compoundWfNodeRef == null || locked) {
        	boolean canUnlock = true; // signing tasks will be unlocked later
        	lockedCompoundWorkflowNodeRef = compoundWfNodeRef;
        	try {
		        Integer outcomeIndex = ActionUtil.getEventParamOrAttirbuteValue(event, ATTRIB_OUTCOME_INDEX, Integer.class);
		
		        if (delegableTaskIndex != null && delegationBean.hasTasksForDelegation(getMyTasks().get(index).getNodeRef())) {
		            delegationBean.delegate(event, delegableTaskIndex);
		            return;
		        }
		
		        List<Pair<String, Object>> params = new ArrayList<>();
		        params.add(new Pair<String, Object>(ATTRIB_INDEX, index));
		        params.add(new Pair<String, Object>(ATTRIB_OUTCOME_INDEX, outcomeIndex));
		
		        if (!saveIfIndependentWorkflow(params, FINISH_TASK, event)) {
		            return;
		        }
		
		        // saving independent compound workflow has succeeded at this point,
		        // we need to update blocks no matter if saving task succeeds or not
		
		        Task task = reloadWorkflow(index);
		        if (task == null) {
		            return;
		        }
		        QName taskType = task.getNode().getType();
		
		        if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)
		                || WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
		            Integer nodeOutcome = (Integer) task.getNode().getProperties().get(WorkflowSpecificModel.Props.TEMP_OUTCOME.toString());
		            if (nodeOutcome != null) {
		                outcomeIndex = nodeOutcome;
		            }
		        } else if (WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)) {
		            if (SignatureTaskOutcome.SIGNED_IDCARD.equals((int) outcomeIndex) || SignatureTaskOutcome.SIGNED_MOBILEID.equals((int) outcomeIndex)) {
		                canUnlock = false;
		            	prepareSigning(outcomeIndex, task, false);
		                return;
		            }
		        } else if (task.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK) && outcomeIndex == DueDateExtensionWorkflowType.DUE_DATE_EXTENSION_OUTCOME_NOT_ACCEPTED) {
		            task.setConfirmedDueDate(null);
		        } else if (task.isType(WorkflowSpecificModel.Types.OPINION_TASK) && CollectionUtils.isEmpty(task.getFiles())) {
		            BeanHelper.getWorkflowService().loadTaskFiles(task);
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
		            addRemovedFiles(task);
		            boolean opinionFilesUploaded = false;
		            if (WorkflowSpecificModel.Types.OPINION_TASK.equals(taskType)) {
		                opinionFilesUploaded = CollectionUtils.isNotEmpty(task.getFiles());
		            }
		            getWorkflowService().finishInProgressTask(task, outcomeIndex);
		            MessageUtil.addInfoMessage("task_finish_success_defaultMsg");
		            if (opinionFilesUploaded) {
		                MessageUtil.addInfoMessage("task_finish_opinionTask_files_uploaded");
		            }
		        } catch (InvalidNodeRefException e) {
		            final FacesContext context = FacesContext.getCurrentInstance();
		            MessageUtil.addErrorMessage(context, "task_finish_error_docDeleted");
		            WebUtil.navigateTo(AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME, context);
		            return;
		        } catch (NodeLockedException e) {
		            log.error("Finishing task failed", e);
		            BeanHelper.getDocumentLockHelperBean().handleLockedNode("task_finish_error_document_locked", e);
		        } catch (WorkflowChangedException e) {
		            CompoundWorkflowDialog.handleWorkflowChangedException(e, "Finishing task failed", "workflow_task_save_failed", log);
		        } catch (WorkflowActiveResponsibleTaskException e) {
		            log.debug("Finishing task failed: more than one active responsible task!", e);
		            MessageUtil.addErrorMessage("workflow_compound_save_failed_responsible");
		        }
        	} finally {
        		if (canUnlock && compoundWfNodeRef != null) {
        			getDocLockService().unlockIfOwner(compoundWfNodeRef);
        		}
        	}
        	notifyDialogsIfNeeded();
        }
        
    }

    private boolean hasUploadedFiles(Task task) {
        List<Object> files = task.getFiles();
        if (CollectionUtils.isEmpty(files)) {
            return false;
        }
        for (Object file : files) {
            if (file instanceof UploadFileInput.FileWithContentType) {
                return true;
            }
        }
        return false;
    }

    private Task reloadWorkflow(Integer index) {
        // get task that has not been saved
        Task task = getMyTasks().get(index);

        if (!(compoundWorkflow != null && compoundWorkflow.isIndependentWorkflow() && task.isSaved())) {
            return task;
        }

        // For performance reasons, preserve only needed properties, not whole workflow hierarchy
        Map<String, Object> changedProps = getWorkflowService().getTaskChangedProperties(task);
        NodeRef taskRef = task.getNodeRef();
        Status status = Status.of(task.getStatus());
        List<Object> files = task.getFiles();
        List<File> unsavedRemovedFiles = removedFiles;
        task = null;

        // init this bean with compoundWorkflowDialog data
        compoundWorkflowDialog.initWorkflowBlockBean();

        removedFiles = unsavedRemovedFiles;
        Task updatedTask = null;
        outer: for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task updated : workflow.getTasks()) {
                if (taskRef.equals(updated.getNodeRef())) {
                    updatedTask = updated;
                    break outer;
                }
            }
        }
        if (updatedTask != null) {
            if (!updatedTask.isStatus(status)) {
                if (log.isDebugEnabled()) {
                    log.error("Task status has changed during saving compound workflow, finishing task is not possible. Original status=" + status +
                            ", saved task=\n" + task);
                }
                MessageUtil.addErrorMessage("workflow_task_finish_failed");
            } else {
                updatedTask.copyFiles(files);
                task = updatedTask;
                task.getNode().getProperties().putAll(changedProps);
            }
        } else {
            // should never actually happen
            if (log.isDebugEnabled()) {
                log.error("Task with nodeRef=" + taskRef + " not found after saving compound workflow with nodeRef=" + compoundWorkflow.getNodeRef());
            }
            MessageUtil.addErrorMessage("workflow_task_finish_failed");
        }

        return task;
    }

    private void prepareSigning(Integer outcomeIndex, Task task, boolean isOwnerDocSigning) {
        final boolean signTogether = WorkflowUtil.isSignTogetherType((String) getNodeService().getProperty(task.getWorkflowNodeRef(), WorkflowSpecificModel.Props.SIGNING_TYPE));
        signingFlow = new SigningFlowContainer(((SignatureTask) task).clone(), signTogether, compoundWorkflow != null ? compoundWorkflow.getNodeRef() : null, containerRef);
        boolean signingPrepared = signingFlow.prepareSigning();
        if (!signingPrepared) {
        	if (lockedCompoundWorkflowNodeRef != null) {
            	getDocLockService().unlockIfOwner(lockedCompoundWorkflowNodeRef);
            }
            return;
        }
        if (SignatureTaskOutcome.SIGNED_IDCARD.equals((int) outcomeIndex)) {
        	showModalDigidoc4j();
        } else {
            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            signingFlow.resolveUserPhoneNr(session);
            if (isOwnerDocSigning) {
                notifyDialogsOnClosingLayer(getMobileIdPhoneNrModal());
            }
            showMobileIdModalOrSign();
        }
    }

    @SuppressWarnings("unchecked")
    private void notifyDialogsOnClosingLayer(ModalLayerComponent modalLayer) {
        if (modalLayer != null) {
            modalLayer.getAttributes().put(ValidatingModalLayerComponent.ATTR_ON_CLOSE_CALLBACK_BINDING, NOTIFY_DIALOGS_BINDING);
        }
    }

    private void addRemovedFiles(Task task) {
        for (File taskFile : getFileService().getFiles(getWorkflowDbService().getTaskFileNodeRefs(task.getNodeRef()))) {
            for (File file : getRemovedFiles()) {
                if (taskFile.getNodeRef().equals(file.getNodeRef())) {
                    task.getRemovedFiles().add(taskFile.getNodeRef());
                }
            }
        }
        removedFiles = null;
    }

    public boolean showOrderAssignmentCategory() {
        return getWorkflowConstantsBean().getOrderAssignmentCategoryEnabled();
    }

    public void sendTaskDueDateExtensionRequest(ActionEvent event) {
        ModalLayerSubmitEvent commentEvent;
        List<Pair<String, Object>> params = new ArrayList<Pair<String, Object>>();
        String reason;
        Date newDate;
        Date dueDate;
        Integer taskIndex;
        String extender;
        String extenderFullName;
        if (event instanceof ModalLayerSubmitEvent) {
            commentEvent = (ModalLayerSubmitEvent) event;
            reason = (String) commentEvent.getSubmittedValue(MODAL_KEY_REASON);
            newDate = (Date) commentEvent.getSubmittedValue(MODAL_KEY_PROPOSED_DUE_DATE);
            dueDate = (Date) commentEvent.getSubmittedValue(MODAL_KEY_DUE_DATE);
            taskIndex = commentEvent.getActionIndex();
            extender = dueDateExtenderUsername;
            extenderFullName = dueDateExtenderUserFullname;
            params.add(new Pair<String, Object>(MODAL_KEY_REASON, reason));
            TypeConverter typeConverter = DefaultTypeConverter.INSTANCE;
            params.add(new Pair<String, Object>(MODAL_KEY_PROPOSED_DUE_DATE, typeConverter.convert(String.class, newDate)));
            params.add(new Pair<String, Object>(MODAL_KEY_DUE_DATE, typeConverter.convert(String.class, dueDate)));
            params.add(new Pair<String, Object>(MODAL_KEY_EXTENDER, dueDateExtenderUsername));
            params.add(new Pair<String, Object>(MODAL_KEY_EXTENDER_FULL_NAME, dueDateExtenderUserFullname));
            params.add(new Pair<String, Object>(ATTRIB_INDEX, taskIndex));
        } else {
            reason = ActionUtil.getParam(event, MODAL_KEY_REASON);
            newDate = ActionUtil.getParam(event, MODAL_KEY_PROPOSED_DUE_DATE, Date.class);
            dueDate = ActionUtil.getParam(event, MODAL_KEY_DUE_DATE, Date.class);
            taskIndex = ActionUtil.getParam(event, ATTRIB_INDEX, Integer.class);
            extender = ActionUtil.getParam(event, MODAL_KEY_EXTENDER);
            extenderFullName = ActionUtil.getParam(event, MODAL_KEY_EXTENDER_FULL_NAME);
        }
        NodeRef compoundWfNodeRef = null;
    	try {
    		if (compoundWorkflow != null && compoundWorkflow.getNodeRef() != null) {
    			compoundWfNodeRef = compoundWorkflow.getNodeRef();
    		} else {
    			compoundWfNodeRef = new NodeRef(getMyTasks().get(taskIndex).getStoreRef() + "/" + getMyTasks().get(taskIndex).getCompoundWorkflowId());
    		}
    	} catch (Throwable t) {
    		log.error("Error getting compoundWfNodeRef: " + t.getMessage());
    	}
    	
    	boolean locked = (compoundWfNodeRef != null)?setLock(FacesContext.getCurrentInstance(), compoundWfNodeRef, "workflow_compond_locked_for_change"):false;
        if (compoundWfNodeRef == null || locked) {
        	try {

		        // Save independent workflow first
		        if (!saveIfIndependentWorkflow(params, SEND_TASK_DUE_DATE_EXTENSION_REQUEST, event)) {
		            return;
		        }
		
		        if (StringUtils.isBlank(reason) || newDate == null || dueDate == null || taskIndex == null || taskIndex < 0) {
		            return;
		        }
		
		        Task initiatingTask = reloadWorkflow(taskIndex);
		
		        getWorkflowService().createDueDateExtension(reason, newDate, dueDate, initiatingTask, containerRef, extender, extenderFullName);
		
		        MessageUtil.addInfoMessage("task_sendDueDateExtensionRequest_success_defaultMsg");
        	
	        } finally {
	    		if (compoundWfNodeRef != null) {
	    			getDocLockService().unlockIfOwner(compoundWfNodeRef);
	    		}
	    	}
        	notifyDialogsIfNeeded();
	    }
        
    }

    public static List<Pair<String, String>> validate(Task task, Integer outcomeIndex) {
        QName taskType = task.getNode().getType();
        if (WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)) {
            if (SignatureTaskOutcome.NOT_SIGNED.equals((int) outcomeIndex) && StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_signatureTask_comment", null));
            }
        } else if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)) {
            if ((outcomeIndex == 1 || outcomeIndex == 2) && StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_reviewTask_comment", null));
            }
        } else if (WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
            if (outcomeIndex == 1 && StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_externalReviewTask_comment", null));
            }
        } else if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK, WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_TASK)) {
            if (StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_assignmentTask_comment", null));
            }
        } else if (outcomeIndex == 1 && task.isType(WorkflowSpecificModel.Types.CONFIRMATION_TASK)) {
            if (StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_confirmationTask_comment", null));
            }
        } else if (WorkflowSpecificModel.Types.OPINION_TASK.equals(taskType)) {
            if (StringUtils.isBlank(task.getComment()) && (task.getFiles() == null || task.getFiles().isEmpty())) {
                return Arrays.asList(new Pair<String, String>("task_validation_opinionTask_comment", null));
            }
        } else if (WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK.equals(taskType)) {
            if (task.getConfirmedDueDate() == null && !outcomeIndex.equals(DueDateExtensionWorkflowType.DUE_DATE_EXTENSION_OUTCOME_NOT_ACCEPTED)) {
                return Arrays.asList(new Pair<String, String>("task_validation_dueDateExtensionTask_confirmedDueDate", null));
            }
        }
        return null;
    }

    public List<NodeRef> getCompoundWorkflows() {
        if (compoundWorkflows == null) {
            getRestoredCompoundWorkflows();
        }
        return compoundWorkflows;
    }

    public List<Task> getMyTasks() {
        if (myTasks == null) {
            getRestoredMyTasks(getRestoredCompoundWorkflows());
        }
        return myTasks;
    }

    public TaskDataProvider getFinishedReviewTasks() {
        if (finishedReviewTasks == null) {
            finishedReviewTasks = new TaskDataProvider(getRestoredCompoundWorkflows(), WorkflowSpecificModel.Types.REVIEW_TASK);
        }
        return finishedReviewTasks;
    }

    public TaskDataProvider getFinishedOpinionTasks() {
        if (finishedOpinionTasks == null) {
            finishedOpinionTasks = new TaskDataProvider(getRestoredCompoundWorkflows(), WorkflowSpecificModel.Types.OPINION_TASK);
        }
        return finishedOpinionTasks;
    }

    public TaskDataProvider getFinishedOrderAssignmentTasks() {
        if (finishedOrderAssignmentTasks == null) {
            finishedOrderAssignmentTasks = new TaskDataProvider(getRestoredCompoundWorkflows(), WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK);
        }
        return finishedOrderAssignmentTasks;
    }

    public boolean getReviewNoteBlockRendered() {
        return getFinishedReviewTasks().getListSize() != 0;
    }

    public String getReviewNotesPrintUrl() {
        return getPrintTableUrl(TableMode.REVIEW_NOTES, true);
    }

    public String getCompoundWorkflowPrintUrl() {
        return getPrintTableUrl(TableMode.COMPOUND_WORKFLOW, true);
    }

    public String getWorkflowGroupTasksUrl() {
        return getPrintTableUrl(TableMode.WORKFLOW_GROUP_TASKS, false);
    }

    private String getPrintTableUrl(TableMode mode, boolean addContextPath) {
        String requestContextPath = "";
        if (addContextPath) {
            requestContextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
        }
        if (!StringUtils.endsWith(requestContextPath, "/")) {
            requestContextPath += "/";
        }
        return requestContextPath + "printTable?" + PrintTableServlet.TABLE_MODE + "=" + mode;
    }

    public boolean getOpinionNoteBlockRendered() {
        return getFinishedOpinionTasks().getListSize() != 0;
    }

    public boolean getOrderAssignmentNoteBlockRendered() {
        return getFinishedOrderAssignmentTasks().getListSize() != 0;
    }

    public String getWorkflowMenuLabel() {
        return MessageUtil.getMessage(MSG_WORKFLOW_ACTION_GROUP);
    }

    public String getDocumentWorkflowMenuTooltip() {
        return MessageUtil.getMessage("compoundWorkflow_new_document_workflow");
    }

    public String getIndependentWorkflowMenuTooltip() {
        return MessageUtil.getMessage("compoundWorkflow_new_independent_workflow");
    }

    public void processCert() {
        @SuppressWarnings("unchecked")
        Map<String, String> requestParameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String certHex = requestParameterMap.get("certHex");
        String certId = requestParameterMap.get("certId");
        try {
            long step0 = System.currentTimeMillis();
            if (!signingFlow.collectAndCheckSigningFiles()) {
                closeModal();
                resetSigningData();
                return;
            }
            SignatureDigest signatureDigest = getDocumentService().prepareDocumentDigest(signingFlow.getSigningDocument(0), certHex,
                    signingFlow.getMainDocumentRef() != null ? compoundWorkflow.getNodeRef() : null);
            long step1 = System.currentTimeMillis();
            showModal(signatureDigest.getDigestHex(), certId);
            signingFlow.setSignatureDigest(signatureDigest);
            if (log.isInfoEnabled()) {
                log.info("prepareDocumentDigest took total time " + (step1 - step0) + " ms\n    service call - " + (step1 - step0) + " ms");
            }
        } catch (SignatureException e) {
            SignatureBlockBean.addSignatureError(e);
            closeModal();
            resetSigningData();
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            closeModal();
            resetSigningData();
        }
    }

    public void signDocument() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String signatureHex = (String) facesContext.getExternalContext().getRequestParameterMap().get("signatureHex");

        try {
            boolean finishTask = signingFlow.isFinishTaskStep();
            boolean finishedSigning = signingFlow.signDocumentImpl(signatureHex);
            if (!finishedSigning) {
                resetSigningData();
            } else {
            	// unlock if compound wf was locked
                if (lockedCompoundWorkflowNodeRef != null) {
                	getDocLockService().unlockIfOwner(lockedCompoundWorkflowNodeRef);
                }
            }
            notifyDialogsIfNeeded(false, finishTask);
        } finally {
            if (signingFlow == null || signingFlow.isSigningQueueEmpty()) {
                closeModal();
                resetSigningData();
            } else {
                showModalOrSign();
            }
        }
    }

    private void showModalOrSign() {
        if (signingFlow.needsSignatureInput(signingFlow.getSigningDocument(0))) {
            showModal();
        } else {
            signDocument();
        }
    }

    private void showMobileIdModalOrSign() {
        if (signingFlow.needsSignatureInput(signingFlow.getSigningDocument(0))) {
            getMobileIdPhoneNrModal().setRendered(true);
        } else {
            finishMobileIdSigning(null);
        }
    }

    public void notifyDialogsIfNeeded() {
        notifyDialogsIfNeeded(false, true);
    }

    public void notifyDialogsIfNeeded(boolean resetExpandedData, boolean initWorkflowBlock) {
        if (compoundWorkflow == null || !compoundWorkflow.isIndependentWorkflow()) {
            getDocumentDialogHelperBean().switchMode(false);
            BeanHelper.getDocumentDynamicDialog().clearRequestCache();
        }
        if (compoundWorkflow != null && compoundWorkflowDialog != null) {
            compoundWorkflowDialog.reload(compoundWorkflow != null ? compoundWorkflow.getNodeRef() : null, resetExpandedData, initWorkflowBlock);
        }
    }

    public void cancelSign() {
        closeModalDigidoc4j();
        resetSigningData();
        notifyDialogsIfNeeded();
    }
    
    public void closeSignSuccess() {
        closeModalDigidoc4j();
        resetSigningData();
        MessageUtil.addInfoMessage("task_finish_success_defaultMsg");
        notifyDialogsIfNeeded();
    }

    public void resetSigningData() {
        signingFlow = null;
    }

    public void startMobileIdSigning(@SuppressWarnings("unused") ActionEvent event) {
        boolean signingStarted = false;
        try {
            String phoneNr = signingFlow.getPhoneNumber();
            if (StringUtils.isNotBlank(phoneNr)) {
                HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
                session.setAttribute(SigningFlowContainer.LAST_USED_MOBILE_ID_NUMBER, phoneNr);
            }
            signingStarted = signingFlow.startMobileIdSigning();
            ModalLayerComponent mobileIdChallengeModal = getMobileIdChallengeModal();
            notifyDialogsOnClosingLayer(mobileIdChallengeModal);
            mobileIdChallengeModal.setRendered(true);
            challengeId = "<div id=\"mobileIdChallengeMessage\" style=\"text-align: center;\"><p>Sõnumit saadetakse, palun oodake...</p><p>Kontrollkood:</p><p id=\"mobileIdChallengeId\" style=\"padding-top: 10px; font-size: 28px; vertical-align: middle;\">"
                    + StringEscapeUtils.escapeXml(signingFlow.getChallengeId()) + "</p></div><script type=\"text/javascript\">$jQ(document).ready(function(){ "
                    + "window.setTimeout(getMobileIdSignature, 2000); "
                    + "});</script>";
            if (!signingStarted) {
                resetSigningData();
                // unlock if compound wf was locked
                if (lockedCompoundWorkflowNodeRef != null) {
                	getDocLockService().unlockIfOwner(lockedCompoundWorkflowNodeRef);
                }
            }
        } finally {
            getMobileIdPhoneNrModal().setRendered(false);
        }
    }
    
    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void getMobileIdSignature() throws IOException {
        ResponseWriter out = FacesContext.getCurrentInstance().getResponseWriter();
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        String requestParamChallengeId = (String) externalContext.getRequestParameterMap().get("mobileIdChallengeId");
        if (signingFlow != null) {
            out.write(signingFlow.getMobileIdSignature(requestParamChallengeId));
        } else {
            out.write(SigningFlowContainer.handleInvalidSigantureState());
        }
    }
    
    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void getDigidoc4jHash() throws IOException {
        ResponseWriter out = FacesContext.getCurrentInstance().getResponseWriter();
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        String certInHex = (String) externalContext.getRequestParameterMap().get("certInHex");
        
        try {
            long step0 = System.currentTimeMillis();
            if (!signingFlow.collectAndCheckSigningFiles()) {
            	closeModalDigidoc4j();
                resetSigningData();
                return;
            }
            SignatureDigest signatureDigest = getDocumentService().prepareDocumentDigest(signingFlow.getSigningDocument(0), certInHex,
                    signingFlow.getMainDocumentRef() != null ? compoundWorkflow.getNodeRef() : null);
            long step1 = System.currentTimeMillis();
            signingFlow.setSignatureDigest(signatureDigest);
            out.write(signatureDigest.getDigestHex());
            if (log.isInfoEnabled()) {
                log.info("prepareDocumentDigest took total time " + (step1 - step0) + " ms\n    service call - " + (step1 - step0) + " ms");
            }
        } catch (SignatureException e) {
            SignatureBlockBean.addSignatureError(e);
            closeModalDigidoc4j();
            resetSigningData();
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            closeModalDigidoc4j();
            resetSigningData();
        }
    }
    
    //@ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void finishDigidoc4jSigning() throws IOException {
    	@SuppressWarnings("unchecked")
        Map<String, String> requestParameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String signInHex = requestParameterMap.get("signInHex");
        
        try {
            boolean finishTask = signingFlow.isFinishTaskStep();
            boolean finishedSigning = signingFlow.signDocumentImpl(signInHex);
            if (!finishedSigning) {
                resetSigningData();
            } else {
            	// unlock if compound wf was locked
                if (lockedCompoundWorkflowNodeRef != null) {
                	getDocLockService().unlockIfOwner(lockedCompoundWorkflowNodeRef);
                }
            }
            notifyDialogsIfNeeded(false, finishTask);
        } finally {
            if (signingFlow == null || signingFlow.isSigningQueueEmpty()) {
            	closeModalDigidoc4j();
                resetSigningData();
                notifyDialogsIfNeeded();
            } else {
            	// TODO: redo digidoc4j
                showModalOrSign();
            }
        }
        
        
    }

    public void finishMobileIdSigning(@SuppressWarnings("unused") ActionEvent event) {
        try {
            boolean finishTask = signingFlow.isFinishTaskStep();
            boolean finishedMobileIdSigning = signingFlow.finishMobileIdSigning();
            if (!finishedMobileIdSigning) {
                resetSigningData();
            } else {
            	// unlock if compound wf was locked
                if (lockedCompoundWorkflowNodeRef != null) {
                	getDocLockService().unlockIfOwner(lockedCompoundWorkflowNodeRef);
                }
            }
            notifyDialogsIfNeeded(false, finishTask);
        } finally {
            getMobileIdChallengeModal().setRendered(false);
            if (signingFlow == null || signingFlow.isSigningQueueEmpty()) {
                resetSigningData();
            } else {
                showMobileIdModalOrSign();
            }
        }
    }

    /**
     * Callback to generate the drop down of outcomes for reviewTask.
     */
    public List<SelectItem> getReviewTaskOutcomes(FacesContext context, UIInput selectComponent) {
        int outcomes = getWorkflowConstantsBean().getWorkflowTypes().get(WorkflowSpecificModel.Types.REVIEW_WORKFLOW).getTaskOutcomes();
        List<SelectItem> selectItems = new ArrayList<SelectItem>(outcomes);

        for (int i = 0; i < outcomes; i++) {
            String label = MessageUtil.getMessage("task_action_outcome_reviewTask" + i);
            selectItems.add(new SelectItem(i, label));
        }
        return selectItems;
    }

    public List<SelectItem> getExternalReviewTaskOutcomes(FacesContext context, UIInput selectComponent) {
        int outcomes = getWorkflowConstantsBean().getWorkflowTypes().get(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW).getTaskOutcomes();
        List<SelectItem> selectItems = new ArrayList<SelectItem>(outcomes);

        for (int i = 0; i < outcomes; i++) {
            String label = MessageUtil.getMessage("task_outcome_externalReviewTask" + i + "_title");
            selectItems.add(new SelectItem(i, label));
        }
        return selectItems;
    }

    public void constructTaskPanelGroup(String action) {
        constructTaskPanelGroup(getDataTableGroupInner(), action);
    }

    /**
     * Manually generate a panel group with everything.
     */

    private void constructTaskPanelGroup(HtmlPanelGroup panelGroup, String action) {
        List<UIComponent> panelGroupChildren = ComponentUtil.getChildren(panelGroup);
        panelGroupChildren.clear();
        FacesContext context = FacesContext.getCurrentInstance();
        Application app = context.getApplication();

        generateSignatureAppletModal(panelGroupChildren, app);

        List<Task> taskList = getMyTasks(); // Restrict loading
        ValidatingModalLayerComponent dueDateExtensionLayer = addDueDateExtensionLayerIfNeeded(panelGroupChildren, context, app, taskList);

        for (int index = 0; index < taskList.size(); index++) {
            Task myTask = taskList.get(index);
            Node myTaskNode = myTask.getNode();
            QName taskType = myTaskNode.getType();
            String taskNodeRefId = myTaskNode.getId();

            UIPropertySheet sheet = new WMUIPropertySheet();
            if (isDelegatableTask(taskType)) {
                // must use a copy of tasks workflow, as there might be at the same time 2 tasks of the same workflow for delegation
                myTask = initDelegatableTask(sheet, myTask.getNodeRef(), taskType);
                myTaskNode = myTask.getNode();
                taskType = myTaskNode.getType();

                taskList.set(index, myTask);
            } else if (myTask.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK) && myTask.getConfirmedDueDate() == null) {
                myTask.setConfirmedDueDate(myTask.getProposedDueDate());
            }

            // the main block panel
            UIPanel panel = constructTaskPanel(taskType, taskNodeRefId);

            // the properties
            assignTaskPropertySheetProperties(index, myTaskNode, taskNodeRefId, sheet);
            getChildren(panel).add(sheet);

            HtmlPanelGroup panelGrid = new HtmlPanelGroup();
            panelGrid.setStyleClass("task-sheet-buttons");
            // panel grid with a column for every button

            // save button used only for some task types
            List<UIComponent> panelGridChildren = getChildren(panelGrid);
            if (isTaskSaveButtonRendered(myTask)) {
                panelGridChildren.add(createTaskSaveButton(app, taskNodeRefId, taskType, index));
            }

            createOutcomeButtons(app, myTask, myTaskNode, index, panelGridChildren);

            if (myTask.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK, WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && StringUtils.isNotBlank(myTask.getCreatorId())) {
                HtmlCommandButton extensionButton = createDueDateExtensionButton(dueDateExtensionLayer, myTask, taskNodeRefId, index, action);
                panelGridChildren.add(extensionButton);
            }

            getChildren(panel).add(panelGrid);
            panelGroupChildren.add(panel);
        }
    }

    private Task initDelegatableTask(UIPropertySheet sheet, NodeRef myTaskNodeRef, QName taskType) {
        Task myTaskCopy = BeanHelper.getWorkflowService().getTaskWithParents(myTaskNodeRef); // Delegation needs complete hirarchy
        Pair<Integer, Task> delegatableTask = delegationBean.initDelegatableTask(myTaskCopy);
        int delegatableTaskIndex = delegatableTask.getFirst();
        putAttribute(sheet, DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, delegatableTaskIndex);

        Task myTask = delegatableTask.getSecond();// first copy of myTask - stored in delegationBean and used in propertySheet
        if (WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK.equals(taskType) && myTask.getProp(WorkflowSpecificModel.Props.SEND_ORDER_ASSIGNMENT_COMPLETED_EMAIL) == null) {
            myTask.setProp(WorkflowSpecificModel.Props.SEND_ORDER_ASSIGNMENT_COMPLETED_EMAIL, Boolean.TRUE);
        }
        return myTask;
    }

    private boolean isDelegatableTask(QName taskType) {
        return WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(taskType)
                || WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK.equals(taskType)
                || WorkflowSpecificModel.Types.INFORMATION_TASK.equals(taskType) && getWorkflowConstantsBean().isInformationWorkflowDelegationEnabled()
                || isDelegatableTaskWithButton(taskType);
    }

    private boolean isDelegatableTaskWithButton(QName taskType) {
        return WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType) && getWorkflowConstantsBean().isReviewWorkflowDelegationEnabled()
                || WorkflowSpecificModel.Types.OPINION_TASK.equals(taskType) && getWorkflowConstantsBean().isOpinionWorkflowDelegationEnabled();
    }

    private HtmlCommandButton createDueDateExtensionButton(ValidatingModalLayerComponent dueDateExtensionLayer, Task myTask, String taskNodeRefId, int taskIndex, String action) {
        HtmlCommandButton extensionButton = new HtmlCommandButton();
        extensionButton.setId("ask-due-date-extension-" + taskNodeRefId);
        extensionButton.setStyleClass("taskOutcome");
        Map<String, Object> extensionBtnAttributes = getAttributes(extensionButton);
        extensionBtnAttributes.put(ATTRIB_INDEX, taskIndex);
        // postpone generating onClick js to rendering phase when we have parent form present
        if (dueDateExtensionLayer != null) {
            extensionBtnAttributes.put(HtmlButtonRenderer.ATTR_ONCLICK_DATA, new Pair<UIComponent, Integer>(dueDateExtensionLayer, taskIndex));
            log.debug("Attatching dueDateExtensionLayer to component id=" + extensionButton.getId() + ", rebuild action=" + action + " parent="
                    + dueDateExtensionLayer.getParent()
                    + ", parent id=" + (dueDateExtensionLayer.getParent() != null ? dueDateExtensionLayer.getParent().getId() : "null") + ", task=" + myTask);
        }
        extensionButton.setRendererType(HtmlButtonRenderer.HTML_BUTTON_RENDERER_TYPE);

        extensionButton.setValue(MessageUtil.getMessage("task_ask_due_date_extension"));
        extensionButton.setStyleClass("taskOutcome");
        extensionBtnAttributes.put(ATTRIB_INDEX, taskIndex);
        return extensionButton;
    }

    private void createOutcomeButtons(Application app, Task myTask, Node myTaskNode, int index, List<UIComponent> panelGridChildren) {
        QName taskType = myTaskNode.getType();
        boolean addDelegatableTaskIndex = DELEGABLE_TASKS.contains(taskType);
        String label = "task_outcome_" + taskType.getLocalName();
        String buttonSuffix = "_title";
        for (int outcomeIndex = 0; outcomeIndex < myTask.getOutcomes(); outcomeIndex++) {
            if (isMobileIdOutcomeAndMobileIdDisabled(taskType, outcomeIndex)) {
                continue;
            }
            HtmlCommandButton outcomeButton = createOutcomeButton(app, label, buttonSuffix, index, outcomeIndex, addDelegatableTaskIndex);
            panelGridChildren.add(outcomeButton);

            // the review and external review task has only 1 button and the outcomes come from TEMP_OUTCOME property
            if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)
                    || WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
                // node.getProperties().put(WorkflowSpecificModel.Props.TEMP_OUTCOME.toString(), 0);
                outcomeButton.setValue(MessageUtil.getMessage(label + buttonSuffix));
                break;
            }
        }
        if (isDelegatableTaskWithButton(taskType)) {
            HtmlCommandButton outcomeButton = DelegateButtonGenerator.createDelegateButton(app, myTaskNode);
            setOutcomeAttributes(outcomeButton, index, 0, true);
            panelGridChildren.add(outcomeButton);
        }
    }

    private HtmlCommandButton createOutcomeButton(Application app, String label, String buttonSuffix, int taskIndex, int outcomeIndex, boolean addDelegatableTaskIndex) {
        HtmlCommandButton outcomeButton = new HtmlCommandButton();
        outcomeButton.setId("outcome-id-" + taskIndex + "-" + outcomeIndex);
        outcomeButton.setActionListener(app.createMethodBinding("#{WorkflowBlockBean.finishTask}", new Class[] { ActionEvent.class }));
        outcomeButton.setValue(MessageUtil.getMessage(label + outcomeIndex + buttonSuffix));
        setOutcomeAttributes(outcomeButton, taskIndex, outcomeIndex, addDelegatableTaskIndex);
        return outcomeButton;
    }

    private void setOutcomeAttributes(HtmlCommandButton outcomeButton, int index, int outcomeIndex, boolean addDelegatableTaskIndex) {
        Map<String, Object> outcomeBtnAttributes = ComponentUtil.putAttribute(outcomeButton, "styleClass", "taskOutcome");
        outcomeBtnAttributes.put(ATTRIB_INDEX, index);
        outcomeBtnAttributes.put(ATTRIB_OUTCOME_INDEX, outcomeIndex);
        if (addDelegatableTaskIndex) {
            outcomeBtnAttributes.put(DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, index);
        }
    }

    private boolean isTaskSaveButtonRendered(Task myTask) {
        return myTask.isType(WorkflowSpecificModel.Types.OPINION_TASK,
                WorkflowSpecificModel.Types.REVIEW_TASK,
                WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK,
                WorkflowSpecificModel.Types.CONFIRMATION_TASK);
    }

    private HtmlCommandButton createTaskSaveButton(Application app, String taskNodeRefId, QName taskType, int index) {
        HtmlCommandButton saveButton = new HtmlCommandButton();
        saveButton.setId("save-id-" + taskNodeRefId);
        saveButton.setActionListener(app.createMethodBinding("#{WorkflowBlockBean.saveTask}", new Class[] { ActionEvent.class }));
        saveButton.setValue(MessageUtil.getMessage("task_save_" + taskType.getLocalName()));
        saveButton.setStyleClass("taskOutcome");
        getAttributes(saveButton).put(ATTRIB_INDEX, index);
        return saveButton;
    }

    private void assignTaskPropertySheetProperties(int index, Node node, String taskNodeRefId, UIPropertySheet sheet) {
        sheet.setId("task-sheet-" + taskNodeRefId);
        sheet.setNode(node);
        // this ensures we can use more than 1 property sheet on the page
        sheet.setVar("taskNode" + index);
        Map<String, Object> sheetAttributes = ComponentUtil.getAttributes(sheet);
        sheetAttributes.put("externalConfig", Boolean.TRUE);
        sheetAttributes.put("labelStyleClass", "propertiesLabel");
        sheetAttributes.put("columns", 1);
        sheet.setRendererType(PropertySheetGridRenderer.class.getCanonicalName());
    }

    private UIPanel constructTaskPanel(QName taskType, String taskNodeRefId) {
        UIPanel panel = new UIPanel();
        panel.setId("workflow-task-block-panel-" + taskNodeRefId);
        panel.setLabel(MessageUtil.getMessage("task_title_main") + MessageUtil.getMessage("task_title_" + taskType.getLocalName()));
        panel.setProgressive(true);
        getAttributes(panel).put("styleClass", "panel-100 workflow-task-block");
        return panel;
    }

    private ValidatingModalLayerComponent addDueDateExtensionLayerIfNeeded(List<UIComponent> panelGroupChildren, FacesContext context, Application app, List<Task> taskList) {
        for (Task task : taskList) {
            if (task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK, WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
                ValidatingModalLayerComponent dueDateExtensionLayer = addDueDateExtensionLayer(panelGroupChildren, context, app, task.getCreatorId(), task.getCreatorName());
                log.debug("Added dueDateExtensionLayer to parent=" + dueDateExtensionLayer.getParent());
                return dueDateExtensionLayer;
            }
        }

        return null;
    }

    
    
    private void generateSignatureAppletModal(List<UIComponent> panelGroupChildren, Application app) {
        // 1st child must always be SignatureAppletModalComponent
        panelGroupChildren.add(new Digidoc4jSignatureModalComponent());

        if (getDigiDoc4JSignatureService().isMobileIdEnabled()) {
            ValidatingModalLayerComponent mobileIdPhoneNrComponent = (ValidatingModalLayerComponent) app.createComponent(ValidatingModalLayerComponent.class.getCanonicalName());
            mobileIdPhoneNrComponent.setId("mobileIdPhoneNrModal");
            mobileIdPhoneNrComponent.setRendered(false);
            mobileIdPhoneNrComponent.setActionListener(app.createMethodBinding("#{WorkflowBlockBean.startMobileIdSigning}", UIActions.ACTION_CLASS_ARGS));
            Map<String, Object> mobileIdPhoneNrAttributes = getAttributes(mobileIdPhoneNrComponent);
            mobileIdPhoneNrAttributes.put(ModalLayerComponent.ATTR_HEADER_KEY, "task_title_signatureTask");
            mobileIdPhoneNrAttributes.put(ModalLayerComponent.ATTR_SUBMIT_BUTTON_MSG_KEY, "task_outcome_signatureTask2_title");
            mobileIdPhoneNrAttributes.put(ModalLayerComponent.ATTR_AUTO_SHOW, Boolean.TRUE);
            mobileIdPhoneNrAttributes.put(ModalLayerComponent.ATTR_SET_RENDERED_FALSE_ON_CLOSE, Boolean.TRUE);

            // 2nd child must always be mobileIdPhoneNrComponent
            panelGroupChildren.add(mobileIdPhoneNrComponent);

            UIInput phoneNrInput = (UIInput) app.createComponent(UIInput.COMPONENT_TYPE);
            phoneNrInput.setId("phoneNr");
            phoneNrInput.setValueBinding("value", app.createValueBinding("#{WorkflowBlockBean.phoneNr}"));
            Map<String, Object> attributes = getAttributes(phoneNrInput);
            attributes.put(ValidatingModalLayerComponent.ATTR_LABEL_KEY, "signatureTask_phoneNr");
            attributes.put(ValidatingModalLayerComponent.ATTR_MANDATORY, Boolean.TRUE);
            attributes.put(ValidatingModalLayerComponent.ATTR_ADDITIONAL_VALIDATION_ARG, SigningFlowContainer.EE_COUNTRY_CODE);
            getChildren(mobileIdPhoneNrComponent).add(phoneNrInput);

            UIInput defaultPhoneNr = (UIInput) app.createComponent(UISelectBoolean.COMPONENT_TYPE);
            defaultPhoneNr.setId("defaultTelephoneForSigning");
            defaultPhoneNr.setValueBinding("value", app.createValueBinding("#{WorkflowBlockBean.defaultTelephoneForSigning}"));
            Map<String, Object> attributes2 = getAttributes(defaultPhoneNr);
            attributes2.put(ValidatingModalLayerComponent.ATTR_LABEL_KEY, "signatureTask_phoneNr_markDefault");
            getChildren(mobileIdPhoneNrComponent).add(defaultPhoneNr);

            ModalLayerComponent mobileIdChallengeComponent = (ModalLayerComponent) app.createComponent(ModalLayerComponent.class.getCanonicalName());
            mobileIdChallengeComponent.setId("mobileIdChallengeModal");
            mobileIdChallengeComponent.setRendered(false);
            mobileIdChallengeComponent.setActionListener(app.createMethodBinding("#{WorkflowBlockBean.finishMobileIdSigning}", UIActions.ACTION_CLASS_ARGS));
            Map<String, Object> mobileIdChallengeAttributes = getAttributes(mobileIdChallengeComponent);
            mobileIdChallengeAttributes.put(ModalLayerComponent.ATTR_HEADER_KEY, "task_title_signatureTask");
            mobileIdChallengeAttributes.put(ModalLayerComponent.ATTR_SUBMIT_BUTTON_MSG_KEY, "task_outcome_signatureTask2_title");
            mobileIdChallengeAttributes.put(ModalLayerComponent.ATTR_SUBMIT_BUTTON_HIDDEN, Boolean.TRUE);
            mobileIdChallengeAttributes.put(ModalLayerComponent.ATTR_AUTO_SHOW, Boolean.TRUE);
            mobileIdChallengeAttributes.put(ModalLayerComponent.ATTR_SET_RENDERED_FALSE_ON_CLOSE, Boolean.TRUE);

            // 3rd child must always be mobileIdChallengeComponent
            panelGroupChildren.add(mobileIdChallengeComponent);

            UIOutput challengeOutput = (UIOutput) app.createComponent(UIOutput.COMPONENT_TYPE);
            challengeOutput.setValueBinding("value", app.createValueBinding("#{WorkflowBlockBean.challengeId}"));
            getAttributes(challengeOutput).put("escape", Boolean.FALSE);
            getChildren(mobileIdChallengeComponent).add(challengeOutput);
        }

        panelGroupChildren.add(generateLinkWithParam(app, "processCert", "#{" + BEAN_NAME + ".processCert}", "cert"));
        panelGroupChildren.add(generateLinkWithParam(app, "signDocument", "#{" + BEAN_NAME + ".signDocument}", "signature"));
        panelGroupChildren.add(generateLinkWithParam(app, "cancelSign", "#{" + BEAN_NAME + ".cancelSign}", null));
        panelGroupChildren.add(generateLinkWithParam(app, "closeSignSuccess", "#{" + BEAN_NAME + ".closeSignSuccess}", null));
    }

    public static boolean isMobileIdOutcomeAndMobileIdDisabled(QName taskType, int outcomeIndex) {
        return isMobileIdOutcome(taskType, outcomeIndex) && !getDigiDoc4JSignatureService().isMobileIdEnabled();
    }

    public static boolean isMobileIdOutcome(QName taskType, int outcomeIndex) {
        return WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType) && WorkflowSpecificModel.SignatureTaskOutcome.SIGNED_MOBILEID.equals(outcomeIndex);
    }

    private ValidatingModalLayerComponent addDueDateExtensionLayer(List<UIComponent> panelGroupChildren, FacesContext context, Application app, String defaultUsername,
            String defaultUserFullname) {

        ValidatingModalLayerComponent dueDateExtensionLayer = (ValidatingModalLayerComponent) app.createComponent(ValidatingModalLayerComponent.class.getCanonicalName());
        dueDateExtensionLayer.setId(TASK_DUE_DATE_EXTENSION_ID);
        Map<String, Object> layerAttributes = ComponentUtil.getAttributes(dueDateExtensionLayer);
        layerAttributes.put(ModalLayerComponent.ATTR_HEADER_KEY, "workflow_dueDateExtensionRequest");
        layerAttributes.put(ModalLayerComponent.ATTR_SUBMIT_BUTTON_MSG_KEY, "workflow_dueDateExtensionRequest_submit");

        List<UIComponent> layerChildren = ComponentUtil.getChildren(dueDateExtensionLayer);

        addDateInput(context, layerChildren, "workflow_dueDateExtension_proposedDueDate", MODAL_KEY_PROPOSED_DUE_DATE);

        TextAreaGenerator textAreaGenerator = new TextAreaGenerator();
        UIInput reasonInput = (UIInput) textAreaGenerator.generate(context, "task-due-date-extension-reason");
        reasonInput.setId(MODAL_KEY_REASON);
        Map<String, Object> reasonAttributes = ComponentUtil.getAttributes(reasonInput);
        reasonAttributes.put(ValidatingModalLayerComponent.ATTR_LABEL_KEY, "workflow_dueDateExtension_Reason");
        reasonAttributes.put(ValidatingModalLayerComponent.ATTR_MANDATORY, Boolean.TRUE);
        reasonAttributes.put("styleClass", "expand19-200");
        reasonAttributes.put("style", "height: 50px;");
        reasonInput.setValue(null);
        layerChildren.add(reasonInput);

        UserSearchGenerator userGenerator = new UserSearchGenerator();
        Search search = (Search) userGenerator.generate(context, "workflow_dueDateExtension_extender");
        search.setId(MODAL_KEY_EXTENDER);
        Map<String, Object> attributes = ComponentUtil.getAttributes(search);
        attributes.put(ValidatingModalLayerComponent.ATTR_LABEL_KEY, "workflow_dueDateExtension_extender");
        attributes.put(ValidatingModalLayerComponent.ATTR_MANDATORY, Boolean.TRUE);
        attributes.put(Search.PICKER_CALLBACK_KEY, "#{UserContactGroupSearchBean.searchAllWithoutLogOnUser}");
        attributes.put(Search.FILTER_INDEX, UserContactGroupSearchBean.USERS_FILTER);
        attributes.put(Search.SETTER_CALLBACK, "#{WorkflowBlockBean.assignDueDateExtender}");
        attributes.put(Search.DATA_TYPE_KEY, String.class);
        boolean taskCreatorExists = StringUtils.isNotBlank(defaultUsername) && (BeanHelper.getUserService().getUser(defaultUsername) != null);
        dueDateExtenderUsername = taskCreatorExists ? defaultUsername : null;
        dueDateExtenderUserFullname = taskCreatorExists ? defaultUserFullname : null;
        search.setValueBinding("value", context.getApplication().createValueBinding("#{WorkflowBlockBean.dueDateExtenderUserFullname}"));
        layerChildren.add(search);

        UIInput dueDateInput = addDateInput(context, layerChildren, "workflow_dueDateExtension_dueDate", MODAL_KEY_DUE_DATE);
        dueDateInput.setValue(CalendarUtil.addWorkingDaysToDate(new LocalDate(), 2, getClassificatorService()).toDateTimeAtCurrentTime().toDate());
        ComponentUtil.putAttribute(dueDateInput, ValidatingModalLayerComponent.ATTR_PRESERVE_VALUES, Boolean.TRUE);

        dueDateExtensionLayer.setActionListener(app.createMethodBinding("#{WorkflowBlockBean.sendTaskDueDateExtensionRequest}", UIActions.ACTION_CLASS_ARGS));
        panelGroupChildren.add(dueDateExtensionLayer);

        return dueDateExtensionLayer;
    }

    public void assignDueDateExtender(String result) {
        if (StringUtils.isBlank(result)) {
            return;
        }

        dueDateExtenderUsername = result;
        dueDateExtenderUserFullname = BeanHelper.getUserService().getUserFullName(dueDateExtenderUsername);
    }

    public UIInput addDateInput(FacesContext context, List<UIComponent> layerChildren, String labelKey, String inputId) {
        UIInput dateInput = (UIInput) (new DatePickerGenerator()).generate(context, inputId);
        ComponentUtil.createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, dateInput);
        Map<String, Object> attributes = ComponentUtil.getAttributes(dateInput);
        attributes.put(ValidatingModalLayerComponent.ATTR_LABEL_KEY, labelKey);
        attributes.put(ValidatingModalLayerComponent.ATTR_MANDATORY, Boolean.TRUE);
        attributes.put(ValidatingModalLayerComponent.ATTR_IS_DATE, Boolean.TRUE);
        attributes.put("styleClass", "date");
        layerChildren.add(dateInput);
        return dateInput;
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
        getIdCardModalApplet().showModal();
    }
    
    private void showModalDigidoc4j() {
    	getIdCardDigidoc4jModalComponent().showModal();
    }

    private void showModal(String digestHex, String certId) {
        getIdCardModalApplet().showModal(digestHex, certId);
    }

    private void closeModal() {
        getIdCardModalApplet().closeModal();
    }
    
    private void closeModalDigidoc4j() {
    	getIdCardDigidoc4jModalComponent().closeModal();
    }

    private SignatureAppletModalComponent getIdCardModalApplet() {
        return (SignatureAppletModalComponent) getDataTableGroupInner().getChildren().get(0);
    }
    
    private Digidoc4jSignatureModalComponent getIdCardDigidoc4jModalComponent() {
        return (Digidoc4jSignatureModalComponent) getDataTableGroupInner().getChildren().get(0);
    }

    private ValidatingModalLayerComponent getMobileIdPhoneNrModal() {
        return (ValidatingModalLayerComponent) getDataTableGroupInner().getChildren().get(1);
    }

    private ModalLayerComponent getMobileIdChallengeModal() {
        return (ModalLayerComponent) getDataTableGroupInner().getChildren().get(2);
    }

    private Map<NodeRef, Boolean> checkRights(Map<NodeRef, Boolean> workflowRights, NodeRef compoundWorkflowRef, List<NodeRef> workflowRefs) {
        if (compoundWorkflow != null) {
            return workflowRights;
        }
        QName parentType = BeanHelper.getNodeService().getType(containerRef);
        boolean isDocumentManager = getUserService().isDocumentManager();
        boolean isOwner = getDocumentDynamicService().isOwner(containerRef, AuthenticationUtil.getRunAsUser()) || getWorkflowService().isOwner(compoundWorkflowRef);
        boolean hasRights = false;
        if (isDocumentWorkflow(parentType)) {
            boolean localRights = isDocumentManager || isOwner
                    || getWorkflowDbService().isOwnerOfInProgressTask(Arrays.asList(compoundWorkflowRef), WorkflowSpecificModel.Types.ASSIGNMENT_TASK, false);
            boolean isEditable = !Boolean.TRUE.equals(container.getProperties().get(DocumentCommonModel.Props.NOT_EDITABLE));
            for (NodeRef workflowRef : workflowRefs) {
                if (localRights) {
                    hasRights = localRights && isEditable;
                }
                workflowRights.put(workflowRef, hasRights);
            }
        } else if (isCaseWorkflow(parentType)) {
            hasRights = isOwner || isAdminOrDocmanagerWithPermission(containerRef, Privilege.VIEW_CASE_FILE) || container.hasPermission(Privilege.EDIT_CASE_FILE);
            for (NodeRef workflowRef : workflowRefs) {
                workflowRights.put(workflowRef, hasRights);
            }
        }

        return workflowRights;
    }

    // START: getters / setters
    public void setDelegationBean(DelegationBean delegationBean) {
        this.delegationBean = delegationBean;
    }

    // NB! Don't call this method from java code; this is meant ONLY for workflow-block.jsp binding
    public HtmlPanelGroup getDataTableGroup() {
        HtmlPanelGroup dataTableGroup = (HtmlPanelGroup) BeanHelper.getJsfBindingHelper().getComponentBinding(getDataTableGroupBindingName());
        if (dataTableGroup == null) {
            dataTableGroup = new HtmlPanelGroup();
            BeanHelper.getJsfBindingHelper().addBinding(getDataTableGroupBindingName(), dataTableGroup);
        }
        taskPanelControlDocument = containerRef;
        return dataTableGroup;
    }

    private HtmlPanelGroup getDataTableGroupInner() {
        // This will be called once in the first RESTORE VIEW phase.
        HtmlPanelGroup dataTableGroup = (HtmlPanelGroup) BeanHelper.getJsfBindingHelper().getComponentBinding(getDataTableGroupBindingName());
        if (dataTableGroup == null) {
            dataTableGroup = new HtmlPanelGroup();
            BeanHelper.getJsfBindingHelper().addBinding(getDataTableGroupBindingName(), dataTableGroup);
        }
        return dataTableGroup;
    }

    public void setDataTableGroup(HtmlPanelGroup dataTableGroup) {
        if (taskPanelControlDocument != null && !taskPanelControlDocument.equals(containerRef)) {
            constructTaskPanelGroup(dataTableGroup, "setDataTableGroup");
            taskPanelControlDocument = containerRef;
        }
        BeanHelper.getJsfBindingHelper().addBinding(getDataTableGroupBindingName(), dataTableGroup);
    }

    protected String getDataTableGroupBindingName() {
        return getBindingName("dataTableGroup");
    }

    public UIRichList getReviewNotesRichList() {
        return null;
    }

    protected String getReviewNotesBindingName() {
        return getBindingName("reviewNotes");
    }

    public void setReviewNotesRichList(UIRichList reviewNotesRichList) {
        for (UIComponent component : ComponentUtil.getChildren(reviewNotesRichList)) {
            if (component instanceof UIColumn) {
                UIColumn column = (UIColumn) component;
                if ("review-note-block-file-versions-col".equals(column.getId())) {
                    column.setRendered(BeanHelper.getDialogManager().getCurrentDialog().getName().equals("documentDynamicDialog"));
                }
            }
        }
    }

    public List<WorkflowBlockItem> getWorkflowBlockItems() {
        if (groupedWorkflowBlockItems == null) {
            groupedWorkflowBlockItems = getWorkflowDbService().getWorkflowBlockItems(compoundWorkflows, checkWorkflowBlockItemRights(), getWorkflowGroupTaskUrl());
        }
        return groupedWorkflowBlockItems;
    }

    public WorkflowBlockItemDataProvider getWorkflowBlockItemDataProvider() {
        if (workflowBlockItemDataProvider == null) {
            PageLoadCallback<Integer, WorkflowBlockItem> callback = new PageLoadCallback<Integer, WorkflowBlockItem>() {

                private static final long serialVersionUID = 1L;

                @Override
                public void doWithPageItems(Map<Integer, WorkflowBlockItem> loadedRows) {
                    Collection<WorkflowBlockItem> items = loadedRows.values();
                    Map<String, List<DueDateHistoryRecord>> historyRecords = new HashMap<>();
                    for (WorkflowBlockItem item : items) {
                        List<DueDateHistoryRecord> records = item.getDueDateHistoryRecords();
                        if (CollectionUtils.isNotEmpty(records)) {
                            historyRecords.put(item.getTaskNodeRef().getId(), records);
                        }
                    }
                    updateDueDateHistoryPanel(historyRecords);
                }
            };

            workflowBlockItemDataProvider = new WorkflowBlockItemDataProvider(compoundWorkflows, checkWorkflowBlockItemRights(), getWorkflowGroupTaskUrl(), callback);
        }
        return workflowBlockItemDataProvider;
    }

    private String getWorkflowGroupTaskUrl() {
        return getWorkflowGroupTasksUrl() + "&amp;" + PrintTableServlet.WORKFLOW_ID + "=";
    }

    private Map<NodeRef, Boolean> checkWorkflowBlockItemRights() {
        Map<NodeRef, Boolean> workflowRights = new HashMap<NodeRef, Boolean>();
        for (Map.Entry<NodeRef, List<NodeRef>> entry : getWorkflowService().getChildWorkflowNodeRefsByCompoundWorkflow(compoundWorkflows).entrySet()) {
            checkRights(workflowRights, entry.getKey(), entry.getValue());
        }
        return workflowRights;
    }

    public String getSignOwnerDocBindingName() {
        return SIGN_OWNER_DOC_BINDING_NAME;
    }

    public void updateDueDateHistoryPanel(Map<String, List<DueDateHistoryRecord>> records) {
        @SuppressWarnings("unchecked")
        List<UIComponent> children = getDueDateHistoryModalPanel().getChildren();
        children.clear();

        for (Map.Entry<String, List<DueDateHistoryRecord>> entry : records.entrySet()) {
            if (CollectionUtils.isNotEmpty(entry.getValue())) {
                children.add(new DueDateHistoryModalComponent(FacesContext.getCurrentInstance(), entry.getKey(), entry.getValue()));
            }
        }
    }

    public String getSignOwnerDocLabel() {
        return MessageUtil.getMessage("document_sign");
    }

    public List<ActionDefinition> createSignOwnerDocMenu(@SuppressWarnings("unused") String s) {
        DocumentType documentType = BeanHelper.getDocumentDynamicDialog().getDocumentType();
        if (documentType == null) {
            return Collections.emptyList();
        }
        String docRef = BeanHelper.getDocumentDynamicDialog().getDocument().getNodeRef().toString();
        List<SignatureTaskOutcome> signingMethods = new ArrayList<>();
        signingMethods.add(SignatureTaskOutcome.SIGNED_IDCARD);
        boolean mobileIdEnabled = getDigiDoc4JSignatureService().isMobileIdEnabled();
        if (mobileIdEnabled) {
            signingMethods.add(SignatureTaskOutcome.SIGNED_MOBILEID);
        }
        List<ActionDefinition> actionDefs = new ArrayList<>(signingMethods.size());

        for (SignatureTaskOutcome signingMethod : signingMethods) {
            ActionDefinition def = new ActionDefinition("signOwnerDoc");
            def.Image = mobileIdEnabled ? DROPDOWN_MENU_ITEM_ICON : ARROW_RIGHT_ICON;
            def.Label = mobileIdEnabled ? OUTCOME_TO_LABEL.get(signingMethod.name()) : getSignOwnerDocLabel();
            def.ActionListener = SIGN_OWNER_DOC_ACTION_LISTENER;
            def.addParam(SIGNING_METHOD, signingMethod.name());
            def.addParam(DOC_REF_PARAM, docRef);
            actionDefs.add(def);
        }

        return actionDefs;
    }

    public void signOwnerDocument(ActionEvent event) {
        Task signatureTask = null;
        try {
            if (BeanHelper.getFileBlockBean().getActiveFilesCount() <= 0) {
                MessageUtil.addErrorMessage("task_files_required");
                return;
            }
            WorkflowConstantsBean wfConstants = getWorkflowConstantsBean();
            NodeRef docRef = ActionUtil.getParam(event, DOC_REF_PARAM, NodeRef.class);
            boolean createIndependentCompoundWorkflow = false;
            NodeRef wfParent;
            WorkflowService workflowService = BeanHelper.getWorkflowService();
            if (wfConstants.isDocumentWorkflowEnabled()) {
                wfParent = docRef;
            } else if (wfConstants.isIndependentWorkflowEnabled()) {
                wfParent = BeanHelper.getConstantNodeRefsBean().getIndependentWorkflowsRoot();
                createIndependentCompoundWorkflow = true;
            } else {
                throw new RuntimeException("Document workflow or independent workflow must be enabled");
            }

            CompoundWorkflow cwf = workflowService.getNewCompoundWorkflow(wfParent);
            if (wfConstants.isWorkflowTitleEnabled()) {
                cwf.setTitle(MessageUtil.getMessage("compoundWorkflow_sign_document_title"));
            }
            cwf.setTypeEnum(createIndependentCompoundWorkflow ? CompoundWorkflowType.INDEPENDENT_WORKFLOW : CompoundWorkflowType.DOCUMENT_WORKFLOW);
            if (createIndependentCompoundWorkflow) {
                cwf.setMainDocument(docRef);
                cwf.setProp(WorkflowCommonModel.Props.DOCUMENTS_TO_SIGN, (Serializable) Collections.singletonList(docRef));
                cwf.getNewAssocs().add(docRef);
            }
            Workflow signatureWorkflow = workflowService.addNewWorkflow(cwf, WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW, 0, false);
            String ownerId = AuthenticationUtil.getRunAsUser();
            signatureWorkflow.setOwnerId(ownerId);
            if (createIndependentCompoundWorkflow) {
                signatureWorkflow.setSigningType(SigningType.SIGN_TOGETHER);
            }
            signatureTask = signatureWorkflow.addTask();
            signatureTask.setOwnerId(ownerId);
            Node user = BeanHelper.getUserService().getUser(ownerId);
            Map<String, Object> properties = user.getProperties();
            Serializable orgName = (Serializable) BeanHelper.getOrganizationStructureService()
                    .getOrganizationStructurePaths((String) properties.get(ContentModel.PROP_ORGID.toPrefixString()));
            WorkflowUtil.setPersonPropsToTask(signatureTask, properties, orgName);
            signatureTask.setDueDate(new Date());

            cwf = workflowService.startCompoundWorkflow(cwf);
            signatureWorkflow = cwf.getWorkflows().get(0);
            signatureTask = signatureWorkflow.getTasks().get(0);

            MessageUtil.addInfoMessage("workflow_compound_start_success");
        } catch (Exception e) {
            MessageUtil.addInfoMessage("workflow_compound_start_workflow_failed");
            return;
        }
        String signingMethodStr = ActionUtil.getParam(event, SIGNING_METHOD, String.class);
        int outcomeIndex = SignatureTaskOutcome.valueOf(signingMethodStr).ordinal();
        prepareSigning(outcomeIndex, signatureTask, true);
    }

    public boolean isMobileIdDisabled() {
        return !getDigiDoc4JSignatureService().isMobileIdEnabled();
    }

    public CustomChildrenCreator getNoteBlockRowFileGenerator() {

        CustomChildrenCreator fileComponentCreator = new CustomChildrenCreator() {

            @Override
            public List<UIComponent> createChildren(Object params, int rowCounter) {
                List<UIComponent> components = new ArrayList<UIComponent>();
                if (params != null) {
                    Application application = FacesContext.getCurrentInstance().getApplication();
                    int fileCounter = 0;
                    Task task = (Task) params;
                    List<Object> paramObjects = task.getFiles();
                    for (Object obj : paramObjects) {
                        File file = (File) obj;
                        final UIActionLink fileLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
                        fileLink.setId("note-file-link-" + rowCounter + "-" + fileCounter);
                        fileLink.setValue("");
                        fileLink.setTooltip(file.getDisplayName());
                        fileLink.setShowLink(false);
                        fileLink.setHref(file.getDownloadUrl());
                        fileLink.setImage("/images/icons/attachment.gif");
                        fileLink.setTarget("_blank");
                        ComponentUtil.getAttributes(fileLink).put("styleClass", "inlineAction webdav-readOnly");
                        components.add(fileLink);
                        fileCounter++;
                    }
                }
                return components;
            }
        };
        return fileComponentCreator;
    }

    public List<File> getRemovedFiles() {
        if (removedFiles == null) {
            removedFiles = new ArrayList<File>();
        }
        return removedFiles;
    }

    public void setRemovedFiles(List<File> removedFiles) {
        this.removedFiles = removedFiles;
    }

    public SignatureTask getSignatureTask() {
        return signingFlow.getSignatureTask();
    }

    public String getPhoneNr() {
        return signingFlow != null ? signingFlow.getPhoneNumber() : null;
    }

    public void setPhoneNr(String phoneNr) {
        signingFlow.setPhoneNumber(phoneNr);
    }

    public boolean getDefaultTelephoneForSigning() {
        return signingFlow != null ? signingFlow.isDefaultTelephoneForSigning() : false;
    }

    public void setDefaultTelephoneForSigning(boolean defaultTelephoneForSigning) {
        signingFlow.setDefaultTelephoneForSigning(defaultTelephoneForSigning);
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setSignature(String signature) {
        signingFlow.setSignature(signature);
    }

    public String getDueDateExtenderUsername() {
        return dueDateExtenderUsername;
    }

    public void setDueDateExtenderUsername(String dueDateExtenderUsername) {
        this.dueDateExtenderUsername = dueDateExtenderUsername;
    }

    public String getDueDateExtenderUserFullname() {
        return dueDateExtenderUserFullname;
    }

    public void setDueDateExtenderUserFullname(String dueDateExtenderUserFullname) {
        this.dueDateExtenderUserFullname = dueDateExtenderUserFullname;
    }

    public HtmlPanelGroup getDueDateHistoryModalPanel() {
        HtmlPanelGroup modalPanel = (HtmlPanelGroup) getJsfBindingHelper().getComponentBinding(getModalPanelBindingName());
        if (modalPanel == null) {
            modalPanel = (HtmlPanelGroup) FacesContext.getCurrentInstance().getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            getJsfBindingHelper().addBinding(getModalPanelBindingName(), modalPanel);
        }
        return modalPanel;
    }

    protected String getModalPanelBindingName() {
        return getBindingName("modalPanel");
    }

    public void setDueDateHistoryModalPanel(HtmlPanelGroup dueDateHistoryModalPanel) {
        getJsfBindingHelper().addBinding(getModalPanelBindingName(), dueDateHistoryModalPanel);
    }

    protected String getBindingName(String name) {
        return this.getClass().getSimpleName() + "." + name;
    }

    public void setRequestCacheBean(RequestCacheBean requestCacheBean) {
        this.requestCacheBean = requestCacheBean;
    }

    // END: getters / setters

}
