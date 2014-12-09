package ee.webmedia.mobile.alfresco.workflow.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ee.webmedia.alfresco.workflow.web.DelegationBean;

public class TaskDelegationForm implements Serializable {

    private static final long serialVersionUID = 1L;
    private Map<Integer, String> choices;

    private String choice;
    private Date dueDate;

    private Map<String, List<TaskElement>> taskElementMap = new HashMap<>();

    public TaskDelegationForm() {
        for (int i = 0; i < DelegationBean.DELEGATION_TASK_CHOICE_COUNT; i++) {
            taskElementMap.put(String.valueOf(i), new ArrayList<TaskDelegationForm.TaskElement>());
        }
    }

    public TaskDelegationForm(Map<Integer, String> choices) {
        this.choices = choices;
        init();
    }

    private void init() {
        for (Integer i : choices.keySet()) {
            taskElementMap.put(String.valueOf(i), new ArrayList<TaskDelegationForm.TaskElement>());
        }
    }

    public Map<Integer, String> getChoices() {
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
