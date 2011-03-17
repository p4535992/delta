package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService.PropertiesModifierCallback;
import ee.webmedia.alfresco.document.service.InMemoryChildNodeHelper;

/**
 * Callback that creates childAssociations to document
 * 
 * @author Ats Uiboupin
 */
public abstract class AbstractDocChildCreator extends PropertiesModifierCallback {
    protected NodeService nodeService;
    protected InMemoryChildNodeHelper inMemoryChildNodeHelper;

    @Override
    public void doWithProperties(Map<QName, Serializable> properties) {
        properties.put(DocumentCommonModel.Props.STORAGE_TYPE, StorageType.DIGITAL.getValueName());
    }

    @Override
    public void doWithNode(Node docNode, String phase) {
        if (StringUtils.equals(phase, "docConstruction")) {
            NodeRef parentRef = docNode.getNodeRef();
            final List<Pair<QName, QName>> assocTypesAndAssocTargetTypes = getAssocTypesAndAssocTargetTypes();
            for (Pair<QName, QName> assocTypeAndAssocTargetType : assocTypesAndAssocTargetTypes) {
                parentRef = createNode(parentRef, assocTypeAndAssocTargetType, null);
            }
        } else if (StringUtils.equals(phase, "docTypeChangeing")) {
            createChildNodes(docNode);
        }
    }

    protected NodeRef createNode(NodeRef parentRef, Pair<QName, QName> assocTypeAndAssocTargetType, Map<QName, Serializable> properties) {
        final QName assoc = assocTypeAndAssocTargetType.getFirst();
        final QName childType = assocTypeAndAssocTargetType.getSecond();
        return nodeService.createNode(parentRef, assoc, assoc, childType, properties).getChildRef();
    }

    protected abstract List<Pair<QName, QName>> getAssocTypesAndAssocTargetTypes();

    protected void createChildNodes(Node docNode) {
        inMemoryChildNodeHelper.addApplicant(docNode);
    }

    public void setInMemoryChildNodeHelper(InMemoryChildNodeHelper inMemoryChildNodeHelper) {
        this.inMemoryChildNodeHelper = inMemoryChildNodeHelper;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

}
