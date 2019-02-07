package ee.smit.adit.domain;

import java.util.Set;

public class AditDocSendStatusRequest extends AditStatusDefaultRequest {
    Set<String> docIds;

    public Set<String> getDocIds() {
        return docIds;
    }

    public void setDocIds(Set<String> docIds) {
        this.docIds = docIds;
    }

}
