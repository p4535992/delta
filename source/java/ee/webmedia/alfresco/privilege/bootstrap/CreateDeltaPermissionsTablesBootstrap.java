package ee.webmedia.alfresco.privilege.bootstrap;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.bootstrap.ExecuteStatementsBootstrap;

public class CreateDeltaPermissionsTablesBootstrap extends ExecuteStatementsBootstrap {

    private SimpleJdbcTemplate jdbcTemplate;

    @Override
    public void executeInternal() throws Exception {
        Boolean newPermissionsExist = jdbcTemplate.queryForObject(
                "SELECT CASE WHEN EXISTS (SELECT * FROM information_schema.tables WHERE table_name = 'delta_node_permission') THEN TRUE ELSE FALSE END", Boolean.class);
        if (!newPermissionsExist) {
            super.executeInternal();
        }
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
