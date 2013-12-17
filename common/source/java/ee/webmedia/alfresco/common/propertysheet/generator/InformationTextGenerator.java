package ee.webmedia.alfresco.common.propertysheet.generator;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

/**
 * @author Alar Kvell
 */
public class InformationTextGenerator extends BaseComponentGenerator {

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item) {
        UIComponent component = createOutputTextComponent(context, getDefaultId(item));
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put("escape", Boolean.FALSE);
        attributes.put("value", "<div class=\"message\">" + Utils.encode(item.getDisplayLabel()) + "</div>");
        return component;
    }

    @Override
    public UIComponent generate(FacesContext context, String id) {
        // Not used
        return null;
    }

}
