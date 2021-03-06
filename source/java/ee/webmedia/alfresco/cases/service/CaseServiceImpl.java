package ee.webmedia.alfresco.cases.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.service.NodeBasedObjectCallback;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

/**
 * Service class for cases
 */
public class CaseServiceImpl implements CaseService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(CaseServiceImpl.class);
    private static final BeanPropertyMapper<Case> caseBeanPropertyMapper = BeanPropertyMapper.newInstance(Case.class);

    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private GeneralService generalService;
    private UserService userService;
    private LogService logService;
    private DocumentService _documentService; // can not be set on bean creation!!
    private BulkLoadNodeService bulkLoadNodeService;
    private SimpleCache<NodeRef, UnmodifiableCase> caseCache;

    @Override
    public List<UnmodifiableCase> getAllCasesByVolume(NodeRef volumeRef, DocListUnitStatus status) {
        List<UnmodifiableCase> allCases = getAllCasesByVolume(volumeRef);
        List<UnmodifiableCase> cases = new ArrayList<>();
        for (UnmodifiableCase unmodifiableCase : allCases) {
            if (status.getValueName().equals(unmodifiableCase.getStatus())) {
                cases.add(unmodifiableCase);
            }
        }
        return cases;
    }

    @Override
    public List<UnmodifiableCase> getAllCasesByVolume(NodeRef volumeRef) {
        List<NodeRef> caseRefs = getCaseChildAssocsByVolume(volumeRef);
        List<UnmodifiableCase> caseOfVolume = new ArrayList<>(caseRefs.size());
        Map<Long, QName> propertyTypes = new HashMap<>();
        for (NodeRef caseRef : caseRefs) {
            UnmodifiableCase unmodifiableCase = getUnmodifiableCase(caseRef, propertyTypes);
            caseOfVolume.add(unmodifiableCase);
        }
        Collections.sort(caseOfVolume);
        return caseOfVolume;
    }

    private UnmodifiableCase getUnmodifiableCase(NodeRef caseRef, Map<Long, QName> propertyTypes) {
        UnmodifiableCase caseOfVolume = caseCache.get(caseRef);
        if (caseOfVolume == null) {
            // beanPropertyMapper is not used here because this method is very heavily used and direct method call should be faster than using reflection
            caseOfVolume = generalService.fetchObject(caseRef, null, new NodeBasedObjectCallback<UnmodifiableCase>() {

                @Override
                public UnmodifiableCase create(Node node) {
                    return new UnmodifiableCase(node);
                }
            }, propertyTypes);
            caseCache.put(caseRef, caseOfVolume);
        }
        return caseOfVolume;
    }

    @Override
    public int getCasesCountByVolume(NodeRef volumeRef) {
        return getCaseChildAssocsByVolume(volumeRef).size();
    }

    @Override
    public List<NodeRef> getCaseRefsByVolume(NodeRef volumeRef) {
        return getCaseChildAssocsByVolume(volumeRef);
    }

    private List<NodeRef> getCaseChildAssocsByVolume(NodeRef volumeRef) {
        return bulkLoadNodeService.loadChildRefs(volumeRef, CaseModel.Types.CASE);
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
    public String getCaseLabel(NodeRef caseRef) {
        UnmodifiableCase theCase = getUnmodifiableCase(caseRef, null);
        return theCase != null ? theCase.getCaseLabel() : "";
    }

    @Override
    public String getCaseTitle(NodeRef caseRef) {
        UnmodifiableCase theCase = getUnmodifiableCase(caseRef, null);
        return theCase != null ? theCase.getTitle() : "";
    }

    @Override
    public boolean isClosed(Node node) {
        return RepoUtil.isExistingPropertyValueEqualTo(node, CaseModel.Props.STATUS, DocListUnitStatus.CLOSED.getValueName());
    }

    @Override
    public void closeCase(Case aCase) {
        Map<String, Object> props = aCase.getNode().getProperties();
        props.put(CaseModel.Props.STATUS.toString(), DocListUnitStatus.CLOSED.getValueName());
        saveOrUpdate(aCase);
    }

    @Override
    public void openCase(Case theCase) {
        Map<String, Object> props = theCase.getNode().getProperties();
        props.put(CaseModel.Props.STATUS.toString(), DocListUnitStatus.OPEN.getValueName());
        saveOrUpdate(theCase);
    }

    @Override
    public void delete(Case theCase) {
        NodeRef caseRef = theCase.getNode().getNodeRef();
        List<NodeRef> documents = getDocumentService().getAllDocumentRefsByParentRefWithoutRestrictedAccess(caseRef);
        if (!documents.isEmpty()) {
            throw new UnableToPerformException("case_delete_not_empty");
        }
        nodeService.deleteNode(caseRef);
        removeFromCache(caseRef);
    }

    @Override
    public void closeAllCasesByVolume(NodeRef volumeRef) {
        final List<NodeRef> allCasesByVolume = getCaseRefsByVolume(volumeRef);
        for (NodeRef caseRef : allCasesByVolume) {
            closeCase(getCaseByNoderef(caseRef));
        }
    }

    @Override
    public void saveOrUpdate(Case theCase) {
        saveOrUpdate(theCase, true);
    }

    @Override
    public void saveOrUpdate(final Case theCase, boolean fromNodeProps) {
        final Node node = theCase.getNode();
        final boolean nodeIsNull = node == null;
        if (nodeIsNull) {
            fromNodeProps = false;
        }
        final boolean isNew = nodeIsNull || node instanceof TransientNode;
        @SuppressWarnings("null")
        final String title = StringUtils.strip((String) (fromNodeProps ? node.getProperties().get(CaseModel.Props.TITLE) : theCase.getTitle()));
        NodeRef newParentRef = theCase.getVolumeNodeRef();
        if (isCaseNameUsed(title, newParentRef, node != null ? node.getNodeRef() : null)) {
            final UnableToPerformException ex = new UnableToPerformException(MessageSeverity.ERROR, "case_save_error_caseNameUsed");
            ex.setMessageValuesForHolders(title);
            throw ex;
        }
        final Map<QName, Serializable> props;
        if (!fromNodeProps) {
            props = caseBeanPropertyMapper.toProperties(theCase);
        } else {
            @SuppressWarnings("null")
            final Map<QName, Serializable> qNameProperties = RepoUtil.toQNameProperties(node.getProperties());
            props = qNameProperties;
        }

        if (isNew || !newParentRef.equals(theCase.getNode().getProperties().get(DocumentCommonModel.Props.VOLUME))) {
            props.put(DocumentCommonModel.Props.VOLUME, newParentRef);
            NodeRef seriesRef = generalService.getPrimaryParent(newParentRef).getNodeRef();
            props.put(DocumentCommonModel.Props.SERIES, seriesRef);
            props.put(DocumentCommonModel.Props.FUNCTION, generalService.getPrimaryParent(seriesRef).getNodeRef());
        }
        NodeRef caseRef;
        if (isNew) { // save
            props.put(CaseModel.Props.TITLE, title);
            props.put(CaseModel.Props.CREATED, new Date());
            caseRef = nodeService.createNode(newParentRef,
                    CaseModel.Associations.CASE, CaseModel.Associations.CASE, CaseModel.Types.CASE, props).getChildRef();
            theCase.setNode(generalService.fetchNode(caseRef));
            logService.addLogEntry(LogEntry.create(LogObject.CASE, userService, caseRef, "applog_space_add", "", title));
        } else { // update
            @SuppressWarnings("null")
            Map<String, Object> stringQNameProperties = node.getProperties();
            stringQNameProperties.put(CaseModel.Props.TITLE.toString(), title);
            caseRef = node.getNodeRef();
            generalService.setPropertiesIgnoringSystem(caseRef, stringQNameProperties);
        }
        generalService.refreshMaterializedViews(CaseModel.Types.CASE);
        removeFromCache(caseRef);
    }

    @Override
    public void removeFromCache(NodeRef caseRef) {
        caseCache.remove(caseRef);
    }

    @Override
    public void saveAddedAssocs(Node caseNode) {
        generalService.saveAddedAssocs(caseNode);
    }

    @Override
    public Case createCase(NodeRef volumeRef) {
        Case theCase = new Case();
        theCase.setStatus(DocListUnitStatus.OPEN.getValueName());
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(CaseModel.Props.STATUS, DocListUnitStatus.OPEN.getValueName());

        TransientNode transientNode = TransientNode.createNew(dictionaryService, dictionaryService.getType(CaseModel.Types.CASE), null, props);
        theCase.setNode(transientNode);
        theCase.setVolumeNodeRef(volumeRef);
        return theCase;
    }

    @Override
    public boolean isCaseNameUsed(final String newCaseTitle, NodeRef volumeRef) {
        return isCaseNameUsed(newCaseTitle, volumeRef, null);
    }

    private boolean isCaseNameUsed(final String newCaseTitle, NodeRef volumeRef, NodeRef caseRef) {
        return getCaseByTitle(newCaseTitle, volumeRef, caseRef) != null;
    }

    @Override
    public UnmodifiableCase getCaseByTitle(final String newCaseTitle, NodeRef volumeRef, NodeRef caseRef) {
        final List<UnmodifiableCase> cases = getAllCasesByVolume(volumeRef);
        for (UnmodifiableCase theCase : cases) {
            if (StringUtils.equals(theCase.getTitle(), newCaseTitle) && (caseRef == null || !caseRef.equals(theCase.getNodeRef()))) {
                if (log.isDebugEnabled()) {
                    log.debug("found case that has the same name as name being checked for availability:\n" + theCase);
                }
                return theCase;
            }
        }
        return null;
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
        if (log.isTraceEnabled()) {
            log.trace("Found case: " + theCase);
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

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public DocumentService getDocumentService() {
        if (_documentService == null) {
            _documentService = BeanHelper.getDocumentService();
        }
        return _documentService;
    }

    // END: getters / setters

    public SimpleCache<NodeRef, UnmodifiableCase> getCaseCache() {
        return caseCache;
    }

    public void setCaseCache(SimpleCache<NodeRef, UnmodifiableCase> caseCache) {
        this.caseCache = caseCache;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

}
