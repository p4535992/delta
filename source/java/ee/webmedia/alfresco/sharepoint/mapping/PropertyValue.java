package ee.webmedia.alfresco.sharepoint.mapping;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * General property value handler class.
 */
public abstract class PropertyValue {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(PropertyValue.class);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    private final String prefix;

    private PropertyValue(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Evaluates a property value.
     * 
     * @param originalValue The current value of the property.
     * @param newValue The value to be parsed for the property
     * @return A new value for the property.
     */
    public final Serializable evaluate(Serializable originalValue, String newValue) {
        if (newValue == null) {
            return originalValue;
        }
        return evaluateInner(originalValue, newValue, prefix);
    }

    Serializable evaluateInner(Serializable originalValue, String newValue, String valuePrefix) {
        return evaluateInner(originalValue, valuePrefix == null ? newValue : valuePrefix + newValue);
    }

    abstract Serializable evaluateInner(Serializable originalValue, String newValue);

    @Override
    public String toString() {
        return prefix == null ? getClass().getSimpleName() : getClass().getSimpleName() + "[" + prefix + "]";
    }

    public static Serializable addComment(String currentPropValue, String prefix, String value) {
        return new CommentPropertyValue(prefix).evaluate(currentPropValue, value);
    }

    public static PropertyValue getHandler(PropertyDefinition propDef, String prefix) {
        QName prop = propDef.getName();
        String name = prop.getLocalName();
        String javaClassName = propDef.getDataType().getJavaClassName();

        PropertyValue result = null;

        if ("comment".equals(name) || "errandComment".equals(name) || "content".equals(name) || "price".equals(name) || "expenseType".equals(name)) {
            result = new CommentPropertyValue(prefix);
        } else if (java.util.Date.class.getName().equals(javaClassName)) {
            result = new DatePropertyValue(prefix);
        } else if (Double.class.getName().equals(javaClassName) || Long.class.getName().equals(javaClassName)) {
            result = new NumberPropertyValue(prefix, Long.class.getName().equals(javaClassName));
        } else if (!String.class.getName().equals(javaClassName)) {
            LOG.info("Data type " + javaClassName + " is not supported for property " + prop.toPrefixString(BeanHelper.getNamespaceService())
                    + ", not allowing mapping");
        } else {
            result = new StringPropertyValue(prefix);
        }

        if (result != null && propDef.isMultiValued()) {
            result = new MultiPropertyValue(result);
        }

        return result;
    }

    private static class StringPropertyValue extends PropertyValue {

        public StringPropertyValue(String prefix) {
            super(prefix);
        }

        @Override
        Serializable evaluateInner(Serializable originalValue, String newValue) {
            return join(", ", (String) originalValue, newValue);
        }
    }

    private static class CommentPropertyValue extends StringPropertyValue {

        private final static String SPACE_DELIMED = ".spaced";

        public CommentPropertyValue(String prefix) {
            super(prefix);
        }

        @Override
        Serializable evaluateInner(Serializable originalValue, String newValue, String valuePrefix) {

            if (StringUtils.isNotBlank(newValue)) {
                if (SPACE_DELIMED.equals(valuePrefix)) {
                    return join(" ", (String) originalValue, newValue);
                } else if (valuePrefix == null) {
                    return join("\n", (String) originalValue, newValue);
                }

                return join("\n", (String) originalValue, valuePrefix + ": " + newValue);
            }

            return originalValue;
        }
    }

    private static class DatePropertyValue extends PropertyValue {

        public DatePropertyValue(String prefix) {
            super(prefix);
        }

        @Override
        protected Serializable evaluateInner(Serializable originalValue, String newValue) {
            try {
                return DATE_FORMAT.parse(newValue);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class NumberPropertyValue extends PropertyValue {

        private final boolean longValue;

        public NumberPropertyValue(String prefix, boolean longValue) {
            super(prefix);
            this.longValue = longValue;
        }

        @Override
        protected Serializable evaluateInner(Serializable originalValue, String newValue) {
            if (newValue.isEmpty()) {
                return originalValue;
            }

            String number = extractNumber(newValue);

            if (number.isEmpty()) {
                throw new RuntimeException("Could not extract any number from '" + newValue + "'.");
            }

            try {
                return longValue ? Long.parseLong(number) : Double.parseDouble(number);
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }

        private static String extractNumber(String s) {
            boolean textAfterNumber = false;
            StringBuilder sb = new StringBuilder(s.length());

            for (Character c : s.toCharArray()) {
                if (Character.isDigit(c)) {
                    if (textAfterNumber) {
                        throw new RuntimeException("Cannot extract number from '" + s + "' - too many text-separated numbers.");
                    }
                    sb.append(c);
                } else if (c == '.' || c == ',') {
                    sb.append('.');
                } else if (Character.isLetter(c) && sb.length() > 0) {
                    textAfterNumber = true;
                }
            }
            return sb.toString();
        }
    }

    private static class MultiPropertyValue extends PropertyValue {

        PropertyValue inner;

        public MultiPropertyValue(PropertyValue inner) {
            super(null);
            this.inner = inner;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Serializable evaluateInner(Serializable originalValue, String newValue) {
            List<Serializable> result;
            if (originalValue instanceof List) {
                result = (List<Serializable>) originalValue;
            } else {
                result = new ArrayList<Serializable>(1);
            }
            result.add(inner.evaluate(null, newValue));
            return (Serializable) result;
        }
    }

    public static String join(String separator, String originalValue, String newValue) {
        if (originalValue == null) {
            return newValue;
        }
        return new StringBuilder(originalValue).append(separator).append(newValue).toString();
    }
}