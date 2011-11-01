package ee.webmedia.alfresco.document.web.evaluator;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;

/**
 * @author Kaarel Jõgeva
 */
public class ChangeByNewDocumentEvaluator extends BaseActionEvaluator {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean evaluate(Node node) {
        return new ViewStateActionEvaluator().evaluate(node) && new IsAdminOrDocManagerEvaluator().evaluate(node) && isChangeByNewDocumentEnabled(node);
    }

    private boolean isChangeByNewDocumentEnabled(Node node) {
        String docTypeId = (String) node.getProperties().get(Props.OBJECT_TYPE_ID);
        NodeRef docTypeRef = getDocumentAdminService().getDocumentTypeRef(docTypeId);
        return docTypeRef != null && getDocumentAdminService().getDocumentTypeProperty(docTypeId, DocumentAdminModel.Props.CHANGE_BY_NEW_DOCUMENT_ENABLED, Boolean.class);
    }

    @Override
    public boolean evaluate(Object obj) {
        throw new RuntimeException("method evaluate(obj) is unimplemented");
    }
}
