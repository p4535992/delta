package ee.webmedia.alfresco.common.web;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.TemplateType;
import ee.webmedia.alfresco.common.job.NightlyDataFixJob;
import ee.webmedia.alfresco.common.service.CustomReindexComponent;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.destruction.model.DestructionModel;
import ee.webmedia.alfresco.docdynamic.bootstrap.DocumentUpdater;
import ee.webmedia.alfresco.document.bootstrap.SearchableSendInfoUpdater;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.volume.VolumeDispositionReportGenerator;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.search.model.VolumeSearchModel;
import ee.webmedia.alfresco.workflow.bootstrap.CompoundWorkflowOwnerPropsUpdater;
import ee.webmedia.alfresco.workflow.bootstrap.TaskUpdater;
import ee.webmedia.xtee.client.dhl.DhlFSStubXTeeServiceImpl;
import org.alfresco.repo.cache.EhCacheTracerJob;
import org.alfresco.repo.search.Indexer;
import org.alfresco.repo.search.impl.lucene.ADMLuceneTest;
import org.alfresco.repo.search.impl.lucene.AbstractLuceneBase;
import org.alfresco.repo.search.impl.lucene.AbstractLuceneIndexerAndSearcherFactory.LuceneIndexBackupComponent;
import org.alfresco.repo.search.impl.lucene.AbstractLuceneIndexerAndSearcherFactory.LuceneIndexBackupJob;
import org.alfresco.repo.search.impl.lucene.LuceneIndexerAndSearcher;
import org.alfresco.repo.search.impl.lucene.index.IndexInfo;
import org.alfresco.repo.security.sync.UserRegistrySynchronizer;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.myfaces.application.jsp.JspStateManagerImpl;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.quartz.JobExecutionException;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getSpringBean;
import static ee.webmedia.alfresco.utils.SearchUtil.*;

/**
 * Bean with method {@link #handleTestEvent(ActionEvent)} that developers can use to test arbitrary code
 */
public class TestingForDeveloperBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(TestingForDeveloperBean.class);
    private static final String CHARSET = AppConstants.CHARSET;
    private transient NodeService nodeService;
    private transient GeneralService generalService;
    private transient SearchService searchService;
    private transient TransactionService transactionService;

    private static final FastDateFormat dateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm:ss.SSSZ");

    private String fileName;
    private String dispositionFileName;
    private String indexInfoText;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getIndexInfoText() {
        return indexInfoText;
    }

    private String missingOwnerId;

    public String getMissingOwnerId() {
        return missingOwnerId;
    }

    public void setMissingOwnerId(String missingOwnerId) {
        this.missingOwnerId = missingOwnerId;
    }

    public String getDispositionFileName() {
        return dispositionFileName;
    }

    public void setDispositionFileName(String dispositionFileName) {
        this.dispositionFileName = dispositionFileName;
    }

    public void searchMissingOwnerId(@SuppressWarnings("unused") ActionEvent event) {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(DocumentCommonModel.Types.DOCUMENT));
        queryParts.add(generateAspectQuery(DocumentCommonModel.Aspects.SEARCHABLE));
        queryParts.add(SearchUtil.generatePropertyNullQuery(DocumentCommonModel.Props.OWNER_ID));
        queryParts.add(SearchUtil.generatePropertyNotNullQuery(DocumentCommonModel.Props.OWNER_NAME));
        queryParts.add(generateStringExactQuery(DocumentStatus.WORKING.getValueName(), DocumentCommonModel.Props.DOC_STATUS));
        String query = joinQueryPartsAnd(queryParts);
        String result = StringUtils.join(BeanHelper.getDocumentSearchService().searchNodes(query, 0, "missingOwnerId"), '\n');
        if (StringUtils.isBlank(result)) {
            result = "0";
        }

        setMissingOwnerId(result);
    }

    public void addNonSerializableObjectToSession(@SuppressWarnings("unused") ActionEvent event) {
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("NonSerializableObject", new Object());
    }

    public void receiveDocStub(@SuppressWarnings("unused") ActionEvent event) {
        DvkService stubDvkService = BeanHelper.getStubDvkService();
        DhlFSStubXTeeServiceImpl dhlXTeeServiceImplFSStub = BeanHelper.getDhlXTeeServiceImplFSStub();
        dhlXTeeServiceImplFSStub.setHasDocuments(true);
        String xmlFile = fileName;
        dhlXTeeServiceImplFSStub.setDvkXmlFile(xmlFile);
        Collection<String> receiveResults = stubDvkService.receiveDocuments();
        LOG.info("created " + receiveResults.size() + " documents based on given xml file");
    }

    public void doStuff(ActionEvent event) {
        // DhlXTeeServiceImpl dhlXTeeService = BeanHelper.getSpringBean(DhlXTeeServiceImpl.class, "dhlXTeeService");
        // dhlXTeeService.markDocumentsReceived(Arrays.asList("10113"));

        Collection<String> receiveDocuments = BeanHelper.getDvkService().receiveDocuments();
        LOG.info("Received following documents:" + receiveDocuments);
    }

    /** Event handler for link "TestingForDeveloper" in /simdhs/faces/jsp/admin/store-browser.jsp */
    public void handleTestEvent(ActionEvent event) throws Exception {
        int testParamValue = ActionUtil.getParam(event, "testP", Integer.class);
        LOG.debug("Received event with testP=" + testParamValue);
        // Developers can use this method for testing, but shouldn't commit changes
        atsTestib(event);
    }

    public void runADMLuceneTestTestMaskDeletes(ActionEvent event) throws Exception {
        LOG.info("Starting to run ADMLuceneTest.testMaskDeletes");
        ADMLuceneTest test = new ADMLuceneTest("admLuceneTest");
        test.setUp(BeanHelper.getApplicationService().getApplicationContext());
        test.testMaskDeletes();
        test.tearDown();
        LOG.info("Completed running ADMLuceneTest.testMaskDeletes");
    }

    public void deleteFieldAndFieldGroupsAndBootstrapInfo(@SuppressWarnings("unused") ActionEvent event) {
        String simdhsModule = "simdhs";
        deleteBootstrap(simdhsModule, "systematicFieldDefinitionsBootstrap1");
        deleteBootstrap(simdhsModule, "systematicFieldDefinitionsBootstrap2");
        deleteBootstrap(simdhsModule, "systematicFieldDefinitionsBootstrap3");
        deleteBootstrap(simdhsModule, "systematicFieldDefinitionsBootstrap4");
        deleteBootstrap(simdhsModule, "systematicFieldGroupDefinitionsBootstrap1");
        deleteBootstrap(simdhsModule, "systematicFieldGroupDefinitionsBootstrap2");
        deleteBootstrap(simdhsModule, "systematicFieldGroupDefinitionsBootstrap3");
        deleteBootstrap(simdhsModule, "systematicFieldGroupDefinitions1FixBootstrap");
        deleteBootstrap(simdhsModule, "systematicDocumentTypesBootstrap");
        deleteChildren(getNodeRef("/docadmin:fieldDefinitions"));
        deleteChildren(getNodeRef("/docadmin:fieldGroupDefinitions"));
        deleteChildren(getNodeRef("/docadmin:documentTypes"));
    }

    public void deleteTestTemplatesBootstrapAndTemplates(@SuppressWarnings("unused") ActionEvent event) {
        deleteBootstrap("simdhs", "testWorkflowTemplatesBootstrap");
        List<FileInfo> templateFiles = BeanHelper.getFileFolderService().listFiles(BeanHelper.getConstantNodeRefsBean().getTemplateRoot());
        LOG.info("Found total " + templateFiles.size() + " templates");
        for (FileInfo fi : templateFiles) {
            if (getNodeService().hasAspect(fi.getNodeRef(), DocumentTemplateModel.Aspects.TEMPLATE_NOTIFICATION)
                    || TemplateType.NOTIFICATION_TEMPLATE.toString().equals(getNodeService().getProperty(fi.getNodeRef(), DocumentTemplateModel.Prop.TEMPLATE_TYPE))
                    || getNodeService().hasAspect(fi.getNodeRef(), QName.createQName(DocumentTemplateModel.URI, "systemTemplate"))
                    || "Süsteemne mall".equals(getNodeService().getProperty(fi.getNodeRef(), DocumentTemplateModel.Prop.DOCTYPE_ID))) {
                LOG.info("Deleting template \"" + fi.getName() + "\"");
                getNodeService().deleteNode(fi.getNodeRef());
            }
        }
        LOG.info("Completed deleting templates");
    }

    public void generateDispositionReport(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("generateDispositionReport " + getDispositionFileName());
        String csvFileName = getDispositionFileName();
        if (StringUtils.isBlank(getDispositionFileName())) {
            throw new UnableToPerformException("Input csv not defined, aborting updater");
        }
        File file = new File(csvFileName);
        if (!file.exists()) {
            throw new UnableToPerformException("Input csv " + csvFileName + " does not exist, aborting updater");
        }

        HSSFWorkbook workbook = VolumeDispositionReportGenerator.generate(file);
        writeDispositionReportToResponse(workbook);
        // hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    public void resetMarkedForDestruction(@SuppressWarnings("unused") ActionEvent event) {
        LOG.info("resetMarkedForDestruction ");
        List<NodeRef> storeNodeRefs = getArchivalStores();
        List<Volume> volumes = BeanHelper.getDocumentSearchService().searchVolumesForArchiveList(
                new TransientNode(VolumeSearchModel.Types.ARCHIVE_LIST_FILTER, null, null), false, false, storeNodeRefs);
        resetMarkedForDestruction(volumes);
        // hack for incorrect view id in the next request
        JspStateManagerImpl.ignoreCurrentViewSequenceHack();
    }

    private List<NodeRef> getArchivalStores() {
        List<NodeRef> storeNodeRefs = new ArrayList<>();
        for (ArchivalsStoreVO archivalsStoreVO : getGeneralService().getArchivalsStoreVOs()) {
            storeNodeRefs.add(archivalsStoreVO.getNodeRef());
        }
        return storeNodeRefs;
    }

    private void resetMarkedForDestruction(List<Volume> volumes) {
        List<NodeRef> jobList = BeanHelper.getArchivalsService().getAllInQueueJobsForDesruction();
        for (Volume volume : volumes) {
            if (!jobList.contains(volume.getNodeRef())) {
                nodeService.setProperty(volume.getNodeRef(), EventPlanModel.Props.MARKED_FOR_DESTRUCTION, Boolean.FALSE);
            }
        }
    }

    private void writeDispositionReportToResponse(HSSFWorkbook workbook) {
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
            response.reset();
            response.setCharacterEncoding(CHARSET);
            response.setContentType("text/csv; charset=" + CHARSET);
            response.setContentType("application/octet-stream");
            response.addHeader("Content-Disposition", "attachment; filename=\"Havitamisakt.xls\"");
            response.setHeader("Expires", "0");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            OutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            out.close();
            fc.responseComplete();
            fc.renderResponse();
            LOG.info("Your excel file has been generated!");
        } catch (IOException e) {
            throw new UnableToPerformException("Error generating Havitamisakt.xls!", e);
        }
    }

    private void deleteBootstrap(String moduleName, String bootstrapName) {
        final NodeRef nodeRef = getGeneralService().deleteBootstrapNodeRef(moduleName, bootstrapName);
        if (nodeRef == null) {
            LOG.info("from module '" + moduleName + "' bootstrap '" + bootstrapName + "' does not exist");
        } else {
            LOG.info("from module '" + moduleName + "' deleted bootstrap '" + bootstrapName + "' (noderef=" + nodeRef + ")");
        }
    }

    private NodeRef getNodeRef(String xpath) {
        return getNodeRef(xpath, getGeneralService().getStore());
    }

    private NodeRef getNodeRef(String xpath, StoreRef store) {
        return getGeneralService().getNodeRef(xpath, store);
    }

    private void deleteChildren(NodeRef deletableNodeRef) {
        for (ChildAssociationRef childAssociationRef : getNodeService().getChildAssocs(deletableNodeRef)) {
            NodeRef childRef = childAssociationRef.getChildRef();
            getNodeService().deleteNode(childRef);
            LOG.info("deleted node " + childRef);
        }
    }

    private List<String> storeRefs = null;

    public List<String> getStoreRefs() {
        if (storeRefs == null) {
            List<StoreRef> stores = getNodeService().getStores();
            storeRefs = new ArrayList<String>(stores.size());
            for (StoreRef storeRef : stores) {
                storeRefs.add(storeRef.toString());
            }
        }
        return storeRefs;
    }

    public void runNightly0230DataMaintenanceJobNow(@SuppressWarnings("unused") ActionEvent event) throws JobExecutionException {
        new NightlyDataFixJob().execute(null);
    }

    public void runNightly0300IndexMaintenanceJobNow(@SuppressWarnings("unused") ActionEvent event) {
        LuceneIndexBackupComponent luceneIndexBackupComponent = BeanHelper.getSpringBean(LuceneIndexBackupComponent.class, "luceneIndexBackupComponent");
        new LuceneIndexBackupJob().executeInternal(luceneIndexBackupComponent);
    }

    public void runMergeNow(ActionEvent event) {
        final StoreRef storeRef = new StoreRef(ActionUtil.getParam(event, "storeRef"));
        final LuceneIndexerAndSearcher indexerAndSearcher = BeanHelper.getSpringBean(LuceneIndexerAndSearcher.class, "admLuceneIndexerAndSearcherFactory");
        getTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
            @Override
            public Void execute() throws Throwable {
                Indexer indexer = indexerAndSearcher.getIndexer(storeRef);
                IndexInfo indexInfo = getIndexInfo(indexer);
                runMergeNow(indexInfo);
                printIndexInfo(indexInfo);
                return null;
            }
        });
    }

    private void runMergeNow(IndexInfo indexInfo) {
        LOG.info("Scheduling special merge to run on indexInfo: " + indexInfo);
        indexInfo.runMergeNow();

        // XXX sõltuvalt mis selgub öise indekseerimisaktiivsuse kohta võib olla vajalik ka protsessi jooksmise ajal määrata mergerTargetOverlaysBlockingFactor väärtus suuremaks et
        // kasutajaid mitte blokeerida ja võibolla lubada Lucenel rohkem mälu kasutada et indeksite ümber kirjutamise effektiivust tõsta.
    }

    private IndexInfo getIndexInfo(Indexer indexer) throws NoSuchFieldException, IllegalAccessException {
        Field indexInfoField = AbstractLuceneBase.class.getDeclaredField("indexInfo");
        indexInfoField.setAccessible(true);
        return (IndexInfo) indexInfoField.get(indexer);
    }

    public void executeDocumentUpdater(ActionEvent event) throws Throwable {
        final DocumentUpdater documentUpdater = BeanHelper.getSpringBean(DocumentUpdater.class, "documentUpdater9");
        documentUpdater.executeUpdaterInBackground();
    }

    public void executeSearchableSendInfoUpdater(ActionEvent event) throws Throwable {
        final SearchableSendInfoUpdater searchableSendInfoUpdater = BeanHelper.getSpringBean(SearchableSendInfoUpdater.class, "searchableSendInfoUpdater");
        searchableSendInfoUpdater.executeUpdaterInBackground();
    }

    public void executeCompoundWorkflowOwnerPropsUpdater(ActionEvent event) throws Throwable {
        final CompoundWorkflowOwnerPropsUpdater compoundWorkflowOwnerPropsUpdater = BeanHelper.getSpringBean(CompoundWorkflowOwnerPropsUpdater.class,
                "compoundWorkflowOwnerPropsUpdater6");
        compoundWorkflowOwnerPropsUpdater.executeUpdaterInBackground();
    }

    public void executeTaskUpdater(ActionEvent event) throws Throwable {
        final TaskUpdater taskUpdater = BeanHelper.getSpringBean(TaskUpdater.class, "taskUpdater");
        taskUpdater.executeUpdaterInBackground();
    }

    public void printIndexInfo(ActionEvent event) {
        final StoreRef storeRef = new StoreRef(ActionUtil.getParam(event, "storeRef"));
        final LuceneIndexerAndSearcher indexerAndSearcher = BeanHelper.getSpringBean(LuceneIndexerAndSearcher.class, "admLuceneIndexerAndSearcherFactory");
        getTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
            @Override
            public Void execute() throws Throwable {
                Indexer indexer = indexerAndSearcher.getIndexer(storeRef);
                IndexInfo indexInfo = getIndexInfo(indexer);
                printIndexInfo(indexInfo);
                return null;
            }
        });
    }

    private void printIndexInfo(IndexInfo indexInfo) {
        String indexInfoTextWithoutDate = indexInfo.toString() + indexInfo.dumpInfoAsString();
        indexInfoText = dateTimeFormat.format(System.currentTimeMillis()) + "\n" + indexInfoTextWithoutDate;
        LOG.info(indexInfoTextWithoutDate);
    }

    public void updateUsersAndGroups(ActionEvent event) {
        LOG.debug("Starting to update users and usergroups");
        UserRegistrySynchronizer userRegistrySynchronizer = getSpringBean(UserRegistrySynchronizer.class, "UserRegistrySynchronizer");
        userRegistrySynchronizer.synchronize(true);
        LOG.debug("Finished updating users and usergroups");
    }

    public void searchHolesAndIndex(ActionEvent event) {
        CustomReindexComponent customReindexComponent = BeanHelper.getSpringBean(CustomReindexComponent.class, "customReindexComponent");
        customReindexComponent.reindex();
    }

    public void executeCacheStatistics(ActionEvent event) throws JobExecutionException {
        (new EhCacheTracerJob()).execute(null);
    }

    protected RetryingTransactionHelper getTransactionHelper() {
        RetryingTransactionHelper helper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
        helper.setMaxRetries(1);
        return helper;
    }

    protected TransactionService getTransactionService() {
        if (transactionService == null) {
            transactionService = BeanHelper.getTransactionService();
        }
        return transactionService;
    }

    protected SearchService getSearchService() {
        if (searchService == null) {
            searchService = BeanHelper.getSearchService();
        }
        return searchService;
    }

    protected NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = BeanHelper.getNodeService();
        }
        return nodeService;
    }

    protected GeneralService getGeneralService() {
        if (generalService == null) {
            generalService = BeanHelper.getGeneralService();
        }
        return generalService;
    }

    private void atsTestib(@SuppressWarnings("unused") ActionEvent e) {
        // ära puutu seda meetodit!
    }

}
