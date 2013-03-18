package ee.webmedia.alfresco.docadmin.service;

import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.collections.comparators.TransformingComparator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.base.BaseService;
import ee.webmedia.alfresco.base.BaseServiceImpl;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docadmin.web.BaseObjectOrderModifier;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.menu.service.MenuService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.ComparableTransformer;
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

    private TransactionService transactionService;
    private NodeService nodeService;
    private GeneralService generalService;
    private BaseService baseService;
    private MenuService menuService;
    private UserService userService;
    private DocumentSearchService documentSearchService;
    private ImporterService importerService;

    private NodeRef fieldDefinitionsRoot;
    private NodeRef fieldGroupDefinitionsRoot;
    private Set<String> fieldPropNames;
    private final Set<String> forbiddenFieldIds = new HashSet<String>();
    private final Set<String> groupShowShowInTwoColumnsOriginalFieldIds = new HashSet<String>();
    private final Set<String> groupNamesLimitSingle = new HashSet<String>();

    /**
     * Get nodeRef lazily.
     * Workaround to NPE when trying to get nodeRef from afterProperties set
     * 
     * @author Ats Uiboupin
     */
    class NodeRefInitializer {
        private final String xPath;
        private NodeRef nodeRef;

        public NodeRefInitializer(String xPath) {
            this.xPath = xPath;
        }

        public NodeRef getNodeRef() {
            if (nodeRef == null) {
                nodeRef = generalService.getNodeRef(xPath);
            }
            return nodeRef;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        baseService.addTypeMapping(DocumentAdminModel.Types.DOCUMENT_TYPE, DocumentType.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.CASE_FILE_TYPE, CaseFileType.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.DOCUMENT_TYPE_VERSION, DocumentTypeVersion.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FIELD, Field.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FIELD_GROUP, FieldGroup.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.SEPARATION_LINE, SeparatorLine.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FIELD_DEFINITION, FieldDefinition.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FOLLOWUP_ASSOCIATION, FollowupAssociation.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.REPLY_ASSOCIATION, ReplyAssociation.class);
        baseService.addTypeMapping(DocumentAdminModel.Types.FIELD_MAPPING, FieldMapping.class);

        addTypeRootRefMappings(DocumentType.class, new NodeRefInitializer(DocumentAdminModel.Repo.DOCUMENT_TYPES_SPACE));
        addTypeRootRefMappings(CaseFileType.class, new NodeRefInitializer(DocumentAdminModel.Repo.CASE_FILE_TYPES_SPACE));
    }

    private final Map<Class<? extends BaseObject>, NodeRefInitializer> typeRootRefMappings = new HashMap<Class<? extends BaseObject>, NodeRefInitializer>();

    private void addTypeRootRefMappings(Class<? extends BaseObject> clazz, NodeRefInitializer initializer) {
        Assert.notNull(clazz, "class");
        Assert.notNull(initializer, "initializer");
        if (!BeanHelper.getApplicationService().isTest()) {
            // this check is disabled in development to allow JRebel do it's magic when reloading spring context
            Assert.isTrue(!typeRootRefMappings.containsKey(clazz), "rootRef by " + clazz.getSimpleName() + " is already mapped");
        }
        typeRootRefMappings.put(clazz, initializer);
    }

    @Override
    public <D extends DynamicType> NodeRef getDynamicTypesRoot(Class<D> dynTypeClass) {
        Assert.notNull(dynTypeClass, "dynTypeClass");
        NodeRefInitializer rootRefInitializer = typeRootRefMappings.get(dynTypeClass);
        Assert.notNull(rootRefInitializer, "rootRefInitializer");
        return rootRefInitializer.getNodeRef();
    }

    private NodeRef getDocumentTypesRoot() {
        return getDynamicTypesRoot(DocumentType.class);
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
    public List<DocumentType> getDocumentTypes(DynTypeLoadEffort effort) {
        return getAllDocumentTypes(null, getDocumentTypesRoot(), effort);
    }

    @Override
    public <T extends DynamicType> List<T> getTypes(Class<T> typeClass, DynTypeLoadEffort effort) {
        NodeRef typesRootRef = getDynamicTypesRoot(typeClass);
        return getAllTypes(typeClass, null, typesRootRef, effort);
    }

    @Override
    public List<DocumentType> getDocumentTypes(DynTypeLoadEffort effort, boolean used) {
        return getAllDocumentTypes(Boolean.valueOf(used), getDocumentTypesRoot(), effort);
    }

    @Override
    public DocumentTypeVersion getLatestDocTypeVer(String documentTypeId) {
        DocumentType documentType = getDocumentType(documentTypeId, DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
        DocumentTypeVersion docVer = documentType.getLatestDocumentTypeVersion();
        return docVer;
    }

    @Override
    public Map<String, DocumentTypeVersion> getLatestDocTypeVersions() {
        Map<String, DocumentTypeVersion> documentTypes = new HashMap<String, DocumentTypeVersion>();
        for (String documentTypeId : getDocumentTypeNames(null).keySet()) {
            documentTypes.put(documentTypeId, getLatestDocTypeVer(documentTypeId));
        }
        return documentTypes;
    }

    @Override
    public DocumentType getDocumentType(String id, DynTypeLoadEffort effort) {
        NodeRef documentTypeRef = getDocumentTypeRef(id);
        return getDocumentType(documentTypeRef, effort);
    }

    @Override
    public <T> T getDocumentTypeProperty(String docTypeId, QName property, Class<T> returnClass) {
        NodeRef documentTypeRef = getDocumentTypeRef(docTypeId);
        return getDocumentTypeProperty(documentTypeRef, property, returnClass);
    }

    @Override
    public <T> T getDocumentTypeProperty(NodeRef documentTypeRef, QName property, Class<T> returnClass) {
        Serializable value = nodeService.getProperty(documentTypeRef, property);
        if (Boolean.class.equals(returnClass)) {
            value = NodeBaseVO.convertNullToFalse((Boolean) value);
        }
        return DefaultTypeConverter.INSTANCE.convert(returnClass, value);
    }

    @Override
    public NodeRef getDocumentTypeRef(String id) {
        return generalService.getNodeRef(DocumentType.getAssocName(id).toString(), getDocumentTypesRoot());
    }

    @Override
    public Pair<DocumentType, DocumentTypeVersion> getDocumentTypeAndVersion(String docTypeId, Integer docTypeVersionNr) {
        DocumentType docType = getDocumentType(docTypeId, DocumentAdminService.DOC_TYPE_WITH_OUT_GRAND_CHILDREN);
        if (docType == null) {
            return null;
        }
        DocumentTypeVersion docVersion = null;
        for (DocumentTypeVersion version : docType.getDocumentTypeVersions()) {
            if (docTypeVersionNr == version.getVersionNr()) {
                baseService.loadChildren(version, null);
                docVersion = version;
                break;
            }
        }
        if (docVersion == null) {
            return null;
        }
        return new Pair<DocumentType, DocumentTypeVersion>(docType, docVersion);
    }

    private DocumentType getDocumentType(NodeRef docTypeRef, DynTypeLoadEffort effort) {
        return getDynamicType(DocumentType.class, docTypeRef, effort);
    }

    @Override
    public <D extends DynamicType> D getDynamicType(Class<D> dynTypeClass, NodeRef dynTypeRef, DynTypeLoadEffort effort) {
        // FIXME DLSeadist - Kui kõik süsteemsed dok.liigid on defineeritud, siis võib null kontrolli ja tagastamise eemdaldada
        if (dynTypeRef == null) {
            return null;
        }
        D object = baseService.getObject(dynTypeRef, dynTypeClass, effort);
        if (effort != null && effort.isReturnLatestDynTypeVersionChildren()) {
            DocumentTypeVersion latestDocTypeVersion = object.getLatestDocumentTypeVersion();
            baseService.loadChildren(latestDocTypeVersion, null);
        }
        return object;
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
    public String getDocumentTypeName(Node document) {
        String documentTypeId = (String) document.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
        return getDocumentTypeName(documentTypeId);
    }

    @Override
    public Pair<String, String> getDocumentTypeNameAndId(Node document) {
        String documentTypeId = (String) document.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
        return new Pair<String, String>(getDocumentTypeName(documentTypeId), documentTypeId);
    }

    @Override
    public Map<String/* docTypeId */, String/* docTypeName */> getDocumentTypeNames(Boolean used) {
        Map<String, String> docTypesByDocTypeId = new HashMap<String, String>();
        for (ChildAssociationRef childAssoc : nodeService.getChildAssocs(getDocumentTypesRoot())) {
            Map<QName, Serializable> props = nodeService.getProperties(childAssoc.getChildRef());
            Boolean documentTypeUsed = (Boolean) props.get(DocumentAdminModel.Props.USED);
            if (used == null || documentTypeUsed == used) {
                String documentTypeId = (String) props.get(DocumentAdminModel.Props.ID);
                String documentTypeName = (String) props.get(DocumentAdminModel.Props.NAME);
                docTypesByDocTypeId.put(documentTypeId, documentTypeName);
            }
        }
        return docTypesByDocTypeId;
    }

    @Override
    public void addSystematicMetadataItems(DocumentTypeVersion docVer) {
        DynamicType dynType = docVer.getParent();
        final boolean dynTypeIsCaseFile = dynType instanceof CaseFileType;
        if (!dynTypeIsCaseFile) {
            Assert.isTrue(dynType instanceof DocumentType, "Parent of dynamic type version should be DocumentType (if it is not CaseFileType)");
        }
        addMetadataItems(docVer, new Predicate<FieldGroup>() {
            @Override
            public boolean eval(FieldGroup sourceGroup) {
                return dynTypeIsCaseFile ? sourceGroup.isMandatoryForVol() : sourceGroup.isMandatoryForDoc();
            }
        }, new Predicate<FieldDefinition>() {
            @Override
            public boolean eval(FieldDefinition sourceFieldDef) {
                return dynTypeIsCaseFile ? sourceFieldDef.isMandatoryForVol() : sourceFieldDef.isMandatoryForDoc();
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

    private List<DocumentType> getAllDocumentTypes(final Boolean used, NodeRef docTypesRootRef, DynTypeLoadEffort effort) {
        return getAllTypes(DocumentType.class, used, docTypesRootRef, effort);
    }

    private <T extends DynamicType> List<T> getAllTypes(Class<T> typeClass, final Boolean used, NodeRef docTypesRootRef, DynTypeLoadEffort effort) {
        return baseService.getChildren(docTypesRootRef, typeClass, new Predicate<T>() {
            @Override
            public boolean eval(T dynType) {
                return used == null || dynType.isUsed() == used;
            }
        }, effort);
    }

    @Override
    public <D extends DynamicType> D createNewUnSavedDynamicType(Class<D> dynamicTypeClass) {
        NodeRef rootRef = getDynamicTypesRoot(dynamicTypeClass);
        if (DocumentType.class.equals(dynamicTypeClass)) {
            @SuppressWarnings("unchecked")
            D tmp = (D) new DocumentType(rootRef);
            return tmp;
        } else if (CaseFileType.class.equals(dynamicTypeClass)) {
            @SuppressWarnings("unchecked")
            D tmp = (D) new CaseFileType(rootRef);
            return tmp;
        } else {
            throw new RuntimeException("Unimplemented crating new unSaved DynamicType " + dynamicTypeClass.getCanonicalName());
        }
    }

    @Override
    public FieldDefinition createNewUnSavedFieldDefinition() {
        return new FieldDefinition(getFieldDefinitionsRoot());
    }

    @Override
    public void deleteDynamicType(NodeRef docTypeRef) {
        DynamicType dynType = getDynamicType(null, docTypeRef, DOC_TYPE_WITHOUT_OLDER_DT_VERSION_CHILDREN);
        if (dynType instanceof DocumentType) {
            DocumentType docType = (DocumentType) dynType;
            if (docType.isSystematic()) {
                throw new IllegalArgumentException("docType_list_action_delete_failed_systematic"); // shouldn't happen, because systematic can't be changed at runtime
            }
            if (isDocumentTypeUsed(docType.getId())) {
                throw new UnableToPerformException("docType_delete_failed_inUse");
            }
        } else {
            Assert.isTrue(CaseFileType.class.equals(dynType.getClass()), "expected that deletable node is CaseFileType (if it is not DocumentType)");
            if (isCaseFileTypeUsed(dynType.getId())) {
                throw new UnableToPerformException("caseFileType_delete_failed_inUse");
            }
        }
        { // remove references to dynamicType from all fieldDefinitions
            String documentTypeId = dynType.getId();
            Set<String> fieldIds = dynType.getLatestDocumentTypeVersion().getFieldsDeeplyById().keySet();
            Map<String, FieldDefinition> fieldDefinitionsByFieldIds = getFieldDefinitionsByFieldIds();
            Map<String, FieldDefinition> fieldsToSave = new HashMap<String, FieldDefinition>(fieldIds.size());
            for (String fieldId : fieldIds) {
                FieldDefinition fieldDef = fieldDefinitionsByFieldIds.get(fieldId);
                if (fieldDef != null && fieldDef.getUsedTypes(dynType.getClass()).remove(documentTypeId)) {
                    fieldsToSave.put(fieldDef.getFieldId(), fieldDef);
                }
            }
            saveOrUpdateFieldDefinitions(fieldsToSave.values());
        }
        nodeService.deleteNode(docTypeRef);
        if (dynType.isUsed()) {
            menuService.menuUpdated();
        }
    }

    @Override
    public Pair<DocumentType, MessageData> saveOrUpdateDocumentType(DocumentType docTypeOriginal) {
        return saveOrUpdateDynamicType(docTypeOriginal);
    }

    @Override
    public <D extends DynamicType> Pair<D, MessageData> saveOrUpdateDynamicType(D dynTypeOriginal) {
        @SuppressWarnings("unchecked")
        D dynType = (D) dynTypeOriginal.clone();
        boolean wasUnsaved = dynType.isUnsaved();

        // validating duplicated documentTypeId is done in baseService
        MessageData message = updateChildren(dynType);
        DocumentType docType = dynType instanceof DocumentType ? (DocumentType) dynType : null;
        if (docType != null) {
            checkFieldMappings(docType);
        }
        if (docType != null) {
            updatePublicAdr(docType, wasUnsaved);
        }
        baseService.saveObject(dynType);
        if (wasUnsaved || dynTypeOriginal.isPropertyChanged(DocumentAdminModel.Props.USED, DocumentAdminModel.Props.NAME, DocumentAdminModel.Props.MENU_GROUP_NAME)) {
            menuService.menuUpdated();
        }
        return Pair.newInstance(dynType, message);
    }

    @Override
    public <F extends Field> F saveOrUpdateField(F originalFieldOrFeildDef) {
        F fieldOrFeildDef = saveOrUpdateFieldInternal(originalFieldOrFeildDef);
        if (originalFieldOrFeildDef instanceof FieldDefinition) {
            FieldDefinition fieldDef = (FieldDefinition) fieldOrFeildDef;
            @SuppressWarnings("unchecked")
            F tmp = (F) reorderFieldDefinitions(fieldDef);
            return tmp;
        }
        return fieldOrFeildDef;
    }

    private <F extends Field> F saveOrUpdateFieldInternal(F originalFieldOrFeildDef) {
        @SuppressWarnings("unchecked")
        F fieldOrFeildDef = (F) originalFieldOrFeildDef.clone();
        baseService.saveObject(fieldOrFeildDef);
        return fieldOrFeildDef;
    }

    private FieldDefinition reorderFieldDefinitions(FieldDefinition fieldDef) {
        // must reorder fieldDefinitions list
        List<FieldDefinition> fieldDefinitions = saveOrUpdateFieldDefinitions(getFieldDefinitions());
        // return fresh copy of originalFieldOrFeildDef
        for (FieldDefinition fd : fieldDefinitions) {
            if (fd.getFieldId().equals(fieldDef.getFieldId())) {
                return fd;
            }
        }
        throw new IllegalStateException("This shouldn't happen - didn't find field that was just saved");
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
    public List<FieldDefinition> saveOrUpdateFieldDefinitions(Collection<FieldDefinition> fieldDefinitions) {
        FieldDefinitionReorderHelper.reorderDocSearchAndVolSearchProps(fieldDefinitions, true);
        List<FieldDefinition> saved = new ArrayList<FieldDefinition>();

        for (FieldDefinition fieldDefinition : fieldDefinitions) {
            saved.add(saveOrUpdateFieldInternal(fieldDefinition));
        }
        return saved;
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions() {
        List<FieldDefinition> fd = baseService.getChildren(getFieldDefinitionsRoot(), FieldDefinition.class);
        { // in case order is uninitialized (for example after initial import) order by name - this will determine the resulting order of elements that have no order
            @SuppressWarnings("unchecked")
            Comparator<FieldDefinition> byNameComparator = new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
                @Override
                public Comparable<?> tr(FieldDefinition input) {
                    return input.getName().toLowerCase();
                }
            });
            Collections.sort(fd, byNameComparator);
        }
        FieldDefinitionReorderHelper.reorderDocSearchAndVolSearchProps(fd, false);
        return fd;
    }

    /**
     * Helps to initialize order or reorder only some items - so that other items will not receive automatically order
     * 
     * @author Ats Uiboupin
     */
    private static class FieldDefinitionReorderHelper {

        static void reorderDocSearchAndVolSearchProps(Collection<FieldDefinition> fd, boolean markBaseCalled) {
            Predicate<FieldDefinition> isParameterInDocSearchPredicate = new Predicate<FieldDefinition>() {
                @Override
                public boolean eval(FieldDefinition o) {
                    return o.isParameterInDocSearch();
                }
            };
            Predicate<FieldDefinition> isParameterInVolSearchPredicate = new Predicate<FieldDefinition>() {
                @Override
                public boolean eval(FieldDefinition o) {
                    return o.isParameterInVolSearch();
                }
            };
            reorderAndMarkBaseState(fd, getByDocSearchOrderModifier(), markBaseCalled, isParameterInDocSearchPredicate);
            reorderAndMarkBaseState(fd, getByVolSearchOrderModifier(), markBaseCalled, isParameterInVolSearchPredicate);
        }

        private static BaseObjectOrderModifier<FieldDefinition> getByVolSearchOrderModifier() {
            return DocAdminUtil.getMetadataItemReorderHelper(DocumentAdminModel.Props.PARAMETER_ORDER_IN_VOL_SEARCH);
        }

        private static BaseObjectOrderModifier<FieldDefinition> getByDocSearchOrderModifier() {
            return DocAdminUtil.getMetadataItemReorderHelper(DocumentAdminModel.Props.PARAMETER_ORDER_IN_DOC_SEARCH);
        }

        /**
         * Helps to initialize order or reorder only items selected by includedItemsPredicate - so that other items will not receive automatically order
         */
        private static void reorderAndMarkBaseState(Collection<FieldDefinition> items, BaseObjectOrderModifier<FieldDefinition> modifier,
                boolean markBaseCalled, Predicate<FieldDefinition> includedItemsPredicate) {
            List<FieldDefinition> docSearchFieldsList = select(items, includedItemsPredicate, new ArrayList<FieldDefinition>());
            if (!markBaseCalled) {
                // markBaseState should be called at least once before reordering
                modifier.markBaseState(docSearchFieldsList);
            }
            DocAdminUtil.reorderAndMarkBaseState(docSearchFieldsList, modifier);
        }

        /**
         * Type safe version of {@link CollectionUtils#selectRejected(Collection, org.apache.commons.collections.Predicate, Collection)} <br>
         * that returns given outputCollection filled by elements that match given predicate
         */
        private static <T, C extends Collection<T>> C select(Collection<T> inputCollection, Predicate<T> predicate, C outputCollection) {
            CollectionUtils.select(inputCollection, predicate, outputCollection);
            return outputCollection;
        }
    }

    @Override
    public List<FieldDefinition> getSearchableFieldDefinitions() {
        List<FieldDefinition> searchable = new ArrayList<FieldDefinition>();
        for (FieldDefinition fieldDefinition : baseService.getChildren(getFieldDefinitionsRoot(), FieldDefinition.class)) {
            if (fieldDefinition.isParameterInDocSearch()) {
                searchable.add(fieldDefinition);
            }
        }
        Collections.sort(searchable, SEARCH_FIELD_COMPARATOR);
        return searchable;
    }

    @SuppressWarnings("unchecked")
    public static final Comparator<FieldDefinition> SEARCH_FIELD_COMPARATOR = new TransformingComparator(new ComparableTransformer<FieldDefinition>() {
        @Override
        public Comparable<?> tr(FieldDefinition input) {
            return input.getParameterOrderInDocSearch();
        }
    }, new NullComparator(true));

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
        return documentSearchService.isMatchAllStoresWithTrashcan(
                joinQueryPartsAnd(
                        joinQueryPartsAnd(
                                generateTypeQuery(DocumentCommonModel.Types.DOCUMENT)
                                , generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE)
                        )
                        , generateStringExactQuery(documentTypeId, Props.OBJECT_TYPE_ID))
                );
    }

    @Override
    public boolean isFieldDefintionUsed(String fieldId) {
        FieldDefinition fieldDefinition = getFieldDefinition(fieldId);
        if (fieldDefinition == null) {
            return false;
        }
        List<String> docTypes = fieldDefinition.getDocTypes();
        if (docTypes != null) {
            for (String docTypeId : docTypes) {
                if (getDocumentTypeProperty(docTypeId, DocumentAdminModel.Props.USED, Boolean.class)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isCaseFileTypeUsed(String caseFileTypeId) {
        return false; // TODO CL_TASK 183635
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
                    if (!docTypesOfFieldDef.contains(docType.getId())) {
                        docTypesOfFieldDef.add(docType.getId());
                        fieldDefinitionsToUpdate.put(fieldDef.getFieldId(), fieldDef);
                    }
                }
            }
        }
        saveOrUpdateFieldDefinitions(fieldDefinitionsToUpdate.values());
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
        DocumentType docType = createNewUnSavedDynamicType(DocumentType.class);
        docType.setId(documentTypeId);
        docType.setName(documentTypeName);
        docType.setSystematic(true);
        DocumentTypeVersion ver = docType.addNewLatestDocumentTypeVersion();
        addMetadataItems(ver, new Predicate<FieldGroup>() {
            @Override
            public boolean eval(FieldGroup sourceGroup) {
                return fieldGroupNames.contains(sourceGroup.getName());
            }
        }, new Predicate<FieldDefinition>() {
            @Override
            public boolean eval(FieldDefinition sourceFieldDef) {
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
            if (metadataItem instanceof FieldAndGroupBase) {
                ((FieldAndGroupBase) metadataItem).setRemovableFromSystematicDocType(false);
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
                LOG.debug("Changing publicAdr of DocumentType " + docType.getId() + " from "
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

    private <D extends DynamicType> MessageData updateChildren(D dynType) {
        int versionNr = 1;
        boolean saved = dynType.isSaved();
        if (saved) {
            DocumentTypeVersion latestDocumentTypeVersion = dynType.getLatestDocumentTypeVersion();
            ChildrenList<MetadataItem> unSavedMetadata = latestDocumentTypeVersion.getMetadata();
            DynamicType latestDocTypeInRepo = getDynamicType(dynType.getClass(), dynType.getNodeRef(), DOC_TYPE_WITH_OUT_GRAND_CHILDREN);
            int docTypeVersions = latestDocTypeInRepo.getDocumentTypeVersions().size();
            Boolean dontSaveDocTypeVer = dynType.getProp(PROP_DONT_SAVED_DOC_TYPE_VER);
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
                ChildrenList<DocumentTypeVersion> documentTypeVersions = dynType.getDocumentTypeVersions();
                { // no need to inspect previous versions of DocType when saving
                    for (DocumentTypeVersion documentTypeVersion : documentTypeVersions) {
                        if (documentTypeVersion != latestDocumentTypeVersion) {
                            documentTypeVersion.setProp(BaseServiceImpl.SKIP_SAVE, true);
                        }
                    }
                }
                ChildrenList<MetadataItem> savedMetadata = documentTypeVersions.get(documentTypeVersions.size() - 2).getMetadata();
                if (!MetadataItemCompareUtil.isClidrenListChanged(savedMetadata, unSavedMetadata)) {
                    // metaData list is not changed, don't save new DocumentTypeVersion (currently as new latestDocumentTypeVersion)
                    documentTypeVersions.remove(latestDocumentTypeVersion);
                    dynType.setLatestVersion(versionNr); // don't overwrite latest version number
                    return null;
                }
                versionNr++;
            } else {
                Assert.isTrue(dontSaveDocTypeVer);
            }
        }
        String userId = AuthenticationUtil.getFullyAuthenticatedUser();
        DocumentTypeVersion docVer = dynType.getLatestDocumentTypeVersion();
        docVer.setCreatorId(userId);
        docVer.setCreatorName(userService.getUserFullName(userId));
        docVer.setVersionNr(versionNr);
        dynType.setLatestVersion(versionNr);
        docVer.setCreatedDateTime(new Date(AlfrescoTransactionSupport.getTransactionStartTime()));
        Map<String, FieldDefinition> fieldDefinitions = getFieldDefinitionsByFieldIds();
        // save new fields to fieldDefinitions
        String documentTypeId = dynType.getId();
        boolean addFieldsAddedRemovedWarning = false;
        List<Field> docTypeVerFields = docVer.getFieldsDeeply();
        Map<String, FieldDefinition> fieldsToSave = new HashMap<String, FieldDefinition>(docTypeVerFields.size());
        for (Field field : docTypeVerFields) {
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
                    List<String> docTypesOfFieldDef = fieldDef.getUsedTypes(dynType.getClass());
                    if (!docTypesOfFieldDef.contains(documentTypeId)) {
                        docTypesOfFieldDef.add(documentTypeId);
                        fieldsToSave.put(fieldDef.getFieldId(), fieldDef);
                    }
                } else {
                    if (field.isCopyOfFieldDefinition()) {
                        field.setSystematic(false); // field is created based on fieldDefinition, but id is changed
                        field.setMandatoryForDoc(false);
                        field.setMandatoryForVol(false);
                    }
                    // added new field (not based on fieldDefinition)
                    fieldDef = createFieldDefinition(field);
                    fieldDef.getUsedTypes(dynType.getClass()).add(documentTypeId);
                    fieldsToSave.put(fieldDef.getFieldId(), fieldDef);
                }
            }
        }

        List<String> removedFieldIds = docVer.getRemovedFieldIdsDeeply();
        if (dynType instanceof DocumentType) {
            DocumentType documentType = (DocumentType) dynType;
            removedFieldIds = deleteFieldMappings(documentType, removedFieldIds);
        }
        if (!removedFieldIds.isEmpty()) {
            addFieldsAddedRemovedWarning = true;
            for (String removedFieldId : removedFieldIds) {
                FieldDefinition removedFieldFD = fieldsToSave.get(removedFieldId);
                if (removedFieldFD == null) {
                    removedFieldFD = getFieldDefinition(removedFieldId);
                    fieldsToSave.put(removedFieldId, removedFieldFD);
                }
                removedFieldFD.getUsedTypes(dynType.getClass()).remove(documentTypeId);
            }
        }
        saveOrUpdateFieldDefinitions(fieldsToSave.values());
        if (addFieldsAddedRemovedWarning && dynType instanceof DocumentType) {
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
                            , docType.getId(), assocType, associationModel.getDocType(), TextUtil.collectionToString(fieldsList), fieldId));
                } else {
                    feedback.addFeedbackItem(new MessageDataImpl(MessageSeverity.ERROR, "docType_save_error_multipleMappingsSameFromField"
                            , docType.getId(), assocType, associationModel.getDocType(), fieldId, TextUtil.collectionToString(fieldsList)));
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

    private List<String> deleteFieldMappings(DocumentType docType, List<String> removedFieldIds) {
        if (removedFieldIds.isEmpty()) {
            return removedFieldIds; // don't need to delete any field mappings
        }
        String docTypeId = docType.getId();
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
                , generateStringExactQuery(docTypeId, DocumentAdminModel.Props.DOC_TYPE)
                );
        List<NodeRef> associatedDocTypes = documentSearchService.searchNodes(query, -1, "searchAssocsToDocType:" + docTypeId);

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

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
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
    public ImportHelper getImportHelper() {
        return new ImportHelper();
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
    public class ImportHelper {

        /**
         * Import dynamic types. NB! This method creates transactions itself, so do NOT use surrounding transactions!
         * 
         * @param <D>
         * @param xmlFile
         * @param dynTypeClass
         */
        public <D extends DynamicType> void importDynamicTypes(final File xmlFile, final Class<D> dynTypeClass) {
            Assert.isTrue(AlfrescoTransactionSupport.getTransactionId() == null, "Surrounding transaction must not be present!");

            RetryingTransactionHelper txHelper = transactionService.getRetryingTransactionHelper();
            final Pair<NodeRef, Pair<NodeRef, Boolean>> result = txHelper.doInTransaction(new RetryingTransactionCallback<Pair<NodeRef, Pair<NodeRef, Boolean>>>() {
                @Override
                public Pair<NodeRef, Pair<NodeRef, Boolean>> execute() throws Throwable {
                    return importDynamicTypesToTemp(xmlFile, dynTypeClass);
                }
            });
            final NodeRef importableDocTypesRootRef = result.getFirst();
            final NodeRef tmpFolderRef = result.getSecond().getFirst();
            final Boolean tmpFolderExisted = result.getSecond().getSecond();

            try {
                txHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                    @Override
                    public Void execute() throws Throwable {
                        importDynamicTypes(importableDocTypesRootRef, dynTypeClass);
                        return null;
                    }
                });
            } finally {
                txHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                    @Override
                    public Void execute() throws Throwable {
                        cleanupTypeImportTempData(tmpFolderRef, tmpFolderExisted, importableDocTypesRootRef);
                        return null;
                    }
                });
            }
        }

        private <D> Pair<NodeRef, Pair<NodeRef, Boolean>> importDynamicTypesToTemp(final File xmlFile, final Class<D> dynTypeClass) {
            final String dynTypeMsg = dynTypeClass.getSimpleName() + "s";
            LOG.info("Importing " + dynTypeMsg + " from file '" + xmlFile.getAbsolutePath() + "' into temporary location");
            QName assocQName = RepoUtil.createTransientProp("tmp_dynamic_import");
            NodeRef tmpFolderRef = generalService.getNodeRef("/" + assocQName);
            QName assocType = ContentModel.ASSOC_CHILDREN;
            final boolean tmpFolderExisted = tmpFolderRef != null;
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
                    public void nodeCreated(NodeRef nodeRef, NodeRef parentRef, QName assocName, QName childAssocName) {
                        super.nodeCreated(nodeRef, parentRef, assocName, childAssocName);
                        if (tmpDocumentTypesRef[0] == null) { // first node imported is the root of imported nodes
                            tmpDocumentTypesRef[0] = nodeRef;
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("importing " + dynTypeMsg + ": created " + assocName.getLocalName() + " using assocName " + childAssocName.getLocalName());
                        }
                    }
                });
                importableDocTypesRootRef = tmpDocumentTypesRef[0];
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Failed to read data for importing parameters from uploaded file: '" + xmlFile.getAbsolutePath() + "'", e);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unsupported encoding of uploaded file: '" + xmlFile.getAbsolutePath() + "'", e);
            } finally {
                IOUtils.closeQuietly(fileReader);
                LOG.info("Finished importing " + dynTypeMsg + " from file '" + xmlFile.getAbsolutePath() + "' into temporary location");
            }
            return Pair.newInstance(importableDocTypesRootRef, Pair.newInstance(tmpFolderRef, tmpFolderExisted));
        }

        private void cleanupTypeImportTempData(NodeRef tmpFolderRef, boolean tmpFolderExisted, NodeRef importableDocTypesRootRef) {
            LOG.info("Deleting temporary location");
            if (!tmpFolderExisted) {
                nodeService.deleteNode(tmpFolderRef);
            }
            if (importableDocTypesRootRef != null && nodeService.exists(importableDocTypesRootRef)) {
                nodeService.deleteNode(importableDocTypesRootRef);
            }
            LOG.info("Finished deleting temporary location");
        }

        private <D extends DynamicType> void importDynamicTypes(NodeRef importableDocTypesRootRef, Class<D> dynTypeClass) {
            List<D> existingDynTypes = getTypes(dynTypeClass, null);
            Map<String, D> existingDocTypesById = new HashMap<String, D>(existingDynTypes.size());
            for (D docType : existingDynTypes) {
                existingDocTypesById.put(docType.getId(), docType);
            }

            Map<String, D> importedDocTypesById = new HashMap<String, D>();
            Map<String, Pair<List<FollowupAssociation>, List<ReplyAssociation>>> imporableDocTypesById = new HashMap<String, Pair<List<FollowupAssociation>, List<ReplyAssociation>>>();
            // first add/merge metadataItems
            List<D> allDocumentTypes = getAllTypes(dynTypeClass, null, importableDocTypesRootRef, null);
            int totalDocTypes = allDocumentTypes.size();
            LOG.info("Starting to import metadata of " + totalDocTypes + " " + dynTypeClass.getSimpleName() + "s");
            int i = 0;
            Set<MessageData> messages = new LinkedHashSet<MessageData>();
            for (D importableDynType : allDocumentTypes) {
                i++;
                LOG.info("Starting to import metadata of " + i + "/" + totalDocTypes + ". " + dynTypeClass.getSimpleName() + ": " + importableDynType.getNameAndId());
                String documentTypeId = importableDynType.getId();
                D existingDocType = existingDocTypesById.get(documentTypeId);
                D importedDocType;
                // in first phase of import don't save add associations so that assocs wouldn't be lost because of fields that might not be present on docType not jet
                if (importableDynType instanceof DocumentType) {
                    collectDocTypeAssocs(importableDynType, documentTypeId, imporableDocTypesById);
                }
                if (existingDocType != null) {
                    setProps(importableDynType, existingDocType);
                    final DocumentTypeVersion lastDocTypeVer = existingDocType.getLatestDocumentTypeVersion();
                    DocumentTypeVersion newLatestDocTypeVer = existingDocType.addNewLatestDocumentTypeVersion();
                    DocumentTypeVersion importableDocTypeVer = importableDynType.getLatestDocumentTypeVersion();

                    int sizeBefore = newLatestDocTypeVer.getMetadata().size();
                    mergeMetadaItems(importableDocTypeVer, lastDocTypeVer, newLatestDocTypeVer);
                    int sizeAfter = newLatestDocTypeVer.getMetadata().size();
                    LOG.debug("Adding " + (sizeAfter - sizeBefore) + " MetadataItems to existing " + dynTypeClass.getSimpleName() + " " + existingDocType.getNodeRef());
                    importedDocType = existingDocType;
                } else {
                    importableDynType.nextSaveToParent(getDynamicTypesRoot(dynTypeClass));
                    importedDocType = importableDynType;
                }
                Pair<D, MessageData> result = saveOrUpdateDynamicType(importedDocType);
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

            if (dynTypeClass == DocumentType.class) {
                @SuppressWarnings("unchecked")
                Map<String, DocumentType> tmp = (Map<String, DocumentType>) importedDocTypesById;
                Map<String, DocumentType> docTypesCache = new HashMap<String, DocumentType>(tmp);
                processDocTypeAssocs(docTypesCache, imporableDocTypesById, totalDocTypes);
            }
        }

        private <D> void collectDocTypeAssocs(D importableDynType, String documentTypeId, Map<String, Pair<List<FollowupAssociation>, List<ReplyAssociation>>> imporableDocTypesById) {
            DocumentType importableDocType = (DocumentType) importableDynType;
            List<FollowupAssociation> followupAssocs = importableDocType.getFollowupAssociations();
            List<ReplyAssociation> replyAssocs = importableDocType.getReplyAssociations();
            List<FollowupAssociation> followUpsToImport = new ArrayList<FollowupAssociation>(followupAssocs);
            List<ReplyAssociation> repliesToImport = new ArrayList<ReplyAssociation>(replyAssocs);
            imporableDocTypesById.put(documentTypeId, Pair.newInstance(followUpsToImport, repliesToImport));
            followupAssocs.clear();
            repliesToImport.clear();
        }

        private <D> void processDocTypeAssocs(Map<String, DocumentType> docTypesCache, Map<String, Pair<List<FollowupAssociation>
                , List<ReplyAssociation>>> imporableDocTypesById, int totalDocTypes) {
            Map<String /* docTypeId */, Set<String> /* docTypeFields */> docTypeFieldsCache = new HashMap<String, Set<String>>();
            int i = 0;
            LOG.info("Starting to import associations of " + totalDocTypes + " document types");
            /** Add {@link AssociationModel}s or merge {@link FieldMapping}s under existing {@link AssociationModel}s <br> */
            MessageDataWrapper errorsMessageDataWrapper = null;
            for (Entry<String, Pair<List<FollowupAssociation>, List<ReplyAssociation>>> entry : imporableDocTypesById.entrySet()) {
                String documentTypeId = entry.getKey();
                i++;
                LOG.info("Starting to import associations of " + i + "/" + totalDocTypes + ". document type: " + documentTypeId);
                try {
                    Pair<List<FollowupAssociation>, List<ReplyAssociation>> importableDocTypeAssocs = entry.getValue();
                    DocumentType importedDocType = docTypesCache.get(documentTypeId);
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
            Set<String> fieldsById = getDocTypeFieldsFromCache(importedDocType.getId(), docTypesCache, docTypeFieldsCache);
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
                documentType = getDocumentType(docType, DOC_TYPE_WITH_OUT_GRAND_CHILDREN_EXEPT_LATEST_DOCTYPE_VER);
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
                    metadataItem.nextSaveToParent(newLatestDocTypeVer, MetadataItem.class);
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
                    if (!neededNodeRefsByGroup.get(null).contains(fieldGroup.getNodeRef())) {
                        it.remove(); // this fieldGroup was not present under docTypeVersion that is being imported
                    } else {
                        FieldGroup existingFieldGroup = existingFieldGroupsByName.get(fieldGroup.getName());
                        if (existingFieldGroup != null) {
                            existingFieldGroup.getFields().iterator();
                            for (Iterator<Field> fieldsIt = existingFieldGroup.getFields().iterator(); fieldsIt.hasNext();) {
                                NodeRef nodeRef = fieldsIt.next().getNodeRef();
                                if (!savedNodeRefsInFieldGroup.contains(nodeRef)) {
                                    fieldsIt.remove(); // this field was not present under the same fieldGroup
                                }
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

    @Override
    public void registerGroupShowShowInTwoColumns(Set<String> originalFieldIds) {
        Assert.isTrue(!CollectionUtils.containsAny(groupShowShowInTwoColumnsOriginalFieldIds, originalFieldIds));
        groupShowShowInTwoColumnsOriginalFieldIds.addAll(originalFieldIds);
    }

    @Override
    public boolean isGroupShowShowInTwoColumns(FieldGroup group) {
        if (!group.isSystematic()) {
            return false;
        }
        Set<String> originalFieldIds = group.getOriginalFieldIds();
        return CollectionUtils.containsAny(groupShowShowInTwoColumnsOriginalFieldIds, originalFieldIds);
    }

    @Override
    public void registerGroupLimitSingle(String groupName) {
        Assert.isTrue(!groupNamesLimitSingle.contains(groupName));
        groupNamesLimitSingle.add(groupName);
    }

    @Override
    public boolean isGroupLimitSingle(FieldGroup group) {
        if (!group.isSystematic()) {
            return false;
        }
        return groupNamesLimitSingle.contains(group.getName());
    }

    @Override
    public Set<String> getAdrDocumentTypeIds() {
        List<DocumentType> documentTypes = getDocumentTypes(DONT_INCLUDE_CHILDREN, true);
        Set<String> typeIds = new HashSet<String>(documentTypes.size());
        for (DocumentType documentType : documentTypes) {
            if (documentType.isPublicAdr()) {
                typeIds.add(documentType.getId());
            }
        }
        return typeIds;
    }

}
