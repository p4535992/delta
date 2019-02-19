package ee.smit.adit.domain;

public class DefaultResponse {
    private int statusCode;
    private String status;

    // -- GETTERS, SETTERS -------------------------------
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatus() {
        return status;
    }

}
