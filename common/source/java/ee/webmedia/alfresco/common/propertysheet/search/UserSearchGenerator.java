package ee.webmedia.alfresco.common.propertysheet.search;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;

<<<<<<< HEAD
/**
 * @author Keit Tehvan
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public class UserSearchGenerator extends SearchGenerator {

    public static final String USERNAME_PROP_ATTR = "usernameProp";
    public static final String EXTRA_INFO_TRANSFORMER = "extraInfoTransformer";

    @Override
    protected Map<String, Object> addAttributes(PropertyDefinition propertyDef, UIComponent component) {
        Map<String, Object> attributes = super.addAttributes(propertyDef, component);
        String propName = getCustomAttributes().get(USERNAME_PROP_ATTR);
        attributes.put(EXTRA_INFO_TRANSFORMER, new SubstituteInfoTransformer(propName));
        return attributes;
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIComponent component = super.generate(context, id);
        component.setRendererType(UserSearchRenderer.USER_SEARCH_RENDERER_TYPE);
        return component;
    }

    @Override
    protected UIOutput createOutputTextComponent(FacesContext context, String id) {
        UIOutput createOutputTextComponent = super.createOutputTextComponent(context, id);
        createOutputTextComponent.setRendererType(UserSearchViewModeRenderer.RENDERER_TYPE);
        String propName = getCustomAttributes().get(USERNAME_PROP_ATTR);
        createOutputTextComponent.getAttributes().put(EXTRA_INFO_TRANSFORMER, new SubstituteInfoTransformer(propName));
        return createOutputTextComponent;
    }
}
