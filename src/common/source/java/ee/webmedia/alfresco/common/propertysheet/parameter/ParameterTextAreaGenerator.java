package ee.webmedia.alfresco.common.propertysheet.parameter;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.alfresco.web.bean.generator.TextAreaGenerator;

import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

public class ParameterTextAreaGenerator extends TextAreaGenerator {
    
    private static final String PARAMETER_NAME = "parameterName";
    private ParametersService parametersService; 
    
    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIInput component = (UIInput) super.generate(context, id);
        component.setValue(parametersService.getStringParameter(Parameters.get(getParameterName())));
        return component;
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
