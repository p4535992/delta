package ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup;

import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.OPTIONS_SEPARATOR;
import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROPERTIES_SEPARATOR;
import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.PROPS;
import static ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.CombinedPropReader.AttributeNames.TEXT_ID;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.VALDIATION_DISABLED;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames.VALIDATION_MARKER_DISABLED;
import static org.alfresco.web.bean.generator.BaseComponentGenerator.CustomConstants.VALUE_INDEX_IN_MULTIVALUED_PROPERTY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Attributes:
 * <ul>
 * <li>{@code textId} - Label-id of main text to be displayed. Each {@code #} in the text is substituted with a component according to {@code props} attribute. Exactly the same
 * number of components are placed in the text, as are defined in {@code props} attribute, remaining {@code #} characters are left untouched.</li>
 * <li>{@code escapeText} - default true. If set to false, html tags could be used in text referenced by textId</li>
 * <li>{@code optionsSeparator} - custom separator for options for cases where default separator can't be used because it is used in options as well</li>
 * <li>{@code props} - Comma-separated list of property names. Each property name may have additional options appended, separated by {@code |} (or custom separator defined with
 * <code>optionsSeparator</code> attribute - for example <code>optionsSeparator="¤"</code>).<br/>
 * The first option after property name must be component generator name. It may be left empty, then a default generator according to property data type is used.<br/>
 * The following options after component generator name are set as component's attributes, these must be in the format {@code attributeName=attributeValue}.<br/>
 * For example: {@code props="ex:regNr1,ex:name1||styleClass=green,ex:date1|CustomDatePicker|styleClass=date inline|mandatoryIf=ex:other1"}</li>
 * </ul>
 */
public class InlinePropertyGroupGenerator extends BaseComponentGenerator implements HandlesViewMode {

    public static final String INLINE_PROPERTY_GROUP_PROP_NAMES_ATTR = "inlinePropertyGroupPropNames";
    private static final String ESCAPE_TEXT = "escapeText";
    private static final String PLACEHOLDER = "#";
    private int propIndex = 0;

    @Override
    public UIComponent generate(FacesContext context, String id) {
        getCustomAttributes().put(VALDIATION_DISABLED, Boolean.TRUE.toString());
        getCustomAttributes().put(VALIDATION_MARKER_DISABLED, Boolean.FALSE.toString());
        propIndex = 0;
        UIComponent container = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_GRID);
        ComponentUtil.putAttribute(container, "styleClass", "inline-property-group");
        FacesHelper.setupComponentId(context, container, null);
        return container;
    }

    @Override
    protected void setupMandatoryPropertyIfNecessary(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item,
            PropertyDefinition propertyDef, UIComponent component) {
        // by now, this component has been added to parent's children list

        String propertyDescriptions = getCustomAttributes().get(PROPS);
        boolean escapeText = Boolean.valueOf(getCustomAttributes().get(ESCAPE_TEXT));
        String optionsSeparator = getCustomAttributes().get(OPTIONS_SEPARATOR);
        String propertiesSeparator = getCustomAttributes().get(PROPERTIES_SEPARATOR);
        final List<ComponentPropVO> propVOs = CombinedPropReader.readProperties(propertyDescriptions, propertiesSeparator, optionsSeparator, propertySheet.getNode(), context);
        List<QName> propNames = new ArrayList<QName>();
        for (ComponentPropVO componentPropVO : propVOs) {
            propNames.add(QName.createQName(componentPropVO.getPropertyName(), BeanHelper.getNamespaceService()));
        }
        ComponentUtil.getAttributes(component).put(INLINE_PROPERTY_GROUP_PROP_NAMES_ATTR, propNames);

        String text = Application.getMessage(FacesContext.getCurrentInstance(), getCustomAttributes().get(TEXT_ID));
        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();
        generate(context, propertySheet, children, propVOs, text, escapeText);

        super.setupMandatoryPropertyIfNecessary(context, propertySheet, item, propertyDef, component);
    }

    protected void generate(FacesContext context, UIPropertySheet propertySheet, List<UIComponent> children,
            List<ComponentPropVO> propVOs, String text, boolean escapeText) {

        int i = 0;
        for (String rowText : text.split("\n")) {
            UIComponent container = context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_PANELGROUP);
            FacesHelper.setupComponentId(context, container, "row" + i);
            children.add(container);

            @SuppressWarnings("unchecked")
            List<UIComponent> rowChildren = container.getChildren();
            generateRow(context, propertySheet, rowChildren, propVOs, rowText, escapeText);
            i++;
        }
    }

    protected void generateRow(FacesContext context, UIPropertySheet propertySheet, List<UIComponent> rowChildren,
            List<ComponentPropVO> propVOs, String text, boolean escapeText) {
        final int nrOfParts;
        if (text.startsWith(PLACEHOLDER) && text.endsWith(PLACEHOLDER)) {
            nrOfParts = propVOs.size();
        } else if (!text.startsWith(PLACEHOLDER) && !text.endsWith(PLACEHOLDER)) {
            nrOfParts = propVOs.size() + 1;
        } else {
            nrOfParts = propVOs.size();
        }
        List<String> textParts = new ArrayList<String>(Arrays.asList(text.split(PLACEHOLDER, nrOfParts)));
        String last = textParts.get(textParts.size() - 1);
        if (last.endsWith(PLACEHOLDER)) {
            textParts.set(textParts.size() - 1, last.substring(0, last.length() - 1));
            textParts.add("");
        }
        String first = textParts.get(0);
        if (first.startsWith(PLACEHOLDER)) {
            textParts.set(0, first.substring(1));
            textParts.add(0, "");
        }
        boolean propSheetReadOnly = UIPropertySheet.VIEW_MODE.equals(propertySheet.getMode());
        for (int i = 0; i < textParts.size(); i++) {
            final String textPart = textParts.get(i);
            if (StringUtils.isNotEmpty(textPart)) {
                UIOutput textComponent = createOutputTextComponent(context, null);
                textComponent.setValue(textPart);
                if (!escapeText) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> attributes = textComponent.getAttributes();
                    attributes.put("escape", false);
                }
                rowChildren.add(textComponent);
            }

            if (nrOfParts > propIndex && i < textParts.size() - 1) {
                final ComponentPropVO componentPropVO = propVOs.get(propIndex);

                // Check if the field is mandatory
                String mandatory = componentPropVO.getCustomAttributes().get(CustomAttributeNames.ATTR_MANDATORY);
                if (!propSheetReadOnly && Boolean.valueOf(mandatory)) {
                    rowChildren.add(ComponentUtil.createMandatoryMarker(context));
                }

                componentPropVO.getCustomAttributes().put(VALUE_INDEX_IN_MULTIVALUED_PROPERTY, "-1");
                ComponentUtil.generateAndAddComponent(context, componentPropVO, propertySheet, rowChildren);
                propIndex++;
            }
        }
    }

}
