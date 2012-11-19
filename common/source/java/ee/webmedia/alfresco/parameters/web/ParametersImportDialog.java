package ee.webmedia.alfresco.parameters.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.FileUploadBean;
import org.springframework.web.jsf.FacesContextUtils;

import com.csvreader.CsvReader;

import de.schlichtherle.io.FileInputStream;
import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.AbstractImportDialog;
import ee.webmedia.alfresco.parameters.model.Parameter;
import ee.webmedia.alfresco.parameters.model.Parameter.ImportStatus;
import ee.webmedia.alfresco.parameters.model.StringParameter;
import ee.webmedia.alfresco.parameters.service.ParametersService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

public class ParametersImportDialog extends AbstractImportDialog {

    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ParametersImportDialog.class);
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "ParametersImportDialog";

    private transient ParametersService parametersService;
    private Collection<Parameter<? extends Serializable>> changedParams;
    private Map<String, ParameterImportLineVo> paramsToImport;
    private List<Parameter<?>> paramsOverview;
    private boolean containsUnknownParameters;

    protected ParametersImportDialog() {
        super("csv", "parameter_import_csv_error_wrongExtension");
    }

    /**
     * @return null if parsing failed, otherwise map of parameter names and values
     */
    private Map<String/* paramName */, ParameterImportLineVo/* paramValue */> getParamsToImport() {
        FileUploadBean fileBean = getFileUploadBean();
        File upFile = fileBean.getFile();

        try {
            final FileInputStream fileInputStream = new FileInputStream(upFile);
            final InputStreamReader isReader = new InputStreamReader(fileInputStream, AppConstants.CHARSET);
            CsvReader csvReader = new CsvReader(isReader);
            csvReader.setDelimiter(';');
            csvReader.readHeaders();
            Map<String/* paramName */, ParameterImportLineVo/* paramValue */> paramsMap = new HashMap<String, ParameterImportLineVo>();
            final int expectedColumnsCount = 3;
            while (csvReader.readRecord()) {
                if (log.isDebugEnabled()) {
                    log.debug("import line: " + csvReader.getRawRecord());
                }
                checkColumnCount(csvReader, expectedColumnsCount);
                String paramName = csvReader.get(0);
                final String paramComment = csvReader.get(1);
                final String paramVal = csvReader.get(2);
                paramsMap.put(paramName, new ParameterImportLineVo(paramComment, paramVal));
            }
            csvReader.close();
            return paramsMap;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read data for importing parameters from uploaded file: '" + upFile.getAbsolutePath() + "'", e);
        }
    }

    private void checkColumnCount(CsvReader csvReader, final int expectedColumnsCount) {
        final int lineColumnCount = csvReader.getColumnCount();
        if (lineColumnCount != expectedColumnsCount) {
            final String rawRecord = csvReader.getRawRecord();
            if (log.isDebugEnabled()) {
                log.error( //
                "During importing parameters expected " + expectedColumnsCount + " columns, but got " + lineColumnCount + " from line:\n" + rawRecord);
            }
            final UnableToPerformException unableToPerformException = new UnableToPerformException( //
                    MessageSeverity.ERROR, "parameter_import_csv_error_wrongNumOfColumns");
            unableToPerformException.setMessageValuesForHolders(expectedColumnsCount, lineColumnCount, rawRecord);
            throw unableToPerformException;
        }
    }

    public List<Parameter<?>> getImportableParams() {
        if (paramsToImport == null) {
            try {
                paramsToImport = getParamsToImport();
            } catch (UnableToPerformException e) {
                // veateate näitamine ja sulgemine millegi pärast ei toimi
                MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
                return null;// problem while parsing the import file
            }
        } else {
            return paramsOverview;
        }
        paramsOverview = new ArrayList<Parameter<?>>(paramsToImport.size());
        final List<Parameter<?>> existingParameters = getParametersService().getAllParameters();
        final Map<String, Parameter<? extends Serializable>> paramsToUpdate = new HashMap<String, Parameter<?>>(existingParameters.size());
        for (Parameter<?> parameter : existingParameters) {
            final String paramName = parameter.getParamName();
            if (paramsToImport.containsKey(paramName)) {
                parameter.setPreviousParamValue();
                parameter.setPreviousParamDescription();
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
                stringParameter.setParamValue(paramsToImport.get(newParamName).value);
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
                final ParameterImportLineVo parameterImportLineVo = paramsToImport.get(paramName);
                importParamVal = parameterImportLineVo.value;
                existingParam.setParamValueFromString(importParamVal);
                existingParam.setParamDescription(parameterImportLineVo.comment);
                paramsOverview.add(existingParam);
                if (existingParam.getStatus().equals(ImportStatus.PARAM_NOT_CHANGED)) {
                    iterator.remove();
                } else {
                    log.debug("paramName is different");
                }
            } else {
                iterator.remove();
            }
            changedParams = new ArrayList<Parameter<? extends Serializable>>(paramsToUpdate.values()); // make HashMap$Values serializable
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
        if (changedParams.size() == 0) {
            log.info("Values in uploaded file contain no changes to existing parameters");
            MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "parameter_import_csv_info_noChanges", getFileName());
        } else {
            log.info("Starting to import parameters");
            try {
                getParametersService().updateParameters(changedParams);
            } catch (UnableToPerformException e) {
                final String msg = "failed to import parameters: ";
                if (log.isDebugEnabled()) {
                    log.error(msg + e.getMessage(), e);
                }
                MessageUtil.addStatusMessage(FacesContext.getCurrentInstance(), e);
                return null;
            }
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
        return getDefaultFinishOutcome();
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

    private static class ParameterImportLineVo {
        public ParameterImportLineVo(String comment, String value) {
            this.comment = comment;
            this.value = value;
        }

        private final String comment;
        private final String value;
    }
}
