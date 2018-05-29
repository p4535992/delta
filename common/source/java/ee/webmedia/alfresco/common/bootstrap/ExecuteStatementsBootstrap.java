package ee.webmedia.alfresco.common.bootstrap;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class ExecuteStatementsBootstrap extends AbstractModuleComponent {

    protected static final Logger LOG = Logger.getLogger(ExecuteStatementsBootstrap.class);

    protected DataSource dataSource;
    private String resourceName;

    @Override
    protected void executeInternal() throws Exception {
        Connection con = null;
        Statement stmt = null;

        try {
            String fileName = FilenameUtils.getName(resourceName);
            LOG.info("Executing SQL statements from file " + fileName);
            con = dataSource.getConnection();
            stmt = con.createStatement();
            for (String stmtSql : IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourceName)).split(";")) {
                stmt.executeUpdate(stmtSql);
            }
            stmt.close();
            con.commit();
            LOG.info("Successfully executed SQL statements from file " + fileName);

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

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

}
