package ee.webmedia.alfresco.help.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;
import static ee.webmedia.alfresco.common.web.BeanHelper.getHelpTextService;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for viewing dialog, document-type, or field help. The URL must contain data <code>/helpType/helpCode</code> following the servlet URL. When an item is not found, HTTP
 * 404 response will be returned.
 */
public class HelpTextServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String CONTENT_HTML = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"><html><head><title>%s</title></head><body>%s</body></html>";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] helpTextInfo = req.getPathInfo().substring(1).split("/");
        String content = null;

        if (helpTextInfo.length == 2) {
            content = getHelpTextService().getHelpContent(helpTextInfo[0], helpTextInfo[1]);
        }

        if (content != null) {
            String title = "Abiinfo";
            content = String.format(CONTENT_HTML, title, content);

            resp.setContentType("text/html;charset=" + CHARSET);
            resp.setContentLength(content.getBytes(CHARSET).length);
            resp.getWriter().write(content);
        } else if (helpTextInfo.length == 2) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Help resource was not found.");
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot serve help resource with provided URL.");
        }
    }
}
