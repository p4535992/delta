package ee.webmedia.alfresco.document.einvoice.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.renderkit.HtmlGridCustomChildAttrRenderer;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.model.Transaction;
import ee.webmedia.alfresco.document.einvoice.model.TransactionTemplate;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.document.model.DocumentSpecificModel;
import ee.webmedia.alfresco.document.web.DocumentDialog;
import ee.webmedia.alfresco.utils.MessageUtil;

public class TransactionsBlockBean extends TransactionsTemplateDetailsDialog implements Serializable {

    private static final long serialVersionUID = 1L;

    private DocumentDialog documentDialog;

    @Override
    protected String getBeanName() {
        return "TransactionsBlockBean";
    }

    public void init(Node node, DocumentDialog documentDialog) {
        this.documentDialog = documentDialog;
        super.init(node);
    }

    public void restore(Node document) {
        setParentNode(document);
        super.restore();
    }

    public void onModeChanged() {
        constructTransactionPanelGroup(false);
    }

    @Override
    protected boolean isInEditMode() {
        if (documentDialog != null) {
            return documentDialog.isInEditMode() && !documentDialog.isClosedOrNotEditable();
        }
        return false;
    }

    @Override
    public boolean isShowTransactionTemplates() {
        return isInEditMode();
    }

    @Override
    public boolean isManageTransactionTemplates() {
        return isShowTransactionTemplates() && BeanHelper.getUserService().isInAccountantGroup();
    }

    @Override
    protected boolean isSumCalculated() {
        return getTransactions() != null && !getTransactions().isEmpty();
    }

    protected void addHiddenTaxCodePercentages(List list) {
    }

    @Override
    protected HtmlPanelGrid constructTransactionPanelGroup(HtmlPanelGroup panelGroup, boolean rowAdded) {
        final HtmlPanelGrid transRowsMainGrid = super.constructTransactionPanelGroup(panelGroup, rowAdded);
        addFooterSums(transRowsMainGrid);
        return transRowsMainGrid;
    }

    public List<SelectItem> getTransactionTemplates(FacesContext context, UIInput selectComponent) {
        setTransactionTemplates(BeanHelper.getEInvoiceService().getActiveTransactionTemplates());
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        selectItems.add(new SelectItem("", MessageUtil.getMessage("transactions_useTemplate")));
        for (TransactionTemplate transactionTemplate : getTransactionTemplates()) {
            selectItems.add(new SelectItem(transactionTemplate.getName(), transactionTemplate.getName()));
        }
        return selectItems;
    }

    @Override
    protected void addInvoiceMessages() {
        if (documentDialog != null) {
            documentDialog.getMeta().addInvoiceMessages();
        }
    }

    /**
     * jsp listener
     */
    public void saveAsTemplate(ActionEvent event) {
        UIInput component = (UIInput) event.getComponent().getParent().findComponent(SAVEAS_TEMPLATE_NAME);
        String templateName = (String) component.getValue();
        if (StringUtils.isBlank(templateName)) {
            MessageUtil.addErrorMessage("transactions_saveas_template_error_no_name");
            return;
        }
        TransactionTemplate template = BeanHelper.getEInvoiceService().getTransactionTemplateByName(templateName);
        if (template == null) {
            template = BeanHelper.getEInvoiceService().createTransactionTemplate(templateName);
        } else {
            BeanHelper.getEInvoiceService().removeTransactions(template.getNode().getNodeRef());
        }
        BeanHelper.getEInvoiceService().copyTransactions(template, getTransactions());
        constructTransactionPanelGroup(false);
    }

    /**
     * jsp listener
     */
    public void copyFromTemplate(ActionEvent event) {
        UIInput component = (UIInput) event.getComponent().getParent().findComponent(SELECT_TEMPLATE_NAME);
        String templateName = (String) component.getValue();
        if (StringUtils.isBlank(templateName)) {
            return;
        }
        TransactionTemplate template = BeanHelper.getEInvoiceService().getTransactionTemplateByName(templateName);
        if (template != null) {
            List<Transaction> transactions = getTransactions();
            getRemovedTransactions().addAll(transactions);
            transactions.clear();
            List<Transaction> templateTransactions = BeanHelper.getEInvoiceService().getInvoiceTransactions(template.getNode().getNodeRef());
            for (Transaction transaction : templateTransactions) {
                Map<QName, Serializable> newProps = new HashMap<QName, Serializable>();
                EInvoiceUtil.copyTransactionProperties(transaction, newProps);
                transactions.add(getNewUnsavedTransaction(newProps));
            }
        }
        constructTransactionPanelGroup(false);
    }

    @SuppressWarnings("unchecked")
    private void addFooterSums(final HtmlPanelGrid transRowsMainGrid) {
        double sumWithoutVatValue = EInvoiceUtil.getSumWithoutVat(getTransactions());
        double vatSumValue = EInvoiceUtil.getVatSum(getTransactions(), getOriginalProperties(), BeanHelper.getEInvoiceService().getVatCodeDimensionValues());

        List<Pair<String, Pair<String, String>>> footerSums = new ArrayList<Pair<String, Pair<String, String>>>();
        transRowsMainGrid.getAttributes().put(HtmlGridCustomChildAttrRenderer.FOOTER_SUMS_ATTR, footerSums);
        footerSums.add(new Pair<String, Pair<String, String>>(MessageUtil.getMessage("transactions_sumWithoutVat"), new Pair<String, String>(EInvoiceUtil.getInvoiceNumberFormat()
                .format(sumWithoutVatValue),
                null)));
        footerSums.add(new Pair<String, Pair<String, String>>(MessageUtil.getMessage("transactions_total_vatSum"), new Pair<String, String>(EInvoiceUtil.getInvoiceNumberFormat()
                .format(vatSumValue), null)));
        Double totalSum = (Double) getParentNode().getProperties().get(DocumentSpecificModel.Props.TOTAL_SUM);
        double transTotalSum = sumWithoutVatValue + vatSumValue;
        String color = totalSum != null && Math.abs(totalSum - transTotalSum) > 0.001 ? "red" : null;
        footerSums.add(new Pair<String, Pair<String, String>>(MessageUtil.getMessage("transactions_sumWithVat"), new Pair<String, String>(EInvoiceUtil.getInvoiceNumberFormat()
                .format(transTotalSum), color)));
    }

    @Override
    protected MethodBinding createAddTransMethodBinding(Application application) {
        return application.createMethodBinding("#{" + getBeanName() + ".addTransaction}", UIActions.ACTION_CLASS_ARGS);
    }

    @Override
    protected boolean isMandatory(QName propName, Set<String> transMandatoryProps) {
        return transMandatoryProps.contains(propName.getLocalName());
    }

    @Override
    protected Set<String> getCostManagerMandatoryFields(Transaction transaction) {
        Set<String> mandatoryFields = new HashSet<String>();
        Map<QName, Serializable> userProps = BeanHelper.getUserService().getCurrentUserProperties();
        @SuppressWarnings("unchecked")
        List<String> userRelatedFundsCenters = (List<String>) userProps.get(ContentModel.PROP_RELATED_FUNDS_CENTER);
        if (userRelatedFundsCenters == null) {
            return mandatoryFields;
        }
        for (String userRelatedFundsCenter : userRelatedFundsCenters) {
            if (transaction.getFundsCenter() != null && userRelatedFundsCenter != null && transaction.getFundsCenter().equalsIgnoreCase(userRelatedFundsCenter)) {
                mandatoryFields.addAll(BeanHelper.getEInvoiceService().getCostManagerMandatoryFields());
                break;
            }
        }
        return mandatoryFields;
    }

    @Override
    protected Set<String> getMandatoryFields() {
        Set<String> mandatoryFields = new HashSet<String>();
        mandatoryFields.addAll(BeanHelper.getEInvoiceService().getAccountantMandatoryFields());
        mandatoryFields.addAll(BeanHelper.getEInvoiceService().getOwnerMandatoryFields());
        return mandatoryFields;
    }

    public boolean checkTotalSum() {
        List<String> errorMessageKeys = new ArrayList<String>();
        boolean result = EInvoiceUtil.checkTotalSum(errorMessageKeys, "document_sendToSap_", (Double) getParentNode().getProperties().get(DocumentSpecificModel.Props.TOTAL_SUM),
                getTransactions(), getOriginalProperties(), true);
        for (String msgKey : errorMessageKeys) {
            MessageUtil.addErrorMessage(msgKey);
        }
        return result;
    }

    @Override
    protected Date getEntryDate() {
        return (Date) documentDialog.getMeta().getDocument().getProperties().get(DocumentSpecificModel.Props.ENTRY_DATE);
    }

}
