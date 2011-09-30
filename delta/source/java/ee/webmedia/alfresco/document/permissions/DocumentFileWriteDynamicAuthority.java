package ee.webmedia.alfresco.document.permissions;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.scanned.model.ScannedModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.imap.model.ImapModel;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

public class DocumentFileWriteDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentFileWriteDynamicAuthority.class);
    public static final String BEAN_NAME = "documentFileWriteDynamicAuthority";

    public static final String DOCUMENT_FILE_WRITE_AUTHORITY = "ROLE_DOCUMENT_FILE_WRITE";
    protected WorkflowService workflowService;

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (!dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT)) {
            // log.trace("Node is not of type 'cm:content', type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        NodeRef parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        if (parent == null) {
            log.trace("File does not have a primary parent, type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        QName parentType = nodeService.getType(parent);
        if (isAllowedForFileNotUnderDocument(userName, parent, parentType)) {
            return true;
        }
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

        Boolean additionalCheck = additional(userName, parent);
        if (additionalCheck != null) {
            return additionalCheck;
        }

        if (isDocumentManager()) {
            log.debug("User " + userName + " is a document manager on node, type=" + type);
            return true;
        }
        String ownerId = (String) nodeService.getProperty(parent, DocumentCommonModel.Props.OWNER_ID);
        if (EqualsHelper.nullSafeEquals(ownerId, userName)) {
            log.debug("User " + userName + " matches document ownerId " + ownerId);
            return true;
        }

        log.trace("No conditions met, refusing authority " + getAuthority());
        return false;
    }

    public boolean isAllowedForFileNotUnderDocument(final String userName, NodeRef parent, QName parentType) {
        if (isDocumentManager()) {
            if (ImapModel.Types.IMAP_FOLDER.equals(parentType)) {
                if (ImapModel.Types.ATTACHMENTS.equals(nodeService.getPrimaryParent(parent).getQName())) {
                    log.debug("File is under imap attachments folder and user " + userName + " is document manager, granting authority " + getAuthority());
                    return true;
                }
            }
            NodeRef grandParent = nodeService.getPrimaryParent(parent).getParentRef();
            QName grantParentType = nodeService.getType(grandParent);
            if (ScannedModel.Types.SCANNED.equals(grantParentType)) {
                log.debug("File is under scanned folder and user " + userName + " is document manager, granting authority " + getAuthority());
                return true;
            }
        }
        return false; // not granting, but not jet refusing either
    }

    public Boolean additional(final String userName, NodeRef parent) {
        boolean hasInProgressWorkflow = false;
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
                hasInProgressWorkflow = true;
                // workFlow is in progress
                QName workflowType = nodeService.getType(workflow);
                if (!WorkflowSpecificModel.Types.REVIEW_WORKFLOW.equals(workflowType) && !WorkflowSpecificModel.Types.SIGNATURE_WORKFLOW.equals(workflowType)) {
                    continue;
                }
                // is review or signature task
                boolean firstTask = true;
                for (ChildAssociationRef taskAssoc : nodeService.getChildAssocs(workflow, WorkflowCommonModel.Assocs.TASK, WorkflowCommonModel.Assocs.TASK)) {
                    NodeRef task = taskAssoc.getChildRef();
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
                        return true;
                    }
                }
                break; // only one workflow can be in progress under a compoundWorkflow
            }
        }

        if (hasInProgressWorkflow) {
            log.trace("Document has in-progress workflows, refusing authority " + getAuthority());
            return false;
        }

        if (!StringUtils.equals(DocumentStatus.WORKING.getValueName(), (String) nodeService.getProperty(parent, DocumentCommonModel.Props.DOC_STATUS))) {
            DocumentService documentService = BeanHelper.getDocumentService();
            Node docNode = documentService.getDocument(parent);
            documentService.throwIfNotDynamicDoc(docNode);
            String docTypeId = (String) docNode.getProperties().get(Props.OBJECT_TYPE_ID);
            if (!getDocumentAdminService().getDocumentType(docTypeId).isEditFilesOfFinishedDocEnabled()) {
                log.trace("Document status is not working, refusing authority " + getAuthority());
                return false;
            }
        }
        return null; // not granting, but not jet refusing either
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
