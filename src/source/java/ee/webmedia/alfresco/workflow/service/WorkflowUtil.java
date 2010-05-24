package ee.webmedia.alfresco.workflow.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.event.WorkflowEventQueue;

/**
 * @author Alar Kvell
 */
public class WorkflowUtil {

    public static WorkflowEventQueue getNewEventQueue() {
        return new WorkflowEventQueue();
    }

    // -------------
    // Checks that are required only on memory object

    public static boolean isStatusAll(Collection<? extends BaseWorkflowObject> objects, Status... statuses) {
        for (BaseWorkflowObject object : objects) {
            if (!isStatus(object, statuses)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isStatusAny(Collection<? extends BaseWorkflowObject> objects, Status... statuses) {
        for (BaseWorkflowObject object : objects) {
            if (isStatus(object, statuses)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStatus(BaseWorkflowObject object, Status... statuses) {
        boolean equals = false;
        for (Status status : statuses) {
            if (status.equals(object.getStatus())) {
                equals = true;
                break;
            }
        }
        return equals;
    }

    public static StatusOrderChecker isStatusOrder(List<? extends BaseWorkflowObject> objects) {
        return new StatusOrderChecker(objects);
    }

    public static class StatusOrderChecker {
        private List<? extends BaseWorkflowObject> objects;
        private boolean result = true;
        private int index = 0;

        StatusOrderChecker(List<? extends BaseWorkflowObject> objects) {
            this.objects = objects;
        }

        public StatusOrderChecker requireAny(Status... statuses) {
            if (!result) {
                return this;
            }
            while (index < objects.size() && isStatus(objects.get(index), statuses)) {
                index++;
            }
            return this;
        }

        public StatusOrderChecker requireOne(Status... statuses) {
            if (!result) {
                return this;
            }
            result = index < objects.size() && isStatus(objects.get(index), statuses);
            index++;
            return this;
        }

        public StatusOrderChecker requireAtLeastOne(Status... statuses) {
            return requireOne(statuses).requireAny(statuses);
        }

        public boolean check() {
            return result && index == objects.size();
        }
    }

    // -------------
    // NEW checks

    public static Status checkTask(Task task, Status... requiredStatuses) {
        return checkTask(task, false, requiredStatuses);
    }

    public static Status checkTask(Task task, boolean skipPropChecks, Status... requiredStatuses) {
        if (!skipPropChecks) {
            // ERKO: Specification and existing code act in a different way. When a user is chosen, both the id and email are stored and used. 
            //if (StringUtils.isBlank(task.getOwnerId()) == StringUtils.isBlank(task.getOwnerEmail())) {
            //    throw new RuntimeException("Exactly one of task's ownerId or ownerEmail must be filled\n" + task);
            //}
        }
        Status status = Status.of(task.getStatus());
        if (requiredStatuses.length > 0 && !isStatus(task, requiredStatuses)) {
            throw new WorkflowChangedException("Task status must be one of [" + StringUtils.join(requiredStatuses, ", ") + "]\n" + task);
        }
        return status;
    }

    public static Status checkWorkflow(Workflow workflow, Status... requiredStatuses) {
        return checkWorkflow(workflow, false, requiredStatuses);
    }

    public static Status checkWorkflow(Workflow workflow, boolean skipPropChecks, Status... requiredStatuses) {
        Status status = Status.of(workflow.getStatus());
        List<Task> tasks = workflow.getTasks();
        if (tasks.size() == 0 && status != Status.NEW && status != Status.FINISHED) {
            throw new WorkflowChangedException("Workflow must have at least one task if status is not NEW nor FINISHED\n" + workflow);
        }
        for (Task task : tasks) {
            checkTask(task, skipPropChecks);
        }
        switch (status) {
        case NEW:
            if (!isStatusAll(tasks, Status.NEW)) {
                throw new WorkflowChangedException("If workflow status is NEW, then all tasks must have status NEW\n" + workflow);
            }
            break;
        case IN_PROGRESS:
            if (workflow.isParallelTasks()) {
                if (!isStatusAny(tasks, Status.IN_PROGRESS) || !isStatusAll(tasks, Status.IN_PROGRESS, Status.FINISHED, Status.UNFINISHED)) {
                    throw new WorkflowChangedException(
                            "If workflow status is IN_PROGRESS, then at least one task must have status IN_PROGRESS and other must have status FINISHED or UNFINISHED\n"
                                    + workflow);
                }
            } else {
                if (!isStatusOrder(tasks).requireAny(Status.FINISHED, status.UNFINISHED).requireOne(Status.IN_PROGRESS).requireAny(Status.NEW).check()) {
                    throw new WorkflowChangedException(
                            "If workflow status is IN_PROGRESS, then tasks must have the following statuses, in order: 0..* FINISHED or UNFINISHED, 1 IN_PROGRESS, 0..* NEW\n"
                                    + workflow);
                }
            }
            break;
        case STOPPED:
            if (workflow.isParallelTasks()) {
                if (!isStatusAll(tasks, Status.NEW, Status.STOPPED, Status.FINISHED, Status.UNFINISHED)) {
                    throw new WorkflowChangedException("If workflow status is STOPPED, then all tasks must have status STOPPED or FINISHED or UNFINISHED\n" + workflow);
                }
            } else {
                if (!isStatusOrder(tasks).requireAny(Status.FINISHED, Status.UNFINISHED).requireOne(Status.STOPPED).requireAny(Status.NEW).check()
                        && !isStatusOrder(tasks).requireAtLeastOne(Status.FINISHED, Status.UNFINISHED).requireAny(Status.NEW).check()) {
                    throw new WorkflowChangedException(
                            "If workflow status is STOPPED, then tasks must have the following statuses, in order: (0..* FINISHED or UNFINISHED, 1 STOPPED, 0..* NEW) or (1..* FINISHED or UNFINISHED, 0..* NEW)\n"
                                    + workflow);
                }
            }
            break;
        case FINISHED:
            if (!isStatusAll(tasks, Status.FINISHED, Status.UNFINISHED)) {
                throw new WorkflowChangedException("If workflow status is FINISHED, then all tasks must have status FINISHED or UNFINISHED\n" + workflow);
            }
            break;
        case UNFINISHED:
            throw new WorkflowChangedException("Workflow cannot have status UNFINISHED\n" + workflow);
        }
        if (requiredStatuses.length > 0 && !isStatus(workflow, requiredStatuses)) {
            throw new WorkflowChangedException("Workflow status must be one of [" + StringUtils.join(requiredStatuses, ", ") + "]\n" + workflow);
        }
        return status;
    }

    public static Status checkCompoundWorkflow(CompoundWorkflow compoundWorkflow, Status... requiredStatuses) {
        return checkCompoundWorkflow(compoundWorkflow, false, requiredStatuses);
    }

    public static Status checkCompoundWorkflow(CompoundWorkflow compoundWorkflow, boolean skipPropChecks, Status... requiredStatuses) {
        Status status = Status.of(compoundWorkflow.getStatus());
        List<Workflow> workflows = compoundWorkflow.getWorkflows();
        if (workflows.size() == 0 && status != Status.NEW && status != Status.FINISHED) {
            throw new WorkflowChangedException("CompoundWorkflow must have at least one workflow if status is not NEW nor FINISHED\n" + compoundWorkflow);
        }
        for (Workflow workflow : workflows) {
            checkWorkflow(workflow, skipPropChecks);
        }
        switch (status) {
        case NEW:
            if (!isStatusAll(workflows, Status.NEW)) {
                throw new WorkflowChangedException("If compoundWorkflow status is NEW, then all workflows must have status NEW\n" + compoundWorkflow);
            }
            break;
        case IN_PROGRESS:
            if (!isStatusOrder(workflows).requireAny(Status.FINISHED).requireOne(Status.IN_PROGRESS).requireAny(Status.NEW).check()) {
                throw new WorkflowChangedException(
                        "If compoundWorkflow status is IN_PROGRESS, then workflows must have the following statuses, in order: 0..* FINISHED, 1 IN_PROGRESS, 0..* NEW\n"
                                + compoundWorkflow);
            }
            break;
        case STOPPED:
            if (!isStatusOrder(workflows).requireAny(Status.FINISHED).requireOne(Status.STOPPED).requireAny(Status.NEW).check()
                    && !isStatusOrder(workflows).requireAtLeastOne(Status.FINISHED).requireAny(Status.NEW).check()) {
                throw new WorkflowChangedException(
                        "If compoundWorkflow status is STOPPED, then workflows must have the following statuses, in order: (0..* FINISHED, 1 STOPPED, 0..* NEW) or (1..* FINISHED, 0..* NEW)\n"
                                + compoundWorkflow);
            }
            break;
        case FINISHED:
            if (!isStatusAll(workflows, Status.FINISHED)) {
                throw new WorkflowChangedException("If compoundWorkflow status is FINISHED, then all workflows must have status FINISHED\n" + compoundWorkflow);
            }
            break;
        case UNFINISHED:
            throw new WorkflowChangedException("CompoundWorkflow cannot have status UNFINISHED\n" + compoundWorkflow);
        }
        if (requiredStatuses.length > 0 && !isStatus(compoundWorkflow, requiredStatuses)) {
            throw new WorkflowChangedException("CompoundWorkflow status must be one of [" + StringUtils.join(requiredStatuses, ", ") + "]\n" + compoundWorkflow);
        }
        return status;
    }

    public static boolean isStatusChanged(BaseWorkflowObject object) {
        if (object.getNode().getNodeRef() != null && object.isChangedProperty(WorkflowCommonModel.Props.STATUS)) {
            return true;
        }
        if (object instanceof CompoundWorkflow) {
            for (Workflow workflow : ((CompoundWorkflow) object).getWorkflows()) {
                if (isStatusChanged(workflow)) {
                    return true;
                }
            }
        } else if (object instanceof Workflow) {
            for (Task task : ((Workflow) object).getTasks()) {
                if (isStatusChanged(task)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void requireStatusUnchanged(BaseWorkflowObject object) {
        if (isStatusChanged(object)) {
            throw new RuntimeException("Changing status is not permitted outside of service:\n" + object);
        }
    }

    // ========================================================================
    // ======================= FILTERING / PERMISSIONS ========================
    // ========================================================================

    public static List<Task> getFinishedTasks(List<CompoundWorkflow> compoundWorkflows, QName taskType) {
        List<Task> finishedTasks = new ArrayList<Task>();
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            List<Workflow> workflows = compoundWorkflow.getWorkflows();
            for (Workflow workflow : workflows) {
                List<Task> tasks = workflow.getTasks();
                for (Task task : tasks) {
                    if (isStatus(task, Status.FINISHED) && task.getNode().getType().equals(taskType)) {
                        finishedTasks.add(task);
                    }
                }
            }
        }
        return finishedTasks;
    }

    public static List<Task> getMyTasksInProgress(List<CompoundWorkflow> compoundWorkflows, String userName) {
        List<Task> myTasks = new ArrayList<Task>();
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            List<Workflow> workflows = compoundWorkflow.getWorkflows();
            for (Workflow workflow : workflows) {
                List<Task> tasks = workflow.getTasks();
                for (Task task : tasks) {
                    if (isOwnerOfInProgressTask(task, userName)) {
                        myTasks.add(task);
                    }
                }
            }
        }
        return myTasks;
    }

    private static boolean isOwnerOfInProgressTask(Task task, String userName) {
        return userName.equals(task.getOwnerId()) && isStatus(task, Status.IN_PROGRESS);
    }

    public static boolean isOwner(List<CompoundWorkflow> compoundWorkflows, String userName) {
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            if (isOwner(compoundWorkflow, userName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOwner(CompoundWorkflow compoundWorkflow, String userName) {
        return userName.equals(compoundWorkflow.getOwnerId());
    }

    public static boolean isOwner(Task task, String userName) {
        return userName.equals(task.getOwnerId());
    }

    /**
     * Gets all workflows from compoundWorkflows, filters out WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW
     */
    public static List<Workflow> getVisibleWorkflows(List<CompoundWorkflow> compoundWorkflows) {
        List<Workflow> workflows = new ArrayList<Workflow>();
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                if (!workflow.getNode().getType().equals(WorkflowSpecificModel.Types.DOC_REGISTRATION_WORKFLOW)) {
                    workflows.add(workflow);
                }
            }
        }
        return workflows;
    }

    public static boolean isActiveResponsible(Task task) {
        return task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)
                && Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE));
    }

    public static boolean isInactiveResponsible(Task task) {
        return task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE)
        && Boolean.FALSE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE));
    }
    
}
