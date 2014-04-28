package ee.webmedia.alfresco.document.sendout.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.MessageUtil;

public abstract class SendInfo {

    public static final String SENT = "saadetud";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public abstract Node getNode();

    public abstract String getRecipient();

    public abstract Date getSendDateTime();

    public abstract String getSendMode();

    public abstract String getSendStatus();

    public abstract String getResolution();

    public abstract String getReceivedDateTime();

    public abstract String getOpenedDateTime();

    public String getSendStatusWithReceivedDateTime() {
        String receivedDateTime = getReceivedDateTime();
        return getSendStatus() + (StringUtils.isBlank(receivedDateTime) ? "" : " " + MessageUtil.getMessage("document_send_received_date_time", receivedDateTime));
    }

    protected String getFormattedDate(QName dateProp) {
        Date date = (Date) getNode().getProperties().get(dateProp);
        if (date == null) {
            return "";
        }
        return dateFormat.format(date);
    }

}