package ee.webmedia.alfresco.workflow.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.collections4.map.MultiValueMap;
import org.springframework.util.Assert;

public class TaskGroupHolder implements Serializable {
    /**
     * Maps workflow index to grouped tasks.
     */
    private final List<WorkflowTaskGroupList> workflowTaskGroupLists = new ArrayList<>();
    /**
     * Maps references to same TaskGroup object. Is used for JSF value binding.
     */
    private final Map<String, TaskGroup> byGroupId = new HashMap<>();

    public TaskGroup addNewTaskGroup(int workflowIndex, int taskIndex, String groupName, boolean isResponsible, boolean isFullAccess) {
        WorkflowTaskGroupList workflowTaskGroupLists1 = getWorkflowTaskGroupLists(workflowIndex);
        TaskGroup taskGroup = new TaskGroup(groupName, taskIndex, isResponsible, isFullAccess);
        workflowTaskGroupLists1.addGroup(taskGroup);
        byGroupId.put(taskGroup.getGroupId(), taskGroup);
        return taskGroup;
    }

    public void removeGroup(int wfIndex, TaskGroup group) {
        getWorkflowTaskGroupLists(wfIndex).removeGroup(group);
        removeWorkflowTaskGroupList(wfIndex);
        byGroupId.remove(group.getGroupId());
    }

    public void removeGroup(int wfIndex, String groupId) {
        TaskGroup group = byGroupId.get(groupId);
        if (group == null) {
            return;
        }
        removeGroup(wfIndex, group);
    }

    public boolean hasTaskGroups(int workflowIndex) {
        return !getWorkflowTaskGroupLists(workflowIndex).isEmpty();
    }

    private WorkflowTaskGroupList getWorkflowTaskGroupLists(int workflowIndex) {
        while (workflowIndex >= workflowTaskGroupLists.size()) {
            workflowTaskGroupLists.add(workflowTaskGroupLists.size(), new WorkflowTaskGroupList());
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

    public void removeTaskFromGroup(Integer wfIndex, Integer taskIndex, boolean removeGroup) {
        WorkflowTaskGroupList groupLists = getWorkflowTaskGroupLists(wfIndex);
        if (groupLists.isEmpty()) {
            return;
        }

        for (TaskGroup group : groupLists.getAllTaskGroups()) {
            if (group.hasTask(taskIndex)) {
                group.removeTask(taskIndex);
                if (removeGroup && group.isEmpty()) {
                    removeGroup(wfIndex, group);
                }
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
        private final MultiValueMap<String, TaskGroup> taskGroupsByGroupName = new MultiValueMap<>();

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
