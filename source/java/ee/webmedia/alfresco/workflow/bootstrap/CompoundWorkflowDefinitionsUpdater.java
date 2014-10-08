package ee.webmedia.alfresco.workflow.bootstrap;

import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.service.WorkflowService;

/**
 * Fill compoundWorkflowDefinitionsCache
 */
public class CompoundWorkflowDefinitionsUpdater extends AbstractModuleComponent {

    public static final String BEAN_NAME = "CompoundWorkflowDefinitionsUpdater";
    private static final Log LOG = LogFactory.getLog(CompoundWorkflowDefinitionsUpdater.class);

    private WorkflowService workflowService;
    private NodeService nodeService;
    private GeneralService generalService;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("CompoundWorkflowDefinitionsUpdater started.");
        initCompoundWorkflowsSharedCache();
        LOG.info("CompoundWorkflowDefinitionsUpdater finished.");
    }

    private void initCompoundWorkflowsSharedCache() {
        NodeRef rootRef = generalService.getNodeRef(WorkflowCommonModel.Repo.WORKFLOWS_SPACE);
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(rootRef);
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef nodeRef = childAssoc.getChildRef();
            workflowService.getCompoundWorkflowDefinition(nodeRef, rootRef);
        }
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

}
