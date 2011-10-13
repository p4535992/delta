package ee.webmedia.alfresco.docconfig.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDictionaryService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.RepoConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.docadmin.service.SeparatorLine;
import ee.webmedia.alfresco.docconfig.generator.FieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.web.DocumentDialogHelperBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * @author Alar Kvell
 */
public class DocumentConfigServiceImpl implements DocumentConfigService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentConfigServiceImpl.class);

    private DocumentAdminService documentAdminService;
    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private UserContactMappingService userContactMappingService;
    private NodeService nodeService;
    private GeneralService generalService;

    private final Map<FieldType, FieldGenerator> fieldGenerators = new HashMap<FieldType, FieldGenerator>();
    private final Map<String, FieldGenerator> originalFieldIdGenerators = new HashMap<String, FieldGenerator>();

    private final Map<Pair<String /* documentTypeId */, Integer /* documentTypeVersionNr */>, Map<String /* fieldId */, Pair<PropertyDefinition, Field>>> propertyDefinitionCache = new ConcurrentHashMap<Pair<String, Integer>, Map<String, Pair<PropertyDefinition, Field>>>();

    private final Map<String /* hiddenFieldId */, String /* fieldIdAndOriginalFieldId */> hiddenFieldDependencies = new HashMap<String, String>();

    // TODO in the future caching

    @Override
    public void registerFieldGeneratorByType(FieldGenerator fieldGenerator, FieldType... fieldTypes) {
        Assert.notNull(fieldGenerator, "fieldGenerator");
        for (FieldType fieldType : fieldTypes) {
            Assert.notNull(fieldType, "fieldType");
            Assert.isTrue(!fieldGenerators.containsKey(fieldType), "FieldGenerator with fieldType already registered: " + fieldType);
            fieldGenerators.put(fieldType, fieldGenerator);
        }
    }

    @Override
    public void registerFieldGeneratorById(FieldGenerator fieldGenerator, String... originalFieldIds) {
        Assert.notNull(fieldGenerator, "fieldGenerator");
        for (String originalFieldId : originalFieldIds) {
            Assert.notNull(originalFieldId, "originalFieldId");
            Assert.isTrue(!originalFieldIdGenerators.containsKey(originalFieldId), "FieldGenerator with originalFieldId already registered: " + originalFieldId);
            originalFieldIdGenerators.put(originalFieldId, fieldGenerator);
        }
    }

    @Override
    public void registerHiddenFieldDependency(String hiddenFieldId, String fieldIdAndOriginalFieldId) {
        Assert.notNull(hiddenFieldId);
        Assert.notNull(fieldIdAndOriginalFieldId);
        Assert.isTrue(!hiddenFieldDependencies.containsKey(hiddenFieldId));
        hiddenFieldDependencies.put(hiddenFieldId, fieldIdAndOriginalFieldId);
        documentAdminService.registerForbiddenFieldId(hiddenFieldId);
    }

    @Override
    public DocumentConfig getConfig(Node documentDynamicNode) {
        Assert.isTrue(DocumentCommonModel.Types.DOCUMENT.equals(documentDynamicNode.getType()));
        Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = getDocumentTypeAndVersion(documentDynamicNode);
        return getConfig(documentTypeAndVersion.getFirst(), documentTypeAndVersion.getSecond());
    }

    private Pair<DocumentType, DocumentTypeVersion> getDocumentTypeAndVersion(Node documentDynamicNode) {
        Pair<String, Integer> docTypeIdAndVersionNr = getDocTypeIdAndVersionNr(documentDynamicNode);
        return getDocumentTypeAndVersion(docTypeIdAndVersionNr);
    }

    private Pair<DocumentType, DocumentTypeVersion> getDocumentTypeAndVersion(Pair<String, Integer> docTypeIdAndVersionNr) {
        String docTypeId = docTypeIdAndVersionNr.getFirst();
        Integer docTypeVersionNr = docTypeIdAndVersionNr.getSecond();

        DocumentType docType = documentAdminService.getDocumentType(docTypeId);
        if (docType == null) {
            throw new RuntimeException("documentType with documentTypeId=" + docTypeId + " not found");
        }
        DocumentTypeVersion docVersion = null;
        List<? extends DocumentTypeVersion> versions = docType.getDocumentTypeVersions().getList();
        for (DocumentTypeVersion version : versions) {
            if (docTypeVersionNr == version.getVersionNr()) {
                docVersion = version;
                break;
            }
        }
        if (docVersion == null) {
            throw new RuntimeException("documentTypeVersion with versionNr=" + docTypeVersionNr + " not found under documentType=" + docType.toString());
        }
        return new Pair<DocumentType, DocumentTypeVersion>(docType, docVersion);
    }

    private Pair<String, Integer> getDocTypeIdAndVersionNr(Node documentDynamicNode) {
        String docTypeId = (String) documentDynamicNode.getProperties().get(Props.OBJECT_TYPE_ID);
        Integer docTypeVersionNr = (Integer) documentDynamicNode.getProperties().get(Props.OBJECT_TYPE_VERSION_NR);
        Pair<String, Integer> docTypeIdAndVersionNr = new Pair<String, Integer>(docTypeId, docTypeVersionNr);
        return docTypeIdAndVersionNr;
    }

    private DocumentConfig getConfig(DocumentType docType, DocumentTypeVersion docVersion) {
        WMPropertySheetConfigElement propSheet = new WMPropertySheetConfigElement();
        Map<String, PropertySheetStateHolder> stateHolders = new HashMap<String, PropertySheetStateHolder>();
        List<String> saveListenerBeanNames = new ArrayList<String>();
        DocumentConfig config = new DocumentConfig(propSheet, stateHolders, saveListenerBeanNames, docType.getName());

        int separatorCount = 0;
        for (MetadataItem metadataItem : docVersion.getMetadata().getList()) {
            if (metadataItem instanceof Field) {
                Field field = (Field) metadataItem;
                processField(config, field);

            } else if (metadataItem instanceof SeparatorLine) {
                processSeparatorLine(config, separatorCount++);

            } else if (metadataItem instanceof FieldGroup) {
                FieldGroup fieldGroup = (FieldGroup) metadataItem;
                processFieldGroup(config, fieldGroup);

            } else {
                throw new RuntimeException("Unsupported metadataItem class=" + metadataItem.getClass() + " under documentTypeVersion=" + docVersion.toString());
            }
        }

        DocumentConfig unmodifiableConfig = config.cloneAsUnmodifiable();
        LOG.info("Returning " + unmodifiableConfig);
        return unmodifiableConfig;
    }

    private static class GeneratorResultsImpl implements GeneratorResults {

        private final ItemConfigVO pregeneratedItem;
        private boolean preGeneratedItemAdded = false;
        private final DocumentConfig config;

        public GeneratorResultsImpl(ItemConfigVO pregeneratedItem, DocumentConfig config) {
            this.pregeneratedItem = pregeneratedItem;
            this.config = config;
        }

        @Override
        public ItemConfigVO getAndAddPreGeneratedItem() {
            Assert.notNull(pregeneratedItem, "Calling this method is not allowed");
            Assert.isTrue(!preGeneratedItemAdded, "This method may be called only once");
            preGeneratedItemAdded = true;
            return pregeneratedItem;
        }

        @Override
        public ItemConfigVO generateAndAddViewModeText(String name, String label) {
            ItemConfigVO viewModeTextItem = new ItemConfigVO(name);
            viewModeTextItem.setConfigItemType(ConfigItemType.PROPERTY);
            viewModeTextItem.setShowInEditMode(false);
            viewModeTextItem.setDisplayLabel(label);
            WMPropertySheetConfigElement propSheet = config.getPropertySheetConfigElement();
            Assert.isTrue(!propSheet.getItems().containsKey(viewModeTextItem.getName()), "PropertySheetItem with name already exists: " + viewModeTextItem.getName());
            propSheet.addItem(viewModeTextItem);
            return viewModeTextItem;
        }

        @Override
        public void addStateHolder(String key, PropertySheetStateHolder stateHolder) {
            Assert.notNull(key, "key");
            Map<String, PropertySheetStateHolder> stateHolders = config.getStateHolders();
            Assert.isTrue(!stateHolders.containsKey(key));
            stateHolders.put(key, stateHolder);
        }

    }

    private void processSeparatorLine(DocumentConfig config, int separatorCount) {
        String name = "_separator" + separatorCount;

        ItemConfigVO item = new ItemConfigVO(name);
        item.setConfigItemType(ConfigItemType.SEPARATOR);
        item.setIgnoreIfMissing(false);
        item.setComponentGenerator(RepoConstants.GENERATOR_SEPARATOR);
        WMPropertySheetConfigElement propSheet = config.getPropertySheetConfigElement();
        Assert.isTrue(!propSheet.getItems().containsKey(item.getName()), "PropertySheetItem with name already exists: " + item.getName());
        propSheet.addItem(item);
    }

    private void processFieldGroup(DocumentConfig config, FieldGroup fieldGroup) {
        ChildrenList<Field> fields = fieldGroup.getFields();
        for (Field field : fields) {
            processField(config, field);
        }
    }

    private boolean processField(DocumentConfig config, Field field) {
        String name = field.getQName().toPrefixString(namespaceService);
        ItemConfigVO item = new ItemConfigVO(name);
        item.setConfigItemType(ConfigItemType.PROPERTY);
        item.setIgnoreIfMissing(false);
        item.setDisplayLabel(field.getName());

        // Default values:
        // item.setDisplayLabelId(null);
        // item.setConverter(null);
        // item.setComponentGenerator(null); // Must be set by implementation
        // item.setReadOnly(false);
        // item.setShowInViewMode(true);
        // item.setShowInEditMode(true);

        // forcedMandatory is not set here; BaseComponentGenerator sets mandatory based on PropertyDefinition.isMandatory
        FieldChangeableIf changeableIf = field.getChangeableIfEnum();
        if (changeableIf != null) {
            switch (changeableIf) {
            case ALWAYS_CHANGEABLE:
                break;
            case ALWAYS_NOT_CHANGEABLE:
                item.setReadOnly(true);
                break;
            case CHANGEABLE_IF_WORKING_DOC:
                item.setReadOnlyIf("#{" + DocumentDialogHelperBean.BEAN_NAME + ".notWorkingOrNotEditable}");
                break;
            }
        }

        /*
         * 1) Run "by fieldType" generator
         * 2) Run "by id" generator if it exists; give the same preGeneratedItem to it that was given to (1), so (1) may have modified it
         * Both can call generateAndAddViewModeText method and multiple times, all textItems will be added in the order called; item name duplicate checks are NOT performed.
         * Both can call addStateHolder method and multiple times, all textItems will be added in the order called; duplicate key will throw exception.
         * ViewModeTextItems and StateHolders will be added regardless if getAndAddPreGeneratedItem is or is not called.
         * 3) If this field belongs to a fieldGroup and this field was added, then run generateFieldGroup on "by id" generator if it supports it; this method will be run only once
         * per fieldGroup+generator
         * .
         * If 1 and 2 call getAndAddPreGeneratedItem method:
         * a) NOT EXIST - warn and return
         * b) FALSE, NOT EXIST - do not add preGeneratedItem
         * c) FALSE, FALSE - do not add preGeneratedItem
         * d) FALSE, TRUE - add
         * e) TRUE, NOT EXIST - add
         * f) TRUE, FALSE - do not add
         * g) TRUE, TRUE - add
         * If (2) does not exist, then add item if (1) called getAndAddPreGeneratedItem method.
         * If (2) exists, then add item if (2) called getAndAddPreGeneratedItem method, regardless what (1) called.
         */

        // 1) Run "by fieldType" generator
        FieldGenerator generatorByFieldType = fieldGenerators.get(field.getFieldTypeEnum());
        if (generatorByFieldType == null) {
            LOG.warn("Unsupported field type, ignoring: " + field.toString());
            return false;
        }
        GeneratorResultsImpl generatorResults = new GeneratorResultsImpl(item, config);
        try {
            generatorByFieldType.generateField(field, generatorResults);
        } catch (Exception e) {
            throw new RuntimeException("Error running generator for field type, field=" + field.toString() + ": " + e.getMessage(), e);
        }
        boolean preGeneratedItemAdded = generatorResults.preGeneratedItemAdded;

        // 2) Run "by id" generator if it exists
        FieldGenerator generatorByOriginalFieldId = originalFieldIdGenerators.get(field.getOriginalFieldId());
        if (generatorByOriginalFieldId != null) {
            generatorResults = new GeneratorResultsImpl(item, config);
            try {
                generatorByOriginalFieldId.generateField(field, generatorResults);
            } catch (Exception e) {
                throw new RuntimeException("Error running generator for field id, field=" + field.toString() + ": " + e.getMessage(), e);
            }
            preGeneratedItemAdded = generatorResults.preGeneratedItemAdded;
        }

        if (!preGeneratedItemAdded) {
            return false;
        }

        WMPropertySheetConfigElement propSheet = config.getPropertySheetConfigElement();
        Assert.isTrue(!propSheet.getItems().containsKey(item.getName()), "PropertySheetItem with name already exists: " + item.getName());
        propSheet.addItem(item);

        addSaveListener(config, generatorByFieldType);
        addSaveListener(config, generatorByOriginalFieldId);

        return true;
    }

    private void addSaveListener(DocumentConfig config, FieldGenerator fieldGenerator) {
        if (fieldGenerator instanceof SaveListener) {
            SaveListener saveListener = (SaveListener) fieldGenerator;
            List<String> saveListenerBeanNames = config.getSaveListenerBeanNames();
            String beanName = saveListener.getBeanName();
            if (!saveListenerBeanNames.contains(beanName)) {
                saveListenerBeanNames.add(beanName);
            }
        }
    }

    @Override
    public void setDefaultPropertyValues(Node documentDynamicNode) {
        Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = getDocumentTypeAndVersion(documentDynamicNode);
        DocumentTypeVersion docVer = documentTypeAndVersion.getSecond();
        for (Field field : docVer.getFieldsDeeply()) {
            setDefaultPropertyValue(documentDynamicNode, field);
        }
    }

    private void setDefaultPropertyValue(Node documentDynamicNode, Field field) {
        Serializable value = (Serializable) documentDynamicNode.getProperties().get(field.getQName());
        if (value != null) {
            return;
        }

        PropertyDefinitionImpl propDef = createPropertyDefinition(field);

        QName dataType = propDef.getDataTypeQName();
        Serializable defaultValue = null;
        if (StringUtils.isNotEmpty(field.getDefaultValue())) {
            if (DataTypeDefinition.TEXT.equals(dataType) && field.getFieldTypeEnum() != FieldType.INFORMATION_TEXT) {
                defaultValue = field.getDefaultValue();
            } else if (DataTypeDefinition.LONG.equals(dataType) || DataTypeDefinition.DOUBLE.equals(dataType)) {
                DataTypeDefinition dataTypeDefinition = dictionaryService.getDataType(dataType);
                try {
                    defaultValue = (Serializable) DefaultTypeConverter.INSTANCE.convert(dataTypeDefinition, field.getDefaultValue());
                } catch (Exception e) {
                    // TODO change to debug
                    LOG.warn("Failed to convert defaultValue to required data type\nfield=" + field.toString() + "\ndocumentDynamicNode=" + documentDynamicNode, e);
                    // Do nothing, defaultValue stays null
                }
            }

        } else if (field.isDefaultDateSysdate()) {
            if (DataTypeDefinition.DATE.equals(dataType)) {
                defaultValue = new Date(AlfrescoTransactionSupport.getTransactionStartTime());
            }

        } else if (field.isDefaultUserLoggedIn()) {
            if (DataTypeDefinition.TEXT.equals(dataType)) {
                defaultValue = UserUtil.getPersonFullName1(BeanHelper.getUserService().getCurrentUserProperties());
                Map<QName, UserContactMappingCode> mapping = userContactMappingService.getFieldIdsMapping(field);
                NodeRef userRef = BeanHelper.getUserService().getCurrentUser();
                userContactMappingService.setMappedValues(documentDynamicNode.getProperties(), mapping, userRef, propDef.isMultiValued());
                return;
            }

        } else if (field.isDefaultSelected()) {
            if (DataTypeDefinition.BOOLEAN.equals(dataType)) {
                defaultValue = Boolean.TRUE;
            }
        }

        if (defaultValue != null && propDef.isMultiValued()) {
            ArrayList<Serializable> list = new ArrayList<Serializable>();
            list.add(defaultValue);
            defaultValue = list;
        }

        // In Search/MultiValueEditor component, display one empty row by default, not zero rows
        if (defaultValue == null
                && (field.getFieldTypeEnum() == FieldType.USERS || field.getFieldTypeEnum() == FieldType.CONTACTS || field.getFieldTypeEnum() == FieldType.USERS_CONTACTS)) {
            ArrayList<Serializable> list = new ArrayList<Serializable>();
            list.add(null);
            defaultValue = list;
        }

        if (defaultValue != null) {
            documentDynamicNode.getProperties().put(field.getQName().toString(), defaultValue);
        }
    }

    @Override
    public PropertyDefinition getPropertyDefinition(Node documentDynamicNode, QName property) {
        if (!DocumentCommonModel.Types.DOCUMENT.equals(documentDynamicNode.getType()) || !DocumentDynamicModel.URI.equals(property.getNamespaceURI())) {
            return null;
        }
        Map<String, Pair<PropertyDefinition, Field>> propertyDefinitions = getPropertyDefinitions(documentDynamicNode);
        Pair<PropertyDefinition, Field> propertyDefinition = propertyDefinitions.get(property.getLocalName());
        if (propertyDefinition == null) {
            LOG.warn("\n\n!!!!!!!!!!!!!!!!!!! fieldId=" + property + " not found, documentDynamicNode=" + documentDynamicNode + "\n");
            return null;
        }
        return propertyDefinition.getFirst();
    }

    @Override
    public Map<String, Pair<PropertyDefinition, Field>> getPropertyDefinitions(Node documentDynamicNode) {
        Pair<String, Integer> cacheKey = getDocTypeIdAndVersionNr(documentDynamicNode);
        Map<String, Pair<PropertyDefinition, Field>> propertyDefinitions = propertyDefinitionCache.get(cacheKey);
        if (propertyDefinitions == null) {
            propertyDefinitions = new HashMap<String, Pair<PropertyDefinition, Field>>();
            Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = getDocumentTypeAndVersion(cacheKey);
            DocumentTypeVersion docVersion = documentTypeAndVersion.getSecond();
            docVersion.resetParent();
            // TODO documentTypeVersion, fields and fieldGroups should be immutable; or they should be cloned in get method
            for (Field field : docVersion.getFieldsDeeply()) {
                String fieldId = field.getFieldId();
                Assert.isTrue(!propertyDefinitions.containsKey(fieldId));
                propertyDefinitions.put(fieldId, new Pair<PropertyDefinition, Field>(createPropertyDefinition(field), field));
            }
            for (Entry<String, String> entry : hiddenFieldDependencies.entrySet()) {
                Pair<PropertyDefinition, Field> originalPropDefAndField = propertyDefinitions.get(entry.getValue());
                if (originalPropDefAndField != null && originalPropDefAndField.getSecond().getOriginalFieldId().equals(entry.getValue())) {
                    String hiddenFieldId = entry.getKey();
                    PropertyDefinitionImpl propDef = createPropertyDefinitionForHiddenField(hiddenFieldId, originalPropDefAndField.getFirst());
                    propertyDefinitions.put(hiddenFieldId, new Pair<PropertyDefinition, Field>(propDef, null));
                }
            }
            if (LOG.isDebugEnabled()) {
                StringBuilder s = new StringBuilder();
                s.append("Created propertyDefinitions for cacheKey=").append(cacheKey).append(" - ");
                s.append("[").append(propertyDefinitions.size()).append("]");
                for (Entry<String, Pair<PropertyDefinition, Field>> entry : propertyDefinitions.entrySet()) {
                    s.append("\n  ").append(entry.getKey()).append("=");
                    s.append("\n    propertyDefinition=").append(entry.getValue().getFirst());
                    Field field = entry.getValue().getSecond();
                    s.append("\n    field=").append(field == null ? "null" : "Field[fieldId=" + field.getFieldId() + "]");
                }
                LOG.debug(s.toString());
            }
            propertyDefinitionCache.put(cacheKey, Collections.unmodifiableMap(propertyDefinitions));
        }
        return propertyDefinitions;
    }

    private PropertyDefinitionImpl createPropertyDefinition(Field field) {
        Boolean multiValuedOverride = getMultiValuedOverride(field);
        return new PropertyDefinitionImpl(field, multiValuedOverride);
    }

    private PropertyDefinitionImpl createPropertyDefinitionForHiddenField(String hiddenFieldId, PropertyDefinition originalPropertyDefinition) {
        return new PropertyDefinitionImpl(hiddenFieldId, (PropertyDefinitionImpl) originalPropertyDefinition);
    }

    public static class PropertyDefinitionImpl implements PropertyDefinition {

        private final QName name;
        private final String title;
        private final FieldType fieldType;
        private final boolean mandatory;
        private final Boolean multiValuedOverride;

        private PropertyDefinitionImpl(Field field, Boolean multiValuedOverride) {
            Assert.notNull(field, "field");
            name = field.getQName();
            title = field.getName();
            fieldType = field.getFieldTypeEnum();
            mandatory = field.isMandatory();
            this.multiValuedOverride = multiValuedOverride;
        }

        private PropertyDefinitionImpl(String hiddenFieldId, PropertyDefinitionImpl originalPropertyDefinition) {
            Assert.notNull(hiddenFieldId, "hiddenFieldId");
            name = Field.getQName(hiddenFieldId);
            title = null;
            fieldType = FieldType.TEXT_FIELD;
            mandatory = originalPropertyDefinition.mandatory;
            multiValuedOverride = originalPropertyDefinition.multiValuedOverride;
        }

        @Override
        public ModelDefinition getModel() {
            return getDictionaryService().getModel(DocumentDynamicModel.MODEL);
        }

        @Override
        public QName getName() {
            return name;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public String getDefaultValue() {
            return null;
        }

        @Override
        public DataTypeDefinition getDataType() {
            return getDictionaryService().getDataType(getDataTypeQName());
        }

        private QName getDataTypeQName() {
            switch (fieldType) {
            case DOUBLE:
                return DataTypeDefinition.DOUBLE;
            case LONG:
                return DataTypeDefinition.LONG;
            case DATE:
                return DataTypeDefinition.DATE;
            case CHECKBOX:
                return DataTypeDefinition.BOOLEAN;
            default:
                return DataTypeDefinition.TEXT;
            }
        }

        @Override
        public ClassDefinition getContainerClass() {
            return null;
        }

        @Override
        public boolean isOverride() {
            return false;
        }

        @Override
        public boolean isMultiValued() {
            if (multiValuedOverride != null) {
                return multiValuedOverride;
            }
            switch (fieldType) {
            case USERS:
            case USERS_CONTACTS:
            case CONTACTS:
            case LISTBOX:
            case HIERARCHICAL_KEYWORD_LEVEL1:
            case HIERARCHICAL_KEYWORD_LEVEL2:
                return true;
            default:
                return false;
            }
        }

        @Override
        public boolean isMandatory() {
            return mandatory;
        }

        @Override
        public boolean isMandatoryEnforced() {
            return false;
        }

        @Override
        public boolean isProtected() {
            return false;
        }

        @Override
        public boolean isIndexed() {
            return true;
        }

        @Override
        public boolean isStoredInIndex() {
            return false;
        }

        @Override
        public IndexTokenisationMode getIndexTokenisationMode() {
            return IndexTokenisationMode.TRUE;
        }

        @Override
        public boolean isIndexedAtomically() {
            return true;
        }

        @Override
        public List<ConstraintDefinition> getConstraints() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return WmNode.toString(this) + "[" +
                    "name=" + name.toPrefixString(BeanHelper.getNamespaceService()) +
                    " fieldType=" + fieldType +
                    " mandatory=" + mandatory +
                    " multiValuedOverride=" + multiValuedOverride +
                    "]";
        }

    }

    private final Set<String> multiValuedOverrideOriginalFieldIds = new HashSet<String>();

    @Override
    public void registerMultiValuedOverrideInSystematicGroup(Set<String> originalFieldIds) {
        Assert.isTrue(!CollectionUtils.containsAny(multiValuedOverrideOriginalFieldIds, originalFieldIds));
        multiValuedOverrideOriginalFieldIds.addAll(originalFieldIds);
    }

    private Boolean getMultiValuedOverride(Field field) {
        Boolean multiValuedOverride = null;
        BaseObject parent = field.getParent();
        boolean inGroup;
        if (parent instanceof FieldGroup) {
            inGroup = true;
        } else if (parent instanceof DocumentTypeVersion) {
            inGroup = false;
        } else {
            throw new RuntimeException("Field parent must be FieldGroup or DocumentTypeVersion, but is " + parent);
        }
        if (inGroup && ((FieldGroup) parent).isSystematic()) {
            Set<String> originalFieldIds = ((FieldGroup) parent).getOriginalFieldIds();
            originalFieldIds.retainAll(multiValuedOverrideOriginalFieldIds);
            if (!originalFieldIds.isEmpty()) {
                multiValuedOverride = Boolean.TRUE;
            }
        }
        return multiValuedOverride;
    }

    // START: setters
    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setUserContactMappingService(UserContactMappingService userContactMappingService) {
        this.userContactMappingService = userContactMappingService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
    // END: setters

}
