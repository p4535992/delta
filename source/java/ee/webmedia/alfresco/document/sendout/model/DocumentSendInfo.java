package ee.webmedia.alfresco.document.sendout.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;

public class DocumentSendInfo extends SendInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public DocumentSendInfo(Map<QName, Serializable> properties) {
        Assert.notNull(properties);
        this.properties = properties;
    }

    @Override
    public String getRecipient() {
        return (String) getProperties().get(DocumentCommonModel.Props.SEND_INFO_RECIPIENT);
    }

    @Override
    public Date getSendDateTime() {
        return (Date) getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_DATE_TIME);
    }

    @Override
    public String getSendMode() {
        return (String) getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_MODE);
    }

    @Override
    public String getSendStatus() {
        return (String) getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_STATUS);
    }

}
