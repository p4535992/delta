package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import org.alfresco.web.ui.repo.RepoConstants;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.converter.DoubleCurrencyConverter_ET_EN;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

/**
 * @author Alar Kvell
 */
public class DoubleGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.DOUBLE };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        item.setComponentGenerator(RepoConstants.GENERATOR_TEXT_FIELD);
        item.setStyleClass("medium");
        item.setConverter(DoubleCurrencyConverter_ET_EN.class.getName());
        item.setAllowCommaAsDecimalSeparator(true);
    }

}
