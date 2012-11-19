package ee.webmedia.alfresco.document.log.service;

import static ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator.ACCESS_RESTRICTION_PROPS;
import static ee.webmedia.alfresco.document.log.service.DocumentLogHelper.format;
import static ee.webmedia.alfresco.document.log.service.DocumentLogHelper.getCaseName;
import static ee.webmedia.alfresco.document.log.service.DocumentLogHelper.getFunctionName;
import static ee.webmedia.alfresco.document.log.service.DocumentLogHelper.getObjectTypeProps;
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

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentChildModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Holds document node property changes, and generates log messages.
 * 
 * @author Vladimir Drozdik
 * @author Martti Tamm
 */
public class DocumentPropertiesChangeHolder {

    public static final String MSG_DOC_PROP_CHANGED_SUFIX = "_log_status_changed";
    public static final String MSG_DOC_LOC_CHANGED_SUFIX = "_log_location_changed";
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
     * Includes a document child node add/remove change in this changes holder.
     * 
     * @param docNodeRef Reference to document where a property value change was detected.
     * @param itemQName The type of child node that was added or removed.
     * @param oldChild References the node that was removed. May be <code>null</code>.
     * @param newChild References the node that was added. May be <code>null</code>.
     */
    public void addChange(NodeRef docNodeRef, QName itemQName, Node oldChild, Node newChild) {
        Serializable oldValue = oldChild;
        Serializable newValue = newChild;

        Node node = oldChild != null ? oldChild : newChild;
        QName type = node != null ? node.getType() : null;
        if (node == null || type == null) {
            return;
        }

        ChildNodeChangeInfo info = ChildNodeChangeInfo.getInstance(type);
        if (info != null && info.getMsgParamProp() != null) {
            String msgParam = (String) node.getProperties().get(info.getMsgParamProp());
            oldValue = oldValue != null && msgParam != null ? msgParam : oldValue;
            newValue = newValue != null && msgParam != null ? msgParam : newValue;
        }

        addChange(docNodeRef, itemQName, oldValue, newValue);
    }

    /**
     * Includes a document child node add/remove change in this changes holder.
     * 
     * @param docNodeRef Reference to document where a property value change was detected.
     * @param itemQName The type of child node that was added or removed.
     * @param oldChild References the node that was removed. May be <code>null</code>.
     * @param newChild References the node that was added. May be <code>null</code>.
     */
    public void addChange(NodeRef docNodeRef, QName itemQName, NodeRef oldChild, NodeRef newChild) {
        NodeService nodeService = BeanHelper.getNodeService();

        Serializable oldValue = oldChild;
        Serializable newValue = newChild;

        NodeRef node = oldChild != null ? oldChild : newChild;
        QName type = null;

        if (node != null && nodeService.exists(node)) {
            type = nodeService.getType(node);
        }
        if (node == null || type == null) {
            return;
        }

        ChildNodeChangeInfo info = ChildNodeChangeInfo.getInstance(type);
        if (info != null && info.getMsgParamProp() != null) {
            String msgParam = (String) nodeService.getProperty(node, info.getMsgParamProp());
            oldValue = oldValue != null && msgParam != null ? msgParam : oldValue;
            newValue = newValue != null && msgParam != null ? msgParam : newValue;
        }

        if (type.equals(DocumentChildModel.Assocs.APPLICANT_ABROAD) || type.equals(DocumentChildModel.Assocs.APPLICANT_DOMESTIC)
                || type.equals(DocumentChildModel.Assocs.APPLICANT_TRAINING)) {
            String applicant = (String) nodeService.getProperty(node, DocumentSpecificModel.Props.APPLICANT_NAME);

            if (applicant != null && oldChild != null) {
                oldValue = applicant;
            } else if (applicant != null && newChild != null) {
                newValue = applicant;
            }

        } else if (type.equals(DocumentChildModel.Assocs.CONTRACT_PARTY)) {
            String party = (String) nodeService.getProperty(node, DocumentSpecificModel.Props.PARTY_NAME);

            if (party != null && oldChild != null) {
                oldValue = party;
            } else if (party != null && newChild != null) {
                newValue = party;
            }
        }

        addChange(docNodeRef, itemQName, oldValue, newValue);
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
     * Return all recorded changes for given NodeRef. If not found or invalid NodeRef is supplied, an empty list is returned
     * 
     * @param nodeRef NodeRef of the modified object
     * @return unmodifiable list with PropertyChange object or empty list.
     */
    public List<PropertyChange> getChanges(NodeRef nodeRef) {
        if (nodeRef == null) {
            return Collections.emptyList();
        }

        final List<PropertyChange> list = nodeChangeMapsMap.get(nodeRef);
        return list == null ? Collections.<PropertyChange> emptyList() : Collections.unmodifiableList(list);
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

    public PropertyChange getPropertyChange(NodeRef docRef, QName property) {
        List<PropertyChange> list = nodeChangeMapsMap.get(docRef);
        if (list == null) {
            return null;
        }
        for (PropertyChange propertyChange : list) {
            if (property.equals(propertyChange.getProperty())) {
                return propertyChange;
            }
        }
        return null;
    }

    public List<String> generateLogMessages(Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, NodeRef docRef) {
        String emptyValue = MessageUtil.getMessage("document_log_status_empty");
        ArrayList<String> messages = new ArrayList<String>();

        messages.addAll(generate(propDefs, docRef, emptyValue));
        nodeChangeMapsMap.remove(docRef);

        for (Iterator<NodeRef> i = nodeChangeMapsMap.keySet().iterator(); i.hasNext();) {
            NodeRef nodeRef = i.next();
            if (BeanHelper.getNodeService().exists(nodeRef)) {
                messages.addAll(generate(propDefs, nodeRef, emptyValue));
                i.remove();
            }
        }

        return messages;
    }

    private List<String> generate(Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, NodeRef docRef, String emptyValue) {
        ArrayList<String> messages = new ArrayList<String>();
        messages.addAll(generateLocationMessages(propDefs, docRef, emptyValue));
        messages.addAll(generateAccessRestrictionMessages(propDefs, docRef, emptyValue));
        messages.addAll(generateChildNodeMessages(docRef, emptyValue));
        String message = getMessage(MSG_DOC_PROP_CHANGED_SUFIX, docRef);
        for (Field field : getObjectTypeProps(docRef).values()) {
            if (!nodeChangeMapsMap.containsKey(docRef)) {
                continue;
            }
            for (PropertyChange propertyChange : nodeChangeMapsMap.get(docRef)) {
                if (propertyChange.getProperty().equals(field.getQName())) {
                    String[] valuePair = format(field, propertyChange, emptyValue);
                    messages.add(MessageUtil.getMessage(message, field.getName(), valuePair[0], valuePair[1]));
                    continue;
                }
            }
        }

        return messages;
    }

    private String getMessage(String sufix, NodeRef nodeRef) {
        return BeanHelper.getNodeService().getType(nodeRef).getLocalName().toLowerCase() + sufix;
    }

    private List<String> generateLocationMessages(Map<String, Pair<DynamicPropertyDefinition, Field>> propDefs, NodeRef docRef, String emptyValue) {
        List<PropertyChange> list = nodeChangeMapsMap.get(docRef);
        if (list == null) {
            return Collections.emptyList();
        }

        String[] msgs = new String[4];
        String message = null;
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
                if (message == null) {
                    message = getMessage(MSG_DOC_LOC_CHANGED_SUFIX, docRef);
                }
                msgs[pos] = msg(propDefs, message, propChange.getProperty(), oldValue, newValue);
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

    private List<String> generateChildNodeMessages(NodeRef docRef, String emptyValue) {
        List<PropertyChange> list = nodeChangeMapsMap.get(docRef);
        if (list == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<String>();
        for (PropertyChange propChange : list) {
            ChildNodeChangeInfo info = ChildNodeChangeInfo.getInstance(propChange.getProperty());
            if (info == null) {
                continue;
            }

            if (propChange.getOldValue() == null && propChange.getNewValue() != null) {
                result.add(I18NUtil.getMessage(info.getAddMessageKey(), requireString(propChange.getNewValue(), emptyValue)));
            } else if (propChange.getOldValue() != null && propChange.getNewValue() == null) {
                result.add(I18NUtil.getMessage(info.getRemoveMessageKey(), requireString(propChange.getOldValue(), emptyValue)));
            }
        }

        return result;
    }

    private static String requireString(Serializable value, String emptyValue) {
        return value instanceof String && StringUtils.isNotBlank((String) value) ? (String) value : emptyValue;
    }

}
