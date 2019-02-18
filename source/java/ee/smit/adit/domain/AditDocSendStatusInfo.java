package ee.smit.adit.domain;

import java.util.Date;

public class AditDocSendStatusInfo {
    String userCode;
    String receiverName;
    String docId;
    boolean containsOpenTime;
    Date openTime;
    boolean opened = false;
    String message;
    Integer statusCode;

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public boolean isContainsOpenTime() {
        return containsOpenTime;
    }

    public void setContainsOpenTime(boolean containsOpenTime) {
        this.containsOpenTime = containsOpenTime;
    }

    public Date getOpenTime() {
        return openTime;
    }

    public void setOpenTime(Date openTime) {
        this.openTime = openTime;
    }

    public boolean isOpened() {
        return opened;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
}
