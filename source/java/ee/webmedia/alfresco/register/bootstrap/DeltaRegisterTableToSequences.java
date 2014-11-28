package ee.webmedia.alfresco.register.bootstrap;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.springframework.jdbc.core.JdbcTemplate;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.register.model.RegisterModel;
import ee.webmedia.alfresco.register.service.RegisterService;

public class DeltaRegisterTableToSequences extends AbstractNodeUpdater {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DeltaRegisterTableToSequences.class);

    private RegisterService registerService;
    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    boolean error = false;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        return null;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        NodeRef root = registerService.getRoot();
        List<ChildAssociationRef> children = nodeService.getChildAssocs(root);
        Set<NodeRef> registerRefs = new HashSet<>(children.size());
        for (ChildAssociationRef childRef : children) {
            registerRefs.add(childRef.getChildRef());
        }
        return registerRefs;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected String[] updateNode(NodeRef registerRef) throws Exception {
        Integer registerId = null;
        Integer counter = null;
        try {
            JdbcTemplate jdbcTemplate = getJdbcTemplate();
            registerId = (Integer) nodeService.getProperty(registerRef, RegisterModel.Prop.ID);
            counter = jdbcTemplate.queryForInt("SELECT counter FROM delta_register WHERE register_id = ? ", registerId) + 1;
            jdbcTemplate.update("CREATE SEQUENCE " + RegisterService.SEQ_REGISTER_PREFIX + registerId + RegisterService.SEQ_REGISTER_SUFFIX + " START " + counter + " MINVALUE 0");
        } catch (Exception e) {
            LOG.error("Error creating sequence", e);
            error = true;
        }
        return new String[] { registerId.toString(), String.valueOf(counter) };
    }

    private JdbcTemplate getJdbcTemplate() {
        if (jdbcTemplate == null) {
            jdbcTemplate = new JdbcTemplate(dataSource);
        }
        return jdbcTemplate;
    }

    @Override
    protected void executeUpdater() throws Exception {
        super.executeUpdater();
        dropTable();
    }

    private void dropTable() {
        if (error) {
            return;
        }
        try (Connection con = dataSource.getConnection();
                Statement stmt = con.createStatement();) {
            stmt.executeUpdate("DROP TABLE delta_register");
            con.commit();
        } catch (SQLException e) {
            LOG.error("Failed to drop delta_register table", e);
        }
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setRegisterService(RegisterService registerService) {
        this.registerService = registerService;
    }

}
