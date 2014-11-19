<<<<<<< HEAD
package ee.webmedia.alfresco.common.propertysheet.datepicker;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.util.Pair;
import org.alfresco.web.ui.common.renderer.BaseRenderer;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

public class DateTimePickerRenderer extends BaseRenderer {

    public static final String TIME_STYLE_CLASS_ATTR = "timeStyleClass";
    public static final String DATE_STYLE_CLASS_ATTR = "dateStyleClass";
    public static final String DATE_TIME_PICKER_RENDERER_TYPE = DateTimePickerRenderer.class.getCanonicalName();

    @Override
    public void decode(FacesContext context, UIComponent component) {
        // It is necessary to set submitted value,
        // otherwise DateTimePicker getConvertedValue methos is not called,
        // although we ignore this value when getting actual converted value in DateTimePicker
        List<UIComponent> children = ComponentUtil.getChildren(component);
        String date = (String) ((EditableValueHolder) children.get(0)).getSubmittedValue();
        String time = (String) ((EditableValueHolder) children.get(1)).getSubmittedValue();
        Pair<String, String> dateTimeStr = new Pair<String, String>(date, time);

        ((EditableValueHolder) component).setSubmittedValue(dateTimeStr);
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        setupChildren(context, component);
        ResponseWriter out = context.getResponseWriter();
        out.startElement("span", component);
    }

    /** Propagate parent's value and attributes to child elements, add styleclasses */
    private void setupChildren(FacesContext context, UIComponent component) throws IOException {
        if (component.getChildren() == null) {
            return;
        }
        Date parentValue = (Date) ((UIInput) component).getValue();
        for (UIComponent child : ComponentUtil.getChildren(component)) {
            // no need to extract date and time parts here, formatting will do this
            UIInput childInput = (UIInput) child;
            childInput.setValue(parentValue);
            ComponentUtil.addAttributes(childInput, component.getAttributes());
            setupStyleClass(context, childInput);
        }
    }

    public void setupStyleClass(FacesContext context, UIComponent component) {
        String styleClass = ((String) component.getAttributes().get("styleClass"));
        String dateStyleClass = ((String) component.getAttributes().get(DATE_STYLE_CLASS_ATTR));
        String timeStyleClass = ((String) component.getAttributes().get(TIME_STYLE_CLASS_ATTR));
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.isEmpty(styleClass) ? "" : styleClass + " ");
        if (Boolean.TRUE.equals(component.getAttributes().get("readonly"))) {
            sb.append("disabled-");
        }
        String id = component.getClientId(context);
        String fieldNameSuffix = id.substring(id.lastIndexOf("_"));
        if (DateTimePickerGenerator.DATE_FIELD_SUFFIX.equalsIgnoreCase(fieldNameSuffix)) {
            sb.append("date").append(dateStyleClass == null ? "" : " " + dateStyleClass);
        } else {
            sb.append("time").append(timeStyleClass == null ? "" : " " + timeStyleClass);
        }
        ComponentUtil.putAttribute(component, "styleClass", sb.toString());
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter out = context.getResponseWriter();
        out.endElement("span");
    }
}
=======
package ee.webmedia.alfresco.common.propertysheet.datepicker;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.util.Pair;
import org.alfresco.web.ui.common.renderer.BaseRenderer;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

public class DateTimePickerRenderer extends BaseRenderer {

    public static final String TIME_STYLE_CLASS_ATTR = "timeStyleClass";
    public static final String DATE_STYLE_CLASS_ATTR = "dateStyleClass";
    public static final String DATE_TIME_PICKER_RENDERER_TYPE = DateTimePickerRenderer.class.getCanonicalName();

    @Override
    public void decode(FacesContext context, UIComponent component) {
        // It is necessary to set submitted value,
        // otherwise DateTimePicker getConvertedValue methos is not called,
        // although we ignore this value when getting actual converted value in DateTimePicker
        List<UIComponent> children = ComponentUtil.getChildren(component);
        String date = (String) ((EditableValueHolder) children.get(0)).getSubmittedValue();
        String time = (String) ((EditableValueHolder) children.get(1)).getSubmittedValue();
        Pair<String, String> dateTimeStr = new Pair<String, String>(date, time);

        ((EditableValueHolder) component).setSubmittedValue(dateTimeStr);
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        setupChildren(context, component);
        ResponseWriter out = context.getResponseWriter();
        out.startElement("span", component);
    }

    /** Propagate parent's value and attributes to child elements, add styleclasses */
    private void setupChildren(FacesContext context, UIComponent component) throws IOException {
        if (component.getChildren() == null) {
            return;
        }
        Date parentValue = (Date) ((UIInput) component).getValue();
        for (UIComponent child : ComponentUtil.getChildren(component)) {
            // no need to extract date and time parts here, formatting will do this
            UIInput childInput = (UIInput) child;
            childInput.setValue(parentValue);
            ComponentUtil.addAttributes(childInput, component.getAttributes());
            setupStyleClass(context, childInput);
        }
    }

    public void setupStyleClass(FacesContext context, UIComponent component) {
        String styleClass = ((String) component.getAttributes().get("styleClass"));
        String dateStyleClass = ((String) component.getAttributes().get(DATE_STYLE_CLASS_ATTR));
        String timeStyleClass = ((String) component.getAttributes().get(TIME_STYLE_CLASS_ATTR));
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.isEmpty(styleClass) ? "" : styleClass + " ");
        if (Boolean.TRUE.equals(component.getAttributes().get("readonly"))) {
            sb.append("disabled-");
        }
        String id = component.getClientId(context);
        String fieldNameSuffix = id.substring(id.lastIndexOf("_"));
        if (DateTimePickerGenerator.DATE_FIELD_SUFFIX.equalsIgnoreCase(fieldNameSuffix)) {
            sb.append("date").append(dateStyleClass == null ? "" : " " + dateStyleClass);
        } else {
            sb.append("time").append(timeStyleClass == null ? "" : " " + timeStyleClass);
        }
        ComponentUtil.putAttribute(component, "styleClass", sb.toString());
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter out = context.getResponseWriter();
        out.endElement("span");
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
