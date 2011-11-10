package ee.webmedia.alfresco.document.file.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.authentication.InMemoryTicketComponentImpl;
import org.alfresco.repo.webdav.WebDAVServlet;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.URLEncoder;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.model.GeneratedFileType;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.versions.model.VersionsModel;

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
    private ContentService contentService;
    private GeneralService generalService;
    private DocumentLogService documentLogService;

    private String scannedFilesPath;

    @Override
    public boolean toggleActive(NodeRef nodeRef) {
        boolean active = true; // If file doesn't have the flag set, then it hasn't been toggled yet, thus active
        if (nodeService.getProperty(nodeRef, FileModel.Props.ACTIVE) != null) {
            active = Boolean.parseBoolean(nodeService.getProperty(nodeRef, FileModel.Props.ACTIVE).toString());
        }
        active = !active;
        nodeService.setProperty(nodeRef, FileModel.Props.ACTIVE, active);
        return active;
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
            final File item = createFile(fi);
            boolean isDdoc = signatureService.isDigiDocContainer(item.getNodeRef());
            item.setDigiDocContainer(isDdoc);
            item.setTransformableToPdf(isTransformableToPdf(fi.getContentData()));
            item.setPdf(isPdfFile(fi.getContentData()));
            if (item.isActive() || !onlyActive) {
                files.add(item);
            }
            if (isDdoc && includeDigidocSubitems) {
                // hack: add another File to display nested tables in JSP.
                // this "item2" should be exactly after the "item" in the list
                try {
                    File item2 = new File();
                    SignatureItemsAndDataItems ddocItems = AuthenticationUtil.runAs(new RunAsWork<SignatureItemsAndDataItems>() {
                        @Override
                        public SignatureItemsAndDataItems doWork() throws Exception {
                            return signatureService.getDataItemsAndSignatureItems(item.getNodeRef(), false);
                        }
                    }, AuthenticationUtil.getSystemUserName());
                    item2.setNode(item.getNode()); // Digidoc item uses node of its container for permission evaluations
                    item2.setDdocItems(ddocItems);
                    item2.setDigiDocItem(true);
                    item2.setActive(item.isActive());
                    if (item.isActive() || !onlyActive) {
                        files.add(item2);
                    }
                } catch (RuntimeException e) {
                    if (e.getCause() instanceof SignatureException) {
                        if (log.isDebugEnabled()) {
                            log.debug("Unable to parse DigiDoc file '" + item.getName() + "':\n" + e.getCause().getMessage());
                        }
                    } else {
                        throw e;
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
        for (File file : getAllActiveFiles(nodeRef)) { // TODO not optimal
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
        for (File file : getAllActiveFiles(document)) {
            FileInfo generatePdf = generatePdf(document, file.getNodeRef());
            if (generatePdf != null) {
                nodeService.setProperty(generatePdf.getNodeRef()
                        , FileModel.Props.GENERATION_TYPE, GeneratedFileType.SIGNED_PDF.name());
                toggleActive(file.getNodeRef());
            }
        }
    }

    private boolean isTransformableToPdf(ContentData contentData) {
        if (contentData == null) {
            return false;
        }
        if (isPdfFile(contentData)) {
            return false;
        }
        ContentTransformer transformer = contentService.getTransformer(contentData.getMimetype(), MimetypeMap.MIMETYPE_PDF);
        return transformer != null;
    }

    private boolean isPdfFile(ContentData contentData) {
        return MimetypeMap.MIMETYPE_PDF.equals(contentData.getMimetype());
    }

    @Override
    public FileInfo transformToPdf(NodeRef file) {
        return generatePdf(nodeService.getPrimaryParent(file).getParentRef(), file);
    }

    private FileInfo generatePdf(NodeRef parent, NodeRef file) {
        ContentReader reader = fileFolderService.getReader(file);
        // Normally reader is not null; only some faulty code may write a file with no contentData
        if (reader == null || MimetypeMap.MIMETYPE_PDF.equals(reader.getMimetype())) {
            return null;
        }
        String filename = (String) nodeService.getProperty(file, ContentModel.PROP_NAME);
        String displayName = (String) nodeService.getProperty(file, FileModel.Props.DISPLAY_NAME);
        displayName = (displayName == null) ? filename : displayName;
        return transformToPdf(parent, reader, filename, displayName);
    }

    @Override
    public FileInfo transformToPdf(NodeRef parent, ContentReader reader, String filename, String displayName) {
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
                        + reader.getSize() + " bytes, mimeType=" + reader.getMimetype() + ", encoding=" + reader.getEncoding() + ", PDF size="
                        + writer.getSize() + " bytes");
            }
        } catch (ContentIOException e) {
            log.debug("Failed to transform file to PDF, filename=" + filename + ", size=" + reader.getSize() + ", mimeType=" + reader.getMimetype()
                    + ", encoding=" + reader.getEncoding(), e);
            return null;
        }

        String name = generalService.getUniqueFileName(parent, FilenameUtils.removeExtension(filename) + ".pdf");
        FileInfo createdFile = fileFolderService.create(
                parent,
                name,
                ContentModel.TYPE_CONTENT);
        nodeService.setProperty(createdFile.getNodeRef(), ContentModel.PROP_CONTENT, writer.getContentData());
        displayName = FilenameUtils.removeExtension(displayName);
        displayName += ".pdf";
        nodeService.setProperty(createdFile.getNodeRef(), FileModel.Props.DISPLAY_NAME, getUniqueFileDisplayName(parent, displayName));

        return createdFile;
    }

    @Override
    public void moveAllFiles(NodeRef fromRef, NodeRef toRef) throws FileNotFoundException {
        List<FileInfo> fileInfos = fileFolderService.listFiles(fromRef);
        for (FileInfo fileInfo : fileInfos) {
            try {
                fileFolderService.move(fileInfo.getNodeRef(), toRef, null);

                String fileName = fileInfo.getName();
                String displayName = (String) fileInfo.getProperties().get(FileModel.Props.DISPLAY_NAME);
                if (StringUtils.isNotBlank(displayName)) {
                    fileName = displayName;
                }
                documentLogService.addDocumentLog(toRef, I18NUtil.getMessage("document_log_status_fileAdded", fileName));
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
    public NodeRef addFileToDocument(final String name, final String displayName, final NodeRef documentNodeRef, final NodeRef fileNodeRef) {
        // Moving is executed with System user rights, because this is not appropriate to implement in permissions model
        NodeRef movedFileNodeRef = AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
            @Override
            public NodeRef doWork() throws Exception {
                // change file name
                nodeService.setProperty(fileNodeRef, ContentModel.PROP_NAME, name);
                // move file
                return nodeService.moveNode(fileNodeRef, documentNodeRef,
                        ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS).getChildRef();
            }
        }, AuthenticationUtil.getSystemUserName());
        addFileToDocument(displayName, documentNodeRef, movedFileNodeRef);
        return movedFileNodeRef;
    }

    @Override
    public NodeRef addFileToDocument(String name, String displayName, NodeRef documentNodeRef, java.io.File file, String mimeType) {
        FileInfo fileInfo = fileFolderService.create(
                documentNodeRef,
                name,
                ContentModel.TYPE_CONTENT);
        NodeRef fileNodeRef = fileInfo.getNodeRef();
        ContentWriter writer = fileFolderService.getWriter(fileNodeRef);
        generalService.writeFile(writer, file, name, mimeType);

        addFileToDocument(displayName, documentNodeRef, fileNodeRef);

        return fileNodeRef;
    }

    private void addFileToDocument(String displayName, NodeRef documentNodeRef, NodeRef fileNodeRef) {
        nodeService.setProperty(fileNodeRef, FileModel.Props.DISPLAY_NAME, displayName);
        addVersionModifiedAspect(fileNodeRef);
        documentLogService.addDocumentLog(documentNodeRef, MessageUtil.getMessage("document_log_status_fileAdded", displayName));
    }

    private void addVersionModifiedAspect(NodeRef nodeRef) {
        if (!nodeService.hasAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED)) {
            Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);

            String user = (String) properties.get(ContentModel.PROP_CREATOR);

            Date modified = DefaultTypeConverter.INSTANCE.convert(Date.class, properties.get(ContentModel.PROP_CREATED));
            Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>(3);
            aspectProperties.put(VersionsModel.Props.VersionModified.MODIFIED, modified);

            Map<QName, Serializable> personProps = userService.getUserProperties(user);
            String first = (String) personProps.get(ContentModel.PROP_FIRSTNAME);
            String last = (String) personProps.get(ContentModel.PROP_LASTNAME);

            aspectProperties.put(VersionsModel.Props.VersionModified.FIRSTNAME, first);
            aspectProperties.put(VersionsModel.Props.VersionModified.LASTNAME, last);

            nodeService.addAspect(nodeRef, VersionsModel.Aspects.VERSION_MODIFIED, aspectProperties);
        }
    }

    @Override
    public List<File> getScannedFolders() {
        if (log.isDebugEnabled()) {
            log.debug("Getting scanned files");
        }
        NodeRef scannedNodeRef = generalService.getNodeRef(scannedFilesPath);
        Assert.notNull(scannedNodeRef, "Scanned files node reference not found");
        List<FileInfo> fileInfos = fileFolderService.listFolders(scannedNodeRef);
        List<File> files = new ArrayList<File>(fileInfos.size());
        for (FileInfo fileInfo : fileInfos) {
            final File file = createFile(fileInfo);
            file.setName(userService.getUserFullName(file.getName())); // real folder names are userNames - replace them with user full name
            final int nrOfChildren = nodeService.getChildAssocs(file.getNodeRef(), ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL).size();
            file.setNrOfChildren(nrOfChildren);
            files.add(file);
        }
        return files;
    }

    @Override
    public List<File> getAllScannedFiles() {
        NodeRef scannedNodeRef = generalService.getNodeRef(scannedFilesPath);
        Assert.notNull(scannedNodeRef, "Scanned files node reference not found");
        List<FileInfo> fileInfos = fileFolderService.listFolders(scannedNodeRef);
        List<File> files = new ArrayList<File>(fileInfos.size());
        for (FileInfo fileInfo : fileInfos) {
            final List<File> filesInFolder = getScannedFiles(fileInfo.getNodeRef());
            files.addAll(filesInFolder);
        }
        return files;
    }

    @Override
    public List<File> getScannedFiles(NodeRef folderRef) {
        if (log.isDebugEnabled()) {
            log.debug("Getting scanned files");
        }
        Assert.notNull(folderRef, "Scanned folder node reference not found");
        return getAllFilesExcludingDigidocSubitems(folderRef);
    }

    private File createFile(FileInfo fi) {
        File item = new File(fi);
        item.setCreator(userService.getUserFullName((String) fi.getProperties().get(ContentModel.PROP_CREATOR)));
        item.setModifier(userService.getUserFullName((String) fi.getProperties().get(ContentModel.PROP_MODIFIER)));
        item.setDownloadUrl(generateURL(item.getNodeRef()));
        return item;
    }

    @Override
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

        path.append("/").append(URLEncoder.encode(AuthenticationUtil.getRunAsUser())); // maybe substituting

        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        path.append("/").append(URLEncoder.encode(parent.getId()));

        String name = fileFolderService.getFileInfo(nodeRef).getName();
        path.append("/").append(URLEncoder.encode(name));

        return path.toString();
    }

    @Override
    public String getUniqueFileDisplayName(NodeRef folder, String displayName) {
        List<String> childDisplayNames = getDocumentFileDisplayNames(folder);
        return FilenameUtil.generateUniqueFileDisplayName(displayName, childDisplayNames);
    }

    @Override
    public List<String> getDocumentFileDisplayNames(NodeRef folder) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(folder);
        List<String> childDisplayNames = new ArrayList<String>(childAssocs.size());

        for (ChildAssociationRef caRef : childAssocs) {
            Serializable property = nodeService.getProperty(caRef.getChildRef(), FileModel.Props.DISPLAY_NAME);
            if (property != null) {
                childDisplayNames.add(property.toString());
            }
        }
        return childDisplayNames;
    }

    @Override
    public void deleteGeneratedFilesByType(NodeRef parentRef, GeneratedFileType type) {
        String typeToDelete = type.name();
        for (FileInfo fi : fileFolderService.listFiles(parentRef)) {
            String generatedType = (String) fi.getProperties().get(FileModel.Props.GENERATION_TYPE);
            if (StringUtils.isNotBlank(generatedType) && generatedType.equals(typeToDelete)) {
                fileFolderService.delete(fi.getNodeRef());
                if (log.isDebugEnabled()) {
                    log.info("Deleted file with generationType=" + typeToDelete + " " + fi.getNodeRef());
                }
            }
        }
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
        scannedFilesPath = scannedDocumentsPath;
    }

    // END: getters / setters

}
