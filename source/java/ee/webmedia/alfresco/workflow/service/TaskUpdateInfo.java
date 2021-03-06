package ee.webmedia.alfresco.workflow.service;

import static ee.webmedia.alfresco.common.search.DbSearchUtil.getQuestionMarks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.search.DbSearchUtil;
import ee.webmedia.alfresco.utils.TextUtil;

public class TaskUpdateInfo {
    private Task task;
    private final NodeRef taskNodeRef;
    private final List<String> fieldNames = new ArrayList<>();
    private final List<Object> arguments = new ArrayList<>();
    private Map<QName, Serializable> postSaveProperties;

    public TaskUpdateInfo(Task task) {
        this.task = task;
        taskNodeRef = task.getNodeRef();
    }

    public TaskUpdateInfo(NodeRef taskNodeRef) {
        this.taskNodeRef = taskNodeRef;
    }

    public void add(String fieldName, Object value) {
        Assert.isTrue(StringUtils.isNotBlank(fieldName));

        fieldNames.add(fieldName);
        arguments.add(value);
    }

    public void remove(String fieldName) {
        Assert.isTrue(StringUtils.isNotBlank(fieldName));

        int fieldIndex = fieldNames.indexOf(fieldName);
        if (fieldIndex > -1) {
            fieldNames.remove(fieldIndex);
            arguments.remove(fieldIndex);
        }
    }

    public String getParameterizedUpdateString() {
        return DbSearchUtil.createCommaSeparatedUpdateString(fieldNames);
    }

    public String getArgumentQuestionMarks() {
        return getQuestionMarks(fieldNames.size());
    }

    public String getFieldNameListing() {
        return TextUtil.joinNonBlankStringsWithComma(fieldNames);
    }

    public void applyPostSaveProperties() {
        task.setChangedProperties(postSaveProperties);
    }

    /**
     * @return Arguments as object array where task id is the last element.
     */
    public Object[] getArgumentArrayWithTaskId() {
        Object[] objects = arguments.toArray(new Object[arguments.size() + 1]);
        objects[objects.length - 1] = taskNodeRef.getId();
        return objects;
    }

    public List<String> getUnmodifiableFieldNames() {
        return Collections.unmodifiableList(fieldNames);
    }

    public boolean isUpdateNeeded() {
        return !fieldNames.isEmpty();
    }

    public Object[] getArgumentArray() {
        return arguments.toArray();
    }

    public Task getTask() {
        return task;
    }

    public NodeRef getTaskNodeRef() {
        return taskNodeRef;
    }

    public void setPostSaveProperties(Map<QName, Serializable> postSaveProperties) {
        this.postSaveProperties = postSaveProperties;
    }

    public String getTaskId() {
        return taskNodeRef.getId();
    }

    public Object getFieldValue(String fieldName, Map<String, QName> usedFieldNameMappings) {
        Assert.isTrue(StringUtils.isNotBlank(fieldName));
        Object value;
        int index = fieldNames.indexOf(fieldName);
        if (index >= 0) {
            value = arguments.get(index);
        } else {
            QName propName = usedFieldNameMappings.get(fieldName);
            value = (propName == null) ? null : task.getProp(propName);
        }

        return value;
    }
}
