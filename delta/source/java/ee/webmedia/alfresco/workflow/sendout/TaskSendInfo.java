package ee.webmedia.alfresco.workflow.sendout;


import java.io.Serializable;
import java.util.Date;

import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.SendMode;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * @author Riina Tens
 */
public class TaskSendInfo implements Serializable, SendInfo {

    private static final long serialVersionUID = 1L;

    private Node node;

    public TaskSendInfo(Node node) {
        Assert.notNull(node);
        this.node = node;
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public String getRecipient() {
        return (String) node.getProperties().get(WorkflowSpecificModel.Props.INSTITUTION_NAME);
    }

    @Override
    public Date getSendDateTime() {
        return (Date) node.getProperties().get(WorkflowSpecificModel.Props.SEND_DATE_TIME);
    }

    @Override
    public String getSendMode() {
        return SendMode.DVK.getValueName();
    }

    @Override
    public String getSendStatus() {
        return (String) node.getProperties().get(WorkflowSpecificModel.Props.SEND_STATUS);
    }

    @Override
    public String getResolution() {
        return (String) node.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
    }

}
