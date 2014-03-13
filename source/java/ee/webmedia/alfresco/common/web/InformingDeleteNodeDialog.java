package ee.webmedia.alfresco.common.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.content.DeleteContentDialog;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * The bean that backs up deleting node
 */
public class InformingDeleteNodeDialog extends DeleteContentDialog {
    private static final long serialVersionUID = 1L;
    private String confirmMsgKey;
    private String successMsgKey;
    private NodeRef nodeRefToDelete;
    private String deletableObjectNameProp;
    private String containerTitleMsgKey;
    private String callback;

    public void setupDelete(ActionEvent event) {
        nodeRefToDelete = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        containerTitleMsgKey = ActionUtil.getParam(event, "containerTitleMsgKey", getDefaultConfirmMsgKey());
        confirmMsgKey = ActionUtil.getParam(event, "confirmMsgKey", getDefaultConfirmMsgKey());
        successMsgKey = ActionUtil.getParam(event, "successMsgKey", getDefaultMsgKey());
        deletableObjectNameProp = ActionUtil.getParam(event, "deletableObjectNameProp", "");
        callback = ActionUtil.getParam(event, "callback", "");
    }

    protected String getDefaultConfirmMsgKey() {
        // feel free to set up some general notification (at the moment default is not used, as it is given with parameter)
        // For example "Are you shure you want to delete selected object?"
        return "";
    }

    protected String getDefaultMsgKey() {
        return "delete_success";
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        super.finishImpl(context, outcome);
        addSuccessMessage();
        processCallback(context);
        return outcome;
    }

    @Override
    protected Pair<String, Object[]> getConfirmMessageKeyAndPlaceholders() {
        final Node nodeToDelete = getNodeToDelete();
        String objectName = deletableObjectNameProp != "" ? (String) nodeToDelete.getProperties().get(deletableObjectNameProp) : null;
        final Object[] msgValueHolder = new Object[] { objectName };
        return new Pair<String, Object[]>(confirmMsgKey, msgValueHolder);
    }

    private void addSuccessMessage() {
        MessageUtil.addInfoMessage(successMsgKey); // notification_delete_success
    }

    private void processCallback(FacesContext context) {
        if (StringUtils.isBlank(callback)) {
            return;
        }

        context.getApplication().createMethodBinding("#{" + callback + "}", new Class[0]).invoke(context, null);
    }

    @Override
    protected Node getNodeToDelete() {
        return new Node(nodeRefToDelete);
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME; // don't go to browse view
    }

    @Override
    public String getContainerTitleMsgKey() {
        return containerTitleMsgKey;
    }

    @Override
    public String getContainerTitle() {
        return MessageUtil.getMessage(getContainerTitleMsgKey());
    }
}
