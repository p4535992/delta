package ee.webmedia.alfresco.document.search.web;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class SearchBlockBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient DocumentSearchService documentSearchService;
    private transient CaseService caseService;
    private transient DocumentService documentService;

    private DocumentSearchBean documentSearchBean;

    private Node node;
    private String searchValue;
    private Date regDateTimeBegin;
    private Date regDateTimeEnd;
    private List<QName> selectedDocumentTypes;
    private List<Document> documents;
    private boolean show;
    private boolean foundSimilar;
    private boolean expanded;

    public void init(Node node) {
        reset();
        this.node = node;
    }

    public void reset() {
        searchValue = null;
        documents = null;
        show = true;
        foundSimilar = false;
        expanded = false;
        documentSearchBean.reset();
    }

    /** @param event from JSP */
    public void setup(ActionEvent event) {
        try {
            documents = getDocumentSearchService().searchDocumentsAndOrCases(searchValue, regDateTimeBegin, regDateTimeEnd, selectedDocumentTypes);
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            documents = Collections.<Document> emptyList();
        }
    }

    public DocAssocInfo addTargetAssoc(NodeRef targetRef, QName assocType) {
        Map<String, Map<String, AssociationRef>> addedAssociations = node.getAddedAssociations();
        Map<String, AssociationRef> newAssoc = addedAssociations.get(assocType.toString());
        if (newAssoc == null) {
            newAssoc = new HashMap<String, AssociationRef>(1);
        }
        final AssociationRef assocRef = new AssociationRef(node.getNodeRef(), assocType, targetRef);
        newAssoc.put(node.getNodeRefAsString(), assocRef);
        addedAssociations.put(assocType.toString(), newAssoc);
        show = false;
        return getDocumentService().getDocAssocInfo(assocRef, true);
    }

    // START: snapshot logic
    public Snapshot createSnapshot() {
        return new Snapshot(this);
    }

    public void restoreSnapshot(Snapshot snapshot) {
        snapshot.restoreState(this);
    }

    public static class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Node node;
        private final String searchValue;
        private final List<Document> documents;
        private final boolean show;
        private final boolean foundSimilar;
        private final boolean expanded;

        private Snapshot(SearchBlockBean bean) {
            node = bean.node;
            searchValue = bean.searchValue;
            documents = bean.documents;
            show = bean.show;
            foundSimilar = bean.foundSimilar;
            expanded = bean.expanded;
        }

        private void restoreState(SearchBlockBean bean) {
            bean.node = node;
            bean.searchValue = searchValue;
            bean.documents = documents;
            bean.show = show;
            bean.foundSimilar = foundSimilar;
            bean.expanded = expanded;
        }
    }

    // END: snapshot logic

    public void findSimilarDocuments(String senderRegNumber, QName documentType) {
        if (StringUtils.isNotBlank(senderRegNumber)) {
            documents = getDocumentSearchService().searchIncomingLetterRegisteredDocuments(senderRegNumber, documentType);
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

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void initDocSearch() {
        documentSearchBean.init();
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

    public void setDocumentSearchBean(DocumentSearchBean documentSearchBean) {
        this.documentSearchBean = documentSearchBean;
    }

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    public Date getRegDateTimeBegin() {
        return regDateTimeBegin;
    }

    public void setRegDateTimeBegin(Date regDateTimeBegin) {
        this.regDateTimeBegin = regDateTimeBegin;
    }

    public Date getRegDateTimeEnd() {
        return regDateTimeEnd;
    }

    public void setRegDateTimeEnd(Date regDateTimeEnd) {
        this.regDateTimeEnd = regDateTimeEnd;
    }

    public List<QName> getSelectedDocumentTypes() {
        return selectedDocumentTypes;
    }

    public void setSelectedDocumentTypes(List<QName> selectedDocumentTypes) {
        this.selectedDocumentTypes = selectedDocumentTypes;
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
