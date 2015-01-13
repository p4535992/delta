package ee.webmedia.alfresco.common.propertysheet.renderkit;

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
 * This class is copy-paste from org.apache.myfaces.shared_impl.renderkit.html.HtmlGroupRendererBase,
 * added rendering <div> element when attribute layout="block" and rendering {@code<p>} element
 * when attribute layout="paragraph" . In later releases of MyFaces rendering <div> can
 * be done using setLayout/getLayout properties.
 */
public class HtmlGroupCustomRenderer extends HtmlGroupRenderer {

    public static final String HTML_GROUP_CUSTOM_RENDERER_TYPE = HtmlGroupCustomRenderer.class.getCanonicalName();
    public static final String LAYOUT_ATTR = "layout";
    public static final String LAYOUT_TYPE_BLOCK = "block";

    @Override
    public void encodeEnd(FacesContext context, UIComponent component)
            throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        boolean span = false;
        boolean div = false;
        boolean p = false;

        if (LAYOUT_TYPE_BLOCK.equals(component.getAttributes().get(LAYOUT_ATTR))) {
            div = true;
            writer.startElement(org.apache.myfaces.shared_impl.renderkit.html.HTML.DIV_ELEM, component);
            HtmlRendererUtils.writeIdIfNecessary(writer, component, context);
            HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
        } else if (component.getId() != null && !component.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
            span = true;

            writer.startElement(org.apache.myfaces.shared_impl.renderkit.html.HTML.SPAN_ELEM, component);

            HtmlRendererUtils.writeIdIfNecessary(writer, component, context);

            HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
            writer.writeAttribute("style", "white-space:nowrap;", null);
        } else {
            span = HtmlRendererUtils.renderHTMLAttributesWithOptionalStartElement(writer,
                                                                     component,
                                                                     HTML.SPAN_ELEM,
                                                                     HTML.COMMON_PASSTROUGH_ATTRIBUTES);
        }

        RendererUtils.renderChildren(context, component);
        if (span) {
            writer.endElement(HTML.SPAN_ELEM);
        } else if (div) {
            writer.endElement(HTML.DIV_ELEM);
        } else if (p) {
            writer.endElement(HTML.DIV_ELEM);
        }
    }

}
