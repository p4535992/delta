package ee.webmedia.alfresco.utils;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentConfigService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Holder;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.converter.MultiValueConverter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;

public class TextUtil {

    public static final String LIST_SEPARATOR = ", ";
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy");

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
<<<<<<< HEAD
            if (list.size() >= 2 && ((list.get(0) instanceof String && list.get(1) instanceof String) || (list.get(0) instanceof List && list.get(1) instanceof List))
                    && FieldType.STRUCT_UNIT == fieldTypeGetter.getValue()) {
                result = UserUtil.getDisplayUnitText(value);
=======
            if (list.size() >= 2 && list.get(0) instanceof String && list.get(1) instanceof String && FieldType.STRUCT_UNIT == fieldTypeGetter.getValue()) {
                @SuppressWarnings("unchecked")
                List<String> orgStruct = (List<String>) value;
                result = UserUtil.getDisplayUnit(orgStruct);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
            String msgKey = (Boolean) value ? "yes" : "no";
            result = MessageUtil.getMessage(msgKey);
        } else if (value instanceof Date) {
            result = DATE_FORMAT.format((Date) value);
        } else {
<<<<<<< HEAD
            result = value.toString(); // TODO Alar: do Long and Double need specific formatting?
=======
            result = value.toString(); // TODO do Long and Double need specific formatting?
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
                QName propName = QName.createQName(DocumentDynamicModel.URI, formula);
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

}
