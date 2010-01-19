package ee.webmedia.alfresco.webdav;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.alfresco.repo.webdav.WebDAV;
import org.alfresco.repo.webdav.WebDAVServlet;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

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

        // Create the WebDAV helper
        m_davHelper = new WebDAVCustomHelper(serviceRegistry, authService, versionsService);

        // Create the WebDAV methods table

        m_davMethods.put(WebDAV.METHOD_COPY, ForbiddenMethod.class);
        m_davMethods.put(WebDAV.METHOD_DELETE, ForbiddenMethod.class);
        m_davMethods.put(WebDAV.METHOD_MKCOL, ForbiddenMethod.class);
        m_davMethods.put(WebDAV.METHOD_MOVE, ForbiddenMethod.class);
        m_davMethods.put(WebDAV.METHOD_LOCK, LockMethod.class);
        m_davMethods.put(WebDAV.METHOD_PUT, PutMethod.class);
        m_davMethods.put(WebDAV.METHOD_UNLOCK, UnlockMethod.class);
    }

}
