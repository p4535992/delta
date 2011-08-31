package ee.webmedia.alfresco.common.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Vladimir Drozdik
 */
public class DeleteDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private static final String MSG_DELETE = "delete";
    public static final String PARAM_OBJECT_NODEREF = "nodeRef";
    public static final String PARAM_OBJECT_NAME = "name";
    public static final String PARAM_TYPE_NAME = "typeName";
    public static final String PARAM_DIALOGS_TOCLOSE = "dialogsToClose";
    private NodeRef objectRef;
    private String objectName;
    private String objectTypeName;
    private String dialogsToClose;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        BeanHelper.getNodeService().deleteNode(objectRef);
        MessageUtil.addInfoMessage("save_success");
        reset();
        if (dialogsToClose != null) {
            return "dialog:close[" + dialogsToClose + "]";
        }
        return outcome;
    }

    private void reset() {
        objectRef = null;

    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    public String getConfirmMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(objectTypeName).append("_remove_").append(objectTypeName).append("_confirm");
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), sb.toString(), objectName);
    }

    @Override
    public String getContainerTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append(objectTypeName).append("_remove_").append(objectTypeName).append("_title");
        return MessageUtil.getMessage(sb.toString(), objectName);
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage(MSG_DELETE);
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    public void setupDeleteDialog(ActionEvent event) {
        String objectRefString = ActionUtil.getParam(event, PARAM_OBJECT_NODEREF);
        objectRef = new NodeRef(objectRefString);
        objectTypeName = BeanHelper.getNodeService().getType(objectRef).getLocalName();
        objectName = ActionUtil.getParam(event, PARAM_OBJECT_NAME);
        if (ActionUtil.hasParam(event, "dialogsToClose")) {
            dialogsToClose = ActionUtil.getParam(event, PARAM_DIALOGS_TOCLOSE);
        }
    }

    public String getObjectName() {
        return objectName;
    }
}
