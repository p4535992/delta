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
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
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

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.model.DataItem;
import ee.webmedia.alfresco.signature.model.SignatureItemsAndDataItems;
import ee.webmedia.alfresco.signature.service.SignatureService;
import ee.webmedia.alfresco.substitute.model.SubstitutionInfo;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.webdav.WebDAVCustomHelper;

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

        name = FilenameUtil.makeSafeFilename(name);

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
        // req.getPathInfo = /workspace/SpacesStore/371b7748-74cd-45d4-ac2a-e6ade845afe4/1/xyz.pdf
        // req.getRequestURI = /dhs/ddc/workspace/SpacesStore/371b7748-74cd-45d4-ac2a-e6ade845afe4/1/xyz.pdf;JSESSIONID=CFE6CC4F12D658B180EB61EF7ABC1C9
        String uri = req.getPathInfo();

        if (logger.isDebugEnabled()) {
            String queryString = req.getQueryString();
            logger.debug("Processing URL: " + uri + (queryString != null && queryString.length() > 0 ? "?" + queryString : ""));
        }

        StringTokenizer t = new StringTokenizer(uri, "/");
        int tokenCount = t.countTokens();

        // always attachment mode

        // a NodeRef must have been specified if no path has been found
        if (tokenCount < 5) {
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

    private void processDigiDocDownloadRequest(HttpServletRequest req, HttpServletResponse res, boolean redirectToLogin, NodeRef dDocRef, int dataFileId)
            throws SocketException, IOException {
        Log logger = getLogger();

        ServiceRegistry serviceRegistry = getServiceRegistry(getServletContext());
        NodeService nodeService = serviceRegistry.getNodeService();
        // check that the user has at least READ_CONTENT access - else redirect to the login
        // page
        try {
            // FIXME Somehow SubstitutionFilter and PermissionService get hold of different ContextHolders and thus filter effect is lost
            SubstitutionInfo substInfo = BeanHelper.getSubstitutionBean().getSubstitutionInfo();
            if (substInfo.isSubstituting()) {
                AuthenticationUtil.setRunAsUser(substInfo.getSubstitution().getReplacedPersonUserName());
            }
            WebDAVCustomHelper.checkDocumentFileReadPermission(dDocRef);
        } catch (AccessDeniedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("User does not have permissions to read content for NodeRef: " + dDocRef.toString() + " - " + e.getMessage());
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
        Date modified = (Date) nodeService.getProperty(dDocRef, ContentModel.PROP_MODIFIED);
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
            SignatureItemsAndDataItems items = signatureService.getDataItemsAndSignatureItems(dDocRef, true, true);
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
            logger.error("Failed to fetch a document from .ddoc, noderef: " + dDocRef + ", id = " + dataFileId, e);
        }
    }

}
