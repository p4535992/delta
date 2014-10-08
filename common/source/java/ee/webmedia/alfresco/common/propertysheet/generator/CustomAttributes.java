package ee.webmedia.alfresco.common.propertysheet.generator;

import java.util.Map;

/**
 * Interface that enables class to have custom attributes. <br>
 * Created to be able to pass custom attributes from property-sheet/show-property element to the componentGenerator
 */
public interface CustomAttributes {

    /**
     * @return UnmodifiableMap of attributes
     */
    Map<String, String> getCustomAttributes();

    /**
     * @param attributes - map of attributes to be set
     */
    void setCustomAttributes(Map<String, String> attributes);

}
