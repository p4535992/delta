package ee.webmedia.alfresco.log.web;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;
import org.springframework.web.filter.GenericFilterBean;

import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;

public class LogInOutAuditFilter extends GenericFilterBean {

    private LogService logService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        final HttpSession session = request.getSession(false);
        final Assertion assertion = session != null ? (Assertion) session.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION) : null;
        final String ticket = CommonUtils.safeGetParameter(request, "ticket");

        boolean isAuthenticating = assertion != null && CommonUtils.isNotBlank(ticket);
        chain.doFilter(request, response);

        if (isAuthenticating && assertion != null && response.isCommitted()) {
            logService.addLogEntry(LogEntry.create(LogObject.LOG_IN_OUT, assertion.getPrincipal().getName(), "applog_login_failed"));
        }
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }
}
