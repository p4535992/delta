package ee.webmedia.alfresco.common.propertysheet.generator;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.HandlesViewMode;

/**
 * Generator for a text area which is editable even if the propertySheet is not in edit mode.
 * Implements HandlesViewMode
 */
public class TextAreaAlwaysEditGenerator extends TextAreaGenerator implements HandlesViewMode {

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        super.setupProperty(context, propertySheet, item, propertyDef, component);
        unsetReadOnly(component);
    }

    @SuppressWarnings("unchecked")
    private void unsetReadOnly(UIComponent component) {
        component.getAttributes().put("readonly", Boolean.FALSE);
        List<UIComponent> children = component.getChildren();
        if (children == null) {
            return;
        }
        for (UIComponent childComponent : children) {
            unsetReadOnly(childComponent);
        }
    }

}
