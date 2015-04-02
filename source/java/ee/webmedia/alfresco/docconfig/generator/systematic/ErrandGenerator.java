package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.common.web.BeanHelper.getClassificatorService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader;
import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.PropsBuilder;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeValidator;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.SaveListener;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.CalendarUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UserUtil;

public class ErrandGenerator extends BaseSystematicGroupGenerator implements SaveListener, BeanNameAware, DocumentTypeValidator {
    private static final String SUBSTITUTE_JOB_TITLE_FIELD_ID = "substituteJobTitle";

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ErrandGenerator.class);

    private static final String ERRAND_STATE_HOLDER_KEY = "errandGroup";

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
            SUBSTITUTE_JOB_TITLE_FIELD_ID,
            "substitutionBeginDate", // NOT removable from systematic group
            "substitutionEndDate"); // NOT removable from systematic group

    private List<String> substituteTableMandatoryFieldIds;

    // Table -- only on abroad
    private final List<String> dailyAllowanceTableFieldIds = Arrays.asList(
            "dailyAllowanceDays", // NOT removable from systematic group
            "dailyAllowanceRate", // NOT removable from systematic group
            "dailyAllowanceSum"); // NOT removable from systematic group

    // Table -- only on errand
    private final List<String> dailyAllowanceTableErrandFieldIds = Arrays.asList(
            "dailyAllowanceCateringCount", // NOT removable from systematic group
            "dailyAllowanceDays", // NOT removable from systematic group
            "dailyAllowanceRate", // NOT removable from systematic group
            "dailyAllowanceFinancingSource"); // NOT removable from systematic group

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
    public void afterPropertiesSet() {
        substituteTableMandatoryFieldIds = new ArrayList<String>(substituteTableFieldIds);
        substituteTableMandatoryFieldIds.remove(SUBSTITUTE_JOB_TITLE_FIELD_ID);
        getDocumentAdminService().registerDocumentTypeValidator("substituteFieldConsistency", this);
        super.afterPropertiesSet();
    }

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

        Set<String> errandFieldIds = new HashSet<String>();
        errandFieldIds.addAll(errandDateInlineFieldIds);
        errandFieldIds.add("country");
        errandFieldIds.add("city");
        errandFieldIds.addAll(dailyAllowanceTableErrandFieldIds);
        errandFieldIds.add("expenseType");
        errandFieldIds.add("expenseDesc");
        errandFieldIds.add("expensesFinancingSource");
        errandFieldIds.add("advancePaymentSum");
        errandFieldIds.add("advancePaymentDesc");
        errandFieldIds.addAll(substituteTableFieldIds);
        errandFieldIds.add("errandComment");

        Map<QName[], Set<String>> applicantDomesticAdditionalHierarchy = new HashMap<QName[], Set<String>>();
        applicantDomesticAdditionalHierarchy.put(new QName[] { DocumentChildModel.Assocs.ERRAND_DOMESTIC }, errandDomesticFieldIds);
        documentConfigService.registerChildAssocTypeQNameHierarchy(SystematicFieldGroupNames.ERRAND_DOMESTIC_APPLICANT, DocumentChildModel.Assocs.APPLICANT_DOMESTIC,
                applicantDomesticAdditionalHierarchy);

        Map<QName[], Set<String>> applicantAbroadAdditionalHierarchy = new HashMap<QName[], Set<String>>();
        applicantAbroadAdditionalHierarchy.put(new QName[] { DocumentChildModel.Assocs.ERRAND_ABROAD }, errandAbroadFieldIds);
        documentConfigService.registerChildAssocTypeQNameHierarchy(SystematicFieldGroupNames.ERRAND_ABROAD_APPLICANT, DocumentChildModel.Assocs.APPLICANT_ABROAD,
                applicantAbroadAdditionalHierarchy);

        documentConfigService.registerChildAssocTypeQNameHierarchy(SystematicFieldGroupNames.TRAINING_APPLICANT, DocumentChildModel.Assocs.APPLICANT_TRAINING, null);

        Map<QName[], Set<String>> applicantErrandAdditionalHierarchy = new HashMap<QName[], Set<String>>();
        applicantErrandAdditionalHierarchy.put(new QName[] { DocumentChildModel.Assocs.ERRAND }, errandFieldIds);
        documentConfigService.registerChildAssocTypeQNameHierarchy(SystematicFieldGroupNames.ERRAND_APPLICANT, DocumentChildModel.Assocs.APPLICANT_ERRAND,
                applicantErrandAdditionalHierarchy);

        Set<String> multiValueOverrideFieldOriginalIds = new HashSet<String>();
        multiValueOverrideFieldOriginalIds.addAll(substituteTableFieldIds);
        multiValueOverrideFieldOriginalIds.addAll(dailyAllowanceTableFieldIds);
        multiValueOverrideFieldOriginalIds.addAll(expenseTableFieldIds);
        documentConfigService.registerMultiValuedOverrideBySystematicGroupName(SystematicFieldGroupNames.ERRAND_DOMESTIC_APPLICANT, multiValueOverrideFieldOriginalIds);
        documentConfigService.registerMultiValuedOverrideBySystematicGroupName(SystematicFieldGroupNames.ERRAND_ABROAD_APPLICANT, multiValueOverrideFieldOriginalIds);
        documentConfigService.registerMultiValuedOverrideBySystematicGroupName(SystematicFieldGroupNames.TRAINING_APPLICANT, multiValueOverrideFieldOriginalIds);
        multiValueOverrideFieldOriginalIds.addAll(Arrays.asList("dailyAllowanceCateringCount", "dailyAllowanceFinancingSource", "expenseDesc", "expensesFinancingSource"));
        documentConfigService.registerMultiValuedOverrideBySystematicGroupName(SystematicFieldGroupNames.ERRAND_APPLICANT, multiValueOverrideFieldOriginalIds);

        return new String[] { SystematicFieldGroupNames.ERRAND_DOMESTIC_APPLICANT, SystematicFieldGroupNames.ERRAND_ABROAD_APPLICANT, SystematicFieldGroupNames.TRAINING_APPLICANT,
                SystematicFieldGroupNames.ERRAND_APPLICANT };
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

        ErrandState primaryStateHolder = new ErrandState(geDailyAllowanceSumParamValue());
        generatorResults.addStateHolder(ERRAND_STATE_HOLDER_KEY, primaryStateHolder);

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
                boolean forceEditMode = false;
                if (!relatedFields.isEmpty()) {
                    forceEditMode = generateReadonlyGroupItem(relatedFields2.getFirst(), relatedFields, Arrays.asList("applicantName", "country"),
                            convertHierarchyToString(hierarchy), generatorResults,
                            fieldsByOriginalId);
                }
                generateFields(generatorResults, items, primaryStateHolder, stateHolders, hierarchy, forceEditMode, relatedFields.toArray(new Field[relatedFields.size()]));
                continue;
            }
            List<String> dailyAllowanceFields = new ArrayList<String>();
            dailyAllowanceFields.add("dailyAllowanceCateringCount");
            dailyAllowanceFields.addAll(dailyAllowanceTableFieldIds);
            dailyAllowanceFields.add("dailyAllowanceFinancingSource");

            relatedFields = collectAndRemoveFieldsInOriginalOrder(notProcessedFields, field, dailyAllowanceFields);
            if (relatedFields != null) {
                List<String> columnStyleClasses = relatedFields.size() < 4 ? Arrays.asList("dailyAllowanceDaysField", "dailyAllowanceRateField", "dailyAllowanceSumField")
                        : Collections.<String> emptyList();
                setDailyAllowanceSumParameter(relatedFields);

                // Check if any of the fields for daily allowance are mandatory
                for (String originalFieldId : dailyAllowanceTableFieldIds) {
                    Field allowanceField = fieldsByOriginalId.get(originalFieldId);
                    if (allowanceField != null && allowanceField.isMandatory()) {
                        primaryStateHolder.dailyAllowanceMandatory = true;
                        break;
                    }
                }

                ItemConfigVO item = generateTable(generatorResults, items, primaryStateHolder, hierarchy, relatedFields, field, "Päevaraha", "add", columnStyleClasses, primaryStateHolder.dailyAllowanceMandatory);
                item.setStyleClass("add-expense");
                continue;
            }

            List<String> expenseFields = new ArrayList<String>(expenseTableFieldIds);
            expenseFields.add("expenseType");
            expenseFields.add("expenseDesc");
            expenseFields.add("expensesFinancingSource");
            relatedFields = collectAndRemoveFieldsInOriginalOrder(notProcessedFields, field, expenseFields);
            if (relatedFields != null) {
                List<String> columnStyleClasses = Arrays.asList("", "expectedExpenseSumField");
                ItemConfigVO item = generateTable(generatorResults, items, primaryStateHolder, hierarchy, relatedFields, field, "Kulud", "add", columnStyleClasses, false);
                item.setStyleClass("add-expense");
                continue;
            }
            relatedFields = collectAndRemoveFieldsInSpecifiedOrder(notProcessedFields, field, eventDateInlineFieldIds);
            if (relatedFields != null) {
                generateInline(generatorResults, items, primaryStateHolder, hierarchy, relatedFields, field, "Ürituse toimumise aeg", "document_eventDates_templateText");
                continue;
            }
            relatedFields = collectAndRemoveFieldsInSpecifiedOrder(notProcessedFields, field, errandDateInlineFieldIds);
            if (relatedFields != null) {
                generateInline(generatorResults, items, primaryStateHolder, hierarchy, relatedFields, field, "Lähetus", "document_eventDates_templateText");
                continue;
            }
            relatedFields = collectAndRemoveFieldsInSpecifiedOrder(notProcessedFields, field, advancePaymentInlineFieldIds);
            if (relatedFields != null) {
                generateInline(generatorResults, items, primaryStateHolder, hierarchy, relatedFields, field, "Soovin ettemaksu", "document_errand_advancePayment_templateText-edit");
                continue;
            }

            // If field is not related to a group of fields, then process it separately
            generateFields(generatorResults, items, primaryStateHolder, stateHolders, hierarchy, false, field);
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

    private ItemConfigVO generateTable(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, ErrandState primaryStateHolder, QName[] hierarchy,
            List<Field> relatedFields,
            Field primaryField, String displayLabel, String addLabelId, List<String> columnStyleClasses, boolean forcedMandatory) {
        Pair<ItemConfigVO, String> result = generateBasePropsItem(generatorResults, items, primaryStateHolder, hierarchy, relatedFields, primaryField, displayLabel,
                columnStyleClasses);

        ItemConfigVO item = result.getFirst();
        item.setComponentGenerator("MultiValueEditorGenerator");
        item.setAddLabelId(addLabelId);
        item.setPropsGeneration(result.getSecond());
        item.setForcedMandatory(forcedMandatory);

        setBelongsToSubPropertySheet(Collections.singletonList(item), hierarchy);
        items.put(item.getName(), item);
        return item;
    }

    private ItemConfigVO generateInline(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, ErrandState primaryStateHolder, QName[] hierarchy,
            List<Field> relatedFields,
            Field primaryField, String displayLabel, String textId) {

        Pair<ItemConfigVO, String> result = generateBasePropsItem(generatorResults, items, primaryStateHolder, hierarchy, relatedFields, primaryField, displayLabel);

        ItemConfigVO item = result.getFirst();
        item.setComponentGenerator("InlinePropertyGroupGenerator");
        item.setTextId(textId);
        String props = result.getSecond();
        
        if (relatedFields.size() >= 2) {
            item.setProps(addMandatoryMarkers(relatedFields, props));
        } else {
            item.setProps(props);
        }
        return item;
    }

    private String addMandatoryMarkers(List<Field> relatedFields, String props) {
        String[] prop = props.split(CombinedPropReader.AttributeNames.DEFAULT_PROPERTIES_SEPARATOR);
        for (int i = 0; i < relatedFields.size(); i++) {
            if (relatedFields.get(i).isMandatory()) {
                prop[i] += PropsBuilder.DEFAULT_OPTIONS_SEPARATOR + CustomAttributeNames.ATTR_MANDATORY + "=true";
            }
        }
        return StringUtils.join(prop, CombinedPropReader.AttributeNames.DEFAULT_PROPERTIES_SEPARATOR);
    }

    private Pair<ItemConfigVO, String> generateBasePropsItem(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, ErrandState primaryStateHolder,
            QName[] hierarchy,
            List<Field> relatedFields, Field primaryField, String displayLabel) {
        return generateBasePropsItem(generatorResults, items, primaryStateHolder, hierarchy, relatedFields, primaryField, displayLabel, null);
    }

    private Pair<ItemConfigVO, String> generateBasePropsItem(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, ErrandState primaryStateHolder,
            QName[] hierarchy,
            List<Field> relatedFields, Field primaryField, String displayLabel, List<String> columnStyleClasses) {
        List<String> props = new ArrayList<String>();
        Pair<Map<String, ItemConfigVO>, Map<String, PropertySheetStateHolder>> columnItemsAndStateHolders = generatorResults.generateItems(relatedFields
                .toArray(new Field[relatedFields.size()]));
        Map<String, ItemConfigVO> columnItems = columnItemsAndStateHolders.getFirst();
        int columnCounter = 0;
        for (ItemConfigVO columnItem : columnItems.values()) {
            QName propName = QName.resolveToQName(namespaceService, columnItem.getName());

            for (Field field : relatedFields) {
                if (field.getFieldId().equals(propName.getLocalName())) {
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
                    setSpecificItemProps(columnItem, field, primaryStateHolder);
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
            relatedFields = collectAndRemoveFieldsInSpecifiedOrder(modifiableFieldsList, field, Arrays.asList("country", "city"));
            readonlyFieldsName = "Toimumiskoht";
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

    private boolean generateReadonlyGroupItem(Field primaryField, List<Field> fields, List<String> primaryFieldRequiredId, String subpropSheetId,
            FieldGroupGeneratorResults generatorResults, Map<String, Field> fieldsByOriginalId) {
        String primaryFakeFieldId = primaryField != null ? primaryField.getFieldId() : null;
        boolean foundRequiredField = false;
        for (String requiredId : primaryFieldRequiredId) {
            Field field = fieldsByOriginalId.get(requiredId);
            if (field != null && field.getFieldId().equals(primaryFakeFieldId)) {
                foundRequiredField = true;
                break;
            }
        }

        if (!foundRequiredField) {
            return false;
        }

        FieldGroup fieldGroup = (FieldGroup) primaryField.getParent();
        StringBuffer readonlyFieldsRule = new StringBuffer("{" + primaryFakeFieldId + "}");

        List<Field> readonlyViewFields = new ArrayList<Field>();
        List<String> ignoredFields = Arrays.asList("applicantName", "applicantId", "country", "city");
        for (Field fakeField : fields) {
            String originalFieldId = fakeField.getOriginalFieldId();
            if (!ignoredFields.contains(originalFieldId)) {
                readonlyViewFields.add(fakeField);
            }
            if ("city".equals(originalFieldId)) {
                readonlyFieldsRule.append(", {" + fakeField.getFieldId() + "}");
            }
        }

        if (!readonlyViewFields.isEmpty()) {
            readonlyFieldsRule.append("/¤ (");
            for (Field readonlyField : readonlyViewFields) {
                readonlyFieldsRule.append("{" + readonlyField.getFieldId() + "}");
            }
            readonlyFieldsRule.append(")¤/");
        }
        fieldGroup.setReadonlyFieldsRule(readonlyFieldsRule.toString());
        ItemConfigVO generateFieldGroupReadonlyItem = documentConfigService.generateFieldGroupReadonlyItem(fieldGroup);
        generateFieldGroupReadonlyItem.setBelongsToSubPropertySheetId(subpropSheetId);
        generatorResults.addItem(generateFieldGroupReadonlyItem);
        return true;
    }

    private void generateFields(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, ErrandState primaryStateHolder,
            Map<String, PropertySheetStateHolder> stateHolders,
            QName[] hierarchy, boolean forceEditMode, Field... fields) {
        Pair<Map<String, ItemConfigVO>, Map<String, PropertySheetStateHolder>> result = generatorResults.generateItems(fields);
        Map<String, ItemConfigVO> generatedItems = result.getFirst();
        Map<String, PropertySheetStateHolder> generatedStateHolders = result.getSecond();
        Assert.isTrue(!CollectionUtils.containsAny(items.keySet(), generatedItems.keySet()));
        Assert.isTrue(!CollectionUtils.containsAny(stateHolders.keySet(), generatedStateHolders.keySet()));
        for (ItemConfigVO item : generatedItems.values()) {
            if (forceEditMode) {
                item.setShowInViewMode(false);
            }
            QName propName = QName.resolveToQName(namespaceService, item.getName());
            for (Field field : fields) {
                if (field.getFieldId().equals(propName.getLocalName())) {
                    setSpecificItemProps(item, field, primaryStateHolder);
                }
            }
        }
        setBelongsToSubPropertySheet(generatedItems.values(), hierarchy);
        items.putAll(generatedItems);
        stateHolders.putAll(generatedStateHolders);
    }

    private void setSpecificItemProps(ItemConfigVO item, Field field, ErrandState primaryStateHolder) {
        String originalFieldId = field.getOriginalFieldId();
        if (DocumentSpecificModel.Props.DAILY_ALLOWANCE_TOTAL_SUM.getLocalName().equals(originalFieldId)) {
            addStyleClass(item, "dailyAllowanceTotalSumField");
            primaryStateHolder.dailyAllowanceTotalSumProp = field.getQName();
        } else if (DocumentSpecificModel.Props.DAILY_ALLOWANCE_DAYS.getLocalName().equals(originalFieldId)) {
            primaryStateHolder.dailyAllowanceDaysProp = field.getQName();
            Field cateringCountField = ((FieldGroup) field.getParent()).getFieldsByOriginalId().get(DocumentSpecificModel.Props.DAILY_ALLOWANCE_CATERING_COUNT.getLocalName());
            if (cateringCountField != null) {
                item.setMandatoryIf(cateringCountField.getQName().toPrefixString(namespaceService) + "!=null");
            }
        } else if (DocumentSpecificModel.Props.DAILY_ALLOWANCE_RATE.getLocalName().equals(originalFieldId)) {
            primaryStateHolder.dailyAllowanceRateProp = field.getQName();
            Field cateringCountField = ((FieldGroup) field.getParent()).getFieldsByOriginalId().get(DocumentSpecificModel.Props.DAILY_ALLOWANCE_CATERING_COUNT.getLocalName());
            if (cateringCountField != null) {
                item.setMandatoryIf(cateringCountField.getQName().toPrefixString(namespaceService) + "!=null");
            }
        } else if (DocumentSpecificModel.Props.DAILY_ALLOWANCE_SUM.getLocalName().equals(originalFieldId)) {
            primaryStateHolder.dailyAllowanceSumProp = field.getQName();
        } else if (DocumentSpecificModel.Props.EXPENSES_TOTAL_SUM.getLocalName().equals(originalFieldId)) {
            addStyleClass(item, "expensesTotalSumField");
            primaryStateHolder.expensesTotalSumProp = field.getQName();
        } else if (DocumentDynamicModel.Props.REPORT_DUE_DATE.getLocalName().equals(originalFieldId)) {
            addStyleClass(item, "date reportDueDate");
        } else if (DocumentSpecificModel.Props.EVENT_BEGIN_DATE.getLocalName().equals(originalFieldId)) {
            addStyleClass(item, "date eventBeginDate");
        } else if (DocumentSpecificModel.Props.EVENT_END_DATE.getLocalName().equals(originalFieldId)) {
            addStyleClass(item, "date eventEndDate");
        } else if (DocumentSpecificModel.Props.ERRAND_BEGIN_DATE.getLocalName().equals(originalFieldId)) {
            addStyleClass(item, "date errandBeginDate");
            primaryStateHolder.errandBeginDateProp = field.getQName();
        } else if (DocumentSpecificModel.Props.ERRAND_END_DATE.getLocalName().equals(originalFieldId)) {
            addStyleClass(item, "date errandEndDate errandReportDateBase");
            primaryStateHolder.errandEndDateProp = field.getQName();
        } else if (DocumentSpecificModel.Props.DAILY_ALLOWANCE_FINANCING_SOURCE.getLocalName().equals(originalFieldId)) {
            Field cateringCountField = ((FieldGroup) field.getParent()).getFieldsByOriginalId().get(DocumentSpecificModel.Props.DAILY_ALLOWANCE_CATERING_COUNT.getLocalName());
            if (cateringCountField != null) {
                item.setMandatoryIf(cateringCountField.getQName().toPrefixString(namespaceService) + "!=null");
            }
        } else if (DocumentSpecificModel.Props.EXPECTED_EXPENSE_SUM.getLocalName().equals(originalFieldId)
                || DocumentSpecificModel.Props.EXPENSES_FINANCING_SOURCE.getLocalName().equals(originalFieldId)) {
            Field expenseTypeField = ((FieldGroup) field.getParent()).getFieldsByOriginalId().get(DocumentDynamicModel.Props.EXPENSE_TYPE.getLocalName());
            if (expenseTypeField != null) {
                item.setMandatoryIf(expenseTypeField.getQName().toPrefixString(namespaceService) + "!=null");
            }
            if (DocumentSpecificModel.Props.EXPECTED_EXPENSE_SUM.getLocalName().equals(originalFieldId)) {
                primaryStateHolder.expectedExpenseSumProp = field.getQName();
            }
        } else if (DocumentSpecificModel.Props.APPLICANT_NAME.getLocalName().equals(originalFieldId)) {
            Assert.isTrue(Boolean.valueOf(item.getCustomAttributes().get(Search.SETTER_CALLBACK_TAKES_NODE)));
            primaryStateHolder.originalApplicantNameSetterCallback = item.getCustomAttributes().get(Search.SETTER_CALLBACK);
            item.setSetterCallback(getBindingName("setApplicantName", ERRAND_STATE_HOLDER_KEY));
        } else if (DocumentDynamicModel.Props.APPLICANT_ORG_STRUCT_UNIT.getLocalName().equals(originalFieldId)) {
            item.setSetterCallbackTakesNode(true);
            item.setSetterCallback(getBindingName("setApplicantOrgStructUnit", ERRAND_STATE_HOLDER_KEY));
            item.setAjaxParentLevel(1);
            primaryStateHolder.applicantOrgStructUnitProp = field.getQName();
        } else if (DocumentSpecificModel.Props.COST_MANAGER.getLocalName().equals(originalFieldId)) {
            primaryStateHolder.costManagerProp = field.getQName();
            String classificatorName = field.getClassificator();
            if (StringUtils.isNotBlank(classificatorName)) {
                primaryStateHolder.costManagerClassificatorValues = getClassificatorService().getActiveClassificatorValues(
                        getClassificatorService().getClassificatorByName(classificatorName));
            }
        } else if (DocumentDynamicModel.Props.COST_CENTER.getLocalName().equals(originalFieldId)) {
            primaryStateHolder.costCenterProp = field.getQName();
            String classificatorName = field.getClassificator();
            if (StringUtils.isNotBlank(classificatorName)) {
                primaryStateHolder.costCenterClassificatorValues = getClassificatorService().getActiveClassificatorValues(
                        getClassificatorService().getClassificatorByName(classificatorName));
            }
        }
    }

    private static void addStyleClass(ItemConfigVO item, String styleClassToAdd) {
        String itemStyleClass = item.getStyleClass();
        itemStyleClass = StringUtils.isBlank(itemStyleClass) ? styleClassToAdd : itemStyleClass + " " + styleClassToAdd;
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
        private String originalApplicantNameSetterCallback;
        private QName applicantOrgStructUnitProp;
        private QName costManagerProp;
        private QName costCenterProp;
        private List<ClassificatorValue> costManagerClassificatorValues = Collections.emptyList();
        private List<ClassificatorValue> costCenterClassificatorValues = Collections.emptyList();
        private final BigDecimal dailyAllowanceSum;
        private QName expectedExpenseSumProp; // NOT removable from systematic group
        private QName expensesTotalSumProp; // NOT removable from systematic group
        private QName dailyAllowanceDaysProp; // NOT removable from systematic group
        private QName dailyAllowanceRateProp; // NOT removable from systematic group
        private QName dailyAllowanceSumProp; // NOT removable from systematic group
        private QName dailyAllowanceTotalSumProp; // NOT removable from systematic group
        private QName errandBeginDateProp; // NOT removable from systematic group
        private QName errandEndDateProp; // NOT removable from systematic group
        private boolean dailyAllowanceMandatory;

        public ErrandState(Double dailyAllowanceSumParamValue) {
            dailyAllowanceSum = dailyAllowanceSumParamValue != null ? new BigDecimal(dailyAllowanceSumParamValue) : BigDecimal.ZERO;
        }

        @Override
        public void reset(boolean inEditMode) {
            if (inEditMode) {
                calculateValues(dialogDataProvider.getNode(), false);
            }
        }

        public void setApplicantName(String result, Node node) {
            FacesContext context = FacesContext.getCurrentInstance();
            MethodBinding mb = context.getApplication().createMethodBinding(originalApplicantNameSetterCallback, new Class[] { String.class, Node.class });
            mb.invoke(context, new Object[] { result, node });

            if (applicantOrgStructUnitProp != null) {
                @SuppressWarnings("unchecked")
                String displayUnit = UserUtil.getDisplayUnit((List<String>) node.getProperties().get(applicantOrgStructUnitProp.toString()));
                applicantOrgStructUnitChanged(displayUnit, node);
            }
        }

        public void setApplicantOrgStructUnit(String result, Node node) {
            applicantOrgStructUnitChanged(result, node);
        }

        // Same rules as in DocumentConfigServiceImpl#setSpecialDependentValues
        private void applicantOrgStructUnitChanged(String displayUnit, Node node) {

            if (costManagerProp != null) {
                node.getProperties().put(costManagerProp.toString(), "");
                for (ClassificatorValue classificatorValue : costManagerClassificatorValues) {
                    if (classificatorValue.getClassificatorDescription().equals(displayUnit)) {
                        node.getProperties().put(costManagerProp.toString(), classificatorValue.getValueName());
                        break;
                    }
                }
            }

            if (costCenterProp != null) {
                node.getProperties().put(costCenterProp.toString(), "");
                for (ClassificatorValue classificatorValue : costCenterClassificatorValues) {
                    if (classificatorValue.getClassificatorDescription().equals(displayUnit)) {
                        node.getProperties().put(costCenterProp.toString(), classificatorValue.getValueName());
                        break;
                    }
                }
            }
        }

        private void calculateValues(Node document, boolean validate) {
            List<Node> applicants = document.getAllChildAssociations(DocumentChildModel.Assocs.APPLICANT_ABROAD);
            if (applicants != null) {
                for (Node applicant : applicants) {
                    List<Node> errands = applicant.getAllChildAssociations(DocumentChildModel.Assocs.ERRAND_ABROAD);
                    if (errands != null) {
                        for (Node errand : errands) {
                            calculateDailyAllowanceSums(errand, validate);
                            calculateExpensesSum(errand);
                        }
                    }
                }
            }

            applicants = document.getAllChildAssociations(DocumentChildModel.Assocs.APPLICANT_DOMESTIC);
            if (applicants != null) {
                for (Node applicant : applicants) {
                    List<Node> errands = applicant.getAllChildAssociations(DocumentChildModel.Assocs.ERRAND_DOMESTIC);
                    if (errands != null) {
                        for (Node errand : errands) {
                            calculateExpensesSum(errand);
                        }
                    }
                }
            }

            applicants = document.getAllChildAssociations(DocumentChildModel.Assocs.APPLICANT_TRAINING);
            if (applicants != null) {
                for (Node applicant : applicants) {
                    calculateDailyAllowanceSums(applicant, validate);
                    calculateExpensesSum(applicant);

                }
            }
        }

        private void calculateExpensesSum(Node errand) {
            Map<String, Object> properties = errand.getProperties();
            @SuppressWarnings("unchecked")
            List<Double> expensesSums = (List<Double>) properties.get(expectedExpenseSumProp.toString());
            BigDecimal expensesTotalSum = BigDecimal.ZERO;
            if (expensesSums != null) {
                for (Double expensesSum : expensesSums) {
                    if (expensesSum != null) {
                        expensesTotalSum = expensesTotalSum.add(BigDecimal.valueOf(expensesSum));
                    }
                }
            }
            properties.put(expensesTotalSumProp.toString(), expensesTotalSum.doubleValue());
        }

        private void calculateDailyAllowanceSums(Node errand, boolean validate) {
            Map<String, Object> properties = errand.getProperties();
            @SuppressWarnings("unchecked")
            List<Long> dailyAllowanceDays = (List<Long>) properties.get(dailyAllowanceDaysProp.toString());
            @SuppressWarnings("unchecked")
            List<String> dailyAllowanceRates = (List<String>) properties.get(dailyAllowanceRateProp.toString());
            List<Double> dailyAllowanceSums = new ArrayList<Double>();
            int dailyAllowanceDaysTotal = 0;
            BigDecimal dailyAllowanceTotalSum = BigDecimal.ZERO;
            if (dailyAllowanceDays != null && dailyAllowanceRates != null) {
                for (int i = 0; i < dailyAllowanceDays.size(); i++) {
                    Long dailyAllowanceDay = dailyAllowanceDays.get(i);
                    dailyAllowanceDaysTotal += dailyAllowanceDay != null ? dailyAllowanceDay : 0;
                    BigDecimal days = dailyAllowanceDay != null ? new BigDecimal(dailyAllowanceDay) : BigDecimal.ZERO;
                    BigDecimal rate = BigDecimal.ZERO;
                    if (i < dailyAllowanceRates.size()) {
                        try {
                            String dailyAllowanceRate = dailyAllowanceRates.get(i);
                            rate = dailyAllowanceRate != null ? new BigDecimal(Double.parseDouble(dailyAllowanceRate)) : BigDecimal.ZERO;
                        } catch (NumberFormatException e) {
                            // Do nothing
                        }
                    }
                    if (days != null && rate != null) {
                        BigDecimal rowSum = days.multiply((rate.divide(new BigDecimal(100)))).multiply(dailyAllowanceSum);
                        dailyAllowanceSums.add(rowSum.doubleValue());
                        dailyAllowanceTotalSum = dailyAllowanceTotalSum.add(rowSum);
                    }
                }
            }
            properties.put(dailyAllowanceSumProp.toString(), dailyAllowanceSums);
            properties.put(dailyAllowanceTotalSumProp.toString(), dailyAllowanceTotalSum.doubleValue());
            if (validate) {
                Date errandBegin = (Date) properties.get(errandBeginDateProp.toString());
                Date errandEnd = (Date) properties.get(errandEndDateProp.toString());
                if (errandBegin != null && errandEnd != null) {
                    int calculatedDays = CalendarUtil.getDaysBetween(new LocalDate(errandBegin.getTime()), new LocalDate(errandEnd.getTime()));
                    if (dailyAllowanceDaysTotal != calculatedDays && (CollectionUtils.isNotEmpty(dailyAllowanceDays) || dailyAllowanceMandatory)) {
                        throw new UnableToPerformException("document_errand_dailyAllowance_days_sum_match_totalDays");
                    }
                }
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
    public void validate(DynamicBase dynamicObject, ValidationHelper validationHelper) {
        ErrandState errandStateHolder = BeanHelper.getPropertySheetStateBean().getStateHolder(ERRAND_STATE_HOLDER_KEY, ErrandState.class);
        // errandStateHolder may be null if save action is not initiated from document dialog.
        // At present, it is assumed that there is no need to check values when saving not from document dialog.
        if (errandStateHolder != null) {
            List<Node> applicants = dynamicObject.getNode().getAllChildAssociations(DocumentChildModel.Assocs.APPLICANT_ABROAD);
            if (applicants != null) {
                for (Node applicant : applicants) {
                    List<Node> errands = applicant.getAllChildAssociations(DocumentChildModel.Assocs.ERRAND_ABROAD);
                    if (errands != null) {
                        validateErrandDailyAllowance(validationHelper, errandStateHolder, errands);
                    }
                }
            }

            applicants = dynamicObject.getNode().getAllChildAssociations(DocumentChildModel.Assocs.APPLICANT_TRAINING);
            if (applicants != null) {
                validateErrandDailyAllowance(validationHelper, errandStateHolder, applicants);
            }
        }
    }

    private void validateErrandDailyAllowance(ValidationHelper validationHelper, ErrandState errandStateHolder, List<Node> errands) {
        if (!errandStateHolder.dailyAllowanceMandatory) {
            return;
        }

        outer: for (Node errand : errands) {
            Map<String, Object> properties = errand.getProperties();
            @SuppressWarnings("unchecked")
            List<Long> dailyAllowanceDays = (List<Long>) properties.get(errandStateHolder.dailyAllowanceDaysProp.toString());
            @SuppressWarnings("unchecked")
            List<String> dailyAllowanceRates = (List<String>) properties.get(errandStateHolder.dailyAllowanceRateProp.toString());
            if (dailyAllowanceDays != null) {
                for (int i = 0; i < dailyAllowanceDays.size(); i++) {
                    if (dailyAllowanceDays.get(i) != null && StringUtils.isNotBlank(dailyAllowanceRates.get(i))) {
                        continue outer;
                    }
                }
            }
            validationHelper.addErrorMessage("document_validationMsg_mandatory_daily_allowance");
        }
    }

    @Override
    public void save(DynamicBase document) {
        if (document instanceof DocumentDynamic) {
            ErrandState errandStateHolder = BeanHelper.getPropertySheetStateBean().getStateHolder(ERRAND_STATE_HOLDER_KEY, ErrandState.class);
            // errandStateHolder may be null if save action is not initiated from document dialog.
            // At present, it is assumed that there is no need to recalculate values when saving not from document dialog.
            if (errandStateHolder != null) {
                errandStateHolder.calculateValues(document.getNode(), true);
            }
        }
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    @Override
    public boolean validate(DocumentTypeVersion type, Map<String, String> errorMessages) {
        boolean valid = true;
        for (Field field : type.getFieldsDeeply()) {
            if (field.getParent() instanceof FieldGroup) {
                FieldGroup parentGroup = (FieldGroup) field.getParent();
                if (parentGroup.isSystematic() && substituteTableFieldIds.contains(field.getOriginalFieldId())) {
                    valid &= parentGroup.getFieldsByOriginalId().keySet().containsAll(substituteTableMandatoryFieldIds);
                    if (!valid && errorMessages.size() == 0) {
                        errorMessages.put("docType_save_error_missing_substitute_field", TextUtil.joinNonBlankStringsWithComma(substituteTableMandatoryFieldIds));
                    }
                }
            }

        }
        return valid;
    }
}
