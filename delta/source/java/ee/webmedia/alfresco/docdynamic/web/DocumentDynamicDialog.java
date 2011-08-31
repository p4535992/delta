package ee.webmedia.alfresco.docdynamic.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getClearStateNotificationHandler;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentDynamicTestDialog;
import static ee.webmedia.alfresco.common.web.BeanHelper.getPropertySheetStateBean;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.config.PropertySheetConfigElement;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.apache.commons.lang.ObjectUtils;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.ClearStateNotificationHandler.ClearStateListener;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docconfig.generator.DialogDataProvider;
import ee.webmedia.alfresco.docconfig.generator.PropertySheetStateHolder;
import ee.webmedia.alfresco.docconfig.service.DocumentConfig;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.simdhs.servlet.ExternalAccessServlet;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * To open this dialog you must:
 * <ol>
 * <li>Call exactly one of the methods in actionListener section, either manually or from {@code <a:actionLink actionListener="..."}</li>
 * <li>To determine the navigation outcome (if this dialog should be opened or you need to stay where you are) you must call {@link #action()} method, either manually or from
 * {@code <a:actionLink action="..."}</li>
 * </ol>
 * <p>
 * For example, to open this dialog from JSP, you should use
 * <code>&lt;a:actionLink actionListener="#{DocumentDynamicDialog.open...}" action="#{DocumentDynamicDialog.action}"&gt;&lt;f:param name="nodeRef" value="..." /&gt;&lt;/a:actionLink&gt;</code>
 * </p>
 * <p>
 * For example, to open this dialog manually, (for example {@link ExternalAccessServlet} does this), you should first call {@code documentDynamicDialog.open...(nodeRef)} and then
 * {@code facesContext.getApplication().getNavigationHandler().handleNavigation(facesContext, null, documentDynamicDialog.action())}
 * </p>
 * 
 * @author Alar Kvell
 */
public class DocumentDynamicDialog extends BaseDialogBean implements ClearStateListener, DialogDataProvider {
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
        NodeRef docRef = getDocumentDynamicService().createDraft(documentTypeId);
        open(docRef, true);

        getDocumentDynamicTestDialog().restored();
    }

    // =========================================================================
    // 2 - ACTION method
    // =========================================================================

    private boolean allow;

    public String action() {
        return allow ? AlfrescoNavigationHandler.DIALOG_PREFIX + "documentDynamicDialog" : null;
    }

    // =========================================================================
    // =========================================================================

    private static class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        private DocumentDynamic document;
        private boolean inEditMode;
        private boolean viewModeWasOpenedInThePast = false; // intended initial value
        private DocumentConfig config;

        @Override
        public String toString() {
            return "Snapshot[document=" + (document == null ? null : document.getNodeRef()) + ", inEditMode=" + inEditMode + ", viewModeWasOpenedInThePast="
                    + viewModeWasOpenedInThePast + ", config=" + config + "]";
        }
    }

    private final Deque<Snapshot> snapshots = new ArrayDeque<Snapshot>();

    private Snapshot getCurrentSnapshot() {
        Snapshot snapshot = snapshots.peekLast();
        return snapshot;
    }

    private void createSnapshot() {
        snapshots.addLast(new Snapshot());
    }

    @Override
    public void clearState() {
        snapshots.clear();
    }

    // =========================================================================

    // All dialog entry point methods must call this method
    private void open(NodeRef docRef, boolean inEditMode) {
        allow = false;
        if (!validateOpen(docRef, inEditMode)) {
            return;
        }

        allow = true;
        createSnapshot();
        openOrSwitchModeCommon(docRef, inEditMode);
    }

    private void switchMode(boolean inEditMode) {
        openOrSwitchModeCommon(getDocument().getNodeRef(), inEditMode);
    }

    private void openOrSwitchModeCommon(NodeRef docRef, boolean inEditMode) {
        getCurrentSnapshot().document = getDocumentDynamicService().getDocument(docRef);
        getCurrentSnapshot().inEditMode = inEditMode;
        if (!inEditMode) {
            getCurrentSnapshot().viewModeWasOpenedInThePast = true;
        }
        DocumentConfig config = BeanHelper.getDocumentConfigService().getConfig(getNode());
        getCurrentSnapshot().config = config;
        resetComponents();
        LOG.info("document before rendering: " + getDocument());
    }

    private String close() {
        snapshots.removeLast();
        resetComponents();
        return super.cancel();
    }

    // =========================================================================

    @Override
    public void init(Map<String, String> params) {
        LOG.info("init");
        getClearStateNotificationHandler().addClearStateListener(this);
        getDocumentDynamicTestDialog().restored();
        super.init(params);
    }

    @Override
    public void restored() {
        LOG.info("restored");
        // Siin ei ole plaanis midagi teha; kui mingi teine dialoog suletakse ja seetõttu pöördutakse tagasi varemavatud dok.dialoogile, siis nimelt ei tee õiguste ega kustutamise
        // kontrolli
        // Õiguste kontroll on ainult dialoogile sisenemisel
        // Ja eksisteerimise kontroll on igasuguste erinevate tegevuste juures, sest suvalisel hetkel võib niikuinii keegi teine dokumendi kustutada
    }

    // =========================================================================
    // Blocks
    // =========================================================================

    private final Map<Class<? extends DocumentDynamicBlock>, DocumentDynamicBlock> blocks = new HashMap<Class<? extends DocumentDynamicBlock>, DocumentDynamicBlock>();

    public DocumentDynamicDialog() {
        blocks.put(MetadataBlock.class, new MetadataBlock());
        for (DocumentDynamicBlock block : blocks.values()) {
            block.setDocumentDynamicDialog(this);
        }
    }

    public MetadataBlock getMeta() {
        return (MetadataBlock) blocks.get(MetadataBlock.class);
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
        if (!isInEditMode() || (isInEditMode() && !getCurrentSnapshot().viewModeWasOpenedInThePast)) {
            // TODO if draft, then delete

            // Close dialog
            return close();
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
        // TODO DLSeadist this is for testing...
        // if ("type1".equals(BeanHelper.getNodeService().getProperty(docRef, DocumentDynamicModel.Props.DOCUMENT_TYPE_ID))) {
        // MessageUtil.addErrorMessage("action_failed_missingPermission", new MessageDataImpl("permission_" + permission));
        // return false;
        // }
        // if (DocumentCommonModel.Privileges.EDIT_DOCUMENT_META_DATA.equals(permission)
        // && "type2".equals(BeanHelper.getNodeService().getProperty(docRef, DocumentDynamicModel.Props.DOCUMENT_TYPE_ID))) {
        // MessageUtil.addErrorMessage("action_failed_missingPermission", new MessageDataImpl("permission_" + permission));
        // return false;
        // }

        // TODO DLSeadist enable real validation
        // try {
        // validatePermission(docRef, permission);
        // } catch (UnableToPerformException e) {
        // MessageUtil.addStatusMessage(e);
        // return false;
        // }
        return true;
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /** For JSP */
    public DocumentDynamic getDocument() {
        Snapshot snapshot = getCurrentSnapshot();
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

    private DocumentConfig getConfig() {
        Snapshot snapshot = getCurrentSnapshot();
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
        Snapshot snapshot = getCurrentSnapshot();
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
        // TODO documentType title
        return "DocumentDynamicDialog";
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

    private void resetComponents() {
        // TODO call clear on all other blocks and components!!!

        LOG.info("clearPropertySheet propertySheet=" + ObjectUtils.toString(propertySheet));
        if (propertySheet != null) {
            propertySheet.getChildren().clear();
            propertySheet.getClientValidations().clear();
            propertySheet.setNode(getNode());
            propertySheet.setMode(getMode());
            propertySheet.setConfig(getPropertySheetConfigElement());
        }
        getPropertySheetStateBean().reset(getStateHolders(), getCurrentSnapshot() == null ? null : this);
    }

    /** @param event */
    public void doNothing(ActionEvent event) {
        LOG.info("doNothing");
    }

    /*
     * Et generator saaks tagastada mitu itemit, nt. üks view ja teine edit mode jaoks
     * Et documentLocation väljad oleksid kuvatud nii view kui edit mode'is
     * Et salvestamisel kasutataks documentLocation välju ja liigutataks dokumenti
     * Et enne salvestamist toimuks documentLocation valideerimine
     * Lisada pealkirja väli
     * Lisada regNumber ja regDateTime väli
     * Teha dokumentide nimekirjadesse tugi, et avaneks dyn dokumendid
     * Lisada docStatus väli
     * Lisada owner* väljad; siis saab ka õigused juurde tuua
     * Teised blokid
     * Lisada accessRestriction* väljad; siis saab ka fn/sari/toimik puhul nende uuendamise välja kutsuda
     */

    // Põhisuunad
    // * tuua juurde ülejäänud blokid ja tegevused
    // * täiendada metaandmetevälju niikaugele, et väljatüüpide lisamine muutuks iseseisvaks
    // ++ kõige olulisem on fn/sari/toimik/asi väljad, siis saaks dok.loetelu alla salvestada
    // ?? dok.staatus, pealkiri väljad.... - common aspekti alt kuidas üksikuid süsteemseid välju kasutan?
    // * metaandmete valideerimine

    // TODO kontrollida et kustutatud dokumendi ekraanile tagasipöördumine töötaks... või tahavad teised blokid laadida uuesti asju? ja siis oleks mõtekam dialoogi mitte kuvada?

    // TODO hiljem - dokumendi otsing
    // TODO hiljem - metaandmete lukustamine, üldisem lahendus
    // TODO kõige hiljem - vanade dok.liikide

    // dokumendile logikirje lisamiseks teha mudelisse meetod ja siis see lisab alfrescotransactionsupport kontrolli et üldse oleks transaktsioon lahti ja et transaktsiooni lõpus
    // oleks salvestamist tehtud

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
