package ee.webmedia.alfresco.document.permissions;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.AccessRestriction;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
<<<<<<< HEAD

/**
 * Checks if document is public based on {@link DocumentCommonModel.Props#ACCESS_RESTRICTION}
 * 
 * @author Ats Uiboupin
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
=======
import ee.webmedia.alfresco.privilege.service.DynamicAuthority;

/**
 * Checks if document is public based on {@link DocumentCommonModel.Props#ACCESS_RESTRICTION}
 */
public class PublicDocumentDynamicAuthority extends DynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PublicDocumentDynamicAuthority.class);

    @Override
    public boolean hasAuthority(final NodeRef docRef, QName type, final String userName) {
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT)) {
            log.trace("Node is not of type 'doccom:document': type=" + type + ", refusing permissions " + getGrantedPrivileges());
            return false;
        }
        String accessRestriction = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.ACCESS_RESTRICTION);
        if (isPublicAccessRestriction(accessRestriction)) {
            log.debug("Document " + docRef + " is public, granting permissions " + getGrantedPrivileges());
>>>>>>> develop-5.1
            return true;
        }
        return false;
    }

<<<<<<< HEAD
    @Override
    public String getAuthority() {
        return ROLE_PUBLIC_DOCUMENT_AUTHORITY;
=======
    public static boolean isPublicAccessRestriction(String accessRestriction) {
        return StringUtils.equals(accessRestriction, AccessRestriction.OPEN.getValueName());
>>>>>>> develop-5.1
    }

}
