package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class TextUtil {

    public static String joinNonBlankStringsWithComma(Collection<String> values) {
        StringBuilder s = new StringBuilder();
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                if (s.length() > 0) {
                    s.append(", ");
                }
                s.append(StringUtils.strip(value));
            }
        }
        return s.toString();
    }

    public static String joinStringAndStringWithComma(String value1, String value2) {
        String result = "";
        if (StringUtils.isNotBlank(value1)) {
            result += value1;
        }
        if (StringUtils.isNotBlank(value2)) {
            if (StringUtils.isNotBlank(result)) {
                result += ", ";
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

}
