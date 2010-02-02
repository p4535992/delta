package ee.webmedia.alfresco.parameters.web;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.Application;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.ui.common.Utils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.parameters.model.Parameter;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.MessageUtil;

public class ParametersListDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;

    private transient ParametersService parametersService;

    private List<Parameter<?>> parameters;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
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
            getParametersService().updateParameters(parameters);
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
