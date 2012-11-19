package ee.webmedia.alfresco.document.file.service;

import static ee.webmedia.alfresco.utils.MimeUtil.isPdf;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.URLEncoder;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.model.GeneratedFileType;
import ee.webmedia.alfresco.document.file.web.Subfolder;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.MimeUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
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
    private ContentService contentService;
    private GeneralService generalService;
    private DocumentLogService documentLogService;
    private DocumentDynamicService _documentDynamicService;
    private VersionsService versionsService;
    private DocumentTemplateService _documentTemplateService;
    private Set<String> openOfficeFiles;

    private String scannedFilesPath;

    @Override
    public InputStream getFileContentInputStream(NodeRef fileRef) {
        ContentReader reader = fileFolderService.getReader(fileRef);
        return reader.getContentInputStream();
    }

    @Override
    public boolean toggleActive(NodeRef nodeRef) {
        boolean active = true; // If file doesn't have the flag set, then it hasn't been toggled yet, thus active
        if (!nodeService.exists(nodeRef)) {
            throw new UnableToPerformException("file_toggle_failed");
        }
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

    @Override
    public List<File> getFiles(List<NodeRef> nodeRefs) {
        List<FileInfo> fileInfos = new ArrayList<FileInfo>();
        for (NodeRef fileRef : nodeRefs) {
            fileInfos.add(fileFolderService.getFileInfo(fileRef));
        }
        return getAllFiles(fileInfos, false, false);
    }

    private List<File> getAllFiles(NodeRef nodeRef, boolean includeDigidocSubitems, boolean onlyActive) {
        List<FileInfo> fileInfos = fileFolderService.listFiles(nodeRef);
        return getAllFiles(fileInfos, includeDigidocSubitems, onlyActive);
    }

    private List<File> getAllFiles(List<FileInfo> fileInfos, boolean includeDigidocSubitems, boolean onlyActive) {
        List<File> files = new ArrayList<File>();
        for (FileInfo fi : fileInfos) {
            final File item = createFile(fi);
            boolean isDdoc = signatureService.isDigiDocContainer(item.getNodeRef());
            item.setDigiDocContainer(isDdoc);
            ContentData contentData = fi.getContentData();
            item.setTransformableToPdf(contentData != null && isTransformableToPdf(contentData.getMimetype()));
            item.setPdf(contentData != null && isPdf(contentData.getMimetype()));
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
    public List<NodeRef> getAllActiveFilesForDdoc(NodeRef nodeRef) {
        List<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        for (File file : getAllActiveFiles(nodeRef)) { // TODO not optimal
            if (file.getGeneratedFileRef() == null) {
                nodeRefs.add(file.getNodeRef());
            }
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
    public void transformActiveFilesToPdf(NodeRef document, boolean inactivateOriginalFiles) {
        List<File> allActiveFiles = getAllActiveFiles(document);
        List<NodeRef> deletedFiles = new ArrayList<NodeRef>();
        for (File file : allActiveFiles) {
            NodeRef fileRef = file.getNodeRef();
            if (deletedFiles.contains(fileRef)) {
                continue;
            }
            NodeRef generatedFileRef = file.getGeneratedFileRef();
            if (generatedFileRef != null && nodeService.exists(generatedFileRef)) {
                nodeService.deleteNode(generatedFileRef);
                deletedFiles.add(generatedFileRef);
            }
            FileInfo generatePdf = file.getConvertToPdfIfSignedFromProps() ? transformToPdf(document, fileRef, false) : null;
            if (generatePdf != null) {
                NodeRef pdfNodeRef = generatePdf.getNodeRef();
                nodeService.setProperty(pdfNodeRef
                        , FileModel.Props.GENERATION_TYPE, GeneratedFileType.SIGNED_PDF.name());
                nodeService.setProperty(file.getNodeRef(), FileModel.Props.GENERATED_FILE, pdfNodeRef);
            }
            if ((generatePdf != null || generatedFileRef != null) && inactivateOriginalFiles) {
                toggleActive(file.getNodeRef());
            }
        }
    }

    @Override
    public boolean isTransformableToPdf(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            return false;
        }
        if (MimeUtil.isPdf(mimeType)) {
            return false;
        }
        ContentTransformer transformer = contentService.getTransformer(mimeType, MimetypeMap.MIMETYPE_PDF);
        return transformer != null;
    }

    @Override
    public FileInfo transformToPdf(NodeRef docRef, NodeRef file, boolean createVersion) {
        if (!nodeService.exists(file)) {
            throw new UnableToPerformException("file_generate_pdf_error_deleted");
        }
        NodeRef previouslyGeneratedPdf = getPreviouslyGeneratedPdf(file);

        String filename = (String) nodeService.getProperty(previouslyGeneratedPdf == null ? file : previouslyGeneratedPdf, ContentModel.PROP_NAME);
        String displayName = (String) nodeService.getProperty(previouslyGeneratedPdf == null ? file : previouslyGeneratedPdf, FileModel.Props.DISPLAY_NAME);
        displayName = (displayName == null) ? filename : displayName;

        if (createVersion && previouslyGeneratedPdf != null) {
            versionsService.addVersionLockableAspect(previouslyGeneratedPdf);
            versionsService.updateVersion(previouslyGeneratedPdf, displayName, !isPdfUpToDate(file, previouslyGeneratedPdf));
            // Unlock the node here, since previous method locked it and there is no session (e.g. Word) that would unlock the file.
            versionsService.setVersionLockableAspect(previouslyGeneratedPdf, false);
        }

        ContentReader reader = fileFolderService.getReader(file);
        // Normally reader is not null; only some faulty code may write a file with no contentData
        if (reader == null || MimetypeMap.MIMETYPE_PDF.equals(reader.getMimetype())) {
            return null;
        }

        return transformToPdf(nodeService.getPrimaryParent(file).getParentRef(), file, reader, filename, displayName, previouslyGeneratedPdf);
    }

    @Override
    public FileInfo transformToPdf(NodeRef parent, NodeRef fileRef, ContentReader reader, String filename, String displayName, NodeRef overwritableNodeRef) {
        ContentWriter writer = overwritableNodeRef == null ? contentService.getWriter(null, null, false) : contentService.getWriter(overwritableNodeRef, ContentModel.PROP_CONTENT,
                true);
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

        FileInfo result;
        if (overwritableNodeRef == null) {
            String name = generalService.getUniqueFileName(parent, FilenameUtils.removeExtension(filename) + ".pdf");
            result = fileFolderService.create(parent, name, ContentModel.TYPE_CONTENT);
            nodeService.setProperty(result.getNodeRef(), ContentModel.PROP_CONTENT, writer.getContentData());
            displayName = FilenameUtils.removeExtension(displayName);
            displayName += ".pdf";
            nodeService.setProperty(result.getNodeRef(), FileModel.Props.DISPLAY_NAME, getUniqueFileDisplayName(parent, displayName));
        } else {
            result = fileFolderService.getFileInfo(overwritableNodeRef);
        }
        nodeService.setProperty(result.getNodeRef(), FileModel.Props.PDF_GENERATED_FROM_FILE, fileRef);

        return result;
    }

    @Override
    public void moveAllFiles(NodeRef fromRef, NodeRef toRef) throws FileNotFoundException {
        List<FileInfo> fileInfos = fileFolderService.listFiles(fromRef);
        for (FileInfo fileInfo : fileInfos) {
            if (FilenameUtil.isEncryptedFile(fileInfo.getName())) {
                throw new UnableToPerformException("file_encrypted_forbidden");
            }
        }
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
        return addFileToDocument(name, displayName, documentNodeRef, fileNodeRef, true, false);
    }

    @Override
    public NodeRef addFileToDocument(final String name, final String displayName, final NodeRef documentNodeRef, final NodeRef fileNodeRef, boolean active,
            boolean associatedWithMetaData) {
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
        checkAssociatedWithMetaData(fileNodeRef, associatedWithMetaData);
        nodeService.setProperty(fileNodeRef, FileModel.Props.ACTIVE, active);
        addFileToDocument(displayName, documentNodeRef, movedFileNodeRef);
        return movedFileNodeRef;
    }

    @Override
    public NodeRef addFileToDocument(String name, String displayName, NodeRef documentNodeRef, java.io.File file, String mimeType) {
        return addFileToDocument(name, displayName, documentNodeRef, file, mimeType, true, false);
    }

    @Override
    public NodeRef addFileToDocument(String name, String displayName, NodeRef documentNodeRef, java.io.File file, String mimeType, boolean active, boolean associatedWithMetaData) {
        NodeRef fileNodeRef = addFile(name, displayName, documentNodeRef, file, mimeType, active, associatedWithMetaData);
        addFileToDocument(displayName, documentNodeRef, fileNodeRef);
        return fileNodeRef;
    }

    @Override
    public NodeRef addFile(String name, String displayName, NodeRef nodeRef, java.io.File file, String mimeType) {
        return addFile(name, displayName, nodeRef, file, mimeType, true, false);
    }

    @Override
    public NodeRef addFile(String name, String displayName, NodeRef nodeRef, java.io.File file, String mimeType, boolean active, boolean associatedWithMetaData) {
        FileInfo fileInfo = fileFolderService.create(
                nodeRef,
                name,
                ContentModel.TYPE_CONTENT);
        NodeRef fileNodeRef = fileInfo.getNodeRef();
        ContentWriter writer = fileFolderService.getWriter(fileNodeRef);
        generalService.writeFile(writer, file, name, mimeType);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        checkAssociatedWithMetaData(associatedWithMetaData, props);
        props.put(FileModel.Props.DISPLAY_NAME, displayName);
        props.put(FileModel.Props.ACTIVE, active);
        props.put(FileModel.Props.CONVERT_TO_PDF_IF_SIGNED, isTransformableToPdf(writer.getMimetype()));
        nodeService.addProperties(fileNodeRef, props);
        if (associatedWithMetaData) {
            getDocumentDynamicService().updateDocumentAndGeneratedFiles(fileNodeRef, nodeRef, false);
        }

        return fileNodeRef;
    }

    private Map<QName, Serializable> checkAssociatedWithMetaData(boolean associatedWithMetaData, Map<QName, Serializable> props) {
        if (props == null) {
            props = new HashMap<QName, Serializable>(2);
        }
        if (!associatedWithMetaData) {
            return props;
        }
        props.put(FileModel.Props.GENERATED_FROM_TEMPLATE, MessageUtil.getMessage("file_uploaded_by_user"));
        props.put(FileModel.Props.GENERATION_TYPE, GeneratedFileType.WORD_TEMPLATE.name());
        props.put(FileModel.Props.UPDATE_METADATA_IN_FILES, Boolean.TRUE);

        return props;
    }

    private void checkAssociatedWithMetaData(NodeRef fileNodeRef, boolean associatedWithMetaData) {
        nodeService.addProperties(fileNodeRef, checkAssociatedWithMetaData(associatedWithMetaData, null));
    }

    @Override
    public NodeRef addFile(String name, String displayName, NodeRef parentNodeRef, ContentReader reader) {
        FileInfo fileInfo = fileFolderService.create(
                parentNodeRef,
                name,
                ContentModel.TYPE_CONTENT);
        NodeRef fileNodeRef = fileInfo.getNodeRef();
        ContentWriter writer = fileFolderService.getWriter(fileNodeRef);
        writer.setEncoding(reader.getEncoding());
        writer.setMimetype(reader.getMimetype());
        writer.putContent(reader.getContentInputStream());
        nodeService.setProperty(fileNodeRef, FileModel.Props.DISPLAY_NAME, displayName);
        return fileNodeRef;
    }

    private void addFileToDocument(String displayName, NodeRef documentNodeRef, NodeRef fileNodeRef) {
        versionsService.addVersionModifiedAspect(fileNodeRef);
        versionsService.addVersionLockableAspect(fileNodeRef);
        documentLogService.addDocumentLog(documentNodeRef, MessageUtil.getMessage("document_log_status_fileAdded", displayName));
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
        String runAsUser = AuthenticationUtil.getRunAsUser();
        String name = fileFolderService.getFileInfo(nodeRef).getName();
        NodeRef primaryParentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        boolean isUnderDocument = DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(primaryParentRef));
        if (!nodeRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE) || StringUtils.isBlank(runAsUser) || AuthenticationUtil.isRunAsUserTheSystemUser()
                || !isUnderDocument) {
            return DownloadContentServlet.generateDownloadURL(nodeRef, name);
        }

        StringBuilder path = new StringBuilder();
        if (isOpenOfficeFile(name)) {
            path.append("vnd.sun.star.webdav://").append(getDocumentTemplateService().getServerUrl().replaceFirst(".*://", ""));
        }
        // calculate a WebDAV URL for the given node
        path.append("/").append(WebDAVServlet.WEBDAV_PREFIX);

        // authentication ticket
        String ticket = authenticationService.getCurrentTicket();
        if (ticket.startsWith(InMemoryTicketComponentImpl.GRANTED_AUTHORITY_TICKET_PREFIX)) {
            ticket = ticket.substring(InMemoryTicketComponentImpl.GRANTED_AUTHORITY_TICKET_PREFIX.length());
        }
        path.append("/").append(URLEncoder.encode(ticket));

        path.append("/").append(URLEncoder.encode(AuthenticationUtil.getRunAsUser())); // maybe substituting

        NodeRef parent = primaryParentRef;
        path.append("/").append(URLEncoder.encode(parent.getId()));

        String filename = URLEncoder.encode(name);
        path.append("/").append(filename);

        return path.toString();
    }

    private boolean isOpenOfficeFile(String name) {
        return openOfficeFiles.contains(FilenameUtils.getExtension(name));
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

    @Override
    public List<Subfolder> getSubfolders(NodeRef parentRef, QName childNodeType, QName countableChildNodeType) {
        List<Subfolder> subfolders = new ArrayList<Subfolder>();
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parentRef, Collections.singleton(childNodeType));
        for (ChildAssociationRef childAssocRef : childAssocs) {
            NodeRef childRef = childAssocRef.getChildRef();
            Node folder = new Node(childRef);
            List<ChildAssociationRef> documents = nodeService.getChildAssocs(childRef, Collections.singleton(countableChildNodeType));
            subfolders.add(new Subfolder(folder, documents == null ? 0 : documents.size()));
        }
        return subfolders;
    }

    @Override
    public NodeRef findSubfolderWithName(NodeRef parentNodeRef, String folderName, QName subfolderType) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parentNodeRef, Collections.singleton(subfolderType));
        for (ChildAssociationRef childAssoc : childAssocs) {
            if (childAssoc.getQName().getLocalName().equals(folderName)) {
                return childAssoc.getChildRef();
            }
        }
        return null;
    }

    @Override
    public boolean isFileGenerated(NodeRef fileRef) {
        return fileRef != null && (isFileGeneratedFromTemplate(fileRef) || nodeService.getProperty(fileRef, FileModel.Props.GENERATION_TYPE) != null);
    }

    @Override
    public boolean isFileGeneratedFromTemplate(NodeRef fileRef) {
        return fileRef != null && nodeService.getProperty(fileRef, FileModel.Props.GENERATED_FROM_TEMPLATE) != null;
    }

    @Override
    public NodeRef getPreviouslyGeneratedPdf(NodeRef sourceFileRef) {
        if (!nodeService.exists(sourceFileRef)) {
            return null;
        }

        NodeRef parentRef = nodeService.getPrimaryParent(sourceFileRef).getParentRef();
        List<FileInfo> listFiles = fileFolderService.listFiles(parentRef);
        for (FileInfo fi : listFiles) {
            NodeRef fromNodeRef = (NodeRef) fi.getProperties().get(FileModel.Props.PDF_GENERATED_FROM_FILE);
            if (ObjectUtils.equals(sourceFileRef, fromNodeRef)) {
                return fi.getNodeRef();
            }
        }

        return null;
    }

    @Override
    public boolean isPdfUpToDate(NodeRef sourceFileRef, NodeRef pdfFileRef) {
        if (sourceFileRef == null || pdfFileRef == null) {
            return false;
        }

        Date sourceModified = (Date) nodeService.getProperty(sourceFileRef, ContentModel.PROP_MODIFIED);
        Date pdfModified = (Date) nodeService.getProperty(pdfFileRef, ContentModel.PROP_MODIFIED);
        return sourceModified != null && pdfModified != null && sourceModified.before(pdfModified);
    }

    @Override
    public boolean isFileAssociatedWithDocMetadata(NodeRef fileRef) {
        return fileRef != null && Boolean.TRUE.equals(nodeService.getProperty(fileRef, FileModel.Props.UPDATE_METADATA_IN_FILES));
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

    public DocumentDynamicService getDocumentDynamicService() {
        if (_documentDynamicService == null) {
            _documentDynamicService = BeanHelper.getDocumentDynamicService();
        }
        return _documentDynamicService;
    }

    public void setVersionsService(VersionsService versionsService) {
        this.versionsService = versionsService;
    }

    public DocumentTemplateService getDocumentTemplateService() {
        if (_documentTemplateService == null) {
            _documentTemplateService = BeanHelper.getDocumentTemplateService();
        }
        return _documentTemplateService;
    }

    public void setOpenOfficeFiles(String openOfficeFiles) {
        if (StringUtils.isBlank(openOfficeFiles)) {
            this.openOfficeFiles = Collections.emptySet();
        }
        String[] extensions = openOfficeFiles.split(",");
        Set<String> extSet = new HashSet<String>();
        for (String ext : extensions) {
            extSet.add(ext);
        }
        this.openOfficeFiles = extSet;
    }

    // END: getters / setters

}