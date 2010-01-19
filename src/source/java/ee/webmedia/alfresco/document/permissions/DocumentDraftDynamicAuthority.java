package ee.webmedia.alfresco.document.permissions;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class DocumentDraftDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentDraftDynamicAuthority.class);

    public static final String DOCUMENT_DRAFT_AUTHORITY = "ROLE_DOCUMENT_DRAFT";

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT)) {
            log.debug("Node is not of type 'doccom:document': type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        if (parent == null) {
            log.warn("Document does not have a primary parent, type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        QName parentType = nodeService.getType(parent);
        if (dictionaryService.isSubClass(parentType, DocumentCommonModel.Types.DRAFTS)) {
            log.debug("Document parent is drafts folder, type=" + parentType + ", granting authority " + getAuthority());
            return true;
        }
        log.debug("Document parent is not drafts folder, type=" + type + ", refusing authority " + getAuthority());
        return false;
    }

    @Override
    public String getAuthority() {
        return DOCUMENT_DRAFT_AUTHORITY;
    }

}
