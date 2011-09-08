package ee.webmedia.alfresco.common.web;

import static org.apache.commons.lang.StringUtils.uncapitalize;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Vladimir Drozdik
 * @author Ats Uiboupin - refactored to be more flexible and use dialog title/success message based on node type
 */
public class DeleteDialog extends BaseDialogBean {
    private static final String DELETE_DIALOG_MSG_PREFIX = "deleteDialog_";
    private static final long serialVersionUID = 1L;
    private NodeRef objectRef;
    private String confirmMessagePlaceholder;
    private QName objectType;
    private Integer dialogsToClose;
    /** optional delete handler (formatted as BeanName.methodName) to be called that performs actual delete */
    private String deleteAfterConfirmHandler;
    private ActionEvent deleteAfterConfirmActionEvent;
    private String containerTitle;
    private String typeNameTranslated;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (StringUtils.isNotBlank(deleteAfterConfirmHandler)) {
            MethodBinding deleteMB = context.getApplication().createMethodBinding("#{" + deleteAfterConfirmHandler + "}", new Class[] { ActionEvent.class });
            outcome = (String) deleteMB.invoke(context, new Object[] { deleteAfterConfirmActionEvent });
            resetAndAddSuccessMessage();
            return outcome;
        }
        BeanHelper.getNodeService().deleteNode(objectRef);
        if (dialogsToClose != null) {
            outcome = getCloseOutcome(dialogsToClose);
        }
        resetAndAddSuccessMessage();
        return outcome;
    }

    private void resetAndAddSuccessMessage() {
        reset();
        MessageUtil.addInfoMessage(DELETE_DIALOG_MSG_PREFIX + "objectDeleted", typeNameTranslated);
    }

    private void reset() {
        objectRef = null;
        confirmMessagePlaceholder = null;
        objectType = null;
        dialogsToClose = null;
        deleteAfterConfirmHandler = null;
        deleteAfterConfirmActionEvent = null;
        containerTitle = null;
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    public String getConfirmMessage() {
        String confirmMsgKey = new StringBuilder(DELETE_DIALOG_MSG_PREFIX).append("delete_").append(objectType.getLocalName()).append("_confirm").toString();
        return MessageUtil.getMessage(confirmMsgKey, confirmMessagePlaceholder);
    }

    @Override
    public String getContainerTitle() {
        if (containerTitle == null) { // cache - this method is called several times within one request
            typeNameTranslated = getTypeName(objectType);
            containerTitle = MessageUtil.getMessage(DELETE_DIALOG_MSG_PREFIX + "containerTitle", uncapitalize(typeNameTranslated));
        }
        return containerTitle;
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("delete");
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    public void setupDeleteDialog(ActionEvent event) {
        deleteAfterConfirmHandler = ActionUtil.getParam(event, "deleteAfterConfirmHandler", "");
        if (StringUtils.isNotBlank(deleteAfterConfirmHandler)) {
            deleteAfterConfirmActionEvent = event;
        }
        objectRef = ActionUtil.getParam(event, "nodeRef", NodeRef.class);
        confirmMessagePlaceholder = ActionUtil.getParam(event, "confirmMessagePlaceholder");
        objectType = getNodeService().getType(objectRef);
        if (ActionUtil.hasParam(event, "dialogsToClose")) {
            dialogsToClose = ActionUtil.getParam(event, "dialogsToClose", Integer.class);
        }
    }

    private String getTypeName(QName objectTypeQName) {
        TypeDefinition typeDef = getDictionaryService().getType(objectTypeQName);
        String translatedTypeName = typeDef.getTitle();
        if (StringUtils.isBlank(translatedTypeName)) {
            throw new IllegalStateException("there should be translation for type " + typeDef
                    + " in model properties file with key '" + getTranslationKeyForType(objectTypeQName, typeDef) + "'");
        }
        return translatedTypeName;
    }

    private String getTranslationKeyForType(QName objectTypeQName, TypeDefinition typeDef) {
        NamespaceService namespaceService = getNamespaceService();
        String model = typeDef.getModel().getName().toPrefixString(namespaceService).replace(":", "_");
        String type = objectTypeQName.toPrefixString(namespaceService).replace(":", "_");
        return model + ".type." + type + ".title";
    }

}
