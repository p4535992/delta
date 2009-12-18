package ee.webmedia.alfresco.webdav;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.webdav.WebDAVHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.web.bean.repository.Repository;

public class WebDAVCustomHelper extends WebDAVHelper {

    protected WebDAVCustomHelper(ServiceRegistry serviceRegistry, AuthenticationService authService) {
        super(serviceRegistry, authService);
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
            NodeRef nodeRef = new NodeRef(Repository.getStoreRef(), pathElements.get(1));
            boolean subContent = false;
            if (pathElements.size() > 2) {
                nodeRef = getNodeService().getChildByName(nodeRef, ContentModel.ASSOC_CONTAINS, pathElements.get(2));
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

}
