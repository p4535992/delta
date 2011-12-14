package ee.webmedia.alfresco.orgstructure.amr.service;

import java.math.BigInteger;

import org.springframework.ws.client.core.WebServiceTemplate;

import smit.ametnik.services.IsikukoodByAsutusIdAndHasRsLubaRequestDocument.IsikukoodByAsutusIdAndHasRsLubaRequest;
import smit.ametnik.services.IsikukoodByAsutusIdAndHasRsLubaResponseDocument.IsikukoodByAsutusIdAndHasRsLubaResponse;
import smit.ametnik.services.RSLubaByIsikukoodRequestDocument.RSLubaByIsikukoodRequest;
import smit.ametnik.services.RSLubaByIsikukoodResponseDocument.RSLubaByIsikukoodResponse;

/**
 * @author Riina Tens
 */
public class RSServiceImpl extends WebServiceTemplate implements RSService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(RSServiceImpl.class);

    private Integer asutusId;
    private boolean restrictedDelta;
    private String restrictedDeltaName;
    private String restrictedDeltaUrl;
    private String deltaName;
    private String deltaUrl;

    @Override
    public boolean hasRsLubaByIsikukood(String idCode) {
        long startTime = System.currentTimeMillis();
        RSLubaByIsikukoodRequest request = RSLubaByIsikukoodRequest.Factory.newInstance();
        request.setIsikukood(idCode);
        request.setAsutusId(BigInteger.valueOf(asutusId));
        RSLubaByIsikukoodResponse response = (RSLubaByIsikukoodResponse) marshalSendAndReceive(request);
        if (log.isDebugEnabled()) {
            log.debug("hasRsLubaByIsikukood idCode '" + idCode + "', time " + (System.currentTimeMillis() - startTime)
                    + " ms, response:\n" + response);
        }
        return response.getRsluba();
    }

    @Override
    public String[] getIsikukoodByAsutusIdAndHasRsLubaRequest() {
        long startTime = System.currentTimeMillis();
        IsikukoodByAsutusIdAndHasRsLubaRequest request = IsikukoodByAsutusIdAndHasRsLubaRequest.Factory.newInstance();
        request.setAsutusId(BigInteger.valueOf(asutusId));
        IsikukoodByAsutusIdAndHasRsLubaResponse response = (IsikukoodByAsutusIdAndHasRsLubaResponse) marshalSendAndReceive(request);
        if (log.isDebugEnabled()) {
            log.debug("getIsikukoodByAsutusIdAndHasRsLubaRequest time " + (System.currentTimeMillis() - startTime)
                    + " ms, response:\n" + response);
        }
        return response.getIsikukoodArray();
    }

    @Override
    public boolean isRestrictedDelta() {
        return restrictedDelta;
    }

    public void setRestrictedDelta(boolean restrictedDelta) {
        this.restrictedDelta = restrictedDelta;
    }

    public void setAsutusId(Integer asutusId) {
        this.asutusId = asutusId;
    }

    public void setRestrictedDeltaName(String restrictedDeltaName) {
        this.restrictedDeltaName = restrictedDeltaName;
    }

    @Override
    public String getRestrictedDeltaName() {
        return restrictedDeltaName;
    }

    public void setRestrictedDeltaUrl(String restrictedDeltaUrl) {
        this.restrictedDeltaUrl = restrictedDeltaUrl;
    }

    @Override
    public String getRestrictedDeltaUrl() {
        return restrictedDeltaUrl;
    }

    public void setDeltaName(String deltaName) {
        this.deltaName = deltaName;
    }

    @Override
    public String getDeltaName() {
        return deltaName;
    }

    public void setDeltaUrl(String deltaUrl) {
        this.deltaUrl = deltaUrl;
    }

    @Override
    public String getDeltaUrl() {
        return deltaUrl;
    }

}
