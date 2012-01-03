package ee.webmedia.alfresco.common.bootstrap;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.AbstractLifecycleBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * @author Alar Kvell
 */
public class RemoveDuplicateAccessControlEntryRowsBootstrap extends AbstractLifecycleBean {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(RemoveDuplicateAccessControlEntryRowsBootstrap.class);

    private SimpleJdbcTemplate jdbcTemplate;

    @Override
    protected void onBootstrap(ApplicationEvent event) {
        try {
            TransactionService transactionService = BeanHelper.getSpringBean(TransactionService.class, "transactionService");
            transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    executeInternal();
                    return null;
                }
            }, false, true);
        } catch (Exception e) {
            LOG.error("Error finding and updating duplicate data", e);
        }
    }

    private void executeInternal() {
        String query = "SELECT COUNT(*) AS count, permission_id, authority_id, allowed, applies FROM alf_access_control_entry WHERE context_id IS null GROUP BY permission_id, authority_id, allowed, applies, context_id ORDER BY count DESC";
        String query2 = "SELECT id FROM alf_access_control_entry WHERE permission_id = ? AND authority_id = ? AND allowed = ? AND applies = ?";
        LOG.info("Finding duplicate data");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
        for (Map<String, Object> row : rows) {
            Long count = (Long) row.get("count");
            if (count.intValue() == 1) {
                break;
            }
            LOG.info("Found duplicate data " + row);
            List<Map<String, Object>> rows2 = jdbcTemplate.queryForList(query2, row.get("permission_id"), row.get("authority_id"), row.get("allowed"), row.get("applies"));
            LOG.info("Found " + rows2.size() + " rows for this data: " + rows2);
            if (rows2.size() < 2) {
                LOG.warn("There are less than 2 rows, skipping");
                continue;
            }
            for (int i = 1; i < rows2.size(); i++) {
                Object newId = rows2.get(0).get("id");
                Object oldId = rows2.get(i).get("id");
                int updated = jdbcTemplate.update("UPDATE alf_acl_member SET ace_id = ? WHERE ace_id = ?", newId, oldId);
                LOG.info("Changed alf_acl_member.ace_id " + oldId + " -> " + newId + " - " + updated + " rows updated");
                int deleted = jdbcTemplate.update("DELETE FROM alf_access_control_entry WHERE id = ?", oldId);
                LOG.info("Deleted alf_access_control_entry.id " + oldId + " - " + deleted + " rows deleted");
            }
        }
        LOG.info("Finding duplicate data done");
    }

    @Override
    protected void onShutdown(ApplicationEvent event) {
        // Do nothing
    }

    public void setDataSource(DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

}
