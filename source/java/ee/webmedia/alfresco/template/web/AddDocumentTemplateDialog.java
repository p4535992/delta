package ee.webmedia.alfresco.template.web;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.integrity.IntegrityException;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.content.AddContentDialog;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.file.web.AddFileDialog;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.MessageUtil;

public class AddDocumentTemplateDialog extends AddContentDialog {

    private static final long serialVersionUID = 1L;
    private static final String ERR_EXISTING_FILE = "add_file_existing_file";
    private static final String ERR_INVALID_FILE_NAME = "add_file_invalid_file_name";
    private transient GeneralService generalService;
    private Node docTemplateNode;
    boolean firstLoad;
    boolean emailTemplate;
    boolean systemTemplate;

    public void startEmail(ActionEvent event) {
        setSystemTemplate(false);
        setEmailTemplate(true);
        start(event);
    }

    public void startSystem(ActionEvent event) {
        setEmailTemplate(false);
        setSystemTemplate(true);
        start(event);
    }

    @Override
    public String cancel() {
        reset();
        return super.cancel();
    }

    @Override
    public void start(ActionEvent event) {

        // Set the templates root space as parent node
        navigator.setCurrentNodeId(getGeneralService().getNodeRef(DocumentTemplateModel.Repo.TEMPLATES_SPACE).getId());
        setDocTemplateNode(TransientNode.createNew(getDictionaryService(), getDictionaryService().getType(ContentModel.TYPE_CONTENT), "",
                new HashMap<QName, Serializable>(0)));
        docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE);
        if (isSystemTemplate()) {
            docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE_SYSTEM);
        } else if (isEmailTemplate()) {
            docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE_EMAIL);
        } else {
            docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT);
        }
        // Eagerly load the properties
        docTemplateNode.getProperties();
        setFirstLoad(true);
        super.start(event);
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
        Map<String, Object> templProp = docTemplateNode.getProperties();
        String newName = templProp.get(DocumentTemplateModel.Prop.NAME).toString() + "." + FilenameUtils.getExtension(super.fileName);
        AddFileDialog.checkPlusInFileName(newName);
        try {
            setFileName(newName);
            saveContent(file, null);
        } catch (FileExistsException e) {
            isFinished = false;
            throw new RuntimeException(MessageUtil.getMessage(context, ERR_EXISTING_FILE, e.getName()));
        }

        Map<QName, Serializable> prop = new HashMap<QName, Serializable>();
        prop.put(DocumentTemplateModel.Prop.NAME, newName);
        prop.put(DocumentTemplateModel.Prop.COMMENT, templProp.get(DocumentTemplateModel.Prop.COMMENT).toString());

        if (isSystemTemplate()) {
            prop.put(DocumentTemplateModel.Prop.DOCTYPE_ID, MessageUtil.getMessage("template_system_template"));
            getNodeService().addAspect(createdNode, DocumentTemplateModel.Aspects.TEMPLATE_SYSTEM, prop);
        } else if (isEmailTemplate()) {
            prop.put(DocumentTemplateModel.Prop.DOCTYPE_ID, MessageUtil.getMessage("template_email_template"));
            getNodeService().addAspect(createdNode, DocumentTemplateModel.Aspects.TEMPLATE_EMAIL, prop);
        } else {
            if (templProp.get(DocumentTemplateModel.Prop.DOCTYPE_ID) != null && !templProp.get(DocumentTemplateModel.Prop.DOCTYPE_ID).toString().equals("")) {
                prop.put(DocumentTemplateModel.Prop.DOCTYPE_ID, templProp.get(DocumentTemplateModel.Prop.DOCTYPE_ID).toString());
            }
            getNodeService().addAspect(createdNode, DocumentTemplateModel.Aspects.TEMPLATE_DOCUMENT, prop);
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

    @Override
    public String getContainerTitle() {
        if (isEmailTemplate()) {
            return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "template_add_new_email_template");
        }

        if (isSystemTemplate()) {
            return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "template_add_new_system_template");
        }

        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "template_add_new_doc_template");
    }

    private void reset() {
        docTemplateNode = null;
        firstLoad = false;
        emailTemplate = false;
        systemTemplate = false;
    }

    /**
     * Must be called in jsp file to update the Node
     * 
     * @param name
     */
    public void updateNodeFileName(String name) {
        docTemplateNode.getProperties().put(DocumentTemplateModel.Prop.NAME.toString(), FilenameUtils.removeExtension(name));
    }

    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(
                    GeneralService.BEAN_NAME);
        }
        return generalService;
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

    public boolean isEmailTemplate() {
        return emailTemplate;
    }

    public void setEmailTemplate(boolean emailTemplate) {
        this.emailTemplate = emailTemplate;
    }

    public boolean isSystemTemplate() {
        return systemTemplate;
    }

    public void setSystemTemplate(boolean systemTemplate) {
        this.systemTemplate = systemTemplate;
    }
    // END: getters / setters

}
