package ee.webmedia.alfresco.filter.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.filter.model.FilterVO;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Base class for filtering services
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> develop-5.1
 */
public abstract class AbstractFilterServiceImpl implements FilterService {

    protected GeneralService generalService;
    protected NodeService nodeService;
    protected UserService userService;
    protected DictionaryService dictionaryService;

    @Override
    public Node createOrSaveFilter(Node filter, boolean isPrivate) {
        NodeRef filterRef = filter.getNodeRef();
        final Map<String, Object> properties = filter.getProperties();
        final String name = (String) properties.get(getFilterNameProperty());
        Assert.isTrue(StringUtils.isNotBlank(name), "filter name is empty");
        Map<QName, Serializable> props = generalService.getPropertiesIgnoringSystem(properties);
        changeLocationIfNeeded(filter, isPrivate);
        if (filter instanceof TransientNode) {
            filterRef = createFilter(props, isPrivate);
        } else {
            nodeService.setProperties(filterRef, props);
        }
        return getFilter(filterRef);
    }

    @Override
    public Node createFilter(Node filter, boolean isPrivate) {
        Map<QName, Serializable> props = generalService.getPropertiesIgnoringSystem(filter.getProperties());
        NodeRef nodeRef = createFilter(props, isPrivate);
        return getFilter(nodeRef);
    }

    @Override
    public void deleteFilter(NodeRef filter) {
        nodeService.deleteNode(filter);
    }

    @Override
    public Node getFilter(NodeRef filter) {
        Node filterNode = generalService.fetchNode(filter);
        initFilterProperties(filterNode.getProperties());
        return filterNode;
    }

    // add empty list as initial value to properties
    // that may potentially be connected to multiselect controls
    @SuppressWarnings("rawtypes")
    private void initFilterProperties(Map<String, Object> properties) {
        Map<QName, PropertyDefinition> propDefs = BeanHelper.getDictionaryService().getPropertyDefs(getFilterNodeType());
        Map<String, Object> propsToAdd = new HashMap<String, Object>();
        for (Map.Entry<QName, PropertyDefinition> entry : propDefs.entrySet()) {
            PropertyDefinition propDef = entry.getValue();
            if (propDef.isMultiValued()) {
                String propName = propDef.getName().toString();
                Object value = properties.get(propName);
                if (value == null) {
                    propsToAdd.put(propName, new ArrayList<Object>());
                }
            }
        }
        properties.putAll(propsToAdd);
    }

    @Override
    public List<FilterVO> getFilters() {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(getRoot());
        List<FilterVO> filters = new ArrayList<FilterVO>(childAssocs.size());
        addFilters(childAssocs, filters, false);
        NodeRef userRef = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        if (nodeService.hasAspect(userRef, getFilterContainerAspect())) {
            addFilters(nodeService.getChildAssocs(userRef, getContainerToFilterAssoc(), getContainerToFilterAssoc()), filters, true);
        }
        return filters;
    }

    private void addFilters(List<ChildAssociationRef> childAssocs, List<FilterVO> filters, boolean isPrivate) {
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef nodeRef = childAssoc.getChildRef();
            String name = (String) nodeService.getProperty(nodeRef, getFilterNameProperty());
            filters.add(new FilterVO(nodeRef, name, isPrivate));
        }
    }

    /**
     * @return true if location was changed
     */
    private boolean changeLocationIfNeeded(Node filter, boolean isPrivate) {
        if (filter instanceof TransientNode) {
            return false;
        }
        NodeRef filterRef = filter.getNodeRef();
        final NodeRef filterParentRef = nodeService.getPrimaryParent(filterRef).getParentRef();
        final boolean wasPrivate = !getRoot().equals(filterParentRef);
        NodeRef newFilterLocationRef = null;
        if ((wasPrivate && !isPrivate) || (isPrivate && !wasPrivate)) {
            final NodeRef filterNewContainer = isPrivate ? getPrivateFilterContainer() : getRoot();
            newFilterLocationRef = nodeService.moveNode(filterRef, filterNewContainer, getContainerToFilterAssoc(), getContainerToFilterAssoc()).getChildRef();
            Assert.isTrue(newFilterLocationRef.equals(filterRef), "Wou, noderef has actually changed while changing filter location!. Old ref = '" //
                    + filterRef + "'; newRef=" + newFilterLocationRef);
        }
        return newFilterLocationRef != null;
    }

    private NodeRef getPrivateFilterContainer() {
        NodeRef userRef = userService.getUser(AuthenticationUtil.getRunAsUser()).getNodeRef();
        if (!nodeService.hasAspect(userRef, getFilterContainerAspect())) {
            nodeService.addAspect(userRef, getFilterContainerAspect(), null);
        }
        return userRef;
    }

    private NodeRef createFilter(Map<QName, Serializable> props, boolean isPrivate) {
        final NodeRef filterParentNode;
        if (isPrivate) {
            filterParentNode = getPrivateFilterContainer();
        } else {
            filterParentNode = getRoot();
        }
        return nodeService.createNode(filterParentNode, getContainerToFilterAssoc() //
                , getContainerToFilterAssoc(), getFilterNodeType(), props).getChildRef();
    }

    // START: getters / setters
    abstract protected QName getContainerToFilterAssoc();

    abstract protected QName getFilterContainerAspect();

    abstract protected NodeRef getRoot();

    abstract protected QName getFilterNodeType();

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }
    // END: getters / setters

}
