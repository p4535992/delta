package ee.webmedia.alfresco.archivals.service;

<<<<<<< HEAD
import java.util.Date;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.archivals.model.ActivityStatus;
import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.archivals.web.ArchivalActivity;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.functions.model.Function;

/**
 * @author Romet Aidla
 */
public interface ArchivalsService {
    String BEAN_NAME = "ArchivalsService";

    void disposeVolumes(List<NodeRef> selectedVolumes, Date destructionStartDate, String docDeletingComment, NodeRef activityRef, NodeRef templateRef, String logMessageKey);

    List<Function> getArchivedFunctions();

    boolean isSimpleDestructionEnabled();

    NodeRef addArchivalActivity(ActivityType activityType, ActivityStatus activityStatus);

    NodeRef addArchivalActivity(ActivityType activityType, ActivityStatus activityStatus, List<NodeRef> volumeRefs, NodeRef templateRef);

    void archiveVolumesOrCaseFiles(List<NodeRef> volumesToArchive);

    NodeRef archiveVolumeOrCaseFile(NodeRef volumeNodeRef);

    void setNewReviewDate(List<NodeRef> volumes, Date reviewDate, NodeRef activityRef);

    void markForTransfer(List<NodeRef> selectedVolumes, NodeRef activityRef);

    void setNextEventDestruction(List<NodeRef> volumes, Date reviewDate, NodeRef activityRef);

    void confirmTransfer(List<NodeRef> selectedVolumes, Date confirmationDate, NodeRef activityRef);

    void exportToUam(List<NodeRef> selectedVolumes, Date exportStartDate, NodeRef activityRef);

    void setDisposalActCreated(List<NodeRef> volumes, NodeRef activityRef);

    ArchivalActivity getArchivalActivity(NodeRef archivalActivityRef);

    void addArchivalActivityDocument(NodeRef archivalActivityRef, NodeRef docRef);

    List<File> getArchivalActivityFiles(NodeRef archivalActivityRef);

=======
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.archivals.model.ArchiveJobStatus;
import ee.webmedia.alfresco.functions.model.Function;

public interface ArchivalsService {
    String BEAN_NAME = "ArchivalsService";

    void archiveVolume(NodeRef archivingJobRef);

    int destroyArchivedVolumes();

    void destroyArchivedVolumes(ActionEvent event);

    List<Function> getArchivedFunctions();

    NodeRef getArchivalRoot();

    List<NodeRef> getAllInQueueJobs();

    ArchiveJobStatus getArchivingStatus(NodeRef archivingJobNodeRef);

    void removeVolumeFromArchivingList(NodeRef volumeRef);

    void markArchivingJobAsRunning(NodeRef archivingJobNodeRef);

    void addVolumeToArchivingList(NodeRef volumeRef);

    void removeJobNodeFromArchivingList(NodeRef archivingJobRef);

    boolean isArchivingPaused();

    void doPauseArchiving();

    void cancelAllArchivingJobs(ActionEvent event);

    void pauseArchiving(ActionEvent event);

    void continueArchiving(ActionEvent event);
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
