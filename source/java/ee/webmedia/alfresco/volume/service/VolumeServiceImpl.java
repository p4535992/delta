package ee.webmedia.alfresco.volume.service;

import static ee.webmedia.alfresco.classificator.enums.DocListUnitStatus.getStatusNames;
import static ee.webmedia.alfresco.common.web.BeanHelper.getEventPlanService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getFunctionsService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getGeneralService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.BulkLoadNodeService;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.service.NodeBasedObjectCallback;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.eventplan.service.EventPlanService;
import ee.webmedia.alfresco.log.PropDiffHelper;
import ee.webmedia.alfresco.log.model.LogEntry;
import ee.webmedia.alfresco.log.model.LogObject;
import ee.webmedia.alfresco.log.service.LogService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.DeletedDocument;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class VolumeServiceImpl implements VolumeService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(VolumeServiceImpl.class);
    private static final BeanPropertyMapper<Volume> volumeBeanPropertyMapper = BeanPropertyMapper.newInstance(Volume.class);
    private static final BeanPropertyMapper<DeletedDocument> deletedDocumentBeanPropertyMapper = BeanPropertyMapper.newInstance(DeletedDocument.class);

    private NodeService nodeService;
    private GeneralService generalService;
    private SeriesService seriesService;
    private CaseService caseService;
    private DocumentService documentService;
    private UserService userService;
    private LogService logService;
    private DocumentAdminService _documentAdminService;
    private EventPlanService eventPlanService;
    private BulkLoadNodeService bulkLoadNodeService;
    private SimpleCache<NodeRef, UnmodifiableVolume> volumeCache;

    @Override
    public List<NodeRef> getAllVolumeRefsBySeries(NodeRef seriesNodeRef) {
        List<NodeRef> childRefs = getVolumeRefs(seriesNodeRef);
        childRefs.addAll(getCaseFileRefs(seriesNodeRef));
        return childRefs;
    }

    private List<NodeRef> getCaseFileRefs(NodeRef seriesNodeRef) {
        return bulkLoadNodeService.loadChildRefs(seriesNodeRef, CaseFileModel.Assocs.CASE_FILE);
    }

    private List<NodeRef> getVolumeRefs(NodeRef seriesNodeRef) {
        return bulkLoadNodeService.loadChildRefs(seriesNodeRef, VolumeModel.Types.VOLUME);
    }

    @Override
    public List<UnmodifiableVolume> getAllVolumesBySeries(NodeRef seriesNodeRef) {
        List<NodeRef> volumeChildRefs = getVolumeRefs(seriesNodeRef);
        List<NodeRef> caseFileChildRefs = getCaseFileRefs(seriesNodeRef);
        List<UnmodifiableVolume> volumeList = new ArrayList<>(volumeChildRefs.size() + caseFileChildRefs.size());
        Map<Long, QName> propertyTypes = new HashMap<>();
        getVolumeFromChildAssoc(volumeChildRefs, volumeList, false, propertyTypes);
        getVolumeFromChildAssoc(caseFileChildRefs, volumeList, true, propertyTypes);
        Collections.sort(volumeList);
        return volumeList;
    }

    private void getVolumeFromChildAssoc(List<NodeRef> volumeChildRefs, List<UnmodifiableVolume> volumeList, boolean isDynamic, Map<Long, QName> propertyTypes) {
        for (NodeRef seriesRef : volumeChildRefs) {
            UnmodifiableVolume volume = getUnmodifiableVolume(seriesRef, isDynamic, propertyTypes);
            volumeList.add(volume);
        }
    }

    private UnmodifiableVolume getUnmodifiableVolume(NodeRef volumeRef, final boolean isDynamic, Map<Long, QName> propertyTypes) {
        UnmodifiableVolume volume = volumeCache.get(volumeRef);
        if (volume == null) {
            // beanPropertyMapper is not used here because this method is very heavily used and direct method call should be faster than using reflection
            volume = generalService.fetchObject(volumeRef, null, new NodeBasedObjectCallback<UnmodifiableVolume>() {

                @Override
                public UnmodifiableVolume create(Node node) {
                    return new UnmodifiableVolume(node, isDynamic);
                }
            }, propertyTypes);
            volumeCache.put(volumeRef, volume);
        }
        return volume;
    }

    @Override
    public List<UnmodifiableVolume> getAllValidVolumesBySeries(NodeRef seriesNodeRef, DocListUnitStatus... statuses) {
        List<UnmodifiableVolume> volumes = getAllValidVolumesBySeries(seriesNodeRef);
        List<String> statusNames = getStatusNames(statuses);
        for (Iterator<UnmodifiableVolume> i = volumes.iterator(); i.hasNext(); ) {
            UnmodifiableVolume volume = i.next();
            if (!statusNames.contains(volume.getStatus())) {
                i.remove();
            }
        }
        return volumes;
    }

    @Override
    public List<UnmodifiableVolume> getAllValidVolumesBySeries(NodeRef seriesNodeRef) {
        List<UnmodifiableVolume> volumes = getAllStartedVolumesBySeries(seriesNodeRef);
        final Calendar cal = Calendar.getInstance();
        for (Iterator<UnmodifiableVolume> i = volumes.iterator(); i.hasNext();) {
            UnmodifiableVolume volume = i.next();
            if (volume.getValidTo() != null) {
                Calendar validTo = Calendar.getInstance();
                validTo.setTime(volume.getValidTo());
                validTo.set(Calendar.HOUR_OF_DAY, 23);
                validTo.set(Calendar.MINUTE, 59);
                validTo.set(Calendar.SECOND, 59);
                if (cal.after(validTo)) {
                    log.debug("Skipping volume '" + volume.getTitle() + "', current date " + cal.getTime() + " is later than volume valid to date "
                            + validTo.getTime());
                    i.remove();
                }
            }
        }
        return volumes;
    }

    public List<UnmodifiableVolume> getAllStartedVolumesBySeries(NodeRef seriesNodeRef) {
        List<UnmodifiableVolume> volumes = getAllVolumesBySeries(seriesNodeRef);
        final Calendar cal = Calendar.getInstance();
        for (Iterator<UnmodifiableVolume> i = volumes.iterator(); i.hasNext();) {
            UnmodifiableVolume volume = i.next();
            Date validFrom = volume.getValidFrom();
            if (validFrom != null && cal.getTime().before(validFrom)) {
                log.debug("Skipping volume '" + volume.getTitle() + "', current date "
                        + cal.getTime() + " is earlier than volume valid from date " + validFrom);
                i.remove();
            }
        }
        return volumes;
    }

    @Override
    public List<UnmodifiableVolume> getAllOpenExpiredVolumesBySeries(NodeRef seriesNodeRef) {
        List<UnmodifiableVolume> volumes = getAllVolumesBySeries(seriesNodeRef);
        final Calendar cal = Calendar.getInstance();
        for (Iterator<UnmodifiableVolume> i = volumes.iterator(); i.hasNext();) {
            UnmodifiableVolume volume = i.next();

            if (!DocListUnitStatus.OPEN.equals(volume.getStatus())) {
                i.remove();
                continue;
            }

            if (volume.getValidTo() == null) {
                log.debug("Skipping volume '" + volume.getTitle() + "', validTo is null");
                i.remove();
                continue;
            }

            Calendar validTo = Calendar.getInstance();
            validTo.setTime(volume.getValidTo());
            validTo.set(Calendar.HOUR_OF_DAY, 23);
            validTo.set(Calendar.MINUTE, 59);
            validTo.set(Calendar.SECOND, 59);
            if (!cal.after(validTo)) {
                log.debug("Skipping volume '" + volume.getTitle() + "', current date " + cal.getTime() + " is not later than volume valid to date "
                        + validTo.getTime());
                i.remove();
                continue;
            }
        }
        return volumes;
    }

    @Override
    public UnmodifiableVolume getUnmodifiableVolume(NodeRef volumeRef, Map<Long, QName> propertyTypes) {
        if (!BeanHelper.getNodeService().exists(volumeRef)) { //TODO-Igor: temporarty for test
            return null;
        }
        UnmodifiableVolume volume = volumeCache.get(volumeRef);
        if (volume == null) {
            // beanPropertyMapper is not used here because this method is very heavily used and direct method call should be faster than using reflection
            volume = generalService.fetchObject(volumeRef, null, new NodeBasedObjectCallback<UnmodifiableVolume>() {

                @Override
                public UnmodifiableVolume create(Node node) {
                    return new UnmodifiableVolume(node, CaseFileModel.Types.CASE_FILE.equals(node.getType()));
                }
            }, propertyTypes);
            volumeCache.put(volumeRef, volume);
        }
        return volume;
    }

    @Override
    public String getVolumeLabel(NodeRef volumeRef) {
        UnmodifiableVolume volume = getUnmodifiableVolume(volumeRef, null);
        return volume != null ? volume.getVolumeLabel() : "";
    }

    @Override
    public Volume getVolumeByNodeRef(NodeRef volumeRef, Map<Long, QName> propertyTypes) {
        return getVolumeByNoderef(volumeRef, null, propertyTypes);
    }

    @Override
    public void saveOrUpdate(Volume volume) {
        saveOrUpdate(volume, true);
    }

    @Override
    public NodeRef saveOrUpdate(Volume volume, boolean fromNodeProps) {
        WmNode volumeNode = volume.getNode();
        boolean isNew = volumeNode.isUnsaved();
        Node series = isNew ? new Node(volume.getSeriesNodeRef()) : generalService.getPrimaryParent(volumeNode.getNodeRef());
        NodeRef newParentRef = series.getNodeRef();
        Map<String, Object> locationProps = new HashMap<String, Object>();
        NodeRef currentParenRef = (NodeRef) volume.getNode().getProperties().get(DocumentCommonModel.Props.SERIES);
        boolean needUpdateVolumeShortcuts = false;
        if (isNew || !newParentRef.equals(currentParenRef)) {
            locationProps.put(DocumentCommonModel.Props.SERIES.toString(), newParentRef);
            NodeRef newFunctionRef = generalService.getPrimaryParent(newParentRef).getNodeRef();
            locationProps.put(DocumentCommonModel.Props.FUNCTION.toString(), newFunctionRef);
            Node previousFunction = !isNew ? getGeneralService().getPrimaryParent(currentParenRef) : null;
            if ((previousFunction == null || !getFunctionsService().isDraftsFunction(previousFunction.getNodeRef()))
                    && getFunctionsService().isDraftsFunction(newFunctionRef)) {
                needUpdateVolumeShortcuts = true;
            }
        }
        Map<QName, Serializable> newProps = RepoUtil.toQNameProperties(volumeNode.getProperties());
        if (isNew) { // save
            Map<QName, Serializable> qNameProperties = fromNodeProps ? newProps
                    : volumeBeanPropertyMapper.toProperties(volume);
            qNameProperties.putAll(RepoUtil.toQNameProperties(locationProps));
            volume.setNode(createVolumeNode(volume.getSeriesNodeRef(), qNameProperties));

            Map<String, Object> props = volume.getNode().getProperties();
            NodeRef volRef = volume.getNode().getNodeRef();
            logService.addLogEntry(LogEntry.create(LogObject.VOLUME, userService, volRef, "applog_space_add",
                    props.get(VolumeModel.Props.VOLUME_MARK.toString()), props.get(VolumeModel.Props.TITLE.toString())));

            eventPlanService.initVolumeOrCaseFileFromSeriesEventPlan(volRef);

        } else { // update
            if (!checkContainsCasesValue(volume)) {
                throw new UnableToPerformException("volume_contains_docs_or_cases");
            }

            Map<QName, Serializable> repoProps = nodeService.getProperties(volumeNode.getNodeRef());
            String propDiff = new PropDiffHelper()
                    .label(VolumeModel.Props.STATUS, "volume_status")
                    .label(VolumeModel.Props.VOLUME_TYPE, "volume_volumeType")
                    .label(VolumeModel.Props.VOLUME_MARK, "volume_volumeMark")
                    .label(VolumeModel.Props.TITLE, "volume_title")
                    .label(VolumeModel.Props.DESCRIPTION, "volume_description")
                    .label(VolumeModel.Props.VALID_FROM, "volume_validFrom")
                    .label(VolumeModel.Props.VALID_TO, "volume_validTo")
                    .label(VolumeModel.Props.CASES_CREATABLE_BY_USER, "volume_casesCreatableByUser")
                    .diff(repoProps, newProps);
            if (propDiff != null) {
                logService.addLogEntry(LogEntry.create(LogObject.VOLUME, userService, volumeNode.getNodeRef(), "applog_space_edit",
                        volume.getVolumeMark(), volume.getTitle(), propDiff));
            }

            if (fromNodeProps) {
                volumeNode.getProperties().putAll(locationProps);
                generalService.setPropertiesIgnoringSystem(volumeNode.getNodeRef(), volumeNode.getProperties());
            } else {
                Map<String, Object> stringProperties = RepoUtil.toStringProperties(volumeBeanPropertyMapper.toProperties(volume));
                stringProperties.putAll(locationProps);
                generalService.setPropertiesIgnoringSystem(volumeNode.getNodeRef(), stringProperties);
            }

        }
        generalService.refreshMaterializedViews(VolumeModel.Types.VOLUME);
        final NodeRef volumeRef = volume.getNode().getNodeRef();
        if (needUpdateVolumeShortcuts) {
            getGeneralService().runOnBackground(new RunAsWork<Void>() {
                @Override
                public Void doWork() throws Exception {
                    BeanHelper.getMenuService().addVolumeShortcuts(volumeRef, false);
                    return null;
                }
            }, "addVolumeShortcuts", true);
        }
        removeFromCache(volumeRef);
        return volumeRef;
    }

    @Override
    public void removeFromCache(NodeRef volRef) {
        volumeCache.remove(volRef);
    }

    @Override
    public List<UnmodifiableVolume> getAllStartedVolumesBySeries(NodeRef seriesRef, DocListUnitStatus... statuses) {
        List<UnmodifiableVolume> volumes = getAllStartedVolumesBySeries(seriesRef);
        List<String> statusNames = getStatusNames(statuses);
        for (Iterator<UnmodifiableVolume> i = volumes.iterator(); i.hasNext(); ) {
            UnmodifiableVolume volume = i.next();
            if (!statusNames.contains(volume.getStatus())) {
                i.remove();
            }
        }
        return volumes;
    }

    private boolean isInClosedSeries(Volume volume) {
        Serializable seriesStatus = nodeService.getProperty(volume.getSeriesNodeRef(), SeriesModel.Props.STATUS);
        return DocListUnitStatus.CLOSED.getValueName().equals(seriesStatus);
    }

    private boolean checkContainsCasesValue(Volume volume) {
        Volume repoVolume = getVolumeByNodeRef(volume.getNode().getNodeRef(), null);
        Boolean containsCasesValue = (Boolean) volume.getNode().getProperties().get(VolumeModel.Props.CONTAINS_CASES);
        Boolean origContainsCasesValue = (Boolean) repoVolume.getNode().getProperties().get(VolumeModel.Props.CONTAINS_CASES);
        if (origContainsCasesValue != null && !origContainsCasesValue.equals(containsCasesValue)) {
            return caseService.getCasesCountByVolume(volume.getNode().getNodeRef()) == 0;
        }
        return true;
    }

    @Override
    public Volume copyVolume(Volume baseVolume) {
        return createOrCopyVolume(baseVolume.getSeriesNodeRef(), baseVolume);
    }

    @Override
    public Volume createVolume(NodeRef seriesNodeRef) {
        return createOrCopyVolume(seriesNodeRef, null);
    }

    private Volume createOrCopyVolume(NodeRef seriesNodeRef, Volume baseVolume) {
        final Volume volume;
        final String volumeMark;
        if (baseVolume != null) {
            final String baseVolumeStatus = baseVolume.getStatus();
            if (!DocListUnitStatus.OPEN.equals(baseVolumeStatus)) {
                throw new IllegalArgumentException("You should probably not create a copy of volume that has status '" + baseVolumeStatus + "'");
            }
            final Map<QName, Serializable> baseProps = generalService.getPropertiesIgnoringSystem(baseVolume.getNode().getProperties());
            volume = volumeBeanPropertyMapper.toObject(baseProps);
            volume.setSeriesNodeRef(baseVolume.getSeriesNodeRef());
            { // determine and set volumeMark based on baseVolume's volumeMark
                final String baseVolumeMark = baseVolume.getVolumeMark();
                final int yearIndex = baseVolumeMark.lastIndexOf("/");
                final String baseVolumeMarkStart;
                if (yearIndex >= 0) {
                    baseVolumeMarkStart = baseVolumeMark.substring(0, yearIndex + 1);
                } else {
                    baseVolumeMarkStart = baseVolumeMark + "/";
                }
                volumeMark = baseVolumeMarkStart + (1 + Calendar.getInstance().get(Calendar.YEAR));
            }
            // set validFrom and validTo
            final Date baseValidFrom = baseVolume.getValidFrom();
            final Date baseValidTo = baseVolume.getValidTo();
            Date newValidFrom = DateUtils.addYears(baseValidFrom, 1);
            newValidFrom = DateUtils.setMonths(newValidFrom, 0);
            newValidFrom = DateUtils.setDays(newValidFrom, 1);
            volume.setValidFrom(newValidFrom);

            if (baseValidTo != null) {
                volume.setValidTo(DateUtils.addYears(baseValidTo, 1));
            }

            volume.setContainingDocsCount(0);
        } else {
            volume = new Volume();
            final Series parentSeries = seriesService.getSeriesByNodeRef(seriesNodeRef);
            volumeMark = parentSeries.getSeriesIdentifier();
            volume.setTitle(parentSeries.getTitle());
            volume.setValidFrom(new Date());
        }

        volume.setVolumeMark(volumeMark);
        volume.setStatus(DocListUnitStatus.OPEN.getValueName());
        final Map<QName, Serializable> props = volumeBeanPropertyMapper.toProperties(volume);
        // create node
        final WmNode volumeNode;
        if (baseVolume != null) {
            volumeNode = createVolumeNode(volume.getSeriesNodeRef(), props); // persist
        } else {
            volumeNode = new WmNode(RepoUtil.createNewUnsavedNodeRef(),
                    VolumeModel.Types.VOLUME,
                    generalService.getDefaultAspects(VolumeModel.Types.VOLUME),
                    props);
        }
        // set properties that are not persisted
        volume.setNode(volumeNode);
        volume.setSeriesNodeRef(seriesNodeRef);
        return volume;
    }

    private WmNode createVolumeNode(NodeRef seriesNodeRef, Map<QName, Serializable> props) {
        props.put(DocumentCommonModel.Props.SERIES, seriesNodeRef);
        props.put(DocumentCommonModel.Props.FUNCTION, nodeService.getPrimaryParent(seriesNodeRef).getParentRef());
        NodeRef volumeNodeRef = nodeService.createNode(seriesNodeRef,
                VolumeModel.Associations.VOLUME, VolumeModel.Associations.VOLUME, VolumeModel.Types.VOLUME,
                props).getChildRef();
        return getVolumeNodeByRef(volumeNodeRef, null);
    }

    @Override
    public boolean isClosed(Node node) {
        return RepoUtil.isExistingPropertyValueEqualTo(node, VolumeModel.Props.STATUS, DocListUnitStatus.CLOSED.getValueName());
    }

    @Override
    public boolean isOpened(Node node) {
        return RepoUtil.isExistingPropertyValueEqualTo(node, VolumeModel.Props.STATUS, DocListUnitStatus.OPEN.getValueName());
    }

    @Override
    public void openVolume(Volume volume) {
        final Node volumeNode = volume.getNode();
        if (isOpened(volumeNode)) {
            return;
        }
        if (isInClosedSeries(volume)) {
            throw new UnableToPerformException("volume_open_error_inClosedSeries");
        }
        Map<String, Object> props = volumeNode.getProperties();
        props.put(VolumeModel.Props.STATUS.toString(), DocListUnitStatus.OPEN.getValueName());
        if (props.containsKey(VolumeModel.Props.MARKED_FOR_ARCHIVING.toString())) {
            props.put(VolumeModel.Props.MARKED_FOR_ARCHIVING.toString(), Boolean.FALSE);
        }
        saveOrUpdate(volume);

    }

    @Override
    public void delete(Volume volume) {
        List<NodeRef> documents = documentService.getAllDocumentRefsByParentRefWithoutRestrictedAccess(volume.getNode().getNodeRef());
        int casesCount = caseService.getCasesCountByVolume(volume.getNode().getNodeRef());
        if (!documents.isEmpty() || casesCount > 0) {
            throw new UnableToPerformException("volume_delete_not_empty");
        }
        nodeService.deleteNode(volume.getNode().getNodeRef());
    }

    @Override
    public Pair<String, Object[]> closeVolume(NodeRef volumeRef, Map<Long, QName> propertyTypes) {
        Pair<Boolean, Date> closeResult = getEventPlanService().closeVolumeOrCaseFile(volumeRef);
        if (!closeResult.getFirst()) {
            return null;
        }

        Volume volume = getVolumeByNodeRef(volumeRef, propertyTypes);
        Map<String, Object> props = volume.getNode().getProperties();
        props.put(VolumeModel.Props.STATUS.toString(), DocListUnitStatus.CLOSED.getValueName());
        if (closeResult.getSecond() != null) {
            props.put(VolumeModel.Props.VALID_TO.toString(), closeResult.getSecond());
        }
        if (volume.isSaved()) { // force closing all cases of given volume even if there are some cases that are still opened
            caseService.closeAllCasesByVolume(volume.getNodeRef());
        }
        try {
            saveOrUpdate(volume);
            return null;
        } catch (UnableToPerformException e) {
            throw e;
        }
    }

    @Override
    public WmNode getVolumeNodeByRef(NodeRef volumeNodeRef, Map<Long, QName> propertyTypes) {
        return generalService.fetchObjectNode(volumeNodeRef, VolumeModel.Types.VOLUME, propertyTypes);
    }

    /**
     * @param volumeNodeRef
     * @param seriesNodeRef if null, then volume.seriesNodeRef is set using association of given volumeNodeRef
     * @return Volume object with reference to corresponding seriesNodeRef
     */
    private Volume getVolumeByNoderef(NodeRef volumeNodeRef, NodeRef seriesNodeRef, Map<Long, QName> propertyTypes) {
        if (!nodeService.exists(volumeNodeRef)) {
            return null;
        }
        QName type = nodeService.getType(volumeNodeRef);
        boolean isDynamic = type.equals(CaseFileModel.Types.CASE_FILE);
        if (!type.equals(VolumeModel.Types.VOLUME) && !isDynamic) {
            throw new RuntimeException("Given noderef '" + volumeNodeRef + "' is not volume type:\n\texpected '" //
                    + VolumeModel.Types.VOLUME + " or " + CaseFileModel.Types.CASE_FILE + "'\n\tbut got '" + type + "'");
        }

        Volume volume = new Volume();
        WmNode node = getVolumeNodeByRef(volumeNodeRef, propertyTypes);
        volume.setNode(node);
        volumeBeanPropertyMapper.toObject(nodeService.getProperties(volumeNodeRef), volume);
        if (seriesNodeRef == null) {
            seriesNodeRef = (NodeRef) volume.getNode().getProperties().get(DocumentCommonModel.Props.SERIES);
        }
        if (seriesNodeRef == null) {
            List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(volumeNodeRef);
            if (parentAssocs.size() != 1) {
                throw new RuntimeException("Volume is expected to have only one parent series, but got " + parentAssocs.size() + " matching the criteria.");
            }
            seriesNodeRef = parentAssocs.get(0).getParentRef();
        }
        if (isDynamic) {
            volume.setVolumeType(null);
        }
        volume.setSeriesNodeRef(seriesNodeRef);
        if (log.isDebugEnabled()) {
            log.debug("Found volume: " + volume);
        }
        return volume;
    }

    @Override
    public NodeRef getArchivedVolumeByOriginalNodeRef(NodeRef archivedSeriesRef, NodeRef volumeNodeRef) {
        List<ChildAssociationRef> volumeChildAssocs = nodeService.getChildAssocs(archivedSeriesRef, Collections.singleton(VolumeModel.Types.VOLUME));
        for (ChildAssociationRef volumeChildAssoc : volumeChildAssocs) {
            NodeRef archivedVolumeRef = volumeChildAssoc.getChildRef();
            NodeRef originalVolumeRef = (NodeRef) nodeService.getProperty(archivedVolumeRef, VolumeModel.Props.ORIGINAL_VOLUME);
            if (originalVolumeRef != null && originalVolumeRef.equals(volumeNodeRef)) {
                return archivedVolumeRef;
            }
        }
        return null;
    }

    @Override
    public void saveDeletedDocument(NodeRef volumeNodeRef, DeletedDocument deletedDocument) {
        nodeService.createNode(volumeNodeRef, VolumeModel.Associations.DELETED_DOCUMENT, VolumeModel.Associations.DELETED_DOCUMENT
                , VolumeModel.Types.DELETED_DOCUMENT, deletedDocumentBeanPropertyMapper.toProperties(deletedDocument));
    }

    @Override
    public List<DeletedDocument> getDeletedDocuments(NodeRef volumeNodeRef) {
        List<ChildAssociationRef> documentAssocs = nodeService.getChildAssocs(volumeNodeRef, RegexQNamePattern.MATCH_ALL, VolumeModel.Associations.DELETED_DOCUMENT);
        List<DeletedDocument> deletedDocuments = new ArrayList<DeletedDocument>(documentAssocs.size());
        for (ChildAssociationRef docAssoc : documentAssocs) {
            NodeRef deletedDocNodeRef = docAssoc.getChildRef();
            deletedDocuments.add(getDeletedDocument(deletedDocNodeRef));
        }
        Collections.sort(deletedDocuments);
        return deletedDocuments;
    }

    @Override
    public DeletedDocument getDeletedDocument(NodeRef deletedDocumentNodeRef) {
        if (!VolumeModel.Types.DELETED_DOCUMENT.equals(nodeService.getType(deletedDocumentNodeRef))) {
            throw new RuntimeException("Given noderef '" + deletedDocumentNodeRef + "' is not deletedDocument type:\n\texpected '" //
                    + VolumeModel.Types.DELETED_DOCUMENT + "'\n\tbut got '" + nodeService.getType(deletedDocumentNodeRef) + "'");
        }
        return deletedDocumentBeanPropertyMapper.toObject(nodeService.getProperties(deletedDocumentNodeRef), new DeletedDocument());
    }

    // START: getters / setters
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setEventPlanService(EventPlanService eventPlanService) {
        this.eventPlanService = eventPlanService;
    }

    public DocumentAdminService getDocumentAdminService() {
        if (_documentAdminService == null) {
            _documentAdminService = BeanHelper.getDocumentAdminService();
        }
        return _documentAdminService;
    }

    public void setVolumeCache(SimpleCache<NodeRef, UnmodifiableVolume> volumeCache) {
        this.volumeCache = volumeCache;
    }

    public void setBulkLoadNodeService(BulkLoadNodeService bulkLoadNodeService) {
        this.bulkLoadNodeService = bulkLoadNodeService;
    }

    // END: getters / setters

}
