package ee.webmedia.alfresco.workflow.service.event;

import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;

public interface WorkflowEvent {

    WorkflowEventType getType();

    BaseWorkflowObject getObject();

    Object[] getExtras();

}
