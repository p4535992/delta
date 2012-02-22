package ee.webmedia.alfresco.log.bootstrap;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Log tables bootstrap: makes sure that delta_log* tables (2) are present, and creates them when necessary.
 * 
 * @author Martti Tamm
 */
public class LogTableBootstrap extends AbstractModuleComponent {

    private static final Logger LOG = Logger.getLogger(LogTableBootstrap.class);

    private DataSource dataSource;

    @Override
    protected void executeInternal() throws Exception {
        Connection con = null;
        Statement stmt = null;

        try {
            con = dataSource.getConnection();

            dropLogTables(con);

            LOG.info("Creating log tables...");
            stmt = con.createStatement();
            for (String stmtSql : getDbStatements()) {
                stmt.executeUpdate(stmtSql);
            }
            stmt.close();
            con.commit();
            LOG.info("Log tables were created successfully");

        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e1) {
                    LOG.error(e1);
                }
            }
        }
    }

    private void dropLogTables(Connection con) throws SQLException {
        boolean logTableExists = true;

        DatabaseMetaData dbMetaData = con.getMetaData();
        ResultSet tables = null;

        try {
            tables = dbMetaData.getTables("", "", "delta_log", null);
            logTableExists = tables.next();
        } finally {
            if (tables != null) {
                try {
                    tables.close();
                } catch (SQLException e1) {
                    LOG.error(e1);
                }
            }
        }

        try {
            if (logTableExists) {
                LOG.info("Log tables exist, dropping...");
                Statement stmt = con.createStatement();
                stmt.executeUpdate("DROP TABLE delta_log_level");
                stmt.executeUpdate("DROP TABLE delta_log");
                stmt.close();
                con.commit();
            }
        } finally {
            if (tables != null) {
                tables.close();
            }
        }
    }

    private String[] getDbStatements() throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream("delta_log_tables.sql")).split(";");
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
