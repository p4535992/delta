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
     * @param attributes
     * @param customAttributes - attributes of show-property element from property-sheet that could be passed on to generator
     */
    protected void addProperty(String name, String displayLabel, String displayLabelId, String readOnly, String converter//
            , String inView, String inEdit, String compGenerator, String ignoreIfMissing, Map<String, String> attributes) {
        addItem(new WMPropertyConfig(name, displayLabel, displayLabelId, Boolean.parseBoolean(readOnly), //
                converter, inView, inEdit, compGenerator, ignoreIfMissing, attributes));
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.web.config.PropertySheetConfigElement#addAssociation(java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    // changed parent modifier from package scope to protected, so that this class could see it
    // and added just a call to superclass so that other classes could also use the method defined in superclass.
    protected void addAssociation(String name, String displayLabel, String displayLabelId, String readOnly,
            String converter, String inView, String inEdit, String compGenerator) {
        super.addAssociation(name, displayLabel, displayLabelId, readOnly, converter, inView, inEdit, compGenerator);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.web.config.PropertySheetConfigElement#addChildAssociation(java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    // changed parent modifier from package scope to protected, so that this class could see it
    // and added just a call to superclass so that other classes could also use the method defined in superclass.
    protected void addChildAssociation(String name, String displayLabel, String displayLabelId, String readOnly, String converter, String inView,
            String inEdit, String compGenerator) {
        super.addChildAssociation(name, displayLabel, displayLabelId, readOnly, converter, inView, inEdit, compGenerator);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.web.config.PropertySheetConfigElement#addSeparator(java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    // changed parent modifier from package scope to protected, so that this class could see it
    // and added just a call to superclass so that other classes could also use the method defined in superclass.
    protected void addSeparator(String name, String displayLabel, String displayLabelId, String inView, String inEdit, String compGenerator) {
        super.addSeparator(name, displayLabel, displayLabelId, inView, inEdit, compGenerator);
    }

    /**
     * Inner class to represent a configured property
     */
    public class WMPropertyConfig extends PropertyConfig implements CustomAttributes {
        protected Map<String, String> customAttributes;

        /**
         * @param customAttributes - attributes of show-property element from property-sheet that could be passed on to generator
         */
        public WMPropertyConfig(String name, String displayLabel, String displayLabelId, boolean readOnly, String converter //
                                , String inView, String inEdit, String compGenerator, String ignoreIfMissing, Map<String, String> customAttributes) {
            super(name, displayLabel, displayLabelId, readOnly, converter, inView, inEdit, compGenerator, ignoreIfMissing);
            this.customAttributes = customAttributes;
        }

        // START: getters / setters
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
        // END: getters / setters
    }

}
