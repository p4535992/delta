package ee.webmedia.alfresco.archivals.service;

import java.util.Date;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.archivals.model.ActivityStatus;
import ee.webmedia.alfresco.archivals.model.ActivityType;
import ee.webmedia.alfresco.archivals.model.ArchiveJobStatus;
import ee.webmedia.alfresco.archivals.web.ArchivalActivity;
import ee.webmedia.alfresco.document.file.model.File;
import ee.webmedia.alfresco.functions.model.UnmodifiableFunction;

public interface ArchivalsService {
    String BEAN_NAME = "ArchivalsService";

    void disposeVolumes(List<NodeRef> selectedVolumes, Date destructionStartDate, String docDeletingComment, NodeRef activityRef, NodeRef templateRef, String logMessageKey);

    List<UnmodifiableFunction> getArchivedFunctions();

    boolean isSimpleDestructionEnabled();

    NodeRef addArchivalActivity(ActivityType activityType, ActivityStatus activityStatus);

    NodeRef addArchivalActivity(ActivityType activityType, ActivityStatus activityStatus, List<NodeRef> volumeRefs, NodeRef templateRef);

    void archiveVolumeOrCaseFile(NodeRef volumeNodeRef);

    void setNewReviewDate(List<NodeRef> volumes, Date reviewDate, NodeRef activityRef);

    void markForTransfer(List<NodeRef> selectedVolumes, NodeRef activityRef);

    void setNextEventDestruction(List<NodeRef> volumes, Date reviewDate, NodeRef activityRef);

    void confirmTransfer(List<NodeRef> selectedVolumes, Date confirmationDate, NodeRef activityRef);

    void exportToUam(List<NodeRef> selectedVolumes, Date exportStartDate, NodeRef activityRef);

    void setDisposalActCreated(List<NodeRef> volumes, NodeRef activityRef);

    ArchivalActivity getArchivalActivity(NodeRef archivalActivityRef);

    void addArchivalActivityDocument(NodeRef archivalActivityRef, NodeRef docRef);

    List<File> getArchivalActivityFiles(NodeRef archivalActivityRef);

    NodeRef getArchivalRoot();

    List<NodeRef> getAllInQueueJobs();

    ArchiveJobStatus getArchivingStatus(NodeRef archivingJobNodeRef);

    void removeVolumeFromArchivingList(NodeRef volumeRef);

    void markArchivingJobAsRunning(NodeRef archivingJobNodeRef);

    void addVolumeOrCaseToArchivingList(NodeRef volumeRef);

    void removeJobNodeFromArchivingList(NodeRef archivingJobRef);

    boolean isVolumeInArchivingQueue(NodeRef volumeOrCaseFileRef);

    boolean isArchivingPaused();

    void doPauseArchiving();

    void cancelAllArchivingJobs(ActionEvent event);

    void pauseArchiving(ActionEvent event);

    void continueArchiving(ActionEvent event);

    boolean isArchivingContinuedManually();

    void resetManualActions();

}
