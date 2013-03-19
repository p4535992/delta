package ee.webmedia.alfresco.common.listener;

import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;

import java.util.Arrays;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.apache.commons.lang.StringUtils;

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
    public static final String OUTCOME_COMPOUND_WORKFLOW_PROCEDURE_ID = "compoundWorkflowProcedureId";
    public static final String OUTCOME_COMPOUND_WORKFLOW_NODEREF = "compoundWorkflowNodeRef";
    public static final String OUTCOME_CASE_FILE = "caseFile";
    public static final String OUTCOME_VOLUME = "volume";

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

        if (outcomeAndArgs.getSecond().length >= 1) {
            String outcome = outcomeAndArgs.getFirst();
            String nodeIdentificator = outcomeAndArgs.getSecond()[0];
            if (OUTCOME_DOCUMENT.equals(outcome)) {
                NodeRef nodeRef = getNodeRefFromNodeId(nodeIdentificator);
                if (nodeRef != null) {
                    // select correct menu
                    MenuBean.clearViewStack(String.valueOf(MenuBean.DOCUMENT_REGISTER_ID), null);
                    // open document dialog
                    if (DocumentCommonModel.Types.DOCUMENT.equals(BeanHelper.getNodeService().getType(nodeRef))) {
                        BeanHelper.getDocumentDynamicDialog().openFromUrl(nodeRef);
                    } else {
                        BeanHelper.getDocumentDialog().open(nodeRef);
                        WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "document", context);
                    }
                } else {
                    MessageUtil.addErrorMessage("document_restore_error_docDeleted");
                }

            } else {
                String nodeIdentificatorStr = "nodeRef";
                if (OUTCOME_COMPOUND_WORKFLOW_NODEREF.equals(outcome)) {
                    navigateCompoundWorkflow(context, nodeIdentificator, getNodeRefFromNodeId(nodeIdentificator), nodeIdentificatorStr);
                } else if (OUTCOME_COMPOUND_WORKFLOW_PROCEDURE_ID.equals(outcome)) {
                    NodeRef compoundWorkflowRef = BeanHelper.getDocumentSearchService().getIndependentCompoundWorkflowByProcedureId(nodeIdentificator);
                    navigateCompoundWorkflow(context, nodeIdentificator, compoundWorkflowRef, "procedureId");
                } else if (OUTCOME_CASE_FILE.equals(outcome)) {
                    navigateCaseFile(context, nodeIdentificator, getNodeRefFromNodeId(nodeIdentificator), nodeIdentificatorStr);
                } else if (OUTCOME_VOLUME.equals(outcome)) {
                    navigateVolume(context, nodeIdentificator, getNodeRefFromNodeId(nodeIdentificator), nodeIdentificatorStr);
                }
            }
        }
    }

    private void navigateCompoundWorkflow(FacesContext context, String nodeIdentificator, NodeRef compoundWorkflowRef, String nodeIdentificatorStr) {
        if (compoundWorkflowRef == null) {
            notifyError(nodeIdentificator, nodeIdentificatorStr, "independent compound workflow", "compoundWorkflow_restore_error_not_found");
        } else {
            MenuBean.clearViewStack(String.valueOf(MenuBean.MY_TASKS_AND_DOCUMENTS_ID), null);
            BeanHelper.getCompoundWorkflowDialog().setupWorkflowFromList(compoundWorkflowRef);
            WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "compoundWorkflowDialog", context);
        }
    }

    private void navigateCaseFile(FacesContext context, String nodeIdentificator, NodeRef caseFileRef, String nodeIdentificatorStr) {
        if (caseFileRef == null) {
            notifyError(nodeIdentificator, nodeIdentificatorStr, "case file", "caseFile_restore_error_not_found");
        } else {
            MenuBean.clearViewStack(String.valueOf(MenuBean.MY_TASKS_AND_DOCUMENTS_ID), null);
            BeanHelper.getCaseFileDialog().open(caseFileRef, false);
            WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "caseFileDialog", context);
        }
    }

    private void navigateVolume(FacesContext context, String nodeIdentificator, NodeRef volumeRef, String nodeIdentificatorStr) {
        if (volumeRef == null) {
            notifyError(nodeIdentificator, nodeIdentificatorStr, "volume", "volume_restore_error_not_found");
        } else {
            MenuBean.clearViewStack(String.valueOf(MenuBean.MY_TASKS_AND_DOCUMENTS_ID), null);
            BeanHelper.getVolumeDetailsDialog().showDetails(volumeRef);
            WebUtil.navigateTo(AlfrescoNavigationHandler.DIALOG_PREFIX + "volumeDetailsDialog", context);
        }
    }

    private void notifyError(String nodeIdentificator, String nodeIdentificatorStr, String objectName, String errorMsgKey) {
        LOG.debug("Could not find " + objectName + " with " + nodeIdentificatorStr + "=" + nodeIdentificator);
        MessageUtil.addErrorMessage(errorMsgKey);
    }

    private NodeRef getNodeRefFromNodeId(String currentNodeId) {
        return StringUtils.isNotBlank(currentNodeId) ? getGeneralService().getExistingNodeRefAllStores(currentNodeId) : null;
    }

}
