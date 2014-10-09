package ee.webmedia.alfresco.workflow.web;

import org.apache.commons.collections4.map.MultiValueMap;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class TaskGroupHolder implements Serializable {
    /**
     * Maps workflow index to grouped tasks.
     */
    private List<WorkflowTaskGroupList> workflowTaskGroupLists = new ArrayList<>();
    /**
     * Maps references to same TaskGroup object. Is used for JSF value binding.
     */
    private Map<String, TaskGroup> byGroupId = new HashMap<>();

    public TaskGroup addNewTaskGroup(int workflowIndex, int taskIndex, String groupName, boolean isResponsible, boolean isFullAccess) {
        WorkflowTaskGroupList workflowTaskGroupLists1 = getWorkflowTaskGroupLists(workflowIndex);
        TaskGroup taskGroup = new TaskGroup(groupName, taskIndex, isResponsible, isFullAccess);
        workflowTaskGroupLists1.addGroup(taskGroup);
        byGroupId.put(taskGroup.getGroupId(), taskGroup);
        return taskGroup;
    }

    public void removeGroup(int wfIndex, TaskGroup group) {
        getWorkflowTaskGroupLists(wfIndex).removeGroup(group);
        byGroupId.remove(group.getGroupId());
    }

    public boolean hasTaskGroups(int workflowIndex) {
        return !getWorkflowTaskGroupLists(workflowIndex).isEmpty();
    }

    private WorkflowTaskGroupList getWorkflowTaskGroupLists(int workflowIndex) {
        Assert.isTrue(workflowIndex >= 0 && workflowIndex <= workflowTaskGroupLists.size());
        if (workflowIndex >= workflowTaskGroupLists.size()) {
            workflowTaskGroupLists.add(workflowIndex, new WorkflowTaskGroupList());
        }

        return workflowTaskGroupLists.get(workflowIndex);
    }

    public void addNewWorkflowTaskGroupList(int workflowIndex) {
        workflowTaskGroupLists.add(workflowIndex, new WorkflowTaskGroupList());
    }

    public WorkflowTaskGroupList removeWorkflowTaskGroupList(int workflowIndex) {
        return workflowTaskGroupLists.remove(workflowIndex);
    }

    public TaskGroup getTaskGroup(Integer workflowIndex, String groupName, String groupId) {
        Assert.notNull(workflowIndex);
        Assert.notNull(groupName);
        Assert.notNull(groupId);

        TaskGroup group = null;
        WorkflowTaskGroupList groupList = getWorkflowTaskGroupLists(workflowIndex);
        if (groupList.isEmpty()) {
            return group;
        }

        return groupList.getTaskGroup(groupName, groupId);
    }

    /**
     * Used by JSP binding.
     * @return All task groups in this holder mapped to task group ID
     */
    public Map<String, TaskGroup> getByGroupId() {
        return Collections.unmodifiableMap(byGroupId);
    }

    public void removeTaskFromGroup(Integer wfIndex, Integer taskIndex) {
        WorkflowTaskGroupList groupLists = getWorkflowTaskGroupLists(wfIndex);
        if (groupLists.isEmpty()) {
            return;
        }

        for (TaskGroup group : groupLists.getAllTaskGroups()) {
            if (group.hasTask(taskIndex)) {
                group.removeTask(taskIndex);
                return;
            }
        }
    }

    public NavigableMap<Integer, TaskGroup> getWorkflowTaskGroupDueDates(int workflowIndex) {
        Collection<TaskGroup> allTaskGroups = getWorkflowTaskGroups(workflowIndex);
        NavigableMap<Integer, TaskGroup> result = new TreeMap<>();

        for (TaskGroup group : allTaskGroups) {
            result.put(group.getMinimumTaskIndex(), group);
        }

        return result;
    }

    public Collection<TaskGroup> getWorkflowTaskGroups(int workflowIndex) {
        WorkflowTaskGroupList taskGroupList = getWorkflowTaskGroupLists(workflowIndex);
        return Collections.unmodifiableCollection(taskGroupList.getAllTaskGroups());
    }

    public TaskGroup getAdjacentTaskGroup(int workflowIndex, String ownerGroup, Integer counter) {
        return getWorkflowTaskGroupLists(workflowIndex).getAdjacentTaskGroup(ownerGroup, counter);
    }

    private static class WorkflowTaskGroupList implements Serializable {
        private MultiValueMap<String, TaskGroup> taskGroupsByGroupName = new MultiValueMap<>();

        public boolean isEmpty() {
            return taskGroupsByGroupName.isEmpty();
        }

        public Collection<TaskGroup> getAllTaskGroups() {
            return (Collection) taskGroupsByGroupName.values();
        }

        public TaskGroup getAdjacentTaskGroup(String groupName, Integer counter) {
            if (isEmpty() || !taskGroupsByGroupName.containsKey(groupName)) {
                return null;
            }

            for (TaskGroup group : taskGroupsByGroupName.getCollection(groupName)) {
                for (Integer taskId : group.getTaskIds()) {
                    if (taskId.equals(counter) || Math.abs(counter - taskId) == 1) {
                        return group;
                    }
                }
            }
            return null;
        }

        private TaskGroup getTaskGroup(String groupName, String groupId) {
            List<TaskGroup> taskGroups = (List<TaskGroup>) taskGroupsByGroupName.get(groupName);
            for (TaskGroup group : taskGroups) {
                if (group.getGroupId().equals(groupId)) {
                    return group;
                }
            }

            return null;
        }

        public void addGroup(TaskGroup taskGroup) {
            taskGroupsByGroupName.put(taskGroup.getGroupName(), taskGroup);
        }

        public void removeGroup(TaskGroup group) {
            taskGroupsByGroupName.removeMapping(group.getGroupName(), group);
        }
    }

}
