package ee.webmedia.alfresco.docadmin.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * Object that represents association with some extra info
 * 
 * @author Ats Uiboupin
 */
public class AssociationToDocType extends BaseObject {
    private static final long serialVersionUID = 1L;

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public AssociationToDocType(BaseObject parent, WmNode node) {
        super(parent, node);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public AssociationToDocType(NodeRef parentNodeRef, WmNode node) {
        super(parentNodeRef, node);
        throw new RuntimeException("FIXME DLSeadist unimplemented"); // FIXME DLSeadist
    }

    public AssociationToDocType(DocumentType docType) {
        super(docType, DocumentAdminModel.Types.ASSOCIATION_TO_DOC_TYPE);
    }

    @Override
    public DocumentType getParent() {
        return (DocumentType) super.getParent();
    }

    @Override
    protected QName getAssocName() {
        return QName.createQName(DocumentAdminModel.URI, getAssociationTypeEnum().name() + "_" + getDocType());
    }

    // START: Properties
    public final String getDocType() {
        return getProp(DocumentAdminModel.Props.DOC_TYPE);
    }

    public final void setDocType(String docType) {
        setProp(DocumentAdminModel.Props.DOC_TYPE, docType);
    }

    public final String getAssociationType() {
        return getProp(DocumentAdminModel.Props.ASSOCIATION_TYPE);
    }

    public final void setAssociationType(String associationType) {
        setProp(DocumentAdminModel.Props.ASSOCIATION_TYPE, associationType);
    }

    public DocTypeAssocType getAssociationTypeEnum() {
        return getEnumFromValue(DocTypeAssocType.class, getAssociationType());
    }

    public void setAssociationTypeEnum(DocTypeAssocType docTypeAssocType) {
        setAssociationType(getValueFromEnum(docTypeAssocType));
    }

    // END: Properties
    @Override
    public AssociationToDocType clone() {
        return (AssociationToDocType) super.clone();
    }
}
