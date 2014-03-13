package ee.webmedia.alfresco.document.assocsdyn.web;

import java.util.List;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;

public abstract class AddFollowUpDocumentTypeAssocEvaluator extends AddFollowUpAssocEvaluator {
    private static final long serialVersionUID = 1L;

    private final String documentTypeId;

    protected AddFollowUpDocumentTypeAssocEvaluator(String documentTypeId) {
        this.documentTypeId = documentTypeId;
    }

    @Override
    public boolean evaluate(Node docNode) {
        boolean result = super.evaluate(docNode);
        if (!result) {
            return false;
        }
        DocumentType documentType = BeanHelper.getDocumentDynamicDialog().getDocumentType();
        List<? extends AssociationModel> associationModels = documentType.getAssociationModels(assocType);
        for (AssociationModel associationModel : associationModels) {
            if (documentTypeId.equals(associationModel.getDocType())) {
                return true;
            }
        }
        return false;
    }

}
