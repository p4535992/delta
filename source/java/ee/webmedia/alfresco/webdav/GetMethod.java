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
 */
public class GetMethod extends org.alfresco.repo.webdav.GetMethod {

    @Override
    protected void checkPreConditions(FileInfo nodeInfo) throws WebDAVServerException {
        NodeRef fileRef = nodeInfo.getNodeRef();
        NodeRef docRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(fileRef, DocumentCommonModel.Types.DOCUMENT, true);
        if (docRef == null && !isAdminOrDocumentManager()) {
            throw new AccessDeniedException("Not allowing reading - file is not under document and user is not documentManager. File=" + fileRef);
        }
        String permission = DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES;
        if (AccessStatus.ALLOWED != BeanHelper.getPermissionService().hasPermission(docRef, permission)) {
            throw new AccessDeniedException("permission " + permission + " denied for file of document " + docRef);
        }
        super.checkPreConditions(nodeInfo);
    }

    protected boolean isAdminOrDocumentManager() {
        final Set<String> authorities = BeanHelper.getAuthorityService().getAuthorities();
        return authorities.contains(UserService.AUTH_ADMINISTRATORS_GROUP) || authorities.contains(PermissionService.ADMINISTRATOR_AUTHORITY);
    }
}
