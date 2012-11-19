package ee.webmedia.alfresco.document.sendout.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.web.evaluator.DocumentSendForInformationEvaluator;
import ee.webmedia.alfresco.template.model.DocumentTemplate;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Riina Tens
 */
public class DocumentSendForInformationDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;

    private List<String> authorities;
    private String selectedEmailTemplate;
    private List<SelectItem> emailTemplates;
    private Node docNode;

    public String init() {
        Node docNode = BeanHelper.getDocumentDialogHelperBean().getNode();
        docNode.clearPermissionsCache();
        if (!new DocumentSendForInformationEvaluator().evaluate(docNode)) {
            MessageUtil.addErrorMessage("document_send_for_information_error_noPermission");
            return null;
        }
        if (!getNodeService().exists(docNode.getNodeRef())) {
            return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
        }
        return "dialog:documentSendForInformationDialog";
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (validate()) {
            BeanHelper.getSendOutService().sendDocumentForInformation(authorities, docNode, selectedEmailTemplate);
            MessageUtil.addInfoMessage("document_send_for_information_success");
            return outcome;
        }
        return null;
    }

    private boolean validate() {
        if (authorities == null || authorities.isEmpty()) {
            MessageUtil.addErrorMessage("common_propertysheet_validator_mandatory", MessageUtil.getMessage("document_send_for_information_users"));
            return false;
        }
        if (StringUtils.isBlank(selectedEmailTemplate)) {
            MessageUtil.addErrorMessage("common_propertysheet_validator_mandatory", MessageUtil.getMessage("document_send_for_information_email_template"));
            return false;
        }
        return true;
    }

    public void loadDocument(ActionEvent event) {
        reset();
        try {
            docNode = getDocumentService().getDocument(new NodeRef(ActionUtil.getParam(event, "documentNodeRef")));
        } catch (InvalidNodeRefException e) {
            MessageUtil.addErrorMessage("document_sendOut_error_docDeleted");
            return;
        }
        List<DocumentTemplate> templates = BeanHelper.getDocumentTemplateService().getEmailTemplates();
        emailTemplates = new ArrayList<SelectItem>();
        for (DocumentTemplate template : templates) {
            emailTemplates.add(new SelectItem(template.getName(), FilenameUtils.getBaseName(template.getName())));
        }
        emailTemplates.add(0, new SelectItem("", MessageUtil.getMessage("select_default_label")));
    }

    private void reset() {
        authorities = null;
        selectedEmailTemplate = null;
        emailTemplates = null;
    }

    public String getBlockTitle() {
        return MessageUtil.getMessage("document_send_for_information");
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("document_send_for_information_action");
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public String getSelectedEmailTemplate() {
        return selectedEmailTemplate;
    }

    public void setSelectedEmailTemplate(String selectedEmailTemplate) {
        this.selectedEmailTemplate = selectedEmailTemplate;
    }

    public List<SelectItem> getEmailTemplates() {
        return emailTemplates;
    }

}
