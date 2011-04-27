package ee.webmedia.alfresco.dvk.web;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.service.ApplicationService;

/**
 * Evaluator that returns true if project.test = true (meaning development or WM internal testing)
 * 
 * @author Ats Uiboupin
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
