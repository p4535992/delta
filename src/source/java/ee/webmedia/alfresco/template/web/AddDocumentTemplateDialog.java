package ee.webmedia.alfresco.template.web;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.content.AddContentDialog;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;

/**
 * @author Kaarel Jõgeva
 */
public class AddDocumentTemplateDialog extends AddContentDialog {

    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "AddDocumentTemplateDialog";
    private transient GeneralService generalService;
    private Node docTemplateNode;
    boolean firstLoad;

    @Override
    public void start(ActionEvent event) {
        // Set the templates root space as parent node
        this.navigator.setCurrentNodeId(getGeneralService().getNodeRef(DocumentTemplateModel.Repo.TEMPLATES_SPACE).getId());
        setDocTemplateNode(TransientNode.createNew(getDictionaryService(), getDictionaryService().getType(ContentModel.TYPE_CONTENT), "",
                new HashMap<QName, Serializable>(0)));
        docTemplateNode.getAspects().add(DocumentTemplateModel.Aspects.TEMPLATE);
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
        
        setFileName(newName);
        saveContent(this.file, null);

        Map<QName, Serializable> prop = new HashMap<QName, Serializable>();
        prop.put(DocumentTemplateModel.Prop.NAME, newName);
        prop.put(DocumentTemplateModel.Prop.COMMENT, templProp.get(DocumentTemplateModel.Prop.COMMENT).toString());
        
        if(templProp.get(DocumentTemplateModel.Prop.DOCTYPE_ID) != null && !templProp.get(DocumentTemplateModel.Prop.DOCTYPE_ID).toString().equals("")) {
            prop.put(DocumentTemplateModel.Prop.DOCTYPE_ID, templProp.get(DocumentTemplateModel.Prop.DOCTYPE_ID).toString());
        }
        getNodeService().addAspect(this.createdNode, DocumentTemplateModel.Aspects.TEMPLATE, prop);

        return outcome;
    }

    /**
     * Must be called in jsp file to update the Node
     * @param name
     */
    public void updateNodeFileName(String name) {
        docTemplateNode.getProperties().put(DocumentTemplateModel.Prop.NAME.toString(), FilenameUtils.removeExtension(name));
    }
    
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

}
