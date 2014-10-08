package ee.webmedia.alfresco.privilege.bootstrap;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.bootstrap.ExecuteStatementsBootstrap;

public class InsertDeltaPermissionsBootstrap extends ExecuteStatementsBootstrap {

    private SimpleJdbcTemplate jdbcTemplate;

    @Override
    public void executeInternal() throws Exception {
        Boolean oldPermissionsExist = jdbcTemplate.queryForObject(
                "SELECT CASE WHEN EXISTS (SELECT * FROM information_schema.tables WHERE table_name = 'alf_access_control_list') THEN TRUE ELSE FALSE END", Boolean.class);
        if (oldPermissionsExist) {
            super.executeInternal();
        }
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
