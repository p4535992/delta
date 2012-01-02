package ee.webmedia.alfresco.document.bootstrap;

import static ee.webmedia.alfresco.utils.SearchUtil.generatePropertyNullQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.common.bootstrap.AbstractNodeUpdater;
import ee.webmedia.alfresco.series.model.SeriesModel;

/**
 * Populates docNumberPattern field for existing series.
 * 
 * @author Vladimir Drozdik
 */
public class SeriesDocNumberPatternUpdater extends AbstractNodeUpdater {

    @Override
    protected List<ResultSet> getNodeLoadingResultSet() {
        String query = joinQueryPartsAnd(Arrays.asList(
                generateTypeQuery(SeriesModel.Types.SERIES),
                generatePropertyNullQuery(SeriesModel.Props.DOC_NUMBER_PATTERN)
                ));
        List<ResultSet> result = new ArrayList<ResultSet>(2);
        result.add(searchService.query(generalService.getStore(), SearchService.LANGUAGE_LUCENE, query));
        result.add(searchService.query(generalService.getArchivalsStoreRef(), SearchService.LANGUAGE_LUCENE, query));

        return result;
    }

    @Override
    protected String[] updateNode(NodeRef nodeRef) {
        Map<QName, Serializable> origProps = nodeService.getProperties(nodeRef);
        Map<QName, Serializable> newProps = new HashMap<QName, Serializable>(3);

        final String defDocNumberPattern = "{S}/{DN}";
        newProps.put(SeriesModel.Props.DOC_NUMBER_PATTERN, defDocNumberPattern);
        newProps.put(ContentModel.PROP_MODIFIER, origProps.get(ContentModel.PROP_MODIFIER));
        newProps.put(ContentModel.PROP_MODIFIED, origProps.get(ContentModel.PROP_MODIFIED));
        nodeService.addProperties(nodeRef, newProps);
        return new String[] { "updateDocRegNumberPattern", defDocNumberPattern };
    }

    @Override
    protected void doAfterTransactionBegin() {
        behaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE); // Allows us to set our own modifier and modified values
    }

    @Override
    protected boolean isRequiresNewTransaction() {
        return false; // otherwise freezes on addProperties when updating from 2.5.x to 3.5.2
    }
}