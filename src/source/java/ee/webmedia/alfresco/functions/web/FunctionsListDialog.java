package ee.webmedia.alfresco.functions.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.exporter.ACPExportPackageHandler;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.view.ExportPackageHandler;
import org.alfresco.service.cmr.view.ExporterCrawlerParameters;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.io.IOUtils;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.importer.excel.bootstrap.SmitExcelImporter;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.MessageUtil;

public class FunctionsListDialog extends BaseDialogBean {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(FunctionsListDialog.class);

    public static final String BEAN_NAME = "FunctionsListDialog";

    private static final long serialVersionUID = 1L;

    private transient FunctionsService functionsService;
    private transient ExporterService exporterService;
    private transient UserService userService;
    protected List<Function> functions;

    @Override
    public void init(Map<String, String> params) {
        reset();
        super.init(params);
        loadFunctions();
    }

    private void reset() {
        functions = null;
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

    public void updateDocCounters(@SuppressWarnings("unused") ActionEvent event) {
        final long docCount = getFunctionsService().updateDocCounters();
        MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "docList_updateDocCounters_success", docCount);
    }

    public void deleteAllDocuments(@SuppressWarnings("unused") ActionEvent event) {
        final long docCount = getFunctionsService().deleteAllDocuments();
        MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "docList_deleteAllDocuments_success", docCount);
    }

    // START: JSP event handlers
    public void export(@SuppressWarnings("unused") ActionEvent event) {
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

            // Erko hack for incorrect view id in the next request
            JspStateManagerImpl.ignoreCurrentViewSequenceHack();

            log.info("docList export completed");
        }
    }

    public void createNewYearBasedVolumes(@SuppressWarnings("unused") ActionEvent event) {
        final long createdVolumesCount = getFunctionsService().createNewYearBasedVolumes();
        MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "docList_createNewYearBasedVolumes_success", createdVolumesCount);
    }

    public void importSmitDocList(@SuppressWarnings("unused") ActionEvent event) {
        final SmitExcelImporter importExcelBootstrap = (SmitExcelImporter) FacesContextUtils.getRequiredWebApplicationContext(
                FacesContext.getCurrentInstance()).getBean("smitExcelImporter");
        try {
            importExcelBootstrap.importSmitDocList();
        } catch (Exception e) {
            final String msg = "failed to import:";
            log.debug(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    // END: JSP event handlers

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
    }

    // END: private methods

    // START: getters / setters
    /**
     * Used in JSP pages.
     */
    public List<Function> getFunctions() {
        return functions;
    }

    public List<Function> getMySeriesFunctions() {
        List<Function> seriesFunctions = new ArrayList<Function>(functions.size());
        for (Function function : getFunctions()) {
            List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(function.getNodeRef());
            for (ChildAssociationRef caRef : childAssocs) {
                @SuppressWarnings("unchecked")
                List<Integer> structUnits = (List<Integer>) getNodeService().getProperty(caRef.getChildRef(), SeriesModel.Props.STRUCT_UNIT);
                boolean contains = structUnits.contains(getCurrentUsersStructUnitId());
                if (contains) {
                    seriesFunctions.add(function);
                    break;
                }
            }
        }

        return seriesFunctions;
    }

    private Integer getCurrentUsersStructUnitId() {
        Map<QName, Serializable> userProperties = userService.getUserProperties(AuthenticationUtil.getRunAsUser());
        Serializable orgId = userProperties.get(ContentModel.PROP_ORGID);
        if (orgId == null)
            return null;
        return Integer.parseInt(orgId.toString());
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

    protected UserService getUserService() {
        if (userService == null) {
            userService = (UserService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean("UserService");
        }
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    // END: getters / setters

}
