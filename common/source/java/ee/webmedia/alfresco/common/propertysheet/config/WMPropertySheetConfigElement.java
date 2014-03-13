package ee.webmedia.alfresco.common.propertysheet.config;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.web.config.PropertySheetConfigElement;

import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;

/**
 * Custom PropertySheetConfigElement that also holds custom attributes read from "show-property" element.
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

    public static class ItemConfigVO extends ItemConfig implements CustomAttributes, ReadOnlyCopiableItemConfig {

        // additional fields not present in parent class
        protected Map<String, String> customAttributes;
        private ConfigItemType configItemType;

        public ItemConfigVO(String name) {
            super(name);
        }

        public enum ConfigItemType {
            ASSOC, CHILD_ASSOC, PROPERTY, SEPPARATOR, SUB_PROPERTY_SHEET;
        }

        @Override
        public String toString() {
            return new StringBuilder(super.toString()) //
                    .append(" configItemType=").append(configItemType).append(")") //
                    .append(" customAttributes=").append(customAttributes).append(")") //
                    .toString();
        }

        public void setDisplayLabel(String displayLabel) {
            this.displayLabel = displayLabel;
        }

        public void setDisplayLabelId(String displayLabelId) {
            this.displayLabelId = displayLabelId;
        }

        public void setConverter(String converter) {
            this.converter = converter;
        }

        public void setComponentGenerator(String componentGenerator) {
            this.componentGenerator = componentGenerator;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public boolean isShowInViewMode() {
            return showInViewMode;
        }

        public void setShowInViewMode(boolean showInViewMode) {
            this.showInViewMode = showInViewMode;
        }

        public boolean isShowInEditMode() {
            return showInEditMode;
        }

        public void setShowInEditMode(boolean showInEditMode) {
            this.showInEditMode = showInEditMode;
        }

        private void setRendered(boolean rendered) {
            this.rendered = rendered;
        }

        public boolean isIgnoreIfMissing() {
            return ignoreIfMissing;
        }

        public void setIgnoreIfMissing(boolean ignoreIfMissing) {
            this.ignoreIfMissing = ignoreIfMissing;
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
            return copy;
        }

        @Override
        public Map<String, String> getCustomAttributes() {
            if (customAttributes == null) {
                customAttributes = new HashMap<String, String>(0);
            }
            return customAttributes;
        }

        @Override
        public void setCustomAttributes(Map<String, String> propertySheetItemAttributes) {
            customAttributes = propertySheetItemAttributes;
        }
    }

    protected void addItem(ItemConfigVO itemConfig) {
        super.addItem(itemConfig);
    }

}
