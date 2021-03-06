package ee.webmedia.alfresco.docadmin.web;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.docadmin.service.FollowupAssociation;

/**
 * Bean for showing followup associations list
 */
public class FollowupAssocsListBean extends DocTypeAssocsListBean<FollowupAssociation> {
    private static final long serialVersionUID = 1L;

    @Override
    protected FollowupAssociation getAssocByNodeRef(NodeRef assocNodeRef) {
        return docTypeDetailsDialog.getDocType().getFollowupAssociations().getChildByNodeRef(assocNodeRef);
    }

    @Override
    public List<FollowupAssociation> getAssocs() {
        return getAssocsOfType(DocTypeAssocType.FOLLOWUP);
    }

    @Override
    protected FollowupAssociation createNewAssoc() {
        return new FollowupAssociation(docTypeDetailsDialog.getDocType());
    }

}
