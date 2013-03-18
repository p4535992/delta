package ee.webmedia.alfresco.volume.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
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
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.service.DocumentService;
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
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;

/**
 * @author Ats Uiboupin
 */
public class VolumeServiceImpl implements VolumeService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(VolumeServiceImpl.class);
    private static final BeanPropertyMapper<Volume> volumeBeanPropertyMapper = BeanPropertyMapper.newInstance(Volume.class);
    private static final BeanPropertyMapper<DeletedDocument> deletedDocumentBeanPropertyMapper = BeanPropertyMapper.newInstance(DeletedDocument.class);

    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private GeneralService generalService;
    private SeriesService seriesService;
    private CaseService caseService;
    private DocumentService documentService;
    private UserService userService;
    private LogService logService;
    private boolean caseVolumeEnabled;

    @Override
    public List<ChildAssociationRef> getAllVolumeRefsBySeries(NodeRef seriesNodeRef) {
        return nodeService.getChildAssocs(seriesNodeRef, RegexQNamePattern.MATCH_ALL, VolumeModel.Associations.VOLUME);
    }

    @Override
    public List<Volume> getAllVolumesBySeries(NodeRef seriesNodeRef) {
        return getAllVolumesBySeries(seriesNodeRef, null);
    }

    @Override
    public List<Volume> getAllVolumesBySeries(NodeRef seriesNodeRef, DocListUnitStatus status) {
        List<ChildAssociationRef> volumeAssocs = getAllVolumeRefsBySeries(seriesNodeRef);
        List<Volume> volumeOfSeries = new ArrayList<Volume>(volumeAssocs.size());
        for (ChildAssociationRef volumeCaRef : volumeAssocs) {
            NodeRef volumeNodeRef = volumeCaRef.getChildRef();
            Volume volume = getVolumeByNoderef(volumeNodeRef, seriesNodeRef);
            if (status == null || status.getValueName().equals(volume.getStatus())) {
                volumeOfSeries.add(volume);
            }
        }
        Collections.sort(volumeOfSeries);
        return volumeOfSeries;
    }

    @Override
    public List<Volume> getAllValidVolumesBySeries(NodeRef seriesNodeRef, DocListUnitStatus status) {
        List<Volume> volumes = getAllValidVolumesBySeries(seriesNodeRef);
        for (Iterator<Volume> i = volumes.iterator(); i.hasNext();) {
            Volume volume = i.next();
            if (!status.getValueName().equals(volume.getStatus())) {
                i.remove();
            }
        }
        return volumes;
    }

    @Override
    public List<Volume> getAllValidVolumesBySeries(NodeRef seriesNodeRef) {
        List<Volume> volumes = getAllVolumesBySeries(seriesNodeRef);
        final Calendar cal = Calendar.getInstance();
        for (Iterator<Volume> i = volumes.iterator(); i.hasNext();) {
            Volume volume = i.next();

            Date validFrom = volume.getValidFrom();
            if (validFrom != null && cal.getTime().before(validFrom)) {
                log.debug("Skipping volume '" + volume.getTitle() + "', current date "
                        + cal.getTime() + " is earlier than volume valid from date " + validFrom);
                i.remove();
                continue;
            }

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
                    continue;
                }
            }
        }
        return volumes;
    }

    @Override
    public List<Volume> getAllOpenExpiredVolumesBySeries(NodeRef seriesNodeRef) {
        List<Volume> volumes = getAllVolumesBySeries(seriesNodeRef);
        final Calendar cal = Calendar.getInstance();
        for (Iterator<Volume> i = volumes.iterator(); i.hasNext();) {
            Volume volume = i.next();

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
    public Volume getVolumeByNodeRef(String volumeNodeRef) {
        return getVolumeByNodeRef(new NodeRef(volumeNodeRef));
    }

    @Override
    public Volume getVolumeByNodeRef(NodeRef volumeRef) {
        return getVolumeByNoderef(volumeRef, null);
    }

    @Override
    public void saveOrUpdate(Volume volume) {
        saveOrUpdate(volume, true);
    }

    @Override
    public void saveOrUpdate(Volume volume, boolean fromNodeProps) {
        Node volumeNode = volume.getNode();
        if (volumeNode instanceof TransientNode) { // save
            Map<QName, Serializable> qNameProperties = fromNodeProps ? RepoUtil.toQNameProperties(volumeNode.getProperties())
                    : volumeBeanPropertyMapper.toProperties(volume);
            volume.setNode(createVolumeNode(volume.getSeriesNodeRef(), qNameProperties));

            Map<String, Object> props = volume.getNode().getProperties();
            logService.addLogEntry(LogEntry.create(LogObject.VOLUME, userService, volume.getNode().getNodeRef(), "applog_space_add",
                    props.get(VolumeModel.Props.VOLUME_MARK.toString()), props.get(VolumeModel.Props.TITLE.toString())));
        } else { // update
            if (!checkContainsCasesValue(volume)) {
                throw new UnableToPerformException("volume_contains_docs_or_cases");
            }

            String propDiff = new PropDiffHelper()
                    .label(VolumeModel.Props.STATUS, "volume_status")
                    .label(VolumeModel.Props.VOLUME_TYPE, "volume_volumeType")
                    .label(VolumeModel.Props.VOLUME_MARK, "volume_volumeMark")
                    .label(VolumeModel.Props.TITLE, "volume_title")
                    .label(VolumeModel.Props.DESCRIPTION, "volume_description")
                    .label(VolumeModel.Props.VALID_FROM, "volume_validFrom")
                    .label(VolumeModel.Props.VALID_TO, "volume_validTo")
                    .label(VolumeModel.Props.ARCHIVING_NOTE, "volume_archive_note")
                    .label(VolumeModel.Props.SEND_TO_DESTRUCTION, "volume_sendToDestruction")
                    .label(VolumeModel.Props.CASES_CREATABLE_BY_USER, "volume_casesCreatableByUser")
                    .diff(nodeService.getProperties(volumeNode.getNodeRef()), RepoUtil.toQNameProperties(volumeNode.getProperties()));

            if (propDiff != null) {
                logService.addLogEntry(LogEntry.create(LogObject.VOLUME, userService, volumeNode.getNodeRef(), "applog_space_edit",
                        volume.getVolumeMark(), volume.getTitle(), propDiff));
            }

            if (fromNodeProps) {
                generalService.setPropertiesIgnoringSystem(volumeNode.getNodeRef(), volumeNode.getProperties());
            } else {
                generalService.setPropertiesIgnoringSystem(volumeNode.getNodeRef(), RepoUtil.toStringProperties(volumeBeanPropertyMapper
                        .toProperties(volume)));
            }

        }
    }

    private boolean isInClosedSeries(Volume volume) {
        Serializable seriesStatus = nodeService.getProperty(volume.getSeriesNodeRef(), SeriesModel.Props.STATUS);
        return DocListUnitStatus.CLOSED.getValueName().equals(seriesStatus);
    }

    private boolean checkContainsCasesValue(Volume volume) {
        Volume repoVolume = getVolumeByNodeRef(volume.getNode().getNodeRef());
        Boolean containsCasesValue = (Boolean) volume.getNode().getProperties().get(VolumeModel.Props.CONTAINS_CASES);
        Boolean origContainsCasesValue = (Boolean) repoVolume.getNode().getProperties().get(VolumeModel.Props.CONTAINS_CASES);
        if (!containsCasesValue.equals(origContainsCasesValue)) {
            return caseService.getCasesCountByVolume(volume.getNode().getNodeRef()) == 0
                    && documentService.getDocumentsCountByVolumeOrCase(volume.getNode().getNodeRef()) == 0;
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
            final Date baseDispositionDate = baseVolume.getDispositionDate();
            Date newValidFrom = DateUtils.addYears(baseValidFrom, 1);
            newValidFrom = DateUtils.setMonths(newValidFrom, 0);
            newValidFrom = DateUtils.setDays(newValidFrom, 1);
            volume.setValidFrom(newValidFrom);

            if (baseValidTo != null) {
                volume.setValidTo(DateUtils.addYears(baseValidTo, 1));
            }
            if (baseDispositionDate != null) {
                volume.setDispositionDate(DateUtils.addYears(baseDispositionDate, 1));
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
        final Node volumeNode;
        if (baseVolume != null) {
            volumeNode = createVolumeNode(volume.getSeriesNodeRef(), props); // persist
        } else {
            volumeNode = TransientNode.createNew(dictionaryService, dictionaryService.getType(VolumeModel.Types.VOLUME), null, props);
        }
        // set properties that are not persisted
        volume.setNode(volumeNode);
        volume.setSeriesNodeRef(seriesNodeRef);
        return volume;
    }

    private Node createVolumeNode(NodeRef seriesNodeRef, Map<QName, Serializable> props) {
        NodeRef volumeNodeRef = nodeService.createNode(seriesNodeRef,
                VolumeModel.Associations.VOLUME, VolumeModel.Associations.VOLUME, VolumeModel.Types.VOLUME,
                props).getChildRef();
        return generalService.fetchNode(volumeNodeRef);
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
        saveOrUpdate(volume);

    }

    @Override
    public void closeVolume(Volume volume) {
        final Node volumeNode = volume.getNode();
        if (isClosed(volumeNode)) {
            return;
        }
        Map<String, Object> props = volumeNode.getProperties();

        props.put(VolumeModel.Props.STATUS.toString(), DocListUnitStatus.CLOSED.getValueName());
        if (props.get(VolumeModel.Props.VALID_TO) == null) {
            props.put(VolumeModel.Props.VALID_TO.toString(), new Date());
        }

        Series series = seriesService.getSeriesByNodeRef(volume.getSeriesNodeRef().toString());
        final Integer retentionPeriod = series.getRetentionPeriod();
        if (retentionPeriod != null) {
            final Calendar cal1 = Calendar.getInstance();
            cal1.set(cal1.get(Calendar.YEAR) + 1 + retentionPeriod, 0, 1);// 1. January next year + retentionPeriod(in years)
            props.put(VolumeModel.Props.DISPOSITION_DATE.toString(), DateUtils.truncate(cal1, Calendar.DAY_OF_MONTH).getTime());
        }
        if (!(volumeNode instanceof TransientNode)) { // force closing all cases of given volume even if there are some cases that are still opened
            caseService.closeAllCasesByVolume(volumeNode.getNodeRef());
        }
        try {
            saveOrUpdate(volume);
        } catch (UnableToPerformException e) {
            throw e;
        }
    }

    @Override
    public Node getVolumeNodeByRef(NodeRef volumeNodeRef) {
        return generalService.fetchNode(volumeNodeRef);
    }

    @Override
    public boolean isCaseVolumeEnabled() {
        return caseVolumeEnabled;
    }

    /**
     * @param volumeNodeRef
     * @param seriesNodeRef if null, then volume.seriesNodeRef is set using association of given volumeNodeRef
     * @return Volume object with reference to corresponding seriesNodeRef
     */
    private Volume getVolumeByNoderef(NodeRef volumeNodeRef, NodeRef seriesNodeRef) {
        if (!nodeService.getType(volumeNodeRef).equals(VolumeModel.Types.VOLUME)) {
            throw new RuntimeException("Given noderef '" + volumeNodeRef + "' is not volume type:\n\texpected '" //
                    + VolumeModel.Types.VOLUME + "'\n\tbut got '" + nodeService.getType(volumeNodeRef) + "'");
        }
        Volume volume = new Volume();
        volume.setNode(getVolumeNodeByRef(volumeNodeRef));
        volumeBeanPropertyMapper.toObject(nodeService.getProperties(volumeNodeRef), volume);
        if (seriesNodeRef == null) {
            List<ChildAssociationRef> parentAssocs = nodeService.getParentAssocs(volumeNodeRef);
            if (parentAssocs.size() != 1) {
                throw new RuntimeException("Volume is expected to have only one parent series, but got " + parentAssocs.size() + " matching the criteria.");
            }
            seriesNodeRef = parentAssocs.get(0).getParentRef();
        }
        volume.setVolumeType((String) nodeService.getProperty(volumeNodeRef, VolumeModel.Props.VOLUME_TYPE));
        volume.setTitle((String) nodeService.getProperty(volumeNodeRef, VolumeModel.Props.TITLE));
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
    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

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

    public void setCaseVolumeEnabled(boolean caseVolumeEnabled) {
        this.caseVolumeEnabled = caseVolumeEnabled;
    }
    // END: getters / setters

}
