package ee.webmedia.alfresco.template.web;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.TemplateType;
import ee.webmedia.alfresco.common.propertysheet.search.MultiSelectConverterBase;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Vladimir Drozdik
 *         Converts enum object name (document template type) into necessary local name.
 */
public class DocumentTemplateTypeConverter extends MultiSelectConverterBase {

    @Override
    public String convertSelectedValueToString(Object value) {
        String docTemplateTypeId = (String) value;
        if (StringUtils.isBlank(docTemplateTypeId)) {
            return "";
        }
        return MessageUtil.getMessage(TemplateType.valueOf(docTemplateTypeId));
    }
}
