package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.ui.repo.RepoConstants;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.enums.DatePeriods;
import ee.webmedia.alfresco.common.propertysheet.classificatorselector.EnumSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

/**
 * @author Alar Kvell
 */
public class DateGenerator extends BaseTypeFieldGenerator {

    public static final String END_PREFIX = "_EndDate";
    public static final String PICKER_PREFIX = "_DateRangePicker";

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.DATE };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (!field.isForSearch()) {
            item.setComponentGenerator(RepoConstants.GENERATOR_DATE_PICKER);
            return;
        }
        /**
         * example from xml
         * <show-property name="docsearch:invoiceDateBegin" display-label-id="document_invoiceDate" component-generator="InlinePropertyGroupGenerator"
         * props="docsearch:invoiceDateBegin||styleClass=date,docsearch:invoiceDateEnd||styleClass=date" textId="document_search_from_to" />
         */
        QName qnameBegin = field.getQName();
        QName qnameEnd = getEndDateQName(qnameBegin);
        QName qnamePicker = getDatePickerQName(qnameBegin);
        item.setComponentGenerator("InlinePropertyGroupGenerator");
        NamespaceService namespaceService = getNamespaceService();
        item.setProps(qnameBegin.toPrefixString(namespaceService) + "|DatePickerGenerator|styleClass=date,"
                + qnameEnd.toPrefixString(namespaceService) + "|DatePickerGenerator|styleClass=date,"
                 + qnamePicker.toPrefixString(namespaceService) + "|EnumSelectorGenerator|" + EnumSelectorGenerator.ATTR_DISABLE_SORTING
                + "=true|styleClass=quickDateRangePicker|enumClass="
                + DatePeriods.class.getCanonicalName()
                );
        item.setTextId("document_search_from_to_with_picker");
    }

    public static QName getEndDateQName(QName propQname) {
        return QName.createQName(propQname.getNamespaceURI(), propQname.getLocalName() + END_PREFIX);
    }

    public static QName getDatePickerQName(QName propQname) {
        return QName.createQName(propQname.getNamespaceURI(), propQname.getLocalName() + PICKER_PREFIX);
    }

}
