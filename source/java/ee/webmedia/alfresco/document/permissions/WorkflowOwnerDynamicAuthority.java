package ee.webmedia.alfresco.document.permissions;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;

import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class WorkflowOwnerDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(WorkflowOwnerDynamicAuthority.class);

    public static final String WORKFLOW_OWNER_AUTHORITY = "ROLE_WORKFLOW_OWNER";

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        NodeRef compoundWorkflow = null;
        if (type.equals(WorkflowCommonModel.Types.COMPOUND_WORKFLOW)) {
            compoundWorkflow = nodeRef;
        } else if (dictionaryService.isSubClass(type, WorkflowCommonModel.Types.WORKFLOW)) {
            compoundWorkflow = nodeService.getPrimaryParent(nodeRef).getParentRef();
        } else if (dictionaryService.isSubClass(type, WorkflowCommonModel.Types.TASK)) {
            NodeRef workflow = nodeService.getPrimaryParent(nodeRef).getParentRef();
            if (workflow != null) {
                compoundWorkflow = nodeService.getPrimaryParent(workflow).getParentRef();
            }
        } else {
            log.trace("Node is not related to workflow, type=" + type + ", refusing authority " + getAuthority());
            return false;
        }
        if (compoundWorkflow == null) {
            log.warn("Workflow node does not have parent compoundWorkflow, type=" + type + ", nodeRef=" + nodeRef + ", refusing authority " + getAuthority());
            return false;
        }
        if (isDocumentManager()) {
            log.debug("User " + userName + " is a document manager on workflow node, type=" + type + ", granting authority " + getAuthority());
            return true;
        }
        String ownerId = (String) nodeService.getProperty(compoundWorkflow, WorkflowCommonModel.Props.OWNER_ID);
        if (EqualsHelper.nullSafeEquals(ownerId, userName)) {
            log.debug("User " + userName + " matches compoundWorkflow ownerId " + ownerId + ", granting authority " + getAuthority());
            return true;
        }
        log.trace("User " + userName + " does not match compoundWorkflow ownerId " + ownerId + ", refusing authority " + getAuthority());
        return false;
    }

    @Override
    public String getAuthority() {
        return WORKFLOW_OWNER_AUTHORITY;
    }

}
