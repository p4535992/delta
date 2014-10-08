<<<<<<< HEAD
package ee.webmedia.alfresco.document.type.service;

import java.util.List;
import java.util.Set;

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
     * @return all document types which are public to ADR
     */
    List<DocumentType> getPublicAdrDocumentTypes();

    /**
     * @return all document types which are public to ADR
     */
    Set<QName> getPublicAdrDocumentTypeQNames();

    /**
     * @param used
     * @return all document type objects from repository
     */
    List<DocumentType> getAllDocumentTypes(boolean used);

    /**
     * Update properties or save new document type.
     * 
     * @param documentType - document type to be saved or updated to the repository
     */
    void saveOrUpdateDocumentType(DocumentType documentType);

    /**
     * Returns DocumentType from repository
     * 
     * @param documentTypeId - childName of the node
     * @return documentType
     */
    DocumentType getDocumentType(QName documentTypeId);

    /**
     * Returns DocumentType from repository
     * 
     * @param documentTypeId - childName of the node
     * @return documentType
     */
    DocumentType getDocumentType(String documentTypeId);

    QName getIncomingLetterType();

    QName getOutgoingLetterType();

    DocumentType createNewUnSavedDocumentType();

}
=======
package ee.webmedia.alfresco.document.type.service;

import java.util.List;
import java.util.Set;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.type.model.DocumentType;

public interface DocumentTypeService {

    String BEAN_NAME = "DocumentTypeService";

    /**
     * @return all document type objects from repository
     */
    List<DocumentType> getAllDocumentTypes();

    /**
     * @return all document types which are public to ADR
     */
    List<DocumentType> getPublicAdrDocumentTypes();

    /**
     * @return all document types which are public to ADR
     */
    Set<QName> getPublicAdrDocumentTypeQNames();

    /**
     * @param used
     * @return all document type objects from repository
     */
    List<DocumentType> getAllDocumentTypes(boolean used);

    /**
     * Update properties or save new document type.
     * 
     * @param documentType - document type to be saved or updated to the repository
     */
    void saveOrUpdateDocumentType(DocumentType documentType);

    /**
     * Returns DocumentType from repository
     * 
     * @param documentTypeId - childName of the node
     * @return documentType
     */
    DocumentType getDocumentType(QName documentTypeId);

    /**
     * Returns DocumentType from repository
     * 
     * @param documentTypeId - childName of the node
     * @return documentType
     */
    DocumentType getDocumentType(String documentTypeId);

    QName getIncomingLetterType();

    QName getOutgoingLetterType();

    DocumentType createNewUnSavedDocumentType();

}
>>>>>>> develop-5.1
