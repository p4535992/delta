package ee.webmedia.alfresco.docconfig.generator.systematic;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader;
import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.PropsBuilder;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docconfig.generator.systematic.UserContactTableGenerator.UserContactTableState;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public class ErrandGenerator extends BaseSystematicGroupGenerator implements SaveListener, BeanNameAware {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ErrandGenerator.class);

    private NamespaceService namespaceService;
    private ParametersService parametersService;
    private String beanName;

    // UserContactRelatedGroupGenerator
    private final List<String> applicantRelatedFieldIds = Arrays.asList(
            "applicantName", // NOT removable from systematic group
            "applicantId",
            "applicantServiceRank",
            "applicantJobTitle",
            "applicantOrgStructUnit",
            "applicantWorkAddress",
            "applicantEmail",
            "applicantPhone");

    // Table, UserContactTableGenerator
    private final List<String> substituteTableFieldIds = Arrays.asList(
            "substituteName", // NOT removable from systematic group
            "substituteJobTitle",
            "substitutionBeginDate", // NOT removable from systematic group
            "substitutionEndDate"); // NOT removable from systematic group

    // Table -- only on abroad
    private final List<String> dailyAllowanceTableFieldIds = Arrays.asList(
            "dailyAllowanceDays", // NOT removable from systematic group
            "dailyAllowanceRate", // NOT removable from systematic group
            "dailyAllowanceSum"); // NOT removable from systematic group

    // Table
    private final List<String> expenseTableFieldIds = Arrays.asList(
            "expenseType", // NOT removable from systematic group
            "expectedExpenseSum"); // NOT removable from systematic group; DOUBLE

    // Inline
    private final List<String> eventDateInlineFieldIds = Arrays.asList(
            "eventBeginDate", // NOT removable from systematic group
            "eventEndDate"); // NOT removable from systematic group

    // Inline
    private final List<String> errandDateInlineFieldIds = Arrays.asList(
            "errandBeginDate", // NOT removable from systematic group
            "errandEndDate"); // NOT removable from systematic group

    // Inline
    private final List<String> advancePaymentInlineFieldIds = Arrays.asList(
            "advancePaymentSum", // NOT removable from systematic group; DOUBLE
            "advancePaymentDesc"); // NOT removable from systematic group

    @Override
    protected String[] getSystematicGroupNames() {
        Set<String> errandCommonFieldIds = new HashSet<String>();
        errandCommonFieldIds.add("eventOrganizer");
        errandCommonFieldIds.addAll(eventDateInlineFieldIds);
        errandCommonFieldIds.addAll(errandDateInlineFieldIds);
        errandCommonFieldIds.add("city");
        errandCommonFieldIds.add("travelPurpose");
        errandCommonFieldIds.addAll(expenseTableFieldIds);
        errandCommonFieldIds.add("expensesTotalSum"); // NOT removable from systematic group; DOUBLE
        errandCommonFieldIds.add("personalCarIsUsed");
        errandCommonFieldIds.add("carMakeOwnerRegNr");
        errandCommonFieldIds.add("kmBackAndForth"); // DOUBLE
        errandCommonFieldIds.add("financingSource");
        errandCommonFieldIds.add("errandSummaryProjectCode");
        errandCommonFieldIds.addAll(advancePaymentInlineFieldIds);
        errandCommonFieldIds.addAll(substituteTableFieldIds);
        errandCommonFieldIds.add("errandComment");

        Set<String> errandDomesticFieldIds = new HashSet<String>(errandCommonFieldIds);
        errandDomesticFieldIds.add("eventName");
        errandDomesticFieldIds.add("county");

        Set<String> errandAbroadFieldIds = new HashSet<String>(errandCommonFieldIds);
        errandAbroadFieldIds.add("country");
        errandAbroadFieldIds.addAll(dailyAllowanceTableFieldIds);
        errandAbroadFieldIds.add("dailyAllowanceTotalSum"); // NOT removable from systematic group
        errandAbroadFieldIds.add("reportDueDate");

        Map<QName[], Set<String>> applicantDomesticAdditionalHierarchy = new HashMap<QName[], Set<String>>();
        applicantDomesticAdditionalHierarchy.put(new QName[] { DocumentChildModel.Assocs.ERRAND_DOMESTIC }, errandDomesticFieldIds);
        documentConfigService.registerChildAssocTypeQNameHierarchy(SystematicFieldGroupNames.ERRAND_DOMESTIC_APPLICANT, DocumentChildModel.Assocs.APPLICANT_DOMESTIC,
                applicantDomesticAdditionalHierarchy);

        Map<QName[], Set<String>> applicantAbroadAdditionalHierarchy = new HashMap<QName[], Set<String>>();
        applicantAbroadAdditionalHierarchy.put(new QName[] { DocumentChildModel.Assocs.ERRAND_ABROAD }, errandAbroadFieldIds);
        documentConfigService.registerChildAssocTypeQNameHierarchy(SystematicFieldGroupNames.ERRAND_ABROAD_APPLICANT, DocumentChildModel.Assocs.APPLICANT_ABROAD,
                applicantAbroadAdditionalHierarchy);

        Set<String> multiValueOverrideFieldOriginalIds = new HashSet<String>();
        multiValueOverrideFieldOriginalIds.addAll(substituteTableFieldIds);
        multiValueOverrideFieldOriginalIds.addAll(dailyAllowanceTableFieldIds);
        multiValueOverrideFieldOriginalIds.addAll(expenseTableFieldIds);
        documentConfigService.registerMultiValuedOverrideBySystematicGroupName(SystematicFieldGroupNames.ERRAND_DOMESTIC_APPLICANT, multiValueOverrideFieldOriginalIds);
        documentConfigService.registerMultiValuedOverrideBySystematicGroupName(SystematicFieldGroupNames.ERRAND_ABROAD_APPLICANT, multiValueOverrideFieldOriginalIds);

        return new String[] { SystematicFieldGroupNames.ERRAND_DOMESTIC_APPLICANT, SystematicFieldGroupNames.ERRAND_ABROAD_APPLICANT };
    }

    @Override
    public void generateFieldGroup(FieldGroup group, FieldGroupGeneratorResults generatorResults) {

        Map<String, ItemConfigVO> items = new LinkedHashMap<String, ItemConfigVO>();
        Map<String, PropertySheetStateHolder> stateHolders = new HashMap<String, PropertySheetStateHolder>();

        Map<String, Field> fieldsByOriginalId = group.getFieldsByOriginalId();
        Set<List<QName>> addedSubPropSheets = new HashSet<List<QName>>();
        List<Field> fields = new ArrayList<Field>(group.getFields());
        // used to keep track of fields that are already generated as part of some related fields group (such fields are removed from this list)
        List<Field> notProcessedFields = new ArrayList<Field>(group.getFields());

        generatorResults.addStateHolder("errandGroup", new ErrandState(geDailyAllowanceSumParamValue()));

        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (isProcessed(field, notProcessedFields)) {
                continue;
            }
            DynamicPropertyDefinition propDef = documentConfigService.createPropertyDefinition(field);
            QName[] hierarchy = propDef.getChildAssocTypeQNameHierarchy();
            Assert.isTrue(hierarchy != null && hierarchy.length >= 1);
            List<QName> hierarchyList = Arrays.asList(hierarchy);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Field " + field.getFieldId() + " multiValuedOverride=" + propDef.getMultiValuedOverride() + " hierarchy="
                        + WmNode.toString(hierarchyList, namespaceService));
            }
            if (!addedSubPropSheets.contains(hierarchyList)) {
                String subPropertySheetId = convertHierarchyToString(hierarchy);

                ItemConfigVO subPropSheetItem = new ItemConfigVO(RepoUtil.createTransientProp("subPropSheet_" + subPropertySheetId).toPrefixString(namespaceService));
                subPropSheetItem.setConfigItemType(ConfigItemType.SUB_PROPERTY_SHEET);
                subPropSheetItem.setSubPropertySheetId(subPropertySheetId);
                subPropSheetItem.setAssocBrand("children");
                subPropSheetItem.setAssocName(hierarchy[hierarchy.length - 1].toPrefixString(namespaceService));
                subPropSheetItem.setActionsGroupId("subPropSheet_" + subPropertySheetId);
                subPropSheetItem.setTitleLabelId("subPropSheet_" + subPropertySheetId);
                if (hierarchy.length > 1) {
                    setBelongsToSubPropertySheet(Collections.singletonList(subPropSheetItem), (QName[]) ArrayUtils.remove(hierarchy, hierarchy.length - 1));
                }
                items.put(subPropSheetItem.getName(), subPropSheetItem);

                addedSubPropSheets.add(hierarchyList);
            }

            // If field is related to a group of fields, then process the whole group together
            Pair<Field, List<Field>> relatedFields2 = collectAndRemoveFieldsInOriginalOrderToFakeGroup(notProcessedFields, field, fieldsByOriginalId);
            List<Field> relatedFields = relatedFields2 == null ? null : relatedFields2.getSecond();
            if (relatedFields != null) {
                generateFields(generatorResults, items, stateHolders, hierarchy, namespaceService, relatedFields.toArray(new Field[relatedFields.size()]));
                continue;
            }
            relatedFields = collectAndRemoveFieldsInOriginalOrder(notProcessedFields, field, dailyAllowanceTableFieldIds);
            if (relatedFields != null) {
                List<String> columnStyleClasses = Arrays.asList("dailyAllowanceDaysField", "dailyAllowanceRateField", "dailyAllowanceSumField");
                setDailyAllowanceSumParameter(relatedFields);
                ItemConfigVO item = generateTable(generatorResults, items, hierarchy, relatedFields, field, "Päevaraha", "add", columnStyleClasses);
                item.setStyleClass("add-expense");
                continue;
            }
            relatedFields = collectAndRemoveFieldsInOriginalOrder(notProcessedFields, field, expenseTableFieldIds);
            if (relatedFields != null) {
                List<String> columnStyleClasses = Arrays.asList("", "expectedExpenseSumField");
                ItemConfigVO item = generateTable(generatorResults, items, hierarchy, relatedFields, field, "Kulud", "add", columnStyleClasses);
                item.setStyleClass("add-expense");
                continue;
            }
            relatedFields = collectAndRemoveFieldsInSpecifiedOrder(notProcessedFields, field, eventDateInlineFieldIds);
            if (relatedFields != null) {
                generateInline(generatorResults, items, hierarchy, relatedFields, field, "Ürituse toimumise aeg", "document_eventDates_templateText");
                continue;
            }
            relatedFields = collectAndRemoveFieldsInSpecifiedOrder(notProcessedFields, field, errandDateInlineFieldIds);
            if (relatedFields != null) {
                generateInline(generatorResults, items, hierarchy, relatedFields, field, "Lähetus", "document_eventDates_templateText");
                continue;
            }
            relatedFields = collectAndRemoveFieldsInSpecifiedOrder(notProcessedFields, field, advancePaymentInlineFieldIds);
            if (relatedFields != null) {
                generateInline(generatorResults, items, hierarchy, relatedFields, field, "Soovin ettemaksu", "document_errand_advancePayment_templateText-edit");
                continue;
            }

            // If field is not related to a group of fields, then process it separately
            generateFields(generatorResults, items, stateHolders, hierarchy, namespaceService, field);
        }

        for (ItemConfigVO item : items.values()) {
            generatorResults.addItem(item);
        }
        for (Entry<String, PropertySheetStateHolder> entry : stateHolders.entrySet()) {
            generatorResults.addStateHolder(entry.getKey(), entry.getValue());
        }
    }

    private Double geDailyAllowanceSumParamValue() {
        return parametersService.getDoubleParameter(Parameters.ERRAND_ORDER_ABROAD_DAILY_ALLOWANCE_SUM);
    }

    private void setDailyAllowanceSumParameter(List<Field> relatedFields) {
        for (Field relatedField : relatedFields) {
            if (DocumentSpecificModel.Props.DAILY_ALLOWANCE_SUM.getLocalName().equals(relatedField.getOriginalFieldId())) {
                relatedField.setDatafieldParamName(Parameters.ERRAND_ORDER_ABROAD_DAILY_ALLOWANCE_SUM.getParameterName());
                break;
            }
        }
    }

    private boolean isProcessed(Field field, List<Field> notProcessedFields) {
        for (Field notProcessedField : notProcessedFields) {
            if (notProcessedField.getOriginalFieldId().equals(field.getOriginalFieldId())) {
                return false;
            }
        }
        return true;
    }

    private ItemConfigVO generateTable(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, QName[] hierarchy, List<Field> relatedFields,
            Field primaryField, String displayLabel, String addLabelId, List<String> columnStyleClasses) {
        Pair<ItemConfigVO, String> result = generateBasePropsItem(generatorResults, items, hierarchy, relatedFields, primaryField, displayLabel, columnStyleClasses);

        ItemConfigVO item = result.getFirst();
        item.setComponentGenerator("MultiValueEditorGenerator");
        item.setAddLabelId(addLabelId);
        item.setShowInViewMode(false);
        item.setPropsGeneration(result.getSecond());

        String stateHolderKey = primaryField.getFieldId();

        // And generate a separate view mode component
        String viewModePropName = RepoUtil.createTransientProp(primaryField.getFieldId() + "Label").toString();
        ItemConfigVO viewModeItem = generatorResults.generateAndAddViewModeText(viewModePropName, displayLabel);
        viewModeItem.setComponentGenerator("UnescapedOutputTextGenerator");

        generatorResults.addStateHolder(stateHolderKey, new UserContactTableState(null, null, null, null, null, null));

        setBelongsToSubPropertySheet(Collections.singletonList(item), hierarchy);
        items.put(item.getName(), item);
        return item;
    }

    private ItemConfigVO generateInline(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, QName[] hierarchy, List<Field> relatedFields,
            Field primaryField, String displayLabel, String textId) {

        Pair<ItemConfigVO, String> result = generateBasePropsItem(generatorResults, items, hierarchy, relatedFields, primaryField, displayLabel);

        ItemConfigVO item = result.getFirst();
        item.setComponentGenerator("InlinePropertyGroupGenerator");
        item.setTextId(textId);
        item.setProps(result.getSecond());

        return item;
    }

    private Pair<ItemConfigVO, String> generateBasePropsItem(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, QName[] hierarchy,
            List<Field> relatedFields, Field primaryField, String displayLabel) {
        return generateBasePropsItem(generatorResults, items, hierarchy, relatedFields, primaryField, displayLabel, null);
    }

    private Pair<ItemConfigVO, String> generateBasePropsItem(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, QName[] hierarchy,
            List<Field> relatedFields, Field primaryField, String displayLabel, List<String> columnStyleClasses) {
        List<String> props = new ArrayList<String>();
        Pair<Map<String, ItemConfigVO>, Map<String, PropertySheetStateHolder>> columnItemsAndStateHolders = generatorResults.generateItems(relatedFields
                .toArray(new Field[relatedFields.size()]));
        Map<String, ItemConfigVO> columnItems = columnItemsAndStateHolders.getFirst();
        int columnCounter = 0;
        for (ItemConfigVO columnItem : columnItems.values()) {

            for (Field field : relatedFields) {
                if (field.getFieldId().equals(QName.resolveToQName(namespaceService, columnItem.getName()).getLocalName())) {
                    String styleClass = StringUtils.trimToEmpty(columnItem.getCustomAttributes().get("styleClass"));
                    if (field.getFieldTypeEnum() == FieldType.LONG) {
                        styleClass = StringUtils.replace(styleClass, "medium", "tiny") + " center";
                    } else if (field.getFieldTypeEnum() == FieldType.DOUBLE) {
                        styleClass = StringUtils.replace(styleClass, "medium", "small") + " center";
                    } else if (field.getFieldTypeEnum() == FieldType.TEXT_FIELD) {
                        styleClass += " medium";
                    }
                    if (columnStyleClasses != null && columnStyleClasses.size() > columnCounter) {
                        styleClass += " " + columnStyleClasses.get(columnCounter);
                    }
                    columnItem.setStyleClass(styleClass);
                }
            }
            String prop = columnItem.toPropString(PropsBuilder.DEFAULT_OPTIONS_SEPARATOR);
            Assert.isTrue(!StringUtils.contains(prop, CombinedPropReader.AttributeNames.DEFAULT_PROPERTIES_SEPARATOR));
            props.add(prop);
            columnCounter++;
        }
        String propsString = StringUtils.join(props, CombinedPropReader.AttributeNames.DEFAULT_PROPERTIES_SEPARATOR);

        ItemConfigVO item = generatorResults.generateItemBase(primaryField);
        item.setName(RepoUtil.createTransientProp(primaryField.getFieldId()).toString());
        item.setDisplayLabel(displayLabel);
        item.setOptionsSeparator(PropsBuilder.DEFAULT_OPTIONS_SEPARATOR);
        setBelongsToSubPropertySheet(Collections.singletonList(item), hierarchy);
        items.put(item.getName(), item);
        return Pair.newInstance(item, propsString);
    }

    @Override
    public Pair<Field, List<Field>> collectAndRemoveFieldsInOriginalOrderToFakeGroup(List<Field> modifiableFieldsList, Field field, Map<String, Field> fieldsByOriginalId) {
        List<Field> relatedFields = null;
        Field primaryFakeField = null;
        String readonlyFieldsName = null;
        relatedFields = collectAndRemoveFieldsInOriginalOrder(modifiableFieldsList, field, applicantRelatedFieldIds);
        readonlyFieldsName = fieldsByOriginalId.get("applicantName").getName();
        if (relatedFields == null) {
            relatedFields = collectAndRemoveFieldsInOriginalOrder(modifiableFieldsList, field, substituteTableFieldIds);
            readonlyFieldsName = "Määrata asendajaks";
        }
        if (relatedFields == null) {
            return null;
        }
        FieldGroup fakeGroup = new FieldGroup(field.getParent().getParent());
        fakeGroup.setSystematic(true);
        fakeGroup.setReadonlyFieldsName(readonlyFieldsName);
        List<Field> fakeFields = new ArrayList<Field>();
        for (Field relatedField : relatedFields) {
            Field fakeField = relatedField.clone();
            fakeField.getNode().getProperties().remove(RepoUtil.createTransientProp("cloneOfNodeRef").toString());
            fakeField.nextSaveToParent(fakeGroup, Field.class);
            fakeFields.add(fakeField);
            if (field.getFieldId().equals(fakeField.getFieldId())) {
                primaryFakeField = fakeField;
            }
        }
        Assert.notNull(primaryFakeField);
        return Pair.newInstance(primaryFakeField, fakeFields);
    }

    private static void generateFields(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, Map<String, PropertySheetStateHolder> stateHolders,
            QName[] hierarchy, NamespaceService namespaceService, Field... fields) {
        Pair<Map<String, ItemConfigVO>, Map<String, PropertySheetStateHolder>> result = generatorResults.generateItems(fields);
        Map<String, ItemConfigVO> generatedItems = result.getFirst();
        Map<String, PropertySheetStateHolder> generatedStateHolders = result.getSecond();
        Assert.isTrue(!CollectionUtils.containsAny(items.keySet(), generatedItems.keySet()));
        Assert.isTrue(!CollectionUtils.containsAny(stateHolders.keySet(), generatedStateHolders.keySet()));
        addStyleClassIfNeeded(generatedItems.values(), namespaceService);
        setBelongsToSubPropertySheet(generatedItems.values(), hierarchy);
        items.putAll(generatedItems);
        stateHolders.putAll(generatedStateHolders);
    }

    private static void addStyleClassIfNeeded(Collection<ItemConfigVO> values, NamespaceService namespaceService) {
        if (values != null) {
            String dailyAllowanceTotalSumPrefixString = DocumentSpecificModel.Props.DAILY_ALLOWANCE_TOTAL_SUM.toPrefixString(namespaceService);
            String expensesTotalSumPrefixString = DocumentSpecificModel.Props.EXPENSES_TOTAL_SUM.toPrefixString(namespaceService);
            for (ItemConfigVO item : values) {
                if (dailyAllowanceTotalSumPrefixString.equals(item.getName())) {
                    addStyleClass(item, "dailyAllowanceTotalSumField");
                }
                if (expensesTotalSumPrefixString.equals(item.getName())) {
                    addStyleClass(item, "expensesTotalSumField");
                }
            }
        }
    }

    private static void addStyleClass(ItemConfigVO item, String dailyAllowanceTotalSumClass) {
        String itemStyleClass = item.getStyleClass();
        itemStyleClass = StringUtils.isBlank(itemStyleClass) ? dailyAllowanceTotalSumClass : itemStyleClass + " " + dailyAllowanceTotalSumClass;
        item.setStyleClass(itemStyleClass);
    }

    private static void setBelongsToSubPropertySheet(Collection<ItemConfigVO> generatedItemValues, QName[] hierarchy) {
        String belongsToSubPropertySheetId = convertHierarchyToString(hierarchy);
        for (ItemConfigVO generatedItem : generatedItemValues) {
            generatedItem.setBelongsToSubPropertySheetId(belongsToSubPropertySheetId);
        }
    }

    private static String convertHierarchyToString(QName[] hierarchy) {
        String belongsToSubPropertySheetId = "";
        for (QName hierarchyPart : hierarchy) {
            if (belongsToSubPropertySheetId.length() > 0) {
                belongsToSubPropertySheetId += "_";
            }
            belongsToSubPropertySheetId += hierarchyPart.getLocalName();
        }
        return belongsToSubPropertySheetId;
    }

    private static List<Field> collectAndRemoveFieldsInOriginalOrder(List<Field> modifiableFieldsList, Field field, List<String> fieldIds) {
        if (!fieldIds.contains(field.getOriginalFieldId())) {
            return null;
        }
        List<Field> relatedFields = new ArrayList<Field>();
        for (int j = 0; j < modifiableFieldsList.size(); j++) {
            Field relatedField = modifiableFieldsList.get(j);
            if (fieldIds.contains(relatedField.getOriginalFieldId())) {
                relatedFields.add(relatedField);
                modifiableFieldsList.remove(j);
                j--;
            }
        }
        return relatedFields;
    }

    private static List<Field> collectAndRemoveFieldsInSpecifiedOrder(List<Field> modifiableFieldsList, Field field, List<String> fieldIds) {
        if (!fieldIds.contains(field.getOriginalFieldId())) {
            return null;
        }
        List<Field> relatedFields = new ArrayList<Field>();
        for (int i = 0; i < fieldIds.size(); i++) {
            String fieldId = fieldIds.get(i);
            for (int j = 0; j < modifiableFieldsList.size(); j++) {
                Field relatedField = modifiableFieldsList.get(j);
                if (fieldId.equals(relatedField.getOriginalFieldId())) {
                    relatedFields.add(relatedField);
                    modifiableFieldsList.remove(j);
                    j--;
                }
            }
        }
        return relatedFields;
    }

    public static class ErrandState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;
        private final Double dailyAllowanceSumParamValue;

        public ErrandState(Double dailyAllowanceSumParamValue) {
            this.dailyAllowanceSumParamValue = dailyAllowanceSumParamValue;
        }

        @Override
        public void reset(boolean inEditMode) {
            if (inEditMode) {
                calculateValues(dialogDataProvider.getNode(), dailyAllowanceSumParamValue);
            }
        }
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    @Override
    public void setBeanName(String name) {
        beanName = name;
    }

    @Override
    public String getBeanName() {
        return beanName;
    }

    @Override
    public void validate(DocumentDynamic document, ValidationHelper validationHelper) {

    }

    @Override
    public void save(DocumentDynamic document) {
        calculateValues(document.getNode(), geDailyAllowanceSumParamValue());
    }

    private static void calculateValues(Node document, Double dailyAllowanceSumParam) {
        List<Node> applicants = document.getAllChildAssociations(DocumentChildModel.Assocs.APPLICANT_ABROAD);
        if (applicants != null) {
            BigDecimal dailyAllowanceSum = dailyAllowanceSumParam != null ? new BigDecimal(dailyAllowanceSumParam) : BigDecimal.ZERO;
            for (Node applicant : applicants) {
                List<Node> errands = applicant.getAllChildAssociations(DocumentChildModel.Assocs.ERRAND_ABROAD);
                if (errands != null) {
                    for (Node errand : errands) {
                        calculateDailyAllowanceSums(dailyAllowanceSum, errand);
                        calculateExpensesSum(errand);
                    }
                }
            }
        }
    }

    private static void calculateExpensesSum(Node errand) {
        Map<String, Object> properties = errand.getProperties();
        @SuppressWarnings("unchecked")
        List<Double> expensesSums = (List<Double>) properties.get(DocumentSpecificModel.Props.EXPECTED_EXPENSE_SUM);
        BigDecimal expensesTotalSum = BigDecimal.ZERO;
        if (expensesSums != null) {
            for (Double expensesSum : expensesSums) {
                if (expensesSum != null) {
                    expensesTotalSum = expensesTotalSum.add(BigDecimal.valueOf(expensesSum));
                }
            }
        }
        properties.put(DocumentSpecificModel.Props.EXPENSES_TOTAL_SUM.toString(), expensesTotalSum.doubleValue());
    }

    private static void calculateDailyAllowanceSums(BigDecimal dailyAllowanceSum, Node errand) {
        Map<String, Object> properties = errand.getProperties();
        @SuppressWarnings("unchecked")
        List<Long> dailyAllowanceDays = (List<Long>) properties.get(DocumentSpecificModel.Props.DAILY_ALLOWANCE_DAYS);
        @SuppressWarnings("unchecked")
        List<String> dailyAllowanceRates = (List<String>) properties.get(DocumentSpecificModel.Props.DAILY_ALLOWANCE_RATE);
        List<Double> dailyAllowanceSums = new ArrayList<Double>();
        BigDecimal dailyAllowanceTotalSum = BigDecimal.ZERO;
        if (dailyAllowanceDays != null && dailyAllowanceRates != null) {
            for (int i = 0; i < dailyAllowanceDays.size(); i++) {
                Long dailyAllowanceDay = dailyAllowanceDays.get(i);
                BigDecimal days = dailyAllowanceDay != null ? new BigDecimal(dailyAllowanceDay) : BigDecimal.ZERO;
                BigDecimal rate = BigDecimal.ZERO;
                if (i < dailyAllowanceRates.size()) {
                    try {
                        String dailyAllowanceRate = dailyAllowanceRates.get(i);
                        rate = dailyAllowanceRate != null ? new BigDecimal(Double.parseDouble(dailyAllowanceRate)) : BigDecimal.ZERO;
                    } catch (NumberFormatException e) {

                    }

                }
                if (days != null && rate != null) {
                    BigDecimal rowSum = days.multiply((rate.divide(new BigDecimal(100)))).multiply(dailyAllowanceSum);
                    dailyAllowanceSums.add(rowSum.doubleValue());
                    dailyAllowanceTotalSum = dailyAllowanceTotalSum.add(rowSum);
                }
            }
        }
        properties.put(DocumentSpecificModel.Props.DAILY_ALLOWANCE_SUM.toString(), dailyAllowanceSums);
        properties.put(DocumentSpecificModel.Props.DAILY_ALLOWANCE_TOTAL_SUM.toString(), dailyAllowanceTotalSum.doubleValue());
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

}
