package ee.webmedia.alfresco.log.bootstrap;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

public class LogTableBootstrap extends CreateTableBootstrap {

    @Override
    protected InputStream getSqlStatementsInputStream() {
        return getClass().getResourceAsStream("delta_log_tables.sql");
    }

    @Override
    protected String getTableToCheck() {
        return "delta_log";
    }

    @Override
    protected void dropTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE delta_log_level");
        stmt.executeUpdate("DROP TABLE delta_log");
    }

    @Override
    protected String getTablesLogName() {
        return "Log";
    }

}
