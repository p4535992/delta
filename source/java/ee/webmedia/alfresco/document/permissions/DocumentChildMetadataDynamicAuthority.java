package ee.webmedia.alfresco.document.permissions;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class DocumentChildMetadataDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentChildMetadataDynamicAuthority.class);

    public static final String DOCUMENT_CHILD_METADATA_AUTHORITY = "ROLE_DOCUMENT_CHILD_METADATA";

    public DocumentChildMetadataDynamicAuthority() {
    }

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.METADATA_CONTAINER)) {
            log.trace("Node is not a document metadata child node, nodeRef=" + nodeRef + "(" + type + "), refusing authority " + getAuthority());
            return false;
        }
        final NodeRef parentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        final QName parentType = nodeService.getType(parentRef);
        if (dictionaryService.isSubClass(parentType, DocumentCommonModel.Types.DOCUMENT)) {
            final boolean hasAuth = permissionServiceImpl.hasPermission(parentRef, "DocumentWrite").equals(AccessStatus.ALLOWED);
            if (!hasAuth && log.isTraceEnabled()) {
                log.trace("nodeRef=" + nodeRef + "(" + type + ") hasAuth?" + hasAuth + " through parent parentRef=" + parentRef + "(" + parentType + ")");
            }
            return hasAuth;
        } else if (dictionaryService.isSubClass(parentType, DocumentCommonModel.Types.METADATA_CONTAINER)) {
            final boolean hasAuth = permissionServiceImpl.hasPermission(parentRef, "DocumentChildMetadata").equals(AccessStatus.ALLOWED);
            if (!hasAuth && log.isTraceEnabled()) {
                log.trace("nodeRef=" + nodeRef + "(" + type + ") hasAuth?" + hasAuth + " through parent parentRef=" + parentRef + "(" + parentType + ")");
            }
            return hasAuth;
        }
        log.warn("Node parent is not a document nor a metadata child node, type="
                + parentType + " nodeRef=" + parentRef + ", refusing authority " + getAuthority());
        return false;
    }

    @Override
    public String getAuthority() {
        return DOCUMENT_CHILD_METADATA_AUTHORITY;
    }

}
