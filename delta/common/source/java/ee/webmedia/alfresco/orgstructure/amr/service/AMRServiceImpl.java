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
import org.springframework.beans.factory.InitializingBean;

/**
 * Web service, to communicate with AmetnikeRegister
 * 
 * @author Ats Uiboupin
 */
public class AMRServiceImpl extends WebServiceTemplate implements AMRService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AMRServiceImpl.class);

    private String defaultAmrOrgId;
    private BigInteger asutusId;

    @Override
    public Yksus[] getYksusByAsutusId() {
        long startTime = System.currentTimeMillis();
        YksusByAsutusIdRequest request = YksusByAsutusIdRequestDocument.Factory.newInstance().addNewYksusByAsutusIdRequest();
        request.setAsutusId(asutusId);
        YksusByAsutusIdResponseDocument response = (YksusByAsutusIdResponseDocument) marshalSendAndReceive(request);
        if (log.isDebugEnabled()) {
            log.debug("getYksusByAsutusId asutusId '" + asutusId + "', time " + (System.currentTimeMillis() - startTime) + " ms, responseDoc:\n" + response);
        }
        YksusByAsutusIdResponse yksusByAsutusIdResponse = response.getYksusByAsutusIdResponse();
        return yksusByAsutusIdResponse.getYksusArray();
    }

    @Override
    public Ametnik[] getAmetnikByAsutusId() {
        long startTime = System.currentTimeMillis();
        AmetnikByAsutusIdRequest request = AmetnikByAsutusIdRequestDocument.Factory.newInstance().addNewAmetnikByAsutusIdRequest();
        request.setAsutusId(asutusId);
        request.setYksusetaAmetnikudOnly(false);
        AmetnikByAsutusIdResponseDocument responseDoc = (AmetnikByAsutusIdResponseDocument) marshalSendAndReceive(request);
        if (log.isDebugEnabled()) {
            log.debug("getAmetnikByAsutusId asutusId '" + asutusId + "', time " + (System.currentTimeMillis() - startTime) + " ms, responseDoc:\n"
                    + responseDoc);
        }
        AmetnikByAsutusIdResponse response = responseDoc.getAmetnikByAsutusIdResponse();
        return response.getAmetnikArray();
    }

    @Override
    public Ametnik getAmetnikByIsikukood(String socialSecurityNr) {
        long startTime = System.currentTimeMillis();
        AmetnikByIsikukoodRequest request = AmetnikByIsikukoodRequestDocument.Factory.newInstance().addNewAmetnikByIsikukoodRequest();
        request.setIsikukood(socialSecurityNr);
        request.setAsutusId(asutusId);
        AmetnikByIsikukoodResponseDocument responseDoc = (AmetnikByIsikukoodResponseDocument) marshalSendAndReceive(request);
        if (log.isDebugEnabled()) {
            log.debug("getAmetnikByIsikukood socialSecurityNr '" + socialSecurityNr + "', time " + (System.currentTimeMillis() - startTime)
                    + " ms, responseDoc:\n" + responseDoc);
        }
        AmetnikByIsikukoodResponse response = responseDoc.getAmetnikByIsikukoodResponse();
        return response.getAmetnik();
    }

    public void setDefaultAmrOrgId(String defaultAmrOrgId) {
        this.defaultAmrOrgId = defaultAmrOrgId;
        this.asutusId = BigInteger.valueOf(Integer.parseInt(defaultAmrOrgId.trim()));
    }
}
