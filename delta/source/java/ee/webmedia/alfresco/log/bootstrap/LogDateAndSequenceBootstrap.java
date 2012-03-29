package ee.webmedia.alfresco.log.bootstrap;

import javax.sql.DataSource;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * @author Alar Kvell
 */
public class LogDateAndSequenceBootstrap extends AbstractModuleComponent {
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
        jdbcTemplate.update("CREATE TABLE delta_log_date (idprefix text)");
        jdbcTemplate.update("INSERT INTO delta_log_date VALUES (to_char(CURRENT_DATE,'YYYYMMDD'))");
        jdbcTemplate.update("CREATE SEQUENCE delta_log_seq");
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

}
