package ee.webmedia.alfresco.document.permissions;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class DocumentOwnerFileDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentOwnerFileDynamicAuthority.class);

    public static final String DOCUMENT_OWNER_FILE_AUTHORITY = "ROLE_DOCUMENT_OWNER_FILE";

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (!dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT)) {
            log.debug("Node is not of type 'cm:content', type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        if (parent == null) {
            log.debug("File does not have a primary parent, type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        QName parentType = nodeService.getType(parent);
        if (!dictionaryService.isSubClass(parentType, DocumentCommonModel.Types.DOCUMENT)) {
            log.debug("Node is not of type 'doccom:document', type=" + parentType + ", refusing authority " + getAuthority());
            return false;
        }
        if (isDocumentManager()) {
            log.debug("User " + userName + " is a document manager on node, type=" + type + ", granting authority " + getAuthority());
            return true;
        }
        if (nodeService.hasAspect(parent, DocumentCommonModel.Aspects.OWNER)) {
            String ownerId = (String) nodeService.getProperty(parent, DocumentCommonModel.Props.OWNER_ID);
            boolean hasAuthority = EqualsHelper.nullSafeEquals(ownerId, userName);
            log.debug("User " + userName + " has " + getAuthority() + " on parent document which has ownerId " + ownerId + " - "
                    + Boolean.toString(hasAuthority).toUpperCase());
            return hasAuthority;
        }
        log.warn("Document does not have " + DocumentCommonModel.Aspects.OWNER + " aspect: type=" + parentType + " nodeRef=" + parent + ", refusing authority"
                + getAuthority());
        return false;
    }

    @Override
    public String getAuthority() {
        return DOCUMENT_OWNER_FILE_AUTHORITY;
    }

}
