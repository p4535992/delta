package ee.webmedia.alfresco.archivals.service;

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

}
