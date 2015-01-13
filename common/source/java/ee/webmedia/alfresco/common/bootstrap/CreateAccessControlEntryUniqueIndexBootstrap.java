package ee.webmedia.alfresco.common.bootstrap;

import javax.sql.DataSource;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Create partial unique constraint on alf_access_control_entry to prevent duplicate rows where context_id is null.
 */
public class CreateAccessControlEntryUniqueIndexBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    private SimpleJdbcTemplate jdbcTemplate;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Creating database index on alf_access_control_entry table");
        jdbcTemplate
                .update("CREATE UNIQUE INDEX delta_access_control_entry_context_id_null_key ON alf_access_control_entry (permission_id, authority_id, allowed, applies) WHERE context_id IS NULL");
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

}
