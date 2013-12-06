package ee.webmedia.alfresco.archivals.service;

import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.adr.service.AdrService;
import ee.webmedia.alfresco.archivals.model.ArchivalsModel;
import ee.webmedia.alfresco.archivals.model.ArchiveJobStatus;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.ProgressTracker;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * @author Romet Aidla
 */
public class ArchivalsServiceImpl implements ArchivalsService {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ArchivalsServiceImpl.class);

    private NodeService nodeService;
    private GeneralService generalService;
    private CopyService copyService;
    private VolumeService volumeService;
    private SeriesService seriesService;
    private FunctionsService functionsService;
    private SearchService searchService;
    private DictionaryService dictionaryService;
    private AdrService adrService;
    private DocumentService documentService;
    private CaseService caseService;

    private StoreRef archivalsStore;
    private boolean archivingPaused;

    @Override
    public void archiveVolume(final NodeRef archivingJobRef) {
        final RetryingTransactionHelper transactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
        try {
            archiveVolumeImpl(archivingJobRef);
            transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                    props.put(ArchivalsModel.Props.ARCHIVING_JOB_STATUS, ArchiveJobStatus.FINISHED);
                    props.put(ArchivalsModel.Props.ARCHIVING_END_TIME, new Date());
                    nodeService.addProperties(archivingJobRef, props);
                    return null;
                }
            }, false, true);
        } catch (final Exception e) {
            transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
                @Override
                public Void execute() throws Throwable {
                    Map<QName, Serializable> props = new HashMap<QName, Serializable>();
                    props.put(ArchivalsModel.Props.ARCHIVING_JOB_STATUS, ArchiveJobStatus.FAILED);
                    props.put(ArchivalsModel.Props.ARCHIVING_END_TIME, new Date());
                    props.put(ArchivalsModel.Props.ERROR_MESSAGE, e.getMessage() + "\n" + e);
                    nodeService.addProperties(archivingJobRef, props);
                    return null;
                }
            }, false, true);
        }
    }

    @Override
    public void addVolumeToArchivingList(NodeRef volumeRef) {
        NodeRef archivingJobRef = nodeService.createNode(getArchivalsSpaceRef(), ArchivalsModel.Assocs.ARCHIVING_JOB,
                ArchivalsModel.Assocs.ARCHIVING_JOB, ArchivalsModel.Types.ARCHIVING_JOB).getChildRef();
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        props.put(ArchivalsModel.Props.VOLUME_REF, volumeRef);
        props.put(ArchivalsModel.Props.ARCHIVE_NOTE, String.format(MessageUtil.getMessage("volume_archiving_note"), df.format(new Date())));
        props.put(ArchivalsModel.Props.ARCHIVING_JOB_STATUS, ArchiveJobStatus.IN_QUEUE);
        nodeService.addProperties(archivingJobRef, props);

    }

    @Override
    public List<NodeRef> getAllInQueueJobs() {
        List<NodeRef> volumeRefs = new ArrayList<NodeRef>();
        for (ChildAssociationRef ref : getArchivingJobChildAssocs()) {
            volumeRefs.add(ref.getChildRef());
        }
        return volumeRefs;
    }

    @Override
    public ArchiveJobStatus getArchivingStatus(NodeRef archivingJobNodeRef) {
        return archivingJobNodeRef == null ? null : ArchiveJobStatus.valueOf((String) nodeService.getProperty(archivingJobNodeRef, ArchivalsModel.Props.ARCHIVING_JOB_STATUS));
    }

    private NodeRef getArchivalsSpaceRef() {
        return generalService.getNodeRef(ArchivalsModel.Repo.ARCHIVALS_SPACE);
    }

    @Override
    public void markArchivingJobAsRunning(NodeRef archivingJobNodeRef) {
        nodeService.setProperty(archivingJobNodeRef, ArchivalsModel.Props.ARCHIVING_JOB_STATUS, ArchiveJobStatus.IN_PROGRESS);
    }

    private List<ChildAssociationRef> getArchivingJobChildAssocs() {
        return nodeService.getChildAssocs(getArchivalsSpaceRef(), Collections.singleton(ArchivalsModel.Types.ARCHIVING_JOB));
    }

    private NodeRef archiveVolumeImpl(final NodeRef archivingJobRef) {
        final NodeRef volumeNodeRef = (NodeRef) nodeService.getProperty(archivingJobRef, ArchivalsModel.Props.VOLUME_REF);
        Assert.notNull(volumeNodeRef, "Reference to volume node must be provided");
        final String archivingNote = (String) nodeService.getProperty(archivingJobRef, ArchivalsModel.Props.ARCHIVE_NOTE);
        final RetryingTransactionHelper transactionHelper = BeanHelper.getTransactionService().getRetryingTransactionHelper();

        Volume volume = volumeService.getVolumeByNodeRef(volumeNodeRef);
        final Series series = seriesService.getSeriesByNodeRef(volume.getSeriesNodeRef());
        final NodeRef originalSeriesRef = series.getNode().getNodeRef();
        final Map<NodeRef, NodeRef> originalToArchivedCaseNodeRef = new HashMap<NodeRef, NodeRef>();

        // do in separate transaction, must be visible in following transactions
        NodeRef[] archivedParentRefs = transactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef[]>() {
            @Override
            public NodeRef[] execute() throws Throwable {
                return createAndRetrieveArchiveStructure(volumeNodeRef, originalSeriesRef, series.getFunctionNodeRef(), originalToArchivedCaseNodeRef);
            }
        }, false, true);

        final NodeRef archivedFunctionRef = archivedParentRefs[0];
        final NodeRef archivedSeriesRef = archivedParentRefs[1];
        final NodeRef archivedVolumeRef = archivedParentRefs[2];
        final NodeRef copiedFunctionRef = archivedParentRefs[3];
        NodeRef copiedVolumeRef = archivedParentRefs[4];

        Assert.notNull(archivedSeriesRef, "Series was not archived");
        Assert.notNull(archivedVolumeRef, "Volume was not archived");

        Set<ChildAssociationRef> notCaseNodeRefs = new HashSet<ChildAssociationRef>();
        final Set<NodeRef> caseNodeRefs = new HashSet<NodeRef>();
        Map<NodeRef, Set<ChildAssociationRef>> archiveNodeRefs = new HashMap<NodeRef, Set<ChildAssociationRef>>();
        // TODO: seems that no separate transaction is needed, read-only operations are performed here?
        collectNodeRefsToArchive(volumeNodeRef, notCaseNodeRefs, caseNodeRefs, archiveNodeRefs);

        int failedNodeCount = 0;
        int failedDocumentsCount = 0;
        int totalArchivedDocumentsCount = 0;
        int archivedNodesCount = 0;

        final Map<NodeRef, Integer> caseDocsUpdated = new HashMap<NodeRef, Integer>();
        int childCount = 0;
        for (Set<ChildAssociationRef> childNodes : archiveNodeRefs.values()) {
            childCount += childNodes.size();
        }
        ProgressTracker progress = new ProgressTracker(childCount, 0);
        int count = 0;
        for (Map.Entry<NodeRef, Set<ChildAssociationRef>> entry : archiveNodeRefs.entrySet()) {

            NodeRef originalParentRef = entry.getKey();
            NodeRef archivedParentRef = null;
            final boolean isInCase = !originalParentRef.equals(volumeNodeRef);
            if (!isInCase) {
                archivedParentRef = archivedVolumeRef;
            } else {
                archivedParentRef = getOrCreateArchiveCase(originalParentRef, archivedVolumeRef, copiedVolumeRef, originalToArchivedCaseNodeRef, transactionHelper);
            }
            final NodeRef archivedParentRefFinal = archivedParentRef;
            for (final ChildAssociationRef childAssocRef : entry.getValue()) {
                doPauseArchiving();
                final NodeRef childRef = childAssocRef.getChildRef();
                if (!nodeService.exists(childRef) || !originalParentRef.equals(nodeService.getPrimaryParent(childRef).getParentRef())) {
                    // node has been deleted or moved, skip it
                    LOG.info("Node not found in volume any more, skipping nodeRef=" + childRef);
                    continue;
                }
                boolean isDocument = DocumentCommonModel.Types.DOCUMENT.equals(nodeService.getType(childRef));
                try {
                    transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                        @Override
                        public Void execute() throws Throwable {
                            NodeRef archivedNodeRef = nodeService.moveNode(childRef, archivedParentRefFinal, childAssocRef.getTypeQName(),
                                    childAssocRef.getQName()).getChildRef();
                            updateDocumentLocationProps(archivedFunctionRef, archivedSeriesRef, archivedVolumeRef, isInCase ? archivedParentRefFinal : null,
                                    archivedNodeRef);
                            return null;
                        }
                    }, false, true);
                    if (isDocument) {
                        if (isInCase) {
                            Integer caseArchivedDocsCount = caseDocsUpdated.get(originalParentRef);
                            if (caseArchivedDocsCount == null) {
                                caseArchivedDocsCount = 0;
                            }
                            caseDocsUpdated.put(originalParentRef, ++caseArchivedDocsCount);
                        }
                        totalArchivedDocumentsCount++;
                    }
                    archivedNodesCount++;
                } catch (Exception e) {
                    failedNodeCount++;
                    failedDocumentsCount += isDocument ? 1 : 0;
                    LOG.error("Error archiving node in volume, volume original nodeRef=" + volumeNodeRef + ", archived volume nodeRef=" + archivedVolumeRef
                            + (isInCase ? ", original case nodeRef=" + originalParentRef + ", archived case nodeRef=" + archivedParentRef : "")
                            + ", child nodeRef=" + childAssocRef.getChildRef(), e);
                    // continue, try to archive as much documents as possible
                }
                if (++count >= 10) {
                    String info = progress.step(count);
                    count = 0;
                    if (info != null) {
                        LOG.info("Archiving volume: " + info);
                    }
                }
            }
        }
        String info = progress.step(count);
        if (info != null) {
            LOG.info("Archiving volume: " + info);
        }
        // update archivingNote property
        transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
            @Override
            public Void execute() {
                nodeService.setProperty(archivedVolumeRef, VolumeModel.Props.ARCHIVING_NOTE, archivingNote);
                nodeService.removeProperty(archivedVolumeRef, VolumeModel.Props.MARKED_FOR_ARCHIVING);
                return null;
            }
        }, false, true);

        updateCounters(volumeNodeRef, archivedVolumeRef, originalSeriesRef, archivedSeriesRef, originalToArchivedCaseNodeRef, totalArchivedDocumentsCount, caseDocsUpdated,
                transactionHelper);

        LOG.info("Archived " + archivedNodesCount + " nodes from volume and contained cases,\n " +
                "   nodeRefs=" + archiveNodeRefs.keySet() + "\n" +
                "   of that " + totalArchivedDocumentsCount + " documents");

        if (failedNodeCount == 0) {
            deleteEmptyVolumeAndCases(volumeNodeRef, caseNodeRefs, transactionHelper);
        } else {
            LOG.info("Not deleting original volume, nodeRef=" + volumeNodeRef + ", because " + failedNodeCount + " errors occurred while archiving the volume, of that "
                    + failedDocumentsCount + " document archivation failures");
        }

        transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable {
                nodeService.deleteNode(copiedFunctionRef);
                return null;
            }
        }, false, true);

        return archivedVolumeRef;

    }

    private void deleteEmptyVolumeAndCases(final NodeRef volumeNodeRef, final Set<NodeRef> caseNodeRefs, final RetryingTransactionHelper transactionHelper) {
        transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable {
                List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(volumeNodeRef);
                boolean deleteVolume = true;
                for (ChildAssociationRef childRef : childRefs) {
                    NodeRef childNodeRef = childRef.getChildRef();
                    if (caseNodeRefs.contains(childNodeRef)) {
                        if (nodeService.getChildAssocs(childNodeRef).size() == 0) {
                            // TODO: Riina - could be optimized, assuming that it is very likely that whole volume is going to be deleted,
                            // but as this runs in background and case has no children, deleting shouldn't take much time either
                            nodeService.deleteNode(childNodeRef);
                            LOG.info("Deleted empty case, nodeRef=" + childNodeRef);
                            continue;
                        }
                        deleteVolume = false;
                        LOG.info("case nodeRef=" + childNodeRef + " has not archived children, not deleting case.");
                        break;
                    }
                    deleteVolume = false;
                    LOG.info("Volume nodeRef=" + volumeNodeRef + " has not archived children created after archivation start, not deleting volume.");
                    break;
                }
                if (deleteVolume) {
                    nodeService.deleteNode(volumeNodeRef);
                    LOG.info("Deleted original volume nodeRef=" + volumeNodeRef);
                }
                return null;
            }
        }, false, true);
    }

    private NodeRef getOrCreateArchiveCase(final NodeRef originalCaseRef, final NodeRef archivedVolumeRef, final NodeRef copiedVolumeRef,
            final Map<NodeRef, NodeRef> originalToArchivedCaseNodeRef, final RetryingTransactionHelper transactionHelper) {
        NodeRef archivedCaseRef = originalToArchivedCaseNodeRef.get(originalCaseRef);
        if (archivedCaseRef == null) {
            final NodeRef originalCaseRefFinal = originalCaseRef;
            // create archive case
            archivedCaseRef = transactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>() {
                @Override
                public NodeRef execute() {
                    NodeRef copiedCaseRef = copyService.copy(originalCaseRef, copiedVolumeRef, CaseModel.Associations.CASE, CaseModel.Associations.CASE);
                    NodeRef archCaseRef = nodeService.moveNode(copiedCaseRef, archivedVolumeRef, CaseModel.Associations.CASE, CaseModel.Associations.CASE).getChildRef();
                    Map<QName, Serializable> caseProps = new HashMap<QName, Serializable>();
                    caseProps.put(CaseModel.Props.CONTAINING_DOCS_COUNT, 0);
                    caseProps.put(CaseModel.Props.ORIGINAL_CASE, originalCaseRefFinal);
                    nodeService.addProperties(archCaseRef, caseProps);
                    return archCaseRef;
                }
            }, false, true);
            originalToArchivedCaseNodeRef.put(originalCaseRef, archivedCaseRef);
        }
        return archivedCaseRef;
    }

    private void updateCounters(final NodeRef originalVolumeRef, final NodeRef archivedVolumeRef, final NodeRef originalSeriesRef,
            final NodeRef archivedSeriesRef, final Map<NodeRef, NodeRef> originalToArchivedCaseNodeRef, final int archivedDocumentsCount,
            final Map<NodeRef, Integer> caseDocsUpdated, final RetryingTransactionHelper transactionHelper) {
        updateCounter(archivedVolumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT, true, archivedDocumentsCount, transactionHelper, false);
        updateCounter(originalVolumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT, false, archivedDocumentsCount, transactionHelper, true);
        updateCounter(archivedSeriesRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, true, archivedDocumentsCount, transactionHelper, false);
        updateCounter(originalSeriesRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, false, archivedDocumentsCount, transactionHelper, true);
        for (Map.Entry<NodeRef, Integer> entry : caseDocsUpdated.entrySet()) {
            NodeRef originalCaseRef = entry.getKey();
            NodeRef archivedCaseRef = originalToArchivedCaseNodeRef.get(originalCaseRef);
            int archivedDocCount = entry.getValue();
            updateCounter(archivedCaseRef, CaseModel.Props.CONTAINING_DOCS_COUNT, true, archivedDocCount, transactionHelper, false);
            updateCounter(originalCaseRef, CaseModel.Props.CONTAINING_DOCS_COUNT, false, archivedDocCount, transactionHelper, true);
        }

    }

    private void updateCounter(final NodeRef nodeRefToUpdate, final QName docCountProp, final boolean added, final int docCount, final RetryingTransactionHelper transactionHelper,
            final boolean checkExisting) {
        try {
            transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

                @Override
                public Void execute() throws Throwable {
                    // node may be deleted while archiving, this is okey and shouldn't produce error
                    if (!checkExisting || nodeService.exists(nodeRefToUpdate)) {
                        generalService.updateParentContainingDocsCount(nodeRefToUpdate, docCountProp, added, docCount);
                    }
                    return null;
                }
            }, false, true);
        } catch (Exception e) {
            LOG.error("Error updating counters in archive volume process, nodeRef=" + nodeRefToUpdate, e);
            // continue updating other counters
        }
    }

    private void collectNodeRefsToArchive(final NodeRef volumeNodeRef, Set<ChildAssociationRef> notCaseNodeRefs, Set<NodeRef> caseNodeRefs,
            Map<NodeRef, Set<ChildAssociationRef>> archiveNodeRefs) {
        List<ChildAssociationRef> volumeChildAssocs = nodeService.getChildAssocs(volumeNodeRef);
        for (ChildAssociationRef childAssocRef : volumeChildAssocs) {
            NodeRef childNodeRef = childAssocRef.getChildRef();
            QName nodeType = nodeService.getType(childNodeRef);
            if (CaseModel.Types.CASE.equals(nodeType)) {
                caseNodeRefs.add(childNodeRef);
            } else {
                notCaseNodeRefs.add(childAssocRef);
            }
        }
        archiveNodeRefs.put(volumeNodeRef, notCaseNodeRefs);
        for (NodeRef caseNodeRef : caseNodeRefs) {
            archiveNodeRefs.put(caseNodeRef, new HashSet<ChildAssociationRef>(nodeService.getChildAssocs(caseNodeRef)));
        }
    }

    private NodeRef[] createAndRetrieveArchiveStructure(final NodeRef volumeNodeRef, final NodeRef originalSeriesRef, final NodeRef originalFunctionRef,
            final Map<NodeRef, NodeRef> originalToArchivedCaseNodeRef) {

        NodeRef copiedFunctionRef = copyService.copy(originalFunctionRef, getTempArchivalRoot(), FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION);
        NodeRef copiedSeriesRef = copyService.copy(originalSeriesRef, copiedFunctionRef, SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES);
        NodeRef copiedVolumeRef = copyService.copy(volumeNodeRef, copiedSeriesRef, VolumeModel.Associations.VOLUME, VolumeModel.Associations.VOLUME);
        nodeService.setProperty(copiedSeriesRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, 0);
        nodeService.setProperty(copiedVolumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT, 0);
        boolean movedHierarchy = true;

        NodeRef archivedFunctionRef = getArchivedFunctionByMark((String) nodeService.getProperty(originalFunctionRef, FunctionsModel.Props.MARK));
        NodeRef archivedSeriesRef = null;
        NodeRef archivedVolumeRef = null;
        if (archivedFunctionRef == null) {
            archivedFunctionRef = nodeService.moveNode(copiedFunctionRef, getArchivalRoot(), FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION)
                    .getChildRef();
            archivedSeriesRef = nodeService.getChildAssocs(archivedFunctionRef, Collections.singleton(SeriesModel.Types.SERIES)).get(0).getChildRef();
            archivedVolumeRef = nodeService.getChildAssocs(archivedSeriesRef, Collections.singleton(VolumeModel.Types.VOLUME)).get(0).getChildRef();
        } else {
            String seriesIdentifier = (String) nodeService.getProperty(originalSeriesRef, SeriesModel.Props.SERIES_IDENTIFIER);
            archivedSeriesRef = getArchivedSeriesByIdentifies(archivedFunctionRef, seriesIdentifier);
            if (archivedSeriesRef == null) {
                archivedSeriesRef = nodeService.moveNode(copiedSeriesRef, archivedFunctionRef, SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES).getChildRef();
                archivedVolumeRef = nodeService.getChildAssocs(archivedSeriesRef, Collections.singleton(VolumeModel.Types.VOLUME)).get(0).getChildRef();
            } else {
                archivedVolumeRef = volumeService.getArchivedVolumeByOriginalNodeRef(archivedSeriesRef, volumeNodeRef);
                if (archivedVolumeRef == null) {
                    archivedVolumeRef = nodeService.moveNode(copiedVolumeRef, archivedSeriesRef, VolumeModel.Associations.VOLUME, VolumeModel.Associations.VOLUME).getChildRef();
                } else {
                    movedHierarchy = false;
                    Map<QName, Serializable> originalVolumeProps = nodeService.getProperties(volumeNodeRef);
                    nodeService.addProperties(archivedVolumeRef, RepoUtil.getPropertiesIgnoringSystem(originalVolumeProps, dictionaryService));
                    List<ChildAssociationRef> caseChildAssocs = nodeService.getChildAssocs(archivedVolumeRef, Collections.singleton(CaseModel.Types.CASE));
                    for (ChildAssociationRef caseChildAssoc : caseChildAssocs) {
                        NodeRef archivedCaseRef = caseChildAssoc.getChildRef();
                        NodeRef originalCaseRef = (NodeRef) nodeService.getProperty(archivedCaseRef, CaseModel.Props.ORIGINAL_CASE);
                        if (originalCaseRef != null) {
                            originalToArchivedCaseNodeRef.put(originalCaseRef, archivedCaseRef);
                        }
                    }
                }
            }
        }
        if (movedHierarchy) {
            // if copied hierarchy was (partly) moved, delete remaining hierarchy and recreate new
            // so it can be used to copy cases during archivation process
            if (nodeService.exists(copiedFunctionRef)) {
                nodeService.deleteNode(copiedFunctionRef);
            }
            copiedFunctionRef = copyService.copy(originalFunctionRef, getTempArchivalRoot(), FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION);
            copiedSeriesRef = copyService.copy(originalSeriesRef, copiedFunctionRef, SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES);
            copiedVolumeRef = copyService.copy(volumeNodeRef, copiedSeriesRef, VolumeModel.Associations.VOLUME, VolumeModel.Associations.VOLUME);
        }
        nodeService.setProperty(archivedVolumeRef, VolumeModel.Props.ORIGINAL_VOLUME, volumeNodeRef);
        return new NodeRef[] { archivedFunctionRef, archivedSeriesRef, archivedVolumeRef, copiedFunctionRef, copiedVolumeRef };
    }

    private void updateDocumentLocationProps(NodeRef archivedFunRef, NodeRef archivedSeriesRef, NodeRef archivedVolumeRef, NodeRef archivedCaseRef, NodeRef docRef) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(DocumentCommonModel.Props.FUNCTION, archivedFunRef);
        props.put(DocumentCommonModel.Props.SERIES, archivedSeriesRef);
        props.put(DocumentCommonModel.Props.VOLUME, archivedVolumeRef);
        props.put(DocumentCommonModel.Props.CASE, archivedCaseRef);
        nodeService.addProperties(docRef, props);
    }

    @Override
    public int destroyArchivedVolumes() {
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        List<NodeRef> volumesForDestruction = searchVolumesForDestruction();
        for (NodeRef volumeNodeRef : volumesForDestruction) {
            HashMap<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(VolumeModel.Props.STATUS, DocListUnitStatus.DESTROYED.getValueName());
            String archivingnote = (String) nodeService.getProperty(volumeNodeRef, VolumeModel.Props.ARCHIVING_NOTE);
            props.put(VolumeModel.Props.ARCHIVING_NOTE, archivingnote + " Hävitatud: " + dateFormat.format(new Date()));

            // remove all childs
            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(volumeNodeRef);
            for (ChildAssociationRef childAssoc : childAssocs) {
                NodeRef nodeRef = childAssoc.getChildRef();
                if (dictionaryService.isSubClass(nodeService.getType(nodeRef), DocumentCommonModel.Types.DOCUMENT)) {
                    adrService.addDeletedDocument(nodeRef);
                }
                nodeService.deleteNode(nodeRef);
            }
            seriesService.updateContainingDocsCountByVolume(volumeService.getVolumeByNodeRef(volumeNodeRef).getSeriesNodeRef(), volumeNodeRef, false);
            props.put(VolumeModel.Props.CONTAINING_DOCS_COUNT, 0);
            if (nodeService.hasAspect(volumeNodeRef, DocumentCommonModel.Aspects.DOCUMENT_REG_NUMBERS_CONTAINER)) {
                props.put(DocumentCommonModel.Props.DOCUMENT_REG_NUMBERS, null);
            }
            nodeService.addProperties(volumeNodeRef, props);
        }

        return volumesForDestruction.size();
    }

    @Override
    public void destroyArchivedVolumes(ActionEvent event) {
        AuthenticationUtil.runAs(new RunAsWork<Integer>() {
            @Override
            public Integer doWork() throws Exception {
                return destroyArchivedVolumes();
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    private List<NodeRef> searchVolumesForDestruction() {
        List<String> queryParts = new ArrayList<String>();
        queryParts.add(generateTypeQuery(VolumeModel.Types.VOLUME));
        queryParts.add(generateStringExactQuery(DocListUnitStatus.CLOSED.getValueName(), VolumeModel.Props.STATUS));
        queryParts.add(generateStringExactQuery("true", VolumeModel.Props.SEND_TO_DESTRUCTION));
        String query = joinQueryPartsAnd(queryParts);
        ResultSet resultSet = doSearch(query);
        try {
            List<NodeRef> volumesForDestruction = new ArrayList<NodeRef>();
            for (ResultSetRow resultSetRow : resultSet) {
                NodeRef nodeRef = resultSetRow.getNodeRef();
                if (!nodeService.exists(nodeRef)) {
                    continue;
                }
                volumesForDestruction.add(nodeRef);
            }
            return volumesForDestruction;
        } finally {
            resultSet.close();
        }
    }

    private ResultSet doSearch(String query) {
        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery(query);
        sp.addStore(archivalsStore);
        sp.setLimitBy(LimitBy.UNLIMITED);
        return searchService.query(sp);
    }

    @Override
    public List<Function> getArchivedFunctions() {
        return functionsService.getFunctions(getArchivalRoot());
    }

    private NodeRef getArchivedFunctionByMark(String functionMark) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(getArchivalRoot(),
                RegexQNamePattern.MATCH_ALL, FunctionsModel.Associations.FUNCTION);

        for (ChildAssociationRef childRef : childRefs) {
            String archivedFunMark = (String) nodeService.getProperty(childRef.getChildRef(), FunctionsModel.Props.MARK);
            if (functionMark.equals(archivedFunMark)) {
                return childRef.getChildRef(); // matching archived function found
            }
        }
        return null; // function is not archived before
    }

    private NodeRef getArchivedSeriesByIdentifies(NodeRef archivedFunRef, String identifier) {
        List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(archivedFunRef,
                RegexQNamePattern.MATCH_ALL, SeriesModel.Associations.SERIES);
        for (ChildAssociationRef childRef : childRefs) {
            String archivedSeriesId = (String) nodeService.getProperty(childRef.getChildRef(), SeriesModel.Props.SERIES_IDENTIFIER);
            if (identifier.equals(archivedSeriesId)) {
                return childRef.getChildRef(); // matching archived series found
            }
        }
        return null; // series is not archived before
    }

    @Override
    public NodeRef getArchivalRoot() {
        return generalService.getPrimaryArchivalsNodeRef();
    }

    private NodeRef getTempArchivalRoot() {
        return generalService.getNodeRef(ArchivalsModel.Repo.ARCHIVALS_TEMP_SPACE);
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setCopyService(CopyService copyService) {
        this.copyService = copyService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setAdrService(AdrService adrService) {
        this.adrService = adrService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }

    public void setArchivalsStore(String archivalsStore) {
        this.archivalsStore = new StoreRef(archivalsStore);
    }

    @Override
    public void removeJobNodeFromArchivingList(NodeRef archivingJobRef) {
        if (archivingJobRef != null) {
            nodeService.deleteNode(archivingJobRef);
        }
    }

    @Override
    public void removeVolumeFromArchivingList(NodeRef volumeRef) {
        if (volumeRef == null) {
            return;
        }
        for (NodeRef archivingJobNodeRef : getAllInQueueJobs()) {
            if (volumeRef.equals(nodeService.getProperty(archivingJobNodeRef, ArchivalsModel.Props.VOLUME_REF))) {
                nodeService.deleteNode(archivingJobNodeRef);
                LOG.info("Volume with nodeRef=" + volumeRef + " was removed from archiving queue.");
                return;
            }
        }
    }

    @Override
    public void cancelAllArchivingJobs(ActionEvent event) {
        StringBuilder sb = new StringBuilder();
        for (NodeRef archivingJobNodeRef : getAllInQueueJobs()) {
            Map<QName, Serializable> props = nodeService.getProperties(archivingJobNodeRef);
            NodeRef volumeRef = (NodeRef) props.get(ArchivalsModel.Props.VOLUME_REF);
            String status = (String) props.get(ArchivalsModel.Props.ARCHIVING_JOB_STATUS);
            if (volumeRef != null && nodeService.exists(volumeRef)) {
                nodeService.setProperty(volumeRef, VolumeModel.Props.MARKED_FOR_ARCHIVING, Boolean.FALSE);
            }
            removeJobNodeFromArchivingList(archivingJobNodeRef);
            sb.append(volumeRef + "\t(status: " + status + ")+\n");
        }
        LOG.info("The archiving of the following volumes was cancelled: " + sb.toString());
    }

    @Override
    public void pauseArchiving(ActionEvent event) {
        archivingPaused = true;
        LOG.info("Volume archiving was paused");
    }

    @Override
    public void continueArchiving(ActionEvent event) {
        archivingPaused = false;
        LOG.info("Volume archiving was resumed");
    }

    @Override
    public boolean isArchivingPaused() {
        return archivingPaused;
    }

    @Override
    public void doPauseArchiving() {
        while (archivingPaused) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

}