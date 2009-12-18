package ee.webmedia.alfresco.volume.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Ats Uiboupin
 */
public class VolumeServiceImpl implements VolumeService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(VolumeServiceImpl.class);
    private static final BeanPropertyMapper<Volume> volumeBeanPropertyMapper = BeanPropertyMapper.newInstance(Volume.class);

    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private GeneralService generalService;
    private SeriesService seriesService;

    @Override
    public List<Volume> getAllVolumesBySeries(NodeRef seriesNodeRef) {
        List<ChildAssociationRef> volumeAssocs = nodeService.getChildAssocs(seriesNodeRef, RegexQNamePattern.MATCH_ALL, VolumeModel.Associations.VOLUME);
        List<Volume> volumeOfSeries = new ArrayList<Volume>(volumeAssocs.size());
        for (ChildAssociationRef volume : volumeAssocs) {
            NodeRef volumeNodeRef = volume.getChildRef();
            volumeOfSeries.add(getVolumeByNoderef(volumeNodeRef, seriesNodeRef));
        }
        return volumeOfSeries;
    }

    @Override
    public List<Volume> getAllValidVolumesBySeries(NodeRef seriesNodeRef) {
        List<Volume> volumes = getAllVolumesBySeries(seriesNodeRef);
        final Calendar cal = Calendar.getInstance();
        for (Iterator<Volume> i = volumes.iterator(); i.hasNext(); ) {
            Volume volume = i.next();

            Date validFrom = volume.getValidFrom();
            if (validFrom != null && cal.getTime().before(validFrom)) {
                log.debug("Skipping volume '" + volume.getTitle() + "', current date " + cal.getTime() + " is earlier than volume valid from date " + validFrom);
                i.remove();
                continue;
            }

            if (volume.getValidTo() != null) {
                Calendar validTo = Calendar.getInstance();
                validTo.setTime(volume.getValidTo());
                validTo.set(Calendar.HOUR_OF_DAY, 23);
                validTo.set(Calendar.MINUTE, 59);
                validTo.set(Calendar.SECOND, 59);
                if (cal.after(validTo)) {
                    log.debug("Skipping volume '" + volume.getTitle() + "', current date " + cal.getTime() + " is later than volume valid to date "
                            + validTo.getTime());
                    i.remove();
                    continue;
                }
            }
        }
        return volumes;
    }

    public Volume getVolumeByNoderef(String volumeNodeRef) {
        return getVolumeByNoderef(new NodeRef(volumeNodeRef), null);
    }

    @Override
    public void saveOrUpdate(Volume volume) {
        Map<String, Object> stringQNameProperties = volume.getNode().getProperties();
        if (volume.getNode() instanceof TransientNode) { // save
            TransientNode transientNode = (TransientNode) volume.getNode();
            NodeRef volumeNodeRef = nodeService.createNode(volume.getSeriesNodeRef(),
                    VolumeModel.Associations.VOLUME, VolumeModel.Associations.VOLUME, VolumeModel.Types.VOLUME,
                    RepoUtil.toQNameProperties(transientNode.getProperties())).getChildRef();
            volume.setNode(RepoUtil.fetchNode(volumeNodeRef));
        } else { // update
            generalService.setPropertiesIgnoringSystem(volume.getNode().getNodeRef(), stringQNameProperties);
        }
    }

    @Override
    public Volume createVolume(NodeRef seriesNodeRef) {
        Volume volume = new Volume();
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(VolumeModel.Props.STATUS, DocListUnitStatus.OPEN.getValueName());

        TransientNode transientNode = TransientNode.createNew(dictionaryService, dictionaryService.getType(VolumeModel.Types.VOLUME), null, props);
        volume.setNode(transientNode);
        volume.setSeriesNodeRef(seriesNodeRef);
        return volume;
    }

    @Override
    public void closeVolume(Volume volume) {
        Map<String, Object> props = volume.getNode().getProperties();
        props.put(VolumeModel.Props.STATUS.toString(), DocListUnitStatus.CLOSED.getValueName());

        Series series = seriesService.getSeriesByNoderef(volume.getSeriesNodeRef().toString());
        final Integer retentionPeriod = series.getRetentionPeriod();
        if (retentionPeriod != null) {
            final Calendar cal1 = Calendar.getInstance();
            cal1.set(cal1.get(Calendar.YEAR) + 1 + retentionPeriod, 0, 1);// 1. January next year + retentionPeriod(in years)
            props.put(VolumeModel.Props.DISPOSITION_DATE.toString(), DateUtils.truncate(cal1, Calendar.DAY_OF_MONTH).getTime());
        }

        saveOrUpdate(volume);
    }
    
    @Override
    public Node getVolumeNodeByRef(NodeRef volumeNodeRef) {
        return RepoUtil.fetchNode(volumeNodeRef);
    }

    @Override
    public void delete(NodeRef nodeRef) {
        nodeService.deleteNode(nodeRef);
    }

    /**
     * @param volumeNodeRef
     * @param seriesNodeRef if null, then volume.seriesNodeRef is set using association of given volumeNodeRef
     * @return Volume object with reference to corresponding seriesNodeRef
     */
    private Volume getVolumeByNoderef(NodeRef volumeNodeRef, NodeRef seriesNodeRef) {
        Volume volume = volumeBeanPropertyMapper.toObject(nodeService.getProperties(volumeNodeRef));
        if (seriesNodeRef == null) {
            List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(volumeNodeRef);
            if (parentAssocs.size() != 1) {
                throw new RuntimeException("Volume is expected to have only one parent series, but got " + parentAssocs.size() + " matching the criteria.");
            }
            seriesNodeRef = parentAssocs.get(0).getParentRef();
        }
        volume.setSeriesNodeRef(seriesNodeRef);
        volume.setNode(getVolumeNodeByRef(volumeNodeRef));
        if (log.isDebugEnabled()) {
            log.debug("Found volume: " + volume);
        }
        return volume;
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

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }
    // END: getters / setters

}
