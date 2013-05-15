package ee.webmedia.alfresco.common.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getSpringBean;
import static ee.webmedia.alfresco.utils.SearchUtil.generateAspectQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.quartz.JobExecutionException;

import ee.webmedia.alfresco.classificator.enums.DocumentStatus;
import ee.webmedia.alfresco.classificator.enums.TemplateType;
import ee.webmedia.alfresco.common.job.NightlyDataFixJob;
import ee.webmedia.alfresco.common.service.CustomReindexComponent;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.utils.ActionUtil;
import ee.webmedia.alfresco.utils.SearchUtil;
import ee.webmedia.xtee.client.dhl.DhlXTeeServiceImplFSStub;

/**
 * Bean with method {@link #handleTestEvent(ActionEvent)} that developers can use to test arbitrary code
 * 
 * @author Ats Uiboupin
 */
public class TestingForDeveloperBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(TestingForDeveloperBean.class);
    private transient NodeService nodeService;
    private transient GeneralService generalService;
    private transient SearchService searchService;
    private transient TransactionService transactionService;

    private static final FastDateFormat dateTimeFormat = FastDateFormat.getInstance("dd.MM.yyyy HH:mm:ss.SSSZ");

    private String fileName;
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
        DhlXTeeServiceImplFSStub dhlXTeeServiceImplFSStub = BeanHelper.getDhlXTeeServiceImplFSStub();
        dhlXTeeServiceImplFSStub.setHasDocuments(true);
        String xmlFile = fileName;
        dhlXTeeServiceImplFSStub.setDvkXmlFile(xmlFile);
        Collection<String> receiveResults = stubDvkService.receiveDocuments();
        LOG.info("created " + receiveResults.size() + " documents based on given xml file");
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
        List<FileInfo> templateFiles = BeanHelper.getFileFolderService().listFiles(BeanHelper.getDocumentTemplateService().getRoot());
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
