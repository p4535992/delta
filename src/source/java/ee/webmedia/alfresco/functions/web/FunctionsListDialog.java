package ee.webmedia.alfresco.functions.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.exporter.ACPExportPackageHandler;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.view.ExportPackageHandler;
import org.alfresco.service.cmr.view.ExporterCrawlerParameters;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.io.IOUtils;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;

public class FunctionsListDialog extends BaseDialogBean {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(FunctionsListDialog.class);

    private static final long serialVersionUID = 1L;

    private transient FunctionsService functionsService;
    private transient ExporterService exporterService;
    protected List<Function> functions;

    @Override
    public void init(Map<String, String> params) {
        super.init(params);
        loadFunctions();
    }

    @Override
    public void restored() {
        loadFunctions();
    }

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Throwable {
        // save button not used or shown
        return null;
    }

    @Override
    public String cancel() {
        functions = null;
        return super.cancel();
    }

    /** @param event */
    public void export(ActionEvent event) {
        log.info("docList export started");
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.setCharacterEncoding(CHARSET);
        OutputStream outputStream = null;
        try {

            String packageName = FunctionsModel.Repo.FUNCTIONS_ROOT;
            File dataFile = new File(packageName);
            File contentDir = new File(packageName);

            outputStream = getExportOutStream(response);
            // setup an ACP Package Handler to export to an ACP file format
            MimetypeService mimetypeService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getMimetypeService();
            ExportPackageHandler handler = new ACPExportPackageHandler(outputStream, dataFile, contentDir, mimetypeService);

            // now export (note: we're not interested in progress in the example)
            getExporterService().exportView(handler, getExportParameters(), null);

            outputStream.flush();
        } catch (IOException e) {
            final String msg = "Failed to export classificators";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            FacesContext.getCurrentInstance().responseComplete();
            log.info("docList export completed");
        }
    }

    private ExporterCrawlerParameters getExportParameters() {
        ExporterCrawlerParameters parameters = new ExporterCrawlerParameters();
        parameters.setExportFrom(getFunctionsService().getDocumentListLocation());
        return parameters;
    }

    private OutputStream getExportOutStream(HttpServletResponse response) throws IOException {
        OutputStream outputStream;
        response.setContentType(MimetypeMap.MIMETYPE_BINARY);
        response.setHeader("Expires", "0");
        response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Pragma", "public");
        response.setHeader("Content-disposition", "attachment;filename=functions-list.acp");
        outputStream = response.getOutputStream();
        return outputStream;
    }

    @Override
    public Object getActionsContext() {
        return new Node(getFunctionsService().getFunctionsRoot());
    }

    // START: private methods
    protected void loadFunctions() {
        functions = getFunctionsService().getAllFunctions();
        Collections.sort(functions);
    }

    // END: private methods

    // START: getters / setters
    /**
     * Used in JSP pages.
     */
    public List<Function> getFunctions() {
        return functions;
    }

    protected FunctionsService getFunctionsService() {
        if (functionsService == null) {
            functionsService = (FunctionsService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(FunctionsService.BEAN_NAME);
        }
        return functionsService;
    }

    protected ExporterService getExporterService() {
        if (exporterService == null) {
            exporterService = (ExporterService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean("ExporterService");
        }
        return exporterService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }
    // END: getters / setters
}
