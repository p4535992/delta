package ee.webmedia.alfresco.common.propertysheet.search;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.MethodBinding;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.bean.groups.AddUsersDialog;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Generate {@link Search} component. Property must be multi-valued and not protected. Usually it is also desireable to specify a {@code converter}. Additional
 * attributes:
 * <ul>
 * <li>{@code pickerCallback} (mandatory) - callback which returns search results for a given input (JSF {@link MethodBinding}). For method signature and description, see
 * {@link AddUsersDialog#pickerCallback(int, String)}</li>
 * <li>{@code dialogTitleId} (optional) - search popup dialog's title message ID</li>
 * <li>{@code editable} (optional - default true if property is not multivalued, false otherwise) - should values be only specified using picker (editable = false) or in addition
 * to picker should component value be editable by typing arbitary value(editable = true) <br>
 * NB! note that for multivalued component you must also implement {@link Converter#getAsObject(FacesContext, UIComponent, String)}</li>
 * </ul>
 * 
 * @author Alar Kvell
 */
public class SearchGenerator extends BaseComponentGenerator {

    private static final String EDITABLE_IF = "editableIf";

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIComponent component = context.getApplication().createComponent(Search.SEARCH_FAMILY);
        FacesHelper.setupComponentId(context, component, id);
        component.setRendererType(SearchRenderer.SEARCH_RENDERER_TYPE);
        return component;
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {

        if (propertyDef == null) {
            throw new RuntimeException("Unable to create Search component for property '" + item.getName()
                    + "' - property definition not found on node: " + propertySheet.getNode());
        } else if (propertyDef.isProtected()) {
            throw new RuntimeException("Unable to create Search component for property '" + item.getName()
                    + "' - property definition is protected. Node: " + propertySheet.getNode());
        }

        super.setupProperty(context, propertySheet, item, propertyDef, component);

        if (!(component instanceof Search)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();

        try {
            Class<?> dataType = Class.forName(propertyDef.getDataType().getJavaClassName());
            attributes.put(Search.DATA_TYPE_KEY, dataType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }

        attributes.put(Search.DATA_MULTI_VALUED, propertyDef.isMultiValued());
        attributes.put("dataMandatory", propertyDef.isMandatory());

        addValueFromCustomAttributes(Search.PICKER_CALLBACK_KEY, attributes);
        addValueFromCustomAttributes(Search.DIALOG_TITLE_ID_KEY, attributes);
        addValueFromCustomAttributes(Search.SETTER_CALLBACK, attributes);
        addValueFromCustomAttributes(Search.SETTER_CALLBACK_TAKES_NODE, attributes, Boolean.class);
        addValueFromCustomAttributes(Search.SHOW_FILTER_KEY, attributes);
        addValueFromCustomAttributes(Search.FILTERS_KEY, attributes);
        addValueFromCustomAttributes(Search.AJAX_PARENT_LEVEL_KEY, attributes, Integer.class);
        addValueFromCustomAttributes(Search.SUGGEST_CHARS_KEY, attributes, Integer.class);
        setEditableAttribute(context, propertySheet, item, propertyDef, attributes);
    }

    @Override
    protected UIComponent setupMultiValuePropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property,
            PropertyDefinition propertyDef, UIComponent component) {
        // Override BaseComponentGenerator method to do nothing
        return component;
    }

    @Override
    protected void setupMandatoryValidation(FacesContext context, UIPropertySheet propertySheet //
            , PropertySheetItem item, UIComponent component, boolean realTimeChecking, String idSuffix) {
        // set realtime validation to true
        super.setupMandatoryValidation(context, propertySheet, item, component, true, idSuffix);
    }

    @Override
    protected String getValidateMandatoryJsFunctionName() {
        return "validateSearchMandatory";
    }

    @Override
    protected void setupConverter(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property, PropertyDefinition propertyDef,
            UIComponent component) {
        if (property.getConverter() != null) {
            if (component instanceof Search) {
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = component.getAttributes();
                attributes.put(Search.CONVERTER_KEY, property.getConverter());
            } else {
                ComponentUtil.createAndSetConverter(context, property.getConverter(), component);
            }
        }
    }

    private void setEditableAttribute(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            Map<String, Object> attributes) {
        if (getCustomAttributes().containsKey("editable")) {
            addValueFromCustomAttributes("editable", attributes, Boolean.class);
        } else {
            final boolean editable;
            if (getCustomAttributes().containsKey(EDITABLE_IF)) {
                String expression = getCustomAttributes().get(EDITABLE_IF);
                boolean isEditable = checkCustomPropertyExpression(context, propertySheet, expression, EDITABLE_IF, item.getName());
                editable = !propertySheet.isReadOnly() && isEditable;
            } else {
                if (propertyDef.isMultiValued()) {
                    editable = false; // default: can't type to input if property is multivalued, must search from picker
                } else {
                    editable = true; // default: can type to input or search from picker
                }
            }
            attributes.put("editable", editable);
        }
    }

}
