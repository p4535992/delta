package ee.webmedia.alfresco.document.permissions;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

public class DocumentOwnerDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentOwnerDynamicAuthority.class);

    public static final String DOCUMENT_OWNER_AUTHORITY = "ROLE_DOCUMENT_OWNER";

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (!dictionaryService.isSubClass(type, DocumentCommonModel.Types.DOCUMENT)) {
            log.trace("Node is not of type 'doccom:document': type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        if (!nodeService.hasAspect(nodeRef, DocumentCommonModel.Aspects.OWNER)) {
            log.warn("Document does not have " + DocumentCommonModel.Aspects.OWNER + " aspect, type=" + type + " nodeRef=" + nodeRef + ", refusing authority "
                    + getAuthority());
            return false;
        }
        if (isDocumentManager()) {
            log.debug("User " + userName + " is a document manager on node, type=" + type + ", granting authority " + getAuthority());
            return true;
        }
        String ownerId = (String) nodeService.getProperty(nodeRef, DocumentCommonModel.Props.OWNER_ID);
        if (EqualsHelper.nullSafeEquals(ownerId, userName)) {
            log.debug("User " + userName + " matches document ownerId " + ownerId + ", granting authority " + getAuthority());
            return true;
        }
        if (DocumentSubtypeModel.Types.LEAVING_LETTER.equals(type)) {
            return false; // no need to check isOwnerOfFirstInprogressSignatureTask() for leavingLetter
        }
        final boolean hasAuthority = isOwnerOfInprogressReviewOrFirstSignatureTask(nodeRef, userName);
        if (!hasAuthority) {
            log.trace("No conditions met, refusing authority " + getAuthority());
        }

        // there are some differences between file-write permissions and document-metadata-edit (this) permissions:
        // * document-owner and document-manager can edit metadata when any workflow is in progress
        // document-owner and document-manager can edit files when he is reviewer or first signer
        // * document-owner and document-manager can't edit files when status=finished and docType=PERSONELLE_ORDER_SIM, REGULATION, DECREE
        // document-owner and document-manager can edit metadata even if the above is true

        return hasAuthority;
    }

    private boolean isOwnerOfInprogressReviewOrFirstSignatureTask(final NodeRef nodeRef, final String userName) {
        for (ChildAssociationRef compoundWorkflowAssoc : nodeService.getChildAssocs(nodeRef, WorkflowCommonModel.Assocs.COMPOUND_WORKFLOW,
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
        return false;
    }

    @Override
    public String getAuthority() {
        return DOCUMENT_OWNER_AUTHORITY;
    }

}
