package ee.webmedia.alfresco.report.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;

public class FakeSendInfo extends SendInfo {

    public FakeSendInfo(Map<QName, Serializable> properties) {
        Assert.notNull(properties);
        this.properties = properties;
    }

    @Override
    public String getRecipient() {
        return (String) properties.get(DocumentCommonModel.Props.SEND_INFO_RECIPIENT);
    }

    @Override
    public Date getSendDateTime() {
        return (Date) properties.get(DocumentCommonModel.Props.SEND_INFO_SEND_DATE_TIME);
    }

    @Override
    public String getSendMode() {
        return (String) properties.get(DocumentCommonModel.Props.SEND_INFO_SEND_MODE);
    }

    @Override
    public String getSendStatus() {
        return (String) properties.get(DocumentCommonModel.Props.SEND_INFO_SEND_STATUS);
    }

}
