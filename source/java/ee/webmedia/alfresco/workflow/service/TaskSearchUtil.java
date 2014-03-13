package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * Utility methods for creating db queries for searching tasks.
 */
public class TaskSearchUtil {

    public static final String ACTIVE_FIELD = "wfs_active";
    public static final String TASK_TYPE_FIELD = "task_type";
    private static final String SEPARATOR = "_";

    public static Pair<String, List<Object>> generateTaskMultiStringExactQuery(List<String> values, QName... propNames) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(propNames.length * values.size());
        List<Object> arguments = new ArrayList<Object>();
        List<String> dbFieldNames = getDbFieldNamesFromPropQNames(propNames);
        for (String value : values) {
            for (String dbFieldName : dbFieldNames) {
                queryParts.add(dbFieldName + "=? ");
                arguments.add(value);
            }
        }
        return new Pair<String, List<Object>>(joinQueryPartsOr(queryParts, false), arguments);
    }

    public static Pair<String, List<Object>> generateTaskMultiStringArrayQuery(List<String> values, QName propName) {
        if (values == null || values.isEmpty() || propName == null) {
            return null;
        }
        List<String> queryParts = new ArrayList<String>(values.size());
        List<Object> arguments = new ArrayList<Object>();
        String dbFieldName = getDbFieldNameFromPropQName(propName);
        for (String value : values) {
            queryParts.add(dbFieldName + " @> ARRAY[?]::TEXT[] ");
            arguments.add(value);
        }
        return new Pair<String, List<Object>>(joinQueryPartsOr(queryParts, false), arguments);
    }

    public static Pair<String, List<Object>> generateTaskDatePropertyRangeQuery(Date beginDate, Date endDate, QName... propNames) {
        if (beginDate == null && endDate == null) {
            return null;
        }
        // if (beginDate != null && endDate != null && beginDate.after(endDate))
        // then we don't display an error message. generated query won't find anything
        boolean hasBeginDate = beginDate != null;
        boolean hasEndDate = endDate != null;
        List<String> queryParts = new ArrayList<String>(propNames.length);
        List<String> dbFieldNames = getDbFieldNamesFromPropQNames(propNames);
        List<Object> arguments = new ArrayList<Object>();
        for (String fieldName : dbFieldNames) {
            queryParts.add(joinQueryPartsAnd(hasBeginDate ? fieldName + " >= ? " : null,
                    hasEndDate ? fieldName + " <= ? " : null));
            if (hasBeginDate) {
                arguments.add(DateUtils.truncate(beginDate, Calendar.DATE));
            }
            if (hasEndDate) {
                Date end = DateUtils.truncate(endDate, Calendar.DATE);
                end.setHours(23);
                end.setMinutes(59);
                end.setSeconds(59);
                arguments.add(end);
            }

        }
        return new Pair<String, List<Object>>(joinQueryPartsOr(queryParts, false), arguments);
    }

    public static Pair<String, List<Object>> generateTaskPropertyBooleanQuery(QName propName, boolean value) {
        return new Pair<String, List<Object>>(getDbFieldNameFromPropQName(propName) + " = ?", Arrays.asList((Object) value));
    }

    /** NB When using this method, ensure that corresponding indexes have been created in database */
    public static String generateTaskStringWordsWildcardQuery(QName... propNames) {
        // List<String> propQueryParts = new ArrayList<String>(documentPropNames.length);
        StringBuilder queryBuilder = new StringBuilder();
        boolean coalesce = propNames.length > 1;
        for (QName propName : propNames) {
            queryBuilder.append((queryBuilder.length() > 0 ? " || ' ' || " : "")
                    + (coalesce ? "coalesce(" : "")
                    + getDbFieldNameFromPropQName(propName)
                    + (coalesce ? ", '')" : ""));
        }
        queryBuilder.insert(0, "to_tsvector('simple', ");
        queryBuilder.append(") @@ ?::tsquery ");
        return queryBuilder.toString();
    }

    public static List<String> getDbFieldNamesFromPropQNames(QName... propNames) {
        if (propNames == null) {
            return null;
        }
        List<String> fieldNames = new ArrayList<String>();
        for (QName propName : propNames) {
            fieldNames.add(getDbFieldNameFromPropQName(propName));
        }
        return fieldNames;
    }

    public static String getDbFieldNameFromPropQName(QName propName) {
        String prefix = WorkflowCommonModel.URI.equals(propName.getNamespaceURI()) ? getPrefix(WorkflowCommonModel.PREFIX) : (WorkflowSpecificModel.URI.equals(propName
                .getNamespaceURI())
                ? getPrefix(WorkflowSpecificModel.PREFIX) : "");
        Assert.isTrue(StringUtils.isNotBlank(prefix));
        String localName = propName.getLocalName();
        Assert.isTrue(StringUtils.isNotBlank(localName));
        StringBuilder sb = new StringBuilder(prefix + SEPARATOR);
        for (int i = 0; i < localName.length(); i++) {
            char c = localName.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(SEPARATOR + Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static QName getPropQNameFromDbFieldName(String fieldName) {
        String namespaceUri = null;
        String prefix = null;
        if (fieldName.startsWith(prefix = getPrefix(WorkflowCommonModel.PREFIX) + SEPARATOR)) {
            namespaceUri = WorkflowCommonModel.URI;
        } else if (fieldName.startsWith(prefix = getPrefix(WorkflowSpecificModel.PREFIX) + SEPARATOR)) {
            namespaceUri = WorkflowSpecificModel.URI;
        } else {
            return null;
        }
        StringTokenizer st = new StringTokenizer(fieldName.substring(prefix.length()), SEPARATOR);
        StringBuilder sb = new StringBuilder();
        boolean firstToken = true;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            char firstChar = firstToken ? token.charAt(0) : Character.toUpperCase(token.charAt(0));
            sb.append(firstChar + token.substring(1));
            firstToken = false;
        }
        return QName.createQName(namespaceUri, sb.toString());
    }

    private static String getPrefix(String prefix) {
        return (new StringTokenizer(prefix, ":")).nextToken();
    }

    public static String generateTaskFieldNotNullQuery(String activeField) {
        if (StringUtils.isBlank(activeField)) {
            return null;
        }
        return activeField + " IS NOT NULL ";
    }

    public static String generateTaskPropertyNullQuery(QName propName) {
        if (propName == null) {
            return null;
        }
        return getDbFieldNameFromPropQName(propName) + " IS NULL ";
    }

    public static String generateTaskPropertyNotNullQuery(QName propName) {
        if (propName == null) {
            return null;
        }
        return getDbFieldNameFromPropQName(propName) + " IS NOT NULL ";
    }

    public static String generateTaskFieldExactQuery(String taskField) {
        return taskField + "=? ";
    }

    public static String generateTaskPropertyExactQuery(QName propName) {
        return getDbFieldNameFromPropQName(propName) + "=? ";
    }

    public static String generateTaskPropertyNotQuery(QName propName) {
        return getDbFieldNameFromPropQName(propName) + "!=? ";
    }

    public static String generateTaskStringHasLengthQuery(QName propName) {
        return generateTaskPropertyNotNullQuery(propName) + " AND length(" + getDbFieldNameFromPropQName(propName) + ") != 0";
    }

    public static String generateTaskPropertiesNotEqualQuery(QName propName1, QName propName2) {
        return getDbFieldNameFromPropQName(propName1) + "!= " + getDbFieldNameFromPropQName(propName2);
    }

}
