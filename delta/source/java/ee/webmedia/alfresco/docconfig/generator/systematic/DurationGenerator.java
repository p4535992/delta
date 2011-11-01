package ee.webmedia.alfresco.docconfig.generator.systematic;

import static ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel.Props.BEGIN_DATE;
import static ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel.Props.CALCULATED_DURATION_DAYS;
import static ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel.Props.END_DATE;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.event.ValueChangeEvent;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.PropertySheetElementReader;
import org.apache.commons.lang.StringUtils;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.YearMonthDay;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.service.FieldGroup;
import ee.webmedia.alfresco.docconfig.generator.BasePropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.BaseSystematicFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public class DurationGenerator extends BaseSystematicFieldGenerator {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DurationGenerator.class);

    private NamespaceService namespaceService;

    @Override
    protected String[] getOriginalFieldIds() {
        return new String[] {
                BEGIN_DATE.getLocalName(),
                END_DATE.getLocalName(),
                CALCULATED_DURATION_DAYS.getLocalName() };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        // Can be used outside systematic field group - then additional functionality is not present
        if (!(field.getParent() instanceof FieldGroup) || !((FieldGroup) field.getParent()).isSystematic()) {
            generatorResults.getAndAddPreGeneratedItem();
            return;
        }

        FieldGroup group = (FieldGroup) field.getParent();
        Map<String, Field> fieldsByOriginalId = group.getFieldsByOriginalId();
        QName beginDateProp = getProp(fieldsByOriginalId, BEGIN_DATE);
        QName endDateProp = getProp(fieldsByOriginalId, END_DATE);
        QName calculatedDaysProp = getProp(fieldsByOriginalId, CALCULATED_DURATION_DAYS);
        String stateHolderKey = beginDateProp.getLocalName();

        // TODO what mandatory value?
        // TODO read-only/if value - praegu ainult beginDate järgi mõlemad?

        if (field.getQName().equals(beginDateProp)) {
            final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
            item.setComponentGenerator("InlinePropertyGroupGenerator");
            item.setName(RepoUtil.createTransientProp(field.getFieldId()).toString());
            item.setDisplayLabel(group.getReadonlyFieldsName());
            item.setOptionsSeparator("¤");
            List<String> components = new ArrayList<String>();

            String beginDateComponent = beginDateProp.toPrefixString(namespaceService) + "¤¤styleClass=date inline";
            if (calculatedDaysProp != null) {
                beginDateComponent += "¤valueChangeListener=" + getBindingName("beginDateValueChanged", stateHolderKey);
            }
            if (item.getCustomAttributes().containsKey(BaseComponentGenerator.READONLY_IF)) {
                beginDateComponent += "¤readOnlyIf=" + item.getCustomAttributes().get(BaseComponentGenerator.READONLY_IF);
            }
            if (item.getCustomAttributes().containsKey(PropertySheetElementReader.ATTR_READ_ONLY)) {
                beginDateComponent += "¤read-only=" + item.getCustomAttributes().get(PropertySheetElementReader.ATTR_READ_ONLY);
            }
            components.add(beginDateComponent);

            String endDateComponent = endDateProp.toPrefixString(namespaceService) + "¤¤styleClass=date inline";
            if (calculatedDaysProp != null) {
                endDateComponent += "¤valueChangeListener=" + getBindingName("endDateValueChanged", stateHolderKey);
            }
            if (item.getCustomAttributes().containsKey(BaseComponentGenerator.READONLY_IF)) {
                endDateComponent += "¤readOnlyIf=" + item.getCustomAttributes().get(BaseComponentGenerator.READONLY_IF);
            }
            if (item.getCustomAttributes().containsKey(PropertySheetElementReader.ATTR_READ_ONLY)) {
                endDateComponent += "¤read-only=" + item.getCustomAttributes().get(PropertySheetElementReader.ATTR_READ_ONLY);
            }
            components.add(endDateComponent);

            if (calculatedDaysProp != null) {
                item.setTextId("document_eventDatesWithDuration_templateText");
                String calculatedDaysComponent = calculatedDaysProp.toPrefixString(namespaceService) + "¤¤read-only=true¤styleClass=tiny inline center";
                components.add(calculatedDaysComponent);
                generatorResults.addStateHolder(stateHolderKey, new DurationState(beginDateProp, endDateProp, calculatedDaysProp));
            } else {
                item.setTextId("document_eventDates_templateText");
            }
            item.setProps(StringUtils.join(components, ','));
        }
    }

    private QName getProp(Map<String, Field> fieldsByOriginalId, QName propName) {
        Field field = fieldsByOriginalId.get(propName.getLocalName());
        return field == null ? null : field.getQName();
    }

    // ===============================================================================================================================

    public static class DurationState extends BasePropertySheetStateHolder {
        private static final long serialVersionUID = 1L;

        private final QName beginDateProp;
        private final QName endDateProp;
        private final QName calculatedDaysProp;

        public DurationState(QName beginDateProp, QName endDateProp, QName calculatedDaysProp) {
            this.beginDateProp = beginDateProp;
            this.endDateProp = endDateProp;
            this.calculatedDaysProp = calculatedDaysProp;
        }

        public void beginDateValueChanged(ValueChangeEvent event) {
            Node document = dialogDataProvider.getNode();
            Date beginDate = (Date) event.getNewValue();
            Date endDate = (Date) document.getProperties().get(endDateProp);
            updateCalculatedDays(beginDate, endDate);
        }

        public void endDateValueChanged(ValueChangeEvent event) {
            Node document = dialogDataProvider.getNode();
            Date beginDate = (Date) document.getProperties().get(beginDateProp);
            Date endDate = (Date) event.getNewValue();
            updateCalculatedDays(beginDate, endDate);
        }

        private void updateCalculatedDays(Date beginDate, Date endDate) {
            Node document = dialogDataProvider.getNode();
            Integer calculatedDays = null;
            if (beginDate != null && endDate != null) {
                calculatedDays = Math.round((float) Math.random() * 10000);
                YearMonthDay begin = new YearMonthDay(beginDate.getTime());
                YearMonthDay end = new YearMonthDay(endDate.getTime());
                calculatedDays = Math.abs(new Period(begin, end, PeriodType.days()).getDays()) + 1;
            }
            document.getProperties().put(calculatedDaysProp.toString(), calculatedDays);
        }

    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

}
