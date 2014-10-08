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

public class EinvoiceUpdateDocumentSapAccountJob implements StatefulJob {
    private static final Log LOG = LogFactory.getLog(EinvoiceUpdateDocumentSapAccountJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.debug("Starting EinvoiceUpdateDocumentSapAccountJob");
        JobDataMap jobData = context.getJobDetail().getJobDataMap();
        Object workerObj = jobData.get(EInvoiceService.BEAN_NAME);
        if (workerObj == null || !(workerObj instanceof EInvoiceService)) {
            throw new AlfrescoRuntimeException("EinvoiceUpdateDocumentSapAccountJob data must contain valid 'EInvoiceService' reference");
        }
        final EInvoiceService worker = (EInvoiceService) workerObj;

        if (!worker.isEinvoiceEnabled()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Einvoice functionality is disabled, skipping  document SAP account update.");
            }
            return;
        }

        // Run job as with systemUser privileges
        final Integer nrOfDocumentsUpdated = AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return worker.updateDocumentsSapAccount();
            }
        }, AuthenticationUtil.getSystemUserName());
        if (LOG.isDebugEnabled()) {
            LOG.debug("EinvoiceUpdateDocumentSapAccountJob done, " + nrOfDocumentsUpdated
                    + " documents' sap account updated.");
        }
    }
}
