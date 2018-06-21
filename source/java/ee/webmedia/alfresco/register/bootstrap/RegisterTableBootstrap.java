package ee.webmedia.alfresco.register.bootstrap;

import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

public class RegisterTableBootstrap extends CreateTableBootstrap {

    @Override
    protected InputStream getSqlStatementsInputStream() {
        return getClass().getResourceAsStream("delta_register_table.sql");
    }

    @Override
    protected String getTableToCheck() {
        return "delta_register";
    }

    @Override
    protected void dropTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE delta_register");
    }

    @Override
    protected String getTablesLogName() {
        return "Register";
    }

}
