package ee.webmedia.alfresco.common.propertysheet.component;

import org.alfresco.web.config.PropertySheetConfigElement.ItemConfig;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;

/**
 * Subclass of UIPropertySheet that copies custom attributes from property-sheet/show-property element to configuration item.
 * 
 * @author Ats Uiboupin
 */
public class WMUIPropertySheet extends UIPropertySheet {
    /**
     * Default constructor
     */
    public WMUIPropertySheet() {
        super();
    }

    @Override
    protected void changePropSheetItem(ItemConfig item, PropertySheetItem propSheetItem) {
        // if both can have custom attributes, then set them from item to propSheetItem
        if (item instanceof CustomAttributes && propSheetItem instanceof CustomAttributes) {
            CustomAttributes wMPropertyConfig = (CustomAttributes) item;
            CustomAttributes wmPropSheetItem = (CustomAttributes) propSheetItem;
            wmPropSheetItem.setCustomAttributes(wMPropertyConfig.getCustomAttributes());
        }
    }
    
    protected String getPostProcessFunctionCall() {
        return "if(typeof postProcessButtonState == 'function') postProcessButtonState();\n";
    }

}
