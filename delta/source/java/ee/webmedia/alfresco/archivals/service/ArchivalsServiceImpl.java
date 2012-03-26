package ee.webmedia.alfresco.archivals.service;

import static ee.webmedia.alfresco.utils.SearchUtil.generateStringExactQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.generateTypeQuery;
import static ee.webmedia.alfresco.utils.SearchUtil.joinQueryPartsAnd;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
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
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * @author Romet Aidla
 */
public class ArchivalsServiceImpl implements ArchivalsService {
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

    @Override
    public NodeRef archiveVolume(NodeRef volumeNodeRef, String archivingNote) {
        Assert.notNull(volumeNodeRef, "Reference to volume node must be provided");
        Volume volume = volumeService.getVolumeByNodeRef(volumeNodeRef);
        Series series = seriesService.getSeriesByNodeRef(volume.getSeriesNodeRef());

        // Make a copy of function and series and then move them if necessary.
        // We can't copy them directly to different store (CopyService doesn't support this).
        NodeRef copiedFunNodeRef = copyService.copy(series.getFunctionNodeRef(), getTempArchivalRoot(),
                FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION);
        String functionMark = (String) nodeService.getProperty(copiedFunNodeRef, FunctionsModel.Props.MARK);
        NodeRef copiedSeriesNodeRef = copyService.copy(series.getNode().getNodeRef(), copiedFunNodeRef,
                SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES);
        String seriesIdentifier = (String) nodeService.getProperty(copiedSeriesNodeRef, SeriesModel.Props.SERIES_IDENTIFIER);

        // find if function with given mark from archival store
        NodeRef archivedFunRef = getArchivedFunctionByMark(functionMark);
        NodeRef archivedSeriesRef = null;
        if (archivedFunRef == null) { // function isn't archived before, let's do this now
            archivedFunRef = nodeService.moveNode(copiedFunNodeRef, getArchivalRoot(),
                    FunctionsModel.Associations.FUNCTION, FunctionsModel.Associations.FUNCTION).getChildRef();
            // with previous call, also series will be moved, find the series node ref
            archivedSeriesRef = getArchivedSeriesByIdentifies(archivedFunRef, seriesIdentifier);
        } else { // function is archived, let's check whether we need to archive series
            archivedSeriesRef = getArchivedSeriesByIdentifies(archivedFunRef, seriesIdentifier);
            if (archivedSeriesRef == null) { // series isn't archived before, let's do this now
                nodeService.setProperty(copiedSeriesNodeRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, 0); // reset value if it' first time
                archivedSeriesRef = nodeService.moveNode(copiedSeriesNodeRef, archivedFunRef,
                        SeriesModel.Associations.SERIES, SeriesModel.Associations.SERIES).getChildRef();
            }

            // let's delete our temporary copy
            nodeService.deleteNode(copiedFunNodeRef);
        }
        Assert.notNull(archivedSeriesRef, "Series was not archived");

        seriesService.updateContainingDocsCountByVolume(series.getNode().getNodeRef(), volumeNodeRef, false);
        // now move volume with all children
        NodeRef archivedVolumeNodeRef = nodeService.moveNode(volumeNodeRef, archivedSeriesRef,
                VolumeModel.Associations.VOLUME, VolumeModel.Associations.VOLUME).getChildRef();
        nodeService.setProperty(archivedVolumeNodeRef, VolumeModel.Props.ARCHIVING_NOTE, archivingNote);
        seriesService.updateContainingDocsCountByVolume(archivedSeriesRef, archivedVolumeNodeRef, true);
        updateDocumentLocation(volume, archivedFunRef, archivedSeriesRef, archivedVolumeNodeRef);
        return archivedVolumeNodeRef;
    }

    private void updateDocumentLocation(Volume originalVolume, NodeRef archivedFunRef, NodeRef archivedSeriesRef, NodeRef archivedVolumeRef) {
        if (originalVolume.isContainsCases()) {
            for (Case aCase : caseService.getAllCasesByVolume(archivedVolumeRef)) {
                List<NodeRef> documents = documentService.getAllDocumentRefsByParentRef(aCase.getNode().getNodeRef());
                updateDocumentLocationProps(archivedFunRef, archivedSeriesRef, archivedVolumeRef, aCase.getNode().getNodeRef(), documents);
            }
        } else {
            List<NodeRef> documents = documentService.getAllDocumentRefsByParentRef(archivedVolumeRef);
            updateDocumentLocationProps(archivedFunRef, archivedSeriesRef, archivedVolumeRef, null, documents);
        }
    }

    private void updateDocumentLocationProps(NodeRef archivedFunRef, NodeRef archivedSeriesRef, NodeRef archivedVolumeRef, NodeRef archivedCaseRef, List<NodeRef> docRefs) {
        for (NodeRef docRef : docRefs) {
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(DocumentCommonModel.Props.FUNCTION, archivedFunRef);
            props.put(DocumentCommonModel.Props.SERIES, archivedSeriesRef);
            props.put(DocumentCommonModel.Props.VOLUME, archivedVolumeRef);
            props.put(DocumentCommonModel.Props.CASE, archivedCaseRef);
            nodeService.addProperties(docRef, props);
        }
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

    private NodeRef getArchivalRoot() {
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

}
