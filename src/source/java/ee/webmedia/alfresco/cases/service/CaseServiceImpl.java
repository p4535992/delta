package ee.webmedia.alfresco.cases.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

/**
 * Service class for cases
 * 
 * @author Ats Uiboupin
 */
public class CaseServiceImpl implements CaseService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(CaseServiceImpl.class);
    private static final BeanPropertyMapper<Case> caseBeanPropertyMapper = BeanPropertyMapper.newInstance(Case.class);

    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private GeneralService generalService;

    @Override
    public List<Case> getAllCasesByVolume(NodeRef volumeRef) {
        List<ChildAssociationRef> caseAssocs = nodeService.getChildAssocs(volumeRef, RegexQNamePattern.MATCH_ALL, CaseModel.Associations.CASE);
        List<Case> caseOfVolume = new ArrayList<Case>(caseAssocs.size());
        for (ChildAssociationRef caseARef : caseAssocs) {
            NodeRef caseNodeRef = caseARef.getChildRef();
            caseOfVolume.add(getCaseByNoderef(caseNodeRef, volumeRef));
        }
        return caseOfVolume;
    }

    @Override
    public Case getCaseByNoderef(String caseNodeRef) {
        return getCaseByNoderef(new NodeRef(caseNodeRef), null);
    }
    
    @Override
    public Case getCaseByNoderef(NodeRef caseNodeRef) {
        return getCaseByNoderef(caseNodeRef, null);
    }
    
    @Override
    public boolean isClosed(Node node) {
        return generalService.isExistingPropertyValueEqualTo(node, CaseModel.Props.STATUS, DocListUnitStatus.CLOSED);
    }

    @Override
    public void closeCase(Case theCase) {
        Map<String, Object> props = theCase.getNode().getProperties();
        props.put(CaseModel.Props.STATUS.toString(), DocListUnitStatus.CLOSED.getValueName());
        saveOrUpdate(theCase);
    }

    @Override
    public void closeAllCasesByVolume(NodeRef volumeRef) {
        final List<Case> allCasesByVolume = getAllCasesByVolume(volumeRef);
        for (Case aCase : allCasesByVolume) {
            closeCase(aCase);
        }
    }

    @Override
    public void saveOrUpdate(Case theCase) {
        saveOrUpdate(theCase, true);
    }

    @Override
    public void saveOrUpdate(Case theCase, boolean fromNodeProps) {

        final Map<QName, Serializable> props;
        if (!fromNodeProps) {
            props = caseBeanPropertyMapper.toProperties(theCase);
        } else {
            props = RepoUtil.toQNameProperties(theCase.getNode().getProperties());
        }

        Map<String, Object> stringQNameProperties = theCase.getNode().getProperties();
        if (theCase.getNode() instanceof TransientNode) { // save
            NodeRef caseRef = nodeService.createNode(theCase.getVolumeNodeRef(),
                    CaseModel.Associations.CASE, CaseModel.Associations.CASE, CaseModel.Types.CASE, props).getChildRef();
            theCase.setNode(generalService.fetchNode(caseRef));
        } else { // update
            generalService.setPropertiesIgnoringSystem(theCase.getNode().getNodeRef(), stringQNameProperties);
        }
    }

    @Override
    public Case createCase(NodeRef volumeRef) {
        Case theCase = new Case();
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(CaseModel.Props.STATUS, DocListUnitStatus.OPEN.getValueName());

        TransientNode transientNode = TransientNode.createNew(dictionaryService, dictionaryService.getType(CaseModel.Types.CASE), null, props);
        theCase.setNode(transientNode);
        theCase.setVolumeNodeRef(volumeRef);
        return theCase;
    }

    @Override
    public Node getCaseNodeByRef(NodeRef caseRef) {
        return generalService.fetchNode(caseRef);
    }

    /**
     * @param caseRef
     * @param volumeRef if null, then case.volumeNodeRef is set using association of given caseRef
     * @return Case object with reference to corresponding volumeNodeRef
     */
    private Case getCaseByNoderef(NodeRef caseRef, NodeRef volumeRef) {
        if (!nodeService.getType(caseRef).equals(CaseModel.Types.CASE)) {
            throw new RuntimeException("Given noderef '" + caseRef + "' is not case type:\n\texpected '" + CaseModel.Types.CASE + "'\n\tbut got '"
                    + nodeService.getType(caseRef) + "'");
        }
        Case theCase = caseBeanPropertyMapper.toObject(nodeService.getProperties(caseRef));
        if (volumeRef == null) {
            List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(caseRef);
            if (parentAssocs.size() != 1) {
                throw new RuntimeException("Case is expected to have only one parent volume, but got " + parentAssocs.size() + " matching the criteria.");
            }
            volumeRef = parentAssocs.get(0).getParentRef();
        }
        theCase.setVolumeNodeRef(volumeRef);
        theCase.setNode(getCaseNodeByRef(caseRef));
        if (log.isDebugEnabled()) {
            log.debug("Found case: " + theCase);
        }
        return theCase;
    }

    // START: getters / setters
    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    // END: getters / setters

}
