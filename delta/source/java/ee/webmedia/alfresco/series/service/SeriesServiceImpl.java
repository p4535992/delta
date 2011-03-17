package ee.webmedia.alfresco.series.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.i18n.I18NUtil;
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
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

public class SeriesServiceImpl implements SeriesService, BeanFactoryAware {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SeriesServiceImpl.class);
    private static BeanPropertyMapper<Series> seriesBeanPropertyMapper = BeanPropertyMapper.newInstance(Series.class);

    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private GeneralService generalService;
    private DocumentLogService logService;
    private CopyService copyService;
    private BeanFactory beanFactory;
    /** NB! not injected - use getter to obtain instance of volumeService */
    private VolumeService _volumeService;
    /** NB! not injected - use getter to obtain instance of functionsService */
    private FunctionsService _functionsService;

    
    public List<ChildAssociationRef> getAllSeriesAssocsByFunction(NodeRef functionRef) {
        return nodeService.getChildAssocs(functionRef, RegexQNamePattern.MATCH_ALL, SeriesModel.Associations.SERIES);
    }

    
    public List<Series> getAllSeriesByFunction(NodeRef functionNodeRef) {
        List<ChildAssociationRef> seriesAssocs = getAllSeriesAssocsByFunction(functionNodeRef);
        List<Series> seriesOfFunction = new ArrayList<Series>(seriesAssocs.size());
        for (ChildAssociationRef series : seriesAssocs) {
            NodeRef seriesNodeRef = series.getChildRef();
            seriesOfFunction.add(getSeriesByNoderef(seriesNodeRef, functionNodeRef));
        }
        Collections.sort(seriesOfFunction);
        return seriesOfFunction;
    }

    
    public List<Series> getAllSeriesByFunction(NodeRef functionNodeRef, DocListUnitStatus status, QName docTypeId) {
        List<Series> series = getAllSeriesByFunction(functionNodeRef);
        for (Iterator<Series> i = series.iterator(); i.hasNext();) {
            Series s = i.next();
            if (!status.getValueName().equals(s.getStatus()) || (docTypeId != null && !s.getDocType().contains(docTypeId))) {
                i.remove();
            }
        }
        return series;
    }

    
    public List<Series> getAllSeriesByFunctionForStructUnit(NodeRef functionNodeRef, Integer structUnitId) {
        List<ChildAssociationRef> seriesAssocs = nodeService.getChildAssocs(functionNodeRef, RegexQNamePattern.MATCH_ALL, SeriesModel.Associations.SERIES);
        List<Series> seriesOfFunction = new ArrayList<Series>(seriesAssocs.size());
        for (ChildAssociationRef series : seriesAssocs) {
            NodeRef seriesNodeRef = series.getChildRef();
            @SuppressWarnings("unchecked")
            List<Integer> structUnits = (List<Integer>) nodeService.getProperty(seriesNodeRef, SeriesModel.Props.STRUCT_UNIT);

            if (structUnits != null && structUnits.contains(structUnitId)) {
                seriesOfFunction.add(getSeriesByNoderef(seriesNodeRef, functionNodeRef));
            }
        }
        return seriesOfFunction;
    }

    public Series getSeriesByNodeRef(String seriesNodeRef) {
        return getSeriesByNoderef(new NodeRef(seriesNodeRef), null);
    }

    
    public Series getSeriesByNodeRef(NodeRef nodeRef) {
        return getSeriesByNoderef(nodeRef, null);
    }
    
    public void saveOrUpdate(Series series) {
        saveOrUpdate(series, true);
    }
    
    public void saveOrUpdateWithoutReorder(Series series){
        saveOrUpdate(series, false);
    }

    private void saveOrUpdate(Series series, boolean performReorder) {
        Map<String, Object> stringQNameProperties = series.getNode().getProperties();
        final NodeRef seriesRef = series.getNode().getNodeRef();
        if (series.getNode() instanceof TransientNode) { // save
            NodeRef seriesNodeRef = nodeService.createNode(series.getFunctionNodeRef(),
                    SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES, SeriesModel.Types.SERIES,
                    RepoUtil.toQNameProperties(stringQNameProperties, false, true)).getChildRef();
            series.setNode(generalService.fetchNode(seriesNodeRef));
            logService.addSeriesLog(seriesNodeRef, I18NUtil.getMessage("series_log_status_created"));
        } else { // update
            final String previousAccessrestriction = (String) nodeService.getProperty(seriesRef, DocumentCommonModel.Props.ACCESS_RESTRICTION);
            generalService.setPropertiesIgnoringSystem(seriesRef, stringQNameProperties);
            logService.addSeriesLog(seriesRef, I18NUtil.getMessage("series_log_status_changed"));
            final String newAccessrestriction = (String) stringQNameProperties.get(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString());
            if (!StringUtils.equals(previousAccessrestriction, newAccessrestriction)) {
                logService.addSeriesLog(seriesRef, I18NUtil.getMessage("series_log_status_accessRestrictionChanged"));
            }
        }
        if (performReorder) {
            reorderSeries(series);
        }
    }

    private void reorderSeries(Series series) {
        final int order = series.getOrder();
        final List<Series> allSeriesByFunction = getAllSeriesByFunction(series.getFunctionNodeRef());
        Collections.sort(allSeriesByFunction, new Comparator<Series>() {

            public int compare(Series s1, Series s2) {
                final int order1 = getSeriesOrder(s1);
                final int order2 = getSeriesOrder(s2);
                if (order1 == order2) {
                    return 0;
                }
                return order1 < order2 ? -1 : 1;
            }

        });

        for (Series otherSeries : allSeriesByFunction) {
            if (series.getNode().getNodeRef().equals(otherSeries.getNode().getNodeRef())) {
                continue;
            }
            final int order2 = (Integer) otherSeries.getNode().getProperties().get(SeriesModel.Props.ORDER.toString());
            if (order2 == order) {
                otherSeries.getNode().getProperties().put(SeriesModel.Props.ORDER.toString(), order2 + 1);
                //reorderSeries is recursively called on all following series in the list by saveOrUpdate
                saveOrUpdate(otherSeries);
                break;
            }
        }
    }
    
    private Integer getSeriesOrder(Series series){
        return (Integer) series.getNode().getProperties().get(SeriesModel.Props.ORDER.toString());
    }    
    
    public Series createSeries(NodeRef functionNodeRef) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
        int order = getNextSeriesOrderNrByFunction(functionNodeRef);
        props.put(SeriesModel.Props.ORDER, order);
        TransientNode transientNode = TransientNode.createNew(dictionaryService, dictionaryService.getType(SeriesModel.Types.SERIES), null, props);
        Series series = seriesBeanPropertyMapper.toObject(RepoUtil.toQNameProperties(transientNode.getProperties()));
        series.setNode(transientNode);
        series.setOrder(order);
        series.setFunctionNodeRef(functionNodeRef);
        final String functionMark = getFunctionsService().getFunctionByNodeRef(functionNodeRef).getMark();
        final String initialSeriesIdentifier = functionMark + "-";
        series.setSeriesIdentifier(initialSeriesIdentifier);
        series.setInitialSeriesIdentifier(initialSeriesIdentifier);
        return series;
    }
    
    public boolean isClosed(Node node) {
        return RepoUtil.isExistingPropertyValueEqualTo(node, SeriesModel.Props.STATUS, DocListUnitStatus.CLOSED.getValueName());
    }

    
    public boolean closeSeries(Series series) {
        final Node seriesNode = series.getNode();
        if (isClosed(seriesNode)) {
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
        logService.addSeriesLog(seriesRef, I18NUtil.getMessage("series_log_status_closed"));
        return true;
    }

    
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
        final Series series = new Series();
        series.setNode(getSeriesNodeByRef(seriesNodeRef)); // node needs to be set before setters are called by mapper
        seriesBeanPropertyMapper.toObject(nodeService.getProperties(seriesNodeRef), series);
        if (functionNodeRef == null) {
            List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(seriesNodeRef);
            if (parentAssocs.size() != 1) {
                throw new RuntimeException("Series is expected to have only one parent function, but got " + parentAssocs.size() + " matching the criteria.");
            }
            functionNodeRef = parentAssocs.get(0).getParentRef();
        }
        series.setFunctionNodeRef(functionNodeRef);
        if (log.isDebugEnabled()) {
            // XXX:ruins everything due to order being NULL log.debug("Found series: " + series);
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

    
    public void updateContainingDocsCountByVolume(NodeRef seriesNodeRef, NodeRef volumeNodeRef, boolean volumeAdded) {
        Integer count = (Integer) nodeService.getProperty(volumeNodeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT);
        generalService.updateParentContainingDocsCount(seriesNodeRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, volumeAdded, count);
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

    public void setLogService(DocumentLogService logService) {
        this.logService = logService;
    }

    
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /** To break circular dependency */
    private FunctionsService getFunctionsService() {
        if (_functionsService == null) {
            _functionsService = (FunctionsService) beanFactory.getBean(FunctionsService.BEAN_NAME);
        }
        return _functionsService;
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