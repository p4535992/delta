package ee.webmedia.alfresco.document.file.service;

<<<<<<< HEAD
import java.io.InputStream;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.util.List;

import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.GeneratedFileType;
import ee.webmedia.alfresco.document.file.web.Subfolder;

<<<<<<< HEAD
/**
 * @author Dmitri Melnikov
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
     * Returns all children of this nodeRef as file items.
     * 
     * @return
     */
    List<File> getAllFiles(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return list of all files(including digidoc container, but witout digidocitems of container)
     */
    List<File> getAllFilesExcludingDigidocSubitems(NodeRef nodeRef);

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

    /**
     * Adds file to given document.
     * 
     * @param name New name of the file
     * @param fileNodeRef Reference to file node
     * @param documentNodeRef Reference to document node
     * @return Reference to created node/file
     */
    NodeRef addFileToDocument(String name, String displayName, NodeRef documentNodeRef, NodeRef fileNodeRef);

<<<<<<< HEAD
    NodeRef addFileToDocument(String name, String displayName, NodeRef documentNodeRef, NodeRef fileNodeRef, boolean active, boolean associatedWithMetaData);

    NodeRef addFileToDocument(String name, String displayName, NodeRef documentNodeRef, java.io.File file, String mimeType);

    NodeRef addFileToDocument(String name, String displayName, NodeRef documentNodeRef, java.io.File file, String mimeType, boolean active, boolean associatedWithMetaData);

    NodeRef addFile(String name, String displayName, NodeRef taskNodeRef, java.io.File file, String mimeType);

    NodeRef addFile(String name, String displayName, NodeRef taskNodeRef, java.io.File file, String mimeType, boolean active, boolean associatedWithMetaData);
=======
    NodeRef addFileToDocument(String name, String displayName, NodeRef documentNodeRef, NodeRef fileNodeRef, boolean active);

    NodeRef addFileToDocument(String name, String displayName, NodeRef documentNodeRef, java.io.File file, String mimeType);

    NodeRef addFileToDocument(String name, String displayName, NodeRef documentNodeRef, java.io.File file, String mimeType, boolean active);

    NodeRef addFile(String name, String displayName, NodeRef taskNodeRef, java.io.File file, String mimeType);

    NodeRef addFile(String name, String displayName, NodeRef taskNodeRef, java.io.File file, String mimeType, boolean active);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

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
<<<<<<< HEAD
    void transformActiveFilesToPdf(NodeRef nodeRef, boolean inactivateOriginalFiles);

    FileInfo transformToPdf(NodeRef docRef, NodeRef fileRef, boolean createVersion);
=======
    void transformActiveFilesToPdf(NodeRef nodeRef);

    FileInfo transformToPdf(NodeRef nodeRef);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    /**
     * Transform a source file to PDF. Original source file is preserved and a new PDF file is created.
     * 
     * @param parent folder where PDF file is created
     * @param reader source file that is converted to PDF
     * @param filename the name that the created PDF file will have
     * @param displayName the name to be displayed in UI
<<<<<<< HEAD
     * @param overwritableNodeRef if not null, then the contents are written to this location (NB! doesn't create version!)
     * @return created PDF file. If transformation was not possible or failed, returns {@code null}.
     */
    FileInfo transformToPdf(NodeRef parent, NodeRef fileRef, ContentReader reader, String filename, String displayName, NodeRef overwritableNodeRef);
=======
     * @return created PDF file. If transformation was not possible or failed, returns {@code null}.
     */
    FileInfo transformToPdf(NodeRef parent, ContentReader reader, String filename, String displayName);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    /**
     * @param nodeRef
     * @return list of all active files
     */
    List<File> getAllActiveFiles(NodeRef nodeRef);

<<<<<<< HEAD
    /** Get all active files, excluding the ones that are sources for generated pdfs */
    List<NodeRef> getAllActiveFilesForDdoc(NodeRef nodeRef);
=======
    List<NodeRef> getAllActiveFilesNodeRefs(NodeRef nodeRef);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

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
     * @return
     */
    List<Subfolder> getSubfolders(NodeRef parentRef, QName subfolderNodeType, QName countableChildNodeType);

    NodeRef findSubfolderWithName(NodeRef parentNodeRef, String folderName, QName subfolderType);

<<<<<<< HEAD
    boolean isTransformableToPdf(String mimeType);

    NodeRef getPreviouslyGeneratedPdf(NodeRef sourceFileRef);

    boolean isPdfUpToDate(NodeRef sourceFileRef, NodeRef pdfFileRef);

    List<File> getFiles(List<NodeRef> taskFileNodeRefs);

    InputStream getFileContentInputStream(NodeRef fileRef);

    String getJumploaderPath();
=======
    List<File> getFiles(List<NodeRef> taskFileNodeRefs);

    void removePreviousParentReference(NodeRef docRef, boolean moveToPreviousParent);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

}
