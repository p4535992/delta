<<<<<<< HEAD
package ee.webmedia.alfresco.volume.service;

import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.volume.model.DeletedDocument;
import ee.webmedia.alfresco.volume.model.Volume;

/**
 * Service class for volumes
 * 
 * @author Ats Uiboupin
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
     */
    void closeVolume(NodeRef volumeRef);

    boolean isClosed(Node volumeNode);

    boolean isCaseVolumeEnabled();

    String getDefaultVolumeSortingField();

    void openVolume(Volume volume);

    void delete(Volume volume);

    boolean isOpened(Node node);

    void saveDeletedDocument(NodeRef volumeNodeRef, DeletedDocument deletedDocument);

    List<DeletedDocument> getDeletedDocuments(NodeRef volumeNodeRef);

    DeletedDocument getDeletedDocument(NodeRef deletedDocumentNodeRef);
}
=======
package ee.webmedia.alfresco.volume.service;

import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.volume.model.DeletedDocument;
import ee.webmedia.alfresco.volume.model.Volume;

/**
 * Service class for volumes
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

    Node getVolumeNodeByRef(NodeRef volumeNodeRef);

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
     * @return
     */
    Pair<String, Object[]> closeVolume(Volume volume);

    boolean isClosed(Node volumeNode);

    boolean isCaseVolumeEnabled();

    void openVolume(Volume volume);

    boolean isOpened(Node node);

    void saveDeletedDocument(NodeRef volumeNodeRef, DeletedDocument deletedDocument);

    List<DeletedDocument> getDeletedDocuments(NodeRef volumeNodeRef);

    DeletedDocument getDeletedDocument(NodeRef deletedDocumentNodeRef);

    NodeRef getArchivedVolumeByOriginalNodeRef(NodeRef archivedSeriesRef, NodeRef volumeNodeRef);
}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
