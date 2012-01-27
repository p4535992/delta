package ee.webmedia.alfresco.docconfig.generator.fieldtype;

import org.alfresco.web.ui.repo.RepoConstants;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.generator.BaseTypeFieldGenerator;
import ee.webmedia.alfresco.docconfig.generator.GeneratorResults;

/**
 * @author Alar Kvell
 */
public class TextFieldGenerator extends BaseTypeFieldGenerator {

    @Override
    protected FieldType[] getFieldTypes() {
        return new FieldType[] { FieldType.TEXT_FIELD };
    }

    @Override
    public void generateField(Field field, GeneratorResults generatorResults) {
        final ItemConfigVO item = generatorResults.getAndAddPreGeneratedItem();
        String datafieldParamName = field.getDatafieldParamName();
        if (StringUtils.isNotBlank(datafieldParamName)) {
            item.setComponentGenerator("ParameterInputAttributeGenerator");
            item.setParameterName(datafieldParamName);
        } else {
            item.setComponentGenerator(RepoConstants.GENERATOR_TEXT_AREA);
        }
        item.setStyleClass("expand19-200");
    }

}
