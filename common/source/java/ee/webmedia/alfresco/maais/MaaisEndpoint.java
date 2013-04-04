package ee.webmedia.alfresco.maais;

import static ee.webmedia.alfresco.common.web.BeanHelper.getApplicationService;
import static ee.webmedia.alfresco.maais.service.MaaisServiceImpl.WS_SERVER_FACTORY;

import java.util.Date;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.util.Pair;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;

import ee.webmedia.alfresco.common.externalsession.service.ExternalSessionService;
import ee.webmedia.alfresco.maais.generated.server.AuthRequest;
import ee.webmedia.alfresco.maais.generated.server.AuthResponse;
import ee.webmedia.alfresco.maais.generated.server.CatalogStructureElement;
import ee.webmedia.alfresco.maais.generated.server.CatalogStructureRequest;
import ee.webmedia.alfresco.maais.generated.server.CatalogStructureResponse;
import ee.webmedia.alfresco.maais.generated.server.RegisterDocumentRequest;
import ee.webmedia.alfresco.maais.generated.server.RegisterDocumentResponse;
import ee.webmedia.alfresco.maais.service.MaaisService;
import ee.webmedia.alfresco.maais.service.MaaisServiceImpl;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.XmlUtil;

/**
 * @author Keit Tehvan
 */
@Endpoint
public class MaaisEndpoint {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(MaaisEndpoint.class);

    private ExternalSessionService externalSessionService;
    private DocumentTemplateService documentTemplateService;
    private MaaisService maaisService;

    @PayloadRoot(localPart = "AuthRequest", namespace = "http://delta/maais/schemas")
    public AuthResponse authRequest(final AuthRequest request) {
        if (getApplicationService().isTest()) {
            System.setProperty("jaxb.debug", "true");
        }
        LOG.debug(MaaisServiceImpl.toString(request));
        RunAsWork<AuthResponse> runAsWork = new RunAsWork<AuthResponse>() {
            @Override
            public AuthResponse doWork() throws Exception {
                Pair<String, Date> created = externalSessionService.createSession(request.getUsername());
                AuthResponse response = WS_SERVER_FACTORY.createAuthResponse();
                String sessIdParam = "?externalSessionId=" + created.getFirst();
                response.setSessionIdWithDelimiter(sessIdParam);
                response.setUrl(documentTemplateService.getServerUrl() + "/" + sessIdParam);
                response.setUrlExpirationTime(XmlUtil.getXmlGregorianCalendar(created.getSecond()));
                return response;
            }
        };
        return AuthenticationUtil.runAs(runAsWork, AuthenticationUtil.getSystemUserName());
    }

    @PayloadRoot(localPart = "CatalogStructureRequest", namespace = "http://delta/maais/schemas")
    public CatalogStructureResponse catalogStructure(final CatalogStructureRequest request) {
        if (getApplicationService().isTest()) {
            System.setProperty("jaxb.debug", "true");
        }
        LOG.debug(MaaisServiceImpl.toString(request));
        RunAsWork<CatalogStructureResponse> runAsWork = new RunAsWork<CatalogStructureResponse>() {
            @Override
            public CatalogStructureResponse doWork() throws Exception {
                CatalogStructureResponse response = WS_SERVER_FACTORY.createCatalogStructureResponse();
                List<CatalogStructureElement> catalogStructure = response.getCatalogStructure();
                String documentTemplate = request.getDocumentTemplate();
                List<CatalogStructureElement> generateCatalogStructureFromTemplateName = maaisService.generateCatalogStructureFromTemplateName(documentTemplate);
                catalogStructure.addAll(generateCatalogStructureFromTemplateName);
                return response;
            }
        };
        return AuthenticationUtil.runAs(runAsWork, AuthenticationUtil.getSystemUserName());
    }

    @PayloadRoot(localPart = "RegisterDocumentRequest", namespace = "http://delta/maais/schemas")
    public RegisterDocumentResponse registerDocument(final RegisterDocumentRequest request) {
        if (getApplicationService().isTest()) {
            System.setProperty("jaxb.debug", "true");
        }
        LOG.debug(MaaisServiceImpl.toString(request));
        RunAsWork<RegisterDocumentResponse> runAsWork = new RunAsWork<RegisterDocumentResponse>() {
            @Override
            public RegisterDocumentResponse doWork() throws Exception {
                return maaisService.registerMaaisDocument(request);
            }
        };
        return AuthenticationUtil.runAs(runAsWork, AuthenticationUtil.getSystemUserName());
    }

    public void setMaaisService(MaaisService maaisService) {
        this.maaisService = maaisService;
    }

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    public void setExternalSessionService(ExternalSessionService externalSessionService) {
        this.externalSessionService = externalSessionService;
    }

}
