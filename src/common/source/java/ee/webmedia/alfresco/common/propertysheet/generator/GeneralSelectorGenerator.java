package ee.webmedia.alfresco.common.propertysheet.generator;

import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS;

import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Component that generates a HtmlSelectOneMenu component that will receive values from method binding defined with "selectionItems" attribute.<br>
 * Method must be public void<br>
 * Method must declare two parameter FacesContext and HtmlSelectOneMenu for creating adding items to selection
 * 
 * @author Ats Uiboupin
 */
public class GeneralSelectorGenerator extends BaseComponentGenerator {

    public static final String ATTR_SELECTION_ITEMS = "selectionItems";

    public UIComponent generate(FacesContext context, String id) {
        // do nothing
        return null;
    }

    @Override
    protected UIComponent setupMultiValuePropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item,
            PropertyDefinition propertyDef, UIComponent component) {

        // if property sheet is in view mode, then an output text component has been generated
        if (component == null) {
            component = generateSelectComponent(context, item.getName(), propertyDef.isMultiValued());
        }
        return component;
    }

    public UIComponent generateSelectComponent(FacesContext context, String id, boolean multiValued) {
        UIComponent component = context.getApplication().createComponent(multiValued ? HtmlSelectManyListbox.COMPONENT_TYPE : HtmlSelectOneMenu.COMPONENT_TYPE);
        // non-null id is needed, otherwise clientId is not written to HTML
        FacesHelper.setupComponentId(context, component, id);
        return component;
    }

    public void setupSelectComponent(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component, boolean multiValued) {

        ComponentUtil.createAndSetConverter(context, getCustomAttributes().get("converter"), component);

        String styleClass = getStyleClass();
        if (StringUtils.isNotBlank(styleClass)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = component.getAttributes();
            attributes.put(STYLE_CLASS, styleClass);
        }

        if (component instanceof UIInput) {
            ValueBinding vb = component.getValueBinding("value");
            List<UISelectItem> results = initializeSelectionItems(context, propertySheet, item, propertyDef, (UIInput) component,
                    vb != null ? vb.getValue(context) : null, multiValued);
            if (results != null) {
                @SuppressWarnings("unchecked")
                List<UIComponent> children = component.getChildren();
                children.addAll(results);
            }
        }
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {

        super.setupProperty(context, propertySheet, item, propertyDef, component);
        setupSelectComponent(context, propertySheet, item, propertyDef, component, propertyDef == null ? false : propertyDef.isMultiValued());
    }

    @Override
    protected void setupMandatoryPropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property,
            PropertyDefinition propertyDef, UIComponent component) {

        super.setupMandatoryPropertyIfNecessary(context, propertySheet, property, propertyDef, component);

        // Must do this after component has beed added to tree
        String valueChangeListener = getCustomAttributes().get("valueChangeListener");
        if (StringUtils.isNotBlank(valueChangeListener)) {
            ((UIInput) component).setValueChangeListener(context.getApplication().createMethodBinding(valueChangeListener,
                    new Class[] { ValueChangeEvent.class }));
            String onchange = Utils.generateFormSubmit(context, component);
            if (component instanceof HtmlSelectOneMenu) {
                ((HtmlSelectOneMenu) component).setOnchange(onchange);
            } else if (component instanceof HtmlSelectManyListbox) {
                ((HtmlSelectManyListbox) component).setOnchange(onchange);
            }
        }
    }

    protected List<UISelectItem> initializeSelectionItems(FacesContext context, UIPropertySheet propertySheet,
            PropertySheetItem item, PropertyDefinition propertyDef, UIInput component, Object boundValue, boolean multiValued) {

        String selectionItems = getSelectionItems();
        MethodBinding mb = context.getApplication().createMethodBinding(selectionItems, new Class[] { FacesContext.class, UIInput.class });
        try {
            @SuppressWarnings("unchecked")
            List<SelectItem> selectItems = (List<SelectItem>) mb.invoke(context, new Object[] { context, component });
            ComponentUtil.addSelectItems(context, component, selectItems);
        } catch (ClassCastException e) {
            throw new RuntimeException("Failed to get values for selection from '" + selectionItems + "'", e);
        }
        return null;
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

    protected String getSelectionItems() {
        return getCustomAttributes().get(ATTR_SELECTION_ITEMS);
    }

    protected String getStyleClass() {
        return getCustomAttributes().get(STYLE_CLASS);
    }

}
