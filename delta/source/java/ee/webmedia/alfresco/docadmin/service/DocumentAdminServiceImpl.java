package ee.webmedia.alfresco.docadmin.service;

import static ee.webmedia.alfresco.utils.RepoUtil.copyProps;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.web.MetadataItemCompareUtil;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
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
        baseService.addTypeMapping(DocumentAdminModel.Types.ASSOCIATION_TO_DOC_TYPE, AssociationToDocType.class);
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
        return baseService.getObject(docTypeRef, DocumentType.class);
    }

    @Override
    public String getDocumentTypeName(String documentTypeId) {
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
        List<QName> fieldDefinitionIds = (List<QName>) targetGroupProps.remove(DocumentAdminModel.Props.FIELD_DEFINITIONS_IDS);
        int groupOrder = 1;
        for (QName fieldDefinitionId : fieldDefinitionIds) {
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
        updateOrRemoveLatestDocTypeVersion(docType);
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
    public AssociationToDocType saveOrUpdateAssocToDocType(AssociationToDocType associationToDocType) {
        AssociationToDocType clone = associationToDocType.clone();
        baseService.saveObject(clone);
        return clone;
    }

    @Override
    public List<FieldDefinition> saveOrUpdateFieldDefinitions(List<FieldDefinition> fieldDefinitions) {
        ArrayList<FieldDefinition> saved = new ArrayList<FieldDefinition>();
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
    public List<FieldDefinition> getFieldDefinitions(List<QName> fieldDefinitionIds) {
        List<FieldDefinition> fieldDefinitions = getFieldDefinitions();
        for (Iterator<FieldDefinition> it = fieldDefinitions.iterator(); it.hasNext();) {
            FieldDefinition fieldDefinition = it.next();
            if (!fieldDefinitionIds.contains(fieldDefinition.getFieldId())) {
                it.remove();
            }
        }
        return fieldDefinitions;
    }

    private Map<QName, FieldDefinition> getFieldDefinitionsByFieldIds() {
        List<FieldDefinition> fieldDefinitions = getFieldDefinitions();
        Map<QName, FieldDefinition> fieldDefs = new HashMap<QName, FieldDefinition>();
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
    public FieldDefinition getFieldDefinition(QName fieldId) {
        return baseService.getChild(getFieldDefinitionsRoot(), fieldId, FieldDefinition.class);
    }

    @Override
    public boolean isFieldDefinitionExisting(String fieldIdLocalname) {
        return null != generalService.getChildByAssocName(getFieldDefinitionsRoot(), new QNameLocalnameMatcher(fieldIdLocalname));
    }

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
                                generateTypeQuery(DocumentDynamicModel.Types.DOCUMENT_DYNAMIC)
                                , generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE)
                        )
                        , generatePropertyExactQuery(DocumentDynamicModel.Props.DOCUMENT_TYPE_ID, documentTypeId, false))
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
                    systematicDocumentType.getValue().getSecond().getSecond());
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

    private void createSystematicDocumentType(String documentTypeId, String documentTypeName, final Set<String> fieldGroupNames, final Set<QName> fieldDefinitionIds) {
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

    /** FIXME DLSeadist test data */
    private List<FieldDefinition> createFieldDefinitionsTestData() {
        FieldDefinition fd1 = new FieldDefinition(getFieldDefinitionsRoot());
        fd1.setName("testFieldDefName");
        fd1.setFieldId(QName.createQName(DocumentDynamicModel.URI, "testFieldId"));
        fd1.setSystematic(true);
        fd1.setDocTypes(Arrays.asList("type1", "type2"));
        fd1.setParameterOrderInDocSearch(1);
        fd1.setParameterOrderInVolSearch(2);
        fd1.setVolTypes(Arrays.asList("volType1", "volType2"));
        fd1.setParameterInDocSearch(true);
        fd1.setParameterInVolSearch(false);

        FieldDefinition fd2 = new FieldDefinition(getFieldDefinitionsRoot());
        fd2.setName("testFieldDefName2");
        fd2.setFieldId(QName.createQName(DocumentDynamicModel.URI, "testFieldId2"));
        fd2.setSystematic(false);
        fd2.setDocTypes(Arrays.asList("type2"));
        fd2.setParameterOrderInDocSearch(3);
        fd2.setParameterOrderInVolSearch(4);
        fd2.setVolTypes(Arrays.asList("volType2"));
        fd2.setParameterInDocSearch(false);
        fd2.setParameterInVolSearch(true);

        FieldDefinition fd3 = new FieldDefinition(getFieldDefinitionsRoot());
        fd3.setName("testFieldDefName3");
        fd3.setFieldId(QName.createQName(DocumentDynamicModel.URI, "testFieldId3"));
        fd3.setSystematic(true);
        fd3.setDocTypes(Collections.<String> emptyList());
        fd3.setParameterOrderInDocSearch(1);
        fd3.setParameterOrderInVolSearch(2);
        // fd3.setVolTypes(null);
        fd3.setVolTypes(Collections.<String> emptyList());
        fd3.setParameterInDocSearch(true);
        fd3.setParameterInVolSearch(false);

        FieldDefinition fd4 = new FieldDefinition(getFieldDefinitionsRoot());
        fd4.setName("testFieldDefName4");
        fd4.setFieldId(QName.createQName(DocumentDynamicModel.URI, "testFieldId4"));
        fd4.setSystematic(false);
        fd4.setDocTypes(null);
        fd4.setParameterOrderInDocSearch(3);
        fd4.setParameterOrderInVolSearch(4);
        fd4.setVolTypes(Arrays.asList("volType2"));
        fd4.setParameterInDocSearch(false);
        fd4.setParameterInVolSearch(true);

        List<FieldDefinition> fieldDefinitions = Arrays.asList(fd1, fd2, fd3, fd4);
        for (FieldDefinition fDef : fieldDefinitions) {
            saveOrUpdateField(fDef);
        }
        return fieldDefinitions;
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

    private void updateOrRemoveLatestDocTypeVersion(DocumentType docType) {
        int versionNr = 1;
        boolean saved = docType.isSaved();
        if (saved) {
            Integer latestVersion = docType.getLatestVersion();
            ChildrenList<DocumentTypeVersion> documentTypeVersions = docType.getDocumentTypeVersions();
            ChildrenList<MetadataItem> savedMetadata = documentTypeVersions.get(latestVersion - 2).getMetadata();
            DocumentTypeVersion latestDocumentTypeVersion = docType.getLatestDocumentTypeVersion();
            ChildrenList<MetadataItem> unSavedMetadata = latestDocumentTypeVersion.getMetadata();
            if (!MetadataItemCompareUtil.clidrenListChanged(savedMetadata, unSavedMetadata)) {
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
        Map<QName, FieldDefinition> fieldDefinitions = getFieldDefinitionsByFieldIds();
        // save new fields to fieldDefinitions
        for (Field field : docVer.getFieldsDeeply()) {
            if (!field.isCopyFromPreviousDocTypeVersion()) {
                // field is not newer version of the same field under previous version of DocumentTypeVersion
                FieldDefinition fieldDef = fieldDefinitions.get(field.getFieldId());
                if (fieldDef != null) {
                    Assert.isTrue(field.getFieldTypeEnum().equals(fieldDef.getFieldTypeEnum()), "fieldDef and new field should have same fieldType");
                    // field is added based on existing fieldDefinition
                    List<String> docTypesOfFieldDef = fieldDef.getDocTypes();
                    if (!docTypesOfFieldDef.contains(docType.getDocumentTypeId())) {
                        docTypesOfFieldDef.add(docType.getDocumentTypeId());
                        fieldDef = saveOrUpdateField(fieldDef);
                    }
                } else {
                    // added new field (not based on fieldDefinition)
                    fieldDef = createFieldDefinition(field);
                    fieldDef.getDocTypes().add(docType.getDocumentTypeId());
                    fieldDef = saveOrUpdateField(fieldDef);
                }
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
