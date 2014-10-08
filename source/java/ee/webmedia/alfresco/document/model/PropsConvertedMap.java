package ee.webmedia.alfresco.document.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.utils.TextUtil;

/**
 *         converts unit structure for binding from JSP.
 *         Refactored from @Document class
 */
public class PropsConvertedMap extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    private final Map<String, Object> props;
    private final boolean structUnit;

    public PropsConvertedMap(Map<String, Object> props, boolean structUnit) {
        this.props = props;
        this.structUnit = structUnit;
    }

    @Override
    public Object get(Object propKey) {
        Object propValue = props.get(propKey);
        if (!(propValue instanceof Serializable)) {
            return propValue == null ? "" : propValue.toString();
        }
        return TextUtil.formatDocumentPropertyValue((Serializable) propValue, structUnit ? FieldType.STRUCT_UNIT : FieldType.TEXT_FIELD, "");
    }

}
