package ee.webmedia.alfresco.workflow.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;

public class WorkflowSummaryItem implements Serializable, Comparable<WorkflowSummaryItem> {

    private static final long serialVersionUID = 1L;

    private String name;
    private Date started;
    private Date stopped;
    private String creatorName;
    private String responsibleName;
    private String status;
    private String resolution;

    private boolean raisedRights;
    private boolean parallel;
    private boolean assignmentWorkflow;
    private Workflow workflow;
    private boolean taskView = false;
    private boolean taskViewVisible = true;
    private String taskOwnerRole;

    private List<Task> tasks;
    private List<Task> assignmentResponsibleTasks;
    private List<Task> assignmentTasks;

    public WorkflowSummaryItem(Workflow workflow) {
        this.workflow = workflow;
        getTitlesByType(workflow.getNode().getType());
        this.started = workflow.getStartedDateTime();
        this.stopped = workflow.getStoppedDateTime();
        this.creatorName = workflow.getCreatorName();
        this.responsibleName = workflow.getParent().getOwnerName();
        this.status = workflow.getStatus();
        this.resolution = workflow.getProp(WorkflowSpecificModel.Props.RESOLUTION);
        this.assignmentWorkflow = workflow.getNode().getType().equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW);

        this.tasks = new ArrayList<Task>();
        for (Task task : workflow.getTasks()) {
            this.tasks.add(task);
        }

        Collections.sort(tasks, new WorkflowSummaryItemTaskComparator());
        if (this.assignmentWorkflow) {
            separateAssignmentTasks();
        }
    }

    private void separateAssignmentTasks() {
        assignmentResponsibleTasks = new ArrayList<Task>();
        assignmentTasks = new ArrayList<Task>();
        for (Task task : tasks) {
            if (task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)) {
                assignmentResponsibleTasks.add(task);
            } else {
                assignmentTasks.add(task);
            }
        }
    }

    private void getTitlesByType(QName type) {
        name = "";
        taskOwnerRole = "";

        if (type.equals(WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW)) {
            name = I18NUtil.getMessage("workflow_signature_title");
            taskOwnerRole = I18NUtil.getMessage("workflow_signature_task_owner_role");
        } else if (type.equals(WorkflowSpecificModel.Types.OPINION_WORKFLOW)) {
            name = I18NUtil.getMessage("workflow_opinion_title");
            taskOwnerRole = I18NUtil.getMessage("workflow_opinion_task_owner_role");
        } else if (type.equals(WorkflowSpecificModel.Types.INFORMATION_WORKFLOW)) {
            name = I18NUtil.getMessage("workflow_information_title");
            taskOwnerRole = I18NUtil.getMessage("workflow_information_task_owner_role");
        } else if (type.equals(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)) {
            name = I18NUtil.getMessage("workflow_assignment_title");
            taskOwnerRole = I18NUtil.getMessage("workflow_assignment_task_owner_role");
        } else if (type.equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW) && workflow.isParallelTasks()) {
            name = I18NUtil.getMessage("workflow_review_parallel_title");
            taskOwnerRole = I18NUtil.getMessage("workflow_review_task_owner_role");
        } else if (type.equals(WorkflowSpecificModel.Types.REVIEW_WORKFLOW) && !workflow.isParallelTasks()) {
            name = I18NUtil.getMessage("workflow_review_title");
            taskOwnerRole = I18NUtil.getMessage("workflow_review_task_owner_role");
        }
    }

    public boolean isAssignment() {
        return false;
    }

    public boolean isNameAsLink() {
        return false;
    }

    public NodeRef getCompoundWorkflowRef() {
        return getWorkflow().getParent().getNode().getNodeRef();
    }

    public void toggleTaskViewVisible(ActionEvent event) {
        this.taskViewVisible = !isTaskViewVisible();
    }

    public boolean isTaskViewRendered() {
        return isTaskView() && isTaskViewVisible() && !isAssignmentWorkflow();
    }

    public boolean isAssignmentTaskViewRendered() {
        return isTaskView() && isTaskViewVisible() && isAssignmentWorkflow();
    }

    // START: getters/setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getStopped() {
        return stopped;
    }

    public void setStopped(Date stopped) {
        this.stopped = stopped;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getResponsibleName() {
        return responsibleName;
    }

    public void setResponsibleName(String responsibleName) {
        this.responsibleName = responsibleName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolution() {
        if(resolution == null) {
            return "";
        }
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public boolean isParallel() {
        return parallel;
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public void setTaskView(boolean taskView) {
        this.taskView = taskView;
    }

    public boolean isTaskView() {
        return taskView;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public boolean isRaisedRights() {
        return raisedRights;
    }

    public void setRaisedRights(boolean raisedRights) {
        this.raisedRights = raisedRights;
    }

    public String getTaskOwnerRole() {
        return taskOwnerRole;
    }

    public void setTaskOwnerRole(String taskOwnerRole) {
        this.taskOwnerRole = taskOwnerRole;
    }

    public boolean isAssignmentWorkflow() {
        return assignmentWorkflow;
    }

    public void setAssignmentWorkflow(boolean assignmentWorkflow) {
        this.assignmentWorkflow = assignmentWorkflow;
    }

    public List<Task> getAssignmentResponsibleTasks() {
        return assignmentResponsibleTasks;
    }

    public void setAssignmentResponsibleTasks(List<Task> assignmentResponsibleTasks) {
        this.assignmentResponsibleTasks = assignmentResponsibleTasks;
    }

    public List<Task> getAssignmentTasks() {
        return assignmentTasks;
    }

    public void setAssignmentTasks(List<Task> assignmentTasks) {
        this.assignmentTasks = assignmentTasks;
    }

    public boolean isTaskViewVisible() {
        return taskViewVisible;
    }

    // END: getters/setters

    // COMPARING

    @Override
    public int compareTo(WorkflowSummaryItem o) {
        // a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
        if (getStarted() != null && o.getStarted() != null) {
            if (getStarted().before(o.getStarted())) {
                return -1;
            } else if (getStarted().after(o.getStarted())) {
                return 1;
            }
        }

        if (getStopped() != null && o.getStopped() != null) {
            if (getStopped().before(o.getStopped())) {
                return -1;
            } else if (getStopped().after(o.getStopped())) {
                return 1;
            }
        }

        if (o.equals(this)) {
            if (isTaskView()) {
                return 1;
            } else if (o.isTaskView()) {
                return -1;
            }
        }

        if (getStarted() == null && getStopped() == null) {
            return 1;
        }

        return 0;
    }

    public class WorkflowSummaryItemTaskComparator implements Comparator<Task> {

        @Override
        public int compare(Task task1, Task task2) {
            if (task1.getStartedDateTime() == null || task2.getStartedDateTime() == null) {
                return checkDueDate(task1, task2);
            }

            if (task1.getStartedDateTime().before(task2.getStartedDateTime())) {
                return -1;
            } else if (task1.getStartedDateTime().after(task2.getStartedDateTime())) {
                return 1;
            }

            return checkDueDate(task1, task2);
        }

        private int checkDueDate(Task task1, Task task2) {
            if (task1.getDueDate() != null && task2.getDueDate() != null) {
                if (task1.getDueDate().before(task2.getDueDate())) {
                    return -1;
                } else if (task1.getDueDate().after(task2.getDueDate())) {
                    return 1;
                }
            }

            return checkOwnerName(task1, task2);
        }

        private int checkOwnerName(Task task1, Task task2) {
            if(task1.getOwnerName() != null && task2.getOwnerName() != null) {
                return task1.getOwnerName().compareTo(task2.getOwnerName());
            }
            if(task1.getOwnerName() == null) {
                return 1;
            }
            return -1;
        }

    }

    public NodeRef getWorkflowRef() {
        return workflow.getNode().getNodeRef();
    }

    @Override
    public int hashCode() {
        return getWorkflowRef().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof WorkflowSummaryItem)) {
            return false;
        }
        return getWorkflowRef().equals(((WorkflowSummaryItem)obj).getWorkflowRef());
        
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WorkflowSummaryItem [");
        if (getWorkflowRef() != null)
            builder.append("workflowRef=").append(getWorkflowRef()).append(", ");
        builder.append("taskView=").append(taskView).append(", ");
        if (status != null)
            builder.append("status=").append(status).append(", \n");
        if (name != null)
            builder.append("name=").append(name).append(", ");
        if (started != null)
            builder.append("started=").append(started).append(", ");
        if (stopped != null)
            builder.append("stopped=").append(stopped).append(", \n");
        builder.append("raisedRights=").append(raisedRights).append(", assignmentWorkflow=").append(assignmentWorkflow).append(", ");
        if (creatorName != null)
            builder.append("creatorName=").append(creatorName).append(", ");
        builder.append("parallel=").append(parallel).append(", ");
        if (responsibleName != null)
            builder.append("responsibleName=").append(responsibleName).append(", \n");
        if (resolution != null)
            builder.append("resolution=").append(resolution);
        builder.append("]");
        return builder.toString();
    }

}
