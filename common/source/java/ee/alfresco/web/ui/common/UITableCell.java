package ee.alfresco.web.ui.common;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

import org.apache.myfaces.shared_impl.renderkit.html.HTML;

import ee.alfresco.web.ui.common.renderer.SimpleHtmlElementRenderer;

/**
 * Component representing table cell
 */
public class UITableCell extends UIComponentBase {

    @Override
    protected Renderer getRenderer(FacesContext context) {
        SimpleHtmlElementRenderer renderer = new SimpleHtmlElementRenderer();
        renderer.setStartElement(HTML.TD_ELEM);
        return renderer;
    }

    @Override
    public String getFamily() {
        return null;
    }

}
