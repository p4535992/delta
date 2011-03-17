package ee.webmedia.alfresco.workflow.service.event;

import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;

/**
 * @author Alar Kvell
 */
public interface WorkflowEvent {

    WorkflowEventType getType();

    BaseWorkflowObject getObject();
    
    Object[] getExtras();

}
