package ee.webmedia.alfresco.workflow.service.type;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.Workflow;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
    boolean isIndependentTaskType();

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
