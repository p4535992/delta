package ee.webmedia.alfresco.series.service;

import static ee.webmedia.alfresco.common.web.BeanHelper.getBulkLoadNodeService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;
import static ee.webmedia.alfresco.log.PropDiffHelper.value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.service.ApplicationConstantsBean;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.service.NodeBasedObjectCallback;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docadmin.web.ListReorderHelper;
import ee.webmedia.alfresco.docadmin.web.NodeOrderModifier;
import ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.eventplan.model.EventPlanModel;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.privilege.model.Privilege;
import ee.webmedia.alfresco.privilege.service.PrivilegeService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.model.UnmodifiableSeries;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

public class SeriesServiceImpl implements SeriesService, BeanFactoryAware {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SeriesServiceImpl.class);
    private static BeanPropertyMapper<Series> seriesBeanPropertyMapper = BeanPropertyMapper.newInstance(Series.class);

    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private PrivilegeService privilegeService;
    private GeneralService generalService;
    private UserService userService;
    private LogService appLogService;
    private BeanFactory beanFactory;
    private ApplicationConstantsBean applicationConstantsBean;
    /** NB! not injected - use getter to obtain instance of volumeService */
    private VolumeService _volumeService;
    /** NB! not injected - use getter to obtain instance of functionsService */
    private FunctionsService _functionsService;
    private BulkLoadNodeService bulkLoadNodeService;
    private SimpleCache<NodeRef, UnmodifiableSeries> seriesCache;
    private static final Map<String, QName> ACCESS_RESTRICTION_DOCCOM_NS_PROPS = new HashMap<>();
    private static final Set<QName> SERIES_DOCUMENT_TYPE_PROPS = new HashSet<>(Arrays.asList(SeriesModel.Props.STATUS, SeriesModel.Props.DOC_TYPE));

    @Override
    public List<NodeRef> getAllSeriesRefsByFunction(NodeRef functionRef) {
        return getSeriesByFunctionNodeRefs(functionRef);
    }

    private List<NodeRef> getSeriesByFunctionNodeRefs(NodeRef functionRef) {
        return bulkLoadNodeService.loadChildRefs(functionRef, SeriesModel.Types.SERIES);
    }

    @Override
    public List<UnmodifiableSeries> getAllSeriesByFunction(NodeRef functionNodeRef) {
        List<NodeRef> childRefs = getSeriesByFunctionNodeRefs(functionNodeRef);
        List<UnmodifiableSeries> seriesList = new ArrayList<>();
        Map<Long, QName> propertyTypes = new HashMap<>();
        for (NodeRef seriesRef : childRefs) {
            UnmodifiableSeries series = getUnmodifiableSeries(seriesRef, functionNodeRef, propertyTypes);
            seriesList.add(series);
        }
        Collections.sort(seriesList);
        return seriesList;
    }

    @Override
    public List<UnmodifiableSeries> getAllCaseFileSeriesByFunction(NodeRef functionNodeRef, DocListUnitStatus status) {
        List<UnmodifiableSeries> series = getAllSeriesByFunction(functionNodeRef);
        for (Iterator<UnmodifiableSeries> i = series.iterator(); i.hasNext();) {
            UnmodifiableSeries s = i.next();
            if (!status.getValueName().equals(s.getStatus()) || !s.getVolType().contains(VolumeType.CASE_FILE.name())) {
                i.remove();
            }
        }
        return series;
    }

    @Override
    public List<UnmodifiableSeries> getAllSeriesByFunction(NodeRef functionNodeRef, DocListUnitStatus status, Set<String> docTypeIds) {
        List<UnmodifiableSeries> series = getAllSeriesByFunction(functionNodeRef);
        for (Iterator<UnmodifiableSeries> i = series.iterator(); i.hasNext();) {
            UnmodifiableSeries s = i.next();
            if (!status.getValueName().equals(s.getStatus()) || !s.getDocTypes().containsAll(docTypeIds)) {
                i.remove();
            }
        }
        return series;
    }

    @Override
    public List<UnmodifiableSeries> getAllSeriesByFunctionForStructUnit(NodeRef functionNodeRef, String structUnitId) {
        List<UnmodifiableSeries> series = getAllSeriesByFunction(functionNodeRef);
        for (Iterator<UnmodifiableSeries> i = series.iterator(); i.hasNext();) {
            UnmodifiableSeries s = i.next();
            List<String> structUnits = s.getStructUnits();
            if (structUnits == null || !structUnits.contains(structUnitId)) {
                i.remove();
            }
        }
        return series;
    }

    @Override
    public Series getSeriesByNodeRef(NodeRef nodeRef) {
        return getSeriesByNoderef(nodeRef, null);
    }

    @Override
    public String getSeriesLabel(NodeRef seriesRef) {
        UnmodifiableSeries series = getUnmodifiableSeries(seriesRef, null);
        return series != null ? series.getSeriesLabel() : "";
    }

    @Override
    public boolean hasSeries(NodeRef functionNodeRef) {
        return !getSeriesByFunctionNodeRefs(functionNodeRef).isEmpty();
    }

    @Override
    public void saveOrUpdate(Series series) {
        saveOrUpdate(series, true);
    }

    @Override
    public void saveOrUpdateWithoutReorder(Series series) {
        saveOrUpdate(series, false);
    }

    private void saveOrUpdate(Series series, boolean performReorder) {
        Map<String, Object> stringQNameProperties = series.getNode().getProperties();
        Integer previousOrder = null;
        NodeRef seriesNodeRef;
        if (series.getNode() instanceof TransientNode) { // save
            seriesNodeRef = nodeService.createNode(series.getFunctionNodeRef(),
                    SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES, SeriesModel.Types.SERIES,
                    RepoUtil.toQNameProperties(stringQNameProperties, false, true)).getChildRef();
            setSeriesDefaultPermissionsOnCreate(seriesNodeRef);
            series.setNode(generalService.fetchNode(seriesNodeRef));
            BeanHelper.getPrivilegeService().setInheritParentPermissions(seriesNodeRef, false);

            Map<String, Object> props = series.getNode().getProperties();
            appLogService.addLogEntry(LogEntry.create(LogObject.SERIES, userService, seriesNodeRef, "applog_space_add",
                    props.get(SeriesModel.Props.SERIES_IDENTIFIER.toString()), props.get(SeriesModel.Props.TITLE.toString())));
        } else { // update
            seriesNodeRef = series.getNode().getNodeRef();
            previousOrder = (Integer) nodeService.getProperty(seriesNodeRef, SeriesModel.Props.ORDER);

            Map<QName, Serializable> repoProps = nodeService.getProperties(seriesNodeRef);
            Map<QName, Serializable> newProps = RepoUtil.toQNameProperties(stringQNameProperties);
            String propDiff = new PropDiffHelper()
            .label(SeriesModel.Props.STATUS, "series_status")
            .label(SeriesModel.Props.ORDER, "series_order")
            .label(SeriesModel.Props.SERIES_IDENTIFIER, "series_seriesIdentifier")
            .label(SeriesModel.Props.TITLE, "series_title")
            .label(SeriesModel.Props.REGISTER, "series_register")
            .label(SeriesModel.Props.INDIVIDUALIZING_NUMBERS, "series_individualizingNumbers")
            .label(SeriesModel.Props.STRUCT_UNIT, "series_structUnit")
            .label(SeriesModel.Props.TYPE, "series_type")
            .label(SeriesModel.Props.DOC_TYPE, "series_docType")
            .label(SeriesModel.Props.DOC_NUMBER_PATTERN, "series_docNumberPattern")
            .label(SeriesModel.Props.NEW_NUMBER_FOR_EVERY_DOC, "series_newNumberForEveryDoc")
            .label(SeriesModel.Props.VALID_FROM_DATE, "series_validFromDate")
            .label(SeriesModel.Props.VALID_TO_DATE, "series_validToDate")
            .label(SeriesModel.Props.VOL_TYPE, "series_volType")
            .label(SeriesModel.Props.VOL_REGISTER, "series_volRegister")
            .label(SeriesModel.Props.VOL_NUMBER_PATTERN, "series_volNumberPattern")
            .diff(repoProps, newProps);

            if (propDiff != null) {
                appLogService.addLogEntry(LogEntry.create(LogObject.SERIES, userService, seriesNodeRef, "applog_space_edit",
                        series.getSeriesIdentifier(), series.getTitle(), propDiff));
            }

            PropDiffHelper labelProvider = new PropDiffHelper().watchAccessRights();
            for (QName accessRestrictionProp : AccessRestrictionGenerator.ACCESS_RESTRICTION_PROPS) {
                QName docComProp = RepoUtil.getFromQNamePool(accessRestrictionProp.getLocalName(), DocumentCommonModel.DOCCOM_URI, ACCESS_RESTRICTION_DOCCOM_NS_PROPS);
                String accessReasonDiff = new PropDiffHelper().label(docComProp, labelProvider.getPropLabels().get(accessRestrictionProp)).diff(repoProps, newProps);
                if (accessReasonDiff != null) {
                    appLogService.addLogEntry(LogEntry.create(LogObject.SERIES, userService, seriesNodeRef, "series_log_status_accessRestrictionChanged", accessReasonDiff));
                }
            }

            NodeRef repoEventPlan = (NodeRef) repoProps.get(SeriesModel.Props.EVENT_PLAN);
            NodeRef newEventPlan = (NodeRef) newProps.get(SeriesModel.Props.EVENT_PLAN);
            if (!ObjectUtils.equals(repoEventPlan, newEventPlan)) {
                String emptyLabel = PropDiffHelper.getEmptyLabel();
                appLogService.addLogEntry(LogEntry.create(LogObject.SERIES, userService, seriesNodeRef, "series_log_eventplan_modified",
                        value((repoEventPlan == null || !nodeService.exists(repoEventPlan)) ? null
                                : nodeService.getProperty(repoEventPlan, EventPlanModel.Props.NAME), emptyLabel),
                                value((newEventPlan == null || !nodeService.exists(newEventPlan)) ? null
                                        : nodeService.getProperty(newEventPlan, EventPlanModel.Props.NAME), emptyLabel)
                        ));
            }
            generalService.refreshMaterializedViews(SeriesModel.Types.SERIES);
            generalService.setPropertiesIgnoringSystem(seriesNodeRef, stringQNameProperties);
        }
        removeFromCache(seriesNodeRef);
        if (performReorder) {
            reorderSeries(series, previousOrder);
        }
    }

    @Override
    public void removeFromCache(NodeRef seriesNodeRef) {
        seriesCache.remove(seriesNodeRef);
    }

    private void reorderSeries(Series series, Integer previousSeriesOrder) {
        final List<Series> allSeriesByFunction = getModifiableSeries(series.getFunctionNodeRef());
        // get Nodes of the Series
        List<Node> allSeriesNodesByFunction = new ArrayList<Node>(allSeriesByFunction.size());
        for (Series ser : allSeriesByFunction) {
            allSeriesNodesByFunction.add(ser.getNode());
        }

        // set previous order for each element
        NodeOrderModifier modifier = new NodeOrderModifier(SeriesModel.Props.ORDER);
        modifier.markBaseState(allSeriesNodesByFunction);
        for (Node node : allSeriesNodesByFunction) {
            if (node.getNodeRef().equals(series.getNode().getNodeRef())) {
                modifier.setPreviousOrder(node, previousSeriesOrder);
            }
        }

        // reorder
        ListReorderHelper.reorder(allSeriesNodesByFunction, modifier);

        // save new order
        for (Series series2 : allSeriesByFunction) {
            saveOrUpdate(series2, false);
        }
    }

    private List<Series> getModifiableSeries(NodeRef functionNodeRef) {
        List<NodeRef> seriesRefs = getAllSeriesRefsByFunction(functionNodeRef);
        List<Series> seriesOfFunction = new ArrayList<Series>(seriesRefs.size());
        for (NodeRef seriesNodeRef : seriesRefs) {
            seriesOfFunction.add(getSeriesByNoderef(seriesNodeRef, functionNodeRef));
        }
        Collections.sort(seriesOfFunction);
        return seriesOfFunction;
    }

    @Override
    public Series createSeries(NodeRef functionNodeRef) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
        int order = getNextSeriesOrderNrByFunction(functionNodeRef);
        props.put(SeriesModel.Props.ORDER, order);
        TransientNode transientNode = TransientNode.createNew(dictionaryService, dictionaryService.getType(SeriesModel.Types.SERIES), null, props);
        Series series = seriesBeanPropertyMapper.toObject(RepoUtil.toQNameProperties(transientNode.getProperties()));
        series.setNode(transientNode);
        series.setOrder(order);
        series.setFunctionNodeRef(functionNodeRef);
        final String functionMark = getFunctionsService().getUnmodifiableFunction(functionNodeRef, null).getMark();
        final String initialSeriesIdentifier = functionMark + "-";
        series.setSeriesIdentifier(initialSeriesIdentifier);
        series.setInitialSeriesIdentifier(initialSeriesIdentifier);
        series.setValidFromDate(new Date());
        series.setVolType(new ArrayList<String>());
        return series;
    }

    @Override
    public void setSeriesDefaultPermissionsOnCreate(NodeRef seriesRef) {
        addPermissions(seriesRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, Arrays.asList(Privilege.VIEW_DOCUMENT_META_DATA));

        List<Privilege> archivistsPermissionsToAdd = new ArrayList<Privilege>();
        archivistsPermissionsToAdd.add(Privilege.VIEW_DOCUMENT_META_DATA);
        boolean caseVolumeEnabled = applicationConstantsBean.isCaseVolumeEnabled();
        if (caseVolumeEnabled) {
            archivistsPermissionsToAdd.add(Privilege.VIEW_CASE_FILE);
        }
        addPermissions(seriesRef, UserService.AUTH_ARCHIVIST_GROUP, archivistsPermissionsToAdd);

        List<Privilege> supervisionsPermissionsToAdd = new ArrayList<Privilege>();
        supervisionsPermissionsToAdd.add(Privilege.VIEW_DOCUMENT_META_DATA);
        supervisionsPermissionsToAdd.add(Privilege.VIEW_DOCUMENT_FILES);
        addPermissions(seriesRef, UserService.AUTH_SUPERVISION_GROUP, supervisionsPermissionsToAdd);
    }

    private void addPermissions(NodeRef seriesRef, String authority, List<Privilege> permissionsToAdd) {
        privilegeService.setPermissions(seriesRef, authority, permissionsToAdd.toArray(new Privilege[permissionsToAdd.size()]));
    }

    @Override
    public boolean isClosed(Node node) {
        return RepoUtil.isExistingPropertyValueEqualTo(node, SeriesModel.Props.STATUS, DocListUnitStatus.CLOSED.getValueName());
    }

    @Override
    public boolean closeSeries(Series series) {
        final Node seriesNode = series.getNode();
        if (isClosed(seriesNode)) {
            return true;
        }
        final NodeRef seriesRef = seriesNode.getNodeRef();
        boolean allVolumesClosed = true;
        Map<String, Object> props = seriesNode.getProperties();
        if (!(seriesNode instanceof TransientNode)) {
            final List<UnmodifiableVolume> allVolumesOfSeries = getVolumeService().getAllVolumesBySeries(seriesRef);
            for (UnmodifiableVolume volume : allVolumesOfSeries) {
                if (StringUtils.equals(DocListUnitStatus.OPEN.getValueName(), volume.getStatus())) {
                    allVolumesClosed = false;
                    break;
                }
            }
        }
        if (!allVolumesClosed) {
            return false; // will not close series or its volumes
        }
        props.put(SeriesModel.Props.STATUS.toString(), DocListUnitStatus.CLOSED.getValueName());
        if (props.get(SeriesModel.Props.VALID_TO_DATE) == null) {
            props.put(SeriesModel.Props.VALID_TO_DATE.toString(), new Date());
        }
        saveOrUpdate(series);
        return true;
    }

    @Override
    public void delete(Series series) {
        NodeRef seriesRef = series.getNode().getNodeRef();
        List<NodeRef> allVolumes = getVolumeService().getAllVolumeRefsBySeries(seriesRef);
        if (!allVolumes.isEmpty()) {
            throw new UnableToPerformException("series_delete_not_empty");
        }
        nodeService.deleteNode(seriesRef);
        removeFromCache(seriesRef);
    }

    private boolean isInClosedFunction(Series series) {
        Serializable functionStatus = nodeService.getProperty(series.getFunctionNodeRef(), FunctionsModel.Props.STATUS);
        return DocListUnitStatus.CLOSED.getValueName().equals(functionStatus);
    }

    @Override
    public void openSeries(Series series) {
        final Node seriesNode = series.getNode();
        if (!isClosed(seriesNode)) {
            return;
        }
        if (isInClosedFunction(series)) {
            throw new UnableToPerformException("series_open_error_inClosedFunction");
        }
        Map<String, Object> props = seriesNode.getProperties();
        props.put(SeriesModel.Props.STATUS.toString(), DocListUnitStatus.OPEN.getValueName());
        saveOrUpdate(series);
    }

    @Override
    public Node getSeriesNodeByRef(NodeRef seriesNodeRef) {
        return generalService.fetchNode(seriesNodeRef);
    }

    /**
     * @param seriesNodeRef
     * @param functionNodeRef if null, then series.functionNodeRef is set using association of given seriesNodeRef
     * @return Series object with reference to corresponding functionNodeRef
     */
    private Series getSeriesByNoderef(NodeRef seriesNodeRef, NodeRef functionNodeRef) {
        if (!nodeService.getType(seriesNodeRef).equals(SeriesModel.Types.SERIES)) {
            throw new RuntimeException("Given noderef '" + seriesNodeRef + "' is not series type:\n\texpected '" + SeriesModel.Types.SERIES + "'\n\tbut got '"
                    + nodeService.getType(seriesNodeRef) + "'");
        }
        final Series series = new Series();
        series.setNode(getSeriesNodeByRef(seriesNodeRef)); // node needs to be set before setters are called by mapper
        seriesBeanPropertyMapper.toObject(nodeService.getProperties(seriesNodeRef), series);
        if (functionNodeRef == null) {
            List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(seriesNodeRef);
            if (parentAssocs.size() != 1) {
                throw new RuntimeException("Series is expected to have only one parent function, but got " + parentAssocs.size() + " matching the criteria.");
            }
            functionNodeRef = parentAssocs.get(0).getParentRef();
        }
        series.setFunctionNodeRef(functionNodeRef);
        return series;
    }

    @Override
    public UnmodifiableSeries getUnmodifiableSeries(NodeRef seriesRef, Map<Long, QName> propertyTypes) {
        return getUnmodifiableSeries(seriesRef, null, propertyTypes);
    }

    private UnmodifiableSeries getUnmodifiableSeries(NodeRef seriesRef, NodeRef functionRef, Map<Long, QName> propertyTypes) {
        UnmodifiableSeries series = seriesCache.get(seriesRef);
        if (series == null) {
            if (functionRef == null) {
                functionRef = nodeService.getPrimaryParent(seriesRef).getParentRef();
            }
            final NodeRef finalFunctionRef = functionRef;
            // beanPropertyMapper is not used here because this method is very heavily used and direct method call should be faster than using reflection
            series = generalService.fetchObject(seriesRef, null, new NodeBasedObjectCallback<UnmodifiableSeries>() {

                @Override
                public UnmodifiableSeries create(Node node) {
                    return new UnmodifiableSeries(node, finalFunctionRef);
                }
            }, propertyTypes);
            seriesCache.put(seriesRef, series);
        }
        return series;
    }

    private int getNextSeriesOrderNrByFunction(NodeRef functionNodeRef) {
        int maxOrder = 0;
        for (UnmodifiableSeries fn : getAllSeriesByFunction(functionNodeRef)) {
            if (maxOrder < fn.getOrder()) {
                maxOrder = fn.getOrder();
            }
        }
        return maxOrder + 1;
    }

    @Override
    public void updateContainingDocsCountByVolume(NodeRef seriesNodeRef, NodeRef volumeNodeRef, boolean volumeAdded) {
        Integer count = (Integer) nodeService.getProperty(volumeNodeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT);
        generalService.updateParentContainingDocsCount(seriesNodeRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, volumeAdded, count);
        removeFromCache(seriesNodeRef);
    }

    @Override
    public boolean hasSelectableSeries(NodeRef functionRef, boolean isSearchFilter, Set<String> idList, boolean forDocumentType) {
        if (isSearchFilter || idList == null) {
            return getBulkLoadNodeService().countChildNodes(functionRef, SeriesModel.Types.SERIES) > 0;
        } else if (getGeneralService().getStore().equals(functionRef.getStoreRef())) {
            if (forDocumentType) {
                return hasOpenSeriesForDocTypes(functionRef, DocListUnitStatus.OPEN, idList);
            }
            return hasCaseFileSeriesWithStatus(functionRef, DocListUnitStatus.OPEN);
        }
        return false;
    }

    private boolean hasOpenSeriesForDocTypes(NodeRef functionNodeRef, DocListUnitStatus status, Set<String> docTypeIds) {
        Map<NodeRef, Map<NodeRef, Map<QName, Serializable>>> allSeries = BeanHelper.getBulkLoadNodeService().loadChildNodes(Collections.singleton(functionNodeRef),
                SERIES_DOCUMENT_TYPE_PROPS);
        Map<NodeRef, Map<QName, Serializable>> seriesProps = allSeries.get(functionNodeRef);
        if (seriesProps == null) {
            return false;
        }
        for (Entry<NodeRef, Map<QName, Serializable>> s : seriesProps.entrySet()) {
            Map<QName, Serializable> props = s.getValue();
            List<String> docTypes = (List<String>) props.get(SeriesModel.Props.DOC_TYPE);
            if (docTypes != null && docTypes.containsAll(docTypeIds) && status.getValueName().equals(props.get(SeriesModel.Props.STATUS))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCaseFileSeriesWithStatus(NodeRef functionNodeRef, DocListUnitStatus status) {
        List<UnmodifiableSeries> series = getAllSeriesByFunction(functionNodeRef);
        String caseFileTypeName = VolumeType.CASE_FILE.name();
        for (UnmodifiableSeries s : series) {
            if (status.getValueName().equals(s.getStatus()) && s.getVolType().contains(caseFileTypeName)) {
                return true;
            }
        }
        return false;
    }

    // START: getters / setters
    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPrivilegeService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setAppLogService(LogService appLogService) {
        this.appLogService = appLogService;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /** To break circular dependency */
    private FunctionsService getFunctionsService() {
        if (_functionsService == null) {
            _functionsService = (FunctionsService) beanFactory.getBean(FunctionsService.BEAN_NAME);
        }
        return _functionsService;
    }

    /**
     * To break Circular dependency between VolumeService and SeriesService
     *
     * @return VolumeService
     */
    private VolumeService getVolumeService() {
        if (_volumeService == null) {
            _volumeService = (VolumeService) beanFactory.getBean(VolumeService.BEAN_NAME);
        }
        return _volumeService;
    }

    // END: getters / setters

    public void setSeriesCache(SimpleCache<NodeRef, UnmodifiableSeries> seriesCache) {
        this.seriesCache = seriesCache;
    }

    public void setApplicationConstantsBean(ApplicationConstantsBean applicationConstantsBean) {
        this.applicationConstantsBean = applicationConstantsBean;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

}
