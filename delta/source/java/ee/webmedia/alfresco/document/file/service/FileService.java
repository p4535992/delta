package ee.webmedia.alfresco.document.file.service;

import java.util.List;

import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.document.file.model.GeneratedFileType;

/**
 * @author Dmitri Melnikov
 */
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

    NodeRef addFileToDocument(String name, String displayName, NodeRef documentNodeRef, java.io.File file, String mimeType);

    List<File> getScannedFolders();

    List<File> getAllScannedFiles();

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
    void transformActiveFilesToPdf(NodeRef nodeRef);

    FileInfo transformToPdf(NodeRef nodeRef);

    /**
     * Transform a source file to PDF. Original source file is preserved and a new PDF file is created.
     * 
     * @param parent folder where PDF file is created
     * @param reader source file that is converted to PDF
     * @param filename the name that the created PDF file will have
     * @param displayName the name to be displayed in UI
     * @return created PDF file. If transformation was not possible or failed, returns {@code null}.
     */
    FileInfo transformToPdf(NodeRef parent, ContentReader reader, String filename, String displayName);

    /**
     * @param nodeRef
     * @return list of all active files
     */
    List<File> getAllActiveFiles(NodeRef nodeRef);

    List<NodeRef> getAllActiveFilesNodeRefs(NodeRef nodeRef);

    void setAllFilesInactiveExcept(NodeRef parent, NodeRef activeFile);

    String getUniqueFileDisplayName(NodeRef folder, String displayName);

    void deleteGeneratedFilesByType(NodeRef parentRef, GeneratedFileType type);

}
