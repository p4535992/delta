package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.ui.common.converter.MultiValueConverter;
import org.apache.commons.lang.StringUtils;

public class TextUtil {

    public static final String LIST_SEPARATOR = ", ";

    public static String joinNonBlankStringsWithComma(Collection<String> values) {
        return joinNonBlankStrings(values, ", ");
    }

    public static String joinNonBlankStrings(Collection<String> values, String separator) {
        StringBuilder s = new StringBuilder();
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                if (s.length() > 0) {
                    s.append(separator);
                }
                s.append(StringUtils.strip(value));
            }
        }
        return s.toString();
    }

    public static String collectionToString(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return new MultiValueConverter().getAsString(null, null, values);
    }

    public static String joinStringAndStringWithComma(String value1, String value2) {
        String separator = ", ";
        return joinStringAndStringWithSeparator(value1, value2, separator);
    }

    public static String joinStringAndStringWithSpace(String value1, String value2) {
        String separator = " ";
        return joinStringAndStringWithSeparator(value1, value2, separator);
    }

    public static String joinStringAndStringWithSeparator(String value1, String value2, String separator) {
        String result = "";
        if (StringUtils.isNotBlank(value1)) {
            result += value1;
        }
        if (StringUtils.isNotBlank(value2)) {
            if (StringUtils.isNotBlank(result)) {
                result += separator;
            }
            result += value2;
        }
        return result;
    }

    public static String joinStringAndStringWithParentheses(String value1, String value2) {
        String result = "";
        if (StringUtils.isNotBlank(value1)) {
            result += value1;
        }
        if (StringUtils.isNotBlank(value2)) {
            if (StringUtils.isNotBlank(result)) {
                result += " ";
            }
            result += "(" + value2 + ")";
        }
        return result;
    }

    /**
     * Case sensitive and does NOT strip/trim.
     */
    public static String joinUniqueStringsWithComma(Collection<String> values) {
        if (values == null) {
            return null;
        }
        List<String> uniqueValues = new ArrayList<String>();
        for (String value : values) {
            if (!uniqueValues.contains(value)) {
                uniqueValues.add(value);
            }
        }
        return StringUtils.join(uniqueValues, ", ");
    }

    /**
     * true if both parameters are blank or equal ignoring case
     * 
     * @param text1
     * @param text2
     * @return
     */
    public static boolean isBlankEqual(String text1, String text2) {
        if (StringUtils.isBlank(text1) && StringUtils.isBlank(text2)) {
            return true;
        }
        if (StringUtils.isBlank(text1) || StringUtils.isBlank(text2)) {
            return false;
        }
        return text1.equalsIgnoreCase(text2);
    }

    public static boolean isBlank(Collection<String> list) {
        if (list == null) {
            return true;
        }
        for (String element : list) {
            if (StringUtils.isNotBlank(element)) {
                return false;
            }
        }
        return true;
    }

    public static String join(Map<String, Object> propertiesMap, QName... props) {
        StringBuilder result = new StringBuilder();
        for (QName prop : props) {
            Object item = propertiesMap.get(prop);
            if (item instanceof Collection<?>) {
                @SuppressWarnings("unchecked")
                Collection<String> list = (Collection<String>) item;
                for (String textItem : list) {
                    if (StringUtils.isNotBlank(textItem)) {
                        if (result.length() > 0) {
                            result.append(LIST_SEPARATOR);
                        }
                        result.append(textItem);
                    }
                }
            } else {
                String textItem = (String) item;
                if (StringUtils.isNotBlank(textItem)) {
                    if (result.length() > 0) {
                        result.append(LIST_SEPARATOR);
                    }
                    result.append(textItem);
                }
            }
        }
        return result.toString();
    }
}
