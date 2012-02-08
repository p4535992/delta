package ee.webmedia.alfresco.document.bootstrap;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.doclist.service.DocumentListService;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Deletes static documents with type defined in DocumentSubtypeModel.MODEL_NAME
 * and update number of documents in functions/series/volumes/cases.
 * NB! In the future replace this with 2.5 -> 3.x migration script.
 * 
 * @author Riina Tens
 */
public class DeleteSubtypedDocuments extends AbstractNodeUpdater {

    private TransactionService transactionService;
    private DictionaryService dictionaryService;
    private DocumentListService documentListService;

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        Collection<QName> docSubTypes = dictionaryService.getTypes(DocumentSubtypeModel.MODEL_NAME);
        String query = SearchUtil.generateTypeQuery(docSubTypes);
        return Arrays.asList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query),
                searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected void executeUpdater() throws Exception {
        super.executeUpdater();
        RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
        retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Integer>() {

            @Override
            public Integer execute() throws Throwable {
                documentListService.updateDocCounters();
                return null;
            }
        });
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName type = nodeService.getType(nodeRef);
        nodeService.deleteNode(nodeRef);
        return new String[] { type.toString() };
    }

    @Override
    protected String[] getCsvFileHeaders() {
        return new String[] { "nodeRef", "type" };
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setDocumentListService(DocumentListService documentListService) {
        this.documentListService = documentListService;
    }

}
