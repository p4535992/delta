<<<<<<< HEAD
package ee.webmedia.alfresco.common.externalsession.bootstrap;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

/**
 * @author Keit Tehvan
 */
public class ExternalSessionTableBootstrap extends CreateTableBootstrap {

    @Override
    protected InputStream getSqlStatementsInputStream() {
        return getClass().getResourceAsStream("delta_externalsession_tables.sql");
    }

    @Override
    protected String getTableToCheck() {
        return "delta_external_session";
    }

    @Override
    protected void dropTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE delta_external_session");
    }

    @Override
    protected String getTablesLogName() {
        return "externalSession";
    }

    @Override
    public boolean isExecuteOnceOnly() {
        return true;
    }

}
=======
package ee.webmedia.alfresco.common.externalsession.bootstrap;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

public class ExternalSessionTableBootstrap extends CreateTableBootstrap {

    @Override
    protected InputStream getSqlStatementsInputStream() {
        return getClass().getResourceAsStream("delta_externalsession_tables.sql");
    }

    @Override
    protected String getTableToCheck() {
        return "delta_external_session";
    }

    @Override
    protected void dropTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE delta_external_session");
    }

    @Override
    protected String getTablesLogName() {
        return "externalSession";
    }

    @Override
    public boolean isExecuteOnceOnly() {
        return true;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
