package ee.webmedia.alfresco.postipoiss.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsImporter;
import ee.webmedia.alfresco.user.service.UserService;

public class IsDocumentsImportEnabledEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 0L;

    @Override
    public boolean evaluate(Object obj) {
        UserService userService = (UserService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserService.BEAN_NAME);
        if (!userService.isAdministrator()) {
            return false;
        }

        PostipoissDocumentsImporter importer = (PostipoissDocumentsImporter)
                FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                        .getBean("postipoissDocumentsImporter");
        return importer.isEnabled() && !importer.isStarted();
    }

    @Override
    public boolean evaluate(Node node) {
        return evaluate((Object) node);
    }
}
