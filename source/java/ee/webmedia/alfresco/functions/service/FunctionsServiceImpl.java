package ee.webmedia.alfresco.functions.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.view.Location;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.EqualsHelper;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.service.NodeBasedObjectCallback;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.model.UnmodifiableFunction;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.series.model.UnmodifiableSeries;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
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
    private UserService userService;
    private LogService logService;
    private DocumentService documentService;
    private SimpleCache<NodeRef, UnmodifiableFunction> functionCache;

    @Override
    public List<UnmodifiableFunction> getAllFunctions() {
        return getFunctions(getFunctionsRoot());
    }

    private List<UnmodifiableFunction> getArchivalStoreFunctions() {
        return getFunctions(getArchivalFunctionsRoot());
    }

    @Override
    public List<UnmodifiableFunction> getFunctions(NodeRef functionsRoot) {
        List<ChildAssociationRef> childRefs = getFunctionAssocs(functionsRoot);
        List<UnmodifiableFunction> functions = new ArrayList<UnmodifiableFunction>(childRefs.size());
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (ChildAssociationRef childRef : childRefs) {
            NodeRef functionRef = childRef.getChildRef();
            UnmodifiableFunction function = getUnmodifiableFunction(functionRef, propertyTypes);
            functions.add(function);
        }
        Collections.sort(functions);
        return functions;
    }

    @Override
    public List<UnmodifiableFunction> getAllFunctions(DocListUnitStatus... statuses) {
        List<UnmodifiableFunction> functions = getAllSpaceStoreFunctions(statuses);
        List<UnmodifiableFunction> archivalFunctions = getArchivalStoreFunctions();
        List<String> statusNames = DocListUnitStatus.getStatusNames(statuses);
        for (Iterator<UnmodifiableFunction> i = archivalFunctions.iterator(); i.hasNext();) {
            UnmodifiableFunction function = i.next();
            if (!statusNames.contains(function.getStatus())) {
                i.remove();
            }
        }
        functions.addAll(archivalFunctions);
        return functions;
    }

    @Override
    public List<UnmodifiableFunction> getAllSpaceStoreFunctions(DocListUnitStatus... statuses) {
        List<UnmodifiableFunction> functions = getAllFunctions();
        List<String> statusNames = DocListUnitStatus.getStatusNames(statuses);
        for (Iterator<UnmodifiableFunction> i = functions.iterator(); i.hasNext();) {
            UnmodifiableFunction function = i.next();
            if (!statusNames.contains(function.getStatus())) {
                i.remove();
            }
        }
        return functions;
    }

    @Override
    public String getFunctionLabel(NodeRef functionRef) {
        UnmodifiableFunction function = getUnmodifiableFunction(functionRef, null);
        return function != null ? function.getFunctionLabel() : "";
    }

    @Override
    public UnmodifiableFunction getUnmodifiableFunction(NodeRef functionRef, Map<Long, QName> propertyTypes) {
        UnmodifiableFunction function = functionCache.get(functionRef);
        if (function == null) {
            // functionsBeanPropertyMapper is not used here because this method is very heavily used and direct method call should be faster than using reflection
            function = generalService.fetchObject(functionRef, null, new NodeBasedObjectCallback<UnmodifiableFunction>() {

                @Override
                public UnmodifiableFunction create(Node node) {
                    return new UnmodifiableFunction(node);
                }
            }, propertyTypes);
            functionCache.put(functionRef, function);
        }
        return function;
    }

    @Override
    public Function getFunction(NodeRef functionRef, Map<Long, QName> propertyTypes) {
        Node functionNode = generalService.fetchNode(functionRef, propertyTypes);
        Function function = functionsBeanPropertyMapper.toObject(RepoUtil.toQNameProperties(functionNode.getProperties()));
        function.setNode(functionNode);
        if (log.isDebugEnabled()) {
            log.debug("Found Function: " + function);
        }
        return function;
    }

    private List<Function> getModifiableFunctions(NodeRef functionsRoot) {
        List<ChildAssociationRef> childRefs = getFunctionAssocs(functionsRoot);
        List<Function> functions = new ArrayList<Function>(childRefs.size());
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (ChildAssociationRef childRef : childRefs) {
            NodeRef functionRef = childRef.getChildRef();
            Function function = getFunction(functionRef, propertyTypes);
            functions.add(function);
        }
        Collections.sort(functions);
        return functions;
    }

    @Override
    public List<ChildAssociationRef> getFunctionAssocs(NodeRef functionsRoot) {
        return nodeService.getChildAssocs(functionsRoot, RegexQNamePattern.MATCH_ALL, FunctionsModel.Associations.FUNCTION);
    }

    @Override
    public List<NodeRef> getAllLimitedActivityFunctions() {
        List<NodeRef> functionRefs = new ArrayList<NodeRef>();
        for (UnmodifiableFunction function : getAllFunctions()) {
            if (function.isDocumentActivitiesAreLimited()) {
                functionRefs.add(function.getNodeRef());
            }
        }
        return functionRefs;
    }

    @Override
    public void saveOrUpdate(Function function) {
        saveOrUpdate(function, getFunctionsRoot());
    }

    @Override
    public Function saveOrUpdate(final Function function, NodeRef functionsRoot) {
        boolean activitiesLimitedChanged = true;
        Map<QName, Serializable> properties = RepoUtil.toQNameProperties(function.getNode().getProperties());
        Boolean documentActivitiesAreLimited = function.isDocumentActivitiesAreLimited();
        NodeRef functionRef = function.getNodeRef();
        if (function.getNode() instanceof TransientNode) {
            functionRef = nodeService.createNode(functionsRoot,
                    FunctionsModel.Associations.FUNCTION,
                    FunctionsModel.Associations.FUNCTION,
                    FunctionsModel.Types.FUNCTION,
                    properties).getChildRef();
            logService.addLogEntry(LogEntry.create(LogObject.FUNCTION, userService, functionRef, "applog_space_add",
                    function.getMark(), function.getTitle()));
            Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
            Node functionNode = generalService.fetchNode(functionRef, propertyTypes);
            function.setNode(functionNode);
        } else {
            Map<QName, Serializable> originalProperties = nodeService.getProperties(functionRef);
            String propDiff = new PropDiffHelper()
                    .label(FunctionsModel.Props.STATUS, "function_status")
                    .label(FunctionsModel.Props.ORDER, "function_status")
                    .label(FunctionsModel.Props.MARK, "function_mark")
                    .label(FunctionsModel.Props.TITLE, "function_title")
                    .label(FunctionsModel.Props.DESCRIPTION, "function_description")
                    .label(FunctionsModel.Props.TYPE, "function_type")
                    .label(FunctionsModel.Props.DOCUMENT_ACTIVITIES_ARE_LIMITED, "function_documentActivitiesAreLimited")
                    .diff(originalProperties, properties);

            if (propDiff != null) {
                logService.addLogEntry(LogEntry.create(LogObject.FUNCTION, userService, functionRef, "applog_space_edit",
                        function.getMark(), function.getTitle(), propDiff));
            }
            activitiesLimitedChanged = !EqualsHelper.nullSafeEquals(documentActivitiesAreLimited,
                    originalProperties.get(FunctionsModel.Props.DOCUMENT_ACTIVITIES_ARE_LIMITED));
            generalService.setPropertiesIgnoringSystem(properties, functionRef);
        }
        if (log.isDebugEnabled()) {
            log.debug("Function updated: \n" + function);
        }
        generalService.refreshMaterializedViews(FunctionsModel.Types.FUNCTION);
        removeFromCache(functionRef);
        reorderFunctions(function, functionsRoot);

        final NodeRef finalFunRef = functionRef;
        if (activitiesLimitedChanged && functionsRoot.equals(getFunctionsRoot()) && documentActivitiesAreLimited) {
            generalService.runOnBackground(new RunAsWork<Void>() {
                @Override
                public Void doWork() throws Exception {
                    BeanHelper.getMenuService().addFunctionVolumeShortcuts(finalFunRef);
                    return null;
                }
            }, "addFunctionVolumeShortcuts", true);
        }

        return getFunction(functionRef, null);

    }

    @Override
    public void removeFromCache(NodeRef functionRef) {
        functionCache.remove(functionRef);
    }

    private void reorderFunctions(Function function, NodeRef functionsRoot) {
        final int order = getFunctionOrder(function);
        final List<Function> allFunctions = getModifiableFunctions(functionsRoot);
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
            if (function.getNodeRef().equals(otherFunction.getNodeRef())) {
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
        List<UnmodifiableSeries> allSeries = seriesService.getAllSeriesByFunction(function.getNodeRef(), true);
        for (UnmodifiableSeries series : allSeries) {
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

    @Override
    public void delete(Function function) {
        if (seriesService.hasSeries(function.getNodeRef())) {
            throw new UnableToPerformException("function_delete_not_empty");
        }
        functionCache.remove(function.getNodeRef());
        nodeService.deleteNode(function.getNodeRef());
    }

    private int getNextFunctionOrderNrByFunction() {
        int maxOrder = 0;
        for (UnmodifiableFunction fn : getAllFunctions()) {
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

    private NodeRef getArchivalFunctionsRoot() {
        return generalService.getArchivalNodeRef(FunctionsModel.Repo.FUNCTIONS_SPACE);
    }


    @Override
    public Location getDocumentListLocation() {
        Location location = new Location(generalService.getStore());
        location.setPath(FunctionsModel.Repo.FUNCTIONS_SPACE);
        return location;
    }

    @Override
    public boolean isDraftsFunction(NodeRef functionRef) {
        if (functionRef == null) {
            return false;
        }
        return functionRef.equals(BeanHelper.getConstantNodeRefsBean().getDraftsRoot())
                || Boolean.TRUE.equals(nodeService.getProperty(functionRef, FunctionsModel.Props.DOCUMENT_ACTIVITIES_ARE_LIMITED));
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

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public SimpleCache<NodeRef, UnmodifiableFunction> getFunctionCache() {
        return functionCache;
    }

    public void setFunctionCache(SimpleCache<NodeRef, UnmodifiableFunction> functionLabels) {
        functionCache = functionLabels;
    }

    // END: getters / setters

}
