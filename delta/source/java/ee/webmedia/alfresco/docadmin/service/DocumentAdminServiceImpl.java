package ee.webmedia.alfresco.docadmin.service;

import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.importer.ImportTimerProgress;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import de.schlichtherle.io.FileInputStream;
import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;

/**
 * @author Ats Uiboupin
 */
public class DocumentAdminServiceImpl implements DocumentAdminService, InitializingBean {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentAdminServiceImpl.class);

    public static final QName PROP_DONT_SAVED_DOC_TYPE_VER = RepoUtil.createTransientProp("dontSaveDocTypeVer");

    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private NodeService nodeService;
    private GeneralService generalService;
    private BaseService baseService;
    private MenuService menuService;
    private UserService userService;
    private DocumentSearchService documentSearchService;
    private ImporterService importerService;

    private NodeRef documentTypesRoot;
    private NodeRef fieldDefinitionsRoot;
    private NodeRef fieldGroupDefinitionsRoot;
    private Set<String> fieldPropNames;
    private final Set<String> forbiddenFieldIds = new HashSet<String>();

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
    public void registerForbiddenFieldId(String forbiddenFieldId) {
        Assert.notNull(forbiddenFieldId);
        Assert.isTrue(!forbiddenFieldIds.contains(forbiddenFieldId));
        forbiddenFieldIds.add(forbiddenFieldId);
    }

    @Override
    public Set<String> getForbiddenFieldIds() {
        return Collections.unmodifiableSet(forbiddenFieldIds);
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
        RepoUtil.copyProperties(fieldGroupDefinition.getNode().getProperties(), targetGroupProps);
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
        RepoUtil.copyProperties(fieldDefinition.getNode().getProperties(), targetFieldProps);
        targetFieldProps.keySet().retainAll(getFieldPropNames()); // remove properties that only fieldDefinition should have
        field.setCopyOfFieldDefinition(fieldDefinition);
    }

    private FieldDefinition createFieldDefinition(Field field) {
        FieldDefinition fieldDef = createNewUnSavedFieldDefinition();
        Map<String, Object> targetFieldProps = fieldDef.getNode().getProperties();
        RepoUtil.copyProperties(field.getNode().getProperties(), targetFieldProps);
        fieldDef.setOrder(null);
        return fieldDef;
    }

    private List<DocumentType> getAllDocumentTypes(final Boolean used) {
        return getAllDocumentTypes(used, getDocumentTypesRoot());
    }

    private List<DocumentType> getAllDocumentTypes(final Boolean used, NodeRef docTypesRootRef) {
        return baseService.getChildren(docTypesRootRef, DocumentType.class, new Predicate<DocumentType>() {
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
        if (isDocumentTypeUsed(documentType.getDocumentTypeId())) {
            throw new UnableToPerformException("docType_delete_failed_inUse");
        }
        nodeService.deleteNode(docTypeRef);
    }

    @Override
    public Pair<DocumentType, MessageData> saveOrUpdateDocumentType(DocumentType docTypeOriginal) {
        DocumentType docType = docTypeOriginal.clone();
        boolean wasUnsaved = docType.isUnsaved();

        // validating duplicated documentTypeId is done in baseService
        MessageData message = updateChildren(docType);
        checkFieldMappings(docType);
        baseService.saveObject(docType);

        updatePublicAdr(docType, wasUnsaved);
        // TODO optimization: probably menu doesn't always need to be updated
        menuService.menuUpdated();
        return Pair.newInstance(docType, message);
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

    @Override
    public Map<String, FieldDefinition> getFieldDefinitionsByFieldIds() {
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

        List<DocumentType> createdDocumentTypes = new ArrayList<DocumentType>();
        Set<MessageData> messages = new LinkedHashSet<MessageData>();
        for (Entry<String, Pair<String, Pair<Set<String>, Set<QName>>>> systematicDocumentType : systematicDocumentTypes.entrySet()) {
            Pair<DocumentType, MessageData> result = createSystematicDocumentType(systematicDocumentType.getKey(), systematicDocumentType.getValue().getFirst(),
                    systematicDocumentType.getValue().getSecond().getFirst(), Field.getLocalNames(systematicDocumentType.getValue().getSecond().getSecond()));
            DocumentType documentType = result.getFirst();
            createdDocumentTypes.add(documentType);
            MessageData messageData = result.getSecond();
            if (messageData != null) {
                messages.add(messageData);
            }
        }
        for (MessageData messageData : messages) {
            MessageUtil.addStatusMessage(messageData);
        }
        fieldGroupDefinitionsRoot = null;
        fieldDefinitionsRoot = null;

        // Update "docTypes" usage markings on real fieldDefinitions
        // Code above did that on temporary fieldDefinitions, not on real fieldDefinitions

        Map<String, FieldDefinition> fieldDefinitions = getFieldDefinitionsByFieldIds();
        Map<String, FieldDefinition> fieldDefinitionsToUpdate = new HashMap<String, FieldDefinition>();
        for (DocumentType docType : createdDocumentTypes) {
            for (Field field : docType.getLatestDocumentTypeVersion().getFieldsDeeply()) {
                FieldDefinition fieldDef = fieldDefinitions.get(field.getFieldId());
                if (fieldDef != null) {
                    Assert.isTrue(field.getFieldTypeEnum().equals(fieldDef.getFieldTypeEnum()));
                    // field is added based on existing fieldDefinition
                    List<String> docTypesOfFieldDef = fieldDef.getDocTypes();
                    if (!docTypesOfFieldDef.contains(docType.getDocumentTypeId())) {
                        docTypesOfFieldDef.add(docType.getDocumentTypeId());
                        fieldDefinitionsToUpdate.put(fieldDef.getFieldId(), fieldDef);
                    }
                }
            }
        }
        for (FieldDefinition fieldDef : fieldDefinitionsToUpdate.values()) {
            saveOrUpdateField(fieldDef);
        }
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

    private Pair<DocumentType, MessageData> createSystematicDocumentType(String documentTypeId, String documentTypeName
            , final Set<String> fieldGroupNames, final Collection<String> fieldDefinitionIds) {
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
        return saveOrUpdateDocumentType(docType);
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

    private MessageData updateChildren(DocumentType docType) {
        int versionNr = 1;
        boolean saved = docType.isSaved();
        if (saved) {
            DocumentTypeVersion latestDocumentTypeVersion = docType.getLatestDocumentTypeVersion();
            ChildrenList<MetadataItem> unSavedMetadata = latestDocumentTypeVersion.getMetadata();
            DocumentType latestDocTypeInRepo = getDocumentType(docType.getNodeRef());
            int docTypeVersions = latestDocTypeInRepo.getDocumentTypeVersions().size();
            Boolean dontSaveDocTypeVer = docType.getProp(PROP_DONT_SAVED_DOC_TYPE_VER);
            if (dontSaveDocTypeVer == null) {
                versionNr = latestDocTypeInRepo.getLatestVersion(); // someone might have saved meanwhile new version of the same docType
                Assert.isTrue(versionNr == docTypeVersions, "in repository DocumentType.latestVersion=" + versionNr + ", but actually it contains " + docTypeVersions + " versions");
            } else {
                Assert.isTrue(dontSaveDocTypeVer);
                versionNr = docTypeVersions;
            }
            boolean latestDocTypeVerSaved = latestDocumentTypeVersion.isSaved();
            if (!latestDocTypeVerSaved) {
                Assert.isTrue(dontSaveDocTypeVer == null);
                ChildrenList<DocumentTypeVersion> documentTypeVersions = docType.getDocumentTypeVersions();
                ChildrenList<MetadataItem> savedMetadata = documentTypeVersions.get(documentTypeVersions.size() - 2).getMetadata();
                if (!MetadataItemCompareUtil.isClidrenListChanged(savedMetadata, unSavedMetadata)) {
                    // metaData list is not changed, don't save new DocumentTypeVersion (currently as new latestDocumentTypeVersion)
                    documentTypeVersions.remove(latestDocumentTypeVersion);
                    docType.setLatestVersion(versionNr); // don't overwrite latest version number
                    return null;
                }
                versionNr++;
            } else {
                Assert.isTrue(dontSaveDocTypeVer);
            }
        }
        String userId = AuthenticationUtil.getFullyAuthenticatedUser();
        DocumentTypeVersion docVer = docType.getLatestDocumentTypeVersion();
        docVer.setCreatorId(userId);
        docVer.setCreatorName(userService.getUserFullName(userId));
        docVer.setVersionNr(versionNr);
        docType.setLatestVersion(versionNr);
        docVer.setCreatedDateTime(new Date(AlfrescoTransactionSupport.getTransactionStartTime()));
        Map<String, FieldDefinition> fieldDefinitions = getFieldDefinitionsByFieldIds();
        // save new fields to fieldDefinitions
        String documentTypeId = docType.getDocumentTypeId();
        boolean addFieldsAddedRemovedWarning = false;
        for (Field field : docVer.getFieldsDeeply()) {
            if (!field.isCopyFromPreviousDocTypeVersion()) {
                addFieldsAddedRemovedWarning = true;
                // field is not newer version of the same field under previous version of DocumentTypeVersion
                FieldDefinition fieldDef = fieldDefinitions.get(field.getFieldId());
                if (fieldDef != null) {
                    if (!field.getFieldTypeEnum().equals(fieldDef.getFieldTypeEnum())) {
                        throw new UnableToPerformException("field_details_error_docField_sameIdFieldDef_differentType"
                                , fieldDef.getFieldNameWithIdAndType(), field.getFieldNameWithIdAndType());
                    }
                    // field is added based on existing fieldDefinition
                    List<String> docTypesOfFieldDef = fieldDef.getDocTypes();
                    if (!docTypesOfFieldDef.contains(documentTypeId)) {
                        docTypesOfFieldDef.add(documentTypeId);
                        fieldDef = saveOrUpdateField(fieldDef);
                    }
                } else {
                    if (field.isCopyOfFieldDefinition()) {
                        field.setSystematic(false); // field is created based on fieldDefinition, but id is changed
                        field.setMandatoryForDoc(false);
                        field.setMandatoryForVol(false);
                    }
                    // added new field (not based on fieldDefinition)
                    fieldDef = createFieldDefinition(field);
                    fieldDef.getDocTypes().add(documentTypeId);
                    fieldDef = saveOrUpdateField(fieldDef);
                }
            }
        }

        List<String> removedFieldIds = deleteFieldMappings(docType, docVer);
        if (!removedFieldIds.isEmpty()) {
            addFieldsAddedRemovedWarning = true;
            for (String removedFieldId : removedFieldIds) {
                FieldDefinition removedFieldFD = getFieldDefinition(removedFieldId);
                removedFieldFD.getDocTypes().remove(documentTypeId);
                saveOrUpdateField(removedFieldFD);
            }
        }
        if (addFieldsAddedRemovedWarning) {
            return new MessageDataImpl(MessageSeverity.INFO, "docType_metadataList_changedWarning");
        }
        return null;
    }

    private void checkFieldMappings(DocumentType docType) {
        MessageDataWrapper feedback = new MessageDataWrapper();
        for (AssociationModel associationModel : docType.getAssociationModels(null)) {
            Map<String, List<String>> usedFromFields = new HashMap<String, List<String>>();
            Map<String, List<String>> usedToFields = new HashMap<String, List<String>>();
            for (FieldMapping fieldMapping : associationModel.getFieldMappings()) {
                addUsedField(fieldMapping.getFromField(), fieldMapping.getToField(), usedFromFields);
                addUsedField(fieldMapping.getToField(), fieldMapping.getFromField(), usedToFields);
            }
            addErrorsIfNeeded(docType, associationModel, true, usedFromFields, feedback);
            addErrorsIfNeeded(docType, associationModel, false, usedToFields, feedback);
        }

        if (feedback.hasErrors()) {
            throw new UnableToPerformMultiReasonException(feedback);
        }
    }

    private void addErrorsIfNeeded(DocumentType docType, AssociationModel associationModel, boolean isFromSide, Map<String, List<String>> usedFields, MessageDataWrapper feedback) {
        for (Entry<String, List<String>> entry : usedFields.entrySet()) {
            List<String> fieldsList = entry.getValue();
            if (fieldsList.size() > 1) {
                String fieldId = entry.getKey();
                String assocType = StringUtils.uncapitalize(MessageUtil.getTypeName(associationModel.getNode().getType()));
                if (!isFromSide) {
                    feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "docType_save_error_multipleMappingsSameToField"
                            , docType.getDocumentTypeId(), assocType, associationModel.getDocType(), TextUtil.collectionToString(fieldsList), fieldId));
                } else {
                    feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "docType_save_error_multipleMappingsSameFromField"
                            , docType.getDocumentTypeId(), assocType, associationModel.getDocType(), fieldId, TextUtil.collectionToString(fieldsList)));
                }
            }
        }
    }

    private void addUsedField(String field, String otherField, Map<String, List<String>> usedFields) {
        List<String> fieldsList = usedFields.get(field);
        if (fieldsList == null) {
            fieldsList = new ArrayList<String>();
            usedFields.put(field, fieldsList);
        }
        fieldsList.add(otherField);
    }

    private List<String> deleteFieldMappings(DocumentType docType, DocumentTypeVersion docVer) {
        List<String> removedFieldIds = docVer.getRemovedFieldIdsDeeply();
        if (removedFieldIds.isEmpty()) {
            return removedFieldIds; // don't need to delete any field mappings
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
        return removedFieldIds;
    }

    @Override
    public NodeRef getDocumentTypesRoot() {
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

    public void setImporterService(ImporterService importerService) {
        this.importerService = importerService;
    }

    /** To break Circular dependency */
    private AdrService getAdrService() {
        return BeanHelper.getAdrService();
    }

    @Override
    public void importDocumentTypes(File xmlFile) {
        new ImportHelper().importDocumentTypes(xmlFile);
    }

    /**
     * Importing {@link DocumentType}s: <br>
     * 1) based on xmlFile containing {@link DocumentType}s, create temp root node for them. <br>
     * 2) read into memory all {@link DocumentType}s under temp node <br>
     * 3) create each {@link DocumentType} (or merge with existing {@link DocumentType}) <br>
     * <br>
     * Main idea to import single documentTypeVersion: <br>
     * 1) create new {@link DocumentTypeVersion} (newLatestDocTypeVer) just like when editing {@link DocumentType} <br>
     * 2) add {@link Field}s and {@link FieldGroup}s to newLatestDocTypeVer (merge with existing field/fieldGroup if it exists) <br>
     * 3) remove {@link Field}s and {@link FieldGroup}s that existed in lastDocTypeVer, but don't exist under {@link DocumentTypeVersion} being imported <br>
     * 4) save newLatestDocTypeVer so that {@link FieldMapping}s under existing {@link AssociationModel}s get updated
     * (to delete {@link FieldMapping}s of fields that don't exist under {@link DocumentTypeVersion} being imported) <br>
     * <br>
     * After all {@link MetadataItem}s have been saved for each {@link DocumentTypeVersion} and former {@link AssociationModel}s of existing {@link DocumentType}s are updated: <br>
     * for each {@link DocumentTypeVersion}: <br>
     * 1) Add {@link AssociationModel}s or merge {@link FieldMapping}s under existing {@link AssociationModel}s <br>
     * 2) save {@link DocumentTypeVersion}: <br>
     * 
     * @author Ats Uiboupin
     */
    private class ImportHelper {

        void importDocumentTypes(File xmlFile) {
            LOG.info("Starting to import docTypes");
            QName assocQName = RepoUtil.createTransientProp("tmp");
            NodeRef tmpFolderRef = generalService.getNodeRef("/" + assocQName);
            QName assocType = ContentModel.ASSOC_CHILDREN;
            boolean tmpFolderExisted = tmpFolderRef != null;
            if (!tmpFolderExisted) {
                NodeRef rootRef = generalService.getNodeRef("/");
                tmpFolderRef = nodeService.createNode(rootRef, assocType, assocQName, ContentModel.TYPE_CONTAINER).getChildRef();
            }
            Reader fileReader = null;
            NodeRef importableDocTypesRootRef = null;
            try {
                Location location = new Location(tmpFolderRef);
                location.setChildAssocType(assocType);
                final NodeRef[] tmpDocumentTypesRef = new NodeRef[1];
                fileReader = new InputStreamReader(new FileInputStream(xmlFile), AppConstants.CHARSET);
                importerService.importView(fileReader, location, null, new ImportTimerProgress() {
                    @Override
                    public void nodeCreated(NodeRef nodeRef, NodeRef parentRef, QName assocName, QName childName) {
                        super.nodeCreated(nodeRef, parentRef, assocName, childName);
                        if (tmpDocumentTypesRef[0] == null) { // first node imported is the root of imported nodes
                            tmpDocumentTypesRef[0] = nodeRef;
                        }
                    }
                });
                importableDocTypesRootRef = tmpDocumentTypesRef[0];
                importDocumentTypes(importableDocTypesRootRef);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Failed to read data for importing parameters from uploaded file: '" + xmlFile.getAbsolutePath() + "'", e);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unsupported encoding of uploaded file: '" + xmlFile.getAbsolutePath() + "'", e);
            } finally {
                IOUtils.closeQuietly(fileReader);
                if (!tmpFolderExisted) {
                    nodeService.deleteNode(tmpFolderRef);
                }
                if (importableDocTypesRootRef != null && nodeService.exists(importableDocTypesRootRef)) {
                    nodeService.deleteNode(importableDocTypesRootRef);
                }
                LOG.info("Finished importing docTypes");
            }
        }

        private void importDocumentTypes(NodeRef importableDocTypesRootRef) {
            List<DocumentType> existingDocTypes = getAllDocumentTypes(null);
            Map<String, DocumentType> existingDocTypesById = new HashMap<String, DocumentType>(existingDocTypes.size());
            for (DocumentType docType : existingDocTypes) {
                existingDocTypesById.put(docType.getDocumentTypeId(), docType);
            }

            Map<String, DocumentType> importedDocTypesById = new HashMap<String, DocumentType>();
            Map<String, Pair<List<FollowupAssociation>, List<ReplyAssociation>>> imporableDocTypesById = new HashMap<String, Pair<List<FollowupAssociation>, List<ReplyAssociation>>>();
            // first add/merge metadataItems
            List<DocumentType> allDocumentTypes = getAllDocumentTypes(null, importableDocTypesRootRef);
            int totalDocTypes = allDocumentTypes.size();
            LOG.info("Starting to import metadata of " + totalDocTypes + " document types");
            int i = 0;
            Set<MessageData> messages = new LinkedHashSet<MessageData>();
            for (DocumentType importableDocType : allDocumentTypes) {
                i++;
                LOG.info("Starting to import metadata of " + i + "/" + totalDocTypes + ". document type: " + importableDocType.getNameAndId());
                String documentTypeId = importableDocType.getDocumentTypeId();
                DocumentType existingDocType = existingDocTypesById.get(documentTypeId);
                DocumentType importedDocType;
                { // in first phase of import don't save add associations so that assocs wouldn't be lost because of fields that might not be present on docType not jet
                    List<FollowupAssociation> followupAssocs = importableDocType.getFollowupAssociations();
                    List<ReplyAssociation> replyAssocs = importableDocType.getReplyAssociations();
                    List<FollowupAssociation> followUpsToImport = new ArrayList<FollowupAssociation>(followupAssocs);
                    List<ReplyAssociation> repliesToImport = new ArrayList<ReplyAssociation>(replyAssocs);
                    imporableDocTypesById.put(documentTypeId, Pair.newInstance(followUpsToImport, repliesToImport));
                    followupAssocs.clear();
                    repliesToImport.clear();
                }
                if (existingDocType != null) {
                    setProps(importableDocType, existingDocType);
                    final DocumentTypeVersion lastDocTypeVer = existingDocType.getLatestDocumentTypeVersion();
                    DocumentTypeVersion newLatestDocTypeVer = existingDocType.addNewLatestDocumentTypeVersion();
                    DocumentTypeVersion importableDocTypeVer = importableDocType.getLatestDocumentTypeVersion();

                    int sizeBefore = newLatestDocTypeVer.getMetadata().size();
                    mergeMetadaItems(importableDocTypeVer, lastDocTypeVer, newLatestDocTypeVer);
                    int sizeAfter = newLatestDocTypeVer.getMetadata().size();
                    LOG.debug("Adding " + (sizeAfter - sizeBefore) + " MetadataItems to existingDocType " + existingDocType.getNodeRef());
                    importedDocType = existingDocType;
                } else {
                    importableDocType.nextSaveToParent(getDocumentTypesRoot());
                    importedDocType = importableDocType;
                }
                Pair<DocumentType, MessageData> result = saveOrUpdateDocumentType(importedDocType);
                importedDocType = result.getFirst();
                MessageData messageData = result.getSecond();
                if (messageData != null) {
                    messages.add(messageData);
                }
                importedDocTypesById.put(documentTypeId, importedDocType);
            }
            for (MessageData messageData : messages) {
                MessageUtil.addStatusMessage(messageData);
            }
            Map<String, DocumentType> docTypesCache = new HashMap<String, DocumentType>(importedDocTypesById);
            Map<String /* docTypeId */, Set<String> /* docTypeFields */> docTypeFieldsCache = new HashMap<String, Set<String>>();

            i = 0;
            LOG.info("Starting to import associations of " + totalDocTypes + " document types");
            /** Add {@link AssociationModel}s or merge {@link FieldMapping}s under existing {@link AssociationModel}s <br> */
            MessageDataWrapper errorsMessageDataWrapper = null;
            for (Entry<String, Pair<List<FollowupAssociation>, List<ReplyAssociation>>> entry : imporableDocTypesById.entrySet()) {
                String documentTypeId = entry.getKey();
                i++;
                LOG.info("Starting to import associations of " + i + "/" + totalDocTypes + ". document type: " + documentTypeId);
                try {
                    Pair<List<FollowupAssociation>, List<ReplyAssociation>> importableDocTypeAssocs = entry.getValue();
                    DocumentType importedDocType = importedDocTypesById.get(documentTypeId);
                    List<ReplyAssociation> replies = importableDocTypeAssocs.getSecond();
                    LOG.info("Starting to import followup associations of " + i + "/" + totalDocTypes + ". document type: " + documentTypeId);
                    mergeAssocModels(DocTypeAssocType.FOLLOWUP, importableDocTypeAssocs.getFirst(), importedDocType, docTypesCache, docTypeFieldsCache);
                    LOG.info("Starting to import reply associations of " + i + "/" + totalDocTypes + ". document type: " + documentTypeId);
                    mergeAssocModels(DocTypeAssocType.REPLY, replies, importedDocType, docTypesCache, docTypeFieldsCache);
                    importedDocType.setProp(PROP_DONT_SAVED_DOC_TYPE_VER, true);
                    saveOrUpdateDocumentType(importedDocType);
                } catch (UnableToPerformMultiReasonException e) {
                    MessageDataWrapper messageDataWrapper = e.getMessageDataWrapper();
                    MessageUtil.logMessage(messageDataWrapper, LOG);
                    if (errorsMessageDataWrapper == null) {
                        errorsMessageDataWrapper = messageDataWrapper;
                    } else {
                        for (MessageData messageData : messageDataWrapper) {
                            errorsMessageDataWrapper.addFeedbackItem(messageData);
                        }
                    }
                }
            }
            if (errorsMessageDataWrapper != null) {
                throw new UnableToPerformMultiReasonException(errorsMessageDataWrapper);
            }
        }

        private void mergeAssocModels(DocTypeAssocType assocType, List<? extends AssociationModel> assocModels, DocumentType importedDocType
                , Map<String, DocumentType> docTypesCache, Map<String /* docTypeId */, Set<String> /* docTypeFields */> docTypeFieldsCache) {
            Map<String, AssociationModel> existingAssocsByDocType = new HashMap<String, AssociationModel>();
            for (AssociationModel existingAssoc : importedDocType.getAssociationModels(assocType)) {
                existingAssocsByDocType.put(existingAssoc.getDocType(), existingAssoc);
            }
            Set<String> fieldsById = getDocTypeFieldsFromCache(importedDocType.getDocumentTypeId(), docTypesCache, docTypeFieldsCache);
            for (AssociationModel importableAssocM : assocModels) {
                String targetDocType = importableAssocM.getDocType();
                Set<String> relatedDocTypeFieldsById = getDocTypeFieldsFromCache(targetDocType, docTypesCache, docTypeFieldsCache);
                AssociationModel existingAssocM = existingAssocsByDocType.get(targetDocType);
                ChildrenList<FieldMapping> importableFieldMappings = importableAssocM.getFieldMappings();
                if (existingAssocM == null) {
                    importableAssocM.nextSaveToParent(importedDocType);
                    for (Iterator<FieldMapping> it = importableFieldMappings.iterator(); it.hasNext();) {
                        FieldMapping importableFieldMapping = it.next();
                        String fromField = importableFieldMapping.getFromField();
                        if (!fieldsById.contains(fromField) || !relatedDocTypeFieldsById.contains(importableFieldMapping.getToField())) {
                            it.remove();
                            continue; // don't try to add fieldMappings for fields that don't exist
                        }
                    }
                } else {
                    // merge FieldMappings of existing AssociationModel
                    setProps(importableAssocM, existingAssocM);
                    Map<String, FieldMapping> existingFieldMappingsByFromField = new HashMap<String, FieldMapping>();
                    for (FieldMapping fieldMapping : existingAssocM.getFieldMappings()) {
                        existingFieldMappingsByFromField.put(fieldMapping.getFromField(), fieldMapping);
                    }
                    for (FieldMapping importableFieldMapping : importableFieldMappings) {
                        String fromField = importableFieldMapping.getFromField();
                        if (!fieldsById.contains(fromField) || !relatedDocTypeFieldsById.contains(importableFieldMapping.getToField())) {
                            continue; // don't try to add fieldMappings for fields that don't exist
                        }
                        FieldMapping existingFieldMapping = existingFieldMappingsByFromField.get(fromField);
                        if (existingFieldMapping != null) {
                            existingAssocM.getFieldMappings().remove(existingFieldMapping);
                        }
                        importableFieldMapping.nextSaveToParent(existingAssocM);
                    }
                }
            }
        }

        private Set<String> getDocTypeFieldsFromCache(String docType, Map<String, DocumentType> docTypesCache, Map<String, Set<String>> docTypeFieldsCache) {
            DocumentType documentType = docTypesCache.get(docType);
            if (documentType == null) {
                documentType = getDocumentType(docType);
                docTypesCache.put(docType, documentType);
            }
            if (documentType == null) {
                throw new RuntimeException("import file contains fieldMapping to documentType that doesn't exist - did someone manually corrupted import file?");
            }
            Set<String> relatedDocTypeFieldsById = docTypeFieldsCache.get(docType);
            if (relatedDocTypeFieldsById == null) {
                relatedDocTypeFieldsById = documentType.getLatestDocumentTypeVersion().getFieldsDeeplyById().keySet();
                docTypeFieldsCache.put(docType, relatedDocTypeFieldsById);
            }
            return relatedDocTypeFieldsById;
        }

        /**
         * @param importableDocTypeVer - documentType version being imported - saved to temp folder for importing
         * @param lastDocTypeVer - last documentType version saved bellow {@link DocumentType}
         * @param newLatestDocTypeVer - new unsaved documentType version to be saved bellow {@link DocumentType} that should receive metadataItems from importable documentType
         */
        private void mergeMetadaItems(DocumentTypeVersion importableDocTypeVer, DocumentTypeVersion lastDocTypeVer, DocumentTypeVersion newLatestDocTypeVer) {
            final Map<String, Field> existingFieldsById = newLatestDocTypeVer.getFieldsDeeplyById();
            final Map<String, FieldGroup> existingFieldGroupsByName = new HashMap<String, FieldGroup>();
            for (MetadataItem metadataItem : newLatestDocTypeVer.getMetadata()) {
                if (metadataItem instanceof FieldGroup) {
                    FieldGroup fg = (FieldGroup) metadataItem;
                    existingFieldGroupsByName.put(fg.getName(), fg);
                }
            }

            Map<String, Set<NodeRef>> neededNodeRefsByGroup = new HashMap<String, Set<NodeRef>>();

            for (MetadataItem metadataItem : importableDocTypeVer.getMetadata()) {
                // merge all fields and fieldGroups from importableDocTypeVer to newLatestDocTypeVer
                NodeRef saveableNodeRef;
                if (metadataItem instanceof FieldGroup) {
                    FieldGroup importableFGroup = (FieldGroup) metadataItem;
                    String fieldGroupName = importableFGroup.getName();
                    FieldGroup existingFGroup = existingFieldGroupsByName.get(fieldGroupName);
                    if (existingFGroup != null) {
                        // merge importableFGroup into existingFGroup
                        setProps(importableFGroup, existingFGroup);
                        // merge fields under importable fieldGroup to existingFGroup
                        for (Field importableFieldInGroup : importableFGroup.getFields()) {
                            NodeRef saveableFieldRef;
                            Field existingField = existingFieldsById.get(importableFieldInGroup.getFieldId());
                            if (existingField != null) {
                                MetadataContainer existingFieldParent = (MetadataContainer) existingField.getParent();
                                if (existingFieldParent instanceof FieldGroup && StringUtils.equals(fieldGroupName, ((FieldGroup) existingFieldParent).getName())) {
                                    // field existed in the same fieldGroup
                                    setProps(importableFieldInGroup, existingField);
                                    saveableFieldRef = existingField.getNodeRef();
                                } else {
                                    // field existed in other fieldGroup or directly bellow DocTypeVersion (not reusing field that is not under same parent)
                                    importableFieldInGroup.nextSaveToParent(existingFGroup);
                                    saveableFieldRef = importableFieldInGroup.getNodeRef();
                                }
                            } else {
                                // change parent so it would be saved to existingFGroup
                                importableFieldInGroup.nextSaveToParent(existingFGroup);
                                saveableFieldRef = importableFieldInGroup.getNodeRef();
                            }
                            addSaveableNodeRef(neededNodeRefsByGroup, fieldGroupName, saveableFieldRef);
                        }
                        saveableNodeRef = existingFGroup.getNodeRef();
                    } else {
                        // change parent so it would be saved to newLatestDocTypeVer
                        importableFGroup.nextSaveToParent(newLatestDocTypeVer);
                        saveableNodeRef = importableFGroup.getNodeRef();
                    }
                } else if (metadataItem instanceof Field) {
                    Field importableField = (Field) metadataItem;
                    Field existingField = existingFieldsById.get(importableField.getFieldId());
                    if (existingField != null) {
                        // merge importableField into existingField
                        setProps(importableField, existingField);
                        saveableNodeRef = existingField.getNodeRef();
                    } else {
                        // change parent so it would be saved to newLatestDocTypeVer
                        importableField.nextSaveToParent(newLatestDocTypeVer);
                        saveableNodeRef = importableField.getNodeRef();
                    }
                } else if (metadataItem instanceof SeparatorLine) {
                    saveableNodeRef = metadataItem.getNodeRef();
                } else {
                    throw new RuntimeException("Unexpected object bellow importable docTypeVersion:\nobject=" + metadataItem);
                }
                addSaveableNodeRef(neededNodeRefsByGroup, null, saveableNodeRef);
            }

            // remove nodeRefs not present under DocTypeVersion being imported
            for (Iterator<MetadataItem> it = newLatestDocTypeVer.getMetadata().iterator(); it.hasNext();) {
                MetadataItem metadataItem = it.next();
                if (metadataItem instanceof FieldGroup) {
                    FieldGroup fieldGroup = (FieldGroup) metadataItem;
                    Set<NodeRef> savedNodeRefsInFieldGroup = neededNodeRefsByGroup.get(fieldGroup.getName());
                    if (savedNodeRefsInFieldGroup == null) {
                        it.remove(); // this fieldGroup was not present under docTypeVersion that is being imported
                    } else {
                        for (Iterator<NodeRef> fieldsIt = savedNodeRefsInFieldGroup.iterator(); fieldsIt.hasNext();) {
                            NodeRef nodeRef = fieldsIt.next();
                            if (!savedNodeRefsInFieldGroup.contains(nodeRef)) {
                                fieldsIt.remove(); // this field was not present under the same fieldGroup
                            }
                        }
                    }
                } else if (metadataItem instanceof Field || metadataItem instanceof SeparatorLine) {
                    Set<NodeRef> savedNodeRefsInDocTypeVer = neededNodeRefsByGroup.get(null);
                    if (savedNodeRefsInDocTypeVer == null || !savedNodeRefsInDocTypeVer.contains(metadataItem.getNodeRef())) {
                        it.remove(); // this field/separator was not present under docTypeVersion that is being imported
                    }
                } else {
                    throw new RuntimeException("Unexpected object bellow importable docTypeVersion:\nobject=" + metadataItem);
                }
            }
        }

        private <B extends BaseObject> void setProps(B from, B to) {
            Map<String, Object> toNodeProps = to.getNode().getProperties();
            toNodeProps.clear();
            RepoUtil.copyProperties(from.getNode().getProperties(), toNodeProps);
        }

        private void addSaveableNodeRef(Map<String, Set<NodeRef>> neededNodeRefsByGroup, String groupName, NodeRef saveableNodeRef) {
            Set<NodeRef> groupRefs = neededNodeRefsByGroup.get(groupName);
            if (groupRefs == null) {
                groupRefs = new HashSet<NodeRef>();
                neededNodeRefsByGroup.put(groupName, groupRefs);
            }
            groupRefs.add(saveableNodeRef);
        }
    }

}
