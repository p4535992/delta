package ee.webmedia.alfresco.functions.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.TransientNode;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;

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

    @Override
    public List<Function> getAllFunctions() {
        NodeRef root = getFunctionsRoot();
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(root, RegexQNamePattern.MATCH_ALL, FunctionsModel.Associations.FUNCTION);
        List<Function> functions = new ArrayList<Function>(childRefs.size());
        for (ChildAssociationRef childRef : childRefs) {
            functions.add(getFunctionByNodeRef(childRef.getChildRef()));
        }
        if (log.isDebugEnabled()) {
            log.debug("Functions found: " + functions);
        }
        return functions;
    }

    @Override
    public List<Function> getAllFunctions(DocListUnitStatus status) {
        List<Function> functions = getAllFunctions();
        for (Iterator<Function> i = functions.iterator(); i.hasNext(); ) {
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
    // END: getters / setters
}
