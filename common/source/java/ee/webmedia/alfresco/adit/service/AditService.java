package ee.webmedia.alfresco.adit.service;

import java.util.Set;

import ee.webmedia.xtee.client.exception.XTeeServiceConsumptionException;

public interface AditService {

    String BEAN_NAME = "AditService";
    String NAME = "adit";

    int updateAditDocViewedStatuses();

    Set<String> getUnregisteredAditUsers(Set<String> userIdCodes) throws XTeeServiceConsumptionException;

}
