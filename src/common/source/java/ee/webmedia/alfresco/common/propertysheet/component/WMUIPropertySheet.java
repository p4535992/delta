package ee.webmedia.alfresco.common.propertysheet.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

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

    public static final String SHOW = "show";

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

    @Override
    protected void createComponentsFromConfig(FacesContext context, Collection<ItemConfig> items) throws IOException {
        List<ItemConfig> filteredItems = new ArrayList<ItemConfig>(items.size());
        for (ItemConfig item : items) {
            if (item instanceof CustomAttributes) {
                String show = ((CustomAttributes) item).getCustomAttributes().get(SHOW);
                if (show != null) {
                    ValueBinding vb = context.getApplication().createValueBinding(show);
                    Boolean value = (Boolean) vb.getValue(context);
                    if (value != null && !value) {
                        continue;
                    }
                }
            }
            filteredItems.add(item);
        }
        super.createComponentsFromConfig(context, filteredItems);
    }

}
