package ee.smit.adit.domain;

public class AditUserInfo {
    String userIdCode;
    boolean hasJoinded = false;
    boolean canRead = false;
    boolean canWrite = false;
    String messages;

    public String getUserIdCode() {
        return userIdCode;
    }

    public void setUserIdCode(String userIdCode) {
        this.userIdCode = userIdCode;
    }

    public boolean isHasJoinded() {
        return hasJoinded;
    }

    public void setHasJoinded(boolean hasJoinded) {
        this.hasJoinded = hasJoinded;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public void setCanRead(boolean canRead) {
        this.canRead = canRead;
    }

    public boolean isCanWrite() {
        return canWrite;
    }

    public void setCanWrite(boolean canWrite) {
        this.canWrite = canWrite;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }
}
