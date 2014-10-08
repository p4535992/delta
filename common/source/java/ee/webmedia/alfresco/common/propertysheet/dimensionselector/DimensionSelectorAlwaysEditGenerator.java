<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.dimensionselector;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.HandlesViewMode;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Generator for a dimension selector which is editable even if the propertySheet is not in edit mode.
 * 
 * @author Riina Tens
 */
public class DimensionSelectorAlwaysEditGenerator extends DimensionSelectorGenerator implements HandlesViewMode {

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        super.setupProperty(context, propertySheet, item, propertyDef, component);
        unsetReadOnly(component);
    }

    private void unsetReadOnly(UIComponent component) {
        ComponentUtil.putAttribute(component, "readonly", Boolean.FALSE);
        ComponentUtil.putAttribute(component, ComponentUtil.IS_ALWAYS_EDIT, Boolean.TRUE);
        List<UIComponent> children = ComponentUtil.getChildren(component);
        if (children == null) {
            return;
        }
        for (UIComponent childComponent : children) {
            unsetReadOnly(childComponent);
        }
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.dimensionselector;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.HandlesViewMode;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Generator for a dimension selector which is editable even if the propertySheet is not in edit mode.
 */
public class DimensionSelectorAlwaysEditGenerator extends DimensionSelectorGenerator implements HandlesViewMode {

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        super.setupProperty(context, propertySheet, item, propertyDef, component);
        unsetReadOnly(component);
    }

    private void unsetReadOnly(UIComponent component) {
        ComponentUtil.putAttribute(component, "readonly", Boolean.FALSE);
        ComponentUtil.putAttribute(component, ComponentUtil.IS_ALWAYS_EDIT, Boolean.TRUE);
        List<UIComponent> children = ComponentUtil.getChildren(component);
        if (children == null) {
            return;
        }
        for (UIComponent childComponent : children) {
            unsetReadOnly(childComponent);
        }
    }

}
>>>>>>> develop-5.1
