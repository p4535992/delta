<<<<<<< HEAD
package ee.webmedia.alfresco.common.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * Deletes node by finding it based on <code>nodeXPath</code> from workspace://SpacesStore
 * 
 * @author Ats Uiboupin
 */
public class DeleteNodeByXPathBootstrap extends AbstractModuleComponent {
    private GeneralService generalService;
    private NodeService nodeService;
    private String nodeXPath;

    @Override
    protected void executeInternal() throws Throwable {
        final NodeRef nodeRef = generalService.getNodeRef(nodeXPath);
        if (nodeRef != null) {
            nodeService.deleteNode(nodeRef);
        }
    }

    // START: getters / setters
    public void setNodeXPath(String nodeXPath) {
        this.nodeXPath = nodeXPath;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    // END: getters / setters
}
=======
package ee.webmedia.alfresco.common.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * Deletes node by finding it based on <code>nodeXPath</code> from workspace://SpacesStore
 */
public class DeleteNodeByXPathBootstrap extends AbstractModuleComponent {
    private GeneralService generalService;
    private NodeService nodeService;
    private String nodeXPath;

    @Override
    protected void executeInternal() throws Throwable {
        final NodeRef nodeRef = generalService.getNodeRef(nodeXPath);
        if (nodeRef != null) {
            nodeService.deleteNode(nodeRef);
        }
    }

    // START: getters / setters
    public void setNodeXPath(String nodeXPath) {
        this.nodeXPath = nodeXPath;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    // END: getters / setters
}
>>>>>>> develop-5.1
