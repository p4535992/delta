package ee.webmedia.alfresco.orgstructure.amr.service;

import java.math.BigInteger;

import smit.ametnik.services.Ametnik;
import smit.ametnik.services.Yksus;

/**
 * Web service, to communicate with AmetnikeRegister
 * 
 * @author Ats Uiboupin
 */
public interface AMRService {
    String BEAN_NAME = "AmrService";

    Yksus[] getYksusByAsutusId(BigInteger asutusId);

    Ametnik[] getAmetnikByAsutusId(BigInteger asutusId);

    Ametnik getAmetnikByIsikukood(String socialSecurityNr);
}
