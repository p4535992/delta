package ee.webmedia.alfresco.docconfig.service;

import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDocTypeIdAndVersionNr;

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

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.RepoConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
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
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.docadmin.service.SeparatorLine;
import ee.webmedia.alfresco.docconfig.generator.FieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.web.DocumentDialogHelperBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.TreeNode;

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
    private final Map<String /* systematicGroupName */, FieldGroupGenerator> fieldGroupGenerators = new HashMap<String, FieldGroupGenerator>();
    private final Map<String /* hiddenFieldId */, String /* fieldIdAndOriginalFieldId */> hiddenFieldDependencies = new HashMap<String, String>();

    // CUSTOM CACHING
    // XXX NB! some returned objects are unfortunately mutable, thus service callers must not modify them !!!
    private final Map<Pair<String /* documentTypeId */, Integer /* documentTypeVersionNr */>, Map<String /* fieldId */, Pair<DynamicPropertyDefinition, Field>>> propertyDefinitionCache = new ConcurrentHashMap<Pair<String, Integer>, Map<String, Pair<DynamicPropertyDefinition, Field>>>();
    private final Map<Pair<String /* documentTypeId */, Integer /* documentTypeVersionNr */>, TreeNode<QName>> childAssocTypeQNameTreeCache = new ConcurrentHashMap<Pair<String, Integer>, TreeNode<QName>>();
    private final Map<String /* fieldId */, DynamicPropertyDefinition> propertyDefinitionForSearchCache = new ConcurrentHashMap<String, DynamicPropertyDefinition>();

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
    public void registerFieldGroupGenerator(FieldGroupGenerator fieldGroupGenerator, String... systematicGroupNames) {
        Assert.notNull(fieldGroupGenerator, "fieldGroupGenerator");
        for (String systematicGroupName : systematicGroupNames) {
            Assert.notNull(systematicGroupName, "systematicGroupName");
            Assert.isTrue(!fieldGroupGenerators.containsKey(systematicGroupName), "FieldGroupGenerator with systematicGroupName already registered: " + systematicGroupName);
            fieldGroupGenerators.put(systematicGroupName, fieldGroupGenerator);
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
        return getConfig(documentTypeAndVersion.getSecond(), documentTypeAndVersion.getFirst().isShowUnvalued());
    }

    @Override
    public DocumentConfig getDocLocationConfig() {
        List<String> defList = new ArrayList<String>();
        defList.add(DocumentCommonModel.Props.FUNCTION.getLocalName());
        defList.add(DocumentCommonModel.Props.SERIES.getLocalName());
        defList.add(DocumentCommonModel.Props.VOLUME.getLocalName());
        defList.add(DocumentCommonModel.Props.CASE.getLocalName());
        DocumentConfig config = getEmptyConfig(null, null);
        for (String localName : defList) {
            FieldDefinition fieldDefinition = documentAdminService.getFieldDefinition(localName);
            fieldDefinition.setChangeableIfEnum(FieldChangeableIf.ALWAYS_CHANGEABLE);
            processField(config, fieldDefinition, false);
        }
        return config;
    }

    @Override
    public DocumentConfig getSearchConfig() {
        DocumentConfig config = getEmptyConfig(null, null);
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
            itemConfig.setConverter("ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter");
            itemConfig.setValueChangeListener("#{DocumentDynamicSearchDialog.storeValueChangeListener}");
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
                ItemConfigVO itemConfig = new ItemConfigVO(DocumentCommonModel.Props.SHORT_REG_NUMBER.toPrefixString(namespaceService));
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

    private DocumentConfig getConfig(DocumentTypeVersion docVersion, Boolean showUnvalued) {
        DocumentConfig config = getEmptyConfig(docVersion, showUnvalued);

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
        return unmodifiableConfig;
    }

    private DocumentConfig getEmptyConfig(DocumentTypeVersion docVersion, Boolean showUnvalued) {
        WMPropertySheetConfigElement propSheet = new WMPropertySheetConfigElement();
        propSheet.setShowUnvalued(showUnvalued != null ? showUnvalued : true);
        Map<String, PropertySheetStateHolder> stateHolders = new HashMap<String, PropertySheetStateHolder>();
        List<String> saveListenerBeanNames = new ArrayList<String>();
        return new DocumentConfig(propSheet, stateHolders, saveListenerBeanNames, docVersion);
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
            return generateAndAddViewModeTextInternal(name, label, config);
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

    private class FieldGroupGeneratorResultsImpl implements FieldGroupGeneratorResults {

        private final DocumentConfig config;

        public FieldGroupGeneratorResultsImpl(DocumentConfig config) {
            this.config = config;
        }

        @Override
        public Pair<Map<String, ItemConfigVO>, Map<String, PropertySheetStateHolder>> generateItems(Field... fields) {
            DocumentConfig tempConfig = getEmptyConfig(null, null);
            for (Field field : fields) {
                processField(tempConfig, field, false);
            }
            Map<?, ?> items1 = tempConfig.getPropertySheetConfigElement().getItems();
            @SuppressWarnings("unchecked")
            Map<String, ItemConfigVO> items = (Map<String, ItemConfigVO>) items1;
            Map<String, PropertySheetStateHolder> stateHolders = tempConfig.getStateHolders();
            return Pair.newInstance(items, stateHolders);
        }

        @Override
        public ItemConfigVO generateItemBase(Field field) {
            return processFieldBase(field, false);
        }

        @Override
        public ItemConfigVO generateAndAddViewModeText(String name, String label) {
            return generateAndAddViewModeTextInternal(name, label, config);
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

    private static ItemConfigVO generateAndAddViewModeTextInternal(String name, String label, DocumentConfig config) {
        ItemConfigVO viewModeTextItem = new ItemConfigVO(name);
        viewModeTextItem.setConfigItemType(ConfigItemType.PROPERTY);
        viewModeTextItem.setShowInEditMode(false);
        viewModeTextItem.setDisplayLabel(label);
        WMPropertySheetConfigElement propSheet = config.getPropertySheetConfigElement();
        Assert.isTrue(!propSheet.getItems().containsKey(viewModeTextItem.getName()), "PropertySheetItem with name already exists: " + viewModeTextItem.getName());
        propSheet.addItem(viewModeTextItem);
        return viewModeTextItem;
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
        if (fieldGroup.isSystematic()) {
            FieldGroupGenerator fieldGroupGenerator = fieldGroupGenerators.get(fieldGroup.getName());
            if (fieldGroupGenerator != null) {
                fieldGroupGenerator.generateFieldGroup(fieldGroup, new FieldGroupGeneratorResultsImpl(config));
                return;
            }
        }
        ChildrenList<Field> fields = fieldGroup.getFields();
        for (Field field : fields) {
            processField(config, field, false);
        }
    }

    private boolean processField(DocumentConfig config, Field field, boolean renderCheckboxAfterLabel) {
        ItemConfigVO item = processFieldBase(field, renderCheckboxAfterLabel);

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

    private ItemConfigVO processFieldBase(Field field, boolean renderCheckboxAfterLabel) {
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
        return item;
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
    public void setDefaultPropertyValues(Node node, QName[] childAssocTypeQNameHierarchy, boolean forceOverwrite, boolean reallySetDefaultValues, DocumentTypeVersion docVer) {
        if (docVer == null) {
            Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = getDocumentTypeAndVersion(node);
            docVer = documentTypeAndVersion.getSecond();
        } else {
            Pair<String, Integer> docTypeIdAndVersionNr = getDocTypeIdAndVersionNr(node);
            Assert.isTrue(ObjectUtils.equals(docVer.getParent().getId(), docTypeIdAndVersionNr.getFirst()));
            Assert.isTrue(ObjectUtils.equals(docVer.getVersionNr(), docTypeIdAndVersionNr.getSecond()));
        }
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = getPropertyDefinitions(getDocTypeIdAndVersionNr(docVer));
        setDefaultPropertyValues(node, childAssocTypeQNameHierarchy, forceOverwrite, reallySetDefaultValues, propertyDefinitions);
    }

    @Override
    public void setDefaultPropertyValues(Node node, QName[] requiredHierarchy, boolean forceOverwrite, boolean reallySetDefaultValues, List<Field> fields) {
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = createPropertyDefinitions(fields);
        setDefaultPropertyValues(node, requiredHierarchy, forceOverwrite, reallySetDefaultValues, propertyDefinitions);
    }

    private void setDefaultPropertyValues(Node node, QName[] requiredHierarchy, boolean forceOverwrite, boolean reallySetDefaultValues,
            Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions) {
        if (requiredHierarchy == null) {
            requiredHierarchy = new QName[] {};
        }
        outer: for (Pair<DynamicPropertyDefinition, Field> fieldAndPropDef : propertyDefinitions.values()) {
            DynamicPropertyDefinition propDef = fieldAndPropDef.getFirst();
            Field field = fieldAndPropDef.getSecond();
            if (field == null) {
                continue;
            }

            // Traverse to child-node

            QName[] hierarchy = propDef.getChildAssocTypeQNameHierarchy();
            if (hierarchy == null) {
                hierarchy = new QName[] {};
            }

            // if (propDef.hierarchy startsWith requiredHierarchy)
            int i = 0;
            for (; i < requiredHierarchy.length; i++) {
                if (hierarchy.length <= i) {
                    // propDef.hierarchy is shorter, so it doesn't match
                    continue outer;
                }
                if (!hierarchy[i].equals(requiredHierarchy[i])) {
                    // doesn't match
                    continue outer;
                }
            }

            // TODO Alar: would be better to refactor this to be more efficient and happen in a more general place (one or two methods above)
            if (field.getParent() instanceof FieldGroup) {
                FieldGroup group = (FieldGroup) field.getParent();
                if (group.isSystematic()) {
                    FieldGroupGenerator fieldGroupGenerator = fieldGroupGenerators.get(group.getName());
                    if (fieldGroupGenerator != null) {
                        Pair<Field, List<Field>> relatedFields = fieldGroupGenerator.collectAndRemoveFieldsInOriginalOrderToFakeGroup(new ArrayList<Field>(group.getFields()),
                                field, group.getFieldsByOriginalId());
                        if (relatedFields != null) {
                            field = relatedFields.getFirst();
                        }
                    }
                }
            }

            List<Node> childNodes = collectChildNodes(node, hierarchy, i);
            for (Node childNode : childNodes) {
                setDefaultPropertyValue(childNode, propDef, forceOverwrite, reallySetDefaultValues, field, propertyDefinitions);
            }
        }
    }

    private List<Node> collectChildNodes(Node node, QName[] hierarchy, int i) {
        if (i >= hierarchy.length) {
            return Collections.singletonList(node);
        }
        List<Node> results = new ArrayList<Node>();
        List<Node> childNodes = node.getAllChildAssociations(hierarchy[i]);
        for (Node childNode : childNodes) {
            results.addAll(collectChildNodes(childNode, hierarchy, i + 1));
        }
        return results;
    }

    private void setDefaultPropertyValue(Node node, DynamicPropertyDefinition propDef, boolean forceOverwrite, boolean reallySetDefaultValues, Field field,
            Map<String, Pair<DynamicPropertyDefinition, Field>> allFieldsAndPropDefs) {
        Serializable value = (Serializable) node.getProperties().get(field.getQName());
        if (value != null && !forceOverwrite) {
            return;
        }

        Serializable defaultValue = null;
        if (reallySetDefaultValues) {
            QName dataType = propDef.getDataTypeQName();
            if (StringUtils.isNotEmpty(field.getDefaultValue())) {
                if (DataTypeDefinition.TEXT.equals(dataType) && field.getFieldTypeEnum() != FieldType.INFORMATION_TEXT) {
                    defaultValue = field.getDefaultValue();
                } else if (DataTypeDefinition.LONG.equals(dataType) || DataTypeDefinition.DOUBLE.equals(dataType)) {
                    DataTypeDefinition dataTypeDefinition = dictionaryService.getDataType(dataType);
                    try {
                        defaultValue = (Serializable) DefaultTypeConverter.INSTANCE.convert(dataTypeDefinition, field.getDefaultValue());
                    } catch (Exception e) {
                        // TODO change to debug
                        LOG.warn("Failed to convert defaultValue to required data type\nfield=" + field.toString() + "\nnode=" + node, e);
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
                    Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                    setUserContactProps(props, AuthenticationUtil.getRunAsUser(), propDef, field);
                    for (Entry<QName, Serializable> entry : props.entrySet()) {
                        QName propName = entry.getKey();
                        Pair<DynamicPropertyDefinition, Field> fieldAndPropDef = allFieldsAndPropDefs.get(propName.getLocalName());
                        // Check that all properties are on the same node
                        Assert.isTrue(fieldAndPropDef != null
                                && Arrays.equals(fieldAndPropDef.getFirst().getChildAssocTypeQNameHierarchy(), propDef.getChildAssocTypeQNameHierarchy()));
                        node.getProperties().put(propName.toString(), entry.getValue());
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
        }

        if (defaultValue == null) {
            // In Search/MultiValueEditor component, display one empty row by default, not zero rows
            Boolean multiValuedOverride = propDef.getMultiValuedOverride();
            if (field.getFieldTypeEnum() == FieldType.USERS || field.getFieldTypeEnum() == FieldType.CONTACTS || field.getFieldTypeEnum() == FieldType.USERS_CONTACTS
                    || (multiValuedOverride != null && multiValuedOverride)) {
                ArrayList<Serializable> list = new ArrayList<Serializable>();
                list.add(null);
                defaultValue = list;

            } else if (field.getFieldTypeEnum() == FieldType.LISTBOX) {
                // UISelectMany component needs value to be List or Array, does not accept null value
                defaultValue = new ArrayList<String>();
            }
        }

        if (defaultValue != null) {
            node.getProperties().put(field.getQName().toString(), defaultValue);
        }
    }

    @Override
    public void setUserContactProps(Map<QName, Serializable> props, String userName, String fieldId) {
        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs = getPropertyDefinitions(getDocTypeIdAndVersionNr(props));
        Pair<DynamicPropertyDefinition, Field> propDefAndField = propDefs.get(fieldId);
        setUserContactProps(props, userName, propDefAndField.getFirst(), propDefAndField.getSecond());
    }

    @Override
    public void setUserContactProps(Map<QName, Serializable> props, String userName, PropertyDefinition propDef, Field field) {
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
    public DynamicPropertyDefinition getPropertyDefinition(Node documentDynamicNode, QName property) {
        if (!DocumentDynamicModel.URI.equals(property.getNamespaceURI())) {
            return null;
        }
        if (DocumentSearchModel.Types.FILTER.equals(documentDynamicNode.getType())) {
            if (hiddenFieldDependencies.containsKey(property.getLocalName())) {
                String originalFieldId = hiddenFieldDependencies.get(property.getLocalName());
                DynamicPropertyDefinition originalPropDef = getPropDefForSearch(originalFieldId);
                DynamicPropertyDefinition propDef = createPropertyDefinitionForHiddenField(property.getLocalName(), originalPropDef);
                return propDef;
            }
            return getPropDefForSearch(property.getLocalName());
        }
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = getPropertyDefinitions(documentDynamicNode);
        if (propertyDefinitions == null) {
            return null;
        }
        Pair<DynamicPropertyDefinition, Field> propertyDefinition = propertyDefinitions.get(property.getLocalName());
        if (propertyDefinition == null) {
            LOG.warn("\n\n!!!!!!!!!!!!!!!!!!! fieldId=" + property + " not found, documentDynamicNode=" + documentDynamicNode + "\n");
            return null;
        }
        return propertyDefinition.getFirst();
    }

    private DynamicPropertyDefinition getPropDefForSearch(String fieldId) {
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
        return new DynamicPropertyDefinitionImpl(field, isFieldForcedMultipleInSearch(field), null);
    }

    @Override
    public DynamicPropertyDefinition getPropertyDefinitionById(String fieldId) {
        if (fieldId == null) {
            return null;
        }

        DynamicPropertyDefinition propertyDefinition = propertyDefinitionForSearchCache.get(fieldId);
        if (propertyDefinition != null) {
            return propertyDefinition;
        }

        propertyDefinition = getPropDefForSearch(fieldId);
        if (propertyDefinition == null) {
            return null;
        }

        propertyDefinitionForSearchCache.put(fieldId, propertyDefinition);
        return propertyDefinition;
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
    public Map<String, Pair<DynamicPropertyDefinition, Field>> getPropertyDefinitions(Node node) {
        QName type = node.getType();
        // XXX Alar: checking hasAspect(OBJECT) would be the same
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type) && !dictionaryService.isSubClass(type, DocumentCommonModel.Types.METADATA_CONTAINER)) {
            return null;
        }
        return getPropertyDefinitions(getDocTypeIdAndVersionNr(node));
    }

    @Override
    public Map<String, Pair<DynamicPropertyDefinition, Field>> getPropertyDefinitions(Pair<String, Integer> docTypeIdAndVersionNr) {
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = propertyDefinitionCache.get(docTypeIdAndVersionNr);
        if (propertyDefinitions == null) {
            Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = getDocumentTypeAndVersion(docTypeIdAndVersionNr);
            if (documentTypeAndVersion == null) {
                return null;
            }
            DocumentTypeVersion docVersion = documentTypeAndVersion.getSecond();
            docVersion.resetParent();
            propertyDefinitions = createPropertyDefinitions(docVersion.getFieldsDeeply());
            if (LOG.isDebugEnabled()) {
                StringBuilder s = new StringBuilder();
                s.append("Created propertyDefinitions for cacheKey=").append(docTypeIdAndVersionNr).append(" - ");
                s.append("[").append(propertyDefinitions.size()).append("]");
                for (Entry<String, Pair<DynamicPropertyDefinition, Field>> entry : propertyDefinitions.entrySet()) {
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

    private Map<String, Pair<DynamicPropertyDefinition, Field>> createPropertyDefinitions(List<Field> fields) {
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = new HashMap<String, Pair<DynamicPropertyDefinition, Field>>();
        // TODO documentTypeVersion, fields and fieldGroups should be immutable; or they should be cloned in get method
        for (Field field : fields) {
            String fieldId = field.getFieldId();
            Assert.isTrue(!propertyDefinitions.containsKey(fieldId));
            DynamicPropertyDefinition propDef = createPropertyDefinition(field);
            propertyDefinitions.put(fieldId, new Pair<DynamicPropertyDefinition, Field>(propDef, field));
        }
        for (Entry<String, String> entry : hiddenFieldDependencies.entrySet()) {
            Pair<DynamicPropertyDefinition, Field> originalPropDefAndField = propertyDefinitions.get(entry.getValue());
            // XXX originalPropDefAndField.getSecond().getOriginalFieldId() may be null if this is a field which is not systematic, but should be - was defined before this
            // systematic fieldDefinition was imported. Correct would be to fix incorrect data, see task 185569
            if (originalPropDefAndField != null && entry.getValue().equals(originalPropDefAndField.getSecond().getOriginalFieldId())) {
                String hiddenFieldId = entry.getKey();
                DynamicPropertyDefinition propDef = createPropertyDefinitionForHiddenField(hiddenFieldId, originalPropDefAndField.getFirst());
                propertyDefinitions.put(hiddenFieldId, new Pair<DynamicPropertyDefinition, Field>(propDef, null));
            }
        }
        return propertyDefinitions;
    }

    @Override
    public DynamicPropertyDefinition createPropertyDefinition(Field field) {
        Boolean multiValuedOverride = getMultiValuedOverride(field);
        QName[] childAssocTypeQNameHierarchy = getChildAssocTypeQNameHierarchy(field);
        return new DynamicPropertyDefinitionImpl(field, multiValuedOverride, childAssocTypeQNameHierarchy);
    }

    private DynamicPropertyDefinition createPropertyDefinitionForHiddenField(String hiddenFieldId, PropertyDefinition originalPropertyDefinition) {
        return new DynamicPropertyDefinitionImpl(hiddenFieldId, (DynamicPropertyDefinitionImpl) originalPropertyDefinition);
    }

    private final Map<Pair<String /* systematicGroupName */, String /* originalFieldId */>, QName[]> childAssocTypeQNameHierarchies = new HashMap<Pair<String, String>, QName[]>();

    @Override
    public void registerChildAssocTypeQNameHierarchy(String systematicGroupName, QName childAssocTypeQName, Map<QName[], Set<String>> additionalChildAssocTypeQNameHierarchy) {
        Assert.hasLength(systematicGroupName);
        Assert.notNull(childAssocTypeQName);
        // Add by groupName only, covers all fields in systematic group
        Pair<String, String> globalKey = Pair.newInstance(systematicGroupName, (String) null);
        Assert.isTrue(!childAssocTypeQNameHierarchies.containsKey(globalKey));
        for (QName[] qNames : childAssocTypeQNameHierarchies.values()) {
            Assert.isTrue(!childAssocTypeQName.equals(qNames[0]));
        }
        childAssocTypeQNameHierarchies.put(globalKey, new QName[] { childAssocTypeQName });

        // Add by groupName + specific originalFieldId
        if (additionalChildAssocTypeQNameHierarchy != null) {
            Set<String> allOriginalFieldIds = new HashSet<String>();
            for (Entry<QName[], Set<String>> entry : additionalChildAssocTypeQNameHierarchy.entrySet()) {
                QName[] additionalAssocTypeQNames = entry.getKey();
                Assert.notNull(additionalAssocTypeQNames);
                Assert.isTrue(additionalAssocTypeQNames.length > 0 && !Arrays.asList(additionalAssocTypeQNames).contains(null));
                for (String originalFieldId : entry.getValue()) {
                    Assert.notNull(originalFieldId);
                    Assert.isTrue(!allOriginalFieldIds.contains(originalFieldId));
                    allOriginalFieldIds.add(originalFieldId);
                    Pair<String, String> key = Pair.newInstance(systematicGroupName, originalFieldId);
                    Assert.isTrue(!childAssocTypeQNameHierarchies.containsKey(key));
                    childAssocTypeQNameHierarchies.put(key, (QName[]) ArrayUtils.add(additionalAssocTypeQNames, 0, childAssocTypeQName));
                }
            }
        }

        documentAdminService.registerGroupLimitSingle(systematicGroupName);
    }

    private QName[] getChildAssocTypeQNameHierarchy(Field field) {
        if (field.getParent() instanceof FieldGroup) {
            FieldGroup group = (FieldGroup) field.getParent();
            if (!group.isSystematic()) {
                return null;
            }
            Assert.notNull(field.getOriginalFieldId());
            // Find by groupName + specific originalFieldId
            QName[] hierarchies = childAssocTypeQNameHierarchies.get(Pair.newInstance(group.getName(), field.getOriginalFieldId()));
            if (hierarchies != null) {
                return hierarchies;
            }
            // Find by groupName only, covers all fields in systematic group
            hierarchies = childAssocTypeQNameHierarchies.get(Pair.newInstance(group.getName(), (String) null));
            return hierarchies;
        }
        return null;
    }

    @Override
    public TreeNode<QName> getChildAssocTypeQNameTree(DocumentTypeVersion docVer) {
        Pair<String, Integer> cacheKey = Pair.newInstance(docVer.getParent().getId(), docVer.getVersionNr());
        TreeNode<QName> root = childAssocTypeQNameTreeCache.get(cacheKey);
        if (root != null) {
            return root;
        }
        root = new TreeNode<QName>(null);
        for (MetadataItem metadataItem : docVer.getMetadata()) {
            if (!(metadataItem instanceof FieldGroup)) {
                continue;
            }
            FieldGroup group = (FieldGroup) metadataItem;
            if (!group.isSystematic()) {
                continue;
            }
            QName[] hierarchy = childAssocTypeQNameHierarchies.get(Pair.newInstance(group.getName(), null));
            if (hierarchy == null) {
                continue;
            }

            Assert.isTrue(hierarchy.length == 1 && hierarchy[0] != null);
            for (TreeNode<QName> treeNode : root.getChildren()) {
                Assert.isTrue(!treeNode.getData().equals(hierarchy[0]));
            }
            TreeNode<QName> treeNode = new TreeNode<QName>(hierarchy[0]);
            root.getChildren().add(treeNode);

            for (Field field : group.getFields()) {
                Assert.notNull(field.getOriginalFieldId());
                hierarchy = childAssocTypeQNameHierarchies.get(Pair.newInstance(group.getName(), field.getOriginalFieldId()));
                if (hierarchy == null) {
                    continue;
                }
                Assert.isTrue(hierarchy.length >= 2 && treeNode.getData().equals(hierarchy[0]));
                int i = 1;
                TreeNode<QName> current = treeNode;
                while (i < hierarchy.length) {
                    Assert.isTrue(hierarchy[i] != null);
                    TreeNode<QName> foundChild = null;
                    for (TreeNode<QName> currentChild : current.getChildren()) {
                        if (currentChild.getData().equals(hierarchy[i])) {
                            Assert.isNull(foundChild);
                            foundChild = currentChild;
                        }
                    }
                    if (foundChild == null) {
                        foundChild = new TreeNode<QName>(hierarchy[i]);
                        current.getChildren().add(foundChild);
                    }
                    current = foundChild;
                    i++;
                }
            }
        }
        childAssocTypeQNameTreeCache.put(cacheKey, root);
        return root;
    }

    @Override
    public TreeNode<QName> getChildAssocTypeQNameTree(Node documentDynamicNode) {
        Pair<String, Integer> cacheKey = getDocTypeIdAndVersionNr(documentDynamicNode);
        TreeNode<QName> childAssocTypeQNames = childAssocTypeQNameTreeCache.get(cacheKey);
        if (childAssocTypeQNames == null) {
            Pair<DocumentType, DocumentTypeVersion> docTypeAndVer = getDocumentTypeAndVersion(cacheKey);
            childAssocTypeQNames = getChildAssocTypeQNameTree(docTypeAndVer.getSecond());
        }
        return childAssocTypeQNames;
    }

    private final Set<String> multiValuedOverrideOriginalFieldIds = new HashSet<String>();
    private final Map<String, Set<String>> multiValuedOverrideBySystematicGroup = new HashMap<String, Set<String>>();

    @Override
    public void registerMultiValuedOverrideInSystematicGroup(String... originalFieldIds) {
        List<String> originalFieldIdsList = Arrays.asList(originalFieldIds);
        Assert.isTrue(!CollectionUtils.containsAny(multiValuedOverrideOriginalFieldIds, originalFieldIdsList));
        multiValuedOverrideOriginalFieldIds.addAll(originalFieldIdsList);
    }

    @Override
    public void registerMultiValuedOverrideBySystematicGroupName(String systematicGroupName, Set<String> originalFieldIds) {
        Assert.isTrue(!multiValuedOverrideBySystematicGroup.containsKey(systematicGroupName));
        multiValuedOverrideBySystematicGroup.put(systematicGroupName, new HashSet<String>(originalFieldIds));
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
            Set<String> set = multiValuedOverrideBySystematicGroup.get(((FieldGroup) parent).getName());
            if (set != null) {
                if (set.contains(field.getOriginalFieldId())) {
                    multiValuedOverride = Boolean.TRUE;
                }
            } else {
                Set<String> originalFieldIds = ((FieldGroup) parent).getOriginalFieldIds();
                originalFieldIds.retainAll(multiValuedOverrideOriginalFieldIds);
                if (!originalFieldIds.isEmpty()) {
                    multiValuedOverride = Boolean.TRUE;
                }
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