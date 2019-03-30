package ee.webmedia.alfresco.adddocument.bootstrap;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Removes hierarchical properties from web service documents that are of type which had (at time of document creation)
 * default location values filled.
 */
public class RemoveAddedDocumentParentHierarchyPropertiesUpdater extends AbstractNodeUpdater {

    private static final Map<QName, Serializable> EMPTY_LOCATION_PROPS;

    static {
        Map<QName, Serializable> props = new HashMap<>();
        props.put(DocumentCommonModel.Props.FUNCTION, null);
        props.put(DocumentCommonModel.Props.SERIES, null);
        props.put(DocumentCommonModel.Props.VOLUME, null);
        props.put(DocumentCommonModel.Props.CASE, null);
        EMPTY_LOCATION_PROPS = props;
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        String query = SearchUtil.joinQueryPartsAnd(
                SearchUtil.generateParentPathQuery(DocumentCommonModel.Repo.WEB_SERVICE_SPACE),
                SearchUtil.generateTypeQuery(DocumentCommonModel.Types.DOCUMENT));
        return Collections.singletonList(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        nodeService.addProperties(nodeRef, EMPTY_LOCATION_PROPS);
        return null;
    }

}
