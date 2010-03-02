package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import static ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSuggestingGenerator.ComponentAttributeNames.CLASSIFICATOR_VALUES;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.myfaces.renderkit.html.HtmlTextRenderer;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;

public class ClassificatorSuggesterGeneratorRenderer extends HtmlTextRenderer {
    public static final String CLASSIFICATOR_SUGGESTER_RENDERER_TYPE = ClassificatorSuggesterGeneratorRenderer.class.getCanonicalName();

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        super.encodeEnd(context, component);
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        @SuppressWarnings("unchecked")
        final List<String> classificatorValues = (List<String>) attributes.get(CLASSIFICATOR_VALUES);
        if (classificatorValues != null) {
            ResponseWriter out = context.getResponseWriter();
            out.startElement(HTML.SCRIPT_ELEM, component);
            out.writeAttribute(HTML.SCRIPT_TYPE_ATTR, HTML.SCRIPT_TYPE_TEXT_JAVASCRIPT, null);
            final String inputClientId = component.getClientId(context);
            String javaScript = getJavascriptFunctionCall(inputClientId, classificatorValues);
            out.write(javaScript);
            out.endElement(HTML.SCRIPT_ELEM);
        }

    }

    private String getJavascriptFunctionCall(String inputClientId, List<String> classificatorValues) {
        String jsValuesArrayString = getValuesAsJsArrayString(classificatorValues);
        String //
        functionCall = " $jQ(document).ready(function() {";
        functionCall += "   setInputAutoCompleteArray('" + inputClientId + "', " + jsValuesArrayString + ");";
        functionCall += "});";
        return functionCall;
    }

    private String getValuesAsJsArrayString(List<String> classificatorValues) {
        final StringBuilder sb = new StringBuilder("[");
        int i = 0;
        for (String value : classificatorValues) {
            final String escapedValue = StringEscapeUtils.escapeJavaScript(value);
            sb.append("\"" + escapedValue + "\"");
            if (i != classificatorValues.size() - 1) {
                sb.append(", ");
            }
            i++;
        }
        return sb.append("]").toString();
    }

}
