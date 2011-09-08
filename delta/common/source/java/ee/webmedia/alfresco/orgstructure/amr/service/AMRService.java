package ee.webmedia.alfresco.orgstructure.amr.service;

import smit.ametnik.services.AmetnikExt;
import smit.ametnik.services.Yksus;

/**
 * Web service, to communicate with AmetnikeRegister
 * 
 * @author Ats Uiboupin
 */
public interface AMRService {
    String BEAN_NAME = "AmrService";

    Yksus[] getYksusByAsutusId();

    AmetnikExt[] getAmetnikByAsutusId();

    AmetnikExt getAmetnikByIsikukood(String socialSecurityNr);
}
