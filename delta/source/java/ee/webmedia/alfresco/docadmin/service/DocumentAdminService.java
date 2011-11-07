package ee.webmedia.alfresco.docadmin.service;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseService.Effort;
import ee.webmedia.alfresco.utils.MessageData;

/**
 * @author Ats Uiboupin
 */
public interface DocumentAdminService {

    String BEAN_NAME = "DocumentAdminService";

    void registerForbiddenFieldId(String forbiddenFieldId);

    Set<String> getForbiddenFieldIds();

    NodeRef getDocumentTypeRef(String id);

    /**
     * @return all document type objects from repository
     */
    List<DocumentType> getDocumentTypes(DocTypeLoadEffort effort);

    /**
     * @param used
     * @return all document type objects from repository with used property equal to argument
     */
    List<DocumentType> getDocumentTypes(DocTypeLoadEffort effort, boolean used);

    DocumentType getDocumentType(String id, DocTypeLoadEffort effort);

    Pair<DocumentType, DocumentTypeVersion> getDocumentTypeAndVersion(String docTypeId, Integer docTypeVersionNr);

    <T> T getDocumentTypeProperty(NodeRef docTypeRef, QName property, Class<T> returnClass);

    /**
     * if you have docTypeRef, use {@link #getDocumentTypeProperty(NodeRef, QName, Class)} instead
     */
    <T> T getDocumentTypeProperty(String docTypeId, QName property, Class<T> returnClass);

    /**
     * Returns documentType with given NodeRef.
     * 
     * @param docTypeRef nodeRef of the document type.
     * @param effort - how much children to load
     * @return null or found document type FIXME DLSeadist - Kui kõik süsteemsed dok.liigid on defineeritud, siis võib null kontrolli ja tagastamise eemdaldada
     */
    DocumentType getDocumentType(NodeRef docTypeRef, DocTypeLoadEffort effort);

    String getDocumentTypeName(String documentTypeId);

    String getDocumentTypeName(Node document);

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

    @SuppressWarnings("unchecked")
    /** It can be used to load DocumentType and first level childNodes */
    DocTypeLoadEffort DOC_TYPE_WITH_OUT_GRAND_CHILDREN = new DocTypeLoadEffort().setReturnChildrenByParent(DocumentType.class);

    /** It can be used to load DocumentType and first level childNodes plus all children of latest {@link DocumentTypeVersion} */
    DocTypeLoadEffort DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER = new DocTypeLoadEffort().setReturnLatestDocTypeVersionChildren();

    /** DocumentType without fetching children of older {@link DocumentTypeVersion} nodes */
    DocTypeLoadEffort DOC_TYPE_WITHOUT_OLDER_DT_VERSION_CHILDREN = new DocTypeLoadEffort() {
        @Override
        public boolean isReturnChildren(BaseObject parent) {
            // everything except children of DocumentTypeVersion
            return (parent instanceof DocumentTypeVersion) ? false : true;
        }
    }.setReturnLatestDocTypeVersionChildren(); // ... except children of latestDocTypeVersion

    /** It can be used to load DocumentType without any child nodes */
    DocTypeLoadEffort DONT_INCLUDE_CHILDREN = new DocTypeLoadEffort() {
        @Override
        public boolean isReturnChildren(BaseObject parent) {
            return false;
        }
    };

    public static class DocTypeLoadEffort implements Effort {
        private Set<Class<? extends BaseObject>> isReturnChildrenByParent;
        private boolean returnLatestDocTypeVersionChildren;

        @Override
        public boolean isReturnChildren(BaseObject parent) {
            if (isReturnChildrenByParent != null) {
                return isReturnChildrenByParent.contains(parent.getClass());
            }
            throw new RuntimeException("Unimplemented");
        }

        public DocTypeLoadEffort setReturnChildrenByParent(Class<? extends BaseObject>... classes) {
            if (isReturnChildrenByParent == null) {
                isReturnChildrenByParent = new HashSet<Class<? extends BaseObject>>(classes.length);
            }
            for (Class<? extends BaseObject> clazz : classes) {
                isReturnChildrenByParent.add(clazz);
            }
            return this;
        }

        public DocTypeLoadEffort setReturnLatestDocTypeVersionChildren() {
            returnLatestDocTypeVersionChildren = true;
            // to return children of latest DocumentTypeVersion, we must load children of DocumentType where DocumentTypeVersion's are
            if (isReturnChildrenByParent == null) {
                isReturnChildrenByParent = new HashSet<Class<? extends BaseObject>>(3);
            }
            isReturnChildrenByParent.add(DocumentType.class);
            return this;
        }

        public boolean isReturnLatestDocTypeVersionChildren() {
            return returnLatestDocTypeVersionChildren;
        }

    }

}
