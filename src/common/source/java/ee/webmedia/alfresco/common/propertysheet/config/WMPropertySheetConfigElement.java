package ee.webmedia.alfresco.common.propertysheet.config;

import java.util.Collections;
import java.util.Map;

import org.alfresco.web.config.PropertySheetConfigElement;

import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;

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
     * see {@link PropertySheetConfigElement#addProperty(String, String, String, String, String, String, String, String, String)}
     * 
     * @param customAttributes - attributes of show-property element from property-sheet that could be passed on to generator
     */
    protected void addProperty(String name, String displayLabel, String displayLabelId, String readOnly, String converter//
            , String inView, String inEdit, String compGenerator, String ignoreIfMissing, Map<String, String> customAttributes) {
        addItem(new WMPropertyConfig(name, displayLabel, displayLabelId, Boolean.parseBoolean(readOnly), //
                converter, inView, inEdit, compGenerator, ignoreIfMissing, customAttributes));
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.web.config.PropertySheetConfigElement#addAssociation(java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    // changed parent modifier from package scope to protected, so that this class could see it
    // and added just a call to superclass so that other classes could also use the method defined in superclass.
    protected void addAssociation(String name, String displayLabel, String displayLabelId, String readOnly,
            String converter, String inView, String inEdit, String compGenerator, Map<String, String> customAttributes) {
        addItem(new WMAssociationConfig(name, displayLabel, displayLabelId, Boolean.parseBoolean(readOnly), //
                converter, inView, inEdit, compGenerator, customAttributes));
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.web.config.PropertySheetConfigElement#addChildAssociation(java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    // changed parent modifier from package scope to protected, so that this class could see it
    // and added just a call to superclass so that other classes could also use the method defined in superclass.
    protected void addChildAssociation(String name, String displayLabel, String displayLabelId, String readOnly, String converter, String inView,
            String inEdit, String compGenerator, Map<String, String> customAttributes) {
        addItem(new WMChildAssociationConfig(name, displayLabel, displayLabelId, Boolean.parseBoolean(readOnly), // 
                converter, inView, inEdit, compGenerator, customAttributes));
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.web.config.PropertySheetConfigElement#addSeparator(java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String)
     */
    // changed parent modifier from package scope to protected, so that this class could see it
    // and added just a call to superclass so that other classes could also use the method defined in superclass.
    protected void addSeparator(String name, String displayLabel, String displayLabelId, String inView, String inEdit, String compGenerator, Map<String, String> customAttributes) {
        addItem(new WMSeparatorConfig(name, displayLabel, displayLabelId, inView, inEdit, compGenerator, customAttributes));
    }

    /**
     * Inner class to represent a configured property
     */
    public interface ReadOnlyCopiableItemConfig {
        ItemConfig copyAsReadOnly();
    }

    /**
     * Inner class to represent a configured property
     */
    public class WMPropertyConfig extends PropertyConfig implements CustomAttributes, ReadOnlyCopiableItemConfig {
        public WMPropertyConfig(String name, String displayLabel, String displayLabelId, boolean readOnly, String converter, String inView, String inEdit,
                              String compGenerator, String ignoreIfMissing, Map<String, String> customAttributes) {
            super(name, displayLabel, displayLabelId, readOnly, converter, inView, inEdit, compGenerator, ignoreIfMissing);
            this.customAttributes = customAttributes;
        }

        protected Map<String, String> customAttributes;
        
        @Override
        public Map<String, String> getCustomAttributes() {
            if (customAttributes == null) {
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(customAttributes);
        }

        @Override
        public void setCustomAttributes(Map<String, String> propertySheetItemAttributes) {
            this.customAttributes = propertySheetItemAttributes;
        }

        @Override
        public WMPropertyConfig copyAsReadOnly() {
            return new WMPropertyConfig(getName(), getDisplayLabel(), getDisplayLabelId(), true, getConverter(), Boolean.toString(isShownInViewMode()), Boolean.toString(isShownInEditMode()), getComponentGenerator(), Boolean.toString(getIgnoreIfMissing()), customAttributes);
        }
    }

    /**
     * Inner class to represent a configured association
     */
    public class WMAssociationConfig extends AssociationConfig implements CustomAttributes, ReadOnlyCopiableItemConfig {
        public WMAssociationConfig(String name, String displayLabel, String displayLabelId, boolean readOnly, String converter, String inView, String inEdit,
                                 String compGenerator, Map<String, String> customAttributes) {
            super(name, displayLabel, displayLabelId, readOnly, converter, inView, inEdit, compGenerator);
            this.customAttributes = customAttributes;
        }

        protected Map<String, String> customAttributes;
        
        @Override
        public Map<String, String> getCustomAttributes() {
            if (customAttributes == null) {
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(customAttributes);
        }

        @Override
        public void setCustomAttributes(Map<String, String> propertySheetItemAttributes) {
            this.customAttributes = propertySheetItemAttributes;
        }

        @Override
        public WMAssociationConfig copyAsReadOnly() {
            return new WMAssociationConfig(getName(), getDisplayLabel(), getDisplayLabelId(), true, getConverter(), Boolean.toString(isShownInViewMode()), Boolean.toString(isShownInEditMode()), getComponentGenerator(), customAttributes);
        }
    }

    /**
     * Inner class to represent a configured child association
     */
    public class WMChildAssociationConfig extends ChildAssociationConfig implements CustomAttributes, ReadOnlyCopiableItemConfig {
        public WMChildAssociationConfig(String name, String displayLabel, String displayLabelId, boolean readOnly, String converter, String inView,
                                      String inEdit, String compGenerator, Map<String, String> customAttributes) {
            super(name, displayLabel, displayLabelId, readOnly, converter, inView, inEdit, compGenerator);
            this.customAttributes = customAttributes;
        }

        protected Map<String, String> customAttributes;
        
        @Override
        public Map<String, String> getCustomAttributes() {
            if (customAttributes == null) {
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(customAttributes);
        }

        @Override
        public void setCustomAttributes(Map<String, String> propertySheetItemAttributes) {
            this.customAttributes = propertySheetItemAttributes;
        }

        @Override
        public WMChildAssociationConfig copyAsReadOnly() {
            return new WMChildAssociationConfig(getName(), getDisplayLabel(), getDisplayLabelId(), true, getConverter(), Boolean.toString(isShownInViewMode()), Boolean.toString(isShownInEditMode()), getComponentGenerator(), customAttributes);
        }
    }

    /**
     * Inner class to represent a configured separator
     */
    public class WMSeparatorConfig extends SeparatorConfig implements CustomAttributes, ReadOnlyCopiableItemConfig {
        public WMSeparatorConfig(String name, String displayLabel, String displayLabelId, String inView, String inEdit, String compGenerator,
                               Map<String, String> customAttributes) {
            super(name, displayLabel, displayLabelId, inView, inEdit, compGenerator);
            this.customAttributes = customAttributes;
        }

        protected Map<String, String> customAttributes;
        
        @Override
        public Map<String, String> getCustomAttributes() {
            if (customAttributes == null) {
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(customAttributes);
        }

        @Override
        public void setCustomAttributes(Map<String, String> propertySheetItemAttributes) {
            this.customAttributes = propertySheetItemAttributes;
        }

        @Override
        public WMSeparatorConfig copyAsReadOnly() {
            return new WMSeparatorConfig(getName(), getDisplayLabel(), getDisplayLabelId(), Boolean.toString(isShownInViewMode()), Boolean.toString(isShownInEditMode()), getComponentGenerator(), customAttributes);
        }
    }

}
