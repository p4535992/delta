package ee.webmedia.alfresco.common.propertysheet.multivalueeditor;

import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.OPTIONS_SEPARATOR;
import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROPERTIES_SEPARATOR;
import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROPS_GENERATION;
import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROP_GENERATOR_DESCRIPTORS;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.VALDIATION_DISABLED;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.VALIDATION_MARKER_DISABLED;

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

import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.ComponentPropVO;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.HandlesViewMode;
import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Generate {@link MultiValueEditor} component, if property sheet is in edit mode.<br>
 * Property columns can be specified using {@code props} attribute in {@code show-property} element in property sheet configuration. It must contain
 * comma-separated list of property names. For example: {@code props="xx:prop1,xx:prop2"}<br>
 * If {@code props} attribute is omitted, only one column is used, it is taken from {@code name} attribute. If {@code props} attribute is specified, then {@code name} attribute is
 * ignored. All properties specified must be multi-valued.
 * 
 * @author Alar Kvell
 */
public class MultiValueEditorGenerator extends BaseComponentGenerator implements HandlesViewMode {

    @Override
    public UIComponent generate(FacesContext context, String id) {
        getCustomAttributes().put(VALDIATION_DISABLED, Boolean.TRUE.toString());
        getCustomAttributes().put(VALIDATION_MARKER_DISABLED, Boolean.FALSE.toString());
        UIComponent component = context.getApplication().createComponent(MultiValueEditor.MULTI_VALUE_EDITOR_FAMILY);
        FacesHelper.setupComponentId(context, component, id);
        component.setRendererType(MultiValueEditorRenderer.MULTI_VALUE_EDITOR_RENDERER_TYPE);
        return component;
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef,
            UIComponent component) {
        String propsAttribute = getCustomAttributes().get(PROPS_GENERATION);
        String optionsSeparator = getCustomAttributes().get(OPTIONS_SEPARATOR);
        if (StringUtils.isBlank(optionsSeparator)) {
            optionsSeparator = "¤";
        }
        String propertiesSeparator = getCustomAttributes().get(PROPERTIES_SEPARATOR);
        final List<ComponentPropVO> propVOs = CombinedPropReader.readProperties(propsAttribute, propertiesSeparator, optionsSeparator, propertySheet.getNode(), context);

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = component.getAttributes();
        attributes.put(PROP_GENERATOR_DESCRIPTORS, propVOs);

        attributes.put(MultiValueEditor.PROPERTY_SHEET_VAR, propertySheet.getVar());
        addValueFromCustomAttributes(Search.PICKER_CALLBACK_KEY, attributes);
        addValueFromCustomAttributes(Search.DIALOG_TITLE_ID_KEY, attributes);
        addValueFromCustomAttributes(Search.SETTER_CALLBACK, attributes);
        addValueFromCustomAttributes(MultiValueEditor.ADD_LABEL_ID, attributes);
        addValueFromCustomAttributes(MultiValueEditor.SHOW_HEADERS, attributes);
        addValueFromCustomAttributes(Search.AJAX_PARENT_LEVEL_KEY, attributes, Integer.class);
        addValueFromCustomAttributes(MultiValueEditor.INITIAL_ROWS, attributes, Integer.class);
        addValueFromCustomAttributes(MultiValueEditor.FILTERS, attributes);
        addValueFromCustomAttributes(MultiValueEditor.FILTER_INDEX, attributes, Integer.class);
        addValueFromCustomAttributes(MultiValueEditor.PREPROCESS_CALLBACK, attributes);
        addValueFromCustomAttributes(MultiValueEditor.NO_ADD_LINK_LABEL, attributes);
        addValueFromCustomAttributes(MultiValueEditor.IS_AUTOMATICALLY_ADD_ROWS, attributes, Boolean.class);
        addValueFromCustomAttributes(ComponentUtil.IS_ALWAYS_EDIT, attributes, Boolean.class);
        addValueFromCustomAttributes(Search.SEARCH_SUGGEST_DISABLED, attributes, Boolean.class, false);

        if (!propertySheet.inEditMode() || item.isReadOnly()) {
            ComponentUtil.setReadonlyAttributeRecursively(component);
        }
    }

    @Override
    protected UIComponent setupMultiValuePropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem property,
            PropertyDefinition propertyDef, UIComponent component) {
        // Override BaseComponentGenerator method to do nothing
        return component;
    }

}
