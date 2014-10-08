package ee.webmedia.alfresco.document.sendout.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.web.bean.repository.Node;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.WebUtil;

<<<<<<< HEAD
/**
 * @author Erko Hansar
 */
public class DocumentSendInfo implements Serializable, SendInfo {
=======
public class DocumentSendInfo extends SendInfo implements Serializable {
>>>>>>> develop-5.1

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
<<<<<<< HEAD
=======
    public String getReceivedDateTime() {
        return getFormattedDate(DocumentCommonModel.Props.SEND_INFO_RECEIVED_DATE_TIME);
    }

    @Override
>>>>>>> develop-5.1
    public String getResolution() {
        return WebUtil.removeHtmlComments((String) node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_RESOLUTION));
    }

<<<<<<< HEAD
=======
    @Override
    public String getOpenedDateTime() {
        return getFormattedDate(DocumentCommonModel.Props.SEND_INFO_OPENED_DATE_TIME);
    }

>>>>>>> develop-5.1
}
