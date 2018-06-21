package ee.webmedia.alfresco.common.propertysheet.dimensionselector;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.scripts.json.JSONWriter;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.renderkit.html.HtmlTextareaRenderer;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRendererUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.propertysheet.multivalueeditor.MultiValueEditor;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import flexjson.JSONSerializer;

public class DimensionSelectorRenderer extends HtmlTextareaRenderer {

    public static final String DIMENSION_SELECTOR_RENDERER_TYPE = DimensionSelectorRenderer.class.getCanonicalName();

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
        super.encodeEnd(facesContext, component);
        RendererUtils.checkParamValidity(facesContext, component, null);
        ResponseWriter out = facesContext.getResponseWriter();
        out.startElement(HTML.SCRIPT_ELEM, component);
        out.writeAttribute(HTML.SCRIPT_TYPE_ATTR, HTML.SCRIPT_TYPE_TEXT_JAVASCRIPT, null);
        final String inputClientId = component.getClientId(facesContext);
        Map attributes = component.getAttributes();
        @SuppressWarnings("unchecked")
        String javaScript = getJavascriptFunctionCall(inputClientId,
                (List<DimensionValue>) attributes.get(DimensionSelectorGenerator.ATTR_DIMENSION_VALUES),
                (String) attributes.get(DimensionSelectorGenerator.ATTR_DIMENSION_NAME),
                (Date) attributes.get(DimensionSelectorGenerator.ATTR_ENTRY_DATE),
                (String) attributes.get(DimensionSelectorGenerator.ATTR_PREDEFINED_FILTER_NAME),
                (String) attributes.get(MultiValueEditor.ATTR_CLICK_LINK_ID));
        out.write(javaScript);
        out.endElement(HTML.SCRIPT_ELEM);

    }

    // copy-paste from HtmlTextareaRendererBase, added null value handling
    // and title attribute rendering
    @Override
    protected void encodeTextArea(FacesContext facesContext, UIComponent uiComponent) throws IOException {
        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement("textarea", uiComponent);

        String clientId = uiComponent.getClientId(facesContext);
        writer.writeAttribute("name", clientId, null);
        HtmlRendererUtils.writeIdIfNecessary(writer, uiComponent, facesContext);

        HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.TEXTAREA_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED);
        if (isDisabled(facesContext, uiComponent)) {
            writer.writeAttribute("disabled", Boolean.TRUE, null);
        }
        String strValue = RendererUtils.getStringValue(facesContext, uiComponent);
        String dimensionName = (String) uiComponent.getAttributes().get(DimensionSelectorGenerator.ATTR_DIMENSION_NAME);
        if (StringUtils.isEmpty(strValue) && Boolean.TRUE.equals(uiComponent.getAttributes().get(DimensionSelectorGenerator.ATTR_USE_DFAULT_VALUE))) {
            strValue = getDefaultValue(dimensionName, uiComponent);
        }
        if (strValue == null) {
            strValue = "";
        }
        String titleStr = getTooltip(dimensionName, strValue);
        if (StringUtils.isNotBlank(titleStr)) {
            writer.writeAttribute("title", titleStr, null);
        }

        writer.writeText(strValue, "value");

        writer.endElement("textarea");
    }

    private String getDefaultValue(String dimensionName, UIComponent uiComponent) {
        EInvoiceService eInvoiceService = BeanHelper.getEInvoiceService();
        DimensionValue dimensionValue = eInvoiceService.getDimensionDefaultValue(eInvoiceService.getDimension(Dimensions.get(dimensionName)));
        if (dimensionValue != null) {
            String filterName = (String) uiComponent.getAttributes().get(DimensionSelectorGenerator.ATTR_PREDEFINED_FILTER_NAME);
            if (StringUtils.isEmpty(filterName) || DimensionSelectorGenerator.predefinedFilters.get(filterName).evaluate(dimensionValue)) {
                return dimensionValue.getValueName();
            }
        }
        return null;
    }

    private String getTooltip(String dimensionName, String selectedValue) {
        NodeRef dimensionRef = BeanHelper.getEInvoiceService().getDimension(Dimensions.get(dimensionName));
        DimensionValue dimensionValue = BeanHelper.getEInvoiceService().getDimensionValue(dimensionRef, selectedValue);
        if (dimensionValue != null) {
            return dimensionValue.getValue() + (StringUtils.isNotBlank(dimensionValue.getValueComment()) ? "; " + dimensionValue.getValueComment() : "");
        }
        return null;
    }

    private String getJavascriptFunctionCall(String inputClientId, List<DimensionValue> suggesterValues, String dimensionName, Date entryDate, String predefinedFilterName,
            String clickLinkId) {
        Assert.notNull(dimensionName);
        DateFormat dateFormat = new SimpleDateFormat("dd.M.yyyy");
        JSONSerializer jsonSerializer = new JSONSerializer();
        List<String> jsFunctionArgs = new ArrayList<String>();
        jsFunctionArgs.add(jsonSerializer.serialize(inputClientId));
        jsFunctionArgs.add(getValuesAsJsArrayString(suggesterValues));
        jsFunctionArgs.add(jsonSerializer.serialize(dimensionName));
        jsFunctionArgs.add(jsonSerializer.serialize(entryDate != null ? dateFormat.format(entryDate) : ""));
        jsFunctionArgs.add(jsonSerializer.serialize(predefinedFilterName != null ? predefinedFilterName : ""));
        jsFunctionArgs.add(jsonSerializer.serialize(clickLinkId != null ? clickLinkId : ""));
        String functionCall = "addUIAutocompleter(" + StringUtils.join(jsFunctionArgs, ",") + ");";
        return functionCall;
    }

    public static String getValuesAsJsArrayString(List<DimensionValue> suggesterValues) {
        List<Map<String, String>> suggesterDimensionsValues = new ArrayList<Map<String, String>>();
        if (suggesterValues != null) {
            DateFormat dateFormat = new SimpleDateFormat("dd.M.yyyy");
            for (DimensionValue value : suggesterValues) {
                Map<String, String> suggesterDimensionValue = new HashMap<String, String>();
                suggesterDimensionValue.put("value", value.getValueName());
                String expiryPeriod = "";
                if (value.getBeginDateTime() != null || value.getEndDateTime() != null) {
                    expiryPeriod = " (kehtiv " + getDateOrDots(value.getBeginDateTime(), dateFormat) + " - " + getDateOrDots(value.getEndDateTime(), dateFormat) + ")";
                }
                String valueCommentStr = StringUtils.isNotBlank(value.getValueComment()) ? ". " + value.getValueComment() : "";
                suggesterDimensionValue.put("label", value.getValue() + valueCommentStr + expiryPeriod);
                suggesterDimensionValue.put("description", value.getValue() + valueCommentStr);
                suggesterDimensionsValues.add(suggesterDimensionValue);
            }

        }
        return (new JSONSerializer()).serialize(suggesterDimensionsValues);
    }

    public static String escapeString(String value, boolean getJsonFormatString) {
        if (getJsonFormatString) {
            return JSONWriter.encodeJSONString(value);
        }
        return StringEscapeUtils.escapeJavaScript(value);
    }

    private static String getDateOrDots(Date date, DateFormat dateFormat) {
        return date == null ? "..." : dateFormat.format(date);
    }

}
