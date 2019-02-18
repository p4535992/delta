package ee.smit.adit.domain;

import java.util.List;
import java.util.Map;

public class AditSendStatusVIResponse extends DefaultResponse {
    Map<String, List<AditDocSendStatusInfo>> documents;

    public void setDocuments(Map<String, List<AditDocSendStatusInfo>> documents) {
        this.documents = documents;
    }

    public Map<String, List<AditDocSendStatusInfo>> getDocuments() {
        return documents;
    }

}
