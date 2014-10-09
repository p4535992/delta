package ee.webmedia.alfresco.document.permissions;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.privilege.service.DynamicAuthority;

/**
 * Checks if {@link DocumentCommonModel.Props#OWNER_ID} of given docRef refers to current user
 */
public class PropDocOwnerDynamicAuthority extends DynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(PropDocOwnerDynamicAuthority.class);

    @Override
    public boolean hasAuthority(final NodeRef docRef, QName type, final String userName, Map<String, Object> properties) {
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type) && !CaseFileModel.Types.CASE_FILE.equals(type)
                && !dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT) && !dictionaryService.isSubClass(type, CaseFileModel.Types.CASE_FILE)) {
            log.trace("Node is not of type 'doccom:document' or cf:caseFile: type=" + type + ", refusing permissions: " + getGrantedPrivileges());
            return false;
        }
        String ownerId = MapUtils.isNotEmpty(properties) ? (String) properties.get(DocumentCommonModel.Props.OWNER_ID.toString()) : (String) nodeService.getProperty(docRef,
                DocumentCommonModel.Props.OWNER_ID);
        if (StringUtils.equals(ownerId, userName)) {
            log.debug("User " + userName + " matches document or case_file ownerId " + ownerId + ", granting permissions " + getGrantedPrivileges());
            return true;
        }
        return false;
    }

}
