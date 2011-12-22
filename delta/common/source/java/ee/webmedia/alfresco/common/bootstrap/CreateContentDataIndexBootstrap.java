package ee.webmedia.alfresco.common.bootstrap;

import javax.sql.DataSource;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Create index on alf_content_data (content_url_id), otherwise database queries perform full scan on table.
 * 
 * @author Alar Kvell
 */
public class CreateContentDataIndexBootstrap extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(CreateContentDataIndexBootstrap.class);

    private SimpleJdbcTemplate jdbcTemplate;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Creating database index on alf_content_data table");
        jdbcTemplate.update("CREATE INDEX alf_content_data_url_id ON alf_content_data (content_url_id)");
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

}
