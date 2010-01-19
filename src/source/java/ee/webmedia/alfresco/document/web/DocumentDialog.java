package ee.webmedia.alfresco.document.web;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Alar Kvell
 */
public class DocumentDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    private static final String ERR_TEMPLATE_NOT_FOUND = "document_errorMsg_template_not_found";
    private static final String ERR_TEMPLATE_PROCESSING_FAILED = "document_errorMsg_template_processsing_failed";

    private transient DocumentService documentService;
    private transient DocumentTemplateService documentTemplateService;
    private MetadataBlockBean metadataBlockBean;
    private FileBlockBean fileBlockBean;
    private boolean isDraft;

    private Node node;
    
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentDialog.class);
    
    public void populateTemplate(ActionEvent event) {
        try {
            getDocumentTemplateService().populateTemplate(new NodeRef(ActionUtil.getParam(event, "documentNodeRef")));
        } catch (FileNotFoundException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), ERR_TEMPLATE_NOT_FOUND);
        } catch (RuntimeException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), ERR_TEMPLATE_PROCESSING_FAILED);
        }
        fileBlockBean.restore();
    }
    
    public void create(ActionEvent event) {
        QName documentTypeId = QName.resolveToQName(getNamespaceService(), ActionUtil.getParam(event, "documentType"));
        node = getDocumentService().createDocument(documentTypeId);
        isDraft = true;
    }

    public void open(ActionEvent event) {
        node = getDocumentService().getDocument(new NodeRef(ActionUtil.getParam(event, "nodeRef")));
        isDraft = false;
    }

    public void copy(ActionEvent event) {
        if (node == null) throw new RuntimeException("No current document");

        node = getDocumentService().copyDocument(node.getNodeRef());

        isDraft = true;
        metadataBlockBean.editDocument(node);
        fileBlockBean.init(node);
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        metadataBlockBean.init(node.getNodeRef(), isDraft);
        fileBlockBean.init(node);
    }

    @Override
    public void restored() {
        fileBlockBean.restore();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (metadataBlockBean.isInEditMode()) {
            metadataBlockBean.save();
            isDraft = false;
            isFinished = false;
            return null;
        }
        reset();
        return outcome;
    }

    @Override
    public String cancel() {
        if (metadataBlockBean.isInEditMode() && !isDraft) {
            metadataBlockBean.cancel();
            return null;
        }
        reset();
        if (metadataBlockBean.isInEditMode() && isDraft) {
            getDocumentService().deleteDocument(node.getNodeRef());
        }
        return super.cancel();
    }

    private void reset() {
        node = null;
        metadataBlockBean.reset();
        fileBlockBean.reset();
        isDraft = false;
    }

    @Override
    public String getContainerTitle() {
        return metadataBlockBean.getDocumentTypeName();
    }

    @Override
    public Object getActionsContext() {
        return metadataBlockBean.getDocument();
    }

    // dialog/container.jsp contains a specific check for a dialog named 'showSpaceDetails'
    public Node getSpace() {
        return null;
    }

    public Node getNode() {
        return node;
    }
    
    // START: getters / setters
    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }
    
    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    public DocumentTemplateService getDocumentTemplateService() {
        if (documentTemplateService == null) {
            documentTemplateService = (DocumentTemplateService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentTemplateService.BEAN_NAME);
        }
        return documentTemplateService;
    }

    public void setMetadataBlockBean(MetadataBlockBean metadataBlockBean) {
        this.metadataBlockBean = metadataBlockBean;
    }

    public MetadataBlockBean getMeta() {
        return metadataBlockBean;
    }

    public void setFileBlockBean(FileBlockBean fileBlockBean) {
        this.fileBlockBean = fileBlockBean;
    }

    public FileBlockBean getFile() {
        return fileBlockBean;
    }

    public boolean isDraft() {
        return isDraft;
    }

    // END: getters / setters
}
