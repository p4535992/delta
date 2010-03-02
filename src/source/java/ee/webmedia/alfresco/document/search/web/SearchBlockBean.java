package ee.webmedia.alfresco.document.search.web;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.metadata.web.MetadataBlockBean;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.ActionUtil;

public class SearchBlockBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient DocumentSearchService documentSearchService;
    private transient CaseService caseService;
    private transient DocumentService documentService;
    
    private Node node;
    private String searchValue;
    private List<Document> documents;
    private boolean show;
    private boolean foundSimilar;
    private boolean includeCaseTitles;

    public void init(Node node) {
        reset();
        this.node = node;
    }
    
    public void reset() {
        searchValue = null;
        documents = null;
        show = true;
        foundSimilar = false;
        includeCaseTitles = false;
    }
    
    /** @param event from JSP */
    public void setup(ActionEvent event) {
        documents = getDocumentSearchService().searchDocumentsQuick(searchValue, isIncludeCases());
    }
    
    public DocAssocInfo addTargetAssoc(NodeRef targetRef, QName assocType) {
        Map<String, Map<String, AssociationRef>> addedAssociations = node.getAddedAssociations();
        Map<String, AssociationRef> newAssoc = addedAssociations.get(assocType.toString());
        if (newAssoc == null) {
            newAssoc = new HashMap<String, AssociationRef>(1);
        }
        final AssociationRef assocInfo = new AssociationRef(node.getNodeRef(), assocType, targetRef);
        newAssoc.put(node.getNodeRefAsString(), assocInfo);
        addedAssociations.put(assocType.toString(), newAssoc);
        show = false;
        return getDocumentService().getDocAssocInfo(assocInfo, false);
    }

    // START: snapshot logic
    public Snapshot createSnapshot() {
        return new Snapshot(this);
    }

    public void restoreSnapshot(Snapshot snapshot) {
        snapshot.restoreState(this);
    }

    public static class Snapshot implements Serializable{
        private static final long serialVersionUID = 1L;

        private Node node;
        private String searchValue;
        private List<Document> documents;
        private boolean show;
        private boolean foundSimilar;
        private boolean includeCaseTitles;

        private Snapshot(SearchBlockBean bean) {
            this.node = bean.node;
            this.searchValue = bean.searchValue;
            this.documents = bean.documents;
            this.show = bean.show;
            this.foundSimilar = bean.foundSimilar;
            this.includeCaseTitles = bean.includeCaseTitles;
        }

        private void restoreState(SearchBlockBean bean) {
            bean.node = this.node;
            bean.searchValue = this.searchValue;
            bean.documents = this.documents;
            bean.show = this.show;
            bean.foundSimilar = this.foundSimilar;
            bean.includeCaseTitles = this.includeCaseTitles;
        }
    }
    // END: snapshot logic
    
    public void findSimilarDocuments(String senderRegNumber) {
        if (StringUtils.isNotBlank(senderRegNumber)) {
            documents = getDocumentSearchService().searchIncomingLetterRegisteredDocuments(senderRegNumber);
            foundSimilar = documents.size() > 0;
        }
    }

    private DocumentService getDocumentService() {
        if (documentService == null) {
            documentService = (DocumentService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(DocumentService.BEAN_NAME);
        }
        return documentService;
    }
    
    public boolean isIncludeCases() {
        return includeCaseTitles;
    }
    
    public void setIncludeCaseTitles(boolean includeCaseTitles) {
        this.includeCaseTitles = includeCaseTitles;
    }
    // START: getters / setters

    public boolean isShow() {
        return show;
    }
    
    public boolean isFoundSimilar() {
        return foundSimilar;
    }

    public void setFoundSimilar(boolean foundSimilar) {
        this.foundSimilar = foundSimilar;
    }

    public int getCount() {
        if (documents == null) {
            return 0;
        }
        return documents.size();
    }
    
    public List<Document> getDocuments() {
        return documents;
    }

    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext( // 
                    FacesContext.getCurrentInstance()).getBean(DocumentSearchService.BEAN_NAME);
        }
        return documentSearchService;
    }
    
    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    protected CaseService getCaseService() {
        if (caseService == null) {
            caseService = (CaseService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(CaseService.BEAN_NAME);
        }
        return caseService;
    }
    // END: getters / setters
}
