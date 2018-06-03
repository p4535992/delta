package ee.webmedia.alfresco.series.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.UnmodifiableSeries;

/**
 * Service class for series
 */
public interface SeriesService {
    String BEAN_NAME = "SeriesService";
    String NON_TX_BEAN_NAME = "seriesService";

    void saveOrUpdate(Series series);

    /**
     * For PP import only. Assumes the order was checked beforehand.
     */
    void saveOrUpdateWithoutReorder(Series series);

    List<UnmodifiableSeries> getAllSeriesByFunction(NodeRef functionNodeRef, boolean checkAdmin);

    List<UnmodifiableSeries> getAllCaseFileSeriesByFunction(NodeRef functionNodeRef, DocListUnitStatus status);

    List<UnmodifiableSeries> getAllSeriesByFunction(NodeRef functionNodeRef, Set<String> docTypeIds, DocListUnitStatus... statuses);

    List<UnmodifiableSeries> getAllSeriesByFunctionForStructUnit(NodeRef functionNodeRef, String structUnitId);
    
    List<UnmodifiableSeries> getAllSeriesByFunctionForRelatedUsersGroups(NodeRef functionNodeRef, String username);
    
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

    void openSeries(Series series);

    void updateContainingDocsCountByVolume(NodeRef seriesNodeRef, NodeRef volumeNodeRef, boolean volumeAdded);

    List<NodeRef> getAllSeriesRefsByFunction(NodeRef functionRef);

    /** this method should only be called by the updater */
    void setSeriesDefaultPermissionsOnCreate(NodeRef seriesRef);

    void delete(Series series);

    boolean hasSelectableSeries(NodeRef functionRef, boolean isSearchFilter, Set<String> idList, boolean forDocumentType);

    boolean hasSelectableSeriesForModal(NodeRef functionRef, Set<String> idList);

    UnmodifiableSeries getUnmodifiableSeries(NodeRef nodeRef, Map<Long, QName> propertyTypes);

    String getSeriesLabel(NodeRef seriesRef);

    boolean hasSeries(NodeRef functionNodeRef);

    void removeFromCache(NodeRef seriesNodeRef);

}
