package ee.webmedia.alfresco.document.forum.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.user.service.UserService;

public class DeleteTopicOrForumEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        boolean documentManager = ((UserService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserService.BEAN_NAME)).isDocumentManager();
        // For topics owner also qualifies
        if (node.getType().equals(ForumModel.TYPE_TOPIC)) {
            return documentManager || AuthenticationUtil.getRunAsUser().equals(node.getProperties().get(ContentModel.PROP_CREATOR).toString());
        }
        return documentManager;
    }
}
