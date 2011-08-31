package ee.webmedia.alfresco.common.propertysheet.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.config.ConfigException;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.config.PropertySheetElementReader;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.propertysheet.component.WMUIProperty;
import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.suggester.SuggesterGenerator;

/**
 * Custom PropertySheetConfigElement that also holds custom attributes read from "show-property" element.
 * 
 * @author Ats Uiboupin
 */
public class WMPropertySheetConfigElement extends PropertySheetConfigElement {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public WMPropertySheetConfigElement() {
        this(CONFIG_ELEMENT_ID);
    }

    /**
     * Constructor
     * 
     * @param name Name of the element this config element represents
     */
    public WMPropertySheetConfigElement(String name) {
        super(name);
    }

    /**
     * Inner class to represent a configured property
     */
    public interface ReadOnlyCopiableItemConfig {
        ItemConfig copyAsReadOnly();
    }

    /**
     * @author Ats Uiboupin
     */
    public static class ItemConfigVO extends ItemConfig implements CustomAttributes, ReadOnlyCopiableItemConfig, Serializable {
        private static final long serialVersionUID = 1L;

        // additional fields not present in parent class
        protected Map<String, String> customAttributes = new HashMap<String, String>();
        private ConfigItemType configItemType;

        public ItemConfigVO(String name) {
            super(name);
            setCustomAttribute(PropertySheetElementReader.ATTR_NAME, name);
        }

        public enum ConfigItemType {
            ASSOC, CHILD_ASSOC, PROPERTY, SEPARATOR, SUB_PROPERTY_SHEET;
        }

        @Override
        public String toString() {
            return new StringBuilder(super.toString()) //
                    .append(" configItemType=").append(configItemType).append(")") //
                    .append(" customAttributes=").append(customAttributes).append(")") //
                    .toString();
        }

        // TODO DLSeadist - maybe remove if not needed
        public void setName(String name) {
            if (name == null || name.length() == 0) {
                throw new ConfigException("You must specify a name for a proprty sheet item");
            }
            this.name = name;
            setCustomAttribute(PropertySheetElementReader.ATTR_NAME, name);
        }

        public void setDisplayLabel(String displayLabel) {
            this.displayLabel = displayLabel;
            setCustomAttribute(PropertySheetElementReader.ATTR_DISPLAY_LABEL, displayLabel);
        }

        public void setDisplayLabelId(String displayLabelId) {
            this.displayLabelId = displayLabelId;
            setCustomAttribute(PropertySheetElementReader.ATTR_DISPLAY_LABEL_ID, displayLabelId);
        }

        public void setConverter(String converter) {
            this.converter = converter;
            setCustomAttribute(PropertySheetElementReader.ATTR_CONVERTER, converter);
        }

        public void setComponentGenerator(String componentGenerator) {
            this.componentGenerator = componentGenerator;
            setCustomAttribute(PropertySheetElementReader.ATTR_COMPONENT_GENERATOR, componentGenerator);
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
            setCustomAttribute(PropertySheetElementReader.ATTR_READ_ONLY, Boolean.toString(readOnly));
        }

        public boolean isShowInViewMode() {
            return showInViewMode;
        }

        public void setShowInViewMode(boolean showInViewMode) {
            this.showInViewMode = showInViewMode;
            setCustomAttribute(PropertySheetElementReader.ATTR_SHOW_IN_VIEW_MODE, Boolean.toString(showInViewMode));
        }

        public boolean isShowInEditMode() {
            return showInEditMode;
        }

        public void setShowInEditMode(boolean showInEditMode) {
            this.showInEditMode = showInEditMode;
            setCustomAttribute(PropertySheetElementReader.ATTR_SHOW_IN_EDIT_MODE, Boolean.toString(showInEditMode));
        }

        private void setRendered(boolean rendered) {
            this.rendered = rendered;
        }

        public boolean isIgnoreIfMissing() {
            return ignoreIfMissing;
        }

        public void setIgnoreIfMissing(boolean ignoreIfMissing) {
            this.ignoreIfMissing = ignoreIfMissing;
            setCustomAttribute(PropertySheetElementReader.ATTR_IGNORE_IF_MISSING, Boolean.toString(ignoreIfMissing));
        }

        public void setConfigItemType(ConfigItemType configItemType) {
            this.configItemType = configItemType;
        }

        public ConfigItemType getConfigItemType() {
            return configItemType;
        }

        @Override
        public ItemConfigVO copyAsReadOnly() {
            ItemConfigVO copy = new ItemConfigVO(name);
            copy.setDisplayLabel(displayLabel);
            copy.setDisplayLabelId(displayLabelId);
            copy.setConverter(converter);
            copy.setComponentGenerator(componentGenerator);
            copy.setReadOnly(readOnly);
            copy.setShowInViewMode(showInViewMode);
            copy.setShowInEditMode(showInEditMode);
            copy.setRendered(rendered);
            copy.setIgnoreIfMissing(ignoreIfMissing);
            copy.setConfigItemType(configItemType);
            copy.setCustomAttributes(customAttributes);
            return copy;
        }

        @Override
        public Map<String, String> getCustomAttributes() {
            return customAttributes;
        }

        @Override
        public void setCustomAttributes(Map<String, String> propertySheetItemAttributes) {
            Assert.isTrue(!propertySheetItemAttributes.containsKey(null), "Attribute with null key not allowed");
            for (Entry<String, String> entry : propertySheetItemAttributes.entrySet()) {
                Assert.notNull(entry.getValue(), "Attribute key '" + entry.getKey() + "' has null value");
            }
            customAttributes.putAll(propertySheetItemAttributes);
        }

        // TODO FIXME XXX DLSeadist Alar - implement it differently
        protected void setCustomAttribute(String key, String value) {
            Assert.notNull(key, "Attribute with null key not allowed");
            if (value == null) {
                customAttributes.remove(key);
            } else {
                customAttributes.put(key, value);
            }
        }

        public void setForcedMandatory(Boolean forcedMandatory) {
            setCustomAttribute(BaseComponentGenerator.CustomAttributeNames.ATTR_FORCED_MANDATORY, forcedMandatory == null ? null : forcedMandatory.toString());
        }

        public void setReadOnlyIf(String valueBinding) {
            setCustomAttribute(BaseComponentGenerator.READONLY_IF, valueBinding);
        }

        public void setStyleClass(String styleClass) {
            setCustomAttribute(BaseComponentGenerator.CustomAttributeNames.STYLE_CLASS, styleClass);
        }

        public void setDontRenderIfDisabled(Boolean dontRenderIfDisabled) {
            setCustomAttribute(WMUIProperty.DONT_RENDER_IF_DISABLED_ATTR, dontRenderIfDisabled == null ? null : dontRenderIfDisabled.toString());
        }

        public void setSelectionItems(String valueBinding) {
            setCustomAttribute(GeneralSelectorGenerator.ATTR_SELECTION_ITEMS, valueBinding);
        }

        public void setValueChangeListener(String valueBinding) {
            setCustomAttribute(GeneralSelectorGenerator.ATTR_VALUE_CHANGE_LISTENER, valueBinding);
        }

        public void setSuggesterValues(String valueBinding) {
            setCustomAttribute(SuggesterGenerator.ComponentAttributeNames.SUGGESTER_VALUES, valueBinding);
        }
    }

    public void addItem(ItemConfigVO itemConfig) {
        super.addItem(itemConfig);
    }

}
