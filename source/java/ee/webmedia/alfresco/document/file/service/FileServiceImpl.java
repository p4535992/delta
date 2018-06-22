package ee.webmedia.alfresco.document.file.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getFileFolderService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getVersionsService;
import static ee.webmedia.alfresco.utils.MimeUtil.isPdf;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.CreateObjectCallback;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.web.ListReorderHelper;
import ee.webmedia.alfresco.docadmin.web.ListReorderHelper.OrderModifier;
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
import ee.webmedia.alfresco.signature.service.DigiDoc4JSignatureService;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.MimeUtil;
import ee.webmedia.alfresco.utils.Transformer;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class FileServiceImpl implements FileService {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(FileServiceImpl.class);

    private UserService userService;
    private DigiDoc4JSignatureService digiDoc4JSignatureService;
    private AuthenticationService authenticationService;
    private NodeService nodeService;
    private ContentService contentService;
    private DocumentLogService documentLogService;
    private DocumentDynamicService _documentDynamicService;
    private DocumentTemplateService _documentTemplateService;
    private DocLockService _docLockService;
    private BulkLoadNodeService bulkLoadNodeService;
    private Set<String> openOfficeFiles;

    @Override
    public InputStream getFileContentInputStream(NodeRef fileRef) {
        ContentReader reader = getFileFolderService().getReader(fileRef);
        return reader.getContentInputStream();
    }

    @Override
    public boolean toggleActive(NodeRef fileRef) {
        boolean active = true; // If file doesn't have the flag set, then it hasn't been toggled yet, thus active
        if (!nodeService.exists(fileRef)) {
            throw new UnableToPerformException("file_toggle_failed");
        }
        if (nodeService.getProperty(fileRef, FileModel.Props.ACTIVE) != null) {
            active = Boolean.parseBoolean(nodeService.getProperty(fileRef, FileModel.Props.ACTIVE).toString());
        }
        active = !active;
        nodeService.setProperty(fileRef, FileModel.Props.ACTIVE, active);
        NodeRef documentRef = nodeService.getPrimaryParent(fileRef).getParentRef();
        getGeneralService().setModifiedToNow(documentRef);
        return active;
    }

    @Override
    public void reorderFiles(NodeRef documentRef) {
        reorderFiles(documentRef, null, true);
    }

    @Override
    public void reorderFiles(List<NodeRef> documentRefs) {
        if (CollectionUtils.isEmpty(documentRefs)) {
            return;
        }
        for (NodeRef docRef : documentRefs) {
            reorderFiles(docRef);
        }
    }

    @Override
    public void reorderFiles(NodeRef documentRef, Map<NodeRef, Long> originalOrders) {
        List<File> files = getAllFilesExcludingDigidocSubitems(documentRef);
        List<File> activeFiles = new ArrayList<>();
        List<File> inacvtiveFiles = new ArrayList<>();

        for (File f : files) {
            if (f.isActiveAndNotDigiDoc()) {
                activeFiles.add(f);
            } else if (f.isNotActiveAndNotDigiDoc()) {
                inacvtiveFiles.add(f);
            }
        }

        FileOrderModifier modifier = new FileOrderModifier(originalOrders);
        ListReorderHelper.reorder(activeFiles, modifier);
        ListReorderHelper.reorder(inacvtiveFiles, modifier);

        updateOrder(activeFiles, originalOrders);
        updateOrder(inacvtiveFiles, originalOrders);
    }

    @Override
    public void reorderFiles(NodeRef documentRef, NodeRef fileRef, boolean active) {
        List<File> files = getAllFilesExcludingDigidocSubitems(documentRef);
        List<File> activeFiles = new ArrayList<>();
        List<File> inacvtiveFiles = new ArrayList<>();

        File toggledFile = null;
        Map<NodeRef, Long> initialOrders = new HashMap<>();
        for (File f : files) {
            initialOrders.put(getFileNodeRef(f), f.getFileOrderInList());
            if (toggledFile == null && fileRef != null && fileRef.equals(getFileNodeRef(f))) {
                toggledFile = f;
            }
            if (f.isActiveAndNotDigiDoc()) {
                activeFiles.add(f);
            } else if (f.isNotActiveAndNotDigiDoc()) {
                inacvtiveFiles.add(f);
            }
        }

        if (toggledFile != null) {
            Long newOrderNr = new Long(active ? inacvtiveFiles.size() + 1 : activeFiles.size() + 1);
            toggledFile.setFileOrderInList(newOrderNr);
        }

        FileOrderModifier modifier = new FileOrderModifier(initialOrders);
        ListReorderHelper.reorder(activeFiles, modifier);
        ListReorderHelper.reorder(inacvtiveFiles, modifier);

        updateOrder(activeFiles, initialOrders);
        updateOrder(inacvtiveFiles, initialOrders);
    }

    private static class FileOrderModifier implements OrderModifier<File, Long> {

        private final Map<NodeRef, Long> initialOrders;

        private FileOrderModifier(Map<NodeRef, Long> initialOrders) {
            this.initialOrders = initialOrders;
        }

        @Override
        public Long getOrder(File object) {
            return object.getFileOrderInList();
        }

        @Override
        public void setOrder(File object, Long previousMaxField) {
            object.setFileOrderInList(LONG_INCREMENT_STRATEGY.tr(previousMaxField));
        }

        @Override
        public Long getOriginalOrder(File file) {
            Long initialOrder = initialOrders != null ? initialOrders.get(getFileNodeRef(file)) : null;
            if (initialOrder == null) {
                initialOrder = file.getFileOrderInList();
                if (initialOrder == null) {
                    initialOrder = Long.MAX_VALUE;
                }
            }
            return initialOrder;
        }

        private static final Transformer<Long, Long> LONG_INCREMENT_STRATEGY = new Transformer<Long, Long>() {
            @Override
            public Long tr(Long previousMaxField) {
                return previousMaxField == null ? 1 : previousMaxField + 1;
            }
        };
    }

    private void updateOrder(List<File> files, Map<NodeRef, Long> initialOrders) {
        for (File f : files) {
            //Long previousOrderNr = initialOrders.get(getFileNodeRef(f));
            //if (previousOrderNr == null || !previousOrderNr.equals(f.getFileOrderInList())) {
            	//String nodeRefstr = getFileNodeRef(f).toString();
            	//long ord = f.getFileOrderInList();
                nodeService.setProperty(getFileNodeRef(f), FileModel.Props.FILE_ORDER_IN_LIST, f.getFileOrderInList());
            //}
        }
    }

    private static NodeRef getFileNodeRef(File f) {
        NodeRef ref = f.getNodeRef();
        if (ref == null) {
            ref = f.getNode() != null ? f.getNode().getNodeRef() : null;
        }
        return ref;
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
            log.debug("Filename: " + fi.getName());
            final File item = createFile(fi);

            // Exclude DEC containers
            if (excludeDecContainers && item.isDecContainer()) {
                continue;
            }

            boolean isDdoc = FilenameUtil.isDigiDocContainerFile(fi);
            final boolean isBdoc = FilenameUtil.isBdocFile(fi.getName());
            item.setDigiDocContainer(isDdoc);
            item.setBdoc(isBdoc);
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
                            return digiDoc4JSignatureService.getDataItemsAndSignatureItems(item.getNodeRef(), false);
                        }
                    }, AuthenticationUtil.getSystemUserName());
                    item2.setNode(item.getNode()); // Digidoc item uses node of its container for permission evaluations
                    item2.setDdocItems(ddocItems);
                    item2.setDigiDocItem(true);
                    item2.setFileOrderInList(item.getFileOrderInList()); // needed to place "item2" after "item" after sorting
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
                if (!Boolean.FALSE.equals(nodeService.getProperty(fileRef, FileModel.Props.ACTIVE)) && nodeService.getProperty(fileRef, DvkModel.Props.DVK_ID) == null) {
                    activeFileRefs.add(fileRef);
                }
            }
            return activeFileRefs;
        }
        return fileRefs;
    }

    @Override
    public List<NodeRef> getAllFileRefsExcludingDecContainer(NodeRef nodeRef) {
        List<NodeRef> fileRefs = getFileFolderService().listFileRefs(nodeRef);
        if (CollectionUtils.isNotEmpty(fileRefs)) {
            NodeRef decContainer = getDecContainer(nodeRef);
            if (decContainer == null) {
                return fileRefs;
            }
            fileRefs.remove(decContainer);
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
        reorderFiles(parent);
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
                if (nodeService.exists(generatedFileRef)) {
                    nodeService.deleteNode(generatedFileRef);
                    deletedFiles.add(generatedFileRef);
                    if (log.isDebugEnabled()) {
                        log.debug("deleted generated file, nodeRef=" + generatedFileRef);
                    }
                }
                // note that file is not deleted completely, but moves to trashcan,
                // so we cannot count on Alfresco functionality that sets nodeRef properties to null when node is deleted
                nodeService.setProperty(file.getNodeRef(), FileModel.Props.GENERATED_FILE, null);
                if (log.isDebugEnabled()) {
                    log.debug("set generated fileRef=null");
                }
            }
            FileInfo generatePdf = file.getConvertToPdfIfSignedFromProps() ? transformToPdf(document, fileRef, false) : null;
            if (generatePdf != null) {
                NodeRef pdfNodeRef = generatePdf.getNodeRef();
                nodeService.setProperty(pdfNodeRef
                        , FileModel.Props.GENERATION_TYPE, GeneratedFileType.SIGNED_PDF.name());
                nodeService.setProperty(file.getNodeRef(), FileModel.Props.GENERATED_FILE, pdfNodeRef);
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
        reorderFiles(document);
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
            String name = getGeneralService().getUniqueFileName(parent, FilenameUtils.removeExtension(filename) + ".pdf");
            result = getFileFolderService().create(parent, name, ContentModel.TYPE_CONTENT);
            nodeService.setProperty(result.getNodeRef(), ContentModel.PROP_CONTENT, writer.getContentData());
            displayName = FilenameUtils.removeExtension(displayName);
            displayName += ".pdf";
            nodeService.setProperty(result.getNodeRef(), FileModel.Props.DISPLAY_NAME, getUniqueFileDisplayName(parent, displayName));
        } else {
            result = getFileFolderService().getFileInfo(overwritableNodeRef);
        }
        nodeService.setProperty(result.getNodeRef(), FileModel.Props.PDF_GENERATED_FROM_FILE, fileRef);
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
    public Pair<Boolean, NodeRef> addExistingFileToDocument(final String name, final String displayName, final NodeRef documentNodeRef, final NodeRef fileNodeRef, boolean active,
            boolean associatedWithMetaData, final Long fileOrderInList) {
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
                props.put(FileModel.Props.PREVIOUS_FILE_PARENT, nodeService.getPrimaryParent(fileNodeRef).getParentRef().toString());
                props.put(FileModel.Props.FILE_ORDER_IN_LIST, fileOrderInList);
                nodeService.addProperties(fileNodeRef, props);

                // move file
                return nodeService.moveNode(fileNodeRef, documentNodeRef,
                        ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS).getChildRef();
            }
        }, AuthenticationUtil.getSystemUserName());

        associatedWithMetaData = addFilePropsAndUpdateDocumentMetadata(documentNodeRef, movedFileNodeRef, associatedWithMetaData, active,
                getFileFolderService().getWriter(movedFileNodeRef)
                .getMimetype(), null, name);

        addDocumentFileVersionAndLog(displayName, documentNodeRef, movedFileNodeRef);
        return Pair.newInstance(associatedWithMetaData, movedFileNodeRef);
    }

    @Override
    public Pair<Boolean, NodeRef> addUploadedFileToDocument(String name, String displayName, NodeRef documentNodeRef, java.io.File file, String mimeType, boolean active,
            boolean associatedWithMetaData, Long fileOrderInList) {
        Pair<NodeRef, Boolean> fileNodeRefAndHasFormulae = addFile(name, displayName, documentNodeRef, file, mimeType, active, associatedWithMetaData);
        NodeRef fileRef = fileNodeRefAndHasFormulae.getFirst();
        nodeService.setProperty(fileRef, FileModel.Props.FILE_ORDER_IN_LIST, fileOrderInList);
        addDocumentFileVersionAndLog(displayName, documentNodeRef, fileNodeRefAndHasFormulae.getFirst());
        return Pair.newInstance(fileNodeRefAndHasFormulae.getSecond(), fileRef);
    }

    @Override
    public NodeRef addFileToTask(String name, String displayName, NodeRef nodeRef, java.io.File file, String mimeType) {
        return addFile(name, displayName, nodeRef, file, mimeType, true, false).getFirst();
    }

    private boolean addFilePropsAndUpdateDocumentMetadata(final NodeRef documentNodeRef, final NodeRef fileNodeRef, boolean associatedWithMetaData, boolean active,
            String mimetype, Map<QName, Serializable> props, String filename) {
        if (props == null) {
            props = new HashMap<>(7);
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
        nodeService.addProperties(fileNodeRef, props);
        if (associatedWithMetaData) {
            associatedWithMetaData = getDocumentDynamicService().updateDocumentAndGeneratedFiles(fileNodeRef, documentNodeRef, false);
            // if document contained no Delta formulae, remove association between document metadata and file
            if (!associatedWithMetaData) {
                nodeService.setProperty(fileNodeRef, FileModel.Props.UPDATE_METADATA_IN_FILES, Boolean.FALSE);
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
        nodeService.setProperty(fileNodeRef, FileModel.Props.DISPLAY_NAME, displayName);
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
        boolean moveFileToPreviousParent = moveToPreviousParent && CollectionUtils.isNotEmpty(listFiles) && BeanHelper.getDocumentDynamicService().isDraft(docRef);
        for (FileInfo fileInfo : listFiles) {
            String parentRefString = (String) fileInfo.getProperties().get(FileModel.Props.PREVIOUS_FILE_PARENT);
            if (StringUtils.isBlank(parentRefString)) {
                continue;
            }

            NodeRef previousParent = new NodeRef(parentRefString);
            if (!nodeService.exists(previousParent)) {
                continue;
            }

            NodeRef fileRef = fileInfo.getNodeRef();
            // Move node back to previous parent and remove property if still present.
            if (moveFileToPreviousParent) {
                fileRef = nodeService.moveNode(fileRef, previousParent, ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS).getChildRef();
            }
            nodeService.removeProperty(fileRef, FileModel.Props.PREVIOUS_FILE_PARENT);
        }
    }

    @Override
    public List<File> getScannedFolders() {
        if (log.isDebugEnabled()) {
            log.debug("Getting scanned files");
        }
        NodeRef scannedNodeRef = BeanHelper.getConstantNodeRefsBean().getScannedFilesRoot();
        Assert.notNull(scannedNodeRef, "Scanned files node reference not found");
        List<FileInfo> fileInfos = getFileFolderService().listFolders(scannedNodeRef);
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
        String fileModifier = (String) fi.getProperties().get(ContentModel.PROP_MODIFIER);
        log.debug("FILE modifier username: " + fileModifier);
        String fileModifierUserFullName = userService.getUserFullName(fileModifier);
        log.debug("FILE modifier userFullName: " + fileModifierUserFullName);
        item.setModifier(fileModifierUserFullName);
        NodeRef nodeRef = item.getNodeRef();
        item.setDownloadUrl(generateURL(nodeRef));
        item.setReadOnlyUrl(DownloadContentServlet.generateDownloadURL(nodeRef, item.getDisplayName()));

        String lockOwnerIfLocked = getDocLockService().getLockOwnerIfLocked(nodeRef);
        if (lockOwnerIfLocked != null && Boolean.TRUE.equals(nodeService.getProperty(nodeRef, FileModel.Props.MANUAL_LOCK))) {
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
        NodeRef primaryParentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
        boolean isUnderDocument = DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(primaryParentRef));
        if (!nodeRef.getStoreRef().getProtocol().equals(StoreRef.PROTOCOL_WORKSPACE) || StringUtils.isBlank(runAsUser) || AuthenticationUtil.isRunAsUserTheSystemUser()
                || !isUnderDocument) {
            return DownloadContentServlet.generateDownloadURL(nodeRef, name);
        }

        StringBuilder path = new StringBuilder();
        if (isOpenOfficeFile(name)) {
            path.append("vnd.sun.star.webdav://").append(getDocumentTemplateService().getServerUrl().replaceFirst(".*://", "").replace("6443", "6080")); // FIXME
        }
        // calculate a WebDAV URL for the given node
        path.append("/").append(WebDAVServlet.WEBDAV_PREFIX);

        // authentication ticket
        String ticket = authenticationService.getCurrentTicket();
        if (ticket.startsWith(InMemoryTicketComponentImpl.GRANTED_AUTHORITY_TICKET_PREFIX)) {
            ticket = ticket.substring(InMemoryTicketComponentImpl.GRANTED_AUTHORITY_TICKET_PREFIX.length());
        }
        path.append("/").append(URLEncoder.encode(ticket));

        path.append("/").append(URLEncoder.encode(runAsUser)); // maybe substituting

        NodeRef parent = primaryParentRef;
        path.append("/").append(URLEncoder.encode(parent.getId()));

        String filename = URLEncoder.encode(name);
        path.append("/").append(filename);

        return path.toString();
    }

    private boolean isOpenOfficeFile(String name) {
        String fileExtension = FilenameUtils.getExtension(name);
        String userSettings = (String) userService.getUserProperties(AuthenticationUtil.getFullyAuthenticatedUser()).get(ContentModel.PROP_OPEN_OFFICE_CLIENT_EXTENSIONS);

        if (StringUtils.isBlank(userSettings)) {
            return openOfficeFiles.contains(fileExtension);
        }

        return FilenameUtil.getFileExtensionsFromCommaSeparated(userSettings).contains(fileExtension);
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
        for (FileInfo fi : getFileFolderService().listFiles(parentRef)) {
            String generatedType = (String) fi.getProperties().get(FileModel.Props.GENERATION_TYPE);
            if (StringUtils.isNotBlank(generatedType) && generatedType.equals(typeToDelete)) {
                getFileFolderService().delete(fi.getNodeRef());
                if (log.isDebugEnabled()) {
                    log.info("Deleted file with generationType=" + typeToDelete + " " + fi.getNodeRef());
                }
            }
        }
        reorderFiles(parentRef);
    }

    @Override
    public List<Subfolder> getSubfolders(NodeRef parentRef, final QName childNodeType, QName countableChildNodeType, boolean countChildren) {
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        Map<NodeRef, List<WmNode>> allSubfolders = bulkLoadNodeService.loadChildNodes(Collections.singletonList(parentRef), null, childNodeType, propertyTypes,
                new CreateObjectCallback<WmNode>() {

                    @Override
                    public WmNode create(NodeRef nodeRef, Map<QName, Serializable> properties) {
                        return new WmNode(nodeRef, childNodeType, null, properties);
                    }
                });
        if (allSubfolders == null || allSubfolders.isEmpty()) {
            return new ArrayList<>();
        }
        List<WmNode> subfolderNodes = allSubfolders.get(parentRef);
        List<NodeRef> subfolderRefs = new ArrayList<>();
        for (WmNode subfolder : subfolderNodes) {
            subfolderRefs.add(subfolder.getNodeRef());
        }
        Map<NodeRef, Integer> childCounts = new HashMap<>();
        if (countChildren) {
            childCounts = bulkLoadNodeService.countChildNodes(subfolderRefs, countableChildNodeType);
        }
        List<Subfolder> subfolders = new ArrayList<>();
        for (WmNode subfolderNode : subfolderNodes) {
            Integer count = childCounts.get(subfolderNode.getNodeRef());
            subfolders.add(new Subfolder(subfolderNode, count != null ? count.intValue() : 0));
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

        Date sourceModified = (Date) nodeService.getProperty(sourceFileRef, ContentModel.PROP_MODIFIED);
        Date pdfModified = (Date) nodeService.getProperty(pdfFileRef, ContentModel.PROP_MODIFIED);
        return sourceModified != null && pdfModified != null && sourceModified.before(pdfModified);
    }

    @Override
    public boolean isFileAssociatedWithDocMetadata(NodeRef fileRef) {
        return fileRef != null && Boolean.TRUE.equals(nodeService.getProperty(fileRef, FileModel.Props.UPDATE_METADATA_IN_FILES));
    }

    // START: getters / setters

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDigiDoc4JSignatureService(DigiDoc4JSignatureService digiDoc4JSignatureService) {
        this.digiDoc4JSignatureService = digiDoc4JSignatureService;
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
        this.openOfficeFiles = FilenameUtil.getFileExtensionsFromCommaSeparated(openOfficeFiles);
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

    // END: getters / setters

}
