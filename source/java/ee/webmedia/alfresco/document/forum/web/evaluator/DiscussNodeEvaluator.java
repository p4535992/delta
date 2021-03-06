package ee.webmedia.alfresco.document.forum.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;

import java.util.List;

import org.alfresco.model.ForumModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.model.Privilege;

/**
 * UI Action Evaluator - Discuss a node.
 */
public class DiscussNodeEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    /**
     * @see org.alfresco.web.action.ActionEvaluator#evaluate(org.alfresco.web.bean.repository.Node)
     */
    @Override
    public boolean evaluate(Node node) {
        if (!node.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE) || getDocumentDialogHelperBean().isInEditMode()) {
            return false;
        }
        boolean result = false;
        node = BeanHelper.getGeneralService().fetchNode(node.getNodeRef()); // refresh the node, because dialog caches it, and sub dialogs change props/aspects

        NodeRef forumNodeRef = null;
        if (node.hasAspect(ForumModel.ASPECT_DISCUSSABLE)) {
            List<ChildAssociationRef> children = BeanHelper.getNodeService().getChildAssocs(node.getNodeRef(), ForumModel.ASSOC_DISCUSSION, RegexQNamePattern.MATCH_ALL);

            // make sure there is one visible child association for the node
            if (children.size() == 1) {
                forumNodeRef = children.get(0).getChildRef();
                result = true;
            }
        }

        return result && (new ManageDiscussionEvaluator().evaluate(node) || isUserInvited(forumNodeRef));
    }

    private boolean isUserInvited(NodeRef forumNodeRef) {
        return BeanHelper.getPrivilegeService().hasPermission(forumNodeRef, AuthenticationUtil.getRunAsUser(), Privilege.PARTICIPATE_AT_FORUM);
    }
}