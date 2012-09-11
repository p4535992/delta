package ee.webmedia.alfresco.user.web;

import ee.webmedia.alfresco.common.propertysheet.search.MultiSelectConverterBase;
import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * @author Priit Pikk
 */
public class UserConverter extends MultiSelectConverterBase {

    @Override
    public String convertSelectedValueToString(Object value) {
        String userId = (String) value;
        return BeanHelper.getUserService().getUserFullName(userId);
    }
}
