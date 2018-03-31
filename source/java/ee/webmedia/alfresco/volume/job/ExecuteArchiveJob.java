package ee.webmedia.alfresco.volume.job;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.archivals.model.ArchivalsModel;
import ee.webmedia.alfresco.archivals.model.ArchiveJobStatus;
import ee.webmedia.alfresco.archivals.service.ArchivalsService;
import ee.webmedia.alfresco.common.web.BeanHelper;

public class ExecuteArchiveJob implements StatefulJob {

    private static final Log LOG = LogFactory.getLog(ExecuteArchiveJob.class);

    private ArchivalsService archivalsService;
    private NodeService nodeService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
    	setServices();
        LOG.debug("Starting ExecuteArchiveJob");
        
        if(archivalsService.isDestructionJobInProgress()) {
        	LOG.debug("Destruction job is in-progress, exiting from Archive job.");	
        	return;
        }

        if(!archivalsService.setArchiveJobInProgress(true)) {
        	return;
        }
        
        while (!archivalsService.isArchivingPaused() && archivalsService.isArchivingAllowed()) {
            List<NodeRef> jobList = archivalsService.getAllInQueueJobs();
            if (jobList.isEmpty()) {
                archivalsService.resetManualActions();
                break;
            }
            Pair<NodeRef, ArchiveJobStatus> jobRefAndStatus = getNextArchivingJobWithStatus(jobList);
            NodeRef archivingJobRef = jobRefAndStatus.getFirst();
            ArchiveJobStatus jobStatus = jobRefAndStatus.getSecond();
            boolean resumingPaused = ArchiveJobStatus.PAUSED.equals(jobStatus);
            if (ArchiveJobStatus.IN_PROGRESS.equals(jobStatus) || resumingPaused) {
                // archiving is executed in this thread so IN_PROGRESS job should only get here if server was restarted in the middle of archiving
                NodeRef volumeRef = (NodeRef) nodeService.getProperty(archivingJobRef, ArchivalsModel.Props.VOLUME_REF);
                if (!nodeService.exists(volumeRef)) {
                    LOG.warn("Archiving of volume (nodeRef=" + volumeRef + ") was removed from archiving job list.");
                    if (resumingPaused) {
                        LOG.warn("Unable to resume paused archiving job");
                    }
                    archivalsService.removeJobNodeFromArchivingList(archivingJobRef);
                    continue;
                }
            }
            if (ArchiveJobStatus.FAILED.equals(jobStatus) || ArchiveJobStatus.FINISHED.equals(jobStatus)) {
                logArchiveResult(archivingJobRef);
                archivalsService.removeJobNodeFromArchivingList(archivingJobRef);
                continue;
            }
            archivalsService.markArchivingJobAsRunning(archivingJobRef);
            archivalsService.archiveVolumeOrCaseFile(archivingJobRef, resumingPaused);
        }
        
        archivalsService.setArchiveJobInProgress(false);
    }

    private Pair<NodeRef, ArchiveJobStatus> getNextArchivingJobWithStatus(List<NodeRef> jobList) {
        Map<NodeRef, Node> jobs = BeanHelper.getBulkLoadNodeService().loadNodes(jobList, Collections.singleton(ArchivalsModel.Props.ARCHIVING_JOB_STATUS));
        for (Map.Entry<NodeRef, Node> entry : jobs.entrySet()) {
            ArchiveJobStatus status = getStatus(entry.getValue());
            if (ArchiveJobStatus.PAUSED.equals(status)) {
                return Pair.newInstance(entry.getKey(), status);
            }
        }
        Node jobNode = jobs.get(jobList.get(0));
        return Pair.newInstance(jobList.get(0), getStatus(jobNode));
    }

    private ArchiveJobStatus getStatus(Node entry) {
        Map<String, Object> jobProps = entry.getProperties();
        return ArchiveJobStatus.valueOf((String) jobProps.get(ArchivalsModel.Props.ARCHIVING_JOB_STATUS.toString()));
    }

    private void logArchiveResult(NodeRef archivingJobRef) {
        Map<QName, Serializable> props = nodeService.getProperties(archivingJobRef);
        NodeRef volumeRef = (NodeRef) props.get(ArchivalsModel.Props.VOLUME_REF);
        ArchiveJobStatus status = ArchiveJobStatus.valueOf((String) props.get(ArchivalsModel.Props.ARCHIVING_JOB_STATUS));
        switch (status) {
        case FAILED:
            LOG.warn("Archiving of volume (nodeRef=" + volumeRef + ") failed with the following error message:\n" + props.get(ArchivalsModel.Props.ERROR_MESSAGE));
            break;
        case FINISHED:
            LOG.info("Archiving of volume (nodeRef=" + volumeRef + ") has been completed successfully.\n" +
                    "Archiving started at " + props.get(ArchivalsModel.Props.ARCHIVING_START_TIME) + " and ended at " + props.get(ArchivalsModel.Props.ARCHIVING_END_TIME));
            break;
        }
    }

    private void setServices() {
        archivalsService = BeanHelper.getArchivalsService();
        nodeService = BeanHelper.getNodeService();
    }

}
