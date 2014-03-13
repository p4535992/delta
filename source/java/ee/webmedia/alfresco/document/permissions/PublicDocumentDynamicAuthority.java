package ee.webmedia.alfresco.document.permissions;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Checks if document is public based on {@link DocumentCommonModel.Props#ACCESS_RESTRICTION}
 */
public class PublicDocumentDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PublicDocumentDynamicAuthority.class);

    public static final String ROLE_PUBLIC_DOCUMENT_AUTHORITY = "ROLE_PUBLIC_DOCUMENT";

    @Override
    public boolean hasAuthority(final NodeRef docRef, final String userName) {
        QName type = nodeService.getType(docRef);
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT)) {
            log.trace("Node is not of type 'doccom:document': type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        String accessRestriction = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.ACCESS_RESTRICTION);
        if (StringUtils.equals(accessRestriction, AccessRestriction.OPEN.getValueName())) {
            log.debug("Document " + docRef + " is public, granting authority " + getAuthority());
            return true;
        }
        return false;
    }

    @Override
    public String getAuthority() {
        return ROLE_PUBLIC_DOCUMENT_AUTHORITY;
    }

}
