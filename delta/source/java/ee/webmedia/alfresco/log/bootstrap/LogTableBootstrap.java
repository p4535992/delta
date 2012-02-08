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

    public LogTableBootstrap() {
        setExecuteOnceOnly(false);
    }

    @Override
    protected void executeInternal() {
        Connection con = null;
        Statement stmt = null;

        try {
            con = dataSource.getConnection();

            if (isLogTablePresent(con)) {
                LOG.info("Delta LOG tables exist.");
            } else {
                LOG.info("Delta LOG tables do NOT exist. Creating...");

                try {
                    stmt = con.createStatement();
                    for (String stmtSql : getDbStatements()) {
                        stmt.executeUpdate(stmtSql);
                    }
                    stmt.close();
                    con.commit();
                } catch (SQLException e) {
                    LOG.error(e);
                    throw new RuntimeException(e);
                }

                LOG.info("Delta LOG tables were created successfully.");
            }

        } catch (SQLException e) {
            LOG.error("Creating DELTA log tables failed.", e);
        } catch (IOException e) {
            LOG.error("Reading DELTA log tables creation script failed.", e);
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

    private boolean isLogTablePresent(Connection con) throws SQLException {
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
            tables = dbMetaData.getColumns("", "", "delta_log", "object_id");

            // Check column "object_id" exists and it must be nullable (ResultSet column 18 = IS_NULLABLE):
            if (!tables.next() || "NO".equals(tables.getString(18))) {
                LOG.info("Log tables exist but are out-of-date. Dropping...");
                Statement stmt = con.createStatement();
                stmt.executeUpdate("DROP TABLE delta_log_level");
                stmt.executeUpdate("DROP TABLE delta_log");
                stmt.close();
                con.commit();
                logTableExists = false;
            }
        } finally {
            if (tables != null) {
                try {
                    tables.close();
                } catch (SQLException e1) {
                    LOG.error(e1);
                }
            }
        }

        return logTableExists;
    }

    private String[] getDbStatements() throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream("delta_log_tables.sql")).split(";");
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
