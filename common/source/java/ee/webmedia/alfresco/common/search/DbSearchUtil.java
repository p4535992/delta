package ee.webmedia.alfresco.common.search;

import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsOr;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * Utility methods for creating db queries.
 */
public class DbSearchUtil {

    public static final String ACTIVE_FIELD = "wfs_active";
    public static final String TASK_TYPE_FIELD = "task_type";
    public static final String SEPARATOR = "_";

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
        StringBuilder queryBuilder = new StringBuilder();
        boolean coalesce = propNames.length > 1;
        for (QName propName : propNames) {
            appendFields(queryBuilder, coalesce, getDbFieldNameFromPropQName(propName));
        }
        return createStringWordsWildcardQuery(queryBuilder);
    }

    /** NB When using this method, ensure that corresponding indexes have been created in database */
    public static String generateStringWordsWildcardQuery(String... fieldNames) {
        StringBuilder queryBuilder = new StringBuilder();
        boolean coalesce = fieldNames.length > 1;
        for (String fieldName : fieldNames) {
            appendFields(queryBuilder, coalesce, fieldName);
        }
        return createStringWordsWildcardQuery(queryBuilder);
    }

    private static String createStringWordsWildcardQuery(StringBuilder queryBuilder) {
        queryBuilder.insert(0, "to_tsvector('simple', ");
        queryBuilder.append(") @@ ?::tsquery ");
        return queryBuilder.toString();
    }

    private static void appendFields(StringBuilder queryBuilder, boolean coalesce, String fieldName) {
        queryBuilder.append((queryBuilder.length() > 0 ? " || ' ' || " : "")
                + (coalesce ? "coalesce(" : "")
                + fieldName
                + (coalesce ? ", '')" : ""));
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
        return getDbFieldNameFromCamelCase(prefix, localName);
    }

    public static String getDbFieldNameFromCamelCase(String localName) {
        return getDbFieldNameFromCamelCase(null, localName);
    }

    private static String getDbFieldNameFromCamelCase(String prefix, String localName) {
        Assert.isTrue(StringUtils.isNotBlank(localName));
        StringBuilder sb = new StringBuilder(StringUtils.isNotBlank(prefix) ? (prefix + SEPARATOR) : "");
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

    public static String getPrefix(String prefix) {
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

    public static String generateTaskFieldNotQuery(String taskField) {
        return taskField + "!=? ";
    }

    public static String generateTaskPropertyExactQuery(QName propName) {
        return getDbFieldNameFromPropQName(propName) + "=? ";
    }

    public static String getQuestionMarks(int size) {
        Assert.isTrue(size > 0, "At least one question mark should be returned.");
        String questionMarks = StringUtils.repeat("?, ", size);
        questionMarks = questionMarks.substring(0, questionMarks.length() - 2);
        return questionMarks;
    }

    public static String generateTaskPropertyNotQuery(QName propName) {
        return getDbFieldNameFromPropQName(propName) + "!=? ";
    }

    public static String generateNotQuery(String query) {
        if (query == null || query.length() == 0) {
            return null;
        }
        return " NOT (" + query + ")";
    }

    public static String generateTaskStringHasLengthQuery(QName propName) {
        return generateTaskPropertyNotNullQuery(propName) + " AND length(" + getDbFieldNameFromPropQName(propName) + ") != 0";
    }

    public static String generateTaskPropertiesNotEqualQuery(QName propName1, QName propName2) {
        return getDbFieldNameFromPropQName(propName1) + "!= " + getDbFieldNameFromPropQName(propName2);
    }

    /**
     * Returns all NodeRef uuids from the given collection of NodeRefs.
     *
     * @param nodeRefs
     * @return Array of workflow uuids or null
     */
    public static List<Object> appendNodeRefIdQueryArguments(Collection<NodeRef> nodeRefs, Object... firstArguments) {
        int headLength = 0;
        if (firstArguments != null) {
            headLength = firstArguments.length;
        }

        int tailLength = 0;
        if (nodeRefs != null) {
            tailLength = nodeRefs.size();
        }

        List<Object> result = new ArrayList<>((headLength + tailLength));
        if (headLength > 0) {
            for (Object argument : firstArguments) {
                result.add(argument);
            }
        }
        if (tailLength > 0) {
            for (NodeRef nodeRef : nodeRefs) {
                result.add(nodeRef.getId());
            }
        }

        return result;
    }

    public static String createCommaSeparatedUpdateString(Collection<String> dbColumnNames) {
        StringBuffer sb = new StringBuffer();
        boolean isNotFirst = false;
        for (String fieldName : dbColumnNames) {
            if (isNotFirst) {
                sb.append(", ");
            }
            isNotFirst = true;
            sb.append(fieldName + "=?");
        }

        return sb.toString();
    }

    public static Map<QName, Serializable> buildSearchableSendInfos(List<SendInfo> sendInfos) {
        int size = sendInfos.size();
        ArrayList<String> sendModes = new ArrayList<>(size);
        ArrayList<String> sendRecipients = new ArrayList<>(size);
        ArrayList<Date> sendTimes = new ArrayList<>(size);
        ArrayList<String> sendResolutions = new ArrayList<>(size);
        for (SendInfo sendInfo : sendInfos) {
            sendModes.add(sendInfo.getSendMode());
            sendRecipients.add(sendInfo.getRecipient());
            sendTimes.add(sendInfo.getSendDateTime());
            sendResolutions.add(sendInfo.getResolution());
        }

        Map<QName, Serializable> props = new HashMap<>();
        props.put(DocumentCommonModel.Props.SEARCHABLE_SEND_MODE, sendModes);
        props.put(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_RECIPIENT, sendRecipients);
        props.put(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_SEND_DATE_TIME, sendTimes);
        props.put(DocumentCommonModel.Props.SEARCHABLE_SEND_INFO_RESOLUTION, sendResolutions);

        return props;
    }

    public static void setParameterValue(PreparedStatement statement, int fieldIndex, Object value) throws SQLException {
        if (value instanceof Date) {
            Timestamp timestamp = new Timestamp(((Date) value).getTime());
            statement.setTimestamp(fieldIndex, timestamp);
        } else if (value instanceof Boolean) {
            statement.setBoolean(fieldIndex, (Boolean) value);
        } else if (value instanceof Integer) {
            statement.setInt(fieldIndex, (Integer) value);
        } else if (value instanceof List) {
            statement.setObject(fieldIndex, statement.getConnection().createArrayOf("text", ((List) value).toArray()));
        } else {
            statement.setObject(fieldIndex, value);
        }
    }

}
