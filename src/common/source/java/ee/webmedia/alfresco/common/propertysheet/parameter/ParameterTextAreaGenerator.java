package ee.webmedia.alfresco.common.propertysheet.parameter;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

public class ParameterTextAreaGenerator extends TextAreaGenerator {

    private static final String PARAMETER_NAME = "parameterName";
    private ParametersService parametersService;

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        super.setupProperty(context, propertySheet, item, propertyDef, component);
        if (component instanceof UIInput) {
            UIInput uiInput = (UIInput) component;
            if (uiInput.getValue() == null) {
                uiInput.setValue(parametersService.getStringParameter(Parameters.get(getParameterName())));
            }
        }
    }

    private String getParameterName() {
        return getCustomAttributes().get(PARAMETER_NAME);
    }

    // START: getters / setters
    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }
    // END: getters / setters
}
