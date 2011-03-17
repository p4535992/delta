package ee.webmedia.alfresco.document.sendout.model;

import java.io.Serializable;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * @author Erko Hansar
 */
public class SendInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Node node;
    
    public SendInfo() {
    }

    public SendInfo(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Object getRecipient() {
        return node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_RECIPIENT);
    }

    public Object getSendDateTime() {
        return node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_DATE_TIME);
    }

    public Object getSendMode() {
        return node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_MODE);
    }

    public Object getSendStatus() {
        return node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_STATUS);
    }

    public Object getResolution() {
        return node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_RESOLUTION);
    }

}
