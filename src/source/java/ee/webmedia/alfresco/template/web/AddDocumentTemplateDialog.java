package ee.webmedia.alfresco.template.web;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.content.AddContentDialog;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Kaarel Jõgeva
 */
public class AddDocumentTemplateDialog extends AddContentDialog {

    private static final long serialVersionUID = 1L;
    private static final String ERR_EXISTING_FILE = "add_file_existing_file";
    private transient GeneralService generalService;
    private Node docTemplateNode;
    boolean firstLoad;
    boolean emailTemplate;

    public void startEmail(ActionEvent event) {
        setEmailTemplate(true);
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
        this.navigator.setCurrentNodeId(getGeneralService().getNodeRef(DocumentTemplateModel.Repo.TEMPLATES_SPACE).getId());
        setDocTemplateNode(TransientNode.createNew(getDictionaryService(), getDictionaryService().getType(ContentModel.TYPE_CONTENT), "",
                new HashMap<QName, Serializable>(0)));
        docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE);
        if (!isEmailTemplate()) {
            docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE_DOC_TYPE);
        }
        // Eagerly load the properties
        docTemplateNode.getProperties();
        setFirstLoad(true);
        super.start(event);
    }

    @Override
    protected String doPostCommitProcessing(FacesContext context, String outcome) {
        clearUpload();

        if (this.showOtherProperties) {
            this.browseBean.setDocument(new Node(this.createdNode));
        }
        return AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        Map<String, Object> templProp = docTemplateNode.getProperties();
        String newName = templProp.get(DocumentTemplateModel.Prop.NAME).toString() + "." + FilenameUtils.getExtension(super.fileName);
        try {
            setFileName(newName);
            saveContent(this.file, null);
        } catch (FileExistsException e) {
            isFinished = false;
            throw new RuntimeException(MessageUtil.getMessage(context, ERR_EXISTING_FILE, e.getName()));
        }

        Map<QName, Serializable> prop = new HashMap<QName, Serializable>();
        prop.put(DocumentTemplateModel.Prop.NAME, newName);
        prop.put(DocumentTemplateModel.Prop.COMMENT, templProp.get(DocumentTemplateModel.Prop.COMMENT).toString());
        getNodeService().addAspect(this.createdNode, DocumentTemplateModel.Aspects.TEMPLATE, prop);

        if (!isEmailTemplate()) {
            Map<QName, Serializable> prop2 = new HashMap<QName, Serializable>();
            if (templProp.get(DocumentTemplateModel.Prop.DOCTYPE_ID) != null && !templProp.get(DocumentTemplateModel.Prop.DOCTYPE_ID).toString().equals("")) {
                prop2.put(DocumentTemplateModel.Prop.DOCTYPE_ID, templProp.get(DocumentTemplateModel.Prop.DOCTYPE_ID).toString());
            }
            getNodeService().addAspect(this.createdNode, DocumentTemplateModel.Aspects.TEMPLATE_DOC_TYPE, prop2);
        }
        reset();
        return outcome;
    }
    
    @Override
    public String getContainerTitle() {
        if (isEmailTemplate()) {
            return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "template_add_new_email_template");
        }
        return MessageUtil.getMessage(FacesContext.getCurrentInstance(), "template_add_new_doc_template");
    }
    
    private void reset() {
        docTemplateNode = null;
        firstLoad = false;
        emailTemplate = false;
    }

    /**
     * Must be called in jsp file to update the Node
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
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance()).getBean(GeneralService.BEAN_NAME);
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
    // END: getters / setters
}
