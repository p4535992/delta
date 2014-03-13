package ee.webmedia.alfresco.monitoring.model;

import java.util.Date;

public class ServiceStatus {
    private String name;
    private String status = "OK";
    private long successCount = 0;
    private Date lastSuccessDateTime;
    private long errorCount = 0;
    private Date lastErrorDateTime;
    private String lastErrorMessage;

    public ServiceStatus(String name) {
        this.name = name;
    }

    public void increaseErrorCount(String errorMessage) {
        errorCount++;
        lastErrorMessage = errorMessage;
        lastErrorDateTime = new Date();
        status = "ERROR";
    }

    public void increaseSuccessCount() {
        successCount++;
        lastSuccessDateTime = new Date();
        status = "OK";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setLastSuccessDateTime(Date lastSuccessDateTime) {
        this.lastSuccessDateTime = lastSuccessDateTime;
    }

    public Date getLastSuccessDateTime() {
        return lastSuccessDateTime;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setLastErrorDateTime(Date lastErrorDateTime) {
        this.lastErrorDateTime = lastErrorDateTime;
    }

    public Date getLastErrorDateTime() {
        return lastErrorDateTime;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

}
