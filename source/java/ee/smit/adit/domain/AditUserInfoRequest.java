package ee.smit.adit.domain;

import java.util.Set;

public class AditUserInfoRequest extends AditStatusDefaultRequest {
    Set<String> userids;

    public Set<String> getUserids() {
        return userids;
    }

    public void setUserids(Set<String> userids) {
        this.userids = userids;
    }

}
