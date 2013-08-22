package ee.webmedia.alfresco.document.permissions;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

/**
 * @author Alar Kvell
 */
public class WorkflowUserDynamicAuthority extends BaseDynamicAuthority {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(WorkflowUserDynamicAuthority.class);

    public static final String WORKFLOW_USER_AUTHORITY = "ROLE_WORKFLOW_USER";

    @Override
    public boolean hasAuthority(final NodeRef nodeRef, final String userName) {
        QName type = nodeService.getType(nodeRef);
        if (type.equals(WorkflowCommonModel.Types.COMPOUND_WORKFLOW) || dictionaryService.isSubClass(type, WorkflowCommonModel.Types.WORKFLOW)
                || dictionaryService.isSubClass(type, WorkflowCommonModel.Types.TASK)) {
            log.debug("Node is related to workflow, type=" + type + ", granting authority " + getAuthority());
            return true;
        }
        log.trace("Node is not related to workflow, type=" + type + ", refusing authority " + getAuthority());
        return false;
    }

    @Override
    public String getAuthority() {
        return WORKFLOW_USER_AUTHORITY;
    }

}
