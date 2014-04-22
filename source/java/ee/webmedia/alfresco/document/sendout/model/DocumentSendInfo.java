package ee.webmedia.alfresco.document.sendout.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public class DocumentSendInfo implements Serializable, SendInfo {

    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

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
        return (String) node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_STATUS) + getReceivedDateTime();
    }

    private String getReceivedDateTime() {
        String received = getFormattedDate(DocumentCommonModel.Props.SEND_INFO_RECEIVED_DATE_TIME);
        return StringUtils.isBlank(received) ? "" : " " + MessageUtil.getMessage("document_send_received_date_time", received);
    }

    @Override
    public String getResolution() {
        return WebUtil.removeHtmlComments((String) node.getProperties().get(DocumentCommonModel.Props.SEND_INFO_RESOLUTION));
    }

    @Override
    public String getOpenedDateTime() {
        return getFormattedDate(DocumentCommonModel.Props.SEND_INFO_OPENED_DATE_TIME);
    }

    private String getFormattedDate(QName dateProp) {
        Date date = (Date) node.getProperties().get(dateProp);
        if (date == null) {
            return "";
        }
        return dateFormat.format(date);
    }

}
