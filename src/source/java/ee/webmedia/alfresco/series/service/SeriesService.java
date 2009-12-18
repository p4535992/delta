package ee.webmedia.alfresco.series.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
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

    List<Series> getAllSeriesByFunction(NodeRef functionNodeRef);

    List<Series> getAllSeriesByFunction(NodeRef functionNodeRef, DocListUnitStatus status, QName docTypeId);

    Series getSeriesByNoderef(String seriesNodeRef);
    
    Node getSeriesNodeByRef(NodeRef seriesNodeRef);

    /**
     * @param functionNodeRef
     * @return Series object with TransientNode and reference to parent function
     */
    Series createSeries(NodeRef functionNodeRef);

    void delete(NodeRef nodeRef);

}
