package ee.webmedia.alfresco.document.einvoice.web;

import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.apache.commons.collections.Predicate;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.converter.DoubleCurrencyConverter_ET_EN;
import ee.webmedia.alfresco.common.propertysheet.dimensionselector.DimensionSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.renderkit.HtmlGridCustomChildAttrRenderer;
import ee.webmedia.alfresco.common.propertysheet.renderkit.HtmlGroupCustomRenderer;
import ee.webmedia.alfresco.common.propertysheet.suggester.SuggesterGenerator;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.model.TransactionModel;
import ee.webmedia.alfresco.document.einvoice.model.TransactionTemplate;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceService;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;

public class TransactionsTemplateDetailsDialog extends BaseDialogBean implements Serializable {

    protected static final String TRANS_TEMPLATE_SELECTOR = "trans-template-selector";
    private static final String NO_OPTION_TITLE = "noOptionTitle";

    protected String getBeanName() {
        return "TransactionsTemplateDetailsDialog";
    }

    public static final String TAX_CODE_ATTR = "taxCode";
    private static final String TRANS_COMPONENT_ID_PREFIX = "trans-";
    protected static final String SELECT_TEMPLATE_NAME = TRANS_COMPONENT_ID_PREFIX + "template-selector";

    protected static final String SAVEAS_TEMPLATE_NAME = TRANS_COMPONENT_ID_PREFIX + "-template-name";

    private static final long serialVersionUID = 1L;

    private static final String TRANSACTION_INDEX = "transIndex";

    private static final List<String> mainHeadingKeys = new ArrayList<String>(Arrays.asList("", "transaction_fundsCenter", "transaction_costCenter", "transaction_fund",
            "transaction_eaCommitmentItem", "transaction_commitmentItem", "transaction_orderNumber", "transaction_assetInventoryNumber", "transaction_sumWithoutVat", ""));
    private static final List<String> secondaryHeadingKeys = new ArrayList<String>(Arrays.asList("transaction_postingKey", "transaction_account",
            "transaction_invoiceTaxCode", "transaction_tradingPartnerCode", "transaction_functionalAreaCode", "transaction_cashFlowCode", "transaction_source",
            "transaction_paymentMethod", "transaction_houseBank", "transaction_entryContent"));

    /** transaction template node or (in subclassing TransactionBlockBean) document node) */
    private Node parentNode;
    private List<Transaction> transactions;
    private final Map<NodeRef, Map<QName, Serializable>> originalProperties = new HashMap<NodeRef, Map<QName, Serializable>>();
    private List<Transaction> removedTransactions = new ArrayList<Transaction>();
    private transient HtmlPanelGroup transactionPanelGroup;
    private NodeRef taskPanelControlNodeRef;
    private TransactionTemplate transactionTemplate;
    private List<TransactionTemplate> transactionTemplates;

    public void init(Node node) {
        reset();
        parentNode = node;
        restore();
    }

    /**
     * JSP event handler.
     * 
     * @param event
     */
    public void select(ActionEvent event) {
        NodeRef nodeRef = new NodeRef(ActionUtil.getParam(event, "nodeRef"));
        WmNode transactionTemplateNode = getExistingTransactionNode(nodeRef);
        reset();
        transactionTemplate = new TransactionTemplate(transactionTemplateNode);
        parentNode = new Node(transactionTemplateNode.getNodeRef());
        restore();
    }

    private WmNode getExistingTransactionNode(NodeRef nodeRef) {
        NodeService nodeService = BeanHelper.getNodeService();
        WmNode transactionTemplateNode = new WmNode(nodeRef, TransactionModel.Types.TRANSACTION_TEMPLATE, RepoUtil.toStringProperties(nodeService
                .getProperties(nodeRef)),
                new HashSet<QName>(
                        nodeService.getAspects(nodeRef)));
        return transactionTemplateNode;
    }

    /**
     * JSP event handler.
     * 
     * @param event
     */
    public void addTemplate(ActionEvent event) {
        WmNode transactionTemplateNode = BeanHelper.getGeneralService().createNewUnSaved(TransactionModel.Types.TRANSACTION_TEMPLATE, null);
        reset();
        parentNode = new Node(transactionTemplateNode.getNodeRef());
        transactionTemplate = new TransactionTemplate(transactionTemplateNode);
        restore();
    }

    public void restore() {
        if (hasExistingParent()) {
            transactions = BeanHelper.getEInvoiceService().getInvoiceTransactions(getParentNodeRef());
            updateOriginalProperties();
        } else {
            transactions = new ArrayList<Transaction>();
        }
        constructTransactionPanelGroup();
    }

    private boolean hasExistingParent() {
        if (transactionTemplate != null) {
            return !transactionTemplate.getNode().isUnsaved();
        }
        return true;
    }

    public NodeRef getParentNodeRef() {
        return parentNode.getNodeRef();
    }

    public Node getParentNode() {
        return parentNode;
    }

    public void setParentNode(Node parentNode) {
        this.parentNode = parentNode;
    }

    public boolean isShowTransactionTemplates() {
        return false;
    }

    private void updateOriginalProperties() {
        for (Transaction transaction : transactions) {
            if (transaction.getNode().getNodeRef() != null) {
                originalProperties.put(transaction.getNode().getNodeRef(),
                        RepoUtil.getPropertiesIgnoringSystem(RepoUtil.toQNameProperties(transaction.getNode().getProperties(), true), BeanHelper.getDictionaryService()));
            }
        }
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        try {
            NodeRef nodeRef = BeanHelper.getEInvoiceService().updateTransactionTemplate(transactionTemplate);
            transactionTemplate = new TransactionTemplate(getExistingTransactionNode(nodeRef));
            parentNode = new Node(nodeRef);
            if (!saveTransactions()) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        reset();
        return outcome;
    }

    public boolean saveTransactions() {
        updateVatCodePercent();
        if (validate()) {
            BeanHelper.getEInvoiceService().updateTransactions(getParentNodeRef(), transactions, removedTransactions);
            return true;
        }
        return false;
    }

    // update vat code for new rows and in case of changed invoiveTaxCode
    // (tax code dimension value may be changed and we don't want to overwrite old values that user didn't change)
    private void updateVatCodePercent() {
        for (Transaction transaction : transactions) {
            Map<QName, Serializable> originalProps = originalProperties.get(transaction.getNode().getNodeRef());
            String taxCode = transaction.getInvoiceTaxCode();
            String originalTaxCode = (String) (originalProps == null ? null : originalProps.get(TransactionModel.Props.INVOICE_TAX_CODE));
            if (originalProps == null || taxCode == null || !taxCode.equals(originalTaxCode)) {
                if (taxCode == null) {
                    transaction.setInvoiceTaxPercent(null);
                } else {
                    transaction.setInvoiceTaxPercent(EInvoiceUtil.getVatPercentageFromDimension(taxCode, BeanHelper.getEInvoiceService().getVatCodeDimensionValues()));
                }
            }
        }
    }

    private boolean validate() {
        for (Transaction transaction : transactions) {
            String entryContent = transaction.getEntryContent();
            if (entryContent != null && entryContent.length() > 50) {
                MessageUtil.addErrorMessage("transactions_entryContentTooLong");
                return false;
            }
        }
        return true;
    }

    private Transaction getNewUnsavedTransaction() {
        return getNewUnsavedTransaction(null);
    }

    protected Transaction getNewUnsavedTransaction(Map<QName, Serializable> props) {
        return new Transaction(BeanHelper.getGeneralService().createNewUnSaved(TransactionModel.Types.TRANSACTION, props));
    }

    public void reset() {
        parentNode = null;
        transactions = null;
        removedTransactions = new ArrayList<Transaction>();
        transactionPanelGroup = null;
        transactionTemplate = null;
        taskPanelControlNodeRef = null;
    }

    protected void constructTransactionPanelGroup() {
        constructTransactionPanelGroup(getTransactionPanelGroupInner());
    }

    protected boolean isInEditMode() {
        return true; // in transaction template view always allow editing
    }

    /**
     * Action listener for JSP.
     */
    public void addTransaction(ActionEvent event) {
        transactions.add(getNewUnsavedTransaction());
        updateTransactionPanelGroup();
        addInvoiceMessages();
    }

    /**
     * Action listener for JSP.
     */
    public void copyTransaction(ActionEvent event) {
        int transIndex = Integer.parseInt(ActionUtil.getParam(event, TRANSACTION_INDEX));
        Transaction originalTransaction = transactions.get(transIndex);
        transactions.add(getNewUnsavedTransaction(RepoUtil.getPropertiesIgnoringSystem(RepoUtil.toQNameProperties(originalTransaction.getNode().getProperties(), true),
                BeanHelper.getDictionaryService())));
        updateTransactionPanelGroup();
        addInvoiceMessages();
    }

    /**
     * Action listener for JSP.
     */
    public void removeTransaction(ActionEvent event) {
        int transIndex = Integer.parseInt(ActionUtil.getParam(event, TRANSACTION_INDEX));
        removedTransactions.add(transactions.remove(transIndex));
        updateTransactionPanelGroup();
        addInvoiceMessages();
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    protected void addInvoiceMessages() {
        // do nothing, override in subclassing TransactionsBlockBean
    }

    private void updateTransactionPanelGroup() {
        getTransactionPanelGroupInner().getChildren().clear();
        constructTransactionPanelGroup();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected HtmlPanelGrid constructTransactionPanelGroup(HtmlPanelGroup panelGroup) {
        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();
        panelGroup.getChildren().clear();

        // main panel
        UIPanel transactionPanel = (UIPanel) application.createComponent("org.alfresco.faces.Panel");
        transactionPanel.setId("transaction-panel");
        transactionPanel.getAttributes().put("styleClass", "panel-100 ie7-workflow");
        transactionPanel.setLabel(MessageUtil.getMessage("transaction_data"));
        transactionPanel.setProgressive(true);
        transactionPanel.setFacetsId("dialog:dialog-body:transaction-panel");
        panelGroup.getChildren().add(transactionPanel);

        String listId = context.getViewRoot().createUniqueId();

        if (isShowTransactionTemplates()) {
            addTransTemplateSelector(context, application, transactionPanel, listId);
        }

        // popup to manually enter sap entry number
        SendManuallyToSapModalComponent entrySapNumber = (SendManuallyToSapModalComponent) application.createComponent(SendManuallyToSapModalComponent.class.getCanonicalName());
        entrySapNumber.setId("entry-sap-popup-" + listId);
        entrySapNumber.setActionListener(application.createMethodBinding("#{DialogManager.bean.sendToSapManually}", UIActions.ACTION_CLASS_ARGS));
        transactionPanel.getChildren().add(entrySapNumber);

        // links for expanding/collapsing all subrows
        final HtmlPanelGroup transExpandingGroup = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        transExpandingGroup.setStyleClass("trans-subrow-toggle");
        transExpandingGroup.setRendererType(HtmlGroupCustomRenderer.HTML_GROUP_CUSTOM_RENDERER_TYPE);
        transExpandingGroup.getAttributes().put(HtmlGroupCustomRenderer.LAYOUT_ATTR, HtmlGroupCustomRenderer.LAYOUT_TYPE_BLOCK);
        transExpandingGroup.setId(TRANS_COMPONENT_ID_PREFIX + "expand-panel-" + listId);
        addExpandingLinks(application, transExpandingGroup, listId);
        transactionPanel.getChildren().add(transExpandingGroup);

        // panel for scrolling transaction rows
        final HtmlPanelGroup transRowsScrollGroup = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        transRowsScrollGroup.setStyleClass("trans-scroll");
        transRowsScrollGroup.setRendererType(HtmlGroupCustomRenderer.HTML_GROUP_CUSTOM_RENDERER_TYPE);
        transRowsScrollGroup.getAttributes().put(HtmlGroupCustomRenderer.LAYOUT_ATTR, HtmlGroupCustomRenderer.LAYOUT_TYPE_BLOCK);
        transRowsScrollGroup.setId(TRANS_COMPONENT_ID_PREFIX + "scroll-panel-" + listId);
        transactionPanel.getChildren().add(transRowsScrollGroup);

        // transaction rows main grid
        final HtmlPanelGrid transRowsMainGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        transRowsMainGrid.setRendererType(HtmlGridCustomChildAttrRenderer.HTML_GRID_CUSTOM_CHILD_ATTR_RENDERER_TYPE);
        transRowsMainGrid.setId(TRANS_COMPONENT_ID_PREFIX + "main-grid-" + listId);
        int mainGridColumnCount = 10;
        transRowsMainGrid.setColumns(mainGridColumnCount);
        transRowsMainGrid.setCellpadding("0");
        transRowsMainGrid.setCellspacing("0");
        transRowsMainGrid.setWidth("100%");

        // custom attributes for rendering
        transRowsMainGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.HEADING_KEYS_ATTR, mainHeadingKeys);
        Map<String, Integer> childrenColspanAttribute = new HashMap<String, Integer>();
        transRowsMainGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.CHILDREN_COLSPANS_ATTR, childrenColspanAttribute);
        Map<String, String> childrenStyleClassAttribute = new HashMap<String, String>();
        transRowsMainGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.CHILDREN_CLASSES_ATTR, childrenStyleClassAttribute);
        List<Pair<String, Pair<String, String>>> footerSums = new ArrayList<Pair<String, Pair<String, String>>>();
        transRowsMainGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.FOOTER_SUMS_ATTR, footerSums);

        transRowsScrollGroup.getChildren().add(transRowsMainGrid);

        List transMainGridChildren = transRowsMainGrid.getChildren();

        // transaction rows
        int transRowCounter = 0;
        String rowClasses = "";
        final String rowClassesEvenValue = "trans-recordSetRow2,trans-subrow,"; // transaction list row and subrow classes
        final String rowClassesOddValue = "trans-recordSetRowAlt2,trans-subrowAlt,";
        Set<String> generalMandatoryFields = getMandatoryFields();
        for (Transaction transaction : transactions) {
            Set<String> transMandatoryProps = getCostManagerMandatoryFields(transaction);
            transMandatoryProps.addAll(generalMandatoryFields);
            // First row inputs
            HtmlOutputLink outputLink = createOutputLink(application, transMainGridChildren, transRowCounter);
            childrenStyleClassAttribute.put(outputLink.getId(), "trans-toggle-subrow");
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_FUNDS_CENTERS, TransactionModel.Props.FUNDS_CENTER, transRowCounter, "width180 ",
                    transMandatoryProps);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_COST_CENTERS, TransactionModel.Props.COST_CENTER, transRowCounter, "width120 ",
                    transMandatoryProps);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_FUNDS, TransactionModel.Props.FUND, transRowCounter, "width120 ",
                    transMandatoryProps);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_COMMITMENT_ITEM, TransactionModel.Props.EA_COMMITMENT_ITEM, transRowCounter, "width180 ",
                    DimensionSelectorGenerator.getEAInclusivePredicate(), transMandatoryProps);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_COMMITMENT_ITEM, TransactionModel.Props.COMMITMENT_ITEM, transRowCounter, "width160 ",
                    DimensionSelectorGenerator.getEAExclusivePredicate(), transMandatoryProps);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_INTERNAL_ORDERS, TransactionModel.Props.ORDER_NUMBER, transRowCounter, "width140 ",
                    transMandatoryProps);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS, TransactionModel.Props.ASSET_INVENTORY_NUMBER, transRowCounter,
                    "width140 ", transMandatoryProps);
            addDoubleInput(context, transMainGridChildren, transRowCounter, TransactionModel.Props.SUM_WITHOUT_VAT, childrenStyleClassAttribute,
                    transMandatoryProps);
            // actions
            transMainGridChildren.add(createTransActions(application, listId, transRowCounter));

            // Second row grid
            final HtmlPanelGrid tranRowSecondaryGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
            String transSecondaryRowId = TRANS_COMPONENT_ID_PREFIX + "sec-grid-" + listId + "-" + transRowCounter;
            tranRowSecondaryGrid.setId(transSecondaryRowId);
            tranRowSecondaryGrid.setRendererType(HtmlGridCustomChildAttrRenderer.HTML_GRID_CUSTOM_CHILD_ATTR_RENDERER_TYPE);
            tranRowSecondaryGrid.setCellpadding("0");
            tranRowSecondaryGrid.setCellspacing("0");
            tranRowSecondaryGrid.setWidth("100%");
            tranRowSecondaryGrid.setColumns(mainGridColumnCount);
            Map<String, String> secondaryChildrenStyleClassAttribute = new HashMap<String, String>();
            tranRowSecondaryGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.CHILDREN_CLASSES_ATTR, secondaryChildrenStyleClassAttribute);
            tranRowSecondaryGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.HEADING_KEYS_ATTR, secondaryHeadingKeys);
            childrenColspanAttribute.put(transSecondaryRowId, mainGridColumnCount);

            transMainGridChildren.add(tranRowSecondaryGrid);

            List tranRowSecondaryGridChildren = tranRowSecondaryGrid.getChildren();

            // Second row inputs
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_POSTING_KEY, TransactionModel.Props.POSTING_KEY, transRowCounter, "width40 ",
                    transMandatoryProps);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_ACCOUNTS, TransactionModel.Props.ACCOUNT, transRowCounter, "width120 ",
                    transMandatoryProps);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.TAX_CODE_ITEMS, TransactionModel.Props.INVOICE_TAX_CODE, transRowCounter, "width40 ",
                    transMandatoryProps);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_TRADING_PARTNER_CODES, TransactionModel.Props.TRADING_PARTNER_CODE, transRowCounter,
                    "width80 ", transMandatoryProps);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_FUNCTIONAL_AREA_CODE, TransactionModel.Props.FUNCTIONAL_ARE_CODE, transRowCounter,
                    "width180 ", transMandatoryProps);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_CASH_FLOW_CODES, TransactionModel.Props.CASH_FLOW_CODE, transRowCounter, "width50 ",
                    transMandatoryProps);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_SOURCE_CODES, TransactionModel.Props.SOURCE, transRowCounter, "width60 ",
                    transMandatoryProps);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_PAYMENT_METHOD_CODES, TransactionModel.Props.PAYMENT_METHOD, transRowCounter,
                    "width40 ", transMandatoryProps);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_HOUSE_BANK_CODES, TransactionModel.Props.HOUSE_BANK, transRowCounter, "width70 ",
                    transMandatoryProps);
            addTextareaInput(context, tranRowSecondaryGridChildren, transRowCounter, TransactionModel.Props.ENTRY_CONTENT, secondaryChildrenStyleClassAttribute,
                    transMandatoryProps);

            if ((transRowCounter & 1) == 0) {
                rowClasses = rowClasses + rowClassesEvenValue;
            } else {
                rowClasses = rowClasses + rowClassesOddValue;
            }
            transRowCounter++;
        }
        transRowsMainGrid.getAttributes().put(JSFAttr.ROW_CLASSES_ATTR, rowClasses);

        // footer elements
        if (isInEditMode()) {
            createAddTransLink(application, listId, transRowsMainGrid.getFacets());
        }

        if (isShowTransactionTemplates()) {
            addTemplateSaveAs(context, application, transactionPanel, listId);
        }

        return transRowsMainGrid;

    }

    @SuppressWarnings("unchecked")
    private void addTemplateSaveAs(FacesContext context, Application application, UIPanel transactionPanel, String listId) {
        UIPanel tranSaveAsTemplatePanel = (UIPanel) application.createComponent("org.alfresco.faces.Panel");
        tranSaveAsTemplatePanel.setId(TRANS_COMPONENT_ID_PREFIX + "saveas-panel");
        tranSaveAsTemplatePanel.setRendererType(HtmlGroupCustomRenderer.HTML_GROUP_CUSTOM_RENDERER_TYPE);
        tranSaveAsTemplatePanel.getAttributes().put(HtmlGroupCustomRenderer.LAYOUT_ATTR, HtmlGroupCustomRenderer.LAYOUT_TYPE_BLOCK);

        final HtmlPanelGrid tranSaveAsTemplateGrid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
        tranSaveAsTemplateGrid.setId(TRANS_COMPONENT_ID_PREFIX + "saveas-template-" + listId);
        tranSaveAsTemplateGrid.setWidth("100%");
        tranSaveAsTemplateGrid.setColumns(3);
        tranSaveAsTemplateGrid.setStyleClass("trans-template-saveas");
        tranSaveAsTemplateGrid.setColumnClasses("propertiesLabel");

        UIOutput selectTemplatelabel = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        selectTemplatelabel.setValue(MessageUtil.getMessage("transactions_saveas_template") + ": ");
        tranSaveAsTemplateGrid.getChildren().add(selectTemplatelabel);

        SuggesterGenerator suggesterGenerator = new SuggesterGenerator();
        suggesterGenerator.getCustomAttributes().put("suggesterValues", "#{" + getBeanName() + ".getActiveTransactionTemplateNames}");
        UIComponent suggester = suggesterGenerator.generate(context, SAVEAS_TEMPLATE_NAME);
        tranSaveAsTemplateGrid.getChildren().add(suggester);

        UIOutput selectDummy = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        selectDummy.setValue("");
        tranSaveAsTemplateGrid.getChildren().add(selectDummy);

        selectDummy = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        selectDummy.setValue("");
        tranSaveAsTemplateGrid.getChildren().add(selectDummy);

        HtmlCommandButton saveAsButton = new HtmlCommandButton();
        saveAsButton.setId(TRANS_COMPONENT_ID_PREFIX + "-template-saveas-button-" + listId);
        saveAsButton.setActionListener(application.createMethodBinding("#{" + getBeanName() + ".saveAsTemplate}", new Class[] { ActionEvent.class }));
        saveAsButton.setValue(MessageUtil.getMessage("transactions_saveas_template_button"));
        tranSaveAsTemplateGrid.getChildren().add(saveAsButton);

        tranSaveAsTemplatePanel.getChildren().add(tranSaveAsTemplateGrid);
        transactionPanel.getChildren().add(tranSaveAsTemplatePanel);

    }

    public List<String> getActiveTransactionTemplateNames(javax.faces.context.FacesContext context, javax.faces.component.UIInput control) {
        return BeanHelper.getEInvoiceService().getActiveTransactionTemplateNames();
    }

    @SuppressWarnings("unchecked")
    private void addTransTemplateSelector(FacesContext context, Application application, UIPanel transactionPanel, String listId) {
        UIPanel transTemplateSelectGroup = (UIPanel) application.createComponent("org.alfresco.faces.Panel");
        transTemplateSelectGroup.setId(TRANS_COMPONENT_ID_PREFIX + "setTemplate-panel");
        transTemplateSelectGroup.setRendererType(HtmlGroupCustomRenderer.HTML_GROUP_CUSTOM_RENDERER_TYPE);

        transTemplateSelectGroup.setId(TRANS_COMPONENT_ID_PREFIX + "select-template-panel-" + listId);
        UIOutput selectTemplatelabel = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        selectTemplatelabel.setValue(MessageUtil.getMessage("transactions_select_tempalte") + ": ");
        transTemplateSelectGroup.getChildren().add(selectTemplatelabel);

        GeneralSelectorGenerator selectorGenerator = new GeneralSelectorGenerator();
        HtmlSelectOneMenu transTemplateSelector = (HtmlSelectOneMenu) selectorGenerator.generateSelectComponent(context, SELECT_TEMPLATE_NAME, false);
        selectorGenerator.getCustomAttributes().put("selectionItems", "#{" + getBeanName() + ".getTransactionTemplates}");
        selectorGenerator.setupSelectComponent(context, null, null, null, transTemplateSelector, false);
        transTemplateSelector.setId(TRANS_TEMPLATE_SELECTOR);
        transTemplateSelector.getAttributes().put(
                "styleClass",
                GeneralSelectorGenerator.ONCHANGE_PARAM_MARKER_CLASS + GeneralSelectorGenerator.ONCHANGE_SCRIPT_START_MARKER
                        + "var link = jQuery('#' + escapeId4JQ(currElId)).nextAll('a').get(0); link.click();");
        transTemplateSelectGroup.getChildren().add(transTemplateSelector);

        // hidden link for submitting form when transTemplateSelector onchange event occurs
        HtmlCommandLink transTemplateSelectorHiddenLink = new HtmlCommandLink();
        transTemplateSelectorHiddenLink.setId(TRANS_COMPONENT_ID_PREFIX + "trans-select-template-link-" + listId);
        transTemplateSelectorHiddenLink.setActionListener(application.createMethodBinding("#{" + getBeanName() + ".copyFromTemplate}", new Class[] { ActionEvent.class }));
        transTemplateSelectorHiddenLink.setStyle("display: none;");

        transTemplateSelectGroup.getChildren().add(transTemplateSelectorHiddenLink);

        transactionPanel.getFacets().put("title", transTemplateSelectGroup);
    }

    protected boolean isMandatory(QName propName, Set<String> transMandatoryProps) {
        return false;
    }

    protected Set<String> getCostManagerMandatoryFields(Transaction transaction) {
        return new HashSet<String>();
    }

    protected Set<String> getMandatoryFields() {
        return new HashSet<String>();
    }

    @SuppressWarnings("unchecked")
    private void addExpandingLinks(Application application, HtmlPanelGroup transExpandingGroup, String listId) {
        HtmlOutputLink expandLink = (HtmlOutputLink) application.createComponent(HtmlOutputLink.COMPONENT_TYPE);
        expandLink.setValue("#");
        expandLink.setTitle(MessageUtil.getMessage("transactions_expand_transactions"));
        expandLink.setStyleClass("open");
        expandLink.setId(TRANS_COMPONENT_ID_PREFIX + "list-expand-" + listId);

        UIOutput expandText = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        expandText.setValue(MessageUtil.getMessage("transactions_expand_transactions"));
        expandLink.getChildren().add(expandText);

        transExpandingGroup.getChildren().add(expandLink);

        UIOutput span = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        span.setValue(" | ");
        transExpandingGroup.getChildren().add(span);

        HtmlOutputLink collapseLink = (HtmlOutputLink) application.createComponent(HtmlOutputLink.COMPONENT_TYPE);
        collapseLink.setValue("#");
        collapseLink.setTitle(MessageUtil.getMessage("transactions_collapse_transactions"));
        collapseLink.setStyleClass("close");
        collapseLink.setId(TRANS_COMPONENT_ID_PREFIX + "list-collapse-" + listId);

        UIOutput collapseText = (UIOutput) application.createComponent(UIOutput.COMPONENT_TYPE);
        collapseText.setValue(MessageUtil.getMessage("transactions_collapse_transactions"));
        collapseLink.getChildren().add(collapseText);

        transExpandingGroup.getChildren().add(collapseLink);
    }

    @SuppressWarnings("unchecked")
    private HtmlPanelGroup createTransActions(Application application, String listId, int transRowCounter) {
        final HtmlPanelGroup columnActions = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        columnActions.setId(TRANS_COMPONENT_ID_PREFIX + "actions-" + listId + "-" + transRowCounter);

        if (isInEditMode()) {
            // delete transaction
            final UIActionLink transDeleteLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
            transDeleteLink.setId(TRANS_COMPONENT_ID_PREFIX + "remove-" + listId + "-" + transRowCounter);
            transDeleteLink.setValue("");
            transDeleteLink.setTooltip(MessageUtil.getMessage("transactions_delete_transaction"));
            transDeleteLink.setActionListener(application.createMethodBinding("#{" + getBeanName() + ".removeTransaction}",
                    UIActions.ACTION_CLASS_ARGS));
            transDeleteLink.setShowLink(false);
            putAttribute(transDeleteLink, "styleClass", "icon-link margin-left-4 delete");
            addChildren(transDeleteLink, createTransIndexParam(transRowCounter, application));
            columnActions.getChildren().add(transDeleteLink);

            // copy transaction
            final UIActionLink transCopyLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
            transCopyLink.setId(TRANS_COMPONENT_ID_PREFIX + "copy-" + listId + "-" + transRowCounter);
            transCopyLink.setValue("");
            transCopyLink.setTooltip(MessageUtil.getMessage("transactions_copy_transaction"));
            transCopyLink.setActionListener(application.createMethodBinding("#{" + getBeanName() + ".copyTransaction}",
                    UIActions.ACTION_CLASS_ARGS));
            transCopyLink.setShowLink(false);
            putAttribute(transCopyLink, "styleClass", "icon-link margin-left-4 version_history");
            addChildren(transCopyLink, createTransIndexParam(transRowCounter, application));
            columnActions.getChildren().add(transCopyLink);
        }

        return columnActions;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void createAddTransLink(Application application, String listId, Map map) {
        UIActionLink taskAddLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
        taskAddLink.setId(TRANS_COMPONENT_ID_PREFIX + "add-link-" + listId);
        taskAddLink.setValue("");
        taskAddLink.setTooltip(MessageUtil.getMessage("transactions_add_transaction"));
        taskAddLink.setActionListener(createAddTransMethodBinding(application));
        taskAddLink.setShowLink(false);
        ComponentUtil.putAttribute(taskAddLink, "styleClass", "icon-link add-person");
        map.put(HtmlGridCustomChildAttrRenderer.FOOTER_ACTIONS_FACET, taskAddLink);
    }

    protected UIParameter createTransIndexParam(int transIndexCounter, Application application) {
        return createUIParam(TRANSACTION_INDEX, transIndexCounter, application);
    }

    protected MethodBinding createAddTransMethodBinding(Application application) {
        return application.createMethodBinding("#{TransactionsTemplateDetailsDialog.addTransaction}", UIActions.ACTION_CLASS_ARGS);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private HtmlOutputLink createOutputLink(Application application, List transactionGridChildren, int transactionIndex) {
        HtmlOutputLink outputLink = (HtmlOutputLink) application.createComponent(HtmlOutputLink.COMPONENT_TYPE);
        outputLink.setValue("#");
        outputLink.setId(TRANS_COMPONENT_ID_PREFIX + "expand-" + transactionIndex);
        transactionGridChildren.add(outputLink);
        return outputLink;
    }

    @SuppressWarnings("rawtypes")
    private void addDimensionSelector(FacesContext context, List siblings, Dimensions dimensions, QName propName, int transactionIndex, Set<String> mandatoryProps) {
        addDimensionSelector(context, siblings, dimensions, propName, transactionIndex, null, null, mandatoryProps);
    }

    @SuppressWarnings("rawtypes")
    private void addDimensionSelector(FacesContext context, List siblings, Dimensions dimensions, QName propName, int transactionIndex, Predicate filter,
            Set<String> mandatoryProps) {
        addDimensionSelector(context, siblings, dimensions, propName, transactionIndex, null, filter, mandatoryProps);
    }

    @SuppressWarnings("rawtypes")
    private void addDimensionSelector(FacesContext context, List siblings, Dimensions dimensions, QName propName, int transactionIndex, String styleClass,
            Set<String> mandatoryProps) {
        addDimensionSelector(context, siblings, dimensions, propName, transactionIndex, styleClass, null, mandatoryProps);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addDimensionSelector(FacesContext context, List siblings, Dimensions dimensions, QName propName, int transactionIndex, String styleClass, Predicate filter,
            Set<String> mandatoryProps) {
        UIComponent dimensionSelector;
        if (isInEditMode()) {
            final HtmlPanelGroup dimensionSelectorPanel = (HtmlPanelGroup) context.getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            dimensionSelectorPanel.setId(TRANS_COMPONENT_ID_PREFIX + "trans-panel-" + propName.getLocalName() + "-" + transactionIndex);
            DimensionSelectorGenerator dimensionGenerator;
            if (filter != null) {
                dimensionGenerator = new DimensionSelectorGenerator(filter);
            } else {
                dimensionGenerator = new DimensionSelectorGenerator();
            }
            dimensionGenerator.getCustomAttributes().put(ClassificatorSelectorGenerator.ATTR_DESCRIPTION_AS_LABEL, "true");
            dimensionGenerator.setSelectedValue((String) transactions.get(transactionIndex).getNode().getProperties().get(propName));
            dimensionSelector = dimensionGenerator.generateSelectComponent(context, null, false);
            dimensionGenerator.getCustomAttributes().put(DimensionSelectorGenerator.ATTR_DIMENSION_NAME, dimensions.getDimensionName());
            dimensionGenerator.setupSelectComponent(context, null, null, null, dimensionSelector, false);
            dimensionSelector.getAttributes().put(CustomAttributeNames.STYLE_CLASS, styleClass == null ? "width120 " + NO_OPTION_TITLE : styleClass + " " + NO_OPTION_TITLE);

            dimensionSelector.getAttributes().put("displayMandatoryMark", true);

            setIdAndValueBinding(context, transactionIndex, propName, dimensionSelector);
            dimensionSelectorPanel.getChildren().add(dimensionSelector);

            if (isMandatory(propName, mandatoryProps)) {
                addMandatorySpan(context, propName, transactionIndex, dimensionSelectorPanel);
            }
            siblings.add(dimensionSelectorPanel);
        } else {
            dimensionSelector = context.getApplication().createComponent(UIOutput.COMPONENT_TYPE);
            setIdAndValueBinding(context, transactionIndex, propName, dimensionSelector);
            String dimensionValueName = (String) transactions.get(transactionIndex).getNode().getProperties().get(propName);
            EInvoiceService eInvoiceService = BeanHelper.getEInvoiceService();
            DimensionValue dimensionValue = eInvoiceService.getDimensionValue(eInvoiceService.getDimension(dimensions), dimensionValueName);
            String tooltip = dimensionValue != null ? TextUtil.joinStringAndStringWithSeparator(dimensionValue.getValue(), dimensionValue.getValueComment(), "; ") : "";
            dimensionSelector.getAttributes().put("title", tooltip);
            siblings.add(dimensionSelector);
        }

    }

    @SuppressWarnings("unchecked")
    private void addMandatorySpan(FacesContext context, QName propName, int transactionIndex, final HtmlPanelGroup dimensionSelectorPanel) {
        UIOutput span = (UIOutput) context.getApplication().createComponent(UIOutput.COMPONENT_TYPE);
        span.setId("trans-panel-mandatory-" + propName.getLocalName() + "-" + transactionIndex);
        span.setValue(" *");
        span.getAttributes().put("style", "color: red;");
        dimensionSelectorPanel.getChildren().add(span);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addDoubleInput(FacesContext context, List siblings, int transactionIndex, QName propName, Map<String, String> childrenStyleClassAttribute,
            Set<String> mandatoryProps) {
        UIComponent doubleInput;
        if (isInEditMode()) {
            final HtmlPanelGroup dimensionSelectorPanel = (HtmlPanelGroup) context.getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            dimensionSelectorPanel.setId(TRANS_COMPONENT_ID_PREFIX + "trans-panel-" + propName.getLocalName() + "-" + transactionIndex);
            doubleInput = context.getApplication().createComponent(HtmlInputText.COMPONENT_TYPE);
            ((HtmlInputText) doubleInput).setConverter(new DoubleCurrencyConverter_ET_EN());
            setIdAndValueBinding(context, transactionIndex, propName, doubleInput);
            doubleInput.getAttributes().put("maxlength", 16);
            doubleInput.getAttributes().put(UIProperty.ALLOW_COMMA_AS_DECIMAL_SEPARATOR_ATTR, "true");
            dimensionSelectorPanel.getChildren().add(doubleInput);

            if (isMandatory(propName, mandatoryProps)) {
                addMandatorySpan(context, propName, transactionIndex, dimensionSelectorPanel);
            }

            siblings.add(dimensionSelectorPanel);
        } else {
            doubleInput = context.getApplication().createComponent(UIOutput.COMPONENT_TYPE);
            Double value = (Double) transactions.get(transactionIndex).getNode().getProperties().get(propName);
            ((UIOutput) doubleInput).setValue(value != null ? EInvoiceUtil.getInvoiceNumberFormat().format(value.doubleValue()) : "");
            doubleInput.setId(getComponentId(transactionIndex, propName));
            childrenStyleClassAttribute.put(doubleInput.getId(), "trans-align-right");
            siblings.add(doubleInput);
        }
        doubleInput.getAttributes().put(CustomAttributeNames.STYLE_CLASS, "margin-left-4 width120");
        doubleInput.getAttributes().put("style", "text-align: right");

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void addTextareaInput(FacesContext context, List siblings, int transactionIndex, QName propName, Map<String, String> secondaryChildrenStyleClassAttribute,
            Set<String> mandatoryProps) {
        UIComponent textareaInput;
        String id = getComponentId(transactionIndex, propName);
        if (isInEditMode()) {
            final HtmlPanelGroup dimensionSelectorPanel = (HtmlPanelGroup) context.getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            dimensionSelectorPanel.setId(TRANS_COMPONENT_ID_PREFIX + "trans-panel-" + propName.getLocalName() + "-" + transactionIndex);
            TextAreaGenerator textAreaGenerator = new TextAreaGenerator();
            textareaInput = textAreaGenerator.generate(context, id);
            dimensionSelectorPanel.getChildren().add(textareaInput);

            if (isMandatory(propName, mandatoryProps)) {
                addMandatorySpan(context, propName, transactionIndex, dimensionSelectorPanel);
            }

            siblings.add(dimensionSelectorPanel);
        } else {
            textareaInput = context.getApplication().createComponent(UIOutput.COMPONENT_TYPE);
            textareaInput.setId(id);
            textareaInput.getAttributes().put("style", "whitespace: normal;");
            secondaryChildrenStyleClassAttribute.put(id, "trans-width-10");
            siblings.add(textareaInput);
        }
        setValueBinding(context, transactionIndex, propName, textareaInput);
        textareaInput.getAttributes().put(CustomAttributeNames.STYLE_CLASS, "expand19-200 medium");
    }

    private void setIdAndValueBinding(FacesContext context, int transactionIndex, QName propName, final UIComponent doubleInput) {
        doubleInput.setId(getComponentId(transactionIndex, propName));
        setValueBinding(context, transactionIndex, propName, doubleInput);
    }

    private void setValueBinding(FacesContext context, int transactionIndex, QName propName, final UIComponent doubleInput) {
        String valueBindingStr = createPropValueBinding(transactionIndex, propName);
        doubleInput.setValueBinding("value", context.getApplication().createValueBinding(valueBindingStr));
    }

    private String getComponentId(int transactionIndex, QName propName) {
        return TRANS_COMPONENT_ID_PREFIX + propName.getLocalName() + "-" + transactionIndex;
    }

    private String createPropValueBinding(int transactionIndex, QName propName) {
        return "#{" + getBeanName() + ".transactions[" + transactionIndex + "].node.properties[\"" + propName + "\"]}";
    }

    public List<Transaction> getTransactions() {
        if (transactions == null) {
            transactions = new ArrayList<Transaction>();
        }
        return transactions;
    }

    public List<Transaction> getRemovedTransactions() {
        if (removedTransactions == null) {
            removedTransactions = new ArrayList<Transaction>();
        }
        return removedTransactions;
    }

    public List<TransactionTemplate> getTransactionTemplates() {
        return transactionTemplates;
    }

    public void setTransactionTemplates(List<TransactionTemplate> transactionTemplates) {
        this.transactionTemplates = transactionTemplates;
    }

    public Map<NodeRef, Map<QName, Serializable>> getOriginalProperties() {
        return originalProperties;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    /**
     * NB! Don't call this method from java code; this is meant ONLY for transaction-block.jsp binding.
     * Use getTransactionPanelGroupInner() instead.
     */
    public HtmlPanelGroup getTransactionPanelGroup() {
        if (transactionPanelGroup == null) {
            transactionPanelGroup = new HtmlPanelGroup();
        }
        taskPanelControlNodeRef = getParentNodeRef();
        return transactionPanelGroup;
    }

    protected HtmlPanelGroup getTransactionPanelGroupInner() {
        // This will be called once in the first RESTORE VIEW phase.
        if (transactionPanelGroup == null) {
            transactionPanelGroup = new HtmlPanelGroup();
        }
        return transactionPanelGroup;
    }

    public void setTransactionPanelGroup(HtmlPanelGroup transactionPanelGroup) {
        if (taskPanelControlNodeRef != null && !taskPanelControlNodeRef.equals(getParentNodeRef())) {
            constructTransactionPanelGroup(transactionPanelGroup);
            taskPanelControlNodeRef = getParentNodeRef();
        }
        this.transactionPanelGroup = transactionPanelGroup;
    }

}
