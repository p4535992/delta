package ee.webmedia.alfresco.postipoiss.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.postipoiss.PostipoissStructureImporter;
import ee.webmedia.alfresco.user.service.UserService;

public class IsStructureImportEnabledEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 0L;
    
    public boolean evaluate(Object obj) {
        UserService userService = (UserService) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), UserService.BEAN_NAME);
        if (!userService.isAdministrator()) {
            return false;
        }

        PostipoissStructureImporter importer = (PostipoissStructureImporter)
                FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                .getBean("postipoissStructureImporter");
        return importer.isEnabled() && !importer.isStarted();
    }
    
    public boolean evaluate(Node node) {
        return evaluate((Object)node);
    }
}
