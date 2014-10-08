package ee.webmedia.alfresco.document.search.web;

<<<<<<< HEAD
import static org.alfresco.web.bean.dialog.BaseDialogBean.hasPermission;
=======
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
>>>>>>> develop-5.1

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

<<<<<<< HEAD
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

=======
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.AssociationRef;
>>>>>>> develop-5.1
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;
import org.springframework.web.jsf.FacesContextUtils;

<<<<<<< HEAD
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
=======
import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.common.listener.RefreshEventListener;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator.DocumentLocationState;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicBlock;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.search.service.AssocBlockObject;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
<<<<<<< HEAD
=======
import ee.webmedia.alfresco.privilege.model.Privilege;
>>>>>>> develop-5.1
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;

<<<<<<< HEAD
public class SearchBlockBean extends AbstractSearchBlockBean implements DocumentDynamicBlock {
=======
public class SearchBlockBean extends AbstractSearchBlockBean implements DocumentDynamicBlock, RefreshEventListener {
>>>>>>> develop-5.1
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "SearchBlockBean";

    private transient DocumentSearchService documentSearchService;
    private transient CaseService caseService;
    private DocumentSearchBean documentSearchBean;

    private boolean show;
<<<<<<< HEAD
    private boolean foundSimilar;
    private DocumentDynamic document;
    private Node node;
=======
    private boolean showSimilarDocumentsBlock;
    private DocumentDynamic document;
    private Node node;
    private List<SelectItem> stores;
>>>>>>> develop-5.1

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
<<<<<<< HEAD
=======
        loadStores();
    }

    private void loadStores() {
        stores = new ArrayList<SelectItem>();
        stores.add(new SelectItem(BeanHelper.getFunctionsService().getFunctionsRoot(), MessageUtil.getMessage("functions_title")));
        for (ArchivalsStoreVO archivalsStoreVO : getGeneralService().getArchivalsStoreVOs()) {
            stores.add(new SelectItem(archivalsStoreVO.getNodeRef(), archivalsStoreVO.getTitle()));
        }
    }

    public List<SelectItem> getStores(FacesContext context, UIInput selectComponent) {
        return stores;
>>>>>>> develop-5.1
    }

    public void init(Node node) {
        this.node = node;
        super.initSearch(node.getNodeRef(), "#{SearchBlockBean.notBaseDocumentSearch}");
    }

<<<<<<< HEAD
=======
    public void storeValueChanged(ValueChangeEvent event) {
        @SuppressWarnings("unchecked")
        final List<NodeRef> selectedStores = (List<NodeRef>) event.getNewValue();
        getFilter().getProperties().put(DocumentDynamicSearchDialog.SELECTED_STORES.toString(), selectedStores);
        refresh();
    }

>>>>>>> develop-5.1
    @Override
    public void reset() {
        super.reset();
        show = true;
<<<<<<< HEAD
        foundSimilar = false;
=======
        showSimilarDocumentsBlock = false;
>>>>>>> develop-5.1
        documentSearchBean.reset();
    }

    @Override
    public void resetOrInit(DialogDataProvider provider) {
        if (provider == null) {
            reset();
        } else {
            init(provider);
        }
    }

    @Override
<<<<<<< HEAD
=======
    public void refresh() {
        for (PropertySheetStateHolder stateHolder : config.getStateHolders().values()) {
            if (stateHolder instanceof DocumentLocationState) {
                ((DocumentLocationState) stateHolder).reset(isInEditMode());
                return;
            }
        }
    }

    @Override
>>>>>>> develop-5.1
    public void addAssocDocHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        QName firstNodeType = node.getType();
        QName secondNodeType = BeanHelper.getNodeService().getType(nodeRef);

<<<<<<< HEAD
        if (secondNodeType.equals(CaseFileModel.Types.CASE_FILE) && !hasPermission(nodeRef, DocumentCommonModel.Privileges.VIEW_CASE_FILE)) {
=======
        if (secondNodeType.equals(CaseFileModel.Types.CASE_FILE) && !hasPermission(nodeRef, Privilege.VIEW_CASE_FILE)) {
>>>>>>> develop-5.1
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

<<<<<<< HEAD
=======
    private boolean hasPermission(NodeRef nodeRef, Privilege viewCaseFile) {
        return BeanHelper.getPrivilegeService().hasPermission(nodeRef, AuthenticationUtil.getRunAsUser(), viewCaseFile);
    }

>>>>>>> develop-5.1
    private boolean isBetweenTypes(QName firstNodeType, QName secondNodeType, QName firstType, QName secondType) {
        return (firstNodeType.equals(secondType) && secondNodeType.equals(firstType))
                || firstNodeType.equals(firstType) && secondNodeType.equals(secondType);
    }

    public void addAssocDoc2CaseHandler(ActionEvent event) {
        NodeRef caseRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
<<<<<<< HEAD
        if (!hasPermission(caseRef, DocumentCommonModel.Privileges.VIEW_CASE_FILE)) {
=======
        if (!hasPermission(caseRef, Privilege.VIEW_CASE_FILE)) {
>>>>>>> develop-5.1
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
        BeanHelper.getAssocsBlockBean().restore();
        MessageUtil.addInfoMessage("document_assocAdd_success");
    }

    @Override
    public String getSearchBlockTitle() {
        if (isBaseDocumentSearch()) {
            return MessageUtil.getMessage("document_search_base_title");
        }
        return MessageUtil.getMessage("document_search_docOrCase_title");
    }

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
        private final List<AssocBlockObject> assocBlockObjects;
        private final boolean show;
        private final boolean foundSimilar;
        private final boolean expanded;

        private Snapshot(SearchBlockBean bean) {
            document = bean.document;
            assocBlockObjects = bean.assocBlockObjects;
            show = bean.show;
<<<<<<< HEAD
            foundSimilar = bean.foundSimilar;
=======
            foundSimilar = bean.showSimilarDocumentsBlock;
>>>>>>> develop-5.1
            expanded = bean.isExpanded();
        }

        private void restoreState(SearchBlockBean bean) {
            bean.document = document;
            bean.assocBlockObjects = assocBlockObjects;
            bean.show = show;
<<<<<<< HEAD
            bean.foundSimilar = foundSimilar;
=======
            bean.showSimilarDocumentsBlock = foundSimilar;
>>>>>>> develop-5.1
            bean.setExpanded(expanded);
        }
    }

    // END: snapshot logic

    public void findSimilarDocuments(String senderRegNumber) {
<<<<<<< HEAD
        if (StringUtils.isNotBlank(senderRegNumber)) {
=======
        final List<AssociationRef> targetAssocs = BeanHelper.getNodeService().getTargetAssocs(document.getNodeRef(), DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
        if (targetAssocs.isEmpty() && StringUtils.isNotBlank(senderRegNumber)) {
>>>>>>> develop-5.1
            List<Document> documents = getDocumentSearchService().searchIncomingLetterRegisteredDocuments(senderRegNumber);
            assocBlockObjects = new ArrayList<AssocBlockObject>();
            for (Document doc : documents) {
                assocBlockObjects.add(new AssocBlockObject(doc));
            }
<<<<<<< HEAD
            foundSimilar = documents.size() > 0;
=======
            showSimilarDocumentsBlock = documents.size() > 0;
>>>>>>> develop-5.1
        }
    }

    // START: getters / setters

    public Node getSourceNode() {
        return node;
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
=======
    public boolean isShowSimilarDocumentsBlock() {
        return showSimilarDocumentsBlock;
    }

    public void setShowSimilarDocumentsBlock(boolean foundSimilar) {
        showSimilarDocumentsBlock = foundSimilar;
>>>>>>> develop-5.1
    }

    public void setDocumentSearchBean(DocumentSearchBean documentSearchBean) {
        this.documentSearchBean = documentSearchBean;
    }

    protected DocumentSearchService getDocumentSearchService() {
        if (documentSearchService == null) {
            documentSearchService = (DocumentSearchService) FacesContextUtils.getRequiredWebApplicationContext( //
                    FacesContext.getCurrentInstance()).getBean(DocumentSearchService.BEAN_NAME);
        }
        return documentSearchService;
    }

    protected CaseService getCaseService() {
        if (caseService == null) {
            caseService = (CaseService) FacesContextUtils.getRequiredWebApplicationContext(//
                    FacesContext.getCurrentInstance()).getBean(CaseService.BEAN_NAME);
        }
        return caseService;
    }

    @Override
    protected String getBeanName() {
        return BEAN_NAME;
    }

    // END: getters / setters

}
