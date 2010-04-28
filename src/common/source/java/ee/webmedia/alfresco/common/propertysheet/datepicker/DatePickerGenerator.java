package ee.webmedia.alfresco.common.propertysheet.datepicker;

import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet.ClientValidation;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Generates text field for a date. UIInput has "date" class for jQuery date picker plug-in.
 * Also adds JavaScript validation.
 * 
 * @author Kaarel Jõgeva
 */
public class DatePickerGenerator extends BaseComponentGenerator {

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIComponent component = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_INPUT);
        FacesHelper.setupComponentId(context, component, id);
        return component;
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {

        super.setupProperty(context, propertySheet, item, propertyDef, component);

        if (!Utils.isComponentDisabledOrReadOnly(component)) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> attributes = component.getAttributes();
            String styleClass = getCustomAttributes().get(STYLE_CLASS);
            if (StringUtils.isBlank(styleClass)) {
                styleClass = "date";
            }
            attributes.put("styleClass", styleClass);
        }
    }

    @Override
    protected void setupConverter(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, PropertyDefinition propertyDef,
            UIComponent component) {
        // Check for a valid date, when user can actually change it.
        if (propertySheet.inEditMode() && !Utils.isComponentDisabledOrReadOnly(component)) {
            setupValidDateConstraint(context, propertySheet, property, component);
        }
        ComponentUtil.createAndSetConverter(context, DatePickerConverter.CONVERTER_ID, component);
    }

    protected void setupValidDateConstraint(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, UIComponent component) {
        if (isCreateOutputText()) {
            return;
        }
        List<String> params = new ArrayList<String>(2);

        // add the value parameter
        String value = "document.getElementById('" +
                component.getClientId(context) + "')";
        params.add(value);

        // add the validation failed messages
        String matchMsg = Application.getMessage(context, "validation_date_failed");
        addStringConstraintParam(params,
                MessageFormat.format(matchMsg, new Object[] { property.getResolvedDisplayLabel() }));

        // add the validation case to the property sheet
        propertySheet.addClientValidation(new ClientValidation("validateDate",
                params, true));
        
        // add event handler to kick off real time checks
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put("onchange", "processButtonState();");
    }

    @Override
    protected void setupMandatoryValidation(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, UIComponent component,
            boolean realTimeChecking, String idSuffix) {
        super.setupMandatoryValidation(context, propertySheet, item, component, true, idSuffix);
    }

}