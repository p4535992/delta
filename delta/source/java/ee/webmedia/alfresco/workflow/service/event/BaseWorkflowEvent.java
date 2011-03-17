package ee.webmedia.alfresco.workflow.service.event;

import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;

/**
 * @author Alar Kvell
 */
public class BaseWorkflowEvent implements WorkflowEvent {

    private WorkflowEventType type;
    private BaseWorkflowObject object;
    private Object[] extras;

    public BaseWorkflowEvent(WorkflowEventType type, BaseWorkflowObject object, Object ... extras) {
        Assert.notNull(type);
        Assert.notNull(object);
        this.type = type;
        this.object = object;
        this.extras = extras;
    }

    @Override
    public WorkflowEventType getType() {
        return type;
    }

    @Override
    public BaseWorkflowObject getObject() {
        return object;
    }

    @Override
    public Object[] getExtras() {
        return extras;
    }

    @Override
    public String toString() {
        return WmNode.toString(this) + "[\n  type=" + type + "\n  object=" + object + "\n]";
    }

}