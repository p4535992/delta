package ee.webmedia.alfresco.workflow.service.event;

import java.util.List;

import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public interface WorkflowEvent {

    WorkflowEventType getType();

    BaseWorkflowObject getObject();

    List<Object> getExtras();

    Status getOriginalStatus();

}
