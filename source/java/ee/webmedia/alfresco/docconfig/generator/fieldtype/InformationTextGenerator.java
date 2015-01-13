package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

public class InformationTextGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.INFORMATION_TEXT };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        item.setConfigItemType(ConfigItemType.SEPARATOR);
        item.setComponentGenerator("InformationTextGenerator");
        item.setDisplayLabel(field.getDefaultValue());
    }

}
