package ee.webmedia.alfresco.common.propertysheet.multivalueeditor;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.InlinePropertyGroupGenerator;
import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Helper class for building definitions of inner components of {@link MultiValueEditorGenerator} ( or even {@link InlinePropertyGroupGenerator})
 * 
 * @author Ats Uiboupin
 */
public class PropsBuilder {
    public static final String DEFAULT_OPTIONS_SEPARATOR = "¤";
    private final StringBuilder sb = new StringBuilder();

    public PropsBuilder(QName fieldId, String generatorName) {
        sb.append(fieldId.toPrefixString(BeanHelper.getNamespaceService())) // frist part is property name
                .append(DEFAULT_OPTIONS_SEPARATOR)
                .append(generatorName != null ? generatorName : "");// second part is generator name(can be empty for default generator)
    }

    public PropsBuilder addProp(String key, String value) {
        sb.append(DEFAULT_OPTIONS_SEPARATOR).append(key).append("=").append(value);
        return this;
    }

    public String build() {
        return sb.toString();
    }

}