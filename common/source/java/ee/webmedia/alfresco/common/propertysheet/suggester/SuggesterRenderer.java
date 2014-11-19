<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.suggester;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.renderkit.html.HtmlTextareaRenderer;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;

import ee.webmedia.alfresco.utils.WebUtil;

public class SuggesterRenderer extends HtmlTextareaRenderer {
    public static final String SUGGESTER_RENDERER_TYPE = SuggesterRenderer.class.getCanonicalName();

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        if (component.getParent() instanceof PropertySheetItem) { // we are directly inside a property sheet item cell, not in multivalueeditor
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = component.getAttributes();
            String componentStyleClass = (String) attributes.get("styleClass");
            if (StringUtils.isBlank(componentStyleClass)) {
                componentStyleClass = "";
            } else {
                componentStyleClass += " ";
            }
            componentStyleClass += "expand19-200";
            attributes.put("styleClass", componentStyleClass);
        }
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

    protected String getJavascriptFunctionCall(String inputClientId, List<String> suggesterValues) {
        String jsValuesArrayString = WebUtil.getValuesAsJsArrayString(suggesterValues);
        String functionCall = "addAutocompleter('" + inputClientId + "', " + jsValuesArrayString + ");";
        return functionCall;
    }
}
=======
package ee.webmedia.alfresco.common.propertysheet.suggester;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.renderkit.html.HtmlTextareaRenderer;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;

import ee.webmedia.alfresco.utils.WebUtil;

public class SuggesterRenderer extends HtmlTextareaRenderer {
    public static final String SUGGESTER_RENDERER_TYPE = SuggesterRenderer.class.getCanonicalName();

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        if (component.getParent() instanceof PropertySheetItem) { // we are directly inside a property sheet item cell, not in multivalueeditor
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = component.getAttributes();
            String componentStyleClass = (String) attributes.get("styleClass");
            if (StringUtils.isBlank(componentStyleClass)) {
                componentStyleClass = "";
            } else {
                componentStyleClass += " ";
            }
            componentStyleClass += "expand19-200";
            attributes.put("styleClass", componentStyleClass);
        }
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

    protected String getJavascriptFunctionCall(String inputClientId, List<String> suggesterValues) {
        String jsValuesArrayString = WebUtil.getValuesAsJsArrayString(suggesterValues);
        String functionCall = "addAutocompleter('" + inputClientId + "', " + jsValuesArrayString + ");";
        return functionCall;
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
