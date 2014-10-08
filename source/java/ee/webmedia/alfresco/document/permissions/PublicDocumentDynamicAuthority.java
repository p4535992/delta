package ee.webmedia.alfresco.document.permissions;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.service.DynamicAuthority;

/**
 * Checks if document is public based on {@link DocumentCommonModel.Props#ACCESS_RESTRICTION}
 */
public class PublicDocumentDynamicAuthority extends DynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PublicDocumentDynamicAuthority.class);

    @Override
    public boolean hasAuthority(final NodeRef docRef, QName type, final String userName, Map<String, Object> properties) {
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT)) {
            log.trace("Node is not of type 'doccom:document': type=" + type + ", refusing permissions " + getGrantedPrivileges());
            return false;
        }
        String accessRestriction = MapUtils.isNotEmpty(properties) ? (String) properties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString())
                : (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.ACCESS_RESTRICTION);
        if (isPublicAccessRestriction(accessRestriction)) {
            log.debug("Document " + docRef + " is public, granting permissions " + getGrantedPrivileges());
            return true;
        }
        return false;
    }

    public static boolean isPublicAccessRestriction(String accessRestriction) {
        return StringUtils.equals(accessRestriction, AccessRestriction.OPEN.getValueName());
    }

}
