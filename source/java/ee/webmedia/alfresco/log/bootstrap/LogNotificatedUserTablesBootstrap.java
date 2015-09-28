package ee.webmedia.alfresco.log.bootstrap;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

public class LogNotificatedUserTablesBootstrap extends CreateTableBootstrap {

	@Override
	protected void dropTables(Statement stmt) throws SQLException {
		stmt.executeUpdate("DROP TABLE DELTA_NOTIFICATION_GROUP_LOG");
		stmt.executeUpdate("DROP TABLE DELTA_NOTIFICATION_USER_GROUP_LOG");
		stmt.executeUpdate("DROP TABLE DELTA_NOTIFICATION_USER_LOG");
	}

	@Override
	protected String getTableToCheck() throws SQLException {
		return "DUMMY_NON_EXISTED";
	}

	@Override
	protected InputStream getSqlStatementsInputStream() {
		return getClass().getResourceAsStream("delta_log_notificated_users_tables.sql");
	}

	@Override
	protected String getTablesLogName() {
		return "log_notificated_users_tables";
	}

}
