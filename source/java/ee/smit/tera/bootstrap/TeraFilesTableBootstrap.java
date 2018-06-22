package ee.smit.tera.bootstrap;


import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;

import ee.webmedia.alfresco.common.bootstrap.CreateTableBootstrap;

public class TeraFilesTableBootstrap extends CreateTableBootstrap {

    @Override
    protected InputStream getSqlStatementsInputStream() {
        return getClass().getResourceAsStream("tera_files_tables.sql");
    }

    @Override
    protected String getTableToCheck() {
        return "tera_files";
    }

    @Override
    protected void dropTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE tera_files");
    }

    @Override
    protected String getTablesLogName() {
        return "tera_files";
    }

}

