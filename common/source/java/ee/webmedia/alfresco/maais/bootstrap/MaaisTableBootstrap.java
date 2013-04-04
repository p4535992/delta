package ee.webmedia.alfresco.maais.bootstrap;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

/**
 * @author Keit Tehvan
 */
public class MaaisTableBootstrap extends CreateTableBootstrap {

    @Override
    protected InputStream getSqlStatementsInputStream() {
        return getClass().getResourceAsStream("delta_maais_tables.sql");
    }

    @Override
    protected String getTableToCheck() {
        return "delta_maais_session";
    }

    @Override
    protected void dropTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE delta_maais_session");
    }

    @Override
    protected String getTablesLogName() {
        return "maais";
    }

    @Override
    public boolean isExecuteOnceOnly() {
        return true;
    }

}
