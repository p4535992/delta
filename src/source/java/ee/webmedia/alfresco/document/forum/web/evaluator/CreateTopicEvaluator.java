package ee.webmedia.alfresco.document.forum.web.evaluator;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;

/**
 * @author Ats Uiboupin
 */
public class CreateTopicEvaluator extends ManageDiscussionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return super.evaluate(node) && !isDiscussionStarted(node);
    }

    private boolean isDiscussionStarted(Node discussionNode) {
        NodeService nodeService = getNodeService();
        List<ChildAssociationRef> topics = nodeService.getChildAssocs(discussionNode.getNodeRef(), ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
        if (topics.size() > 0) {
            return true;
        }
        return false;
    }

}
