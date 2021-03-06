package ee.webmedia.alfresco.docadmin.web;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.docadmin.service.ReplyAssociation;

/**
 * Bean for showing reply associations list
 */
public class ReplyAssocsListBean extends DocTypeAssocsListBean<ReplyAssociation> {
    private static final long serialVersionUID = 1L;

    @Override
    protected ReplyAssociation getAssocByNodeRef(NodeRef assocNodeRef) {
        return docTypeDetailsDialog.getDocType().getReplyAssociations().getChildByNodeRef(assocNodeRef);
    }

    @Override
    public List<ReplyAssociation> getAssocs() {
        return getAssocsOfType(DocTypeAssocType.REPLY);
    }

    @Override
    protected ReplyAssociation createNewAssoc() {
        return new ReplyAssociation(docTypeDetailsDialog.getDocType());
    }
}
