package ee.webmedia.alfresco.document.einvoice.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;

/**
 * Delete expired dimension values that are not used in any document or transaction template
 */
public class EinvoiceDeleteNotUsedDimensionValuesJob implements StatefulJob {
    private static final Log LOG = LogFactory.getLog(EinvoiceDeleteNotUsedDimensionValuesJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Starting EinvoiceDeleteNotUsedDimensionValuesJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get(EInvoiceService.BEAN_NAME);
        if (workerObj == null || !(workerObj instanceof EInvoiceService)) {
            throw new AlfrescoRuntimeException("EinvoiceDeleteNotUsedDimensionValuesJob data must contain valid 'EInvoiceService' reference");
        }
        final EInvoiceService worker = (EInvoiceService) workerObj;

        if (!worker.isEinvoiceEnabled()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Einvoice functionality is disabled, skipping deleting dimension values.");
            }
            return;
        }

        // Run job as with systemUser privileges
        final Integer nrOfDimensionValuesDeleted = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.deleteUnusedDimensionValues();
            }
        }, AuthenticationUtil.getSystemUserName());
        if (LOG.isDebugEnabled()) {
            LOG.debug("EinvoiceDeleteNotUsedDimensionValuesJob done, " + nrOfDimensionValuesDeleted
                    + " unused dimension values deleted.");
        }
    }
}
