package ee.webmedia.alfresco.parameters.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.AlfrescoNavigationHandler;
import org.alfresco.web.bean.FileUploadBean;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.web.AbstractImportDialog;
import ee.webmedia.alfresco.parameters.model.Parameter;
import ee.webmedia.alfresco.parameters.model.StringParameter;
import ee.webmedia.alfresco.parameters.model.Parameter.ImportStatus;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.MessageUtil;

public class ParametersImportDialog extends AbstractImportDialog {

    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ParametersImportDialog.class);
    private static final long serialVersionUID = 1L;
    public String BEAN_NAME = "ParametersImportDialog";

    private transient ParametersService parametersService;
    private Collection<Parameter<? extends Serializable>> changedParams;
    private Map<String, String> paramsToImport;
    private List<Parameter<?>> paramsOverview;
    private boolean containsUnknownParameters;

    protected ParametersImportDialog() {
        super(".csv", "parameter_import_csv_error_wrongExtension");
    }

    /**
     * @return null if parsing failed, otherwise map of parameter names and values
     */
    private Map<String/* paramName */, String/* paramValue */> getParamsToImport() {
        FileUploadBean fileBean = getFileUploadBean();
        File upFile = fileBean.getFile();

        try {
            final BufferedReader br = new BufferedReader(new FileReader(upFile));
            Map<String/* paramName */, String/* paramValue */> paramsMap = new HashMap<String, String>();
            String csvLine;
            int lineNr = 0;
            while ((csvLine = br.readLine()) != null) {
                lineNr++;
                if (lineNr == 1) {
                    continue;
                }
                final int seppIndex = csvLine.indexOf(";");
                if (seppIndex < 1) {
                    final String msg = "Line doesn't contain parameter name. Line:\n'" + csvLine + "'";
                    log.error(msg);
                    final FacesContext context = FacesContext.getCurrentInstance();
                    // veateate näitamine ja sulgemine millegi pärast ei toimi
                    MessageUtil.addErrorMessage(context, "parameter_import_csv_error_wrongFileContent", getFileName());
                    context.getApplication().getNavigationHandler().handleNavigation(context, null, AlfrescoNavigationHandler.CLOSE_DIALOG_OUTCOME);
                    reset();
                    return null;
                }
                final String paramName = csvLine.substring(0, seppIndex);
                final String paramVal = csvLine.substring(seppIndex + 1);
                paramsMap.put(paramName, paramVal);
            }
            return paramsMap;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read data for importing parameters from uploaded file: '" + upFile.getAbsolutePath() + "'", e);
        }
    }

    public List<Parameter<?>> getImportableParams() {
        if (paramsToImport == null) {
            paramsToImport = getParamsToImport();
            if(paramsToImport == null) {
                return null;//problem while parsing the import file
            }
        } else {
            return paramsOverview;
        }
        paramsOverview = new ArrayList<Parameter<?>>(paramsToImport.size());
        final List<Parameter<?>> existingParameters = getParametersService().getAllParameters();
        final Map<String, Parameter<? extends Serializable>> paramsToUpdate = new HashMap<String, Parameter<?>>(existingParameters.size());
        for (Parameter<?> parameter : existingParameters) {
            final String paramName = parameter.getParamName();
            if(paramsToImport.containsKey(paramName)) {
                parameter.setPreviousParamValue();
                paramsToUpdate.put(parameter.getParamName(), parameter);
            }
        }
        final Set<String> existingParamNames = Collections.unmodifiableSet(paramsToUpdate.keySet());
        final Set<String> newParameterNames = new HashSet<String>(paramsToImport.keySet());
        newParameterNames.removeAll(existingParamNames);
        if (newParameterNames.size() > 0) {
            containsUnknownParameters = true;
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "parameter_import_csv_error_unknownParametersExist");
            for (String newParamName : newParameterNames) {
                final StringParameter stringParameter = new StringParameter(newParamName);
                stringParameter.setParamValue(paramsToImport.get(newParamName));
                paramsOverview.add(stringParameter);
            }
        }

        // iterate over existing parameters, remove all items, that have not changed, leaving only items that must be updated
        for (Iterator<Entry<String, Parameter<?>>> iterator = paramsToUpdate.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, Parameter<?>> entry = iterator.next();
            final String paramName = entry.getKey();
            final Parameter<?> existingParam = entry.getValue();

            String importParamVal = null;
            if (paramsToImport.containsKey(paramName)) {
                importParamVal = paramsToImport.get(paramName);
                existingParam.setParamValueFromString(importParamVal);
                paramsOverview.add(existingParam);
                if (existingParam.getStatus().equals(ImportStatus.PARAM_NOT_CHANGED)) {
                    iterator.remove();
                } else {
                    log.debug("paramName is different");
                }
            } else {
                iterator.remove();
            }
            this.changedParams = new ArrayList<Parameter<? extends Serializable>>(paramsToUpdate.values()); // make HashMap$Values serializable
        }
        Collections.sort(paramsOverview, new Comparator<Parameter<?>>() {

            @Override
            public int compare(Parameter<?> o1, Parameter<?> o2) {
                return o1.getStatus().compareTo(o2.getStatus());
            }

        });
        return paramsOverview;
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (containsUnknownParameters) {
            MessageUtil.addErrorMessage(FacesContext.getCurrentInstance(), "parameter_import_csv_error_unknownParametersExist");
            return null;
        }
        if(changedParams.size()==0) {
            log.info("Values in uploaded file contain no changes to existing parameters");
            MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "parameter_import_csv_info_noChanges", getFileName());
        } else {
            log.info("Starting to import parameters");
            getParametersService().updateParameters(changedParams);
            log.info("Finished importing parameters");
            MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "parameter_import_csv_success", changedParams.size(), getFileName());
        }
        return outcome;
    }

    @Override
    public String reset() {
        super.reset();
        changedParams = null;
        paramsToImport = null;
        paramsOverview = null;
        containsUnknownParameters = false;
        return "dialog:close";
    }

    @Override
    public String getFileUploadSuccessMsg() {
        return MessageUtil.getMessage("parameter_import_reviewChanges");
    }

    // START: getters / setters
    public ParametersService getParametersService() {
        if (parametersService == null) {
            parametersService = (ParametersService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())//
                    .getBean(ParametersService.BEAN_NAME);
        }
        return parametersService;
    }

    // END: getters / setters
}
