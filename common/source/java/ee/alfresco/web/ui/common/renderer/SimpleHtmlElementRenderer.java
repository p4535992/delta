<<<<<<< HEAD
package ee.alfresco.web.ui.common.renderer;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.renderkit.html.HtmlGroupRenderer;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRendererUtils;

/**
 * more flexible version of {@link HtmlGroupRenderer} that writes id (if needed) and {@link HTML#COMMON_PASSTROUGH_ATTRIBUTES}
 * 
 * @author Ats Uiboupin
 */
public class SimpleHtmlElementRenderer extends HtmlGroupRenderer {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SimpleHtmlElementRenderer.class);

    private String startElement = org.apache.myfaces.shared_impl.renderkit.html.HTML.DIV_ELEM;
    boolean startElemWritten;
    private final boolean alwaysWriteStartElement = true;

    private ResponseWriter writer;

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        writer = context.getResponseWriter();
        startElemWritten = false;

        if (component.getId() != null && !component.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
            startElemWritten = true;
            writer.startElement(startElement, component);
            HtmlRendererUtils.writeIdIfNecessary(writer, component, context);
            HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
        } else if (alwaysWriteStartElement) {
            startElemWritten = true;
            writer.startElement(startElement, component);
            HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
        } else {
            startElemWritten = HtmlRendererUtils.renderHTMLAttributesWithOptionalStartElement(writer, component, startElement, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
        }
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        try {
            RendererUtils.renderChildren(context, component);
        } catch (RuntimeException e) {
            LOG.error("failed to render childre of component " + component, e);
            throw e;
        }
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        if (startElemWritten) {
            writer.endElement(startElement);
        }
    }

    public String getStartElement() {
        return startElement;
    }

    public void setStartElement(String startElement) {
        this.startElement = startElement;
    }

}
=======
package ee.alfresco.web.ui.common.renderer;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.renderkit.html.HtmlGroupRenderer;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRendererUtils;

/**
 * more flexible version of {@link HtmlGroupRenderer} that writes id (if needed) and {@link HTML#COMMON_PASSTROUGH_ATTRIBUTES}
 */
public class SimpleHtmlElementRenderer extends HtmlGroupRenderer {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SimpleHtmlElementRenderer.class);

    private String startElement = org.apache.myfaces.shared_impl.renderkit.html.HTML.DIV_ELEM;
    boolean startElemWritten;
    private final boolean alwaysWriteStartElement = true;

    private ResponseWriter writer;

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        writer = context.getResponseWriter();
        startElemWritten = false;

        if (component.getId() != null && !component.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
            startElemWritten = true;
            writer.startElement(startElement, component);
            HtmlRendererUtils.writeIdIfNecessary(writer, component, context);
            HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
        } else if (alwaysWriteStartElement) {
            startElemWritten = true;
            writer.startElement(startElement, component);
            HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
        } else {
            startElemWritten = HtmlRendererUtils.renderHTMLAttributesWithOptionalStartElement(writer, component, startElement, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
        }
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        try {
            RendererUtils.renderChildren(context, component);
        } catch (RuntimeException e) {
            LOG.error("failed to render childre of component " + component, e);
            throw e;
        }
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        if (startElemWritten) {
            writer.endElement(startElement);
        }
    }

    public String getStartElement() {
        return startElement;
    }

    public void setStartElement(String startElement) {
        this.startElement = startElement;
    }

}
>>>>>>> develop-5.1
