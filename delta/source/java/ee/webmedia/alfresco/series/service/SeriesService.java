package ee.webmedia.alfresco.series.service;

import java.util.List;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.series.model.Series;

/**
 * Service class for series
 * 
 * @author Ats Uiboupin
 */
public interface SeriesService {
    String BEAN_NAME = "SeriesService";

    void saveOrUpdate(Series series);

    /**
     * For PP import only. Assumes the order was checked beforehand.
     */
    void saveOrUpdateWithoutReorder(Series series);

    List<Series> getAllSeriesByFunction(NodeRef functionNodeRef);

    List<Series> getAllSeriesByFunction(NodeRef functionNodeRef, DocListUnitStatus status, Set<String> docTypeIds);

    List<Series> getAllSeriesByFunctionForStructUnit(NodeRef functionNodeRef, Integer structUnitId);

    Series getSeriesByNodeRef(String seriesNodeRef);

    Node getSeriesNodeByRef(NodeRef seriesNodeRef);

    Series getSeriesByNodeRef(NodeRef nodeRef);

    /**
     * @param functionNodeRef
     * @return Series object with TransientNode and reference to parent function
     */
    Series createSeries(NodeRef functionNodeRef);

    /**
     * @param series series to be closed, if it doesn't have any unclosed volumes
     * @return flase, if didn't close series, because it had some Volumes that were not already closed, true otherwise(if closing was successful)
     */
    boolean closeSeries(Series series);

    boolean isClosed(Node currentNode);

    void updateContainingDocsCountByVolume(NodeRef seriesNodeRef, NodeRef volumeNodeRef, boolean volumeAdded);

    List<NodeRef> getAllSeriesRefsByFunction(NodeRef functionRef);

    /** this method should only be called by the updater */
    void setSeriesDefaultPermissionsOnCreate(NodeRef seriesRef);

}
