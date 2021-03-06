package ee.webmedia.alfresco.docadmin.service;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Object that models allowed association between two (document) nodes
 */
public abstract class AssociationModel extends BaseObject {
    private static final long serialVersionUID = 1L;

    private static final Map<String, QName> DOC_TYPE_TO_ASSOC_QNAME = new HashMap<>();

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection (when loading ancestor object) */
    public AssociationModel(BaseObject parent, WmNode node) {
        super(parent, node);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection (when loading this object) */
    public AssociationModel(NodeRef parentRef, WmNode node) {
        super(parentRef, node);
    }

    public AssociationModel(DocumentType docType, QName type) {
        super(docType, type);
    }

    @Override
    public DocumentType getParent() {
        return (DocumentType) super.getParent();
    }

    @Override
    public NodeRef getParentNodeRef() {
        return super.getParentNodeRef();
    }

    @Override
    protected QName getAssocName() {
        return RepoUtil.getFromQNamePool(getDocType(), DocumentAdminModel.URI, DOC_TYPE_TO_ASSOC_QNAME);
    }

    public void nextSaveToParent(DocumentType documentType) {
        nextSaveToParent(documentType, FollowupAssociation.class);
    }

    public abstract DocTypeAssocType getAssociationType();

    public ChildrenList<FieldMapping> getFieldMappings() {
        return getChildren(FieldMapping.class);
    }

    // START: Properties
    public final String getDocType() {
        return getProp(DocumentAdminModel.Props.DOC_TYPE);
    }

    public final boolean getAssociateWithSourceDocument() {
        return getPropBoolean(DocumentAdminModel.Props.ASSOCIATE_WITH_SOURCE_DOCUMENT_WORKFLOW);
    }

    public final void setDocType(String docType) {
        setProp(DocumentAdminModel.Props.DOC_TYPE, docType);
    }

    // END: Properties
    @Override
    public AssociationModel clone() {
        return (AssociationModel) super.clone();
    }
}
