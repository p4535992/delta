package ee.webmedia.alfresco.common.propertysheet.generator;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectOne;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

/**
 * Component that generates a HtmlSelectOneMenu component that will receive values from method binding defined with "selectionItems" attribute.<br>
 * Method must be public void<br>
 * Method must declare two parameter FacesContext and HtmlSelectOneMenu for creating adding items to selection
 * 
 * @author Ats Uiboupin
 */
public class GeneralSelectorGenerator extends BaseComponentGenerator {
    public static final String ATTR_SELECTION_ITEMS = "selectionItems";

    public UISelectOne generate(FacesContext context, String id) {
        HtmlSelectOneMenu selectComponent = getSelectComponent(context);
        selectComponent.setId("genSelector");
        final String converterName = getCustomAttributes().get("converter");
        if (StringUtils.isNotBlank(converterName)) {
            selectComponent.setConverter(context.getApplication().createConverter(converterName));
        }
        return selectComponent;
    }

    @Override
    protected void setupMandatoryValidation(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, UIComponent component,
            boolean realTimeChecking, String idSuffix) {
        super.setupMandatoryValidation(context, propertySheet, item, component, true, idSuffix);
        // add event handler to kick off real time checks
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put("onchange", "processButtonState();");
    }

    protected HtmlSelectOneMenu getSelectComponent(FacesContext context) {
        return (HtmlSelectOneMenu) context.getApplication().createComponent(HtmlSelectOneMenu.COMPONENT_TYPE);
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        super.setupProperty(context, propertySheet, item, propertyDef, component);
        if (component instanceof HtmlSelectOneMenu) {
            HtmlSelectOneMenu htmlSelectOneMenu = (HtmlSelectOneMenu) component;
            ValueBinding vb = component.getValueBinding("value");
            initializeSelectionItems(context, htmlSelectOneMenu, vb.getValue(context), propertySheet, item, propertyDef);
        }
    }

    protected void initializeSelectionItems(FacesContext context, HtmlSelectOneMenu selectComponent, Object boundValue, UIPropertySheet propertySheet,
            PropertySheetItem item, PropertyDefinition propertyDef) {
        String selectionItems = getSelectionItems();
        MethodBinding mb = context.getApplication().createMethodBinding(selectionItems, new Class[] { FacesContext.class, HtmlSelectOneMenu.class });
        try {
            mb.invoke(context, new Object[] { context, selectComponent });
        } catch (ClassCastException e) {
            throw new RuntimeException("Failed to get values for selection from '" + selectionItems + "'", e);
        }
    }

    private String getSelectionItems() {
        return getCustomAttributes().get(ATTR_SELECTION_ITEMS);
    }

}
