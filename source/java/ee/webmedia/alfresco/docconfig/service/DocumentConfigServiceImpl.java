package ee.webmedia.alfresco.docconfig.service;

import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDocTypeIdAndVersionNr;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getDynamicTypeClass;
import static ee.webmedia.alfresco.docadmin.web.DocAdminUtil.getPropDefCacheKey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.cache.SimpleCache;
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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.base.BaseObject.ChildrenList;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.TemplateReportOutputType;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.EnumSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ValidatingModalLayerComponent;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.UserContactGroupSearchBean;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docadmin.service.MetadataItem;
import ee.webmedia.alfresco.docadmin.service.SeparatorLine;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.DateGenerator;
import ee.webmedia.alfresco.docconfig.generator.fieldtype.UserContactGenerator;
import ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.web.DocumentDialogHelperBean;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.search.model.DocumentReportModel;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.document.search.web.DocumentDynamicSearchDialog;
import ee.webmedia.alfresco.document.search.web.SearchBlockBean;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TreeNode;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.search.model.VolumeReportModel;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;
import ee.webmedia.alfresco.workflow.search.model.CompoundWorkflowSearchModel;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;

/**
 * @author Alar Kvell
 */
public class DocumentConfigServiceImpl implements DocumentConfigService, BeanFactoryAware {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentConfigServiceImpl.class);

    private DocumentAdminService documentAdminService;
    private DictionaryService dictionaryService;
    private NamespaceService namespaceService;
    private UserContactMappingService userContactMappingService;
    private UserService userService;
    private ClassificatorService classificatorService;
    private EInvoiceService _einvoiceService;
    protected BeanFactory beanFactory;

    private final Map<FieldType, FieldGenerator> fieldGenerators = new HashMap<FieldType, FieldGenerator>();
    private final Map<String, FieldGenerator> originalFieldIdGenerators = new HashMap<String, FieldGenerator>();
    private final Map<String /* systematicGroupName */, FieldGroupGenerator> fieldGroupGenerators = new HashMap<String, FieldGroupGenerator>();
    private final Map<String /* hiddenFieldId */, String /* fieldIdAndOriginalFieldId */> hiddenFieldDependencies = new HashMap<String, String>();
    public static final Map<QName, String> searchLabelIds;

    private boolean regDateFilterInAssociationsSearch;

    static {
        searchLabelIds = new HashMap<QName, String>();
        searchLabelIds.put(DocumentSearchModel.Props.STORE, "document_search_stores");
        searchLabelIds.put(DocumentDynamicSearchDialog.SELECTED_STORES, "document_search_stores");
        searchLabelIds.put(DocumentSearchModel.Props.INPUT, "document_search_input");
        searchLabelIds.put(DocumentSearchModel.Props.DOCUMENT_TYPE, "document_docType");
        searchLabelIds.put(DocumentSearchModel.Props.SEND_MODE, "document_send_mode");
        searchLabelIds.put(DocumentSearchModel.Props.SEND_INFO_RECIPIENT, "document_search_export_recipient");
        searchLabelIds.put(DocumentSearchModel.Props.SEND_INFO_SEND_DATE_TIME, "document_search_send_info_time_period");
        searchLabelIds.put(DocumentSearchModel.Props.SEND_INFO_RESOLUTION, "document_search_send_info_resolution");
        searchLabelIds.put(DocumentSearchModel.Props.FUND, "transaction_fund");
        searchLabelIds.put(DocumentSearchModel.Props.FUNDS_CENTER, "transaction_fundsCenter");
        searchLabelIds.put(DocumentSearchModel.Props.EA_COMMITMENT_ITEM, "transaction_eaCommitmentItem");
        searchLabelIds.put(DocumentSearchModel.Props.DOCUMENT_CREATED, "document_search_document_created");
        searchLabelIds.put(DocumentReportModel.Props.REPORT_OUTPUT_TYPE, "document_report_output");
        searchLabelIds.put(DocumentReportModel.Props.REPORT_TEMPLATE, "document_report_template");
        searchLabelIds.put(DocumentCommonModel.Props.SHORT_REG_NUMBER, "document_shortRegNumber");
        searchLabelIds.put(DocumentLocationGenerator.CASE_LABEL_EDITABLE, "case");

        searchLabelIds.put(TaskSearchModel.Props.STARTED_DATE_TIME_BEGIN, "task_search_startedDateTime");
        searchLabelIds.put(TaskSearchModel.Props.TASK_TYPE, "task_search_taskType");
        searchLabelIds.put(TaskSearchModel.Props.OWNER_NAME, "task_search_owner");
        searchLabelIds.put(TaskSearchModel.Props.CREATOR_NAME, "task_search_creator");
        searchLabelIds.put(TaskSearchModel.Props.ORGANIZATION_NAME, "task_search_organization");
        searchLabelIds.put(TaskSearchModel.Props.JOB_TITLE, "task_search_job_title");
        searchLabelIds.put(TaskSearchModel.Props.DUE_DATE_TIME_BEGIN, "task_search_dueDateTime");
        searchLabelIds.put(TaskSearchModel.Props.ONLY_RESPONSIBLE, "task_search_only_responsible");
        searchLabelIds.put(TaskSearchModel.Props.COMPLETED_DATE_TIME_BEGIN, "task_search_completedDateTime");
        searchLabelIds.put(TaskSearchModel.Props.OUTCOME, "task_search_outcome");
        searchLabelIds.put(TaskSearchModel.Props.COMMENT, "task_search_comment");
        searchLabelIds.put(TaskSearchModel.Props.RESOLUTION, "task_search_resolution");
        searchLabelIds.put(TaskSearchModel.Props.STATUS, "task_search_status");
        searchLabelIds.put(TaskSearchModel.Props.COMPLETED_OVERDUE, "task_search_completed_overdue");
        searchLabelIds.put(TaskSearchModel.Props.STOPPED_DATE_TIME_BEGIN, "task_search_stoppedDateTime");
        searchLabelIds.put(TaskSearchModel.Props.DOC_TYPE, "document_docType");

        searchLabelIds.put(CompoundWorkflowSearchModel.Props.TYPE, "cw_search_type");
        searchLabelIds.put(CompoundWorkflowSearchModel.Props.TITLE, "cw_search_title");
        searchLabelIds.put(CompoundWorkflowSearchModel.Props.OWNER_NAME, "cw_search_owner");
        searchLabelIds.put(CompoundWorkflowSearchModel.Props.STRUCT_UNIT, "cw_search_struct_unit");
        searchLabelIds.put(CompoundWorkflowSearchModel.Props.JOB_TITLE, "cw_search_job_title");
        searchLabelIds.put(CompoundWorkflowSearchModel.Props.CREATED_DATE, "cw_search_create_date");
        searchLabelIds.put(CompoundWorkflowSearchModel.Props.IGNITION_DATE, "cw_search_ignition_date");
        searchLabelIds.put(CompoundWorkflowSearchModel.Props.STOPPED_DATE, "cw_search_stopped_date");
        searchLabelIds.put(CompoundWorkflowSearchModel.Props.ENDING_DATE, "cw_search_ending_date");
        searchLabelIds.put(CompoundWorkflowSearchModel.Props.STATUS, "cw_search_status");
        searchLabelIds.put(CompoundWorkflowSearchModel.Props.COMMENT, "cw_search_comment");

        searchLabelIds.put(VolumeSearchModel.Props.STORE, "volume_search_stores");
        searchLabelIds.put(VolumeSearchModel.Props.INPUT, "volume_search_input");
        searchLabelIds.put(VolumeSearchModel.Props.VOLUME_TYPE, "volume_search_volume_type");
        searchLabelIds.put(VolumeSearchModel.Props.CASE_FILE_TYPE, "volume_search_case_volume_type");
        searchLabelIds.put(VolumeReportModel.Props.REPORT_TEMPLATE, "volume_search_report_template");

    }

    // XXX NB! some returned objects are unfortunately mutable, thus service callers must not modify them !!!
    private SimpleCache<PropDefCacheKey, Map<String /* fieldId */, Pair<DynamicPropertyDefinition, Field>>> propertyDefinitionCache;
    private SimpleCache<Pair<String /* documentTypeId */, Integer /* documentTypeVersionNr */>, TreeNode<QName>> childAssocTypeQNameTreeCache;
    private SimpleCache<String /* fieldId */, DynamicPropertyDefinition> propertyDefinitionForSearchCache;

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
        QName type = documentDynamicNode.getType();
        if (DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = getDocumentTypeAndVersion(documentDynamicNode);
            return getConfig(documentTypeAndVersion.getSecond(), documentTypeAndVersion.getFirst().isShowUnvalued());
        } else if (CaseFileModel.Types.CASE_FILE.equals(type)) {
            PropDefCacheKey key = DocAdminUtil.getPropDefCacheKey(documentDynamicNode);
            return getConfig(documentAdminService.getCaseFileTypeAndVersion(key.getDynamicTypeId(), key.getVersion()).getSecond(), Boolean.TRUE);
        }

        throw new RuntimeException("Config isn't supported for " + type);
    }

    @Override
    public DocumentConfig getDocLocationConfig() {
        DocumentConfig config = getEmptyConfig(null, null);
        addDocLocationConfigFields(config, false, null);
        return config;
    }

    private void addDocLocationConfigFields(DocumentConfig config, boolean forceEditMode, String additionalStateHolderKey) {
        List<String> defList = new ArrayList<String>();
        defList.add(DocumentCommonModel.Props.FUNCTION.getLocalName());
        defList.add(DocumentCommonModel.Props.SERIES.getLocalName());
        defList.add(DocumentCommonModel.Props.VOLUME.getLocalName());
        defList.add(DocumentCommonModel.Props.CASE.getLocalName());
        for (String localName : defList) {
            FieldDefinition fieldDefinition = documentAdminService.getFieldDefinition(localName);
            fieldDefinition.setChangeableIfEnum(FieldChangeableIf.ALWAYS_CHANGEABLE);
            processField(config, fieldDefinition, false, forceEditMode, additionalStateHolderKey);
        }
    }

    @Override
    public DocumentConfig getAssocObjectSearchConfig(String additionalStateHolderKey, String renderAssocObjectFieldValueBinding) {
        DocumentConfig config = getEmptyConfig(null, null);

        // docsearch:store
        if (SearchBlockBean.BEAN_NAME.equals(additionalStateHolderKey)) {
            QName prop = DocumentSearchModel.Props.STORE;
            ItemConfigVO itemConfig = createItemConfigVO(prop);
            itemConfig.setComponentGenerator("GeneralSelectorGenerator");
            itemConfig.setSelectionItems("#{DialogManager.bean.search.getStores}");
            itemConfig.setConverter("ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter");
            itemConfig.setValueChangeListener("#{DialogManager.bean.search.storeValueChanged}");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }

        {
            // docsearch:objectType
            ItemConfigVO itemConfig = new ItemConfigVO(DocumentSearchModel.Props.OBJECT_TYPE.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("document_search_object_type");
            itemConfig.setComponentGenerator("EnumSelectorGenerator");
            itemConfig.setRenderCheckboxAfterLabel(false);
            itemConfig.setEnumClass("ee.webmedia.alfresco.document.search.model.AssocSearchObjectType");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            itemConfig.getCustomAttributes().put(EnumSelectorGenerator.ATTR_DISABLE_DEFAULT, Boolean.TRUE.toString());
            itemConfig.setRendered(renderAssocObjectFieldValueBinding);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }

        addInputConfigItem(config, true);

        {
            // docsearch:objectTitle
            ItemConfigVO itemConfig = new ItemConfigVO(DocumentSearchModel.Props.OBJECT_TITLE.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("document_search_object_title");
            itemConfig.setStyleClass("searchAssocOnEnter focus");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }

        addDocLocationConfigFields(config, true, additionalStateHolderKey);

        addDocumentCreatedConfigItem(config);

        if (regDateFilterInAssociationsSearch) {
            // docsearch:documentRegistered
            ItemConfigVO itemConfig = new ItemConfigVO(DocumentCommonModel.Props.REG_DATE_TIME.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("document_regDateTime2");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            DateGenerator.setupDateFilterItemConfig(itemConfig, DocumentCommonModel.Props.REG_DATE_TIME);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }

        addDocumentTypeConfigItem(false, config);

        return config;
    }

    @Override
    public DocumentConfig getSearchConfig() {
        return getFilterConfig(true);
    }

    protected DocumentConfig getFilterConfig(boolean withCheckboxes) {
        DocumentConfig config = getEmptyConfig(null, null);
        /**
         * <show-property name="docsearch:store" display-label-id="document_search_stores" component-generator="GeneralSelectorGenerator"
         * selectionItems="#{DocumentSearchDialog.getStores}" converter="ee.webmedia.alfresco.common.propertysheet.converter.StoreRefConverter" />
         */
        WMPropertySheetConfigElement propertySheetConfigElement = config.getPropertySheetConfigElement();
        {
            // docsearch:store
            QName prop = DocumentSearchModel.Props.STORE;
            ItemConfigVO itemConfig = createItemConfigVO(prop);
            itemConfig.setComponentGenerator("GeneralSelectorGenerator");
            itemConfig.setSelectionItems("#{DialogManager.bean.getStores}");
            itemConfig.setConverter("ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter");
            itemConfig.setValueChangeListener("#{DialogManager.bean.storeValueChanged}");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            propertySheetConfigElement.addItem(itemConfig);
        }

        addInputConfigItem(config, false);

        addDocumentTypeConfigItem(withCheckboxes, config);

        /**
         * <show-property name="docsearch:sendMode" display-label-id="document_send_mode" component-generator="ClassificatorSelectorGenerator" classificatorName="sendModeSearch" />
         */
        {
            // docsearch:sendMode
            QName prop = DocumentSearchModel.Props.SEND_MODE;
            ItemConfigVO itemConfig = createItemConfigVO(prop);
            itemConfig.setComponentGenerator("ClassificatorSelectorGenerator");
            itemConfig.setRenderCheckboxAfterLabel(withCheckboxes);
            itemConfig.setClassificatorName("transmittalMode"); // sendModeSearch classificator is deprecated
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            propertySheetConfigElement.addItem(itemConfig);
        }

        addSendInfoConfigItems(withCheckboxes, propertySheetConfigElement);

        List<FieldDefinition> fields = documentAdminService.getSearchableDocumentFieldDefinitions();
        for (FieldDefinition fieldDefinition : fields) {
            processFieldForSearchView(fieldDefinition);
            processField(config, fieldDefinition, withCheckboxes, false);
            if (fieldDefinition.getFieldId().equals("regNumber")) {
                QName prop = DocumentCommonModel.Props.SHORT_REG_NUMBER;
                ItemConfigVO itemConfig = createItemConfigVO(prop);
                itemConfig.setComponentGenerator("TextAreaGenerator");
                itemConfig.setStyleClass("expand19-200");
                // itemConfig.setIgnoreIfMissing(false);
                itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
                propertySheetConfigElement.addItem(itemConfig);
            }
        }

        if (getEInvoiceServiceService().isEinvoiceEnabled()) {

            /**
             * <show-property name="docsearch:fund" display-label-id="transaction_fund" component-generator="MultiValueEditorGenerator"
             * showHeaders="false" styleClass="add-default" noAddLinkLabel="true" addLabelId="add_row" isAutomaticallyAddRows="true"
             * propsGeneration="docsearch:fund¤DimensionSelectorGenerator¤dimensionName=invoiceFunds¤styleClass=expand19-200 tooltip¤converter="/>
             */
            {
                // docsearch:fund
                QName prop = DocumentSearchModel.Props.FUND;
                ItemConfigVO itemConfig = createItemConfigVO(prop);
                itemConfig.setComponentGenerator("MultiValueEditorGenerator");
                itemConfig.setShowHeaders(false);
                itemConfig.setStyleClass("add-default");
                itemConfig.setNoAddLinkLabel(true);
                itemConfig.setAddLabelId("add_row");
                itemConfig.setIsAutomaticallyAddRows(true);
                itemConfig.setPropsGeneration("docsearch:fund¤DimensionSelectorGenerator¤dimensionName=invoiceFunds¤styleClass=expand19-200 tooltip¤converter=");
                itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
                propertySheetConfigElement.addItem(itemConfig);
            }

            /**
             * <show-property name="docsearch:fundsCenter" display-label-id="transaction_fundsCenter" component-generator="MultiValueEditorGenerator"
             * showHeaders="false" styleClass="add-default" noAddLinkLabel="true" addLabelId="add_row" isAutomaticallyAddRows="true"
             * propsGeneration="docsearch:fundsCenter¤DimensionSelectorGenerator¤dimensionName=invoiceFundsCenters¤styleClass=expand19-200 tooltip¤converter="/>
             */
            {
                // docsearch:fundsCenter
                QName prop = DocumentSearchModel.Props.FUNDS_CENTER;
                ItemConfigVO itemConfig = createItemConfigVO(prop);
                itemConfig.setComponentGenerator("MultiValueEditorGenerator");
                itemConfig.setShowHeaders(false);
                itemConfig.setStyleClass("add-default");
                itemConfig.setNoAddLinkLabel(true);
                itemConfig.setAddLabelId("add_row");
                itemConfig.setIsAutomaticallyAddRows(true);
                itemConfig.setPropsGeneration("docsearch:fundsCenter¤DimensionSelectorGenerator¤dimensionName=invoiceFundsCenters¤styleClass=expand19-200 tooltip¤converter=");
                itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
                propertySheetConfigElement.addItem(itemConfig);
            }

            /**
             * <show-property name="docsearch:eaCommitmentItem" display-label-id="transaction_eaCommitmentItem" component-generator="MultiValueEditorGenerator"
             * showHeaders="false" styleClass="add-default" noAddLinkLabel="true" addLabelId="add_row" filter="eaPrefixInclude" isAutomaticallyAddRows="true"
             * propsGeneration="docsearch:eaCommitmentItem¤DimensionSelectorGenerator¤dimensionName=invoiceCommitmentItem¤styleClass=expand19-200 tooltip¤converter="/>
             */
            {
                // docsearch:eaCommitmentItem
                QName prop = DocumentSearchModel.Props.EA_COMMITMENT_ITEM;
                ItemConfigVO itemConfig = createItemConfigVO(prop);
                itemConfig.setComponentGenerator("MultiValueEditorGenerator");
                itemConfig.setShowHeaders(false);
                itemConfig.setStyleClass("add-default");
                itemConfig.setNoAddLinkLabel(true);
                itemConfig.setAddLabelId("add_row");
                itemConfig.setFilter("eaPrefixInclude");
                itemConfig.setIsAutomaticallyAddRows(true);
                itemConfig
                        .setPropsGeneration(
                        "docsearch:eaCommitmentItem¤DimensionSelectorGenerator¤filter=eaPrefixInclude¤dimensionName=invoiceCommitmentItem¤styleClass=expand19-200 tooltip¤converter=");
                itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
                propertySheetConfigElement.addItem(itemConfig);
            }
        }

        addDocumentCreatedConfigItem(config);

        return config;
    }

    public void addSendInfoConfigItems(boolean withCheckboxes, WMPropertySheetConfigElement propertySheetConfigElement) {
        {
            QName prop = DocumentSearchModel.Props.SEND_INFO_RECIPIENT;
            ItemConfigVO itemConfig = createItemConfigVO(prop);
            itemConfig.setSearchSuggestDisabled(Boolean.TRUE);
            UserContactGenerator.setupDefaultUserSearch(itemConfig);
            itemConfig.setFilterIndex(UserContactGroupSearchBean.CONTACTS_FILTER);
            itemConfig.setRenderCheckboxAfterLabel(withCheckboxes);
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            propertySheetConfigElement.addItem(itemConfig);
        }

        {
            QName prop = DocumentSearchModel.Props.SEND_INFO_SEND_DATE_TIME;
            ItemConfigVO itemConfig = createItemConfigVO(prop);
            itemConfig.setRenderCheckboxAfterLabel(withCheckboxes);
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            DateGenerator.setupDateFilterItemConfig(itemConfig, prop);
            propertySheetConfigElement.addItem(itemConfig);
        }

        {
            QName prop = DocumentSearchModel.Props.SEND_INFO_RESOLUTION;
            ItemConfigVO itemConfig = createItemConfigVO(prop);
            itemConfig.setComponentGenerator("TextAreaGenerator");
            itemConfig.setRenderCheckboxAfterLabel(withCheckboxes);
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            propertySheetConfigElement.addItem(itemConfig);
        }
    }

    private void addDocumentTypeConfigItem(boolean withCheckboxes, DocumentConfig config) {
        /**
         * <show-property name="docsearch:documentType" display-label-id="document_docType" component-generator="GeneralSelectorGenerator"
         * selectionItems="#{DocumentSearchBean.getDocumentTypes}" converter="ee.webmedia.alfresco.common.propertysheet.converter.QNameConverter" />
         */
        {
            // docsearch:documentType
            QName prop = DocumentSearchModel.Props.DOCUMENT_TYPE;
            ItemConfigVO itemConfig = createItemConfigVO(prop);
            itemConfig.setComponentGenerator("GeneralSelectorGenerator");
            itemConfig.setSelectionItems("#{DocumentSearchBean.getDocumentTypes}");
            itemConfig.setRenderCheckboxAfterLabel(withCheckboxes);
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }
    }

    private void addDocumentCreatedConfigItem(DocumentConfig config) {
        {
            // docsearch:documentCreated
            QName prop = DocumentSearchModel.Props.DOCUMENT_CREATED;
            ItemConfigVO itemConfig = createItemConfigVO(prop);
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            DateGenerator.setupDateFilterItemConfig(itemConfig, prop);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }
    }

    private void addInputConfigItem(DocumentConfig config, boolean isAssociationSearch) {
        /**
         * <show-property name="docsearch:input" display-label-id="document_search_input" component-generator="TextAreaGenerator" styleClass="expand19-200" />
         */
        {
            // docsearch:input
            QName prop = DocumentSearchModel.Props.INPUT;
            ItemConfigVO itemConfig = createItemConfigVO(prop);
            if (isAssociationSearch) {
                itemConfig.setStyleClass("searchAssocOnEnter focus");
            } else {
                itemConfig.setComponentGenerator("TextAreaGenerator");
                itemConfig.setStyleClass("expand19-200 focus");
            }
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }
    }

    @Override
    public DocumentConfig getEventPlanVolumeSearchFilterConfig() {
        DocumentConfig config = getEmptyConfig(null, null);
        WMPropertySheetConfigElement propertySheetConfigElement = config.getPropertySheetConfigElement();
        {
            ItemConfigVO itemConfig = new ItemConfigVO(VolumeModel.Props.TITLE.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("volume_title");
            itemConfig.setComponentGenerator("TextAreaGenerator");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            propertySheetConfigElement.addItem(itemConfig);
        }
        {
            ItemConfigVO itemConfig = new ItemConfigVO(VolumeModel.Props.STATUS.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("volume_status");
            itemConfig.setComponentGenerator("ClassificatorSelectorGenerator");
            itemConfig.setClassificatorName("docListUnitStatus");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            propertySheetConfigElement.addItem(itemConfig);
        }
        {
            ItemConfigVO itemConfig = new ItemConfigVO(VolumeSearchModel.Props.STORE.toPrefixString(namespaceService));
            itemConfig.setDisplayLabelId("volume_location");
            itemConfig.setComponentGenerator("GeneralSelectorGenerator");
            itemConfig.setSelectionItems("#{DialogManager.bean.getStores}");
            itemConfig.setConverter("ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            propertySheetConfigElement.addItem(itemConfig);
        }
        return config;
    }

    @Override
    public DocumentConfig getVolumeSearchFilterConfig(boolean withCheckboxes) {
        DocumentConfig config = getEmptyConfig(null, null);
        WMPropertySheetConfigElement propertySheetConfigElement = config.getPropertySheetConfigElement();
        {
            ItemConfigVO itemConfig = createItemConfigVO(VolumeSearchModel.Props.STORE);
            itemConfig.setComponentGenerator("GeneralSelectorGenerator");
            itemConfig.setSelectionItems("#{DialogManager.bean.getStores}");
            itemConfig.setConverter("ee.webmedia.alfresco.common.propertysheet.converter.NodeRefConverter");
            itemConfig.setValueChangeListener("#{DialogManager.bean.storeValueChanged}");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            propertySheetConfigElement.addItem(itemConfig);
        }

        {
            ItemConfigVO itemConfig = createItemConfigVO(VolumeSearchModel.Props.INPUT);
            itemConfig.setComponentGenerator("TextAreaGenerator");
            itemConfig.setStyleClass("expand19-200 focus");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            propertySheetConfigElement.addItem(itemConfig);
        }

        {
            ItemConfigVO itemConfig = createItemConfigVO(VolumeSearchModel.Props.VOLUME_TYPE);
            itemConfig.setComponentGenerator("EnumSelectorGenerator");
            itemConfig.getCustomAttributes().put(EnumSelectorGenerator.ATTR_ENUM_CLASS, VolumeType.class.getCanonicalName());
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            propertySheetConfigElement.addItem(itemConfig);
        }

        {
            ItemConfigVO itemConfig = createItemConfigVO(VolumeSearchModel.Props.CASE_FILE_TYPE);
            itemConfig.setRenderCheckboxAfterLabel(withCheckboxes);
            itemConfig.setComponentGenerator("GeneralSelectorGenerator");
            itemConfig.setSelectionItems("#{DialogManager.bean.getCaseFileTypes}");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            propertySheetConfigElement.addItem(itemConfig);
        }

        List<FieldDefinition> fields = documentAdminService.getSearchableVolumeFieldDefinitions();
        for (FieldDefinition fieldDefinition : fields) {
            processFieldForSearchView(fieldDefinition);
            processField(config, fieldDefinition, withCheckboxes, false);
        }

        if (!withCheckboxes) {
            {
                ItemConfigVO itemConfig = createItemConfigVO(VolumeReportModel.Props.REPORT_TEMPLATE);
                itemConfig.setComponentGenerator("GeneralSelectorGenerator");
                itemConfig.setSelectionItems("#{DialogManager.bean.getReportTemplates}");
                itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
                propertySheetConfigElement.addItem(itemConfig);
            }
        }

        return config;
    }

    private ItemConfigVO createItemConfigVO(QName prop) {
        ItemConfigVO itemConfig = new ItemConfigVO(prop.toPrefixString(namespaceService));
        itemConfig.setDisplayLabelId(searchLabelIds.get(prop));
        return itemConfig;
    }

    @Override
    public DocumentConfig getReportConfig() {
        DocumentConfig config = getFilterConfig(false);

        {
            // docreport:reportOutputType
            QName prop = DocumentReportModel.Props.REPORT_OUTPUT_TYPE;
            ItemConfigVO itemConfig = createItemConfigVO(prop);
            itemConfig.setComponentGenerator("EnumSelectorGenerator");
            itemConfig.getCustomAttributes().put("enumClass", TemplateReportOutputType.class.getCanonicalName());
            itemConfig.setValueChangeListener("#{DialogManager.bean.reportTypeChanged}");
            itemConfig.setConfigItemType(ConfigItemType.PROPERTY);
            config.getPropertySheetConfigElement().addItem(itemConfig);
        }

        {
            // docreport:reportTemplate
            QName prop = DocumentReportModel.Props.REPORT_TEMPLATE;
            ItemConfigVO itemConfig = createItemConfigVO(prop);
            itemConfig.setComponentGenerator("GeneralSelectorGenerator");
            itemConfig.setSelectionItems("#{DocumentDynamicReportDialog.getReportTemplates}");
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
                processField(config, field, false, false);

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
        private final List<ItemConfigVO> itemsAfterPregeneratedItem = new ArrayList<WMPropertySheetConfigElement.ItemConfigVO>();
        private boolean preGeneratedItemAdded = false;
        private final DocumentConfig config;
        private final boolean forceEditMode;

        public GeneratorResultsImpl(ItemConfigVO pregeneratedItem, DocumentConfig config, boolean forceEditMode) {
            this.pregeneratedItem = pregeneratedItem;
            this.config = config;
            this.forceEditMode = forceEditMode;
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
            return generateAndAddViewModeTextInternal(name, label, config, forceEditMode);
        }

        @Override
        public void addItem(ItemConfigVO item) {
            WMPropertySheetConfigElement propSheet = processItem(item);
            propSheet.addItem(item);
        }

        public WMPropertySheetConfigElement processItem(ItemConfigVO item) {
            WMPropertySheetConfigElement propSheet = config.getPropertySheetConfigElement();
            Assert.isTrue(!propSheet.getItems().containsKey(item.getName()), "PropertySheetItem with name already exists: " + item.getName());
            if (forceEditMode) {
                item.setShowInViewMode(false);
            }
            return propSheet;
        }

        @Override
        public void addStateHolder(String key, PropertySheetStateHolder stateHolder) {
            Assert.notNull(key, "key");
            Map<String, PropertySheetStateHolder> stateHolders = config.getStateHolders();
            Assert.isTrue(!stateHolders.containsKey(key), "Stateholder already exists for " + key);
            stateHolders.put(key, stateHolder);
        }

        @Override
        public void addItemAfterPregeneratedItem(ItemConfigVO item) {
            processItem(item);
            String itemName = item.getName();
            for (ItemConfigVO additionalItem : itemsAfterPregeneratedItem) {
                Assert.isTrue(!additionalItem.getName().equals(itemName), "PropertySheetItem with name already exists: " + itemName);
            }
            itemsAfterPregeneratedItem.add(item);
        }

        @Override
        public boolean hasStateHolder(String key) {
            return config.getStateHolders().containsKey(key);
        }

    }

    private class FieldGroupGeneratorResultsImpl implements FieldGroupGeneratorResults {

        private final DocumentConfig config;
        private final boolean forceEditMode;

        public FieldGroupGeneratorResultsImpl(DocumentConfig config, boolean forceEditMode) {
            this.config = config;
            this.forceEditMode = forceEditMode;
        }

        @Override
        public Pair<Map<String, ItemConfigVO>, Map<String, PropertySheetStateHolder>> generateItems(Field... fields) {
            DocumentConfig tempConfig = getEmptyConfig(null, null);
            for (Field field : fields) {
                processField(tempConfig, field, false, false);
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
            return generateAndAddViewModeTextInternal(name, label, config, forceEditMode);
        }

        @Override
        public void addItem(ItemConfigVO item) {
            WMPropertySheetConfigElement propSheet = config.getPropertySheetConfigElement();
            Assert.isTrue(!propSheet.getItems().containsKey(item.getName()), "PropertySheetItem with name already exists: " + item.getName());
            if (forceEditMode) {
                item.setShowInViewMode(false);
            }
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

    private static ItemConfigVO generateAndAddViewModeTextInternal(String name, String label, DocumentConfig config, boolean forceEditMode) {
        ItemConfigVO viewModeTextItem = new ItemConfigVO(name);
        viewModeTextItem.setConfigItemType(ConfigItemType.PROPERTY);
        viewModeTextItem.setIgnoreIfMissing(false);
        viewModeTextItem.setShowInEditMode(false);
        viewModeTextItem.setDisplayLabel(label);
        WMPropertySheetConfigElement propSheet = config.getPropertySheetConfigElement();
        Assert.isTrue(!propSheet.getItems().containsKey(viewModeTextItem.getName()), "PropertySheetItem with name already exists: " + viewModeTextItem.getName());
        if (!forceEditMode) {
            propSheet.addItem(viewModeTextItem);
        }
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
        boolean forceEditMode = false;
        if (StringUtils.isNotBlank(fieldGroup.getReadonlyFieldsName()) && StringUtils.isNotBlank(fieldGroup.getReadonlyFieldsRule())) {
            config.getPropertySheetConfigElement().addItem(generateFieldGroupReadonlyItem(fieldGroup));
            forceEditMode = true;
        }
        if (fieldGroup.isSystematic()) {
            FieldGroupGenerator fieldGroupGenerator = fieldGroupGenerators.get(fieldGroup.getName());
            if (fieldGroupGenerator != null) {
                fieldGroupGenerator.generateFieldGroup(fieldGroup, new FieldGroupGeneratorResultsImpl(config, forceEditMode));
                addSaveListener(config, fieldGroupGenerator);
                return;
            }
        }
        ChildrenList<Field> fields = fieldGroup.getFields();
        for (Field field : fields) {
            processField(config, field, false, forceEditMode);
        }
    }

    @Override
    public ItemConfigVO generateFieldGroupReadonlyItem(FieldGroup fieldGroup) {
        ItemConfigVO item = new ItemConfigVO(RepoUtil.createTransientProp(fieldGroup.getFields().get(0).getFieldId() + "Group").toString());
        item.setConfigItemType(ConfigItemType.PROPERTY);
        item.setIgnoreIfMissing(false);
        item.setDisplayLabel(fieldGroup.getReadonlyFieldsName());
        item.setShowInEditMode(false);
        item.setComponentGenerator("PatternOutputGenerator");
        item.setPattern(fieldGroup.getReadonlyFieldsRule());
        return item;
    }

    private boolean processField(DocumentConfig config, Field field, boolean renderCheckboxAfterLabel, boolean forceEditMode) {
        return processField(config, field, renderCheckboxAfterLabel, forceEditMode, null);
    }

    private boolean processField(DocumentConfig config, Field field, boolean renderCheckboxAfterLabel, boolean forceEditMode, String additionalStateHolderKey) {
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
        GeneratorResultsImpl generatorResults = new GeneratorResultsImpl(item, config, forceEditMode);
        try {
            generatorByFieldType.generateField(field, generatorResults);
        } catch (Exception e) {
            throw new RuntimeException("Error running generator for field type, field=" + field.toString() + ": " + e.getMessage(), e);
        }
        boolean preGeneratedItemAdded = generatorResults.preGeneratedItemAdded;

        // 2) Run "by id" generator if it exists
        FieldGenerator generatorByOriginalFieldId = originalFieldIdGenerators.get(field.getOriginalFieldId());
        if (generatorByOriginalFieldId != null) {
            generatorResults = new GeneratorResultsImpl(item, config, forceEditMode);
            try {
                if (generatorByOriginalFieldId instanceof BaseSystematicFieldGenerator) {
                    ((BaseSystematicFieldGenerator) generatorByOriginalFieldId).setUseAdditionalStateHolders(additionalStateHolderKey);
                }
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
        if (forceEditMode) {
            item.setShowInViewMode(false);
        }
        propSheet.addItem(item);
        for (ItemConfigVO additionalItem : generatorResults.itemsAfterPregeneratedItem) {
            propSheet.addItem(additionalItem);
        }

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
                item.setOutputTextPropertyValue(true);
                break;
            case CHANGEABLE_IF_WORKING_DOC:
                item.setReadOnlyIf("#{" + DocumentDialogHelperBean.BEAN_NAME + ".notWorkingOrNotEditable}");
                break;
            }
        }

        if (field.isMandatory()) {
            item.setForcedMandatory(Boolean.TRUE);
            item.getCustomAttributes().put(ValidatingModalLayerComponent.ATTR_MANDATORY, Boolean.TRUE.toString());
        }

        return item;
    }

    private void addSaveListener(DocumentConfig config, FieldGenerator fieldGenerator) {
        if (fieldGenerator instanceof SaveListener) {
            SaveListener saveListener = (SaveListener) fieldGenerator;
            addSaveListener(config, saveListener);
        }
    }

    private void addSaveListener(DocumentConfig config, FieldGroupGenerator fieldGroupGenerator) {
        if (fieldGroupGenerator instanceof SaveListener) {
            SaveListener saveListener = (SaveListener) fieldGroupGenerator;
            addSaveListener(config, saveListener);
        }
    }

    private void addSaveListener(DocumentConfig config, SaveListener saveListener) {
        List<String> saveListenerBeanNames = config.getSaveListenerBeanNames();
        String beanName = saveListener.getBeanName();
        if (!saveListenerBeanNames.contains(beanName)) {
            saveListenerBeanNames.add(beanName);
        }
    }

    @Override
    public void setDefaultPropertyValues(Node node, QName[] childAssocTypeQNameHierarchy, boolean forceOverwrite, boolean reallySetDefaultValues, DocumentTypeVersion docVer) {
        Pair<DocumentType, DocumentTypeVersion> documentTypeAndVersion = getDocumentTypeAndVersion(node);
        if (docVer == null) {
            docVer = documentTypeAndVersion.getSecond();
        } else {
            Pair<String, Integer> docTypeIdAndVersionNr = getDocTypeIdAndVersionNr(node);
            Assert.isTrue(ObjectUtils.equals(docVer.getParent().getId(), docTypeIdAndVersionNr.getFirst()));
            Assert.isTrue(ObjectUtils.equals(docVer.getVersionNr(), docTypeIdAndVersionNr.getSecond()));
        }
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = getPropertyDefinitions(getPropDefCacheKey(getDynamicTypeClass(node), docVer));
        setDefaultPropertyValues(node, childAssocTypeQNameHierarchy, forceOverwrite, reallySetDefaultValues, propertyDefinitions, documentTypeAndVersion != null
                ? documentTypeAndVersion.getFirst() : null);
    }

    @Override
    public void setDefaultPropertyValues(Node node, QName[] requiredHierarchy, boolean forceOverwrite, boolean reallySetDefaultValues, List<Field> fields) {
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = createPropertyDefinitions(fields);
        DocumentType documentType = getDocumentTypeAndVersion(node).getFirst();
        setDefaultPropertyValues(node, requiredHierarchy, forceOverwrite, reallySetDefaultValues, propertyDefinitions, documentType);
    }

    private void setDefaultPropertyValues(Node node, QName[] requiredHierarchy, boolean forceOverwrite, boolean reallySetDefaultValues,
            Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions, DocumentType documentType) {
        if (requiredHierarchy == null) {
            requiredHierarchy = new QName[] {};
        }
        FieldGroup accessRestrictionGroup = null;
        FieldGroup documentLocationGroup = null;
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
                    if (SystematicFieldGroupNames.ACCESS_RESTRICTION.equals(group.getName())) {
                        accessRestrictionGroup = group;
                    }
                    if (SystematicFieldGroupNames.DOCUMENT_LOCATION.equals(group.getName())) {
                        documentLocationGroup = group;
                    }
                }
            }

            List<Node> childNodes = collectChildNodes(node, hierarchy, i);
            for (Node childNode : childNodes) {
                setDefaultPropertyValue(childNode, propDef, forceOverwrite, reallySetDefaultValues, field, propertyDefinitions, documentType);
            }
        }
        if (accessRestrictionGroup != null && reallySetDefaultValues) {
            // cannot calculate this in setSpecialDependentValues (called at the end of setDefaultPropertyValue),
            // because multiple other default values must be set before the calculation
            AccessRestrictionGenerator.calculateAccessRestrictionValues(accessRestrictionGroup, node.getProperties());
            if (documentLocationGroup != null) {
                AccessRestrictionGenerator.setAccessRestrictionFromSeries(documentLocationGroup, accessRestrictionGroup, node.getProperties());
            }
        }
    }

    private List<Node> collectChildNodes(Node node, QName[] hierarchy, int i) {
        if (i >= hierarchy.length) {
            return Collections.singletonList(node);
        }
        List<Node> results = new ArrayList<Node>();
        List<Node> childNodes = node.getAllChildAssociations(hierarchy[i]);
        if (childNodes != null) {
            for (Node childNode : childNodes) {
                results.addAll(collectChildNodes(childNode, hierarchy, i + 1));
            }
        }
        return results;
    }

    private List<String> getDefaultTableValue(QName fieldType) {
        List<ClassificatorValue> classValues = BeanHelper.getClassificatorService().getAllClassificatorValues(fieldType.getLocalName());
        Collections.sort(classValues, new Comparator<ClassificatorValue>() {
            @Override
            public int compare(ClassificatorValue o1, ClassificatorValue o2) {
                return o1.getValueData().compareTo(o2.getValueData());
            }
        });
        List<String> defaultTableValues = new ArrayList<String>();
        for (ClassificatorValue classValue : classValues) {
            String valueData = classValue.getValueData();
            if (!StringUtils.isEmpty(valueData) && StringUtils.isNumeric(valueData)) {
                defaultTableValues.add(classValue.getValueName());
            }
        }
        return defaultTableValues;
    }

    private void setDefaultPropertyValue(Node node, DynamicPropertyDefinition propDef, boolean forceOverwrite, boolean reallySetDefaultValues, Field field,
            Map<String, Pair<DynamicPropertyDefinition, Field>> allFieldsAndPropDefs, DocumentType documentType) {
        Serializable value = (Serializable) node.getProperties().get(field.getQName());
        if (!(value == null || (value instanceof String && StringUtils.isBlank((String) value))) && !forceOverwrite) {
            return;
        }

        Serializable defaultValue = null;
        if (reallySetDefaultValues) {
            if (isPropField(DocumentDynamicModel.Props.EXPENSE_TYPE_CHOICE, field)) {
                defaultValue = (Serializable) getDefaultTableValue(DocumentDynamicModel.Props.EXPENSE_TYPE_CHOICE);
            }
            if (documentType != null) {
                if (isPropField(DocumentCommonModel.Props.FUNCTION, field)) {
                    defaultValue = documentType.getFunction();
                } else if (isPropField(DocumentCommonModel.Props.SERIES, field)) {
                    defaultValue = documentType.getSeries();
                } else if (isPropField(DocumentCommonModel.Props.VOLUME, field)) {
                    defaultValue = documentType.getVolume();
                } else if (isPropField(DocumentCommonModel.Props.CASE, field)) {
                    defaultValue = documentType.getCase();
                }
            }
        }
        if (defaultValue == null && reallySetDefaultValues) {
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
                        List<ClassificatorValue> classificatorValues = classificatorService.getActiveClassificatorValues(classificatorService.getClassificatorByName(field
                                .getClassificator()));
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
                        setSpecialDependentValues(node, fieldAndPropDef.getSecond(), entry.getValue());
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
            setSpecialDependentValues(node, field, defaultValue);
        }
    }

    private boolean isPropField(QName propQName, Field field) {
        return propQName.getLocalName().equals(field.getOriginalFieldId());
    }

    // Same rules as in ErrandGenerator#applicantOrgStructUnitChanged
    private void setSpecialDependentValues(Node node, Field field, Serializable value) {
        if (field != null
                && DocumentDynamicModel.Props.APPLICANT_ORG_STRUCT_UNIT.getLocalName().equals(field.getOriginalFieldId())
                && field.getParent() instanceof FieldGroup
                && ((FieldGroup) field.getParent()).isSystematic()) {

            @SuppressWarnings("unchecked")
            String displayUnit = UserUtil.getDisplayUnit((List<String>) value);

            // If applicantOrgStructUnit is in a systematic group, then that group may also contain costManager and costCenter
            Map<String, Field> fieldsByOriginalId = ((FieldGroup) field.getParent()).getFieldsByOriginalId();
            setValueFromClassificatorValueName(node, fieldsByOriginalId, DocumentSpecificModel.Props.COST_MANAGER.getLocalName(), displayUnit);
            setValueFromClassificatorValueName(node, fieldsByOriginalId, DocumentDynamicModel.Props.COST_CENTER.getLocalName(), displayUnit);
        }
    }

    private void setValueFromClassificatorValueName(Node node, Map<String, Field> fieldsByOriginalId, String originalFieldId, String classificatorValueDescription) {
        Field field = fieldsByOriginalId.get(originalFieldId);
        if (field == null) {
            return; // costManager and costCenter could be removed from group
        }
        String classificatorName = field.getClassificator();
        if (StringUtils.isBlank(classificatorName)) {
            return;
        }
        List<ClassificatorValue> classificatorValues = classificatorService.getActiveClassificatorValues(classificatorService.getClassificatorByName(classificatorName));
        for (ClassificatorValue classificatorValue : classificatorValues) {
            if (StringUtils.equals(classificatorValue.getClassificatorDescription(), classificatorValueDescription)) {
                node.getProperties().put(field.getQName().toString(), classificatorValue.getValueName());
                break;
            }
        }
    }

    @Override
    public void setUserContactProps(Map<QName, Serializable> props, String userName, String fieldId, Class<? extends DynamicType> typeClass) {
        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs = getPropertyDefinitions(getPropDefCacheKey(typeClass, props));
        Pair<DynamicPropertyDefinition, Field> propDefAndField = propDefs.get(fieldId);
        setUserContactProps(props, userName, propDefAndField.getFirst(), propDefAndField.getSecond());
    }

    @Override
    public void setUserContactProps(Map<QName, Serializable> props, String userName, String fieldId) {
        setUserContactProps(props, userName, fieldId, DocumentType.class);
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
    public PropertyDefinition getStaticOrDynamicPropertyDefinition(QName propName) {
        if (propName == null) {
            return null;
        }

        PropertyDefinition property = dictionaryService.getProperty(propName);
        if (property == null && DocumentDynamicModel.URI.equals(propName.getNamespaceURI())) {
            property = getPropertyDefinitionById(propName.getLocalName());
        }

        return property;
    }

    @Override
    public DynamicPropertyDefinition getPropertyDefinition(Node documentDynamicNode, QName property) {
        if (!DocumentDynamicModel.URI.equals(property.getNamespaceURI())) {
            return null;
        }
        // XXX a little hack for the docdyn:status property on volumes
        // the volume type is not dynamic but the status property has to be
        if (documentDynamicNode.getType().equals(VolumeModel.Types.VOLUME) && property.equals(VolumeModel.Props.STATUS)) {
            FieldDefinition field = documentAdminService.getFieldDefinition(property.getLocalName());
            return new DynamicPropertyDefinitionImpl(field, false, null);
        }
        if (isFilterType(documentDynamicNode.getType())) {
            if (hiddenFieldDependencies.containsKey(property.getLocalName())) {
                String originalFieldId = hiddenFieldDependencies.get(property.getLocalName());
                DynamicPropertyDefinition originalPropDef = getPropDefForSearch(originalFieldId, true);
                DynamicPropertyDefinition propDef = createPropertyDefinitionForHiddenField(property.getLocalName(), originalPropDef);
                return propDef;
            }
            return getPropDefForSearch(property.getLocalName(), true);
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

    protected boolean isFilterType(QName type) {
        return DocumentSearchModel.Types.FILTER.equals(type) || DocumentReportModel.Types.FILTER.equals(type) || VolumeSearchModel.Types.FILTER.equals(type)
                || VolumeReportModel.Types.FILTER.equals(type) || DocumentSearchModel.Types.OBJECT_FILTER.equals(type);
    }

    private DynamicPropertyDefinition getPropDefForSearch(String fieldId, boolean processForSearch) {
        FieldDefinition field;
        if (fieldId.contains("_")) {
            field = documentAdminService.getFieldDefinition(fieldId.substring(0, fieldId.indexOf("_")));
            if (field != null) {
                field.setFieldId(fieldId);
                if (fieldId.endsWith(WMUIProperty.AFTER_LABEL_BOOLEAN)) {
                    field.setOriginalFieldId(fieldId);
                    field.setFieldTypeEnum(FieldType.CHECKBOX);
                    field.setMandatory(false);
                } else if (fieldId.endsWith(DateGenerator.PICKER_PREFIX)) {
                    field.setOriginalFieldId(fieldId);
                    field.setFieldTypeEnum(FieldType.COMBOBOX);
                    field.setMandatory(false);
                }
            }
        } else {
            field = documentAdminService.getFieldDefinition(fieldId);
        }
        if (field == null) {
            return null;
        }
        if (processForSearch) {
            processFieldForSearchView(field);
            return new DynamicPropertyDefinitionImpl(field, isFieldForcedMultipleInSearch(field), null);
        }

        return new DynamicPropertyDefinitionImpl(field, null, null);
    }

    // ConcurrentHashMap does not allow null values, so we objects from this special class
    public static class NullDynamicPropertyDefinition implements DynamicPropertyDefinition {

        @Override
        public ModelDefinition getModel() {
            return null;
        }

        @Override
        public QName getName() {
            return null;
        }

        @Override
        public String getTitle() {
            return null;
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
            return null;
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
            return false;
        }

        @Override
        public boolean isMandatory() {
            return false;
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
            return false;
        }

        @Override
        public boolean isStoredInIndex() {
            return false;
        }

        @Override
        public IndexTokenisationMode getIndexTokenisationMode() {
            return null;
        }

        @Override
        public boolean isIndexedAtomically() {
            return false;
        }

        @Override
        public List<ConstraintDefinition> getConstraints() {
            return null;
        }

        @Override
        public QName[] getChildAssocTypeQNameHierarchy() {
            return null;
        }

        @Override
        public Boolean getMultiValuedOverride() {
            return null;
        }

        @Override
        public QName getDataTypeQName() {
            return null;
        }

        @Override
        public FieldType getFieldType() {
            return null;
        }

    }

    @Override
    public DynamicPropertyDefinition getPropertyDefinitionById(String fieldId) {
        if (fieldId == null) {
            return null;
        }

        DynamicPropertyDefinition propertyDefinition = propertyDefinitionForSearchCache.get(fieldId);
        if (propertyDefinition instanceof NullDynamicPropertyDefinition) {
            return null;
        }
        if (propertyDefinition != null) {
            return propertyDefinition;
        }

        propertyDefinition = getPropDefForSearch(fieldId, false);
        if (propertyDefinition == null) {
            String fieldIdWithoutSuffix = fieldId;
            String fieldIdSuffix = "";
            if (fieldId.contains("_")) {
                fieldIdWithoutSuffix = fieldId.substring(0, fieldId.indexOf("_"));
                fieldIdSuffix = fieldId.substring(fieldId.indexOf("_"));
            }
            if (hiddenFieldDependencies.containsKey(fieldIdWithoutSuffix)) {
                String originalFieldId = hiddenFieldDependencies.get(fieldIdWithoutSuffix) + fieldIdSuffix;
                DynamicPropertyDefinition originalPropDef = getPropDefForSearch(originalFieldId, false);
                if (originalPropDef == null) {
                    LOG.warn("PropertyDefinition docdyn:" + fieldId + " not found (hidden field, whose originalFieldId is " + originalFieldId + ")");
                    propertyDefinitionForSearchCache.put(fieldId, new NullDynamicPropertyDefinition());
                    return null;
                }
                propertyDefinition = createPropertyDefinitionForHiddenField(fieldId, originalPropDef);
                propertyDefinitionForSearchCache.put(fieldId, propertyDefinition);
                return propertyDefinition;
            }

            LOG.warn("PropertyDefinition docdyn:" + fieldId + " not found");
            propertyDefinitionForSearchCache.put(fieldId, new NullDynamicPropertyDefinition());
            return null;
        }
        propertyDefinitionForSearchCache.put(fieldId, propertyDefinition);
        return propertyDefinition;
    }

    private static final List<String> comboboxFieldsNotMultiple = Arrays.asList("function", "series", "volume");

    private Boolean isFieldForcedMultipleInSearch(FieldDefinition field) {
        if (field.getFieldTypeEnum().equals(FieldType.COMBOBOX) && !comboboxFieldsNotMultiple.contains(field.getOriginalFieldId())
                && !field.getFieldId().endsWith(DateGenerator.PICKER_PREFIX)) {
            return true;
        }

        if (DocumentDynamicModel.Props.FIRST_KEYWORD_LEVEL.getLocalName().equals(field.getOriginalFieldId())
                || DocumentDynamicModel.Props.SECOND_KEYWORD_LEVEL.getLocalName().equals(field.getOriginalFieldId())
                || DocumentDynamicModel.Props.THESAURUS.getLocalName().equals(field.getOriginalFieldId())) {
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
        if (getDynamicTypeClass(node) == null && !dictionaryService.isSubClass(type, DocumentCommonModel.Types.METADATA_CONTAINER)) {
            return null;
        }
        PropDefCacheKey propDefCacheKey = getPropDefCacheKey(node);
        return getPropertyDefinitions(propDefCacheKey);
    }

    @Override
    public Map<String, Pair<DynamicPropertyDefinition, Field>> getPropertyDefinitions(PropDefCacheKey cacheKey) {
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = propertyDefinitionCache.get(cacheKey);
        if (propertyDefinitions == null) {
            Pair<? extends DynamicType, DocumentTypeVersion> documentTypeAndVersion = null;
            String dynamicTypeId = cacheKey.getDynamicTypeId();
            Integer version = cacheKey.getVersion();
            if (dynamicTypeId == null || version == null) {
                return null;
            }
            if (cacheKey.isDocumentType()) {
                documentTypeAndVersion = documentAdminService.getDocumentTypeAndVersion(dynamicTypeId, version);
            } else if (cacheKey.isCaseFileType()) {
                documentTypeAndVersion = documentAdminService.getCaseFileTypeAndVersion(dynamicTypeId, version);
            }
            if (documentTypeAndVersion == null) {
                return null;
            }
            DocumentTypeVersion docVersion = documentTypeAndVersion.getSecond();
            docVersion.resetParent();
            propertyDefinitions = createPropertyDefinitions(docVersion.getFieldsDeeply());
            if (LOG.isDebugEnabled()) {
                StringBuilder s = new StringBuilder();
                s.append("Created propertyDefinitions for cacheKey=").append(cacheKey).append(" - ");
                s.append("[").append(propertyDefinitions.size()).append("]");
                for (Entry<String, Pair<DynamicPropertyDefinition, Field>> entry : propertyDefinitions.entrySet()) {
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

    private Map<String, Pair<DynamicPropertyDefinition, Field>> createPropertyDefinitions(List<Field> fields) {
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = new LinkedHashMap<String, Pair<DynamicPropertyDefinition, Field>>();
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

    @Override
    public void removeFrompPopertyDefinitionForSearchCache(String fieldId) {
        propertyDefinitionForSearchCache.remove(fieldId);
    }

    @Override
    public void removeFromChildAssocTypeQNameTreeCache(Pair<String, Integer> typeAndVersion) {
        childAssocTypeQNameTreeCache.remove(typeAndVersion);
    }

    @Override
    public void removeFromPropertyDefinitionCache(PropDefCacheKey key) {
        propertyDefinitionCache.remove(key);
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

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private EInvoiceService getEInvoiceServiceService() {
        if (_einvoiceService == null) {
            _einvoiceService = (EInvoiceService) beanFactory.getBean(EInvoiceService.BEAN_NAME);
        }
        return _einvoiceService;
    }

    @Override
    public boolean isRegDateFilterInAssociationsSearch() {
        return regDateFilterInAssociationsSearch;
    }

    public void setRegDateFilterInAssociationsSearch(boolean regDateFilterInAssociationsSearch) {
        this.regDateFilterInAssociationsSearch = regDateFilterInAssociationsSearch;
    }

    public void setPropertyDefinitionCache(SimpleCache<PropDefCacheKey, Map<String /* fieldId */, Pair<DynamicPropertyDefinition, Field>>> propertyDefinitionCache) {
        this.propertyDefinitionCache = propertyDefinitionCache;
    }

    public void setChildAssocTypeQNameTreeCache(SimpleCache<Pair<String /* documentTypeId */, Integer /* documentTypeVersionNr */>, TreeNode<QName>> childAssocTypeQNameTreeCache) {
        this.childAssocTypeQNameTreeCache = childAssocTypeQNameTreeCache;
    }

    public void setPropertyDefinitionForSearchCache(SimpleCache<String /* fieldId */, DynamicPropertyDefinition> propertyDefinitionForSearchCache) {
        this.propertyDefinitionForSearchCache = propertyDefinitionForSearchCache;
    }

    // END: setters
}