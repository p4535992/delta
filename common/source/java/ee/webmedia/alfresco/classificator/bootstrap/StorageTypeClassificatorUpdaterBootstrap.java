package ee.webmedia.alfresco.classificator.bootstrap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.classificator.model.ClassificatorModel;
import ee.webmedia.alfresco.common.service.GeneralService;

/**
 * Modifies the classificator so user can change values
 */
public class StorageTypeClassificatorUpdaterBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    private static final String STORAGE_TYPE_CLASSIFICATOR_PATH = "cl:classificators/cl:storageType";

    private GeneralService generalService;
    private NodeService nodeService;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Executing " + getName());
        final NodeRef nodeRef = generalService.getNodeRef(STORAGE_TYPE_CLASSIFICATOR_PATH);
        if (nodeRef != null) {
            Map<QName, Serializable> props = new HashMap<QName, Serializable>(2);
            props.put(ClassificatorModel.Props.ADD_REMOVE_VALUES, Boolean.TRUE);
            props.put(ClassificatorModel.Props.DELETE_ENABLED, Boolean.FALSE);
            nodeService.addProperties(nodeRef, props);
        }
    }

    // START: getters / setters

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    // END: getters / setters
}