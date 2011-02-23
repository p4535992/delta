package ee.webmedia.alfresco.document.permissions;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.workflow.model.Status;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class DocumentFileReadDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentFileReadDynamicAuthority.class);

    public static final String DOCUMENT_FILE_READ_AUTHORITY = "ROLE_DOCUMENT_FILE_READ";

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
            log.warn("Document does not have " + DocumentCommonModel.Aspects.OWNER + " aspect: type=" + parentType + " nodeRef=" + parent + ", refusing authority"
                    + getAuthority());
            return false;
        }
        if (isDocumentManager()) {
            log.debug("User " + userName + " is a document manager on node, type=" + type + ", granting authority " + getAuthority());
            return true;
        }
        String ownerId = (String) nodeService.getProperty(parent, DocumentCommonModel.Props.OWNER_ID);
        if (EqualsHelper.nullSafeEquals(ownerId, userName)) {
            log.debug("User " + userName + " matches document ownerId " + ownerId + ", granting authority " + getAuthority());
            return true;
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
                for (ChildAssociationRef taskAssoc : nodeService.getChildAssocs(workflow, WorkflowCommonModel.Assocs.TASK, WorkflowCommonModel.Assocs.TASK)) {
                    NodeRef task = taskAssoc.getChildRef();
                    if (!Status.IN_PROGRESS.equals((String) nodeService.getProperty(task, WorkflowCommonModel.Props.STATUS))) {
                        continue;
                    }
                    if (userName.equals(nodeService.getProperty(task, WorkflowCommonModel.Props.OWNER_ID))) {
                        log.debug("User " + userName + " is owner of in-progress task '" + nodeService.getType(task).toPrefixString(namespaceService)
                                + "', granting authority " + getAuthority());
                        return true;
                    }
                }
                break; // only one workflow can be in progress under a compoundWorkflow
            }
        }
        log.trace("No conditions met, refusing authority " + getAuthority());
        return false;
    }

    @Override
    public String getAuthority() {
        return DOCUMENT_FILE_READ_AUTHORITY;
    }

}
