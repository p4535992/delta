package ee.webmedia.alfresco.common.externalsession.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.filter.beans.DependencyInjectedFilter;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.app.servlet.AuthenticationHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.AssertionImpl;

import ee.webmedia.alfresco.common.externalsession.service.ExternalSessionService;

public class ExternalSessionAuthenticationFilter implements DependencyInjectedFilter {
    private static Log logger = LogFactory.getLog(ExternalSessionAuthenticationFilter.class);

    private AuthenticationService authService;
    private PersonService personService;
    private TransactionService transactionService;
    private ExternalSessionService externalSessionService;

    public void setAuthenticationService(AuthenticationService authService) {
        this.authService = authService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setExternalSessionService(ExternalSessionService externalSessionService) {
        this.externalSessionService = externalSessionService;
    }

    @Override
    public void doFilter(ServletContext context, ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        // Assume it's an HTTP request
        HttpServletRequest httpReq = (HttpServletRequest) req;
        String parameter = CommonUtils.safeGetParameter(httpReq, "externalSessionId");

        if (StringUtils.isBlank(parameter)) {
            chain.doFilter(req, resp);
            return;
        }
        // Check if the request includes an authentication ticket
        final HttpSession session = httpReq.getSession(false);
        String currentUsername = externalSessionService.getUserForSession(parameter);

        if (StringUtils.isNotBlank(currentUsername)) {
            if (!personService.personExists(currentUsername)) {
                chain.doFilter(req, resp);
                return;
            }
            AuthenticationUtil.setFullyAuthenticatedUser(currentUsername);
            String ticket = authService.getNewTicket();
            authService.validate(ticket);
            AuthenticationHelper.setUser(context, (HttpServletRequest) req, currentUsername, ticket, false);
            session.setAttribute(AbstractCasFilter.CONST_CAS_ASSERTION, new AssertionImpl(currentUsername));
        }
        chain.doFilter(req, resp);
    }

}
