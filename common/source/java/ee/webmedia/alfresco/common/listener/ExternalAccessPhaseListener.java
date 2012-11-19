package ee.webmedia.alfresco.common.listener;

import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;

import java.util.Arrays;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
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

    private NodeRef getNodeRefFromNodeId(String currentNodeId) {
        Assert.notNull(currentNodeId);
        NodeRef nodeRef = getGeneralService().getExistingNodeRefAllStores(currentNodeId);
        if (nodeRef == null) {
            throw new InvalidNodeRefException("Invalid URI provided (" + currentNodeId + ")", nodeRef);
        }
        return nodeRef;
    }

}
