package ee.webmedia.alfresco.common.web;

import java.io.Serializable;
import java.util.Collection;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.transaction.TransactionService;

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.dvk.service.DvkService;
import ee.webmedia.alfresco.utils.ActionUtil;
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

    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
    public void handleTestEvent(ActionEvent event) {
        int testParamValue = ActionUtil.getParam(event, "testP", Integer.class);
        LOG.debug("Received event with testP=" + testParamValue);
        // Developers can use this method for testing, but shouldn't commit changes
        atsTestib(event);
    }

    protected RetryingTransactionHelper getTransactionHelper() {
        RetryingTransactionHelper helper = new RetryingTransactionHelper();
        helper.setMaxRetries(1);
        helper.setTransactionService(getTransactionService());
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
