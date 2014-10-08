package ee.webmedia.alfresco.common.propertysheet.datepicker;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.utils.ComponentUtil;

public class DateTimePickerGenerator extends DatePickerGenerator {

    private static final String ATTRIBUTE_READONLY = "readonly";
    private static final String TIME_FIELD_SUFFIX = "_time";
    public static final String DATE_FIELD_SUFFIX = "_date";

    @Override
    public UIComponent generate(FacesContext context, String id) {
        // Container to hold date and time inputs and value binding to corresponding property.
        // Container attributes are propagated to both children in renderer
        UIInput container = (UIInput) context.getApplication().createComponent(DateTimePicker.DATE_TIME_PICKER_FAMILY);
        FacesHelper.setupComponentId(context, container, null);
        container.setRendererType(DateTimePickerRenderer.DATE_TIME_PICKER_RENDERER_TYPE);

        generateAndSetupChild(context, id, container, DATE_FIELD_SUFFIX, DatePickerConverter.CONVERTER_ID);
        generateAndSetupChild(context, id, container, TIME_FIELD_SUFFIX, TimePickerConverter.CONVERTER_ID);

        return container;

    }

    private void generateAndSetupChild(FacesContext context, String id, UIInput container, String fieldNameSuffix, String converterId) {
        UIInput childComponent = (UIInput) super.generate(context, id + fieldNameSuffix);
        ComponentUtil.addChildren(container, childComponent);
        ComponentUtil.createAndSetConverter(context, converterId, childComponent);
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef, UIComponent component) {
        super.setupProperty(context, propertySheet, item, propertyDef, component);

        Map<String, Object> attributes = ComponentUtil.getAttributes(component);

        addValueFromCustomAttributes(DateTimePickerRenderer.DATE_STYLE_CLASS_ATTR, attributes);
        addValueFromCustomAttributes(DateTimePickerRenderer.TIME_STYLE_CLASS_ATTR, attributes);
    }

    @Override
    public void setupValidDateConstraint(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, UIComponent component) {
        verifyChildren(component);
        UIComponent input = (UIComponent) component.getChildren().get(0);
        addClientValidation(context, propertySheet, property, input, component.getClientId(context) + DATE_FIELD_SUFFIX, "validateDate");
        input = (UIComponent) component.getChildren().get(1);
        addClientValidation(context, propertySheet, property, input, component.getClientId(context) + TIME_FIELD_SUFFIX, "validateTime");
    }

    @Override
    protected void setupConverter(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, PropertyDefinition propertyDef,
            UIComponent component) {
    }

    private void verifyChildren(UIComponent component) {
        Assert.isTrue(component != null && component.getChildren() != null && component.getChildCount() == 2);
    }

    public void setReadonly(UIComponent component, boolean readonly) {
        verifyChildren(component);
        ComponentUtil.setReadonlyAttributeRecursively(component);
    }
}
