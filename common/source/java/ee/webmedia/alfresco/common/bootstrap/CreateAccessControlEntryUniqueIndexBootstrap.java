package ee.webmedia.alfresco.common.bootstrap;

import javax.sql.DataSource;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Create partial unique constraint on alf_access_control_entry to prevent duplicate rows where context_id is null,
 * if the index doesn't exist. It is possible that the index has been created already by the bootstrap,
 * because this bootstrap's module was changed from common to simdhs. So in some upgrades from 3.13.15 to 3.13.16 branch
 * it may run twice.
 * 
 * @author Alar Kvell
 */
public class CreateAccessControlEntryUniqueIndexBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    private static final String ACE_INDEX_KEY = "delta_access_control_entry_context_id_null_key";

    private SimpleJdbcTemplate jdbcTemplate;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Creating database index on alf_access_control_entry table");
        long existingIndexCount = jdbcTemplate.queryForLong("SELECT COUNT(*) FROM pg_indexes WHERE indexname = '" + ACE_INDEX_KEY + "'");
        if (existingIndexCount == 0) {
            jdbcTemplate
                    .update("CREATE UNIQUE INDEX " + ACE_INDEX_KEY + " ON alf_access_control_entry (permission_id, authority_id, allowed, applies) WHERE context_id IS NULL");
        }
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

}
