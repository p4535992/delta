package ee.webmedia.alfresco.classificator.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.ISO9075;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorModel;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

public class ClassificatorServiceImpl implements ClassificatorService {
    
    private static final Log log = LogFactory.getLog(ClassificatorServiceImpl.class);
    
    private static BeanPropertyMapper<Classificator> classificatorBeanPropertyMapper;
    private static BeanPropertyMapper<ClassificatorValue> classificatorValueBeanPropertyMapper;

    static {
        classificatorBeanPropertyMapper = BeanPropertyMapper.newInstance(Classificator.class);
        classificatorValueBeanPropertyMapper = BeanPropertyMapper.newInstance(ClassificatorValue.class);
    }
    
    private GeneralService generalService;
    private NodeService nodeService;


    @Override
    public List<Classificator> getAllClassificators() {
        NodeRef root = generalService.getNodeRef("/" + ClassificatorModel.NAMESPACE_PREFFIX + ClassificatorModel.Types.CLASSIFICATOR_ROOT.getLocalName());
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(root);
        List<Classificator> classificators = new ArrayList<Classificator>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            Classificator cl = new Classificator();
            cl.setNodeRef(childRef.getChildRef());
            classificatorBeanPropertyMapper.toObject(nodeService.getProperties(childRef.getChildRef()), cl);
            classificators.add(cl);
        }
        if (log.isDebugEnabled()) {
            log.debug("Classificators found: " + classificators);
        }
        return classificators;
    }
    
    @Override
    public Classificator getClassificatorByNodeRef(String ref) {
        return getClassificatorByNodeRef(new NodeRef(ref));
    }
    
    @Override
    public Classificator getClassificatorByNodeRef(NodeRef nodeRef) {
        Classificator cl = new Classificator();
        cl.setNodeRef(nodeRef);
        classificatorBeanPropertyMapper.toObject(nodeService.getProperties(nodeRef), cl);
        return cl;
    }
    
    @Override
    public Classificator getClassificatorByName(String name) {
        String xpath = "/" + ClassificatorModel.NAMESPACE_PREFFIX + ClassificatorModel.Types.CLASSIFICATOR_ROOT.getLocalName() + "/"
                + ClassificatorModel.NAMESPACE_PREFFIX + ISO9075.encode(name);
        NodeRef classifNodeRef = generalService.getNodeRef(xpath);
        Assert.notNull(classifNodeRef, "Unknown classificator '"+name+"'");
        return getClassificatorByNodeRef(classifNodeRef);
    }

    @Override
    public List<ClassificatorValue> getAllClassificatorValues(Classificator classificator) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(classificator.getNodeRef(), ClassificatorModel.Associations.CLASSIFICATOR_VALUE, RegexQNamePattern.MATCH_ALL);  
        List<ClassificatorValue> classificatorValues = new ArrayList<ClassificatorValue>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            ClassificatorValue clv = new ClassificatorValue();
            clv.setNodeRef(childRef.getChildRef());
            classificatorValueBeanPropertyMapper.toObject(nodeService.getProperties(childRef.getChildRef()), clv);
            classificatorValues.add(clv);
        }
        if (log.isDebugEnabled()) {
            log.debug("Classificator values found: " + classificatorValues);
        }
        return classificatorValues;
    }
    
    @Override
    public List<ClassificatorValue> getActiveClassificatorValues(Classificator classificator) {
        List<ClassificatorValue> allClassificatorValues = getAllClassificatorValues(classificator);
        List<ClassificatorValue> activeClassificatorValues = new ArrayList<ClassificatorValue>(allClassificatorValues.size());
        for (ClassificatorValue classificatorValue : allClassificatorValues) {
            if(classificatorValue.isActive()) {
                activeClassificatorValues.add(classificatorValue);
            }
        }
        return activeClassificatorValues;
    }
    
    @Override
    public void removeClassificatorValue(Classificator classificator, ClassificatorValue classificatorValue) {
        nodeService.removeChild(classificator.getNodeRef(), classificatorValue.getNodeRef());
        if (log.isDebugEnabled()) {
            log.debug("Node (" + classificatorValue.getNodeRef() + ") removed: " + classificatorValue);
        }
    }

    @Override
    public void removeClassificatorValueByNodeRef(Classificator classificator, String ref) {
        NodeRef nodeRef = new NodeRef(ref);
        removeClassificatorValueByNodeRef(classificator, nodeRef);
    }

    @Override
    public void removeClassificatorValueByNodeRef(Classificator classificator, NodeRef nodeRef) {
        nodeService.removeChild(classificator.getNodeRef(), nodeRef);
        if (log.isDebugEnabled()) {
            log.debug("Node (" + nodeRef + ") removed.");
        }
    }
    
    @Override
    public NodeRef addClassificatorValue(Classificator classificator, ClassificatorValue classificatorValue) {
        Map<QName, Serializable> properties = classificatorValueBeanPropertyMapper.toProperties(classificatorValue);
        ChildAssociationRef assoc = nodeService.createNode(classificator.getNodeRef(), ClassificatorModel.Associations.CLASSIFICATOR_VALUE,
                ClassificatorModel.Types.CLASSIFICATOR_VALUE,
                ClassificatorModel.Types.CLASSIFICATOR_VALUE,
                properties);
        if (log.isDebugEnabled()) {
            log.debug("Node (" + assoc.getChildRef() + ") added: " + classificatorValue);
        }
        return assoc.getChildRef();
    }
    
    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
    // END: getters / setters
}
