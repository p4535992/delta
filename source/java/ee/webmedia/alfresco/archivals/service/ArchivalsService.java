package ee.webmedia.alfresco.archivals.service;

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
}
