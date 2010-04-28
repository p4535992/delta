package ee.webmedia.alfresco.document.file.service;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.security.authentication.InMemoryTicketComponentImpl;
import org.alfresco.repo.webdav.WebDAVServlet;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.URLEncoder;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.user.service.UserService;

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
    private ContentService contentService;
    private GeneralService generalService;
    private DocumentLogService documentLogService; 

    private String scannedFilesPath;

    @Override
    public void toggleActive(NodeRef nodeRef) {
        boolean active = true; // If file doesn't have the flag set, then it hasn't been toggled yet, thus active
        if(nodeService.getProperty(nodeRef, File.ACTIVE) != null) {
            active = Boolean.parseBoolean(nodeService.getProperty(nodeRef, File.ACTIVE).toString());
        }
        nodeService.setProperty(nodeRef, File.ACTIVE, !active);
    }

    @Override
    public List<File> getAllFilesExcludingDigidocSubitems(NodeRef nodeRef) {
        return getAllFiles(nodeRef, false, false);
    }
    
    @Override
    public List<File> getAllFiles(NodeRef nodeRef) {
        return getAllFiles(nodeRef, true, false);
    }
    
    private List<File> getAllFiles(NodeRef nodeRef, boolean includeDigidocSubitems, boolean onlyActive) {
        List<File> files = new ArrayList<File>();
        List<FileInfo> fileInfos = fileFolderService.listFiles(nodeRef);
        for (FileInfo fi : fileInfos) {
            File item = createFile(fi);
            boolean isDdoc = signatureService.isDigiDocContainer(item.getNodeRef());
            item.setDigiDocContainer(isDdoc);
            item.setTransformableToPdf(isTransformableToPdf(fi.getContentData()));
            if (item.isActive() || !onlyActive) {
                files.add(item);
            }
            if (isDdoc && includeDigidocSubitems && permissionService.hasPermission(item.getNodeRef(), PermissionService.READ_CONTENT).equals(AccessStatus.ALLOWED)) {
                // hack: add another File to display nested tables in JSP.
                // this "item2" should be exactly after the "item" in the list
                try {
                    File item2 = new File();
                    item2.setDdocItems(signatureService.getDataItemsAndSignatureItems(item.getNodeRef(), false));
                    item2.setDigiDocItem(true);
                    item2.setActive(item.isActive());
                    if (item.isActive() || !onlyActive) {
                        files.add(item2);
                    }
                } catch (SignatureException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Unable to parse DigiDoc file '" + item.getName() + "':\n" + e.getMessage());
                    }
                }
            }
        }
        return files;
    }
    
    @Override
    public List<File> getAllActiveFiles(NodeRef nodeRef) {
        return getAllFiles(nodeRef, false, true);
    }

    @Override
    public List<NodeRef> getAllActiveFilesNodeRefs(NodeRef nodeRef) {
        List<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        for (File file : getAllActiveFiles(nodeRef)) {  // TODO not optimal
            nodeRefs.add(file.getNodeRef());
        }
        return nodeRefs;
    }

    @Override
    public void setAllFilesInactiveExcept(NodeRef parent, NodeRef activeFile) {
        for (File file : getAllActiveFiles(parent)) {
            if (!file.getNodeRef().equals(activeFile)) {
                toggleActive(file.getNodeRef());
            }
        }
    }

    @Override
    public void transformActiveFilesToPdf(NodeRef document) {
        List<File> files = getAllActiveFiles(document);
        for (File file : files) {
            if (generatePdf(document, file.getNodeRef()) != null) {
                toggleActive(file.getNodeRef());
            }
        }
    }

    private boolean isTransformableToPdf(ContentData contentData) {
        if (contentData == null) {
            return false;
        }
        if (MimetypeMap.MIMETYPE_PDF.equals(contentData.getMimetype())) {
            return false;
        }
        ContentTransformer transformer = contentService.getTransformer(contentData.getMimetype(), MimetypeMap.MIMETYPE_PDF);
        return transformer != null;
    }

    @Override
    public void transformToPdf(NodeRef file) {
        generatePdf(nodeService.getPrimaryParent(file).getParentRef(), file);
    }

    private FileInfo generatePdf(NodeRef parent, NodeRef file) {
        ContentReader reader = fileFolderService.getReader(file);
        if (MimetypeMap.MIMETYPE_PDF.equals(reader.getMimetype())) {
            return null;
        }
        String filename = (String) nodeService.getProperty(file, ContentModel.PROP_NAME);
        return transformToPdf(parent, reader, filename);
    }

    @Override
    public FileInfo transformToPdf(NodeRef parent, ContentReader reader, String filename) {
        ContentWriter writer = contentService.getWriter(null, null, false);
        writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
        ContentTransformer transformer = contentService.getTransformer(reader.getMimetype(), writer.getMimetype());
        if (transformer == null) {
            log.debug("No transformation available from '" + reader.getMimetype() + "' to '" + writer.getMimetype() + "', skipping PDF file creation");
            return null;
        }
        try {
            long startTime = System.currentTimeMillis();
            transformer.transform(reader, writer);
            if (log.isDebugEnabled()) {
                log.debug("Transformed file to PDF, time " + (System.currentTimeMillis() - startTime) + " ms, filename=" + filename + ", size="
                        + reader.getSize() + " bytes, mimeType=" + reader.getMimetype() + ", encoding=" + reader.getEncoding() + ", PDF size=" + writer.getSize() + " bytes");
            }
        } catch (ContentIOException e) {
            log.debug("Failed to transform file to PDF, filename=" + filename + ", size=" + reader.getSize() + ", mimeType=" + reader.getMimetype()
                    + ", encoding=" + reader.getEncoding(), e);
            return null;
        }

        FileInfo createdFile = fileFolderService.create(
                parent,
                generalService.getUniqueFileName(parent, FilenameUtils.removeExtension(filename) + ".pdf"),
                ContentModel.TYPE_CONTENT);
        nodeService.setProperty(createdFile.getNodeRef(), ContentModel.PROP_CONTENT, writer.getContentData());
        return createdFile;
    }

    @Override
    public void moveAllFiles(NodeRef fromRef, NodeRef toRef) throws FileNotFoundException {
        List<FileInfo> fileInfos = fileFolderService.listFiles(fromRef);
        for (FileInfo fileInfo : fileInfos) {
            try {
                fileFolderService.move(fileInfo.getNodeRef(), toRef, null);
                documentLogService.addDocumentLog(toRef, I18NUtil.getMessage("document_log_status_fileAdded", fileInfo.getName()));
            } catch (DuplicateChildNodeNameException e) {
                log.warn("Move failed. File '" + fileInfo.getName() + "' already exists");
                throw e;
            } catch (FileExistsException e) {
                log.warn("Move failed. File '" + fileInfo.getName() + "' already exists");
                throw e; 
            } catch (FileNotFoundException e) {
                log.warn("Move failed. File '" + fileInfo.getName() + "' not found");
                throw e; 
            }
        }
    }


    @Override
    public NodeRef addFileToDocument(String name, NodeRef fileNodeRef, NodeRef documentNodeRef) {
        // change file name
        nodeService.setProperty(fileNodeRef, ContentModel.PROP_NAME, name);
        // move file
        return nodeService.moveNode(fileNodeRef, documentNodeRef,
                ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS).getChildRef();
    }

    @Override
    public List<File> getScannedFiles() {
        if (log.isDebugEnabled()) log.debug("Getting scanned files");
        NodeRef scannedFilesNodeRef  = generalService.getNodeRef(scannedFilesPath);
        Assert.notNull(scannedFilesNodeRef, "Scanned files node reference not found");
        return getAllFilesExcludingDigidocSubitems(scannedFilesNodeRef);
    }

    private File createFile(FileInfo fi) {
        File item = new File(fi);
        item.setCreator(userService.getUserFullName((String) fi.getProperties().get(ContentModel.PROP_CREATOR)));
        item.setModifier(userService.getUserFullName((String) fi.getProperties().get(ContentModel.PROP_MODIFIER)));
        item.setDownloadUrl(generateURL(item.getNodeRef()));
        return item;
    }

    public File getFile(NodeRef nodeRef) {
        Assert.notNull(nodeRef);

        FileInfo fi = fileFolderService.getFileInfo(nodeRef);
        Assert.notNull(fi);
        return createFile(fi);
    }

    @Override
    public String generateURL(NodeRef nodeRef) {
        if (!generalService.getStore().equals(nodeRef.getStoreRef())) {
            String name = fileFolderService.getFileInfo(nodeRef).getName();
            return DownloadContentServlet.generateDownloadURL(nodeRef, name);
        }

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

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
    
    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setScannedFilesPath(String scannedDocumentsPath) {
        this.scannedFilesPath = scannedDocumentsPath;
    }

    // END: getters / setters

}
