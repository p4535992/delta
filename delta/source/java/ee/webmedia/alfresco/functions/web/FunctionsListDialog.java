package ee.webmedia.alfresco.functions.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.exporter.ACPExportPackageHandler;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.view.ExportPackageHandler;
import org.alfresco.service.cmr.view.ExporterCrawlerParameters;
import org.alfresco.service.cmr.view.ExporterService;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.io.IOUtils;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WMAdminNodeBrowseBean;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.importer.excel.bootstrap.SmitExcelImporter;
import ee.webmedia.alfresco.postipoiss.PostipoissDocumentsImporter;
import ee.webmedia.alfresco.postipoiss.PostipoissStructureImporter;
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
    private transient GeneralService generalService;
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

    /**
     * NB! this method doesn't delete files associated with cases or documents that will get deleted - hence wasting disk usage. <br>
     * But as at the moment this method is meant to be called only in cases where repository and DB backups will be restored(before final successful import), it
     * is not an issue.
     * 
     * @param event
     */
    public void deleteAllDocuments(@SuppressWarnings("unused") ActionEvent event) {
        final Pair<List<NodeRef>, Long> allDocumentAndCaseRefs = getFunctionsService().getAllDocumentAndCaseRefs();
        final List<NodeRef> refsToDelete = allDocumentAndCaseRefs.getFirst();
        final int batchMaxSize = 30;
        ArrayList<NodeRef> nodeRefsBatch = new ArrayList<NodeRef>(batchMaxSize);
        for (int i = 0; i < refsToDelete.size(); i++) {
            nodeRefsBatch.add(refsToDelete.get(i));
            if (i == (refsToDelete.size() - 1) || (i % batchMaxSize == 0)) {
                log.info("Deleting " + nodeRefsBatch.size() + " case or document nodeRefs");
                getGeneralService().deleteNodeRefs(nodeRefsBatch);
                log.info("Deleted " + nodeRefsBatch.size() + " case or document nodeRefs");
                nodeRefsBatch.clear();
            }
        }
        final long docCount = allDocumentAndCaseRefs.getSecond();
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

            outputStream = WMAdminNodeBrowseBean.getExportOutStream(response, "functions-list.acp");
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

    @Override
    public Object getActionsContext() {
        return new Node(getFunctionsService().getFunctionsRoot());
    }

    public void startPostipoissStructureImport(javax.faces.event.ActionEvent ev) {
        PostipoissStructureImporter importer = (PostipoissStructureImporter)
                FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                        .getBean("postipoissStructureImporter");
        if (importer.isStarted()) {
            log.info("Not running structure import, already started");
            return;
        }
        try {
            importer.runImport();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void startPostipoissDocumentsImport(javax.faces.event.ActionEvent ev) {
        PostipoissDocumentsImporter importer = (PostipoissDocumentsImporter)
                FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                        .getBean("postipoissDocumentsImporter");
        if (importer.isStarted()) {
            log.info("Not running documents import, already started");
            return;
        }
        try {
            importer.runImport();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // @formatter:off
    /*
    public void startPostipoissDocumentsFix(javax.faces.event.ActionEvent ev) {
        PostipoissDocumentsImporter importer = (PostipoissDocumentsImporter) 
            FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
            .getBean("postipoissDocumentsImporter");
        if (importer.isStarted()) {
            log.info("Not running documents fix, already started");
            return;
        }
        try {
            importer.runFixDocuments();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
     */
    // @formatter:on
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
        Integer currentUsersStructUnitId = getUserService().getCurrentUsersStructUnitId();
        for (Function function : getFunctions()) {
            List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(function.getNodeRef());
            for (ChildAssociationRef caRef : childAssocs) {
                @SuppressWarnings("unchecked")
                List<Integer> structUnits = (List<Integer>) getNodeService().getProperty(caRef.getChildRef(), SeriesModel.Props.STRUCT_UNIT);
                boolean contains = structUnits != null && structUnits.contains(currentUsersStructUnitId);
                if (contains) {
                    seriesFunctions.add(function);
                    break;
                }
            }
        }

        return seriesFunctions;
    }

    protected FunctionsService getFunctionsService() {
        if (functionsService == null) {
            functionsService = (FunctionsService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(FunctionsService.BEAN_NAME);
        }
        return functionsService;
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = (GeneralService) FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
                    .getBean(GeneralService.BEAN_NAME);
        }
        return generalService;
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
