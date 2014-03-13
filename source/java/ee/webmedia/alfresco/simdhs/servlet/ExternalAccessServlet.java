package ee.webmedia.alfresco.simdhs.servlet;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.AuthenticationStatus;
import org.alfresco.web.app.servlet.BaseServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener;

/**
 * Servlet allowing external URL access to various global JSF views in the Web Client.
 * Available URL-s: <li><code>http://&lt;server&gt;/simdhs/&lt;servlet name&gt;/document/&lt;document node ref&gt;</code> - for viewing document</li>
 */
public class ExternalAccessServlet extends BaseServlet {
    private static final long serialVersionUID = 7348802704715012097L;

    private static Log logger = LogFactory.getLog(ExternalAccessServlet.class);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String uri = req.getRequestURI();

        if (logger.isDebugEnabled()) {
            logger.debug("Processing URL: " + uri + (req.getQueryString() != null ? ("?" + req.getQueryString()) : ""));
        }

        if (AuthenticationStatus.Failure == servletAuthenticate(req, res)) {
            return;
        }
        setNoCacheHeaders(res);

        Pair<String, String[]> outcomeAndArgs = getDocumentUriTokens(req.getContextPath().length(), uri);
        req.setAttribute(ExternalAccessPhaseListener.OUTCOME_AND_ARGS_ATTR, outcomeAndArgs);

        // Now handleNavigation puts this as the first item in the view stack
        getServletContext().getRequestDispatcher(FACES_SERVLET + "/jsp/dashboards/container.jsp").forward(req, res);
    }

    public static Pair<String, String[]> getDocumentUriTokens(int substringLength, String uri) {
        Pair<String, String[]> outcomeAndArgs = new Pair<String, String[]>(null, null);
        if (substringLength > 0) {
            uri = uri.substring(substringLength);
        }
        StringTokenizer t = new StringTokenizer(uri, "/");
        int tokenCount = t.countTokens();
        if (tokenCount < 2) {
            throw new IllegalArgumentException("Externally addressable URL did not contain all required args: " + uri);
        }
        // 1. servlet name (not used)
        t.nextToken();
        // 2. outcome
        outcomeAndArgs.setFirst(t.nextToken());
        // 3. rest of the tokens arguments
        outcomeAndArgs.setSecond(extractArguments(t, tokenCount));
        return outcomeAndArgs;
    }

    private static String[] extractArguments(StringTokenizer t, int tokenCount) {
        String[] args = new String[tokenCount - 2];
        for (int i = 0; i < tokenCount - 2; i++) {
            args[i] = t.nextToken();
        }
        return args;
    }

}
