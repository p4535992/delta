package ee.webmedia.alfresco.document.propmodifiers;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.classificator.enums.StorageType;
import ee.webmedia.alfresco.document.model.DocChildAssocInfoHolder;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService.PropertiesModifierCallback;
import ee.webmedia.alfresco.document.service.InMemoryChildNodeHelper;

/**
 * Callback that creates childAssociations to document
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
            final List<DocChildAssocInfoHolder> assocInfos = getDocChildAssocInfo(docNode);
            for (DocChildAssocInfoHolder assocInfo : assocInfos) {
                parentRef = createNode(parentRef, assocInfo);
            }
        } else if (StringUtils.equals(phase, "docTypeChangeing")) {
            createChildNodes(docNode);
        }
    }

    protected NodeRef createNode(NodeRef parentRef, DocChildAssocInfoHolder docChildAssocInfo) {
        final QName assoc = docChildAssocInfo.getAssocType();
        final QName childType = docChildAssocInfo.getAssocTargetType();
        final Map<QName, Serializable> properties = docChildAssocInfo.getProperties();
        return nodeService.createNode(parentRef, assoc, assoc, childType, properties).getChildRef();
    }

    protected abstract List<DocChildAssocInfoHolder> getDocChildAssocInfo(Node docNode);

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
