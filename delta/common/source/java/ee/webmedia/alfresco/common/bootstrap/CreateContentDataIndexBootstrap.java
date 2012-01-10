package ee.webmedia.alfresco.common.bootstrap;

import javax.sql.DataSource;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Create index on alf_content_data (content_url_id), otherwise database queries perform full scan on table.
 * 
 * @author Alar Kvell
 */
public class CreateContentDataIndexBootstrap extends AbstractModuleComponent {
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
        LOG.info("Creating database index on alf_content_data table");
        jdbcTemplate.update("CREATE INDEX alf_content_data_url_id ON alf_content_data (content_url_id)");
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

}
