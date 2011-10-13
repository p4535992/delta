package ee.webmedia.alfresco.docdynamic.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDialogHelperBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;

import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.ObjectUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.web.DocumentDynamicDialog.DocDialogSnapshot;
import ee.webmedia.alfresco.document.file.web.FileBlockBean;
import ee.webmedia.alfresco.document.log.web.LogBlockBean;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.sendout.web.SendOutBlockBean;
import ee.webmedia.alfresco.simdhs.servlet.ExternalAccessServlet;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
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
 * 
 * @author Alar Kvell
 */
public class DocumentDynamicDialog extends BaseSnapshotCapableWithBlocksDialog<DocDialogSnapshot, DocumentDynamicBlock, DialogDataProvider> implements DialogDataProvider {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DocumentDynamicDialog.class);

    public static final String BEAN_NAME = "DocumentDynamicDialog";

    // Closing this dialog has the following logic:
    // ... view -> *back -> close
    // ... edit -> *back -> kui on tuldud view'ist, siis sinna tagasi, muidu close; ja kui draft, siis lisaks delete
    // URL -> openView -> *back -> close -> siis kuvatakse avaleht, sest URList avamine teeb viewstack'i tühjaks ja paneb avalehe esimeseks

    // =========================================================================
    // Dialog entry points start
    // 1 - ACTIONLISTENER methods
    // =========================================================================

    public void openFromUrl(NodeRef docRef) {
        LOG.info("openFromUrl");
        open(docRef, false);
    }

    /** @param event */
    public void openFromDocumentList(ActionEvent event) {
        LOG.info("openFromDocumentList");
        NodeRef docRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        // TODO if (isFromDVK() || isFromImap() || isIncomingInvoice()) { inEditMode = true; } else { inEditMode = false; }
        open(docRef, false);
    }

    /** @param event */
    public void openView(ActionEvent event) {
        LOG.info("openView");
        NodeRef docRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        open(docRef, false);
    }

    /** @param event */
    public void openEdit(ActionEvent event) {
        LOG.info("openView");
        NodeRef docRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        open(docRef, true);
    }

    /** @param event */
    public void createDraft(ActionEvent event) {
        LOG.info("createDraft");
        String documentTypeId = ActionUtil.getParam(event, "documentTypeId");
        NodeRef docRef = getDocumentDynamicService().createNewDocumentInDrafts(documentTypeId);
        open(docRef, true);
    }

    // =========================================================================

    static class DocDialogSnapshot implements BaseSnapshotCapableDialog.Snapshot {
        private static final long serialVersionUID = 1L;

        private DocumentDynamic document;
        private boolean inEditMode;
        private boolean viewModeWasOpenedInThePast = false; // intended initial value
        private DocumentConfig config;

        @Override
        public String getOpenDialogNavigationOutcome() {
            return AlfrescoNavigationHandler.DIALOG_PREFIX + "documentDynamicDialog";
        }

        @Override
        public String toString() {
            return "DocDialogSnapshot[document=" + (document == null ? null : document.getNodeRef()) + ", inEditMode=" + inEditMode + ", viewModeWasOpenedInThePast="
                    + viewModeWasOpenedInThePast + ", config=" + config + "]";
        }
    }

    // =========================================================================

    // All dialog entry point methods must call this method
    private void open(NodeRef docRef, boolean inEditMode) {
        if (!validateOpen(docRef, inEditMode)) {
            return;
        }
        createSnapshot(new DocDialogSnapshot());
        openOrSwitchModeCommon(docRef, inEditMode);
    }

    @Override
    public void switchMode(boolean inEditMode) {
        openOrSwitchModeCommon(getDocument().getNodeRef(), inEditMode);
    }

    private void openOrSwitchModeCommon(NodeRef docRef, boolean inEditMode) {
        DocDialogSnapshot currentSnapshot = getCurrentSnapshot();
        currentSnapshot.document = getDocumentDynamicService().getDocument(docRef);
        currentSnapshot.inEditMode = inEditMode;
        if (!inEditMode) {
            currentSnapshot.viewModeWasOpenedInThePast = true;
        }
        currentSnapshot.config = BeanHelper.getDocumentConfigService().getConfig(getNode());
        resetOrInit(getDataProvider());
        LOG.info("document before rendering: " + getDocument());
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
        }
        return blocks;
    }

    // =========================================================================
    // Navigation with buttons - edit/save/back
    // =========================================================================

    /** @param event */
    public void switchToEditMode(ActionEvent event) {
        LOG.info("switchToEditMode");
        if (isInEditMode()) {
            throw new RuntimeException("Document metadata block is already in edit mode");
        }

        // Permission check
        if (!validateEditMetaDataPermission(getDocument().getNodeRef())) {
            return;
        }

        // Switch from view mode to edit mode
        switchMode(true);
    }

    @Override
    public String cancel() {
        LOG.info("cancel");

        if (getCurrentSnapshot() == null) {
            Throwable e = new Throwable("!!!!!!!!!!!!!!!!!!!!!!!!! Cancel is called too many times !!!!!!!!!!!!!!!!!!!!!!!!!");
            LOG.warn(e.getMessage(), e);
            return cancel(false);
        }

        if (!isInEditMode() || !getCurrentSnapshot().viewModeWasOpenedInThePast) {
            getDocumentDynamicService().deleteDocumentIfDraft(getDocument().getNodeRef());
            return super.cancel(); // closeDialogSnapshot
        }

        // Switch from edit mode back to view mode
        switchMode(false);
        return null;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        LOG.info("finishImpl");
        if (!isInEditMode()) {
            throw new RuntimeException("Document metadata block is not in edit mode");
        }

        // May throw UnableToPerformException or UnableToPerformMultiReasonException; these are handled in BaseDialogBean
        getDocumentDynamicService().updateDocument(getDocument(), getConfig().getSaveListenerBeanNames());

        // Switch from edit mode back to view mode
        switchMode(false);
        return null;
    }

    @Override
    public boolean isFinishButtonVisible(boolean dialogConfOKButtonVisible) {
        return isInEditMode();
    }

    // =========================================================================
    // Permission checks on open or switch mode
    // =========================================================================

    private static boolean validateOpen(NodeRef docRef, boolean inEditMode) {
        if (!validateExists(docRef) || !validateViewMetaDataPermission(docRef) || (inEditMode && !validateEditMetaDataPermission(docRef))) {
            return false;
        }
        return true;
    }

    private static boolean validateExists(NodeRef docRef) {
        if (!BeanHelper.getNodeService().exists(docRef)) {
            MessageUtil.addErrorMessage("document_restore_error_docDeleted");
            return false;
        }
        return true;
    }

    private static boolean validateViewMetaDataPermission(NodeRef docRef) {
        return validatePermissionWithErrorMessage(docRef, DocumentCommonModel.Privileges.VIEW_DOCUMENT_META_DATA);
    }

    private static boolean validateEditMetaDataPermission(NodeRef docRef) {
        return validatePermissionWithErrorMessage(docRef, DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA);
    }

    private static boolean validatePermissionWithErrorMessage(NodeRef docRef, String permission) {
        try {
            validatePermission(docRef, permission);
        } catch (UnableToPerformException e) {
            MessageUtil.addStatusMessage(e);
            return false;
        }
        return true;
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /** For JSP */
    public DocumentDynamic getDocument() {
        DocDialogSnapshot snapshot = getCurrentSnapshot();
        if (snapshot == null) {
            return null;
        }
        return snapshot.document;
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
    public Object getActionsContext() {
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

    @Override
    public <E extends PropertySheetStateHolder> E getStateHolder(String key, Class<E> clazz) {
        Map<String, PropertySheetStateHolder> stateHolders = getStateHolders();
        if (stateHolders == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        E stateHolder = (E) stateHolders.get(key);
        return stateHolder;
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

    private transient UIPropertySheet propertySheet;

    @Override
    public UIPropertySheet getPropertySheet() {
        LOG.info("getPropertySheet propertySheet=" + ObjectUtils.toString(propertySheet));
        // Additional checks are no longer needed, because ExternalAccessServlet beahviour with JSF is now correct
        return propertySheet;
    }

    public void setPropertySheet(UIPropertySheet propertySheet) {
        LOG.info("setPropertySheet propertySheet=" + ObjectUtils.toString(propertySheet));
        // Additional checks are no longer needed, because ExternalAccessServlet beahviour with JSF is now correct
        this.propertySheet = propertySheet;
    }

    @Override
    protected void resetOrInit(DialogDataProvider provider) {
        // TODO call clear on all other blocks and components!!!

        LOG.info("clearPropertySheet propertySheet=" + ObjectUtils.toString(propertySheet));
        if (propertySheet != null) {
            propertySheet.getChildren().clear();
            propertySheet.getClientValidations().clear();
            propertySheet.setNode(getNode());
            propertySheet.setMode(getMode());
            propertySheet.setConfig(getPropertySheetConfigElement());
        }
        getPropertySheetStateBean().reset(getStateHolders(), provider);
        getDocumentDialogHelperBean().reset(provider);
        super.resetOrInit(provider); // reset blocks
    }

    @Override
    protected DialogDataProvider getDataProvider() {
        return getCurrentSnapshot() == null ? null : this;
    }

    /** @param event */
    public void doNothing(ActionEvent event) {
        LOG.info("doNothing");
    }

    // Põhisuunad
    // * tuua juurde ülejäänud blokid ja tegevused
    // * täiendada metaandmetevälju niikaugele, et väljatüüpide lisamine muutuks iseseisvaks
    // ++ kõige olulisem on fn/sari/toimik/asi väljad, siis saaks dok.loetelu alla salvestada
    // ?? dok.staatus, pealkiri väljad.... - common aspekti alt kuidas üksikuid süsteemseid välju kasutan?
    // * metaandmete valideerimine

    // TODO javascript valideerimine propsheetil katki?
    // TODO lemmiku tegevus katki? kas foorumi tegevused töötavad?

    // TODO kas on võimalik viia dictionaryservice'isse sisse docdyn propdef'ide tagastamine, nii et olulsied kohad neid kasutaks?
    // TODO kontaktide jms asjad: reposse list, tagasi string bugi; default väärtus sisselogitud kasutaja

    // TODO tuua põhiliste süsteemsete väljade ja gruppide täiendav funktsionaalsus üle !!!
    // TODO teiste süsteemsete doccom/docspec väljade kasutusele võtmisel: vaadata et vajalikud aspektid documentDynamic node'ile külge saaks

    // TODO kontrollida et kustutatud dokumendi ekraanile tagasipöördumine töötaks... või tahavad teised blokid laadida uuesti asju? ja siis oleks mõtekam dialoogi mitte kuvada?

    // TODO veateadete näitamised
    // TODO hiljem - dokumendi otsing
    // TODO hiljem - metaandmete lukustamine, üldisem lahendus -- arvestada kaarli kommentaariga, et cancelit ei pruugita alati välja kutsuda
    // TODO kõige hiljem - vanade dok.liikide ja andmete ülekandmine

    // dokumendile logikirje lisamiseks teha mudelisse meetod ja siis see lisab alfrescotransactionsupport kontrolli et üldse oleks transaktsioon lahti ja et transaktsiooni lõpus
    // oleks salvestamist tehtud

    // TODO dokumendi originaalprop'id võiks saada mudeli objekti käest?

    // TODO üle vaadata et kõik realiseeritud asjad ka spekis oleksid :)

    // Uutele paigaldustele imporditakse sellised süsteemsed dok.liigid mis spekis kirjeldatud
    // Olemasolevatele paigaldustele (SiM, SMIT, ViljMV) igaühele imporditakse neil kasutusel olevate dok.liikide ülekantud variandid dün'iks, mis on visuaalselt 100% identsed, aga
    // väljad on juba ümberstruktureeritud (nt kõik xxDueDate viidud ühe dueDate peale)
    // Vanade dokkide ülekandmise skripti käigus oleks mõistlik kirjutada uus dok.loetelu juba eraldi store'i!!!

    /*
     * Rakenduses lingi/nupu kaudu dialoogi avamine ESIMEST KORDA
     * 1) actionListener (open)
     * 2) init
     * 3) getPropertySheet -> null
     * 4) setPropertySheet -- JSF'i poolt nullist loodud ja puhas, OK
     * .
     * Rakenduses lingi/nupu kaudu dialoogi avamine järgmised korrad:
     * 1) setPropertySheet -- vana seisuga propertysheet; peab clearima
     * 2) actionListener (open)
     * 3) init
     * 4) getPropertySheet
     * .
     * Rakenduses URL'i kaudu dialoogi avamine ESIMEST KORDA - ENNE ÜMBERTEGEMIST
     * 1) ExternalAccessServlet -> openFromUrl
     * 2) init
     * 3) getPropertySheet -> null
     * 4) setPropertySheet -- JSF'i poolt nullist loodud ja puhas
     * .
     * Rakenduses URL'i kaudu dialoogi avamine ESIMEST KORDA - ENNE ÜMBERTEGEMIST
     * 1) actionListener (openFromUrl)
     * 2) init
     * 3) getPropertySheet -> null
     * 4) setPropertySheet -- JSF'i poolt nullist loodud ja puhas, OK
     * .
     * Rakenduses URL'i kaudu dialoogi avamine järgmised korrad -- ENNE ÜMBERTEGEMIST
     * 1) ExternalAccessServlet -> openFromUrl
     * 2) navigationHandler.handleNavigation --> init
     * 3) getRequestDispatcher.forward -> execute phase RESTORE_VIEW -> setPropertySheet -- vana seisuga propertysheet; peab clearima
     * 4) render
     * viimasel juhul on setPropertySheet liiga hilja ja tuleb mingi propsheet millel vanad andmed ja meil ei õnnestu seda clearida
     * .
     * Rakenduses URL'i kaudu dialoogi avamine järgmised korrad -- PÄRAST ÜMBERTEGEMIST
     * 1) setPropertySheet -- vana seisuga propertysheet; peab clearima
     * 2) actionListener (openFromUrl)
     * 3) init
     * 4) getPropertySheet
     * .
     * Klikk mingil teisel nupul, nii et jäädakse samale dialoogile:
     * 1) setPropertySheet -- vana seisuga propertysheet; kuna dialoogi seis jäi samaks, siis OK
     * .
     * Kas me tahame setPropertySheet puhul alati clearida? oleks ohutu; samas võtab see natuke rohkem aega, sest mingite väljade ehitamisel vist käiakse ka baasis
     */

}
