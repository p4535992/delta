package ee.webmedia.mobile.alfresco.workflow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.mobile.alfresco.util.Util;
import ee.webmedia.mobile.alfresco.workflow.model.Task;

/**
 * Maps result sets to {@link ee.webmedia.mobile.alfresco.workflow.model.Task} objects used in lists.
 * @param <T> For WorkflowDbService compatibility, use {@link ee.webmedia.mobile.alfresco.workflow.model.Task}
 */
public class TaskRowMapper<T> implements ParameterizedRowMapper<Task> {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(TaskRowMapper.class);

    private final MessageSource messageSource;
    private GeneralService generalService;
    private NodeService nodeService;
    private WorkflowService workflowService;
    private DocumentAdminService documentAdminService;

    // Every row mapper instance has it's own cache so we don't have to worry about invalidation
    private final Map<String, String> docTypeCache = new HashMap<String, String>();

    public TaskRowMapper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public Task mapRow(ResultSet rs, int i) throws SQLException {
        Task task = new Task();

        // Set common properties
        task.setId(rs.getString("task_id"));
        task.setResolution(rs.getString("wfs_resolution"));
        task.setCreatorName(rs.getString("wfc_creator_name"));
        task.setDueDate(rs.getTimestamp("wfs_due_date"));
        task.setType(QName.createQName(WorkflowSpecificModel.URI, rs.getString("task_type")));
        task.setViewedByOwner(rs.getBoolean("wfc_viewed_by_owner"));

        // Map specific properties
        String compoundWorkflowType = rs.getString("wfs_searchable_compound_workflow_type");
        if (WorkflowSpecificModel.Types.LINKED_REVIEW_TASK.getLocalName().equals(rs.getString("task_type"))) {
            mapLinkedReview(rs, task);
        } else if (CompoundWorkflowType.DOCUMENT_WORKFLOW.toString().equals(compoundWorkflowType)) {
            mapDocumentWorkflow(rs, task);
        } else if (CompoundWorkflowType.INDEPENDENT_WORKFLOW.toString().equals(compoundWorkflowType)) {
            mapIndependentWorkflow(rs, task);
        } else if (CompoundWorkflowType.CASE_FILE_WORKFLOW.toString().equals(compoundWorkflowType)) {
            mapCaseFileWorkflow(rs, task);
        }

        return task;
    }

    private void mapCaseFileWorkflow(ResultSet rs, Task task) throws SQLException {
        NodeRef caseFileRef = getGeneralService().getAncestorNodeRefWithType(getWorkflowNodeRef(rs), CaseFileModel.Types.CASE_FILE);
        task.setTitle((String) getNodeService().getProperty(caseFileRef, DocumentDynamicModel.Props.DOC_TITLE));
        task.setKind(Util.translate(messageSource, "workflow.task.kind.caseFileWorkflow"));

    }

    private void mapIndependentWorkflow(ResultSet rs, Task task) throws SQLException {
        task.setTitle(rs.getString("wfs_compound_workflow_title"));
        NodeRef compoundWorkflowNodeRef = getCompoundWorkflowNodeRef(rs);
        if (compoundWorkflowNodeRef == null) {
            LOG.error("Unable to retrieve compound workflow for task " + task);
            return;
        }
        task.setCompoundWorkflowRef(compoundWorkflowNodeRef);
        // Set task kind
        String kind;
        int docCount = getWorkflowService().getCompoundWorkflowDocumentCount(compoundWorkflowNodeRef);
        if (docCount > 0) {
            kind = Util.translate(messageSource, "workflow.task.kind.independentWorkflow.documents", docCount);
        } else {
            kind = Util.translate(messageSource, "workflow.task.kind.independentWorkflow");
        }
        task.setKind(kind);
    }

    private void mapDocumentWorkflow(ResultSet rs, Task task) throws SQLException {
        NodeRef docRef = getGeneralService().getAncestorNodeRefWithType(getWorkflowNodeRef(rs), DocumentCommonModel.Types.DOCUMENT);
        task.setTitle((String) getNodeService().getProperty(docRef, DocumentCommonModel.Props.DOC_NAME));
        String docType = rs.getString("wfc_document_type");
        task.setKind(getDocTypeName(docType));
        String senderName;
        if (SystematicDocumentType.INCOMING_LETTER.isSameType(docType)) {
            senderName = (String) getNodeService().getProperty(docRef, DocumentDynamicModel.Props.SENDER_PERSON_NAME); // XXX Or DocumentSpecificModel.Props.SENDER_DETAILS_NAME?
        } else {
            senderName = (String) getNodeService().getProperty(docRef, DocumentDynamicModel.Props.OWNER_NAME);
        }
        task.setSenderName(senderName);
    }

    private void mapLinkedReview(ResultSet rs, Task task) throws SQLException {
        task.setTitle(rs.getString("wfs_compound_workflow_title")); // XXX Is this correct column?
    }

    private NodeRef getWorkflowNodeRef(ResultSet rs) throws SQLException {
        StoreRef storeRef = new StoreRef(rs.getString("store_id"));
        String id = rs.getString("workflow_id");
        return new NodeRef(storeRef, id);
    }

    private NodeRef getCompoundWorkflowNodeRef(ResultSet rs) throws SQLException {
        return getGeneralService().getAncestorNodeRefWithType(getWorkflowNodeRef(rs), WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
    }

    private String getDocTypeName(String docType) {
        if (StringUtils.isBlank(docType)) {
            return "";
        }

        if (docTypeCache.containsKey(docType)) {
            return docTypeCache.get(docType);
        }

        DocumentType documentType = getDocumentAdminService().getDocumentType(docType, DocumentAdminService.DONT_INCLUDE_CHILDREN);
        if (documentType == null) {
            return "";
        }

        return documentType.getName();
    }

    private GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = BeanHelper.getGeneralService();
        }
        return generalService;
    }

    private NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = BeanHelper.getNodeService();
        }
        return nodeService;
    }

    private WorkflowService getWorkflowService() {
        if (workflowService == null) {
            workflowService = BeanHelper.getWorkflowService();
        }
        return workflowService;
    }

    public DocumentAdminService getDocumentAdminService() {
        if (documentAdminService == null) {
            documentAdminService = BeanHelper.getDocumentAdminService();
        }
        return documentAdminService;
    }

}
