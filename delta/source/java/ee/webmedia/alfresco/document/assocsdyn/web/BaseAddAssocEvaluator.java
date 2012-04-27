package ee.webmedia.alfresco.document.assocsdyn.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.document.web.evaluator.RegisterDocumentEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.ViewStateActionEvaluator;

/**
 * Base evaluator that decides if add association button should be visible for given {@link DocTypeAssocType}
 * 
 * @author Ats Uiboupin
 */
public abstract class BaseAddAssocEvaluator extends BaseActionEvaluator {
    private static final long serialVersionUID = 1L;

    protected DocTypeAssocType assocType;
    protected boolean skipFollowUpReportAndErrandOrderAbroad = true;

    protected BaseAddAssocEvaluator(DocTypeAssocType assocType) {
        this.assocType = assocType;
    }

    @Override
    public boolean evaluate(Node docNode) {
        if (docNode != null && !docNode.getNodeRef().getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            return false;
        }
        if (docNode == null || !new ViewStateActionEvaluator().evaluate(docNode)) {
            return false;
        }
        DocumentType documentType = BeanHelper.getDocumentDynamicDialog().getDocumentType();
        boolean registered = !RegisterDocumentEvaluator.isNotRegistered(docNode);
        if (registered || isAddAssocToUnregistratedDocEnabled(documentType)) {
            List<? extends AssociationModel> associationModels = new ArrayList<AssociationModel>(documentType.getAssociationModels(assocType));
            if (skipFollowUpReportAndErrandOrderAbroad) {
                for (Iterator<? extends AssociationModel> it = associationModels.iterator(); it.hasNext();) {
                    AssociationModel associationModel = it.next();
                    String docTypeId = associationModel.getDocType();
                    if (assocType == DocTypeAssocType.FOLLOWUP
                            && (SystematicDocumentType.REPORT.isSameType(docTypeId) || SystematicDocumentType.ERRAND_ORDER_ABROAD.isSameType(docTypeId))) {
                        it.remove();
                    }
                }
            }
            return !associationModels.isEmpty();
        }
        return false;
    }

    abstract protected boolean isAddAssocToUnregistratedDocEnabled(DocumentType documentType);

    @Override
    public boolean evaluate(Object obj) {
        throw new RuntimeException("method evaluate(Object) is unimplemented");
    }

}
