package ee.webmedia.alfresco.workflow.service.type;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.LinkedReviewTask;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Class for linked review task type which has no associated workflow type.
 * 
 * @author Riina Tens
 */
public class LinkedReviewTaskType implements WorkflowType, InitializingBean {

    private QName taskType = WorkflowCommonModel.Types.TASK;
    private final Class<? extends Task> taskClass = LinkedReviewTask.class;
    private int taskOutcomes = 0;

    protected WorkflowService workflowService;
    private NamespaceService namespaceService;

    @Override
    public void afterPropertiesSet() throws Exception {
        workflowService.registerWorkflowType(this);
    }

    @Override
    public QName getWorkflowType() {
        return null;
    }

    @Override
    public Class<? extends Workflow> getWorkflowClass() {
        return null;
    }

    @Override
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

    @Override
    public Class<? extends Task> getTaskClass() {
        return taskClass;
    }

    @Override
    public int getTaskOutcomes() {
        return taskOutcomes;
    }

    public void setTaskOutcomes(int taskOutcomes) {
        Assert.isTrue(taskOutcomes >= 0);
        this.taskOutcomes = taskOutcomes;
    }

    @Override
    public String toString() {
        return WmNode.toString(this) + "[\n  workflowType=null"
                + "\n  workflowClass=null"
                + "\n  taskType=" + taskType
                + "\n  taskClass=" + taskClass
                + "\n  taskOutcomes=" + taskOutcomes
                + "\n]";
    }

    @Override
    public boolean isIndependentTaskType() {
        return true;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

}
