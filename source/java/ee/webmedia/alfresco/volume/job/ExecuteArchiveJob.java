package ee.webmedia.alfresco.volume.job;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.archivals.model.ArchivalsModel;
import ee.webmedia.alfresco.archivals.model.ArchiveJobStatus;
import ee.webmedia.alfresco.archivals.service.ArchivalsService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;

public class ExecuteArchiveJob implements StatefulJob {

    private static final Log LOG = LogFactory.getLog(ExecuteArchiveJob.class);
    private final SimpleDateFormat ARCHIVING_TIME_FORMAT = new SimpleDateFormat("hh:mm");
    private ArchivalsService archivalsService;
    private NodeService nodeService;
    private ParametersService parametersService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Starting ExecuteArchiveJob");
        setServices();

        while (!archivalsService.isArchivingPaused() && isArchivingAllowed()) {
            List<NodeRef> jobList = archivalsService.getAllInQueueJobs();
            if (jobList.isEmpty()) {
                archivalsService.resetManualActions();
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
            archivalsService.archiveVolumeOrCaseFile(archivingJobRef);
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

    private boolean isArchivingAllowed() {
        boolean allowedNow = isArchivingAllowedAtThisTime();

        if (allowedNow) {
            archivalsService.resetManualActions();
            return true;
        }

        if (!allowedNow && archivalsService.isArchivingContinuedManually()) {
            return true;
        }

        return allowedNow;
    }

    private boolean isArchivingAllowedAtThisTime() {
        DateTime now = new DateTime();
        if (Boolean.valueOf(parametersService.getStringParameter(Parameters.CONTINUE_ARCIVING_OVER_WEEKEND))) {
            int weekDay = now.getDayOfWeek();
            if (DateTimeConstants.SATURDAY == weekDay || DateTimeConstants.SUNDAY == weekDay) {
                return true;
            }
        }
        String beginTimeStr = StringUtils.deleteWhitespace(parametersService.getStringParameter(Parameters.ARCHIVING_BEGIN_TIME));
        String endTimeStr = StringUtils.deleteWhitespace(parametersService.getStringParameter(Parameters.ARCHIVING_END_TIME));
        if (StringUtils.isBlank(beginTimeStr) || StringUtils.isBlank(endTimeStr)) {
            return true;
        }
        DateTime beginTime;
        DateTime endTime;
        try {
            beginTime = getDateTime(now, beginTimeStr);
            endTime = getDateTime(now, endTimeStr);
            if (beginTime.isAfter(endTime)) {
                endTime = endTime.plusDays(1);
            }
        } catch (ParseException e) {
            LOG.warn("Unable to parse " + Parameters.ARCHIVING_BEGIN_TIME.getParameterName() + " (value=" + beginTimeStr + ") or "
                    + Parameters.ARCHIVING_END_TIME.getParameterName() + " (value=" + endTimeStr + "), continuing archiving. " +
                    "Required format is " + ARCHIVING_TIME_FORMAT.toPattern());
            return true;
        }
        if (beginTime.isBefore(now) && endTime.isAfter(now)) {
            return true;
        }
        return false;
    }

    private DateTime getDateTime(DateTime now, String timeString) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ARCHIVING_TIME_FORMAT.parse(timeString));
        return new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), 0, 0);
    }

    private void setServices() {
        archivalsService = BeanHelper.getArchivalsService();
        nodeService = BeanHelper.getNodeService();
        parametersService = BeanHelper.getParametersService();
    }

}
