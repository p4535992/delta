package ee.webmedia.alfresco.document.search.web;

<<<<<<< HEAD
import static org.alfresco.web.bean.dialog.BaseDialogBean.hasPermission;

import java.io.Serializable;
import java.util.ArrayList;
=======
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAssociationsService;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

<<<<<<< HEAD
=======
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.AssociationRef;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
<<<<<<< HEAD
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
=======
import org.springframework.web.jsf.FacesContextUtils;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
<<<<<<< HEAD
import ee.webmedia.alfresco.document.search.service.AssocBlockObject;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class SearchBlockBean extends AbstractSearchBlockBean implements DocumentDynamicBlock {
=======
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class SearchBlockBean implements DocumentDynamicBlock {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "SearchBlockBean";

<<<<<<< HEAD
    private transient DocumentSearchService documentSearchService;
    private transient CaseService caseService;
    private DocumentSearchBean documentSearchBean;

    private boolean show;
    private boolean foundSimilar;
    private DocumentDynamic document;
    private Node node;

    public void init(DialogDataProvider provider) {
        document = provider.getDocument();
        init(provider.getNode());
        if (document != null && document.isIncomingInvoice()) {
            Map<String, Object> properties = node.getProperties();
            List<Document> documents = getDocumentSearchService().searchInvoiceBaseDocuments((String) properties.get(DocumentSpecificModel.Props.CONTRACT_NUMBER)
                    , (String) properties.get(DocumentSpecificModel.Props.SELLER_PARTY_NAME));
            assocBlockObjects = new ArrayList<AssocBlockObject>();
            for (Document doc : documents) {
                assocBlockObjects.add(new AssocBlockObject(doc));
            }
        }
    }

    public void init(Node node) {
        this.node = node;
        super.initSearch(node.getNodeRef(), "#{SearchBlockBean.notBaseDocumentSearch}");
    }

    @Override
    public void reset() {
        super.reset();
        show = true;
        foundSimilar = false;
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        documentSearchBean.reset();
    }

    @Override
    public void resetOrInit(DialogDataProvider provider) {
        if (provider == null) {
            reset();
        } else {
<<<<<<< HEAD
            init(provider);
        }
    }

    @Override
    public void addAssocDocHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        QName firstNodeType = node.getType();
        QName secondNodeType = BeanHelper.getNodeService().getType(nodeRef);

        if (secondNodeType.equals(CaseFileModel.Types.CASE_FILE) && !hasPermission(nodeRef, DocumentCommonModel.Privileges.VIEW_CASE_FILE)) {
            MessageUtil.addErrorMessage("caseFile_addAssoc_erro_no_permissions");
            return;
        }

        QName assocType = null;
        NodeRef sourceRef = null;
        NodeRef targetRef = null;
        boolean fromCurrentNode = true;
        // associations between document and some other object
        boolean isFirstNodeDocument = firstNodeType.equals(DocumentCommonModel.Types.DOCUMENT);
        if (isFirstNodeDocument && secondNodeType.equals(DocumentCommonModel.Types.DOCUMENT)) {
            assocType = DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT;
        } else if (isBetweenTypes(firstNodeType, secondNodeType, CaseModel.Types.CASE, DocumentCommonModel.Types.DOCUMENT)) {
            assocType = CaseModel.Associations.CASE_DOCUMENT;
            if (isFirstNodeDocument) {
                fromCurrentNode = false;
            }
        } else if (isBetweenTypes(firstNodeType, secondNodeType, VolumeModel.Types.VOLUME, DocumentCommonModel.Types.DOCUMENT)) {
            assocType = VolumeModel.Associations.VOLUME_DOCUMENT;
            if (isFirstNodeDocument) {
                fromCurrentNode = false;
            }
        } else if (isBetweenTypes(firstNodeType, secondNodeType, CaseFileModel.Types.CASE_FILE, DocumentCommonModel.Types.DOCUMENT)) {
            assocType = CaseFileModel.Assocs.CASE_FILE_DOCUMENT;
            if (isFirstNodeDocument) {
                fromCurrentNode = false;
            }
        } else {
            // associations between volume and some other object (excluding document)
            boolean isFirstNodeVolume = firstNodeType.equals(VolumeModel.Types.VOLUME);
            if (isFirstNodeVolume && secondNodeType.equals(VolumeModel.Types.VOLUME)) {
                assocType = VolumeModel.Associations.VOLUME_VOLUME;
            } else if (isBetweenTypes(firstNodeType, secondNodeType, VolumeModel.Types.VOLUME, CaseFileModel.Types.CASE_FILE)) {
                assocType = CaseFileModel.Assocs.CASE_FILE_VOLUME;
                if (isFirstNodeVolume) {
                    fromCurrentNode = false;
                }
            } else if (isBetweenTypes(firstNodeType, secondNodeType, VolumeModel.Types.VOLUME, CaseModel.Types.CASE)) {
                assocType = VolumeModel.Associations.VOLUME_CASE;
                if (!isFirstNodeVolume) {
                    fromCurrentNode = false;
                }
            } else {
                // associations between case and case file
                if (isBetweenTypes(firstNodeType, secondNodeType, CaseModel.Types.CASE, CaseFileModel.Types.CASE_FILE)) {
                    assocType = CaseFileModel.Assocs.CASE_FILE_CASE;
                    if (firstNodeType.equals(CaseModel.Types.CASE)) {
                        fromCurrentNode = false;
                    }
                }
                // associations between caseFile and caseFile
                else if (firstNodeType.equals(CaseFileModel.Types.CASE_FILE) && secondNodeType.equals(CaseFileModel.Types.CASE_FILE)) {
                    assocType = CaseFileModel.Assocs.CASE_FILE_CASE_FILE;
                }
            }
        }

        if (fromCurrentNode) {
            sourceRef = node.getNodeRef();
            targetRef = nodeRef;
        } else {
            sourceRef = nodeRef;
            targetRef = node.getNodeRef();
        }
        Assert.isTrue(assocType != null);
        saveAssocNow(sourceRef, targetRef, assocType);
    }

    private boolean isBetweenTypes(QName firstNodeType, QName secondNodeType, QName firstType, QName secondType) {
        return (firstNodeType.equals(secondType) && secondNodeType.equals(firstType))
                || firstNodeType.equals(firstType) && secondNodeType.equals(secondType);
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public void addAssocDoc2CaseHandler(ActionEvent event) {
        NodeRef caseRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
<<<<<<< HEAD
        if (!hasPermission(caseRef, DocumentCommonModel.Privileges.VIEW_CASE_FILE)) {
            MessageUtil.addErrorMessage("caseFile_addAssoc_erro_no_permissions");
            return;
        }

        final QName assocType = CaseModel.Associations.CASE_DOCUMENT;
        saveAssocNow(caseRef, sourceObjectRef, assocType);
    }

    @Override
    protected QName getDefaultAssocType() {
        return DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT;
    }

    @Override
    protected void doPostSave() {
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        BeanHelper.getAssocsBlockBean().restore();
        MessageUtil.addInfoMessage("document_assocAdd_success");
    }

<<<<<<< HEAD
    @Override
=======
    private void handleLockedNode(String messageId, NodeRef nodeRef) {
        MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), messageId,
                BeanHelper.getUserService().getUserFullName((String) BeanHelper.getNodeService().getProperty(nodeRef, ContentModel.PROP_LOCK_OWNER)));
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public String getSearchBlockTitle() {
        if (isBaseDocumentSearch()) {
            return MessageUtil.getMessage("document_search_base_title");
        }
        return MessageUtil.getMessage("document_search_docOrCase_title");
    }

<<<<<<< HEAD
    @Override
    protected boolean searchCases() {
        return !isBaseDocumentSearch();
    }

    @Override
    public String getActionColumnFileName() {
        return "/WEB-INF/classes/ee/webmedia/alfresco/document/search/web/document-search-block-actions-column.jsp";
    }

    public boolean isNotBaseDocumentSearch() {
        return !isBaseDocumentSearch();
    }

    private boolean isBaseDocumentSearch() {
        return document != null && ((document.isImapOrDvk() && !document.isNotEditable()) || document.isIncomingInvoice());
=======
    private boolean isBaseDocumentSearch() {
        return (document.isImapOrDvk() && !document.isNotEditable()) || document.isIncomingInvoice();
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        private final List<AssocBlockObject> assocBlockObjects;
=======
        private final String searchValue;
        private final Date regDateTimeBegin;
        private final Date regDateTimeEnd;
        private final List<String> selectedDocumentTypes;
        private final List<Document> documents;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        private final boolean show;
        private final boolean foundSimilar;
        private final boolean expanded;

        private Snapshot(SearchBlockBean bean) {
            document = bean.document;
<<<<<<< HEAD
            assocBlockObjects = bean.assocBlockObjects;
            show = bean.show;
            foundSimilar = bean.foundSimilar;
            expanded = bean.isExpanded();
=======
            searchValue = bean.searchValue;
            regDateTimeBegin = bean.regDateTimeBegin;
            regDateTimeEnd = bean.regDateTimeEnd;
            selectedDocumentTypes = bean.selectedDocumentTypes;

            documents = bean.documents;
            show = bean.show;
            foundSimilar = bean.showSimilarDocumentsBlock;
            expanded = bean.expanded;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        }

        private void restoreState(SearchBlockBean bean) {
            bean.document = document;
<<<<<<< HEAD
            bean.assocBlockObjects = assocBlockObjects;
            bean.show = show;
            bean.foundSimilar = foundSimilar;
            bean.setExpanded(expanded);
=======
            bean.searchValue = searchValue;
            bean.regDateTimeBegin = regDateTimeBegin;
            bean.regDateTimeEnd = regDateTimeEnd;
            bean.selectedDocumentTypes = selectedDocumentTypes;
            bean.documents = documents;
            bean.show = show;
            bean.showSimilarDocumentsBlock = foundSimilar;
            bean.expanded = expanded;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        }
    }

    // END: snapshot logic

    public void findSimilarDocuments(String senderRegNumber) {
<<<<<<< HEAD
        if (StringUtils.isNotBlank(senderRegNumber)) {
            List<Document> documents = getDocumentSearchService().searchIncomingLetterRegisteredDocuments(senderRegNumber);
            assocBlockObjects = new ArrayList<AssocBlockObject>();
            for (Document doc : documents) {
                assocBlockObjects.add(new AssocBlockObject(doc));
            }
            foundSimilar = documents.size() > 0;
        }
    }

    // START: getters / setters

    public Node getSourceNode() {
        return node;
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

<<<<<<< HEAD
    public boolean isFoundSimilar() {
        return foundSimilar;
    }

    public void setFoundSimilar(boolean foundSimilar) {
        this.foundSimilar = foundSimilar;
    }

    public void setDocumentSearchBean(DocumentSearchBean documentSearchBean) {
        this.documentSearchBean = documentSearchBean;
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }

    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentSearchService.BEAN_NAME);
        }
        return documentSearchService;
    }

<<<<<<< HEAD
=======
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

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    protected CaseService getCaseService() {
        if (caseService == null) {
            caseService = (CaseService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(CaseService.BEAN_NAME);
        }
        return caseService;
    }

<<<<<<< HEAD
    @Override
    protected String getBeanName() {
        return BEAN_NAME;
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    // END: getters / setters

}
