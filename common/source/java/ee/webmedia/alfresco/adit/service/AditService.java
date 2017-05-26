package ee.webmedia.alfresco.adit.service;

import java.util.Set;

import com.nortal.jroad.client.exception.XRoadServiceConsumptionException;

public interface AditService {

    String BEAN_NAME = "AditService";
    String NAME = "adit";

    int updateAditDocViewedStatuses();

    Set<String> getUnregisteredAditUsers(Set<String> userIdCodes) throws XRoadServiceConsumptionException;

}
