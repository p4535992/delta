package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import static ee.webmedia.alfresco.common.web.BeanHelper.getNamespaceService;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UISelectItem;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.converter.EnumConverter;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.HandlesViewMode;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Generator, that generates a DropDown selection with values of enum constant specified by either
 * 1) using "enumClass" attribute on property-sheet/show-property element element.
 * or
 * 2) using "enumProp" attribute on property-sheet/show-property element element that refers to another property of the node containing the name of constant
 * 
 * @author Ats Uiboupin
 */
public class EnumSelectorGenerator extends GeneralSelectorGenerator implements HandlesViewMode {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(EnumSelectorGenerator.class);

    public static final String ATTR_ENUM_CLASS = "enumClass";
    public static final String ATTR_ENUM_PROP = "enumProp";

    @Override
    protected UIComponent createComponent(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item) {
        if (propertySheet.inEditMode()) {
            return super.createComponent(context, propertySheet, item);
        }
        return createOutputTextComponent(context, getDefaultId(item));
    }

    @Override
    protected void setupProperty(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item, PropertyDefinition propertyDef, UIComponent component) {
        super.setupProperty(context, propertySheet, item, propertyDef, component);
        if (!propertySheet.inEditMode()) {
            UIOutput outputText = (UIOutput) component;
            ValueBinding vb = outputText.getValueBinding("value");
            Object boundValue = vb != null ? vb.getValue(context) : null;
            String enumClassName = getValueProviderName(propertySheet.getNode());
            setConvertedValue(outputText, boundValue, enumClassName);
        }
    }

    private void setConvertedValue(UIOutput uiOutput, Object boundValue, String enumClassName) {
        Enum<?> enumValue = DefaultTypeConverter.INSTANCE.convert(EnumConverter.getEnumClass(enumClassName), boundValue);
        uiOutput.setValue(MessageUtil.getMessage(enumValue));
    }

    @Override
    public UIComponent generateSelectComponent(FacesContext context, String id, boolean multiValued) {
        final UIComponent selectComponent = super.generateSelectComponent(context, id, multiValued);
        if (!log.isDebugEnabled()) {
            return selectComponent;
        }
        // for debugging purpose in development
        return ComponentUtil.setTooltip(selectComponent, MessageUtil.getMessage("constant_source", getValueProviderName(null)));
    }

    @Override
    protected List<UISelectItem> initializeSelectionItems(FacesContext context, UIPropertySheet propertySheet, PropertySheetItem item,
            PropertyDefinition propertyDef, UIInput component, Object boundValue, boolean multiValued) {
        ValueBinding vb = component.getValueBinding("value");
        if (boundValue == null && multiValued) {
            boundValue = new ArrayList<Object>();
            vb.setValue(context, boundValue);
        }
        String enumClassName = getValueProviderName(propertySheet.getNode());
        if (StringUtils.isBlank(enumClassName)) {
            return null;
        }
        Class<? extends Enum<?>> en = EnumConverter.getEnumClass(enumClassName);
        List<UISelectItem> selectOptions = new ArrayList<UISelectItem>();
        for (Enum<?> c : en.getEnumConstants()) {
            UISelectItem selectItem = (UISelectItem) context.getApplication().createComponent(UISelectItem.COMPONENT_TYPE);
            selectItem.setItemLabel(MessageUtil.getMessage(c));
            selectItem.setItemValue(c.name());
            selectOptions.add(selectItem);
        }

        ComponentUtil.sortByLabel(selectOptions);

        if (!multiValued) {
            ClassificatorSelectorGenerator.addDefault(context, selectOptions);

            setConverter(context, component, boundValue, enumClassName);
        }

        return selectOptions;
    }

    private void setConverter(FacesContext context, UIInput component, Object boundValue, String enumClassName) {
        EnumConverter converter = new EnumConverter();
        converter.setEnumClass(enumClassName);
        UIInput uiInput = component;
        uiInput.setValue(converter.getAsObject(context, component, (String) boundValue)); // needed to make default selectItem work(if property value is null, then
        uiInput.setConverter(converter);
    }

    /**
     * @param node property sheet node (null when creating tooltip)
     * @return classificator name that is used to generate select values (translated text pointing to field used as a source of classificator name)
     */
    protected String getValueProviderName(Node node) {
        String classificatorProviderProp = getCustomAttributes().get(ATTR_ENUM_PROP);
        if (StringUtils.isNotBlank(classificatorProviderProp)) {
            QName propQName = QName.createQName(classificatorProviderProp, getNamespaceService());
            if (node == null) {
                return MessageUtil.getMessage("constant_source_enumNameContainer", classificatorProviderProp);
            }
            return (String) node.getProperties().get(propQName);
        }
        return getCustomAttributes().get(ATTR_ENUM_CLASS);
    }

}
