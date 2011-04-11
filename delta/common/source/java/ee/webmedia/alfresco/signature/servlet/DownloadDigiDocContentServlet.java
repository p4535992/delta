package ee.webmedia.alfresco.signature.servlet;

import java.io.IOException;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.URLDecoder;
import org.alfresco.util.URLEncoder;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.bean.LoginBean;
import org.apache.commons.logging.Log;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.SignatureService;

/**
 * Servlet that can read and return selected files from the DigiDoc container.
 */
public class DownloadDigiDocContentServlet extends DownloadContentServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Helper to generate a URL to a content node for downloading content from the server.
     * 
     * @param pattern
     *            The pattern to use for the URL
     * @param ref
     *            NodeRef of the content node to generate URL for (cannot be null)
     * @param name
     *            File name to return in the URL (cannot be null)
     * @return URL to download the content from the specified node
     */
    public final static String generateUrl(NodeRef ref, int id, String name) {
        Assert.notNull(ref, "Parameter 'ref' is mandatory");
        Assert.isTrue(id >= 0, "Parameter 'id' must not be negative");
        Assert.notNull(name, "Parameter 'name' is mandatory");
        return MessageFormat.format("/ddc/{0}/{1}/{2}/{3}/{4}", new Object[] { ref.getStoreRef().getProtocol(),
                ref.getStoreRef().getIdentifier(), ref.getId(), id, URLEncoder.encode(name) });
    }

    /**
     * Processes the download request using the current context i.e. no authentication checks are
     * made, it is presumed they have already been done.
     * 
     * @param req
     *            The HTTP request
     * @param res
     *            The HTTP response
     * @param redirectToLogin
     *            Flag to determine whether to redirect to the login page if the user does not have
     *            the correct permissions
     */
    @Override
    protected void processDownloadRequest(final HttpServletRequest req, final HttpServletResponse res, final boolean redirectToLogin)
            throws ServletException, IOException {
        Log logger = getLogger();
        String uri = req.getRequestURI();

        if (logger.isDebugEnabled()) {
            String queryString = req.getQueryString();
            logger.debug("Processing URL: " + uri + (queryString != null && queryString.length() > 0 ? "?" + queryString : ""));
        }

        uri = uri.substring(req.getContextPath().length());
        StringTokenizer t = new StringTokenizer(uri, "/");
        int tokenCount = t.countTokens();

        t.nextToken(); // skip servlet name
        // always attachment mode

        // a NodeRef must have been specified if no path has been found
        if (tokenCount < 6) {
            throw new IllegalArgumentException("Download URL did not contain all required args: " + uri);
        }

        // assume 'workspace' or other NodeRef based protocol for remaining URL elements
        StoreRef storeRef = new StoreRef(t.nextToken(), t.nextToken());
        String id = URLDecoder.decode(t.nextToken());

        final int dataFileId;
        try {
            dataFileId = new Integer(t.nextToken());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Download URL did not contain valid data file id : " + uri);
        }

        // build noderef from the appropriate URL elements
        final NodeRef nodeRef = new NodeRef(storeRef, id);

        try {
            ServiceRegistry serviceRegistry = getServiceRegistry(getServletContext());
            TransactionService transactionService = serviceRegistry.getTransactionService();
            RetryingTransactionHelper txHelper = transactionService.getRetryingTransactionHelper();
            try {
                txHelper.doInTransaction(new RetryingTransactionCallback<Object>() {
                    @Override
                    public Object execute() throws Throwable {
                        processDigiDocDownloadRequest(req, res, redirectToLogin, nodeRef, dataFileId);
                        return null;
                    }
                }, true);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof SocketException) {
                    throw (SocketException) e.getCause();
                }
                throw e;
            }
        } catch (SocketException e1) {
            // the client cut the connection - our mission was accomplished apart from a little
            // error message
            if (logger.isInfoEnabled()) {
                logger.info("Client aborted stream read:\n\tnode: " + nodeRef);
            }
        } catch (ContentIOException e2) {
            if (logger.isInfoEnabled()) {
                logger.info("Client aborted stream read:\n\tnode: " + nodeRef);
            }
        }
    }

    private void processDigiDocDownloadRequest(HttpServletRequest req, HttpServletResponse res, boolean redirectToLogin, NodeRef nodeRef, int dataFileId)
            throws SocketException, IOException {
        Log logger = getLogger();

        ServiceRegistry serviceRegistry = getServiceRegistry(getServletContext());
        NodeService nodeService = serviceRegistry.getNodeService();
        PermissionService permissionService = serviceRegistry.getPermissionService();
        // check that the user has at least READ_CONTENT access - else redirect to the login
        // page
        if (permissionService.hasPermission(nodeRef, PermissionService.READ_CONTENT) == AccessStatus.DENIED) {
            if (logger.isDebugEnabled()) {
                logger.debug("User does not have permissions to read content for NodeRef: " + nodeRef.toString());
            }

            if (redirectToLogin) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Redirecting to login page...");
                }

                // TODO: replace with serviceRegistry.getAuthorityService().hasGuestAuthority() from 3.1E
                if (!AuthenticationUtil.getFullyAuthenticatedUser().equals(AuthenticationUtil.getGuestUserName())) {
                    req.getSession().setAttribute(LoginBean.LOGIN_NOPERMISSIONS, Boolean.TRUE);
                }
                redirectToLoginPage(req, res, getServletContext());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Returning 403 Forbidden error...");
                }

                res.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            return;
        }

        // check If-Modified-Since header and set Last-Modified header as appropriate
        Date modified = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED);
        if (modified != null) {
            long modifiedSince = req.getDateHeader("If-Modified-Since");
            if (modifiedSince > 0L) {
                // round the date to the ignore millisecond value which is not supplied by header
                long modDate = (modified.getTime() / 1000L) * 1000L;
                if (modDate <= modifiedSince) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Returning 304 Not Modified.");
                    }
                    res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
            }
            res.setDateHeader("Last-Modified", modified.getTime());
            res.setHeader("Cache-Control", "must-revalidate");
            res.setHeader("ETag", "\"" + Long.toString(modified.getTime()) + "\"");
        }

        // attachment mode - will force a Save As from the browse if it doesn't recognise it;
        // this is better than the default response of the browser trying to display the
        // contents
        res.setHeader("Content-Disposition", "attachment");

        try {
            WebApplicationContext webAppContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
            SignatureService signatureService = (SignatureService) webAppContext.getBean(SignatureService.BEAN_NAME);
            SignatureItemsAndDataItems items = signatureService.getDataItemsAndSignatureItems(nodeRef, true);
            DataItem item = items.getDataItems().get(dataFileId);

            long size = item.getSize();
            res.setHeader("Content-Range", "bytes 0-" + Long.toString(size - 1L) + "/" + Long.toString(size));
            res.setHeader("Content-Length", Long.toString(size));

            // set mimetype for the content and the character encoding for the stream
            res.setContentType(item.getMimeType());
            res.setCharacterEncoding(item.getEncoding());

            ServletOutputStream os = res.getOutputStream();
            FileCopyUtils.copy(item.getData(), os); // closes both streams
        } catch (SignatureException e) {
            logger.error("Failed to fetch a document from .ddoc, noderef: " + nodeRef + ", id = " + dataFileId, e);
        }
    }

}
