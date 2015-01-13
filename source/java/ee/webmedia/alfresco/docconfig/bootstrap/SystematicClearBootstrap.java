package ee.webmedia.alfresco.docconfig.bootstrap;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import ee.webmedia.alfresco.common.bootstrap.ImporterModuleComponent;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.SearchUtil;

public class SystematicClearBootstrap extends AbstractModuleComponent implements BeanFactoryAware {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SystematicClearBootstrap.class);

    private GeneralService generalService;
    private BeanFactory beanFactory;

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("Deleting fieldDefinitions");
        if (deleteChildren("/docadmin:fieldDefinitions")) {
            LOG.info("Executing systematicFieldDefinitionsBootstrap1");
            ((ImporterModuleComponent) beanFactory.getBean("systematicFieldDefinitionsBootstrap1")).executeInternal();
        }
        LOG.info("Deleting fieldGroupDefinitions");
        if (deleteChildren("/docadmin:fieldGroupDefinitions")) {
            LOG.info("Executing systematicFieldGroupDefinitionsBootstrap1");
            ((ImporterModuleComponent) beanFactory.getBean("systematicFieldGroupDefinitionsBootstrap1")).executeInternal();
        }
        LOG.info("Deleting documentTypes");
        deleteChildren("/docadmin:documentTypes");

        LOG.info("Deleting documentDynamic nodes");
        ResultSet resultSet = serviceRegistry.getSearchService().query(generalService.getStore(), SearchService.LANGUAGE_LUCENE,
                SearchUtil.generateTypeQuery(QName.createQName(DocumentDynamicModel.URI, "documentDynamic")));
        try {
            int deletedCount = 0;
            NodeService nodeService = serviceRegistry.getNodeService();
            for (NodeRef nodeRef : resultSet.getNodeRefs()) {
                if (nodeService.exists(nodeRef) && nodeService.hasAspect(nodeRef, DocumentCommonModel.Aspects.COMMON) /* second check is only for development environment */) {
                    nodeService.deleteNode(nodeRef);
                    deletedCount++;
                }
            }
            LOG.info("Deleted " + deletedCount + " documentDynamic nodes");
        } finally {
            resultSet.close();
        }
    }

    private boolean deleteChildren(String deletableXpath) {
        return deleteChildren(generalService.getNodeRef(deletableXpath));
    }

    private boolean deleteChildren(NodeRef deletableNodeRef) {
        boolean childrenFound = false;
        for (ChildAssociationRef childAssociationRef : serviceRegistry.getNodeService().getChildAssocs(deletableNodeRef)) {
            childrenFound = true;
            NodeRef childRef = childAssociationRef.getChildRef();
            serviceRegistry.getNodeService().deleteNode(childRef);
        }
        return childrenFound;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
