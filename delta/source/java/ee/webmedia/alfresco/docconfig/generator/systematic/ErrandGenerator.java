package ee.webmedia.alfresco.docconfig.generator.systematic;

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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
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
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicGroupGenerator;
import ee.webmedia.alfresco.docconfig.generator.FieldGroupGeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.systematic.UserContactTableGenerator.UserContactTableState;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public class ErrandGenerator extends BaseSystematicGroupGenerator {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ErrandGenerator.class);

    private NamespaceService namespaceService;

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

        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
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
            Pair<Field, List<Field>> relatedFields2 = collectAndRemoveFieldsInOriginalOrderToFakeGroup(fields, field, fieldsByOriginalId);
            List<Field> relatedFields = relatedFields2 == null ? null : relatedFields2.getSecond();
            if (relatedFields != null) {
                generateFields(generatorResults, items, stateHolders, hierarchy, relatedFields.toArray(new Field[relatedFields.size()]));
                continue;
            }
            relatedFields = collectAndRemoveFieldsInOriginalOrder(fields, field, dailyAllowanceTableFieldIds);
            if (relatedFields != null) {
                ItemConfigVO item = generateTable(generatorResults, items, hierarchy, relatedFields, field, "Päevaraha", "add");
                item.setStyleClass("add-expense");
                continue;
            }
            relatedFields = collectAndRemoveFieldsInOriginalOrder(fields, field, expenseTableFieldIds);
            if (relatedFields != null) {
                ItemConfigVO item = generateTable(generatorResults, items, hierarchy, relatedFields, field, "Kulud", "add");
                item.setStyleClass("add-expense");
                continue;
            }
            relatedFields = collectAndRemoveFieldsInSpecifiedOrder(fields, field, eventDateInlineFieldIds);
            if (relatedFields != null) {
                generateInline(generatorResults, items, hierarchy, relatedFields, field, "Ürituse toimumise aeg", "document_eventDates_templateText");
                continue;
            }
            relatedFields = collectAndRemoveFieldsInSpecifiedOrder(fields, field, errandDateInlineFieldIds);
            if (relatedFields != null) {
                generateInline(generatorResults, items, hierarchy, relatedFields, field, "Lähetus", "document_eventDates_templateText");
                continue;
            }
            relatedFields = collectAndRemoveFieldsInSpecifiedOrder(fields, field, advancePaymentInlineFieldIds);
            if (relatedFields != null) {
                generateInline(generatorResults, items, hierarchy, relatedFields, field, "Soovin avanssi", "document_errand_advancePayment_templateText-edit");
                continue;
            }

            // If field is not related to a group of fields, then process it separately
            generateFields(generatorResults, items, stateHolders, hierarchy, field);
        }

        for (ItemConfigVO item : items.values()) {
            generatorResults.addItem(item);
        }
        for (Entry<String, PropertySheetStateHolder> entry : stateHolders.entrySet()) {
            generatorResults.addStateHolder(entry.getKey(), entry.getValue());
        }
    }

    private ItemConfigVO generateTable(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, QName[] hierarchy, List<Field> relatedFields,
            Field primaryField, String displayLabel, String addLabelId) {

        Pair<ItemConfigVO, Pair<String, List<QName>>> result = generateBasePropsItem(generatorResults, items, hierarchy, relatedFields, primaryField, displayLabel);

        ItemConfigVO item = result.getFirst();
        item.setComponentGenerator("MultiValueEditorGenerator");
        item.setAddLabelId(addLabelId);
        item.setShowInViewMode(false);
        item.setPropsGeneration(result.getSecond().getFirst());

        String stateHolderKey = primaryField.getFieldId();

        // And generate a separate view mode component
        String viewModePropName = RepoUtil.createTransientProp(primaryField.getFieldId() + "Label").toString();
        ItemConfigVO viewModeItem = generatorResults.generateAndAddViewModeText(viewModePropName, displayLabel);
        viewModeItem.setComponentGenerator("UnescapedOutputTextGenerator");

        generatorResults.addStateHolder(stateHolderKey, new UserContactTableState(result.getSecond().getSecond(), null, viewModePropName, null, new HashMap<QName, String>()));

        setBelongsToSubPropertySheet(Collections.singletonList(item), hierarchy);
        items.put(item.getName(), item);
        return item;
    }

    private ItemConfigVO generateInline(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, QName[] hierarchy, List<Field> relatedFields,
            Field primaryField, String displayLabel, String textId) {

        Pair<ItemConfigVO, Pair<String, List<QName>>> result = generateBasePropsItem(generatorResults, items, hierarchy, relatedFields, primaryField, displayLabel);

        ItemConfigVO item = result.getFirst();
        item.setComponentGenerator("InlinePropertyGroupGenerator");
        item.setTextId(textId);
        item.setProps(result.getSecond().getFirst());

        return item;
    }

    private Pair<ItemConfigVO, Pair<String, List<QName>>> generateBasePropsItem(FieldGroupGeneratorResults generatorResults, Map<String, ItemConfigVO> items, QName[] hierarchy,
            List<Field> relatedFields, Field primaryField, String displayLabel) {
        List<String> props = new ArrayList<String>();
        List<QName> propNames = new ArrayList<QName>();
        Pair<Map<String, ItemConfigVO>, Map<String, PropertySheetStateHolder>> columnItemsAndStateHolders = generatorResults.generateItems(relatedFields
                .toArray(new Field[relatedFields.size()]));
        Map<String, ItemConfigVO> columnItems = columnItemsAndStateHolders.getFirst();
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
                    columnItem.setStyleClass(styleClass);
                }
            }

            String prop = columnItem.toPropString(PropsBuilder.DEFAULT_OPTIONS_SEPARATOR);
            Assert.isTrue(!StringUtils.contains(prop, CombinedPropReader.AttributeNames.DEFAULT_PROPERTIES_SEPARATOR));
            props.add(prop);
            propNames.add(QName.resolveToQName(namespaceService, columnItem.getName()));
        }
        String propsString = StringUtils.join(props, CombinedPropReader.AttributeNames.DEFAULT_PROPERTIES_SEPARATOR);

        ItemConfigVO item = generatorResults.generateItemBase(primaryField);
        item.setName(RepoUtil.createTransientProp(primaryField.getFieldId()).toString());
        item.setDisplayLabel(displayLabel);
        item.setOptionsSeparator(PropsBuilder.DEFAULT_OPTIONS_SEPARATOR);
        setBelongsToSubPropertySheet(Collections.singletonList(item), hierarchy);
        items.put(item.getName(), item);
        return Pair.newInstance(item, Pair.newInstance(propsString, propNames));
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
            QName[] hierarchy, Field... fields) {
        Pair<Map<String, ItemConfigVO>, Map<String, PropertySheetStateHolder>> result = generatorResults.generateItems(fields);
        Map<String, ItemConfigVO> generatedItems = result.getFirst();
        Map<String, PropertySheetStateHolder> generatedStateHolders = result.getSecond();
        Assert.isTrue(!CollectionUtils.containsAny(items.keySet(), generatedItems.keySet()));
        Assert.isTrue(!CollectionUtils.containsAny(stateHolders.keySet(), generatedStateHolders.keySet()));
        setBelongsToSubPropertySheet(generatedItems.values(), hierarchy);
        items.putAll(generatedItems);
        stateHolders.putAll(generatedStateHolders);
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

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

}
