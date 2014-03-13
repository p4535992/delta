package ee.webmedia.alfresco.docdynamic.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Fixes invalid properties belonging to different document type that are not removed when changing document type.
 */
public class DocumentChangedTypePropertiesUpdater extends AbstractNodeUpdater {

    public static final String BEAN_NAME = "documentChangedTypePropertiesUpdater";

    @Override
    protected boolean usePreviousState() {
        return false;
    }

    @Override
    protected Set<NodeRef> loadNodesFromRepo() throws Exception {
        log.info("Searching nodes from database");
        Set<NodeRef> nodeSet = new HashSet<NodeRef>(BeanHelper.getLogService().getDocumentsWithImapImportLog());
        nodeSet.remove(null);
        List<NodeRef> resolvedNodeRefs = new ArrayList<NodeRef>();
        for (Iterator<NodeRef> i = nodeSet.iterator(); i.hasNext();) {
            NodeRef nodeRef = i.next();
            if (!nodeService.exists(nodeRef)) {
                i.remove();
                nodeRef = generalService.getExistingNodeRefAllStores(nodeRef.getId());
                if (nodeRef != null) {
                    resolvedNodeRefs.add(nodeRef);
                }
            }
        }
        nodeSet.addAll(resolvedNodeRefs);
        log.info("Loaded total " + nodeSet.size() + " nodes from repository");
        return nodeSet;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        // not used in this updater
        return null;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName type = nodeService.getType(nodeRef);
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            return new String[] { "Type is not document, but " + type + ", skipping node" };
        }
        if (generalService.getAncestorNodeRefWithType(nodeRef, SeriesModel.Types.SERIES) == null) {
            return new String[] { getNotSeriesChildMsg(nodeRef) };
        }
        DocumentDynamic document = BeanHelper.getDocumentDynamicService().getDocument(nodeRef);
        return updateNode(nodeRef, document);
    }

    protected String[] updateNode(NodeRef nodeRef, DocumentDynamic document) {
        Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions = BeanHelper.getDocumentConfigService().getPropertyDefinitions(document.getNode());
        Set<String> typeVersionFields = propertyDefinitions.keySet();
        List<QName> propsToNull = new ArrayList<QName>();
        for (Map.Entry<QName, Serializable> entry : RepoUtil.toQNameProperties(document.getNode().getProperties()).entrySet()) {
            QName propQName = entry.getKey();
            String localName = propQName.getLocalName();
            if (DocumentDynamicModel.URI.equals(propQName.getNamespaceURI()) && (!typeVersionFields.contains(localName) || isChildNodeProperty(propertyDefinitions, localName))
                    && entry.getValue() != null) {
                nodeService.removeProperty(nodeRef, propQName);
                propsToNull.add(propQName);
            }
        }
        return new String[] { "Removed properties: " + StringUtils.join(propsToNull, ", ") };
    }

    protected String getNotSeriesChildMsg(NodeRef nodeRef) {
        return "Document is not saved under series, parent= " + generalService.getPrimaryParent(nodeRef) + ", skipping node";
    }

    private boolean isChildNodeProperty(Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions, String localName) {
        Pair<DynamicPropertyDefinition, Field> propDefAndField = propertyDefinitions.get(localName);
        if (propDefAndField == null) {
            return false;
        }
        QName[] childAssocTypeQNAmeHierarchy = propDefAndField.getFirst().getChildAssocTypeQNameHierarchy();
        return childAssocTypeQNAmeHierarchy != null && childAssocTypeQNAmeHierarchy.length > 0;
    }
}
