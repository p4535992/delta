package ee.webmedia.alfresco.document.file.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.GeneratedFileType;
import ee.webmedia.alfresco.document.file.web.Subfolder;

public interface FileService {

    String BEAN_NAME = "FileService";

    /**
     * Marks the file as active/inactive
     *
     * @param nodeRef
     * @return true, if new status is active, false otherwise
     */
    boolean toggleActive(NodeRef nodeRef);

    /**
     * Returns the Dec container from document files. If not available, null is returned.
     *
     * @param documentNodeRef document that contains the container file
     * @return container node ref or null
     * @see {@link ee.webmedia.alfresco.dvk.service.DvkService#DEC_CONTAINER_FILE_NAME}
     */
    NodeRef getDecContainer(NodeRef documentNodeRef);

    /**
     * Removes the Dec container from document files if possible.
     *
     * @param documentNodeRef document that contains the container file
     * @return true if removed, false oterwise
     */
    boolean removeDecContainer(NodeRef documentNodeRef);

    /**
     * Returns all children of this nodeRef as file items.
     *
     * @return
     */
    List<File> getAllFiles(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return list of all files without dec containers (including digidoc container, but without digidocitems of container)
     */
    List<File> getAllFilesExcludingDigidocSubitems(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return list of all files(including dec containers and digidoc container, but without digidocitems of container)
     */
    List<File> getAllFilesExcludingDigidocSubitemsAndIncludingDecContainers(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return
     */
    File getFile(NodeRef nodeRef);

    /**
     * Calculatees WebDAV URL for given node
     *
     * @param nodeRef
     * @return
     */
    String generateURL(NodeRef nodeRef);

    /**
     * Move all files under fromRef to (under) toRef.
     *
     * @param fromRef
     * @param toRef
     * @throws FileNotFoundException
     */
    void moveAllFiles(NodeRef fromRef, NodeRef toRef) throws FileNotFoundException;

    Pair<Boolean, NodeRef> addExistingFileToDocument(String name, String displayName, NodeRef documentNodeRef, NodeRef fileNodeRef, boolean active, boolean associatedWithMetaData,
            Long fileOrderInList);

    Pair<Boolean, NodeRef> addUploadedFileToDocument(String name, String displayName, NodeRef documentNodeRef, java.io.File file, String mimeType, boolean active,
            boolean associatedWithMetaData, Long fileOrderInList);

    NodeRef addFileToTask(String name, String displayName, NodeRef taskNodeRef, java.io.File file, String mimeType);

    NodeRef addFile(String name, String displayName, NodeRef taskNodeRef, ContentReader reader);

    List<File> getScannedFolders();

    /**
     * Gets list of scanned files.
     *
     * @param folderRef
     * @return list of scanned files
     */
    List<File> getScannedFiles(NodeRef folderRef);

    /**
     * Transforms all the active files under nodeRef into PDF.
     * The created PDF-files are saved under the same nodeRef and are active.
     * The original files, when transformed successfully, become inactive.
     *
     * @param nodeRef
     */
    void transformActiveFilesToPdf(NodeRef nodeRef, boolean inactivateOriginalFiles);

    FileInfo transformToPdf(NodeRef docRef, NodeRef fileRef, boolean createVersion);

    /**
     * Transform a source file to PDF. Original source file is preserved and a new PDF file is created.
     *
     * @param parent folder where PDF file is created
     * @param reader source file that is converted to PDF
     * @param filename the name that the created PDF file will have
     * @param displayName the name to be displayed in UI
     * @param overwritableNodeRef if not null, then the contents are written to this location (NB! doesn't create version!)
     * @return created PDF file. If transformation was not possible or failed, returns {@code null}.
     */
    FileInfo transformToPdf(NodeRef parent, NodeRef fileRef, ContentReader reader, String filename, String displayName, NodeRef overwritableNodeRef);

    /**
     * @param nodeRef
     * @return list of all active files
     */
    List<File> getAllActiveFiles(NodeRef nodeRef);

    List<NodeRef> getAllFileRefs(NodeRef nodeRef, boolean activeFilesOnly);

    List<NodeRef> getAllFileRefsExcludingDecContainer(NodeRef nodeRef);

    List<File> getAllActiveAndInactiveFiles(NodeRef nodeRef);

    /** Get all active files, excluding the ones that are sources for generated pdfs */
    List<NodeRef> getAllActiveFilesForDdoc(NodeRef nodeRef);

    void setAllFilesInactiveExcept(NodeRef parent, NodeRef activeFile);

    String getUniqueFileDisplayName(NodeRef folder, String displayName);

    void deleteGeneratedFilesByType(NodeRef parentRef, GeneratedFileType type);

    boolean isFileGenerated(NodeRef fileRef);

    boolean isFileGeneratedFromTemplate(NodeRef fileRef);

    boolean isFileAssociatedWithDocMetadata(NodeRef fileRef);

    List<String> getDocumentFileDisplayNames(NodeRef folder);

    /**
     * @param parentRef
     * @param subfolderNodeType - type to get as subfolder
     * @param countableChildNodeType - type for counting non-subfolder children
     * @param countChildren
     * @return
     */
    List<Subfolder> getSubfolders(NodeRef parentRef, QName subfolderNodeType, QName countableChildNodeType, boolean countChildren);

    NodeRef findSubfolderWithName(NodeRef parentNodeRef, String folderName, QName subfolderType);

    boolean isTransformableToPdf(String mimeType);

    NodeRef getPreviouslyGeneratedPdf(NodeRef sourceFileRef);

    boolean isPdfUpToDate(NodeRef sourceFileRef, NodeRef pdfFileRef);

    List<File> getFiles(List<NodeRef> taskFileNodeRefs);

    InputStream getFileContentInputStream(NodeRef fileRef);

    void removePreviousParentReference(NodeRef docRef, boolean moveToPreviousParent);

    void reorderFiles(NodeRef documentRef);

    void reorderFiles(List<NodeRef> documentRefs);

    void reorderFiles(NodeRef doocumentRef, Map<NodeRef, Long> originalOrders);

    /**
     * @param documentRef
     * @param fileRef - ref of file that must be added to the end of the list
     * @param active - previous state of the file
     */
    void reorderFiles(NodeRef documentRef, NodeRef fileRef, boolean active);

}
