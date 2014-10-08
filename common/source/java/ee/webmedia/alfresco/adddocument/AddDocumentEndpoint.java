package ee.webmedia.alfresco.adddocument;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import ee.webmedia.alfresco.adddocument.generated.AddDocumentRequest;
import ee.webmedia.alfresco.adddocument.generated.AddDocumentResponse;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.monitoring.MonitoredService;
import ee.webmedia.alfresco.monitoring.MonitoringUtil;

@Endpoint
public class AddDocumentEndpoint {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AddDocumentEndpoint.class);

    @PayloadRoot(localPart = "addDocumentRequest", namespace = "http://delta/addDocument/schemas")
    public AddDocumentResponse addDocument(final AddDocumentRequest request) {
        if (BeanHelper.getApplicationService().isTest()) {
            System.setProperty("jaxb.debug", "true");
        }
        try {
            return AuthenticationUtil.runAs(new RunAsWork<AddDocumentResponse>() {
                @Override
                public AddDocumentResponse doWork() throws Exception {
                    AddDocumentResponse importDocument = BeanHelper.getAddDocumentService().importDocument(request);
                    MonitoringUtil.logSuccess(MonitoredService.IN_ADD_DOCUMENT);
                    return importDocument;
                }
            }, AuthenticationUtil.getSystemUserName());
        } catch (AddDocumentException e) {
            MonitoringUtil.logSuccess(MonitoredService.IN_ADD_DOCUMENT);
            throw e;
        } catch (Exception e) {
            LOG.error("Error adding document from request: " + request.toString(), e);
            MonitoringUtil.logError(MonitoredService.IN_ADD_DOCUMENT, e);
            throw new RuntimeException("Unknown error: " + e.toString(), e);
        }

    }

}
