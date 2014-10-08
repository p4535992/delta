package ee.webmedia.alfresco.user.service;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.AuthenticationFilter;
import org.alfresco.web.app.servlet.AuthenticationHelper;
import org.alfresco.web.app.servlet.AuthenticationStatus;
import org.alfresco.web.app.servlet.BaseServlet;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.utils.UserUtil;

/**
 * AuthenticationFilter that uses AMRService for authentication.
 */
public class SimpleAuthenticationFilter extends AuthenticationFilter {
    public static final String AUTHENTICATION_EXCEPTION = "AUTHENTICATION_EXCEPTION";

    private LogService logService;

    @Override
    public void doFilter(ServletContext context, ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;

        String reloginURI = httpReq.getContextPath() + BaseServlet.FACES_SERVLET + Application.getLoginPage(context);
        String requestURI = httpReq.getRequestURI();
        if (requestURI.equalsIgnoreCase(reloginURI)) {
            chain.doFilter(httpReq, httpRes);// continue filter chaining
            httpReq.getSession().invalidate(); // invalidate session so that authentication filter would step in
        } else if (isAuthenticationException(httpReq)) {
            BaseServlet.redirectToLoginPage(httpReq, httpRes, context);
        } else {
            boolean isAuthenticating = httpReq.getSession().getAttribute(AuthenticationHelper.AUTHENTICATION_USER) == null;
            AuthenticationStatus status;
            try {
                status = AuthenticationHelper.authenticate(context, httpReq, httpRes, false);
            } catch (AuthenticationException e) {
                if (log.isWarnEnabled()) {
                    log.warn("Authentication failed: ", e);
                }
                httpReq.getSession().setAttribute(AUTHENTICATION_EXCEPTION, "true");// save attribute that is used to show errMsgin jsp
                status = AuthenticationStatus.Failure;
                // authentication failed - so end servlet execution and redirect to login page
                // also save the requested URL so the login page knows where to redirect too later
            } catch (InvalidNodeRefException e) {
                if (log.isWarnEnabled()) {
                    log.warn("User was deleted, preferences node does not exist", e);
                }
                httpReq.getSession().setAttribute(AUTHENTICATION_EXCEPTION, "true");// save attribute that is used to show errMsgin jsp
                status = AuthenticationStatus.Failure;
            }

            if (status == AuthenticationStatus.Success || status == AuthenticationStatus.Guest) {
                if (isAuthenticating) {
                    logSuccess(AuthenticationHelper.getUser(context, httpReq, httpRes).getUserName());
                }
                chain.doFilter(httpReq, httpRes);// continue filter chaining
            } else {
                if (isAuthenticating) {
                    logFail(null);
                }
                BaseServlet.redirectToLoginPage(httpReq, httpRes, context);
            }
        }
    }

    private boolean isAuthenticationException(HttpServletRequest httpReq) {
        boolean isAuthenticationException = Boolean.parseBoolean((String) httpReq.getSession().getAttribute(AUTHENTICATION_EXCEPTION));
        return isAuthenticationException;
    }

    private void logSuccess(String userName) {
        NodeRef personRef = BeanHelper.getPersonService().getPerson(userName);
        String userFullName = null;
        if (personRef != null) {
            NodeService nodeService = BeanHelper.getNodeService();
            String firstName = (String) nodeService.getProperty(personRef, ContentModel.PROP_FIRSTNAME);
            String lastName = (String) nodeService.getProperty(personRef, ContentModel.PROP_LASTNAME);
            userFullName = UserUtil.getPersonFullName(userName, firstName, lastName);
        }
        logService.addLogEntry(LogEntry.create(LogObject.LOG_IN_OUT, userName, userFullName, null, "applog_login_success"));
    }

    private void logFail(String userName) {
        logService.addLogEntry(LogEntry.create(LogObject.LOG_IN_OUT, userName, "applog_login_failed"));
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }
}
