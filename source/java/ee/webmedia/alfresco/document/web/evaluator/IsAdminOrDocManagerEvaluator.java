package ee.webmedia.alfresco.document.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.user.service.UserService;

/**
 * UI action evaluator for validating whether current user is in Administrators or Document Managers user group.
 * 
 * @author Romet Aidla
 */
public class IsAdminOrDocManagerEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        UserService userService = (UserService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserService.BEAN_NAME);
        return userService.isAdministrator() || userService.isDocumentManager();
    }

    @Override
    public boolean evaluate(Node node) {
        return evaluate((Object) node);
    }
}
