package ee.webmedia.alfresco.document.type.service;

import java.util.Collection;
import java.util.List;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.type.model.DocumentType;

/**
 * @author Alar Kvell
 */
public interface DocumentTypeService {
	
    String BEAN_NAME = "DocumentTypeService";

    /**
     * @return all document type objects from repository
     */
    List<DocumentType> getAllDocumentTypes();

    /**
     * @param used
     * @return all document type objects from repository
     */
    List<DocumentType> getAllDocumentTypes(boolean used);

    /**
     * Update properties for document types. All properties except {@code id} and {@code name} are written.
     * @param parameters - document types to be updated to the repository
     */
    void updateDocumentTypes(Collection<DocumentType> documentTypes);

    /**
     * Returns DocumentType from repository
     * @param documentTypeId - childName of the node 
     * @return documentType
     */
    DocumentType getDocumentType(QName documentTypeId);

    /**
     * Returns DocumentType from repository
     * @param documentTypeId - childName of the node 
     * @return documentType
     */
    DocumentType getDocumentType(String documentTypeId);

}
