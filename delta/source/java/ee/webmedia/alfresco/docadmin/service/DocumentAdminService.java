package ee.webmedia.alfresco.docadmin.service;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

import ee.webmedia.alfresco.utils.MessageData;

/**
 * @author Ats Uiboupin
 */
public interface DocumentAdminService {

    String BEAN_NAME = "DocumentAdminService";

    void registerForbiddenFieldId(String forbiddenFieldId);

    Set<String> getForbiddenFieldIds();

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

    /**
     * Returns documentType with given NodeRef.
     * 
     * @param docTypeRef nodeRef of the document type.
     * @return null or found document type FIXME DLSeadist - Kui kõik süsteemsed dok.liigid on defineeritud, siis võib null kontrolli ja tagastamise eemdaldada
     */
    DocumentType getDocumentType(NodeRef docTypeRef);

    String getDocumentTypeName(String documentTypeId);

    Map<String/* docTypeId */, String/* docTypeName */> getDocumentTypeNames(Boolean used);

    NodeRef getDocumentTypesRoot();

    /**
     * Update properties or save new document type.
     * 
     * @param documentType - document type to be saved or updated to the repository
     * @return saved instance that was first created by cloning given object <br>
     *         and message if needed
     */
    Pair<DocumentType, MessageData> saveOrUpdateDocumentType(DocumentType docType);

    DocumentType createNewUnSaved();

    FieldDefinition createNewUnSavedFieldDefinition();

    void deleteDocumentType(NodeRef docTypeRef);

    void importDocumentTypes(File xmlFile);

    <F extends Field> F saveOrUpdateField(F originalFieldDef);

    List<FieldDefinition> saveOrUpdateFieldDefinitions(Collection<FieldDefinition> fieldDefinitions);

    List<FieldDefinition> getFieldDefinitions();

    Map<String, FieldDefinition> getFieldDefinitionsByFieldIds();

    List<FieldDefinition> getFieldDefinitions(List<String> fieldDefinitionIds);

    FieldDefinition getFieldDefinition(String fieldId);

    boolean isFieldDefinitionExisting(String fieldIdLocalname);

    List<FieldDefinition> searchFieldDefinitions(String searchCriteria);

    List<FieldGroup> searchFieldGroupDefinitions(String searchCriteria);

    FieldGroup getFieldGroup(NodeRef fieldGroupRef);

    Field getField(NodeRef fieldDefRef);

    void deleteFieldDefinition(NodeRef fieldDefRef);

    void addSystematicMetadataItems(DocumentTypeVersion docVer);

    void addSystematicFields(FieldGroup fieldGroupDefinition, FieldGroup fieldGroup);

    List<FieldGroup> getFieldGroupDefinitions();

    FieldGroup getFieldGroupDefinition(String fieldGroupName);

    void copyFieldProps(FieldDefinition fieldDefinition, Field field);

    /** @return true if at least one document is created based on this documentType */
    boolean isDocumentTypeUsed(String documentTypeId);

    AssociationModel saveOrUpdateAssocToDocType(AssociationModel associationModel);

    void deleteAssocToDocType(NodeRef assocRef);

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

    List<FieldDefinition> getSearchableFieldDefinitions();

    void registerGroupShowShowInTwoColumns(Set<String> originalFieldIds);

    boolean isGroupShowShowInTwoColumns(FieldGroup group);

}
