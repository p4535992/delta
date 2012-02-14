package ee.webmedia.alfresco.document.log.service;

import static ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator.ACCESS_RESTRICTION_PROPS;
import static ee.webmedia.alfresco.document.log.service.DocumentLogHelper.format;
import static ee.webmedia.alfresco.document.log.service.DocumentLogHelper.getCaseName;
import static ee.webmedia.alfresco.document.log.service.DocumentLogHelper.getDocumentTypeProps;
import static ee.webmedia.alfresco.document.log.service.DocumentLogHelper.getFunctionName;
import static ee.webmedia.alfresco.document.log.service.DocumentLogHelper.getSeriesName;
import static ee.webmedia.alfresco.document.log.service.DocumentLogHelper.getVolumeName;
import static ee.webmedia.alfresco.document.log.service.DocumentLogHelper.msg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Holds document node property changes, and generates log messages.
 * 
 * @author Vladimir Drozdik
 * @author Martti Tamm
 */
public class DocumentPropertiesChangeHolder {

    public static final String MSG_DOC_PROP_CHANGED = "document_log_status_changed";
    public static final String MSG_DOC_LOC_CHANGED = "document_log_location_changed";
    public static final String MSG_DOC_ACCESS_RESTRICTION_CHANGED = "document_log_status_accessRestrictionChanged";

    private final Map<NodeRef, List<PropertyChange>> nodeChangeMapsMap = new LinkedHashMap<NodeRef, List<PropertyChange>>();

    /**
     * Includes a document property value change in this changes holder.
     * 
     * @param docNodeRef Reference to document where a property value change was detected.
     * @param itemQName The property where value change was detected.
     * @param oldValue The previous value of the property.
     * @param newValue The new value of the property.
     */
    public void addChange(NodeRef docNodeRef, QName itemQName, Serializable oldValue, Serializable newValue) {
        if (!nodeChangeMapsMap.containsKey(docNodeRef)) {
            nodeChangeMapsMap.put(docNodeRef, new ArrayList<PropertyChange>());
        }
        nodeChangeMapsMap.get(docNodeRef).add(new PropertyChange(itemQName, oldValue, newValue));
    }

    /**
     * Includes changes from another document property value change holder. No attempt to detect duplicate changes will be done.
     * 
     * @param holder Reference to document property value changes holder to merged with this holder.
     */
    public void addChanges(DocumentPropertiesChangeHolder holder) {
        for (Entry<NodeRef, List<PropertyChange>> entry : holder.nodeChangeMapsMap.entrySet()) {
            NodeRef docNodeRef = entry.getKey();

            if (nodeChangeMapsMap.keySet().contains(docNodeRef)) {
                nodeChangeMapsMap.get(docNodeRef).addAll(entry.getValue());
            } else {
                nodeChangeMapsMap.put(docNodeRef, entry.getValue());
            }
        }
    }

    /**
     * Reports whether any property value change is stored by this holder.
     * 
     * @return A Boolean that is <code>true</code> when no property change is stored in this holder.
     */
    public boolean isEmpty() {
        return nodeChangeMapsMap.isEmpty();
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
        Set<QName> keys = new HashSet<QName>(nodeChangeMapsMap.size() * 10);
        for (List<PropertyChange> changes : nodeChangeMapsMap.values()) {
            for (PropertyChange change : changes) {
                keys.add(change.getProperty());
            }
        }
        return keys;
    }

    public List<Serializable> generateLogMessages(Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, NodeRef docRef) {

        String emptyValue = MessageUtil.getMessage("document_log_status_empty");

        ArrayList<Serializable> messages = new ArrayList<Serializable>();
        messages.addAll(generateLocationMessages(propDefs, docRef, emptyValue));
        messages.addAll(generateAccessRestrictionMessages(propDefs, docRef, emptyValue));

        for (Field field : getDocumentTypeProps(docRef).values()) {
            for (List<PropertyChange> list : nodeChangeMapsMap.values()) {
                for (PropertyChange propertyChange : list) {
                    if (propertyChange.getProperty().equals(field.getQName())) {
                        String[] valuePair = format(field, propertyChange, emptyValue);
                        messages.add(MessageUtil.getMessage(MSG_DOC_PROP_CHANGED, field.getName(), valuePair[0], valuePair[1]));
                        continue;
                    }
                }
            }
        }

        return messages;
    }

    private List<String> generateLocationMessages(Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, NodeRef docRef, String emptyValue) {
        List<PropertyChange> list = nodeChangeMapsMap.get(docRef);
        if (list == null) {
            return Collections.emptyList();
        }

        String[] msgs = new String[4];
        for (Iterator<PropertyChange> i = list.iterator(); i.hasNext();) {
            PropertyChange propChange = i.next();

            String oldValue = null;
            String newValue = null;
            int pos = -1;

            if (DocumentCommonModel.Props.FUNCTION.equals(propChange.getProperty())) {
                oldValue = getFunctionName((NodeRef) propChange.getOldValue(), emptyValue);
                newValue = getFunctionName((NodeRef) propChange.getNewValue(), emptyValue);
                pos = 0;

            } else if (DocumentCommonModel.Props.SERIES.equals(propChange.getProperty())) {
                oldValue = getSeriesName((NodeRef) propChange.getOldValue(), emptyValue);
                newValue = getSeriesName((NodeRef) propChange.getNewValue(), emptyValue);
                pos = 1;

            } else if (DocumentCommonModel.Props.VOLUME.equals(propChange.getProperty())) {
                oldValue = getVolumeName((NodeRef) propChange.getOldValue(), emptyValue);
                newValue = getVolumeName((NodeRef) propChange.getNewValue(), emptyValue);
                pos = 2;

            } else if (DocumentCommonModel.Props.CASE.equals(propChange.getProperty())) {
                oldValue = getCaseName((NodeRef) propChange.getOldValue(), emptyValue);
                newValue = getCaseName((NodeRef) propChange.getNewValue(), emptyValue);
                pos = 3;
            }

            if (oldValue != null && newValue != null) {
                msgs[pos] = msg(propDefs, MSG_DOC_LOC_CHANGED, propChange.getProperty(), oldValue, newValue);
                i.remove();
            }
        }

        List<String> result = new ArrayList<String>();
        for (String msg : msgs) {
            if (msg != null) {
                result.add(msg);
            }
        }
        return result;
    }

    private List<String> generateAccessRestrictionMessages(Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, NodeRef docRef, String emptyValue) {
        List<PropertyChange> list = nodeChangeMapsMap.get(docRef);
        if (list == null) {
            return Collections.emptyList();
        }

        String[] msgs = new String[ACCESS_RESTRICTION_PROPS.length];
        String changeReason = null;
        for (PropertyChange propertyChange : list) {
            if (propertyChange.getProperty().equals(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON)) {
                changeReason = (String) propertyChange.getNewValue();
            }
        }
        changeReason = StringUtils.defaultString(changeReason, emptyValue);

        for (Iterator<PropertyChange> i = list.iterator(); i.hasNext();) {
            PropertyChange propChange = i.next();

            for (int j = 0; j < ACCESS_RESTRICTION_PROPS.length; j++) {
                if (propChange.getProperty().equals(ACCESS_RESTRICTION_PROPS[j])) {
                    String[] valuePair = format(propDefs.get(propChange.getProperty().getLocalName()).getSecond(), propChange, emptyValue);
                    msgs[j] = msg(propDefs, MSG_DOC_ACCESS_RESTRICTION_CHANGED, propChange.getProperty(), valuePair[0], valuePair[1], changeReason);
                    i.remove();
                }
            }
        }

        List<String> result = new ArrayList<String>();
        for (String msg : msgs) {
            if (msg != null) {
                result.add(msg);
            }
        }
        return result;
    }
}
