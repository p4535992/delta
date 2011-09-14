package ee.webmedia.alfresco.docadmin.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

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

    String getDocumentTypeName(String documentTypeId);

    Map<String/* docTypeId */, String/* docTypeName */> getDocumentTypeNames(Boolean used);

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

    List<FieldDefinition> getFieldDefinitions(List<QName> fieldDefinitionIds);

    FieldDefinition getFieldDefinition(QName fieldId);

    boolean isFieldDefinitionExisting(String fieldIdLocalname);

    List<FieldDefinition> searchFieldDefinitions(String searchCriteria);

    List<FieldGroup> searchFieldGroupDefinitions(String searchCriteria);

    FieldGroup getFieldGroup(NodeRef fieldGroupRef);

    Field getField(NodeRef fieldDefRef);

    void deleteFieldDefinition(NodeRef fieldDefRef);

    void addSystematicMetadataItems(DocumentTypeVersion docVer);

    void addSystematicFields(FieldGroup fieldGroupDefinition, FieldGroup fieldGroup);

    List<FieldGroup> getFieldGroupDefinitions();

    void copyFieldProps(FieldDefinition fieldDefinition, Field field);

    /** @return true if at least one document is created based on this documentType */
    boolean isDocumentTypeUsed(String documentTypeId);

    AssociationToDocType saveOrUpdateAssocToDocType(AssociationToDocType associationToDocType);

    Set<String> getNonExistingDocumentTypes(Set<String> documentTypeIds);

    /**
     * Create systematic document types. Document types with specified id-s must not exist before. <br/>
     * <strong>NB!</strong> This method may only be called when no other threads are using this service (e.g. from bootstrap), because it temporarily modifies service-wide
     * locations for fieldGroupDefinitions and fieldDefinitions.
     * 
     * @param systematicDocumentTypes
     * @param fieldGroupDefinitionsTmp alternate location for fieldGroupDefinitions
     * @param fieldDefinitionsTmp alternate location for fieldDefinitions
     */
    void createSystematicDocumentTypes(
            Map<String /* documentTypeId */, Pair<String /* documentTypeName */, Pair<Set<String> /* fieldGroupNames */, Set<QName> /* fieldGroupNames */>>> systematicDocumentTypes,
            NodeRef fieldGroupDefinitionsTmp, NodeRef fieldDefinitionsTmp);

}
