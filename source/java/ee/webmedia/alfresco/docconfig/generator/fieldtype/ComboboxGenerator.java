package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

public class ComboboxGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.COMBOBOX, FieldType.LISTBOX };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        item.setComponentGenerator("ClassificatorSelectorGenerator");
        // Should only happen on special systematic fields: function, series, volume
        if (field.getClassificator() != null) {
            item.setClassificatorName(field.getClassificator());
        }
    }
}
