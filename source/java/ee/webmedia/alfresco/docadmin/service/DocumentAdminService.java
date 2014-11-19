package ee.webmedia.alfresco.docadmin.service;

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
import ee.webmedia.alfresco.docadmin.service.DocumentAdminServiceImpl.ImportHelper;
import ee.webmedia.alfresco.utils.MessageData;

<<<<<<< HEAD
/**
 * @author Ats Uiboupin
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public interface DocumentAdminService {

    String BEAN_NAME = "DocumentAdminService";

    void registerForbiddenFieldId(String forbiddenFieldId);

    Set<String> getForbiddenFieldIds();

    NodeRef getDocumentTypeRef(String id);

    /**
     * @return all document type objects from repository
     */
    List<DocumentType> getDocumentTypes(DynTypeLoadEffort effort);

    Set<String> getAdrDocumentTypeIds();

    <T extends DynamicType> List<T> getTypes(Class<T> typeClass, DynTypeLoadEffort effort);

    /**
     * @param used
     * @return all document type objects from repository with used property equal to argument
     */
    List<DocumentType> getDocumentTypes(DynTypeLoadEffort effort, boolean used);

    DocumentType getDocumentType(String id, DynTypeLoadEffort effort);

    Pair<DocumentType, DocumentTypeVersion> getDocumentTypeAndVersion(String docTypeId, Integer docTypeVersionNr);

<<<<<<< HEAD
    Pair<CaseFileType, DocumentTypeVersion> getCaseFileTypeAndVersion(String caseFileTypeId, Integer docTypeVersionNr);

    <T> T getTypeProperty(NodeRef docTypeRef, QName property, Class<T> returnClass);
=======
    <T> T getDocumentTypeProperty(NodeRef docTypeRef, QName property, Class<T> returnClass);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    /**
     * if you have docTypeRef, use {@link #getDocumentTypeProperty(NodeRef, QName, Class)} instead
     */
    <T> T getDocumentTypeProperty(String docTypeId, QName property, Class<T> returnClass);

    String getDocumentTypeName(String documentTypeId);

    String getDocumentTypeName(Node document);

    Pair<String, String> getDocumentTypeNameAndId(Node document);

    Map<String/* docTypeId */, String/* docTypeName */> getDocumentTypeNames(Boolean used);

    /**
     * Update properties or save new document type.
     * 
     * @param documentType - document type to be saved or updated to the repository
     * @return saved instance that was first created by cloning given object <br>
     *         and message if needed
     */
    Pair<DocumentType, MessageData> saveOrUpdateDocumentType(DocumentType docType);

    /**
     * Returns {@link DynamicType} with given NodeRef.
     * 
     * @param <D> subclass of {@link DynamicType}
     * @param dynTypeClass - subclass of DynamicType that this returnable object is expected to be
     * @param dynTypeRef - object reference
     * @param effort - how much children to load
     * @return null or found document type
     *         FIXME DLSeadist - Kui kõik süsteemsed dok.liigid on defineeritud, siis võib null kontrolli ja tagastamise eemdaldada
     */
    <D extends DynamicType> D getDynamicType(Class<D> dynTypeClass, NodeRef dynTypeRef, DynTypeLoadEffort effort);

    <D extends DynamicType> Pair<D, MessageData> saveOrUpdateDynamicType(D docType);

    <D extends DynamicType> D createNewUnSavedDynamicType(Class<D> dynamicTypeClass);

    <D extends DynamicType> NodeRef getDynamicTypesRoot(Class<D> dynTypeClass);

    FieldDefinition createNewUnSavedFieldDefinition();

    void deleteDynamicType(NodeRef dynTypeRef);

    /**
     * @return import helper, that handles transactions itself.
     */
    ImportHelper getImportHelper();

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

<<<<<<< HEAD
    void deleteFieldDefinition(NodeRef fieldDefRef);
=======
    void deleteFieldDefinition(Field field);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    void addSystematicMetadataItems(DocumentTypeVersion docVer);

    void addSystematicFields(FieldGroup fieldGroupDefinition, FieldGroup fieldGroup);

    List<FieldGroup> getFieldGroupDefinitions();

    FieldGroup getFieldGroupDefinition(String fieldGroupName);

    void copyFieldProps(FieldDefinition fieldDefinition, Field field);

    /** @return true if at least one document is created based on this documentType */
    boolean isDocumentTypeUsed(String documentTypeId);

    boolean isCaseFileTypeUsed(String caseFileTypeId);

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

<<<<<<< HEAD
    List<FieldDefinition> getSearchableDocumentFieldDefinitions();

    List<FieldDefinition> getSearchableVolumeFieldDefinitions();
=======
    List<FieldDefinition> getSearchableFieldDefinitions();
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    void registerGroupShowShowInTwoColumns(Set<String> originalFieldIds);

    boolean isGroupShowShowInTwoColumns(FieldGroup group);

    void registerGroupLimitSingle(String groupName);

    boolean isGroupLimitSingle(FieldGroup group);

    @SuppressWarnings("unchecked")
    /** It can be used to load DocumentType and first level childNodes */
    DynTypeLoadEffort DOC_TYPE_WITH_OUT_GRAND_CHILDREN = new DynTypeLoadEffort().setReturnChildrenByParent(DynamicType.class);

    /** It can be used to load DocumentType and first level childNodes plus all children of latest {@link DocumentTypeVersion} */
    DynTypeLoadEffort DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER = new DynTypeLoadEffort().setReturnLatestDynTypeVersionChildren();

    /** DocumentType without fetching children of older {@link DocumentTypeVersion} nodes */
    DynTypeLoadEffort DOC_TYPE_WITHOUT_OLDER_DT_VERSION_CHILDREN = new DynTypeLoadEffort() {
        @Override
        public boolean isReturnChildren(BaseObject parent) {
            // everything except children of DocumentTypeVersion
            return (parent instanceof DocumentTypeVersion) ? false : true;
        }
    }.setReturnLatestDynTypeVersionChildren(); // ... except children of latestDocTypeVersion

    /** It can be used to load DocumentType without any child nodes */
    DynTypeLoadEffort DONT_INCLUDE_CHILDREN = new DynTypeLoadEffort() {
        @Override
        public boolean isReturnChildren(BaseObject parent) {
            return false;
        }
    };

    public static class DynTypeLoadEffort implements Effort {
        private Set<Class<? extends BaseObject>> isReturnChildrenByParent;
        private boolean returnLatestDocTypeVersionChildren;

        @Override
        public boolean isReturnChildren(BaseObject parent) {
            if (isReturnChildrenByParent != null) {
                Class<? extends BaseObject> parentClass = parent.getClass();
                for (Class<? extends BaseObject> returnChildrenByParent : isReturnChildrenByParent) {
                    if (returnChildrenByParent.isAssignableFrom(parentClass)) {
                        return true;
                    }
                }
                return false;
            }
            throw new RuntimeException("Unimplemented");
        }

        public DynTypeLoadEffort setReturnChildrenByParent(Class<? extends BaseObject>... classes) {
            if (isReturnChildrenByParent == null) {
                isReturnChildrenByParent = new HashSet<Class<? extends BaseObject>>(classes.length);
            }
            for (Class<? extends BaseObject> clazz : classes) {
                isReturnChildrenByParent.add(clazz);
            }
            return this;
        }

        public DynTypeLoadEffort setReturnLatestDynTypeVersionChildren() {
            returnLatestDocTypeVersionChildren = true;
            // to return children of latest DocumentTypeVersion, we must load children of DocumentType where DocumentTypeVersion's are
            if (isReturnChildrenByParent == null) {
                isReturnChildrenByParent = new HashSet<Class<? extends BaseObject>>(3);
            }
            isReturnChildrenByParent.add(DynamicType.class);
            return this;
        }

        public boolean isReturnLatestDynTypeVersionChildren() {
            return returnLatestDocTypeVersionChildren;
        }

    }

    /** Return true if fieldDefinition is used in some docType where used=true */
    boolean isFieldDefintionUsed(String fieldId);

<<<<<<< HEAD
    List<CaseFileType> getUsedCaseFileTypes(DynTypeLoadEffort effort);

    <T> T getCaseFileTypeProperty(String caseFileTypeId, QName property, Class<T> returnClass);

    NodeRef getCaseFileTypeRef(String id);

    CaseFileType getCaseFileType(String id, DynTypeLoadEffort effort);

    String getCaseFileTypeName(Node caseFile);

    List<FieldDefinition> getVolumeFieldDefinitions();

    List<CaseFileType> getAllCaseFileTypes(DynTypeLoadEffort effort);

    DocumentType getUsedDocumentType(String documentTypeId);

    Map<String, String> getCaseFileTypeNames(Boolean used);

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    DocumentTypeVersion getLatestDocTypeVer(String documentTypeId);

    Map<String, DocumentTypeVersion> getLatestDocTypeVersions();

}