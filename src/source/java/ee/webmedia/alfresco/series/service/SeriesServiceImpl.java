package ee.webmedia.alfresco.series.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class SeriesServiceImpl implements SeriesService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SeriesServiceImpl.class);
    private static BeanPropertyMapper<Series> seriesBeanPropertyMapper = BeanPropertyMapper.newInstance(Series.class);

    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private GeneralService generalService;

    @Override
    public List<Series> getAllSeriesByFunction(NodeRef functionNodeRef) {
        List<ChildAssociationRef> seriesAssocs = nodeService.getChildAssocs(functionNodeRef, RegexQNamePattern.MATCH_ALL, SeriesModel.Associations.SERIES);
        List<Series> seriesOfFunction = new ArrayList<Series>(seriesAssocs.size());
        for (ChildAssociationRef series : seriesAssocs) {
            NodeRef seriesNodeRef = series.getChildRef();
            seriesOfFunction.add(getSeriesByNoderef(seriesNodeRef, functionNodeRef));
        }
        return seriesOfFunction;
    }

    @Override
    public List<Series> getAllSeriesByFunction(NodeRef functionNodeRef, DocListUnitStatus status, QName docTypeId) {
        List<Series> series = getAllSeriesByFunction(functionNodeRef);
        for (Iterator<Series> i = series.iterator(); i.hasNext(); ) {
            Series s = i.next();
            if (!status.getValueName().equals(s.getStatus()) || !s.getDocType().contains(docTypeId.toString())) {
                i.remove();
            }
        }
        return series;
    }

    public Series getSeriesByNoderef(String seriesNodeRef) {
        return getSeriesByNoderef(new NodeRef(seriesNodeRef), null);
    }

    @Override
    public void saveOrUpdate(Series series) {
        Map<String, Object> stringQNameProperties = series.getNode().getProperties();
        if (series.getNode() instanceof TransientNode) { // save
            TransientNode transientNode = (TransientNode) series.getNode();
            NodeRef seriesNodeRef = nodeService.createNode(series.getFunctionNodeRef(),
                    SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES, SeriesModel.Types.SERIES,
                    RepoUtil.toQNameProperties(transientNode.getProperties())).getChildRef();
            series.setNode(RepoUtil.fetchNode(seriesNodeRef));
        } else { // update
            generalService.setPropertiesIgnoringSystem(series.getNode().getNodeRef(), stringQNameProperties);
        }
    }

    @Override
    public Series createSeries(NodeRef functionNodeRef) {
        Series series = new Series();
        Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
        props.put(SeriesModel.Props.ORDER, getNextSeriesOrderNrByFunction(functionNodeRef));
        TransientNode transientNode = TransientNode.createNew(dictionaryService, dictionaryService.getType(SeriesModel.Types.SERIES), null, props);
        series.setNode(transientNode);
        series.setFunctionNodeRef(functionNodeRef);
        return series;
    }

    @Override
    public Node getSeriesNodeByRef(NodeRef seriesNodeRef) {
        return RepoUtil.fetchNode(seriesNodeRef);
    }

    @Override
    public void delete(NodeRef nodeRef) {
        nodeService.deleteNode(nodeRef);
    }

    /**
     * @param seriesNodeRef
     * @param functionNodeRef if null, then series.functionNodeRef is set using association of given seriesNodeRef
     * @return Series object with reference to corresponding functionNodeRef
     */
    private Series getSeriesByNoderef(NodeRef seriesNodeRef, NodeRef functionNodeRef) {
        Series series = seriesBeanPropertyMapper.toObject(nodeService.getProperties(seriesNodeRef));
        if (functionNodeRef == null) {
            List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(seriesNodeRef);
            if (parentAssocs.size() != 1) {
                throw new RuntimeException("Series is expected to have only one parent function, but got " + parentAssocs.size() + " matching the criteria.");
            }
            functionNodeRef = parentAssocs.get(0).getParentRef();
        }
        series.setFunctionNodeRef(functionNodeRef);
        series.setNode(getSeriesNodeByRef(seriesNodeRef));
        if (log.isDebugEnabled()) {
            log.debug("Found series: " + series);
        }
        return series;
    }

    private int getNextSeriesOrderNrByFunction(NodeRef functionNodeRef) {
        int maxOrder = 0;
        for (Series fn : getAllSeriesByFunction(functionNodeRef)) {
            if (maxOrder < fn.getOrder()) {
                maxOrder = fn.getOrder();
            }
        }
        return maxOrder + 1;
    }

    // START: getters / setters
    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }
    // END: getters / setters

}
