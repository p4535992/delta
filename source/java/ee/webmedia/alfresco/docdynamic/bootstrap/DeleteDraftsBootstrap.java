package ee.webmedia.alfresco.docdynamic.bootstrap;

import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.transaction.TransactionService;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.ProgressTracker;

/**
 * Delete all nodes under drafts, except if running not on startup, then exclude nodes that have a lock which is not expired.
 */
public class DeleteDraftsBootstrap extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DeleteDraftsBootstrap.class);

    private NodeService nodeService;
    private DocumentService documentService;
    private TransactionService transactionService;

    @Override
    public boolean isRequiresNewTransaction() {
        return false;
    }

    /**
     * When application is running
     */
    public void execute(@SuppressWarnings("unused") ActionEvent event) {
        executeInternal(false);
    }

    /**
     * On application startup
     */
    @Override
    protected void executeInternal() throws Throwable {
        executeInternal(true);
    }

    public void executeInternal(final boolean deleteAll) {
        final NodeRef drafts = documentService.getDrafts();
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(drafts);
        LOG.info("Found " + childAssocs.size() + " nodes under drafts" + (childAssocs.isEmpty() ? "" : ", deleting"));
        if (childAssocs.isEmpty()) {
            return;
        }
        int local = 0;
        int deletedSuccess = 0, deletedError = 0, notDeleted = 0;
        ProgressTracker progress = new ProgressTracker(childAssocs.size(), 0);
        for (ChildAssociationRef childAssociationRef : childAssocs) {
            final NodeRef nodeRef = childAssociationRef.getChildRef();
            try {
                if (checkAndDelete(nodeRef, drafts, deleteAll)) {
                    deletedSuccess++;
                } else {
                    notDeleted++;
                }
            } catch (Exception e) {
                LOG.error("Error deleting node " + nodeRef + ", ignoring and continuing");
                deletedError++;
            }
            if (++local > 10) {
                String info = progress.step(local);
                local = 0;
                if (info != null) {
                    LOG.info("Deleting: " + info);
                }
            }
        }
        String info = progress.step(local);
        if (info != null) {
            LOG.info("Deleting: " + info);
        }
        LOG.info("Completed deleting " + deletedSuccess + " nodes successfully, " + deletedError + " nodes not deleted because of errors, " + notDeleted
                + " nodes not deleted because in use");
    }

    private boolean checkAndDelete(final NodeRef nodeRef, final NodeRef drafts, final boolean deleteAll) {
        return AuthenticationUtil.runAs(new RunAsWork<Boolean>() {
            @Override
            public Boolean doWork() throws Exception {
                return transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Boolean>() {
                    @Override
                    public Boolean execute() throws Throwable {
                        if (!drafts.equals(nodeService.getPrimaryParent(nodeRef).getParentRef())) {
                            return false;
                        }
                        boolean delete = deleteAll;
                        if (!delete) {
                            if (nodeService.hasAspect(nodeRef, DocumentCommonModel.Aspects.SEARCHABLE)) {
                                delete = true;
                            } else {
                                LockStatus lockStatus = BeanHelper.getDocLockService().getLockStatus(nodeRef, "");
                                delete = lockStatus == LockStatus.NO_LOCK || lockStatus == LockStatus.LOCK_EXPIRED;
                            }
                        }
                        if (delete) {
                            nodeService.deleteNode(nodeRef);
                            return true;
                        }
                        return false;
                    }
                });
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

}
