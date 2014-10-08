package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.model.NodeBaseVO;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public abstract class BaseWorkflowObject extends NodeBaseVO {

    /**
     * denotes that BaseWorkflowObject (task or information/opinion workflow) temporarily having this property is not saved,
     * but generated for delegating original assignment task to other people
     */
    public static final QName TMP_ADDED_BY_DELEGATION = RepoUtil.createTransientProp("addedByDelegation");

    private Map<QName, Serializable> originalProperties;

    protected BaseWorkflowObject(WmNode node) {
        Assert.notNull(node);
        this.node = node;
        if (node.isUnsaved()) {
            setOriginalProperties(new HashMap<QName, Serializable>());
        } else {
            setOriginalProperties(getProperties(true));
        }
    }

    protected <T extends BaseWorkflowObject> T copyImpl(T copy) {
        copy.setOriginalProperties(RepoUtil.copyProperties(originalProperties));
        return copy;
    }

    // wfc:common aspect

    public String getStatus() {
        return getProp(WorkflowCommonModel.Props.STATUS);
    }

    protected void setStatus(String status) {
        setProp(WorkflowCommonModel.Props.STATUS, status);
    }

    public QName getType() {
        return getNode().getType();
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

    protected void setCreatedDateTime(Date createdDateTime) {
        setProp(WorkflowCommonModel.Props.CREATED_DATE_TIME, createdDateTime);
    }

    public Date getStoppedDateTime() {
        return getProp(WorkflowCommonModel.Props.STOPPED_DATE_TIME);
    }

    protected void setStoppedDateTime(Date stoppedDateTime) {
        setProp(WorkflowCommonModel.Props.STOPPED_DATE_TIME, stoppedDateTime);
    }

    protected void setFinishedDateTime(Date finishedDateTime) {
        setProp(WorkflowCommonModel.Props.FINISHED_DATE_TIME, finishedDateTime);
    }

    public String getOwnerId() {
        return getProp(WorkflowCommonModel.Props.OWNER_ID);
    }

    public void setOwnerId(String ownerId) {
        setProp(WorkflowCommonModel.Props.OWNER_ID, ownerId);
    }

    // -----------------

    private Map<QName, Serializable> getProperties(boolean copy) {
        return RepoUtil.toQNameProperties(getNode().getProperties(), copy);
    }

    private Map<QName, Serializable> getNewProperties() {
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

    protected Map<QName, Serializable> getOriginalProperties() {
        return Collections.unmodifiableMap(originalProperties);
    }

    public boolean isStatus(Status... statuses) {
        return WorkflowUtil.isStatus(this, statuses);
    }

    public boolean isType(QName... types) {
        for (QName type : types) {
            if (type.equals(getType())) {
                return true;
            }
        }
        return false;
    }

    protected String additionalToString() {
        return "";
    }

    @Override
    public String toString() {
        return WmNode.toString(this) + " status=" + getStatus() + " [\n  node=" + StringUtils.replace(getNode().toString(), "\n", "\n  ") + additionalToString() + "\n]";
    }

    protected void preSave() {
        // Subclasses can override
    }

    protected void setOriginalProperties(Map<QName, Serializable> originalProperties) {
        this.originalProperties = originalProperties;
    }

}
