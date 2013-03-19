package ee.webmedia.alfresco.orgstructure.amr.service;

import java.math.BigInteger;

import org.springframework.ws.client.core.WebServiceTemplate;

import smit.ametnik.services.AmetnikByAsutusId2RequestDocument;
import smit.ametnik.services.AmetnikByAsutusId2RequestDocument.AmetnikByAsutusId2Request;
import smit.ametnik.services.AmetnikByAsutusId2ResponseDocument;
import smit.ametnik.services.AmetnikByAsutusId2ResponseDocument.AmetnikByAsutusId2Response;
import smit.ametnik.services.AmetnikByIsikukood2RequestDocument;
import smit.ametnik.services.AmetnikByIsikukood2RequestDocument.AmetnikByIsikukood2Request;
import smit.ametnik.services.AmetnikByIsikukood2ResponseDocument;
import smit.ametnik.services.AmetnikByIsikukood2ResponseDocument.AmetnikByIsikukood2Response;
import smit.ametnik.services.AmetnikExt;
import smit.ametnik.services.YksusByAsutusId2RequestDocument;
import smit.ametnik.services.YksusByAsutusId2RequestDocument.YksusByAsutusId2Request;
import smit.ametnik.services.YksusByAsutusId2ResponseDocument;
import smit.ametnik.services.YksusByAsutusId2ResponseDocument.YksusByAsutusId2Response;
import smit.ametnik.services.YksusExt;

/**
 * Web service, to communicate with AmetnikeRegister
 * 
 * @author Ats Uiboupin
 */
public class AMRServiceImpl extends WebServiceTemplate implements AMRService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AMRServiceImpl.class);

    private BigInteger asutusId;

    @Override
    public YksusExt[] getYksusByAsutusId() {
        long startTime = System.currentTimeMillis();
        YksusByAsutusId2Request request = YksusByAsutusId2RequestDocument.Factory.newInstance().addNewYksusByAsutusId2Request();
        request.setAsutusId(asutusId);
        YksusByAsutusId2ResponseDocument response = (YksusByAsutusId2ResponseDocument) marshalSendAndReceive(request);
        if (log.isDebugEnabled()) {
            log.debug("getYksusByAsutusId asutusId '" + asutusId + "', time " + (System.currentTimeMillis() - startTime) + " ms, responseDoc:\n" + response);
        }
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
        if (log.isDebugEnabled()) {
            log.debug("getAmetnikByAsutusId asutusId '" + asutusId + "', time " + (System.currentTimeMillis() - startTime) + " ms, responseDoc:\n"
                    + responseDoc);
        }
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
            log.debug("getAmetnikByIsikukood socialSecurityNr '" + socialSecurityNr + "', time " + (System.currentTimeMillis() - startTime)
                    + " ms, responseDoc:\n" + responseDoc);
        }
        AmetnikByIsikukood2Response response = responseDoc.getAmetnikByIsikukood2Response();
        return response.getAmetnik();
    }

    public void setAsutusId(String asutusId) {
        this.asutusId = new BigInteger(asutusId);
    }

}
