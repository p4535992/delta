package ee.webmedia.alfresco.log.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.log.LogHelper;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Entity object for storing log table data.
 * 
 * @author Martti Tamm
 */
public class LogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String logEntryId;

    private String level;

    private Date createdDateTime;

    private String creatorName;

    private String creatorId;

    private String computerIp;

    private String computerName;

    private String objectId;

    private String objectName;

    private String description;

    public LogEntry() {
    }

    public String getLogEntryId() {
        return logEntryId;
    }

    public void setLogEntryId(String logEntryId) {
        this.logEntryId = logEntryId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getComputerIp() {
        return computerIp;
    }

    public void setComputerIp(String computerIp) {
        this.computerIp = computerIp;
    }

    public String getComputerName() {
        return computerName;
    }

    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getEventDescription() {
        return description;
    }

    public void setEventDescription(String eventDescription) {
        description = eventDescription;
    }

    public static LogEntry create(LogObject object, UserService service, String msgCode, Object... params) {
        return create(object, service, null, msgCode, params);
    }

    public static LogEntry create(LogObject object, UserService service, NodeRef nodeRef, String msgCode, Object... params) {
        String userId = service.getCurrentUserName();
        String userName = service.getUserFullName();
        return create(object, userId, userName, nodeRef, msgCode, params);
    }

    public static LogEntry create(LogObject object, String userId, String msgCode, Object... params) {
        return create(object, userId, null, null, msgCode, params);
    }

    public static LogEntry create(LogObject object, String userId, String userName, NodeRef nodeRef, String msgCode, Object... params) {
        String desc = I18NUtil.getMessage(msgCode, params);
        return createLoc(object, userId, userName, nodeRef, desc);
    }

    public static LogEntry createLoc(LogObject object, String userId, String userName, NodeRef nodeRef, String desc) {
        LogEntry result = new LogEntry();
        result.creatorId = StringUtils.defaultString(userId, "DHS");
        result.creatorName = StringUtils.defaultString(userName, userId);
        result.level = object.getLevel();
        result.objectName = object.getObjectName();
        result.objectId = nodeRef != null ? nodeRef.toString() : null;
        result.description = desc;
        LogHelper.update(result);
        return result;
    }
}
