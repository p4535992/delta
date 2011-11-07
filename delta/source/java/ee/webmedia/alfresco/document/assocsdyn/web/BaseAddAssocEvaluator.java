package ee.webmedia.alfresco.document.assocsdyn.web;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.document.web.evaluator.RegisterDocumentEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.ViewStateActionEvaluator;

/**
 * Base evaluator that decides if add association button should be visible for given {@link DocTypeAssocType}
 * 
 * @author Ats Uiboupin
 */
public abstract class BaseAddAssocEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    DocTypeAssocType assocType;

    protected BaseAddAssocEvaluator(DocTypeAssocType assocType) {
        this.assocType = assocType;
    }

    @Override
    public boolean evaluate(Node docNode) {
        if (docNode == null || !new ViewStateActionEvaluator().evaluate(docNode)) {
            return false;
        }
        DocumentType documentType = BeanHelper.getDocumentDynamicDialog().getDocumentType();
        boolean registered = !RegisterDocumentEvaluator.isNotRegistered(docNode);
        return (registered || isAddAssocToUnregistratedDocEnabled(documentType)) && !documentType.getAssociationModels(assocType).isEmpty();
    }

    abstract protected boolean isAddAssocToUnregistratedDocEnabled(DocumentType documentType);

    @Override
    public boolean evaluate(Object obj) {
        throw new RuntimeException("method evaluate(Object) is unimplemented");
    }

}
