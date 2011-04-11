package ee.webmedia.alfresco.simdhs.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.AuthenticationStatus;
import org.alfresco.web.app.servlet.BaseServlet;
import org.alfresco.web.app.servlet.FacesHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.menu.ui.MenuBean;

/**
 * Servlet allowing external URL access to various global JSF views in the Web Client.
 * Available URL-s: <li><code>http://&lt;server&gt;/simdhs/&lt;servlet name&gt;/document/&lt;document node ref&gt;</code> - for viewing document</li>
 * 
 * @author Romet Aidla
 */
public class ExternalAccessServlet extends BaseServlet {
    private static final long serialVersionUID = 7348802704715012097L;

    private static Log logger = LogFactory.getLog(ExternalAccessServlet.class);

    private static final String VIEW_STACK = "_alfViewStack";
    private static final String OUTCOME_DOCUMENT = "document";

    private static Map<String, String> dialogMappings = new HashMap<String, String>();

    static {
        dialogMappings.put(OUTCOME_DOCUMENT, AlfrescoNavigationHandler.DIALOG_PREFIX + "document");
    }

    private static final String STORE_PARAMETER_LABEL = "documentStores";
    private List<String> storeNames;

    @Override
    public void init() throws ServletException {
        super.init();
        String storeName = getServletContext().getInitParameter(STORE_PARAMETER_LABEL);
        Assert.hasText(storeName, "At least one store name must be provided");
        StringTokenizer tokenizer = new StringTokenizer(storeName, ",");
        storeNames = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            storeNames.add(StringUtils.trimToEmpty(tokenizer.nextToken()));
        }
    }

    @SuppressWarnings("unchecked")
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

        uri = uri.substring(req.getContextPath().length());
        StringTokenizer t = new StringTokenizer(uri, "/");
        int tokenCount = t.countTokens();
        if (tokenCount < 2) {
            throw new IllegalArgumentException("Externally addressable URL did not contain all required args: " + uri);
        }

        // 1. servlet name (not used)
        t.nextToken();
        // 2. outcome
        String outcome = t.nextToken();
        // 3. rest of the tokens arguments
        String[] args = extractArguments(t, tokenCount);

        if (logger.isDebugEnabled()) {
            logger.debug("External outcome found: " + outcome);
        }

        FacesContext fc = FacesHelper.getFacesContext(req, res, getServletContext());
        ServiceRegistry serviceRegistry = getServiceRegistry(getServletContext());

        // always allow missing bindings from ExternalAccessServlet:
        // when redirecting from ExternalAccessServlet, jsp binding attribute value may be queried from wrong bean
        // CL task 143975
        req.setAttribute("allow_missing_bindings", Boolean.TRUE);

        if (OUTCOME_DOCUMENT.equals(outcome)) {
            String currentNodeId = args[0];
            Assert.notNull(currentNodeId);

            if (logger.isDebugEnabled()) {
                logger.debug("currentNodeId: " + currentNodeId);
            }

            boolean nodeExists = false;
            String nodeRefsStr = null;
            NodeRef nodeRef = null;
            for (String storeName : storeNames) {
                StoreRef storeRef = new StoreRef(storeName);
                nodeRef = new NodeRef(storeRef, currentNodeId);
                nodeRefsStr += (nodeRefsStr == null) ? nodeRef : (";" + nodeRef);
                if (serviceRegistry.getNodeService().exists(nodeRef)) {
                    nodeExists = true;
                    break;
                }
            }

            if (!nodeExists) {
                throw new InvalidNodeRefException("Invalid URI provided (" + nodeRefsStr + ")", nodeRef);
            }

            // select correct menu
            MenuBean.clearViewStack(String.valueOf(MenuBean.DOCUMENT_REGISTER_ID), null);

            // open document dialog
            DocumentDialog dialog = (DocumentDialog) FacesHelper.getManagedBean(fc, DocumentDialog.BEAN_NAME);
            dialog.open(nodeRef);

            NavigationHandler navigationHandler = fc.getApplication().getNavigationHandler();
            navigationHandler.handleNavigation(fc, null, dialogMappings.get(OUTCOME_DOCUMENT));
        }

        // perform the forward to the page processed by the Faces servlet
        getServletContext().getRequestDispatcher(FACES_SERVLET + fc.getViewRoot().getViewId()).forward(req, res);
    }

    private String[] extractArguments(StringTokenizer t, int tokenCount) {
        String[] args = new String[tokenCount - 2];
        for (int i = 0; i < tokenCount - 2; i++) {
            args[i] = t.nextToken();
        }
        return args;
    }

}
