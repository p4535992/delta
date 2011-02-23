package ee.webmedia.alfresco.workflow.service.type;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * @author Alar Kvell
 */
public class BaseWorkflowType implements WorkflowType, InitializingBean {

    private QName workflowType = WorkflowCommonModel.Types.WORKFLOW;
    private Class<? extends Workflow> workflowClass = Workflow.class;
    private QName taskType = WorkflowCommonModel.Types.TASK;
    private Class<? extends Task> taskClass = Task.class;
    private int taskOutcomes = 0;

    private WorkflowService workflowService;
    private NamespaceService namespaceService;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerWorkflowType(this);
    }

    public QName getWorkflowType() {
        return workflowType;
    }

    public void setWorkflowType(QName workflowType) {
        Assert.notNull(workflowType);
        this.workflowType = workflowType;
    }

    public void setWorkflowTypeString(String workflowType) {
        Assert.notNull(workflowType);
        this.workflowType = QName.resolveToQName(namespaceService, workflowType);
    }

    public Class<? extends Workflow> getWorkflowClass() {
        return workflowClass;
    }

    public void setWorkflowClass(Class<? extends Workflow> workflowClass) {
        Assert.notNull(workflowClass);
        this.workflowClass = workflowClass;
    }

    public QName getTaskType() {
        return taskType;
    }

    public void setTaskType(QName taskType) {
        this.taskType = taskType;
    }

    public void setTaskTypeString(String taskType) {
        if (taskType == null || taskType.isEmpty()) {
            this.taskType = null;
        } else {
            this.taskType = QName.resolveToQName(namespaceService, taskType);
        }
    }

    public Class<? extends Task> getTaskClass() {
        return taskClass;
    }

    public void setTaskClass(Class<? extends Task> taskClass) {
        this.taskClass = taskClass;
    }

    public int getTaskOutcomes() {
        return taskOutcomes;
    }

    public void setTaskOutcomes(int taskOutcomes) {
        Assert.isTrue(taskOutcomes >= 0);
        this.taskOutcomes = taskOutcomes;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    @Override
    public String toString() {
        return WmNode.toString(this) + "[\n  workflowType=" + workflowType
                + "\n  workflowClass=" + workflowClass
                + "\n  taskType=" + taskType
                + "\n  taskClass=" + taskClass
                + "\n  taskOutcomes=" + taskOutcomes
                + "\n]";
    }

}
