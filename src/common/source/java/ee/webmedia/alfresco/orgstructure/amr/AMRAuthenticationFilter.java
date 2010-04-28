package ee.webmedia.alfresco.orgstructure.amr;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.AuthenticationFilter;
import org.alfresco.web.app.servlet.AuthenticationHelper;
import org.alfresco.web.app.servlet.AuthenticationStatus;
import org.alfresco.web.app.servlet.BaseServlet;

/**
 * AuthenticationFilter that uses AMRService for authentication.
 * 
 * @author Ats Uiboupin
 */
public class AMRAuthenticationFilter extends AuthenticationFilter {
    public static final String AUTHENTICATION_EXCEPTION = "AUTHENTICATION_EXCEPTION";

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
        } else {
            if (isAuthenticationException(httpReq)) {
                BaseServlet.redirectToLoginPage(httpReq, httpRes, context);
            } else {
                AuthenticationStatus status;
                try {
                    status = AuthenticationHelper.authenticate(context, httpReq, httpRes, false);
                } catch (AMRAuthenticationException e) {
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
                    chain.doFilter(httpReq, httpRes);// continue filter chaining
                    return;
                }
                BaseServlet.redirectToLoginPage(httpReq, httpRes, context);
            }
        }
    }

    private boolean isAuthenticationException(HttpServletRequest httpReq) {
        boolean isAuthenticationException = "true".equalsIgnoreCase((String) httpReq.getSession().getAttribute(AUTHENTICATION_EXCEPTION));
        return isAuthenticationException;
    }

}
