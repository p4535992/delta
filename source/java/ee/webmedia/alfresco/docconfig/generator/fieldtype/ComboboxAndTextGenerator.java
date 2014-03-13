package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

public class ComboboxAndTextGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.COMBOBOX_AND_TEXT, FieldType.COMBOBOX_AND_TEXT_NOT_EDITABLE };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        item.setComponentGenerator("ClassificatorSelectorAndTextGenerator");
        item.setStyleClass("expand19-200");
        item.setClassificatorName(field.getClassificator());
        if (field.getFieldTypeEnum() == FieldType.COMBOBOX_AND_TEXT_NOT_EDITABLE) {
            item.setNotEditable(true);
        }
    }

}
