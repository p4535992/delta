package ee.webmedia.alfresco.common.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.ServletContext;

import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicServiceImpl;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.WebUtil;

/**
 * @author Alar Kvell
 */
public class ExternalAccessPhaseListener implements PhaseListener {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentDynamicServiceImpl.class);

    public static final String OUTCOME_AND_ARGS_ATTR = ExternalAccessPhaseListener.class.getName() + ".OUTCOME_AND_ARGS";
    public static final String OUTCOME_DOCUMENT = "document";

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        // do nothing
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        FacesContext context = event.getFacesContext();

        @SuppressWarnings("unchecked")
        Pair<String, String[]> outcomeAndArgs = (Pair<String, String[]>) context.getExternalContext().getRequestMap().get(OUTCOME_AND_ARGS_ATTR);
        if (outcomeAndArgs == null) {
            return;
        }

        LOG.info("Performing navigation triggered from external access servlet:\n  outcome=" + outcomeAndArgs.getFirst() + "\n  args=" + Arrays.asList(outcomeAndArgs.getSecond()));

        processExternalAcessAction(context, outcomeAndArgs);
    }

    private void processExternalAcessAction(FacesContext context, Pair<String, String[]> outcomeAndArgs) {

        // TODO Alar -> Riina - is this needed any more?
        // always allow missing bindings from ExternalAccessServlet:
        // when redirecting from ExternalAccessServlet, jsp binding attribute value may be queried from wrong bean
        // CL task 143975
        // req.setAttribute("allow_missing_bindings", Boolean.TRUE);

        if (OUTCOME_DOCUMENT.equals(outcomeAndArgs.getFirst()) && outcomeAndArgs.getSecond().length >= 1) {

            try {
                NodeRef nodeRef = getNodeRefFromNodeId(outcomeAndArgs.getSecond()[0]);

                // select correct menu
                MenuBean.clearViewStack(String.valueOf(MenuBean.DOCUMENT_REGISTER_ID), null);

                // open document dialog
                if (DocumentCommonModel.Types.DOCUMENT.equals(BeanHelper.getNodeService().getType(nodeRef))) {
                    BeanHelper.getDocumentDynamicDialog().openFromUrl(nodeRef);
                } else {
                    BeanHelper.getDocumentDialog().open(nodeRef);
                    WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "document", context);
                }
            } catch (InvalidNodeRefException e) {
                MessageUtil.addErrorMessage("document_restore_error_docDeleted");
            }
        }
    }

    private static final String STORE_PARAMETER_LABEL = "documentStores";
    private List<String> storeNames;

    private List<String> getStoreNames() {
        if (storeNames == null) {
            ServletContext servletContext = BeanHelper.getDocumentTemplateService().getServletContext();
            String storeName = servletContext.getInitParameter(STORE_PARAMETER_LABEL);
            Assert.hasText(storeName, "At least one store name must be provided");
            StringTokenizer tokenizer = new StringTokenizer(storeName, ",");
            List<String> storeNamesTmp = new ArrayList<String>();
            while (tokenizer.hasMoreTokens()) {
                storeNamesTmp.add(StringUtils.trimToEmpty(tokenizer.nextToken()));
            }
            storeNames = storeNamesTmp;
        }
        return storeNames;
    }

    private NodeRef getNodeRefFromNodeId(String currentNodeId) {
        Assert.notNull(currentNodeId);

        boolean nodeExists = false;
        String nodeRefsStr = "";
        NodeRef nodeRef = null;
        for (String storeName : getStoreNames()) {
            StoreRef storeRef = new StoreRef(storeName);
            nodeRef = new NodeRef(storeRef, currentNodeId);
            nodeRefsStr += (nodeRefsStr.isEmpty() ? "" : "; ") + nodeRef.toString();
            if (BeanHelper.getNodeService().exists(nodeRef)) {
                nodeExists = true;
                break;
            }
        }

        if (!nodeExists) {
            throw new InvalidNodeRefException("Invalid URI provided (" + nodeRefsStr + ")", nodeRef);
        }
        return nodeRef;
    }

}
