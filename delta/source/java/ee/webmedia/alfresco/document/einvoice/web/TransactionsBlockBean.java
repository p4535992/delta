package ee.webmedia.alfresco.document.einvoice.web;

import static ee.webmedia.alfresco.utils.ComponentUtil.addChildren;
import static ee.webmedia.alfresco.utils.ComponentUtil.createUIParam;
import static ee.webmedia.alfresco.utils.ComponentUtil.putAttribute;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.generator.BaseComponentGenerator.CustomAttributeNames;
import org.alfresco.web.bean.generator.TextAreaGenerator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.common.component.UIPanel;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.renderkit.JSFAttr;

import ee.webmedia.alfresco.common.propertysheet.converter.DoubleCurrencyConverter;
import ee.webmedia.alfresco.common.propertysheet.dimensionselector.DimensionSelectorGenerator;
import ee.webmedia.alfresco.common.propertysheet.renderkit.HtmlGridCustomChildAttrRenderer;
import ee.webmedia.alfresco.common.propertysheet.renderkit.HtmlGroupCustomRenderer;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.DimensionValue;
import ee.webmedia.alfresco.document.einvoice.model.Dimensions;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.model.TransactionModel;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.RepoUtil;

public class TransactionsBlockBean implements Serializable {

    private static final String EA_PREFIX = "EA";
    public static final String TAX_CODE_ATTR = "taxCode";
    private static final String TRANS_COMPONENT_ID_PREFIX = "trans-";
    private static final DecimalFormat df = new DecimalFormat("#,##0.00");

    private static final long serialVersionUID = 1L;

    private static final String TRANSACTION_INDEX = "transIndex";

    private static final List<String> mainHeadingKeys = new ArrayList<String>(Arrays.asList("", "transaction_fundsCenter", "transaction_costCenter", "transaction_fund",
            "transaction_eaCommitmentItem", "transaction_commitmentItem", "transaction_orderNumber", "transaction_assetInventaryNumber", "transaction_sumWithoutVat", ""));
    private static final List<String> secondaryHeadingKeys = new ArrayList<String>(Arrays.asList("transaction_postingKey", "transaction_account",
            "transaction_invoiceTaxCode", "transaction_traidingPartnerCode", "transaction_functionalAreaCode", "transaction_cashFlowCode", "transaction_source",
            "transaction_paymentMethod", "transaction_houseBank", "transaction_entryContent"));

    private Node document;
    private List<Transaction> transactions;
    private List<Transaction> removedTransactions = new ArrayList<Transaction>();
    private transient HtmlPanelGroup transactionPanelGroup;
    private NodeRef taskPanelControlDocument;
    private DocumentDialog documentDialog;

    public void init(Node node, DocumentDialog documentDialog) {
        this.documentDialog = documentDialog;
        reset();
        document = node;
        restore();
    }

    public void restore() {
        transactions = BeanHelper.getEInvoiceService().getInvoiceTransactions(document.getNodeRef());
        constructTransactionPanelGroup();
    }

    public void save() {
        if (validate()) {
            BeanHelper.getEInvoiceService().updateTransactions(document.getNodeRef(), transactions, removedTransactions);
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

    private Transaction getNewUnsavedTransaction(Map<QName, Serializable> props) {
        return new Transaction(BeanHelper.getGeneralService().createNewUnSaved(TransactionModel.Types.TRANSACTION, props));
    }

    public void reset() {
        document = null;
        transactions = null;
        removedTransactions = new ArrayList<Transaction>();
        transactionPanelGroup = null;
    }

    public void onModeChanged() {
        constructTransactionPanelGroup(getTransactionPanelGroupInner());
    }

    private void constructTransactionPanelGroup() {
        constructTransactionPanelGroup(getTransactionPanelGroupInner());
    }

    private boolean isInEditMode() {
        if (documentDialog != null) {
            return documentDialog.isInEditMode();
        }
        return false;
    }

    /**
     * Action listener for JSP.
     */
    public void addTransaction(ActionEvent event) {
        transactions.add(getNewUnsavedTransaction());
        updateTransactionPanelGroup();
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
    }

    /**
     * Action listener for JSP.
     */
    public void removeTransaction(ActionEvent event) {
        int transIndex = Integer.parseInt(ActionUtil.getParam(event, TRANSACTION_INDEX));
        removedTransactions.add(transactions.remove(transIndex));
        updateTransactionPanelGroup();
    }

    private void updateTransactionPanelGroup() {
        getTransactionPanelGroupInner().getChildren().clear();
        constructTransactionPanelGroup();
    }

    private void constructTransactionPanelGroup(HtmlPanelGroup panelGroup) {
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
        final String rowClassesValue = "trans-recordSetRowAlt2,trans-subrow,"; // transaction list row and subrow classes
        final String styleDisplayNone = "display: none;";
        for (Transaction transaction : transactions) {
            // First row inputs
            HtmlOutputLink outputLink = createOutputLink(application, transMainGridChildren, transRowCounter);
            childrenStyleClassAttribute.put(outputLink.getId(), "trans-toggle-subrow");
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_FUNDS_CENTERS, TransactionModel.Props.FUNDS_CENTER, transRowCounter);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_COST_CENTERS, TransactionModel.Props.COST_CENTER, transRowCounter);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_FUNDS, TransactionModel.Props.FUND, transRowCounter);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_COMMITMENT_ITEM, TransactionModel.Props.EA_COMMITMENT_ITEM, transRowCounter,
                    getEAExclusivePredicate());
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_COMMITMENT_ITEM, TransactionModel.Props.COMMITMENT_ITEM, transRowCounter,
                    getEAInclusivePredicate());
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_INTERNAL_ORDERS, TransactionModel.Props.ORDER_NUMBER, transRowCounter);
            addDimensionSelector(context, transMainGridChildren, Dimensions.INVOICE_ASSET_INVENTORY_NUMBERS, TransactionModel.Props.ASSET_INVENTARY_NUMBER,
                    transRowCounter);
            addDoubleInput(context, transMainGridChildren, transRowCounter, TransactionModel.Props.SUM_WITHOUT_VAT);
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
            tranRowSecondaryGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.HEADING_KEYS_ATTR, secondaryHeadingKeys);
            childrenColspanAttribute.put(transSecondaryRowId, mainGridColumnCount);

            transMainGridChildren.add(tranRowSecondaryGrid);

            List tranRowSecondaryGridChildren = tranRowSecondaryGrid.getChildren();

            // Second row inputs
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_POSTING_KEY, TransactionModel.Props.POSTING_KEY, transRowCounter);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_ACCOUNTS, TransactionModel.Props.ACCOUNT, transRowCounter, "small");
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.TAX_CODE_ITEMS, TransactionModel.Props.INVOICE_TAX_CODE, transRowCounter, "small");
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_TRADING_PARTNER_CODES, TransactionModel.Props.TRADING_PARTNER_CODE,
                    transRowCounter);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_FUNCTIONAL_AREA_CODE, TransactionModel.Props.FUNCTIONAL_ARE_CODE, transRowCounter);
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_CASH_FLOW_CODES, TransactionModel.Props.CASH_FLOW_CODE, transRowCounter, "small");
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_SOURCE_CODES, TransactionModel.Props.SOURCE, transRowCounter, "small");
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_PAYMENT_METHOD_CODES, TransactionModel.Props.PAYMENT_METHOD, transRowCounter,
                    "small");
            addDimensionSelector(context, tranRowSecondaryGridChildren, Dimensions.INVOICE_HOUSE_BANK_CODES, TransactionModel.Props.HOUSE_BANK, transRowCounter);
            addTextareaInput(context, tranRowSecondaryGridChildren, transRowCounter, TransactionModel.Props.ENTRY_CONTENT);

            rowClasses = rowClasses + rowClassesValue;
            transRowCounter++;
        }
        transRowsMainGrid.getAttributes().put(JSFAttr.ROW_CLASSES_ATTR, rowClasses);

        // footer elements
        if (isInEditMode()) {
            createAddTransLink(application, listId, transRowsMainGrid.getFacets());
        }

        double sumWithoutVatValue = getSumWithoutVat();
        double vatSumValue = getVatSum();

        footerSums.add(new Pair<String, Pair<String, String>>(MessageUtil.getMessage("transactions_sumWithoutVat"), new Pair<String, String>(df.format(sumWithoutVatValue),
                null)));
        footerSums.add(new Pair<String, Pair<String, String>>(MessageUtil.getMessage("transactions_total_vatSum"), new Pair<String, String>(df.format(vatSumValue), null)));
        Double totalSum = (Double) document.getProperties().get(DocumentSpecificModel.Props.TOTAL_SUM);
        double transTotalSum = sumWithoutVatValue + vatSumValue;
        String color = totalSum != null && Math.abs(totalSum - transTotalSum) > 0.001 ? "red" : null;
        footerSums.add(new Pair<String, Pair<String, String>>(MessageUtil.getMessage("transactions_sumWithVat"), new Pair<String, String>(df.format(transTotalSum), color)));

    }

    private double getSumWithoutVat() {
        BigDecimal sum = new BigDecimal("0.0");
        for (Transaction transaction : transactions) {
            Double rowSumWithoutVat = transaction.getSumWithoutVat();
            if (rowSumWithoutVat != null) {
                sum = sum.add(new BigDecimal(rowSumWithoutVat));
            }
        }
        return sum.doubleValue();
    }

    private double getVatSum() {
        BigDecimal sum = new BigDecimal("0.0");
        for (Transaction transaction : transactions) {
            Double rowSumWithoutVat = transaction.getSumWithoutVat();
            Integer rowVatPercentage = getVatPercentage(transaction.getInvoiceTaxCode());
            if (rowSumWithoutVat != null) {
                sum = sum.add((new BigDecimal(rowSumWithoutVat)).multiply(new BigDecimal(rowVatPercentage)).divide(new BigDecimal(100)));
            }
        }
        return sum.doubleValue();
    }

    private Integer getVatPercentage(String invoiceTaxCode) {
        for (DimensionValue dimensionValue : BeanHelper.getEInvoiceService().getVatCodeDimensionValues()) {
            if (dimensionValue.getValueName().equalsIgnoreCase(invoiceTaxCode)) {
                try {
                    return Integer.parseInt(dimensionValue.getValue());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

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
        span.setValue("|");
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

    private HtmlPanelGroup createTransActions(Application application, String listId, int transRowCounter) {
        final HtmlPanelGroup columnActions = (HtmlPanelGroup) application.createComponent(HtmlPanelGroup.COMPONENT_TYPE);
        columnActions.setId(TRANS_COMPONENT_ID_PREFIX + "actions-" + listId + "-" + transRowCounter);

        if (isInEditMode()) {
            // delete transaction
            final UIActionLink transDeleteLink = (UIActionLink) application.createComponent("org.alfresco.faces.ActionLink");
            transDeleteLink.setId(TRANS_COMPONENT_ID_PREFIX + "remove-" + listId + "-" + transRowCounter);
            transDeleteLink.setValue("");
            transDeleteLink.setTooltip(MessageUtil.getMessage("transactions_delete_transaction"));
            transDeleteLink.setActionListener(application.createMethodBinding("#{TransactionsBlockBean.removeTransaction}",
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
            transCopyLink.setActionListener(application.createMethodBinding("#{TransactionsBlockBean.copyTransaction}",
                    UIActions.ACTION_CLASS_ARGS));
            transCopyLink.setShowLink(false);
            putAttribute(transCopyLink, "styleClass", "icon-link margin-left-4 version_history");
            addChildren(transCopyLink, createTransIndexParam(transRowCounter, application));
            columnActions.getChildren().add(transCopyLink);
        }

        return columnActions;
    }

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

    private MethodBinding createAddTransMethodBinding(Application application) {
        return application.createMethodBinding("#{TransactionsBlockBean.addTransaction}", UIActions.ACTION_CLASS_ARGS);
    }

    private HtmlOutputLink createOutputLink(Application application, List transactionGridChildren, int transactionIndex) {
        HtmlOutputLink outputLink = (HtmlOutputLink) application.createComponent(HtmlOutputLink.COMPONENT_TYPE);
        outputLink.setValue("#");
        outputLink.setId(TRANS_COMPONENT_ID_PREFIX + "expand-" + transactionIndex);
        transactionGridChildren.add(outputLink);
        return outputLink;
    }

    private void addDimensionSelector(FacesContext context, List siblings, Dimensions dimensions, QName propName, int transactionIndex) {
        addDimensionSelector(context, siblings, dimensions, propName, transactionIndex, null, null);
    }

    private void addDimensionSelector(FacesContext context, List siblings, Dimensions dimensions, QName propName, int transactionIndex, Predicate filter) {
        addDimensionSelector(context, siblings, dimensions, propName, transactionIndex, null, filter);
    }

    private void addDimensionSelector(FacesContext context, List siblings, Dimensions dimensions, QName propName, int transactionIndex, String styleClass) {
        addDimensionSelector(context, siblings, dimensions, propName, transactionIndex, styleClass, null);
    }

    private void addDimensionSelector(FacesContext context, List siblings, Dimensions dimensions, QName propName, int transactionIndex, String styleClass, Predicate filter) {
        UIComponent dimensionSelector;
        if (isInEditMode()) {
            DimensionSelectorGenerator dimensionGenerator = new DimensionSelectorGenerator();
            dimensionSelector = dimensionGenerator.generateSelectComponent(context, null, false);
            dimensionGenerator.getCustomAttributes().put(DimensionSelectorGenerator.ATTR_DIMENSION_NAME, dimensions.getDimensionName());
            dimensionGenerator.setupSelectComponent(context, null, null, null, dimensionSelector, false);
            dimensionSelector.getAttributes().put(CustomAttributeNames.STYLE_CLASS, styleClass == null ? "width120" : styleClass);
        } else {
            dimensionSelector = context.getApplication().createComponent(UIOutput.COMPONENT_TYPE);
        }
        setIdAndValueBinding(context, transactionIndex, propName, dimensionSelector);
        siblings.add(dimensionSelector);
    }

    private Predicate getEAInclusivePredicate() {
        return getEAPredicate(false);
    }

    private Predicate getEAExclusivePredicate() {
        return getEAPredicate(true);
    }

    private Predicate getEAPredicate(final boolean negate) {
        return new Predicate() {
            @Override
            public boolean evaluate(Object arg0) {
                String valueName = ((DimensionValue) arg0).getValueName();
                return negate ? !StringUtils.startsWith(valueName, EA_PREFIX) : StringUtils.startsWith(valueName, EA_PREFIX);
            }
        };
    }

    private void addDoubleInput(FacesContext context, List siblings, int transactionIndex, QName propName) {
        UIComponent doubleInput;
        if (isInEditMode()) {
            doubleInput = context.getApplication().createComponent(HtmlInputText.COMPONENT_TYPE);
            ((HtmlInputText) doubleInput).setConverter(new DoubleCurrencyConverter());
            setIdAndValueBinding(context, transactionIndex, propName, doubleInput);
            doubleInput.getAttributes().put("maxlength", 16);
        } else {
            doubleInput = context.getApplication().createComponent(UIOutput.COMPONENT_TYPE);
            Double value = (Double) transactions.get(transactionIndex).getNode().getProperties().get(propName);
            ((UIOutput) doubleInput).setValue(value != null ? df.format(value.doubleValue()) : "");
        }
        doubleInput.getAttributes().put(CustomAttributeNames.STYLE_CLASS, "margin-left-4 width120");
        doubleInput.getAttributes().put("style", "text-align: right");
        siblings.add(doubleInput);
    }

    private void addTextareaInput(FacesContext context, List siblings, int transactionIndex, QName propName) {
        UIComponent textareaInput;
        String id = getComponentId(transactionIndex, propName);
        if (isInEditMode()) {
            TextAreaGenerator textAreaGenerator = new TextAreaGenerator();
            textareaInput = textAreaGenerator.generate(context, id);
            textareaInput.getAttributes().put("maxlength", 50);
            textareaInput.getAttributes().put("maxlength", 50);
        } else {
            textareaInput = context.getApplication().createComponent(UIOutput.COMPONENT_TYPE);
            textareaInput.setId(id);
            textareaInput.getAttributes().put("style", "whitespace: normal;");
        }
        setValueBinding(context, transactionIndex, propName, textareaInput);
        textareaInput.getAttributes().put(CustomAttributeNames.STYLE_CLASS, "expand19-200 medium");
        siblings.add(textareaInput);
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
        return "#{TransactionsBlockBean.transactions[" + transactionIndex + "].node.properties[\"" + propName + "\"]}";
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    /**
     * NB! Don't call this method from java code; this is meant ONLY for transaction-block.jsp binding.
     * Use getTransactionPanelGroupInner() instead.
     */
    public HtmlPanelGroup getTransactionPanelGroup() {
        if (transactionPanelGroup == null) {
            transactionPanelGroup = new HtmlPanelGroup();
        }
        taskPanelControlDocument = document.getNodeRef();
        return transactionPanelGroup;
    }

    private HtmlPanelGroup getTransactionPanelGroupInner() {
        // This will be called once in the first RESTORE VIEW phase.
        if (transactionPanelGroup == null) {
            transactionPanelGroup = new HtmlPanelGroup();
        }
        return transactionPanelGroup;
    }

    public void setTransactionPanelGroup(HtmlPanelGroup transactionPanelGroup) {
        if (taskPanelControlDocument != null && !taskPanelControlDocument.equals(document.getNodeRef())) {
            constructTransactionPanelGroup(transactionPanelGroup);
            taskPanelControlDocument = document.getNodeRef();
        }
        this.transactionPanelGroup = transactionPanelGroup;
    }

}
