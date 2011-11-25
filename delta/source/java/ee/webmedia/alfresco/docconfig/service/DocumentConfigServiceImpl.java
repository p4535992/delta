package ee.webmedia.alfresco.docconfig.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDictionaryService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.RepoConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.docadmin.service.SeparatorLine;
import ee.webmedia.alfresco.docconfig.generator.FieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.web.DocumentDialogHelperBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * @author Alar Kvell
 */
public class DocumentConfigServiceImpl implements DocumentConfigService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentConfigServiceImpl.class);

    private DocumentAdminService documentAdminService;
    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private UserContactMappingService userContactMappingService;
    private UserService userService;
    private ClassificatorService classificatorService;

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
    public Set<String> getHiddenPropFieldIds(Collection<Field> originalFields) {
        Set<String> hiddenFields = new HashSet<String>();
        Set<String> fieldIds = new HashSet<String>();
        for (Field field : originalFields) {
            if (field.getFieldId().equals(field.getOriginalFieldId())) {
                fieldIds.add(field.getFieldId());
            }
        }
        for (Entry<String, String> entry : hiddenFieldDependencies.entrySet()) {
            if (fieldIds.contains(entry.getValue())) {
                hiddenFields.add(entry.getKey());
            }
        }
        return hiddenFields;
    }

    @Override
    public DocumentConfig getConfig(Node documentDynamicNode) {
        Assert.isTrue(DocumentCommonModel.Types.DOCUMENT.equals(documentDynamicNode.getType()));
        Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = getDocumentTypeAndVersion(documentDynamicNode);
        return getConfig(documentTypeAndVersion.getFirst(), documentTypeAndVersion.getSecond());
    }

    @Override
    public DocumentConfig getSearchConfig() {
        DocumentConfig config = getEmptyConfig(null);
        /**
         * <show-property name="docsearch:store" display-label-id="document_search_stores" component-generator="GeneralSelectorGenerator"
         * selectionItems="#{DocumentSearchDialog.getStores}" converter="ee.webmedia.alfresco.common.propertysheet.converter.StoreRefConverter" />
         */
        {
            // docsearch:store
            ItemConfigVO itemConfig = new ItemConfigVO(DocumentSearchModel.Props.STORE.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("document_search_stores");
            itemConfig.setComponentGenerator("GeneralSelectorGenerator");
            itemConfig.setSelectionItems("#{DocumentDynamicSearchDialog.getStores}");
            itemConfig.setConverter("ee.webmedia.alfresco.common.propertysheet.converter.StoreRefConverter");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }

        /**
         * <show-property name="docsearch:input" display-label-id="document_search_input" component-generator="TextAreaGenerator" styleClass="expand19-200" />
         */
        {
            // docsearch:input
            ItemConfigVO itemConfig = new ItemConfigVO(DocumentSearchModel.Props.INPUT.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("document_search_input");
            itemConfig.setComponentGenerator("TextAreaGenerator");
            itemConfig.setStyleClass("expand19-200 focus");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }

        /**
         * <show-property name="docsearch:documentType" display-label-id="document_docType" component-generator="GeneralSelectorGenerator"
         * selectionItems="#{DocumentSearchBean.getDocumentTypes}" converter="ee.webmedia.alfresco.common.propertysheet.converter.QNameConverter" />
         */
        {
            // docsearch:documentType
            ItemConfigVO itemConfig = new ItemConfigVO(DocumentSearchModel.Props.DOCUMENT_TYPE.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("document_docType");
            itemConfig.setComponentGenerator("GeneralSelectorGenerator");
            itemConfig.setSelectionItems("#{DocumentSearchBean.getDocumentTypes}");
            itemConfig.setRenderCheckboxAfterLabel(true);
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }

        /**
         * <show-property name="docsearch:sendMode" display-label-id="document_send_mode" component-generator="ClassificatorSelectorGenerator" classificatorName="sendModeSearch" />
         */
        {
            // docsearch:sendMode
            ItemConfigVO itemConfig = new ItemConfigVO(DocumentSearchModel.Props.SEND_MODE.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("document_send_mode");
            itemConfig.setComponentGenerator("ClassificatorSelectorGenerator");
            itemConfig.setRenderCheckboxAfterLabel(true);
            itemConfig.setClassificatorName("transmittalMode"); // sendModeSearch classificator is deprecated
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }

        List<FieldDefinition> fields = documentAdminService.getSearchableFieldDefinitions();
        for (FieldDefinition fieldDefinition : fields) {
            processFieldForSearchView(fieldDefinition);
            processField(config, fieldDefinition, true);
            if (fieldDefinition.getFieldId().equals("regNumber")) {
                ItemConfigVO itemConfig = new ItemConfigVO(DocumentDynamicModel.Props.SHORT_REG_NUMBER.toPrefixString(namespaceService));
                itemConfig.setDisplayLabelId("document_shortRegNumber");
                itemConfig.setComponentGenerator("TextAreaGenerator");
                itemConfig.setStyleClass("expand19-200");
                // itemConfig.setIgnoreIfMissing(false);
                itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
                config.getPropertySheetConfigElement().addItem(itemConfig);
            }
        }

        /**
         * <show-property name="docsearch:fund" display-label-id="transaction_fund" component-generator="MultiValueEditorGenerator"
         * showHeaders="false" styleClass="add-default" noAddLinkLabel="true" addLabelId="add_row" isAutomaticallyAddRows="true"
         * propsGeneration="docsearch:fund¤DimensionSelectorGenerator¤dimensionName=invoiceFunds¤styleClass=expand19-200 tooltip¤converter="/>
         */
        {
            // docsearch:fund
            ItemConfigVO itemConfig = new ItemConfigVO(DocumentSearchModel.Props.FUND.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("transaction_fund");
            itemConfig.setComponentGenerator("MultiValueEditorGenerator");
            itemConfig.setShowHeaders(false);
            itemConfig.setStyleClass("add-default");
            itemConfig.setNoAddLinkLabel(true);
            itemConfig.setAddLabelId("add_row");
            itemConfig.setIsAutomaticallyAddRows(true);
            itemConfig.setPropsGeneration("docsearch:fund¤DimensionSelectorGenerator¤dimensionName=invoiceFunds¤styleClass=expand19-200 tooltip¤converter=");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }

        /**
         * <show-property name="docsearch:fundsCenter" display-label-id="transaction_fundsCenter" component-generator="MultiValueEditorGenerator"
         * showHeaders="false" styleClass="add-default" noAddLinkLabel="true" addLabelId="add_row" isAutomaticallyAddRows="true"
         * propsGeneration="docsearch:fundsCenter¤DimensionSelectorGenerator¤dimensionName=invoiceFundsCenters¤styleClass=expand19-200 tooltip¤converter="/>
         */
        {
            // docsearch:fundsCenter
            ItemConfigVO itemConfig = new ItemConfigVO(DocumentSearchModel.Props.FUNDS_CENTER.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("transaction_fundsCenter");
            itemConfig.setComponentGenerator("MultiValueEditorGenerator");
            itemConfig.setShowHeaders(false);
            itemConfig.setStyleClass("add-default");
            itemConfig.setNoAddLinkLabel(true);
            itemConfig.setAddLabelId("add_row");
            itemConfig.setIsAutomaticallyAddRows(true);
            itemConfig.setPropsGeneration("docsearch:fundsCenter¤DimensionSelectorGenerator¤dimensionName=invoiceFundsCenters¤styleClass=expand19-200 tooltip¤converter=");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }

        /**
         * <show-property name="docsearch:eaCommitmentItem" display-label-id="transaction_eaCommitmentItem" component-generator="MultiValueEditorGenerator"
         * showHeaders="false" styleClass="add-default" noAddLinkLabel="true" addLabelId="add_row" filter="eaPrefixInclude" isAutomaticallyAddRows="true"
         * propsGeneration="docsearch:eaCommitmentItem¤DimensionSelectorGenerator¤dimensionName=invoiceCommitmentItem¤styleClass=expand19-200 tooltip¤converter="/>
         */
        {
            // docsearch:eaCommitmentItem
            ItemConfigVO itemConfig = new ItemConfigVO(DocumentSearchModel.Props.EA_COMMITMENT_ITEM.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("transaction_eaCommitmentItem");
            itemConfig.setComponentGenerator("MultiValueEditorGenerator");
            itemConfig.setShowHeaders(false);
            itemConfig.setStyleClass("add-default");
            itemConfig.setNoAddLinkLabel(true);
            itemConfig.setAddLabelId("add_row");
            itemConfig.setFilter("eaPrefixInclude");
            itemConfig.setIsAutomaticallyAddRows(true);
            itemConfig.setPropsGeneration(
                    "docsearch:eaCommitmentItem¤DimensionSelectorGenerator¤filter=eaPrefixInclude¤dimensionName=invoiceCommitmentItem¤styleClass=expand19-200 tooltip¤converter=");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }
        return config;
    }

    @Override
    public Pair<DocumentType, DocumentTypeVersion> getDocumentTypeAndVersion(Node documentDynamicNode) {
        Pair<String, Integer> docTypeIdAndVersionNr = getDocTypeIdAndVersionNr(documentDynamicNode);
        return getDocumentTypeAndVersion(docTypeIdAndVersionNr);
    }

    private Pair<DocumentType, DocumentTypeVersion> getDocumentTypeAndVersion(Pair<String, Integer> docTypeIdAndVersionNr) {
        if (StringUtils.isBlank(docTypeIdAndVersionNr.getFirst()) || docTypeIdAndVersionNr.getSecond() == null) {
            return null;
        }
        return documentAdminService.getDocumentTypeAndVersion(docTypeIdAndVersionNr.getFirst(), docTypeIdAndVersionNr.getSecond());
    }

    private Pair<String, Integer> getDocTypeIdAndVersionNr(Node documentDynamicNode) {
        String docTypeId = (String) documentDynamicNode.getProperties().get(Props.OBJECT_TYPE_ID);
        Integer docTypeVersionNr = (Integer) documentDynamicNode.getProperties().get(Props.OBJECT_TYPE_VERSION_NR);
        Pair<String, Integer> docTypeIdAndVersionNr = new Pair<String, Integer>(docTypeId, docTypeVersionNr);
        return docTypeIdAndVersionNr;
    }

    private Pair<String, Integer> getDocTypeIdAndVersionNr(Map<QName, Serializable> props) {
        String docTypeId = (String) props.get(Props.OBJECT_TYPE_ID);
        Integer docTypeVersionNr = (Integer) props.get(Props.OBJECT_TYPE_VERSION_NR);
        Pair<String, Integer> docTypeIdAndVersionNr = new Pair<String, Integer>(docTypeId, docTypeVersionNr);
        return docTypeIdAndVersionNr;
    }

    private DocumentConfig getConfig(DocumentType docType, DocumentTypeVersion docVersion) {
        DocumentConfig config = getEmptyConfig(docType);

        int separatorCount = 0;
        for (MetadataItem metadataItem : docVersion.getMetadata()) {
            if (metadataItem instanceof Field) {
                Field field = (Field) metadataItem;
                processField(config, field, false);

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

    private DocumentConfig getEmptyConfig(DocumentType docType) {
        WMPropertySheetConfigElement propSheet = new WMPropertySheetConfigElement();
        Map<String, PropertySheetStateHolder> stateHolders = new HashMap<String, PropertySheetStateHolder>();
        List<String> saveListenerBeanNames = new ArrayList<String>();
        return new DocumentConfig(propSheet, stateHolders, saveListenerBeanNames, docType);
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
        public void addItem(ItemConfigVO item) {
            WMPropertySheetConfigElement propSheet = config.getPropertySheetConfigElement();
            Assert.isTrue(!propSheet.getItems().containsKey(item.getName()), "PropertySheetItem with name already exists: " + item.getName());
            propSheet.addItem(item);
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
            processField(config, field, false);
        }
    }

    private boolean processField(DocumentConfig config, Field field, boolean renderCheckboxAfterLabel) {
        String name = field.getQName().toPrefixString(namespaceService);
        ItemConfigVO item = new ItemConfigVO(name);
        item.setConfigItemType(ConfigItemType.PROPERTY);
        item.setIgnoreIfMissing(false);
        item.setRenderCheckboxAfterLabel(renderCheckboxAfterLabel);
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
    public void setDefaultPropertyValues(Node documentDynamicNode, DocumentTypeVersion docVer) {
        if (docVer == null) {
            Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = getDocumentTypeAndVersion(documentDynamicNode);
            docVer = documentTypeAndVersion.getSecond();
        } else {
            Pair<String, Integer> docTypeIdAndVersionNr = getDocTypeIdAndVersionNr(documentDynamicNode);
            Assert.isTrue(ObjectUtils.equals(docVer.getParent().getId(), docTypeIdAndVersionNr.getFirst()));
            Assert.isTrue(ObjectUtils.equals(docVer.getVersionNr(), docTypeIdAndVersionNr.getSecond()));
        }
        for (Field field : docVer.getFieldsDeeply()) {
            setDefaultPropertyValue(documentDynamicNode, field, false);
        }
    }

    @Override
    public void setDefaultPropertyValues(Node documentDynamicNode, List<Field> fields, boolean forceOverwrite) {
        for (Field field : fields) {
            setDefaultPropertyValue(documentDynamicNode, field, forceOverwrite);
        }
    }

    private void setDefaultPropertyValue(Node documentDynamicNode, Field field, boolean forceOverwrite) {
        Serializable value = (Serializable) documentDynamicNode.getProperties().get(field.getQName());
        if (value != null && !forceOverwrite) {
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

        } else if (StringUtils.isNotBlank(field.getClassificator())) {
            if (DataTypeDefinition.TEXT.equals(dataType) && field.getFieldTypeEnum() != FieldType.INFORMATION_TEXT) {
                if (StringUtils.isNotBlank(field.getClassificatorDefaultValue())) {
                    defaultValue = field.getClassificatorDefaultValue();
                } else {
                    List<ClassificatorValue> classificatorValues = classificatorService.getAllClassificatorValues(field.getClassificator());
                    for (ClassificatorValue classificatorValue : classificatorValues) {
                        if (classificatorValue.isByDefault()) {
                            defaultValue = classificatorValue.getValueName();
                            break;
                        }
                    }
                }
            }

        } else if (field.isDefaultDateSysdate()) {
            if (DataTypeDefinition.DATE.equals(dataType)) {
                defaultValue = new Date(AlfrescoTransactionSupport.getTransactionStartTime());
            }

        } else if (field.isDefaultUserLoggedIn()) {
            if (DataTypeDefinition.TEXT.equals(dataType) && field.getFieldTypeEnum() != FieldType.INFORMATION_TEXT) {
                // XXX Alar inconvenient
                Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                setUserContactProps(props, AuthenticationUtil.getRunAsUser(), propDef, field);
                for (Entry<QName, Serializable> entry : props.entrySet()) {
                    documentDynamicNode.getProperties().put(entry.getKey().toString(), entry.getValue());
                }
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
        if (defaultValue == null) {
            Boolean multiValuedOverride = getMultiValuedOverride(field);
            if (field.getFieldTypeEnum() == FieldType.USERS || field.getFieldTypeEnum() == FieldType.CONTACTS || field.getFieldTypeEnum() == FieldType.USERS_CONTACTS ||
                    (multiValuedOverride != null && multiValuedOverride)) {
                ArrayList<Serializable> list = new ArrayList<Serializable>();
                list.add(null);
                defaultValue = list;
            }
        }

        if (defaultValue != null) {
            documentDynamicNode.getProperties().put(field.getQName().toString(), defaultValue);
        }
    }

    @Override
    public void setUserContactProps(Map<QName, Serializable> props, String userName, String fieldId) {
        Map<String, Pair<PropertyDefinition, Field>> propDefs = getPropertyDefinitions(getDocTypeIdAndVersionNr(props));
        Pair<PropertyDefinition, Field> propDefAndField = propDefs.get(fieldId);
        setUserContactProps(props, userName, propDefAndField.getFirst(), propDefAndField.getSecond());
    }

    private void setUserContactProps(Map<QName, Serializable> props, String userName, PropertyDefinition propDef, Field field) {
        NodeRef userRef = userService.getPerson(userName);
        // userRef may be null, then all fields are set to null
        Map<QName, UserContactMappingCode> mapping = userContactMappingService.getFieldIdsMappingOrDefault(field);
        userContactMappingService.setMappedValues(props, mapping, userRef, propDef.isMultiValued());
        if (userRef == null) {
            for (Entry<QName, UserContactMappingCode> entry : mapping.entrySet()) {
                if (entry.getValue() == UserContactMappingCode.CODE) {
                    props.put(entry.getKey(), userName);
                }
            }
        }
    }

    @Override
    public PropertyDefinition getPropertyDefinition(Node documentDynamicNode, QName property) {
        if (!DocumentDynamicModel.URI.equals(property.getNamespaceURI())) {
            return null;
        }
        if (DocumentSearchModel.Types.FILTER.equals(documentDynamicNode.getType())) {
            if (hiddenFieldDependencies.containsKey(property.getLocalName())) {
                String originalFieldId = hiddenFieldDependencies.get(property.getLocalName());
                PropertyDefinition originalPropDef = getPropDefForSearch(originalFieldId);
                PropertyDefinitionImpl propDef = createPropertyDefinitionForHiddenField(property.getLocalName(), originalPropDef);
                return propDef;
            }
            return getPropDefForSearch(property.getLocalName());
        }
        Map<String, Pair<PropertyDefinition, Field>> propertyDefinitions = getPropertyDefinitions(documentDynamicNode);
        if (propertyDefinitions == null) {
            return null;
        }
        Pair<PropertyDefinition, Field> propertyDefinition = propertyDefinitions.get(property.getLocalName());
        if (propertyDefinition == null) {
            LOG.warn("\n\n!!!!!!!!!!!!!!!!!!! fieldId=" + property + " not found, documentDynamicNode=" + documentDynamicNode + "\n");
            return null;
        }
        return propertyDefinition.getFirst();
    }

    private PropertyDefinition getPropDefForSearch(String fieldId) {
        FieldDefinition field;
        if (fieldId.contains("_")) {
            field = documentAdminService.getFieldDefinition(fieldId.substring(0, fieldId.indexOf("_")));
            field.setFieldId(fieldId);
        } else {
            field = documentAdminService.getFieldDefinition(fieldId);
        }
        if (field == null) {
            return null;
        }
        processFieldForSearchView(field);
        return new PropertyDefinitionImpl(field, isFieldForcedMultipleInSearch(field));
    }

    private static final List<String> comboboxFieldsNotMultiple = Arrays.asList("function", "series", "volume");

    private Boolean isFieldForcedMultipleInSearch(FieldDefinition field) {
        if (field.getFieldTypeEnum().equals(FieldType.COMBOBOX) && !comboboxFieldsNotMultiple.contains(field.getFieldId())) {
            return true;
        }
        return null;
    }

    private void processFieldForSearchView(FieldDefinition field) {
        if (field.getFieldTypeEnum().equals(FieldType.USER)) {
            field.setFieldTypeEnum(FieldType.USERS);
        }
        if (field.getFieldTypeEnum().equals(FieldType.CONTACT)) {
            field.setFieldTypeEnum(FieldType.CONTACTS);
        }
        if (field.getFieldTypeEnum().equals(FieldType.USER_CONTACT)) {
            field.setFieldTypeEnum(FieldType.USERS_CONTACTS);
        }
        field.setForSearch(Boolean.TRUE);
        field.setChangeableIfEnum(FieldChangeableIf.ALWAYS_CHANGEABLE);
        field.setMandatory(false);
    }

    @Override
    public Map<String, Pair<PropertyDefinition, Field>> getPropertyDefinitions(Node documentDynamicNode) {
        // TODO Alar: restore this behaviour or leave current?
        // while (DocumentCommonModel.Types.METADATA_CONTAINER.equals(documentDynamicNode.getType())) {
        // documentDynamicNode = generalService.getPrimaryParent(documentDynamicNode.getNodeRef());
        // }
        if (!DocumentCommonModel.Types.DOCUMENT.equals(documentDynamicNode.getType()) && !DocumentCommonModel.Types.METADATA_CONTAINER.equals(documentDynamicNode.getType())) {
            return null;
        }
        return getPropertyDefinitions(getDocTypeIdAndVersionNr(documentDynamicNode));
    }

    private Map<String, Pair<PropertyDefinition, Field>> getPropertyDefinitions(Pair<String, Integer> docTypeIdAndVersionNr) {
        Map<String, Pair<PropertyDefinition, Field>> propertyDefinitions = propertyDefinitionCache.get(docTypeIdAndVersionNr);
        if (propertyDefinitions == null) {
            propertyDefinitions = new HashMap<String, Pair<PropertyDefinition, Field>>();
            Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = getDocumentTypeAndVersion(docTypeIdAndVersionNr);
            if (documentTypeAndVersion == null) {
                return null;
            }
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
                s.append("Created propertyDefinitions for cacheKey=").append(docTypeIdAndVersionNr).append(" - ");
                s.append("[").append(propertyDefinitions.size()).append("]");
                for (Entry<String, Pair<PropertyDefinition, Field>> entry : propertyDefinitions.entrySet()) {
                    s.append("\n  ").append(entry.getKey()).append("=");
                    s.append("\n    propertyDefinition=").append(entry.getValue().getFirst());
                    Field field = entry.getValue().getSecond();
                    s.append("\n    field=").append(field == null ? "null" : "Field[fieldId=" + field.getFieldId() + "]");
                }
                LOG.debug(s.toString());
            }
            propertyDefinitionCache.put(docTypeIdAndVersionNr, Collections.unmodifiableMap(propertyDefinitions));
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
        private final String originalFieldId;
        private final String title;
        private final FieldType fieldType;
        private final boolean mandatory;
        private final Boolean multiValuedOverride;

        private PropertyDefinitionImpl(Field field, Boolean multiValuedOverride) {
            Assert.notNull(field, "field");
            name = field.getQName();
            originalFieldId = field.getOriginalFieldId();
            title = field.getName();
            fieldType = field.getFieldTypeEnum();
            mandatory = field.isMandatory();
            this.multiValuedOverride = multiValuedOverride;
        }

        private PropertyDefinitionImpl(String hiddenFieldId, PropertyDefinitionImpl originalPropertyDefinition) {
            Assert.notNull(hiddenFieldId, "hiddenFieldId");
            name = Field.getQName(hiddenFieldId);
            originalFieldId = null;
            title = null;
            fieldType = FieldType.TEXT_FIELD;
            mandatory = originalPropertyDefinition.mandatory;
            multiValuedOverride = originalPropertyDefinition.multiValuedOverride;
        }

        @Override
        public ModelDefinition getModel() {
            return getDictionaryService().getModel(DocumentDynamicModel.MODEL); // TODO FIXME WARNING!! uses BeanHelper
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
            return getDictionaryService().getDataType(getDataTypeQName()); // TODO FIXME WARNING!! uses BeanHelper
        }

        private QName getDataTypeQName() {
            if (originalFieldId != null && Arrays.asList(DocumentLocationGenerator.NODE_REF_FIELD_IDS).contains(originalFieldId)) {
                return DataTypeDefinition.NODE_REF;
            }
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
                    "name=" + name.toPrefixString(getNamespaceService()) +
                    " fieldType=" + fieldType +
                    " mandatory=" + mandatory +
                    " multiValuedOverride=" + multiValuedOverride +
                    "]";
        }

    }

    private final Set<String> multiValuedOverrideOriginalFieldIds = new HashSet<String>();

    @Override
    public void registerMultiValuedOverrideInSystematicGroup(String... originalFieldIds) {
        List<String> originalFieldIdsList = Arrays.asList(originalFieldIds);
        Assert.isTrue(!CollectionUtils.containsAny(multiValuedOverrideOriginalFieldIds, originalFieldIdsList));
        multiValuedOverrideOriginalFieldIds.addAll(originalFieldIdsList);
    }

    private Boolean getMultiValuedOverride(Field field) {
        Boolean multiValuedOverride = null;
        BaseObject parent = field.getParent();
        boolean inGroup;
        if (parent instanceof FieldGroup) {
            inGroup = true;
        } else {
            return null;
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

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }
    // END: setters

}
