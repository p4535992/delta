<<<<<<< HEAD
package ee.webmedia.alfresco.orgstructure.amr.service;

import java.math.BigInteger;

import org.springframework.ws.client.core.WebServiceTemplate;

import smit.ametnik.services.IsikukoodByAsutusIdAndHasRsLubaRequestDocument;
import smit.ametnik.services.IsikukoodByAsutusIdAndHasRsLubaRequestDocument.IsikukoodByAsutusIdAndHasRsLubaRequest;
import smit.ametnik.services.IsikukoodByAsutusIdAndHasRsLubaResponseDocument;
import smit.ametnik.services.RSLubaByIsikukoodRequestDocument;
import smit.ametnik.services.RSLubaByIsikukoodRequestDocument.RSLubaByIsikukoodRequest;
import smit.ametnik.services.RSLubaByIsikukoodResponseDocument;

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
        RSLubaByIsikukoodRequest request = RSLubaByIsikukoodRequestDocument.Factory.newInstance().addNewRSLubaByIsikukoodRequest();
        request.setIsikukood(idCode);
        request.setAsutusId(BigInteger.valueOf(asutusId));
        RSLubaByIsikukoodResponseDocument response = (RSLubaByIsikukoodResponseDocument) marshalSendAndReceive(request);
        if (log.isDebugEnabled()) {
            log.debug("hasRsLubaByIsikukood idCode '" + idCode + "', time " + (System.currentTimeMillis() - startTime)
                    + " ms, response:\n" + response);
        }
        return response.getRSLubaByIsikukoodResponse().getRsluba();
    }

    @Override
    public String[] getIsikukoodByAsutusIdAndHasRsLubaRequest() {
        long startTime = System.currentTimeMillis();
        IsikukoodByAsutusIdAndHasRsLubaRequest request = IsikukoodByAsutusIdAndHasRsLubaRequestDocument.Factory.newInstance().addNewIsikukoodByAsutusIdAndHasRsLubaRequest();
        request.setAsutusId(BigInteger.valueOf(asutusId));
        IsikukoodByAsutusIdAndHasRsLubaResponseDocument response = (IsikukoodByAsutusIdAndHasRsLubaResponseDocument) marshalSendAndReceive(request);
        if (log.isDebugEnabled()) {
            log.debug("getIsikukoodByAsutusIdAndHasRsLubaRequest time " + (System.currentTimeMillis() - startTime)
                    + " ms, response:\n" + response);
        }
        return response.getIsikukoodByAsutusIdAndHasRsLubaResponse().getIsikukoodArray();
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
=======
package ee.webmedia.alfresco.orgstructure.amr.service;

import java.math.BigInteger;

import org.springframework.ws.client.core.WebServiceTemplate;

import smit.ametnik.services.IsikukoodByAsutusIdAndHasRsLubaRequestDocument;
import smit.ametnik.services.IsikukoodByAsutusIdAndHasRsLubaRequestDocument.IsikukoodByAsutusIdAndHasRsLubaRequest;
import smit.ametnik.services.IsikukoodByAsutusIdAndHasRsLubaResponseDocument;
import smit.ametnik.services.RSLubaByIsikukoodRequestDocument;
import smit.ametnik.services.RSLubaByIsikukoodRequestDocument.RSLubaByIsikukoodRequest;
import smit.ametnik.services.RSLubaByIsikukoodResponseDocument;

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
        RSLubaByIsikukoodRequest request = RSLubaByIsikukoodRequestDocument.Factory.newInstance().addNewRSLubaByIsikukoodRequest();
        request.setIsikukood(idCode);
        request.setAsutusId(BigInteger.valueOf(asutusId));
        RSLubaByIsikukoodResponseDocument response = (RSLubaByIsikukoodResponseDocument) marshalSendAndReceive(request);
        if (log.isDebugEnabled()) {
            log.debug("hasRsLubaByIsikukood idCode '" + idCode + "', time " + (System.currentTimeMillis() - startTime)
                    + " ms, response:\n" + response);
        }
        return response.getRSLubaByIsikukoodResponse().getRsluba();
    }

    @Override
    public String[] getIsikukoodByAsutusIdAndHasRsLubaRequest() {
        long startTime = System.currentTimeMillis();
        IsikukoodByAsutusIdAndHasRsLubaRequest request = IsikukoodByAsutusIdAndHasRsLubaRequestDocument.Factory.newInstance().addNewIsikukoodByAsutusIdAndHasRsLubaRequest();
        request.setAsutusId(BigInteger.valueOf(asutusId));
        IsikukoodByAsutusIdAndHasRsLubaResponseDocument response = (IsikukoodByAsutusIdAndHasRsLubaResponseDocument) marshalSendAndReceive(request);
        if (log.isDebugEnabled()) {
            log.debug("getIsikukoodByAsutusIdAndHasRsLubaRequest time " + (System.currentTimeMillis() - startTime)
                    + " ms, response:\n" + response);
        }
        return response.getIsikukoodByAsutusIdAndHasRsLubaResponse().getIsikukoodArray();
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
>>>>>>> develop-5.1
