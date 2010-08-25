package ee.webmedia.alfresco.functions.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.importer.excel.service.DocumentImportServiceImpl;
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
                    FunctionsModel.Types.FUNCTION,
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
                    if (DocListUnitStatus.OPEN.equals(volume.getStatus()) && VolumeType.YEAR_BASED.equals(volume.getVolumeType())) {
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
    public long updateDocCounters() {
        long docCountInRepo = 0;
        log.info("Starting to update documents count in documentList");
        for (Function function : getAllFunctions()) {
            long docCountInFunction = 0;
            final NodeRef functionRef = function.getNodeRef();
            for (Series series : seriesService.getAllSeriesByFunction(functionRef)) {
                long docCountInSeries = 0;
                final NodeRef seriesRef = series.getNode().getNodeRef();
                for (Volume volume : volumeService.getAllVolumesBySeries(seriesRef)) {
                    long docCountInVolume = 0;
                    final NodeRef volumeRef = volume.getNode().getNodeRef();
                    if (volume.isContainsCases()) {
                        for (Case aCase : caseService.getAllCasesByVolume(volumeRef)) {
                            final NodeRef caseRef = aCase.getNode().getNodeRef();
                            final List<Document> allDocumentsByCase = documentService.getAllDocumentsByCase(caseRef);
                            for (Document doc : allDocumentsByCase) {
                                final Map<String, Object> props = doc.getNode().getProperties();
                                props.put(DocumentCommonModel.Props.CASE.toString(), caseRef.toString());
                                setFunctionSeriesVolumeRefs(functionRef, seriesRef, volumeRef, doc.getNodeRef(), props);
                            }
                            final int documentsCountByCase = allDocumentsByCase.size();
                            nodeService.setProperty(caseRef, CaseModel.Props.CONTAINING_DOCS_COUNT, documentsCountByCase);
                            docCountInVolume += documentsCountByCase;
                        }
                    } else {
                        final List<Document> allDocumentsByVolume = documentService.getAllDocumentsByVolume(volumeRef);
                        final int documentsCountByVolume = allDocumentsByVolume.size();
                        for (Document doc : allDocumentsByVolume) {
                            final Map<String, Object> props = doc.getNode().getProperties();
                            setFunctionSeriesVolumeRefs(functionRef, seriesRef, volumeRef, doc.getNodeRef(), props);
                        }
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
            for (ChildAssociationRef series : seriesService.getAllSeriesAssocsByFunction(functionRef)) {
                long docCountInSeries = 0;
                final NodeRef seriesRef = series.getChildRef();
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

    private void setFunctionSeriesVolumeRefs(final NodeRef functionRef, final NodeRef seriesRef, final NodeRef volumeRef, NodeRef docRef,
            final Map<String, Object> props) {
        props.put(DocumentCommonModel.Props.FUNCTION.toString(), functionRef.toString());
        props.put(DocumentCommonModel.Props.SERIES.toString(), seriesRef.toString());
        props.put(DocumentCommonModel.Props.VOLUME.toString(), volumeRef.toString());
        final Map<QName, Serializable> qNameProperties = RepoUtil.toQNameProperties(props);
        nodeService.setProperties(docRef, qNameProperties);
    }
}
