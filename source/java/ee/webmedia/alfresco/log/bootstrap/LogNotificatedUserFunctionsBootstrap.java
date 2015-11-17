package ee.webmedia.alfresco.log.bootstrap;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

public class LogNotificatedUserFunctionsBootstrap extends CreateTableBootstrap {

	@Override
	protected String getSeparator() {
		return ";;";
	}

	@Override
	protected void dropTables(Statement stmt) throws SQLException {
		stmt.executeUpdate("DROP FUNCTION IF EXISTS fn_get_group_users(usergroup text)");
		stmt.executeUpdate("DROP FUNCTION IF EXISTS fn_log_user_groups_notifications(notificationid bigint, groups text)");
		stmt.executeUpdate("DROP FUNCTION IF EXISTS fn_get_log_notificated_users(notificationid bigint,  grouphash character varying)");

	}

	@Override
	protected String getTableToCheck() throws SQLException {
		return "DUMMY_NON_EXISTED";
	}

	@Override
	protected InputStream getSqlStatementsInputStream() {
		return getClass().getResourceAsStream("delta_log_notificated_users_functions.sql");
	}

	@Override
	protected String getTablesLogName() {
		return "log_notificated_users_functions";
	}

}
