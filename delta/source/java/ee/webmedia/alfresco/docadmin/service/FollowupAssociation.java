package ee.webmedia.alfresco.docadmin.service;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * When added to {@link DocumentType} then means that that document type can have followup association to another document
 * 
 * @author Ats Uiboupin
 */
public class FollowupAssociation extends AssociationModel {
    private static final long serialVersionUID = 1L;

    public FollowupAssociation(BaseObject parentDocType, WmNode node) {
        super(parentDocType, node);
    }

    public FollowupAssociation(NodeRef parentDocTypeRef, WmNode node) {
        super(parentDocTypeRef, node);
    }

    public FollowupAssociation(DocumentType parentDocType) {
        super(parentDocType, DocumentAdminModel.Types.FOLLOWUP_ASSOCIATION);
    }

    @Override
    public DocTypeAssocType getAssociationType() {
        return DocTypeAssocType.FOLLOWUP;
    }

}
