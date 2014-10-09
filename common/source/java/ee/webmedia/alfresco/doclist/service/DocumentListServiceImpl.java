package ee.webmedia.alfresco.doclist.service;

import static ee.webmedia.alfresco.utils.MessageUtil.getMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.FastDateFormat;

import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.cases.service.UnmodifiableCase;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.UnmodifiableFunction;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.register.service.RegisterService;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.model.UnmodifiableSeries;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.UnmodifiableVolume;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Service to get consolidated list of documents and hierarchy.
 */
public class DocumentListServiceImpl implements DocumentListService {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(DocumentListServiceImpl.class);

    private FunctionsService functionsService;
    private SeriesService seriesService;
    private VolumeService volumeService;
    private CaseService caseService;
    private DocumentDynamicService documentDynamicService;
    private DocumentService documentService;
    private NodeService nodeService;
    private GeneralService generalService;
    private RegisterService registerService;

    private final FastDateFormat fastDateFormat = FastDateFormat.getInstance("dd.MM.yyyy");

    private ContentWriter generateCsvFile(final NodeRef parentNodeRef, ContentWriter writer) {

        OutputStream out = null;
        CsvWriter csvWriter = null;
        writer.setMimetype("application/csv");
        writer.setEncoding("UTF-8");
        try {
            out = writer.getContentOutputStream();
            // the Unicode value for UTF-8 BOM, is needed so that Excel would recognize the file in correct encoding
            out.write("\ufeff".getBytes("UTF-8"));
            csvWriter = new CsvWriter(out, ';', Charset.forName("UTF-8"));
            csvWriter.writeRecord(new String[] { getMessage("doclist_type"), getMessage("doclist_mark"), getMessage("doclist_title"), getMessage("doclist_doc_count"),
                    getMessage("doclist_retention_period"), getMessage("doclist_access_restriction"), getMessage("doclist_status") });
            printFunctions(csvWriter, functionsService.getFunctions(parentNodeRef));
            return writer;
        } catch (IOException e) {
            final String msg = "Outputstream exception while exporting consolidated docList row to CSV-stream";
            throw new RuntimeException(msg, e);
        } finally {
            csvWriter.close();
            IOUtils.closeQuietly(out);
        }
    }

    @Override
    public Map<QName, Serializable> exportCsv(final NodeRef parentNodeRef, final NodeRef reportsSpaceRef) {

        final Map<QName, Serializable> fileProps = new HashMap<QName, Serializable>(2);
        RetryingTransactionHelper helper = BeanHelper.getTransactionService().getRetryingTransactionHelper();
        final NodeRef fileRef = helper.doInTransaction(new RetryingTransactionCallback<NodeRef>() {

            @Override
            public NodeRef execute() throws Throwable {
                ContentWriter writer = generateCsvFile(parentNodeRef, BeanHelper.getContentService().getWriter(null, null, false));
                NodeRef fileRef = BeanHelper.getFileFolderService().create(reportsSpaceRef, "result.csv", ContentModel.TYPE_CONTENT).getNodeRef();
                fileProps.put(ContentModel.PROP_CONTENT, writer.getContentData());
                fileProps.put(ReportModel.Props.REPORT_TYPE, TemplateReportType.CONSOLIDATED_LIST.name());
                nodeService.addProperties(fileRef, fileProps);
                return fileRef;
            }
        }, false, true);

        return nodeService.getProperties(fileRef);
    }

    private void printFunctions(CsvWriter csvWriter, List<UnmodifiableFunction> functions) {
        Map<QName, Pair<Long, QName>> propertyTypes = new HashMap<QName, Pair<Long, QName>>();
        for (UnmodifiableFunction function : functions) {
            List<UnmodifiableSeries> series = seriesService.getAllSeriesByFunction(function.getNodeRef());
            Map<NodeRef, Integer> seriesDocCount = new HashMap<NodeRef, Integer>();
            printLine(csvWriter, function.getType(), function.getMark(), function.getTitle(), getContainingDocsCount(series, seriesDocCount, propertyTypes), "", "",
                    function.getStatus());
            printSeries(csvWriter, series, seriesDocCount, propertyTypes);
        }
    }

    private void printSeries(CsvWriter csvWriter, List<UnmodifiableSeries> series, Map<NodeRef, Integer> seriesDocCounts, Map<QName, Pair<Long, QName>> propertyTypes) {
        for (UnmodifiableSeries serie : series) {
            NodeRef seriesRef = serie.getSeriesRef();
            printLine(csvWriter, serie.getType(), serie.getSeriesIdentifier(), serie.getTitle(), seriesDocCounts.get(seriesRef), "",
                    (String) nodeService.getProperty(seriesRef, SeriesModel.Props.ACCESS_RESTRICTION, propertyTypes),
                    serie.getStatus());
            printVolumes(csvWriter, seriesRef);
        }
    }

    private long getContainingDocsCount(List<UnmodifiableSeries> series, Map<NodeRef, Integer> seriesDocCounts, Map<QName, Pair<Long, QName>> propertyTypes) {
        long count = 0;
        for (UnmodifiableSeries serie : series) {
            int seriesDocCount = serie.getContainingDocsCount();
            count += seriesDocCount;
            seriesDocCounts.put(serie.getSeriesRef(), seriesDocCount);
        }
        return count;
    }

    private void printVolumes(CsvWriter csvWriter, NodeRef nodeRef) {
        List<UnmodifiableVolume> volumes = volumeService.getAllVolumesBySeries(nodeRef);
        String label = MessageUtil.getMessage("volume");
        for (UnmodifiableVolume volume : volumes) {
            NodeRef volumeRef = volume.getNodeRef();
            Date dispositionDate = volume.getRetainUntilDate();
            printLine(csvWriter, label, volume.getVolumeMark(), volume.getTitle(),
                    volume.getContainingDocsCount(), (dispositionDate == null ? "" : fastDateFormat.format(dispositionDate)), "", volume.getStatus());

            // If volume contains cases and documents, then documents must be printed first
            if (volume.isContainsCases()) {
                printCases(csvWriter, volumeRef);
            }
        }
    }

    private void printCases(CsvWriter csvWriter, NodeRef nodeRef) {
        List<UnmodifiableCase> ccases = caseService.getAllCasesByVolume(nodeRef);
        String label = MessageUtil.getMessage("case");
        for (UnmodifiableCase ccase : ccases) {
            printLine(csvWriter, label, "", ccase.getTitle(), ccase.getContainingDocsCount(),
                    "", "", "");
        }
    }

    private void printLine(CsvWriter csvWriter, String type, String mark, String title, long documentNumber, String retentionDate, String accessRestriction, String status) {
        try {
            csvWriter.writeRecord(new String[] {
                    type, mark == null ? "" : mark, title,
                            (documentNumber < 0 ? "" : documentNumber).toString(),
                            retentionDate, accessRestriction, status
            });
        } catch (IOException e) {
            final String msg = "Failed to export consolidated docList row to CSV-stream";
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    public long createNewYearBasedVolumes() {
        log.info("creating copy of year-based volumes that are opened.");
        long counter = 0;
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (UnmodifiableFunction function : functionsService.getAllFunctions()) {
            for (UnmodifiableSeries series : seriesService.getAllSeriesByFunction(function.getNodeRef())) {
                for (UnmodifiableVolume unmodifiableVolume : volumeService.getAllValidVolumesBySeries(series.getSeriesRef())) {
                    if (DocListUnitStatus.OPEN.equals(unmodifiableVolume.getStatus()) && VolumeType.ANNUAL_FILE.name().equals(unmodifiableVolume.getVolumeType())) {
                        Volume volume = volumeService.getVolumeByNodeRef(unmodifiableVolume.getNodeRef(), propertyTypes);
                        volumeService.copyVolume(volume);
                        counter++;
                        log.info("created copy of " + counter + " volume: [" + function.getMark() + "]" + function.getTitle()
                                + "/[" + series.getSeriesIdentifier() + "]" + series.getTitle() + "/[" + unmodifiableVolume.getVolumeMark() + "]" + unmodifiableVolume.getTitle());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("skipping volume: [" + function.getMark() + "]" + function.getTitle()
                                    + "/[" + series.getSeriesIdentifier() + "]" + series.getTitle() + "/[" + unmodifiableVolume.getVolumeMark() + "]"
                                    + unmodifiableVolume.getTitle());
                        }
                    }
                }
            }
        }
        registerService.resetAllAutoResetCounters();
        log.info("created copy of " + counter + " year-based volumes that were opened.");
        return counter;
    }

    @Override
    public long closeAllOpenExpiredVolumes() {
        log.info("Closing all expired volumes that are opened");
        long counter = 0;
        Map<Long, QName> propertyTypes = new HashMap<Long, QName>();
        for (UnmodifiableFunction function : functionsService.getAllFunctions()) {
            for (UnmodifiableSeries series : seriesService.getAllSeriesByFunction(function.getNodeRef())) {
                for (UnmodifiableVolume volume : volumeService.getAllOpenExpiredVolumesBySeries(series.getSeriesRef())) {
                    volumeService.closeVolume(volume.getNodeRef(), propertyTypes);
                    counter++;
                    log.info("Closed volume: [" + function.getMark() + "]" + function.getTitle() + "/[" + series.getSeriesIdentifier() + "]"
                            + series.getTitle() + "/[" + volume.getVolumeMark() + "]" + volume.getTitle() + ", validTo=" + volume.getValidTo());
                }
            }
        }
        log.info("Closed " + counter + " expired volumes that were opened");
        return counter;
    }

    @Override
    public long updateDocCounters() {
        long result = updateDocCounters(functionsService.getFunctionsRoot());
        LinkedHashSet<ArchivalsStoreVO> archivalsStoreVOs = generalService.getArchivalsStoreVOs();
        for (ArchivalsStoreVO archivalsStoreVO : archivalsStoreVOs) {
            result += updateDocCounters(archivalsStoreVO.getNodeRef());
        }
        return result;
    }

    @Override
    public long updateDocCounters(NodeRef functionsRoot) {
        long docCountInRepo = 0;
        log.info("Starting to update documents count in documentList");
        for (ChildAssociationRef functionAssocRef : functionsService.getFunctionAssocs(functionsRoot)) {
            long docCountInFunction = 0;
            final NodeRef functionRef = functionAssocRef.getChildRef();
            for (NodeRef seriesRef : seriesService.getAllSeriesRefsByFunction(functionRef)) {
                long docCountInSeries = 0;
                for (UnmodifiableVolume volume : volumeService.getAllVolumesBySeries(seriesRef)) {
                    long docCountInVolume = 0;
                    final NodeRef volumeRef = volume.getNodeRef();
                    if (volume.isContainsCases()) {
                        for (NodeRef caseRef : caseService.getCaseRefsByVolume(volumeRef)) {
                            final List<NodeRef> allDocumentsByCase = documentService.getAllDocumentRefsByParentRefWithoutRestrictedAccess(caseRef);
                            final int documentsCountByCase = allDocumentsByCase.size();
                            nodeService.setProperty(caseRef, CaseModel.Props.CONTAINING_DOCS_COUNT, documentsCountByCase);
                            caseService.removeFromCache(caseRef);
                            docCountInVolume += documentsCountByCase;
                        }
                    }
                    // Also include documents
                    final List<NodeRef> allDocumentsByVolume = documentService.getAllDocumentRefsByParentRefWithoutRestrictedAccess(volumeRef);
                    final int documentsCountByVolume = allDocumentsByVolume.size();
                    docCountInVolume += documentsCountByVolume;

                    nodeService.setProperty(volumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT, docCountInVolume);
                    volumeService.removeFromCache(volumeRef);
                    docCountInSeries += docCountInVolume;
                }
                nodeService.setProperty(seriesRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, docCountInSeries);
                seriesService.removeFromCache(seriesRef);
                docCountInFunction += docCountInSeries;
            }
            docCountInRepo += docCountInFunction;
        }
        log.info("Updated documents count in documentList. Found " + docCountInRepo + " documents.");
        return docCountInRepo;
    }

    @Override
    public Pair<List<NodeRef>, List<NodeRef>> getAllDocumentAndStructureRefs(NodeRef functionsRoot) {
        final List<NodeRef> docRefs = new ArrayList<NodeRef>(4000);
        final List<NodeRef> functionRefs = new ArrayList<NodeRef>();
        final List<NodeRef> seriesRefs = new ArrayList<NodeRef>();
        final List<NodeRef> volumeRefs = new ArrayList<NodeRef>();
        final List<NodeRef> caseRefs = new ArrayList<NodeRef>();
        for (ChildAssociationRef function : functionsService.getFunctionAssocs(functionsRoot)) {
            final NodeRef functionRef = function.getChildRef();
            for (NodeRef seriesRef : seriesService.getAllSeriesRefsByFunction(functionRef)) {
                for (ChildAssociationRef volume : volumeService.getAllVolumeRefsBySeries(seriesRef)) {
                    final NodeRef volumeRef = volume.getChildRef();
                    Boolean containsCases = (Boolean) nodeService.getProperty(volumeRef, VolumeModel.Props.CONTAINS_CASES);
                    if (containsCases != null && containsCases) {
                        for (NodeRef caseRef : caseService.getCaseRefsByVolume(volumeRef)) {
                            docRefs.addAll(documentService.getAllDocumentRefsByParentRefWithoutRestrictedAccess(caseRef));
                            caseRefs.add(caseRef);
                        }
                    }
                    docRefs.addAll(documentService.getAllDocumentRefsByParentRefWithoutRestrictedAccess(volumeRef));
                    volumeRefs.add(volumeRef);
                }
                seriesRefs.add(seriesRef);
            }
            functionRefs.add(functionRef);
        }
        final List<NodeRef> structRefs = new ArrayList<NodeRef>(4000);
        structRefs.addAll(caseRefs);
        structRefs.addAll(volumeRefs);
        structRefs.addAll(seriesRefs);
        structRefs.addAll(functionRefs);
        return new Pair<List<NodeRef>, List<NodeRef>>(docRefs, structRefs);
    }

    @Override
    public String getDisplayPath(NodeRef nodeRef, boolean showLeaf) {
        StringBuilder buf = new StringBuilder(64);
        Path path = nodeService.getPath(nodeRef);

        int count = path.size() - (showLeaf ? 0 : 1);
        boolean checkfirst = true;
        for (int i = 0; i < count; i++) {
            String elementString = null;
            Path.Element element = path.get(i);
            if (element instanceof Path.ChildAssocElement) {
                ChildAssociationRef elementRef = ((Path.ChildAssocElement) element).getRef();
                if (elementRef.getChildRef() != null && elementRef.getQName() != null) {
                    if (checkfirst) {
                        LinkedHashSet<ArchivalsStoreVO> storeVOs = generalService.getArchivalsStoreVOs();
                        for (ArchivalsStoreVO storeVO : storeVOs) {
                            if (storeVO.getNodeRef().equals(elementRef.getChildRef())) {
                                elementString = storeVO.getTitle();
                                break;
                            }
                        }
                        checkfirst = false;
                    }
                    if (elementString == null) {
                        elementString = DocumentLocationGenerator.getDocumentListUnitLabel(elementRef.getChildRef());
                        if (elementString == null) {
                            if (DocumentCommonModel.Types.DOCUMENT.equals(elementRef.getQName())) {
                                elementString = (String) nodeService.getProperties(elementRef.getChildRef()).get(DocumentCommonModel.Props.DOC_NAME);
                            } else {
                                elementString = MessageUtil.getMessage("trashcan_" + QName.createQName(element.getElementString()).getLocalName());
                                if (elementString != null && elementString.startsWith("$$")) {
                                    elementString = (String) nodeService.getProperties(elementRef.getChildRef()).get(ContentModel.PROP_NAME);
                                }
                            }
                        }
                    }
                }
            } else {
                elementString = element.getElementString();
            }

            if (elementString != null) {
                buf.append("/");
                buf.append(elementString);
            }
        }

        return buf.toString();
    }

    // BEGIN GETTERS

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setVolumeService(VolumeService volumeService) {
        this.volumeService = volumeService;
    }

    public void setCaseService(CaseService caseService) {
        this.caseService = caseService;
    }

    public void setDocumentDynamicService(DocumentDynamicService documentDynamicService) {
        this.documentDynamicService = documentDynamicService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setRegisterService(RegisterService registerService) {
        this.registerService = registerService;
    }

    // END GETTERS_SETTERS

}
