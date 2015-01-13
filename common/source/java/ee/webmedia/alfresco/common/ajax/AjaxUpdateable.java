package ee.webmedia.alfresco.common.ajax;

import javax.faces.context.FacesContext;

/**
 * If component implements AjaxUpdateable and also contains child components, then this component should also implement NamingContainer.
 */
public interface AjaxUpdateable {

    /**
     * @return the HTML element id, that this components defining HTML element has.
     *         Your component must render itself as one HTML element (which can contain child elements), whose "id" attribute must be this.
     *         If your component does not use pure clientId as id for a HTML element, then use this.
     */
    String getAjaxClientId(FacesContext context);

    public static final String AJAX_DISABLED_ATTR = "ajaxDisabled";

}
