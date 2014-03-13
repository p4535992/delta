package ee.webmedia.alfresco.privilege.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.security.permissions.ACLType;
import org.alfresco.repo.security.permissions.impl.AclChange;
import org.alfresco.repo.security.permissions.impl.AclDaoComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.privilege.service.AccessControlListExtDAO;
import ee.webmedia.alfresco.utils.ProgressTracker;

/**
 * Updater for fixing invalid acls. Logic is adapted from Alfresco later branch FixAclInheritancePatch.java.
 * See cl task 209621 for details.
 */
public class FixAclInheritanceUpdater extends AbstractModuleComponent {

    private static final String PARENT_NODE_STORE_IDENTIFIER = "parent_node_store_identifier";

    private static final String PARENT_NODE_STORE_PROTOCOL = "parent_node_store_protocol";

    private static final String PARENT_NODE_UUID = "parent_node_uuid";

    private static final String PRIMARY_PARENT_ACL_ID = "primary_parent_acl_id";

    private static final String CHILD_ACL_TYPE = "child_acl_type";

    private static final String CHILD_ACL_ID = "child_acl_id";

    protected final Log log = LogFactory.getLog(getClass());

    private SimpleJdbcTemplate jdbcTemplate;
    private AclDaoComponent aclDaoComponent;
    private AccessControlListExtDAO accessControlListDao;
    private boolean enabled = true;

    private String nodeUuidP3;
    private String nodeUuidP4;

    private static final String ALL_ACLS_THAT_INHERIT_WITH_INHERITANCE_UNSET_SQL = "select distinct " +
            "childAcl.id as child_acl_id, " +
            "childAcl.type as child_acl_type, " +
            "acl.id      as primary_parent_acl_id, " +
            "acl.type    as primary_parent_acl_type, " +
            "child.id    as child_node_id, " +
            "node.uuid    as parent_node_uuid, " +
            "store.protocol as parent_node_store_protocol, " +
            "store.identifier as parent_node_store_identifier, " +
            "child.uuid    as child_node_uuid " +
            "        from " +
            "alf_access_control_list acl " +
            "join alf_acl_member mem on mem.acl_id = acl.id " +
            "join alf_node node on node.acl_id = acl.id " +
            "join alf_child_assoc priChild on priChild.parent_node_id = node.id and is_primary = 'true' " +
            " join alf_node child on priChild.child_node_id = child.id " +
            " join alf_store store on store.id = node.store_id " +
            "join alf_access_control_list childAcl on childAcl.id = child.acl_id AND childAcl.inherits = 'true' " +
            " where      " +
            "           childAcl.id is not null " +
            "     AND childAcl.inherits_from is null " +
            "     AND childAcl.id <> acl.id ";

    @Override
    public boolean isRequiresNewTransaction() {
        return false;
    }

    @Override
    /** p3 errors */
    protected void executeInternal() {
        if (enabled) {
            fixAllAclsThatInheritFromNonPrimaryParent();
        }
    }

    /** Fix all p3 errors */
    public void fixAllAclsThatInheritFromNonPrimaryParent() {
        executeInternal(false, true, null, null);
    }

    /** Fix p4 errors, if nodeUuidP4 is set, uses it to fix only given node */
    public void fixAclsThatInheritWithInheritanceUnset(@SuppressWarnings("unused") ActionEvent event) throws Throwable {
        executeInternal(true, false, nodeUuidP4, null);
    }

    /** Fix p3 errors, if nodeUuidP3 is set, uses it to fix only given node */
    public void fixAclsThatInheritFromNonPrimaryParent(@SuppressWarnings("unused") ActionEvent event) throws Throwable {
        executeInternal(false, true, null, nodeUuidP3);
    }

    private synchronized void executeInternal(boolean fixAclsThatInheritWithInheritanceUnset, boolean fixAclsThatInheritFromNonPrimaryParent, String nodeUuidP4, String nodeUuidP3) {
        log.info("Started fixAclInheritanceUpdater");

        // p4:
        // inherit_from = null errors only affect deleting nodes and the fix is applied on deleting node if needed.
        // Corresponds to Cl task 209621 p4 error.
        if (fixAclsThatInheritWithInheritanceUnset) {
            fixAclsThatInheritWithInheritanceUnset(createUuidCondition(nodeUuidP4, "fixAclsThatInheritWithInheritanceUnset"));
        } else {
            log.info("Skipped fixAclsThatInheritWithInheritanceUnset");
        }

        // NB: although in original updater this code runs after p4 (aclsThatInheritWithInheritanceUnset),
        // it may be more efficient to run it before p4, because p4 creates entries that correspond to p3 (aclsThatInheritFromNonPrimaryParent),
        // although such errors seem not to be actual errors (child nodes are not referring to parent acl,
        // but grand parent acl, what according to Alfresco acl documentation is legal situation)

        // p3:
        // Corresponds to Cl task 209621 p3 error.
        // aclsThatInheritFromNonPrimaryParent
        if (fixAclsThatInheritFromNonPrimaryParent) {
            fixAclsThatInheritFromNonPrimaryParent(createUuidCondition(nodeUuidP3, "fixAclsThatInheritFromNonPrimaryParent"));
        } else {
            log.info("Skipped fixAclsThatInheritFromNonPrimaryParent");
        }

        // NB! Original patch updater includes several other acl fixes; implementations should be adapted here
        // if it occurs that corresponding erroneous data is present in Delta.
    }

    public String createUuidCondition(String nodeUuid, String updaterName) {
        String nodeUuidCondition = "";
        if (StringUtils.isNotBlank(nodeUuid)) {
            log.info("Running " + updaterName + " for node.uuid=" + nodeUuid);
            nodeUuidCondition = " AND child.uuid='" + nodeUuid + "'";
        }
        return nodeUuidCondition;
    }

    private void fixAclsThatInheritFromNonPrimaryParent(String nodeUuidCondition) {
        final String allAclsThatInheritFromNonPrimaryParentSql = "select distinct " +
                "childAcl.id as child_acl_id, " +
                "childAcl.type as child_acl_type, " +
                "acl.id      as primary_parent_acl_id,  " +
                "acl.type    as primary_parent_acl_type, " +
                "node.uuid    as parent_node_uuid, " +
                "store.protocol as parent_node_store_protocol, " +
                "store.identifier as parent_node_store_identifier, " +
                "child.id    as child_node_id " +
                "from  " +
                " alf_access_control_list acl  " +
                "    join alf_acl_member mem on mem.acl_id = acl.id " +
                "join alf_node node on node.acl_id = acl.id " +
                "join alf_child_assoc priChild on priChild.parent_node_id = node.id and is_primary = true " +
                "join alf_node child on priChild.child_node_id = child.id " +
                "join alf_store store on store.id = node.store_id " +
                "join alf_access_control_list childAcl on childAcl.id = child.acl_id AND childAcl.inherits = true " +
                " where   " +
                "( " +
                "           childAcl.id is not null " +
                "AND acl.id <> childAcl.id  " +
                "  AND acl.id <> childAcl.inherits_from  " +
                " AND acl.inherited_acl <> childAcl.inherits_from " +
                nodeUuidCondition +
                ") ";

        RetryingTransactionHelper retryingTransactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
        List<Map<String, Object>> rows = queryForList(allAclsThatInheritFromNonPrimaryParentSql, retryingTransactionHelper);

        int rowsSize = rows.size();
        log.info("fixAclInheritanceUpdater found " + rowsSize + " invalid acls which inherit not from primary parent.");
        if (rowsSize <= 0) {
            return;
        }

        int fixedCount = 0;
        int count = 0;
        ProgressTracker progress = new ProgressTracker(rowsSize, 0);
        for (Map<String, Object> row : rows) {
            Long childAclId = (Long) row.get(CHILD_ACL_ID);
            Integer childAclType = (Integer) row.get(CHILD_ACL_TYPE);
            Long primaryParentAclId = (Long) row.get(PRIMARY_PARENT_ACL_ID);
            String parentNodeUuid = (String) row.get(PARENT_NODE_UUID);
            String parentNodeStoreProtocol = (String) row.get(PARENT_NODE_STORE_PROTOCOL);
            String parentNodeStoreIdentifier = (String) row.get(PARENT_NODE_STORE_IDENTIFIER);

            ACLType childType = ACLType.getACLTypeFromId(childAclType.intValue());

            RetryingTransactionCallback<Void> cb = null;
            switch (childType) {
            case DEFINING:
                cb = new FixInherited(primaryParentAclId, childAclId);
                retryingTransactionHelper.doInTransaction(cb, false, true);
                fixedCount++;
                break;
            case FIXED:
                break;
            case GLOBAL:
                break;
            case LAYERED:
                break;
            case OLD:
                break;
            case SHARED:
                NodeRef parentNodeRef = new NodeRef(parentNodeStoreProtocol, parentNodeStoreIdentifier, parentNodeUuid);
                cb = new SetFixedAclsCallback(primaryParentAclId, parentNodeRef);
                retryingTransactionHelper.doInTransaction(cb, false, true);
                fixedCount++;
                break;
            }
            if (++count >= 200) {
                String info = progress.step(count);
                count = 0;
                if (info != null) {
                    log.info("Fixed p3: " + info);
                }
            }
        }
        String info = progress.step(count);
        if (info != null) {
            log.info("Fixed p3: " + info);
        }
        List<Map<String, Object>> check = jdbcTemplate.queryForList(allAclsThatInheritFromNonPrimaryParentSql);
        log.info("Applied acl fix aclsThatInheritFromNonPrimaryParent on " + fixedCount + " nodes, remaining " + check.size() + " nodes with invalid acl(s)");
    }

    public List<Map<String, Object>> queryForList(final String allAclsThatInheritFromNonPrimaryParentSql, RetryingTransactionHelper retryingTransactionHelper) {
        log.info("Querying for acls...");
        List<Map<String, Object>> rows = retryingTransactionHelper
                .doInTransaction(new RetryingTransactionCallback<List<Map<String, Object>>>()
                {

                    @Override
                    public List<Map<String, Object>> execute() throws Throwable
                    {
                        return jdbcTemplate.queryForList(allAclsThatInheritFromNonPrimaryParentSql);
                    }
                }, false, true);
        return rows;
    }

    public void fixAclsThatInheritWithInheritanceUnset(String nodeUuidCondition) {
        RetryingTransactionHelper retryingTransactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
        String sql = ALL_ACLS_THAT_INHERIT_WITH_INHERITANCE_UNSET_SQL + (StringUtils.isNotBlank(nodeUuidCondition) ? nodeUuidCondition : "");
        List<Map<String, Object>> rows = queryForList(sql,
                retryingTransactionHelper);

        int rowsSize = rows.size();
        log.info("fixAclInheritanceUpdater found " + rowsSize + " invalid acls where inherits_from=null.");
        if (rowsSize <= 0) {
            return;
        }

        int fixedCount = 0;
        int count = 0;
        ProgressTracker progress = new ProgressTracker(rowsSize, 0);
        for (Map<String, Object> row : rows) {
            Long childAclId = (Long) row.get(CHILD_ACL_ID);
            Integer childAclType = (Integer) row.get(CHILD_ACL_TYPE);
            Long primaryParentAclId = (Long) row.get(PRIMARY_PARENT_ACL_ID);
            String parentNodeUuid = (String) row.get(PARENT_NODE_UUID);
            String parentNodeStoreProtocol = (String) row.get(PARENT_NODE_STORE_PROTOCOL);
            String parentNodeStoreIdentifier = (String) row.get(PARENT_NODE_STORE_IDENTIFIER);

            ACLType childType = ACLType.getACLTypeFromId(childAclType.intValue());

            RetryingTransactionCallback<Void> cb = null;

            switch (childType) {
            case DEFINING:
                // TODO: implement if needed; check if Delta applications contain such erroneous data
                // cb = new FixInherited(primaryParentAclId, childAclId);
                // retryingTransactionHelper.doInTransaction(cb, false, true);
                // fixedCount++;
                log.info("Found node with invalid defining acl, patch should be implemented!");
                break;
            case FIXED:
                break;
            case GLOBAL:
                break;
            case LAYERED:
                break;
            case OLD:
                break;
            case SHARED:
                NodeRef parentNodeRef = new NodeRef(parentNodeStoreProtocol, parentNodeStoreIdentifier, parentNodeUuid);
                cb = new FixSharedUnsetInheritanceCallback(primaryParentAclId, childAclId, parentNodeRef);
                retryingTransactionHelper.doInTransaction(cb, false, true);
                fixedCount++;
                break;
            }
            if (++count >= 200) {
                String info = progress.step(count);
                count = 0;
                if (info != null) {
                    log.info("Fixed p4: " + info);
                }
            }
        }
        String info = progress.step(count);
        if (info != null) {
            log.info("Fixed p4: " + info);
        }
        List<Map<String, Object>> check = queryForList(sql, retryingTransactionHelper);
        log.info("Applied acl fix aclsThatInheritWithInheritanceUnset on " + fixedCount + " nodes, remaining " + check.size() + " nodes with invalid acl(s)");
    }

    private class FixSharedUnsetInheritanceCallback implements RetryingTransactionCallback<Void> {

        Long primaryParentAclId;
        Long childAclId;
        NodeRef parentNodeRef;

        FixSharedUnsetInheritanceCallback(Long primaryParentAclId, Long childAclId, NodeRef parentNodeRef) {
            this.primaryParentAclId = primaryParentAclId;
            this.childAclId = childAclId;
            this.parentNodeRef = parentNodeRef;
        }

        @Override
        public Void execute() throws Throwable {
            fixAclInheritFromNull(childAclId, primaryParentAclId, parentNodeRef, accessControlListDao);
            return null;
        }
    }

    public static void fixAclInheritFromNull(Long childAclId, Long primaryParentAclId, NodeRef parentNodeRef, AccessControlListExtDAO accessControlListDao) {
        accessControlListDao.fixAclInheritFromNull(childAclId, primaryParentAclId, parentNodeRef);
    }

    private class FixInherited implements RetryingTransactionCallback<Void> {

        Long primaryParentAclId;

        Long childAclId;

        FixInherited(Long primaryParentAclId, Long childAclId) {
            this.primaryParentAclId = primaryParentAclId;
            this.childAclId = childAclId;
        }

        @Override
        public Void execute() throws Throwable {
            aclDaoComponent.enableInheritance(childAclId, primaryParentAclId);
            return null;
        }
    }

    private class SetFixedAclsCallback implements RetryingTransactionCallback<Void> {
        Long primaryParentAclId;
        NodeRef parentNodeRef;

        SetFixedAclsCallback(Long primaryParentAclId, NodeRef parentNodeRef) {
            this.primaryParentAclId = primaryParentAclId;
            this.parentNodeRef = parentNodeRef;
        }

        @Override
        public Void execute() throws Throwable {
            List<AclChange> changes = new ArrayList<AclChange>();
            accessControlListDao.setFixedAcls(parentNodeRef, primaryParentAclId, null, changes, true);
            return null;
        }
    }

    public SimpleJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AclDaoComponent getAclDaoComponent() {
        return aclDaoComponent;
    }

    public void setAclDaoComponent(AclDaoComponent aclDaoComponent) {
        this.aclDaoComponent = aclDaoComponent;
    }

    public void setAccessControlListDao(AccessControlListExtDAO accessControlListDao) {
        this.accessControlListDao = accessControlListDao;
    }

    public void setNodeUuidP3(String nodeUuidP3) {
        this.nodeUuidP3 = nodeUuidP3;
    }

    public String getNodeUuidP3() {
        return nodeUuidP3;
    }

    public void setNodeUuidP4(String nodeUuidP4) {
        this.nodeUuidP4 = nodeUuidP4;
    }

    public String getNodeUuidP4() {
        return nodeUuidP4;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
