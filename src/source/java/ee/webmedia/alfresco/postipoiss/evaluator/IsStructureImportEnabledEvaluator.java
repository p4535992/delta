package ee.webmedia.alfresco.postipoiss.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.postipoiss.PostipoissStructureImporter;

public class IsStructureImportEnabledEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 0L;
    
    public boolean evaluate(Object obj) {
        PostipoissStructureImporter importer = (PostipoissStructureImporter)
                FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                .getBean("postipoissStructureImporter");
        return importer.isEnabled() && !importer.isStarted();
    }
    
    public boolean evaluate(Node node) {
        return evaluate((Object)node);
    }
}
