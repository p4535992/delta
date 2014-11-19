<<<<<<< HEAD
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
=======
package ee.webmedia.alfresco.user.web;

import ee.webmedia.alfresco.common.propertysheet.search.MultiSelectConverterBase;
import ee.webmedia.alfresco.common.web.BeanHelper;

public class UserConverter extends MultiSelectConverterBase {

    @Override
    public String convertSelectedValueToString(Object value) {
        String userId = (String) value;
        return BeanHelper.getUserService().getUserFullName(userId);
    }
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
