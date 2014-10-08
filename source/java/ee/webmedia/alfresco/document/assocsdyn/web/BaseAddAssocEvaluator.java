package ee.webmedia.alfresco.document.assocsdyn.web;

import java.util.List;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.evaluator.SharedResourceEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.document.web.evaluator.DocumentDynamicActionsGroupResources;
import ee.webmedia.alfresco.document.web.evaluator.DocumentNotInDraftsFunctionActionEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.RegisterDocumentEvaluator;

/**
 * Base evaluator that decides if add association button should be visible for given {@link DocTypeAssocType}
 */
public abstract class BaseAddAssocEvaluator extends SharedResourceEvaluator {
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
        if (docNode == null || BeanHelper.getDocumentDialogHelperBean().isInEditMode() || !new DocumentNotInDraftsFunctionActionEvaluator().evaluate(docNode)) {
            return false;
        }
        return isRegisteredOrAddAssocToUnregisteredDocEnabled(docNode);
    }

    private boolean isRegisteredOrAddAssocToUnregisteredDocEnabled(Node docNode) {
        DocumentType documentType = BeanHelper.getDocumentDynamicDialog().getDocumentType();
        boolean registered = RegisterDocumentEvaluator.isRegistered(docNode);
        if (registered || isAddAssocToUnregistratedDocEnabled(documentType)) {
            List<? extends AssociationModel> associationModels = documentType.getAssociationModels(assocType);
            int initialSize = associationModels.size();
            if (skipFollowUpReportAndErrandOrderAbroad && assocType == DocTypeAssocType.FOLLOWUP) {
                for (AssociationModel associationModel : associationModels) {
                    String docTypeId = associationModel.getDocType();
                    if ((SystematicDocumentType.REPORT.isSameType(docTypeId) || SystematicDocumentType.ERRAND_ORDER_ABROAD.isSameType(docTypeId))) {
                        initialSize--;
                    } else {
                        break;
                    }
                }
            }
            return initialSize > 0;
        }
        return false;
    }

    abstract protected boolean isAddAssocToUnregistratedDocEnabled(DocumentType documentType);

    @Override
    public boolean evaluate(Object obj) {
        throw new RuntimeException("method evaluate(Object) is unimplemented");
    }

    @Override
    public boolean evaluate() {
        DocumentDynamicActionsGroupResources resource = (DocumentDynamicActionsGroupResources) sharedResource;
        if (resource.isAddAssoc() != null) {
            return resource.isAddAssoc();
        }
        Boolean result = null;
        if (resource.getObject() != null && !resource.isWorkspaceNode()) {
            result = false;
        }
        if (result == null && (resource.getObject() == null || resource.isInEditMode() || !resource.isNotInDraftsFunction())) {
            result = false;
        }
        result = result == null && isRegisteredOrAddAssocToUnregisteredDocEnabled(resource.getObject());
        resource.setAddAssoc(result);
        return result;
    }

}
