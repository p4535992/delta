package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Find documents that
 * 1) belong to independent compound workflows finished during this year
 * 2) and have log record reading "Dokumenti ei saa lõpetada, kuna dokument on avatud muutmiseks null poolt."
 * 3) and don't have log record "Dokument taasavatud" created later than the previous log record
 * 4) and which are in status is "Avatud"
 * 5) and that don't belong to other in process compound workflows.
 * I.e. close documents that should have been closed by closing compound workflow ant that are not opened by hand later than closing compound wokrflow.
 * See https://jira.nortal.com/browse/SIMDHS-4068 for details.
 */
public class FinishUnfinshedDocumentsInCompWorkflows extends AbstractNodeUpdater {

    private static final String DOC_FINISH_FAILURE_LOG_SQL = "SELECT MAX(created_date_time) FROM delta_log " +
            "where description='Dokumenti ei saa lõpetada, kuna dokument on avatud muutmiseks null poolt.'" +
            "AND object_id=?";
    private static final String DOC_REOPEN_SUCCESS_LOG_SQL = "SELECT MAX(created_date_time) FROM delta_log " +
            "where description='Dokument taasavatud'" +
            "AND object_id=?";
    private WorkflowService workflowService;
    private DocumentService documentService;
    private SimpleJdbcTemplate jdbcTemplate;
    private final Set<NodeRef> processedDocuments = new HashSet<NodeRef>();

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR), 1, 1, 0, 0, 0);
        String query = SearchUtil.joinQueryPartsAnd(SearchUtil.generateAndNotQuery(SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW),
                SearchUtil.generateTypeQuery(WorkflowCommonModel.Types.COMPOUND_WORKFLOW_DEFINITION)),
                SearchUtil.generatePropertyExactQuery(WorkflowCommonModel.Props.TYPE, CompoundWorkflowType.INDEPENDENT_WORKFLOW.name()),
                SearchUtil.generateDatePropertyRangeQuery(calendar.getTime(), new Date(), WorkflowCommonModel.Props.FINISHED_DATE_TIME),
                SearchUtil.generatePropertyExactQuery(WorkflowCommonModel.Props.STATUS, Status.FINISHED.getName()));
        List<ResultSet> result = new ArrayList<ResultSet>(6);
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            result.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef compoundWorkflowRef) throws Exception {
        List<NodeRef> documentRefs = workflowService.getCompoundWorkflowDocumentRefs(compoundWorkflowRef);
        if (documentRefs.isEmpty()) {
            return new String[] { "no documents" };
        }
        List<String> finishedDocumentRefs = new ArrayList<String>();
        DOCUMENT: for (NodeRef documentRef : documentRefs) {
            if (processedDocuments.contains(documentRef)) {
                continue;
            }
            processedDocuments.add(documentRef);
            String docStatus = (String) nodeService.getProperty(documentRef, DocumentCommonModel.Props.DOC_STATUS);
            if (DocumentStatus.FINISHED.equals(docStatus)) {
                continue;
            }
            Date maxFinishFailedDate = jdbcTemplate.queryForObject(DOC_FINISH_FAILURE_LOG_SQL, Date.class, documentRef.toString());
            if (maxFinishFailedDate == null) {
                continue;
            }
            Date maxReopenSucceededDate = jdbcTemplate.queryForObject(DOC_REOPEN_SUCCESS_LOG_SQL, Date.class, documentRef.toString());
            if (maxReopenSucceededDate != null && maxReopenSucceededDate.after(maxFinishFailedDate)) {
                continue;
            }
            List<NodeRef> connectedCompoundWorkflowRefs = BeanHelper.getDocumentAssociationsService().getDocumentIndependentWorkflowAssocs(documentRef);
            for (NodeRef connectedCompoundWorkflowRef : connectedCompoundWorkflowRefs) {
                String compoundWorkflowStatus = (String) nodeService.getProperty(connectedCompoundWorkflowRef, WorkflowCommonModel.Props.STATUS);
                if (Status.IN_PROGRESS.equals(compoundWorkflowStatus)) {
                    continue DOCUMENT;
                }
            }
            documentService.endDocument(documentRef);
            finishedDocumentRefs.add(documentRef.toString());
        }
        return new String[] { TextUtil.joinNonBlankStringsWithComma(finishedDocumentRefs) };
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }
}
