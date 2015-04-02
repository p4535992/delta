package ee.webmedia.mobile.alfresco.workflow.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.MapUtils;

public class TaskDelegationForm implements Serializable {

    private static final long serialVersionUID = 1L;
    private LinkedHashMap<String, String> choices;

    private String choice;
    private Date dueDate;
    private String taskDueDate;
    private NodeRef compoundWorkflowRef;
    private String taskType;

    private final Map<String, String> translations = new HashMap<>();

    private Map<String, List<TaskElement>> taskElementMap = new HashMap<>();

    public TaskDelegationForm() {
        taskElementMap = MapUtils.lazyMap(new HashMap<String, List<TaskElement>>(), new Factory<List<TaskElement>>() {
            @Override
            public List<TaskElement> create() {
                return new ArrayList<TaskDelegationForm.TaskElement>();
            }
        });
    }

    public TaskDelegationForm(LinkedHashMap<String, String> choices) {
        this.choices = choices;
        if (choices == null) {
            return;
        }
        init();
    }

    private void init() {
        for (String s : choices.keySet()) {
            taskElementMap.put(s, new ArrayList<TaskDelegationForm.TaskElement>());
        }
    }

    public Map<String, String> getChoices() {
        return choices;
    }

    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Map<String, List<TaskElement>> getTaskElementMap() {
        return taskElementMap;
    }

    public void setTaskElementMap(Map<String, List<TaskElement>> taskElementMap) {
        this.taskElementMap = taskElementMap;
    }

    public String getTaskDueDate() {
        return taskDueDate;
    }

    public void setTaskDueDate(String taskDueDate) {
        this.taskDueDate = taskDueDate;
    }

    public NodeRef getCompoundWorkflowRef() {
        return compoundWorkflowRef;
    }

    public void setCompoundWorkflowRef(NodeRef compoundWorkflowRef) {
        this.compoundWorkflowRef = compoundWorkflowRef;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public static class TaskElement {

        private String ownerId;
        private String resolution;
        private Date dueDate;
        private String groupMembers; // comma separated list of group members

        public String getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(String ownerId) {
            this.ownerId = ownerId;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public Date getDueDate() {
            return dueDate;
        }

        public void setDueDate(Date dueDate) {
            this.dueDate = dueDate;
        }

        public String getGroupMembers() {
            return groupMembers;
        }

        public void setGroupMembers(String groupMembers) {
            this.groupMembers = groupMembers;
        }

    }

}
