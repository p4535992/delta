package ee.webmedia.alfresco.common.propertysheet.multivalueeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Generate {@link MultiValueEditor} component, if property sheet is in edit mode.<br>
 * Property columns can be specified using {@code props} attribute in {@code show-property} element in property sheet configuration. It must contain
 * comma-separated list of property names. For example: {@code props="xx:prop1,xx:prop2"}<br>
 * If {@code props} attribute is omitted, only one column is used, it is taken from {@code name} attribute. If {@code props} attribute is specified, then
 * {@code name} attribute is ignored. All properties specified must be multi-valued.
 * 
 * @author Alar Kvell
 */
public class MultiValueEditorGenerator extends BaseComponentGenerator {

    @Override
    public UIComponent generate(FacesContext context, String id) {
        UIComponent component = context.getApplication().createComponent(MultiValueEditor.MULTI_VALUE_EDITOR_FAMILY);
        FacesHelper.setupComponentId(context, component, id);
        component.setRendererType(MultiValueEditorRenderer.MULTI_VALUE_EDITOR_RENDERER_TYPE);
        return component;
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        if (!propertySheet.inEditMode()) {
            super.setupProperty(context, propertySheet, item, propertyDef, component);
            return;
        }

        List<String> props;
        String propsAttribute = getCustomAttributes().get("props");
        if (propsAttribute == null) {
            props = new ArrayList<String>(1);
            props.add(item.getName());
        } else {
            props = Arrays.asList(StringUtils.split(propsAttribute, ','));
        }

        List<String> propNames = new ArrayList<String>(props.size());
        List<String> propTitles = new ArrayList<String>(props.size());
        for (String prop : props) {
            PropertyDefinition propertyDefinition = getPropertyDefinition(context, propertySheet.getNode(), prop);
            if (propertyDefinition == null) {
                throw new RuntimeException("Property definition '" + prop + "' not found on node: " + propertySheet.getNode() + " (PropertySheetItem "
                        + item.getName() + ")");
            } else if (!propertyDefinition.isMultiValued()) {
                throw new RuntimeException("Single-valued property is not supported: " + propertyDefinition.getName() + " (PropertySheetItem " + item.getName()
                        + ")");
            } else if (propertyDefinition.isProtected()) {
                throw new RuntimeException("Protected property is not supported: " + propertyDefinition.getName() + " (PropertySheetItem " + item.getName()
                        + ")");
            }
            propNames.add(propertyDefinition.getName().toString());
            propTitles.add(propertyDefinition.getTitle());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put("propNames", propNames);
        attributes.put("propTitles", propTitles);
        attributes.put("propertySheetVar", propertySheet.getVar());
        if (getCustomAttributes().containsKey(Search.PICKER_CALLBACK_KEY)) {
            attributes.put(Search.PICKER_CALLBACK_KEY, getCustomAttributes().get(Search.PICKER_CALLBACK_KEY));
        }
        if (getCustomAttributes().containsKey(Search.DIALOG_TITLE_ID_KEY)) {
            attributes.put(Search.DIALOG_TITLE_ID_KEY, getCustomAttributes().get(Search.DIALOG_TITLE_ID_KEY));
        }
        if (getCustomAttributes().containsKey("setterCallback")) {
            attributes.put("setterCallback", getCustomAttributes().get("setterCallback"));
        }

        if (item.isReadOnly()) {
           ComponentUtil.setDisabledAttributeRecursively(component);
        }
    }

    @Override
    protected UIComponent setupMultiValuePropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property,
            PropertyDefinition propertyDef, UIComponent component) {
        // Override BaseComponentGenerator method to do nothing
        return component;
    }

}
