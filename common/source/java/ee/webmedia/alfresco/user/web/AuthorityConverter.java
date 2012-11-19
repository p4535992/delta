package ee.webmedia.alfresco.user.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import ee.webmedia.alfresco.common.propertysheet.search.MultiSelectConverterBase;
import ee.webmedia.alfresco.user.model.Authority;

/**
 * @author Riina Tens
 */
public class AuthorityConverter extends MultiSelectConverterBase {

    @Override
    protected String convertSelectedValueToString(Object selectedValue) {
        Authority authority = getUserService().getAuthorityOrNull((String) selectedValue);
        if (authority.isGroup()) {
            return authority.getName();
        }
        return getUserService().getUserFullName(authority.getAuthority());
    }

}
