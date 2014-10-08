package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import org.alfresco.web.ui.repo.RepoConstants;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

public class TextFieldGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.TEXT_FIELD };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        item.setComponentGenerator(RepoConstants.GENERATOR_TEXT_AREA);
        item.setStyleClass("expand19-200");
    }

}
