package ee.webmedia.alfresco.workflow.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alfresco.util.GUID;

/**
 * Model object for task groups
 */
public class TaskGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    final private String groupId;
    private String groupName;
    private Date dueDate;
    private List<Integer> taskIds;
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

    public List<Integer> getTaskIds() {
        if (taskIds == null) {
            taskIds = new ArrayList<Integer>();
        }
        return taskIds;
    }

    public void setTaskIds(List<Integer> taskIds) {
        this.taskIds = taskIds;
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
