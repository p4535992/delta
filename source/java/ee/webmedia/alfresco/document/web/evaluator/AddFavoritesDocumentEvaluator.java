package ee.webmedia.alfresco.document.web.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.service.DocumentService;

public class AddFavoritesDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        if (!viewStateEval.evaluate(docNode)) {
            return false;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        DocumentService documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(//
                context).getBean(DocumentService.BEAN_NAME);
        return documentService.isFavoriteAddable(docNode.getNodeRef());
    }

}
