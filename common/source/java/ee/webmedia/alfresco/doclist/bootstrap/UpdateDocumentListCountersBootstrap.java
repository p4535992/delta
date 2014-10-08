<<<<<<< HEAD
package ee.webmedia.alfresco.doclist.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.doclist.service.DocumentListService;
import ee.webmedia.alfresco.functions.service.FunctionsService;

/**
 * @author Alar Kvell
 */
public class UpdateDocumentListCountersBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    private GeneralService generalService;
    private FunctionsService functionsService;
    private DocumentListService documentListService;

    @Override
    protected void executeInternal() throws Throwable {
        List<NodeRef> roots = new ArrayList<NodeRef>();
        roots.add(functionsService.getFunctionsRoot());
        for (ArchivalsStoreVO archivalsStoreVO : generalService.getArchivalsStoreVOs()) {
            roots.add(archivalsStoreVO.getNodeRef());
        }
        for (final NodeRef nodeRef : roots) {
            LOG.info("Starting to update document list counters under " + nodeRef);
            documentListService.updateDocCounters(nodeRef);
            LOG.info("Completed updating document list counters under " + nodeRef);
        }
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    public void setDocumentListService(DocumentListService documentListService) {
        this.documentListService = documentListService;
    }

}
=======
package ee.webmedia.alfresco.doclist.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.doclist.service.DocumentListService;
import ee.webmedia.alfresco.functions.service.FunctionsService;

public class UpdateDocumentListCountersBootstrap extends AbstractModuleComponent {
    protected final Log LOG = LogFactory.getLog(getClass());

    private GeneralService generalService;
    private FunctionsService functionsService;
    private DocumentListService documentListService;

    @Override
    protected void executeInternal() throws Throwable {
        List<NodeRef> roots = new ArrayList<NodeRef>();
        roots.add(functionsService.getFunctionsRoot());
        for (ArchivalsStoreVO archivalsStoreVO : generalService.getArchivalsStoreVOs()) {
            roots.add(archivalsStoreVO.getNodeRef());
        }
        for (final NodeRef nodeRef : roots) {
            LOG.info("Starting to update document list counters under " + nodeRef);
            documentListService.updateDocCounters(nodeRef);
            LOG.info("Completed updating document list counters under " + nodeRef);
        }
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    public void setDocumentListService(DocumentListService documentListService) {
        this.documentListService = documentListService;
    }

}
>>>>>>> develop-5.1
