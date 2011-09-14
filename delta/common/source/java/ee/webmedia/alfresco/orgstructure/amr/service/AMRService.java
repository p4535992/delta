package ee.webmedia.alfresco.orgstructure.amr.service;

import smit.ametnik.services.AmetnikExt;
import smit.ametnik.services.YksusExt;

/**
 * Web service, to communicate with AmetnikeRegister
 * 
 * @author Ats Uiboupin
 */
public interface AMRService {
    String BEAN_NAME = "AmrService";

    YksusExt[] getYksusByAsutusId();

    AmetnikExt[] getAmetnikByAsutusId();

    AmetnikExt getAmetnikByIsikukood(String socialSecurityNr);
}
