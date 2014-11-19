package ee.webmedia.alfresco.simdhs.servlet;

<<<<<<< HEAD
import static ee.webmedia.alfresco.common.web.BeanHelper.getApplicationService;

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
<<<<<<< HEAD
import javax.servlet.ServletOutputStream;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.util.Pair;
import org.alfresco.web.app.servlet.AuthenticationStatus;
import org.alfresco.web.app.servlet.BaseServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
<<<<<<< HEAD
import org.springframework.util.FileCopyUtils;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

import ee.webmedia.alfresco.common.listener.ExternalAccessPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListener;
import ee.webmedia.alfresco.common.listener.StatisticsPhaseListenerLogColumn;

/**
 * Servlet allowing external URL access to various global JSF views in the Web Client.
 * Available URL-s: <li><code>http://&lt;server&gt;/simdhs/&lt;servlet name&gt;/document/&lt;document node ref&gt;</code> - for viewing document</li>
<<<<<<< HEAD
 * 
 * @author Romet Aidla
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class ExternalAccessServlet extends BaseServlet {
    private static final long serialVersionUID = 7348802704715012097L;

    private static Log logger = LogFactory.getLog(ExternalAccessServlet.class);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // req.getPathInfo = /document/3ab24e39-f9b1-4e8a-9e4e-129134a4fed0
        // req.getRequestURI = /dhs/n/document/3ab24e39-f9b1-4e8a-9e4e-129134a4fed0;jSESSIONID=CFE6CC4F12D658B180EB61EF7ABC1C9
        String uri = req.getPathInfo();

        if (logger.isDebugEnabled()) {
            logger.debug("Processing URL: " + uri + (req.getQueryString() != null ? ("?" + req.getQueryString()) : ""));
        }

        if (AuthenticationStatus.Failure == servletAuthenticate(req, res)) {
            return;
        }
<<<<<<< HEAD

        if ("/logo".equals(uri)) {
            Pair<byte[], String> logo = getApplicationService().getCustomLogo();
            if (logo != null) {
                res.setHeader("Content-Length", Integer.toString(logo.getFirst().length));
                res.setContentType(logo.getSecond());
                ServletOutputStream os = res.getOutputStream();
                FileCopyUtils.copy(logo.getFirst(), os); // closes both streams
            }
            return;
        }

        if ("/jumploader".equals(uri)) {
            Pair<byte[], String> applet = getApplicationService().getJumploaderApplet();
            if (applet != null) {
                res.setHeader("Content-Length", Integer.toString(applet.getFirst().length));
                res.setContentType(applet.getSecond());
                ServletOutputStream os = res.getOutputStream();
                FileCopyUtils.copy(applet.getFirst(), os); // closes both streams
            }
            return;
        }

        setNoCacheHeaders(res);
=======
        setNoCacheHeaders(res);

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        Pair<String, String[]> outcomeAndArgs = getDocumentUriTokens(uri);
        req.setAttribute(ExternalAccessPhaseListener.OUTCOME_AND_ARGS_ATTR, outcomeAndArgs);

        StatisticsPhaseListener.add(StatisticsPhaseListenerLogColumn.ACTION, outcomeAndArgs.getFirst());

        // Now handleNavigation puts this as the first item in the view stack
        getServletContext().getRequestDispatcher(FACES_SERVLET + "/jsp/dashboards/container.jsp").forward(req, res);
    }

    public static Pair<String, String[]> getDocumentUriTokens(String uri) {
        Pair<String, String[]> outcomeAndArgs = new Pair<String, String[]>(null, null);
        StringTokenizer t = new StringTokenizer(uri, "/");
        int tokenCount = t.countTokens();
        if (tokenCount < 2) {
            throw new IllegalArgumentException("Externally addressable URL did not contain all required args: " + uri);
        }
        // 1. outcome
        outcomeAndArgs.setFirst(t.nextToken());
        // 2. rest of the tokens arguments
        outcomeAndArgs.setSecond(extractArguments(t, tokenCount));
        return outcomeAndArgs;
    }

    private static String[] extractArguments(StringTokenizer t, int tokenCount) {
        String[] args = new String[tokenCount - 1];
        for (int i = 0; i < tokenCount - 1; i++) {
            args[i] = t.nextToken();
        }
        return args;
    }

}
