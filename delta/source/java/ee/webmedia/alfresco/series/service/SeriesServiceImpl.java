package ee.webmedia.alfresco.series.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.TransientNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.web.ListReorderHelper;
import ee.webmedia.alfresco.docadmin.web.NodeOrderModifier;
import ee.webmedia.alfresco.docconfig.generator.systematic.AccessRestrictionGenerator;
import ee.webmedia.alfresco.document.log.service.DocumentLogService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel.Privileges;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

public class SeriesServiceImpl implements SeriesService, BeanFactoryAware {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SeriesServiceImpl.class);
    private static BeanPropertyMapper<Series> seriesBeanPropertyMapper = BeanPropertyMapper.newInstance(Series.class);

    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private PermissionService permissionService;
    private GeneralService generalService;
    private UserService userService;
    private DocumentLogService docLogService;
    private LogService appLogService;
    private BeanFactory beanFactory;
    /** NB! not injected - use getter to obtain instance of volumeService */
    private VolumeService _volumeService;
    /** NB! not injected - use getter to obtain instance of functionsService */
    private FunctionsService _functionsService;

    @Override
    public List<NodeRef> getAllSeriesRefsByFunction(NodeRef functionRef) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(functionRef, RegexQNamePattern.MATCH_ALL, SeriesModel.Associations.SERIES);
        List<NodeRef> seriesRefs = new ArrayList<NodeRef>(childAssocs.size());
        for (ChildAssociationRef series : childAssocs) {
            seriesRefs.add(series.getChildRef());
        }
        return seriesRefs;
    }

    @Override
    public List<Series> getAllSeriesByFunction(NodeRef functionNodeRef) {
        List<NodeRef> seriesRefs = getAllSeriesRefsByFunction(functionNodeRef);
        List<Series> seriesOfFunction = new ArrayList<Series>(seriesRefs.size());
        for (NodeRef seriesNodeRef : seriesRefs) {
            seriesOfFunction.add(getSeriesByNoderef(seriesNodeRef, functionNodeRef));
        }
        Collections.sort(seriesOfFunction);
        return seriesOfFunction;
    }

    @Override
    public List<Series> getAllSeriesByFunction(NodeRef functionNodeRef, DocListUnitStatus status, Set<String> docTypeIds) {
        List<Series> series = getAllSeriesByFunction(functionNodeRef);
        for (Iterator<Series> i = series.iterator(); i.hasNext();) {
            Series s = i.next();
            if (!status.getValueName().equals(s.getStatus()) || !s.getDocType().containsAll(docTypeIds)) {
                i.remove();
            }
        }
        return series;
    }

    @Override
    public List<Series> getAllSeriesByFunctionForStructUnit(NodeRef functionNodeRef, Integer structUnitId) {
        List<ChildAssociationRef> seriesAssocs = nodeService.getChildAssocs(functionNodeRef, RegexQNamePattern.MATCH_ALL, SeriesModel.Associations.SERIES);
        List<Series> seriesOfFunction = new ArrayList<Series>(seriesAssocs.size());
        for (ChildAssociationRef series : seriesAssocs) {
            NodeRef seriesNodeRef = series.getChildRef();
            @SuppressWarnings("unchecked")
            List<Integer> structUnits = (List<Integer>) nodeService.getProperty(seriesNodeRef, SeriesModel.Props.STRUCT_UNIT);

            if (structUnits != null && structUnits.contains(structUnitId)) {
                seriesOfFunction.add(getSeriesByNoderef(seriesNodeRef, functionNodeRef));
            }
        }
        return seriesOfFunction;
    }

    @Override
    public Series getSeriesByNodeRef(String seriesNodeRef) {
        return getSeriesByNoderef(new NodeRef(seriesNodeRef), null);
    }

    @Override
    public Series getSeriesByNodeRef(NodeRef nodeRef) {
        return getSeriesByNoderef(nodeRef, null);
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
        if (series.getNode() instanceof TransientNode) { // save
            NodeRef seriesNodeRef = nodeService.createNode(series.getFunctionNodeRef(),
                    SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES, SeriesModel.Types.SERIES,
                    RepoUtil.toQNameProperties(stringQNameProperties, false, true)).getChildRef();
            setSeriesDefaultPermissionsOnCreate(seriesNodeRef);
            series.setNode(generalService.fetchNode(seriesNodeRef));

            Map<String, Object> props = series.getNode().getProperties();
            appLogService.addLogEntry(LogEntry.create(LogObject.SERIES, userService, seriesNodeRef, "applog_space_add",
                    props.get(SeriesModel.Props.SERIES_IDENTIFIER.toString()), props.get(SeriesModel.Props.TITLE.toString())));
        } else { // update
            final NodeRef seriesRef = series.getNode().getNodeRef();
            previousOrder = (Integer) nodeService.getProperty(seriesRef, SeriesModel.Props.ORDER);

            Map<QName, Serializable> repoProps = nodeService.getProperties(seriesRef);
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
                    .label(SeriesModel.Props.RETENTION_PERIOD, "series_retentionPeriod")
                    .label(SeriesModel.Props.DOC_NUMBER_PATTERN, "series_docNumberPattern")
                    .label(SeriesModel.Props.NEW_NUMBER_FOR_EVERY_DOC, "series_newNumberForEveryDoc")
                    .label(SeriesModel.Props.VALID_FROM_DATE, "series_validFromDate")
                    .label(SeriesModel.Props.VALID_TO_DATE, "series_validToDate")
                    .label(SeriesModel.Props.VOL_TYPE, "series_volType")
                    .label(SeriesModel.Props.VOL_REGISTER, "series_volRegister")
                    .label(SeriesModel.Props.VOL_NUMBER_PATTERN, "series_volNumberPattern")
                    .diff(repoProps, newProps);

            if (propDiff != null) {
                appLogService.addLogEntry(LogEntry.create(LogObject.SERIES, userService, seriesRef, "applog_space_edit",
                        series.getSeriesIdentifier(), series.getTitle(), propDiff));
            }

            PropDiffHelper labelProvider = new PropDiffHelper().watchAccessRights();
            for (QName accessRestrictionProp : AccessRestrictionGenerator.ACCESS_RESTRICTION_PROPS) {
                QName docComProp = QName.createQName(DocumentCommonModel.DOCCOM_URI, accessRestrictionProp.getLocalName());
                String accessReasonDiff = new PropDiffHelper().label(docComProp, labelProvider.getPropLabels().get(accessRestrictionProp)).diff(repoProps, newProps);
                if (accessReasonDiff != null) {
                    appLogService.addLogEntry(LogEntry.create(LogObject.SERIES, userService, seriesRef, "series_log_status_accessRestrictionChanged", accessReasonDiff));
                }
            }
            generalService.setPropertiesIgnoringSystem(seriesRef, stringQNameProperties);
        }
        if (performReorder) {
            reorderSeries(series, previousOrder);
        }
    }

    private void reorderSeries(Series series, Integer previousSeriesOrder) {
        final List<Series> allSeriesByFunction = getAllSeriesByFunction(series.getFunctionNodeRef());
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
        final String functionMark = getFunctionsService().getFunctionByNodeRef(functionNodeRef).getMark();
        final String initialSeriesIdentifier = functionMark + "-";
        series.setSeriesIdentifier(initialSeriesIdentifier);
        series.setInitialSeriesIdentifier(initialSeriesIdentifier);
        series.setValidFromDate(new Date());
        return series;
    }

    @Override
    public void setSeriesDefaultPermissionsOnCreate(NodeRef seriesRef) {
        addPermissions(seriesRef, UserService.AUTH_DOCUMENT_MANAGERS_GROUP, Arrays.asList(Privileges.VIEW_DOCUMENT_META_DATA));

        List<String> archivistsPermissionsToAdd = new ArrayList<String>();
        archivistsPermissionsToAdd.add(Privileges.VIEW_DOCUMENT_META_DATA);
        boolean caseVolumeEnabled = getVolumeService().isCaseVolumeEnabled();
        if (caseVolumeEnabled) {
            archivistsPermissionsToAdd.add(Privileges.VIEW_CASE_FILE);
        }
        addPermissions(seriesRef, UserService.AUTH_ARCHIVIST_GROUP, archivistsPermissionsToAdd);

        List<String> supervisionsPermissionsToAdd = new ArrayList<String>();
        supervisionsPermissionsToAdd.add(Privileges.VIEW_DOCUMENT_META_DATA);
        supervisionsPermissionsToAdd.add(Privileges.VIEW_DOCUMENT_FILES);
        addPermissions(seriesRef, UserService.AUTH_SUPERVISION_GROUP, supervisionsPermissionsToAdd);
    }

    private void addPermissions(NodeRef seriesRef, String authority, List<String> permissionsToAdd) {
        for (String permission : permissionsToAdd) {
            permissionService.setPermission(seriesRef, authority, permission, true);
        }
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
        final List<Volume> allVolumesOfSeries = getVolumeService().getAllVolumesBySeries(seriesRef);
        boolean allVolumesClosed = true;
        Map<String, Object> props = seriesNode.getProperties();
        if (!(seriesNode instanceof TransientNode)) {
            for (Volume volume : allVolumesOfSeries) {
                final Map<String, Object> volProps = volume.getNode().getProperties();
                if (StringUtils.equals(DocListUnitStatus.OPEN.getValueName(), (String) volProps.get(VolumeModel.Props.STATUS))) {
                    allVolumesClosed = false;
                    break;
                }
            }
        }
        if (!allVolumesClosed) {
            return false; // will not close series or its volumes
        }
        props.put(SeriesModel.Props.STATUS.toString(), DocListUnitStatus.CLOSED.getValueName());
        saveOrUpdate(series);
        return true;
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
        if (log.isDebugEnabled()) {
            // XXX:ruins everything due to order being NULL log.debug("Found series: " + series);
        }
        return series;
    }

    private int getNextSeriesOrderNrByFunction(NodeRef functionNodeRef) {
        int maxOrder = 0;
        for (Series fn : getAllSeriesByFunction(functionNodeRef)) {
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
    }

    // START: getters / setters
    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setDocLogService(DocumentLogService docLogService) {
        this.docLogService = docLogService;
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

}