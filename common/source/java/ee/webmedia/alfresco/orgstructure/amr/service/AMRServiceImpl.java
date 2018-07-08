package ee.webmedia.alfresco.orgstructure.amr.service;

import org.springframework.ws.client.core.WebServiceTemplate;
import ee.webmedia.alfresco.utils.TextUtil;
import smit.ametnik.services.*;
import smit.ametnik.services.AmetnikByAsutusId2RequestDocument.AmetnikByAsutusId2Request;
import smit.ametnik.services.AmetnikByAsutusId2ResponseDocument.AmetnikByAsutusId2Response;
import smit.ametnik.services.AmetnikByIsikukood2RequestDocument.AmetnikByIsikukood2Request;
import smit.ametnik.services.AmetnikByIsikukood2ResponseDocument.AmetnikByIsikukood2Response;
import smit.ametnik.services.YksusByAsutusId2RequestDocument.YksusByAsutusId2Request;
import smit.ametnik.services.YksusByAsutusId2ResponseDocument.YksusByAsutusId2Response;

import java.math.BigInteger;

/**
 * Web service, to communicate with AmetnikeRegister
 */
public class AMRServiceImpl extends WebServiceTemplate implements AMRService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AMRServiceImpl.class);

    private BigInteger asutusId;
    private Boolean removeGroupsEmail = false;

    @Override
    public YksusExt[] getYksusByAsutusId() {
        long startTime = System.currentTimeMillis();
        YksusByAsutusId2Request request = YksusByAsutusId2RequestDocument.Factory.newInstance().addNewYksusByAsutusId2Request();
        request.setAsutusId(asutusId);
        YksusByAsutusId2ResponseDocument response = (YksusByAsutusId2ResponseDocument) marshalSendAndReceive(request);
        log.info("YksusByAsutusId2 Request asutusId '" + asutusId + "', time " + (System.currentTimeMillis() - startTime) + " ms, responseDoc:\n" + response);

        YksusByAsutusId2Response yksusByAsutusId2Response = response.getYksusByAsutusId2Response();
        return yksusByAsutusId2Response.getYksusArray();
    }

    @Override
    public AmetnikExt[] getAmetnikByAsutusId() {
        long startTime = System.currentTimeMillis();
        AmetnikByAsutusId2Request request = AmetnikByAsutusId2RequestDocument.Factory.newInstance().addNewAmetnikByAsutusId2Request();
        request.setAsutusId(asutusId);
        request.setYksusetaAmetnikudOnly(false);
        AmetnikByAsutusId2ResponseDocument responseDoc = (AmetnikByAsutusId2ResponseDocument) marshalSendAndReceive(request);
        log.info("AmetnikByAsutusId2 Request: asutusId '" + asutusId + "', time " + (System.currentTimeMillis() - startTime) + " ms, responseDoc:\n"
                    + responseDoc);

        AmetnikByAsutusId2Response response = responseDoc.getAmetnikByAsutusId2Response();
        return response.getAmetnikArray();
    }

    @Override
    public AmetnikExt getAmetnikByIsikukood(String socialSecurityNr) {
        long startTime = System.currentTimeMillis();
        AmetnikByIsikukood2Request request = AmetnikByIsikukood2RequestDocument.Factory.newInstance().addNewAmetnikByIsikukood2Request();
        request.setIsikukood(socialSecurityNr);
        request.setAsutusId(asutusId);
        AmetnikByIsikukood2ResponseDocument responseDoc = (AmetnikByIsikukood2ResponseDocument) marshalSendAndReceive(request);
        if (log.isDebugEnabled()) {
            log.debug("AmetnikByIsikukood2 Request socialSecurityNr '" + socialSecurityNr + "', time " + (System.currentTimeMillis() - startTime)
                    + " ms, responseDoc:\n" + responseDoc);
        }
        AmetnikByIsikukood2Response response = responseDoc.getAmetnikByIsikukood2Response();
        return response.getAmetnik();
    }

    // START: getters / setters

    public void setAsutusId(String asutusId) {
        this.asutusId = new BigInteger(asutusId);
    }

    public void setRemoveGroupsEmail(boolean removeGroupsEmail){
        this.removeGroupsEmail = removeGroupsEmail;
        log.info("---------------------------------------------------------------------------");
        log.info("setRemoveGroupsEmail(): value = " + this.removeGroupsEmail);
        log.info("---------------------------------------------------------------------------");
    }

    public boolean getRemoveGroupsEmail(){
        log.info("---------------------------------------------------------------------------");
        log.info("getRemoveGroupsEmail config value: " + this.removeGroupsEmail);
        log.info("---------------------------------------------------------------------------");
        return  this.removeGroupsEmail;
    }

    // END: getters / setters
}
