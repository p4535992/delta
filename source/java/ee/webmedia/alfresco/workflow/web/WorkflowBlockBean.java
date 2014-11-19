package ee.webmedia.alfresco.workflow.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDvkService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getEInvoiceService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPrivilegeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSignatureService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowService;
import static ee.webmedia.alfresco.privilege.service.PrivilegeUtil.isAdminOrDocmanagerWithPermission;
import static ee.webmedia.alfresco.utils.ComponentUtil.getAttributes;
import static ee.webmedia.alfresco.utils.ComponentUtil.getChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;
<<<<<<< HEAD
=======
import static ee.webmedia.alfresco.workflow.web.CompoundWorkflowDialog.handleWorkflowChangedException;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
<<<<<<< HEAD
import java.util.HashMap;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
<<<<<<< HEAD
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConverter;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.ajax.InvokeCommand.ResponseMimetype;
import org.alfresco.web.bean.FileUploadBean;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIPanel;
<<<<<<< HEAD
import org.alfresco.web.ui.common.component.data.UIColumn;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.alfresco.web.ui.common.component.data.UIRichList;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;

import ee.webmedia.alfresco.app.AppConstants;
<<<<<<< HEAD
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
import ee.webmedia.alfresco.common.web.BeanHelper;
=======
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.search.UserSearchGenerator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.document.einvoice.web.TransactionsBlockBean;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
<<<<<<< HEAD
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.web.evaluator.DocumentNotInDraftsFunctionActionEvaluator;
=======
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.SignatureChallenge;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.web.SignatureAppletModalComponent;
import ee.webmedia.alfresco.signature.web.SignatureBlockBean;
<<<<<<< HEAD
import ee.webmedia.alfresco.utils.ActionUtil;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.workflow.exception.WorkflowActiveResponsibleTaskException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
<<<<<<< HEAD
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowBlockItem;
import ee.webmedia.alfresco.workflow.model.WorkflowBlockItemGroup;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel.SignatureTaskOutcome;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
<<<<<<< HEAD
import ee.webmedia.alfresco.workflow.service.DueDateHistoryRecord;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.workflow.service.SignatureTask;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import ee.webmedia.alfresco.workflow.service.type.DueDateExtensionWorkflowType;
import ee.webmedia.alfresco.workflow.web.PrintTableServlet.TableMode;

<<<<<<< HEAD
/**
 * @author Dmitri Melnikov
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class WorkflowBlockBean implements DocumentDynamicBlock {

    private static final long serialVersionUID = 1L;
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(WorkflowBlockBean.class);
    public static final String BEAN_NAME = "WorkflowBlockBean";

    private static final String WORKFLOW_METHOD_BINDING_NAME = "#{WorkflowBlockBean.findCompoundWorkflowDefinitions}";
<<<<<<< HEAD
    private static final String INDEPENDENT_WORKFLOW_METHOD_BINDING_NAME = "#{WorkflowBlockBean.findIndependentCompoundWorkflowDefinitions}";
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private static final String DROPDOWN_MENU_ITEM_ICON = "/images/icons/versioned_properties.gif";
    private static final String MSG_WORKFLOW_ACTION_GROUP = "workflow_compound_start_workflow";
    private static final String TASK_DUE_DATE_EXTENSION_ID = "task-due-date-extension";
    private static final String ATTRIB_OUTCOME_INDEX = "outcomeIndex";
<<<<<<< HEAD
    private static final String ATTRIB_MODAL_EVENT = "modalEvent";
    public static final String ATTRIB_FINISH_VALIDATED = "finishValidated";
    private static final String FINISH_TASK = "WorkflowBlockBean.finishTask";
    private static final String SAVE_TASK = "WorkflowBlockBean.saveTask";
    private static final String SEND_TASK_DUE_DATE_EXTENSION_REQUEST = "WorkflowBlockBean.sendTaskDueDateExtensionRequest";

    /** task index attribute name */
    public static final String ATTRIB_INDEX = "index";
    private static final String MODAL_KEY_REASON = "reason";
=======

    /** task index attribute name */
    private static final String ATTRIB_INDEX = "index";
    private static final String MODAL_KEY_REASON = "reason";
    private static final String MODAL_KEY_EXTENDER = "extender";
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private static final String MODAL_KEY_DUE_DATE = "dueDate";
    private static final String MODAL_KEY_PROPOSED_DUE_DATE = "proposedDueDate";
    private FileBlockBean fileBlockBean;
    private DelegationBean delegationBean;
    private TransactionsBlockBean transactionsBlockBean;
<<<<<<< HEAD
    private CompoundWorkflowDialog compoundWorkflowDialog;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    private transient HtmlPanelGroup dataTableGroup;
    private transient UIRichList reviewNotesRichList;

<<<<<<< HEAD
    private NodeRef containerRef;
    private Node container;
    private NodeRef taskPanelControlDocument;
    private List<CompoundWorkflow> compoundWorkflows;
    // in case of independent workflow, use only given workflow
    private CompoundWorkflow compoundWorkflow;
=======
    private NodeRef docRef;
    private Node document;
    private NodeRef taskPanelControlDocument;
    private List<CompoundWorkflow> compoundWorkflows;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private List<Task> myTasks;
    private List<Task> finishedReviewTasks;
    private List<Task> finishedOpinionTasks;
    private List<Task> finishedOrderAssignmentTasks;
    private List<WorkflowBlockItem> groupedWorkflowBlockItems;
    private SignatureTask signatureTask;
    private List<File> removedFiles;
    private String phoneNr;
    private String challengeId;
    private String signature;
    private MessageData signatureError;
<<<<<<< HEAD
    private List<NodeRef> signingQueue;
    Map<NodeRef, List<File>> signingFiles;
    /** Has non-null value if current signing is signing multiple (> 1) documents together */
    private NodeRef mainDocumentRef;
    private Map<NodeRef, String> originalStatuses;

    private String renderedModal;
    private transient UIPanel modalContainer;
=======
    private String dueDateExtenderUsername;
    private String dueDateExtenderUserFullname;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    @Override
    public void resetOrInit(DialogDataProvider provider) {
        if (provider == null) {
            reset();
        } else {
<<<<<<< HEAD
            compoundWorkflow = null;
            compoundWorkflowDialog = null;
            init(provider.getNode());
        }
        resetModals();
    }

    public void init(Node container) {
        this.container = container;
        containerRef = container.getNodeRef();
        delegationBean.setWorkflowBlockBean(this);
        // // jsp:include parameters are not taken in account in list construction if list is not nulled
        // reviewNotesRichList = null;
        restore("init");
    }

    public void initIndependentWorkflow(CompoundWorkflow compoundWorkflow, CompoundWorkflowDialog compoundWorkflowDialog) {
        // use copy of compound workflow because otherwise both
        // CompoundWorkflowDialog and WorkflowBlockBean display inputs are bind to same task object
        // resulting in undetermined task data
        this.compoundWorkflow = getWorkflowService().copyCompoundWorkflowInMemory(compoundWorkflow);
        this.compoundWorkflowDialog = compoundWorkflowDialog;
        init(new Node(BeanHelper.getWorkflowService().getIndependentWorkflowsRoot()));
    }

    public void reset() {
        container = null;
        containerRef = null;
        compoundWorkflows = null;
        compoundWorkflow = null;
=======
            init(provider.getNode());
        }
    }

    public void init(Node document) {
        this.document = document;
        docRef = document.getNodeRef();
        delegationBean.setWorkflowBlockBean(this);
        restore("init");
    }

    public void reset() {
        document = null;
        docRef = null;
        compoundWorkflows = null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        myTasks = null;
        finishedReviewTasks = null;
        finishedOpinionTasks = null;
        finishedOrderAssignmentTasks = null;
        groupedWorkflowBlockItems = null;
        signatureTask = null;
        dataTableGroup = null;
        removedFiles = null;
        delegationBean.reset();
        reviewNotesRichList = null;
<<<<<<< HEAD
        signingQueue = null;
        signingFiles = null;
        mainDocumentRef = null;
        originalStatuses = null;
    }

    public void restore(String action) {
        if (compoundWorkflow != null && compoundWorkflow.isIndependentWorkflow()) {
            compoundWorkflows = Arrays.asList(compoundWorkflow);
        } else if (BeanHelper.getWorkflowService().getIndependentWorkflowsRoot().equals(containerRef)) {
            compoundWorkflows = Collections.emptyList();
        } else if (containerRef != null) {
            compoundWorkflows = getWorkflowService().getCompoundWorkflows(containerRef);
        } else {
            compoundWorkflows = new ArrayList<CompoundWorkflow>();
        }
=======
        dueDateExtenderUsername = null;
        dueDateExtenderUserFullname = null;
    }

    public void restore(String action) {
        compoundWorkflows = getWorkflowService().getCompoundWorkflows(docRef);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        myTasks = getWorkflowService().getMyTasksInProgress(compoundWorkflows);
        Map<NodeRef, List<NodeRef>> taskFiles = BeanHelper.getWorkflowDbService().getCompoundWorkflowsTaskFiles(compoundWorkflows);
        getFiles(myTasks, taskFiles, false);
        finishedReviewTasks = WorkflowUtil.getFinishedTasks(compoundWorkflows, WorkflowSpecificModel.Types.REVIEW_TASK);

        finishedOpinionTasks = WorkflowUtil.getFinishedTasks(compoundWorkflows, WorkflowSpecificModel.Types.OPINION_TASK);
        getFiles(finishedOpinionTasks, taskFiles, true);
        finishedOrderAssignmentTasks = WorkflowUtil.getFinishedTasks(compoundWorkflows, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK);
        getFiles(finishedOrderAssignmentTasks, taskFiles, true);

        // jsp:include parameters are not taken in account in list construction if list is not nulled
        reviewNotesRichList = null;

<<<<<<< HEAD
        if (signingQueue == null || signingQueue.isEmpty()) {
            signatureTask = null;
        }
=======
        signatureTask = null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        removedFiles = null;
        delegationBean.reset();
        // rebuild the whole task panel
        constructTaskPanelGroup(action);

        if (!myTasks.isEmpty()) {
            List<String> taskTypes = new ArrayList<String>(5);

            for (Task task : myTasks) {
                String type = task.getType().getLocalName();

                if (!taskTypes.contains(type)) {
<<<<<<< HEAD
                    getLogService().addLogEntry(LogEntry.create(LogObject.WORKFLOW, getUserService(), containerRef, "applog_task_view", MessageUtil.getTypeName(task.getType())));
=======
                    getLogService().addLogEntry(LogEntry.create(LogObject.WORKFLOW, getUserService(), docRef, "applog_task_view", MessageUtil.getTypeName(task.getType())));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                    taskTypes.add(type);
                }
            }
        }
        clearFileUploadBean();
    }

    private void clearFileUploadBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        FileUploadBean fileBean = (FileUploadBean) context.getExternalContext().getSessionMap().
                get(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
        if (fileBean != null) {
            context.getExternalContext().getSessionMap().remove(FileUploadBean.FILE_UPLOAD_BEAN_NAME);
        }
    }

    private void getFiles(List<Task> tasks, Map<NodeRef, List<NodeRef>> taskFiles, boolean retrieveAll) {
        for (Task task : tasks) {
            if (retrieveAll || task.isType(WorkflowSpecificModel.Types.OPINION_TASK, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK)) {
                getWorkflowService().retrieveTaskFiles(task, taskFiles.get(task.getNodeRef()));
            }
        }
    }

<<<<<<< HEAD
    private void resetModals() {
        renderedModal = null;
        List<UIComponent> children = ComponentUtil.getChildren(getModalContainer());
        children.clear();
        DueDateHistoryModalComponent linkModal = new DueDateHistoryModalComponent();
        linkModal.setId("document-link-modal-container");
        children.add(linkModal);
    }

    public boolean isModalRendered() {
        return StringUtils.isNotBlank(renderedModal);
    }

    public String getFetchAndResetRenderedModal() {
        try {
            return renderedModal;
        } finally {
            renderedModal = null;
        }
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public boolean isCompoundWorkflowOwner() {
        return getWorkflowService().isOwner(getCompoundWorkflows());
    }

<<<<<<< HEAD
    public boolean isWorkflowSummaryBlockExpanded() {
        return compoundWorkflow != null && compoundWorkflow.isIndependentWorkflow() && !compoundWorkflow.isStatus(Status.NEW);
    }

    public boolean getIsShowDocumentWorkflowSummaryBlock() {
        return getWorkflowService().isDocumentWorkflowEnabled();
    }

    // This method (UIAction's "value" methodBinding) is called on every render, so caching results is useful
    @SuppressWarnings("unchecked")
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
        List<CompoundWorkflowDefinition> workflowDefs = wfService.getCompoundWorkflowDefinitionsByType(userId, compoundWorkflowType);

        for (Iterator<CompoundWorkflowDefinition> it = workflowDefs.iterator(); it.hasNext();) {
            CompoundWorkflowDefinition compoundWorkflowDefinition = it.next();
            if (!wfService.externalReviewWorkflowEnabled() && containsExternalReviewWorkflows(compoundWorkflowDefinition)) {
                it.remove();
            }
        }
        List<ActionDefinition> actionDefinitions = new ArrayList<ActionDefinition>(workflowDefs.size());
        if (CompoundWorkflowType.DOCUMENT_WORKFLOW.equals(compoundWorkflowType)) {
            boolean showCWorkflowDefsWith1Workflow = false;
            for (CompoundWorkflow cWorkflow : compoundWorkflows) {
                if (cWorkflow.isStatus(Status.IN_PROGRESS, Status.STOPPED) && cWorkflow.getWorkflows().size() > 1) {
                    showCWorkflowDefsWith1Workflow = true;
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
            boolean hasPrivEditDoc = getPrivilegeService().hasPermissions(containerRef, Privileges.EDIT_DOCUMENT);
            boolean hasViewPrivs = getPrivilegeService().hasPermissions(containerRef, Privileges.VIEW_DOCUMENT_META_DATA, Privileges.VIEW_DOCUMENT_FILES);
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
                        adminOrDocmanagerWithPermission = isAdminOrDocmanagerWithPermission(new Node(containerRef), Privileges.VIEW_DOCUMENT_META_DATA,
                                Privileges.VIEW_DOCUMENT_FILES);
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
=======
    // This method (UIAction's "value" methodBinding) is called on every render, so caching results is useful
    @SuppressWarnings("unchecked")
    public List<ActionDefinition> findCompoundWorkflowDefinitions(@SuppressWarnings("unused") String nodeTypeId) {
        if (document == null) {
            return Collections.emptyList();
        }

        boolean showCWorkflowDefsWith1Workflow = false;
        for (CompoundWorkflow cWorkflow : compoundWorkflows) {
            if (cWorkflow.isStatus(Status.IN_PROGRESS, Status.STOPPED) && cWorkflow.getWorkflows().size() > 1) {
                showCWorkflowDefsWith1Workflow = true;
            }
        }

        String documentTypeId = (String) document.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
        String documentStatus = (String) document.getProperties().get(DocumentCommonModel.Props.DOC_STATUS);

        WorkflowService wfService = getWorkflowService();
        List<CompoundWorkflowDefinition> workflowDefs = wfService.getCompoundWorkflowDefinitions(false);
        List<ActionDefinition> actionDefinitions = new ArrayList<ActionDefinition>(workflowDefs.size());
        String userId = AuthenticationUtil.getRunAsUser();
        // remove CompoundWorkflowDefinitions that shouldn't be visible the user viewing this document regardless permissions
        for (Iterator<CompoundWorkflowDefinition> it = workflowDefs.iterator(); it.hasNext();) {
            CompoundWorkflowDefinition compoundWorkflowDefinition = it.next();
            String cWFUserId = compoundWorkflowDefinition.getUserId();
            if (cWFUserId != null && !StringUtils.equals(cWFUserId, userId)) {
                it.remove(); // defined by other user for private use
            } else if (!compoundWorkflowDefinition.getDocumentTypes().contains(documentTypeId)) {
                it.remove(); // not for same DocType
            } else if (showCWorkflowDefsWith1Workflow && compoundWorkflowDefinition.getWorkflows().size() > 1) {
                it.remove(); // already have active cWorkflow with multiple workflows - allowed only one at the time
            } else if (!wfService.externalReviewWorkflowEnabled() && containsExternalReviewWorkflows(compoundWorkflowDefinition)) {
                it.remove();
            }
        }

        boolean isWorking = DocumentStatus.WORKING.getValueName().equals(documentStatus);
        boolean isFinished = DocumentStatus.FINISHED.getValueName().equals(documentStatus);
        boolean hasPrivEditDoc = getPrivilegeService().hasPermissions(docRef, Privileges.EDIT_DOCUMENT);
        boolean hasViewPrivs = getPrivilegeService().hasPermissions(docRef, Privileges.VIEW_DOCUMENT_META_DATA, Privileges.VIEW_DOCUMENT_FILES);
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
                    adminOrDocmanagerWithPermission = isAdminOrDocmanagerWithPermission(new Node(docRef), Privileges.VIEW_DOCUMENT_META_DATA, Privileges.VIEW_DOCUMENT_FILES);
                }
                if (adminOrDocmanagerWithPermission
                        && !hasOtherWFs(cWorkflowDef, WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.INFORMATION_WORKFLOW,
                                WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_WORKFLOW, WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)) {
                    actionDefinitions.add(createActionDef(cWorkflowDef));
                }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            }
        }
        TransformingComparator transformingComparator = new TransformingComparator(new Transformer() {
            @Override
            public Object transform(Object input) {
                return ((ActionDefinition) input).Label;
            }
        }, AppConstants.DEFAULT_COLLATOR);
        Collections.sort(actionDefinitions, transformingComparator);
        return actionDefinitions;
    }

<<<<<<< HEAD
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

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private ActionDefinition createActionDef(CompoundWorkflowDefinition compoundWorkflowDefinition) {
        ActionDefinition actionDefinition = new ActionDefinition("compoundWorkflowDefinitionAction");
        actionDefinition.Image = DROPDOWN_MENU_ITEM_ICON;
        actionDefinition.Label = compoundWorkflowDefinition.getName();
        actionDefinition.Action = "#{WorkflowBlockBean.getCompoundWorkflowDialog}";
        actionDefinition.ActionListener = "#{CompoundWorkflowDialog.setupNewWorkflow}";
<<<<<<< HEAD
        actionDefinition.addParam("parentNodeRef", container.getNodeRefAsString());
        actionDefinition.addParam("compoundWorkflowDefinitionNodeRef", compoundWorkflowDefinition.getNodeRef().toString());
        return actionDefinition;
    }

    private ActionDefinition createIndependentWorkflowActionDef(CompoundWorkflowDefinition compoundWorkflowDefinition) {
        ActionDefinition actionDefinition = createActionDef(compoundWorkflowDefinition);
        // override parent node set in createActionDef
        actionDefinition.addParam("parentNodeRef", getWorkflowService().getIndependentWorkflowsRoot().toString());
        actionDefinition.ActionListener = "#{CompoundWorkflowDialog.setupNewIndependentWorkflowFromDocument}";
        actionDefinition.addParam("docAssocNodeRef", container.getNodeRefAsString());
=======
        actionDefinition.addParam("compoundWorkflowDefinitionNodeRef", compoundWorkflowDefinition.getNodeRef().toString());
        actionDefinition.addParam("documentNodeRef", document.getNodeRefAsString());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
        return "dialog:compoundWorkflowDialog";
    }

    public String getWorkflowMethodBindingName() {
<<<<<<< HEAD
        try {
            // independent compound workflow
            if (compoundWorkflow != null) {
                return null;
            }
            QName parentType = null;
            if (containerRef != null && getNodeService().exists(containerRef)) {
                parentType = getNodeService().getType(containerRef);
            }
            if ((isDocumentWorkflow(parentType) && getWorkflowService().isDocumentWorkflowEnabled() && getPrivilegeService().hasPermissions(containerRef,
                    Privileges.VIEW_DOCUMENT_META_DATA, Privileges.VIEW_DOCUMENT_FILES)
                    && (containerRef == null || new DocumentNotInDraftsFunctionActionEvaluator().evaluate(new Node(containerRef))))
                    || isCaseWorkflow(parentType) && BeanHelper.getWorkflowService().hasNoStoppedOrInprogressCompoundWorkflows(containerRef)) {
                return WORKFLOW_METHOD_BINDING_NAME;
            }
            return null;
        } catch (RuntimeException e) {
            // Log error here, because JSF EL evaluator does not log detailed error cause
            log.error("Error getting workflowMethodBindingName", e);
            throw e;
        }
    }

    public String getIndependentWorkflowMethodBindingName() {
        try {
            if (compoundWorkflow == null
                    && getWorkflowService().isIndependentWorkflowEnabled()
                    && getPrivilegeService().hasPermissions(containerRef, Privileges.VIEW_DOCUMENT_META_DATA, Privileges.VIEW_DOCUMENT_FILES)
                    && (containerRef == null || !isDocumentWorkflow(BeanHelper.getNodeService().getType(containerRef))
                    || new DocumentNotInDraftsFunctionActionEvaluator().evaluate(new Node(containerRef)))) {
                return INDEPENDENT_WORKFLOW_METHOD_BINDING_NAME;
            }
            return null;
        } catch (RuntimeException e) {
            // Log error here, because JSF EL evaluator does not log detailed error cause
            log.error("Error getting independentWorkflowMethodBindingName", e);
            throw e;
        }
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
        // Save all changes to independent workflow before updating task.
        Integer index = ActionUtil.hasParam(event, ATTRIB_INDEX) ? ActionUtil.getParam(event, ATTRIB_INDEX, Integer.class) : (Integer) event.getComponent().getAttributes()
                .get(ATTRIB_INDEX);
        List<Pair<String, Object>> params = new ArrayList<Pair<String, Object>>();
        params.add(new Pair<String, Object>(ATTRIB_INDEX, index));
        if (!saveIfIndependentWorkflow(params, SAVE_TASK, event)) {
            return;
        }

        try {
            Task task = reloadWorkflow(index);
=======
        // Check if at least one condition is true, if not return null (don't show the button)
        // the logged in user is an admin or doc.manager
        // or user's id is document 'ownerId'
        // or user's id is 'taskOwnerId' and 'taskStatus' = IN_PROGRESS of some document's task

        if (getPrivilegeService().hasPermissions(docRef, Privileges.VIEW_DOCUMENT_META_DATA, Privileges.VIEW_DOCUMENT_FILES)) {
            return WORKFLOW_METHOD_BINDING_NAME;
        }
        return null;
    }

    public boolean isInWorkspace() {
        return docRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE);
    }

    public void saveTask(ActionEvent event) {
        Integer index = (Integer) event.getComponent().getAttributes().get(ATTRIB_INDEX);
        try {
            Task task = getMyTasks().get(index);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            addRemovedFiles(task);
            getWorkflowService().saveInProgressTask(task);
            // as service operates on copy of task, we need to clear files lists here also
            // force reloading files
            task.clearFiles();
            // clear removed files
            task.getRemovedFiles().clear();
            MessageUtil.addInfoMessage("save_success");
        } catch (WorkflowChangedException e) {
<<<<<<< HEAD
            log.debug("Saving task failed", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_task_save_failed");
        }
        notifyDialogsIfNeeded();
    }

    protected boolean saveIfIndependentWorkflow(List<Pair<String, Object>> params, String workflowBlockCallback, ActionEvent event) {
        if (compoundWorkflow != null && compoundWorkflowDialog != null && compoundWorkflow.isIndependentWorkflow()) {
            boolean confirmationsProcessed = ActionUtil.hasParam(event, ATTRIB_FINISH_VALIDATED) && ActionUtil.getParam(event, ATTRIB_FINISH_VALIDATED, Boolean.class);
            String response = null;
            if (confirmationsProcessed) {
                response = compoundWorkflowDialog.saveOrConfirmValidatedWorkflow(null, true);
            } else {
                params.add(new Pair<String, Object>(ATTRIB_FINISH_VALIDATED, Boolean.TRUE));
                response = compoundWorkflowDialog.saveWorkflow(FacesContext.getCurrentInstance(), workflowBlockCallback, params, null);
            }
            return StringUtils.isNotBlank(response);
        }
        return true; // CaseFile and Document workflows

    }

    public void finishTask(ActionEvent event) {
        Integer index = (Integer) (ActionUtil.hasParam(event, ATTRIB_INDEX) ? ActionUtil.getParam(event, ATTRIB_INDEX, Integer.class) : event.getComponent().getAttributes()
                .get(ATTRIB_INDEX));
        Integer outcomeIndex = (Integer) (ActionUtil.hasParam(event, ATTRIB_OUTCOME_INDEX) ? ActionUtil.getParam(event, ATTRIB_OUTCOME_INDEX, Integer.class) : event.getComponent()
                .getAttributes().get(ATTRIB_OUTCOME_INDEX));
        List<Pair<String, Object>> params = new ArrayList<Pair<String, Object>>();
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
=======
            handleWorkflowChangedException(e, "Saving task failed", "workflow_task_save_failed", log);
        }
        restore("saveTask");
    }

    public void finishTask(ActionEvent event) {
        Integer index = (Integer) event.getComponent().getAttributes().get(ATTRIB_INDEX);
        Integer outcomeIndex = (Integer) event.getComponent().getAttributes().get(ATTRIB_OUTCOME_INDEX);
        Task task = getMyTasks().get(index);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        QName taskType = task.getNode().getType();

        if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)
                || WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
<<<<<<< HEAD
            Integer nodeOutcome = (Integer) task.getNode().getProperties().get(WorkflowSpecificModel.Props.TEMP_OUTCOME.toString());
            if (nodeOutcome != null) {
                outcomeIndex = nodeOutcome;
            }
        } else if (WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)) {
            if (SignatureTaskOutcome.SIGNED_IDCARD.equals((int) outcomeIndex) || SignatureTaskOutcome.SIGNED_MOBILEID.equals((int) outcomeIndex)) {
                prepareSigning(outcomeIndex, task);
=======
            outcomeIndex = (Integer) task.getNode().getProperties().get(WorkflowSpecificModel.Props.TEMP_OUTCOME.toString());
        } else if (WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)) {
            if (SignatureTaskOutcome.SIGNED_IDCARD.equals((int) outcomeIndex) || SignatureTaskOutcome.SIGNED_MOBILEID.equals((int) outcomeIndex)) {

                // signing requires that at least 1 active file exists within this document
                long step0 = System.currentTimeMillis();
                List<File> activeFiles = getFileService().getAllActiveFiles(docRef);
                if (activeFiles == null || activeFiles.isEmpty()) {
                    MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "task_files_required");
                    return;
                }

                signatureTask = ((SignatureTask) task).clone();
                try {
                    long step1 = System.currentTimeMillis();
                    getDocumentService().prepareDocumentSigning(docRef);
                    long step2 = System.currentTimeMillis();
                    fileBlockBean.restore();
                    long step3 = System.currentTimeMillis();
                    if (log.isInfoEnabled()) {
                        log.info("prepareDocumentSigning took total time " + (step3 - step0) + " ms\n    load file list - " + (step1 - step0)
                                + " ms\n    service call - " + (step2 - step1) + " ms\n    reload file list - " + (step3 - step2) + " ms");
                    }
                } catch (UnableToPerformException e) {
                    MessageUtil.addStatusMessage(e);
                    return;
                }
                if (SignatureTaskOutcome.SIGNED_IDCARD.equals((int) outcomeIndex)) {
                    showModal();
                } else {
                    getMobileIdPhoneNrModal().setRendered(true);
                }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                return;
            }
        } else if (task.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK) && outcomeIndex == DueDateExtensionWorkflowType.DUE_DATE_EXTENSION_OUTCOME_NOT_ACCEPTED) {
            task.setConfirmedDueDate(null);
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
            getWorkflowService().finishInProgressTask(task, outcomeIndex);
            MessageUtil.addInfoMessage("task_finish_success_defaultMsg");
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "task_finish_error_docDeleted");
            WebUtil.navigateTo(AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME, context);
            return;
        } catch (NodeLockedException e) {
            log.error("Finishing task failed", e);
<<<<<<< HEAD
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("task_finish_error_document_locked", e);
        } catch (WorkflowChangedException e) {
            log.error("Finishing task failed", e);
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "workflow_task_save_failed");
=======
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("task_finish_error_document_locked");
        } catch (WorkflowChangedException e) {
            CompoundWorkflowDialog.handleWorkflowChangedException(e, "Finishing task failed", "workflow_task_save_failed", log);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        } catch (WorkflowActiveResponsibleTaskException e) {
            log.debug("Finishing task failed: more than one active responsible task!", e);
            MessageUtil.addErrorMessage("workflow_compound_save_failed_responsible");
        }
<<<<<<< HEAD
        notifyDialogsIfNeeded();
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

    private void prepareSigning(Integer outcomeIndex, Task task) {
        // signing requires that at least 1 active file exists within this document
        long step0 = System.currentTimeMillis();

        List<File> activeFiles = new ArrayList<File>();
        Map<NodeRef, List<File>> signingFiles = new HashMap<NodeRef, List<File>>();
        collectSigningFiles(activeFiles, signingFiles);
        if (!checkSigningFiles(activeFiles, false)) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("found " + activeFiles.size() + "active files for signing:\n");
            for (File activeFile : activeFiles) {
                log.debug("nodeRef=" + activeFile.getNodeRef() + "; displayName=" + activeFile.getDisplayName() + "\n");
            }
        }

        signatureTask = ((SignatureTask) task).clone();
        try {
            long step1 = System.currentTimeMillis();
            mainDocumentRef = null;
            signingQueue = new ArrayList<NodeRef>();
            originalStatuses = new HashMap<NodeRef, String>();
            boolean signSeparately = true;
            boolean hasExistingDigiDoc = false;
            List<NodeRef> signingDocumentRefs = null;
            if (compoundWorkflow == null) {
                signingQueue.add(containerRef);
                addDocumentStatus(containerRef);
            } else {
                signingDocumentRefs = getWorkflowService().getCompoundWorkflowSigningDocumentRefs(compoundWorkflow.getNodeRef());
                NodeRef compoundWorkflowRef = compoundWorkflow.getNodeRef();
                if (signatureTask.getParent().isSignTogether() && signingDocumentRefs.size() > 1) {
                    mainDocumentRef = compoundWorkflow.getMainDocument();
                    if (mainDocumentRef == null || !getNodeService().exists(mainDocumentRef)) {
                        throw new UnableToPerformException("compoundWorkflow_main_document_missing");
                    }
                    hasExistingDigiDoc = getDocumentService().checkExistingDdoc(mainDocumentRef, compoundWorkflowRef) != null;
                    signSeparately = false;
                    signingQueue.add(mainDocumentRef);
                    Map<NodeRef, List<File>> tmpFileMap = new HashMap<NodeRef, List<File>>();
                    List<File> tmpFiles = new ArrayList<File>();
                    tmpFileMap.put(mainDocumentRef, tmpFiles);
                    for (Map.Entry<NodeRef, List<File>> entry : signingFiles.entrySet()) {
                        List<File> documentFiles = entry.getValue();
                        addDocumentStatus(entry.getKey());
                        if (documentFiles != null) {
                            tmpFiles.addAll(documentFiles);
                        }
                    }
                    signingFiles = tmpFileMap;

                } else {
                    signingQueue.addAll(signingDocumentRefs);
                    addDocumentStatuses(signingQueue);
                }
            }
            this.signingFiles = signingFiles;
            getDocumentService().prepareDocumentSigning(signSeparately ? signingQueue : signingDocumentRefs, !hasExistingDigiDoc, signSeparately);
            long step2 = System.currentTimeMillis();
            if (compoundWorkflow == null) {
                fileBlockBean.restore();
            } else {
                BeanHelper.getCompoundWorkflowAssocListDialog().restored();
            }
            long step3 = System.currentTimeMillis();
            if (log.isInfoEnabled()) {
                log.info("prepareDocumentSigning took total time " + (step3 - step0) + " ms\n    load file list - " + (step1 - step0)
                        + " ms\n    service call - " + (step2 - step1) + " ms\n    reload file list - " + (step3 - step2) + " ms");
            }
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            return;
        }
        if (SignatureTaskOutcome.SIGNED_IDCARD.equals((int) outcomeIndex)) {
            showModalOrSign();
        } else {
            showMobileIdModalOrSign();
        }
    }

    public boolean checkSigningFiles(List<File> activeFiles, boolean checkReference) {
        if (activeFiles == null || activeFiles.isEmpty()) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "task_files_required");
            return false;
        }
        if (checkReference) {
            boolean hasNotReferenceFile = false;
            for (File file : activeFiles) {
                if (file.getGeneratedFileRef() == null) {
                    hasNotReferenceFile = true;
                    break;
                }
            }
            if (!hasNotReferenceFile) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "task_files_required");
                return false;
            }
        }
        if (hasZeroByteFile(activeFiles)) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "task_files_zero_byte_file");
            return false;
        }
        return true;
    }

    public void collectSigningFiles(List<File> activeFiles, Map<NodeRef, List<File>> signingFiles) {
        if (compoundWorkflow == null) {
            List<File> documentFiles = getFileService().getAllActiveFiles(containerRef);
            if (documentFiles != null) {
                activeFiles.addAll(documentFiles);
            }
            signingFiles.put(containerRef, activeFiles);
        } else {
            signingFiles.putAll(getWorkflowService().getCompoundWorkflowSigningFiles(compoundWorkflow));
            for (Map.Entry<NodeRef, List<File>> entry : signingFiles.entrySet()) {
                List<File> documentFiles = entry.getValue();
                if (documentFiles != null) {
                    activeFiles.addAll(documentFiles);
                }
            }
        }
    }

    private void addDocumentStatuses(List<NodeRef> docRefs) {
        if (docRefs != null) {
            for (NodeRef docRef : docRefs) {
                addDocumentStatus(docRef);
            }
        }
    }

    private void addDocumentStatus(NodeRef docRef) {
        originalStatuses.put(docRef, (String) getNodeService().getProperty(docRef, DocumentCommonModel.Props.DOC_STATUS));
    }

    private boolean needsSignatureInput(NodeRef nodeRef) {
        List<File> files = signingFiles.get(nodeRef);
        return files != null && !files.isEmpty();
    }

    private boolean hasZeroByteFile(List<File> files) {
        for (File file : files) {
            if (file.getSize() == 0) {
                return true;
            }
        }
        return false;
=======

        getDocumentDialogHelperBean().switchMode(false);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    private void addRemovedFiles(Task task) {
        for (File taskFile : getFileService().getFiles(BeanHelper.getWorkflowDbService().getTaskFileNodeRefs(task.getNodeRef()))) {
            for (File file : getRemovedFiles()) {
                if (taskFile.getNodeRef().equals(file.getNodeRef())) {
                    task.getRemovedFiles().add(taskFile.getNodeRef());
                }
            }
        }
        removedFiles = null;
    }

    public boolean showOrderAssignmentCategory() {
        return getWorkflowService().getOrderAssignmentCategoryEnabled();
    }

    public void sendTaskDueDateExtensionRequest(ActionEvent event) {
<<<<<<< HEAD
        ModalLayerSubmitEvent commentEvent;
        List<Pair<String, Object>> params = new ArrayList<Pair<String, Object>>();
        String reason;
        Date newDate;
        Date dueDate;
        Integer taskIndex;
        if (event instanceof ModalLayerSubmitEvent) {
            commentEvent = (ModalLayerSubmitEvent) event;
            reason = (String) commentEvent.getSubmittedValue(MODAL_KEY_REASON);
            newDate = (Date) commentEvent.getSubmittedValue(MODAL_KEY_PROPOSED_DUE_DATE);
            dueDate = (Date) commentEvent.getSubmittedValue(MODAL_KEY_DUE_DATE);
            taskIndex = commentEvent.getActionIndex();
            params.add(new Pair<String, Object>(MODAL_KEY_REASON, reason));
            TypeConverter typeConverter = DefaultTypeConverter.INSTANCE;
            params.add(new Pair<String, Object>(MODAL_KEY_PROPOSED_DUE_DATE, typeConverter.convert(String.class, newDate)));
            params.add(new Pair<String, Object>(MODAL_KEY_DUE_DATE, typeConverter.convert(String.class, dueDate)));
            params.add(new Pair<String, Object>(ATTRIB_INDEX, taskIndex));
        } else {
            reason = ActionUtil.getParam(event, MODAL_KEY_REASON);
            newDate = ActionUtil.getParam(event, MODAL_KEY_PROPOSED_DUE_DATE, Date.class);
            dueDate = ActionUtil.getParam(event, MODAL_KEY_DUE_DATE, Date.class);
            taskIndex = ActionUtil.getParam(event, ATTRIB_INDEX, Integer.class);
        }

        // Save independent workflow first
        if (!saveIfIndependentWorkflow(params, SEND_TASK_DUE_DATE_EXTENSION_REQUEST, event)) {
            return;
        }

        if (StringUtils.isBlank(reason) || newDate == null || dueDate == null || taskIndex == null || taskIndex < 0) {
            return;
        }

        Task initiatingTask = reloadWorkflow(taskIndex);

        getWorkflowService().createDueDateExtension(reason, newDate, dueDate, initiatingTask, containerRef);

        MessageUtil.addInfoMessage("task_sendDueDateExtensionRequest_success_defaultMsg");
        notifyDialogsIfNeeded();
=======
        ModalLayerSubmitEvent commentEvent = (ModalLayerSubmitEvent) event;
        String reason = (String) commentEvent.getSubmittedValue(MODAL_KEY_REASON);
        Date newDate = (Date) commentEvent.getSubmittedValue(MODAL_KEY_PROPOSED_DUE_DATE);
        Date dueDate = (Date) commentEvent.getSubmittedValue(MODAL_KEY_DUE_DATE);
        Integer taskIndex = commentEvent.getActionIndex();
        if (StringUtils.isBlank(reason) || newDate == null || dueDate == null || taskIndex == null || taskIndex < 0) {
            return;
        }
        Task initiatingTask = myTasks.get(taskIndex);

        getWorkflowService().createDueDateExtension(reason, newDate, dueDate, initiatingTask, docRef, dueDateExtenderUsername, dueDateExtenderUserFullname);

        MessageUtil.addInfoMessage("task_sendDueDateExtensionRequest_success_defaultMsg");
        getDocumentDialogHelperBean().switchMode(false);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    @SuppressWarnings("unchecked")
    private List<Pair<String, String>> validate(Task task, Integer outcomeIndex) {
        QName taskType = task.getNode().getType();
        if (WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)) {
            if (SignatureTaskOutcome.NOT_SIGNED.equals((int) outcomeIndex) && StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_signatureTask_comment", null));
            }
        } else if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)) {
            if ((outcomeIndex == 1 || outcomeIndex == 2) && StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_reviewTask_comment", null));
            }
<<<<<<< HEAD
            if (DocumentSubtypeModel.Types.INVOICE.equals(container.getType())) {
=======
            if (DocumentSubtypeModel.Types.INVOICE.equals(document.getType())) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                return checkTransactionCostManagers();
            }
        } else if (WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
            if (outcomeIndex == 1 && StringUtils.isBlank(task.getComment())) {
                return Arrays.asList(new Pair<String, String>("task_validation_externalReviewTask_comment", null));
            }
<<<<<<< HEAD
        } else if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK, WorkflowSpecificModel.Types.GROUP_ASSIGNMENT_TASK)) {
=======
        } else if (task.isType(WorkflowSpecificModel.Types.ASSIGNMENT_TASK, WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK)) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

    private List<Pair<String, String>> checkTransactionCostManagers() {
        List<Transaction> transactions = transactionsBlockBean.getTransactions();
        if (transactions == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<String> relatedFundsCenters = (List<String>) getNodeService().getProperty(getUserService().getCurrentUser(), ContentModel.PROP_RELATED_FUNDS_CENTER);
        if (relatedFundsCenters == null || relatedFundsCenters.isEmpty()) {
            return null;
        }
        List<String> mandatoryForCostManager = getEInvoiceService().getCostManagerMandatoryFields();
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
            restore("getCompoundWorkflows");
        }
        return compoundWorkflows;
    }

    public List<Task> getMyTasks() {
        if (myTasks == null) {
            restore("getMyTasks");
        }
        return myTasks;
    }

    public List<Task> getFinishedReviewTasks() {
        if (finishedReviewTasks == null) {
            restore("getFinishedReviewTasks");
        }
        return finishedReviewTasks;
    }

    public List<Task> getFinishedOpinionTasks() {
        if (finishedOpinionTasks == null) {
            restore("getFinishedOpinionTasks");
        }
        return finishedOpinionTasks;
    }

    public List<Task> getFinishedOrderAssignmentTasks() {
        if (finishedOrderAssignmentTasks == null) {
            restore("getFinishedOrderAssignmentTasks");
        }
        return finishedOrderAssignmentTasks;
    }

    public boolean getReviewNoteBlockRendered() {
        return getFinishedReviewTasks().size() != 0;
    }

    public String getReviewNotesPrintUrl() {
        return getPrintTableUrl(TableMode.REVIEW_NOTES, true);
    }

<<<<<<< HEAD
    public String getCompoundWorkflowPrintUrl() {
        return getPrintTableUrl(TableMode.COMPOUND_WORKFLOW, true);
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
        return getFinishedOpinionTasks().size() != 0;
    }

    public boolean getOrderAssignmentNoteBlockRendered() {
        return getFinishedOrderAssignmentTasks().size() != 0;
    }

    public String getWorkflowMenuLabel() {
        return MessageUtil.getMessage(MSG_WORKFLOW_ACTION_GROUP);
    }

<<<<<<< HEAD
    public String getDocumentWorkflowMenuTooltip() {
        return MessageUtil.getMessage("compoundWorkflow_new_document_workflow");
    }

    public String getIndependentWorkflowMenuTooltip() {
        return MessageUtil.getMessage("compoundWorkflow_new_independent_workflow");
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public void processCert() {
        @SuppressWarnings("unchecked")
        Map<String, String> requestParameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String certHex = requestParameterMap.get("certHex");
        String certId = requestParameterMap.get("certId");
        try {
            long step0 = System.currentTimeMillis();
<<<<<<< HEAD
            List<File> activeFiles = new ArrayList<File>();
            collectSigningFiles(activeFiles, new HashMap<NodeRef, List<File>>());
            if (!checkSigningFiles(activeFiles, true)) {
                closeModal();
                resetSigningData();
                return;
            }
            SignatureDigest signatureDigest = getDocumentService().prepareDocumentDigest(signingQueue.get(0), certHex,
                    mainDocumentRef != null ? compoundWorkflow.getNodeRef() : null);
=======
            SignatureDigest signatureDigest = getDocumentService().prepareDocumentDigest(docRef, certHex);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            long step1 = System.currentTimeMillis();
            showModal(signatureDigest.getDigestHex(), certId);
            signatureTask.setSignatureDigest(signatureDigest);
            if (log.isInfoEnabled()) {
                log.info("prepareDocumentDigest took total time " + (step1 - step0) + " ms\n    service call - " + (step1 - step0) + " ms");
            }
        } catch (SignatureException e) {
            SignatureBlockBean.addSignatureError(e);
            closeModal();
<<<<<<< HEAD
            resetSigningData();
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            closeModal();
            resetSigningData();
=======
            signatureTask = null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        }
    }

    public void signDocument() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        @SuppressWarnings("cast")
        String signatureHex = (String) facesContext.getExternalContext().getRequestParameterMap().get("signatureHex");

        try {
            signDocumentImpl(signatureHex);
        } finally {
<<<<<<< HEAD
            if (signingQueue == null || signingQueue.isEmpty()) {
                closeModal();
                resetSigningData();
            } else {
                showModalOrSign();
            }
        }
    }

    private void showModalOrSign() {
        if (needsSignatureInput(signingQueue.get(0))) {
            showModal();
        } else {
            signDocument();
        }
    }

    private void showMobileIdModalOrSign() {
        if (needsSignatureInput(signingQueue.get(0))) {
            getMobileIdPhoneNrModal().setRendered(true);
        } else {
            finishMobileIdSigning(null);
=======
            closeModal();
            signatureTask = null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        }
    }

    private void signDocumentImpl(String signatureHex) {
        try {
            long step0 = System.currentTimeMillis();
<<<<<<< HEAD
            List<File> activeFiles = new ArrayList<File>();
            collectSigningFiles(activeFiles, new HashMap<NodeRef, List<File>>());
            if (!checkSigningFiles(activeFiles, true)) {
                signingQueue = null;
                return;
            }
            boolean finishTask = signingQueue.size() == 1;
            getDocumentService().finishDocumentSigning(signatureTask, signatureHex, signingQueue.get(0), mainDocumentRef == null, finishTask, originalStatuses);
            signingQueue.remove(0);
            long step1 = System.currentTimeMillis();
            notifyDialogsIfNeeded();
=======
            getDocumentService().finishDocumentSigning(signatureTask, signatureHex);
            long step1 = System.currentTimeMillis();
            getDocumentDialogHelperBean().switchMode(false);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            long step2 = System.currentTimeMillis();
            if (log.isInfoEnabled()) {
                log.info("finishDocumentSigning took total time " + (step2 - step0) + " ms\n    service call - " + (step1 - step0) + " ms\n    reload document - "
                        + (step2 - step1) + " ms");
            }
<<<<<<< HEAD
            if (finishTask) {
                MessageUtil.addInfoMessage("task_finish_success_defaultMsg");
            }
            return;
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
        } catch (WorkflowChangedException e) {
            log.debug("Finishing signature task failed", e);
            MessageUtil.addErrorMessage("workflow_task_save_failed");
=======
            MessageUtil.addInfoMessage("task_finish_success_defaultMsg");
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
        } catch (WorkflowChangedException e) {
            handleWorkflowChangedException(e, "Finishing signature task failed", "workflow_task_save_failed", log);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        } catch (SignatureRuntimeException e) {
            SignatureBlockBean.addSignatureError(e);
        } catch (FileExistsException e) {
            if (log.isDebugEnabled()) {
<<<<<<< HEAD
                log.debug("Failed to create ddoc, file with same name already exists, parentRef = " + containerRef, e);
            }
            Utils.addErrorMessage(MessageUtil.getMessage("ddoc_file_exists"));
        }
        // this code must be reached ONLY in case of error
        signingQueue = null;
    }

    public void notifyDialogsIfNeeded() {
        notifyDialogsIfNeeded(false);
    }

    public void notifyDialogsIfNeeded(boolean resetExpandedData) {
        if (compoundWorkflow == null || !compoundWorkflow.isIndependentWorkflow()) {
            getDocumentDialogHelperBean().switchMode(false);
        }
        if (compoundWorkflow != null && compoundWorkflowDialog != null) {
            compoundWorkflowDialog.reload(compoundWorkflow != null ? compoundWorkflow.getNodeRef() : null, resetExpandedData);
        }
=======
                log.debug("Failed to create ddoc, file with same name already exists, parentRef = " + docRef, e);
            }
            Utils.addErrorMessage(MessageUtil.getMessage("ddoc_file_exists"));
        }
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public void cancelSign() {
        closeModal();
<<<<<<< HEAD
        resetSigningData();
        notifyDialogsIfNeeded();
    }

    public void resetSigningData() {
        signatureTask = null;
        signingQueue = null;
        mainDocumentRef = null;
        signingFiles = null;
        originalStatuses = null;
=======
        signatureTask = null;
        getDocumentDialogHelperBean().switchMode(false);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public void startMobileIdSigning(@SuppressWarnings("unused") ActionEvent event) {
        phoneNr = StringUtils.stripToEmpty(phoneNr);
        if (!phoneNr.startsWith("+")) {
            phoneNr = "+372" + phoneNr;
        }
        try {
            long step0 = System.currentTimeMillis();
<<<<<<< HEAD
            List<File> activeFiles = new ArrayList<File>();
            collectSigningFiles(activeFiles, new HashMap<NodeRef, List<File>>());
            if (!checkSigningFiles(activeFiles, true)) {
                resetSigningData();
                return;
            }
            SignatureChallenge signatureChallenge = getDocumentService().prepareDocumentChallenge(signingQueue.get(0), phoneNr,
                    mainDocumentRef != null ? compoundWorkflow.getNodeRef() : null);
=======
            SignatureChallenge signatureChallenge = getDocumentService().prepareDocumentChallenge(docRef, phoneNr);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            long step1 = System.currentTimeMillis();
            getMobileIdChallengeModal().setRendered(true);
            challengeId = "<div id=\"mobileIdChallengeMessage\" style=\"text-align: center;\"><p>Sõnumit saadetakse, palun oodake...</p><p>Kontrollkood:</p><p id=\"mobileIdChallengeId\" style=\"padding-top: 10px; font-size: 28px; vertical-align: middle;\">"
                    + StringEscapeUtils.escapeXml(signatureChallenge.getChallengeId()) + "</p></div><script type=\"text/javascript\">$jQ(document).ready(function(){ "
                    + "window.setTimeout(getMobileIdSignature, 2000); "
                    + "});</script>";
            signatureTask.setSignatureChallenge(signatureChallenge);
            signature = null;
            signatureError = null;
            if (log.isInfoEnabled()) {
                log.info("startMobileIdSigning took total time " + (step1 - step0) + " ms\n    service call - " + (step1 - step0) + " ms");
            }
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
<<<<<<< HEAD
            resetSigningData();
        } catch (SignatureException e) {
            SignatureBlockBean.addSignatureError(e);
            resetSigningData();
=======
            signatureTask = null;
        } catch (SignatureException e) {
            SignatureBlockBean.addSignatureError(e);
            signatureTask = null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        } finally {
            getMobileIdPhoneNrModal().setRendered(false);
        }
    }

    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void getMobileIdSignature() throws IOException {
        ResponseWriter out = FacesContext.getCurrentInstance().getResponseWriter();
        signature = null;
        try {
            if (!checkSignatureData()) {
                out.write("ERROR" + MessageUtil.getMessage("task_finish_error_signature_data_changed"));
                return;
            }
            signature = getSignatureService().getMobileIdSignature(signatureTask.getSignatureChallenge());
            if (signature == null) {
                out.write("REPEAT");
            } else {
                out.write("FINISH");
            }
        } catch (UnableToPerformException e) {
            out.write("FINISH");
            signatureError = e;
        }
    }

    private boolean checkSignatureData() {
        if (signatureTask == null || signatureTask.getSignatureChallenge() == null) {
            return false;
        }
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
<<<<<<< HEAD
=======
        @SuppressWarnings("cast")
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        String requestParamChallengeId = (String) externalContext.getRequestParameterMap().get("mobileIdChallengeId");
        if (StringUtils.isBlank(requestParamChallengeId) || !requestParamChallengeId.equals(signatureTask.getSignatureChallenge().getChallengeId())) {
            return false;
        }
        return true;
    }

    public void finishMobileIdSigning(@SuppressWarnings("unused") ActionEvent event) {
        try {
            if (signatureError == null) {
                signDocumentImpl(signature);
            } else {
                MessageUtil.addStatusMessage(signatureError);
<<<<<<< HEAD
                signingQueue = null;
            }
        } finally {
            getMobileIdChallengeModal().setRendered(false);
            if (signingQueue == null || signingQueue.isEmpty()) {
                resetSigningData();
            } else {
                showMobileIdModalOrSign();
            }
=======
            }
        } finally {
            getMobileIdChallengeModal().setRendered(false);
            signatureTask = null;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        }
    }

    /**
     * Callback to generate the drop down of outcomes for reviewTask.
     */
    public List<SelectItem> getReviewTaskOutcomes(FacesContext context, UIInput selectComponent) {
        int outcomes = getWorkflowService().getWorkflowTypes().get(WorkflowSpecificModel.Types.REVIEW_WORKFLOW).getTaskOutcomes();
        List<SelectItem> selectItems = new ArrayList<SelectItem>(outcomes);

        for (int i = 0; i < outcomes; i++) {
            String label = MessageUtil.getMessage("task_action_outcome_reviewTask" + i);
            selectItems.add(new SelectItem(i, label));
        }
        return selectItems;
    }

    public List<SelectItem> getExternalReviewTaskOutcomes(FacesContext context, UIInput selectComponent) {
        int outcomes = getWorkflowService().getWorkflowTypes().get(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW).getTaskOutcomes();
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

        // 1st child must always be SignatureAppletModalComponent
        panelGroupChildren.add(new SignatureAppletModalComponent());

        if (getSignatureService().isMobileIdEnabled()) {
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
            getChildren(mobileIdPhoneNrComponent).add(phoneNrInput);

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

        ValidatingModalLayerComponent dueDateExtensionLayer = null;
        for (Task task : getMyTasks()) {
            if (task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK, WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
<<<<<<< HEAD
                dueDateExtensionLayer = addDueDateExtensionLayer(panelGroupChildren, context, app);
=======
                dueDateExtensionLayer = addDueDateExtensionLayer(panelGroupChildren, context, app, task.getCreatorId(), task.getCreatorName());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                log.debug("Added dueDateExtensionLayer to parent=" + dueDateExtensionLayer.getParent());
                break;
            }
        }

        for (int index = 0; index < getMyTasks().size(); index++) {
            Task myTask = getMyTasks().get(index);
            Node node = myTask.getNode();
            QName taskType = node.getType();
            UIPropertySheet sheet = new WMUIPropertySheet();
            if (WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(taskType) || WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK.equals(taskType)) {
                // must use a copy of tasks workflow, as there might be at the same time 2 tasks of the same workflow for delegation
                Task myTaskCopy = WorkflowUtil.createTaskCopy(myTask);
                Pair<Integer, Task> delegatableTask = delegationBean.initDelegatableTask(myTaskCopy);
                int delegatableTaskIndex = delegatableTask.getFirst();
                putAttribute(sheet, DelegationBean.ATTRIB_DELEGATABLE_TASK_INDEX, delegatableTaskIndex);
                myTask = delegatableTask.getSecond();// first copy of myTask - stored in delegationBean and used in propertySheet
                if (WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK.equals(taskType) && myTask.getProp(WorkflowSpecificModel.Props.SEND_ORDER_ASSIGNMENT_COMPLETED_EMAIL) == null) {
                    myTask.setProp(WorkflowSpecificModel.Props.SEND_ORDER_ASSIGNMENT_COMPLETED_EMAIL, Boolean.TRUE);
                }
                getMyTasks().set(index, myTask);
                node = myTask.getNode();
            } else if (myTask.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK) && myTask.getConfirmedDueDate() == null) {
                myTask.setConfirmedDueDate(myTask.getProposedDueDate());
            }

            // the main block panel
            UIPanel panel = new UIPanel();
            panel.setId("workflow-task-block-panel-" + node.getId());
            panel.setLabel(MessageUtil.getMessage("task_title_main") + MessageUtil.getMessage("task_title_" + taskType.getLocalName()));
            panel.setProgressive(true);
            getAttributes(panel).put("styleClass", "panel-100 workflow-task-block");

            // the properties
            sheet.setId("task-sheet-" + node.getId());
            sheet.setNode(node);
            // this ensures we can use more than 1 property sheet on the page
            sheet.setVar("taskNode" + index);
            Map<String, Object> sheetAttributes = ComponentUtil.getAttributes(sheet);
            sheetAttributes.put("externalConfig", Boolean.TRUE);
            sheetAttributes.put("labelStyleClass", "propertiesLabel");
            sheetAttributes.put("columns", 1);
            sheet.setRendererType(PropertySheetGridRenderer.class.getCanonicalName());
            getChildren(panel).add(sheet);

            HtmlPanelGroup panelGrid = new HtmlPanelGroup();
            panelGrid.setStyleClass("task-sheet-buttons");
            // panel grid with a column for every button

            // save button used only for some task types
            List<UIComponent> panelGridChildren = getChildren(panelGrid);
            if (myTask.isType(WorkflowSpecificModel.Types.OPINION_TASK,
                    WorkflowSpecificModel.Types.REVIEW_TASK,
                    WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK,
                    WorkflowSpecificModel.Types.CONFIRMATION_TASK)) {

                // the save button
                HtmlCommandButton saveButton = new HtmlCommandButton();
                saveButton.setId("save-id-" + node.getId());
                saveButton.setActionListener(app.createMethodBinding("#{WorkflowBlockBean.saveTask}", new Class[] { ActionEvent.class }));
                saveButton.setValue(MessageUtil.getMessage("task_save_" + taskType.getLocalName()));
                saveButton.setStyleClass("taskOutcome");
                getAttributes(saveButton).put(ATTRIB_INDEX, index);

                panelGridChildren.add(saveButton);
            }

            // the outcome buttons
            String label = "task_outcome_" + node.getType().getLocalName();
            for (int outcomeIndex = 0; outcomeIndex < myTask.getOutcomes(); outcomeIndex++) {
                if (WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)
                        && WorkflowSpecificModel.SignatureTaskOutcome.SIGNED_MOBILEID.equals(outcomeIndex)
                        && !getSignatureService().isMobileIdEnabled()) {
                    continue;
                }
                HtmlCommandButton outcomeButton = new HtmlCommandButton();
                outcomeButton.setId("outcome-id-" + index + "-" + outcomeIndex);
                Map<String, Object> outcomeBtnAttributes = ComponentUtil.putAttribute(outcomeButton, "styleClass", "taskOutcome");
                outcomeButton.setActionListener(app.createMethodBinding("#{WorkflowBlockBean.finishTask}", new Class[] { ActionEvent.class }));
                String buttonSuffix = "_title";
                outcomeButton.setValue(MessageUtil.getMessage(label + outcomeIndex + buttonSuffix));
                outcomeBtnAttributes.put(ATTRIB_INDEX, index);
                outcomeBtnAttributes.put(ATTRIB_OUTCOME_INDEX, outcomeIndex);

                panelGridChildren.add(outcomeButton);

                // the review and external review task has only 1 button and the outcomes come from TEMP_OUTCOME property
                if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)
                        || WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK.equals(taskType)) {
                    // node.getProperties().put(WorkflowSpecificModel.Props.TEMP_OUTCOME.toString(), 0);
                    outcomeButton.setValue(MessageUtil.getMessage(label + buttonSuffix));
                    break;
                }
            }

            if (myTask.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK, WorkflowSpecificModel.Types.ASSIGNMENT_TASK) && StringUtils.isNotBlank(myTask.getCreatorId())) {
                // ask-due-date-extension button
                HtmlCommandButton extensionButton = new HtmlCommandButton();
                extensionButton.setId("ask-due-date-extension-" + node.getId());
                extensionButton.setStyleClass("taskOutcome");
                Map<String, Object> extensionBtnAttributes = getAttributes(extensionButton);
                extensionBtnAttributes.put(ATTRIB_INDEX, index);
                // postpone generating onClick js to rendering phase when we have parent form present
                if (dueDateExtensionLayer != null) {
                    extensionBtnAttributes.put(HtmlButtonRenderer.ATTR_ONCLICK_DATA, new Pair<UIComponent, Integer>(dueDateExtensionLayer, index));
                    log.debug("Attatching dueDateExtensionLayer to component id=" + extensionButton.getId() + ", rebuild action=" + action + " parent="
                            + dueDateExtensionLayer.getParent()
                            + ", parent id=" + (dueDateExtensionLayer.getParent() != null ? dueDateExtensionLayer.getParent().getId() : "null") + ", task=" + myTask);
                }
                extensionButton.setRendererType(HtmlButtonRenderer.HTML_BUTTON_RENDERER_TYPE);

<<<<<<< HEAD
=======
                extensionButton.setActionListener(app.createMethodBinding("#{WorkflowBlockBean.saveTask}", new Class[] { ActionEvent.class }));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                extensionButton.setValue(MessageUtil.getMessage("task_ask_due_date_extension"));
                extensionButton.setStyleClass("taskOutcome");
                extensionBtnAttributes.put(ATTRIB_INDEX, index);

                panelGridChildren.add(extensionButton);
            }

            getChildren(panel).add(panelGrid);
            panelGroupChildren.add(panel);
        }
    }

<<<<<<< HEAD
    private ValidatingModalLayerComponent addDueDateExtensionLayer(List<UIComponent> panelGroupChildren, FacesContext context, Application app) {
=======
    private ValidatingModalLayerComponent addDueDateExtensionLayer(List<UIComponent> panelGroupChildren, FacesContext context, Application app, String defaultUsername,
            String defaultUserFullname) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

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

<<<<<<< HEAD
=======
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
        dueDateExtenderUsername = defaultUsername;
        dueDateExtenderUserFullname = defaultUserFullname;
        search.setValueBinding("value", context.getApplication().createValueBinding("#{WorkflowBlockBean.dueDateExtenderUserFullname}"));
        layerChildren.add(search);

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        UIInput dueDateInput = addDateInput(context, layerChildren, "workflow_dueDateExtension_dueDate", MODAL_KEY_DUE_DATE);
        dueDateInput.setValue(CalendarUtil.addWorkingDaysToDate(new LocalDate(), 2, getClassificatorService()).toDateTimeAtCurrentTime().toDate());
        ComponentUtil.putAttribute(dueDateInput, ValidatingModalLayerComponent.ATTR_PRESERVE_VALUES, Boolean.TRUE);

        dueDateExtensionLayer.setActionListener(app.createMethodBinding("#{WorkflowBlockBean.sendTaskDueDateExtensionRequest}", UIActions.ACTION_CLASS_ARGS));
        panelGroupChildren.add(dueDateExtensionLayer);

        return dueDateExtensionLayer;
    }

<<<<<<< HEAD
=======
    public void assignDueDateExtender(String result) {
        if (StringUtils.isBlank(result)) {
            return;
        }

        dueDateExtenderUsername = result;
        dueDateExtenderUserFullname = BeanHelper.getUserService().getUserFullName(dueDateExtenderUsername);
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

    private void showModal(String digestHex, String certId) {
        getIdCardModalApplet().showModal(digestHex, certId);
    }

    private void closeModal() {
        getIdCardModalApplet().closeModal();
    }

    private SignatureAppletModalComponent getIdCardModalApplet() {
        return (SignatureAppletModalComponent) getDataTableGroupInner().getChildren().get(0);
    }

    private ValidatingModalLayerComponent getMobileIdPhoneNrModal() {
        return (ValidatingModalLayerComponent) getDataTableGroupInner().getChildren().get(1);
    }

    private ModalLayerComponent getMobileIdChallengeModal() {
        return (ModalLayerComponent) getDataTableGroupInner().getChildren().get(2);
    }

    private boolean checkRights(Workflow workflow) {
<<<<<<< HEAD
        if (compoundWorkflow != null) {
            return false;
        }
        QName parentType = BeanHelper.getNodeService().getType(containerRef);
        boolean isDocumentManager = getUserService().isDocumentManager();
        boolean isOwner = getDocumentDynamicService().isOwner(containerRef, AuthenticationUtil.getRunAsUser()) || getWorkflowService().isOwner(workflow.getParent());
        if (isDocumentWorkflow(parentType)) {
            boolean localRights = isDocumentManager || isOwner || getWorkflowService().isOwnerOfInProgressAssignmentTask(workflow.getParent());
            boolean externalReviewRights = !workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)
                    || !Boolean.TRUE.equals(container.getProperties().get(DocumentCommonModel.Props.NOT_EDITABLE))
                    || !hasCurrentInstitutionTask(workflow);
            return localRights && externalReviewRights;
        } else if (isCaseWorkflow(parentType)) {
            return isOwner || isAdminOrDocmanagerWithPermission(containerRef, DocumentCommonModel.Privileges.VIEW_CASE_FILE)
                    || BeanHelper.getPrivilegeService().hasPermissions(containerRef, DocumentCommonModel.Privileges.EDIT_CASE_FILE);
        }
        return false;
=======
        boolean localRights = getUserService().isDocumentManager()
                || getDocumentDynamicService().isOwner(docRef, AuthenticationUtil.getRunAsUser())
                || getWorkflowService().isOwner(workflow.getParent())
                || getWorkflowService().isOwnerOfInProgressAssignmentTask(workflow.getParent());
        boolean externalReviewRights = !workflow.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_WORKFLOW)
                || !Boolean.TRUE.equals(document.getProperties().get(DocumentSpecificModel.Props.NOT_EDITABLE))
                || !hasCurrentInstitutionTask(workflow);
        return localRights && externalReviewRights;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        taskPanelControlDocument = containerRef;
=======
        taskPanelControlDocument = docRef;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        if (taskPanelControlDocument != null && !taskPanelControlDocument.equals(containerRef)) {
            constructTaskPanelGroup(dataTableGroup, "setDataTableGroup");
            taskPanelControlDocument = containerRef;
=======
        if (taskPanelControlDocument != null && !taskPanelControlDocument.equals(docRef)) {
            constructTaskPanelGroup(dataTableGroup, "setDataTableGroup");
            taskPanelControlDocument = docRef;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        }
        this.dataTableGroup = dataTableGroup;
    }

    public UIRichList getReviewNotesRichList() {
        return reviewNotesRichList;
    }

    public void setReviewNotesRichList(UIRichList reviewNotesRichList) {
        this.reviewNotesRichList = reviewNotesRichList;
<<<<<<< HEAD
        for (UIComponent component : ComponentUtil.getChildren(reviewNotesRichList)) {
            if (component instanceof UIColumn) {
                UIColumn column = (UIColumn) component;
                if ("review-note-block-file-versions-col".equals(column.getId())) {
                    column.setRendered(BeanHelper.getDialogManager().getCurrentDialog().getName().equals("documentDynamicDialog"));
                }
            }
        }
    }

    // this method assumes we are in DOCUMENT_WORKFLOW view
=======
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public List<WorkflowBlockItem> getWorkflowBlockItems() {
        List<WorkflowBlockItemGroup> workflowBlockItemGroups = new ArrayList<WorkflowBlockItemGroup>();
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
<<<<<<< HEAD
                    if (task.isSaved()) {
                        items.add(new WorkflowBlockItem(task, raisedRights));
                    }
=======
                    items.add(new WorkflowBlockItem(task, raisedRights));
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
                }
            }
            Collections.sort(items, WorkflowBlockItem.COMPARATOR);
            workflowBlockItemGroups.add(new WorkflowBlockItemGroup(items, wfs.size()));
        }

        if (workflowBlockItemGroups.isEmpty()) {
            return Collections.<WorkflowBlockItem> emptyList();
        }

        // Sort by workflows
        Collections.sort(workflowBlockItemGroups, WorkflowBlockItemGroup.COMPARATOR);

        // Flatten the structure.
        groupedWorkflowBlockItems = new ArrayList<WorkflowBlockItem>();
        int numberInGroupedBlock = 0;
        String workflowGroupTasksUrl = getWorkflowGroupTasksUrl() + "&amp;" + PrintTableServlet.GROUP_NR_PARAM + "=";
        for (WorkflowBlockItemGroup workflowBlockItemGroup : workflowBlockItemGroups) {
            List<WorkflowBlockItem> groupedItems = workflowBlockItemGroup.getGroupedItems();
            for (WorkflowBlockItem groupedItem : groupedItems) {
                groupedItem.setWorkflowGroupTasksUrl(workflowGroupTasksUrl + numberInGroupedBlock++);
            }
            groupedWorkflowBlockItems.addAll(groupedItems);
            groupedWorkflowBlockItems.add(new WorkflowBlockItem(true));
            groupedWorkflowBlockItems.add(new WorkflowBlockItem(false));
            numberInGroupedBlock += 2;
        }
        groupedWorkflowBlockItems.remove(groupedWorkflowBlockItems.size() - 1); // remove two last ones
        groupedWorkflowBlockItems.remove(groupedWorkflowBlockItems.size() - 1);
        return groupedWorkflowBlockItems;
    }

    public List<WorkflowBlockItem> getGroupedWorkflowBlockItems() {
        return groupedWorkflowBlockItems;
    }

    /** Return groups that have same compund workflow ref, same workflow id and same group name as argument group */
    public List<WorkflowBlockItem> getSameGroupWorkflowBlockItems(int numberInGroup) {
        WorkflowBlockItem initiatingWorkflowBlockItem = groupedWorkflowBlockItems.get(numberInGroup);
        if (!initiatingWorkflowBlockItem.isGroupBlockItem()) {
            return Collections.singletonList(initiatingWorkflowBlockItem);
        }
        NodeRef compoundWorkflowNodeRef = initiatingWorkflowBlockItem.getCompoundWorkflowNodeRef();
        int workflowIndex = initiatingWorkflowBlockItem.getGroupWorkflowIndex();
        String groupName = initiatingWorkflowBlockItem.getGroupName();
        List<WorkflowBlockItem> workflowBlockItems = new ArrayList<WorkflowBlockItem>();
        for (WorkflowBlockItem item : groupedWorkflowBlockItems) {
            if (item.isGroupBlockItem() && item.getCompoundWorkflowNodeRef().equals(compoundWorkflowNodeRef) && item.getWorkflowIndex() == workflowIndex
                    && groupName.equals(item.getGroupName())) {
                workflowBlockItems.add(item);
            }
        }
        return workflowBlockItems;
    }

    public CustomChildrenCreator getNoteBlockRowFileGenerator() {
        CustomChildrenCreator fileComponentCreator = new CustomChildrenCreator() {

            @Override
            public List<UIComponent> createChildren(List<Object> params, int rowCounter) {
                List<UIComponent> components = new ArrayList<UIComponent>();
                if (params != null) {
                    Application application = FacesContext.getCurrentInstance().getApplication();
                    int fileCounter = 0;
                    for (Object obj : params) {
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

<<<<<<< HEAD
    public CustomChildrenCreator getDueDateHistoryRecordsGenerator() {
        CustomChildrenCreator dueDateHistoryGenerator = new CustomChildrenCreator() {

            @SuppressWarnings("unchecked")
            @Override
            public List<UIComponent> createChildren(List<Object> params, int rowCounter) {
                List<UIComponent> components = new ArrayList<UIComponent>();
                if (params != null && params.size() > 0) {
                    String modalId = ((DueDateHistoryRecord) params.get(0)).getTaskId();
                    components.add(new DueDateHistoryModalComponent(FacesContext.getCurrentInstance(), modalId, (List) params));
                }
                return components;
            }
        };
        return dueDateHistoryGenerator;
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
        return signatureTask;
    }

    public String getPhoneNr() {
        return phoneNr;
    }

    public void setPhoneNr(String phoneNr) {
        this.phoneNr = phoneNr;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

<<<<<<< HEAD
    public UIPanel getModalContainer() {
        if (modalContainer == null) {
            modalContainer = new UIPanel();
        }
        return modalContainer;
    }

    public void setModalContainer(UIPanel modalContainer) {
        this.modalContainer = modalContainer;
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    // END: getters / setters

}
