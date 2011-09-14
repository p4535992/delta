package ee.webmedia.alfresco.common.web;

import java.io.Serializable;
import java.util.Collection;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.transaction.TransactionService;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docconfig.bootstrap.SystematicDocumentTypesBootstrap;
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

    public void deleteFieldAndFieldGroupsAndBootstrapInfo(@SuppressWarnings("unused") ActionEvent event) {
        String simdhsModule = "simdhs";
        deleteBootstrap(simdhsModule, "systematicFieldGroupDefinitionsBootstrap1");
        deleteBootstrap(simdhsModule, "systematicFieldGroupDefinitionsBootstrap2");
        deleteBootstrap(simdhsModule, "systematicFieldDefinitionsBootstrap1");
        deleteBootstrap(simdhsModule, "systematicFieldDefinitionsBootstrap2");
        deleteBootstrap(simdhsModule, "systematicFieldGroupDefinitions1FixBootstrap");
        deleteBootstrap(simdhsModule, "systematicDocumentTypesBootstrap");
        NodeRef fieldDefsNodeRef = getNodeRef("/{http://alfresco.webmedia.ee/model/document/admin/1.0}fieldDefinitions");
        NodeRef fieldGroupDefsNodeRef = getNodeRef("/{http://alfresco.webmedia.ee/model/document/admin/1.0}fieldGroupDefinitions");
        deleteChildren(fieldDefsNodeRef);
        deleteChildren(fieldGroupDefsNodeRef);

        NodeRef documentTypesNodeRef = getNodeRef("/{http://alfresco.webmedia.ee/model/document/admin/1.0}documentTypes");
        for (ChildAssociationRef childAssociationRef : getNodeService().getChildAssocs(documentTypesNodeRef)) {
            NodeRef childRef = childAssociationRef.getChildRef();
            if (Boolean.TRUE.equals(nodeService.getProperty(childRef, DocumentAdminModel.Props.SYSTEMATIC))) {
                getNodeService().deleteNode(childRef);
                LOG.info("deleted node " + childRef);
            }
        }
    }

    public void importSystematicDocumentTypes(@SuppressWarnings("unused") ActionEvent event) {
        final SystematicDocumentTypesBootstrap systematicDocumentTypesBootstrap = (SystematicDocumentTypesBootstrap) AppConstants.getBeanFactory().getBean(
                "systematicDocumentTypesBootstrap");
        BeanHelper.getTransactionService().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
            @Override
            public Void execute() throws Throwable {
                systematicDocumentTypesBootstrap.executeInternalImpl();
                return null;
            }
        });
    }

    private void deleteBootstrap(String moduleName, String bootstrapName) {
        String systematicFieldGroupDefinitionsBootstrap1 = getBootstrapXPath(moduleName, bootstrapName);
        StoreRef store = new StoreRef("system://system");
        final NodeRef nodeRef = getNodeRef(systematicFieldGroupDefinitionsBootstrap1, store);
        if (nodeRef == null) {
            LOG.info("from module '" + moduleName + "' bootstrap '" + bootstrapName + "' does not exist");
        } else {
            getNodeService().deleteNode(nodeRef);
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

    private String getBootstrapXPath(String moduleName, String bootstrapName) {
        return "/{http://www.alfresco.org/model/system/1.0}system-registry/{http://www.alfresco.org/system/modules/1.0}modules/" +
                "{http://www.alfresco.org/system/modules/1.0}" + moduleName
                + "/{http://www.alfresco.org/system/modules/1.0}components/{http://www.alfresco.org/system/modules/1.0}" + bootstrapName;
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
