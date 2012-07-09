package ee.webmedia.alfresco.log.bootstrap;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Copy all document and series log objects (of type documentLog) to application log,
 * if created more than one minute before first application log record of given document or series.
 * Delete log objects after that.
 * One minute delay is supposed to be enough time for different logs created during same transaction taken in account
 * and not long enough that application can be restarted with different logging functionality.
 * 
 * @author Riina Tens
 */
public class MoveDocumentAndSeriesLogToAppLog extends AbstractNodeUpdater {

    private long logTableSequence = 1;
    private String idPrefix;
    private final List<NodeRef> logNodesToDelete = new ArrayList<NodeRef>();

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsOr(SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT),
                SearchUtil.generateTypeQuery(SeriesModel.Types.SERIES));
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllWithArchivalsStoreRefs()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        LogObject logObject = DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(nodeRef)) ? LogObject.DOCUMENT : LogObject.SERIES;
        Date firstAppLogDate = BeanHelper.getLogService().getFirstLogEntryDate(nodeRef);
        boolean noExistingAppLog = firstAppLogDate == null;
        List<ChildAssociationRef> logChildAssocs = nodeService.getChildAssocs(nodeRef, Collections.singleton(DocumentCommonModel.Types.DOCUMENT_LOG));
        StringBuffer copiedLogEntries = new StringBuffer("");
        StringBuffer deletedLogEntries = new StringBuffer("");
        int logCount = 0;
        for (ChildAssociationRef childAssoc : logChildAssocs) {
            NodeRef logRef = childAssoc.getChildRef();
            Map<QName, Serializable> logProps = nodeService.getProperties(logRef);
            Date logDate = (Date) logProps.get(DocumentCommonModel.Props.CREATED_DATETIME);
            String creatorName = (String) logProps.get(DocumentCommonModel.Props.CREATOR_NAME);
            String description = (String) logProps.get(DocumentCommonModel.Props.EVENT_DESCRIPTION);
            String result = "LOG" + logCount + ": logDate=" + logDate + "; creatorName=" + creatorName + "; desc=" + description + "\n";
            if (noExistingAppLog || logDate == null || (firstAppLogDate.after(logDate) && (firstAppLogDate.getTime() - logDate.getTime()) > 60000)) {
                BeanHelper.getLogService().addLogEntry(LogEntry.createLoc(logObject, null, creatorName, nodeRef, description), logDate, idPrefix, logTableSequence++);
                copiedLogEntries.append(result);
            } else {
                deletedLogEntries.append(result);
            }
            logNodesToDelete.add(logRef);
            logCount++;
        }
        return new String[] { copiedLogEntries.toString(), deletedLogEntries.toString() };
    }

    @Override
    protected void executeUpdater() throws Exception {
        DateFormat idDateFormat = new SimpleDateFormat("yyyyMMdd");
        Date idPrefixDate = BeanHelper.getLogService().getFirstLogEntryDate();
        if (idPrefixDate == null) {
            idPrefixDate = new Date();
        }
        idPrefixDate = DateUtils.addDays(idPrefixDate, -1);
        idPrefix = idDateFormat.format(idPrefixDate);

        super.executeUpdater();

        final Iterator<NodeRef> logNodeRefIterator = logNodesToDelete.iterator();
        log.info("Starting to delete " + logNodesToDelete.size() + " log nodes.");
        int nodesRemaining = logNodesToDelete.size();
        while (logNodeRefIterator.hasNext()) {
            int nodesDeleted = BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Integer>() {

                @Override
                public Integer execute() throws Throwable {
                    int transactionCommitCounter = 0;
                    while (logNodeRefIterator.hasNext() && transactionCommitCounter < 30) {
                        NodeRef logRef = logNodeRefIterator.next();
                        try {
                            nodeService.deleteNode(logRef);
                        } catch (IllegalArgumentException e) {
                            // continue deleting succeeding nodeRefs in new transaction
                            log.error("Failed to delete log, skipping and continuing. Skipped nodeRef=" + logRef);
                            return ++transactionCommitCounter;
                        }
                        transactionCommitCounter++;
                    }
                    return transactionCommitCounter;
                }
            }, false, true);
            nodesRemaining -= nodesDeleted;
            log.info("Deleted or skipped " + nodesDeleted + " log nodes, " + nodesRemaining + " nodes remaining.");
        }
    }

}
