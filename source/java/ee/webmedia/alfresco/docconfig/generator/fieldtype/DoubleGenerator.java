package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;
import static ee.webmedia.alfresco.utils.TextUtil.replaceLast;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.ui.repo.RepoConstants;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.converter.DoubleCurrencyConverter_ET_EN;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class DoubleGenerator extends BaseTypeFieldGenerator {
    public static final String END_PREFIX = "_EndNumber";

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.DOUBLE };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (!field.isForSearch()) {
            String datafieldParamName = field.getDatafieldParamName();
            if (StringUtils.isNotBlank(datafieldParamName)) {
                item.setComponentGenerator("ParameterInputAttributeGenerator");
                item.setParameterName(datafieldParamName);
            } else {
                item.setComponentGenerator(RepoConstants.GENERATOR_TEXT_FIELD);
            }
            item.setStyleClass("medium");
            item.setConverter(DoubleCurrencyConverter_ET_EN.class.getName());
            item.setAllowCommaAsDecimalSeparator(true);
            return;
        }
        QName qnameBegin = field.getQName();
        QName qnameEnd = getEndNumberQName(qnameBegin);
        item.setComponentGenerator("InlinePropertyGroupGenerator");
        NamespaceService namespaceService = getNamespaceService();
        item.setProps(qnameBegin.toPrefixString(namespaceService) + "|" + RepoConstants.GENERATOR_TEXT_FIELD
                + "|styleClass=medium|allowCommaAsDecimalSeparator=true|converter=" + DoubleCurrencyConverter_ET_EN.class.getName() + ","
                + qnameEnd.toPrefixString(namespaceService) + "|" + RepoConstants.GENERATOR_TEXT_FIELD + "|styleClass=medium|allowCommaAsDecimalSeparator=true|converter="
                + DoubleCurrencyConverter_ET_EN.class.getName()
                );
        item.setTextId("document_search_from_to");

    }

    public static QName getEndNumberQName(QName propQname) {
        return QName.createQName(propQname.getNamespaceURI(), propQname.getLocalName() + END_PREFIX);
    }

    public static boolean isEndNumber(QName propQName) {
        return propQName.getLocalName().endsWith(END_PREFIX);
    }

    public static QName getOriginalQName(QName propQname) {
        String localName = propQname.getLocalName();
        if (localName.endsWith(END_PREFIX)) {
            localName = replaceLast(localName, END_PREFIX, "");
        }
        return QName.createQName(propQname.getNamespaceURI(), localName);
    }

}
