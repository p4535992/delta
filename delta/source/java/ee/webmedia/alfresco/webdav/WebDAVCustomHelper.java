package ee.webmedia.alfresco.webdav;

import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPrivilegeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVHelper;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.transaction.TransactionService;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.privilege.service.PrivilegeUtil;
import ee.webmedia.alfresco.versions.service.VersionsService;

public class WebDAVCustomHelper extends WebDAVHelper {

    // Custom services
    private final VersionsService m_versionsService;
    private final DocumentService documentService;
    private final DocumentLogService documentLogService;
    private final TransactionService transactionService;

    protected WebDAVCustomHelper(ServiceRegistry serviceRegistry, AuthenticationService authService, VersionsService versionsService, DocumentService documentService,
                                 DocumentLogService documentLogService, TransactionService transactionService) {
        super(serviceRegistry, authService);
        m_versionsService = versionsService;
        this.documentService = documentService;
        this.documentLogService = documentLogService;
        this.transactionService = transactionService;
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
            String id = pathElements.get(2);
            NodeRef nodeRef = BeanHelper.getGeneralService().getExistingNodeRefAllStores(id);
            if (nodeRef == null) {
                throw new FileNotFoundException(path);
            }
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

    public DocumentLogService getDocumentLogService() {
        return documentLogService;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public static void checkDocumentFileReadPermission(NodeRef fileRef) {
        NodeService nodeService = BeanHelper.getNodeService();
        NodeRef docRef = nodeService.getPrimaryParent(fileRef).getParentRef();
        if (!DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(docRef))) {
            if (!getUserService().isAdministrator() && !hasViewDocFilesPermission(fileRef)) {
                throw new AccessDeniedException("Not allowing reading - file is not under document and user has no permission to view files. File=" + fileRef);
            }
        } else if (!hasViewDocFilesPermission(docRef)) {
            throw new AccessDeniedException("permission " + DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES + " denied for file of document " + docRef);
        }
    }

    /**
     * @param docOrFileRef - docRef when file is under document (then dynamic permissions can be evaluated)
     *            or fileRef when file is not under document (for example under email attachments)
     * @return
     */
    private static boolean hasViewDocFilesPermission(NodeRef docOrFileRef) {
        return AccessStatus.ALLOWED == BeanHelper.getPermissionService().hasPermission(docOrFileRef, DocumentCommonModel.Privileges.VIEW_DOCUMENT_FILES);
    }

    public static void checkDocumentFileWritePermission(NodeRef fileRef) throws WebDAVServerException {
        if (getGeneralService().getArchivalsStoreRef().equals(fileRef.getStoreRef())) {
            throw new AccessDeniedException("not allowing writing - document is under primary archivals store");
        }

        if (!fileRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE)) {
            throw new AccessDeniedException("not allowing writing - document storeRef protocol is not " + StoreRef.PROTOCOL_WORKSPACE);
        }

        // check for special cases
        NodeService nodeService = BeanHelper.getNodeService();
        NodeRef parentRef = nodeService.getPrimaryParent(fileRef).getParentRef();
        if (!DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(parentRef))) {
            throw new WebDAVServerException(WebDAV.WEBDAV_SC_LOCKED, new AccessDeniedException("Not allowing writing - file is not under document. File=" + fileRef));
        }

        Boolean additionalCheck = PrivilegeUtil.additionalDocumentFileWritePermission(parentRef, nodeService);
        if (additionalCheck != null) {
            if (additionalCheck) {
                return; // allow writing based on additional logic
            }
            throw new WebDAVServerException(WebDAV.WEBDAV_SC_LOCKED, new AccessDeniedException(
                    "not allowing writing - document is finished or has in-progress workflows or is incoming letter"));
        }

        if (!getPrivilegeService().hasPermissions(parentRef, DocumentCommonModel.Privileges.EDIT_DOCUMENT)) {
            throw new WebDAVServerException(WebDAV.WEBDAV_SC_LOCKED, new AccessDeniedException("permission editDocument denied for file under " + parentRef));
        }
    }

}
