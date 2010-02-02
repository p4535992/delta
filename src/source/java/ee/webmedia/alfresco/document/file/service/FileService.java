package ee.webmedia.alfresco.document.file.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.file.model.File;

/**
 * @author Dmitri Melnikov
 */
public interface FileService {
    
    String BEAN_NAME = "FileService";
    
    /**
     * Returns all children of this nodeRef as file items.
     * @return
     */
    List<File> getAllFiles(NodeRef nodeRef);

    /**
     * @param nodeRef
     * @return list of all files(including digidoc container, but witout digidocitems of container)
     */
    List<File> getAllFilesExcludingDigidocSubitems(NodeRef nodeRef);

    /**
     * 
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

}
