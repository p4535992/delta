package ee.webmedia.alfresco.docadmin.service;

import static ee.webmedia.alfresco.utils.RepoUtil.copyProps;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docadmin.web.MetadataItemCompareUtil;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * @author Ats Uiboupin
 */
public class DocumentAdminServiceImpl implements DocumentAdminService, InitializingBean {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentAdminServiceImpl.class);

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;
    private BaseService baseService;
    private MenuService menuService;
    private UserService userService;
    private DocumentSearchService documentSearchService;

    private NodeRef documentTypesRoot;
    private NodeRef fieldDefinitionsRoot;
    private NodeRef fieldGroupDefinitionsRoot;
    private Set<String> fieldPropNames;

    @Override
    public void afterPropertiesSet() throws Exception {
        baseService.addTypeMapping(DocumentAdminModel.Types.DOCUMENT_TYPE, DocumentType.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.DOCUMENT_TYPE_VERSION, DocumentTypeVersion.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FIELD, Field.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FIELD_GROUP, FieldGroup.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.SEPARATION_LINE, SeparatorLine.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FIELD_DEFINITION, FieldDefinition.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FOLLOWUP_ASSOCIATION, FollowupAssociation.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.REPLY_ASSOCIATION, ReplyAssociation.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FIELD_MAPPING, FieldMapping.class);
    }

    @Override
    public List<DocumentType> getDocumentTypes() {
        return getAllDocumentTypes(null);
    }

    @Override
    public List<DocumentType> getDocumentTypes(boolean used) {
        return getAllDocumentTypes(Boolean.valueOf(used));
    }

    @Override
    public DocumentType getDocumentType(String id) {
        NodeRef documentTypeRef = getDocumentTypeRef(id);
        return getDocumentType(documentTypeRef);
    }

    private NodeRef getDocumentTypeRef(String id) {
        return generalService.getNodeRef(DocumentType.getAssocName(id).toString(), getDocumentTypesRoot());
    }

    @Override
    public DocumentType getDocumentType(NodeRef docTypeRef) {
        // FIXME DLSeadist - Kui kõik süsteemsed dok.liigid on defineeritud, siis võib null kontrolli ja tagastamise eemdaldada
        if (docTypeRef == null) {
            return null;
        }
        return baseService.getObject(docTypeRef, DocumentType.class);
    }

    @Override
    public String getDocumentTypeName(String documentTypeId) {
        if (StringUtils.isBlank(documentTypeId)) {
            return null;
        }
        NodeRef documentTypeRef = getDocumentTypeRef(documentTypeId);
        if (documentTypeRef == null) {
            // Should not usually happen
            return null;
        }
        return (String) nodeService.getProperty(documentTypeRef, DocumentAdminModel.Props.NAME);
    }

    @Override
    public Map<String/* docTypeId */, String/* docTypeName */> getDocumentTypeNames(Boolean used) {
        List<DocumentType> documentTypes = getAllDocumentTypes(used);
        Map<String, String> docTypesByDocTypeId = new HashMap<String, String>(documentTypes.size());
        for (DocumentType documentType : documentTypes) {
            docTypesByDocTypeId.put(documentType.getDocumentTypeId(), documentType.getName());
        }
        return docTypesByDocTypeId;
    }

    @Override
    public void addSystematicMetadataItems(DocumentTypeVersion docVer) {
        addMetadataItems(docVer, new Predicate<FieldGroup>() {
            @Override
            public boolean evaluate(FieldGroup sourceGroup) {
                return sourceGroup.isMandatoryForDoc();
            }
        }, new Predicate<FieldDefinition>() {
            @Override
            public boolean evaluate(FieldDefinition sourceFieldDef) {
                return sourceFieldDef.isMandatoryForDoc();
            }
        });
    }

    private void addMetadataItems(DocumentTypeVersion docVer, Predicate<FieldGroup> fieldGroupPredicate, Predicate<FieldDefinition> fieldDefinitionPredicate) {
        // order of mandatory metadataItems under docVer should be set on fieldDefinitions and fieldGroups
        List<FieldDefinition> fieldDefinitions = getFieldDefinitions();
        ChildrenList<MetadataItem> metadata = docVer.getMetadata();
        for (FieldGroup sourceGroup : getFieldGroupDefinitions()) {
            if (fieldGroupPredicate.evaluate(sourceGroup)) {
                FieldGroup targetGroup = metadata.add(FieldGroup.class);
                addSystematicFields(sourceGroup, targetGroup, fieldDefinitions);
            }
        }

        for (FieldDefinition sourceFieldDef : fieldDefinitions) {
            if (fieldDefinitionPredicate.evaluate(sourceFieldDef)) {
                Field targetField = metadata.add(Field.class);
                copyFieldProps(sourceFieldDef, targetField);
            }
        }
    }

    @Override
    public void addSystematicFields(FieldGroup fieldGroupDefinition, FieldGroup fieldGroup) {
        addSystematicFields(fieldGroupDefinition, fieldGroup, getFieldDefinitions());
    }

    private void addSystematicFields(FieldGroup fieldGroupDefinition, FieldGroup fieldGroup, List<FieldDefinition> fieldDefinitions) {
        Map<String, Object> targetGroupProps = fieldGroup.getNode().getProperties();
        copyProps(fieldGroupDefinition.getNode().getProperties(), targetGroupProps);
        @SuppressWarnings("unchecked")
        List<String> fieldDefinitionIds = (List<String>) targetGroupProps.remove(DocumentAdminModel.Props.FIELD_DEFINITIONS_IDS);
        int groupOrder = 1;
        for (String fieldDefinitionId : fieldDefinitionIds) {
            FieldDefinition sourceFieldDef = null;
            { // getFieldDefinition
                for (FieldDefinition fieldDefinition : fieldDefinitions) {
                    if (fieldDefinition.getFieldId().equals(fieldDefinitionId)) {
                        sourceFieldDef = fieldDefinition;
                        break;
                    }
                }
                if (sourceFieldDef == null) {
                    throw new IllegalArgumentException("FieldGroup '" + fieldGroupDefinition.getName() + "' references unknown fieldDefinition '" + fieldDefinitionId + "'");
                }
            }
            Field targetField = fieldGroup.getFields().add();
            copyFieldProps(sourceFieldDef, targetField);
            targetField.setOrder(groupOrder++);
            fieldDefinitions.remove(sourceFieldDef);
        }
    }

    private Set<String> getFieldPropNames() {
        if (fieldPropNames == null) {
            TypeDefinition fieldTypeDef = generalService.getAnonymousType(DocumentAdminModel.Types.FIELD);
            Set<String> tmp = new HashSet<String>();
            for (QName qName : fieldTypeDef.getProperties().keySet()) {
                tmp.add(qName.toString());
            }
            fieldPropNames = Collections.unmodifiableSet(tmp);
        }
        return fieldPropNames;
    }

    @Override
    public void copyFieldProps(FieldDefinition fieldDefinition, Field field) {
        Map<String, Object> targetFieldProps = field.getNode().getProperties();
        copyProps(fieldDefinition.getNode().getProperties(), targetFieldProps);
        targetFieldProps.keySet().retainAll(getFieldPropNames()); // remove properties that only fieldDefinition should have
        field.setCopyOfFieldDefinition(fieldDefinition);
    }

    private FieldDefinition createFieldDefinition(Field field) {
        FieldDefinition fieldDef = createNewUnSavedFieldDefinition();
        Map<String, Object> targetFieldProps = fieldDef.getNode().getProperties();
        copyProps(field.getNode().getProperties(), targetFieldProps);
        fieldDef.setOrder(null);
        return fieldDef;
    }

    private List<DocumentType> getAllDocumentTypes(final Boolean used) {
        return baseService.getChildren(getDocumentTypesRoot(), DocumentType.class, new Predicate<DocumentType>() {
            @Override
            public boolean evaluate(DocumentType documentType) {
                return used == null || documentType.isUsed() == used;
            }
        });
    }

    @Override
    public DocumentType createNewUnSaved() {
        return new DocumentType(getDocumentTypesRoot());
    }

    @Override
    public FieldDefinition createNewUnSavedFieldDefinition() {
        return new FieldDefinition(getFieldDefinitionsRoot());
    }

    @Override
    public void deleteDocumentType(NodeRef docTypeRef) {
        DocumentType documentType = getDocumentType(docTypeRef);
        if (documentType.isSystematic()) {
            throw new IllegalArgumentException("docType_list_action_delete_failed_systematic"); // shouldn't happen, because systematic can't be changed at runtime
        }
        boolean docTypeInUse = false; // TODO DLSeadist: CLTASK 166371 determine if type is in use
        if (docTypeInUse) {
            throw new UnableToPerformException("docType_delete_failed_inUse");
        }
        nodeService.deleteNode(docTypeRef);
    }

    @Override
    public DocumentType saveOrUpdateDocumentType(DocumentType docTypeOriginal) {
        DocumentType docType = docTypeOriginal.clone();
        boolean wasUnsaved = docType.isUnsaved();

        // validating duplicated documentTypeId is done in baseService
        updateChildren(docType);
        baseService.saveObject(docType);

        updatePublicAdr(docType, wasUnsaved);
        menuService.menuUpdated();
        return docType;
    }

    @Override
    public <F extends Field> F saveOrUpdateField(F originalFieldDef) {
        @SuppressWarnings("unchecked")
        F fieldDef = (F) originalFieldDef.clone();
        baseService.saveObject(fieldDef);
        return fieldDef;
    }

    @Override
    public AssociationModel saveOrUpdateAssocToDocType(AssociationModel associationModel) {
        AssociationModel clone = associationModel.clone();
        baseService.saveObject(clone);
        return clone;
    }

    private List<AssociationModel> getAssocsToDocType(List<NodeRef> assocRefs) {
        return baseService.getObjects(assocRefs, AssociationModel.class);
    }

    @Override
    public void deleteAssocToDocType(NodeRef assocRef) {
        nodeService.deleteNode(assocRef);
    }

    @Override
    public List<FieldDefinition> saveOrUpdateFieldDefinitions(List<FieldDefinition> fieldDefinitions) {
        List<FieldDefinition> saved = new ArrayList<FieldDefinition>();
        for (FieldDefinition fieldDefinition : fieldDefinitions) {
            saved.add(saveOrUpdateField(fieldDefinition));
        }
        // TODO DLSeadist task 166391:
        // 4.1.11.2. Kui mõnel andmevälja real on muudetud parameterOrderInDocSearch väärtust,
        // siis teostatakse andmeväljade ümberjärjestamine veeru parameterOrderInDocSearch alusel (vt Üldised reeglid.docx punkt 3).
        // 4.1.10.1.4.1.11.3. Kui mõnel andmevälja real on muudetud parameterOrderInVolSearch väärtust,
        // siis teostatakse andmeväljade ümberjärjestamine veeru parameterOrderInVolSearch alusel (vt Üldised reeglid.docx punkt 3).
        return saved;
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        // createFieldDefinitionsTestData(); // FIXME DLSeadist test data
        return baseService.getChildren(getFieldDefinitionsRoot(), FieldDefinition.class);
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions(List<String> fieldDefinitionIds) {
        List<FieldDefinition> fieldDefinitions = getFieldDefinitions();
        for (Iterator<FieldDefinition> it = fieldDefinitions.iterator(); it.hasNext();) {
            FieldDefinition fieldDefinition = it.next();
            if (!fieldDefinitionIds.contains(fieldDefinition.getFieldId())) {
                it.remove();
            }
        }
        return fieldDefinitions;
    }

    private Map<String, FieldDefinition> getFieldDefinitionsByFieldIds() {
        List<FieldDefinition> fieldDefinitions = getFieldDefinitions();
        Map<String, FieldDefinition> fieldDefs = new HashMap<String, FieldDefinition>();
        for (FieldDefinition fieldDefinition : fieldDefinitions) {
            fieldDefs.put(fieldDefinition.getFieldId(), fieldDefinition);
        }
        return fieldDefs;
    }

    @Override
    public List<FieldGroup> getFieldGroupDefinitions() {
        return baseService.getChildren(getFieldGroupDefinitionsRoot(), FieldGroup.class);
    }

    @Override
    public FieldGroup getFieldGroupDefinition(String fieldGroupName) {
        for (FieldGroup fieldGroup : getFieldGroupDefinitions()) {
            if (StringUtils.equals(fieldGroupName, fieldGroup.getName())) {
                return fieldGroup;
            }
        }
        throw new IllegalArgumentException("Didn't find fieldGroup wiht name '" + fieldGroupName + "'");
    }

    @Override
    public FieldDefinition getFieldDefinition(String fieldId) {
        return baseService.getChild(getFieldDefinitionsRoot(), new QNameLocalnameMatcher(fieldId), FieldDefinition.class);
    }

    @Override
    public boolean isFieldDefinitionExisting(String fieldIdLocalname) {
        return null != generalService.getChildByAssocName(getFieldDefinitionsRoot(), new QNameLocalnameMatcher(fieldIdLocalname));
    }

    /**
     * {@link QNamePattern} that considers {@link QName#getLocalName()} when matching. <br>
     * XXX: created for {@link DocumentAdminServiceImpl#isFieldDefinitionExisting(String)} and {@link DocumentAdminServiceImpl#getFieldDefinition(String)} because at the time there
     * were several diferent namespaces for previous built-in fields and for user defined fields. It must change in future!
     * 
     * @author Ats Uiboupin
     */
    private class QNameLocalnameMatcher implements QNamePattern {
        private final String localName;

        public QNameLocalnameMatcher(String localName) {
            this.localName = localName;
        }

        @Override
        public boolean isMatch(QName qname) {
            return qname.getLocalName().equals(localName);
        }

        @Override
        public String toString() {
            return getClass().getName() + ":" + localName;
        }
    }

    @Override
    public List<FieldDefinition> searchFieldDefinitions(String searchCriteria) {
        List<NodeRef> resultRefs = documentSearchService.simpleSearch(searchCriteria, getFieldDefinitionsRoot()
                , DocumentAdminModel.Types.FIELD_DEFINITION, DocumentAdminModel.Props.NAME, DocumentAdminModel.Props.FIELD_ID);
        return baseService.getObjects(resultRefs, FieldDefinition.class);
    }

    @Override
    public List<FieldGroup> searchFieldGroupDefinitions(String searchCriteria) {
        List<NodeRef> resultRefs = documentSearchService.simpleSearch(searchCriteria, getFieldGroupDefinitionsRoot()
                , DocumentAdminModel.Types.FIELD_GROUP, DocumentAdminModel.Props.NAME);
        return baseService.getObjects(resultRefs, FieldGroup.class);
    }

    @Override
    public FieldGroup getFieldGroup(NodeRef fieldGroupRef) {
        return baseService.getObject(fieldGroupRef, FieldGroup.class);
    }

    @Override
    public Field getField(NodeRef fieldDefRef) {
        return baseService.getObject(fieldDefRef, FieldDefinition.class);
    }

    @Override
    public void deleteFieldDefinition(NodeRef fieldDefRef) {
        nodeService.deleteNode(fieldDefRef);
    }

    @Override
    public boolean isDocumentTypeUsed(String documentTypeId) {
        // TODO DLSeadist maybe need to cache the result (very rare that document type that was used becomes unused)
        return documentSearchService.isMatch(
                joinQueryPartsAnd(
                        joinQueryPartsAnd(
                                generateTypeQuery(DocumentCommonModel.Types.DOCUMENT)
                                , generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE)
                        )
                        , generatePropertyExactQuery(Props.OBJECT_TYPE_ID, documentTypeId, false))
        );
    }

    @Override
    public void createSystematicDocumentTypes(
            Map<String /* documentTypeId */, Pair<String /* documentTypeName */, Pair<Set<String> /* fieldGroupNames */, Set<QName> /* fieldGroupNames */>>> systematicDocumentTypes,
            NodeRef fieldGroupDefinitionsTmp, NodeRef fieldDefinitionsTmp) {

        Assert.notNull(fieldGroupDefinitionsTmp);
        Assert.notNull(fieldDefinitionsTmp);

        fieldGroupDefinitionsRoot = fieldGroupDefinitionsTmp;
        fieldDefinitionsRoot = fieldDefinitionsTmp;

        for (Entry<String, Pair<String, Pair<Set<String>, Set<QName>>>> systematicDocumentType : systematicDocumentTypes.entrySet()) {
            createSystematicDocumentType(systematicDocumentType.getKey(), systematicDocumentType.getValue().getFirst(), systematicDocumentType.getValue().getSecond().getFirst(),
                    Field.getLocalNames(systematicDocumentType.getValue().getSecond().getSecond()));
        }

        fieldGroupDefinitionsRoot = null;
        fieldDefinitionsRoot = null;
    }

    @Override
    public Set<String> getNonExistingDocumentTypes(Set<String> documentTypeIds) {
        Set<String> result = new HashSet<String>(documentTypeIds);
        CollectionUtils.filter(result, new org.apache.commons.collections.Predicate() {
            @Override
            public boolean evaluate(Object documentTypeId) {
                return !isDocumentTypeExisting((String) documentTypeId);
            }
        });
        return result;
    }

    private void createSystematicDocumentType(String documentTypeId, String documentTypeName, final Set<String> fieldGroupNames, final Collection<String> fieldDefinitionIds) {
        LOG.info("Creating systematic document type: " + documentTypeId);
        DocumentType docType = createNewUnSaved();
        docType.setDocumentTypeId(documentTypeId);
        docType.setName(documentTypeName);
        docType.setSystematic(true);
        DocumentTypeVersion ver = docType.addNewLatestDocumentTypeVersion();
        addMetadataItems(ver, new Predicate<FieldGroup>() {
            @Override
            public boolean evaluate(FieldGroup sourceGroup) {
                return fieldGroupNames.contains(sourceGroup.getName());
            }
        }, new Predicate<FieldDefinition>() {
            @Override
            public boolean evaluate(FieldDefinition sourceFieldDef) {
                return fieldDefinitionIds.contains(sourceFieldDef.getFieldId());
            }
        });

        int order = -1;
        for (MetadataItem metadataItem : ver.getMetadata()) {
            if (metadataItem.getOrder() != null) {
                order = Math.max(order, metadataItem.getOrder());
            }
        }
        for (MetadataItem metadataItem : ver.getMetadata()) {
            if (metadataItem.getOrder() == null || metadataItem.getOrder() < 1) {
                metadataItem.setOrder(++order);
            }
        }

        saveOrUpdateDocumentType(docType);
    }

    private boolean isDocumentTypeExisting(String documentTypeId) {
        return getDocumentTypeRef(documentTypeId) != null;
    }

    private void updatePublicAdr(DocumentType docType, boolean wasUnsaved) {
        Boolean oldPublicAdr = wasUnsaved ? false : (Boolean) nodeService.getProperty(docType.getNodeRef(), DocumentAdminModel.Props.PUBLIC_ADR);
        if (oldPublicAdr == null) {
            oldPublicAdr = Boolean.FALSE;
        }
        Boolean newPublicAdr = docType.isPublicAdr();

        if (oldPublicAdr.booleanValue() != newPublicAdr.booleanValue()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Changing publicAdr of DocumentType " + docType.getDocumentTypeId() + " from "
                        + oldPublicAdr.toString().toUpperCase() + " to " + newPublicAdr.toString().toUpperCase());
            }
            QName id = docType.getAssocName();
            if (newPublicAdr) {
                getAdrService().addDocumentType(id);
            } else {
                getAdrService().deleteDocumentType(id);
            }
        }
    }

    private void updateChildren(DocumentType docType) {
        int versionNr = 1;
        boolean saved = docType.isSaved();
        if (saved) {
            Integer latestVersion = docType.getLatestVersion();
            ChildrenList<DocumentTypeVersion> documentTypeVersions = docType.getDocumentTypeVersions();
            ChildrenList<MetadataItem> savedMetadata = documentTypeVersions.get(latestVersion - 2).getMetadata();
            DocumentTypeVersion latestDocumentTypeVersion = docType.getLatestDocumentTypeVersion();
            ChildrenList<MetadataItem> unSavedMetadata = latestDocumentTypeVersion.getMetadata();
            if (!MetadataItemCompareUtil.isClidrenListChanged(savedMetadata, unSavedMetadata)) {
                // metaData list is not changed, don't save new DocumentTypeVersion (currently as new latestDocumentTypeVersion)
                documentTypeVersions.remove(latestDocumentTypeVersion);
                docType.restoreProp(DocumentAdminModel.Props.LATEST_VERSION); // don't overwrite latest version number
                return;
            }
            // someone might have saved meanwhile new version of the same docType
            DocumentType latestDocTypeInRepo = getDocumentType(docType.getNodeRef());
            versionNr = latestDocTypeInRepo.getLatestVersion() + 1;
        }
        String userId = AuthenticationUtil.getFullyAuthenticatedUser();
        DocumentTypeVersion docVer = docType.getLatestDocumentTypeVersion();
        docVer.setCreatorId(userId);
        docVer.setCreatorName(userService.getUserFullName(userId));
        docVer.setVersionNr(versionNr);
        docVer.setCreatedDateTime(new Date(AlfrescoTransactionSupport.getTransactionStartTime()));
        Map<String, FieldDefinition> fieldDefinitions = getFieldDefinitionsByFieldIds();
        // save new fields to fieldDefinitions
        for (Field field : docVer.getFieldsDeeply()) {
            if (!field.isCopyFromPreviousDocTypeVersion()) {
                // field is not newer version of the same field under previous version of DocumentTypeVersion
                FieldDefinition fieldDef = fieldDefinitions.get(field.getFieldId());
                if (fieldDef != null) {
                    if (!field.getFieldTypeEnum().equals(fieldDef.getFieldTypeEnum())) {
                        throw new UnableToPerformException("field_details_error_docField_sameIdFieldDef_differentType", field.getFieldNameWithIdAndType(),
                                fieldDef.getFieldNameWithIdAndType());
                    }
                    // field is added based on existing fieldDefinition
                    List<String> docTypesOfFieldDef = fieldDef.getDocTypes();
                    if (!docTypesOfFieldDef.contains(docType.getDocumentTypeId())) {
                        docTypesOfFieldDef.add(docType.getDocumentTypeId());
                        fieldDef = saveOrUpdateField(fieldDef);
                    }
                } else {
                    if (field.isCopyOfFieldDefinition()) {
                        field.setSystematic(false); // field is created based on fieldDefinition, but id is changed
                    }
                    // added new field (not based on fieldDefinition)
                    fieldDef = createFieldDefinition(field);
                    fieldDef.getDocTypes().add(docType.getDocumentTypeId());
                    fieldDef = saveOrUpdateField(fieldDef);
                }
            }
        }
        deleteFieldMappings(docType, docVer);
    }

    private void deleteFieldMappings(DocumentType docType, DocumentTypeVersion docVer) {
        List<String> removedFieldIds = docVer.getRemovedFieldIdsDeeply();
        if (removedFieldIds.isEmpty()) {
            return; // don't need to delete any field mappings
        }
        String docTypeId = docType.getDocumentTypeId();
        for (AssociationModel associationModel : docType.getAssociationModels(null)) { // for each removed field remove field mapping held in memory
            ChildrenList<FieldMapping> fieldMappings = associationModel.getFieldMappings();
            String otherDocType = associationModel.getDocType();
            boolean assocToSameType = otherDocType.equals(docTypeId);
            for (Iterator<FieldMapping> it = fieldMappings.iterator(); it.hasNext();) {
                FieldMapping fieldMapping = it.next();
                if (removedFieldIds.contains(fieldMapping.getFromField())
                        || (assocToSameType && removedFieldIds.contains(fieldMapping.getToField()))) {
                    it.remove(); // remove field mapping from removable field of this documentType to docType of associationModel.docType
                }
            }
        }
        // reverse associations to same type
        String query = joinQueryPartsAnd(
                generateTypeQuery(DocumentAdminModel.Types.FOLLOWUP_ASSOCIATION, DocumentAdminModel.Types.REPLY_ASSOCIATION)
                , generatePropertyExactQuery(DocumentAdminModel.Props.DOC_TYPE, docTypeId, false)
                );
        List<NodeRef> associatedDocTypes = documentSearchService.searchNodes(query, false, "searchAssocsToDocType:" + docTypeId);

        List<AssociationModel> assocsToDocTypes = getAssocsToDocType(associatedDocTypes);
        NodeRef docTypeRef = docType.getNodeRef();
        for (AssociationModel reverseAssocsToDocType : assocsToDocTypes) {
            if (reverseAssocsToDocType.getParentNodeRef().equals(docTypeRef)) {
                continue; // field mappings of the same documentType should have been already removed in memory and will be persisted when saving is completed
            }
            ChildrenList<FieldMapping> fieldMappings = reverseAssocsToDocType.getFieldMappings();
            boolean foundFieldMappingToDelete = false;
            for (Iterator<FieldMapping> it = fieldMappings.iterator(); it.hasNext();) {
                FieldMapping fieldMapping = it.next();
                if (removedFieldIds.contains(fieldMapping.getToField())) {
                    it.remove();
                    foundFieldMappingToDelete = true;
                }
            }
            if (foundFieldMappingToDelete) {
                saveOrUpdateAssocToDocType(reverseAssocsToDocType);
            }
        }
    }

    private NodeRef getDocumentTypesRoot() {
        if (documentTypesRoot == null) {
            String xPath = DocumentAdminModel.Repo.DOCUMENT_TYPES_SPACE;
            documentTypesRoot = generalService.getNodeRef(xPath);
        }
        return documentTypesRoot;
    }

    private NodeRef getFieldDefinitionsRoot() {
        if (fieldDefinitionsRoot == null) {
            String xPath = DocumentAdminModel.Repo.FIELD_DEFINITIONS_SPACE;
            fieldDefinitionsRoot = generalService.getNodeRef(xPath);
        }
        return fieldDefinitionsRoot;
    }

    private NodeRef getFieldGroupDefinitionsRoot() {
        if (fieldGroupDefinitionsRoot == null) {
            String xPath = DocumentAdminModel.Repo.FIELD_GROUP_DEFINITIONS_SPACE;
            fieldGroupDefinitionsRoot = generalService.getNodeRef(xPath);
        }
        return fieldGroupDefinitionsRoot;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setBaseService(BaseService baseService) {
        this.baseService = baseService;
    }

    public void setMenuService(MenuService menuService) {
        this.menuService = menuService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    /** To break Circular dependency */
    private AdrService getAdrService() {
        return BeanHelper.getAdrService();
    }

}
