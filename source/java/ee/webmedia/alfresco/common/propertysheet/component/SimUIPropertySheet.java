package ee.webmedia.alfresco.common.propertysheet.component;

import ee.webmedia.alfresco.common.propertysheet.renderkit.PropertySheetGridRenderer;

/**
 * Subclass to overwrite renderer type.
 */
public class SimUIPropertySheet extends WMUIPropertySheet {

    @Override
    public String getRendererType() {
        return PropertySheetGridRenderer.class.getCanonicalName();
    }

}
