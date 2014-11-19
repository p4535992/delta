<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.ajaxcapablepanelgroup;

import javax.faces.component.NamingContainer;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;

import ee.webmedia.alfresco.common.ajax.AjaxUpdateable;

/**
 * AjaxCapablePanelGroup can be used for generating ajax capable sections in page without altering page formatting.
 * Renders itself as span tag with id.
 * 
 * @author Riina Tens
 */
public class AjaxCapablePanelGroup extends HtmlPanelGroup implements AjaxUpdateable, NamingContainer {

    public static final String AJAX_CAPABLE_PANEL_GROUP_COMPONENT_TYPE = AjaxCapablePanelGroup.class.getCanonicalName();

    @Override
    public String getAjaxClientId(FacesContext context) {
        return getClientId(getFacesContext());
    }

}
=======
package ee.webmedia.alfresco.common.propertysheet.ajaxcapablepanelgroup;

import javax.faces.component.NamingContainer;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;

import ee.webmedia.alfresco.common.ajax.AjaxUpdateable;

/**
 * AjaxCapablePanelGroup can be used for generating ajax capable sections in page without altering page formatting.
 * Renders itself as span tag with id.
 */
public class AjaxCapablePanelGroup extends HtmlPanelGroup implements AjaxUpdateable, NamingContainer {

    public static final String AJAX_CAPABLE_PANEL_GROUP_COMPONENT_TYPE = AjaxCapablePanelGroup.class.getCanonicalName();

    @Override
    public String getAjaxClientId(FacesContext context) {
        return getClientId(getFacesContext());
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
