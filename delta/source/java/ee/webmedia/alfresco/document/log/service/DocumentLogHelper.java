package ee.webmedia.alfresco.document.log.service;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Helper class for composing Document property change log messages.
 * 
 * @see DocumentPropertiesChangeHolder
 * @author Martti Tamm
 */
public class DocumentLogHelper {

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy");

    public static String getFunctionName(NodeRef functionRef, String emptyValueText) {
        if (functionRef == null) {
            return emptyValueText;
        }

        NodeService nodeService = BeanHelper.getNodeService();
        String mark = (String) nodeService.getProperty(functionRef, FunctionsModel.Props.MARK);
        String title = (String) nodeService.getProperty(functionRef, FunctionsModel.Props.TITLE);
        return new StringBuilder(mark).append(' ').append(title).toString();
    }

    public static String getSeriesName(NodeRef seriesRef, String emptyValueText) {
        if (seriesRef == null) {
            return emptyValueText;
        }

        NodeService nodeService = BeanHelper.getNodeService();
        String identifier = (String) nodeService.getProperty(seriesRef, SeriesModel.Props.SERIES_IDENTIFIER);
        String title = (String) nodeService.getProperty(seriesRef, SeriesModel.Props.TITLE);
        return new StringBuilder(identifier).append(' ').append(title).toString();
    }

    public static String getVolumeName(NodeRef volumeRef, String emptyValueText) {
        if (volumeRef == null) {
            return emptyValueText;
        }

        NodeService nodeService = BeanHelper.getNodeService();
        String mark = (String) nodeService.getProperty(volumeRef, VolumeModel.Props.MARK);
        String title = (String) nodeService.getProperty(volumeRef, VolumeModel.Props.TITLE);
        return new StringBuilder(mark).append(' ').append(title).toString();
    }

    public static String getCaseName(NodeRef caseRef, String emptyValueText) {
        if (caseRef == null) {
            return emptyValueText;
        }

        NodeService nodeService = BeanHelper.getNodeService();
        return (String) nodeService.getProperty(caseRef, CaseModel.Props.TITLE);
    }

    public static String msg(Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, String msgCode, QName prop, String oldValue, String newValue, String... otherParams) {
        Object[] args = ArrayUtils.addAll(new Object[] { propDefs.get(prop.getLocalName()).getSecond().getName(), oldValue, newValue }, otherParams);
        return MessageUtil.getMessage(msgCode, args);
    }

    public static Map<QName, Field> getDocumentTypeProps(NodeRef docRef) {
        WmNode docNode = BeanHelper.getDocumentDynamicService().getDocument(docRef).getNode();
        Pair<String, Integer> typeAndVersion = DocAdminUtil.getDocTypeIdAndVersionNr(docNode);
        DocumentTypeVersion docTypeVersion = BeanHelper.getDocumentAdminService().getDocumentTypeAndVersion(typeAndVersion.getFirst(), typeAndVersion.getSecond()).getSecond();
        List<Field> fields = docTypeVersion.getFieldsDeeply();

        Map<QName, Field> docTypeProps = new LinkedHashMap<QName, Field>(fields.size(), 1);
        for (Field field : fields) {
            docTypeProps.put(field.getQName(), field);
        }
        return docTypeProps;
    }

    public static String[] format(Field field, PropertyChange propChange, String emptyValue) {
        String[] result = new String[2];
        FieldType fieldType = field.getFieldTypeEnum();

        result[0] = formatValue(propChange.getOldValue(), fieldType, emptyValue);
        result[1] = formatValue(propChange.getNewValue(), fieldType, emptyValue);

        return result;
    }

    @SuppressWarnings("unchecked")
    private static String formatValue(Serializable value, FieldType fieldType, String emptyValue) {
        String result = null;
        if (value instanceof List) {
            String[] listItems = new String[((List<?>) value).size()];
            int pos = 0;
            for (Serializable valueItem : (List<Serializable>) value) {
                listItems[pos++] = formatSingleValue(valueItem, fieldType, emptyValue);
            }
            result = StringUtils.join(listItems, ", ");
        } else {
            result = formatSingleValue(value, fieldType, emptyValue);
        }
        return StringUtils.defaultIfEmpty(result, emptyValue);
    }

    private static String formatSingleValue(Serializable value, FieldType fieldType, String emptyValue) {
        String result = null;

        if (isEmpty(value)) {
            result = emptyValue;
        } else if (value instanceof Boolean) {
            String msgKey = (Boolean) value ? "yes" : "no";
            result = MessageUtil.getMessage(msgKey);
        } else if (value instanceof Date) {
            result = DATE_FORMAT.format((Date) value);
        } else if (FieldType.STRUCT_UNIT == fieldType) {
            @SuppressWarnings("unchecked")
            List<String> orgStruct = (List<String>) value;
            result = UserUtil.getDisplayUnit(orgStruct);
        } else {
            result = value.toString();
        }

        return result;
    }

    private static boolean isEmpty(Serializable value) {
        return value == null || value instanceof String && StringUtils.isBlank((String) value);
    }
}
