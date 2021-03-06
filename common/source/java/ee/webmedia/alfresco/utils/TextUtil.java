package ee.webmedia.alfresco.utils;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Holder;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.converter.MultiValueConverter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;

public class TextUtil {

    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(TextUtil.class);

    public static final String LIST_SEPARATOR = ", ";
    public static final String SEMICOLON_SEPARATOR = "; ";
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy");

    public static String joinNonBlankStringsWithComma(Collection<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return "";
        }
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

    public static String joinStringLists(List<String> firstValues, List<String> secondValues) {
        if (firstValues == null || secondValues == null) {
            return "";
        }
        List<String> keywordList = new ArrayList<String>(firstValues.size());
        for (int i = 0; i < firstValues.size(); i++) {
            String firstValue = firstValues.get(i);
            StringBuilder sb = new StringBuilder(firstValue != null ? firstValue : "");
            String secondValue = secondValues.get(i);
            if (StringUtils.isNotBlank(secondValue)) {
                sb.append(" - ").append(secondValues.get(i));
            }
            keywordList.add(sb.toString());
        }

        return StringUtils.join(keywordList, "; ");
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
            buildAndAppend(propertiesMap, prop, null, result);
        }
        return result.toString();
    }

    public static String join(Map<String, Object> propertiesMap, Map<QName, QName> propsWithAlternatives) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<QName, QName> entry : propsWithAlternatives.entrySet()) {
            buildAndAppend(propertiesMap, entry.getKey(), entry.getValue(), result);
        }
        return result.toString();
    }

    public static String join(Map<String, Object> propertiesMap, Map<QName, QName> propsWithAlternatives, String separator) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<QName, QName> entry : propsWithAlternatives.entrySet()) {
            buildAndAppend(propertiesMap, entry.getKey(), entry.getValue(), result, separator);
        }
        return result.toString();
    }

    public static String joinUsingInitialsForAlternativeValue(Map<String, Object> propertiesMap, Map<QName, QName> propsWithAlternatives) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<QName, QName> entry : propsWithAlternatives.entrySet()) {
            buildAndAppend(propertiesMap, entry.getKey(), entry.getValue(), result, true, LIST_SEPARATOR);
        }
        return result.toString();
    }

    private static void buildAndAppend(Map<String, Object> propertiesMap, QName property, QName altProperty, StringBuilder result) {
        buildAndAppend(propertiesMap, property, altProperty, result, false, LIST_SEPARATOR);
    }

    private static void buildAndAppend(Map<String, Object> propertiesMap, QName property, QName altProperty, StringBuilder result, String separator) {
        buildAndAppend(propertiesMap, property, altProperty, result, false, separator);
    }

    @SuppressWarnings("unchecked")
    private static void buildAndAppend(Map<String, Object> propertiesMap, QName property, QName altProperty, StringBuilder result, boolean useInitials, String separator) {
        Object item = propertiesMap.get(property);
        Object altItem = altProperty != null ? propertiesMap.get(altProperty) : null;
        if (item instanceof Collection<?>) {
            Collection<String> list = (Collection<String>) item;
            Collection<String> altList = (altItem instanceof Collection<?>) ? (Collection<String>) altItem : null;
            Iterator<String> altIterator = altList != null ? altList.iterator() : null;
            for (String textItem : list) {
                String altTextItem = altIterator != null ? altIterator.next() : null;
                if (!appendIfNotBlank(result, textItem, separator)) {
                    appendIfNotBlank(result, useInitials ? UserUtil.getInitials(altTextItem) : altTextItem, separator);
                }
            }
        } else {
            String textItem = (String) item;
            if (!appendIfNotBlank(result, textItem, separator) && altItem instanceof String) {
                appendIfNotBlank(result, useInitials ? UserUtil.getInitials((String) altItem) : (String) altItem, separator);
            }
        }
    }

    private static boolean appendIfNotBlank(StringBuilder result, String stringToAppend, String separator) {
        boolean valueAdded = false;
        if (StringUtils.isNotBlank(stringToAppend)) {
            if (result.length() > 0) {
                result.append(separator);
            }
            result.append(stringToAppend);
            valueAdded = true;
        }
        return valueAdded;
    }

    public static String formatDateOrEmpty(FastDateFormat dateFormat, Date date) {
        if (date == null) {
            return "";
        }
        return dateFormat.format(date);
    }

    public static boolean isValueOrListNotBlank(Object value) {
        if (value instanceof String) {
            if (StringUtils.isNotBlank((String) value)) {
                return true;
            }
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            for (Object listValue : list) {
                if (listValue instanceof String) {
                    if (StringUtils.isNotBlank((String) listValue)) {
                        return true;
                    }
                } else {
                    if (listValue != null) {
                        return true;
                    }
                }
            }
        } else {
            if (value != null) {
                return true;
            }
        }
        return false;
    }

    public interface ValueGetter<T> {
        T getValue();
    }

    public static class StaticValueGetter<T> implements ValueGetter<T> {

        private final T value;

        public StaticValueGetter(T value) {
            this.value = value;
        }

        @Override
        public T getValue() {
            return value;
        }

    }

    public static String formatDocumentPropertyValue(Serializable value, FieldType fieldType, String emptyValue) {
        return formatDocumentPropertyValue(value, new StaticValueGetter<FieldType>(fieldType), emptyValue);
    }

    public static String formatDocumentPropertyValue(Serializable value, ValueGetter<FieldType> fieldTypeGetter, String emptyValue) {
        String result = null;
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Serializable> list = (List<Serializable>) value;
            if (list.size() >= 2 && ((list.get(0) instanceof String && list.get(1) instanceof String) || (list.get(0) instanceof List && list.get(1) instanceof List))
                    && FieldType.STRUCT_UNIT == fieldTypeGetter.getValue()) {
                result = UserUtil.getDisplayUnitText(value);
            } else {
                String[] resultValues = new String[((List<?>) value).size()];
                int pos = 0;
                for (Serializable listValue : list) {
                    resultValues[pos++] = formatSingleValue(listValue, emptyValue);
                }
                result = StringUtils.isEmpty(emptyValue) ? joinNonBlankStringsWithComma(Arrays.asList(resultValues)) : StringUtils.join(resultValues, ", ");
            }
        } else {
            result = formatSingleValue(value, emptyValue);
        }
        return StringUtils.defaultIfEmpty(result, emptyValue);
    }

    private static String formatSingleValue(Serializable value, String emptyValue) {
        String result;
        if (value == null || value instanceof String && StringUtils.isBlank((String) value)) {
            result = emptyValue;
        } else if (value instanceof Boolean) {
            result = (Boolean) value ? BeanHelper.getApplicationConstantsBean().getMessageYes() : BeanHelper.getApplicationConstantsBean().getMessageNo();
        } else if (value instanceof Date) {
            result = DATE_FORMAT.format((Date) value);
        } else {
            result = value.toString(); // TODO do Long and Double need specific formatting?
        }
        return result;
    }

    public static Map<String, String> getFormulaValues(List<String> formulas, final Node node) {
        Map<String, String> formulaValues = new HashMap<String, String>();
        final Holder<Map<String, Pair<DynamicPropertyDefinition, Field>>> propertyDefinitionsHolder = new Holder<Map<String, Pair<DynamicPropertyDefinition, Field>>>();
        for (final String formula : formulas) {
            if (formulaValues.containsKey(formula)) {
                continue;
            }
            String formulaValue = "";
            if (StringUtils.isNotBlank(formula)) {
                QName propName = RepoUtil.getFromQNamePool(formula, DocumentDynamicModel.URI, DocumentDynamicService.DOC_DYNAMIC_URI_PROPS_POOL);
                Serializable propValue = (Serializable) node.getProperties().get(propName);
                formulaValue = TextUtil.formatDocumentPropertyValue(propValue, new TextUtil.ValueGetter<FieldType>() {
                    @Override
                    public FieldType getValue() {
                        if (propertyDefinitionsHolder.value == null) {
                            propertyDefinitionsHolder.value = getDocumentConfigService().getPropertyDefinitions(node);
                        }
                        Pair<DynamicPropertyDefinition, Field> pair = propertyDefinitionsHolder.value.get(formula);
                        if (pair == null || pair.getSecond() == null) {
                            return null;
                        }
                        return pair.getSecond().getFieldTypeEnum();
                    }
                }, "");
            }
            formulaValues.put(formula, formulaValue);
        }
        return formulaValues;
    }

    public static String replaceLast(String string, String toReplace, String replacement) {
        int pos = string.lastIndexOf(toReplace);
        if (pos > -1) {
            return string.substring(0, pos)
                    + replacement
                    + string.substring(pos + toReplace.length(), string.length());
        }
        return string;
    }

    /**
     * @return decoded url using {@link ee.webmedia.alfresco.app.AppConstants#CHARSET} as encoding or original input if decoding fails
     */
    public static String decodeUrl(String str) {
        try {
            return URLDecoder.decode(str, AppConstants.CHARSET);
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    /**
     * Convert string to boolean
     * @param target
     * @return
     */
    public static boolean toBoolean(String target) {
        if (target == null) return false;
        return target.matches("(?i:^(1|y|Y|n|N|Yes|YES|yes|No|NO|no|true|false|True|False|TRUE|FALSE)$)");
    }

    /**
     * Converts string to boolean:
     * accepted string values: y|Y|n|N|Yes|YES|yes|No|NO|no|true|false|True|False|TRUE|FALSE|NULL (false)
     *
     * @param value
     * @return
     */
    public static Boolean formatStringToBoolean(String value) {
        if (value == null) {
            log.warn("formatStringToBoolean(): Value is NULL! Can't convert string to boolean! Return FALSE!");
            return false;
        }

        String[] values = new String[]{"y", "Y", "n", "N", "Yes", "YES", "yes", "no", "No", "NO", "true", "false", "True", "False", "TRUE", "FALSE", null};
        for (String booleanStr : values) {
            System.out.println("Str =" + booleanStr + ": boolean =" + BooleanUtils.toBoolean(booleanStr));
        }

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            log.debug("formatStringToBoolean(): Value exists! Convert to boolean: " + value);
            return Boolean.valueOf(value);
        }

        log.warn("formatStringToBoolean(): Value is not TRUE nor FALSE! Can't convert to boolean! Return FALSE!");
        return false;
    }

}
