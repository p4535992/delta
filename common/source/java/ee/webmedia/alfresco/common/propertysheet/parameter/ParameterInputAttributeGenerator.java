package ee.webmedia.alfresco.common.propertysheet.parameter;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.bean.generator.TextFieldGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

/**
 * Adds parameter value to input datafld attribute.
 * NB! Currently supports only double parameters!
 */
public class ParameterInputAttributeGenerator extends TextFieldGenerator {

    public static final String PARAMETER_NAME = "parameterName";
    private ParametersService parametersService;

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        super.setupProperty(context, propertySheet, item, propertyDef, component);
        Map<String, Object> attributes = component.getAttributes();
        attributes.put("datafld", getParameterValue().toString());
    }

    private Double getParameterValue() {
        return parametersService.getDoubleParameter(Parameters.get(getCustomAttributes().get(PARAMETER_NAME)));
    }

    // START: getters / setters
    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }
    // END: getters / setters

}
