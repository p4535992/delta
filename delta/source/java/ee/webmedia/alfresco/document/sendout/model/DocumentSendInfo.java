package ee.webmedia.alfresco.document.sendout.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * @author Erko Hansar
 */
public class DocumentSendInfo implements Serializable, SendInfo {

    private static final long serialVersionUID = 1L;

    private final Node node;

    public DocumentSendInfo(Node node) {
        Assert.notNull(node);
        this.node = node;
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public String getRecipient() {
        return (String) node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_RECIPIENT);
    }

    @Override
    public Date getSendDateTime() {
        return (Date) node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_DATE_TIME);
    }

    @Override
    public String getSendMode() {
        return (String) node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_MODE);
    }

    @Override
    public String getSendStatus() {
        return (String) node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_STATUS);
    }

    @Override
    public String getResolution() {
        return (String) node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_RESOLUTION);
    }

}
