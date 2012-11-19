package ee.webmedia.alfresco.log.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO used for gathering system log filtering data.
 * 
 * @author Martti Tamm
 */
public class LogFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    private String logEntryId;

    private Date dateCreatedStart;

    private Date dateCreatedEnd;

    private String creatorName;

    private String computerId;

    private String description;

    private Set<String> excludedDescriptions;

    private String objectName;

    private String objectId;
    private boolean isExactObjectId;

    public String getLogEntryId() {
        return logEntryId;
    }

    public void setLogEntryId(String logEntryId) {
        this.logEntryId = logEntryId;
    }

    public Date getDateCreatedStart() {
        return dateCreatedStart;
    }

    public void setDateCreatedStart(Date dateCreatedStart) {
        this.dateCreatedStart = dateCreatedStart;
    }

    public Date getDateCreatedEnd() {
        return dateCreatedEnd;
    }

    public void setDateCreatedEnd(Date dateCreatedEnd) {
        this.dateCreatedEnd = dateCreatedEnd;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getComputerId() {
        return computerId;
    }

    public void setComputerId(String computerId) {
        this.computerId = computerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public void setExcludedDescriptions(Set<String> excludedDescriptions) {
        this.excludedDescriptions = excludedDescriptions;
    }

    public Set<String> getExcludedDescriptions() {
        if (excludedDescriptions == null) {
            excludedDescriptions = new HashSet<String>();
        }
        return excludedDescriptions;
    }

    public boolean isExactObjectId() {
        return isExactObjectId;
    }

    public void setExactObjectId(boolean isExactObjectId) {
        this.isExactObjectId = isExactObjectId;
    }
}
