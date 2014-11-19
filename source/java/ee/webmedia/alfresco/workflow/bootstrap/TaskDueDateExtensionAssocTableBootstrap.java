<<<<<<< HEAD
package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

public class TaskDueDateExtensionAssocTableBootstrap extends CreateTableBootstrap {

    @Override
    protected InputStream getSqlStatementsInputStream() {
        return getClass().getResourceAsStream("delta_task_due_date_extension_assoc_table.sql");
    }

    @Override
    protected String getTableToCheck() {
        return "delta_task_due_date_extension_assoc";
    }

    @Override
    protected void dropTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE delta_task_due_date_extension_assoc");
    }

    @Override
    protected String getTablesLogName() {
        return "DueDateExtensionAssoc";
    }

}
=======
package ee.webmedia.alfresco.workflow.bootstrap;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

public class TaskDueDateExtensionAssocTableBootstrap extends CreateTableBootstrap {

    @Override
    protected InputStream getSqlStatementsInputStream() {
        return getClass().getResourceAsStream("delta_task_due_date_extension_assoc_table.sql");
    }

    @Override
    protected String getTableToCheck() {
        return "delta_task_due_date_extension_assoc";
    }

    @Override
    protected void dropTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE delta_task_due_date_extension_assoc");
    }

    @Override
    protected String getTablesLogName() {
        return "DueDateExtensionAssoc";
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
