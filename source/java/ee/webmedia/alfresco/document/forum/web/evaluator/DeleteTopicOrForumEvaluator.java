<<<<<<< HEAD
package ee.webmedia.alfresco.document.forum.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static org.alfresco.repo.security.authentication.AuthenticationUtil.getRunAsUser;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * @author Kaarel Jõgeva
 */
public class DeleteTopicOrForumEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        boolean admin = getUserService().isAdministrator();
        String ownerId = (String) getGeneralService().getAncestorWithType(node.getNodeRef(), DocumentCommonModel.Types.DOCUMENT).getProperties()
                .get(DocumentCommonModel.Props.OWNER_ID);
        boolean documentOwner = getRunAsUser().equals(ownerId);

        // Creator also qualifies for topics
        if (ForumModel.TYPE_TOPIC.equals(node.getType())) {
            return admin || documentOwner || getRunAsUser().equals(node.getProperties().get(ContentModel.PROP_CREATOR).toString());
        }

        return admin || documentOwner; // Is administrator or parent document owner
    }
}
=======
package ee.webmedia.alfresco.document.forum.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static org.alfresco.repo.security.authentication.AuthenticationUtil.getRunAsUser;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class DeleteTopicOrForumEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        boolean admin = getUserService().isAdministrator();
        String ownerId = (String) getGeneralService().getAncestorWithType(node.getNodeRef(), DocumentCommonModel.Types.DOCUMENT).getProperties()
                .get(DocumentCommonModel.Props.OWNER_ID);
        boolean documentOwner = getRunAsUser().equals(ownerId);

        // Creator also qualifies for topics
        if (ForumModel.TYPE_TOPIC.equals(node.getType())) {
            return admin || documentOwner || getRunAsUser().equals(node.getProperties().get(ContentModel.PROP_CREATOR).toString());
        }

        return admin || documentOwner; // Is administrator or parent document owner
    }
}
>>>>>>> develop-5.1
