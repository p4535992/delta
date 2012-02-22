package ee.webmedia.alfresco.log.bootstrap;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.alfresco.repo.module.AbstractModuleComponent;

/**
 * Log tables bootstrap: fixes computer_ip: "NOT NULL" to "NULLABLE".
 * 
 * @author Martti Tamm
 */
public class LogTableBootstrapFix extends AbstractModuleComponent {

    private DataSource dataSource;

    @Override
    protected void executeInternal() throws Exception {
        Connection con = dataSource.getConnection();
        Statement stmt = con.createStatement();
        stmt.executeUpdate("ALTER TABLE delta_log ALTER COLUMN computer_ip DROP NOT NULL");
        stmt.close();
        con.commit();
        con.close();
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
