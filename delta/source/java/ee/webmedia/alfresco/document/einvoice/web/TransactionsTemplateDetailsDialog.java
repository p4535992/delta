package ee.webmedia.alfresco.document.einvoice.web;

import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.property.UIProperty;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;

import ee.webmedia.alfresco.common.propertysheet.converter.DoubleCurrencyConverter_ET_EN;
import ee.webmedia.alfresco.common.propertysheet.dimensionselector.DimensionSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.generator.GeneralSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
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
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.TextUtil;

public class TransactionsTemplateDetailsDialog extends BaseDialogBean implements Serializable {

    public static final String MODAL_KEY_ENTRY_SAP_NUMBER = "entrySapNumber";

    private static final String EA_COMMITMENT_ITEM_HEADING_KEY = "transaction_eaCommitmentItem";
    private static final String ORIGINAL_TAX_OPTION_SEPARATOR = "¤";
    private static final String INLINE_STYLE_DISPLAY_NONE = "display: none;";
    private static final String TRANS_ROW_SUM_INPUT_CLASS = "trans-row-sum-input";
    private static final String TRANS_ROW_VAT_CODE_INPUT_CLASS = "trans-row-vat-code-input";
    private static final String TRANS_ROW_ENTRY_CONTENT_INPUT_CLASS = "trans-row-entry-content-input";
    private static final String TRANS_MAIN_TABLE_CLASS = "trans-main-table";
    protected static final String TRANS_TEMPLATE_SELECTOR = "trans-template-selector";
    protected static final String TRANS_TAX_CODE_SELECTOR = "trans-tax-code-selector";
    protected static final String TRANS_TAX_ORIGINAL_INFO_SELECTOR = "trans-tax-original-info-selector";
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

    private static final Map<Dimensions, String> headingKeyMapping;
    static {
        Map<Dimensions, String> dimensionToHeadingKeyMapping = new HashMap<Dimensions, String>();
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_ACCOUNTS, "transaction_account");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS, "transaction_assetInventoryNumber");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_CASH_FLOW_CODES, "transaction_cashFlowCode");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_COMMITMENT_ITEM, "transaction_commitmentItem"); // must check here if eaCommitmentItem or commitmentItem is actually
                                                                                                            // required
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_COST_CENTERS, "transaction_costCenter");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_FUNCTIONAL_AREA_CODE, "transaction_functionalAreaCode");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_FUNDS, "transaction_fund");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_FUNDS_CENTERS, "transaction_fundsCenter");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_HOUSE_BANK_CODES, "transaction_houseBank");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_INTERNAL_ORDERS, "transaction_orderNumber");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_PAYMENT_METHOD_CODES, "transaction_paymentMethod");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_POSTING_KEY, "transaction_postingKey");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_SOURCE_CODES, "transaction_source");
        dimensionToHeadingKeyMapping.put(Dimensions.INVOICE_TRADING_PARTNER_CODES, "transaction_tradingPartnerCode");
        dimensionToHeadingKeyMapping.put(Dimensions.TAX_CODE_ITEMS, "transaction_invoiceTaxCode");
        headingKeyMapping = dimensionToHeadingKeyMapping;
    }
    private static final List<String> mainHeadingKeys = new ArrayList<String>(Arrays.asList("", headingKeyMapping.get(Dimensions.INVOICE_FUNDS_CENTERS),
            headingKeyMapping.get(Dimensions.INVOICE_COST_CENTERS), headingKeyMapping.get(Dimensions.INVOICE_FUNDS),
            EA_COMMITMENT_ITEM_HEADING_KEY, headingKeyMapping.get(Dimensions.INVOICE_COMMITMENT_ITEM), headingKeyMapping.get(Dimensions.INVOICE_INTERNAL_ORDERS),
            headingKeyMapping.get(Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS), "transaction_sumWithoutVat", ""));
    private static final List<String> secondaryHeadingKeys = new ArrayList<String>(Arrays.asList(headingKeyMapping.get(Dimensions.INVOICE_POSTING_KEY),
            headingKeyMapping.get(Dimensions.INVOICE_ACCOUNTS),
            headingKeyMapping.get(Dimensions.TAX_CODE_ITEMS), headingKeyMapping.get(Dimensions.INVOICE_TRADING_PARTNER_CODES),
            headingKeyMapping.get(Dimensions.INVOICE_FUNCTIONAL_AREA_CODE), headingKeyMapping.get(Dimensions.INVOICE_CASH_FLOW_CODES),
            headingKeyMapping.get(Dimensions.INVOICE_SOURCE_CODES),
            headingKeyMapping.get(Dimensions.INVOICE_PAYMENT_METHOD_CODES), headingKeyMapping.get(Dimensions.INVOICE_HOUSE_BANK_CODES), "transaction_entryContent"));

    /** transaction template node or (in subclassing TransactionBlockBean) document node) */
    private Node parentNode;
    private List<Transaction> transactions;
    private final Map<NodeRef, Map<QName, Serializable>> originalProperties = new HashMap<NodeRef, Map<QName, Serializable>>();
    private List<Transaction> removedTransactions = new ArrayList<Transaction>();
    private transient HtmlPanelGroup transactionPanelGroup;
    private NodeRef taskPanelControlNodeRef;
    private TransactionTemplate transactionTemplate;
    private List<TransactionTemplate> transactionTemplates;
    private List<SelectItem> taxOriginalInfoSelectItems = null;

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
        constructTransactionPanelGroup(false);
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

    public boolean isManageTransactionTemplates() {
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
        List<String> errorMsgs = new ArrayList<String>();
        EInvoiceService einvoiceService = BeanHelper.getEInvoiceService();
        Map<Dimensions, NodeRef> dimensionsToNodeRefs = einvoiceService.getDimensionToNodeRefMappings();
        Date entryDate = getEntryDate();
        for (Transaction transaction : transactions) {
            String entryContent = transaction.getEntryContent();
            if (entryContent != null && entryContent.length() > 50) {
                errorMsgs.add(MessageUtil.getMessage("transactions_entryContentTooLong"));
            }
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getAccount(), Dimensions.INVOICE_ACCOUNTS);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getAssetInventoryNumber(), Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getCashFlowCode(), Dimensions.INVOICE_CASH_FLOW_CODES);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getCommitmentItem(), Dimensions.INVOICE_COMMITMENT_ITEM,
                    null, DimensionSelectorGenerator.predefinedFilters.get(DimensionSelectorGenerator.EA_PREFIX_EXCLUDE_FILTER_KEY));
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getEaCommitmentItem(), Dimensions.INVOICE_COMMITMENT_ITEM,
                    EA_COMMITMENT_ITEM_HEADING_KEY, DimensionSelectorGenerator.predefinedFilters.get(DimensionSelectorGenerator.EA_PREFIX_INCLUDE_FILTER_KEY));
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getCostCenter(), Dimensions.INVOICE_COST_CENTERS);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getFunctionalAreaCode(), Dimensions.INVOICE_FUNCTIONAL_AREA_CODE);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getFund(), Dimensions.INVOICE_FUNDS);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getFundsCenter(), Dimensions.INVOICE_FUNDS_CENTERS);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getHouseBank(), Dimensions.INVOICE_HOUSE_BANK_CODES);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getOrderNumber(), Dimensions.INVOICE_INTERNAL_ORDERS);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getPaymentMethod(), Dimensions.INVOICE_PAYMENT_METHOD_CODES);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getPostingKey(), Dimensions.INVOICE_POSTING_KEY);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getSource(), Dimensions.INVOICE_SOURCE_CODES);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getTradingPartnerCode(), Dimensions.INVOICE_TRADING_PARTNER_CODES);
            validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, transaction.getInvoiceTaxCode(), Dimensions.TAX_CODE_ITEMS);
        }
        if (!errorMsgs.isEmpty()) {
            for (String msg : errorMsgs) {
                Utils.addErrorMessage(msg);
            }
            return false;
        }
        return true;
    }

    private void validateDimensionValue(List<String> errorMsgs, Map<Dimensions, NodeRef> dimensionsToNodeRefs, Date entryDate, final String currentValueName,
            Dimensions currentDimension) {
        validateDimensionValue(errorMsgs, dimensionsToNodeRefs, entryDate, currentValueName, currentDimension, null, null);
    }

    private void validateDimensionValue(List<String> errorMsgs, Map<Dimensions, NodeRef> dimensionsToNodeRefs, Date entryDate, final String currentValueName,
            Dimensions currentDimension, String dimensionLabelKey, Predicate filter) {
        if (StringUtils.isNotBlank(currentValueName)) {
            List<DimensionValue> dimensionValues = BeanHelper.getEInvoiceService().getAllDimensionValuesFromCache(dimensionsToNodeRefs.get(currentDimension));
            DimensionValue existingValue = EInvoiceUtil.findDimensionValueByValueName(currentValueName, dimensionValues);
            if (filter != null && existingValue != null && !filter.evaluate(existingValue)) {
                existingValue = null;
            }
            if (dimensionLabelKey == null) {
                dimensionLabelKey = headingKeyMapping.get(currentDimension);
            }
            if (existingValue == null) {
                errorMsgs
                        .add(MessageUtil.getMessage("transactions_no_existing_dimension_value", MessageUtil.getMessage(dimensionLabelKey), currentValueName));
            } else if (entryDate != null) {
                if (!EInvoiceUtil.isDateInPeriod(entryDate, existingValue.getBeginDateTime(), existingValue.getEndDateTime())) {
                    errorMsgs.add(MessageUtil.getMessage("transactions_dimension_value_not_in_period",
                            I18NUtil.getMessage("docspec_documentSpecificModel.property.docspec_" + DocumentSpecificModel.Props.ENTRY_DATE.getLocalName() + ".title"),
                            MessageUtil.getMessage(dimensionLabelKey)));
                }
            }
        }
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

    protected void constructTransactionPanelGroup(boolean rowAdded) {
        constructTransactionPanelGroup(getTransactionPanelGroupInner(), rowAdded);
    }

    protected boolean isInEditMode() {
        return true; // in transaction template view always allow editing
    }

    /**
     * Action listener for JSP.
     */
    public void addTransaction(ActionEvent event) {
        transactions.add(getNewUnsavedTransaction());
        updateTransactionPanelGroup(true);
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
        updateTransactionPanelGroup(false);
        addInvoiceMessages();
    }

    /**
     * Action listener for JSP.
     */
    public void removeTransaction(ActionEvent event) {
        int transIndex = Integer.parseInt(ActionUtil.getParam(event, TRANSACTION_INDEX));
        removedTransactions.add(transactions.remove(transIndex));
        updateTransactionPanelGroup(false);
        addInvoiceMessages();
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    protected void addInvoiceMessages() {
        // do nothing, override in subclassing TransactionsBlockBean
    }

    private void updateTransactionPanelGroup(boolean rowAdded) {
        getTransactionPanelGroupInner().getChildren().clear();
        constructTransactionPanelGroup(rowAdded);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected HtmlPanelGrid constructTransactionPanelGroup(HtmlPanelGroup panelGroup, boolean rowAdded) {
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
        ModalLayerComponent entrySapNumber = (ModalLayerComponent) application.createComponent(ModalLayerComponent.class.getCanonicalName());
        entrySapNumber.setId("entrySapNumber");
        entrySapNumber.getAttributes().put(ModalLayerComponent.ATTR_HEADER_KEY, "document_invoiceEntrySapNumber_insert");
        UIInput entryNumberInput = (UIInput) application.createComponent(HtmlInputText.COMPONENT_TYPE);
        entryNumberInput.setId(MODAL_KEY_ENTRY_SAP_NUMBER);
        Map attributes = entryNumberInput.getAttributes();
        attributes.put(ModalLayerComponent.ATTR_LABEL_KEY, "document_invoiceEntrySapNumber");
        attributes.put(ModalLayerComponent.ATTR_MANDATORY, Boolean.TRUE);
        entrySapNumber.getChildren().add(entryNumberInput);
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
        transRowsMainGrid.setStyleClass(TRANS_MAIN_TABLE_CLASS);
        transRowsMainGrid.setWidth("100%");

        // custom attributes for rendering
        transRowsMainGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.HEADING_KEYS_ATTR, mainHeadingKeys);
        Map<String, Integer> childrenColspanAttribute = new HashMap<String, Integer>();
        transRowsMainGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.CHILDREN_COLSPANS_ATTR, childrenColspanAttribute);
        Map<String, String> childrenStyleClassAttribute = new HashMap<String, String>();
        transRowsMainGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.CHILDREN_CLASSES_ATTR, childrenStyleClassAttribute);
        List<Pair<String, Pair<String, String>>> footerSums = new ArrayList<Pair<String, Pair<String, String>>>();
        transRowsMainGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.FOOTER_SUMS_ATTR, footerSums);
        transRowsMainGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.FOOTER_ERROR_MESSAGES, getFooterMessages());

        transRowsScrollGroup.getChildren().add(transRowsMainGrid);

        List transMainGridChildren = transRowsMainGrid.getChildren();

        // transaction rows
        int transRowCounter = 0;
        String rowClasses = "";
        final String rowClassesEvenValue = "trans-recordSetRow2,trans-subrow,"; // transaction list row and subrow classes
        final String rowClassesOddValue = "trans-recordSetRowAlt2,trans-subrowAlt,";
        Set<String> generalMandatoryFields = getMandatoryFields();
        if (isSumCalculated()) {
            addHiddenTaxCodePercentages(context, transExpandingGroup.getChildren());
        }
        for (Transaction transaction : transactions) {
            Set<String> transMandatoryProps = getCostManagerMandatoryFields(transaction);
            transMandatoryProps.addAll(generalMandatoryFields);
            // First row inputs
            HtmlOutputLink outputLink = createOutputLink(application, transMainGridChildren, transRowCounter);
            childrenStyleClassAttribute.put(outputLink.getId(), "trans-toggle-subrow");
            if (isSumCalculated()) {
                addOriginalTaxInfo(transaction, transRowCounter);
            }
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_FUNDS_CENTERS, TransactionModel.Props.FUNDS_CENTER, transRowCounter, "width160 ",
                    transMandatoryProps, rowAdded);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_COST_CENTERS, TransactionModel.Props.COST_CENTER, transRowCounter, "width120 ",
                    transMandatoryProps, rowAdded);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_FUNDS, TransactionModel.Props.FUND, transRowCounter, "width120 ",
                    transMandatoryProps, rowAdded);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_COMMITMENT_ITEM, TransactionModel.Props.EA_COMMITMENT_ITEM, transRowCounter, "width120 ",
                    DimensionSelectorGenerator.EA_PREFIX_INCLUDE_FILTER_KEY, transMandatoryProps, rowAdded);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_COMMITMENT_ITEM, TransactionModel.Props.COMMITMENT_ITEM, transRowCounter, "width190 ",
                    DimensionSelectorGenerator.EA_PREFIX_EXCLUDE_FILTER_KEY, transMandatoryProps, rowAdded);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_INTERNAL_ORDERS, TransactionModel.Props.ORDER_NUMBER, transRowCounter, "width140 ",
                    transMandatoryProps, rowAdded);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS, TransactionModel.Props.ASSET_INVENTORY_NUMBER, transRowCounter,
                    "width120 ", transMandatoryProps, rowAdded);
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
                    transMandatoryProps, rowAdded);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_ACCOUNTS, TransactionModel.Props.ACCOUNT, transRowCounter, "width120 ",
                    transMandatoryProps, rowAdded);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.TAX_CODE_ITEMS, TransactionModel.Props.INVOICE_TAX_CODE,
                    transRowCounter, "width40 " + TRANS_ROW_VAT_CODE_INPUT_CLASS, transMandatoryProps, rowAdded);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_TRADING_PARTNER_CODES, TransactionModel.Props.TRADING_PARTNER_CODE, transRowCounter,
                    "width80 ", transMandatoryProps, rowAdded);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_FUNCTIONAL_AREA_CODE, TransactionModel.Props.FUNCTIONAL_ARE_CODE, transRowCounter,
                    "width80 ", transMandatoryProps, rowAdded);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_CASH_FLOW_CODES, TransactionModel.Props.CASH_FLOW_CODE, transRowCounter, "width50 ",
                    transMandatoryProps, rowAdded);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_SOURCE_CODES, TransactionModel.Props.SOURCE, transRowCounter, "width40 ",
                    transMandatoryProps, rowAdded);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_PAYMENT_METHOD_CODES, TransactionModel.Props.PAYMENT_METHOD, transRowCounter,
                    "width40 ", transMandatoryProps, rowAdded);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_HOUSE_BANK_CODES, TransactionModel.Props.HOUSE_BANK, transRowCounter, "width70 ",
                    transMandatoryProps, rowAdded);
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

        if (isManageTransactionTemplates()) {
            addTemplateSaveAs(context, application, transactionPanel, listId);
        }

        return transRowsMainGrid;

    }

    private List<String> getFooterMessages() {
        List<String> messages = new ArrayList<String>();
        messages.add(MessageUtil.getMessage("transactions_footer_sum_error_message"));
        messages.add(MessageUtil.getMessage("transactions_footer_vat_error_message"));
        return messages;
    }

    protected void addOriginalTaxInfo(Transaction transaction, int transRowCounter) {
        List<SelectItem> selectItems = getTaxOriginalInfoSelectItems();
        Map<QName, Serializable> origProps = originalProperties.get(transaction.getNode().getNodeRef());
        if (origProps != null) {
            Serializable originalTaxCode = origProps.get(TransactionModel.Props.INVOICE_TAX_CODE);
            Serializable originalTaxPercentage = origProps.get(TransactionModel.Props.INVOICE_TAX_PERCENT);
            String originalTaxCodeStr = originalTaxCode != null ? originalTaxCode.toString() : "";
            String originalTaxPercentageStr = originalTaxPercentage != null ? originalTaxPercentage.toString() : "";
            selectItems.add(new SelectItem(String.valueOf(transRowCounter), originalTaxCodeStr + ORIGINAL_TAX_OPTION_SEPARATOR + originalTaxPercentageStr));
        }
    }

    protected boolean isSumCalculated() {
        return false;
    }

    protected void addHiddenTaxCodePercentages(FacesContext context, List siblings) {
        GeneralSelectorGenerator selectorGenerator = new GeneralSelectorGenerator();
        HtmlSelectOneMenu transTaxPercentageSelector = (HtmlSelectOneMenu) selectorGenerator.generateSelectComponent(context, TRANS_TAX_CODE_SELECTOR, false);
        selectorGenerator.getCustomAttributes().put("selectionItems", "#{" + getBeanName() + ".getTaxCodeSelectItems}");
        selectorGenerator.setupSelectComponent(context, null, null, null, transTaxPercentageSelector, false);
        transTaxPercentageSelector.setStyle(INLINE_STYLE_DISPLAY_NONE);
        transTaxPercentageSelector.setStyleClass(TRANS_TAX_CODE_SELECTOR);
        siblings.add(transTaxPercentageSelector);
        HtmlSelectOneMenu transOriginalTaxInfoSelector = (HtmlSelectOneMenu) selectorGenerator.generateSelectComponent(context, TRANS_TAX_ORIGINAL_INFO_SELECTOR, false);
        selectorGenerator.getCustomAttributes().put("selectionItems", "#{" + getBeanName() + ".getTaxOriginalInfoSelectItems}");
        selectorGenerator.setupSelectComponent(context, null, null, null, transOriginalTaxInfoSelector, false);
        transOriginalTaxInfoSelector.setStyle(INLINE_STYLE_DISPLAY_NONE);
        transOriginalTaxInfoSelector.setStyleClass(TRANS_TAX_ORIGINAL_INFO_SELECTOR);
        siblings.add(transOriginalTaxInfoSelector);
    }

    public List<SelectItem> getTaxCodeSelectItems(FacesContext context, UIInput input) {
        return getTaxCodeSelectItems();
    }

    private List<SelectItem> getTaxCodeSelectItems() {
        List<DimensionValue> dimensionValues = BeanHelper.getEInvoiceService().getAllDimensionValuesFromCache(
                BeanHelper.getEInvoiceService().getDimension(Dimensions.TAX_CODE_ITEMS));
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (DimensionValue dimensionValue : dimensionValues) {
            selectItems.add(new SelectItem(dimensionValue.getValueName(), dimensionValue.getValue()));
        }
        return selectItems;
    }

    public List<SelectItem> getTaxOriginalInfoSelectItems(FacesContext context, UIInput input) {
        return getTaxOriginalInfoSelectItems();
    }

    protected List<SelectItem> getTaxOriginalInfoSelectItems() {
        if (taxOriginalInfoSelectItems == null) {
            taxOriginalInfoSelectItems = new ArrayList<SelectItem>();
        }
        return taxOriginalInfoSelectItems;
    }

    @SuppressWarnings("unchecked")
    private void addTemplateSaveAs(FacesContext context, Application application, UIPanel transactionPanel, String listId) {
        UIPanel tranSaveAsTemplatePanel = (UIPanel) application.createComponent("org.alfresco.faces.Panel");
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
        suggesterGenerator.getCustomAttributes().put(SuggesterGenerator.ComponentAttributeNames.SUGGESTER_VALUES, "#{" + getBeanName() + ".getActiveTransactionTemplateNames}");
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
        saveAsButton.setOnclick("setPageScrollY();");
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
        List<UIComponent> children = ComponentUtil.getChildren(transTemplateSelectGroup);
        children.add(selectTemplatelabel);

        GeneralSelectorGenerator selectorGenerator = new GeneralSelectorGenerator();
        HtmlSelectOneMenu transTemplateSelector = (HtmlSelectOneMenu) selectorGenerator.generateSelectComponent(context, SELECT_TEMPLATE_NAME, false);
        selectorGenerator.getCustomAttributes().put("selectionItems", "#{" + getBeanName() + ".getTransactionTemplates}");
        selectorGenerator.setupSelectComponent(context, null, null, null, transTemplateSelector, false);
        transTemplateSelector.setId(TRANS_TEMPLATE_SELECTOR);
        ComponentUtil.addOnchangeJavascript(transTemplateSelector);
        children.add(transTemplateSelector);

        // hidden link for submitting form when transTemplateSelector onchange event occurs
        ComponentUtil.addOnchangeClickLink(application, children, "#{" + getBeanName() + ".copyFromTemplate}", TRANS_COMPONENT_ID_PREFIX + "trans-select-template-link-" + listId);

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
        columnActions.setRendererType(HtmlGroupCustomRenderer.HTML_GROUP_CUSTOM_RENDERER_TYPE);

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
    private void addDimensionSelector(FacesContext context, List siblings, Dimensions dimensions, QName propName, int transactionIndex, String styleClass,
            Set<String> mandatoryProps, boolean rowAdded) {
        addDimensionSelector(context, siblings, dimensions, propName, transactionIndex, styleClass, null, mandatoryProps, rowAdded);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addDimensionSelector(FacesContext context, List siblings, Dimensions dimensions, QName propName, int transactionIndex, String styleClass,
            String predefinedFilterName,
            Set<String> mandatoryProps,
            boolean rowAdded) {
        UIComponent dimensionSelector;
        if (isInEditMode()) {
            final HtmlPanelGroup dimensionSelectorPanel = (HtmlPanelGroup) context.getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            dimensionSelectorPanel.setId(TRANS_COMPONENT_ID_PREFIX + "trans-panel-" + propName.getLocalName() + "-" + transactionIndex);
            dimensionSelectorPanel.setRendererType(HtmlGroupCustomRenderer.HTML_GROUP_CUSTOM_RENDERER_TYPE);
            DimensionSelectorGenerator dimensionGenerator;
            if (predefinedFilterName != null) {
                dimensionGenerator = new DimensionSelectorGenerator(DimensionSelectorGenerator.predefinedFilters.get(predefinedFilterName));
            } else {
                dimensionGenerator = new DimensionSelectorGenerator();
            }
            dimensionGenerator.setSelectedValue((String) transactions.get(transactionIndex).getNode().getProperties().get(propName));
            String dimensionName = dimensions.getDimensionName();
            dimensionGenerator.getCustomAttributes().put(DimensionSelectorGenerator.ATTR_DIMENSION_NAME, dimensionName);
            dimensionGenerator.getCustomAttributes().put(DimensionSelectorGenerator.ATTR_GENERATE_DIMENSION_VALUES, Boolean.valueOf(transactionIndex == 0).toString());
            dimensionGenerator.setEntryDate(getEntryDate());

            dimensionSelector = dimensionGenerator.generate(context, null);
            Map componentAttributes = dimensionSelector.getAttributes();
            // The following line must execute before setupSelectComponent(), because the latter needs a value binding.
            setIdAndValueBinding(context, transactionIndex, propName, dimensionSelector);

            componentAttributes.put(CustomAttributeNames.STYLE_CLASS, styleClass == null ? "expand19-200 width120 tooltip " : styleClass + " expand19-200 tooltip ");
            if (predefinedFilterName != null) {
                componentAttributes.put(DimensionSelectorGenerator.ATTR_PREDEFINED_FILTER_NAME, predefinedFilterName);
            }
            componentAttributes.put("displayMandatoryMark", true);
            if (transactionIndex == transactions.size() - 1 && rowAdded) {
                componentAttributes.put(DimensionSelectorGenerator.ATTR_USE_DFAULT_VALUE, Boolean.TRUE);
            } else {
                componentAttributes.remove(DimensionSelectorGenerator.ATTR_USE_DFAULT_VALUE);
            }

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
            dimensionSelector.getAttributes().put("styleClass", "tooltip");
            siblings.add(dimensionSelector);
        }
    }

    protected Date getEntryDate() {
        return null;
    }

    private void addMandatorySpan(FacesContext context, QName propName, int transactionIndex, final UIComponent parent) {
        UIOutput span = (UIOutput) context.getApplication().createComponent(UIOutput.COMPONENT_TYPE);
        span.setId("trans-panel-mandatory-" + propName.getLocalName() + "-" + transactionIndex);
        span.setValue(" *");
        ComponentUtil.putAttribute(span, "style", "color: red;");
        ComponentUtil.addChildren(parent, span);
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
        doubleInput.getAttributes().put(CustomAttributeNames.STYLE_CLASS, "margin-left-4 width120 " + TRANS_ROW_SUM_INPUT_CLASS);
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
        textareaInput.getAttributes().put(CustomAttributeNames.STYLE_CLASS, "expand19-200 medium " + TRANS_ROW_ENTRY_CONTENT_INPUT_CLASS);
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
            constructTransactionPanelGroup(transactionPanelGroup, false);
            taskPanelControlNodeRef = getParentNodeRef();
        }
        this.transactionPanelGroup = transactionPanelGroup;
    }

}
