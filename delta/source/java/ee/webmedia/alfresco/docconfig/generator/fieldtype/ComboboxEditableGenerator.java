package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import java.util.Collections;
import java.util.List;

import org.alfresco.web.ui.repo.RepoConstants;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

/**
 * @author Alar Kvell
 */
public class ComboboxEditableGenerator extends BaseTypeFieldGenerator {

    private ClassificatorService classificatorService;

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.COMBOBOX_EDITABLE };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        final List<ClassificatorValue> values;
        if (field.getClassificator() == null) {
            // Only "case" field should have this special case
            values = Collections.emptyList();
        } else {
            values = classificatorService.getActiveClassificatorValues(classificatorService.getClassificatorByName(field.getClassificator()));
        }
        if (values.isEmpty()) {
            item.setComponentGenerator(RepoConstants.GENERATOR_TEXT_AREA);
            item.setStyleClass("expand19-200");
        } else {
            item.setComponentGenerator("ClassificatorSuggesterGenerator");
            item.setClassificatorName(field.getClassificator());
        }
    }

    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }

}
