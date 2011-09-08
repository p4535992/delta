package ee.webmedia.alfresco.functions.web;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.repo.importer.ACPImportPackageHandler;
import org.alfresco.repo.importer.ImportTimerProgress;
import org.alfresco.service.cmr.view.ImporterException;
import org.alfresco.service.cmr.view.ImporterService;
import org.alfresco.service.cmr.view.Location;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.AbstractImportDialog;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog for importing documents list
 * 
 * @author Ats Uiboupin
 */
public class DocumentListImportDialog extends AbstractImportDialog {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentListImportDialog.class);
    private static final long serialVersionUID = 1L;
    public String BEAN_NAME = "DocumentListImportDialog";

    private transient ImporterService importerService;
    private transient FunctionsService functionsService;

    public DocumentListImportDialog() {
        super(".acp", "docList_import_error_wrongExtension");
    }

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        reset();
    }

    @Override
    public String getFileUploadSuccessMsg() {
        return MessageUtil.getMessage("docList_import_beforeImport");
    }

    @Override
    public String getFinishButtonLabel() {
        return MessageUtil.getMessage("docList_import");
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        if (StringUtils.isBlank(getFileName())) {
            MessageUtil.addErrorMessage("docList_import_error_wrongExtension", getFileName());
            return null;
        }
        if (file == null) {
            MessageUtil.addErrorMessage("docList_import_error_noFile");
            return outcome;
        }
        if (!isCorrectExtension(getFileName())) {
            MessageUtil.addErrorMessage("docList_import_error_wrongExtension", getFileName());
            return outcome;
        }
        log.info("Starting to import docList");
        final ACPImportPackageHandler importHandler = new ACPImportPackageHandler(file, AppConstants.CHARSET);
        final Location location = getFunctionsService().getDocumentListLocation();
        try {
            getImporterService().importView(importHandler, location, null, new ImportTimerProgress(log));
            log.info("Finished importing docList");
            MessageUtil.addInfoMessage("docList_import_success", getFileName());
        } catch (ImporterException e) {
            log.error("Failed to import documents list", e);
            MessageUtil.addErrorMessage("docList_import_error_wrongFileContent", getFileName());
            // show file upload again
            reset();
            isFinished = false;
            return outcome;
        }
        return outcome;
    }

    // START: getters / setters

    protected FunctionsService getFunctionsService() {
        if (functionsService == null) {
            functionsService = (FunctionsService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(FunctionsService.BEAN_NAME);
        }
        return functionsService;
    }

    protected ImporterService getImporterService() {
        if (importerService == null) {
            importerService = (ImporterService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean("DocListImporterComponent");
        }
        return importerService;
    }
    // END: getters / setters
}
