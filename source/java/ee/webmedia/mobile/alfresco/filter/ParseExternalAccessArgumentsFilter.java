package ee.webmedia.mobile.alfresco.filter;

import static ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener.OUTCOME_CASE_FILE;
import static ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener.OUTCOME_COMPOUND_WORKFLOW_NODEREF;
import static ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener.OUTCOME_DOCUMENT;
import static ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener.OUTCOME_VOLUME;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.simdhs.servlet.ExternalAccessServlet;
import ee.webmedia.mobile.alfresco.workflow.CompundWorkflowDetailsController;

public class ParseExternalAccessArgumentsFilter implements Filter {

    public static final String MOBILE_REDIRECT_UNAVAILABLE = "redirectUnavailable";
    private static final List<String> validNavigationParts = Arrays.asList(OUTCOME_CASE_FILE, OUTCOME_COMPOUND_WORKFLOW_NODEREF, OUTCOME_DOCUMENT, OUTCOME_VOLUME);

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        String uri = request.getPathInfo();
        IllegalArgumentException parseException = null;
        Pair<String, String[]> outcomeAndArgs = null;
        if (!ExternalAccessServlet.isLogoUri(uri) && !ExternalAccessServlet.isJumploaderUri(uri)) {
            try {
                outcomeAndArgs = ExternalAccessServlet.getDocumentUriTokens(uri);
            } catch (IllegalArgumentException e) {
                parseException = e;
            }
            org.springframework.mobile.device.Device device = (org.springframework.mobile.device.Device) request
                    .getAttribute(org.springframework.mobile.device.DeviceUtils.CURRENT_DEVICE_ATTRIBUTE);

            if (device != null && (device.isTablet() || device.isMobile())) {
                if (parseException != null) {
                    redirectWithMessage(request, response, "redirect.unavailable");
                }
                String objectType = outcomeAndArgs.getFirst();
                String nodeId = outcomeAndArgs.getSecond()[0];
                if (StringUtils.isNotBlank(nodeId) && ExternalAccessPhaseListener.OUTCOME_COMPOUND_WORKFLOW_NODEREF.equals(objectType)) {
                    response.sendRedirect(getMobilePrefixUrl() + "/" + CompundWorkflowDetailsController.COMPOUND_WORKFLOW_DETAILS_MAPPING + "/" + nodeId);
                    return;
                } else if (validNavigationParts.contains(objectType)) {
                    redirectWithMessage(request, response, "redirect.unavailable." + objectType);
                    return;
                }
                redirectWithMessage(request, response, "redirect.unavailable");
                return;
            }
        }

        if (parseException != null) {
            throw parseException;
        }
        request.setAttribute(ExternalAccessPhaseListener.OUTCOME_AND_ARGS_ATTR, outcomeAndArgs);
        chain.doFilter(request, response);
    }

    private void redirectWithMessage(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        HttpSession session = request.getSession();
        session.setAttribute(MOBILE_REDIRECT_UNAVAILABLE, message);
        response.sendRedirect(getMobilePrefixUrl());
    }

    public String getMobilePrefixUrl() {
        return BeanHelper.getDocumentTemplateService().getServerUrl() + "/m";
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {

    }

    @Override
    public void destroy() {

    }

}
