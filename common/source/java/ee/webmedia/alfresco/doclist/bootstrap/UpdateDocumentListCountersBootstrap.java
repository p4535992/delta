package ee.webmedia.alfresco.doclist.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.doclist.service.DocumentListService;
import ee.webmedia.alfresco.functions.service.FunctionsService;

public class UpdateDocumentListCountersBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    private GeneralService generalService;
    private FunctionsService functionsService;
    private TransactionService transactionService;
    private DocumentListService documentListService;

    @Override
    protected void executeInternal() throws Throwable {
        List<NodeRef> roots = new ArrayList<NodeRef>();
        roots.add(functionsService.getFunctionsRoot());
        for (ArchivalsStoreVO archivalsStoreVO : generalService.getArchivalsStoreVOs()) {
            roots.add(archivalsStoreVO.getNodeRef());
        }
        RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
        for (final NodeRef nodeRef : roots) {
            LOG.info("Starting to update document list counters under " + nodeRef);
            documentListService.updateDocCounters(nodeRef);
            LOG.info("Completed updating document list counters under " + nodeRef);
        }
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setDocumentListService(DocumentListService documentListService) {
        this.documentListService = documentListService;
    }

}
