package ee.webmedia.alfresco.common.ajax;

import javax.faces.context.FacesContext;

/**
 * If component implements AjaxUpdateable and also contains child components, then this component should also implement NamingContainer.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
