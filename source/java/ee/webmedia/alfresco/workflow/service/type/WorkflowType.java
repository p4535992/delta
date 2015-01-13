package ee.webmedia.alfresco.workflow.service.type;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;

public interface WorkflowType {

    QName getWorkflowType();

    Class<? extends Workflow> getWorkflowClass();

    /**
     * May be {@code null}.
     * 
     * @return
     */
    QName getTaskType();

    /**
     * May be {@code null}.
     * 
     * @return
     */
    Class<? extends Task> getTaskClass();

    int getTaskOutcomes();

}
