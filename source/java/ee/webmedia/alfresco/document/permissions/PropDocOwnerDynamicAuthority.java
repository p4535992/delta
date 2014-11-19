package ee.webmedia.alfresco.document.permissions;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

<<<<<<< HEAD
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * Checks if {@link DocumentCommonModel.Props#OWNER_ID} of given docRef refers to current user
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class PropDocOwnerDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PropDocOwnerDynamicAuthority.class);

    public static final String PROP_DOC_OWNER_AUTHORITY = "ROLE_PROP_DOC_OWNER";

    @Override
    public boolean hasAuthority(final NodeRef docRef, final String userName) {
        QName type = nodeService.getType(docRef);
<<<<<<< HEAD
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT) && !dictionaryService.isSubClass(type, CaseFileModel.Types.CASE_FILE)) {
            log.trace("Node is not of type 'doccom:document' or 'cf:caseFile': type=" + type + ", refusing authority " + getAuthority());
=======
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT)) {
            log.trace("Node is not of type 'doccom:document': type=" + type + ", refusing authority " + getAuthority());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            return false;
        }
        String ownerId = (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.OWNER_ID);
        if (StringUtils.equals(ownerId, userName)) {
<<<<<<< HEAD
            log.debug("User " + userName + " matches document or case file ownerId " + ownerId + ", granting authority " + getAuthority());
=======
            log.debug("User " + userName + " matches document ownerId " + ownerId + ", granting authority " + getAuthority());
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            return true;
        }
        return false;
    }

    @Override
    public String getAuthority() {
        return PROP_DOC_OWNER_AUTHORITY;
    }

}
