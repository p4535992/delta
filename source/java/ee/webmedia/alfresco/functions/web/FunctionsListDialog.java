package ee.webmedia.alfresco.functions.web;

import static ee.webmedia.alfresco.app.AppConstants.CHARSET;
<<<<<<< HEAD
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentListService;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
<<<<<<< HEAD
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
=======
import java.util.List;
import java.util.Map;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

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
<<<<<<< HEAD
import org.apache.commons.lang.StringUtils;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import org.apache.myfaces.application.jsp.JspStateManagerImpl;
import org.springframework.web.jsf.FacesContextUtils;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WMAdminNodeBrowseBean;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.importer.excel.bootstrap.SmitExcelImporter;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.service.UserService;
<<<<<<< HEAD
import ee.webmedia.alfresco.utils.ActionUtil;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
    private int deleteBatchSize = 30;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

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
        final long docCount = BeanHelper.getDocumentListService().updateDocCounters();
        MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "docList_updateDocCounters_success", docCount);
    }

<<<<<<< HEAD
=======
    public void updateArchivedDocCounters(@SuppressWarnings("unused") ActionEvent event) {
        final long docCount = BeanHelper.getDocumentListService().updateDocCounters(BeanHelper.getArchivalsService().getArchivalRoot());
        MessageUtil.addInfoMessage(FacesContext.getCurrentInstance(), "docList_updateDocCounters_success", docCount);
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    /**
     * NB! this method doesn't delete files associated with cases or documents that will get deleted - hence wasting disk usage. <br>
     * But as at the moment this method is meant to be called only in cases where repository and DB backups will be restored(before final successful import), it
     * is not an issue.
     * 
     * @param event
     */
    public void deleteAllDocuments(@SuppressWarnings("unused") ActionEvent event) {
<<<<<<< HEAD
        deleteDocumentListAndArchivalsListContents(false, false);
    }

    public void deleteAllDocumentsAndStructure(@SuppressWarnings("unused") ActionEvent event) {
        deleteDocumentListAndArchivalsListContents(true, false);
    }

    public void deleteAllDocumentsAndStructureAndIndependentCompoundWorkflows(@SuppressWarnings("unused") ActionEvent event) {
        deleteDocumentListAndArchivalsListContents(true, true);
    }

    public void deleteAllIndependentCompoundWorkflows(@SuppressWarnings("unused") ActionEvent event) {
        deleteIndependentCompoundWorkflows();
    }

    private static final AtomicBoolean deleteInProgress = new AtomicBoolean(false);

    private void deleteDocumentListAndArchivalsListContents(boolean deleteStructure, boolean deleteIndependentCompoundWorkflows) {
        if (!deleteInProgress.compareAndSet(false, true)) {
            throw new RuntimeException("Delete already in progress, wait for it to complete");
        }
        try {
            log.info("Deleting from document list...");
            NodeRef documentListRootRef = getFunctionsService().getFunctionsRoot();
            deleteAllDocumentsAndStructure(documentListRootRef, deleteStructure);
            log.info("Deleting from archivals list...");
            NodeRef archivalsListRootRef = getGeneralService().getArchivalsStoreVOs().iterator().next().getNodeRef();
            deleteAllDocumentsAndStructure(archivalsListRootRef, deleteStructure);
            if (deleteIndependentCompoundWorkflows) {
                deleteIndependentCompoundWorkflows();
            }
            log.info("Completed all deleting");
        } finally {
            deleteInProgress.set(false);
        }
    }

    private void deleteIndependentCompoundWorkflows() {
        log.info("Deleting independent compound workflows...");
        NodeRef independentWorkflowsRoot = BeanHelper.getWorkflowService().getIndependentWorkflowsRoot();
        log.info("Finding nodes to delete...");
        List<NodeRef> independentWorkflows = new ArrayList<NodeRef>();
        for (ChildAssociationRef childAssocRef : getNodeService().getChildAssocs(independentWorkflowsRoot)) {
            independentWorkflows.add(childAssocRef.getChildRef());
        }
        log.info("There are " + independentWorkflows.size() + " independent compound workflow nodes to delete");
        deleteNodeRefsBatch(independentWorkflows, deleteBatchSize);
    }

    private void deleteAllDocumentsAndStructure(NodeRef functionsRoot, boolean deleteStructure) {
        log.info("Finding nodes to delete...");
        Pair<List<NodeRef>, List<NodeRef>> allDocumentAndStructureRefs = getDocumentListService().getAllDocumentAndStructureRefs(functionsRoot);
        List<NodeRef> docRefs = allDocumentAndStructureRefs.getFirst();
        List<NodeRef> structRefs = allDocumentAndStructureRefs.getSecond();
        log.info("There are " + docRefs.size() + " document nodes" + (deleteStructure ? " and " + structRefs.size() + " structure nodes" : "") + " to delete");
        deleteNodeRefsBatch(docRefs, deleteBatchSize);
        if (deleteStructure) {
            deleteNodeRefsBatch(structRefs, deleteBatchSize);
        }
    }

    private void deleteNodeRefsBatch(final List<NodeRef> refsToDelete, final int batchMaxSize) {
        log.info("Total nodes to delete is " + refsToDelete.size() + ", starting to delete " + batchMaxSize + " nodes at a time...");
        List<NodeRef> nodeRefsBatch = new ArrayList<NodeRef>(batchMaxSize);
        for (int i = 0; i < refsToDelete.size(); i++) {
            nodeRefsBatch.add(refsToDelete.get(i));
            if ((i + 1) == refsToDelete.size() || ((i + 1) % batchMaxSize == 0)) {
                try {
                    getGeneralService().deleteNodeRefs(nodeRefsBatch, false);
                    log.info("Deleted " + (i + 1) + " nodes out of " + refsToDelete.size() + " nodes");
                } catch (Exception e) {
                    log.error("Error deleting " + batchMaxSize + " nodes, continuing:\n" + nodeRefsBatch, e);
                }
                nodeRefsBatch.clear();
            }
        }
        log.info("Completed deleting " + refsToDelete.size() + " nodes");
=======
        final Pair<List<NodeRef>, Long> allDocumentAndCaseRefs = BeanHelper.getDocumentListService().getAllDocumentAndCaseRefs();
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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

<<<<<<< HEAD
            // Erko hack for incorrect view id in the next request
=======
            // hack for incorrect view id in the next request
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
            JspStateManagerImpl.ignoreCurrentViewSequenceHack();

            log.info("docList export completed");
        }
    }

<<<<<<< HEAD
    public void createNewYearBasedVolumes(ActionEvent event) {
        if (!ActionUtil.hasParam(event, "eventConfirmed")) {
            Map<String, String> params = new HashMap<String, String>(1);
            params.put("eventConfirmed", "eventConfirmed");
            BeanHelper.getUserConfirmHelper().setup("docList_createNewYearBasedVolumes_confirmProceed", null, "#{FunctionsListDialog.createNewYearBasedVolumes}", params);
            return;
        }

=======
    public void createNewYearBasedVolumes(@SuppressWarnings("unused") ActionEvent event) {
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        final long createdVolumesCount = BeanHelper.getDocumentListService().createNewYearBasedVolumes();
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

    public void exportDocumentConsolidatedList(@SuppressWarnings("unused") ActionEvent event) {
        exportConsolidatedList(functionsService.getFunctionsRoot());
    }

    public static void exportConsolidatedList(NodeRef nodeRef) {
        log.info("consolidated docList started");
<<<<<<< HEAD
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.setCharacterEncoding(CHARSET);
        OutputStream outputStream = null;
        try {
            outputStream = WMAdminNodeBrowseBean.getExportOutStream(response, "consolidated-list.csv");
            getDocumentListService().getExportCsv(outputStream, nodeRef);
            outputStream.flush();
=======
        try {
            BeanHelper.getReportService().createCsvReportResult(nodeRef);
            MessageUtil.addInfoMessage("docList_consolidatedList_background_job");
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
        } catch (Exception e) {
            final String msg = "Failed to export consolidated docList";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
<<<<<<< HEAD
            FacesContext.getCurrentInstance().responseComplete();
            JspStateManagerImpl.ignoreCurrentViewSequenceHack();
            log.info("consolidated docList export completed");
=======
            log.info("consolidated list job started in background");
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
<<<<<<< HEAD
        String currentUsersStructUnitId = getUserService().getCurrentUsersStructUnitId();
        if (StringUtils.isNotBlank(currentUsersStructUnitId)) {
            for (Function function : getFunctions()) {
                List<ChildAssociationRef> childAssocs = getNodeService().getChildAssocs(function.getNodeRef());
                for (ChildAssociationRef caRef : childAssocs) {
                    @SuppressWarnings("unchecked")
                    List<String> structUnits = (List<String>) getNodeService().getProperty(caRef.getChildRef(), SeriesModel.Props.STRUCT_UNIT);
                    boolean contains = structUnits != null && structUnits.contains(currentUsersStructUnitId);
                    if (contains) {
                        seriesFunctions.add(function);
                        break;
                    }
                }
            }
        }
        return seriesFunctions;
    }

    public int getDeleteBatchSize() {
        return deleteBatchSize;
    }

    public void setDeleteBatchSize(int deleteBatchSize) {
        this.deleteBatchSize = deleteBatchSize;
=======
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
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
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
