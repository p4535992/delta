package ee.webmedia.alfresco.document.log.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.constant.FieldType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docadmin.web.DocAdminUtil;
import ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * Holds document nodes changes, and generates log messages.
 * 
 * @author Vladimir Drozdik
 */
public class DocumentPropertiesChangeHolder {

    private final Map<NodeRef, Map<QName, Pair<Serializable, Serializable>>> nodeChangeMapsMap;
    private static FastDateFormat dateFormat;

    public DocumentPropertiesChangeHolder() {
        nodeChangeMapsMap = new HashMap<NodeRef, Map<QName, Pair<Serializable, Serializable>>>();
    }

    public void addLog(NodeRef docNodeRef, QName itemQName, Serializable oldValue, Serializable newValue) {
        if (!nodeChangeMapsMap.keySet().contains(docNodeRef)) {
            nodeChangeMapsMap.put(docNodeRef, new HashMap<QName, Pair<Serializable, Serializable>>());
        }
        nodeChangeMapsMap.get(docNodeRef).put(itemQName, new Pair<Serializable, Serializable>(oldValue, newValue));
    }

    public void joinAnotherHolder(DocumentPropertiesChangeHolder holder) {
        for (Entry<NodeRef, Map<QName, Pair<Serializable, Serializable>>> entry : holder.getNodeChangeMapsMap().entrySet()) {
            NodeRef docNodeRef = entry.getKey();
            Map<QName, Pair<Serializable, Serializable>> oneNodeChangeMap = entry.getValue();
            if (nodeChangeMapsMap.keySet().contains(docNodeRef)) {
                nodeChangeMapsMap.get(docNodeRef).putAll(oneNodeChangeMap);
            } else {
                nodeChangeMapsMap.put(docNodeRef, oneNodeChangeMap);
            }
        }
    }

    public boolean isEmpty() {
        return getNodeChangeMapsMap().isEmpty();
    }

    public boolean isOnlyAccessRestrictionPropsChanged() {
        Set<QName> keys = getAllKeys();
        List<QName> accRestProp = new ArrayList<QName>(Arrays.asList(AccessRestrictionGenerator.ACCESS_RESTRICTION_PROPS));
        accRestProp.add(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON);
        keys.removeAll(accRestProp);
        return keys.isEmpty();
    }

    public boolean isOnlyLocationPropsChanged() {
        Set<QName> keys = getAllKeys();
        keys.removeAll(Arrays.asList(DocumentCommonModel.Props.SERIES, DocumentCommonModel.Props.FUNCTION, DocumentCommonModel.Props.VOLUME,
                 DocumentCommonModel.Props.CASE));
        return keys.isEmpty();

    }

    public boolean isStructureChanged() {
        Set<QName> keys = getAllKeys();
        for (QName qName : DocumentChildModel.Assocs.ALL_ASSOCS) {
            if (keys.contains(qName)) {
                return true;
            }
        }
        return false;
    }

    private Set<QName> getAllKeys() {
        Set<QName> keys = new HashSet<QName>();
        for (Map<QName, Pair<Serializable, Serializable>> oneNodeChange : nodeChangeMapsMap.values()) {
            keys.addAll(oneNodeChange.keySet());
        }
        return keys;
    }

    public List<Serializable> generateLogMessages(Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, NodeRef docRef) {
        ArrayList<Serializable> messages = new ArrayList<Serializable>();
        for (Map<QName, Pair<Serializable, Serializable>> oneNodeChangeMap : nodeChangeMapsMap.values()) {
            // LOCATION
            String logMessageKey = "document_log_status_changed";
            Pair<Serializable, Serializable> values = oneNodeChangeMap.remove(DocumentCommonModel.Props.FUNCTION);
            if (values != null) {
                Serializable oldValue = values.getFirst();
                NodeRef functionRef = (NodeRef) oldValue;
                if (functionRef != null) {
                    oldValue = BeanHelper.getNodeService().getProperty(functionRef, FunctionsModel.Props.MARK) + " "
                            + BeanHelper.getNodeService().getProperty(functionRef, FunctionsModel.Props.TITLE);
                }
                Serializable newValue = values.getSecond();
                newValue = BeanHelper.getNodeService().getProperty((NodeRef) newValue, FunctionsModel.Props.MARK) + " "
                        + BeanHelper.getNodeService().getProperty((NodeRef) newValue, FunctionsModel.Props.TITLE);
                messages.add(MessageUtil.getMessage(logMessageKey, propDefs.get(DocumentCommonModel.Props.FUNCTION.getLocalName()).getSecond().getName(), oldValue, newValue));
            }
            values = oneNodeChangeMap.remove(DocumentCommonModel.Props.SERIES);
            if (values != null) {
                Serializable oldValue = values.getFirst();
                NodeRef seriesRef = (NodeRef) oldValue;
                if (seriesRef != null) {
                    oldValue = BeanHelper.getNodeService().getProperty(seriesRef, SeriesModel.Props.SERIES_IDENTIFIER) + " "
                            + BeanHelper.getNodeService().getProperty(seriesRef, SeriesModel.Props.TITLE);
                }
                Serializable newValue = values.getSecond();
                newValue = BeanHelper.getNodeService().getProperty((NodeRef) newValue, SeriesModel.Props.SERIES_IDENTIFIER) + " "
                        + BeanHelper.getNodeService().getProperty((NodeRef) newValue, SeriesModel.Props.TITLE);
                messages.add(MessageUtil.getMessage(logMessageKey, propDefs.get(DocumentCommonModel.Props.SERIES.getLocalName()).getSecond().getName(), oldValue, newValue));
            }
            values = oneNodeChangeMap.remove(DocumentCommonModel.Props.VOLUME);
            if (values != null) {
                Serializable oldValue = values.getFirst();
                NodeRef volumeRef = (NodeRef) oldValue;
                if (volumeRef != null) {
                    oldValue = BeanHelper.getNodeService().getProperty(volumeRef, VolumeModel.Props.VOLUME_MARK) + " "
                            + BeanHelper.getNodeService().getProperty(volumeRef, VolumeModel.Props.TITLE);
                }
                Serializable newValue = values.getSecond();
                newValue = BeanHelper.getNodeService().getProperty((NodeRef) newValue, VolumeModel.Props.VOLUME_MARK) + " "
                        + BeanHelper.getNodeService().getProperty((NodeRef) newValue, VolumeModel.Props.TITLE);
                messages.add(MessageUtil.getMessage(logMessageKey, propDefs.get(DocumentCommonModel.Props.VOLUME.getLocalName()).getSecond().getName(), oldValue, newValue));
            }
            values = oneNodeChangeMap.remove(DocumentCommonModel.Props.CASE);
            if (values != null) {
                Serializable oldValue = values.getFirst();
                NodeRef caseRef = (NodeRef) oldValue;
                if (caseRef != null) {
                    oldValue = BeanHelper.getNodeService().getProperty(caseRef, CaseModel.Props.TITLE) + " "
                            + BeanHelper.getNodeService().getProperty(caseRef, CaseModel.Props.TITLE);
                }
                Serializable newValue = values.getSecond();
                newValue = BeanHelper.getNodeService().getProperty((NodeRef) newValue, CaseModel.Props.TITLE) + " "
                        + BeanHelper.getNodeService().getProperty((NodeRef) newValue, CaseModel.Props.TITLE);
                messages.add(MessageUtil.getMessage(logMessageKey, propDefs.get(DocumentCommonModel.Props.CASE.getLocalName()).getSecond().getName(), oldValue, newValue));
            }
            // ACCESS RESTRICTIONS
            values = oneNodeChangeMap.remove(DocumentCommonModel.Props.ACCESS_RESTRICTION);
            Serializable oldValue = null;
            Serializable newValue = null;
            if (values != null) {
                oldValue = values.getFirst();
                newValue = values.getSecond();
            }
            Pair<Serializable, Serializable> reasonValue = oneNodeChangeMap.remove(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON);
            if (reasonValue != null) {
                final String reason = (String) reasonValue.getSecond();
                Pair<DynamicPropertyDefinition, Field> pair = propDefs.get(DocumentCommonModel.Props.ACCESS_RESTRICTION.getLocalName());
                Field field = pair.getSecond();
                messages.add(MessageUtil.getMessage("document_log_status_accessRestrictionChanged"
                        , field.getName(), oldValue, newValue, reason));
            }
            // OTHER PROPS
            WmNode docNode = BeanHelper.getDocumentDynamicService().getDocument(docRef).getNode();
            Pair<String, Integer> typeAndVersion = DocAdminUtil.getDocTypeIdAndVersionNr(docNode);
            DocumentTypeVersion docTypeVersion = BeanHelper.getDocumentAdminService().getDocumentTypeAndVersion(typeAndVersion.getFirst(), typeAndVersion.getSecond()).getSecond();
            for (Field field : docTypeVersion.getFieldsDeeply()) {
                values = oneNodeChangeMap.remove(field.getQName());
                if (values != null) {
                    values = getConvertedValues(field, values);
                    String message = MessageUtil.getMessage(logMessageKey, field.getName(), values.getFirst(), values.getSecond());
                    messages.add(message);
                }
            }
            // HIDDEN PROPS WITHOUT FIELD
            for (Entry<QName, Pair<Serializable, Serializable>> entry : oneNodeChangeMap.entrySet()) {
                values = getConvertedValues(null, entry.getValue());
                String message = MessageUtil.getMessage(logMessageKey, entry.getKey().getLocalName(), values.getFirst(), values.getSecond());
                messages.add(message);
            }
        }
        return messages;
    }

    private Pair<Serializable, Serializable> getConvertedValues(Field field, Pair<Serializable, Serializable> values) {
        // Convert special cases
        List<Serializable> convertedValues = new ArrayList<Serializable>();
        if (field == null) {
            for (Serializable propValue : Arrays.asList(values.getFirst(), values.getSecond())) {
                if (propValue == null || propValue instanceof String && StringUtils.isBlank((String) propValue)) {
                    convertedValues.add(MessageUtil.getMessage("document_log_status_empty"));
                } else {
                    convertedValues.add(propValue);
                }
            }
        } else {
            dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");
            FieldType fieldType = field.getFieldTypeEnum();
            for (Serializable propValue : Arrays.asList(values.getFirst(), values.getSecond())) {
                if (propValue.equals(MessageUtil.getMessage("document_log_status_empty"))) {
                    convertedValues.add(propValue);
                } else if (FieldType.DATE == fieldType) {
                    if (propValue instanceof List && ((List<?>) propValue).size() == 1) {
                        propValue = (Serializable) ((List<?>) propValue).get(0);
                    }
                    if (propValue != null) {
                        convertedValues.add(dateFormat.format(propValue));
                    }
                    continue;
                } else if (FieldType.LISTBOX == fieldType && propValue instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Serializable> listPropValue = (List<Serializable>) propValue;
                    convertedValues.add(StringUtils.join(listPropValue, "; "));
                    continue;
                } else if (FieldType.CHECKBOX == fieldType) {
                    String msgKey = (Boolean) propValue ? "yes" : "no";
                    convertedValues.add(MessageUtil.getMessage(msgKey));
                    continue;
                } else if (FieldType.STRUCT_UNIT == fieldType) {
                    @SuppressWarnings("unchecked")
                    List<String> orgStruct = (List<String>) propValue;
                    convertedValues.add(UserUtil.getDisplayUnit(orgStruct));
                    continue;
                } else if (propValue instanceof String && StringUtils.isBlank((String) propValue)) {
                    convertedValues.add(MessageUtil.getMessage("document_log_status_empty"));
                    continue;
                }
                convertedValues.add(propValue);
            }
        }
        return new Pair<Serializable, Serializable>(convertedValues.get(0), convertedValues.get(1));
    }

    private Map<NodeRef, Map<QName, Pair<Serializable, Serializable>>> getNodeChangeMapsMap() {
        return nodeChangeMapsMap;
    }

}
