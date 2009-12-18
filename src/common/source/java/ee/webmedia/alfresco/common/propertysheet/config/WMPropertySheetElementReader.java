package ee.webmedia.alfresco.common.propertysheet.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigException;
import org.alfresco.web.config.PropertySheetElementReader;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 * Custom PropertySheetElementReader that also reads custom attributes from "show-property" element
 * 
 * @author Ats Uiboupin
 */
public class WMPropertySheetElementReader extends PropertySheetElementReader {

    /*
     * (non-Javadoc)
     * @see org.alfresco.web.config.PropertySheetElementReader#parse(org.dom4j.Element)
     */
    @Override
    public ConfigElement parse(Element element) {
        WMPropertySheetConfigElement configElement = null;

        if (element != null) {
            String name = element.getName();
            if (name.equals(ELEMENT_PROPERTY_SHEET) == false) {
                throw new ConfigException(//
                        "PropertySheetElementReader can only parse " + ELEMENT_PROPERTY_SHEET + "elements, " + "the element passed was '" + name + "'");
            }

            configElement = createConfigElement();

            // go through the items to show
            @SuppressWarnings("unchecked")
            Iterator<Element> items = element.elementIterator();
            while (items.hasNext()) {
                parsePropertySheetItem(configElement, items.next());
            }
        }

        return configElement;
    }

    protected WMPropertySheetConfigElement createConfigElement() {
        return new AdminPropertySheetConfigElement();
    }

    protected void parsePropertySheetItem(WMPropertySheetConfigElement configElement, Element item) {
        String propName = item.attributeValue(ATTR_NAME);
        String label = item.attributeValue(ATTR_DISPLAY_LABEL);
        String labelId = item.attributeValue(ATTR_DISPLAY_LABEL_ID);
        String readOnly = item.attributeValue(ATTR_READ_ONLY);
        String converter = item.attributeValue(ATTR_CONVERTER);
        String inEdit = item.attributeValue(ATTR_SHOW_IN_EDIT_MODE);
        String inView = item.attributeValue(ATTR_SHOW_IN_VIEW_MODE);
        String compGenerator = item.attributeValue(ATTR_COMPONENT_GENERATOR);

        if (ELEMENT_SHOW_PROPERTY.equals(item.getName())) {
            @SuppressWarnings("unchecked")
            List<Attribute> allAttributes = item.attributes();
            Map<String, String> attributes = new HashMap<String, String>(allAttributes.size());
            for (Attribute attribute : allAttributes) {
                attributes.put(attribute.getName(), attribute.getValue());
            }
            // add the property to show to the custom config element
            configElement.addProperty(propName, label, labelId, readOnly, converter, inView //
                    , inEdit, compGenerator, item.attributeValue(ATTR_IGNORE_IF_MISSING), attributes);
        } else if (ELEMENT_SHOW_ASSOC.equals(item.getName())) {
            configElement.addAssociation(propName, label, labelId, readOnly, converter, inView, inEdit, compGenerator);
        } else if (ELEMENT_SHOW_CHILD_ASSOC.equals(item.getName())) {
            configElement.addChildAssociation(propName, label, labelId, readOnly, converter, inView, inEdit, compGenerator);
        } else if (ELEMENT_SEPARATOR.equals(item.getName())) {
            configElement.addSeparator(propName, label, labelId, inView, inEdit, compGenerator);
        }
    }

}
