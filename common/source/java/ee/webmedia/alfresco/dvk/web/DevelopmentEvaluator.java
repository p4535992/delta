package ee.webmedia.alfresco.dvk.web;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.service.ApplicationService;

/**
 * Evaluator that returns true if project.test = true (meaning development or WM internal testing)
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class DevelopmentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Object node) {
        return ((ApplicationService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), ApplicationService.BEAN_NAME)).isTest();
    }

    @Override
    public boolean evaluate(Node node) {
        return evaluate((Object) node);
    }

}
