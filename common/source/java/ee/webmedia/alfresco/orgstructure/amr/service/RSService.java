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
