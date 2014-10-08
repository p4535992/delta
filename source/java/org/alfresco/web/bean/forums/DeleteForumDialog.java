<<<<<<< HEAD
package org.alfresco.web.bean.forums;

import javax.faces.context.FacesContext;

import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.spaces.DeleteSpaceDialog;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Kaarel Jõgeva
 */
public class DeleteForumDialog extends DeleteSpaceDialog {

    private static final long serialVersionUID = 1L;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        Node node = this.browseBean.getActionSpace();
        NodeRef documentNodeRef = null;
        ChildAssociationRef assoc = null;
        if (node != null) {
            // remove discussable aspect from parent
            assoc = getNodeService().getPrimaryParent(node.getNodeRef());
            if(assoc != null) {
                documentNodeRef = assoc.getParentRef();
            }
        }

        outcome = super.finishImpl(context, outcome);

        if(assoc != null && documentNodeRef != null) {
            getNodeService().removeAspect(documentNodeRef, ForumModel.ASPECT_DISCUSSABLE);
        }

        return outcome;
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        Node node = this.browseBean.getActionSpace();

        if (node != null && this.getNodeService().exists(node.getNodeRef()) == false) {
            // remove this node from the breadcrumb if required
            this.browseBean.removeSpaceFromBreadcrumb(node);

            // clear action context
            this.browseBean.setActionSpace(null);
        }
        
        return AlfrescoNavigationHandler.getMultipleCloseOutcome(2);
    }

    @Override
    protected String getErrorMessageId() {
        return "error_delete_space";
    }

    @Override
    public String getConfirmMessage() {
        NodeRef parentNodeRef = getNodeService().getPrimaryParent(navigator.getCurrentNode().getNodeRef()).getParentRef();
        return MessageUtil.getMessage("forum_delete_forum_confirm", getNodeService().getProperty(parentNodeRef, DocumentCommonModel.Props.DOC_NAME).toString());
    }

    @Override
    public String getContainerTitle() {
        return MessageUtil.getMessage("forum_delete_forum");
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("delete");
    }

}
=======
package org.alfresco.web.bean.forums;

import javax.faces.context.FacesContext;

import org.alfresco.model.ForumModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.spaces.DeleteSpaceDialog;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;

public class DeleteForumDialog extends DeleteSpaceDialog {

    private static final long serialVersionUID = 1L;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        Node node = this.browseBean.getActionSpace();
        NodeRef documentNodeRef = null;
        ChildAssociationRef assoc = null;
        if (node != null) {
            // remove discussable aspect from parent
            assoc = getNodeService().getPrimaryParent(node.getNodeRef());
            if(assoc != null) {
                documentNodeRef = assoc.getParentRef();
            }
        }

        outcome = super.finishImpl(context, outcome);

        if(assoc != null && documentNodeRef != null) {
            getNodeService().removeAspect(documentNodeRef, ForumModel.ASPECT_DISCUSSABLE);
        }

        return outcome;
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        Node node = this.browseBean.getActionSpace();

        if (node != null && this.getNodeService().exists(node.getNodeRef()) == false) {
            // remove this node from the breadcrumb if required
            this.browseBean.removeSpaceFromBreadcrumb(node);

            // clear action context
            this.browseBean.setActionSpace(null);
        }
        
        return AlfrescoNavigationHandler.getMultipleCloseOutcome(2);
    }

    @Override
    protected String getErrorMessageId() {
        return "error_delete_space";
    }

    @Override
    public String getConfirmMessage() {
        NodeRef parentNodeRef = getNodeService().getPrimaryParent(navigator.getCurrentNode().getNodeRef()).getParentRef();
        return MessageUtil.getMessage("forum_delete_forum_confirm", getNodeService().getProperty(parentNodeRef, DocumentCommonModel.Props.DOC_NAME).toString());
    }

    @Override
    public String getContainerTitle() {
        return MessageUtil.getMessage("forum_delete_forum");
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("delete");
    }

}
>>>>>>> develop-5.1
