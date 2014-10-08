package ee.webmedia.alfresco.docconfig.generator.systematic;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.convert.LongConverter;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.RepoConstants;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.constant.FieldChangeableIf;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.model.DynamicBase;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.converter.DoubleCurrencyConverter_ET_EN;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicFieldGroupNames;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.utils.RepoUtil;

<<<<<<< HEAD
/**
 * @author Priit Pikk
 */
=======
>>>>>>> develop-5.1
public class ExpensesTableGenerator extends BaseSystematicFieldGenerator {

    private NamespaceService namespaceService;
    private final Set<String> singleFields = new HashSet<String>(10);
    private final Set<String> sumFields = new HashSet<String>(10);
    private String[] originalFieldIds;

    @Override
    public void afterPropertiesSet() {
        Set<String> fields = new HashSet<String>();

        Set<String> expensesMapping = new HashSet<String>();
        expensesMapping.add("expenseTypeChoice");
        expensesMapping.add("financingSourceChoice");
        expensesMapping.add("payingReasonAndExtent");
        expensesMapping.add("errandExpectedExpense");
        fields.addAll(expensesMapping);

        Set<String> errandExpensesReportMapping = new HashSet<String>();
        errandExpensesReportMapping.add("expenseTypeChoice");
        errandExpensesReportMapping.add("errandReportPaymentMethod");
        errandExpensesReportMapping.add("errandReportSum");
        fields.addAll(errandExpensesReportMapping);
        singleFields.add("errandReportTotalSum");
        sumFields.add("errandReportSum");

        Set<String> errandExpensesReportSummaryMapping = new HashSet<String>();
        errandExpensesReportSummaryMapping.add("expenseTypeChoice");
        errandExpensesReportSummaryMapping.add("errandSummaryProject");
        errandExpensesReportSummaryMapping.add("errandSummaryDepartment");
        errandExpensesReportSummaryMapping.add("financingSource");
        errandExpensesReportSummaryMapping.add("errandSummaryExpenseItem");
        errandExpensesReportSummaryMapping.add("errandSummaryAccount");
        errandExpensesReportSummaryMapping.add("errandSummaryDebit");
        errandExpensesReportSummaryMapping.add("errandSummaryCredit");
        fields.addAll(errandExpensesReportSummaryMapping);
        singleFields.add("errandSummaryDebitTotal");
        singleFields.add("errandSummaryCreditTotal");
        sumFields.add("errandSummaryDebit");
        sumFields.add("errandSummaryCredit");

        singleFields.add("driveTotalKm");
        singleFields.add("driveCompensationRate");
        singleFields.add("driveTotalCompensation");
        sumFields.add("driveTotalCompensation");

        fields.addAll(singleFields);

        Set<String> errandExpensesReportFieldIds = new HashSet<String>();
        errandExpensesReportFieldIds.add("expenseTypeChoice");
        errandExpensesReportFieldIds.add("errandReportPaymentMethod");
        errandExpensesReportFieldIds.add("errandReportSum");
        errandExpensesReportFieldIds.add("errandReportTotalSum");

        documentConfigService.registerMultiValuedOverrideBySystematicGroupName(SystematicFieldGroupNames.ERRAND_EXPENSES, expensesMapping);
        documentConfigService.registerMultiValuedOverrideBySystematicGroupName(SystematicFieldGroupNames.ERRAND_EXPENSES_REPORT, errandExpensesReportMapping);
        documentConfigService.registerMultiValuedOverrideBySystematicGroupName(SystematicFieldGroupNames.ERRAND_EXPENSES_REPORT_SUMMARY,
                errandExpensesReportSummaryMapping);

        originalFieldIds = fields.toArray(new String[fields.size()]);
        super.afterPropertiesSet();
    }

    @Override
    protected String[] getOriginalFieldIds() {
        return originalFieldIds;
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        // Can be used outside systematic field group - then additional functionality is not present
        if (!(field.getParent() instanceof FieldGroup) || !((FieldGroup) field.getParent()).isSystematic()) {
            generatorResults.getAndAddPreGeneratedItem();
            return;
        }
        FieldGroup group = (FieldGroup) field.getParent();
        String groupName = group.getName();
        // ErrandGenerator also uses financingSource field and in that case additional processing is not needed
        if ("financingSource".equals(field.getOriginalFieldId())
                && (SystematicFieldGroupNames.ERRAND_DOMESTIC_APPLICANT.equals(groupName) || SystematicFieldGroupNames.ERRAND_ABROAD_APPLICANT.equals(groupName))) {
            generatorResults.getAndAddPreGeneratedItem();
            return;
        }
        if (singleFields.contains(field.getOriginalFieldId())) {
            ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
            item.setStyleClass("small " + field.getQName().getLocalName() + "Field");
            if (DocumentDynamicModel.Props.DRIVE_TOTAL_COMPENSATION.getLocalName().equals(field.getOriginalFieldId())) {
                ExpensesTableState primaryStateHolder = new ExpensesTableState();
                if (FieldChangeableIf.ALWAYS_NOT_CHANGEABLE.equals(field.getChangeableIfEnum())) {
                    generatorResults.addStateHolder(field.getFieldId(), primaryStateHolder);
                    primaryStateHolder.driveTotalCompensationField = field.getQName();
                    Field driveTotalKM = group.getFieldsByOriginalId().get(DocumentDynamicModel.Props.DRIVE_TOTAL_KM.getLocalName());
                    if (driveTotalKM != null) {
                        primaryStateHolder.driveTotalKMField = driveTotalKM.getQName();
                    }
                    Field driveCompRate = group.getFieldsByOriginalId().get(DocumentDynamicModel.Props.DRIVE_COMPENSATION_RATE.getLocalName());
                    if (driveCompRate != null) {
                        primaryStateHolder.driveCompRateField = driveCompRate.getQName();
                    }
                }
            }
            return;
        }

        if (field != group.getFields().get(0)) {
            return;
        }

        ExpensesTableState primaryStateHolder = new ExpensesTableState();
        generatorResults.addStateHolder(field.getFieldId(), primaryStateHolder);
        List<String> properties = new ArrayList<String>();
        for (Field child : group.getFields()) {
            if (DocumentDynamicModel.Props.ERRAND_REPORT_SUM.getLocalName().equals(child.getOriginalFieldId())) {
                primaryStateHolder.errandReportSumField = child.getQName();
            }
            if (DocumentDynamicModel.Props.ERRAND_REPORT_TOTAL_SUM.getLocalName().equals(child.getOriginalFieldId())) {
                primaryStateHolder.errandReportTotalSumField = child.getQName();
            }
            if (DocumentDynamicModel.Props.ERRAND_SUMMARY_CREDIT.getLocalName().equals(child.getOriginalFieldId())) {
                primaryStateHolder.errandSummaryCreditField = child.getQName();
            }
            if (DocumentDynamicModel.Props.ERRAND_SUMMARY_CREDIT_TOTAL.getLocalName().equals(child.getOriginalFieldId())) {
                primaryStateHolder.errandSummaryCreditTotalField = child.getQName();
            }
            if (DocumentDynamicModel.Props.ERRAND_SUMMARY_DEBIT.getLocalName().equals(child.getOriginalFieldId())) {
                primaryStateHolder.errandSummaryDebitField = child.getQName();
            }
            if (DocumentDynamicModel.Props.ERRAND_SUMMARY_DEBIT_TOTAL.getLocalName().equals(child.getOriginalFieldId())) {
                primaryStateHolder.errandSummaryDebitTotalField = child.getQName();
            }
            QName fieldId = child.getQName();
            if (singleFields.contains(child.getOriginalFieldId())) {
                continue;
            }
            String componentGeneratorAndProps;
            if (FieldType.TEXT_FIELD.equals(child.getFieldTypeEnum())) {
                componentGeneratorAndProps = RepoConstants.GENERATOR_TEXT_AREA + "¤styleClass=expand19-200 small";// expand19-200";
            } else if (FieldType.COMBOBOX_EDITABLE.equals(child.getFieldTypeEnum()) || FieldType.COMBOBOX.equals(child.getFieldTypeEnum())) {
                componentGeneratorAndProps = "ClassificatorSelectorGenerator¤styleClass=width120¤classificatorName=" + child.getClassificator();
            } else if (FieldType.LONG.equals(child.getFieldTypeEnum())) {
                componentGeneratorAndProps = RepoConstants.GENERATOR_TEXT_FIELD + "¤styleClass=tiny right¤converter=" + LongConverter.CONVERTER_ID;
            } else if (FieldType.DOUBLE.equals(child.getFieldTypeEnum())) {
                componentGeneratorAndProps = RepoConstants.GENERATOR_TEXT_FIELD + "¤styleClass=tiny right"
                        + (sumFields.contains(child.getOriginalFieldId()) ? " " + child.getOriginalFieldId() + "Field" : "")
                        + "¤converter=" + DoubleCurrencyConverter_ET_EN.class.getName() + "¤" + UIProperty.ALLOW_COMMA_AS_DECIMAL_SEPARATOR_ATTR + "=true";
            } else {
                throw new RuntimeException("FieldType " + child.getFieldTypeEnum() + " is not supported inside a table");
            }
            properties.add(fieldId.toPrefixString(namespaceService) + "¤" + componentGeneratorAndProps);
        }

        ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        item.setName(RepoUtil.createTransientProp(field.getFieldId()).toString());
        item.setDisplayLabel(groupName);
        item.setComponentGenerator("MultiValueEditorGenerator");
        item.setStyleClass("");
        item.setAddLabelId("add");
        item.setPropsGeneration(StringUtils.join(properties, CombinedPropReader.AttributeNames.DEFAULT_PROPERTIES_SEPARATOR));
    }

    // ===============================================================================================================================

    @Override
    public void save(DynamicBase document) {
        if (!(document instanceof DocumentDynamic)) {
            return;
        }

        for (PropertySheetStateHolder propertySheetStateHolder : BeanHelper.getPropertySheetStateBean().getStateHolders().values()) {
            if (propertySheetStateHolder instanceof ExpensesTableState) {
                ((ExpensesTableState) propertySheetStateHolder).calculateValues(document.getNode());
            }
        }
    }

    public static class ExpensesTableState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;
        private QName errandReportSumField;
        private QName errandReportTotalSumField;
        private QName errandSummaryCreditField;
        private QName errandSummaryCreditTotalField;
        private QName errandSummaryDebitField;
        private QName errandSummaryDebitTotalField;
        private QName driveTotalCompensationField;
        private QName driveTotalKMField;
        private QName driveCompRateField;

        private void calculateValues(Node document) {
            Map<String, Object> properties = document.getProperties();
            if (errandReportSumField != null && errandReportTotalSumField != null && properties.containsKey(errandReportSumField.toString())) {
                properties.put(errandReportTotalSumField.toString(),
                        calculateSum((List<Double>) properties.get(errandReportSumField.toString())).doubleValue());
            }
            if (errandSummaryCreditField != null && errandSummaryCreditTotalField != null && properties.containsKey(errandSummaryCreditField.toString())) {
                properties.put(errandSummaryCreditTotalField.toString(),
                        calculateSum((List<Double>) properties.get(errandSummaryCreditField.toString())).doubleValue());
            }
            if (errandSummaryDebitField != null && errandSummaryDebitTotalField != null && properties.containsKey(errandSummaryDebitField.toString())) {
                properties.put(errandSummaryDebitTotalField.toString(),
                        calculateSum((List<Double>) properties.get(errandSummaryDebitField.toString())).doubleValue());
            }
            if (driveTotalCompensationField != null && driveTotalKMField != null && driveCompRateField != null) {
                BigDecimal totalSum = BigDecimal.ZERO;
                Long driveTotalKM = (Long) properties.get(driveTotalKMField.toString());
                Double driveCompRate = (Double) properties.get(driveCompRateField.toString());
                if (driveTotalKM != null && driveCompRate != null) {
                    properties.put(driveTotalCompensationField.toString(),
                            totalSum.add(BigDecimal.valueOf(driveTotalKM))
                                    .multiply(BigDecimal.valueOf(driveCompRate))
                                    .doubleValue());
                }
            }
        }

        private BigDecimal calculateSum(List<Double> sums) {
            BigDecimal totalSum = BigDecimal.ZERO;
            if (sums != null) {
                for (Double expensesSum : sums) {
                    if (expensesSum != null) {
                        totalSum = totalSum.add(BigDecimal.valueOf(expensesSum));
                    }
                }
            }
            return totalSum;
        }
    }

    // ===============================================================================================================================

    // START: setters
    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }
    // END: setters

}
