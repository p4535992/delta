<<<<<<< HEAD
package ee.webmedia.alfresco.orgstructure.amr.service;

/**
 * @author Riina Tens
 */
public interface RSService {

    String BEAN_NAME = "RsService";

    boolean isRestrictedDelta();

    boolean hasRsLubaByIsikukood(String idCode);

    String[] getIsikukoodByAsutusIdAndHasRsLubaRequest();

    String getDeltaUrl();

    String getDeltaName();

    String getRestrictedDeltaUrl();

    String getRestrictedDeltaName();

}
=======
package ee.webmedia.alfresco.orgstructure.amr.service;

public interface RSService {

    String BEAN_NAME = "RsService";

    boolean isRestrictedDelta();

    boolean hasRsLubaByIsikukood(String idCode);

    String[] getIsikukoodByAsutusIdAndHasRsLubaRequest();

    String getDeltaUrl();

    String getDeltaName();

    String getRestrictedDeltaUrl();

    String getRestrictedDeltaName();

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
