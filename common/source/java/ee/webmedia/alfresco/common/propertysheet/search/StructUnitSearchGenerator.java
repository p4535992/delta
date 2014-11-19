<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.search;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;

/**
 * @author Riina Tens
 */
public class StructUnitSearchGenerator extends SearchGenerator {

    public static final String IS_MULTIVALUE_EDITOR_COMPONENT_KEY = "isMultivalueEditorComponent";

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIComponent component = context.getApplication().createComponent(StructUnitSearch.STRUCT_UNIT_SEARCH_FAMILY);
        FacesHelper.setupComponentId(context, component, id);
        component.setRendererType(SearchRenderer.SEARCH_RENDERER_TYPE);
        return component;
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.search;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;

public class StructUnitSearchGenerator extends SearchGenerator {

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIComponent component = context.getApplication().createComponent(StructUnitSearch.STRUCT_UNIT_SEARCH_FAMILY);
        FacesHelper.setupComponentId(context, component, id);
        component.setRendererType(SearchRenderer.SEARCH_RENDERER_TYPE);
        return component;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
