package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import javax.faces.convert.IntegerConverter;

import org.alfresco.web.ui.repo.RepoConstants;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

/**
 * @author Alar Kvell
 */
public class IntGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.INT };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        item.setComponentGenerator(RepoConstants.GENERATOR_TEXT_FIELD);
        item.setStyleClass("medium");
        item.setConverter(IntegerConverter.CONVERTER_ID);
    }

}
