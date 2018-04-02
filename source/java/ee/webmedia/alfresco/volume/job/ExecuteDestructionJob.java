package ee.webmedia.alfresco.volume.job;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import org.springframework.util.Assert;

import ee.webmedia.alfresco.destruction.model.DestructionModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.destruction.model.DestructionJobStatus;
import ee.webmedia.alfresco.archivals.model.ArchivalsModel;
import ee.webmedia.alfresco.archivals.service.ArchivalsService;
import ee.webmedia.alfresco.common.web.BeanHelper;

public class ExecuteDestructionJob implements StatefulJob {

    private static final Log LOG = LogFactory.getLog(ExecuteDestructionJob.class);

    private ArchivalsService archivalsService;
    private NodeService nodeService;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		setServices();
        LOG.debug("Starting ExecuteDestructionJob");
		
		if (archivalsService.isArchiveJobInProgress()) {
			LOG.debug("Achive job is in-progress, exiting from Destruction job.");
			return;
		}
		
		if(!archivalsService.setDestructionJobInProgress(true))
			return;
		
        
        while (!archivalsService.isDestructionPaused() && archivalsService.isDestructionAllowed()) {
            List<NodeRef> jobList = archivalsService.getAllInQueueJobsForDesruction();
            if (jobList.isEmpty()) {
                archivalsService.resetDestructionManualActions();
                break;
            }
            
            Pair<NodeRef, DestructionJobStatus> jobRefAndStatus = getNextDestructingJobWithStatus(jobList);
            NodeRef destructingJobRef = jobRefAndStatus.getFirst();
            DestructionJobStatus jobStatus = jobRefAndStatus.getSecond();

            boolean resumingPaused = DestructionJobStatus.PAUSED.equals(jobStatus);
            if (DestructionJobStatus.IN_PROGRESS.equals(jobStatus) || resumingPaused) {
                // archiving is executed in this thread so IN_PROGRESS job should only get here if server was restarted in the middle of archiving
                NodeRef volumeRef = (NodeRef) nodeService.getProperty(destructingJobRef, DestructionModel.Props.VOLUME_REF);
                if (!nodeService.exists(volumeRef)) {
                    LOG.warn("Destruction of volume (nodeRef=" + volumeRef + ") was removed from destructing job list.");
                    if (resumingPaused) {
                        LOG.warn("Unable to resume paused destructing job");
                    }
                    archivalsService.removeJobNodeFromDestructingList(destructingJobRef);
                    continue;
                }
            }
            if (DestructionJobStatus.FAILED.equals(jobStatus) || DestructionJobStatus.FINISHED.equals(jobStatus)) {
                logDestructionResult(destructingJobRef);
                archivalsService.removeJobNodeFromDestructingList(destructingJobRef);
                continue;
            }
            
            final Map<QName, Serializable> jobProps = nodeService.getProperties(destructingJobRef);
            final NodeRef volumeNodeRef = (NodeRef) jobProps.get(DestructionModel.Props.VOLUME_REF);
            Assert.notNull(volumeNodeRef, "Reference to volume node must be provided");

            // fill single item list for old methods compatibility.
            List<NodeRef> selectedVolumes = new ArrayList<NodeRef>();
            selectedVolumes.add(volumeNodeRef);
            
            NodeRef activityRef = archivalsService.getDestructionJobArchivalActivity(destructingJobRef);
            archivalsService.markDestructingJobAsRunning(destructingJobRef);

            if (archivalsService.disposeVolumes(selectedVolumes, new Date(), activityRef,
                    null /*templateRef*/, MessageUtil.getMessage("applog_archivals_volume_disposed"))) {
            
            	archivalsService.markDestructionJobFinished(destructingJobRef);
            }
            else {
            	archivalsService.markDestructingJobAsPaused(destructingJobRef);
            }
        }
        
		archivalsService.setDestructionJobInProgress(false);
	}
	
    private Pair<NodeRef, DestructionJobStatus> getNextDestructingJobWithStatus(List<NodeRef> jobList) {
        Map<NodeRef, Node> jobs = BeanHelper.getBulkLoadNodeService().loadNodes(jobList, Collections.singleton(DestructionModel.Props.DESTRUCING_JOB_STATUS));
        for (Map.Entry<NodeRef, Node> entry : jobs.entrySet()) {
            DestructionJobStatus status = getStatus(entry.getValue());
            if (DestructionJobStatus.PAUSED.equals(status)) {
                return Pair.newInstance(entry.getKey(), status);
            }
        }
        Node jobNode = jobs.get(jobList.get(0));
        return Pair.newInstance(jobList.get(0), getStatus(jobNode));
    }

    private DestructionJobStatus getStatus(Node entry) {
        Map<String, Object> jobProps = entry.getProperties();
        return DestructionJobStatus.valueOf((String) jobProps.get(DestructionModel.Props.DESTRUCING_JOB_STATUS.toString()));
    }

    private void logDestructionResult(NodeRef destructingJobRef) {
        Map<QName, Serializable> props = nodeService.getProperties(destructingJobRef);
        NodeRef volumeRef = (NodeRef) props.get(DestructionModel.Props.VOLUME_REF);
        DestructionJobStatus status = DestructionJobStatus.valueOf((String) props.get(DestructionModel.Props.DESTRUCING_JOB_STATUS));
        switch (status) {
        case FAILED:
            LOG.warn("Destructing of volume (nodeRef=" + volumeRef + ") failed with the following error message:\n" + props.get(DestructionModel.Props.ERROR_MESSAGE));
            break;
        case FINISHED:
            LOG.info("Destructing of volume (nodeRef=" + volumeRef + ") has been completed successfully.\n" +
                    "Destructing started at " + props.get(DestructionModel.Props.DESTRUCTING_START_TIME) + " and ended at " + props.get(DestructionModel.Props.DESTRUCTING_END_TIME));
            break;
        }
    }
	
    private void setServices() {
        archivalsService = BeanHelper.getArchivalsService();
        nodeService = BeanHelper.getNodeService();
    }

}
