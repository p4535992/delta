package ee.webmedia.alfresco.document.permissions;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Checks if {@link DocumentCommonModel.Props#OWNER_ID} of given docRef refers to current user
 * 
 * @author Ats Uiboupin
 */
public class PropDocOwnerDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PropDocOwnerDynamicAuthority.class);

    public static final String PROP_DOC_OWNER_AUTHORITY = "ROLE_PROP_DOC_OWNER";

    @Override
    public boolean hasAuthority(final NodeRef docRef, final String userName) {
        QName type = nodeService.getType(docRef);
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT)) {
            log.trace("Node is not of type 'doccom:document': type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        String ownerId = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.OWNER_ID);
        if (StringUtils.equals(ownerId, userName)) {
            log.debug("User " + userName + " matches document ownerId " + ownerId + ", granting authority " + getAuthority());
            return true;
        }
        return false;
    }

    @Override
    public String getAuthority() {
        return PROP_DOC_OWNER_AUTHORITY;
    }

}
