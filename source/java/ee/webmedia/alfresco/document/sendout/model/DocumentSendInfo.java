package ee.webmedia.alfresco.document.sendout.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;

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
    public String getSender() {
        return (String) getProperties().get(DocumentCommonModel.Props.SEND_INFO_SENDER);
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
    public String getSendModeExtended() {
    	Boolean isZipped = (Boolean)getProperties().get(DocumentCommonModel.Props.SEND_INFO_IS_ZIPPED);
    	Boolean isEncrypted = (Boolean)getProperties().get(DocumentCommonModel.Props.SEND_INFO_IS_ENCRYPTED);
    	String modeExtended = (String) getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_MODE);
    	
    	String extention = null;
    	if (isEncrypted != null && isEncrypted) {
   			extention = MessageUtil.getMessage("document_send_zipped") + ", " + MessageUtil.getMessage("document_send_encrypted");
    	} else if (isZipped != null && isZipped) {
    		extention = MessageUtil.getMessage("document_send_zipped");
    	}
    	
    	if (modeExtended != null && StringUtils.isNotBlank(extention)) {
    		modeExtended += "(" + extention + ")";
    	}
        return modeExtended;
    }
    
    @Override
    public String getSentFiles() {
        return (String) getProperties().get(DocumentCommonModel.Props.SEND_INFO_SENT_FILES);
    }

    @Override
    public String getSendStatus() {
        return (String) getProperties().get(DocumentCommonModel.Props.SEND_INFO_SEND_STATUS);
    }

}
