package ee.webmedia.alfresco.common.bootstrap;

import java.sql.SQLException;

import javax.faces.event.ActionEvent;
import javax.sql.DataSource;

import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.transaction.TransactionService;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

public class InvalidNodeFixerBootstrap {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(InvalidNodeFixerBootstrap.class);

    private TransactionService transactionService;
    private SimpleJdbcTemplate jdbcTemplate;

    public void execute(@SuppressWarnings("unused") ActionEvent event) {
        execute();
    }

    public synchronized void execute() {
        try {
            transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    executeInternal();
                    return null;
                }
            }, false);
        } catch (Exception e) {
            LOG.error("Error finding and fixing invalid nodes from database", e);
        }
    }

    private void executeInternal() {
        LOG.info("Finding invalid nodes from database...");
        jdbcTemplate.query("SELECT alf_node.id, alf_store.protocol, alf_store.identifier, alf_node.uuid, alf_transaction.change_txn_id, " +
                "alf_node.audit_creator, alf_node.audit_created, alf_node.audit_modifier, alf_node.audit_modified, " +
                "alf_qname.local_name, alf_namespace.uri " +
                "FROM alf_node " +
                "LEFT JOIN alf_transaction ON alf_node.transaction_id = alf_transaction.id " +
                "LEFT JOIN alf_store ON alf_node.store_id = alf_store.id " +
                "LEFT JOIN alf_store alf_store2 ON alf_node.id = alf_store2.root_node_id " +
                "LEFT JOIN alf_child_assoc ON alf_node.id = alf_child_assoc.child_node_id " +
                "LEFT JOIN alf_qname ON alf_node.type_qname_id = alf_qname.id " +
                "LEFT JOIN alf_namespace ON alf_qname.ns_id = alf_namespace.id " +
                "WHERE alf_node.node_deleted = false " +
                "AND alf_store2.id IS NULL " +
                "AND alf_child_assoc.id IS NULL", new ParameterizedRowMapper<NodeRef>() {
            @Override
            public NodeRef mapRow(java.sql.ResultSet rs, int rowNum) throws SQLException {
                StoreRef storeRef = new StoreRef(rs.getString("protocol"), rs.getString("identifier"));
                NodeRef nodeRef = new NodeRef(storeRef, rs.getString("uuid"));
                LOG.warn("Found node with no parents and no root aspect - fixing:"
                        + "\n  nodeRef=" + nodeRef
                        + "\n  changeTxnId=" + rs.getObject("change_txn_id")
                        + "\n  type={" + rs.getObject("uri") + "}" + rs.getObject("local_name")
                        + "\n  creator=" + rs.getObject("audit_creator")
                        + "\n  created=" + rs.getObject("audit_created")
                        + "\n  modifier=" + rs.getObject("audit_modifier")
                        + "\n  modified=" + rs.getObject("audit_modified")
                        + "\n  properties: " + jdbcTemplate.queryForList("SELECT * FROM alf_node_properties WHERE node_id = ?", rs.getObject("id"))
                        + "\n  aspects: " + jdbcTemplate.queryForList("SELECT * FROM alf_node_aspects WHERE node_id = ?", rs.getObject("id"))
                        + "\n  assocs: " + jdbcTemplate.queryForList("SELECT * FROM alf_node_assoc WHERE source_node_id = ? OR target_node_id = ?",
                                rs.getObject("id"), rs.getObject("id")));
                // TODO log alf_content for files ?
                Assert.isTrue(jdbcTemplate.update("UPDATE alf_node SET node_deleted = true WHERE id = ?", rs.getObject("id")) == 1);
                LOG.info("Fixed: set node_deleted = true for node id=" + rs.getObject("id") + " nodeRef=" + nodeRef);
                return null;
            }
        });
        LOG.info("Finished finding invalid nodes from database.");
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

}
