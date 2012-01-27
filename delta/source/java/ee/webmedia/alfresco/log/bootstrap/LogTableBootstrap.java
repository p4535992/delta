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
        try {
            con = dataSource.getConnection();
        } catch (SQLException e) {
            LOG.error("Failed to get DB connection. Skipping LOG table test", e);
            return;
        }

        if (!isLogTablePresent(con)) {
            LOG.info("Delta LOG tables do NOT exist. Creating...");

            String stmts;
            try {
                stmts = IOUtils.toString(getClass().getResourceAsStream("delta_log_tables.sql"));
            } catch (IOException e1) {
                LOG.error(e1);
                return;
            }

            Statement stmt = null;
            try {
                stmt = con.createStatement();

                for (String stmtSql : stmts.split(";")) {
                    stmt.executeUpdate(stmtSql);
                }
                con.commit();
            } catch (SQLException e) {
                LOG.error(e);
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e) {
                        LOG.error(e);
                    }
                }
            }

            LOG.info("Delta LOG tables were created successfully.");
        } else {
            LOG.info("Delta LOG tables exist.");
        }

        if (con != null) {
            try {
                con.close();
            } catch (SQLException e1) {
                LOG.error(e1);
            }
        }

    }

    private boolean isLogTablePresent(Connection con) {
        boolean logTableExists = true;

        ResultSet tables = null;

        try {
            DatabaseMetaData dbMetaData = con.getMetaData();
            tables = dbMetaData.getTables("", "", "delta_log", null);
            logTableExists = tables.next();
        } catch (SQLException e) {
            LOG.error(e);
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

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
