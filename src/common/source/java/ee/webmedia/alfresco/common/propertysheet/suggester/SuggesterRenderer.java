package ee.webmedia.alfresco.common.propertysheet.suggester;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.myfaces.renderkit.html.HtmlTextRenderer;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;

public class SuggesterRenderer extends HtmlTextRenderer {
    public static final String SUGGESTER_RENDERER_TYPE = SuggesterRenderer.class.getCanonicalName();

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        super.encodeBegin(context, component);
        ResponseWriter out = context.getResponseWriter();
        out.startElement(HTML.SPAN_ELEM, component);// Add wrapper to fix IE bug related to input with background image and text shadowing
        out.writeAttribute(HTML.CLASS_ATTR, "suggest-wrapper", null);
    }
    
    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        super.encodeEnd(context, component);
        ResponseWriter out = context.getResponseWriter();
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        @SuppressWarnings("unchecked")
        final List<String> suggesterValues = (List<String>) attributes.get(SuggesterGenerator.ComponentAttributeNames.SUGGESTER_VALUES);
        if (suggesterValues != null) {
            out.startElement(HTML.SCRIPT_ELEM, component);
            out.writeAttribute(HTML.SCRIPT_TYPE_ATTR, HTML.SCRIPT_TYPE_TEXT_JAVASCRIPT, null);
            final String inputClientId = component.getClientId(context);
            String javaScript = getJavascriptFunctionCall(inputClientId, suggesterValues);
            out.write(javaScript);
            out.endElement(HTML.SCRIPT_ELEM);
        }
        out.endElement(HTML.SPAN_ELEM);
    }

    private String getJavascriptFunctionCall(String inputClientId, List<String> suggesterValues) {
        String jsValuesArrayString = getValuesAsJsArrayString(suggesterValues);
        String functionCall = "addAutocompleter('" + inputClientId + "', " + jsValuesArrayString + ");";
        return functionCall;
    }

    private String getValuesAsJsArrayString(List<String> suggesterValues) {
        final StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (String value : suggesterValues) {
            final String escapedValue = StringEscapeUtils.escapeJavaScript(value);
            sb.append("\"" + escapedValue + "\"");
            if (i != suggesterValues.size() - 1) {
                sb.append(", ");
            }
            i++;
        }
        return sb.append("]").toString();
    }

}
