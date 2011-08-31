package ee.webmedia.alfresco.docadmin.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * @author Ats Uiboupin
 */
public interface DocumentAdminService {

    String BEAN_NAME = "DocumentAdminService";

    /**
     * @return all document type objects from repository
     */
    List<DocumentType> getDocumentTypes();

    /**
     * @param used
     * @return all document type objects from repository with used property equal to argument
     */
    List<DocumentType> getDocumentTypes(boolean used);

    DocumentType getDocumentType(String id);

    DocumentType getDocumentType(NodeRef docTypeRef);

    /**
     * Update properties or save new document type.
     * 
     * @param documentType - document type to be saved or updated to the repository
     * @return saved instance that was first created by cloning given object
     */
    DocumentType saveOrUpdateDocumentType(DocumentType docType);

    DocumentType createNewUnSaved();

    FieldDefinition createNewUnSavedFieldDefinition();

    void deleteDocumentType(NodeRef docTypeRef);

    <F extends Field> F saveOrUpdateField(F originalFieldDef);

    List<FieldDefinition> saveOrUpdateFieldDefinitions(List<FieldDefinition> fieldDefinitions);

    List<FieldDefinition> getFieldDefinitions();

    FieldDefinition getFieldDefinition(QName fieldId);

    Field getField(NodeRef fieldDefRef);

    void deleteFieldDefinition(NodeRef fieldDefRef);

    // TODO DLSeadist (Alar): temporary solution until Ats finishes proper support for this
    void addSystematicMetadataItems(DocumentTypeVersion docVer);

}
