package ee.webmedia.alfresco.log.model;

import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.LogHelper;
import ee.webmedia.alfresco.log.service.LogListItem;
import ee.webmedia.alfresco.notification.service.NotificationService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * Entity object for storing log table data.
 */
public class LogEntry implements Serializable, LogListItem {

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

    @Override
    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    @Override
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

    @Override
    public String getEventDescription() {
        return description;
    }

    public String getEventDescriptionAndLinks() {
        return WebUtil.escapeHtmlExceptLinks(WebUtil.processLinks(description));
    }

    public void setEventDescription(String eventDescription) {
        description = eventDescription;
    }

    /**
     * Log entry factory method.
     * 
     * @param object Category for object being logged.
     * @param service User service for retrieving current user data.
     * @param msgCode Resource bundle key for localizing the event message.
     * @param params Optional parameters for the localized message.
     * @return A new log entry based on given data.
     */
    public static LogEntry create(LogObject object, UserService service, String msgCode, Object... params) {
        return create(object, service, null, msgCode, params);
    }

    /**
     * Log entry factory method.
     *
     * @param object Category for object being logged.
     * @param service User service for retrieving current user data.
     * @param nodeRef Reference to the node being logged about (optional).
     * @param msgCode Resource bundle key for localizing the event message.
     * @param params Optional parameters for the localized message.
     * @return A new log entry based on given data.
     */
    public static LogEntry create(LogObject object, UserService service, NodeRef nodeRef, String msgCode, Object... params) {
        String userId = service.getCurrentUserName();
        String userName = service.getUserFullName();
        return create(object, userId, userName, nodeRef, msgCode, params);
    }

    /**
     * Log entry factory method.
     * 
     * @param object Category for object being logged.
     * @param userId Identification of the user performing the action. Defaults to "DHS".
     * @param msgCode Resource bundle key for localizing the event message.
     * @param params Optional parameters for the localized message.
     * @return A new log entry based on given data.
     */
    public static LogEntry create(LogObject object, String userId, String msgCode, Object... params) {
        return create(object, userId, null, null, msgCode, params);
    }

    /**
     * Log entry factory method.
     * 
     * @param object Category for object being logged.
     * @param userId Identification of the user performing the action. Defaults to "DHS".
     * @param userName Actual name of the user performing the action. Defaults to <code>userId</code> or "DHS".
     * @param nodeRef Reference to the node being logged about (optional).
     * @param msgCode Resource bundle key for localizing the event message.
     * @param params Optional parameters for the localized message.
     * @return A new log entry based on given data.
     */
    public static LogEntry create(LogObject object, String userId, String userName, NodeRef nodeRef, String msgCode, Object... params) {
        String desc = MessageUtil.getMessage(msgCode, params);
        return createLoc(object, userId, userName, nodeRef, desc);
    }

    public static LogEntry createWithSystemUser(LogObject object, NodeRef nodeRef, String msgCode, Object... params) {
        String desc = MessageUtil.getMessage(msgCode, params);
        return createLoc(object, null, null, nodeRef, desc);
    }

    /**
     * Log entry factory method for already localized descriptions.
     * 
     * @param object Category for object being logged.
     * @param userId Identification of the user performing the action. Defaults to "DHS".
     * @param userName Actual name of the user performing the action. Defaults to <code>userId</code> or "DHS".
     * @param nodeRef Reference to the node being logged about (optional).
     * @param desc Localized log entry description.
     * @return A new log entry based on given data.
     */
    public static LogEntry createLoc(LogObject object, String userId, String userName, NodeRef nodeRef, String desc) {
        LogEntry result = new LogEntry();
        result.creatorId = StringUtils.defaultString(userId, "DHS");
        result.creatorName = StringUtils.defaultString(userName, result.creatorId);
        result.level = object.getLevel();
        result.objectName = object.getObjectName();
        result.objectId = nodeRef != null ? nodeRef.toString() : null;
        result.description = desc + result.getSubstitutionLog();
        LogHelper.update(result);
        return result;
    }

    private String getSubstitutionLog() {
        String runAsSystemOverlay = AuthenticationUtil.getOriginalRunAsAuthenticationForSystemOverlay();
        // If code is run as system user, lets get the real user behind the action
        String runAsUser = runAsSystemOverlay != null ? runAsSystemOverlay : AuthenticationUtil.getRunAsUser();
        if (runAsUser == null || AuthenticationUtil.SYSTEM_USER_NAME.equals(runAsUser) || runAsUser.equals(getUserService().getCurrentUserName())) {
            return "";
        }
        return " " + MessageUtil.getMessage("applog_log_subtitution", getUserService().getUserFullName(runAsUser), runAsUser);
    }
}
