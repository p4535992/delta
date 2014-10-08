package ee.webmedia.alfresco.volume.service;

import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
<<<<<<< HEAD
=======
import org.alfresco.util.Pair;
>>>>>>> develop-5.1
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.volume.model.DeletedDocument;
import ee.webmedia.alfresco.volume.model.Volume;

/**
 * Service class for volumes
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> develop-5.1
 */
public interface VolumeService {
    String BEAN_NAME = "VolumeService";

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
     */
    void saveOrUpdate(Volume volume, boolean fromNodeProps);

    List<ChildAssociationRef> getAllVolumeRefsBySeries(NodeRef seriesNodeRef);

    List<Volume> getAllVolumesBySeries(NodeRef seriesNodeRef);

    List<Volume> getAllVolumesBySeries(NodeRef seriesNodeRef, DocListUnitStatus status);

    List<Volume> getAllValidVolumesBySeries(NodeRef seriesNodeRef);

    List<Volume> getAllValidVolumesBySeries(NodeRef seriesNodeRef, DocListUnitStatus status);

    List<Volume> getAllOpenExpiredVolumesBySeries(NodeRef seriesNodeRef);

    Volume getVolumeByNodeRef(String volumeNodeRef);

    Volume getVolumeByNodeRef(NodeRef volumeRef);

    WmNode getVolumeNodeByRef(NodeRef volumeNodeRef);

    /**
     * @param seriesNodeRef
     * @return Volume object with TransientNode and reference to parent series
     */
    Volume createVolume(NodeRef seriesNodeRef);

    Volume copyVolume(Volume baseVolume);

    /**
     * Close given volume and all cases under given volume.
     * 
     * @param volume
<<<<<<< HEAD
     */
    void closeVolume(NodeRef volumeRef);
=======
     * @return
     */
    Pair<String, Object[]> closeVolume(NodeRef volumeRef);
>>>>>>> develop-5.1

    boolean isClosed(Node volumeNode);

    boolean isCaseVolumeEnabled();

    String getDefaultVolumeSortingField();

    void openVolume(Volume volume);

    void delete(Volume volume);

    boolean isOpened(Node node);

    void saveDeletedDocument(NodeRef volumeNodeRef, DeletedDocument deletedDocument);

    List<DeletedDocument> getDeletedDocuments(NodeRef volumeNodeRef);

    DeletedDocument getDeletedDocument(NodeRef deletedDocumentNodeRef);
<<<<<<< HEAD
=======

    NodeRef getArchivedVolumeByOriginalNodeRef(NodeRef archivedSeriesRef, NodeRef volumeNodeRef);

>>>>>>> develop-5.1
}
