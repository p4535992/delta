package ee.smit.adit;

import ee.smit.adit.domain.AditSendStatusVIResponse;

import java.util.Set;

public interface AditAdapterSearches {
    String BEAN_NAME = "AditAdapterSearches";

    Set<String> getUnregistredUsers(Set<String> userIdCodes, String userIdCode);

    AditSendStatusVIResponse getSendStatuses(Set<String> docIds, String userIdCode);

}
