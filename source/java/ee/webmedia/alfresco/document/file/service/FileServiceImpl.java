package ee.webmedia.alfresco.document.file.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getFileFolderService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVersionsService;
import static ee.webmedia.alfresco.utils.MimeUtil.isPdf;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.alfresco.util.Pair;
import org.alfresco.util.URLEncoder;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.FileModel;
import ee.webmedia.alfresco.document.file.model.GeneratedFileType;
import ee.webmedia.alfresco.document.file.web.Subfolder;
import ee.webmedia.alfresco.document.lock.service.DocLockService;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.dvk.model.DvkModel;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.MimeUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class FileServiceImpl implements FileService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(FileServiceImpl.class);

    private UserService userService;
    private SignatureService signatureService;
    private AuthenticationService authenticationService;
    private NodeService nodeService;
    private ContentService contentService;
    private DocumentLogService documentLogService;
    private DocumentDynamicService _documentDynamicService;
    private DocumentTemplateService _documentTemplateService;
    private DocLockService _docLockService;
    private Set<String> openOfficeFiles;
    private String jumploaderPath;

    private String scannedFilesPath;

    @Override
    public InputStream getFileContentInputStream(NodeRef fileRef) {
        ContentReader reader = getFileFolderService().getReader(fileRef);
        return reader.getContentInputStream();
    }

    @Override
    public boolean toggleActive(NodeRef nodeRef) {
        boolean active = true; // If file doesn't have the flag set, then it hasn't been toggled yet, thus active
        if (!getNodeService().exists(nodeRef)) {
            throw new UnableToPerformException("file_toggle_failed");
        }
        if (getNodeService().getProperty(nodeRef, FileModel.Props.ACTIVE) != null) {
            active = Boolean.parseBoolean(getNodeService().getProperty(nodeRef, FileModel.Props.ACTIVE).toString());
        }
        active = !active;
        getNodeService().setProperty(nodeRef, FileModel.Props.ACTIVE, active);
        NodeRef documentRef = getNodeService().getPrimaryParent(nodeRef).getParentRef();
        getGeneralService().setModifiedToNow(documentRef);
        return active;
    }

    @Override
    public List<File> getAllFilesExcludingDigidocSubitems(NodeRef nodeRef) {
        return getAllFiles(nodeRef, false, false);
    }

    @Override
    public List<File> getAllFilesExcludingDigidocSubitemsAndIncludingDecContainers(NodeRef nodeRef) {
        List<FileInfo> fileInfos = getFileFolderService().listFiles(nodeRef);
        return getAllFiles(fileInfos, false, false, false);
    }

    @Override
    public NodeRef getDecContainer(NodeRef documentNodeRef) {
        return (NodeRef) nodeService.getProperty(documentNodeRef, DvkModel.Props.DEC_CONTAINER);
    }

    @Override
    public boolean removeDecContainer(NodeRef documentNodeRef) {
        boolean decContainerDeleted = false;
        NodeRef decContainer = getDecContainer(documentNodeRef);
        if (decContainer != null) {
            nodeService.removeProperty(documentNodeRef, DvkModel.Props.DEC_CONTAINER);
            BeanHelper.getGeneralService().deleteNodeRefs(Arrays.asList(decContainer), false); // Avoid archiving!
            decContainerDeleted = true;
        }
        return decContainerDeleted;
    }

    @Override
    public List<File> getAllFiles(NodeRef nodeRef) {
        return getAllFiles(nodeRef, true, false);
    }

    @Override
    public List<File> getFiles(List<NodeRef> nodeRefs) {
        List<FileInfo> fileInfos = new ArrayList<FileInfo>();
        for (NodeRef fileRef : nodeRefs) {
            fileInfos.add(getFileFolderService().getFileInfo(fileRef));
        }
        return getAllFiles(fileInfos, false, false);
    }

    private List<File> getAllFiles(NodeRef nodeRef, boolean includeDigidocSubitems, boolean onlyActive) {
        List<FileInfo> fileInfos = BeanHelper.getFileFolderService().listFiles(nodeRef);
        return getAllFiles(fileInfos, includeDigidocSubitems, onlyActive);
    }

    private List<File> getAllFiles(List<FileInfo> fileInfos, boolean includeDigidocSubitems, boolean onlyActive) {
        return getAllFiles(fileInfos, includeDigidocSubitems, onlyActive, true);
    }

    private List<File> getAllFiles(List<FileInfo> fileInfos, boolean includeDigidocSubitems, boolean onlyActive, boolean excludeDecContainers) {
        List<File> files = new ArrayList<File>();
        for (FileInfo fi : fileInfos) {
            final File item = createFile(fi);

            // Exclude DEC containers
            if (excludeDecContainers && item.isDecContainer()) {
                continue;
            }

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
    public List<NodeRef> getAllFileRefs(NodeRef nodeRef, boolean activeFilesOnly) {
        List<NodeRef> fileRefs = getFileFolderService().listFileRefs(nodeRef);
        if (activeFilesOnly && CollectionUtils.isNotEmpty(fileRefs)) {
            List<NodeRef> activeFileRefs = new ArrayList<NodeRef>(fileRefs.size());
            for (NodeRef fileRef : fileRefs) {
                if (!Boolean.FALSE.equals(getNodeService().getProperty(fileRef, FileModel.Props.ACTIVE)) && getNodeService().getProperty(fileRef, DvkModel.Props.DVK_ID) == null) {
                    activeFileRefs.add(fileRef);
                }
            }
            return activeFileRefs;
        }
        return fileRefs;
    }

    @Override
    public List<File> getAllActiveAndInactiveFiles(NodeRef nodeRef) {
        return getAllFiles(nodeRef, false, false);
    }

    @Override
    public List<NodeRef> getAllActiveFilesForDdoc(NodeRef nodeRef) {
        List<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        for (File file : getAllActiveFiles(nodeRef)) { // TODO not optimal
            if (file.getGeneratedFileRef() == null) {
                nodeRefs.add(file.getNodeRef());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("skipping signing file, name=" + file.getDisplayName() + ", nodeRef=" + file.getNodeRef() + ", generatedFileRef=" + file.getGeneratedFileRef());
                }
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
            if (log.isDebugEnabled()) {
                log.debug("start transforming file to pdf, nodeRef=" + fileRef);
            }
            if (deletedFiles.contains(fileRef)) {
                if (log.isDebugEnabled()) {
                    log.debug("skipping deletable generated file");
                }
                continue;
            }
            NodeRef generatedFileRef = file.getGeneratedFileRef();
            if (generatedFileRef != null) {
                if (getNodeService().exists(generatedFileRef)) {
                    getNodeService().deleteNode(generatedFileRef);
                    deletedFiles.add(generatedFileRef);
                    if (log.isDebugEnabled()) {
                        log.debug("deleted generated file, nodeRef=" + generatedFileRef);
                    }
                }
                // note that file is not deleted completely, but moves to trashcan,
                // so we cannot count on Alfresco functionality that sets nodeRef properties to null when node is deleted
                getNodeService().setProperty(file.getNodeRef(), FileModel.Props.GENERATED_FILE, null);
                if (log.isDebugEnabled()) {
                    log.debug("set generated fileRef=null");
                }
            }
            FileInfo generatePdf = file.getConvertToPdfIfSignedFromProps() ? transformToPdf(document, fileRef, false) : null;
            if (generatePdf != null) {
                NodeRef pdfNodeRef = generatePdf.getNodeRef();
                getNodeService().setProperty(pdfNodeRef
                        , FileModel.Props.GENERATION_TYPE, GeneratedFileType.SIGNED_PDF.name());
                getNodeService().setProperty(file.getNodeRef(), FileModel.Props.GENERATED_FILE, pdfNodeRef);
                if (log.isDebugEnabled()) {
                    log.debug("added generated file reference, generatedNodeRef=" + pdfNodeRef + ", active=" + generatePdf.getProperties().get(FileModel.Props.ACTIVE)
                            + ", properties=\n" + generatePdf.getProperties());
                }
            }
            if (generatePdf != null && inactivateOriginalFiles) {
                toggleActive(fileRef);
                if (log.isDebugEnabled()) {
                    log.debug("set file inactive");
                }
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
        if (!getNodeService().exists(file)) {
            throw new UnableToPerformException("file_generate_pdf_error_deleted");
        }
        NodeRef previouslyGeneratedPdf = getPreviouslyGeneratedPdf(file);

        String filename = (String) getNodeService().getProperty(previouslyGeneratedPdf == null ? file : previouslyGeneratedPdf, ContentModel.PROP_NAME);
        String displayName = (String) getNodeService().getProperty(previouslyGeneratedPdf == null ? file : previouslyGeneratedPdf, FileModel.Props.DISPLAY_NAME);
        displayName = (displayName == null) ? filename : displayName;

        if (createVersion && previouslyGeneratedPdf != null) {
            getVersionsService().addVersionLockableAspect(previouslyGeneratedPdf);
            getVersionsService().updateVersion(previouslyGeneratedPdf, displayName, !isPdfUpToDate(file, previouslyGeneratedPdf));
            // Unlock the node here, since previous method locked it and there is no session (e.g. Word) that would unlock the file.
            getVersionsService().setVersionLockableAspect(previouslyGeneratedPdf, false);
        }

        ContentReader reader = getFileFolderService().getReader(file);
        // Normally reader is not null; only some faulty code may write a file with no contentData
        if (reader == null || MimetypeMap.MIMETYPE_PDF.equals(reader.getMimetype())) {
            return null;
        }

        return transformToPdf(getNodeService().getPrimaryParent(file).getParentRef(), file, reader, filename, displayName, previouslyGeneratedPdf);
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
            String name = getGeneralService().getUniqueFileName(parent, FilenameUtils.removeExtension(filename) + ".pdf");
            result = getFileFolderService().create(parent, name, ContentModel.TYPE_CONTENT);
            getNodeService().setProperty(result.getNodeRef(), ContentModel.PROP_CONTENT, writer.getContentData());
            displayName = FilenameUtils.removeExtension(displayName);
            displayName += ".pdf";
            getNodeService().setProperty(result.getNodeRef(), FileModel.Props.DISPLAY_NAME, getUniqueFileDisplayName(parent, displayName));
        } else {
            result = getFileFolderService().getFileInfo(overwritableNodeRef);
        }
        getNodeService().setProperty(result.getNodeRef(), FileModel.Props.PDF_GENERATED_FROM_FILE, fileRef);
        getGeneralService().setModifiedToNow(parent);
        return result;
    }

    @Override
    public void moveAllFiles(NodeRef fromRef, NodeRef toRef) throws FileNotFoundException {
        List<FileInfo> fileInfos = getFileFolderService().listFiles(fromRef);
        for (FileInfo fileInfo : fileInfos) {
            if (FilenameUtil.isEncryptedFile(fileInfo.getName())) {
                throw new UnableToPerformException("file_encrypted_forbidden");
            }
        }
        for (FileInfo fileInfo : fileInfos) {
            try {
                getFileFolderService().move(fileInfo.getNodeRef(), toRef, null);

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
    public boolean addExistingFileToDocument(final String name, final String displayName, final NodeRef documentNodeRef, final NodeRef fileNodeRef, boolean active,
            boolean associatedWithMetaData) {
        // Moving is executed with System user rights, because this is not appropriate to implement in permissions model
        NodeRef movedFileNodeRef = AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
            @Override
            public NodeRef doWork() throws Exception {
                // change file name
                Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                props.put(ContentModel.PROP_NAME, name);
                if (!StringUtils.isBlank(displayName)) {
                    props.put(FileModel.Props.DISPLAY_NAME, displayName);
                }
                // Store current location (as string!) (in case where user decides to abort document creation)
                props.put(FileModel.Props.PREVIOUS_FILE_PARENT, getNodeService().getPrimaryParent(fileNodeRef).getParentRef().toString());
                getNodeService().addProperties(fileNodeRef, props);

                // move file
                return getNodeService().moveNode(fileNodeRef, documentNodeRef,
                        ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS).getChildRef();
            }
        }, AuthenticationUtil.getSystemUserName());

        associatedWithMetaData = addFilePropsAndUpdateDocumentMetadata(documentNodeRef, fileNodeRef, associatedWithMetaData, active,
                getFileFolderService().getWriter(movedFileNodeRef)
                .getMimetype(), null, name);

        addDocumentFileVersionAndLog(displayName, documentNodeRef, movedFileNodeRef);
        return associatedWithMetaData;
    }

    @Override
    public boolean addUploadedFileToDocument(String name, String displayName, NodeRef documentNodeRef, java.io.File file, String mimeType, boolean active,
            boolean associatedWithMetaData) {
        Pair<NodeRef, Boolean> fileNodeRefAndHasFormulae = addFile(name, displayName, documentNodeRef, file, mimeType, active, associatedWithMetaData);
        addDocumentFileVersionAndLog(displayName, documentNodeRef, fileNodeRefAndHasFormulae.getFirst());
        return fileNodeRefAndHasFormulae.getSecond();
    }

    @Override
    public NodeRef addFileToTask(String name, String displayName, NodeRef nodeRef, java.io.File file, String mimeType) {
        return addFile(name, displayName, nodeRef, file, mimeType, true, false).getFirst();
    }

    private boolean addFilePropsAndUpdateDocumentMetadata(final NodeRef documentNodeRef, final NodeRef fileNodeRef, boolean associatedWithMetaData, boolean active,
            String mimetype, Map<QName, Serializable> props, String filename) {
        if (props == null) {
            props = new HashMap<QName, Serializable>(3);
        }
        props.put(FileModel.Props.ACTIVE, active);
        props.put(FileModel.Props.CONVERT_TO_PDF_IF_SIGNED, isTransformableToPdf(mimetype));
        if (associatedWithMetaData) {
            GeneratedFileType generationType = null;
            if (FilenameUtils.isExtension(filename, Arrays.asList("doc", "docx", "xls", "xlsx", "rtf"))) {
                generationType = GeneratedFileType.WORD_TEMPLATE;
            } else if (FilenameUtils.isExtension(filename, Arrays.asList("odt", "ods"))) {
                generationType = GeneratedFileType.OPENOFFICE_TEMPLATE;
            } else {
                throw new RuntimeException("Unknown generation type for filename " + filename);
            }
            props.put(FileModel.Props.GENERATION_TYPE, generationType.name());
            props.put(FileModel.Props.GENERATED_FROM_TEMPLATE, MessageUtil.getMessage("file_uploaded_by_user"));
            props.put(FileModel.Props.UPDATE_METADATA_IN_FILES, Boolean.TRUE);
        }
        getNodeService().addProperties(fileNodeRef, props);
        if (associatedWithMetaData) {
            associatedWithMetaData = getDocumentDynamicService().updateDocumentAndGeneratedFiles(fileNodeRef, documentNodeRef, false);
            // if document contained no Delta formulae, remove association between document metadata and file
            if (!associatedWithMetaData) {
                getNodeService().setProperty(fileNodeRef, FileModel.Props.UPDATE_METADATA_IN_FILES, Boolean.FALSE);
            }
        }
        return associatedWithMetaData;
    }

    private Pair<NodeRef, Boolean> addFile(String name, String displayName, NodeRef nodeRef, java.io.File file, String mimeType, boolean active, boolean associatedWithMetaData) {
        FileInfo fileInfo = getFileFolderService().create(
                nodeRef,
                name,
                ContentModel.TYPE_CONTENT);
        NodeRef fileNodeRef = fileInfo.getNodeRef();
        ContentWriter writer = getFileFolderService().getWriter(fileNodeRef);
        getGeneralService().writeFile(writer, file, name, mimeType);

        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(FileModel.Props.DISPLAY_NAME, displayName);

        associatedWithMetaData = addFilePropsAndUpdateDocumentMetadata(nodeRef, fileNodeRef, associatedWithMetaData, active, writer.getMimetype(), props, name);

        return Pair.newInstance(fileNodeRef, associatedWithMetaData);
    }

    @Override
    public NodeRef addFile(String name, String displayName, NodeRef parentNodeRef, ContentReader reader) {
        FileInfo fileInfo = getFileFolderService().create(
                parentNodeRef,
                name,
                ContentModel.TYPE_CONTENT);
        NodeRef fileNodeRef = fileInfo.getNodeRef();
        ContentWriter writer = getFileFolderService().getWriter(fileNodeRef);
        writer.setEncoding(reader.getEncoding());
        writer.setMimetype(reader.getMimetype());
        writer.putContent(reader.getContentInputStream());
        getNodeService().setProperty(fileNodeRef, FileModel.Props.DISPLAY_NAME, displayName);
        return fileNodeRef;
    }

    private void addDocumentFileVersionAndLog(String displayName, NodeRef documentNodeRef, NodeRef fileNodeRef) {
        getVersionsService().addVersionModifiedAspect(fileNodeRef);
        getVersionsService().addVersionLockableAspect(fileNodeRef);
        documentLogService.addDocumentLog(documentNodeRef, MessageUtil.getMessage("document_log_status_fileAdded", displayName));
    }

    @Override
    public void removePreviousParentReference(NodeRef docRef, boolean moveToPreviousParent) {
        List<FileInfo> listFiles = getFileFolderService().listFiles(docRef);
        for (FileInfo fileInfo : listFiles) {
            String parentRefString = (String) fileInfo.getProperties().get(FileModel.Props.PREVIOUS_FILE_PARENT);
            if (StringUtils.isBlank(parentRefString)) {
                continue;
            }

            NodeRef previousParent = new NodeRef(parentRefString);
            if (!getNodeService().exists(previousParent)) {
                continue;
            }

            NodeRef fileRef = fileInfo.getNodeRef();
            // Move node back to previous parent and remove property if still present.
            if (moveToPreviousParent) {
                fileRef = getNodeService().moveNode(fileRef, previousParent, ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS).getChildRef();
            }
            getNodeService().removeProperty(fileRef, FileModel.Props.PREVIOUS_FILE_PARENT);
        }
    }

    @Override
    public List<File> getScannedFolders() {
        if (log.isDebugEnabled()) {
            log.debug("Getting scanned files");
        }
        NodeRef scannedNodeRef = getGeneralService().getNodeRef(scannedFilesPath);
        Assert.notNull(scannedNodeRef, "Scanned files node reference not found");
        List<FileInfo> fileInfos = getFileFolderService().listFolders(scannedNodeRef);
        List<File> files = new ArrayList<File>(fileInfos.size());
        for (FileInfo fileInfo : fileInfos) {
            final File file = createFile(fileInfo);
            file.setName(userService.getUserFullName(file.getName())); // real folder names are userNames - replace them with user full name
            final int nrOfChildren = getNodeService().getChildAssocs(file.getNodeRef(), ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL).size();
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
        NodeRef nodeRef = item.getNodeRef();
        item.setDownloadUrl(generateURL(nodeRef));
        item.setReadOnlyUrl(DownloadContentServlet.generateDownloadURL(nodeRef, item.getDisplayName()));

        String lockOwnerIfLocked = getDocLockService().getLockOwnerIfLocked(nodeRef);
        if (lockOwnerIfLocked != null && Boolean.TRUE.equals(getNodeService().getProperty(nodeRef, FileModel.Props.MANUAL_LOCK))) {
            item.setActiveLockOwner(StringUtils.substringBefore(lockOwnerIfLocked, "_")); // Store only user name part, to enable actions in different sessions.
        }

        return item;
    }

    @Override
    public File getFile(NodeRef nodeRef) {
        Assert.notNull(nodeRef);

        FileInfo fi = getFileFolderService().getFileInfo(nodeRef);
        Assert.notNull(fi);
        return createFile(fi);
    }

    @Override
    public String generateURL(NodeRef nodeRef) {
        String runAsUser = AuthenticationUtil.getRunAsUser();
        String name = getFileFolderService().getFileInfo(nodeRef).getName();
        NodeRef primaryParentRef = getNodeService().getPrimaryParent(nodeRef).getParentRef();
        boolean isUnderDocument = DocumentCommonModel.Types.DOCUMENT.equals(getNodeService().getType(primaryParentRef));
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
        List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(folder);
        List<String> childDisplayNames = new ArrayList<String>(childAssocs.size());

        for (ChildAssociationRef caRef : childAssocs) {
            Serializable property = getNodeService().getProperty(caRef.getChildRef(), FileModel.Props.DISPLAY_NAME);
            if (property != null) {
                childDisplayNames.add(property.toString());
            }
        }
        return childDisplayNames;
    }

    @Override
    public void deleteGeneratedFilesByType(NodeRef parentRef, GeneratedFileType type) {
        String typeToDelete = type.name();
        for (FileInfo fi : getFileFolderService().listFiles(parentRef)) {
            String generatedType = (String) fi.getProperties().get(FileModel.Props.GENERATION_TYPE);
            if (StringUtils.isNotBlank(generatedType) && generatedType.equals(typeToDelete)) {
                getFileFolderService().delete(fi.getNodeRef());
                if (log.isDebugEnabled()) {
                    log.info("Deleted file with generationType=" + typeToDelete + " " + fi.getNodeRef());
                }
            }
        }
    }

    @Override
    public List<Subfolder> getSubfolders(NodeRef parentRef, QName childNodeType, QName countableChildNodeType) {
        List<Subfolder> subfolders = new ArrayList<Subfolder>();
        List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(parentRef, Collections.singleton(childNodeType));
        for (ChildAssociationRef childAssocRef : childAssocs) {
            NodeRef childRef = childAssocRef.getChildRef();
            Node folder = new Node(childRef);
            List<ChildAssociationRef> documents = getNodeService().getChildAssocs(childRef, Collections.singleton(countableChildNodeType));
            subfolders.add(new Subfolder(folder, documents == null ? 0 : documents.size()));
        }
        return subfolders;
    }

    @Override
    public NodeRef findSubfolderWithName(NodeRef parentNodeRef, String folderName, QName subfolderType) {
        List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(parentNodeRef, Collections.singleton(subfolderType));
        for (ChildAssociationRef childAssoc : childAssocs) {
            if (childAssoc.getQName().getLocalName().equals(folderName)) {
                return childAssoc.getChildRef();
            }
        }
        return null;
    }

    @Override
    public boolean isFileGenerated(NodeRef fileRef) {
        return fileRef != null && (isFileGeneratedFromTemplate(fileRef) || getNodeService().getProperty(fileRef, FileModel.Props.GENERATION_TYPE) != null);
    }

    @Override
    public boolean isFileGeneratedFromTemplate(NodeRef fileRef) {
        return fileRef != null && getNodeService().getProperty(fileRef, FileModel.Props.GENERATED_FROM_TEMPLATE) != null;
    }

    @Override
    public NodeRef getPreviouslyGeneratedPdf(NodeRef sourceFileRef) {
        if (!getNodeService().exists(sourceFileRef)) {
            return null;
        }

        NodeRef parentRef = getNodeService().getPrimaryParent(sourceFileRef).getParentRef();
        List<FileInfo> listFiles = getFileFolderService().listFiles(parentRef);
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

        Date sourceModified = (Date) getNodeService().getProperty(sourceFileRef, ContentModel.PROP_MODIFIED);
        Date pdfModified = (Date) getNodeService().getProperty(pdfFileRef, ContentModel.PROP_MODIFIED);
        return sourceModified != null && pdfModified != null && sourceModified.before(pdfModified);
    }

    @Override
    public boolean isFileAssociatedWithDocMetadata(NodeRef fileRef) {
        return fileRef != null && Boolean.TRUE.equals(getNodeService().getProperty(fileRef, FileModel.Props.UPDATE_METADATA_IN_FILES));
    }

    // START: getters / setters

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

    public void setDocumentLogService(DocumentLogService documentLogService) {
        this.documentLogService = documentLogService;
    }

    public void setScannedFilesPath(String scannedDocumentsPath) {
        scannedFilesPath = scannedDocumentsPath;
    }

    public DocLockService getDocLockService() {
        if (_docLockService == null) {
            _docLockService = BeanHelper.getDocLockService();
        }
        return _docLockService;
    }

    public DocumentDynamicService getDocumentDynamicService() {
        if (_documentDynamicService == null) {
            _documentDynamicService = BeanHelper.getDocumentDynamicService();
        }
        return _documentDynamicService;
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

    @Override
    public String getJumploaderPath() {
        return jumploaderPath;
    }

    public void setJumploaderPath(String jumploaderPath) {
        this.jumploaderPath = jumploaderPath;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    // END: getters / setters

}
