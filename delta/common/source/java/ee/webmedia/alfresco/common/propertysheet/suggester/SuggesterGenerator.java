package ee.webmedia.alfresco.common.propertysheet.suggester;

import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;

import org.alfresco.util.Pair;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Generator, that generates a TextArea that suggests values (defined using "suggesterValues" attribute in the show-property
 * element, which contains a value-binding ...), and allows to insert value that is not predefined
 * 
 * @author Ats Uiboupin
 */
public class SuggesterGenerator extends TextAreaGenerator {

    private GeneralService generalService;

    public interface ComponentAttributeNames {
        String SUGGESTER_VALUES = "suggesterValues";
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIInput component = (UIInput) super.generate(context, id);
        component.setRendererType(SuggesterRenderer.SUGGESTER_RENDERER_TYPE);

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        String styleClass = (String) attributes.get("styleClass");
        if (StringUtils.isBlank(styleClass)) {
            styleClass = "suggest";
        } else {
            styleClass += " suggest";
        }
        attributes.put("styleClass", styleClass);

        Pair<List<String>, String> suggesterValues = getSuggesterValues(context, component);
        if (suggesterValues != null) {
            setValue(component, suggesterValues.getFirst());
        }

        String existingValue = getGeneralService().getExistingRepoValue4ComponentGenerator();
        if (existingValue == null && suggesterValues != null) {
            existingValue = suggesterValues.getSecond();
        }
        component.setValue(existingValue);

        return component;
    }

    public static void setValue(UIInput component, List<String> list) {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();

        if (list == null) {
            attributes.remove(ComponentAttributeNames.SUGGESTER_VALUES);
            component.setValue(null);
        } else {
            attributes.put(ComponentAttributeNames.SUGGESTER_VALUES, list);
        }
        ComponentUtil.setReadonlyAttributeRecursively(component, list == null);
    }

    public Pair<List<String>, String> getSuggesterValues(FacesContext context, UIInput component) {
        MethodBinding mb = context.getApplication().createMethodBinding(
                getCustomAttributes().get(ComponentAttributeNames.SUGGESTER_VALUES), new Class[] { FacesContext.class, UIInput.class });
        @SuppressWarnings("unchecked")
        List<String> suggesterValues = (List<String>) mb.invoke(context, new Object[] { context, component });
        return new Pair<List<String>, String>(suggesterValues, null);
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
    }

}
