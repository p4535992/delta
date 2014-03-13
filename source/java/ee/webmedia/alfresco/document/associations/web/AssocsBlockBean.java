package ee.webmedia.alfresco.document.associations.web;

import java.io.Serializable;
import java.util.List;

import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.service.DocumentService;

/**
 * Block that shows associations of given document with other documents and related cases
 */
public class AssocsBlockBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient DocumentService documentService;
    private Node document;
    private List<DocAssocInfo> docAssocInfos;

    public void init(Node node) {
        reset();
        document = node;
        restore();
    }

    public void restore() {
        docAssocInfos = getDocumentService().getAssocInfos(document);
    }

    public void reset() {
        document = null;
        docAssocInfos = null;
    }

    // START: getters / setters

    public Node getDocument() {
        return document;
    }

    public List<DocAssocInfo> getDocAssocInfos() {
        return docAssocInfos;
    }

    protected DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = BeanHelper.getDocumentService();
        }
        return documentService;
    }
    // END: getters / setters

}
