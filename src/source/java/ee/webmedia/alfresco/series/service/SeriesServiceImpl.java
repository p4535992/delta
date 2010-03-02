package ee.webmedia.alfresco.series.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;
import org.springframework.util.Assert;

public class SeriesServiceImpl implements SeriesService, BeanFactoryAware {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SeriesServiceImpl.class);
    private static BeanPropertyMapper<Series> seriesBeanPropertyMapper = BeanPropertyMapper.newInstance(Series.class);

    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private GeneralService generalService;
    /** NB! not injected - use getter to obtain instance of volumeService */
    private VolumeService _volumeService;
    private CopyService copyService;
    private BeanFactory beanFactory;

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
        for (Iterator<Series> i = series.iterator(); i.hasNext();) {
            Series s = i.next();
            if (!status.getValueName().equals(s.getStatus()) || !s.getDocType().contains(docTypeId)) {
                i.remove();
            }
        }
        return series;
    }

    public Series getSeriesByNodeRef(String seriesNodeRef) {
        return getSeriesByNoderef(new NodeRef(seriesNodeRef), null);
    }

    @Override
    public Series getSeriesByNodeRef(NodeRef nodeRef) {
        return getSeriesByNoderef(nodeRef, null);
    }

    @Override
    public void saveOrUpdate(Series series) {
        Map<String, Object> stringQNameProperties = series.getNode().getProperties();
        if (series.getNode() instanceof TransientNode) { // save
            NodeRef seriesNodeRef = nodeService.createNode(series.getFunctionNodeRef(),
                    SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES, SeriesModel.Types.SERIES,
                    RepoUtil.toQNameProperties(series.getNode().getProperties())).getChildRef();
            series.setNode(generalService.fetchNode(seriesNodeRef));
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
    public boolean isClosed(Node node) {
        return generalService.isExistingPropertyValueEqualTo(node, SeriesModel.Props.STATUS, DocListUnitStatus.CLOSED);
    }

    @Override
    public boolean closeSeries(Series series) {
        final Node seriesNode = series.getNode();
        if(isClosed(seriesNode)) {
            return true;
        }
        final NodeRef seriesRef = seriesNode.getNodeRef();
        final List<Volume> allVolumesOfSeries = getVolumeService().getAllVolumesBySeries(seriesRef);
        boolean allVolumesClosed = true;
        Map<String, Object> props = seriesNode.getProperties();
        if (!(seriesNode instanceof TransientNode)) {
            for (Volume volume : allVolumesOfSeries) {
                final Map<String, Object> volProps = volume.getNode().getProperties();
                if (StringUtils.equals(DocListUnitStatus.OPEN.getValueName(), (String) volProps.get(VolumeModel.Props.STATUS))) {
                    allVolumesClosed = false;
                    break;
                }
            }
        }
        if (!allVolumesClosed) {
            return false; // will not close series or its volumes
        }
        props.put(SeriesModel.Props.STATUS.toString(), DocListUnitStatus.CLOSED.getValueName());
        saveOrUpdate(series);
        return true;
    }

    @Override
    public Node getSeriesNodeByRef(NodeRef seriesNodeRef) {
        return generalService.fetchNode(seriesNodeRef);
    }

    /**
     * @param seriesNodeRef
     * @param functionNodeRef if null, then series.functionNodeRef is set using association of given seriesNodeRef
     * @return Series object with reference to corresponding functionNodeRef
     */
    private Series getSeriesByNoderef(NodeRef seriesNodeRef, NodeRef functionNodeRef) {
        if (!nodeService.getType(seriesNodeRef).equals(SeriesModel.Types.SERIES)) {
            throw new RuntimeException("Given noderef '" + seriesNodeRef + "' is not series type:\n\texpected '" + SeriesModel.Types.SERIES + "'\n\tbut got '"
                    + nodeService.getType(seriesNodeRef) + "'");
        }
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

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void setCopyService(CopyService copyService) {
        this.copyService = copyService;
    }

    /**
     * To break Circular dependency between VolumeService and SeriesService
     * 
     * @return VolumeService
     */
    private VolumeService getVolumeService() {
        if (_volumeService == null) {
            _volumeService = (VolumeService) beanFactory.getBean(VolumeService.BEAN_NAME);
        }
        return _volumeService;
    }
    // END: getters / setters

}
