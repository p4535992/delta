package ee.webmedia.alfresco.common.propertysheet.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.config.ConfigElement;
import org.alfresco.config.ConfigException;
import org.alfresco.web.config.PropertySheetElementReader;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;

import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO;
import ee.webmedia.alfresco.common.propertysheet.config.WMPropertySheetConfigElement.ItemConfigVO.ConfigItemType;

/**
 * Custom PropertySheetElementReader that also reads custom attributes from "show-property" element
 */
public class WMPropertySheetElementReader extends PropertySheetElementReader {
    public static final String ELEMENT_SUB_PROPERTY_SHEET = "subPropertySheet";

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
                final Element childConfElem = items.next();
                try {
                    parsePropertySheetItem(configElement, childConfElem);
                } catch (ConfigException e) {
                    throw new ConfigException("Failed to parse xml config element '" + childConfElem + "' into object. Parent element: '" + element + "'", e);
                }
            }
        }
        return configElement;
    }

    protected WMPropertySheetConfigElement createConfigElement() {
        return new AdminPropertySheetConfigElement();
    }

    protected void parsePropertySheetItem(WMPropertySheetConfigElement configElement, Element item) {
        ItemConfigVO itemConf = new ItemConfigVO(item.attributeValue(ATTR_NAME));
        ConfigItemType configItemType;
        if (ELEMENT_SHOW_PROPERTY.equals(item.getName())) {
            configItemType = ConfigItemType.PROPERTY;
            itemConf.setIgnoreIfMissing(readBooleanAttribute(ATTR_IGNORE_IF_MISSING, item, true));
        } else if (ELEMENT_SHOW_ASSOC.equals(item.getName())) {
            configItemType = ConfigItemType.ASSOC;
        } else if (ELEMENT_SHOW_CHILD_ASSOC.equals(item.getName())) {
            configItemType = ConfigItemType.CHILD_ASSOC;
        } else if (ELEMENT_SEPARATOR.equals(item.getName())) {
            configItemType = ConfigItemType.SEPARATOR;
        } else if (ELEMENT_SUB_PROPERTY_SHEET.equals(item.getName())) {
            configItemType = ConfigItemType.SUB_PROPERTY_SHEET;
        } else {
            throw new IllegalArgumentException("Unknown item name: '" + item.getName() + "'");
        }
        @SuppressWarnings("unchecked")
        List<Attribute> allAttributes = item.attributes();
        Map<String, String> attributes = new HashMap<String, String>(allAttributes.size());
        for (Attribute attribute : allAttributes) {
            attributes.put(attribute.getName(), attribute.getValue());
        }
        itemConf.setCustomAttributes(attributes);
        itemConf.setDisplayLabel(item.attributeValue(ATTR_DISPLAY_LABEL));
        itemConf.setDisplayLabelId(item.attributeValue(ATTR_DISPLAY_LABEL_ID));
        itemConf.setConverter(item.attributeValue(ATTR_CONVERTER));
        itemConf.setComponentGenerator(item.attributeValue(ATTR_COMPONENT_GENERATOR));
        itemConf.setReadOnly(readBooleanAttribute(ATTR_READ_ONLY, item, false));
        itemConf.setShowInViewMode(readBooleanAttribute(ATTR_SHOW_IN_VIEW_MODE, item, true));
        itemConf.setShowInEditMode(readBooleanAttribute(ATTR_SHOW_IN_EDIT_MODE, item, true));
        itemConf.setConfigItemType(configItemType);
        configElement.addItem(itemConf);
    }

    private boolean readBooleanAttribute(String attributeName, Element item, boolean defaultValue) {
        String strValue = item.attributeValue(attributeName);
        return StringUtils.isBlank(strValue) ? defaultValue : Boolean.valueOf(strValue);
    }

}
