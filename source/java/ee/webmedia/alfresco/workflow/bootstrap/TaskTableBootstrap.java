package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

/**
 * @author Riina Tens
 */
public class TaskTableBootstrap extends CreateTableBootstrap {

    @Override
    protected InputStream getSqlStatementsInputStream() {
        return getClass().getResourceAsStream("delta_task_table.sql");
    }

    @Override
    protected String getTableToCheck() {
        return "delta_task";
    }

    @Override
    protected void dropTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE delta_task");
    }

    @Override
    protected String getTablesLogName() {
        return "Task";
    }
}
