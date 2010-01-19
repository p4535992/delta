package ee.webmedia.alfresco.volume.service;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.volume.model.Volume;

/**
 * Service class for volumes
 * 
 * @author Ats Uiboupin
 */
public interface VolumeService {
    String BEAN_NAME = "VolumeService";

    void saveOrUpdate(Volume volume);

    List<Volume> getAllVolumesBySeries(NodeRef seriesNodeRef);

    List<Volume> getAllValidVolumesBySeries(NodeRef seriesNodeRef);

    Volume getVolumeByNoderef(String volumeNodeRef);

    Volume getVolumeByNodeRef(NodeRef volumeRef);
    
    Node getVolumeNodeByRef(NodeRef volumeNodeRef);

    /**
     * @param seriesNodeRef
     * @return Volume object with TransientNode and reference to parent series
     */
    Volume createVolume(NodeRef seriesNodeRef);

    /**
     * Close given volume and all cases under given volume.
     * @param volume
     */
    void closeVolume(Volume volume);

    boolean isClosed(Node volumeNode);

}
