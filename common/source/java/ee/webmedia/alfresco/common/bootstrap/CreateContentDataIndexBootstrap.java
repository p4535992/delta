package ee.webmedia.alfresco.common.bootstrap;

import javax.sql.DataSource;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Create index on alf_content_data (content_url_id), otherwise database queries perform full scan on table.
<<<<<<< HEAD
 * 
 * @author Alar Kvell
=======
>>>>>>> develop-5.1
 */
public class CreateContentDataIndexBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

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
