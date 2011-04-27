package ee.webmedia.alfresco.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.lucene.queryParser.QueryParser;

/**
 * @author Alar Kvell
 */
public class SearchUtil {

    public static FastDateFormat luceneDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'00:00:00.000");

    public static String formatLuceneDate(Date date) {
        return luceneDateFormat.format(date);
    }

    public static boolean isBlank(List<String> list) {
        boolean blank = true;
        for (String element : list) {
            if (StringUtils.isNotBlank(element)) {
                blank = false;
                break;
            }
        }
        return blank;
    }

    /**
     * Replace characters that have special meaning in Lucene query.
     * 
     * @see QueryParser#escape(String)
     */
    // TODO use replaceCustom instead, it replaces fewer characters
    public static String replace(String s, String replacement) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '"' || c == '{'
                    || c == '}' || c == '~' || c == '*' || c == '?' || c == '|' || c == '&') {
                // replace special characters
                sb.append(replacement);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Generate Lucene query for searching nodes by property values. Input string is tokenized by space and search query is constructed so that all tokens must
     * be present in any node property (AND search).
     * 
     * @param input search string, all special characters must be stripped or escaped previously
     * @param type node type, may be {@code null}
     * @param props node properties that are searched
     * @return Lucene search query
     */
    public static String generateQuery(String input, QName type, Set<QName> props) {
        StringBuilder query = new StringBuilder(128);
        if (type != null) {
            query.append("+(TYPE:\"").append(QueryParser.escape(type.toString())).append("\")");
        }
        for (StringTokenizer t = new StringTokenizer(input, " "); t.hasMoreTokens();) {
            String term = t.nextToken();
            query.append(" +(");
            for (QName prop : props) {
                query.append(" @").append(QueryParser.escape(prop.toString())).append(":\"*");
                query.append(term);
                query.append("*\"");
            }
            query.append(")");
        }
        return query.toString();
    }

    /**
     * Generate Lucene query for searching nodes by property values. Input string is tokenized by space and search query is constructed so that all tokens must
     * be present in any node property (AND search).
     * 
     * @param input search string, all special characters must be stripped or escaped previously
     * @param type node type, may be {@code null}
     * @param props node property that is searched
     * @return Lucene search query
     */
    public static String generateQuery(String input, QName type, QName prop) {
        Set<QName> props = new HashSet<QName>(1);
        props.add(prop);
        return generateQuery(input, type, props);
    }

    /**
     * Replace a custom set of characters that have special meaning in Lucene query, others need to be replaced outside of this method.
     * Compared to default set we don't replace +, -, &
     * 
     * @see QueryParser#escape(String) for default set
     */
    public static String replaceCustom(String s, String replacement) {
        // \ ! ( ) : ^ [ ] " { } ~ * ? | , ´ ` ; + _ < > ½ § = % $ ¤ # £ ¹ ˇ ¬ … ' && ..
        // Not done . - / @ &
        StringBuffer sb = new StringBuffer();
        boolean skip = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' || c == '.') {
                if (s.length() > i + 1 && s.charAt(i + 1) == c) {
                    skip = true;
                    continue;
                }
                if (skip) {
                    skip = false;
                    continue;
                }
            }
            if (c == '\\' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '"' || c == '{' || c == '}' || c == '~'
                    || c == '*' || c == '?' || c == '|' || c == ',' || c == '´' || c == '`' || c == ';' || c == '+' || c == '_' || c == '<'
                    || c == '>' || c == '½' || c == '§' || c == '=' || c == '%' || c == '$' || c == '¤' || c == '#' || c == '£' || c == '¹' || c == 'ˇ'
                    || c == '¬' || c == '…' || c == '\'') {
                sb.append(replacement);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Not so beautiful workaround for the problem where special symbols like +, -, _, / etc cause a problem with
     * Alfresco lucene query parser and break the query.
     * For example *11-21/344* will find the correct results, *11-21* will also find the correct results
     * but *11-21/* will be replaced with *11-21 (missing the end wildcard) and will not find the correct results.
     */
    public static String stripCustom(String s) {
        int firstChar = 0;
        int lastChar = s.length();
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetterOrDigit(s.charAt(i))) {
                firstChar = i;
                break;
            }
        }
        for (int i = s.length(); i > 0; i--) {
            if (Character.isLetterOrDigit(s.charAt(i - 1))) {
                lastChar = i;
                break;
            }
        }
        if (lastChar > firstChar) {
            return s.substring(firstChar, lastChar);
        }
        return "";
    }

    // Low-level generation

    public static String generatePropertyExactQuery(QName documentPropName, String value, boolean escape) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (escape) {
            value = QueryParser.escape(stripCustom(value));
        }
        return "@" + Repository.escapeQName(documentPropName) + ":\"" + value + "\"";
    }

    private static String generatePropertyNotEmptyQuery(QName documentPropName) {
        return "@" + Repository.escapeQName(documentPropName) + ":*";
    }

    public static String generatePropertyBooleanQuery(QName documentPropName, boolean value) {
        return "@" + Repository.escapeQName(documentPropName) + ":" + Boolean.toString(value);
    }

    public static String generatePropertyNullQuery(QName documentPropName) {
        return "ISNULL:" + Repository.escapeQName(documentPropName);
    }

    public static String generatePropertyNotNullQuery(QName documentPropName) {
        return "ISNOTNULL:" + Repository.escapeQName(documentPropName);
    }

    public static String generatePropertyUnsetQuery(QName documentPropName) {
        return "ISUNSET:" + Repository.escapeQName(documentPropName);
    }

    public static String generatePropertyWildcardQuery(QName documentPropName, String value, boolean escape) {
        return generatePropertyWildcardQuery(documentPropName, value, escape, true, true);
    }

    public static String generatePropertyWildcardQuery(QName documentPropName, String value, boolean escape, boolean leftWildcard, boolean rightWildcard) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (escape) {
            value = QueryParser.escape(stripCustom(value));
        }
        return "@" + Repository.escapeQName(documentPropName) + ":\"" + (leftWildcard ? "*" : "") + value + (rightWildcard ? "*" : "") + "\"";
    }

    public static String generatePropertyDateQuery(QName documentPropName, Date date) {
        if (date == null) {
            return null;
        }
        // format is like dateProp:"2010-01-08T00:00:00.000"
        return generatePropertyExactQuery(documentPropName, formatLuceneDate(date), false);
    }

    // High-level generation

    public static String generateTypeQuery(QName... documentTypes) {
        return generateTypeQuery(Arrays.asList(documentTypes));
    }

    public static String generateTypeQuery(Collection<QName> documentTypes) {
        if (documentTypes == null || documentTypes.size() == 0) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(documentTypes.size());
        for (QName documentType : documentTypes) {
            queryParts.add("TYPE:" + Repository.escapeQName(documentType));
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateNotTypeQuery(QName... documentTypes) {
        return generateNotTypeQuery(Arrays.asList(documentTypes));
    }

    public static String generateNotTypeQuery(List<QName> documentTypes) {
        if (documentTypes == null || documentTypes.size() == 0) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(documentTypes.size());
        for (QName documentType : documentTypes) {
            queryParts.add("TYPE:" + Repository.escapeQName(documentType));
        }
        return "NOT (" + joinQueryPartsOr(queryParts, false) + ")";
    }

    public static String generateAspectQuery(QName... documentTypes) {
        if (documentTypes == null || documentTypes.length == 0) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(documentTypes.length);
        for (QName documentType : documentTypes) {
            queryParts.add("ASPECT:" + Repository.escapeQName(documentType));
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateStringWordsWildcardQuery(List<String> words, QName... documentPropNames) {
        if (words.isEmpty()) {
            return null;
        }
        List<String> wordQueryParts = new ArrayList<String>(words.size());
        for (String word : words) {
            List<String> propQueryParts = new ArrayList<String>(documentPropNames.length);
            for (QName documentPropName : documentPropNames) {
                propQueryParts.add(generatePropertyWildcardQuery(documentPropName, word, false));
            }
            wordQueryParts.add(joinQueryPartsOr(propQueryParts, false));
        }
        return joinQueryPartsAnd(wordQueryParts);
    }

    public static String generateNodeRefQuery(NodeRef value, QName... documentPropNames) {
        if (value == null) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(documentPropNames.length);
        for (QName documentPropName : documentPropNames) {
            queryParts.add(generatePropertyExactQuery(documentPropName, value.toString(), true));
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateMultiNodeRefQuery(List<NodeRef> values, QName... documentPropNames) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(documentPropNames.length);
        for (NodeRef value : values) {
            for (QName documentPropName : documentPropNames) {
                queryParts.add(generatePropertyExactQuery(documentPropName, value.toString(), true));
            }
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateStringExactQuery(String value, QName... documentPropNames) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(documentPropNames.length);
        for (QName documentPropName : documentPropNames) {
            queryParts.add(generatePropertyExactQuery(documentPropName, value, true));
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateStringNullQuery(QName... documentPropNames) {
        List<String> queryParts = new ArrayList<String>(documentPropNames.length);
        for (QName documentPropName : documentPropNames) {
            queryParts.add(generatePropertyNullQuery(documentPropName));
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateStringNotEmptyQuery(QName... documentPropNames) {
        List<String> queryParts = new ArrayList<String>(documentPropNames.length);
        for (QName documentPropName : documentPropNames) {
            queryParts.add(generatePropertyNotEmptyQuery(documentPropName));
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateMultiStringExactQuery(List<String> values, QName... documentPropNames) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(documentPropNames.length * values.size());
        for (String value : values) {
            for (QName documentPropName : documentPropNames) {
                queryParts.add(generatePropertyExactQuery(documentPropName, value, true));
            }
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateDatePropertyRangeQuery(Date beginDate, Date endDate, QName... documentPropNames) {
        if (beginDate == null && endDate == null) {
            return null;
        }
        // if (beginDate != null && endDate != null && beginDate.after(endDate))
        // then we don't display an error message. generated query won't find anything
        String begin = "MIN";
        String end = "MAX";
        if (beginDate != null) {
            begin = formatLuceneDate(beginDate);
        }
        if (endDate != null) {
            end = formatLuceneDate(endDate);
        }
        List<String> queryParts = new ArrayList<String>(documentPropNames.length);
        for (QName documentPropName : documentPropNames) {
            String query = "@" + Repository.escapeQName(documentPropName) + ":[" + begin + " TO " + end + "]";
            queryParts.add(query);
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateDoublePropertyRangeQuery(Double minValue, Double maxValue, QName... documentPropNames) {
        if (minValue == null && maxValue == null) {
            return null;
        }
        // if (minValue != null && maxValue != null && maxValue < minValue)
        // then we don't display an error message. generated query won't find anything
        String begin = "MIN";
        String end = "MAX";
        if (minValue != null) {
            begin = minValue.toString();
        }
        if (maxValue != null) {
            end = maxValue.toString();
        }
        List<String> queryParts = new ArrayList<String>(documentPropNames.length);
        for (QName documentPropName : documentPropNames) {
            String query = "@" + Repository.escapeQName(documentPropName) + ":[" + begin + " TO " + end + "]";
            queryParts.add(query);
        }
        return joinQueryPartsOr(queryParts, false);
    }

    // Join

    public static String joinQueryPartsAnd(List<String> queryParts) {
        return joinQueryParts(queryParts, "AND", true);
    }

    public static String joinQueryPartsAnd(List<String> queryParts, boolean parenthesis) {
        return joinQueryParts(queryParts, "AND", parenthesis);
    }

    public static String joinQueryPartsOr(List<String> queryParts) {
        return joinQueryParts(queryParts, "OR", true);
    }

    public static String joinQueryPartsOr(List<String> queryParts, boolean parenthesis) {
        return joinQueryParts(queryParts, "OR", parenthesis);
    }

    public static String joinQueryParts(List<String> queryParts, String keyword, boolean parenthesis) {
        if (queryParts == null || queryParts.size() == 0) {
            return "";
        }
        if (queryParts.size() == 1) {
            return queryParts.get(0);
        }
        StringBuilder query = new StringBuilder(queryParts.size() * 100);
        for (String queryPart : queryParts) {
            if (StringUtils.isNotBlank(queryPart)) {
                if (query.length() > 0) {
                    query.append(" ").append(keyword).append(" ");
                }
                if (parenthesis) {
                    query.append("(");
                }
                query.append(queryPart);
                if (parenthesis) {
                    query.append(")");
                }
            }
        }
        return query.toString();
    }

    public static boolean isStringProperty(QName dataType) {
        return dataType.equals(DataTypeDefinition.TEXT) || dataType.equals(DataTypeDefinition.INT) || dataType.equals(DataTypeDefinition.LONG) ||
                dataType.equals(DataTypeDefinition.FLOAT) || dataType.equals(DataTypeDefinition.DOUBLE) || dataType.equals(DataTypeDefinition.CONTENT);
    }

    public static boolean isDateProperty(QName dataType) {
        return dataType.equals(DataTypeDefinition.DATE) || dataType.equals(DataTypeDefinition.DATETIME);
    }

}
