package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props.OBJECT_TYPE_ID;
import static ee.webmedia.alfresco.docadmin.model.DocumentAdminModel.Props.OBJECT_TYPE_VERSION_NR;
import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNotNullQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.docadmin.service.Field;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.DynamicPropertyDefinition;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;

/**
 * @author Keit Tehvan
 */
public class RepairPropertyTypeMismatchUpdater extends AbstractNodeUpdater {

    private DocumentConfigService documentConfigService;

    @Override
    protected boolean isRequiresNewTransaction() {
        return false;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = generatePropertyNotNullQuery(OBJECT_TYPE_ID);
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        NamespaceService namespaceService = serviceRegistry.getNamespaceService();
        QName type = nodeService.getType(nodeRef);
        if (!DocumentCommonModel.Types.DOCUMENT.equals(type)) {
            return new String[] { "notDocumentType:" + type.toPrefixString(namespaceService) };
        }
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        Map<String, Pair<DynamicPropertyDefinition, Field>> defs = null;
        try {
            defs = documentConfigService.getPropertyDefinitions(new Node(nodeRef));
        } catch (RuntimeException ex) {
            // Should not happen, because getPropertyDefinitions should return null if docType or ver does not exist
            return new String[] { "Failed to get propertyDefinitions for objectTypeId=" + props.get(OBJECT_TYPE_ID) + ", objectTypeVersionNr=" + props.get(OBJECT_TYPE_VERSION_NR)
                    + ": " + ex.getMessage() };
        }
        if (defs == null) {
            nodeService.deleteNode(nodeRef);
            return new String[] { "Deleted node, because failed to get propertyDefinitions for objectTypeId=" + props.get(OBJECT_TYPE_ID) + ", objectTypeVersionNr="
                    + props.get(OBJECT_TYPE_VERSION_NR) + ": returned null" };
        }
        List<String> result = new ArrayList<String>();
        for (Entry<QName, Serializable> entry : props.entrySet()) {
            if (!entry.getKey().getNamespaceURI().equals(DocumentDynamicModel.URI)) {
                continue;
            }
            Pair<DynamicPropertyDefinition, Field> def = defs.get(entry.getKey().getLocalName());
            if (def == null) {
                nodeService.removeProperty(nodeRef, entry.getKey());
                result.add(entry.getKey().toPrefixString(namespaceService) + " had null propertyDefinition, removed property: "
                        + (entry.getValue() == null ? "null" : entry.getValue().getClass().getSimpleName()));
                continue;
            }
            if (entry.getValue() == null) {
                continue;
            }
            Serializable value = null;
            if (def.getFirst().isMultiValued() && !(entry.getValue() instanceof List)) {
                result.add(entry.getKey().toPrefixString(namespaceService) + " was not a multivalued List: " + entry.getValue().getClass().getSimpleName());
                value = (Serializable) Collections.singletonList(entry.getValue());
                nodeService.setProperty(nodeRef, entry.getKey(), value);
            } else if (def.getFirst().isMultiValued() && (entry.getValue() instanceof List) && !((List<?>) entry.getValue()).isEmpty()) {
                String className = def.getFirst().getDataType().getJavaClassName();
                if (className == null) {
                    continue;
                }
                List<?> list = (List<?>) entry.getValue();
                boolean listModified = false;
                for (Iterator<?> i = list.iterator(); i.hasNext();) {
                    Object object = i.next();
                    if (object != null && !className.equals(object.getClass().getName())) {
                        result.add(entry.getKey().toPrefixString(namespaceService) + " list's element was not a " + className + ", removing from list : "
                                + object.getClass().getSimpleName());
                        i.remove();
                        listModified = true;
                    }
                }
                if (listModified) {
                    nodeService.setProperty(nodeRef, entry.getKey(), (Serializable) list);
                }
            } else if (!def.getFirst().isMultiValued()) {
                String className = def.getFirst().getDataType().getJavaClassName();
                if (className == null || className.equals(entry.getValue().getClass().getName())) {
                    continue;
                }
                String resultString = entry.getKey().toPrefixString(namespaceService) + " was not a " + className + " : " + entry.getValue().getClass().getSimpleName();
                try {
                    value = (Serializable) DefaultTypeConverter.INSTANCE.convert(def.getFirst().getDataType(), entry.getValue());
                } catch (Exception ex) {
                    resultString += " CONVERSION FAILED: " + ex.getMessage();
                    // no continue here, if conversion fails, set the property to null
                }
                result.add(resultString);
                nodeService.setProperty(nodeRef, entry.getKey(), value);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

}
