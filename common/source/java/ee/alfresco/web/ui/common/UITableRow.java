<<<<<<< HEAD
package ee.alfresco.web.ui.common;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

import org.apache.myfaces.shared_impl.renderkit.html.HTML;

import ee.alfresco.web.ui.common.renderer.SimpleHtmlElementRenderer;

/**
 * Component representing table row
 * @author Ats Uiboupin
 */
public class UITableRow extends UIComponentBase {

    @Override
    protected Renderer getRenderer(FacesContext context) {
        SimpleHtmlElementRenderer renderer = new SimpleHtmlElementRenderer();
        renderer.setStartElement(HTML.TR_ELEM);
        return renderer;
    }

    @Override
    public String getFamily() {
        return null;
    }

}
=======
package ee.alfresco.web.ui.common;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

import org.apache.myfaces.shared_impl.renderkit.html.HTML;

import ee.alfresco.web.ui.common.renderer.SimpleHtmlElementRenderer;

/**
 * Component representing table row
 */
public class UITableRow extends UIComponentBase {

    @Override
    protected Renderer getRenderer(FacesContext context) {
        SimpleHtmlElementRenderer renderer = new SimpleHtmlElementRenderer();
        renderer.setStartElement(HTML.TR_ELEM);
        return renderer;
    }

    @Override
    public String getFamily() {
        return null;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
