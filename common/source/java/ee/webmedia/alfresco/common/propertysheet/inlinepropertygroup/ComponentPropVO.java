package ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.IComponentGenerator;

import ee.webmedia.alfresco.common.propertysheet.generator.CustomAttributes;

/**
 * VO that hold information about component to be generated
 */
public class ComponentPropVO implements Serializable, CustomAttributes {
    private static final long serialVersionUID = 1L;

    private String propertyLabel;
    /** property name e.g. <code>doccom:docName</code> */
    private String propertyName;
    /** component generator name, optional, e.g. <code>TextFieldGenerator</code>, see more from RepoConstants */
    private String generatorName;
    private Map<String, String> customAttributes;
    /**
     * set it to false to indicate that component should just be constructed based on generatorName, <br>
     * but not using generator(for example because of using tag in jsp and as propertySheet and nodes might not be available)
     */
    private boolean useComponentGenerator = true;

    public IComponentGenerator getComponentGenerator(FacesContext context) {
        IComponentGenerator generator = FacesHelper.getComponentGenerator(context, generatorName);
        if (generator instanceof CustomAttributes) {
            ((CustomAttributes) generator).setCustomAttributes(customAttributes);
        }
        return generator;
    }

    // START: getters / setters
    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getGeneratorName() {
        return generatorName;
    }

    public void setGeneratorName(String generatorName) {
        this.generatorName = generatorName;
    }

    public String getPropertyLabel() {
        return propertyLabel;
    }

    public void setPropertyLabel(String propertyLabel) {
        this.propertyLabel = propertyLabel;
    }

    public void setUseComponentGenerator(boolean useComponentGenerator) {
        this.useComponentGenerator = useComponentGenerator;
    }

    public boolean isUseComponentGenerator() {
        return useComponentGenerator;
    }

    // END: getters / setters

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

    @Override
    public String toString() {
        return "ComponentPropVO ["
                + "\n\t, propertyName=" + propertyName
                + "\n\t, propertyLabel=" + propertyLabel
                + "\n\t, useComponentGenerator=" + useComponentGenerator
                + "\n\t, generatorName=" + generatorName
                + "\n\t  customAttributes=" + customAttributes
                + "\n]";
    }

}
