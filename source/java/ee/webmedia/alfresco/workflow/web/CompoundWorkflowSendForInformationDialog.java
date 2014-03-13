package ee.webmedia.alfresco.workflow.web;

import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.sendout.web.DocumentSendForInformationDialog;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.web.evaluator.CompoundWorkflowSendForInformationEvaluator;

public class CompoundWorkflowSendForInformationDialog extends DocumentSendForInformationDialog {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getDialogText() {
        return "dialog:compoundWorkflowSendForInformationDialog";
    }

    @Override
    protected Node getNode() {
        return BeanHelper.getCompoundWorkflowDialog().getWorkflow().getNode();
    }

    @Override
    protected boolean evaluatePermission(Node contentNode) {
        return new CompoundWorkflowSendForInformationEvaluator().evaluate(BeanHelper.getWorkflowService().getCompoundWorkflow(contentNode.getNodeRef()));
    }

    @Override
    protected void initNodeValue(ActionEvent event) {
        contentNode = getNode();
    }

    @Override
    protected void addSuccessMessage() {
        MessageUtil.addInfoMessage("compoundWorkflow_send_for_information_success");
    }

    @Override
    protected String getUrl() {
        return BeanHelper.getDocumentTemplateService().getCompoundWorkflowUrl(contentNode.getNodeRef());
    }

    @Override
    protected String getLogMessage() {
        return "compoundWorkflow_send_for_information_log_entry";
    }

    @Override
    protected LogObject getLogObject() {
        return LogObject.COMPOUND_WORKFLOW;
    }

    @Override
    protected String getKeyPrefix() {
        return WorkflowCommonModel.Types.COMPOUND_WORKFLOW.getLocalName();
    }

}
