package ee.webmedia.alfresco.webdav;

import java.util.Set;

import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Implements the WebDAV GET method, checks if {@link DocumentCommonModel.Privileges#VIEW_DOCUMENT_FILES} is granted to the document of the file
 * 
 * @author Ats Uiboupin
 */
public class GetMethod extends org.alfresco.repo.webdav.GetMethod {

    @Override
    protected void checkPreConditions(FileInfo nodeInfo) throws WebDAVServerException {
        NodeRef fileRef = nodeInfo.getNodeRef();
        NodeRef docRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(fileRef, DocumentCommonModel.Types.DOCUMENT, true);
        if (docRef == null && !isAdmin()) {
            if (!hasViewDocFilesPermission(fileRef)) {
                throw new AccessDeniedException("Not allowing reading - file is not under document and user has no permission to view files. File=" + fileRef);
            }
        } else if (!hasViewDocFilesPermission(docRef)) {
            throw new AccessDeniedException("permission " + DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES + " denied for file of document " + docRef);
        }
        super.checkPreConditions(nodeInfo);
    }

    /**
     * @param docOrFileRef - docRef when file is under document (then dynamic permissions can be evaluated)
     *            or fileRef when file is not under document (for example under email attachments)
     * @return
     */
    private boolean hasViewDocFilesPermission(NodeRef docOrFileRef) {
        return AccessStatus.ALLOWED == BeanHelper.getPermissionService().hasPermission(docOrFileRef, DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES);
    }

    private boolean isAdmin() {
        final Set<String> authorities = BeanHelper.getAuthorityService().getAuthorities();
        return authorities.contains(UserService.AUTH_ADMINISTRATORS_GROUP) || authorities.contains(PermissionService.ADMINISTRATOR_AUTHORITY);
    }
}
