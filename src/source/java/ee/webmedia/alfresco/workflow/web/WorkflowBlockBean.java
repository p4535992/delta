package ee.webmedia.alfresco.workflow.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.ConstantMethodBinding;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIPropertySheet;
import ee.webmedia.alfresco.common.propertysheet.renderkit.PropertySheetGridRenderer;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.signature.web.SignatureAppletModalComponent;
import ee.webmedia.alfresco.signature.web.SignatureBlockBean;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSummaryItem;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflowDefinition;
import ee.webmedia.alfresco.workflow.service.SignatureTask;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;

/**
 * @author Dmitri Melnikov
 */
public class WorkflowBlockBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(WorkflowBlockBean.class);
    public static final String BEAN_NAME = "WorkflowBlockBean";

    private static final String WORKFLOW_METHOD_BINDING_NAME = "#{DocumentDialog.workflow.findCompoundWorkflowDefinitions}";
    private static final String DROPDOWN_MENU_ITEM_ICON = "/images/icons/versioned_properties.gif";
    private static final String MSG_WORKFLOW_ACTION_GROUP = "workflow_compound_start_workflow";
    private static final String ATTRIB_OUTCOME_INDEX = "outcomeIndex";
    private static final String ATTRIB_INDEX = "index";
    private static final String PARAM_NODEREF = "nodeRef";

    private FileBlockBean fileBlockBean;
    private MetadataBlockBean metadataBlockBean;

    private transient DocumentService documentService;
    private transient WorkflowService workflowService;
    private transient UserService userService;
    private transient FileService fileService;
    private transient SignatureService signatureService;

    private transient HtmlPanelGroup dataTableGroup;

    private NodeRef document;
    private List<CompoundWorkflow> compoundWorkflows;
    private List<WorkflowSummaryItem> workflowSummaryItems;
    private transient HtmlPanelGroup wfPanelGroup;
    private List<Task> myTasks;
    private List<Task> finishedReviewTasks;
    private List<Task> finishedOpinionTasks;
    private SignatureTask signatureTask;

    public void init(NodeRef document) {
        this.document = document;
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
        workflowSummaryItems = null;
        wfPanelGroup = null;
    }

    public void restore() {
        compoundWorkflows = getWorkflowService().getCompoundWorkflows(document);
        // if (log.isDebugEnabled()) {
        // log.debug("Document has workflows " + WmNode.toString(compoundWorkflows));
        // }
        myTasks = getWorkflowService().getMyTasksInProgress(compoundWorkflows);
        finishedReviewTasks = WorkflowUtil.getFinishedTasks(compoundWorkflows, WorkflowSpecificModel.Types.REVIEW_TASK);
        finishedOpinionTasks = WorkflowUtil.getFinishedTasks(compoundWorkflows, WorkflowSpecificModel.Types.OPINION_TASK);
        signatureTask = null;
        workflowSummaryItems = null;
        // rebuild the whole task panel
        constructTaskPanelGroup();
        // refresh workflow panel
        renderWorkflowPanel();
    }

    public boolean isCompoundWorkflowOwner() {
        return getWorkflowService().isOwner(getCompoundWorkflows());
    }

    public List<ActionDefinition> findCompoundWorkflowDefinitions(String documentTypeQName, String documentStatus) {
        QName documentType = QName.createQName(documentTypeQName);
        List<CompoundWorkflowDefinition> workflowDefs = getWorkflowService().getCompoundWorkflowDefinitions(documentType, documentStatus);
        List<ActionDefinition> actionDefinitions = new ArrayList<ActionDefinition>(workflowDefs.size());
        for (CompoundWorkflowDefinition compoundWorkflowDefinition : workflowDefs) {
            ActionDefinition actionDefinition = new ActionDefinition("compoundWorkflowDefinitionAction");
            actionDefinition.Image = DROPDOWN_MENU_ITEM_ICON;
            actionDefinition.Label = compoundWorkflowDefinition.getName();
            actionDefinition.Action = "#{DocumentDialog.workflow.getCompoundWorkflowDialog}";
            actionDefinition.ActionListener = "#{CompoundWorkflowDialog.setupNewWorkflow}";
            actionDefinition.addParam("compoundWorkflowDefinitionNodeRef", compoundWorkflowDefinition.getNode().getNodeRef().toString());

            actionDefinitions.add(actionDefinition);
        }

        return actionDefinitions;
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

        if (getUserService().isDocumentManager() || getDocumentService().isDocumentOwner(document, AuthenticationUtil.getRunAsUser())
                || getMyTasks().size() > 0) {
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

        if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)) {
            outcomeIndex = (Integer) task.getNode().getProperties().get(WorkflowSpecificModel.Props.TEMP_OUTCOME.toString());
        } else if (WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)) {
            if (outcomeIndex == 1) {

                // signing requires that at least 1 active file exists within this document
                long step0 = System.currentTimeMillis();
                List<File> activeFiles = getFileService().getAllActiveFiles(document);
                if (activeFiles == null || activeFiles.isEmpty()) {
                    MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "task_files_required");
                    return;
                }

                signatureTask = (SignatureTask) task;
                try {
                    long step1 = System.currentTimeMillis();
                    getDocumentService().prepareDocumentSigning(document);
                    long step2 = System.currentTimeMillis();
                    fileBlockBean.restore();
                    showModal();
                    long step3 = System.currentTimeMillis();
                    if (log.isInfoEnabled()) {
                        log.info("prepareDocumentSigning took total time " + (step3 - step0) + " ms\n    load file list - " + (step1 - step0)
                                + " ms\n    service call - " + (step2 - step1) + " ms\n    reload file list - " + (step3 - step2) + " ms");
                    }
                } catch (UnableToPerformException e) {
                    MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
                }
                MessageUtil.addInfoMessage("task_finish_success_defaultMsg");
                return;
            }
        }

        String validationMsg = null;
        if ((validationMsg = validate(task, outcomeIndex)) != null) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), validationMsg);
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

    private String validate(Task task, Integer outcomeIndex) {
        QName taskType = task.getNode().getType();
        if (WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)) {
            if (outcomeIndex == 0 && StringUtils.isBlank(task.getComment())) {
                return "task_validation_signatureTask_comment";
            }
        } else if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)) {
            if ((outcomeIndex == 1 || outcomeIndex == 2) && StringUtils.isBlank(task.getComment())) {
                return "task_validation_reviewTask_comment";
            }
        } else if (WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(taskType)) {
            if (StringUtils.isBlank(task.getComment())) {
                return "task_validation_assignmentTask_comment";
            }
        } else if (WorkflowSpecificModel.Types.OPINION_TASK.equals(taskType)) {
            if (StringUtils.isBlank(task.getComment()) && task.getProp(WorkflowSpecificModel.Props.FILE) == null) {
                return "task_validation_opinionTask_comment";
            }
        }
        return null;
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
            SignatureDigest signatureDigest = getDocumentService().prepareDocumentDigest(document, certHex);
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
                log.debug("Failed to create ddoc, file with same name already exists, parentRef = " + document, e);
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
    public List<SelectItem> getReviewTaskOutcomes(@SuppressWarnings("unused") FacesContext context, @SuppressWarnings("unused") UIInput selectComponent) {
        int outcomes = getWorkflowService().getWorkflowTypes().get(WorkflowSpecificModel.Types.REVIEW_WORKFLOW).getTaskOutcomes();
        List<SelectItem> selectItems = new ArrayList<SelectItem>(outcomes);

        for (int i = 0; i < outcomes; i++) {
            String label = MessageUtil.getMessage("task_outcome_reviewTask" + i);
            selectItems.add(new SelectItem(i, label));
        }
        return selectItems;
    }

    /**
     * Manually generate a panel group with everything.
     */
    @SuppressWarnings("unchecked")
    private void constructTaskPanelGroup() {
        getDataTableGroup().getChildren().clear();
        Application app = FacesContext.getCurrentInstance().getApplication();

        // add 2 hidden links and a modal applet so signing
        dataTableGroup.getChildren().add(new SignatureAppletModalComponent());
        dataTableGroup.getChildren().add(generateLinkWithParam(app, "processCert", "#{" + BEAN_NAME + ".processCert}", "cert"));
        dataTableGroup.getChildren().add(generateLinkWithParam(app, "signDocument", "#{" + BEAN_NAME + ".signDocument}", "signature"));
        dataTableGroup.getChildren().add(generateLinkWithParam(app, "cancelSign", "#{" + BEAN_NAME + ".cancelSign}", null));

        for (int index = 0; index < getMyTasks().size(); index++) {
            Task myTask = getMyTasks().get(index);

            Node node = myTask.getNode();
            QName taskType = node.getType();
            // the main block panel
            UIPanel panel = new UIPanel();
            panel.setId("workflow-task-block-panel-" + node.getId());
            panel.setLabel(MessageUtil.getMessage("task_title_main") + MessageUtil.getMessage("task_title_" + taskType.getLocalName()));
            panel.setProgressive(true);
            panel.getAttributes().put("styleClass", "panel-100 workflow-task-block");

            // the properties
            UIPropertySheet sheet = new WMUIPropertySheet();
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
                    WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)) {

                // the save button
                HtmlCommandButton saveButton = new HtmlCommandButton();
                saveButton.setId("save-id-" + node.getId());
                saveButton.setActionListener(app.createMethodBinding("#{DocumentDialog.workflow.saveTask}", new Class[] { ActionEvent.class }));
                saveButton.setValue(MessageUtil.getMessage("task_save_" + taskType.getLocalName()));
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
                outcomeButton.getAttributes().put(ATTRIB_INDEX, index);
                outcomeButton.getAttributes().put(ATTRIB_OUTCOME_INDEX, outcomeIndex);
                panelGrid.getChildren().add(outcomeButton);

                // the review task has only 1 button and the outcomes come from TEMP_OUTCOME property
                if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)) {
                    // node.getProperties().put(WorkflowSpecificModel.Props.TEMP_OUTCOME.toString(), 0);
                    outcomeButton.setValue(MessageUtil.getMessage(label));
                    break;
                }
            }

            // delegate button for assignment task only
            if (WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(taskType)) {
                // the delegate button
                HtmlCommandButton delegateButton = new HtmlCommandButton();
                delegateButton.setId("delegate-id-" + node.getId());
                delegateButton.setAction(app.createMethodBinding("#{" + BEAN_NAME + ".showCompoundWorkflowDialog}", new Class[] {}));
                delegateButton.setActionListener(app.createMethodBinding("#{CompoundWorkflowDialog.setupWorkflow}", new Class[] { ActionEvent.class }));
                delegateButton.setValue(MessageUtil.getMessage("task_delegate_" + taskType.getLocalName()));

                UIParameter param = (UIParameter) app.createComponent(ComponentConstants.JAVAX_FACES_PARAMETER);
                param.setId("delegate-param-" + node.getId());
                param.setName(PARAM_NODEREF);
                param.setValue(myTask.getParent().getParent().getNode().getNodeRef().toString());
                delegateButton.getChildren().add(param);

                panelGrid.getChildren().add(delegateButton);
            }

            panel.getChildren().add(panelGrid);
            dataTableGroup.getChildren().add(panel);
        }
    }

    public void showCompoundWorkflowDialog() {
        FacesContext fc = FacesContext.getCurrentInstance();
        NavigationHandler navigationHandler = fc.getApplication().getNavigationHandler();
        navigationHandler.handleNavigation(fc, null, "dialog:compoundWorkflowDialog");
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
        return (SignatureAppletModalComponent) getDataTableGroup().getChildren().get(0);
    }

    public List<WorkflowSummaryItem> getWorkflowSummaryItems() {
        if (workflowSummaryItems == null) {
            workflowSummaryItems = new ArrayList<WorkflowSummaryItem>();
            List<Workflow> workflows = WorkflowUtil.getVisibleWorkflows(getCompoundWorkflows());

            for (Workflow workflow : workflows) {
                WorkflowSummaryItem workflowSummaryItem = new WorkflowSummaryItem(workflow);
                workflowSummaryItem.setRaisedRights(checkRights(workflow));
                workflowSummaryItems.add(workflowSummaryItem);
                WorkflowSummaryItem workflowSummaryItemTaskView = new WorkflowSummaryItem(workflow);
                workflowSummaryItemTaskView.setTaskView(true);
                workflowSummaryItems.add(workflowSummaryItemTaskView);
            }

            Collections.sort(workflowSummaryItems);
        }
        return workflowSummaryItems;
    }

    private boolean checkRights(Workflow workflow) {
        return getUserService().isDocumentManager()
                || getDocumentService().isDocumentOwner(document, AuthenticationUtil.getRunAsUser())
                || getWorkflowService().isOwner(workflow.getParent())
                || getWorkflowService().isOwnerOfInProgressAssignmentTask(workflow.getParent());
    }

    public void setWorkflowSummaryItems(List<WorkflowSummaryItem> workflowSummaryItems) {
        this.workflowSummaryItems = workflowSummaryItems;
    }

    public void toggleTaskViewVisible(ActionEvent event) {
        NodeRef workflowNodeRef = new NodeRef(ActionUtil.getParam(event, "workflowNodeRef"));
        for (WorkflowSummaryItem item : getWorkflowSummaryItems()) {
            if (item.getWorkflowRef().equals(workflowNodeRef)) {
                item.toggleTaskViewVisible(event);
            }
        }
    }

    // START: getters / setters
    public void setFileBlockBean(FileBlockBean fileBlockBean) {
        this.fileBlockBean = fileBlockBean;
    }

    public void setMetadataBlockBean(MetadataBlockBean metadataBlockBean) {
        this.metadataBlockBean = metadataBlockBean;
    }

    public HtmlPanelGroup getDataTableGroup() {
        // This will be called once in the first RESTORE VIEW phase.
        if (dataTableGroup == null) {
            dataTableGroup = new HtmlPanelGroup();
        }
        return dataTableGroup;
    }

    public void setDataTableGroup(HtmlPanelGroup dataTableGroup) {
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

    // END: getters / setters

    @SuppressWarnings("unchecked")
    private void renderWorkflowPanel() {
        Application app = FacesContext.getCurrentInstance().getApplication();
        getWfPanelGroup().getChildren().clear();

        UIPanel workflowPanel = (UIPanel) app.createComponent("org.alfresco.faces.Panel");
        workflowPanel.setId("workflow-summary-panel");
        workflowPanel.setProgressive(true);
        workflowPanel.getAttributes().put("styleClass", "panel-100");
        workflowPanel.setExpanded(false);
        workflowPanel.setLabel(MessageUtil.getMessage("workflow_workflows"));
        generateCompoundWorkflowTables(app, workflowPanel);
        wfPanelGroup.getChildren().add(workflowPanel);

    }

    @SuppressWarnings("unchecked")
    private void generateCompoundWorkflowTables(Application app, UIPanel workflowPanel) {
        for (CompoundWorkflow compound : compoundWorkflows) {
            HtmlPanelGrid grid = (HtmlPanelGrid) app.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
            grid.setColumns(4);
            grid.setStyleClass("compound-workflow-table");
            grid.setRowClasses("header,compound-details");
            grid.setColumnClasses("width25,width25,width25,width25");

            List<UIComponent> children = grid.getChildren();

            // Header
            children.add(createOutput(app, MessageUtil.getMessage("workflow_workflow")));
            children.add(createOutput(app, MessageUtil.getMessage("workflow_started")));
            children.add(createOutput(app, MessageUtil.getMessage("workflow_stopped")));
            children.add(createOutput(app, MessageUtil.getMessage("workflow_creator")));

            // Values
            final UIComponent compundWFOtuput = createOutput(app, MessageUtil.getMessage("workflow_compound"));
            ComponentUtil.setTooltip(compundWFOtuput, MessageUtil.getMessage("compoundWorkflow_status", compound.getStatus()));
            children.add(compundWFOtuput);
            children.add(createOutput(app, formatDate(compound.getStartedDateTime())));
            children.add(createOutput(app, formatDate(compound.getStoppedDateTime())));
            children.add(createOutput(app, compound.getCreatorName()));

            generateWorkflowTables(app, grid, compound);

            workflowPanel.getChildren().add(grid);
        }

    }

    @SuppressWarnings("unchecked")
    private void generateWorkflowTables(Application app, HtmlPanelGrid htmlPanelGrid, CompoundWorkflow compound) {

        HtmlPanelGroup workflowWrapper = (HtmlPanelGroup) app.createComponent(HtmlPanelGroup.COMPONENT_TYPE);

        for (Workflow workflow : compound.getWorkflows()) {
            if (WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW.equals(workflow.getNode().getType())) {
                continue; // Don't display registration workflows
            }

            WorkflowSummaryItem summaryItem = new WorkflowSummaryItem(workflow);
            summaryItem.setRaisedRights(checkRights(workflow));
            HtmlPanelGrid grid = (HtmlPanelGrid) app.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
            grid.setColumns(1);
            grid.setStyleClass("workflow-table");
            grid.setRowClasses("workflow-header,workflow-details");

            List<UIComponent> gridChildren = grid.getChildren();

            // Heading
            HtmlPanelGroup headingGroup = (HtmlPanelGroup) app.createComponent(HtmlPanelGroup.COMPONENT_TYPE);

            UIActionLink toggle = (UIActionLink) app.createComponent(UIActions.COMPONENT_ACTIONLINK);
            toggle.setValue("");
            toggle.setShowLink(false);
            toggle.setOnclick("return false;");
            toggle.getAttributes().put("styleClass", "toggle-tasks icon-link expanded");
            headingGroup.getChildren().add(toggle);

            if (summaryItem.isRaisedRights()) {
                UIActionLink link = (UIActionLink) app.createComponent(UIActions.COMPONENT_ACTIONLINK);
                link.setValue(summaryItem.getName());
                link.setAction(new ConstantMethodBinding("dialog:compoundWorkflowDialog"));
                link.setActionListener(app.createMethodBinding("#{CompoundWorkflowDialog.setupWorkflow}", new Class[] { javax.faces.event.ActionEvent.class }));
                link.getAttributes().put("styleClass", "workflow-conf");

                UIParameter nodeRef = (UIParameter) app.createComponent(UIParameter.COMPONENT_TYPE);
                nodeRef.setName("nodeRef");
                nodeRef.setValue(summaryItem.getCompoundWorkflowRef().toString());
                link.getChildren().add(nodeRef);

                headingGroup.getChildren().add(link);
            } else {
                headingGroup.getChildren().add(createOutput(app, summaryItem.getName(), "workflow-conf"));
            }

            headingGroup.getChildren().add(createOutput(app, MessageUtil.getMessage("task_property_resolution") + ": " + summaryItem.getResolution()));

            HtmlPanelGroup taskGroup = (HtmlPanelGroup) app.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            if (summaryItem.isAssignmentWorkflow()) {
                generateTaskTables(app, summaryItem, taskGroup, true, true);
                if (summaryItem.getAssignmentTasks().size() > 0) {
                    generateTaskTables(app, summaryItem, taskGroup, true, false);
                }
            } else {
                generateTaskTables(app, summaryItem, taskGroup, false, false);
            }

            gridChildren.add(headingGroup);
            gridChildren.add(taskGroup);
            workflowWrapper.getChildren().add(grid);
        }

        htmlPanelGrid.getFacets().put("footer", workflowWrapper);
    }

    @SuppressWarnings("unchecked")
    private void generateTaskTables(Application app, WorkflowSummaryItem summaryItem, HtmlPanelGroup group, boolean isAssignment, boolean isPrimary) {
        HtmlPanelGrid taskGrid = (HtmlPanelGrid) app.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        taskGrid.setStyleClass("task-table");
        taskGrid.setRowClasses("th,");

        if (isAssignment) {
            taskGrid.setColumns(8);
            taskGrid.setColumnClasses("width10,width10,width10,width30,width10,width10,width10,width10");
        } else {
            taskGrid.setColumns(7);
            taskGrid.setColumnClasses("width10,width15,width15,width10,width30,width10,width10");
        }

        List<UIComponent> children = taskGrid.getChildren();

        // Heading
        children.add(createOutput(app, MessageUtil.getMessage("workflow_started")));
        children.add(createOutput(app, MessageUtil.getMessage("workflow_creator")));
        if (isAssignment && isPrimary) {
            children.add(createOutput(app, MessageUtil.getMessage("assignmentWorkflow_tasks")));
        } else if (isAssignment && !isPrimary) {
            children.add(createOutput(app, MessageUtil.getMessage("assignmentWorkflow_tasks_co")));
        } else {
            children.add(createOutput(app, summaryItem.getTaskOwnerRole()));
        }
        if (isAssignment) {
            children.add(createOutput(app, MessageUtil.getMessage("task_property_resolution")));
        }
        children.add(createOutput(app, MessageUtil.getMessage("task_property_due_date")));
        children.add(createOutput(app, MessageUtil.getMessage("workflow_completed")));
        children.add(createOutput(app, MessageUtil.getMessage("task_property_comment_assignmentTask")));
        children.add(createOutput(app, MessageUtil.getMessage("workflow_status")));

        List<Task> tasks;
        if (isAssignment && isPrimary) {
            tasks = summaryItem.getAssignmentResponsibleTasks();
        } else if (isAssignment & !isPrimary) {
            tasks = summaryItem.getAssignmentTasks();
        } else {
            tasks = summaryItem.getTasks();
        }

        for (Task task : tasks) {
            children.add(createOutput(app, formatDate(task.getStartedDateTime())));
            children.add(createOutput(app, task.getCreatorName()));
            children.add(createOutput(app, task.getOwnerName()));
            if (isAssignment) {
                children.add(createOutput(app, task.getResolution()));
            }
            children.add(createOutput(app, formatDate(task.getDueDate())));
            children.add(createOutput(app, formatDate(task.getCompletedDateTime())));
            final UIComponent outcomeAndComments = createOutput(app, task.getOutcomeAndComments());
            children.add(ComponentUtil.makeCondenced(outcomeAndComments, 150));
            children.add(createOutput(app, task.getStatus()));
        }

        group.getChildren().add(taskGrid);
    }

    private String formatDate(Date date) {
        if (date != null) {
            return DateFormatUtils.format(date, "dd.MM.yyyy");
        }
        return "&nbsp;";
    }

    private UIComponent createOutput(Application app, Object value) {
        return createOutput(app, value, null);
    }

    private UIComponent createOutput(Application app, Object value, String styleClass) {
        HtmlOutputText output = (HtmlOutputText) app.createComponent(HtmlOutputText.COMPONENT_TYPE);
        if (styleClass != null) {
            output.setStyleClass(styleClass);
        }
        if (value != null && value.equals("&nbsp;")) {
            output.setEscape(false);
        }
        output.setValue(value);
        return output;
    }

    public HtmlPanelGroup getWfPanelGroup() {
        if (wfPanelGroup == null) {
            wfPanelGroup = new HtmlPanelGroup();
        }
        return wfPanelGroup;
    }

    public void setWfPanelGroup(HtmlPanelGroup wfPanelGroup) {
        this.wfPanelGroup = wfPanelGroup;
    }

}
