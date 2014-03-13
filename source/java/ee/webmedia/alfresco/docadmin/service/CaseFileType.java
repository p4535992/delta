package ee.webmedia.alfresco.docadmin.service;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;

/**
 * Dynamic caseFile type
 */
public class CaseFileType extends DynamicType {
    private static final long serialVersionUID = 1L;

    public CaseFileType(NodeRef parentRef) {
        super(parentRef, DocumentAdminModel.Types.CASE_FILE_TYPE);
    }

    /** Used by {@link BaseServiceImpl#getObject(NodeRef, Class)} through reflection */
    public CaseFileType(NodeRef parentNodeRef, WmNode docTypeNode) {
        super(parentNodeRef, docTypeNode);
    }

    // ChildrenList

    public List<? extends AssociationModel> getAssociationModels(DocTypeAssocType associationTypeEnum) {
        if (associationTypeEnum == null) {
            List<AssociationModel> allAssocsToDocType = new ArrayList<AssociationModel>();
            allAssocsToDocType.addAll(getFollowupAssociations());
            allAssocsToDocType.addAll(getReplyAssociations());
            return allAssocsToDocType;
        }
        return DocTypeAssocType.FOLLOWUP == associationTypeEnum ? getFollowupAssociations() : getReplyAssociations();
    }

    public ChildrenList<FollowupAssociation> getFollowupAssociations() {
        return getChildren(FollowupAssociation.class);
    }

    public ChildrenList<ReplyAssociation> getReplyAssociations() {
        return getChildren(ReplyAssociation.class);
    }

    // Properties

    @Override
    public CaseFileType clone() {
        return (CaseFileType) super.clone(); // just return casted type
    }

}
