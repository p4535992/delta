<<<<<<< HEAD
package ee.webmedia.alfresco.doclist.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang.time.FastDateFormat;

import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.register.service.RegisterService;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.Volume;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.volume.service.VolumeService;

/**
 * Service to get consolidated list of documents and hierarchy.
 * 
 * @author Priit Pikk
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

    @Override
    public void getExportCsv(OutputStream outputStream, NodeRef rootRef) {
        CsvWriter csvWriter = new CsvWriter(outputStream, ';', Charset.forName("UTF-8"));
        try {
            // the Unicode value for UTF-8 BOM, is needed so that Excel would recognize the file in correct encoding
            outputStream.write("\ufeff".getBytes("UTF-8"));
            printFunctions(csvWriter, functionsService.getFunctions(rootRef));
        } catch (IOException e) {
            final String msg = "Outputstream exception while exporting consolidated docList row to CSV-stream";
            throw new RuntimeException(msg, e);
        } finally {
            csvWriter.close();
        }
    }

    private void printFunctions(CsvWriter csvWriter, List<Function> functions) {
        for (Function function : functions) {
            List<Series> series = seriesService.getAllSeriesByFunction(function.getNode().getNodeRef());
            printLine(csvWriter, function.getType(), function.getMark(), function.getTitle(), getContainingDocsCount(series), "", "",
                    function.getStatus());
            printSeries(csvWriter, series);
        }
    }

    private void printSeries(CsvWriter csvWriter, List<Series> series) {
        for (Series serie : series) {
            Integer retensionPeriod = serie.getRetentionPeriod();
            printLine(csvWriter, serie.getType(), serie.getSeriesIdentifier(), serie.getTitle(), serie.getContainingDocsCount(),
                    (retensionPeriod == null ? "" : retensionPeriod.toString()),
                    nodeService.getProperty(serie.getNode().getNodeRef(), SeriesModel.Props.ACCESS_RESTRICTION).toString(),
                    serie.getStatus());
            printVolumes(csvWriter, serie.getNode().getNodeRef());
        }
    }

    private long getContainingDocsCount(List<Series> series) {
        long count = 0;
        for (Series serie : series) {
            count += serie.getContainingDocsCount();
        }
        return count;
    }

    private void printVolumes(CsvWriter csvWriter, NodeRef nodeRef) {
        List<Volume> volumes = volumeService.getAllVolumesBySeries(nodeRef);
        String label = MessageUtil.getMessage("volume");
        for (Volume volume : volumes) {
            Date dispositionDate = volume.getRetainUntilDate();
            printLine(csvWriter, label, volume.getVolumeMark(), volume.getTitle(),
                    volume.getContainingDocsCount(), (dispositionDate == null ? "" : fastDateFormat.format(dispositionDate)), "", volume.getStatus());

            // If volume contains cases and documents, then documents must be printed first
            printDocuments(csvWriter, volume.getNode().getNodeRef());
            if (volume.isContainsCases()) {
                printCases(csvWriter, volume.getNode().getNodeRef());
            }
        }
    }

    private void printCases(CsvWriter csvWriter, NodeRef nodeRef) {
        List<Case> ccases = caseService.getAllCasesByVolume(nodeRef);
        String label = MessageUtil.getMessage("case");
        for (Case ccase : ccases) {
            printLine(csvWriter, label, "", ccase.getTitle(), ccase.getContainingDocsCount(),
                    "", "", "");
            printDocuments(csvWriter, ccase.getNode().getNodeRef());
        }
    }

    private void printDocuments(CsvWriter csvWriter, NodeRef nodeRef) {
        List<NodeRef> allDocumentsByCase = documentService.getAllDocumentRefsByParentRef(nodeRef);
        String label = MessageUtil.getMessage("document");
        for (NodeRef docuRef : allDocumentsByCase) {
            DocumentDynamic document = documentDynamicService.getDocument(docuRef);
            printLine(csvWriter, label, document.getRegNumber(), document.getDocName(), -1, "",
                    (String) document.getNode().getProperties().get(DocumentCommonModel.Props.ACCESS_RESTRICTION),
                    document.getDocStatus());
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
        for (Function function : functionsService.getAllFunctions()) {
            for (Series series : seriesService.getAllSeriesByFunction(function.getNodeRef())) {
                for (Volume volume : volumeService.getAllValidVolumesBySeries(series.getNode().getNodeRef())) {
                    if (DocListUnitStatus.OPEN.equals(volume.getStatus()) && VolumeType.ANNUAL_FILE.equals(volume.getVolumeTypeEnum())) {
                        volumeService.copyVolume(volume);
                        counter++;
                        log.info("created copy of " + counter + " volume: [" + function.getMark() + "]" + function.getTitle()
                                + "/[" + series.getSeriesIdentifier() + "]" + series.getTitle() + "/[" + volume.getVolumeMark() + "]" + volume.getTitle());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("skipping volume: [" + function.getMark() + "]" + function.getTitle()
                                    + "/[" + series.getSeriesIdentifier() + "]" + series.getTitle() + "/[" + volume.getVolumeMark() + "]" + volume.getTitle());
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
        for (Function function : functionsService.getAllFunctions()) {
            for (Series series : seriesService.getAllSeriesByFunction(function.getNodeRef())) {
                for (Volume volume : volumeService.getAllOpenExpiredVolumesBySeries(series.getNode().getNodeRef())) {
                    volumeService.closeVolume(volume.getNodeRef());
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
        for (Function function : functionsService.getFunctions(functionsRoot)) {
            long docCountInFunction = 0;
            final NodeRef functionRef = function.getNodeRef();
            for (NodeRef seriesRef : seriesService.getAllSeriesRefsByFunction(functionRef)) {
                long docCountInSeries = 0;
                for (Volume volume : volumeService.getAllVolumesBySeries(seriesRef)) {
                    long docCountInVolume = 0;
                    final NodeRef volumeRef = volume.getNode().getNodeRef();
                    if (volume.isContainsCases()) {
                        for (Case aCase : caseService.getAllCasesByVolume(volumeRef)) {
                            final NodeRef caseRef = aCase.getNode().getNodeRef();
                            final List<NodeRef> allDocumentsByCase = documentService.getAllDocumentRefsByParentRefWithoutRestrictedAccess(caseRef);
                            final int documentsCountByCase = allDocumentsByCase.size();
                            nodeService.setProperty(caseRef, CaseModel.Props.CONTAINING_DOCS_COUNT, documentsCountByCase);
                            docCountInVolume += documentsCountByCase;
                        }
                    }
                    // Also include documents
                    final List<NodeRef> allDocumentsByVolume = documentService.getAllDocumentRefsByParentRefWithoutRestrictedAccess(volumeRef);
                    final int documentsCountByVolume = allDocumentsByVolume.size();
                    docCountInVolume += documentsCountByVolume;

                    nodeService.setProperty(volumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT, docCountInVolume);
                    docCountInSeries += docCountInVolume;
                }
                nodeService.setProperty(seriesRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, docCountInSeries);
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
=======
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
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.Pair;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.FastDateFormat;

import com.csvreader.CsvWriter;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.cases.model.Case;
import ee.webmedia.alfresco.cases.model.CaseModel;
import ee.webmedia.alfresco.cases.service.CaseService;
import ee.webmedia.alfresco.classificator.enums.DocListUnitStatus;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.service.DocumentDynamicService;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService;
import ee.webmedia.alfresco.functions.model.Function;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.series.model.Series;
import ee.webmedia.alfresco.series.model.SeriesModel;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.utils.MessageUtil;
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

    private void printFunctions(CsvWriter csvWriter, List<Function> functions) {
        for (Function function : functions) {
            List<Series> series = seriesService.getAllSeriesByFunction(function.getNode().getNodeRef());
            printLine(csvWriter, function.getType(), function.getMark(), function.getTitle(), getContainingDocsCount(series), "", "",
                    function.getStatus());
            printSeries(csvWriter, series);
        }
    }

    private void printSeries(CsvWriter csvWriter, List<Series> series) {
        for (Series serie : series) {
            Integer retensionPeriod = serie.getRetentionPeriod();
            printLine(csvWriter, serie.getType(), serie.getSeriesIdentifier(), serie.getTitle(), serie.getContainingDocsCount(),
                    (retensionPeriod == null ? "" : retensionPeriod.toString()),
                    (String) nodeService.getProperty(serie.getNode().getNodeRef(), SeriesModel.Props.ACCESS_RESTRICTION),
                    serie.getStatus());
            printVolumes(csvWriter, serie.getNode().getNodeRef());
        }
    }

    private long getContainingDocsCount(List<Series> series) {
        long count = 0;
        for (Series serie : series) {
            count += serie.getContainingDocsCount();
        }
        return count;
    }

    private void printVolumes(CsvWriter csvWriter, NodeRef nodeRef) {
        List<Volume> volumes = volumeService.getAllVolumesBySeries(nodeRef);
        String label = MessageUtil.getMessage("volume");
        for (Volume volume : volumes) {
            Date dispositionDate = volume.getDispositionDate();
            printLine(csvWriter, label, volume.getVolumeMark(), volume.getTitle(),
                    volume.getContainingDocsCount(), (dispositionDate == null ? "" : fastDateFormat.format(dispositionDate)), "", volume.getStatus());
            if (volume.isContainsCases()) {
                printCases(csvWriter, volume.getNode().getNodeRef());
            }
        }
    }

    private void printCases(CsvWriter csvWriter, NodeRef nodeRef) {
        List<Case> ccases = caseService.getAllCasesByVolume(nodeRef);
        String label = MessageUtil.getMessage("case");
        for (Case ccase : ccases) {
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
        for (Function function : functionsService.getAllFunctions()) {
            for (Series series : seriesService.getAllSeriesByFunction(function.getNodeRef())) {
                for (Volume volume : volumeService.getAllValidVolumesBySeries(series.getNode().getNodeRef())) {
                    if (DocListUnitStatus.OPEN.equals(volume.getStatus()) && VolumeType.ANNUAL_FILE.equals(volume.getVolumeTypeEnum())) {
                        volumeService.copyVolume(volume);
                        counter++;
                        log.info("created copy of " + counter + " volume: [" + function.getMark() + "]" + function.getTitle()
                                + "/[" + series.getSeriesIdentifier() + "]" + series.getTitle() + "/[" + volume.getVolumeMark() + "]" + volume.getTitle());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("skipping volume: [" + function.getMark() + "]" + function.getTitle()
                                    + "/[" + series.getSeriesIdentifier() + "]" + series.getTitle() + "/[" + volume.getVolumeMark() + "]" + volume.getTitle());
                        }
                    }
                }
            }
        }
        log.info("created copy of " + counter + " year-based volumes that were opened.");
        return counter;
    }

    @Override
    public long closeAllOpenExpiredVolumes() {
        log.info("Closing all expired volumes that are opened");
        long counter = 0;
        for (Function function : functionsService.getAllFunctions()) {
            for (Series series : seriesService.getAllSeriesByFunction(function.getNodeRef())) {
                for (Volume volume : volumeService.getAllOpenExpiredVolumesBySeries(series.getNode().getNodeRef())) {
                    Pair<String, Object[]> error = volumeService.closeVolume(volume);
                    String volumeInfo = "[" + function.getMark() + "]" + function.getTitle() + "/[" + series.getSeriesIdentifier() + "]"
                            + series.getTitle() + "/[" + volume.getVolumeMark() + "]" + volume.getTitle() + ", validTo=" + volume.getValidTo();
                    if (error == null) {
                        counter++;
                        log.info("Closed volume:" + volumeInfo);
                    } else {
                        log.error("Close volume failed:" + MessageUtil.getMessage(error.getFirst(), error.getSecond()) + "\nvolume info: " + volumeInfo);
                    }
                }
            }
        }
        log.info("Closed " + counter + " expired volumes that were opened");
        return counter;
    }

    @Override
    public long updateDocCounters() {
        return updateDocCounters(functionsService.getFunctionsRoot());
    }

    @Override
    public long updateDocCounters(NodeRef functionsRoot) {
        long docCountInRepo = 0;
        log.info("Starting to update documents count in documentList");
        for (Function function : functionsService.getFunctions(functionsRoot)) {
            long docCountInFunction = 0;
            final NodeRef functionRef = function.getNodeRef();
            for (NodeRef seriesRef : seriesService.getAllSeriesRefsByFunction(functionRef)) {
                long docCountInSeries = 0;
                for (Volume volume : volumeService.getAllVolumesBySeries(seriesRef)) {
                    long docCountInVolume = 0;
                    final NodeRef volumeRef = volume.getNode().getNodeRef();
                    if (volume.isContainsCases()) {
                        for (Case aCase : caseService.getAllCasesByVolume(volumeRef)) {
                            final NodeRef caseRef = aCase.getNode().getNodeRef();
                            final List<NodeRef> allDocumentsByCase = documentService.getAllDocumentRefsByParentRef(caseRef);
                            final int documentsCountByCase = allDocumentsByCase.size();
                            nodeService.setProperty(caseRef, CaseModel.Props.CONTAINING_DOCS_COUNT, documentsCountByCase);
                            docCountInVolume += documentsCountByCase;
                        }
                    } else {
                        final List<NodeRef> allDocumentsByVolume = documentService.getAllDocumentRefsByParentRef(volumeRef);
                        final int documentsCountByVolume = allDocumentsByVolume.size();
                        docCountInVolume += documentsCountByVolume;
                    }
                    nodeService.setProperty(volumeRef, VolumeModel.Props.CONTAINING_DOCS_COUNT, docCountInVolume);
                    docCountInSeries += docCountInVolume;
                }
                nodeService.setProperty(seriesRef, SeriesModel.Props.CONTAINING_DOCS_COUNT, docCountInSeries);
                docCountInFunction += docCountInSeries;
            }
            docCountInRepo += docCountInFunction;
        }
        log.info("Updated documents count in documentList. Found " + docCountInRepo + " documents.");
        return docCountInRepo;
    }

    @Override
    public Pair<List<NodeRef>, Long> getAllDocumentAndCaseRefs() {
        long deletedDocCount = 0;
        log.info("Starting to gather all noderefs of documents and cases to be removed");
        final ArrayList<NodeRef> c = new ArrayList<NodeRef>(4000);
        for (ChildAssociationRef function : functionsService.getFunctionAssocs(functionsService.getFunctionsRoot())) {
            long docCountInFunction = 0;
            final NodeRef functionRef = function.getChildRef();
            for (NodeRef seriesRef : seriesService.getAllSeriesRefsByFunction(functionRef)) {
                long docCountInSeries = 0;
                for (ChildAssociationRef volume : volumeService.getAllVolumeRefsBySeries(seriesRef)) {
                    long docCountInVolume = 0;
                    final NodeRef volumeRef = volume.getChildRef();
                    Boolean containsCases = (Boolean) nodeService.getProperty(volumeRef, VolumeModel.Props.CONTAINS_CASES);
                    if (containsCases != null && containsCases) {
                        for (ChildAssociationRef aCase : caseService.getCaseRefsByVolume(volumeRef)) {
                            final NodeRef caseRef = aCase.getChildRef();
                            Integer docsUnderCase = (Integer) nodeService.getProperty(caseRef, CaseModel.Props.CONTAINING_DOCS_COUNT);
                            c.add(caseRef);
                            docCountInVolume += docsUnderCase != null ? docsUnderCase : 0;
                        }
                    } else {
                        List<ChildAssociationRef> docsOfVolumeAssocs = nodeService.getChildAssocs(volumeRef, RegexQNamePattern.MATCH_ALL,
                                RegexQNamePattern.MATCH_ALL);
                        final int documentsCountByVolume = docsOfVolumeAssocs.size();
                        for (ChildAssociationRef childAssociationRef : docsOfVolumeAssocs) {
                            c.add(childAssociationRef.getChildRef());
                        }
                        docCountInVolume += documentsCountByVolume;
                    }
                    docCountInSeries += docCountInVolume;
                }
                docCountInFunction += docCountInSeries;
            }
            deletedDocCount += docCountInFunction;
        }
        log.info("found " + deletedDocCount + " documents from documentList to delete.");
        return new Pair<List<NodeRef>, Long>(c, deletedDocCount);
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

    // END GETTERS_SETTERS

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
