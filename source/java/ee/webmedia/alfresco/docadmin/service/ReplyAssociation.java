<<<<<<< HEAD
package ee.webmedia.alfresco.docadmin.service;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * When added to {@link DocumentType} then means that that document type can have reply association to another document
 * 
 * @author Ats Uiboupin
 */
public class ReplyAssociation extends AssociationModel {
    private static final long serialVersionUID = 1L;

    public ReplyAssociation(BaseObject parentDocType, WmNode node) {
        super(parentDocType, node);
    }

    public ReplyAssociation(NodeRef parentDocTypeRef, WmNode node) {
        super(parentDocTypeRef, node);
    }

    public ReplyAssociation(DocumentType parentDocType) {
        super(parentDocType, DocumentAdminModel.Types.REPLY_ASSOCIATION);
    }

    @Override
    public DocTypeAssocType getAssociationType() {
        return DocTypeAssocType.REPLY;
    }

}
=======
package ee.webmedia.alfresco.docadmin.service;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * When added to {@link DocumentType} then means that that document type can have reply association to another document
 */
public class ReplyAssociation extends AssociationModel {
    private static final long serialVersionUID = 1L;

    public ReplyAssociation(BaseObject parentDocType, WmNode node) {
        super(parentDocType, node);
    }

    public ReplyAssociation(NodeRef parentDocTypeRef, WmNode node) {
        super(parentDocTypeRef, node);
    }

    public ReplyAssociation(DocumentType parentDocType) {
        super(parentDocType, DocumentAdminModel.Types.REPLY_ASSOCIATION);
    }

    @Override
    public DocTypeAssocType getAssociationType() {
        return DocTypeAssocType.REPLY;
    }

}
>>>>>>> develop-5.1
