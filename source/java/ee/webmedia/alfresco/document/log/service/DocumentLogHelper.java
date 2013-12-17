package ee.webmedia.alfresco.document.log.service;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.ArrayUtils;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Helper class for composing Document property change log messages.
 * 
 * @see DocumentPropertiesChangeHolder
 * @author Martti Tamm
 */
public class DocumentLogHelper {

    /**
     * Provides the complete name of given function.
     * 
     * @param functionRef Reference to function.
     * @param emptyValueText When function cannot be found, this empty value will be returned instead.
     * @return Function name (mark + name) or the empty value text.
     */
    public static String getFunctionName(NodeRef functionRef, String emptyValueText) {
        NodeService nodeService = BeanHelper.getNodeService();
        if (functionRef == null || !nodeService.exists(functionRef)) {
            return emptyValueText;
        }

        String mark = (String) nodeService.getProperty(functionRef, FunctionsModel.Props.MARK);
        String title = (String) nodeService.getProperty(functionRef, FunctionsModel.Props.TITLE);
        return new StringBuilder(mark).append(' ').append(title).toString();
    }

    /**
     * Provides the complete name of given series.
     * 
     * @param seriesRef Reference to series.
     * @param emptyValueText When series cannot be found, this empty value will be returned instead.
     * @return Series name (seriesIdentifier + name) or the empty value text.
     */
    public static String getSeriesName(NodeRef seriesRef, String emptyValueText) {
        NodeService nodeService = BeanHelper.getNodeService();
        if (seriesRef == null || !nodeService.exists(seriesRef)) {
            return emptyValueText;
        }

        String identifier = (String) nodeService.getProperty(seriesRef, SeriesModel.Props.SERIES_IDENTIFIER);
        String title = (String) nodeService.getProperty(seriesRef, SeriesModel.Props.TITLE);
        return new StringBuilder(identifier).append(' ').append(title).toString();
    }

    /**
     * Provides the complete name of volume.
     * 
     * @param volumeRef Reference to volume.
     * @param emptyValueText When volume cannot be found, this empty value will be returned instead.
     * @return Volume (mark + name) or the empty value text.
     */
    public static String getVolumeName(NodeRef volumeRef, String emptyValueText) {
        NodeService nodeService = BeanHelper.getNodeService();
        if (volumeRef == null || !nodeService.exists(volumeRef)) {
            return emptyValueText;
        }

        String mark = (String) nodeService.getProperty(volumeRef, VolumeModel.Props.MARK);
        String title = (String) nodeService.getProperty(volumeRef, VolumeModel.Props.TITLE);
        return new StringBuilder(mark).append(' ').append(title).toString();
    }

    /**
     * Provides the complete name of case.
     * 
     * @param caseRef Reference to case.
     * @param emptyValueText When case cannot be found, this empty value will be returned instead.
     * @return Case name or the empty value text.
     */
    public static String getCaseName(NodeRef caseRef, String emptyValueText) {
        NodeService nodeService = BeanHelper.getNodeService();
        if (caseRef == null || !nodeService.exists(caseRef)) {
            return emptyValueText;
        }

        return (String) BeanHelper.getNodeService().getProperty(caseRef, CaseModel.Props.TITLE);
    }

    /**
     * Composes a message about a property change.
     * 
     * @param propDefs Property definitions.
     * @param msgCode The base message ID for localizing the property change message.
     * @param prop The property that was changed.
     * @param oldValue The old property value as pre-formatted string.
     * @param newValue The new property value as pre-formatted string.
     * @param otherParams Optional other parameters to the message.
     * @return Composed message.
     */
    public static String msg(Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, String msgCode, QName prop, String oldValue, String newValue, String... otherParams) {
        Object[] args = ArrayUtils.addAll(new Object[] { propDefs.get(prop.getLocalName()).getSecond().getName(), oldValue, newValue }, otherParams);
        return MessageUtil.getMessage(msgCode, args);
    }

    /**
     * Provides document property and field mapping of the document type that the provided document uses.
     * 
     * @param docRef Reference to a document.
     * @return Property and corresponding document type field mapping.
     */
    public static Map<QName, Field> getDocumentTypeProps(NodeRef docRef) {
        if (docRef == null || !BeanHelper.getNodeService().exists(docRef) || !BeanHelper.getNodeService().getType(docRef).equals(DocumentCommonModel.Types.DOCUMENT)) {
            return Collections.emptyMap();
        }
        Map<QName, Serializable> props = BeanHelper.getNodeService().getProperties(docRef);
        Pair<String, Integer> typeAndVersion = DocAdminUtil.getDocTypeIdAndVersionNr(props);
        Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs = BeanHelper.getDocumentConfigService().getPropertyDefinitions(typeAndVersion);
        Map<QName, Field> docTypeProps = new LinkedHashMap<QName, Field>(propDefs.size(), 1);
        for (Pair<DynamicPropertyDefinition, Field> propDefAndField : propDefs.values()) {
            Field field = propDefAndField.getSecond();
            if (field != null) {
                docTypeProps.put(field.getQName(), field);
            }
        }
        return docTypeProps;
    }

    /**
     * Formats the values that were changed. When a value is empty, the string <code>emptyValue</code> will be used instead.
     * 
     * @param field Document type field of the property that was changed (required). Used for determining the type of the changed value.
     * @param propChange A property change information (required).
     * @param emptyValue The string to use when a value is empty.
     * @return A string array with length of 2. The first string is formatted old value, the second is formatted new value.
     */
    public static String[] format(Field field, PropertyChange propChange, String emptyValue) {
        String[] result = new String[2];
        FieldType fieldType = field.getFieldTypeEnum();

        result[0] = TextUtil.formatDocumentPropertyValue(propChange.getOldValue(), fieldType, emptyValue);
        result[1] = TextUtil.formatDocumentPropertyValue(propChange.getNewValue(), fieldType, emptyValue);

        return result;
    }

    /**
     * Attempts to find the property value of the changed property value (a Node). First, the old value is checked to contain a {@link Node}, if not, the new value will be checked.
     * Next, when the node value is found, its property value will be returned. When the property value is <code>null</code>, <code>emptyValue</code> will be returned.
     * 
     * @param propChange The property value change information.
     * @param prop The property for which value will be returned from the {@link Node}.
     * @param emptyValue Default value to return when the node was not found in property change information or when the property of the node has <code>null</code> value.
     * @return The resolved property value, or, when the latter is not found or <code>null</code>, the value of <code>emptyValue</code> parameter.
     */
    public static Object getNodeValueProp(PropertyChange propChange, QName prop, String emptyValue) {
        NodeRef node = propChange.getOldValue() instanceof NodeRef ? (NodeRef) propChange.getOldValue() : null;
        if (node == null && propChange.getNewValue() instanceof NodeRef) {
            node = (NodeRef) propChange.getNewValue();
        }

        Object value = null;
        if (node != null && prop != null) {
            value = BeanHelper.getNodeService().getProperty(node, prop);
        }

        return value == null ? emptyValue : value;
    }

}
