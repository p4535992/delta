package ee.webmedia.alfresco.importer.excel.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.TransmittalMode;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.classificator.model.Classificator;
import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.classificator.service.ClassificatorService;
import ee.webmedia.alfresco.common.service.IClonable;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.model.DocumentParentNodesVO;
import ee.webmedia.alfresco.document.model.DocumentSubtypeModel;
import ee.webmedia.alfresco.document.model.LetterDocument;
import ee.webmedia.alfresco.document.service.DocumentServiceImpl;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.model.FunctionsModel;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.importer.excel.vo.CaseImportVO;
import ee.webmedia.alfresco.importer.excel.vo.ContractSmitDocument;
import ee.webmedia.alfresco.importer.excel.vo.ImportDocument;
import ee.webmedia.alfresco.importer.excel.vo.IncomingLetter;
import ee.webmedia.alfresco.importer.excel.vo.SendInfo;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.utils.beanmapper.BeanPropertyMapper;
import ee.webmedia.alfresco.volume.model.Volume;

/**
 * <b> NB! This class is not stateless as usually service classes are.</b><br>
 * <br>
 * The problem is that while importing hundreds of documents, transactions get long and operations related to repository get extremely slow. <br>
 * To overcome this issue, not all documents are imported within the same transaction and hence there is some extra effort to maintain data consistency. For
 * that reason this there are {@link #casesCache} and {@link #assocsToCreate} that remember successfully created cases(by previous SUCCESSFUL transactions)
 * and associations that need to be later created (as we might find that the source of all the previous associations should be replaced
 * with another document). <br>
 * <br>
 * There is also field {@link #locationCache} that keep in memory functions, series and volumes for performance reasons.
 * 
 * @author Ats Uiboupin
 */
public class DocumentImportServiceImpl extends DocumentServiceImpl implements DocumentImportService {
    private static final org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentImportServiceImpl.class);

    /** NB! not injected, use getter to access this service */
    private FunctionsService _functionsService;
    private Classificator transmittalModeClassificator;
    private HashMap<String/* transmittalMode valueName in lowerCase */, ClassificatorValue> transmittalModes;

    // private HashMap<NodeRef /* volumeRef */, Map<String /* caseId */, Case>> casesCache = new HashMap<NodeRef, Map<String, Case>>();
    // START: fields that need to be restored if something fails
    private HashMap<NodeRef /* volumeRef */, CasesByVolumeHolder> casesCache = new HashMap<NodeRef, CasesByVolumeHolder>();
    private Map<String, AssocVO> assocsToCreate = new HashMap<String, AssocVO>();
    // END: fields that need to be restored if something fails
    // START: fields that hold values to fields that need to be restored if something fails
    private HashMap<NodeRef /* volumeRef */, CasesByVolumeHolder> casesCacheBeforeFailure;
    private Map<String, AssocVO> assocsToCreateBeforeFailure;
    private boolean processRunning;
    // END: fields that hold values to fields that need to be restored if something fails

    private LocationCache locationCache = new LocationCache();
    private LocationCache locationCacheBeforeFailure;

    private String attachmentFilesLocationBase;
    private ClassificatorService classificatorService;
    public static final QName smitDocListImported = QName.createQName(FunctionsModel.URI, "smitDocListImported");

    private static final Comparator<ImportDocument> regDateComparator = new Comparator<ImportDocument>() {
        @Override
        public int compare(ImportDocument d1, ImportDocument d2) {
            if (d1.getRegDateTime() == null || d2.getRegDateTime() == null) {
                throw new IllegalStateException("regDateTime is null while comparing documents:\n" + d1 + "\nand\n" + d2);
            }
            int res = d1.getRegDateTime().compareTo(d2.getRegDateTime());
            if (res == 0) {
                final long o1 = d1.getOrderOfAppearance();
                final long o2 = d2.getOrderOfAppearance();
                if (o1 == o2) {
                    throw new RuntimeException("Orders should not be equal!\nd1=" + d1 + "\n\nd2=" + d2);
                }
                res = (o1 < o2 ? -1 : 1);
            }
            return res;
        }
    };

    /**
     * {@inheritDoc} There is a speed issue when hundreds of documents are imported into the repository within the same transaction. <br>
     * The solution is that documents are imported within different transactions one after another.
     * This implementation is much more complex just because we can't expect that all documents that must be created under the same case
     * would be passed into the method at once.
     */
    public <IDoc extends ImportDocument> void importDocuments(List<IDoc> documents) {
        if (processRunning) {
            throw new RuntimeException("Another instance is already running, can't import documents");
        }
        processRunning = true;
        casesCacheBeforeFailure = new HashMap<NodeRef, CasesByVolumeHolder>(casesCache);
        assocsToCreateBeforeFailure = new HashMap<String, AssocVO>(assocsToCreate);
        locationCacheBeforeFailure = locationCache.clone();
        HashMap<NodeRef /* volumeRef */, CasesByVolumeHolder> newCasesCache = new HashMap<NodeRef, CasesByVolumeHolder>();
        final HashMap<String, AssocVO> newAssocsToCreate = new HashMap<String, AssocVO>();
        try {
            // final HashMap<String, Case> newCases = new HashMap<String, Case>();
            final HashMap<String /* volume&regNumber */, Set<ImportDocument>> documentsByRegNr = orderDocumentsByTargetLocation(documents);
            for (Entry<String/* volume&regNumber */, Set<ImportDocument>> entry : documentsByRegNr.entrySet()) {
                final String volumeAndRegNumber = entry.getKey();
                log.info("Adding " + entry.getValue().size() + " documents to " + volumeAndRegNumber);
                Volume targetVolume = null;
                // save the case of previous document that was imported into the same location
                Case caseOfPreviousDocumentInSameTargetLocation = null;
                ExtendedDocumentParentNodesVO location = null;
                int docNrInSamePlace = 0;
                for (ImportDocument doc : entry.getValue()) {
                    docNrInSamePlace++;
                    if (StringUtils.isNotBlank(doc.getNodeRefInRepo())) {
                        if (!nodeService.exists(new NodeRef(doc.getNodeRefInRepo()))) {
                            throw new RuntimeException(
                                    "According to importfile this document is was already stored in repository, but according nodeRef is not found:\n" + doc);
                        }
                        log.info("skipping document, as it is already stored to " + volumeAndRegNumber);
                        continue; // already imported
                    }
                    try {
                        if (log.isTraceEnabled()) {
                            log.trace("Adding document to " + volumeAndRegNumber + " doc:\n" + doc);
                        }
                        if (targetVolume == null) {
                            location = getDocLocation(doc);
                            targetVolume = location.volume;
                        }
                        location.setPropertiesTo(doc);
                        if (targetVolume.isContainsCases()) {
                            final NodeRef volumeRef = targetVolume.getNode().getNodeRef();
                            // search from cases created in this transaction
                            CasesByVolumeHolder casesByVolumeHolder = newCasesCache.get(volumeRef);
                            if (casesByVolumeHolder == null) {
                                casesByVolumeHolder = casesCache.get(volumeRef); // search from cases created from all transactions
                                if (casesByVolumeHolder != null) {
                                    // make sure that fields of casesByVolumeHolder don't get corrupted by transaction rollBack
                                    casesByVolumeHolder = casesByVolumeHolder.clone();
                                } else {
                                    final List<Case> allCasesByVolume = getCaseService().getAllCasesByVolume(volumeRef, DocListUnitStatus.OPEN);
                                    casesByVolumeHolder = new CasesByVolumeHolder(allCasesByVolume);
                                }
                                newCasesCache.put(volumeRef, casesByVolumeHolder);
                            }
                            caseOfPreviousDocumentInSameTargetLocation //
                            = casesByVolumeHolder.getCaseForDoc(doc, volumeRef, caseOfPreviousDocumentInSameTargetLocation);
                        }
                        final NodeRef parentRef;
                        final Map<QName, Serializable> docProps = getDocProperties(doc);
                        if (caseOfPreviousDocumentInSameTargetLocation != null) {
                            parentRef = caseOfPreviousDocumentInSameTargetLocation.getNode().getNodeRef();
                            docProps.put(QName.createQName(TransientProps.CASE_NODEREF), caseOfPreviousDocumentInSameTargetLocation.getNode().getNodeRef());
                        } else {
                            parentRef = targetVolume.getNode().getNodeRef();
                        }
                        // final Node createdDocNode = createDocument(doc.getDocumentTypeId(), parentRef, docProps);
                        Node createdDocNode = null;
                        try {
                            createdDocNode = createDocument(doc.getDocumentTypeId(), parentRef, docProps);
                        } catch (Exception e) {
                            throw new RuntimeException("failed to save " + docNrInSamePlace + ". doc under the same case: "
                                    + caseOfPreviousDocumentInSameTargetLocation, e);
                        }
                        doc.setNodeRefInRepo(createdDocNode.getNodeRefAsString());
                        if (doc instanceof LetterDocument && StringUtils.isNotBlank(doc.getRegNumber())) {
                            if (caseOfPreviousDocumentInSameTargetLocation == null) {
                                log.error("caseOfPreviousDocumentInSameTargetLocation=" + caseOfPreviousDocumentInSameTargetLocation);
                                locationCache.reset();
                                throw new RuntimeException("Letter documents should be put under volumes that contain cases, but target volume for this " +
                                        "document is configured not to contain cases. Maybe You should reconfigure volume:\n" + targetVolume);
                            }
                            // Series where letters are saved, must all contain cases...
                            // ..and case of type CaseImportVO must have been created while importing if case regNumber is not blank
                            CaseImportVO importedCase = (CaseImportVO) caseOfPreviousDocumentInSameTargetLocation;
                            // 
                            Node initialDocNode = importedCase.getInitialDocNode();
                            if (initialDocNode == null) {
                                importedCase.setInitialDocNode(createdDocNode);
                                newAssocsToCreate.put(volumeAndRegNumber, new AssocVO(createdDocNode, doc.getRegDateTime()));
                            } else {
                                AssocVO assocVO = newAssocsToCreate.get(volumeAndRegNumber);
                                if (assocVO == null) {
                                    assocVO = assocsToCreate.get(volumeAndRegNumber);
                                    if (assocVO != null) {
                                        assocVO = assocVO.clone();
                                    } else {
                                        // assocVO = new AssocVO(createdDocNode, doc.getRegDateTime());
                                        throw new RuntimeException("Unexpected importedCase=" + importedCase);
                                    }
                                    newAssocsToCreate.put(volumeAndRegNumber, assocVO);
                                } else {
                                    // make sure that assocVO.relatedDocs don't get corrupted by transaction rollBack
                                    newAssocsToCreate.get(volumeAndRegNumber).clone();
                                }
                                assocVO.addRelatedDoc(createdDocNode, doc.getRegDateTime());
                            }
                        }
                        addRelatedNodes(doc, createdDocNode);
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Failed to store document to repository:\n" + doc, e);
                    }
                }
            }
            // managed to import documents into the repository, save nodeRefs to Excel source file, so that if one of the following transactions fails,
            // then we know which documents to import and which to exclude
            saveNoderefsToSourceFile(documents);
        } catch (RuntimeException e) {
            // restore fields that are affected by the failure of transaction
            assocsToCreate = assocsToCreateBeforeFailure;
            locationCache = locationCacheBeforeFailure;
            casesCache = casesCacheBeforeFailure;
            processRunning = false;
            throw e;
        }
        casesCache.putAll(newCasesCache);
        assocsToCreate.putAll(newAssocsToCreate);
        processRunning = false;
    }

    private <IDoc extends ImportDocument> void saveNoderefsToSourceFile(List<IDoc> documents) {
        if (documents.size() == 0) {
            return;
        }
        final IDoc sampleDoc = documents.get(0);
        InputStream inp = null;
        final File rowSourceFile = sampleDoc.getRowSourceFile();
        FileOutputStream fileOut = null;
        try {
            inp = new FileInputStream(rowSourceFile);
            final Workbook wb = WorkbookFactory.create(inp);
            final Sheet sheet = wb.getSheet(sampleDoc.getRowSourceSheet());
            for (IDoc doc : documents) {
                final int rowNr = doc.getRowSourceNumber();
                final String docRef = doc.getNodeRefInRepo();
                final Row row = sheet.getRow(rowNr);
                final Cell cell = row.createCell(/* col Z */25);
                if (StringUtils.isBlank(docRef)) {
                    throw new RuntimeException("Document has still no nodeRef: doc=\n" + doc);
                }
                cell.setCellValue(docRef);
            }
            final boolean canWrite = rowSourceFile.canWrite();
            if (!canWrite) {
                throw new RuntimeException("Can't write to '" + rowSourceFile + "' (can" + (rowSourceFile.canRead() ? "" : "'t") + " read from file)");
            }
            try {
                fileOut = new FileOutputStream(rowSourceFile);
                wb.write(fileOut);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Can't write to file as it is used by another process", e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to file", e);
        } finally {
            IOUtils.closeQuietly(inp);
            IOUtils.closeQuietly(fileOut);
        }
    }

    @Override
    public void createAssocs() {
        if (processRunning) {
            throw new RuntimeException("Another instance is already running, can't create associations");
        }
        try {
            processRunning = true;
            final Collection<AssocVO> assocs = assocsToCreate.values();
            int totalNrOfFollowUps = 0;
            int totalNrOfReplies = 0;
            for (AssocVO assocVO : assocs) {
                final Node initialDoc = assocVO.getInitialDoc();
                final QName initialDocType = initialDoc.getType();
                final List<Node> relatedDocs = assocVO.getRelatedDocs();
                int nrOfFollowUps = 0;
                int nrOfReplies = 0;
                for (Node relatedDoc : relatedDocs) {
                    final QName relatedDocType = relatedDoc.getType();
                    if (DocumentSubtypeModel.Types.INCOMING_LETTER.equals(initialDocType)) {
                        if (DocumentSubtypeModel.Types.INCOMING_LETTER.equals(relatedDocType)) {
                            addFollowupAssoc(relatedDoc.getNodeRef(), initialDoc.getNodeRef());
                            nrOfFollowUps++;
                        } else {
                            addReplyAssoc(relatedDoc.getNodeRef(), initialDoc.getNodeRef());
                            nrOfReplies++;
                        }
                    } else if (DocumentSubtypeModel.Types.OUTGOING_LETTER.equals(initialDocType)) {
                        if (DocumentSubtypeModel.Types.INCOMING_LETTER.equals(relatedDocType)) {
                            addReplyAssoc(relatedDoc.getNodeRef(), initialDoc.getNodeRef());
                            nrOfReplies++;
                        } else {
                            addFollowupAssoc(relatedDoc.getNodeRef(), initialDoc.getNodeRef());
                            nrOfFollowUps++;
                        }
                    } else {
                        throw new RuntimeException("initialDoc has unknown document type: initialDocNode:\n" + initialDoc);
                    }
                }
                if (log.isDebugEnabled() && relatedDocs.size() > 0) {
                    log.debug("Added " + nrOfFollowUps + " followups and " + nrOfReplies + " replies");
                }
                totalNrOfFollowUps += nrOfFollowUps;
                totalNrOfReplies += nrOfReplies;
            }
            if (log.isInfoEnabled() && assocs.size() > 0) {
                log.info("Created assocs from " + assocs.size() + " documents in total to " + totalNrOfFollowUps + " followups and " + totalNrOfReplies
                        + " replies");
            }
            final NodeRef functionsRoot = getFunctionsService().getFunctionsRoot();
            nodeService.setProperty(functionsRoot, smitDocListImported, Boolean.TRUE);
        } finally {
            processRunning = false;
        }
    }

    private static Map<Class<? extends ImportDocument>, BeanPropertyMapper> mappers = new HashMap<Class<? extends ImportDocument>, BeanPropertyMapper>();

    private Map<QName, Serializable> getDocProperties(ImportDocument doc) {
        @SuppressWarnings("unchecked")
        BeanPropertyMapper<ImportDocument> mapper = mappers.get(doc.getClass());
        if (mapper == null) {
            @SuppressWarnings("unchecked")
            BeanPropertyMapper<ImportDocument> propertyMapper = (BeanPropertyMapper<ImportDocument>) BeanPropertyMapper.newInstance(doc.getClass());
            mapper = propertyMapper;
            mappers.put(doc.getClass(), mapper);
        }
        final Map<QName, Serializable> properties = mapper.toProperties(doc);
        return properties;
    }

    /**
     * @param documents - expected that the iterator returns documents in the order they appear in the documents list
     * @return documents grouped by volume + regNumber, so that the values of the same map entry are guaranteed to have same target location in
     *         documents register (either under the case if volume contains cases or directly under volume)
     */
    private <IDoc extends ImportDocument> HashMap<String, Set<ImportDocument>> orderDocumentsByTargetLocation(List<IDoc> documents) {
        final HashMap<String /* fnSerVol */, Set<ImportDocument>> documentsByRegNr = new HashMap<String /* fnSerVol */, Set<ImportDocument>>();
        for (ImportDocument doc : documents) {
            final String separator = " ¤>¤ ";
            String regNrWoIndividualizingNr = doc.getRegNumber();
            if (StringUtils.isNotBlank(regNrWoIndividualizingNr)) {
                // documents with the same regNr must be put into the same case.
                // If regNr contains individualizing number(part after the first dash), then individualizing number is insignificant for grouping under the case
                regNrWoIndividualizingNr = getRegNrWoIndividualizingNr(regNrWoIndividualizingNr);
                Assert.notNull(doc.getRegDateTime(), "Document registration date must not be null when registration number is set! Document = " + doc);
            }
            // String docLocationKey = doc.getFunction() + separator + doc.getSeries() + separator + doc.getVolume() + separator + regNrWoIndividualizingNr;
            String docLocationKey = doc.getVolume() + separator + regNrWoIndividualizingNr;
            Set<ImportDocument> docsInSameLocation = documentsByRegNr.get(docLocationKey);
            if (docsInSameLocation == null) {
                if (doc.getRegNumber() == null) {
                    docsInSameLocation = new HashSet<ImportDocument>(); // no ordering needed
                } else {
                    // must order documents based on registration regDateTime (and order of appearance in original file)
                    docsInSameLocation = new TreeSet<ImportDocument>(regDateComparator);
                }
                documentsByRegNr.put(docLocationKey, docsInSameLocation);
            }
            docsInSameLocation.add(doc);
        }
        return documentsByRegNr;
    }

    public static String getRegNrWoIndividualizingNr(String regNr) {
        if (StringUtils.isBlank(regNr)) {
            return null;
        }
        final int regNrPartEndIndex = regNr.indexOf('-');
        if (regNrPartEndIndex >= 0) {
            regNr = regNr.substring(0, regNrPartEndIndex);
        }
        return regNr;
    }

    interface MissingLocationResolveStrategy {
        NodeRef resolveMissingFunction();

    }

    private <X> void debugExistingContent(String x, NodeRef parentRef, Map<String, X> map) {
        if (log.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder(x + (parentRef != null ? " under " + parentRef : "") + ":\n");
            final Set<Entry<String, X>> entrySet = map.entrySet();
            for (Entry<String, X> entry : entrySet) {
                sb.append("\n").append(entry.getKey()).append(":\t").append(entry.getValue());
            }
            log.debug(sb.toString());
        }
    }

    /**
     * 2.1.3. Kui dokumendi Juurdepääsupiirang Excelis != null, siis dokumendi accessRestriction, accessRestrictionReason ja väärtused võetakse sarja
     * küljest, kuhu dokument lisatakse
     */
    private void setAccessRestrictionFromSeriesIfNeeded(ImportDocument doc, Series series) {
        final String accessRestriction = doc.getAccessRestriction();
        if (StringUtils.isNotBlank(accessRestriction)) {
            final Map<String, Object> serProps = series.getNode().getProperties();
            final String serAccessRestriction = (String) serProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION);
            final String serAccessRestrictionReason = (String) serProps.get(DocumentCommonModel.Props.ACCESS_RESTRICTION_REASON);
            doc.setAccessRestriction(serAccessRestriction);
            doc.setAccessRestrictionReason(serAccessRestrictionReason);
        }
    }

    private ExtendedDocumentParentNodesVO getDocLocation(ImportDocument doc) {
        final NodeRef functionRef = locationCache.getFunctionRef(doc, false);
        final Series series = locationCache.getSeries(doc, functionRef, false);
        final Node seriesNode = series.getNode();
        final Volume volume = locationCache.getVolume(doc, seriesNode.getNodeRef(), false);
        setAccessRestrictionFromSeriesIfNeeded(doc, series);
        return new ExtendedDocumentParentNodesVO(new Node(functionRef), seriesNode, volume.getNode(), null, volume);
    }

    /**
     * Cache functions, series and volumes for performance reasons
     * 
     * @author Ats Uiboupin
     */
    private class LocationCache implements IClonable<LocationCache> {
        private HashMap<String /* functionMark */, NodeRef> functionsCache;
        private HashMap<NodeRef /* functionRef */, Map<String /* seriesIdentifier */, Series>> seriesCache = new HashMap<NodeRef, Map<String, Series>>();
        private HashMap<NodeRef /* seriesRef */, Map<String /* volume... */, Volume>> volumesCache = new HashMap<NodeRef, Map<String, Volume>>();

        @Override
        public LocationCache clone() {
            final LocationCache clone = new LocationCache();
            if (functionsCache != null) {
                clone.functionsCache = new HashMap<String /* functionMark */, NodeRef>(functionsCache);
            }
            clone.seriesCache = new HashMap<NodeRef /* functionRef */, Map<String /* seriesIdentifier */, Series>>(seriesCache);
            clone.volumesCache = new HashMap<NodeRef /* seriesRef */, Map<String /* volume... */, Volume>>(volumesCache);
            return clone;
        }

        private void reset() {
            functionsCache = null;
            seriesCache = new HashMap<NodeRef, Map<String, Series>>();
            volumesCache = new HashMap<NodeRef, Map<String, Volume>>();
        }

        private NodeRef getFunctionRef(ImportDocument doc, boolean forceRefresh) {
            if (functionsCache == null || forceRefresh) {
                List<Function> allOpenedFunctions = getFunctionsService().getAllFunctions(DocListUnitStatus.OPEN);
                functionsCache = new HashMap<String, NodeRef>(allOpenedFunctions.size());
                for (Function function : allOpenedFunctions) {
                    functionsCache.put(function.getMark(), function.getNodeRef());
                }
            }
            NodeRef functionRef = functionsCache.get(doc.getFunction());
            if (functionRef == null) {
                if (forceRefresh) {
                    debugExistingContent("Functions", null, functionsCache);
                    throw new IllegalStateException("No function exists with mark '" + doc.getFunction() + "' - unable to import document:\n" + doc);
                }
                functionRef = getFunctionRef(doc, true);
            }
            return functionRef;
        }

        private Series getSeries(ImportDocument doc, final NodeRef functionRef, boolean forceRefresh) {
            Map<String, Series> seriesMap = seriesCache.get(functionRef);
            if (seriesMap == null || forceRefresh) {
                final List<Series> allOpenedSeries = seriesService.getAllSeriesByFunction(functionRef, DocListUnitStatus.OPEN, null);
                seriesMap = new HashMap<String, Series>(allOpenedSeries.size());
                for (Series series : allOpenedSeries) {
                    seriesMap.put(series.getSeriesIdentifier(), series);
                }
                seriesCache.put(functionRef, seriesMap);
            }
            Series series = seriesMap.get(doc.getSeries());
            if (series == null) {
                if (forceRefresh) {
                    debugExistingContent("Series", functionRef, seriesMap);
                    throw new IllegalStateException("No series exists with seriesIdentifier '" + doc.getSeries() + "' - unable to import document:\n" + doc);
                }
                series = getSeries(doc, functionRef, true);
            }
            if (!series.getDocType().contains(doc.getDocumentTypeId())) {
                throw new IllegalStateException("Found series by seriesIdentifier, but adding document of type '" + doc.getDocumentTypeId()
                        + "' is not suported" + "' - unable to import document:\n" + doc);
            }
            return series;
        }

        private Volume getVolume(ImportDocument doc, final NodeRef seriesRef, boolean forceRefresh) {
            Map<String, Volume> volumesMap = volumesCache.get(seriesRef);
            if (volumesMap == null || forceRefresh) {
                final List<Volume> allVolumesBySeries = volumeService.getAllVolumesBySeries(seriesRef);
                volumesMap = new HashMap<String, Volume>(allVolumesBySeries.size());
                for (Volume volume : allVolumesBySeries) {
                    if (DocListUnitStatus.OPEN.getValueName().equals(volume.getStatus())) {
                        volumesMap.put(volume.getVolumeMark(), volume);
                    }
                }
                volumesCache.put(seriesRef, volumesMap);
            }
            final String volumeMark = doc.getVolume();
            Volume volume = volumesMap.get(volumeMark);
            if (volume == null) {
                if (doc instanceof ContractSmitDocument) {
                    final Volume newVolume = volumeService.createVolume(seriesRef);
                    newVolume.setVolumeMark(volumeMark);
                    newVolume.setTitle(((ContractSmitDocument) doc).getVolumeTitle());
                    newVolume.setVolumeType(VolumeType.YEAR_BASED.getValueName());
                    newVolume.setContainsCases(false);
                    volumeService.saveOrUpdate(newVolume, false);
                    volumesMap.put(newVolume.getVolumeMark(), newVolume);
                    return newVolume;
                }
                if (forceRefresh) {
                    debugExistingContent("Volumes", seriesRef, volumesMap);
                    throw new IllegalStateException("No volume exists with volumeMark '" + volumeMark + "' - unable to import document:\n" + doc);
                }
                volume = getVolume(doc, seriesRef, true);
            }
            return volume;
        }
    }

    /**
     * Keeps track of associations that need to be created.
     * 
     * @author Ats Uiboupin
     */
    static class AssocVO implements IClonable<AssocVO> {
        private Node initialDoc;
        private List<Node> relatedDocs;
        private Date dateOfEarliestDocRegistration;

        public AssocVO(Node initialDoc, Date docRegistrationDate) {
            this.initialDoc = initialDoc;
            this.dateOfEarliestDocRegistration = docRegistrationDate;
        }

        @Override
        public AssocVO clone() {
            final AssocVO clone = new AssocVO(initialDoc, dateOfEarliestDocRegistration);
            clone.relatedDocs = relatedDocs == null ? null : new ArrayList<Node>(relatedDocs);
            return clone;
        }

        public void addRelatedDoc(Node relatedDoc, Date docRegistrationDate) {
            if (relatedDocs == null) {
                relatedDocs = new ArrayList<Node>(5);
            }
            if (dateOfEarliestDocRegistration != null && docRegistrationDate != null && docRegistrationDate.before(dateOfEarliestDocRegistration)) {
                // swap initialDoc
                dateOfEarliestDocRegistration = docRegistrationDate;
                relatedDocs.add(initialDoc);
                initialDoc = relatedDoc;
            } else {
                relatedDocs.add(relatedDoc);
            }
        }

        public Node getInitialDoc() {
            return initialDoc;
        }

        public List<Node> getRelatedDocs() {
            return relatedDocs != null ? relatedDocs : Collections.<Node> emptyList();
        }
    }

    class ExtendedDocumentParentNodesVO extends DocumentParentNodesVO {
        private Volume volume;

        public ExtendedDocumentParentNodesVO(Node functionNode, Node seriesNode, Node volumeNode, Node caseNode, Volume volume) {
            super(functionNode, seriesNode, volumeNode, caseNode);
            this.volume = volume;
        }

        public void setPropertiesTo(ImportDocument doc) {
            doc.setFunction(getFunctionNode().getNodeRefAsString());
            doc.setSeries(getSeriesNode().getNodeRefAsString());
            doc.setVolume(getVolumeNode().getNodeRefAsString());
        }

    }

    class CasesByVolumeHolder implements IClonable<CasesByVolumeHolder> {
        private Map<String, Case> casesByTitle;
        /** Cases corresponding to registered documents regNumber without individualizing part of the regNumber */
        private Map<String /* docRegNrWoIndividualizingNr */, CaseImportVO> casesByRegNumber;

        public CasesByVolumeHolder(List<Case> allCasesByVolume) {
            casesByTitle = new HashMap<String, Case>(allCasesByVolume.size());
            for (Case theCase : allCasesByVolume) {
                casesByTitle.put(theCase.getTitle(), theCase);
            }
        }

        private CasesByVolumeHolder() {
            // for cloning
        }

        @Override
        public CasesByVolumeHolder clone() {
            final CasesByVolumeHolder clone = new CasesByVolumeHolder();
            clone.casesByTitle = cloneMap(this.casesByTitle.entrySet());
            clone.casesByRegNumber = cloneMap(this.casesByRegNumber.entrySet());
            return clone;
        }

        private <ACase extends Case> HashMap<String, ACase> cloneMap(final Set<Entry<String, ACase>> originalMap) {
            final HashMap<String, ACase> clonedMap = new HashMap<String, ACase>(originalMap.size());
            for (Entry<String, ACase> entry : originalMap) {
                ACase case2 = entry.getValue();
                if (case2 instanceof IClonable<?>) {
                    @SuppressWarnings("unchecked")
                    final IClonable<ACase> clonableCase = (IClonable<ACase>) case2;
                    case2 = clonableCase.clone();
                }
                clonedMap.put(entry.getKey(), case2);
            }
            return clonedMap;
        }

        /**
         * @param doc
         * @param volumeRef
         * @param targetCaseCandidate - if previous document had the same volume and regNumber(without individualizing part of the regNumber), then provide the
         *            case used by previous document
         * @param newCases
         * @return
         */
        public Case getCaseForDoc(ImportDocument doc, NodeRef volumeRef, Case targetCaseCandidate) {
            final String regNumber = doc.getRegNumber();
            if (regNumber == null) {
                String caseTitle = createUniqueCaseTitle(doc);
                targetCaseCandidate = new Case(); // new case just for one document
                targetCaseCandidate.setTitle(caseTitle);
                targetCaseCandidate.setVolumeNodeRef(volumeRef);
                targetCaseCandidate.setStatus(DocListUnitStatus.OPEN.getValueName());
                getCaseService().saveOrUpdate(targetCaseCandidate);
                casesByTitle.put(caseTitle, targetCaseCandidate);
            } else {
                if (targetCaseCandidate != null) {
                    return changeCaseNameIfNeeded((CaseImportVO) targetCaseCandidate, doc);
                }
                if (casesByRegNumber == null) {
                    casesByRegNumber = new HashMap<String, CaseImportVO>();
                }
                final String regNrWoIndividualizingNr = getRegNrWoIndividualizingNr(regNumber);
                targetCaseCandidate = casesByRegNumber.get(regNrWoIndividualizingNr);
                if (targetCaseCandidate != null) {
                    return changeCaseNameIfNeeded((CaseImportVO) targetCaseCandidate, doc);
                }
                String caseTitle = createUniqueCaseTitle(doc);
                CaseImportVO importCase = new CaseImportVO(); // create new case, that is probably used by many documents
                importCase.setTitle(caseTitle);
                importCase.setVolumeNodeRef(volumeRef);
                importCase.setRegNumber(regNumber);
                importCase.setDateOfEarliestDocRegistration(doc.getRegDateTime());
                casesByRegNumber.put(regNrWoIndividualizingNr, importCase);
                targetCaseCandidate = importCase;
                getCaseService().saveOrUpdate(targetCaseCandidate);
                casesByTitle.put(caseTitle, targetCaseCandidate);
            }
            return targetCaseCandidate;
        }

        /**
         * rule 2.1.12.a from spec SMIT andmete ülekandmine.docx:
         * Asja nimeks määratakse kõige varasema Kuupäev väärtusega dokumendi nimi; kui leidub mitu sama Kuupäevaga dokumenti, võetakse pealkiri nimekirjas
         * eespool olevast dokumendist.
         */
        private Case changeCaseNameIfNeeded(CaseImportVO targetCaseCandidate, ImportDocument doc) {
            final String previousTitle = StringUtils.trimToEmpty(targetCaseCandidate.getTitle());
            final String docName = StringUtils.trimToEmpty(doc.getDocName());
            // previous title might have been suffixed with number in brackets
            final boolean namesSimilar = previousTitle.startsWith(docName) && (previousTitle.length() - docName.length()) <= 5;
            if (!namesSimilar && targetCaseCandidate.getDateOfEarliestDocRegistration().after((doc.getRegDateTime()))) {
                casesByTitle.remove(previousTitle);
                String caseNewTitle = createUniqueCaseTitle(doc);
                targetCaseCandidate.setTitle(caseNewTitle);
                getCaseService().saveOrUpdate(targetCaseCandidate, false);
                casesByTitle.put(caseNewTitle, targetCaseCandidate);
                log.info("Case renamed, because found document with earlier registration date. CaseRef="
                        + targetCaseCandidate.getNode().getNodeRefAsString() + " \noldTitle='" + previousTitle + "'\nnewTitle=" + caseNewTitle);
            }
            return targetCaseCandidate;
        }

        private String createUniqueCaseTitle(ImportDocument doc) {
            String caseTitle = doc.getDocName();
            String caseTitleBaseName = caseTitle;
            int i = 1;
            do {
                final Case caseWithTheSameName = casesByTitle.get(caseTitle);
                if (caseWithTheSameName == null) {
                    return caseTitle;
                } else if (caseWithTheSameName instanceof CaseImportVO) {
                    CaseImportVO importCase = (CaseImportVO) caseWithTheSameName;
                    if (importCase.couldContain(doc)) {
                        return caseWithTheSameName.getTitle();
                    }
                }
                caseTitle = caseTitleBaseName + "(" + i + ")";
                i++;
            } while (true);
        }

        @Override
        public String toString() {
            return "CasesByVolumeHolder:\ncasesByTitle " + casesByTitle.keySet() + "\ncasesByRegNumber" + casesByRegNumber.keySet();
        }
    }

    private void addRelatedNodes(ImportDocument doc, Node docNode) {
        final SendInfo sendInfo = doc.getSendInfo();
        if (sendInfo != null) {
            Map<QName, Serializable> props = new HashMap<QName, Serializable>();
            props.put(DocumentCommonModel.Props.SEND_INFO_SEND_MODE, sendInfo.getSendMode());
            sendOutService.addSendinfo(docNode.getNodeRef(), props);
        }
        final List<String> fileLocations = doc.getFileLocations();
        if (fileLocations != null) {
            for (String fileLocation : fileLocations) {
                File fileToAdd = getFile(attachmentFilesLocationBase, fileLocation);
                if (fileToAdd != null) {
                    generalService.addFileOrFolder(fileToAdd, docNode.getNodeRef(), true);
                } else {
                    throw new RuntimeException("file not found - Can't add file '" + attachmentFilesLocationBase + fileLocation + "' to document:\n" + doc);
                }
            }
        }
        if (doc instanceof IncomingLetter) {
            setTransmittalMode((IncomingLetter) doc);
        }
    }

    public static File getFile(String attachmentFilesLocationBase, String fileLocation) {
        if (StringUtils.isNotBlank(fileLocation)) {
            File file = new File(fileLocation);
            if (!file.exists()) {
                fileLocation = fileLocation.replace('\\', '/');
                if (fileLocation.startsWith("\\") || fileLocation.startsWith("/")) {
                    fileLocation = fileLocation.substring(1);
                }
                String filePath = attachmentFilesLocationBase + fileLocation;
                file = new File(filePath);
                if (!file.exists()) {
                    return null;
                }
            } else {
                log.info("File/folder from network, adding might take a while: " + file.getAbsolutePath());
            }
            return file;
        }
        return null;
    }

    private void setTransmittalMode(IncomingLetter in) {
        final String transmittalMode = in.getTransmittalMode();
        if (StringUtils.isNotBlank(transmittalMode)) {
            final String transmittalModeClassificatorName = TransmittalMode.getClassificatorName();
            if (transmittalModes == null) {
                transmittalModeClassificator = classificatorService.getClassificatorByName(transmittalModeClassificatorName);
                final List<ClassificatorValue> transmittalModeValues = classificatorService.getAllClassificatorValues(transmittalModeClassificator);
                transmittalModes = new HashMap<String, ClassificatorValue>(transmittalModeValues.size());
                for (ClassificatorValue classificatorValue : transmittalModeValues) {
                    transmittalModes.put(classificatorValue.getValueName().trim().toLowerCase(), classificatorValue);
                }
            }
            if (!transmittalModes.containsKey(transmittalMode.trim().toLowerCase())) {
                // create new classificatorValue
                int maxOrder = -1;
                for (ClassificatorValue classificator : transmittalModes.values()) {
                    maxOrder = Math.max(classificator.getOrder(), maxOrder);
                }
                final ClassificatorValue classificatorValue = new ClassificatorValue();
                classificatorValue.setActive(true);
                classificatorValue.setOrder(maxOrder + 1);
                classificatorValue.setValueName(transmittalMode.trim());
                classificatorService.addClassificatorValue(transmittalModeClassificator, classificatorValue);
            }
        }
    }

    public void setAttachmentFilesLocationBase(String attachmentFilesLocationBase) {
        attachmentFilesLocationBase = attachmentFilesLocationBase.replace('\\', '/');
        if (!attachmentFilesLocationBase.endsWith("/")) {
            attachmentFilesLocationBase = attachmentFilesLocationBase + "/";
        }
        this.attachmentFilesLocationBase = attachmentFilesLocationBase;
    }

    private FunctionsService getFunctionsService() {
        if (_functionsService == null) {
            _functionsService = (FunctionsService) beanFactory.getBean(FunctionsService.BEAN_NAME);
        }
        return _functionsService;
    }

    public void setClassificatorService(ClassificatorService classificatorService) {
        this.classificatorService = classificatorService;
    }

}
