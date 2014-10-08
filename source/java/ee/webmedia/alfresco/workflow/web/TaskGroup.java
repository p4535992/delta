package ee.webmedia.alfresco.workflow.web;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.alfresco.util.GUID;

/**
 * Model object for task groups
 */
public class TaskGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    final private String groupId;
    private String groupName;
    private Date dueDate;
    private Integer minimumTaskIndex;
    private Set<Integer> taskIds;
    private boolean expanded;
    private boolean responsible;
    private boolean fullAccess;

    public TaskGroup() {
        groupId = GUID.generate();
    }

    public TaskGroup(String groupName, Integer taskId, boolean responsible, boolean fullAccess) {
        this();
        this.groupName = groupName;
        this.responsible = responsible;
        this.fullAccess = fullAccess;
        dueDate = null;
        getTaskIds().add(taskId);
    }

    public void addTask(Integer taskIndex) {
        getTaskIds().add(taskIndex);
        if (minimumTaskIndex > taskIndex) {
            minimumTaskIndex = taskIndex;
        }
    }

    public boolean hasTask(Integer taskIndex) {
        return getTaskIds().contains(taskIndex);
    }

    public void removeTask(Integer taskIndex) {
        getTaskIds().remove(taskIndex);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Set<Integer> getTaskIds() {
        if (taskIds == null) {
            taskIds = new LinkedHashSet<>();
        }
        return taskIds;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isResponsible() {
        return responsible;
    }

    public void setResponsible(boolean responsible) {
        this.responsible = responsible;
    }

    public boolean isFullAccess() {
        return fullAccess;
    }

    public void setFullAccess(boolean fullAccess) {
        this.fullAccess = fullAccess;
    }

    public Integer getMinimumTaskIndex() {
        if (minimumTaskIndex == null) {
            minimumTaskIndex = Collections.min(getTaskIds());
        }
        return minimumTaskIndex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TaskGroup other = (TaskGroup) obj;
        if (groupId == null) {
            if (other.groupId != null) {
                return false;
            }
        } else if (!groupId.equals(other.groupId)) {
            return false;
        }
        return true;
    }

}
