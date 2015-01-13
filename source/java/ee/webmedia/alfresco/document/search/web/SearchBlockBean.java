package ee.webmedia.alfresco.document.search.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAssociationsService;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class SearchBlockBean implements DocumentDynamicBlock {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "SearchBlockBean";

    public static final String PARAM_NODEREF = "nodeRef";
    private transient DocumentSearchService documentSearchService;
    private transient CaseService caseService;
    private transient DocumentService documentService;

    private DocumentSearchBean documentSearchBean;

    private DocumentDynamic document;
    private String searchValue;
    private Date regDateTimeBegin;
    private Date regDateTimeEnd;
    private List<String> selectedDocumentTypes;
    private List<Document> documents;
    private boolean show;
    private boolean showSimilarDocumentsBlock;
    private boolean expanded;

    public void init(DocumentDynamic document) {
        reset();
        this.document = document;
        if (document.isIncomingInvoice()) {
            Map<String, Object> properties = document.getNode().getProperties();
            documents = getDocumentSearchService().searchInvoiceBaseDocuments((String) properties.get(DocumentSpecificModel.Props.CONTRACT_NUMBER)
                    , (String) properties.get(DocumentSpecificModel.Props.SELLER_PARTY_NAME));
        }
    }

    public void reset() {
        searchValue = null;
        regDateTimeBegin = null;
        regDateTimeEnd = null;
        documents = null;
        show = true;
        showSimilarDocumentsBlock = false;
        expanded = false;
        document = null;
        documentSearchBean.reset();
    }

    @Override
    public void resetOrInit(DialogDataProvider provider) {
        if (provider == null) {
            reset();
        } else {
            init(provider.getDocument());
        }
    }

    public void setup(ActionEvent event) {
        try {
            documents = getDocumentSearchService().searchDocumentsAndOrCases(searchValue, regDateTimeBegin, regDateTimeEnd, selectedDocumentTypes, !isBaseDocumentSearch())
                    .getFirst();
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            documents = Collections.<Document> emptyList();
        }
    }

    public void addAssocDocHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        saveAssocNow(document.getNodeRef(), nodeRef, DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT);
    }

    public void addAssocDoc2CaseHandler(ActionEvent event) {
        NodeRef caseRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        final QName assocType = CaseModel.Associations.CASE_DOCUMENT;
        saveAssocNow(caseRef, document.getNodeRef(), assocType);
    }

    private void saveAssocNow(final NodeRef sourceRef, final NodeRef targetRef, final QName assocType) {
        final List<AssociationRef> targetAssocs = BeanHelper.getNodeService().getTargetAssocs(sourceRef, assocType);
        for (AssociationRef associationRef : targetAssocs) {
            if (associationRef.getTargetRef().equals(targetRef) && associationRef.getTypeQName().equals(assocType)) {
                MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_assocAdd_error_alreadyExists");
                return;
            }
        }
        try {
            getDocumentAssociationsService().createAssoc(sourceRef, targetRef, assocType);
        } catch (NodeLockedException e) {
            NodeRef nodeRef = e.getNodeRef();
            String messageId = nodeRef.equals(sourceRef) ? "document_assocAdd_error_sourceLocked" : "document_assocAdd_error_targetLocked";
            handleLockedNode(messageId, nodeRef);
            return;
        }
        BeanHelper.getAssocsBlockBean().restore();
        MessageUtil.addInfoMessage("document_assocAdd_success");
    }

    private void handleLockedNode(String messageId, NodeRef nodeRef) {
        MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), messageId,
                BeanHelper.getUserService().getUserFullName((String) BeanHelper.getNodeService().getProperty(nodeRef, ContentModel.PROP_LOCK_OWNER)));
    }

    public String getSearchBlockTitle() {
        if (isBaseDocumentSearch()) {
            return MessageUtil.getMessage("document_search_base_title");
        }
        return MessageUtil.getMessage("document_search_docOrCase_title");
    }

    private boolean isBaseDocumentSearch() {
        return (document.isImapOrDvk() && !document.isNotEditable()) || document.isIncomingInvoice();
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

        private final DocumentDynamic document;
        private final String searchValue;
        private final Date regDateTimeBegin;
        private final Date regDateTimeEnd;
        private final List<String> selectedDocumentTypes;
        private final List<Document> documents;
        private final boolean show;
        private final boolean foundSimilar;
        private final boolean expanded;

        private Snapshot(SearchBlockBean bean) {
            document = bean.document;
            searchValue = bean.searchValue;
            regDateTimeBegin = bean.regDateTimeBegin;
            regDateTimeEnd = bean.regDateTimeEnd;
            selectedDocumentTypes = bean.selectedDocumentTypes;

            documents = bean.documents;
            show = bean.show;
            foundSimilar = bean.showSimilarDocumentsBlock;
            expanded = bean.expanded;
        }

        private void restoreState(SearchBlockBean bean) {
            bean.document = document;
            bean.searchValue = searchValue;
            bean.regDateTimeBegin = regDateTimeBegin;
            bean.regDateTimeEnd = regDateTimeEnd;
            bean.selectedDocumentTypes = selectedDocumentTypes;
            bean.documents = documents;
            bean.show = show;
            bean.showSimilarDocumentsBlock = foundSimilar;
            bean.expanded = expanded;
        }
    }

    // END: snapshot logic

    public void findSimilarDocuments(String senderRegNumber) {
        final List<AssociationRef> targetAssocs = BeanHelper.getNodeService().getTargetAssocs(getNode().getNodeRef(), DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
        if (targetAssocs.isEmpty() && StringUtils.isNotBlank(senderRegNumber)) {
            documents = getDocumentSearchService().searchIncomingLetterRegisteredDocuments(senderRegNumber);
            showSimilarDocumentsBlock = documents.size() > 0;
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    // START: getters / setters

    public Node getNode() {
        return document.getNode();
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    public boolean isShowSimilarDocumentsBlock() {
        return showSimilarDocumentsBlock;
    }

    public void setShowSimilarDocumentsBlock(boolean foundSimilar) {
        showSimilarDocumentsBlock = foundSimilar;
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

    public List<String> getSelectedDocumentTypes() {
        return selectedDocumentTypes;
    }

    public void setSelectedDocumentTypes(List<String> selectedDocumentTypes) {
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
