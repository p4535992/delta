package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;

import javax.faces.convert.LongConverter;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.ui.repo.RepoConstants;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public class LongGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.LONG };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (!field.isForSearch()) {
            item.setComponentGenerator(RepoConstants.GENERATOR_TEXT_FIELD);
            item.setStyleClass("medium");
            item.setConverter(LongConverter.CONVERTER_ID);
            return;
        }
        QName qnameBegin = field.getQName();
        QName qnameEnd = DoubleGenerator.getEndNumberQName(qnameBegin);
        item.setComponentGenerator("InlinePropertyGroupGenerator");
        NamespaceService namespaceService = getNamespaceService();
        item.setProps(qnameBegin.toPrefixString(namespaceService) + "|" + RepoConstants.GENERATOR_TEXT_FIELD + "|styleClass=medium|converter= " + LongConverter.CONVERTER_ID
                + ","
                + qnameEnd.toPrefixString(namespaceService) + "|" + RepoConstants.GENERATOR_TEXT_FIELD + "|styleClass=medium|converter= " + LongConverter.CONVERTER_ID
                );
        item.setTextId("document_search_from_to");

    }

}
