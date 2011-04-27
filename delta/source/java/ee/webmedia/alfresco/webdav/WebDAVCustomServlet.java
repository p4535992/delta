package ee.webmedia.alfresco.webdav;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVServerException;
import org.alfresco.repo.webdav.WebDAVServlet;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.versions.service.VersionsService;

public class WebDAVCustomServlet extends WebDAVServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());

        ServiceRegistry serviceRegistry = (ServiceRegistry) context.getBean(ServiceRegistry.SERVICE_REGISTRY);
        AuthenticationService authService = (AuthenticationService) context.getBean("authenticationService");
        VersionsService versionsService = (VersionsService) context.getBean(VersionsService.BEAN_NAME);
        DocumentService documentService = (DocumentService) context.getBean(DocumentService.BEAN_NAME);

        // Create the WebDAV helper
        m_davHelper = new WebDAVCustomHelper(serviceRegistry, authService, versionsService, documentService);

        // Create the WebDAV methods table

        m_davMethods.put(WebDAV.METHOD_GET, GetMethod.class);
        m_davMethods.put(WebDAV.METHOD_COPY, ForbiddenMethod.class);
        m_davMethods.put(WebDAV.METHOD_DELETE, ForbiddenMethod.class);
        m_davMethods.put(WebDAV.METHOD_MKCOL, ForbiddenMethod.class);
        m_davMethods.put(WebDAV.METHOD_MOVE, ForbiddenMethod.class);
        m_davMethods.put(WebDAV.METHOD_LOCK, LockMethod.class);
        m_davMethods.put(WebDAV.METHOD_PUT, PutMethod.class);
        m_davMethods.put(WebDAV.METHOD_UNLOCK, UnlockMethod.class);
    }

    @Override
    protected void sendErrorResponse(HttpServletResponse response, WebDAVServerException error) throws IOException {
        logger.error("Failed to serve webdav request - sending error page", error);
        response.setStatus(error.getHttpStatusCode());
        response.getWriter().append(getResponseText(error));
    }

    private String getResponseText(WebDAVServerException error) throws IOException, UnsupportedEncodingException {
        final InputStream is = new ClassPathResource("org/alfresco/repo/webdav/error.html").getInputStream();
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            is.close();
        }
        String responseText = sb.toString();
        responseText = StringUtils.replace(responseText, "{webdav_error_}", I18NUtil.getMessage("webdav_error_" + error.getHttpStatusCode()));
        return responseText;
    }
}
