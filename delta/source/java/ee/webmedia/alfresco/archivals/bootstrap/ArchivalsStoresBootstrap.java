package ee.webmedia.alfresco.archivals.bootstrap;

import java.util.LinkedHashSet;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.axis.utils.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.functions.model.FunctionsModel;

/**
 * @author Alar Kvell
 */
public class ArchivalsStoresBootstrap extends AbstractModuleComponent {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ArchivalsStoresBootstrap.class);

    private GeneralService generalService;
    private String additionalArchivals;

    private final LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs = new LinkedHashSet<ArchivalsStoreVO>();

    @Override
    protected void executeInternal() throws Throwable {
        archivalsStoreVOs.add(new ArchivalsStoreVO("", "Arhivaalide loetelu"));
        for (String string : StringUtils.split(additionalArchivals, ';')) {
            ArchivalsStoreVO archivalsStoreVO = ArchivalsStoreVO.newInstance(string);
            Assert.isTrue(!archivalsStoreVOs.contains(archivalsStoreVO));
            archivalsStoreVOs.add(archivalsStoreVO);
        }
        NodeService nodeService = serviceRegistry.getNodeService();
        List<StoreRef> stores = nodeService.getStores();
        for (ArchivalsStoreVO archivalsStoreVO : archivalsStoreVOs) {
            StoreRef storeRef = archivalsStoreVO.getStoreRef();
            if (!stores.contains(storeRef)) {
                LOG.info("Store doesn't exist, creating: " + storeRef);
                StoreRef createdStoreRef = nodeService.createStore(storeRef.getProtocol(), storeRef.getIdentifier());
                Assert.isTrue(storeRef.equals(createdStoreRef));
                stores.add(storeRef);
            }
            NodeRef nodeRef = generalService.getNodeRef(archivalsStoreVO.getPrimaryPath(), storeRef);
            boolean created = false;
            if (nodeRef != null) {
                QName type = nodeService.getType(nodeRef);
                Assert.isTrue(FunctionsModel.Types.FUNCTIONS_ROOT.equals(type));
            } else {
                QName assocQName = QName.createQName(archivalsStoreVO.getPrimaryPath().substring(1), serviceRegistry.getNamespaceService());
                nodeRef = nodeService.createNode(
                        nodeService.getRootNode(storeRef),
                        ContentModel.ASSOC_CHILDREN,
                        assocQName,
                        FunctionsModel.Types.FUNCTIONS_ROOT).getChildRef();
                created = true;
            }
            if (!nodeService.hasAspect(nodeRef, ContentModel.ASPECT_ROOT)) {
                nodeService.addAspect(nodeRef, ContentModel.ASPECT_ROOT, null);
            }
            archivalsStoreVO.setNodeRef(nodeRef);
            LOG.info(archivalsStoreVO.toString() + " - " + (created ? "didn't exist, created" : "existed"));
        }
        generalService.setArchivalsStoreVOs(archivalsStoreVOs);
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setAdditionalArchivals(String additionalArchivals) {
        this.additionalArchivals = additionalArchivals;
    }

}
