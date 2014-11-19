package ee.webmedia.alfresco.workflow.service.event;

import java.util.List;

import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
public interface WorkflowEvent {

    WorkflowEventType getType();

    BaseWorkflowObject getObject();

    List<Object> getExtras();

    Status getOriginalStatus();

}
