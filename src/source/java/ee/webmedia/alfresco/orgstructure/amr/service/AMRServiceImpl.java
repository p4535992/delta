package ee.webmedia.alfresco.orgstructure.amr.service;

import java.math.BigInteger;

import org.springframework.ws.client.core.WebServiceTemplate;

import smit.ametnik.services.Ametnik;
import smit.ametnik.services.AmetnikByAsutusIdRequestDocument;
import smit.ametnik.services.AmetnikByAsutusIdResponseDocument;
import smit.ametnik.services.AmetnikByIsikukoodRequestDocument;
import smit.ametnik.services.AmetnikByIsikukoodResponseDocument;
import smit.ametnik.services.Yksus;
import smit.ametnik.services.YksusByAsutusIdRequestDocument;
import smit.ametnik.services.YksusByAsutusIdResponseDocument;
import smit.ametnik.services.AmetnikByAsutusIdRequestDocument.AmetnikByAsutusIdRequest;
import smit.ametnik.services.AmetnikByAsutusIdResponseDocument.AmetnikByAsutusIdResponse;
import smit.ametnik.services.AmetnikByIsikukoodRequestDocument.AmetnikByIsikukoodRequest;
import smit.ametnik.services.AmetnikByIsikukoodResponseDocument.AmetnikByIsikukoodResponse;
import smit.ametnik.services.YksusByAsutusIdRequestDocument.YksusByAsutusIdRequest;
import smit.ametnik.services.YksusByAsutusIdResponseDocument.YksusByAsutusIdResponse;

/**
 * Web service, to communicate with AmetnikeRegister
 * 
 * @author Ats Uiboupin
 */
public class AMRServiceImpl extends WebServiceTemplate implements AMRService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AMRServiceImpl.class);

    @Override
    public Yksus[] getYksusByAsutusId(BigInteger asutusId) {
        YksusByAsutusIdRequest request = YksusByAsutusIdRequestDocument.Factory.newInstance().addNewYksusByAsutusIdRequest();
        request.setAsutusId(asutusId);

        YksusByAsutusIdResponseDocument response = (YksusByAsutusIdResponseDocument) marshalSendAndReceive(request);
        if(log.isDebugEnabled()) {
            log.debug("getYksusByAsutusId:\n" + response);
        }
        YksusByAsutusIdResponse yksusByAsutusIdResponse = response.getYksusByAsutusIdResponse();
        return yksusByAsutusIdResponse.getYksusArray();
    }

    @Override
    public Ametnik[] getAmetnikByAsutusId(BigInteger asutusId) {
        AmetnikByAsutusIdRequest request = AmetnikByAsutusIdRequestDocument.Factory.newInstance().addNewAmetnikByAsutusIdRequest();
        request.setAsutusId(asutusId);
        request.setYksusetaAmetnikudOnly(false);
        AmetnikByAsutusIdResponseDocument responseDoc = (AmetnikByAsutusIdResponseDocument) marshalSendAndReceive(request);
        if(log.isDebugEnabled()) {
            log.debug("getAmetnikByAsutusId responseDoc:\n" + responseDoc);
        }
        AmetnikByAsutusIdResponse response = responseDoc.getAmetnikByAsutusIdResponse();
        return response.getAmetnikArray();
    }

    @Override
    public Ametnik getAmetnikByIsikukood(String socialSecurityNr) {
        AmetnikByIsikukoodRequest request = AmetnikByIsikukoodRequestDocument.Factory.newInstance().addNewAmetnikByIsikukoodRequest();
        request.setIsikukood(socialSecurityNr);
        AmetnikByIsikukoodResponseDocument responseDoc = (AmetnikByIsikukoodResponseDocument) marshalSendAndReceive(request);
        if(log.isDebugEnabled()) {
            log.debug("getAmetnikByIsikukood responseDoc:\n" + responseDoc);
        }
        AmetnikByIsikukoodResponse response = responseDoc.getAmetnikByIsikukoodResponse();
        return response.getAmetnik();
    }

}