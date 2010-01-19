package ee.webmedia.alfresco.document.permissions;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class DocumentOwnerDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentOwnerDynamicAuthority.class);

    public static final String DOCUMENT_OWNER_AUTHORITY = "ROLE_DOCUMENT_OWNER";

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT)) {
            log.debug("Node is not of type 'doccom:document': type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        if (isDocumentManager()) {
            log.debug("User " + userName + " is a document manager on node, type=" + type + ", granting authority " + getAuthority());
            return true;
        }
        if (nodeService.hasAspect(nodeRef, DocumentCommonModel.Aspects.OWNER)) {
            String ownerId = (String) nodeService.getProperty(nodeRef, DocumentCommonModel.Props.OWNER_ID);
            boolean hasAuthority = EqualsHelper.nullSafeEquals(ownerId, userName);
            log.debug("User " + userName + " has " + getAuthority() + " on document which has ownerId " + ownerId + " - "
                    + Boolean.toString(hasAuthority).toUpperCase());
            return hasAuthority;
        }
        log.warn("Document does not have " + DocumentCommonModel.Aspects.OWNER + " aspect, type=" + type + " nodeRef=" + nodeRef + ", refusing authority"
                + getAuthority());
        return false;
    }

    @Override
    public String getAuthority() {
        return DOCUMENT_OWNER_AUTHORITY;
    }

}
