package ee.webmedia.alfresco.log.model;

import java.util.Date;
import java.util.List;

public class LoggedNotification {
	private Date notificationDate;
	private List<LoggedNotificatedUser> notificatedUsers;

	public Date getNotificationDate() {
		return notificationDate;
	}

	public void setNotificationDate(Date notificationDate) {
		this.notificationDate = notificationDate;
	}

	public List<LoggedNotificatedUser> getNotificatedUsers() {
		return notificatedUsers;
	}

	public void setNotificatedUsers(List<LoggedNotificatedUser> notificatedUsers) {
		this.notificatedUsers = notificatedUsers;
	}

}
