package ee.webmedia.alfresco.parameters.web;

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
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.parameters.model.Parameter;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.simdhs.CSVExporter;
import ee.webmedia.alfresco.simdhs.DataReader;
import ee.webmedia.alfresco.simdhs.EscapingCSVExporter;
import ee.webmedia.alfresco.simdhs.RichListDataReader;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;

public class ParametersListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient ParametersService parametersService;

    private List<Parameter<?>> parameters;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        restored();
    }

    @Override
    public void restored() {
        parameters = getParametersService().getAllParameters();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
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
        parameters = null;
        return super.cancel();
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
        // Erko hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();        
    }

    @Override
    public Object getActionsContext() {
        return null;
    }

    // START: getters / setters
    /**
     * Used in JSP page to create table rows
     */
    public List<Parameter<?>> getParameters() {
        return parameters;
    }

    public void setParametersService(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    public ParametersService getParametersService() {
        if (parametersService == null) {
            parametersService = (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(ParametersService.BEAN_NAME);
        }
        return parametersService;
    }
    // END: getters / setters

}