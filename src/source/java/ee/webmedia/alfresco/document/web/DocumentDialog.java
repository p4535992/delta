package ee.webmedia.alfresco.document.web;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.ActionUtil;

/**
 * @author Alar Kvell
 */
public class DocumentDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient DocumentService documentService;
    private MetadataBlockBean metadataBlockBean;
    private FileBlockBean fileBlockBean;
    private boolean created;

    private Node node;

    public void create(ActionEvent event) {
        QName documentTypeId = QName.resolveToQName(getNamespaceService(), ActionUtil.getParam(event, "documentType"));
        node = getDocumentService().createDocument(documentTypeId);
        created = true;
    }

    public void open(ActionEvent event) {
        node = getDocumentService().getDocument(new NodeRef(ActionUtil.getParam(event, "nodeRef")));
        created = false;
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        metadataBlockBean.init(node.getNodeRef(), created);
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
            created = false;
            isFinished = false;
            return null;
        }
        reset();
        return outcome;
    }

    @Override
    public String cancel() {
        if (metadataBlockBean.isInEditMode() && !created) {
            metadataBlockBean.cancel();
            return null;
        }
        if (metadataBlockBean.isInEditMode() && created) {
            getDocumentService().deleteDocument(node.getNodeRef());
        }
        reset();
        return super.cancel();
    }

    private void reset() {
        node = null;
        metadataBlockBean.reset();
        fileBlockBean.reset();
        created = false;
    }

    @Override
    public String getContainerTitle() {
        return metadataBlockBean.getDocumentTypeName();
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
    // END: getters / setters
}
