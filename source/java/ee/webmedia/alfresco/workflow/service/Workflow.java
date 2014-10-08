package ee.webmedia.alfresco.workflow.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.SigningType;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

public class Workflow extends BaseWorkflowObject implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(BaseWorkflowObject.class);

    private final CompoundWorkflow parent;
    private final List<Task> tasks = new ArrayList<Task>();
    private final List<Task> removedTasks = new ArrayList<Task>();
    protected WmNode newTaskTemplate;
    protected Class<? extends Task> newTaskClass;
    protected int newTaskOutcomes;
    /**
     * Workflow's index in compound workflow during last save
     * (may not be current index if compond workflow is changed in memory).
     * At the moment used for secondary ordering in WorkflowBlock
     */
    private int indexInCompoundWorkflow;

    public static <T extends Workflow> T create(Class<T> workflowClass, WmNode node, CompoundWorkflow parent, WmNode newTaskTemplate,
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
        return copyImpl(new Workflow(getNode().clone(), copyParent, newTaskTemplate, newTaskClass, newTaskOutcomes));
    }

    @Override
    protected <T extends BaseWorkflowObject> T copyImpl(T copy) {
        Workflow workflow = (Workflow) super.copyImpl(copy);
        workflow.setIndexInCompoundWorkflow(indexInCompoundWorkflow);
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

    public void addTask(Task task) {
        tasks.add(task);
    }

    protected void addTask(Task task, int index) {
        tasks.add(index, task);
    }

    protected void addTasks(List<Task> tasks) {
        this.tasks.addAll(tasks);
    }

    public boolean isAddTaskAllowed() {
        return newTaskTemplate != null && newTaskClass != null;
    }

    public Task addTask() {
        return addTask(tasks.size());
    }

    public boolean hasTaskResolution() {
        return newTaskTemplate != null && newTaskTemplate.hasAspect(WorkflowSpecificModel.Aspects.RESOLUTION);
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

    public boolean isMandatory() {
        Boolean mandatory = getProp(WorkflowCommonModel.Props.MANDATORY);
        if (mandatory == null) {
            return false;
        }
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        setProp(WorkflowCommonModel.Props.MANDATORY, mandatory);
    }

    public Boolean getMandatory() {
        return getProp(WorkflowCommonModel.Props.MANDATORY);
    }

    public void setParallelTasks(boolean parallelTasks) {
        setProp(WorkflowCommonModel.Props.PARALLEL_TASKS, parallelTasks);
    }

    public Boolean getParallelTasks() {
        return getProp(WorkflowCommonModel.Props.PARALLEL_TASKS);
    }

    public boolean isStopOnFinish() {
        Boolean stopOnFinish = getProp(WorkflowCommonModel.Props.STOP_ON_FINISH);
        return stopOnFinish != null && stopOnFinish;
    }

    public void setStopOnFinish(boolean stopOnFinish) {
        setProp(WorkflowCommonModel.Props.STOP_ON_FINISH, stopOnFinish);
    }

    public Boolean getStopOnFinish() {
        return getProp(WorkflowCommonModel.Props.STOP_ON_FINISH);
    }

    public void setDescription(String description) {
        setProp(WorkflowSpecificModel.Props.DESCRIPTION, description);
    }

    public String getDescription() {
        return getProp(WorkflowSpecificModel.Props.DESCRIPTION);
    }

    public void setResolution(String resolution) {
        setProp(WorkflowSpecificModel.Props.RESOLUTION, resolution);
    }

    public String getResolution() {
        return getProp(WorkflowSpecificModel.Props.RESOLUTION);
    }

    public void setCategory(String category) {
        setProp(WorkflowSpecificModel.Props.CATEGORY, category);
    }

    public String getCategory() {
        return getProp(WorkflowSpecificModel.Props.CATEGORY);
    }

    public void setSigningType(SigningType signingType) {
        setProp(WorkflowSpecificModel.Props.SIGNING_TYPE, signingType != null ? signingType.toString() : null);
    }

    public boolean isSignTogether() {
        String signingTypeStr = getSigningTypeStr();
        return WorkflowUtil.isSignTogetherType(signingTypeStr);
    }

    public SigningType getSigningType() {
        String signingTypeStr = getSigningTypeStr();
        return StringUtils.isBlank(signingTypeStr) ? null : SigningType.valueOf(signingTypeStr);
    }

    private String getSigningTypeStr() {
        return getProp(WorkflowSpecificModel.Props.SIGNING_TYPE);
    }

    @Override
    protected String additionalToString() {
        if (LOG.isTraceEnabled()) {
            return "\n  parent=" + WmNode.toString(getParent()) + "\n  tasks=" + WmNode.toString(getTasks()) + "\n  removedTasks="
                    + WmNode.toString(getRemovedTasks());
        }
        return "";
    }

    @Override
    protected void preSave() {
        super.preSave();
        if (hasTaskResolution()) {
            WorkflowUtil.setWorkflowResolution(getTasks(), getProp(WorkflowSpecificModel.Props.RESOLUTION), Status.NEW, Status.IN_PROGRESS);
        }
        if (getChangedProperties().containsKey(WorkflowSpecificModel.Props.RESOLUTION)) {
            for (Task task : getTasks()) {
                task.setProp(WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION, getProp(WorkflowSpecificModel.Props.RESOLUTION));
            }
        }
    }

    public int getIndexInCompoundWorkflow() {
        return indexInCompoundWorkflow;
    }

    public void setIndexInCompoundWorkflow(int indexInCompoundWorkflow) {
        this.indexInCompoundWorkflow = indexInCompoundWorkflow;
    }
}
