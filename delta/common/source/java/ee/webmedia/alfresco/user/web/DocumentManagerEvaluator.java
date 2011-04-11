package ee.webmedia.alfresco.user.web;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.user.service.UserService;

/**
 * Evaluator, that evaluates to true if user is admin or document manager
 * 
 * @author Ats Uiboupin
 */
public class DocumentManagerEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 2958297435415449179L;

    @Override
    public boolean evaluate(Node node) {
        UserService userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(//
                FacesContext.getCurrentInstance()).getBean(UserService.BEAN_NAME);
        return userService.isDocumentManager();
    }
}
