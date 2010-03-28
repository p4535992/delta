package ee.webmedia.alfresco.workflow.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIPropertySheet;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.exception.SignatureRuntimeException;
import ee.webmedia.alfresco.signature.model.SignatureDigest;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.signature.web.SignatureAppletModalComponent;
import ee.webmedia.alfresco.signature.web.SignatureBlockBean;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ActionUtil;
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
    }

    public boolean isCompoundWorkflowOwner() {
        return getWorkflowService().isOwner(getCompoundWorkflows());
    }

    public List<ActionDefinition> findCompoundWorkflowDefinitions(String documentTypeQName) {
        QName documentType = QName.createQName(documentTypeQName);
        List<CompoundWorkflowDefinition> workflowDefs = getWorkflowService().getCompoundWorkflowDefinitions(documentType);
        List<ActionDefinition> actionDefinitions = new ArrayList<ActionDefinition>(workflowDefs.size());
        for (CompoundWorkflowDefinition compoundWorkflowDefinition : workflowDefs) {
            ActionDefinition actionDefinition = new ActionDefinition("compoundWorkflowDefinitionAction");
            actionDefinition.Image = DROPDOWN_MENU_ITEM_ICON;
            actionDefinition.Label = compoundWorkflowDefinition.getName();
            actionDefinition.Action = "dialog:compoundWorkflowDialog";
            actionDefinition.ActionListener = "#{CompoundWorkflowDialog.setupNewWorkflow}";
            actionDefinition.addParam("compoundWorkflowDefinitionNodeRef", compoundWorkflowDefinition.getNode().getNodeRef().toString());
            actionDefinition.addParam("documentNodeRef", document.toString());

            actionDefinitions.add(actionDefinition);
        }

        return actionDefinitions;
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
            outcomeIndex = (Integer) task.getNode().getProperties().get("{temp}outcomes");
        } else if (WorkflowSpecificModel.Types.SIGNATURE_TASK.equals(taskType)) {
            if (outcomeIndex == 1) {
                signatureTask = (SignatureTask) task;
                try {
                    getDocumentService().prepareDocumentSigning(document);
                    fileBlockBean.restore();
                    showModal();
                } catch (UnableToPerformException e) {
                    MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
                }
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
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String certHex = (String) facesContext.getExternalContext().getRequestParameterMap().get("cert");
        try {
            SignatureDigest signatureDigest = getDocumentService().prepareDocumentDigest(document, certHex);
            showModal(signatureDigest.getDigestHex());
            signatureTask.setSignatureDigest(signatureDigest);
        } catch (SignatureException e) {
            SignatureBlockBean.addSignatureError(e);
            closeModal();
            signatureTask = null;
        }
    }

    public void signDocument() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String signatureHex = (String) facesContext.getExternalContext().getRequestParameterMap().get("signature");

        try {
            getDocumentService().finishDocumentSigning(signatureTask, signatureHex);
            fileBlockBean.restore();
            metadataBlockBean.viewDocument(getDocumentService().getDocument(metadataBlockBean.getDocument().getNodeRef()));
            restore();
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
            panel.getAttributes().put("styleClass", "panel-100");

            // the properties
            UIPropertySheet sheet = new WMUIPropertySheet();
            sheet.setId("task-sheet-" + node.getId());
            sheet.setNode(node);
            // this ensures we can use more than 1 property sheet on the page
            sheet.setVar("taskNode" + index);
            sheet.getAttributes().put("externalConfig", Boolean.TRUE);
            sheet.getAttributes().put("labelStyleClass", "propertiesLabel");
            sheet.getAttributes().put("columns", 1);
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

                // the review task has only 1 button and the outcomes come from {temp}outcomes property
                if (WorkflowSpecificModel.Types.REVIEW_TASK.equals(taskType)) {
                    node.getProperties().put("{temp}outcomes", 0);
                    outcomeButton.setValue(MessageUtil.getMessage(label));
                    break;
                }
            }

            // delegate button for assignment task only
            if (WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(taskType)) {
                // the save button
                HtmlCommandButton delegateButton = new HtmlCommandButton();
                delegateButton.setId("delegate-id-" + node.getId());
                delegateButton.setAction(app.createMethodBinding("#{" + BEAN_NAME + ".showCompoundWorkflowDialog}", new Class[] {}));
                delegateButton.setActionListener(app.createMethodBinding("#{CompoundWorkflowDialog.setupWorkflow}", new Class[] { ActionEvent.class }));
                delegateButton.setValue(MessageUtil.getMessage("task_delegate_" + taskType.getLocalName()));
                
                UIParameter param = (UIParameter)app.createComponent(ComponentConstants.JAVAX_FACES_PARAMETER);
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

    private void showModal(String digestHex) {
        getModalApplet().showModal(digestHex);
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
        for(WorkflowSummaryItem item : getWorkflowSummaryItems()) {
            if(item.getWorkflowRef().equals(workflowNodeRef)) {
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

}
