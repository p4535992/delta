package ee.webmedia.alfresco.volume.job;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
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
    ArchivalsService archivalsService;
    NodeService nodeService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Starting ExecuteArchiveJob");
        setServices(context);

        while (!archivalsService.isArchivingPaused()) {
            List<NodeRef> jobList = archivalsService.getAllInQueueJobs();
            if (jobList.isEmpty()) {
                break;
            }
            NodeRef archivingJobRef = jobList.get(0);
            ArchiveJobStatus jobStatus = archivalsService.getArchivingStatus(archivingJobRef);
            if (ArchiveJobStatus.IN_PROGRESS.equals(jobStatus)) {
                // archiving is executed in this thread so IN_PROGRESS job should only get here if server was restarted in the middle of archiving
                NodeRef volumeRef = (NodeRef) nodeService.getProperty(archivingJobRef, ArchivalsModel.Props.VOLUME_REF);
                if (!nodeService.exists(volumeRef)) {
                    LOG.warn("Archiving of volume (nodeRef=" + volumeRef + ") was removed from archiving job list.");
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
            archivalsService.archiveVolume(archivingJobRef);
        }
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

    private void setServices(JobExecutionContext context) {
        archivalsService = BeanHelper.getArchivalsService();
        nodeService = BeanHelper.getNodeService();
    }

}
