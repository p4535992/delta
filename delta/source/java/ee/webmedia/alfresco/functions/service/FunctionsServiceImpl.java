package ee.webmedia.alfresco.functions.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.TransientNode;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

public class FunctionsServiceImpl implements FunctionsService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(FunctionsServiceImpl.class);
    private static BeanPropertyMapper<Function> functionsBeanPropertyMapper;

    static {
        functionsBeanPropertyMapper = BeanPropertyMapper.newInstance(Function.class);
    }

    private GeneralService generalService;
    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private SeriesService seriesService;
    private VolumeService volumeService;
    private CaseService caseService;
    private DocumentService documentService;

    @Override
    public List<Function> getAllFunctions() {
        return getFunctions(getFunctionsRoot());
    }

    @Override
    public List<Function> getFunctions(NodeRef functionsRoot) {
        List<ChildAssociationRef> childRefs = getFunctionAssocs(functionsRoot);
        List<Function> functions = new ArrayList<Function>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            functions.add(getFunctionByNodeRef(childRef.getChildRef()));
        }
        if (log.isDebugEnabled()) {
            log.debug("Functions found: " + functions);
        }
        Collections.sort(functions);
        return functions;
    }

    private List<ChildAssociationRef> getFunctionAssocs(NodeRef functionsRoot) {
        return nodeService.getChildAssocs(functionsRoot, RegexQNamePattern.MATCH_ALL, FunctionsModel.Associations.FUNCTION);
    }

    @Override
    public List<Function> getAllFunctions(DocListUnitStatus status) {
        List<Function> functions = getAllFunctions();
        for (Iterator<Function> i = functions.iterator(); i.hasNext();) {
            Function function = i.next();
            if (!status.getValueName().equals(function.getStatus())) {
                i.remove();
            }
        }
        return functions;
    }

    @Override
    public Function getFunctionByNodeRef(String ref) {
        return getFunctionByNodeRef(new NodeRef(ref));
    }

    @Override
    public Function getFunctionByNodeRef(NodeRef nodeRef) {
        Function function = functionsBeanPropertyMapper.toObject(nodeService.getProperties(nodeRef));
        function.setNode(generalService.fetchNode(nodeRef)); // FIXME: FacesContext can't be null
        if (log.isDebugEnabled()) {
            log.debug("Found Function: " + function);
        }
        return function;
    }

    @Override
    public void saveOrUpdate(Function function) {
        Map<String, Object> stringQNameProperties = function.getNode().getProperties();
        if (function.getNode() instanceof TransientNode) {
            TransientNode transientNode = (TransientNode) function.getNode();
            NodeRef functionNodeRef = nodeService.createNode(getFunctionsRoot(),
                    FunctionsModel.Associations.FUNCTION,
                    FunctionsModel.Associations.FUNCTION,
                    FunctionsModel.Types.FUNCTION,
                    RepoUtil.toQNameProperties(transientNode.getProperties())).getChildRef();
            function.setNode(generalService.fetchNode(functionNodeRef));
        } else {
            generalService.setPropertiesIgnoringSystem(function.getNode().getNodeRef(), stringQNameProperties);
        }
        if (log.isDebugEnabled()) {
            log.debug("Function updated: \n" + function);
        }

        reorderFunctions(function);

    }

    private void reorderFunctions(Function function) {
        final int order = getFunctionOrder(function);
        final List<Function> allFunctions = getAllFunctions();
        Collections.sort(allFunctions, new Comparator<Function>() {

            @Override
            public int compare(Function f1, Function f2) {
                final int order1 = getFunctionOrder(f1);
                final int order2 = getFunctionOrder(f2);
                if (order1 == order2) {
                    return 0;
                }
                return order1 < order2 ? -1 : 1;
            }

        });

        for (Function otherFunction : allFunctions) {
            if (function.getNode().getNodeRef().equals(otherFunction.getNode().getNodeRef())) {
                continue;
            }
            final int order2 = getFunctionOrder(otherFunction);
            if (order2 == order) {
                // since collection is ordered, no need to check if(order2 >= order)
                otherFunction.getNode().getProperties().put(FunctionsModel.Props.ORDER.toString(), order2 + 1);
                // reorderFunctions is recursively called on all following functions in the list by saveOrUpdate
                saveOrUpdate(otherFunction);
                break;
            }
        }
    }

    private Integer getFunctionOrder(Function function) {
        Integer order = (Integer) function.getNode().getProperties().get(FunctionsModel.Props.ORDER.toString());
        if (order == null) {
            order = Integer.MIN_VALUE;
        }
        return order;
    }

    @Override
    public Function createFunction() {
        Function function = new Function();
        Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
        props.put(FunctionsModel.Props.ORDER, getNextFunctionOrderNrByFunction());
        TransientNode transientNode = TransientNode.createNew(dictionaryService, dictionaryService.getType(FunctionsModel.Types.FUNCTION), null, props);
        function.setNode(transientNode);
        return function;
    }

    @Override
    public boolean closeFunction(Function function) {
        List<Series> allSeries = seriesService.getAllSeriesByFunction(function.getNodeRef());
        for (Series series : allSeries) {
            if (!DocListUnitStatus.CLOSED.equals(series.getStatus())) {
                return false;
            }
        }

        function.getNode().getProperties().put(FunctionsModel.Props.STATUS.toString(), DocListUnitStatus.CLOSED.getValueName());
        saveOrUpdate(function);
        return true;
    }

    @Override
    public void reopenFunction(Function function) {
        function.getNode().getProperties().put(FunctionsModel.Props.STATUS.toString(), DocListUnitStatus.OPEN.getValueName());
        saveOrUpdate(function);
    }

    private int getNextFunctionOrderNrByFunction() {
        int maxOrder = 0;
        for (Function fn : getAllFunctions()) {
            if (maxOrder < fn.getOrder()) {
                maxOrder = fn.getOrder();
            }
        }
        return maxOrder + 1;
    }

    @Override
    public NodeRef getFunctionsRoot() {
        return generalService.getNodeRef(FunctionsModel.Repo.FUNCTIONS_SPACE);
    }

    @Override
    public Location getDocumentListLocation() {
        Location location = new Location(generalService.getStore());
        location.setPath(FunctionsModel.Repo.FUNCTIONS_SPACE);
        return location;
    }

    // START: getters / setters
    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    // END: getters / setters

    @Override
    public long createNewYearBasedVolumes() {
        log.info("creating copy of year-based volumes that are opened.");
        long counter = 0;
        for (Function function : getAllFunctions()) {
            for (Series series : seriesService.getAllSeriesByFunction(function.getNodeRef())) {
                for (Volume volume : volumeService.getAllValidVolumesBySeries(series.getNode().getNodeRef())) {
                    if (DocListUnitStatus.OPEN.equals(volume.getStatus()) && VolumeType.ANNUAL_FILE.equals(volume.getVolumeTypeEnum())) {
                        volumeService.copyVolume(volume);
                        counter++;
                        log.info("created copy of " + counter + " volume: [" + function.getMark() + "]" + function.getTitle()
                                + "/[" + series.getSeriesIdentifier() + "]" + series.getTitle() + "/[" + volume.getVolumeMark() + "]" + volume.getTitle());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("skipping volume: [" + function.getMark() + "]" + function.getTitle()
                                    + "/[" + series.getSeriesIdentifier() + "]" + series.getTitle() + "/[" + volume.getVolumeMark() + "]" + volume.getTitle());
                        }
                    }
                }
            }
        }
        log.info("created copy of " + counter + " year-based volumes that were opened.");
        return counter;
    }

    @Override
    public long closeAllOpenExpiredVolumes() {
        log.info("Closing all expired volumes that are opened");
        long counter = 0;
        for (Function function : getAllFunctions()) {
            for (Series series : seriesService.getAllSeriesByFunction(function.getNodeRef())) {
                for (Volume volume : volumeService.getAllOpenExpiredVolumesBySeries(series.getNode().getNodeRef())) {
                    volumeService.closeVolume(volume);
                    counter++;
                    log.info("Closed volume: [" + function.getMark() + "]" + function.getTitle() + "/[" + series.getSeriesIdentifier() + "]"
                            + series.getTitle() + "/[" + volume.getVolumeMark() + "]" + volume.getTitle() + ", validTo=" + volume.getValidTo());
                }
            }
        }
        log.info("Closed " + counter + " expired volumes that were opened");
        return counter;
    }

    @Override
    public long updateDocCounters() {
        long docCountInRepo = 0;
        log.info("Starting to update documents count in documentList");
        for (Function function : getAllFunctions()) {
            long docCountInFunction = 0;
            final NodeRef functionRef = function.getNodeRef();
            for (NodeRef seriesRef : seriesService.getAllSeriesRefsByFunction(functionRef)) {
                long docCountInSeries = 0;
                for (Volume volume : volumeService.getAllVolumesBySeries(seriesRef)) {
                    long docCountInVolume = 0;
                    final NodeRef volumeRef = volume.getNode().getNodeRef();
                    if (volume.isContainsCases()) {
                        for (Case aCase : caseService.getAllCasesByVolume(volumeRef)) {
                            final NodeRef caseRef = aCase.getNode().getNodeRef();
                            final List<NodeRef> allDocumentsByCase = documentService.getAllDocumentRefsByParentRef(caseRef);
                            final int documentsCountByCase = allDocumentsByCase.size();
                            nodeService.setProperty(caseRef, CaseModel.Props.CONTAINING_DOCS_COUNT, documentsCountByCase);
                            docCountInVolume += documentsCountByCase;
                        }
                    } else {
                        final List<NodeRef> allDocumentsByVolume = documentService.getAllDocumentRefsByParentRef(volumeRef);
                        final int documentsCountByVolume = allDocumentsByVolume.size();
                        docCountInVolume += documentsCountByVolume;
                    }
                    nodeService.setProperty(volumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT, docCountInVolume);
                    docCountInSeries += docCountInVolume;
                }
                nodeService.setProperty(seriesRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, docCountInSeries);
                docCountInFunction += docCountInSeries;
            }
            docCountInRepo += docCountInFunction;
        }
        log.info("Updated documents count in documentList. Found " + docCountInRepo + " documents.");
        return docCountInRepo;
    }

    @Override
    public Pair<List<NodeRef>, Long> getAllDocumentAndCaseRefs() {
        long deletedDocCount = 0;
        log.info("Starting to gather all noderefs of documents and cases to be removed");
        final ArrayList<NodeRef> c = new ArrayList<NodeRef>(4000);
        for (ChildAssociationRef function : getFunctionAssocs(getFunctionsRoot())) {
            long docCountInFunction = 0;
            final NodeRef functionRef = function.getChildRef();
            for (NodeRef seriesRef : seriesService.getAllSeriesRefsByFunction(functionRef)) {
                long docCountInSeries = 0;
                for (ChildAssociationRef volume : volumeService.getAllVolumeRefsBySeries(seriesRef)) {
                    long docCountInVolume = 0;
                    final NodeRef volumeRef = volume.getChildRef();
                    Boolean containsCases = (Boolean) nodeService.getProperty(volumeRef, VolumeModel.Props.CONTAINS_CASES);
                    if (containsCases != null && containsCases) {
                        for (ChildAssociationRef aCase : caseService.getCaseRefsByVolume(volumeRef)) {
                            final NodeRef caseRef = aCase.getChildRef();
                            Integer docsUnderCase = (Integer) nodeService.getProperty(caseRef, CaseModel.Props.CONTAINING_DOCS_COUNT);
                            c.add(caseRef);
                            docCountInVolume += docsUnderCase != null ? docsUnderCase : 0;
                        }
                    } else {
                        List<ChildAssociationRef> docsOfVolumeAssocs = nodeService.getChildAssocs(volumeRef, RegexQNamePattern.MATCH_ALL,
                                RegexQNamePattern.MATCH_ALL);
                        final int documentsCountByVolume = docsOfVolumeAssocs.size();
                        for (ChildAssociationRef childAssociationRef : docsOfVolumeAssocs) {
                            c.add(childAssociationRef.getChildRef());
                        }
                        docCountInVolume += documentsCountByVolume;
                    }
                    docCountInSeries += docCountInVolume;
                }
                docCountInFunction += docCountInSeries;
            }
            deletedDocCount += docCountInFunction;
        }
        log.info("found " + deletedDocCount + " documents from documentList to delete.");
        return new Pair<List<NodeRef>, Long>(c, deletedDocCount);
    }

}
