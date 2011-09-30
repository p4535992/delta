package ee.webmedia.alfresco.common.web;

import static org.apache.commons.lang.StringUtils.uncapitalize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
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
        confirmMessagePlaceholders = null;
        objectType = null;
        dialogsToClose = null;
        deleteAfterConfirmHandler = null;
        deleteAfterConfirmActionComponentState = null;
        deleteAfterConfirmActionEventParams = null;
        containerTitle = null;
        showObjectData = Boolean.FALSE;
        showConfirm = Boolean.TRUE;
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    public String getConfirmMessage() {
        String confirmMsgKey = new StringBuilder(DELETE_DIALOG_MSG_PREFIX).append("delete_").append(objectType.getLocalName()).append("_confirm").toString();
        return MessageUtil.getMessage(confirmMsgKey, confirmMessagePlaceholders);
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

}
