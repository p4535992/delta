package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * @author Alar Kvell
 */
public abstract class BaseWorkflowObject {

    private WmNode node;
    private Map<QName, Serializable> originalProperties;

    protected BaseWorkflowObject(WmNode node) {
        Assert.notNull(node);
        this.node = node;
        if (node.getNodeRef() == null) {
            originalProperties = new HashMap<QName, Serializable>();
        } else {
            originalProperties = getProperties(true);
        }
    }

    protected <T extends BaseWorkflowObject> T copyImpl(T copy) {
        copy.originalProperties = RepoUtil.copyProperties(originalProperties);
        return copy;
    }

    public WmNode getNode() {
        return node;
    }

    // wfc:common aspect

    public String getStatus() {
        return getProp(WorkflowCommonModel.Props.STATUS);
    }

    protected void setStatus(String status) {
        setProp(WorkflowCommonModel.Props.STATUS, status);
    }

    public String getCreatorName() {
        return getProp(WorkflowCommonModel.Props.CREATOR_NAME);
    }

    protected void setCreatorName(String creatorName) {
        setProp(WorkflowCommonModel.Props.CREATOR_NAME, creatorName);
    }

    public Date getStartedDateTime() {
        return getProp(WorkflowCommonModel.Props.STARTED_DATE_TIME);
    }

    protected void setStartedDateTime(Date startedDateTime) {
        setProp(WorkflowCommonModel.Props.STARTED_DATE_TIME, startedDateTime);
    }

    public Date getStoppedDateTime() {
        return getProp(WorkflowCommonModel.Props.STOPPED_DATE_TIME);
    }

    protected void setStoppedDateTime(Date stoppedDateTime) {
        setProp(WorkflowCommonModel.Props.STOPPED_DATE_TIME, stoppedDateTime);
    }

    // -----------------

    private Map<QName, Serializable> getProperties(boolean copy) {
        return RepoUtil.toQNameProperties(getNode().getProperties(), copy);
    }

    protected Map<QName, Serializable> getNewProperties() {
        return getProperties(false);
    }

    protected Map<QName, Serializable> getChangedProperties() {
        Map<QName, Serializable> newProperties = getNewProperties();
        Map<QName, Serializable> changedProperties = new HashMap<QName, Serializable>(newProperties.size());
        for (Entry<QName, Serializable> entry : newProperties.entrySet()) {
            QName key = entry.getKey();
            Serializable newValue = entry.getValue();
            if (!originalProperties.containsKey(key) || !EqualsHelper.nullSafeEquals(originalProperties.get(key), newValue)) {
                changedProperties.put(key, newValue);
            }
        }
        // Properties that were in original map, but do not exist in new map
        Set<QName> removedProperties = new HashSet<QName>(originalProperties.keySet());
        removedProperties.removeAll(newProperties.keySet());
        for (QName propName : removedProperties) {
            changedProperties.put(propName, null);
        }
        return changedProperties;
    }

    protected boolean isChangedProperty(QName propName) {
        Map<QName, Serializable> newProperties = getNewProperties();
        return originalProperties.containsKey(propName) != newProperties.containsKey(propName)
                || !EqualsHelper.nullSafeEquals(originalProperties.get(propName), newProperties.get(propName));
    }

    protected void setChangedProperties(Map<QName, Serializable> changedProperties) {
        originalProperties.putAll(changedProperties);
    }

    protected void clearOriginalProperties() {
        originalProperties.clear();
    }
    
    public boolean isStatus(Status... statuses){
        return WorkflowUtil.isStatus(this, statuses);
    }

    protected String additionalToString() {
        return "";
    }

    @Override
    public String toString() {
        return WmNode.toString(this) + "[\n  node=" + StringUtils.replace(getNode().toString(), "\n", "\n  ") + additionalToString() + "\n]";
    }

    public <T extends Serializable> T getProp(QName propName) {
        @SuppressWarnings("unchecked")
        T value = (T) getNode().getProperties().get(propName);
        return value;
    }

    public <T extends List<? extends Serializable>> T getPropList(QName propName) {
        @SuppressWarnings("unchecked")
        T value = (T) getNode().getProperties().get(propName);
        return value;
    }

    protected void setProp(QName propName, Serializable propValue) {
        getNode().getProperties().put(propName.toString(), propValue);
    }

    protected void setPropList(QName propName, List<? extends Serializable> propValue) {
        getNode().getProperties().put(propName.toString(), propValue);
    }

    protected void preSave() {
        // Subclasses can override
    }

}