package ee.webmedia.alfresco.common.propertysheet.search;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
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
 * <li>{@code pickerCallback} (mandatory) - callback which returns search results for a given input (JSF {@link MethodBinding}). For method signature and
 * description, see {@link AddUsersDialog#pickerCallback(int, String)}</li>
 * <li>{@code dialogTitleId} (optional) - search popup dialog's title message ID</li>
 * </ul>
 * 
 * @author Alar Kvell
 */
public class SearchGenerator extends BaseComponentGenerator {

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
            throw new RuntimeException("Property definition not found on node: " + propertySheet.getNode() + " (PropertySheetItem " + item.getName() + ")");
        } else if (propertyDef.isProtected()) {
            throw new RuntimeException("Protected property is not supported: " + propertyDef.getName() + " (PropertySheetItem " + item.getName()
                    + ")");
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
        attributes.put(Search.PICKER_CALLBACK_KEY, getCustomAttributes().get(Search.PICKER_CALLBACK_KEY));

        if (getCustomAttributes().containsKey(Search.DIALOG_TITLE_ID_KEY)) {
            attributes.put(Search.DIALOG_TITLE_ID_KEY, getCustomAttributes().get(Search.DIALOG_TITLE_ID_KEY));
        }
        if (getCustomAttributes().containsKey("setterCallback")) {
            attributes.put("setterCallback", getCustomAttributes().get("setterCallback"));
        }
        if (getCustomAttributes().containsKey("editable")) {
            attributes.put("editable", Boolean.parseBoolean(getCustomAttributes().get("editable")));
        }
    }

    @Override
    protected UIComponent setupMultiValuePropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property,
            PropertyDefinition propertyDef, UIComponent component) {
        // Override BaseComponentGenerator method to do nothing
        return component;
    }

    @Override
    protected void setupMandatoryValidation(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, UIComponent component, boolean realTimeChecking, String idSuffix) {
        // set realtime validation to true
        super.setupMandatoryValidation(context, propertySheet, item, component, true, idSuffix);
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

}
