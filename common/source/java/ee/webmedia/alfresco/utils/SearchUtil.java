package ee.webmedia.alfresco.utils;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.repo.search.impl.lucene.LuceneQueryParser;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.lucene.queryParser.QueryParser;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.series.model.SeriesModel;

public class SearchUtil {

    public static FastDateFormat luceneDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'00:00:00.000");

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d\\d?\\.\\d\\d?\\.\\d\\d\\d\\d");

    /**
     * Query for retrieving series where document access is restricted. Searches using {@link #generateDocAccess(List, String)} can use this query to find such series.
     */
    public static final String QUERY_RESTRICTED_SERIES = joinQueryPartsAnd(generateTypeQuery(SeriesModel.Types.SERIES),
            generatePropertyBooleanQuery(SeriesModel.Props.DOCUMENTS_VISIBLE_FOR_USERS_WITHOUT_ACCESS, false));

    /**
     * @param date
     * @return "yyyy-MM-dd'T'00:00:00.000" if the property is not residual, else "yyyy-MM-dd"
     */
    private static String formatLuceneDate(Date date) {
        return luceneDateFormat.format(date);
    }

    /**
     * Not so beautiful workaround for the problem where special symbols like +, -, _, / etc cause a problem with
     * Alfresco lucene query parser and break the query.
     * For example *11-21/344* will find the correct results, *11-21* will also find the correct results
     * but *11-21/* will be replaced with *11-21 (missing the end wildcard) and will not find the correct results.
     */
    private static String stripCustom(String s) {
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

    /**
     * @param propName
     * @param acceptablePropertyValues
     * @param escape - should values be escaped?
     * @return Lucene query string that accepts any given property value for given property
     */
    public static String generatePropertyExactQuery(QName propName, Collection<String> acceptablePropertyValues) {
        List<String> queryParts = new ArrayList<String>();
        for (String value : acceptablePropertyValues) {
            queryParts.add(generatePropertyExactQuery(propName, value));
        }
        return joinQueryPartsOr(queryParts);
    }

    public static String generatePropertyExactQuery(QName propName, String value) {
        return generatePropertyExactQuery(propName, value, true);
    }

    public static String generatePropertyExactQuery(QName propName, String value, boolean stripCustom) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return "@" + Repository.escapeQName(propName) + ":\"" + (stripCustom ? QueryParser.escape(stripCustom(value)) : QueryParser.escape(value)) + "\"";
    }

    public static String generatePropertyExactNotQuery(QName documentPropName, String value) {
        return "NOT " + generatePropertyExactQuery(documentPropName, value);
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

    /**
     * Generates "VALUE:xxx" query where VALUE is a custom indexed Field with document property values. So this clause can only be used in document search.
     * <p>
     * Only String and date (in format: dd.MM.yyyy) values are indexed there. File contents are not indexed there.
     *
     * @param value The document property value to search for.
     * @return The generated clause as string.
     */
    public static String generateValuesWildcardQuery(String value) {
        return "VALUES:\"" + QueryParser.escape(stripCustom(value)) + "*\"";
    }

    /** Generates a query that searches all nodes in any depth below the specified path */
    public static String generateParentPathQuery(String path) {
        return "PATH:\"" + QueryParser.escape(path) + "//*\"";
    }

    public static String generateIdExactQuery(List<NodeRef> documentsForPermissionCheck) {
        List<String> queryParts = new ArrayList<>();
        for (NodeRef nodeRef : documentsForPermissionCheck) {
            queryParts.add("ID:\"" + QueryParser.escape(stripCustom(nodeRef.toString())) + "\"");
        }
        return joinQueryPartsOr(queryParts);
    }

    public static String generatePropertyWildcardQuery(QName documentPropName, String value, boolean leftWildcard, boolean rightWildcard) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return "@" + Repository.escapeQName(documentPropName) + ":\"" + (leftWildcard ? "*" : "") + QueryParser.escape(stripCustom(value)) + (rightWildcard ? "*" : "") + "\"";
    }

    public static String generateParentQuery(NodeRef parentRef) {
        return "PARENT:\"" + QueryParser.escape(parentRef.toString()) + "\"";
    }

    public static String generatePrimaryParentQuery(NodeRef parentRef) {
        return "PRIMARYPARENT:\"" + QueryParser.escape(parentRef.toString()) + "\"";
    }

    public static String generatePropertyDateQuery(QName documentPropName, Date date) {
        if (date == null) {
            return null;
        }
        return generatePropertyExactQuery(documentPropName, formatLuceneDate(date));
    }

    // High-level generation

    public static String generateTypeQuery(QName... nodeTypes) {
        return generateTypeQuery(Arrays.asList(nodeTypes));
    }

    public static String generateTypeQuery(Collection<QName> nodeTypes) {
        if (nodeTypes == null || nodeTypes.isEmpty()) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(nodeTypes.size());
        for (QName documentType : nodeTypes) {
            queryParts.add("TYPE:" + Repository.escapeQName(documentType));
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateNotTypeQuery(QName... documentTypes) {
        return generateNotTypeQuery(Arrays.asList(documentTypes));
    }

    public static String generateNotTypeQuery(List<QName> documentTypes) {
        if (documentTypes == null || documentTypes.isEmpty()) {
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

    public static String generateAspectMissingQuery(QName aspect) {
        if (aspect == null) {
            return null;
        }
        return "-ASPECT:" + Repository.escapeQName(aspect);
    }

    public static String generateStringWordsWildcardQuery(List<String> words, boolean leftWildcard, boolean rightWildcard, QName... documentPropNames) {
        if (words.isEmpty()) {
            return null;
        }
        List<String> wordQueryParts = new ArrayList<String>(words.size());
        for (String word : words) {
            List<String> propQueryParts = new ArrayList<String>(documentPropNames.length);
            for (QName documentPropName : documentPropNames) {
                propQueryParts.add(generatePropertyWildcardQuery(documentPropName, word, leftWildcard, rightWildcard));
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
            queryParts.add(generatePropertyExactQuery(documentPropName, value.toString()));
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
                queryParts.add(generatePropertyExactQuery(documentPropName, value.toString()));
            }
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateNotMultiNodeRefQuery(String positiveArgument, List<NodeRef> values, QName... documentPropNames) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        Assert.isTrue(StringUtils.isNotBlank(positiveArgument));
        List<String> queryParts = new ArrayList<String>(documentPropNames.length + 1);
        queryParts.add(positiveArgument);
        for (NodeRef value : values) {
            for (QName documentPropName : documentPropNames) {
                queryParts.add("NOT " + generateStringExactQuery(value.toString(), documentPropName));
            }
        }
        return joinQueryPartsAnd(queryParts, false);
    }

    public static String generateStringExactQuery(String value, QName... documentPropNames) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(documentPropNames.length);
        for (QName documentPropName : documentPropNames) {
            queryParts.add(generatePropertyExactQuery(documentPropName, value));
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateAndNotQuery(String query1, String query2) {
        return "(" + query1 + ") AND NOT (" + query2 + ")";
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
        return generateMultiStringExactQuery(values, true, documentPropNames);
    }

    public static String generateMultiStringExactQuery(List<String> values, boolean stripCustom, QName... documentPropNames) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(documentPropNames.length * values.size());
        for (String value : values) {
            for (QName documentPropName : documentPropNames) {
                queryParts.add(generatePropertyExactQuery(documentPropName, value, stripCustom));
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
            String query = "@" + Repository.escapeQName(documentPropName) + ":[" + QueryParser.escape(begin) + " TO " + QueryParser.escape(end) + "]";
            queryParts.add(query);
        }
        return joinQueryPartsOr(queryParts, false);
    }

    public static String generateNumberPropertyRangeQuery(Number minValue, Number maxValue, QName... documentPropNames) {
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

    public static String joinQueryPartsAnd(String... queryParts) {
        return joinQueryPartsAnd(Arrays.asList(queryParts));
    }

    public static String joinQueryPartsAnd(List<String> queryParts) {
        return joinQueryParts(queryParts, "AND", true);
    }

    /** NB! When using this method with parenthesis=false, ensure that subqueries have set required parenthesis themselves */
    public static String joinQueryPartsAnd(List<String> queryParts, boolean parenthesis) {
        return joinQueryParts(queryParts, "AND", parenthesis);
    }

    public static String joinQueryPartsOr(String... queryParts) {
        return joinQueryPartsOr(Arrays.asList(queryParts));
    }

    public static String joinQueryPartsOr(List<String> queryParts) {
        return joinQueryParts(queryParts, "OR", true);
    }

    /** NB! When using this method with parenthesis=false, ensure that subqueries have set required parenthesis themselves */
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

    public static SearchParameters generateLuceneSearchParams(String query, StoreRef store, int limit) {
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery(query);
        sp.addStore(store);
        if (limit < 0) {
            sp.setLimitBy(LimitBy.UNLIMITED);
        } else {
            sp.setLimit(limit + 1);
            sp.setLimitBy(LimitBy.FINAL_SIZE);
        }
        return sp;
    }

    /**
     * @param userId - if null, use current user
     */
    public static String generateDocAccess(List<NodeRef> restrictedSeriesRefs, String userId) {
        if (restrictedSeriesRefs.isEmpty() || getUserService().isAdministrator()) {
            return null;
        }

        if (userId == null) {
            userId = AuthenticationUtil.getRunAsUser();
        }

        Set<String> userGroups = new HashSet<String>();
        userGroups.add(userId);
        userGroups.addAll(getUserService().getUsersGroups(userId));

        List<String> query = new ArrayList<String>(userGroups.size() + 2);
        query.add(generateNotMultiNodeRefQuery(generateTypeQuery(DocumentCommonModel.Types.DOCUMENT), restrictedSeriesRefs, DocumentCommonModel.Props.SERIES));
        query.add(generateStringExactQuery(userId, DocumentCommonModel.Props.OWNER_ID));

        for (String group : userGroups) {
            if (StringUtils.isNotBlank(group)) {
                query.add("DOC_VISIBLE_TO:\"" + QueryParser.escape(group) + "\"");
            }
        }

        return joinQueryPartsOr(query);
    }

    /**
     * @param userId - if null, use current user
     */
    public static String generateSearchableDocListAccess(List<NodeRef> docRef, String userId) {
        if (docRef.isEmpty() || getUserService().isAdministrator()) {
            return null;
        }

        if (userId == null) {
            userId = AuthenticationUtil.getRunAsUser();
        }

        Set<String> userGroups = new HashSet<String>();
        userGroups.add(userId);
        userGroups.addAll(getUserService().getUsersGroups(userId));

        List<String> query = new ArrayList<String>(userGroups.size() + 2);
        query.add(generateStringExactQuery(userId, DocumentCommonModel.Props.OWNER_ID));
        for (String group : userGroups) {
            if (StringUtils.isNotBlank(group)) {
                query.add("DOC_VISIBLE_TO:\"" + QueryParser.escape(group) + "\"");
            }
        }

        return joinQueryPartsAnd(joinQueryPartsOr(query), generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
    }

    public static String generateUnsentDocQuery() {
        return LuceneQueryParser.IS_UNSENT_DOC_FIELD + ":\"" + QueryParser.escape(Boolean.TRUE.toString()) + "\"";
    }

    /**
     * Extracts dates (as 'dd.MM.yyyy' from given text) and stores them in given list in format 'ddMMyyyy'.
     * Duplicate formatted date values won't be added to the list.
     *
     * @param text A not null String value.
     * @param list A not null list where found dates will be stored.
     */
    public static void extractDates(String text, List<String> list) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        while (matcher.find()) {
            String date = matcher.group();

            // When day or month is not in two-digit form, add these missing zeros.
            if (date.length() < 10) {
                if (date.indexOf('.') != 2) {
                    date = "0" + date;
                }
                if (date.indexOf('.', 3) != 5) {
                    date = date.substring(0, 3) + "0" + date.substring(3);
                }
            }

            date = StringUtils.remove(date, '.'); // Store date as 'ddMMyyyy' (8-digit number).
            if (!list.contains(date)) {
                list.add(date);
            }
        }
    }

}
