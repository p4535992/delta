package ee.webmedia.alfresco.adr.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.adr.model.AdrModel;
import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.utils.SearchUtil;

/**
 * Updates the NodeRef type property to Text
 */
public class AdrDeletedDocumentNodeRefToTextUpdater extends AbstractNodeUpdater {

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() throws Exception {
        List<String> queryParts = new ArrayList<String>(2);
        queryParts.add(generateTypeQuery(AdrModel.Types.ADR_DELETED_DOCUMENT));
        queryParts.add(SearchUtil.generatePropertyNotNullQuery(AdrModel.Props.NODEREF));
        String query = joinQueryPartsAnd(queryParts);

        List<ResultSet> result = new ArrayList<ResultSet>(1);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        return result;
    }


    @Override
    protected String[] updateNode(NodeRef nodeRef) throws Exception {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        Serializable nodeRefProp = origProps.get(AdrModel.Props.NODEREF);
        if (nodeRefProp == null) {
            return new String[] { "AdrDeletedDocumentNodeRefToTextUpdater", "nodeRefIsNull" };
        }

        Map<QName, Serializable> newProps = new HashMap<QName, Serializable>(3);

        String stringValue = nodeRefProp.toString();
        newProps.put(AdrModel.Props.NODEREF, stringValue);
        newProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        newProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, newProps);
        return new String[] { "AdrDeletedDocumentNodeRefToTextUpdater", stringValue };
    }

}
