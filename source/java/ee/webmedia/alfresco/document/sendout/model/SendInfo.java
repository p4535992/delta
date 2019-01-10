package ee.webmedia.alfresco.document.sendout.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

public abstract class SendInfo {

    public static final String SENT = "saadetud";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    protected Map<QName, Serializable> properties;
    protected String receivedDateTimeStr;
    protected String openedDateTimeStr;
    protected String formattedResolution;

    public Map<QName, Serializable> getProperties() {
        return properties;
    }

    public abstract String getRecipient();
    
    public abstract String getSender();

    public abstract Date getSendDateTime();

    public abstract String getSendMode();

    public abstract String getSendModeExtended();

    public abstract String getSentFiles();
    
    public abstract String getSendStatus();

    public String getReceivedDateTime() {
        if (receivedDateTimeStr == null) {
            receivedDateTimeStr = getFormattedDate(DocumentCommonModel.Props.SEND_INFO_RECEIVED_DATE_TIME);
            if (receivedDateTimeStr == null) {
                receivedDateTimeStr = "";
            }
        }
        return receivedDateTimeStr;
    }

    public String getResolution() {
        if (formattedResolution == null) {
            formattedResolution = WebUtil.removeHtmlComments((String) getProperties().get(DocumentCommonModel.Props.SEND_INFO_RESOLUTION));
            if (formattedResolution == null) {
                formattedResolution = "";
            }
        }
        return formattedResolution;
    }

    public String getOpenedDateTime() {
        if (openedDateTimeStr == null) {
            openedDateTimeStr = getFormattedDate(DocumentCommonModel.Props.SEND_INFO_OPENED_DATE_TIME);
            if (openedDateTimeStr == null) {
                openedDateTimeStr = "";
            }
        }
        return openedDateTimeStr;
    }

    public String getSendStatusWithReceivedDateTime() {
        String receivedDateTime = getReceivedDateTime();
        return getSendStatus() + (StringUtils.isBlank(receivedDateTime) ? "" : " " + MessageUtil.getMessage("document_send_received_date_time", receivedDateTime));
    }

    protected String getFormattedDate(QName dateProp) {
        Date date = (Date) getProperties().get(dateProp);
        if (date == null) {
            return "";
        }
        return dateFormat.format(date);
    }

}