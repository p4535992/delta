package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.Predicate;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.workflow.exception.WorkflowChangedException;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.web.TaskGroup;

/**
 * @author Alar Kvell
 */
public class WorkflowUtil {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(WorkflowUtil.class);
    /**
     * denotes that BaseWorkflowObject (task or information/opinion workflow) temporarily having this property is not saved,
     * but generated for delegating original assignment task to other people
     */
    private static final QName TMP_ADDED_BY_DELEGATION = RepoUtil.createTransientProp("addedByDelegation");
    public static final String TASK_INDEX = "taskIndex";

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
        return isStatusAndType(object, (QName[]) null, statuses);
    }

    public static boolean isStatusAndType(BaseWorkflowObject object, QName[] types, Status... statuses) {
        String realStatus = object.getStatus();
        for (Status status : statuses) {
            if (status.equals(realStatus) && (types == null || object.isType(types))) {
                return true;
            }
        }
        return false;
    }

    public static StatusOrderChecker isStatusOrder(List<? extends BaseWorkflowObject> objects) {
        return new StatusOrderChecker(objects);
    }

    public static class StatusOrderChecker {
        private final List<? extends BaseWorkflowObject> objects;
        private boolean result = true;
        private int index = 0;
        private final StringBuilder sb = new StringBuilder();

        StatusOrderChecker(List<? extends BaseWorkflowObject> objects) {
            if (LOG.isDebugEnabled()) {
                sb.append("Checking " + objects.size() + " objects: [");
                for (BaseWorkflowObject object : objects) {
                    sb.append(object.getStatus()).append(" ").append(object.getType().getLocalName()).append(", ");
                }
                sb.append("]: ");
            }
            this.objects = objects;
        }

        /**
         * Allows/consumes objects(increments index for each consecutive object) that have status one of given <code>statuses</code>
         * 
         * @param statuses - acceptable statuses
         * @return this for method chaining
         */
        public StatusOrderChecker requireAny(Status... statuses) {
            return requireAny((QName[]) null, statuses);
        }

        /**
         * Allows/consumes objects(increments index for each consecutive object) that have status one of given <code>statuses</code> and if <code>types</code> are given, then
         * object type is one of <code>types</code>
         * 
         * @param types if not null, then object type must match one of these
         * @param statuses - acceptable statuses
         * @return this for method chaining
         */
        public StatusOrderChecker requireAny(QName[] types, Status... statuses) {
            addTypesAndStatusesInfo("requireAny", types, statuses);
            if (!result) {
                return this;
            }
            while (index < objects.size() && isStatusAndType(objects.get(index), types, statuses)) {
                index++;
            }
            return this;
        }

        public StatusOrderChecker requireOne(Status... statuses) {
            return requireOne((QName[]) null, statuses);
        }

        private StatusOrderChecker requireOne(QName[] types, Status... statuses) {
            addTypesAndStatusesInfo("requireOne", types, statuses);
            if (!result) {
                return this;
            }
            result = index < objects.size() && isStatusAndType(objects.get(index), types, statuses);
            if (!result) {
                addFailureInfo();
            }
            index++;
            return this;
        }

        private void addFailureInfo() {
            sb.append("<-FAILURE! ");
        }

        public StatusOrderChecker requireAtLeastOne(Status... statuses) {
            return requireOne(statuses).requireAny(statuses);
        }

        public StatusOrderChecker requireAtLeastOne(QName[] types, Status... statuses) {
            return requireOne(types, statuses).requireAny(types, statuses);
        }

        /**
         * Consume objects with given types and statuses, return true if at least one of consumed workflows had required status
         */
        public StatusOrderChecker requireAtLeastOneWithinStatuses(QName[] types, Status requiredStatus, Status... statuses) {
            addTypesAndStatusesInfo("requireAtLeastOneWithinStatuses", types, statuses);
            if (!result) {
                return this;
            }
            boolean hasRequiredStatus = false;
            while (index < objects.size() && isStatusAndType(objects.get(index), types, statuses)) {
                hasRequiredStatus |= isStatus(objects.get(index), requiredStatus);
                index++;
            }
            result = hasRequiredStatus;
            if (!result) {
                addFailureInfo();
            }
            return this;
        }

        public boolean check() {
            return result && index == objects.size();
        }

        // METHODs for creating info about validation
        private void addTypesAndStatusesInfo(String constraint, QName[] types, Status... statuses) {
            if (LOG.isDebugEnabled()) {
                sb.append(constraint).append("{");
                if (types != null) {
                    sb.append("types(");
                    List<String> typesStr = new ArrayList<String>(statuses.length);
                    for (QName type : types) {
                        typesStr.add(type.getLocalName());
                    }
                    sb.append(StringUtils.join(typesStr, "|")).append(") ");
                }
                sb.append("statuses(");
                addStatusesInfo(statuses);
                sb.append("} ");
            }
        }

        private void addStatusesInfo(Status... statuses) {
            if (LOG.isDebugEnabled()) {
                List<String> parts = new ArrayList<String>(statuses.length);
                for (Status status : statuses) {
                    parts.add(status.getName());
                }
                sb.append(StringUtils.join(parts, "|"));
            }
        }

        @Override
        public String toString() {
            String result = sb.toString();
            if (check()) {
                return result;
            }
            int unConsumed = objects.size() - index;
            return "INVALID ORDER (" + (unConsumed != 0 ? unConsumed + " elements unconsumed" : "") + ") " + result;
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
            // if (StringUtils.isBlank(task.getOwnerId()) == StringUtils.isBlank(task.getOwnerEmail())) {
            // throw new RuntimeException("Exactly one of task's ownerId or ownerEmail must be filled\n" + task);
            // }
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

    public static List<String> getOwnersWithNoEmail(CompoundWorkflow compoundWorkflow) {
        List<String> ownersNames = new ArrayList<String>();
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            for (Task task : workflow.getTasks()) {
                String ownerEmail = task.getOwnerEmail();
                String ownerName = task.getOwnerName();
                if (StringUtils.isNotBlank(ownerName) && StringUtils.isBlank(ownerEmail)) {
                    ownersNames.add(ownerName);
                }
            }
        }
        return ownersNames;
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
                if (!isStatusOrder(tasks).requireAny(Status.FINISHED, Status.UNFINISHED).requireOne(Status.IN_PROGRESS).requireAny(Status.NEW).check()) {
                    throw new WorkflowChangedException(
                            "If workflow status is IN_PROGRESS, then tasks must have the following statuses, in order: 0..* FINISHED or UNFINISHED, 1 IN_PROGRESS, 0..* NEW\n"
                                    + workflow);
                }
            }
            break;
        case STOPPED:
            if (workflow.isParallelTasks()) {
                if (!isStatusAll(tasks, Status.NEW, Status.STOPPED, Status.FINISHED, Status.UNFINISHED)) {
                    throw new WorkflowChangedException("If workflow status is STOPPED, then all tasks must have status STOPPED or FINISHED or UNFINISHED\n"
                            + workflow);
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
        Status cWfStatus = Status.of(compoundWorkflow.getStatus());
        List<Workflow> workflows = compoundWorkflow.getWorkflows();
        if (workflows.size() == 0 && cWfStatus != Status.NEW && cWfStatus != Status.FINISHED) {
            throw new WorkflowChangedException("CompoundWorkflow must have at least one workflow if status is not NEW nor FINISHED\n" + compoundWorkflow);
        }
        for (Workflow workflow : workflows) {
            checkWorkflow(workflow, skipPropChecks);
        }
        switch (cWfStatus) {
        case NEW:
            if (!isStatusAll(workflows, Status.NEW)) {
                throw new WorkflowChangedException("If compoundWorkflow status is NEW, then all workflows must have status NEW\n" + compoundWorkflow);
            }
            break;
        case IN_PROGRESS:
            Status[] inProgressAllowedStatuses = { Status.IN_PROGRESS, Status.FINISHED };
            if (!isValidInProgressOrStopped(workflows, cWfStatus, inProgressAllowedStatuses)) {
                throw new WorkflowChangedException(getNotValidInProgressOrStoppedMsg(compoundWorkflow, cWfStatus, inProgressAllowedStatuses));
            }
            break;
        case STOPPED:
            Status[] stoppedAllowedStatuses = { Status.STOPPED, Status.FINISHED };
            if (!isValidInProgressOrStopped(workflows, cWfStatus, stoppedAllowedStatuses)
                    && !isStatusOrder(workflows).requireAtLeastOne(Status.FINISHED).requireAny(Status.NEW, Status.FINISHED).check()) {
                throw new WorkflowChangedException(getNotValidInProgressOrStoppedMsg(compoundWorkflow, cWfStatus, stoppedAllowedStatuses)
                        + "\nOR as an alternative following order: 1..* FINISHED, 0..* NEW or FINISHED\n" + compoundWorkflow);
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
        return cWfStatus;
    }

    private static String getNotValidInProgressOrStoppedMsg(CompoundWorkflow compoundWorkflow, Status cWfStatus, Status[] allowedStatuses) {
        List<String> statusNames = new ArrayList<String>();
        for (Status status : allowedStatuses) {
            statusNames.add(status.name());
        }
        return "If compoundWorkflow status is " + cWfStatus.name() + ", then workflows must have the following statuses, in order:" +
                " 0..* FINISHED, (1 " + cWfStatus.name() + " OR (1..* parallely startable workflows " + TextUtil.joinNonBlankStrings(statusNames, " OR ")
                + " with at least one " + cWfStatus.name() + ")), 0..* NEW or FINISHED\n" + compoundWorkflow;
    }

    private static boolean isValidInProgressOrStopped(List<Workflow> workflows, Status requiredStatus, Status... cWfStatuses) {
        boolean isValidWithoutParallel = isStatusOrder(workflows).requireAny(Status.FINISHED).requireOne(requiredStatus).requireAny(Status.NEW, Status.FINISHED).check();
        return isValidWithoutParallel || isValidParallel(workflows, requiredStatus, cWfStatuses);
    }

    private static boolean isValidParallel(List<Workflow> workflows, Status requiredStatus, Status... cWfStatuses) {
        return isStatusOrder(workflows).requireAny(Status.FINISHED).requireAtLeastOneWithinStatuses(WorkflowSpecificModel.CAN_START_PARALLEL, requiredStatus, cWfStatuses)
                .requireAny(Status.NEW, Status.FINISHED).check();
    }

    public static boolean isStatusChanged(BaseWorkflowObject object) {
        if (RepoUtil.isSaved(object.getNode()) && object.isChangedProperty(WorkflowCommonModel.Props.STATUS)) {
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
            throw new WorkflowChangedException("Changing status is not permitted outside of service:\n" + object);
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

    public static List<NodeRef> getExcludedNodeRefsOnFinishWorkflows(CompoundWorkflow compoundWorkflow) {
        List<NodeRef> excludedNodeRefs = new ArrayList<NodeRef>();
        for (Workflow workflow : compoundWorkflow.getWorkflows()) {
            if (WorkflowSpecificModel.Types.INFORMATION_WORKFLOW.equals(workflow.getNode().getType())) {
                excludedNodeRefs.add(workflow.getNodeRef());
            }
        }
        return excludedNodeRefs;
    }

    public static boolean isActiveResponsible(Task task) {
        return isResponsible(task) && Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE));
    }

    public static boolean isInactiveResponsible(Task task) {
        return isResponsible(task) && Boolean.FALSE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE));
    }

    public static boolean isResponsible(Task task) {
        return task.getNode().hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE);
    }

    public static void removeEmptyTasks(CompoundWorkflow cWorkflow) {
        for (Workflow workflow : cWorkflow.getWorkflows()) {
            WorkflowUtil.removeEmptyTasks(workflow);
        }
    }

    private static void removeEmptyTasks(Workflow workflow) {
        ArrayList<Integer> emptyTaskIndexes = new ArrayList<Integer>();
        int index = 0;
        for (Task task : workflow.getTasks()) {
            if (task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK)) {
                if (StringUtils.isBlank(task.getInstitutionName())
                        && task.getDueDate() == null) {
                    emptyTaskIndexes.add(index);
                }
            } else if (isEmptyTask(task)) {
                emptyTaskIndexes.add(index);
            }
            index++;
        }
        Collections.reverse(emptyTaskIndexes);
        for (int taskIndex : emptyTaskIndexes) {
            workflow.removeTask(taskIndex);
        }
    }

    private static boolean isEmptyTask(Task task) {
        return StringUtils.isBlank(task.getOwnerName()) && task.getDueDate() == null && task.getDueDateDays() == null && StringUtils.isBlank(task.getResolutionOfTask())
                && !(isGeneratedByDelegation(task) && WorkflowUtil.isActiveResponsible(task) && !task.isType(WorkflowSpecificModel.Types.ORDER_ASSIGNMENT_TASK));
    }

    public static boolean isGeneratedByDelegation(BaseWorkflowObject workflowObject) {
        return Boolean.TRUE.equals(workflowObject.getNode().getProperties().get(TMP_ADDED_BY_DELEGATION.toString()));
    }

    public static void markAsGeneratedByDelegation(BaseWorkflowObject workflowObject) {
        workflowObject.getNode().getProperties().put(TMP_ADDED_BY_DELEGATION.toString(), Boolean.TRUE);
    }

    /**
     * Controls if there are tasks with same type and ownerId, except tasks with status NEW or UNFINISHED
     * 
     * @param compoundWorkflow
     * @return
     */
    public static Set<Pair<String, QName>> haveSameTask(CompoundWorkflow compoundWorkflow) {
        Set<Pair<String, QName>> ownerNameTypeSet = new HashSet<Pair<String, QName>>();
        Set<Pair<String, QName>> thisTasks = new HashSet<Pair<String, QName>>();
        for (Workflow wf : compoundWorkflow.getWorkflows()) {
            for (Task task : wf.getTasks()) {
                if (StringUtils.isBlank(task.getOwnerId())) {
                    continue;
                }
                Pair<String, QName> taskOwnerNameAndType = new Pair<String, QName>(task.getOwnerId(), task.getType());
                if (thisTasks.contains(taskOwnerNameAndType)) {
                    if (!task.isStatus(Status.NEW, Status.UNFINISHED)) {
                        ownerNameTypeSet.add(taskOwnerNameAndType);
                    }
                } else {
                    thisTasks.add(taskOwnerNameAndType);
                }
            }
        }
        if (thisTasks.isEmpty()) {
            return ownerNameTypeSet;
        }
        for (CompoundWorkflow compWf : compoundWorkflow.getOtherCompoundWorkflows()) {
            if (isStatus(compWf, Status.NEW)) {
                continue;
            }
            for (Workflow workflow : compWf.getWorkflows()) {
                if (isStatus(workflow, Status.NEW)) {
                    continue;
                }
                for (Task task : workflow.getTasks()) {
                    if (task.isStatus(Status.NEW, Status.UNFINISHED) || StringUtils.isBlank(task.getOwnerId())) {
                        continue;
                    }
                    Pair<String, QName> taskOwnerNameAndType = new Pair<String, QName>(task.getOwnerId(), task.getType());
                    if (thisTasks.contains(taskOwnerNameAndType)) {
                        ownerNameTypeSet.add(taskOwnerNameAndType);
                    }
                }
            }
        }

        return ownerNameTypeSet;
    }

    public static Task createTaskCopy(Task myTask) {
        Workflow workflow = myTask.getParent();
        CompoundWorkflow cWorkflowCopy = workflow.getParent().copy();
        NodeRef myTaskRef = myTask.getNodeRef();
        for (Workflow wf : cWorkflowCopy.getWorkflows()) {
            if (wf.getNodeRef().equals(workflow.getNodeRef())) {
                for (Task task : wf.getTasks()) {
                    if (myTaskRef.equals(task.getNodeRef())) {
                        return task;
                    }
                }
            }
        }
        throw new RuntimeException("This never happens");
    }

    public static void setWorkflowResolution(List<Task> tasks, Serializable workflowResolution, Status... allowedStatuses) {
        if (tasks == null) {
            return;
        }
        for (Task task : tasks) {
            if (task.isStatus(allowedStatuses)
                    && StringUtils.isBlank((String) task.getProp(WorkflowSpecificModel.Props.RESOLUTION))
                    && !WorkflowUtil.isGeneratedByDelegation(task)) {
                task.setProp(WorkflowSpecificModel.Props.RESOLUTION, workflowResolution);
            }
        }

    }

    public static String getDialogId(FacesContext context, UIComponent component) {
        return component.getClientId(context) + "_popup";
    }

    public static String getActionId(FacesContext context, UIComponent component) {
        return component.getParent().getClientId(context) + "_action";
    }

    public static boolean isTaskRowEditable(boolean responsible, boolean fullAccess, Task task, String taskStatus) {
        return fullAccess && Status.NEW.equals(taskStatus)
                && (!responsible || Boolean.TRUE.equals(task.getNode().getProperties().get(WorkflowSpecificModel.Props.ACTIVE)));
    }

    public static void setGroupTasksDueDates(TaskGroup taskGroup, List<Task> tasks) {
        Date dueDate = taskGroup.getDueDate();
        for (Integer taskId : taskGroup.getTaskIds()) {
            Task task = tasks.get(taskId);
            if (isTaskRowEditable(taskGroup.isResponsible(), taskGroup.isFullAccess(), task, task.getStatus())) {
                task.setDueDate(dueDate);
            }
        }
    }

    public static Set<Task> getTasks(Set<Task> selectedTasks, List<CompoundWorkflow> compoundWorkflows, Predicate<Task> taskPredicate) {
        if (compoundWorkflows == null) {
            return selectedTasks;
        }
        for (CompoundWorkflow compoundWorkflow : compoundWorkflows) {
            for (Workflow workflow : compoundWorkflow.getWorkflows()) {
                List<Task> tasks = workflow.getTasks();
                for (Task task : tasks) {
                    if (taskPredicate.evaluate(task)) {
                        selectedTasks.add(task);
                    }
                }
            }
        }
        return selectedTasks;
    }

}