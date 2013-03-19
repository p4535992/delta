package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectMany;
import javax.faces.component.UISelectOne;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.apache.myfaces.renderkit.html.HtmlMenuRenderer;
import org.apache.myfaces.shared_impl.component.EscapeCapable;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.renderkit.html.HTML;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlRendererUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Render different texts in value attribute and option label
 * 
 * @author Riina Tens
 */
public class LabelAndValueSelectorRenderer extends HtmlMenuRenderer {

    public static final String LABEL_AND_VALUE_SELECTOR_RENDERER_TYPE = LabelAndValueSelectorRenderer.class.getCanonicalName();
    public static final String ATTR_DESCRIPTION_AS_TOOLTIP = "descriptionAsTooltip";

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
        RendererUtils.checkParamValidity(facesContext, component, null);

        if (component instanceof UISelectMany) {
            renderMenu(facesContext, (UISelectMany) component, isDisabled(facesContext, component));
        } else if (component instanceof UISelectOne) {
            renderMenu(facesContext, (UISelectOne) component, isDisabled(facesContext, component));
        } else {
            throw new IllegalArgumentException("Unsupported component class " + component.getClass().getName());
        }
    }

    /* The following code is mostly copy-paste from HtmlRenderUtils, as it is final and cannot be extended */

    private void renderMenu(FacesContext facesContext, UISelectOne selectOne, boolean disabled) throws IOException {
        internalRenderSelect(facesContext, selectOne, disabled, 1, false);
    }

    private void renderMenu(FacesContext facesContext, UISelectMany selectMany, boolean disabled) throws IOException {
        internalRenderSelect(facesContext, selectMany, disabled, 5, true);
    }

    private void internalRenderSelect(FacesContext facesContext, UIComponent uiComponent, boolean disabled, int size, boolean selectMany)
            throws IOException {
        ResponseWriter writer = facesContext.getResponseWriter();

        writer.startElement("select", uiComponent);
        HtmlRendererUtils.writeIdIfNecessary(writer, uiComponent, facesContext);
        writer.writeAttribute("name", uiComponent.getClientId(facesContext), null);
        List selectItemList;
        Converter converter;
        if (selectMany) {
            writer.writeAttribute("multiple", "multiple", null);
            selectItemList = RendererUtils.getSelectItemList((UISelectMany) uiComponent);

            converter = HtmlRendererUtils.findUISelectManyConverterFailsafe(facesContext, uiComponent);
        } else {
            selectItemList = RendererUtils.getSelectItemList((UISelectOne) uiComponent);
            converter = HtmlRendererUtils.findUIOutputConverterFailSafe(facesContext, uiComponent);
        }

        if (size == 0) {
            writer.writeAttribute("size", Integer.toString(selectItemList.size()), null);
        } else {
            writer.writeAttribute("size", Integer.toString(size), null);
        }
        // don't overwrite title attribute value in javascript
        ComponentUtil.addStyleClass(uiComponent, "noOptionTitle");

        HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.SELECT_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED);

        if (disabled) {
            writer.writeAttribute("disabled", Boolean.TRUE, null);
        }

        Set lookupSet = HtmlRendererUtils.getSubmittedOrSelectedValuesAsSet(selectMany, uiComponent, facesContext, converter);

        renderSelectOptions(facesContext, uiComponent, converter, lookupSet, selectItemList);

        writer.writeText("", null);
        writer.endElement("select");
    }

    protected void renderSelectOptions(FacesContext context, UIComponent component, Converter converter, Set lookupSet, List selectItemList)
            throws IOException {
        renderOptions(context, component, converter, lookupSet, selectItemList);
    }

    protected void renderOptions(FacesContext context, UIComponent component, Converter converter, Set lookupSet, List selectItemList) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        for (Iterator it = selectItemList.iterator(); it.hasNext();) {
            SelectItem selectItem = (SelectItem) it.next();

            if (selectItem instanceof SelectItemGroup) {
                writer.startElement("optgroup", component);
                writer.writeAttribute("label", selectItem.getLabel(), null);

                SelectItem[] selectItems = ((SelectItemGroup) selectItem).getSelectItems();

                renderSelectOptions(context, component, converter, lookupSet, Arrays.asList(selectItems));

                writer.endElement("optgroup");
            } else {
                String itemStrValue = RendererUtils.getConvertedStringValue(context, component, converter, selectItem);

                writer.write(9);
                writer.startElement("option", component);
                writer.writeAttribute("value", itemStrValue == null ? "" : itemStrValue, null);
                if (isTrue(component.getAttributes().get(ATTR_DESCRIPTION_AS_TOOLTIP))) {
                    writer.writeAttribute("title", selectItem.getDescription(), null);
                } else {
                    writer.writeAttribute("title", selectItem.getLabel(), null);
                }

                if (lookupSet.contains(itemStrValue)) {
                    writer.writeAttribute("selected", "selected", null);
                }

                boolean disabled = selectItem.isDisabled();
                if (disabled) {
                    writer.writeAttribute("disabled", "disabled", null);
                }

                boolean componentDisabled = isTrue(component.getAttributes().get("disabled"));
                String labelClass;

                if ((componentDisabled) || (disabled)) {
                    labelClass = (String) component.getAttributes().get("disabledClass");
                } else {
                    labelClass = (String) component.getAttributes().get("enabledClass");
                }

                if (labelClass != null) {
                    writer.writeAttribute("class", labelClass, "labelClass");
                }
                boolean escape;
                if (component instanceof EscapeCapable) {
                    escape = ((EscapeCapable) component).isEscape();
                } else {
                    escape = RendererUtils.getBooleanAttribute(component, "escape", true);
                }

                String textValue = selectItem.getValue().toString();
                textValue = selectItem.getLabel() != null ? selectItem.getLabel() : "";
                if (escape) {
                    writer.writeText(textValue, null);
                } else {
                    writer.write(textValue);
                }

                writer.endElement("option");
            }
        }
    }

    private boolean isTrue(Object obj) {
        if (!(obj instanceof Boolean)) {
            return false;
        }
        return ((Boolean) obj).booleanValue();
    }

}
