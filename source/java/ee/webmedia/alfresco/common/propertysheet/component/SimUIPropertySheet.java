<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.component;

import ee.webmedia.alfresco.common.propertysheet.renderkit.PropertySheetGridRenderer;

/**
 * Subclass to overwrite renderer type.
 * 
 * @author Erko Hansar
 */
public class SimUIPropertySheet extends WMUIPropertySheet {

    @Override
    public String getRendererType() {
        return PropertySheetGridRenderer.class.getCanonicalName();
    }

}
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
