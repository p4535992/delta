package ee.webmedia.alfresco.document.web.evaluator;

<<<<<<< HEAD
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * @author Alar Kvell
 */
=======
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.service.DocumentService;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class AddFavoritesDocumentEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node docNode) {
        if (!docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        ViewStateActionEvaluator viewStateEval = new ViewStateActionEvaluator();
        if (!viewStateEval.evaluate(docNode)) {
            return false;
        }
<<<<<<< HEAD
        return BeanHelper.getDocumentFavoritesService().isFavoriteAddable(docNode.getNodeRef());
=======

        FacesContext context = FacesContext.getCurrentInstance();
        DocumentService documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(//
                context).getBean(DocumentService.BEAN_NAME);
        return documentService.isFavoriteAddable(docNode.getNodeRef());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

}
