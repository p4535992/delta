package ee.webmedia.alfresco.common.propertysheet.generator;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

/**
 * Generates <div class="message">PROP_VALUE_FROM_NODE</div> message box.
 * 
 * @author Erko Hansar
 */
public class PropValueSeparatorGenerator extends BaseComponentGenerator {

    @Override
    public UIComponent generate(FacesContext context, String id) {
        throw new RuntimeException("This is never called!");
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item) {
        UIComponent component = this.createOutputTextComponent(context, item.getName());      
        Object propValue = propertySheet.getNode().getProperties().get(item.getName());
        if (propValue != null && propValue.toString().length() > 0) {
            component.getAttributes().put("escape", Boolean.FALSE);
            component.getAttributes().put("value", "<div class=\"message\">" + propValue + "</div>");
        }
        else {
            component.setRendered(false);
        }
        return component;
    }

}
