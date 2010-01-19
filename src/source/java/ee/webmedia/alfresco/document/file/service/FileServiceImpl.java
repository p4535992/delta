package ee.webmedia.alfresco.document.file.service;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.InMemoryTicketComponentImpl;
import org.alfresco.repo.webdav.WebDAVServlet;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.URLEncoder;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.versions.service.VersionsService;

/**
 * @author Dmitri Melnikov
 */
public class FileServiceImpl implements FileService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(FileServiceImpl.class);

    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private UserService userService;
    private SignatureService signatureService;
    private AuthenticationService authenticationService;
    private PermissionService permissionService;

    @Override
    public List<File> getAllFilesExcludingDigidocSubitems(NodeRef nodeRef) {
        return getAllFiles(nodeRef, false);
    }
    
    public List<File> getAllFiles(NodeRef nodeRef) {
        return getAllFiles(nodeRef, true);
    }
    
    public List<File> getAllFiles(NodeRef nodeRef, boolean includeDigidocSubitems) {
        List<File> files = new ArrayList<File>();
        List<FileInfo> fileInfos = fileFolderService.listFiles(nodeRef);
        for (FileInfo fi : fileInfos) {
            File item = new File(fi);
            item.setCreator(userService.getUserFullName((String) fi.getProperties().get(ContentModel.PROP_CREATOR)));
            item.setModifier(userService.getUserFullName((String) fi.getProperties().get(ContentModel.PROP_MODIFIER)));
            item.setDownloadUrl(generateURL(item.getNodeRef()));
            files.add(item);
            boolean isDdoc = signatureService.isDigiDocContainer(item.getNodeRef());
            item.setDigiDocContainer(isDdoc);
            if (isDdoc && includeDigidocSubitems && permissionService.hasPermission(item.getNodeRef(), PermissionService.READ_CONTENT).equals(AccessStatus.ALLOWED)) {
                // hack: add another File to display nested tables in JSP.
                // this "item2" should be exactly after the "item" in the list
                File item2 = new File();
                item2.setDdocItems(signatureService.getDataItemsAndSignatureItems(item.getNodeRef(), false));
                item2.setDigiDocItem(true);
                files.add(item2);
            }
        }
        return files;
    }

    @Override
    public String generateURL(NodeRef nodeRef) {
        // calculate a WebDAV URL for the given node
        StringBuilder path = new StringBuilder("/").append(WebDAVServlet.WEBDAV_PREFIX);

        // authentication ticket
        String ticket = authenticationService.getCurrentTicket();
        if (ticket.startsWith(InMemoryTicketComponentImpl.GRANTED_AUTHORITY_TICKET_PREFIX)) {
            ticket = ticket.substring(InMemoryTicketComponentImpl.GRANTED_AUTHORITY_TICKET_PREFIX.length());
        }
        path.append("/").append(URLEncoder.encode(ticket));

        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        path.append("/").append(URLEncoder.encode(parent.getId()));

        String name = fileFolderService.getFileInfo(nodeRef).getName();
        path.append("/").append(URLEncoder.encode(name));

        return path.toString();
    }

    // START: getters / setters
    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setSignatureService(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
    // END: getters / setters
}
