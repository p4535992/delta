package ee.webmedia.alfresco.common.propertysheet.generator;

import static ee.webmedia.alfresco.utils.ComponentUtil.DEFAULT_SELECT_VALUE;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomConstants.VALUE_INDEX_IN_MULTIVALUED_PROPERTY;

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

import ee.webmedia.alfresco.common.ajax.AjaxUpdateable;
import ee.webmedia.alfresco.common.propertysheet.converter.BooleanToLabelConverter;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Component that generates a HtmlSelectOneMenu component that will receive values from method binding defined with "selectionItems" attribute.<br>
 * Method must be public void<br>
 * Method must declare two parameter FacesContext and HtmlSelectOneMenu for creating adding items to selection
 * 
 * @author Ats Uiboupin
 */
public class GeneralSelectorGenerator extends BaseComponentGenerator {

    public static final String ATTR_SELECTION_ITEMS = "selectionItems";
    public static final String ATTR_VALUE_CHANGE_LISTENER = "valueChangeListener";
    // The following variables' values are also hardcoded in scripts.js, so change the values simultaneously here and in javascript
    public static final String ONCHANGE_SCRIPT_START_MARKER = "¤¤¤¤";
    public static final String ONCHANGE_MARKER_CLASS = "selectWithOnchangeEvent";
    // call javascript function that takes current element id as parameter
    public static final String ONCHANGE_PARAM_MARKER_CLASS = "selectWithOnchangeEventParam";

    @Override
    public UIComponent generate(FacesContext context, String id) {
        // do nothing
        return null;
    }

    @Override
    protected UIComponent setupMultiValuePropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item,
            PropertyDefinition propertyDef, UIComponent component) {

        // if property sheet is in view mode, then an output text component has been generated
        if (component == null) {
            component = generateSelectComponent(context, getDefaultId(item), isMultiValued(propertyDef));
        }
        return component;
    }

    protected boolean isMultiValued(PropertyDefinition propertyDef) {
        return propertyDef == null ? false : propertyDef.isMultiValued();
    }

    public UIComponent generateSelectComponent(FacesContext context, String id, boolean multiValued) {
        multiValued = generateMultivalued(context, multiValued);
        UIComponent component = context.getApplication().createComponent(multiValued ? HtmlSelectManyListbox.COMPONENT_TYPE : HtmlSelectOneMenu.COMPONENT_TYPE);
        // non-null id is needed, otherwise clientId is not written to HTML
        FacesHelper.setupComponentId(context, component, id);
        return component;
    }

    private boolean generateMultivalued(FacesContext context, boolean multiValued) {
        @SuppressWarnings("unchecked")
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        final Integer valueIndex = (Integer) requestMap.get(VALUE_INDEX_IN_MULTIVALUED_PROPERTY);
        if (valueIndex == null || valueIndex < 0) {
            return multiValued;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void setupSelectComponent(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component, boolean multiValued) {

        final Map<String, String> customAttributes = getCustomAttributes();
        ComponentUtil.createAndSetConverter(context, customAttributes.get("converter"), component);

        String styleClass = getStyleClass();
        if (StringUtils.isNotBlank(styleClass)) {
            Map<String, Object> attributes = component.getAttributes();
            attributes.put(STYLE_CLASS, styleClass);
        }

        if (customAttributes.containsKey(BooleanToLabelConverter.CONVERTER_LABEL_PREFIX)) {
            component.getAttributes().put(BooleanToLabelConverter.CONVERTER_LABEL_PREFIX, customAttributes.get(BooleanToLabelConverter.CONVERTER_LABEL_PREFIX));
        }

        if (component instanceof UIInput) {
            ValueBinding vb = component.getValueBinding("value");
            List<UIComponent> results = initializeSelectionItems(context, propertySheet, item, propertyDef, (UIInput) component,
                    vb != null ? vb.getValue(context) : null, multiValued);
            if (results != null) {
                List<UIComponent> children = component.getChildren();
                children.addAll(results);
                ComponentUtil.setHtmlSelectManyListboxSize(component, results);
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
        if (!isValidationDisabled(context, propertySheet)) {
            super.setupMandatoryPropertyIfNecessary(context, propertySheet, property, propertyDef, component);
        }
        // Must do this after component has beed added to tree
        setupValueChangeListener(context, component, getCustomAttributes());
    }

    /**
     * Must do this after component has beed added to tree
     * 
     * @param customAttributes ...of the component generator
     */
    public static void setupValueChangeListener(FacesContext context, UIComponent component, Map<String, String> customAttributes) {
        String valueChangeListener = customAttributes.get(ATTR_VALUE_CHANGE_LISTENER);
        if (StringUtils.isNotBlank(valueChangeListener) && component instanceof UIInput) {
            ((UIInput) component).setValueChangeListener(context.getApplication().createMethodBinding(valueChangeListener,
                    new Class[] { ValueChangeEvent.class }));
            final String onchange;
            if (Boolean.valueOf(customAttributes.get(AjaxUpdateable.AJAX_DISABLED_ATTR))) {
                onchange = Utils.generateFormSubmit(context, component);
            } else {
                onchange = ComponentUtil.generateAjaxFormSubmit(context, component);
            }
            if (component instanceof HtmlSelectOneMenu) {
                ((HtmlSelectOneMenu) component).setStyleClass(ONCHANGE_MARKER_CLASS + ONCHANGE_SCRIPT_START_MARKER + onchange);
            } else if (component instanceof HtmlSelectManyListbox) {
                // TODO: check if this class behaves correctly in IE8 with onChange and jQuery change event both active
                ((HtmlSelectManyListbox) component).setOnchange(onchange);
            } else {
                ComponentUtil.putAttribute(component, "onchange", onchange);
            }
        }
    }

    protected List<UIComponent> initializeSelectionItems(FacesContext context, UIPropertySheet propertySheet,
            PropertySheetItem item, PropertyDefinition propertyDef, UIInput component, Object boundValue, boolean multiValued) {

        String selectionItems = getSelectionItems();
        MethodBinding mb = context.getApplication().createMethodBinding(selectionItems, new Class[] { FacesContext.class, UIInput.class });
        try {
            @SuppressWarnings("unchecked")
            List<SelectItem> selectItems = (List<SelectItem>) mb.invoke(context, new Object[] { context, component });
            ComponentUtil.setSelectItems(context, component, selectItems);
        } catch (ClassCastException e) {
            throw new RuntimeException("Failed to get values for selection from '" + selectionItems + "'", e);
        }
        return null;
    }

    @Override
    protected void setupMandatoryValidation(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, UIComponent component,
            boolean realTimeChecking, String idSuffix) {
        if (!isValidationDisabled(context, propertySheet)) {
            super.setupMandatoryValidation(context, propertySheet, item, component, true, idSuffix);
        }
        // currently valuechangelistener and mandatory validation are not used together in any property sheet
        if (StringUtils.isBlank(getCustomAttributes().get(ATTR_VALUE_CHANGE_LISTENER))) {
            // add event handler to kick off real time checks
            Map<String, Object> attributes = component.getAttributes();
            attributes.put("onchange", "processButtonState();");
        }
    }

    private boolean isValidationDisabled(FacesContext context, UIPropertySheet propertySheet) {
        return StringUtils.endsWith(propertySheet.getClientId(context), "assoc-object-search-filter") || isValidationDisabled();
    }

    protected String getSelectionItems() {
        return getCustomAttributes().get(ATTR_SELECTION_ITEMS);
    }

    protected String getStyleClass() {
        return getCustomAttributes().get(STYLE_CLASS);
    }

    /**
     * Similar method for {@link SelectItem} objects is {@link ComponentUtil#addDefault(List, FacesContext)}
     * 
     * @param context
     * @param results
     */
    public static void addDefault(FacesContext context, List<UIComponent> results) {
        UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
        selectItem.setItemLabel(MessageUtil.getMessage(context, "select_default_label"));
        selectItem.setItemValue(DEFAULT_SELECT_VALUE); // value of SelectItem can't be null
        results.add(0, selectItem);
    }

}
