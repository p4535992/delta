package ee.webmedia.alfresco.common.propertysheet.search;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;

/**
 * @author Keit Tehvan
 */
public class UserSearchGenerator extends SearchGenerator {

    public static final String EXTRA_INFO_TRANSFORMER = "extraInfoTransformer";

    @Override
    protected Map<String, Object> addAttributes(PropertyDefinition propertyDef, UIComponent component) {
        Map<String, Object> attributes = super.addAttributes(propertyDef, component);
        String propName = getCustomAttributes().get("usernameProp");
        attributes.put(EXTRA_INFO_TRANSFORMER, new SubstituteInfoTransformer(propName));
        return attributes;
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIComponent component = super.generate(context, id);
        component.setRendererType(UserSearchRenderer.USER_SEARCH_RENDERER_TYPE);
        return component;
    }
}
