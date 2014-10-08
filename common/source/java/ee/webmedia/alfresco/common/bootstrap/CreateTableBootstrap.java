package ee.webmedia.alfresco.common.bootstrap;

import java.io.InputStream;
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
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> develop-5.1
 */
public abstract class CreateTableBootstrap extends AbstractModuleComponent {

    protected static final Logger LOG = Logger.getLogger(CreateTableBootstrap.class);

    private DataSource dataSource;

    @Override
    protected void executeInternal() throws Exception {
        Connection con = null;
        Statement stmt = null;

        try {
            con = dataSource.getConnection();

            dropTables(con);

            LOG.info("Creating " + (getTablesLogName() != null ? getTablesLogName().toLowerCase() : "") + " tables...");
            stmt = con.createStatement();
            for (String stmtSql : IOUtils.toString(getSqlStatementsInputStream()).split(";")) {
                stmt.executeUpdate(stmtSql);
            }
            stmt.close();
            con.commit();
            LOG.info(getTablesLogName() + " tables were created successfully");

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

    private void dropTables(Connection con) throws SQLException {
        boolean tableExists = true;

        DatabaseMetaData dbMetaData = con.getMetaData();
        ResultSet tables = null;

        try {
            tables = dbMetaData.getTables("", "", getTableToCheck(), null);
            tableExists = tables.next();
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
            if (tableExists) {
                LOG.info(getTablesLogName() + " tables exist, dropping...");
                Statement stmt = con.createStatement();
                dropTables(con.createStatement());
                stmt.close();
                con.commit();
            }
        } finally {
            if (tables != null) {
                tables.close();
            }
        }
    }

    protected abstract void dropTables(Statement stmt) throws SQLException;

    protected abstract String getTableToCheck() throws SQLException;

    protected abstract InputStream getSqlStatementsInputStream();

    protected abstract String getTablesLogName();

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
