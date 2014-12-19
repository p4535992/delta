package ee.webmedia.alfresco.docdynamic.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAssociationsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentLockHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFileService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getLogService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getUserService;
import static ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator.ACCESS_RESTRICTION_CHANGE_REASON_ERROR;
import static ee.webmedia.alfresco.docdynamic.web.ChangeReasonModalComponent.ACCESS_RESTRICTION_CHANGE_REASON_MODAL_ID;
import static ee.webmedia.alfresco.docdynamic.web.ChangeReasonModalComponent.DELETE_DOCUMENT_REASON_MODAL_ID;
import static ee.webmedia.alfresco.utils.RepoUtil.getReferenceOrNull;
import static ee.webmedia.alfresco.utils.RepoUtil.isInWorkspace;
import static ee.webmedia.alfresco.utils.RepoUtil.isSaved;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.alfresco.web.config.DialogsConfigElement.DialogConfig;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.cases.service.UnmodifiableCase;
import ee.webmedia.alfresco.classificator.constant.DocTypeAssocType;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.common.propertysheet.component.SubPropertySheetItem;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.AssociationModel;
import ee.webmedia.alfresco.docadmin.service.DocumentType;
import ee.webmedia.alfresco.docadmin.service.DocumentTypeVersion;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentType;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.web.ChangeReasonModalComponent.ChangeReasonEvent;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog.DocDialogSnapshot;
import ee.webmedia.alfresco.document.associations.model.DocAssocInfo;
import ee.webmedia.alfresco.document.associations.web.AssocsBlockBean;
import ee.webmedia.alfresco.document.assocsdyn.service.DocumentAssociationsService;
import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.search.web.AbstractSearchBlockBean;
import ee.webmedia.alfresco.document.search.web.BlockBeanProviderProvider;
import ee.webmedia.alfresco.document.search.web.SearchBlockBean;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.sendout.web.SendOutBlockBean;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.document.service.EventsLoggingHelper;
import ee.webmedia.alfresco.document.web.FavoritesModalComponent;
import ee.webmedia.alfresco.document.web.FavoritesModalComponent.AddToFavoritesEvent;
import ee.webmedia.alfresco.document.web.evaluator.DeleteDocumentEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.DocumentNotInDraftsFunctionActionEvaluator;
import ee.webmedia.alfresco.document.web.evaluator.RegisterDocumentEvaluator;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.menu.ui.MenuBean;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.simdhs.servlet.ExternalAccessServlet;
import ee.webmedia.alfresco.user.model.UserModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageData;
import ee.webmedia.alfresco.utils.MessageDataImpl;
import ee.webmedia.alfresco.utils.MessageDataWrapper;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformMultiReasonException;
import ee.webmedia.alfresco.utils.WebUtil;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.web.WorkflowBlockBean;

/**
 * To open this dialog you must call exactly one of the methods in actionListener section, either manually or from {@code <a:actionLink actionListener="..."}
 * <p>
 * For example, to open this dialog from JSP, you should use
 * <code>&lt;a:actionLink actionListener="#{DocumentDynamicDialog.open...}" &gt;&lt;f:param name="nodeRef" value="..." /&gt;&lt;/a:actionLink&gt;</code>
 * </p>
 * <p>
 * For example, to open this dialog manually, (for example {@link ExternalAccessServlet} does this), you should call {@code documentDynamicDialog.open...(nodeRef)}
 * </p>
 */
public class DocumentDynamicDialog extends BaseSnapshotCapableWithBlocksDialog<DocDialogSnapshot, DocumentDynamicBlock, DialogDataProvider> implements DialogDataProvider,
        BlockBeanProviderProvider {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentDynamicDialog.class);

    public static final String BEAN_NAME = "DocumentDynamicDialog";
    public static final QName TEMP_ACCESS_RESTRICTION_CHANGE_REASON = RepoUtil.createTransientProp(DocumentCommonModel.Props.ACCESS_RESTRICTION_CHANGE_REASON.getLocalName());
    public static final QName TEMP_ARCHIVAL_ACTIVITY_NODE_REF = RepoUtil.createTransientProp("archivalActivityNodeRef");
    public static final QName TEMP_VALIDATE_WITHOUT_SAVE = RepoUtil.createTransientProp("validateWithoutSave");
    public static final String DVK_RECEIVED = QName.createQName(DocumentCommonModel.DOCCOM_URI, "dvkReceived").toString();
    public static final String FORWARDED_DEC_DOCUMENTS = QName.createQName(DocumentCommonModel.DOCCOM_URI, "forwardedDecDocuments").toString();
    private static final String PARAM_NODEREF = "nodeRef";
    private String renderedModal;
    private boolean showConfirmationPopup;

    // TODO kontrollida et kustutatud dokumendi ekraanile tagasipöördumine töötaks... või tahavad teised blokid laadida uuesti asju? ja siis oleks mõtekam dialoogi mitte kuvada?

    // Closing this dialog has the following logic:
    // ... view -> *back -> close
    // ... edit -> *back -> kui on tuldud view'ist, siis sinna tagasi, muidu close; ja kui draft, siis lisaks delete
    // URL -> openView -> *back -> close -> siis kuvatakse avaleht, sest URList avamine teeb viewstack'i tühjaks ja paneb avalehe esimeseks

    // =========================================================================
    // Dialog entry points start
    // 1 - ACTIONLISTENER methods
    // =========================================================================

    public void openFromUrl(NodeRef docRef) {
        open(docRef, false);
    }

    /** @param event */
    public void openFromChildList(ActionEvent event) {
        if (!ActionUtil.hasParam(event, "nodeRef")) {
            return;
        }
        NodeRef childNodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        NodeRef docRef = BeanHelper.getGeneralService().getAncestorNodeRefWithType(childNodeRef, DocumentCommonModel.Types.DOCUMENT);
        if (docRef != null) {
            openFromDocumentList(docRef);
        }
    }

    /** @param event */
    public void openFromDocumentList(ActionEvent event) {
        openFromDocumentList(new NodeRef(ActionUtil.getParam(event, "nodeRef")));
    }

    /** @param docRef */
    private void openFromDocumentList(NodeRef docRef) {
        if (validateExists(docRef)) {
            // TODO if isIncomingInvoice() then inEditMode = true;
            boolean inEditMode = getDocumentDynamicService().isImapOrDvk(docRef);
            open(docRef, inEditMode);
        }
    }

    /** @param event */
    public void createDraft(ActionEvent event) {
        String documentTypeId = ActionUtil.getParam(event, "typeId");
        DocumentDynamic doc = getDocumentDynamicService().createNewDocumentInDrafts(documentTypeId).getFirst();
        setLocationFromVolume(doc);
        propertySheet = null;
        open(doc.getNodeRef(), doc, true, true);
    }

    /** @param event */
    public void createDraftForArchivalActivity(ActionEvent event) {
        NodeRef archivalActivityNodeRef = new NodeRef(ActionUtil.getParam(event, "archivalNodeRef"));
        String documentTypeId = BeanHelper.getParametersService().getStringParameter(Parameters.ARCHIVAL_ACTIVITY_DOC_TYPE_ID);
        DocumentDynamic doc = BeanHelper.getDocumentDynamicService().createNewDocumentForArchivalActivity(archivalActivityNodeRef, documentTypeId);
        if (doc == null) {
            MessageUtil.addErrorMessage("document_create_invalid_doc_type", documentTypeId);
            return;
        }
        setLocationFromVolume(doc);
        open(doc.getNodeRef(), doc, true, true);
    }

    /** When creating draft from caseFile/volume view, set caseFile/volume location as document location */
    private void setLocationFromVolume(DocumentDynamic doc) {
        NodeRef functionRef = null;
        NodeRef seriesRef = null;
        NodeRef volumeRef = null;
        DialogConfig currentDialog = null;
        try {
            currentDialog = BeanHelper.getDialogManager() != null ? BeanHelper.getDialogManager().getCurrentDialog() : null;
        } catch (Exception e) {

        }
        String previousDialogName = currentDialog != null ? currentDialog.getName() : null;
        if ("caseFileDialog".equals(previousDialogName)) {
            CaseFile caseFile = BeanHelper.getCaseFileDialog().getCaseFile();
            WmNode node = caseFile != null ? caseFile.getNode() : null;
            if (caseFile != null && isValidLocation(node, caseFile.getSeries(), doc.getDocumentTypeId())) {
                volumeRef = caseFile.getNodeRef();
                seriesRef = caseFile.getSeries();
                functionRef = caseFile.getFunction();
            }
        } else if ("caseDocListDialog".equals(previousDialogName)) {
            Volume volume = BeanHelper.getCaseDocumentListDialog().getParent();
            Node node = volume != null ? volume.getNode() : null;
            if (volume != null && isValidLocation(node, volume.getSeriesNodeRef(), doc.getDocumentTypeId())) {
                volumeRef = node.getNodeRef();
                seriesRef = volume.getSeriesNodeRef();
                functionRef = volume.getFunctionNodeRef();
            }
        }
        if (functionRef != null) {
            doc.setFunction(functionRef);
            doc.setCase(null);
        }
        if (seriesRef != null) {
            doc.setSeries(seriesRef);
        }
        if (volumeRef != null) {
            doc.setVolume(volumeRef);
        }
    }

    private boolean isValidLocation(Node node, NodeRef seriesRef, String documentTypeId) {
        if (seriesRef != null && node != null && isSaved(node) && isInWorkspace(node) && BeanHelper.getGeneralService().getStore().equals(node.getNodeRef().getStoreRef())) {
            Series series = BeanHelper.getSeriesService().getSeriesByNodeRef(seriesRef);
            return series.getDocType().contains(documentTypeId);
        }
        return false;
    }

    public void changeByNewDocument(@SuppressWarnings("unused") ActionEvent event) {
        DocumentDynamic baseDoc = getDocument();
        String docName;
        if (baseDoc.getRegDateTime() != null) {
            docName = MessageUtil.getMessage("docdyn_changeByNewDocument_docName_registered"//
                    , getDocumentType().getName()
                    , baseDoc.getRegNumber()
                    , Utils.getDateFormat(FacesContext.getCurrentInstance()).format(baseDoc.getRegDateTime()));
        } else {
            docName = MessageUtil.getMessage("docdyn_changeByNewDocument_docName"//
                    , getDocumentType().getName());
        }

        Map<QName, Serializable> overrides = new HashMap<QName, Serializable>(2);
        overrides.put(DocumentCommonModel.Props.DOC_NAME, docName);
        overrides.put(DocumentCommonModel.Props.UPDATE_METADATA_IN_FILES, Boolean.TRUE);

        NodeRef docRef = getDocumentDynamicService().copyDocumentToDrafts(baseDoc, overrides,
                DocumentCommonModel.Props.REG_NUMBER,
                DocumentCommonModel.Props.REG_DATE_TIME,
                DocumentCommonModel.Props.SHORT_REG_NUMBER,
                DocumentCommonModel.Props.INDIVIDUAL_NUMBER,
                DocumentCommonModel.Props.DOC_STATUS);

        open(docRef, true);

        addTargetAssoc(baseDoc.getNodeRef(), DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP, false, true);
    }

    public void copyDocument(@SuppressWarnings("unused") ActionEvent event) {
        DocumentDynamic baseDoc = getCurrentSnapshot().document;

        Map<QName, Serializable> overrides = new HashMap<QName, Serializable>(1);
        overrides.put(DocumentCommonModel.Props.UPDATE_METADATA_IN_FILES, Boolean.TRUE);
        NodeRef docRef = getDocumentDynamicService().copyDocumentToDrafts(baseDoc, overrides,
                DocumentSpecificModel.Props.ENTRY_DATE,
                DocumentSpecificModel.Props.ENTRY_SAP_NUMBER,
                DocumentCommonModel.Props.REG_NUMBER,
                DocumentCommonModel.Props.REG_DATE_TIME,
                DocumentCommonModel.Props.SHORT_REG_NUMBER,
                DocumentCommonModel.Props.INDIVIDUAL_NUMBER,
                DocumentCommonModel.Props.DOC_STATUS);

        getDocumentDynamicService().setOwner(docRef, AuthenticationUtil.getRunAsUser(), false);

        open(docRef, true);
    }

    public void createAssoc(ActionEvent event) {
        NodeRef assocModelRef = ActionUtil.getParam(event, AssocsBlockBean.PARAM_ASSOC_MODEL_REF, NodeRef.class);
        createAssoc(assocModelRef);
    }

    private void createAssoc(NodeRef assocModelRef) {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            throw new RuntimeException("No current document");
        }
        NodeRef baseDocRef = snapshot.document.getNodeRef();
        try {
            BeanHelper.getDocLockService().checkForLock(baseDocRef);
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("docdyn_createAssoc_error_docLocked");
            return;
        }
        Pair<DocumentDynamic, AssociationModel> newDocumentAndAssociatonModel = BeanHelper.getDocumentAssociationsService().createAssociatedDocFromModel(baseDocRef, assocModelRef);
        DocumentDynamic newDocument = newDocumentAndAssociatonModel.getFirst();
        open(newDocument.getNodeRef(), newDocument, true, false);
        addWorkflowAssocs(baseDocRef, newDocumentAndAssociatonModel.getSecond().getAssociationType().getAssocBetweenDocs());
        getAssocsBlockBean().sortDocAssocInfos();
    }

    public void createFollowUpReport(@SuppressWarnings("unused") ActionEvent event) {
        DocumentDynamic baseDoc = getDocument();
        String docName;
        if (baseDoc.getRegDateTime() != null) {
            docName = MessageUtil.getMessage("docdyn_createFollowUpReport_docName_registered"//
                    , getDocumentType().getName()
                    , baseDoc.getRegNumber()
                    , Utils.getDateFormat(FacesContext.getCurrentInstance()).format(baseDoc.getRegDateTime()));
        } else {
            docName = MessageUtil.getMessage("docdyn_createFollowUpReport_docName"//
                    , getDocumentType().getName());
        }
        createAssoc(DocTypeAssocType.FOLLOWUP, SystematicDocumentType.REPORT.getId());
        if (baseDoc.equals(getDocument())) {
            return;
        }
        getDocument().setDocName(docName);
    }

    public void createFollowUpErrandOrderAbroad(@SuppressWarnings("unused") ActionEvent event) {
        createAssoc(DocTypeAssocType.FOLLOWUP, SystematicDocumentType.ERRAND_ORDER_ABROAD.getId());
    }

    private void createAssoc(DocTypeAssocType assocType, String targetDocTypeId) {
        List<? extends AssociationModel> associationModels = getDocumentType().getAssociationModels(assocType);
        for (AssociationModel associationModel : associationModels) {
            if (targetDocTypeId.equals(associationModel.getDocType())) {
                createAssoc(associationModel.getNodeRef());
                break;
            }
        }
    }

    public void searchDocsAndCases(@SuppressWarnings("unused") ActionEvent event) {
        getCurrentSnapshot().showDocsAndCasesAssocs = true;
        SearchBlockBean searchBlockBean = getSearchBlock();
        searchBlockBean.init(getDataProvider());
        searchBlockBean.setExpanded(true);
    }

    public void addFollowUpHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        addTargetAssocAndReopen(nodeRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
    }

    public void addFollowUpHandlerSimilarDocuments(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        addTargetAssocAndReopen(nodeRef, DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP);
        getSearchBlock().setShowSimilarDocumentsBlock(false);
        setShowSaveAndRegisterButton(true);
    }

    public void addReplyHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        addTargetAssocAndReopen(nodeRef, DocumentCommonModel.Assocs.DOCUMENT_REPLY);
    }

    private void addTargetAssocAndReopen(NodeRef targetRef, QName targetType) {
        addTargetAssoc(targetRef, targetType, true, true);
        addWorkflowAssocs(targetRef, targetType);
        updateFollowUpOrReplyProperties(targetRef);
        openOrSwitchModeCommon(getDocument(), true);
        getSearchBlock().setShow(false);
        MessageUtil.addInfoMessage("document_assocAdd_success");
    }

    private void addWorkflowAssocs(NodeRef targetRef, QName targetType) {
        String currentDocTypeId = getDocumentType() != null ? getDocumentType().getId() : null;
        QName assocType = DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(targetType) ? DocumentAdminModel.Types.FOLLOWUP_ASSOCIATION
                : DocumentAdminModel.Types.REPLY_ASSOCIATION;
        DocumentAssociationsService documentAssociationsService = BeanHelper.getDocumentAssociationsService();
        if (BeanHelper.getDocumentAssociationsService().isAddCompoundWorkflowAssoc(targetRef, currentDocTypeId, assocType)) {
            WorkflowService workflowService = BeanHelper.getWorkflowService();
            WmNode document = getNode();
            for (NodeRef workflowRef : documentAssociationsService.getDocumentIndependentWorkflowAssocs(targetRef)) {
                addTargetAssoc(workflowRef, DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT, true, false);
                Map<QName, Serializable> docProps = RepoUtil.toQNameProperties(document.getProperties());
                getDocumentDynamicService().setOwnerFromActiveResponsibleTask(
                        workflowService.getCompoundWorkflowOfType(workflowRef, Collections.singletonList(WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW)),
                        document.getNodeRef(), docProps);
                document.getProperties().putAll(RepoUtil.toStringProperties(docProps));
            }
        }
    }

    private void addTargetAssoc(NodeRef targetRef, QName targetType, boolean isSourceAssoc, boolean skipNotSearchable) {
        AssocsBlockBean assocsBlockBean = getAssocsBlockBean();
        AssociationRef assocRef = RepoUtil.addAssoc(getNode(), targetRef, targetType, true);
        // TODO: clarify this logic (would be better if retrieving associations could be done on common basis,
        // but as compoundWorkflow associations are retrieved differently, this is workaround to retrieve workflow associations here)
        boolean getAsSourceAssoc = DocumentCommonModel.Assocs.WORKFLOW_DOCUMENT.equals(targetType) ? !isSourceAssoc : isSourceAssoc;
        final DocAssocInfo docAssocInfo = getDocumentAssociationsService().getDocListUnitAssocInfo(assocRef, getAsSourceAssoc, skipNotSearchable);
        assocsBlockBean.getDocAssocInfos().add(docAssocInfo);
    }

    private AssocsBlockBean getAssocsBlockBean() {
        AssocsBlockBean assocsBlockBean = (AssocsBlockBean) getBlocks().get(AssocsBlockBean.class);
        return assocsBlockBean;
    }

    /**
     * Called when a new (not yet saved) document is set to be a reply or a follow up to some base document
     * and is filled with some properties of the base document.
     *
     * @param nodeRef to the base document
     */
    private void updateFollowUpOrReplyProperties(NodeRef nodeRef) {
        BeanHelper.getDocumentDynamicService().setOwner(getDocument().getNodeRef(), AuthenticationUtil.getRunAsUser(), false);
        DocumentParentNodesVO parents = getDocumentService().getAncestorNodesByDocument(nodeRef);
        Map<String, Object> docProps = getNode().getProperties();
        docProps.put(DocumentCommonModel.Props.FUNCTION.toString(), parents.getFunctionNode().getNodeRef());
        NodeRef seriesRef = parents.getSeriesNode().getNodeRef();
        docProps.put(DocumentCommonModel.Props.SERIES.toString(), seriesRef);
        docProps.put(DocumentCommonModel.Props.VOLUME.toString(), parents.getVolumeNode().getNodeRef());
        Node caseNode = parents.getCaseNode();
        if (caseNode != null) {
            docProps.put(DocumentLocationGenerator.CASE_LABEL_EDITABLE.toString(), BeanHelper.getCaseService().getCaseByNoderef(caseNode.getNodeRef()).getTitle());
            docProps.put(DocumentCommonModel.Props.CASE.toString(), caseNode.getNodeRef());
        }
        updateAccessRestrictionProperties(docProps, seriesRef);
    }

    public void addNotification() {
        BeanHelper.getNotificationService().addNotificationAssocForCurrentUser(getNode().getNodeRef(), UserModel.Assocs.DOCUMENT_NOTIFICATION,
                UserModel.Aspects.DOCUMENT_NOTIFICATIONS);
    }

    public void removeNotification() {
        BeanHelper.getNotificationService().removeNotificationAssocForCurrentUser(getNode().getNodeRef(), UserModel.Assocs.DOCUMENT_NOTIFICATION,
                UserModel.Aspects.DOCUMENT_NOTIFICATIONS);
    }

    private void updateAccessRestrictionProperties(Map<String, Object> docProps, NodeRef seriesRef) {
        final String accessRestriction = (String) docProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString());
        if (StringUtils.isBlank(accessRestriction)) {
            // read serAccessRestriction-related values from series
            final Series series = BeanHelper.getSeriesService().getSeriesByNodeRef(seriesRef);
            final Map<String, Object> seriesProps = series.getNode().getProperties();
            final String serAccessRestriction = (String) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString());
            final String serAccessRestrictionReason = (String) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.toString());
            final Date serAccessRestrictionBeginDate = (Date) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString());
            final Date serAccessRestrictionEndDate = (Date) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.toString());
            final String serAccessRestrictionEndDesc = (String) seriesProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC.toString());
            // write them to the document
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION.toString(), serAccessRestriction);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON.toString(), serAccessRestrictionReason);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_BEGIN_DATE.toString(), serAccessRestrictionBeginDate);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DATE.toString(), serAccessRestrictionEndDate);
            docProps.put(DocumentCommonModel.Props.ACCESS_RESTRICTION_END_DESC.toString(), serAccessRestrictionEndDesc);
        }
    }

    /**
     * Move all the files to the selected nodeRef, delete the current doc
     * and show the nodeRef doc.
     *
     * @param event
     */
    public void addFilesHandler(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, PARAM_NODEREF));
        FileBlockBean fileBlockBean = (FileBlockBean) getBlocks().get(FileBlockBean.class);
        boolean success = fileBlockBean.moveAllFiles(nodeRef);
        if (success) {
            getDocumentService().deleteDocument(getNode().getNodeRef());
            open(nodeRef, false);
        }
    }

    private SearchBlockBean getSearchBlock() {
        return (SearchBlockBean) getBlocks().get(SearchBlockBean.class);
    }

    @Override
    public AbstractSearchBlockBean getSearch() {
        return (AbstractSearchBlockBean) getBlocks().get(SearchBlockBean.class);
    }

    @Override
    public LogBlockBean getLog() {
        return (LogBlockBean) getBlocks().get(LogBlockBean.class);
    }

    public void addFile(ActionEvent event) {
        BeanHelper.getAddFileDialog().start(event);
        WebUtil.navigateTo("dialog:addFile");
    }

    public void addInactiveFile(ActionEvent event) {
        BeanHelper.getAddFileDialog().startInactive(event);
        WebUtil.navigateTo("dialog:addInactiveFile");
    }

    public boolean isShowDocsAndCasesAssocs() {
        return getCurrentSnapshot().showDocsAndCasesAssocs;
    }

    public boolean isShowAddAssocsLink() {
        return BeanHelper.getDocumentDialogHelperBean().isInWorkspace() && !isInEditMode();
    }

    @Override
    public List<DialogButtonConfig> getAdditionalButtons() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) { // XXX Why is this method called when snapshot is null? (CL 189462)
            return null;
        }
        DocumentConfig config = snapshot.config;
        DocumentDynamic document = snapshot.document;
        WmNode node = document.getNode();

        if (config == null && node != null) {
            snapshot.config = BeanHelper.getDocumentConfigService().getConfig(node);
            config = snapshot.config;
        }

        List<DialogButtonConfig> buttons = new ArrayList<DialogButtonConfig>(1);
        DocumentType docType = (DocumentType) config.getDocType();
        if (snapshot.inEditMode && SystematicDocumentType.INCOMING_LETTER.isSameType(document.getDocumentTypeId())
                && docType.isRegistrationEnabled() && docType.isRegistrationOnDocFormEnabled()
                && RegisterDocumentEvaluator.isNotRegistered(node)) {
            if (getSearchBlock().isShowSimilarDocumentsBlock() || getShowSaveAndRegisterButton()) {
                buttons.add(new DialogButtonConfig("documentRegisterButton", null, "document_registerDoc_continue",
                        "#{DocumentDynamicDialog.saveAndRegisterContinue}", "false", null));
            } else {
                buttons.add(new DialogButtonConfig("documentRegisterButton", null, "document_registerDoc", "#{DocumentDynamicDialog.saveAndRegister}", "false", null));
            }
        }

        String path = node.getPath();
        if (path != null && (path.contains(DVK_RECEIVED) || path.contains(FORWARDED_DEC_DOCUMENTS))) {
            buttons.add(new DialogButtonConfig("forwardDecDocumentButton", null, "document_forward_dec_document", "#{DialogManager.bean.forwardDecDocuments}", "false", null));
        }
        return buttons;
    }

    public void forwardDecDocuments() {
        String result = BeanHelper.getForwardDecDocumentDialog().init();
        if (result != null) {
            WebUtil.navigateTo("dialog:forwardDecDocumentDialog", FacesContext.getCurrentInstance());
        }
    }

    /**
     * Should be called only when the document was received from DVK or Outlook.
     */
    public void selectedDocumentTypeChanged(ActionEvent event) {
        String newType = (String) ((UIInput) event.getComponent().findComponent("doc-types-select")).getValue();
        if (StringUtils.isBlank(newType)) {
            return;
        }
        DocumentDynamic document = getDocument();
        if (document.getDocumentTypeId().equals(newType)) {
            return;
        }
        BeanHelper.getDocumentDynamicService().changeTypeInMemory(document, newType);
        openOrSwitchModeCommon(document, true);
    }

    public List<SelectItem> getDocumentTypeListItems() {
        List<SelectItem> types = BeanHelper.getDocumentSearchBean().getDocumentTypes();
        String currentDocumentType = getDocument().getDocumentTypeId();
        if (StringUtils.isBlank(currentDocumentType)) {
            return types;
        }
        for (SelectItem item : types) {
            if (item.getValue().equals(currentDocumentType)) {
                return types;
            }
        }
        types.add(new SelectItem(currentDocumentType, getDocumentTypeName()));
        WebUtil.sort(types);
        return types;
    }

    /** Used in jsp */
    public String getOnChangeStyleClass() {
        return ComponentUtil.getOnChangeStyleClass();
    }

    // =========================================================================

    static class DocDialogSnapshot implements BaseSnapshotCapableDialog.Snapshot {
        private static final long serialVersionUID = 1L;

        private DocumentDynamic document;
        private boolean inEditMode;
        private boolean viewModeWasOpenedInThePast = false; // intended initial value
        private boolean showDocsAndCasesAssocs;
        private boolean saveAndRegister;
        private boolean saveAndRegisterContinue;
        private boolean showSaveAndRegisterContinueButton;
        private boolean confirmMoveAssociatedDocuments;
        private boolean moveAssociatedDocumentsConfirmed;
        private DocumentConfig config;
        private boolean needRegisteringAfterCreateCaseFile;
        private boolean needSendNotificationAfterCreateCaseFile;

        @Override
        public String getOpenDialogNavigationOutcome() {
            return AlfrescoNavigationHandler.DIALOG_PREFIX + "documentDynamicDialog";
        }

        public void setDocument(DocumentDynamic document) {
            this.document = document;
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(boolean detailed) {
            return "DocDialogSnapshot[document=" + (document == null ? null : (detailed ? document : document.getNodeRef())) + ", inEditMode=" + inEditMode
                    + ", viewModeWasOpenedInThePast=" + viewModeWasOpenedInThePast + ", config=" + config + ", showDocsAndCasesAssocs=" + showDocsAndCasesAssocs + "]";
        }
    }

    // =========================================================================

    // All dialog entry point methods must call this method
    private void open(NodeRef docRef, boolean inEditMode) {
        open(docRef, null, inEditMode, false);
    }

    private void open(NodeRef docRef, DocumentDynamic document, boolean inEditMode, boolean isDraft) {
        if (!validateOpen(docRef, inEditMode, isDraft)) {
            return;
        }
        createSnapshot(new DocDialogSnapshot());
        if (document != null) {
            openOrSwitchModeCommon(document, inEditMode);
        } else {
            openOrSwitchModeCommon(docRef, inEditMode);
        }
    }

    @Override
    public void switchMode(boolean inEditMode) {
        openOrSwitchModeCommon(getDocument().getNodeRef(), inEditMode);
    }

    private void openOrSwitchModeCommon(NodeRef docRef, boolean inEditMode) {
        DocumentDynamic document = inEditMode
                ? getDocumentDynamicService().getDocumentWithInMemoryChangesForEditing(docRef)
                : getDocumentDynamicService().getDocument(docRef);
        openOrSwitchModeCommon(document, inEditMode);
    }

    private void openOrSwitchModeCommon(DocumentDynamic document, boolean inEditMode) {
        NodeRef docRef = document.getNodeRef();
        DocDialogSnapshot currentSnapshot = getCurrentSnapshot();
        try {
            currentSnapshot.document = document;

            // Lock or unlock the node also
            getDocumentDialogHelperBean().reset(getDataProvider());
            DocumentLockHelperBean documentLockHelperBean = BeanHelper.getDocumentLockHelperBean();
            documentLockHelperBean.lockOrUnlockIfNeeded(documentLockHelperBean.isLockingAllowed(inEditMode));

            if (!currentSnapshot.inEditMode || (currentSnapshot.inEditMode && !inEditMode)) {
                // only reset registering process if mode is actually changed from edit mode to view mode
                setSaveAndRegister(false);
                setSaveAndRegisterContinue(false);
            }
            currentSnapshot.confirmMoveAssociatedDocuments = false;
            currentSnapshot.moveAssociatedDocumentsConfirmed = false;
            currentSnapshot.inEditMode = inEditMode;
            if (!inEditMode) {
                if (StringUtils.isBlank(document.getRegNumber()) && document.getRegDateTime() == null && BeanHelper.getDocumentDynamicService().isShowMessageIfUnregistered()) {
                    MessageUtil.addInfoMessage("document_info_not_registered");
                }
                currentSnapshot.viewModeWasOpenedInThePast = true;
                getLogService().addLogEntry(LogEntry.create(LogObject.DOCUMENT, getUserService(), docRef, "document_log_status_opened_not_inEditMode"));
            }
            currentSnapshot.config = BeanHelper.getDocumentConfigService().getConfig(getNode());
            if (document.isDraftOrImapOrDvk()) {
                getCurrentSnapshot().showDocsAndCasesAssocs = false;
            }
            resetOrInit(getDataProvider());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Document before rendering: " + getDocument());
            }
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_validation_alreadyLocked");
        } catch (UnableToPerformException e) {
            throw e;
        } catch (UnableToPerformMultiReasonException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to switch mode to " + (inEditMode ? "edit" : "view") + "\n  docRef=" + docRef + "\n  snapshot="
                    + (currentSnapshot == null ? null : currentSnapshot.toString(true)), e);
        }
    }

    // =========================================================================
    // Blocks
    // =========================================================================

    @Override
    protected Map<Class<? extends DocumentDynamicBlock>, DocumentDynamicBlock> getBlocks() {
        Map<Class<? extends DocumentDynamicBlock>, DocumentDynamicBlock> blocks = super.getBlocks();
        if (blocks.isEmpty()) {
            blocks = new HashMap<Class<? extends DocumentDynamicBlock>, DocumentDynamicBlock>();
            blocks.put(FileBlockBean.class, BeanHelper.getFileBlockBean());
            blocks.put(LogBlockBean.class, BeanHelper.getLogBlockBean());
            blocks.put(WorkflowBlockBean.class, BeanHelper.getWorkflowBlockBean());
            blocks.put(SendOutBlockBean.class, BeanHelper.getSendOutBlockBean());
            blocks.put(AssocsBlockBean.class, BeanHelper.getAssocsBlockBean());
            blocks.put(SearchBlockBean.class, BeanHelper.getSearchBlockBean());
        }
        return blocks;
    }

    // =========================================================================
    // Navigation with buttons - edit/save/back
    // =========================================================================

    /** @param event */
    public void switchToEditMode(ActionEvent event) {
        if (isInEditMode()) {
            throw new RuntimeException("Document metadata block is already in edit mode");
        }

        // Permission check
        NodeRef docRef = getDocument().getNodeRef();
        if (!validateExists(docRef) || !validateViewMetaDataPermission(docRef) || !validateEditMetaDataPermission(docRef)) {
            return;
        }

        // Switch from view mode to edit mode
        switchMode(true);
    }

    public void sendAccessRestrictionChangedEmails(ActionEvent event) {
        if (isCreateNewCaseFile()) {
            getCurrentSnapshot().needSendNotificationAfterCreateCaseFile = true;
            navigateCreateNewCaseFile(false);
        } else {
            DocumentDynamic document = getDocument();
            notifyAccessRestrictionChanged(document,
                    BeanHelper.getNotificationService().getExistingAndMissingEmails(BeanHelper.getSendOutService().getDocumentSendInfos(document.getNodeRef())));
            cancel();
        }
    }

    public void notifyAccessRestrictionChanged(DocumentDynamic document, Pair<List<String>, List<SendInfo>> existingAndMissingEmails) {
        BeanHelper.getNotificationService().processAccessRestrictionChangedNotification(document, existingAndMissingEmails.getFirst());

        if (!existingAndMissingEmails.getSecond().isEmpty()) {
            List<String> names = new ArrayList<String>(existingAndMissingEmails.getSecond().size());
            for (SendInfo sendInfo : existingAndMissingEmails.getSecond()) {
                names.add(sendInfo.getRecipient());
            }
            MessageUtil.addInfoMessage("docdyn_accessRestriction_missingEmails", StringUtils.join(names, ", "));
        }
    }

    public void dontSendAccessRestrictionChangedEmails(@SuppressWarnings("unused") ActionEvent event) {
        if (isCreateNewCaseFile()) {

        } else {
            cancel();
        }
    }

    public String cancel(@SuppressWarnings("unused") ActionEvent event) {
        return cancel();
    }

    @Override
    public String cancel() {
        if (getCurrentSnapshot() == null) {
            Throwable e = new Throwable("!!!!!!!!!!!!!!!!!!!!!!!!! Cancel is called too many times !!!!!!!!!!!!!!!!!!!!!!!!!");
            LOG.warn(e.getMessage(), e);
            return cancel(false);
        }
        boolean isInEditMode = isInEditMode();
        if (isInEditMode) {
            try {
                BeanHelper.getDocumentLockHelperBean().lockOrUnlockIfNeeded(false);
            } catch (UnableToPerformException e) {
                MessageUtil.addErrorMessage("document_deleted");
                return null;
            }
        }
        if (!isInEditMode || !getCurrentSnapshot().viewModeWasOpenedInThePast || !canRestore()) {
            getDocumentDynamicService().deleteDocumentIfDraft(getDocument().getNodeRef());
            cleanDyamicBlocks();
            return super.cancel(); // closeDialogSnapshot
        }
        // Switch from edit mode back to view mode
        switchMode(false);
        return null;
    }

    private boolean isIncorrectSeries() {
        DocumentDynamic doc = getDocument();
        String docType = doc.getDocumentTypeId();
        List<String> allowedTypes = (List) BeanHelper.getNodeService().getProperty(doc.getSeries(), SeriesModel.Props.DOC_TYPE);
        return !allowedTypes.contains(docType);
    }

    private boolean isNodeRefValid() {
        NodeRef docRef = getDocument().getNodeRef();
        return BeanHelper.getNodeService().exists(docRef);
    }

    @Override
    /** Transaction created by BaseDialogBean.finish method is not needed by us here, and it would be clearer to turn it off -
     * but we haven't turned it off and thus it is still created and therefore it is the super transaction.
     * Actions in finishImpl method are performed in two separate child transactions for the following reason:
     * integrity checker runs at the end of the transaction and we want integrity checker to run before switchMode is run.
     */
    protected String finishImpl(final FacesContext context, String outcome) throws Throwable {
        if (!isInEditMode()) {
            throw new RuntimeException("Document metadata block is not in edit mode");
        }
        if (!isNodeRefValid()) {
            MessageUtil.addErrorMessage("docdyn_deleted");
            return null;
        }
        if (BeanHelper.getDocumentLockHelperBean().isLockReleased(getDocument().getNodeRef())) {
            MessageUtil.addErrorMessage("lock_document_administrator_released");
            WebUtil.navigateTo("dialog:close");
            return null;
        }
        if (isIncorrectSeries()) {
            MessageUtil.addErrorMessage(context, "document_save_error_invalid_series");
            return null;
        }

        if (isSaveAndRegister()) {
            // select followup document before saving
            if (checkSimilarDocuments()) {
                setSaveAndRegister(false);
                isFinished = false;
                return null;
            }
        }
        final boolean relocateAssociations = isRelocatingAssociations();
        if (!getCurrentSnapshot().moveAssociatedDocumentsConfirmed && relocateAssociations) {
            BeanHelper.getUserConfirmHelper().setup(new MessageDataImpl("document_move_associated_documents_confirmation"), null,
                    "#{DocumentDynamicDialog.changeDocLocationConfirmed}", null, null, null, null);
            getCurrentSnapshot().confirmMoveAssociatedDocuments = true;
            isFinished = false;
            return null;
        }
        final DocumentDynamic savedDocument;
        final boolean isDraft = getDocument().isDraft();
        final boolean createNewCaseFile = isCreateNewCaseFile();
        try {
            if (!createNewCaseFile) {
                savedDocument = save(getDocument(), getConfig().getSaveListenerBeanNames(), getCurrentSnapshot().confirmMoveAssociatedDocuments, true).getFirst();
            }
            // when new case file is created, actual saving (and registering, if required) is postponed to after saving case file
            else {
                savedDocument = getDocument();
                savedDocument.setProp(TEMP_VALIDATE_WITHOUT_SAVE, Boolean.TRUE);
                getDocumentDynamicService().validateDocument(getConfig().getSaveListenerBeanNames(), savedDocument);
                savedDocument.getNode().getProperties().remove(TEMP_VALIDATE_WITHOUT_SAVE);
            }

        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("docdyn_createAssoc_error_docLocked", e.getNodeRef());
            isFinished = false;
            return null;
        } catch (UnableToPerformMultiReasonException e) {
            if (!handleAccessRestrictionChange(e)) {
                isFinished = false;
                return null;
            }

            // This is handled in BaseDialogBean
            throw e;
        }

        // Do in new transaction, because otherwise saved data from the new transaction above is not visible
        return getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<String>() {
            @Override
            public String execute() throws Throwable {
                if (isSaveAndRegister() || isSaveAndRegisterContinue()) {
                    getCurrentSnapshot().needRegisteringAfterCreateCaseFile = true;
                    setSaveAndRegister(false);
                    setSaveAndRegisterContinue(false);
                    if (!createNewCaseFile) {
                        register(isDraft);
                    }
                }
                if (savedDocument.isAccessRestrictionPropsChanged() && BeanHelper.getSendOutService().hasDocumentSendInfos(savedDocument.getNodeRef())) {
                    isFinished = false;
                    // modal has already been displayed, if displaying was necessary
                    renderedModal = null;
                    // confirmation popup shall be displayed
                    showConfirmationPopup = true;
                    return null;
                }
                if (!createNewCaseFile) {
                    NodeRef newRef = savedDocument.getNodeRef();
                    NodeRef oldRef = null;
                    DocDialogSnapshot snap = getCurrentSnapshot();
                    if (snap != null && snap.document != null) {
                        oldRef = snap.document.getNodeRef();
                    }
                    // Switch from edit mode back to view mode
                    if (!(newRef == null || oldRef == null) && !newRef.equals(oldRef)) {
                        openOrSwitchModeCommon(newRef, false);
                    } else {
                        switchMode(false);
                    }
                } else {
                    navigateCreateNewCaseFile(relocateAssociations);
                }
                return null;
            }

        }, false, true);
    }

    public Pair<DocumentDynamic, List<Pair<NodeRef, NodeRef>>> save(final DocumentDynamic document, final List<String> saveListenerBeanNames, final boolean relocateAssocDocs,
            boolean newTransaction) {
        Pair<DocumentDynamic, List<Pair<NodeRef, NodeRef>>> result;
        // Do in new transaction, because we want to catch integrity checker exceptions now, not at the end of this method when mode is already switched
        RetryingTransactionCallback<Pair<DocumentDynamic, List<Pair<NodeRef, NodeRef>>>> callback = new RetryingTransactionCallback<Pair<DocumentDynamic, List<Pair<NodeRef, NodeRef>>>>() {
            @Override
            public Pair<DocumentDynamic, List<Pair<NodeRef, NodeRef>>> execute() throws Throwable {
                // May throw UnableToPerformException or UnableToPerformMultiReasonException
                ((FileBlockBean) getBlocks().get(FileBlockBean.class)).updateFilesProperties();
                Pair<DocumentDynamic, List<Pair<NodeRef, NodeRef>>> saveResult = getDocumentDynamicService().updateDocumentGetDocAndNodeRefs(document, saveListenerBeanNames,
                        relocateAssocDocs, true);
                // Delete DecContainer after document saving. Just in case.
                getFileService().removeDecContainer(saveResult.getFirst().getNodeRef());

                return saveResult;
            }
        };
        if (newTransaction) {
            result = getTransactionService().getRetryingTransactionHelper().doInTransaction(callback, false, true);
        } else {
            try {
                result = callback.execute();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        for (Pair<NodeRef, NodeRef> pair : result.getSecond()) {
            if (pair.getFirst().equals(pair.getSecond())) {
                continue;
            }
            for (DocDialogSnapshot snapshot : getSnapshots()) {
                if (!pair.getFirst().equals(snapshot.document.getNodeRef())) {
                    continue;
                }
                snapshot.document.getNode().updateNodeRef(pair.getSecond());
            }
        }
        return result;
    }

    private void navigateCreateNewCaseFile(boolean isRelocatingAssociations) {
        String caseFileType = getDocument().getProp(DocumentLocationGenerator.CASE_FILE_TYPE_PROP);
        BeanHelper.getCaseFileDialog().createCaseFile(caseFileType, getDocument(), getCurrentSnapshot().needRegisteringAfterCreateCaseFile,
                getCurrentSnapshot().needSendNotificationAfterCreateCaseFile, getConfig().getSaveListenerBeanNames(), isRelocatingAssociations);
    }

    private boolean isCreateNewCaseFile() {
        NodeRef volumeRef = getDocument().getVolume();
        final boolean createNewCaseFile = volumeRef != null && RepoUtil.isUnsaved(volumeRef);
        return createNewCaseFile;
    }

    private boolean isRelocatingAssociations() {
        DocumentDynamic document = getDocument();

        List<DocAssocInfo> docAssocs = getAssocsBlockBean().getDocAssocInfos();
        if (isCreateNewCaseFile() && docAssocs != null && !docAssocs.isEmpty()) {
            return true;
        }
        if (!RepoUtil.isSaved(document.getVolume())) {
            return false;
        }

        NodeRef newParentRef = getParent(document.getVolume(), StringUtils.trimToNull((String) document.getProp(DocumentLocationGenerator.CASE_LABEL_EDITABLE)));
        // For documents not under volume or case, check location change against associated document
        // TODO : in 3.10 branch, add document.isFromWebService() check here
        if (document.isDraftOrImapOrDvk() || document.isIncomingInvoice()) {
            if (docAssocs == null || docAssocs.isEmpty()) {
                return false;
            }
            NodeRef associatedDocParentRef = null;
            for (DocAssocInfo docAssocInfo : docAssocs) {
                if (docAssocInfo.isFollowUpOrReplyAssoc()) {
                    associatedDocParentRef = getNodeService().getPrimaryParent(docAssocInfo.getOtherNodeRef()).getParentRef();
                    break;
                }
            }
            return associatedDocParentRef != null && !associatedDocParentRef.equals(newParentRef);
        }
        NodeRef currentParentRef = getNodeService().getPrimaryParent(document.getNodeRef()).getParentRef();
        if (currentParentRef.equals(newParentRef)) {
            return false;
        }
        return BeanHelper.getDocumentAssociationsService().isBaseOrReplyOrFollowUpDocument(getNode().getNodeRef(), null);
    }

    public static NodeRef getParent(NodeRef volumeRef, String caseLabel) {
        UnmodifiableVolume volume = BeanHelper.getVolumeService().getUnmodifiableVolume(volumeRef, null);
        NodeRef parentRef = null;
        if (volume.isContainsCases() && caseLabel != null) {
            UnmodifiableCase existingCase = BeanHelper.getCaseService().getCaseByTitle(caseLabel, volumeRef, null);
            parentRef = existingCase != null ? existingCase.getNodeRef() : null;
        } else {
            parentRef = volumeRef;
        }
        return parentRef;
    }

    public boolean isConfirmMoveAssociatedDocuments() {
        DocDialogSnapshot currentSnapshot = getCurrentSnapshot();
        if (currentSnapshot == null) { // XXX Why is this method called when snapshot is null? (CL 207223)
            return false;
        }
        return currentSnapshot.confirmMoveAssociatedDocuments;
    }

    private void setSaveAndRegister(boolean saveAndRegister) {
        getCurrentSnapshot().saveAndRegister = saveAndRegister;
    }

    private void setSaveAndRegisterContinue(boolean saveAndRegisterContinue) {
        getCurrentSnapshot().saveAndRegisterContinue = saveAndRegisterContinue;
        if (saveAndRegisterContinue) {
            setShowSaveAndRegisterButton(true);
        }
    }

    private boolean isSaveAndRegisterContinue() {
        return getCurrentSnapshot().saveAndRegisterContinue;
    }

    private boolean isSaveAndRegister() {
        return getCurrentSnapshot().saveAndRegister;
    }

    private boolean setShowSaveAndRegisterButton(boolean showSaveAndRegisterContinueButton) {
        return getCurrentSnapshot().showSaveAndRegisterContinueButton = showSaveAndRegisterContinueButton;
    }

    private boolean getShowSaveAndRegisterButton() {
        return getCurrentSnapshot().showSaveAndRegisterContinueButton;
    }

    private boolean checkSimilarDocuments() {
        SearchBlockBean searchBlock = getSearchBlock();
        // search for similar documents if it's an incoming letter
        if (SystematicDocumentType.INCOMING_LETTER.isSameType(getDocument().getDocumentTypeId())) {
            String senderRegNum = getDocument().getProp(DocumentSpecificModel.Props.SENDER_REG_NUMBER);
            searchBlock.findSimilarDocuments(senderRegNum);
        }
        if (searchBlock.isShowSimilarDocumentsBlock()) {
            return true;
        }
        return false;
    }

    public void saveAndRegisterContinue() {
        if (checkCanRegister()) {
            // similar documents were found before, finish registering
            setSaveAndRegister(false);
            setSaveAndRegisterContinue(true);
            super.finish();
            getSearchBlock().setShowSimilarDocumentsBlock(false);
        }
    }

    public void saveAndRegister() {
        if (checkCanRegister()) {
            setSaveAndRegister(true);
            setSaveAndRegisterContinue(false);
            super.finish();
        }
    }

    private boolean checkCanRegister() {
        WmNode node = getNode();
        try {
            BeanHelper.getDocumentLockHelperBean().checkAssocDocumentLocks(node, null);
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_registerDoc_error_docLocked_initialDocument", e);
            setSaveAndRegister(false);
            setSaveAndRegisterContinue(false);
            return false;
        }

        if (new DocumentNotInDraftsFunctionActionEvaluator().evaluate(node)) {
            return true;
        }
        setSaveAndRegister(false);
        setSaveAndRegisterContinue(false);
        MessageUtil.addErrorMessage("document_registerDoc_error_drafts_function");
        return false;
    }

    public void changeDocLocationConfirmed(ActionEvent event) {
        getCurrentSnapshot().moveAssociatedDocumentsConfirmed = true;
        super.finish();
    }

    private void register(boolean isDraft) {
        register(isDraft, getDocument(), getNode(), getDocumentDialogHelperBean().getNode());
    }

    public void register(boolean isDraft, DocumentDynamic documentDynamic, WmNode node, Node document) {
        documentDynamic.setProp(RepoUtil.createTransientProp(DocumentService.TransientProps.TEMP_DOCUMENT_IS_DRAFT), isDraft);
        EventsLoggingHelper.disableLogging(node, DocumentService.TransientProps.TEMP_LOGGING_DISABLED_DOCUMENT_METADATA_CHANGED);
        try {
            DocumentParentNodesVO parentNodes = getDocumentService().getAncestorNodesByDocument(document.getNodeRef());
            getDocumentService().setTransientProperties(document, parentNodes);
            NodeRef docRef = getDocumentService().registerDocument(document);
            // Update generated files
            BeanHelper.getDocumentTemplateService().updateGeneratedFiles(docRef, true);
            ((MenuBean) FacesHelper.getManagedBean(FacesContext.getCurrentInstance(), MenuBean.BEAN_NAME)).processTaskItems();
            MessageUtil.addInfoMessage("document_registerDoc_success");
        } catch (UnableToPerformException e) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("failed to register: " + e.getMessage());
            }
            MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_registerDoc_error_docLocked", e);
        }
    }

    private boolean handleAccessRestrictionChange(UnableToPerformMultiReasonException e) {
        final MessageDataWrapper messageDataWrapper = e.getMessageDataWrapper();
        if (messageDataWrapper.getFeedbackItemCount() == 1 && ACCESS_RESTRICTION_CHANGE_REASON_ERROR.equals(messageDataWrapper.iterator().next().getMessageKey())) {
            isFinished = false;
            renderedModal = ChangeReasonModalComponent.ACCESS_RESTRICTION_CHANGE_REASON_MODAL_ID;
            return false;
        }

        // Remove the error if there are other errors
        for (Iterator<MessageData> iterator = messageDataWrapper.iterator(); iterator.hasNext();) {
            final MessageData data = iterator.next();
            if (ACCESS_RESTRICTION_CHANGE_REASON_ERROR.equals(data.getMessageKey())) {
                iterator.remove();
                return true;
            }
        }

        return true;
    }

    public void showDocumentLink(ActionEvent event) {
        renderedModal = DocumentLinkGeneratorModalComponent.DOCUMENT_LINK_MODAL_ID;
    }

    public void setAccessRestrictionChangeReason(ActionEvent event) {
        getDocument().setProp(TEMP_ACCESS_RESTRICTION_CHANGE_REASON, ((ChangeReasonEvent) event).getReason());
        finish();
    }

    public String getFetchAndResetRenderedModal() {
        try {
            return renderedModal;
        } finally {
            renderedModal = null;
        }
    }

    public boolean isModalRendered() {
        return StringUtils.isNotBlank(renderedModal);
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return isInEditMode();
    }

    public boolean isShowSearchBlock() {
        SearchBlockBean searchBlockBean = (SearchBlockBean) getBlocks().get(SearchBlockBean.class);
        if ((searchBlockBean.isExpanded() && !getCurrentSnapshot().inEditMode)) {
            return true;
        }
        return getCurrentSnapshot().inEditMode && searchBlockBean.isShow() && !searchBlockBean.isShowSimilarDocumentsBlock()
                && ((getDocument().isImapOrDvk() && !getDocument().isNotEditable())
                || BeanHelper.getDocumentDynamicService().isInForwardedDecDocuments(getCurrentSnapshot().document.getNodeRef()));
    }

    public boolean isAssocsBlockExpanded() {
        return isShowSearchBlock();
    }

    public boolean isShowTypeBlock() {
        return !getCurrentSnapshot().document.isDraft() && getCurrentSnapshot().inEditMode
                && DocumentStatus.WORKING.getValueName().equals(getCurrentSnapshot().document.getDocStatus())
                && validateViewMetaDataPermission(getCurrentSnapshot().document.getNodeRef())
                || BeanHelper.getDocumentDynamicService().isInForwardedDecDocuments(getCurrentSnapshot().document.getNodeRef());

    }

    public boolean isShowFoundSimilar() {
        SearchBlockBean searchBlockBean = (SearchBlockBean) getBlocks().get(SearchBlockBean.class);
        return getCurrentSnapshot().inEditMode && searchBlockBean.isShowSimilarDocumentsBlock();
    }

    // =========================================================================
    // Action handlers
    // =========================================================================

    public void addDocumentToFavorites(ActionEvent event) {
        if (!(event instanceof AddToFavoritesEvent)) {
            renderedModal = FavoritesModalComponent.ADD_TO_FAVORITES_MODAL_ID;
            return;
        }
        if (event instanceof AddToFavoritesEvent) {
            BeanHelper.getDocumentFavoritesService().addFavorite(getNode().getNodeRef(), ((AddToFavoritesEvent) event).getFavoriteDirectoryName(), true);
        }
    }

    public void deleteDocument(ActionEvent event) {
        Node node = getDocumentDialogHelperBean().getNode();
        Assert.notNull(node, "No current document");
        try {
            // Before asking for reason let's check if it is locked
            BeanHelper.getDocLockService().checkForLock(node.getNodeRef());
            if (!(event instanceof ChangeReasonEvent) || StringUtils.isBlank(((ChangeReasonEvent) event).getReason())) {
                renderedModal = DELETE_DOCUMENT_REASON_MODAL_ID;
                return;
            }

            if (!new DeleteDocumentEvaluator().evaluate(node)) {
                throw new UnableToPerformException("action_failed_missingPermission", new MessageDataImpl("permission_deleteDocumentMetaData"));
            }
            getDocumentService().deleteDocument(node.getNodeRef(), ((ChangeReasonEvent) event).getReason());
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            return;
        } catch (AccessDeniedException e) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "document_delete_error_accessDenied");
            return;
        } catch (NodeLockedException e) {
            BeanHelper.getDocumentLockHelperBean().handleLockedNode("document_delete_error_docLocked");
            return;
        } catch (InvalidNodeRefException e) {
            final FacesContext context = FacesContext.getCurrentInstance();
            MessageUtil.addErrorMessage(context, "document_delete_error_docDeleted");
            WebUtil.navigateTo(getDefaultCancelOutcome(), context);
            return;
        }
        // go back
        WebUtil.navigateWithCancel();
        MessageUtil.addInfoMessage("document_delete_success");
    }

    // =========================================================================
    // Permission checks on open or switch mode
    // =========================================================================

    public static boolean validateOpen(NodeRef docRef, boolean inEditMode, boolean isDraft) {
        boolean exists = validateExists(docRef);
        if (exists && isDraft) {
            return true;
        }
        if (!exists || !validateViewMetaDataPermission(docRef) || (inEditMode && !validateEditMetaDataPermission(docRef))
                || (inEditMode && !getDocumentLockHelperBean().isLockable(docRef))) {
            return false;
        }
        return true;
    }

    public static boolean validateExists(NodeRef docRef) {
        Assert.notNull(docRef, "docRef is not given");
        if (!BeanHelper.getNodeService().exists(docRef)) {
            MessageUtil.addErrorMessage("document_restore_error_docDeleted");
            return false;
        }
        return true;
    }

    private static boolean validateViewMetaDataPermission(NodeRef docRef) {
        return validatePermissionWithErrorMessage(docRef, Privilege.VIEW_DOCUMENT_META_DATA);
    }

    private static boolean validateEditMetaDataPermission(NodeRef docRef) {
        return validatePermissionWithErrorMessage(docRef, Privilege.EDIT_DOCUMENT);
    }

    public static boolean validatePermissionWithErrorMessage(NodeRef docRef, Privilege permission) {
        try {
            validatePermission(docRef, permission);
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            return false;
        }
        return true;
    }

    @Override
    public void clean() {
        clearState();
        modalContainer = null;
        cleanDyamicBlocks();
    }

    private void cleanDyamicBlocks() {
        for (DocumentDynamicBlock documentDynamicBlock : getBlocks().values()) {
            documentDynamicBlock.clean();
        }
    }

    // =========================================================================
    // Getters
    // =========================================================================

    @Override
    public DocumentDynamic getDocument() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return null;
        }
        return snapshot.document;
    }

    public DocumentType getDocumentType() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return null;
        }
        return (DocumentType) snapshot.config.getDocType();
    }

    public DocumentTypeVersion getDocumentVersion() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return null;
        }
        return snapshot.config.getDocVersion();
    }

    @Override
    public WmNode getNode() {
        DocumentDynamic document = getDocument();
        if (document == null) {
            return null;
        }
        return document.getNode();
    }

    @Override
    public WmNode getActionsContext() {
        return getNode();
    }

    private DocumentConfig getConfig() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return null;
        }
        return snapshot.config;
    }

    public PropertySheetConfigElement getPropertySheetConfigElement() {
        DocumentConfig config = getConfig();
        if (config == null) {
            return null;
        }
        return config.getPropertySheetConfigElement();
    }

    private Map<String, PropertySheetStateHolder> getStateHolders() {
        DocumentConfig config = getConfig();
        if (config == null) {
            return null;
        }
        return config.getStateHolders();
    }

    // Metadata block

    @Override
    public boolean isInEditMode() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return false;
        }
        return snapshot.inEditMode;
    }

    public String getMode() {
        return isInEditMode() ? UIPropertySheet.EDIT_MODE : UIPropertySheet.VIEW_MODE;
    }

    @Override
    public String getContainerTitle() {
        return getDocumentTypeName();
    }

    private String getDocumentTypeName() {
        DocumentConfig config = getConfig();
        if (config == null) {
            return null;
        }
        return config.getDocumentTypeName();
    }

    @Override
    public String getMoreActionsConfigId() {
        return "";
    }

    // =========================================================================
    // Components
    // =========================================================================

    private transient WeakReference<UIPropertySheet> propertySheet;

    @Override
    public UIPropertySheet getPropertySheet() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("getPropertySheet propertySheet=" + ObjectUtils.toString(propertySheet));
        }
        // Additional checks are no longer needed, because ExternalAccessServlet behavior with JSF is now correct
        return getReferenceOrNull(propertySheet);
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("setPropertySheet propertySheet=" + ObjectUtils.toString(propertySheet));
        }
        // Additional checks are no longer needed, because ExternalAccessServlet behavior with JSF is now correct
        this.propertySheet = new WeakReference<>(propertySheet);
    }

    @Override
    protected void resetOrInit(DialogDataProvider provider) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("resetOrInit propertySheet=" + ObjectUtils.toString(propertySheet));
        }
        WmNode node = getNode();
        UIPropertySheet propertySheetComponent = getReferenceOrNull(propertySheet);
        if (propertySheetComponent != null) {
            propertySheetComponent.getChildren().clear();
            propertySheetComponent.getClientValidations().clear();
            propertySheetComponent.setNode(node);
            propertySheetComponent.setMode(getMode());
            propertySheetComponent.setConfig(getPropertySheetConfigElement());
        }
        if (node != null) {
            BeanHelper.getVisitedDocumentsBean().getVisitedDocuments().add(node.getNodeRef());
        }
        getPropertySheetStateBean().reset(getStateHolders(), provider);
        getDocumentDialogHelperBean().reset(provider);
        resetModals();
        setShowSaveAndRegisterButton(false);
        super.resetOrInit(provider); // reset blocks
    }

    @Override
    public boolean canRestore() {
        WmNode node = getNode();
        return node == null || getNodeService().exists(node.getNodeRef());
    }

    private void resetModals() {
        renderedModal = null;
        showConfirmationPopup = false;
        // Add favorite modal component
        FavoritesModalComponent favoritesModal = new FavoritesModalComponent();
        final FacesContext context = FacesContext.getCurrentInstance();
        final Application application = context.getApplication();
        favoritesModal.setActionListener(application.createMethodBinding("#{DocumentDynamicDialog.addDocumentToFavorites}", UIActions.ACTION_CLASS_ARGS));
        favoritesModal.setId("favorite-popup-" + context.getViewRoot().createUniqueId());

        // Access restriction change reason
        ChangeReasonModalComponent accessRestrictionChangeReasonModal = new ChangeReasonModalComponent(ACCESS_RESTRICTION_CHANGE_REASON_MODAL_ID
                , "docdyn_accesRestrictionChangeReason_modal_header", "docdyn_accesRestrictionChangeReason");
        accessRestrictionChangeReasonModal.setActionListener(application.createMethodBinding("#{DocumentDynamicDialog.setAccessRestrictionChangeReason}",
                UIActions.ACTION_CLASS_ARGS));
        accessRestrictionChangeReasonModal.setId("access-restriction-change-reason-popup-" + context.getViewRoot().createUniqueId());

        ChangeReasonModalComponent docDeleteReasonModal = new ChangeReasonModalComponent(DELETE_DOCUMENT_REASON_MODAL_ID
                , "docdyn_deleteDocumentReason_modal_header", "docdyn_deleteDocumentReason");
        docDeleteReasonModal.setActionListener(application.createMethodBinding("#{DocumentDynamicDialog.deleteDocument}", UIActions.ACTION_CLASS_ARGS));
        docDeleteReasonModal.setFinishButtonLabelId("delete");
        docDeleteReasonModal.setId("document-delete-reason-popup-" + context.getViewRoot().createUniqueId());

        DocumentLinkGeneratorModalComponent linkModal = new DocumentLinkGeneratorModalComponent();
        linkModal.setId("document-link-modal-" + context.getViewRoot().createUniqueId());

        List<UIComponent> children = ComponentUtil.getChildren(getModalContainer());
        children.clear();
        children.add(favoritesModal);
        children.add(accessRestrictionChangeReasonModal);
        children.add(docDeleteReasonModal);
        children.add(linkModal);
    }

    public boolean isShowConfirmationPopup() {
        return showConfirmationPopup;
    }

    public String getAccessRestrictionChangedMsg() {
        return MessageUtil.getMessage("document_access_restriction_changed_confirmation");
    }

    @Override
    protected DialogDataProvider getDataProvider() {
        return getCurrentSnapshot() == null ? null : this;
    }

    private transient WeakReference<UIPanel> modalContainer;

    public UIPanel getModalContainer() {
        UIPanel panel = modalContainer != null ? modalContainer.get() : null;
        if (panel == null) {
            panel = new UIPanel();
            modalContainer = new WeakReference(panel);
        }
        return panel;
    }

    public void setModalContainer(UIPanel modalContainer) {
        this.modalContainer = new WeakReference(modalContainer);
    }

    // =========================================================================
    // Child-node logic
    // =========================================================================

    public void addChildNode(ActionEvent event) {
        final Node parentNode = getParentNode(event);
        final QName[] childAssocTypeQNameHierarchy = getChildAssocTypeQNameHierarchy(event);

        getDocumentDynamicService().createChildNodesHierarchyAndSetDefaultPropertyValues(parentNode, childAssocTypeQNameHierarchy, getDocumentVersion());
    }

    public void removeChildNode(ActionEvent event) {
        final Node parentNode = getParentNode(event);
        final QName[] childAssocTypeQNameHierarchy = getChildAssocTypeQNameHierarchy(event);
        final QName assocTypeQName = childAssocTypeQNameHierarchy[childAssocTypeQNameHierarchy.length - 1];

        final String assocIndexParam = ActionUtil.getParam(event, SubPropertySheetItem.PARAM_ASSOC_INDEX);
        final int assocIndex = Integer.parseInt(assocIndexParam);

        parentNode.removeChildAssociations(assocTypeQName, assocIndex);
    }

    private Node getParentNode(ActionEvent event) {
        final SubPropertySheetItem propSheet = ComponentUtil.getAncestorComponent(event.getComponent(), SubPropertySheetItem.class);
        return propSheet.getParentPropSheetNode();
    }

    private QName[] getChildAssocTypeQNameHierarchy(ActionEvent event) {
        final String[] childAssocTypeQNameHierarchyParam = StringUtils.split(ActionUtil.getParam(event, "childAssocTypeQNameHierarchy"), '/');
        final QName[] childAssocTypeQNameHierarchy = new QName[childAssocTypeQNameHierarchyParam.length];
        for (int i = 0; i < childAssocTypeQNameHierarchyParam.length; i++) {
            childAssocTypeQNameHierarchy[i] = QName.createQName(childAssocTypeQNameHierarchyParam[i], getNamespaceService());
        }
        return childAssocTypeQNameHierarchy;
    }

    @Override
    public CaseFile getCaseFile() {
        // Not used.
        return null;
    }

}
