package ee.webmedia.alfresco.parameters.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getParametersService;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.Utils;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.einvoice.service.EInvoiceUtil;
import ee.webmedia.alfresco.parameters.model.Parameter;
import ee.webmedia.alfresco.parameters.model.Parameters;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.EscapingCSVExporter;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class ParametersListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    public static final String BEAN_NAME = "ParametersListDialog";

    private List<Parameter<? extends Serializable>> parameters;
    private static final Set<Parameters> IGNORED_PARAMETERS;

    static {
        IGNORED_PARAMETERS = new HashSet<>(Arrays.asList(
                Parameters.DIMENSION_DELETION_PERIOD,
                Parameters.DIMENSION_DELETION_TIME,
                Parameters.DVK_RECEIVE_DOCUMENTS_INVOICE_FOLDER,
                Parameters.INVOICE_ENTRY_DATE_ONLY_IN_CURRENT_YEAR,
                Parameters.REVIEW_WORKFLOW_COST_MANAGER_WORKFLOW_NUMBER,
                Parameters.SAP_CONTACT_UPDATE_TIME,
                Parameters.SAP_DVK_CODE,
                Parameters.SAP_FINANCIAL_DIMENSIONS_FOLDER_IN_DVK,
                Parameters.SEND_INVOICE_TO_DVK_FOLDER,
                Parameters.SEND_PURCHASE_ORDER_INVOICE_TO_DVK_FOLDER,
                Parameters.SEND_XXL_INVOICE_TO_DVK_FOLDER));
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        restored();
    }

    @Override
    public void restored() {
        ParametersService parametersService = getParametersService();
        parameters = parametersService.getAllParameters();
        filterEinvoiceParameters();
        if (!parametersService.isJobsEnabled()) {
            MessageUtil.addErrorMessage("parameters_jobs_not_rescheduled");
        }
    }

    private void filterEinvoiceParameters() {
        if (parameters == null || BeanHelper.getApplicationConstantsBean().isEinvoiceEnabled()) {
            return;
        }
        Set<Parameter<?>> parametersToRemove = new HashSet<>();
        for (Parameter<?> param : parameters) {
            Parameters parameter = Parameters.get(param);
            if (EInvoiceUtil.DIMENSION_PARAMETERS.containsKey(parameter) || IGNORED_PARAMETERS.contains(parameter)) {
                parametersToRemove.add(param);
            }
        }
        parameters.removeAll(parametersToRemove);
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (!getParametersService().isJobsEnabled()) {
            MessageUtil.addErrorMessage("parameters_jobs_not_rescheduled");
        }

        final Set<String> messages = new HashSet<String>(3); // there will probably be not more than 3 messages
        for (Parameter<?> parameter : parameters) {
            String validationMessage = parameter.validateValue();
            if (validationMessage != null) {
                messages.add(validationMessage);
            }
        }
        if (messages.size() > 0) {
            for (String message : messages) {
                Utils.addErrorMessage(Application.getMessage(context, message));
            }
        } else {
            try {
                getParametersService().updateParameters(parameters);
                MessageUtil.addInfoMessage("save_success");
            } catch (UnableToPerformException e) {
                MessageUtil.addStatusMessage(context, e);
            }
        }

        // We need to stay on the same dialog
        isFinished = false;
        return null;
    }

    @Override
    public String cancel() {
        clean();
        return super.cancel();
    }

    @Override
    public void clean() {
        parameters = null;
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    /** @param event */
    public void exportAsCsv(ActionEvent event) {
        DataReader dataReader = new RichListDataReader();
        CSVExporter exporter = new EscapingCSVExporter(dataReader);
        exporter.export("parametersList");
        // hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    /**
     * Used in JSP page to create table rows
     */
    public List<Parameter<?>> getParameters() {
        return parameters;
    }

}
