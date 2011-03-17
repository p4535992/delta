package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * @author Alar Kvell
 */
public class Workflow extends BaseWorkflowObject implements Serializable {
    private static final long serialVersionUID = 1L;

    private CompoundWorkflow parent;
    private List<Task> tasks = new ArrayList<Task>();
    private List<Task> removedTasks = new ArrayList<Task>();
    protected WmNode newTaskTemplate;
    protected Class<? extends Task> newTaskClass;
    protected int newTaskOutcomes;

    protected static <T extends Workflow> T create(Class<T> workflowClass, WmNode node, CompoundWorkflow parent, WmNode newTaskTemplate,
            Class<? extends Task> newTaskClass, int newTaskOutcomes) {
        try {
            return workflowClass.getDeclaredConstructor(WmNode.class, CompoundWorkflow.class, WmNode.class, Class.class, Integer.class).newInstance(node,
                    parent, newTaskTemplate, newTaskClass, newTaskOutcomes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // If you change constructor arguments, update create method above accordingly
    protected Workflow(WmNode node, CompoundWorkflow parent, WmNode newTaskTemplate, Class<? extends Task> newTaskClass, Integer newTaskOutcomes) {
        super(node);
        // parent can be null, WorkflowService#getTask(NodeRef) does not fetch parent
        this.parent = parent;
        Assert.isTrue((newTaskTemplate != null && newTaskClass != null) || (newTaskTemplate == null && newTaskClass == null));
        this.newTaskTemplate = newTaskTemplate;
        this.newTaskClass = newTaskClass;
        Assert.notNull(newTaskOutcomes);
        this.newTaskOutcomes = newTaskOutcomes;
    }

    protected Workflow copy(CompoundWorkflow copyParent) {
        // no need to copy newTaskTemplate, it is not changed ever
        return copyImpl(new Workflow(getNode().copy(), copyParent, newTaskTemplate, newTaskClass, newTaskOutcomes));
    }

    @Override
    protected <T extends BaseWorkflowObject> T copyImpl(T copy) {
        Workflow workflow = (Workflow) super.copyImpl(copy);
        for (Task task : tasks) {
            workflow.tasks.add(task.copy(workflow));
        }
        for (Task removedTask : removedTasks) {
            workflow.removedTasks.add(removedTask.copy(workflow));
        }
        @SuppressWarnings("unchecked")
        T result = (T) workflow;
        return result;
    }

    protected void postCreate() {
        // Subclasses can override
    }

    public CompoundWorkflow getParent() {
        return parent;
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    protected List<Task> getModifiableTasks() {
        return tasks;
    }

    protected List<Task> getRemovedTasks() {
        return removedTasks;
    }

    public void removeTask(int index) {
        removedTasks.add(tasks.remove(index));
    }

    protected void addTask(Task task) {
        tasks.add(task);
    }

    public boolean isAddTaskAllowed() {
        return newTaskTemplate != null && newTaskClass != null;
    }

    public Task addTask() {
        return addTask(tasks.size());
    }

    public Task addTask(int index) {
        if (newTaskTemplate == null || newTaskClass == null) {
            throw new RuntimeException("Adding tasks is not allowed");
        }
        WmNode node = new WmNode(null, newTaskTemplate.getType(), newTaskTemplate.getProperties(), newTaskTemplate.getAspects());
        Task task = Task.create(newTaskClass, node, this, newTaskOutcomes);
        tasks.add(index, task);
        return task;
    }

    public boolean isParallelTasks() {
        Boolean parallelTasks = getProp(WorkflowCommonModel.Props.PARALLEL_TASKS);
        if (parallelTasks == null) {
            return false;
        }
        return parallelTasks;
    }

    public void setParallelTasks(boolean parallelTasks) {
        setProp(WorkflowCommonModel.Props.PARALLEL_TASKS, parallelTasks);
    }

    public boolean isStopOnFinish() {
        Boolean stopOnFinish = getProp(WorkflowCommonModel.Props.STOP_ON_FINISH);
        return stopOnFinish;
    }

    public void setStopOnFinish(boolean stopOnFinish) {
        setProp(WorkflowCommonModel.Props.STOP_ON_FINISH, stopOnFinish);
    }

    @Override
    protected String additionalToString() {
        return "\n  parent=" + WmNode.toString(getParent()) + "\n  tasks=" + WmNode.toString(getTasks()) + "\n  removedTasks="
                + WmNode.toString(getRemovedTasks());
    }

    @Override
    protected void preSave() {
        super.preSave();

        if (getChangedProperties().containsKey(WorkflowSpecificModel.Props.RESOLUTION)) {
            for (Task task : getTasks()) {
                task.setProp(WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION, getProp(WorkflowSpecificModel.Props.RESOLUTION));
            }
        }
    }
}