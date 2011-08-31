package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

/**
 * @author Alar Kvell
 */
public class ComboboxEditableGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.COMBOBOX_EDITABLE };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        if (StringUtils.isBlank(field.getClassificator())) {
            item.setComponentGenerator("TextAreaGenerator");
            item.setStyleClass("expand19-200");
        } else {
            item.setComponentGenerator("SuggesterGenerator");
        }
    }

}
