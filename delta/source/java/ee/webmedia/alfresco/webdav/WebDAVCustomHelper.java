package ee.webmedia.alfresco.webdav;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.webdav.WebDAVHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Repository;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.permissions.DocumentFileWriteDynamicAuthority;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.versions.service.VersionsService;

public class WebDAVCustomHelper extends WebDAVHelper {

    // Custom services
    private final VersionsService m_versionsService;
    private final DocumentService documentService;

    protected WebDAVCustomHelper(ServiceRegistry serviceRegistry, AuthenticationService authService, VersionsService versionsService, DocumentService documentService) {
        super(serviceRegistry, authService);
        m_versionsService = versionsService;
        this.documentService = documentService;
    }

    /**
     * Get the file info for the given paths
     * 
     * @param rootNodeRef the acting webdav root
     * @param path the path to search for
     * @param servletPath the base servlet path, which may be null or empty
     * @return Return the file info for the path
     * @throws FileNotFoundException
     *             if the path doesn't refer to a valid node
     */
    @Override
    public FileInfo getNodeForPath(NodeRef rootNodeRef, String path, String servletPath) throws FileNotFoundException {
        if (path == null) {
            throw new IllegalArgumentException("Path may not be null");
        }

        List<String> pathElements = splitAllPaths(path);
        if (pathElements.isEmpty()) {
            throw new FileNotFoundException(path);
        }

        try {
            NodeRef nodeRef = new NodeRef(Repository.getStoreRef(), pathElements.get(2));
            boolean subContent = false;
            if (pathElements.size() > 3) {
                nodeRef = getNodeService().getChildByName(nodeRef, ContentModel.ASSOC_CONTAINS, pathElements.get(3));
                subContent = true;
            }
            FileInfo fi = getFileFolderService().getFileInfo(nodeRef);
            // Browsing subfolders not allowed
            if (subContent && fi.isFolder()) {
                throw new FileNotFoundException(nodeRef);
            }
            return fi;
        } catch (IllegalArgumentException e) {
            throw new FileNotFoundException(path);
        } catch (InvalidNodeRefException e) {
            throw new FileNotFoundException(path);
        }
    }

    /**
     * @return Return custom versions service
     */
    public VersionsService getVersionsService() {
        return m_versionsService;
    }

    public DocumentService getDocumentService() {
        return documentService;
    }

    public static void checkDocumentFileWritePermission(FileInfo nodeInfo) {
        // check for special cases
        NodeRef fileRef = nodeInfo.getNodeRef();
        NodeService nodeService = BeanHelper.getNodeService();
        NodeRef parentRef = nodeService.getPrimaryParent(fileRef).getParentRef();
        QName parentType = nodeService.getType(parentRef);
        String userName = AuthenticationUtil.getRunAsUser();
        DocumentFileWriteDynamicAuthority documentFileWriteDynamicAuthority = BeanHelper.getDocumentFileWriteDynamicAuthority();
        if (documentFileWriteDynamicAuthority.isAllowedForFileNotUnderDocument(userName, parentRef, parentType)) {
            return; // file is not under document and some special conditions hold, so let's allow
        }
        NodeRef docRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(fileRef, DocumentCommonModel.Types.DOCUMENT, true);
        if (docRef == null) {
            throw new AccessDeniedException("File is not under document. File=" + fileRef);
        }
        Boolean additionalCheck = documentFileWriteDynamicAuthority.additional(userName, docRef);
        if (additionalCheck != null) {
            if (additionalCheck) {
                return; // allow writing based on additional logic
            }
            throw new AccessDeniedException("not allowing writing - document is finished or has in-progress workflows");
        }

        // no special cases, just check permission on document
        String permission = DocumentCommonModel.Privileges.EDIT_DOCUMENT_FILES;
        if (AccessStatus.ALLOWED != BeanHelper.getPermissionService().hasPermission(docRef, permission)) {
            throw new AccessDeniedException("permission " + permission + " denied for file of document " + docRef);
        }
    }

}
