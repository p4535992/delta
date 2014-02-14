package ee.webmedia.alfresco.template.web;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.FilenameUtils;

import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.classificator.enums.TemplateType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.template.service.DocumentTemplateServiceImpl;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * Form backing bean for DocumentTemplate details view.
 * 
 * @author Vladimir Drozdik
 */
public class DocumentTemplateDetailsDialog extends BaseDialogBean {

    private static final long serialVersionUID = 1L;
    private Node docTemplNode;
    private boolean showReportOutputType;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        BeanHelper.getDocumentTemplateService().updateDocTemplate(docTemplNode);
        showReportOutputType = false;
        return outcome;
    }

    /**
     * Action listener for JSP pages
     * 
     * @param event ActionLink
     */
    public void setupDocTemplate(ActionEvent event) {
        NodeRef docTemplateNodeRef = ActionUtil.getParam(event, "docTemplateNodeRef", NodeRef.class);
        docTemplNode = BeanHelper.getGeneralService().fetchNode(docTemplateNodeRef);
        String templateType = (String) docTemplNode.getProperties().get(DocumentTemplateModel.Prop.TEMPLATE_TYPE);
        if (TemplateType.REPORT_TEMPLATE.name().equals(templateType)
                && TemplateReportType.DOCUMENTS_REPORT.name().equals(docTemplNode.getProperties().get(DocumentTemplateModel.Prop.REPORT_TYPE))) {
            showReportOutputType = true;
        }
        String templateName = (String) docTemplNode.getProperties().get(DocumentTemplateModel.Prop.NAME);
        String fileNameBase = FilenameUtils.getBaseName(templateName);
        String fileNameExtension = FilenameUtils.getExtension(templateName);
        docTemplNode.getProperties().put(DocumentTemplateServiceImpl.TEMP_PROP_FILE_NAME_BASE.toString(), fileNameBase);
        docTemplNode.getProperties().put(DocumentTemplateServiceImpl.TEMP_PROP_FILE_NAME_EXTENSION.toString(), "." + fileNameExtension);
        // Safe-guard for old data
        if ((TemplateType.EMAIL_TEMPLATE.name().equals(templateType) || TemplateType.NOTIFICATION_TEMPLATE.name().equals(templateType))
                && !docTemplNode.hasAspect(DocumentTemplateModel.Aspects.SUBJECT)) {
            docTemplNode.getAspects().add(DocumentTemplateModel.Aspects.SUBJECT);
        }
    }

    public Node getCurrentNode() {
        return docTemplNode;
    }

    public boolean isShowReportOutputType() {
        return showReportOutputType;
    }

    @Override
    public String cancel() {
        docTemplNode = null;
        showReportOutputType = false;
        return super.cancel();
    }
}
