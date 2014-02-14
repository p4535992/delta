package ee.webmedia.alfresco.casefile.web;

import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.casefile.web.evaluator.CaseFileSendForInformationEvaluator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.sendout.web.DocumentSendForInformationDialog;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Marti Laast
 */
public class CaseFileSendForInformationDialog extends DocumentSendForInformationDialog {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getDialogText() {
        return "dialog:caseFileSendForInformationDialog";
    }

    @Override
    protected boolean evaluatePermission(Node contentNode) {
        return new CaseFileSendForInformationEvaluator().evaluate(contentNode);
    }

    @Override
    protected void initNodeValue(ActionEvent event) {
        contentNode = BeanHelper.getCaseFileDialog().getNode();
    }

    @Override
    protected void addSuccessMessage() {
        MessageUtil.addInfoMessage("caseFile_send_for_information_success");
    }

    @Override
    protected String getUrl() {
        return BeanHelper.getDocumentTemplateService().getCaseFileUrl(contentNode.getNodeRef());
    }

    @Override
    protected String getLogMessage() {
        return "caseFile_send_for_information_log_entry";
    }

    @Override
    protected LogObject getLogObject() {
        return LogObject.CASE_FILE;
    }

    @Override
    protected String getKeyPrefix() {
        return CaseFileModel.Types.CASE_FILE.getLocalName();
    }

}
