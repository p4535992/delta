package ee.webmedia.alfresco.document.permissions;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
<<<<<<< HEAD

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
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT) && !dictionaryService.isSubClass(type, CaseFileModel.Types.CASE_FILE)) {
            log.trace("Node is not of type 'doccom:document' or 'cf:caseFile': type=" + type + ", refusing authority " + getAuthority());
=======
import ee.webmedia.alfresco.privilege.service.DynamicAuthority;

/**
 * Checks if {@link DocumentCommonModel.Props#OWNER_ID} of given docRef refers to current user
 */
public class PropDocOwnerDynamicAuthority extends DynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PropDocOwnerDynamicAuthority.class);

    @Override
    public boolean hasAuthority(final NodeRef docRef, QName type, final String userName) {
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT) && !dictionaryService.isSubClass(type, CaseFileModel.Types.CASE_FILE)) {
            log.trace("Node is not of type 'doccom:document' or cf:caseFile: type=" + type + ", refusing permissions: " + getGrantedPrivileges());
>>>>>>> develop-5.1
            return false;
        }
        String ownerId = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.OWNER_ID);
        if (StringUtils.equals(ownerId, userName)) {
<<<<<<< HEAD
            log.debug("User " + userName + " matches document or case file ownerId " + ownerId + ", granting authority " + getAuthority());
=======
            log.debug("User " + userName + " matches document or case_file ownerId " + ownerId + ", granting permissions " + getGrantedPrivileges());
>>>>>>> develop-5.1
            return true;
        }
        return false;
    }

<<<<<<< HEAD
    @Override
    public String getAuthority() {
        return PROP_DOC_OWNER_AUTHORITY;
    }

=======
>>>>>>> develop-5.1
}
