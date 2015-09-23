package ee.webmedia.alfresco.parameters.bootstrap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.parameters.model.ParametersModel;
import ee.webmedia.alfresco.parameters.model.ParametersModel.Repo;

public class AddNamePropertyToParametersBootstrap extends AbstractModuleComponent {

    private GeneralService generalService;
    private NodeService nodeService;

    @Override
    protected void executeInternal() throws Throwable {
        final NodeRef parametersRootNodeRef = generalService.getNodeRef(Repo.PARAMETERS_SPACE);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(parametersRootNodeRef);
        for (ChildAssociationRef ref : childRefs) {
            final String paramName = ref.getQName().getLocalName();
            NodeRef paramNodeRef = ref.getChildRef();
            Serializable name = nodeService.getProperty(paramNodeRef, ParametersModel.Props.Parameter.NAME);
            if (name != null) {
                continue;
            }
            Map<QName, Serializable> props = new HashMap<>();
            props.put(ParametersModel.Props.Parameter.NAME, paramName);
            nodeService.addProperties(paramNodeRef, props);
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
