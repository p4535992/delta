package ee.smit.adit.domain;

import java.util.List;

public class AditUserInfoResponse extends DefaultResponse {
    List<AditUserInfo> activeUserList;
    List<AditUserInfo> unreqistredUserList;
    String message;

    public List<AditUserInfo> getActiveUserList() {
        return activeUserList;
    }

    public void setActiveUserList(List<AditUserInfo> activeUserList) {
        this.activeUserList = activeUserList;
    }

    public List<AditUserInfo> getUnreqistredUserList() {
        return unreqistredUserList;
    }

    public void setUnreqistredUserList(List<AditUserInfo> unreqistredUserList) {
        this.unreqistredUserList = unreqistredUserList;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
