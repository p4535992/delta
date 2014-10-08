package ee.webmedia.alfresco.volume.service;

import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.volume.model.DeletedDocument;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;
import ee.webmedia.alfresco.volume.model.Volume;

/**
 * Service class for volumes
 */
public interface VolumeService {
    String BEAN_NAME = "VolumeService";
    String NON_TX_BEAN_NAME = "volumeService";

    /**
     * Save or update volume - new values taken from node properties
     *
     * @param volume
     */
    void saveOrUpdate(Volume volume);

    /**
     * Save or update volume - new values taken from node properties or from volume fields
     *
     * @param volume
     * @param fromNodeProps
     * @return TODO
     */
    NodeRef saveOrUpdate(Volume volume, boolean fromNodeProps);

    List<ChildAssociationRef> getAllVolumeRefsBySeries(NodeRef seriesNodeRef);

    List<UnmodifiableVolume> getAllVolumesBySeries(NodeRef seriesNodeRef);

    List<UnmodifiableVolume> getAllValidVolumesBySeries(NodeRef seriesNodeRef);

    List<UnmodifiableVolume> getAllValidVolumesBySeries(NodeRef seriesNodeRef, DocListUnitStatus status);

    List<UnmodifiableVolume> getAllOpenExpiredVolumesBySeries(NodeRef seriesNodeRef);

    Volume getVolumeByNodeRef(NodeRef volumeRef, Map<Long, QName> propertyTypes);

    WmNode getVolumeNodeByRef(NodeRef volumeNodeRef, Map<Long, QName> propertyTypes);

    /**
     * @param seriesNodeRef
     * @return Volume object with TransientNode and reference to parent series
     */
    Volume createVolume(NodeRef seriesNodeRef);

    Volume copyVolume(Volume baseVolume);

    /**
     * Close given volume and all cases under given volume.
     *
     * @param propertyTypes
     * @param volume
     * @return
     */
    Pair<String, Object[]> closeVolume(NodeRef volumeRef, Map<Long, QName> propertyTypes);

    boolean isClosed(Node volumeNode);

    void openVolume(Volume volume);

    void delete(Volume volume);

    boolean isOpened(Node node);

    void saveDeletedDocument(NodeRef volumeNodeRef, DeletedDocument deletedDocument);

    List<DeletedDocument> getDeletedDocuments(NodeRef volumeNodeRef);

    DeletedDocument getDeletedDocument(NodeRef deletedDocumentNodeRef);

    NodeRef getArchivedVolumeByOriginalNodeRef(NodeRef archivedSeriesRef, NodeRef volumeNodeRef);

    UnmodifiableVolume getUnmodifiableVolume(NodeRef volumeRef, Map<Long, QName> propertyTypes);

    String getVolumeLabel(NodeRef volumeRef);

    void removeFromCache(NodeRef volRef);

}
