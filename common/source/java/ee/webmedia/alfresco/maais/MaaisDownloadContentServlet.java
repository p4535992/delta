package ee.webmedia.alfresco.maais;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.servlet.BaseDownloadContentServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * security hazard if not properly firewalled.. allows unauthorized download of files
 */
public class MaaisDownloadContentServlet extends BaseDownloadContentServlet {
    private static final long serialVersionUID = -576405943603122206L;

    private static Log logger = LogFactory.getLog(MaaisDownloadContentServlet.class);

    private static final String DOWNLOAD_URL = "/service/maaisdownload/" + URL_ATTACH + "/{0}/{1}/{2}/{3}";
    private static final String BROWSER_URL = "/service/maaisdownload/" + URL_DIRECT + "/{0}/{1}/{2}/{3}";

    @Override
    protected Log getLogger() {
        return logger;
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res)
            throws ServletException, IOException {
        if (!BeanHelper.getMaaisService().isServiceAvailable()) {
            return;
        }
        RunAsWork<Object> runAsWork = new RunAsWork<Object>() {
            @Override
            public Object doWork() throws Exception {
                processDownloadRequest(req, res, true);
                return null;
            }
        };
        AuthenticationUtil.runAs(runAsWork, AuthenticationUtil.getSystemUserName());
    }

    @Override
    protected String getRequestURI(HttpServletRequest req) {
        return super.getRequestURI(req).replace("/service", "");
    }

    /**
     * Helper to generate a URL to a content node for downloading content from the server.
     * The content is supplied as an HTTP1.1 attachment to the response. This generally means
     * a browser should prompt the user to save the content to specified location.
     * 
     * @param ref NodeRef of the content node to generate URL for (cannot be null)
     * @param name File name to return in the URL (cannot be null)
     * @return URL to download the content from the specified node
     */
    public final static String generateDownloadURL(NodeRef ref, String name) {
        return generateUrl(DOWNLOAD_URL, ref, name);
    }

    /**
     * Helper to generate a URL to a content node for downloading content from the server.
     * The content is supplied directly in the reponse. This generally means a browser will
     * attempt to open the content directly if possible, else it will prompt to save the file.
     * 
     * @param ref NodeRef of the content node to generate URL for (cannot be null)
     * @param name File name to return in the URL (cannot be null)
     * @return URL to download the content from the specified node
     */
    public final static String generateBrowserURL(NodeRef ref, String name) {
        return generateUrl(BROWSER_URL, ref, name);
    }
}
