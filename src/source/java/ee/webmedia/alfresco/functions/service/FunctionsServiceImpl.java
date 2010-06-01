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

    public List<Function> getFunctions(NodeRef functionsRoot) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(functionsRoot, RegexQNamePattern.MATCH_ALL, FunctionsModel.Associations.FUNCTION);
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
        function.setNode(generalService.fetchNode(nodeRef));
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
            for (Series series : seriesService.getAllSeriesByFunction(function.getNodeRef())) {
                long docCountInSeries = 0;
                final NodeRef volumeRef = series.getNode().getNodeRef();
                for (Volume volume : volumeService.getAllVolumesBySeries(volumeRef)) {
                    long docCountInVolume = 0;
                    if (volume.isContainsCases()) {
                        for (Case aCase : caseService.getAllCasesByVolume(volume.getNode().getNodeRef())) {
                            final NodeRef caseRef = aCase.getNode().getNodeRef();
                            final int documentsCountByCase = documentService.getDocumentsCountByVolumeOrCase(caseRef);
                            nodeService.setProperty(caseRef, CaseModel.Props.CONTAINING_DOCS_COUNT, documentsCountByCase);
                            docCountInVolume += documentsCountByCase;
                        }
                    } else {
                        final int documentsCountByVolume = documentService.getDocumentsCountByVolumeOrCase(volume.getNode().getNodeRef());
                        docCountInVolume += documentsCountByVolume;
                    }
                    nodeService.setProperty(volumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT, docCountInVolume);
                    docCountInSeries += docCountInVolume;
                }
                nodeService.setProperty(volumeRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, docCountInSeries);
                docCountInFunction += docCountInSeries;
            }
            docCountInRepo += docCountInFunction;
        }
        log.info("Updated documents count in documentList. Found " + docCountInRepo + " documents.");
        return docCountInRepo;
    }
    // END: getters / setters
}
