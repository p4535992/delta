<<<<<<< HEAD
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
=======
package ee.webmedia.alfresco.orgstructure.amr.service;

import smit.ametnik.services.AmetnikExt;
import smit.ametnik.services.YksusExt;

/**
 * Web service, to communicate with AmetnikeRegister
 */
public interface AMRService {
    String BEAN_NAME = "AmrService";

    YksusExt[] getYksusByAsutusId();

    AmetnikExt[] getAmetnikByAsutusId();

    AmetnikExt getAmetnikByIsikukood(String socialSecurityNr);
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
