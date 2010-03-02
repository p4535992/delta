package ee.webmedia.alfresco.document.forum.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.user.service.UserService;

public class DeleteTopicOrForumEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return ((UserService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserService.BEAN_NAME)).isAdministrator();
    }
}

