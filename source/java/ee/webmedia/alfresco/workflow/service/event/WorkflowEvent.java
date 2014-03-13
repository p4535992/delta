package ee.webmedia.alfresco.workflow.service.event;

import java.util.List;

import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;

public interface WorkflowEvent {

    WorkflowEventType getType();

    BaseWorkflowObject getObject();

    List<Object> getExtras();

    Status getOriginalStatus();

}
