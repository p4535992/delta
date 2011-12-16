package ee.webmedia.alfresco.template.web;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.integrity.IntegrityException;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.content.AddContentDialog;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.collections.Closure;
import org.apache.commons.io.FilenameUtils;

import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.classificator.enums.TemplateType;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.template.service.DocumentTemplateServiceImpl;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

/**
 * @author Kaarel Jõgeva
 * @author Vladimir Drozdik
 */
public class AddDocumentTemplateDialog extends AddContentDialog {

    private static final long serialVersionUID = 1L;
    private static final String ERR_EXISTING_FILE = "add_file_existing_file";
    private static final String ERR_INVALID_FILE_NAME = "add_file_invalid_file_name";
    private Node docTemplateNode;
    boolean firstLoad;
    private String templateType;
    private transient UIPropertySheet propertySheet;
    private boolean showReportOutputType;

    public void reportTypeValueChanged(final ValueChangeEvent event) {
        ComponentUtil.executeLater(PhaseId.INVOKE_APPLICATION, getPropertySheet(), new Closure() {
            @Override
            public void execute(Object input) {
                if (event.getNewValue().equals(TemplateReportType.DOCUMENTS_REPORT.name())) {
                    setShowReportOutputType(true);
                } else {
                    setShowReportOutputType(false);
                }
                if (propertySheet != null) {
                    propertySheet.getChildren().clear();
                    propertySheet.getClientValidations().clear();
                    propertySheet.setMode(null);
                    propertySheet.setNode(null);
                }
            }
        });
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    @Override
    public void start(ActionEvent event) {
        templateType = ActionUtil.getParam(event, "templateType");
        String defaultComment;

        // Set the templates root space as parent node
        setDocTemplateNode(TransientNode.createNew(getDictionaryService(), getDictionaryService().getType(ContentModel.TYPE_CONTENT), "",
                new HashMap<QName, Serializable>(0)));
        docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE);
        if (TemplateType.DOCUMENT_TEMPLATE.name().equals(templateType)) {
            docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT);
            defaultComment = "template_doc_template";
        } else if (TemplateType.EMAIL_TEMPLATE.name().equals(templateType)) {
            docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE_EMAIL);
            defaultComment = "template_email_template";
        } else if (TemplateType.NOTIFICATION_TEMPLATE.name().equals(templateType)) {
            docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE_NOTIFICATION);
            defaultComment = "template_system_template";
        } else {
            docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE_REPORT);
            defaultComment = "template_report_template";
        }
        docTemplateNode.getProperties().put(DocumentTemplateModel.Prop.COMMENT.toString(), MessageUtil.getMessage(defaultComment));
        docTemplateNode.getProperties().put(DocumentTemplateModel.Prop.TEMPLATE_TYPE.toString(), templateType);

        setFirstLoad(true);
        super.start(event);
    }

    public void setTempPropsFromFileName() {
        docTemplateNode.getProperties().put(DocumentTemplateServiceImpl.TEMP_PROP_FILE_NAME_BASE.toString(), FilenameUtils.getBaseName(fileName));
        docTemplateNode.getProperties().put(DocumentTemplateServiceImpl.TEMP_PROP_FILE_NAME_EXTENSION.toString(), FilenameUtils.getExtension(fileName));
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        clearUpload();
        reset();

        if (showOtherProperties) {
            browseBean.setDocument(new Node(createdNode));
        }
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        // User didn't select a file
        if (isFirstLoad()) {
            throw new UnableToPerformException("templates_file_mandatory");
        }

        Map<String, Object> props = docTemplateNode.getProperties();
        String newName = props.remove(DocumentTemplateServiceImpl.TEMP_PROP_FILE_NAME_BASE.toString()) + "."
                + props.remove(DocumentTemplateServiceImpl.TEMP_PROP_FILE_NAME_EXTENSION.toString());
        FilenameUtil.checkPlusInFileName(newName);
        try {
            setFileName(newName);
            navigator.setCurrentNodeId(BeanHelper.getDocumentTemplateService().getRoot().getId());
            saveContent(file, null);
        } catch (FileExistsException e) {
            isFinished = false;
            throw new RuntimeException(MessageUtil.getMessage(context, ERR_EXISTING_FILE, e.getName()));
        }
        Map<QName, Serializable> properties = RepoUtil.toQNameProperties(props);
        properties.put(DocumentTemplateModel.Prop.NAME, newName);
        if (TemplateType.NOTIFICATION_TEMPLATE.name().equals(templateType)) {
            getNodeService().addAspect(createdNode, DocumentTemplateModel.Aspects.TEMPLATE_NOTIFICATION, properties);
        } else if (TemplateType.EMAIL_TEMPLATE.name().equals(templateType)) {
            getNodeService().addAspect(createdNode, DocumentTemplateModel.Aspects.TEMPLATE_EMAIL, properties);
        } else if (TemplateType.REPORT_TEMPLATE.name().equals(templateType)) {
            getNodeService().addAspect(createdNode, DocumentTemplateModel.Aspects.TEMPLATE_REPORT, properties);
        } else if (TemplateType.DOCUMENT_TEMPLATE.name().equals(templateType)) {
            getNodeService().addAspect(createdNode, DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT, properties);
        }
        return outcome;
    }

    @Override
    protected String getErrorOutcome(Throwable exception) {
        if (exception instanceof IntegrityException) {
            Utils.addErrorMessage(MessageUtil.getMessage(FacesContext.getCurrentInstance(), ERR_INVALID_FILE_NAME));
            return "";
        }
        return super.getErrorOutcome(exception);
    }

    private void reset() {
        docTemplateNode = null;
        firstLoad = false;
        showReportOutputType = false;
        propertySheet = null;
    }

    public boolean validateFileExtension() {
        String name = getFileName();
        return TemplateType.DOCUMENT_TEMPLATE.name().equals(templateType) && isFileNameHasValidExtension(name, "dotx", "dot")
                || TemplateType.REPORT_TEMPLATE.name().equals(templateType) && isFileNameHasValidExtension(name, "xltx", "xlt")
                || (TemplateType.NOTIFICATION_TEMPLATE.name().equals(templateType) || TemplateType.EMAIL_TEMPLATE.name().equals(templateType))
                && isFileNameHasValidExtension(name, "htm", "html");
    }

    private boolean isFileNameHasValidExtension(String fileName, String... extensions) {
        for (String extension : extensions) {
            if (FilenameUtils.getExtension(fileName).equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    public String getWrongFormatMsgKey() {
        String errorMsgKey;
        if (TemplateType.DOCUMENT_TEMPLATE.name().equals(templateType)) {
            errorMsgKey = "template_wrong_file_format_doc";
        } else if (TemplateType.REPORT_TEMPLATE.name().equals(templateType)) {
            errorMsgKey = "template_wrong_file_format_report";
        } else {
            errorMsgKey = "template_wrong_file_format_email";
        }
        return errorMsgKey;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        this.propertySheet = propertySheet;
    }

    public UIPropertySheet getPropertySheet() {
        return propertySheet;
    }

    public boolean isShowReportOutputType() {
        return showReportOutputType;
    }

    public void setShowReportOutputType(boolean showReportOutputType) {
        this.showReportOutputType = showReportOutputType;
    }

    public Node getDocTemplateNode() {
        return docTemplateNode;
    }

    public void setDocTemplateNode(Node docTemplateNode) {
        this.docTemplateNode = docTemplateNode;
    }

    public boolean isFirstLoad() {
        return firstLoad;
    }

    public void setFirstLoad(boolean firstLoad) {
        this.firstLoad = firstLoad;
    }

    // END: getters / setters
}
