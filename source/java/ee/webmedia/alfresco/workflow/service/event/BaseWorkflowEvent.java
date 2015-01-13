package ee.webmedia.alfresco.workflow.service.event;

import static java.util.Arrays.asList;

import java.util.List;

import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.service.BaseWorkflowObject;

public class BaseWorkflowEvent implements WorkflowEvent {

    private final WorkflowEventType type;
    private final BaseWorkflowObject object;
    private final List<Object> extras;

    public BaseWorkflowEvent(WorkflowEventType type, BaseWorkflowObject object, Object... extras) {
        Assert.notNull(type);
        Assert.notNull(object);
        this.type = type;
        this.object = object;
        this.extras = asList(extras);
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
    public List<Object> getExtras() {
        return extras;
    }

    @Override
    public Status getOriginalStatus() {
        if (extras == null) {
            return null;
        }
        for (Object extra : extras) {
            if (extra instanceof Status) {
                return (Status) extra;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return WmNode.toString(this) + "[\n  type=" + type + "\n  object=" + object + "\n]";
    }

}
