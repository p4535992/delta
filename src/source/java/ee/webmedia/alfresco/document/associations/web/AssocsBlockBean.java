package ee.webmedia.alfresco.document.associations.web;

import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.bean.repository.Node;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.service.DocumentService;

/**
 * Block that shows associations of given document with other documents and related cases
 * 
 * @author Ats Uiboupin
 */
public class AssocsBlockBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient DocumentService documentService;
    private Node document;
    private List<DocAssocInfo> docAssocInfos;
    private boolean expanded = false;

    public void init(Node node) {
        reset();
        this.document = node;
        restore();
    }

    public void restore() {
        docAssocInfos = getDocumentService().getAssocInfos(document);
    }

    public void reset() {
        document = null;
        docAssocInfos = null;
        expanded = false;
    }

    public void expandedAction(ActionEvent event) {
        this.expanded = !this.expanded;
    }

    // START: getters / setters

    public Node getDocument() {
        return document;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public List<DocAssocInfo> getDocAssocInfos() {
        return docAssocInfos;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(
                    FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }
    // END: getters / setters

}
