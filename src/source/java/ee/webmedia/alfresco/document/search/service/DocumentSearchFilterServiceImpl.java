package ee.webmedia.alfresco.document.search.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.search.model.DocumentSearchModel;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * @author Alar Kvell
 */
public class DocumentSearchFilterServiceImpl implements DocumentSearchFilterService {

    private GeneralService generalService;
    private NodeService nodeService;

    @Override
    public Node createOrSaveFilter(Node filter) {
        Map<QName, Serializable> props = generalService.getPropertiesIgnoringSystem(filter.getProperties());
        NodeRef nodeRef = filter.getNodeRef();
        if (filter instanceof TransientNode) {
            nodeRef = createFilter(props);
        } else {
            nodeService.setProperties(nodeRef, props);
        }
        return getFilter(nodeRef);
    }

    private NodeRef createFilter(Map<QName, Serializable> props) {
        return nodeService.createNode(getRoot(), DocumentSearchModel.Assocs.FILTER, DocumentSearchModel.Assocs.FILTER, DocumentSearchModel.Types.FILTER,
                props).getChildRef();
    }

    @Override
    public Node createFilter(Node filter) {
        Map<QName, Serializable> props = generalService.getPropertiesIgnoringSystem(filter.getProperties());
        NodeRef nodeRef = createFilter(props);
        return getFilter(nodeRef);
    }

    @Override
    public Node getFilter(NodeRef filter) {
        return generalService.fetchNode(filter);
    }

    @Override
    public Map<NodeRef, String> getFilters() {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(getRoot());
        Map<NodeRef, String> filters = new HashMap<NodeRef, String>(childAssocs.size());
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef nodeRef = childAssoc.getChildRef();
            String name = (String) nodeService.getProperty(nodeRef, DocumentSearchModel.Props.NAME);
            filters.put(nodeRef, name);
        }
        return filters;
    }

    @Override
    public void deleteFilter(NodeRef filter) {
        nodeService.deleteNode(filter);
    }

    private NodeRef getRoot() {
        return generalService.getNodeRef(DocumentSearchModel.Repo.FILTERS_SPACE);
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
