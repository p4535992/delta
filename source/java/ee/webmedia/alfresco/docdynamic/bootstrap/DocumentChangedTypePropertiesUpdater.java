<<<<<<< HEAD
package ee.webmedia.alfresco.docdynamic.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
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
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Fixes invalid properties belonging to different document type that are not removed when changing document type.
 * 
 * @author Riina Tens
 */
public class DocumentChangedTypePropertiesUpdater extends AbstractNodeUpdater {

    @Override
    protected boolean usePreviousState() {
        return false;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName type = nodeService.getType(nodeRef);
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            return new String[] { "Type is not document, but " + type + ", skipping node" };
        }
        if (generalService.getAncestorNodeRefWithType(nodeRef, SeriesModel.Types.SERIES) == null) {
            return new String[] { "Document is not saved under series, parent= " + generalService.getPrimaryParent(nodeRef) + ", skipping node" };
        }
        DocumentDynamic document = BeanHelper.getDocumentDynamicService().getDocument(nodeRef);
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

    private boolean isChildNodeProperty(Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions, String localName) {
        Pair<DynamicPropertyDefinition, Field> propDefAndField = propertyDefinitions.get(localName);
        if (propDefAndField == null) {
            return false;
        }
        QName[] childAssocTypeQNAmeHierarchy = propDefAndField.getFirst().getChildAssocTypeQNameHierarchy();
        return childAssocTypeQNAmeHierarchy != null && childAssocTypeQNAmeHierarchy.length > 0;
    }
}
=======
package ee.webmedia.alfresco.docdynamic.bootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
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
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Fixes invalid properties belonging to different document type that are not removed when changing document type.
 */
public class DocumentChangedTypePropertiesUpdater extends AbstractNodeUpdater {

    @Override
    protected boolean usePreviousState() {
        return false;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT);
        List<ResultSet> resultSets = new ArrayList<ResultSet>();
        for (StoreRef storeRef : generalService.getAllStoreRefsWithTrashCan()) {
            resultSets.add(searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query));
        }
        return resultSets;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        QName type = nodeService.getType(nodeRef);
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            return new String[] { "Type is not document, but " + type + ", skipping node" };
        }
        if (generalService.getAncestorNodeRefWithType(nodeRef, SeriesModel.Types.SERIES) == null) {
            return new String[] { "Document is not saved under series, parent= " + generalService.getPrimaryParent(nodeRef) + ", skipping node" };
        }
        DocumentDynamic document = BeanHelper.getDocumentDynamicService().getDocument(nodeRef);
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

    private boolean isChildNodeProperty(Map<String, Pair<DynamicPropertyDefinition, Field>> propertyDefinitions, String localName) {
        Pair<DynamicPropertyDefinition, Field> propDefAndField = propertyDefinitions.get(localName);
        if (propDefAndField == null) {
            return false;
        }
        QName[] childAssocTypeQNAmeHierarchy = propDefAndField.getFirst().getChildAssocTypeQNameHierarchy();
        return childAssocTypeQNAmeHierarchy != null && childAssocTypeQNAmeHierarchy.length > 0;
    }
}
>>>>>>> develop-5.1
