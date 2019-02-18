package ee.smit.adit.domain;

public class AditStatusDefaultRequest {
    String userIdCode; // Request maker ID-code
    String systemId; // Request maker reg.code

    public String getUserIdCode() {
        return userIdCode;
    }

    public void setUserIdCode(String userIdCode) {
        this.userIdCode = userIdCode;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }
}
