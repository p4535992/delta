package ee.webmedia.alfresco.docconfig.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyBooleanQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.CreateObjectCallback;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.CaseFileType;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DynamicType;
import ee.webmedia.alfresco.docconfig.service.DocumentConfigService;
import ee.webmedia.alfresco.docconfig.service.PropDefCacheKey;

/**
 * Fill propertyDefinitionCache with {@value #NUMBER_OF_LAST_VERSIONS_TO_LOAD} latest versions of dynamic types.
 */
public class PropertyDefinitionCacheUpdater extends AbstractModuleComponent {

    public static final String BEAN_NAME = "PropertyDefinitionCacheUpdater";
    private static final Log LOG = LogFactory.getLog(PropertyDefinitionCacheUpdater.class);
    private static final Set<QName> PROPS_TO_LOAD = Collections.singleton(DocumentAdminModel.Props.VERSION_NR);
    private static final String DYNAMIC_TYPE = "type";
    private static final String TYPE_ID = "id";
    private static final int NUMBER_OF_LAST_VERSIONS_TO_LOAD = 3;

    private GeneralService generalService;
    private SearchService searchService;
    private NodeService nodeService;
    private BulkLoadNodeService bulkLoadNodeService;
    private DocumentConfigService documentConfigService;

    @Override
    public void executeInternal() throws Throwable {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(DocumentAdminModel.Types.DOCUMENT_TYPE, DocumentAdminModel.Types.CASE_FILE_TYPE),
                generatePropertyBooleanQuery(DocumentAdminModel.Props.USED, Boolean.TRUE)));

        List<ResultSet> result = new ArrayList<>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));

        List<NodeRef> parentRefs = new ArrayList<>();
        for (ResultSet resultSet : result) {
            parentRefs.addAll(resultSet.getNodeRefs());
        }
        final Map<NodeRef, Map<String, Serializable>> parentRefsWithProps = new HashMap<>();
        for (NodeRef parentRef : parentRefs) {
            Map<String, Serializable> props = new HashMap<>();
            QName type = nodeService.getType(parentRef);
            props.put(DYNAMIC_TYPE, getDynamicType(type));
            props.put(TYPE_ID, nodeService.getProperty(parentRef, DocumentAdminModel.Props.ID));
            parentRefsWithProps.put(parentRef, props);
        }

        LOG.info("PropertyDefinitionCacheUpdater started.");
        generalService.runOnBackground(new RunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                loadPropDefs(parentRefsWithProps);
                LOG.info("PropertyDefinitionCacheUpdater finished.");
                return null;
            }
        }, "loadPropertyDefinitionCache", true);
    }

    private <T extends DynamicType> void loadPropDefs(Map<NodeRef, Map<String, Serializable>> parentsWithProps) {
        CreateObjectCallback<Integer> createCacheKeyCallback = new CreateObjectCallback<Integer>() {
            @Override
            public Integer create(NodeRef nodeRef, Map<QName, Serializable> properties) {
                return (Integer) properties.get(DocumentAdminModel.Props.VERSION_NR);
            }
        };

        Map<NodeRef, List<Integer>> children = bulkLoadNodeService.loadChildNodes(new ArrayList<>(parentsWithProps.keySet()),
                PROPS_TO_LOAD, DocumentAdminModel.Types.DOCUMENT_TYPE_VERSION, null, createCacheKeyCallback);

        for (Map.Entry<NodeRef, List<Integer>> entry : children.entrySet()) {
            Map<String, Serializable> props = parentsWithProps.get(entry.getKey());
            List<Integer> versionNumbers = entry.getValue();
            Collections.sort(versionNumbers);
            int size = versionNumbers.size();
            versionNumbers = versionNumbers.subList(Math.max(0, size - NUMBER_OF_LAST_VERSIONS_TO_LOAD), size);

            Class<T> dynamicClass = (Class<T>) props.get(DYNAMIC_TYPE);
            String typeId = (String) props.get(TYPE_ID);
            for (Integer version : versionNumbers) {
                PropDefCacheKey cacheKey = new PropDefCacheKey(dynamicClass, typeId, version);
                documentConfigService.getPropertyDefinitions(cacheKey);
            }
        }
    }

    private <T extends DynamicType> Class<T> getDynamicType(QName type) {
        if (DocumentAdminModel.Types.DOCUMENT_TYPE.equals(type)) {
            return (Class<T>) DocumentType.class;
        } else if (DocumentAdminModel.Types.CASE_FILE_TYPE.equals(type)) {
            return (Class<T>) CaseFileType.class;
        }
        throw new IllegalArgumentException("Illegal argument for dynamic type: " + type);
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

    public void setDocumentConfigService(DocumentConfigService documentConfigService) {
        this.documentConfigService = documentConfigService;
    }

}
