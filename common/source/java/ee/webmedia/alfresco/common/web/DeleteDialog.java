package ee.webmedia.alfresco.common.web;

import static org.apache.commons.lang.StringUtils.uncapitalize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

<<<<<<< HEAD
/**
 * @author Vladimir Drozdik
 * @author Ats Uiboupin - refactored to be more flexible and use dialog title/success message based on node type
 */
=======
>>>>>>> develop-5.1
public class DeleteDialog extends BaseDialogBean {
    private static final String DELETE_DIALOG_MSG_PREFIX = "deleteDialog_";
    private static final long serialVersionUID = 1L;
    private NodeRef objectRef;
    private Object[] confirmMessagePlaceholders;
    private QName objectType;
    private Integer dialogsToClose;
    /** optional delete handler (formatted as BeanName.methodName) to be called that performs actual delete */
    private String deleteAfterConfirmHandler;
    private Object deleteAfterConfirmActionComponentState;
    private Map<String, String> deleteAfterConfirmActionEventParams;
    private String containerTitle;
    private String typeNameTranslated;
    private Boolean showObjectData = Boolean.FALSE;
    private Boolean showConfirm = Boolean.TRUE;
<<<<<<< HEAD

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (StringUtils.isNotBlank(deleteAfterConfirmHandler)) {
            MethodBinding deleteMB = context.getApplication().createMethodBinding("#{" + deleteAfterConfirmHandler + "}", new Class[] { ActionEvent.class });
            UIActionLink actionLink = (UIActionLink) context.getApplication().createComponent(UIActions.COMPONENT_ACTIONLINK); // Is UIActionLink always correct?
            actionLink.restoreState(context, deleteAfterConfirmActionComponentState);
            actionLink.getParameterMap().putAll(deleteAfterConfirmActionEventParams);
            outcome = (String) deleteMB.invoke(context, new Object[] { new ActionEvent(actionLink) });
            resetAndAddSuccessMessage();
            return outcome;
        }
        NodeService nodeService = BeanHelper.getNodeService();
        boolean isDocument = DocumentCommonModel.Types.DOCUMENT.equals(objectType);
        LogEntry logEntry = null;
        if (isDocument) {
            logEntry = LogEntry.create(LogObject.DOCUMENT, BeanHelper.getUserService(), objectRef, "document_log_status_deleted_from",
                    nodeService.getProperty(objectRef, DocumentCommonModel.Props.DOC_STATUS), BeanHelper.getDocumentListService().getDisplayPath(objectRef, false));
        }
        nodeService.deleteNode(objectRef);
        if (dialogsToClose != null) {
            outcome = getCloseOutcome(dialogsToClose);
        }
        if (isDocument) {
            BeanHelper.getLogService().addLogEntry(logEntry);
        }
        resetAndAddSuccessMessage();
        return outcome;
    }

    private void resetAndAddSuccessMessage() {
        reset();
        MessageUtil.addInfoMessage(DELETE_DIALOG_MSG_PREFIX + "objectDeleted", typeNameTranslated);
=======
    private boolean alreadyDeleted;
    private String alreadyDeletedHandler;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        NodeService nodeService = BeanHelper.getNodeService();
        boolean exists = nodeService.exists(objectRef);
        if (exists) {
            deleteExistingNode(context, nodeService);
        } else {
            handleAlreadyDeleted(context);
        }
        outcome = getCustomOutcome(outcome);
        resetAndAddSuccessMessage(exists);
        return outcome;
    }
    
    private void handleAlreadyDeleted(FacesContext context) {
        if (StringUtils.isNotBlank(alreadyDeletedHandler)) {
            MethodBinding handleAlreadyDeletedMB = context.getApplication().createMethodBinding("#{" + alreadyDeletedHandler + "}", new Class[] {});
            handleAlreadyDeletedMB.invoke(context, new Object[] {});
        }
    }    

    private void deleteExistingNode(FacesContext context, NodeService nodeService) {
        if (isCustomDelete()) {
            invokeCustomDelete(context);
        } else {
            deleteExistingNode(nodeService);
        }
    }

    @SuppressWarnings("deprecation")
    private void invokeCustomDelete(FacesContext context) {
        MethodBinding deleteMB = context.getApplication().createMethodBinding("#{" + deleteAfterConfirmHandler + "}", new Class[] { ActionEvent.class });
        UIActionLink actionLink = (UIActionLink) context.getApplication().createComponent(UIActions.COMPONENT_ACTIONLINK); // Is UIActionLink always correct?
        actionLink.restoreState(context, deleteAfterConfirmActionComponentState);
        actionLink.getParameterMap().putAll(deleteAfterConfirmActionEventParams);
        deleteMB.invoke(context, new Object[] { new ActionEvent(actionLink) });
    }

    private void deleteExistingNode(NodeService nodeService) {
        if (DocumentCommonModel.Types.DOCUMENT.equals(objectType)) {
            LogEntry logEntry = LogEntry.create(LogObject.DOCUMENT, BeanHelper.getUserService(), objectRef, "document_log_status_deleted_from",
                    nodeService.getProperty(objectRef, DocumentCommonModel.Props.DOC_STATUS), BeanHelper.getDocumentListService().getDisplayPath(objectRef, false));
            BeanHelper.getLogService().addLogEntry(logEntry);
        }
        nodeService.deleteNode(objectRef);
    }

    private String getCustomOutcome(String outcome) {
        if (dialogsToClose != null) {
            outcome = getCloseOutcome(dialogsToClose);
        }
        return outcome;
    }

    private boolean isCustomDelete() {
        return StringUtils.isNotBlank(deleteAfterConfirmHandler);
    }

    private void resetAndAddSuccessMessage(boolean deleted) {
        reset();
        if (deleted) {
            MessageUtil.addInfoMessage(DELETE_DIALOG_MSG_PREFIX + "objectDeleted", typeNameTranslated);
        } else {
            MessageUtil.addInfoMessage("delete_error_already_deleted");
        }
>>>>>>> develop-5.1
    }

    private void reset() {
        objectRef = null;
        confirmMessagePlaceholders = null;
        objectType = null;
        dialogsToClose = null;
        deleteAfterConfirmHandler = null;
        deleteAfterConfirmActionComponentState = null;
        deleteAfterConfirmActionEventParams = null;
        containerTitle = null;
        showObjectData = Boolean.FALSE;
        showConfirm = Boolean.TRUE;
<<<<<<< HEAD
=======
        alreadyDeleted = false;
        alreadyDeletedHandler = null;
>>>>>>> develop-5.1
    }

    @Override
    public String cancel() {
<<<<<<< HEAD
        reset();
        return super.cancel();
=======
        String outcome = super.cancel();
        if (alreadyDeleted) {
            handleAlreadyDeleted(FacesContext.getCurrentInstance());
            outcome = getCustomOutcome(outcome);
        }
        reset();
        return outcome;
>>>>>>> develop-5.1
    }

    public String getConfirmMessage() {
        String confirmMsgKey = new StringBuilder(DELETE_DIALOG_MSG_PREFIX).append("delete_").append(objectType.getLocalName()).append("_confirm").toString();
        return MessageUtil.getMessage(confirmMsgKey, confirmMessagePlaceholders);
    }

    @Override
    public String getContainerTitle() {
        if (containerTitle == null) { // cache - this method is called several times within one request
<<<<<<< HEAD
            typeNameTranslated = MessageUtil.getTypeName(objectType);
            containerTitle = MessageUtil.getMessage(DELETE_DIALOG_MSG_PREFIX + "containerTitle", uncapitalize(typeNameTranslated));
=======
            if (alreadyDeleted) {
                containerTitle = MessageUtil.getMessage("delete_error_already_deleted");
            } else {
                typeNameTranslated = MessageUtil.getTypeName(objectType);
                containerTitle = MessageUtil.getMessage(DELETE_DIALOG_MSG_PREFIX + "containerTitle", uncapitalize(typeNameTranslated));
            }
>>>>>>> develop-5.1
        }
        return containerTitle;
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("delete");
    }

    @Override
<<<<<<< HEAD
=======
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return !alreadyDeleted;
    }

    @Override
>>>>>>> develop-5.1
    public boolean getFinishButtonDisabled() {
        return false;
    }

    public void setupDeleteDialog(ActionEvent event) {
<<<<<<< HEAD
        deleteAfterConfirmHandler = ActionUtil.getParam(event, "deleteAfterConfirmHandler", "");
        if (StringUtils.isNotBlank(deleteAfterConfirmHandler)) {
            deleteAfterConfirmActionComponentState = event.getComponent().saveState(FacesContext.getCurrentInstance());
            deleteAfterConfirmActionEventParams = ActionUtil.getParams(event);
        }
        objectRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        String placeHolderPrefix = "confirmMessagePlaceholder";
        int index = 0;
        List<String> confirmMsgPlaceholders = new ArrayList<String>();
        while (ActionUtil.hasParam(event, placeHolderPrefix + index)) {
            confirmMsgPlaceholders.add(ActionUtil.getParam(event, placeHolderPrefix + index++));
        }
        confirmMessagePlaceholders = confirmMsgPlaceholders.toArray(new String[confirmMsgPlaceholders.size()]);
        objectType = getNodeService().getType(objectRef);
        if (ActionUtil.hasParam(event, "dialogsToClose")) {
            dialogsToClose = ActionUtil.getParam(event, "dialogsToClose", Integer.class);
        }
        if (ActionUtil.hasParam(event, "showObjectData")) {
            showObjectData = ActionUtil.getParam(event, "showObjectData", Boolean.class);
        }
        if (ActionUtil.hasParam(event, "showConfirm")) {
            showConfirm = ActionUtil.getParam(event, "showConfirm", Boolean.class);
        }
    }
=======
        objectRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        boolean nodeExists = BeanHelper.getNodeService().exists(objectRef);
        if (!nodeExists) {
            alreadyDeleted = true;
            showObjectData = false;
            showConfirm = false;
        } else {
            deleteAfterConfirmHandler = ActionUtil.getParam(event, "deleteAfterConfirmHandler", "");
            if (StringUtils.isNotBlank(deleteAfterConfirmHandler)) {
                deleteAfterConfirmActionComponentState = event.getComponent().saveState(FacesContext.getCurrentInstance());
                deleteAfterConfirmActionEventParams = ActionUtil.getParams(event);
            }
            objectType = getNodeService().getType(objectRef);
            String placeHolderPrefix = "confirmMessagePlaceholder";
            int index = 0;
            List<String> confirmMsgPlaceholders = new ArrayList<String>();
            while (ActionUtil.hasParam(event, placeHolderPrefix + index)) {
                confirmMsgPlaceholders.add(ActionUtil.getParam(event, placeHolderPrefix + index++));
            }
            confirmMessagePlaceholders = confirmMsgPlaceholders.toArray(new String[confirmMsgPlaceholders.size()]);
            if (ActionUtil.hasParam(event, "showObjectData")) {
                showObjectData = ActionUtil.getParam(event, "showObjectData", Boolean.class);
            }
            if (ActionUtil.hasParam(event, "showConfirm")) {
                showConfirm = ActionUtil.getParam(event, "showConfirm", Boolean.class);
            }
        }
        alreadyDeletedHandler = ActionUtil.getParam(event, "alreadyDeletedHandler", "");
        if (ActionUtil.hasParam(event, "dialogsToClose")) {
            dialogsToClose = ActionUtil.getParam(event, "dialogsToClose", Integer.class);
        }
    }

>>>>>>> develop-5.1
    public Boolean getShowObjectData() {
        return showObjectData;
    }

    public Node getObjectNode() {
        if (objectRef == null) {
            return null;
        }
        return new MapNode(objectRef);
    }

    public Boolean getShowConfirm() {
        return showConfirm;
    }

<<<<<<< HEAD
}
=======
}
>>>>>>> develop-5.1
