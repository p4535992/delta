package ee.webmedia.alfresco.privilege.bootstrap;

import javax.sql.DataSource;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Change alt_authority table authority column length restriction to 1024 chars
 * because we have user groups with longer names than current restriction 100 chars
 * NB! It may not be possible to create groups with longer names than 100 chars in future releases of Alfresco
 * (user groups synchronization should be checked when upgrading to newer version of Alfresco).
 * 
 * @author Riina Tens
 */
public class AlterAltAuthorityTable extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    private SimpleJdbcTemplate jdbcTemplate;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Executing " + getName());
        serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
            @Override
            public Void execute() throws Throwable {
                executeInTransaction();
                return null;
            }
        }, false, true);
    }

    private void executeInTransaction() {
        LOG.info("Change alf_authority table authority column length restriction to 1024 chars");
        jdbcTemplate.update("ALTER TABLE alf_authority ALTER authority TYPE character varying(1024)");
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

}
