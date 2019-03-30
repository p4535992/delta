package ee.webmedia.alfresco.classificator.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.module.AbstractModuleComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.ProgressTracker;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class ClassificatorsCacheUpdater extends AbstractModuleComponent {

    public static final String BEAN_NAME = "ClassificatorsCacheUpdater";
    private static final Log LOG = LogFactory.getLog(ClassificatorsCacheUpdater.class);

    private GeneralService generalService;
    private NodeService nodeService;
    private ClassificatorService classificatorService;

    private boolean running;
    private String classificatorsPath;

    public static BeanPropertyMapper<Classificator> classificatorBeanPropertyMapper = BeanPropertyMapper.newInstance(Classificator.class);

    @Override
    protected void executeInternal() throws Throwable {
        LOG.info("ClassificatorsCacheUpdater started.");
        running = true;
        generalService.runOnBackground(new RunAsWork<Void>() {

            @Override
            public Void doWork() throws Exception {
                try {
                    updateClassificatorsCache();
                    LOG.info("ClassificatorsCacheUpdater finished.");
                    return null;
                } finally {
                    running = false;
                }
            }

        }, "loadClassificatorsCache", true);
    }

    private void updateClassificatorsCache() {
        NodeRef rootNodeRef = generalService.getNodeRef(classificatorsPath);
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(rootNodeRef);
        List<Classificator> classificators = new ArrayList<>(childRefs.size());
        int classificatorCount = classificators.size();
        LOG.info("Updating classificator cache for " + classificatorCount + " classificators");
        int count = 0;
        ProgressTracker progress = new ProgressTracker(classificatorCount, 0);
        for (ChildAssociationRef childRef : childRefs) {
            Classificator cl = new Classificator();
            NodeRef classificatorRef = childRef.getChildRef();
            cl.setNodeRef(classificatorRef);
            classificatorBeanPropertyMapper.toObject(nodeService.getProperties(classificatorRef), cl);
            cl.addClassificatorValues(classificatorService.loadAllClassificatorValuesFromDB(classificatorRef));
            classificators.add(cl);
            classificatorService.addToClassificatorsCache(cl);
            if (++count >= 100) {
                String info = progress.step(count);
                count = 0;
                if (info != null) {
                    LOG.info("Updated classificators cache: " + info);
                }
            }
        }
        String info = progress.step(count);
        if (info != null) {
            LOG.info("Updated classificators cache: " + info);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setClassificatorsPath(String classificatorsPath) {
        this.classificatorsPath = classificatorsPath;
    }

    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }

}
