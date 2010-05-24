package ee.webmedia.alfresco.document.permissions;

import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.DECREE;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.PERSONELLE_ORDER_SIM;
import static ee.webmedia.alfresco.document.model.DocumentSubtypeModel.Types.REGULATION;

import java.util.Arrays;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

public class DocumentFileWriteDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentFileWriteDynamicAuthority.class);

    /**
     * List of document types that must be opened without ability to modify document with webdav if document status is FINISHED
     */
    public static final List<QName> downloadFilesReadOnlyDocTypes = Arrays.asList(PERSONELLE_ORDER_SIM, REGULATION, DECREE);
    public static final String DOCUMENT_FILE_WRITE_AUTHORITY = "ROLE_DOCUMENT_FILE_WRITE";
    protected WorkflowService workflowService;

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (!dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT)) {
            log.trace("Node is not of type 'cm:content', type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        if (parent == null) {
            log.trace("File does not have a primary parent, type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        QName parentType = nodeService.getType(parent);
        if (!dictionaryService.isSubClass(parentType, DocumentCommonModel.Types.DOCUMENT)) {
            log.trace("Node is not of type 'doccom:document', type=" + parentType + ", refusing authority " + getAuthority());
            return false;
        }
        if (!nodeService.hasAspect(parent, DocumentCommonModel.Aspects.OWNER)) {
            log.warn("Document does not have " + DocumentCommonModel.Aspects.OWNER + " aspect: type=" + parentType
                    + " nodeRef=" + parent + ", refusing authority"
                    + getAuthority());
            return false;
        }
        if (isDocumentManager()) {
            log.debug("User " + userName + " is a document manager on node, type=" + type);
            return isWriteAllowedByStatusAndDocType(parentType, parent);
        }
        String ownerId = (String) nodeService.getProperty(parent, DocumentCommonModel.Props.OWNER_ID);
        if (EqualsHelper.nullSafeEquals(ownerId, userName)) {
            log.debug("User " + userName + " matches document ownerId " + ownerId);
            return isWriteAllowedByStatusAndDocType(parentType, parent);
        }
        for (ChildAssociationRef compoundWorkflowAssoc : nodeService.getChildAssocs(parent, WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
                WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW)) {
            NodeRef compoundWorkflow = compoundWorkflowAssoc.getChildRef();
            if (!Status.IN_PROGRESS.equals((String) nodeService.getProperty(compoundWorkflow, WorkflowCommonModel.Props.STATUS))) {
                continue;
            }
            for (ChildAssociationRef workflowAssoc : nodeService.getChildAssocs(compoundWorkflow, WorkflowCommonModel.Assocs.WORKFLOW,
                    WorkflowCommonModel.Assocs.WORKFLOW)) {
                NodeRef workflow = workflowAssoc.getChildRef();
                if (!Status.IN_PROGRESS.equals((String) nodeService.getProperty(workflow, WorkflowCommonModel.Props.STATUS))) {
                    continue;
                }
                // workFlow is in progress
                QName workflowType = nodeService.getType(workflow);
                if (!WorkflowSpecificModel.Types.REVIEW_WORKFLOW.equals(workflowType) && !WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(workflowType)) {
                    continue;
                }
                // neither review nor signature task
                boolean firstTask = true;
                for (ChildAssociationRef taskAssoc : nodeService.getChildAssocs(workflow, WorkflowCommonModel.Assocs.TASK, WorkflowCommonModel.Assocs.TASK)) {
                    NodeRef task = taskAssoc.getChildRef();
                    // FIXME: never true: WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(workflowType)
                    if (WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(workflowType) && !firstTask) {
                        break; // only first signatureTask is considered; all reviewTasks are considered
                    }
                    firstTask = false;
                    if (!Status.IN_PROGRESS.equals((String) nodeService.getProperty(task, WorkflowCommonModel.Props.STATUS))) {
                        continue;
                    }
                    // task is in progress
                    if (userName.equals(nodeService.getProperty(task, WorkflowCommonModel.Props.OWNER_ID))) {
                        log.debug("User " + userName + " is owner of in-progress task of workflow '" + workflowType.toPrefixString(namespaceService)
                                + "', granting authority " + getAuthority());
                        // user is the owner of the task
                        return isWriteAllowedByStatusAndDocType(parentType, parent);
                    }
                }
                break; // only one workflow can be in progress under a compoundWorkflow
            }
        }
        log.trace("No conditions met, refusing authority " + getAuthority());
        return false;
    }

    private boolean isWriteAllowedByStatusAndDocType(QName parentType, NodeRef docRef) {
        if (downloadFilesReadOnlyDocTypes.contains(parentType)
                && StringUtils.equals(DocumentStatus.FINISHED.getValueName(), (String) nodeService.getProperty(docRef, DocumentCommonModel.Props.DOC_STATUS))) {
            return false;
        }
        if (workflowService.hasInprogressCompoundWorkflows(docRef)) {
            return false;
        }
        log.debug("granting authority " + getAuthority());
        return true;
    }

    @Override
    public String getAuthority() {
        return DOCUMENT_FILE_WRITE_AUTHORITY;
    }

    // START: getters / setters
    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }
    // END: getters / setters
}
