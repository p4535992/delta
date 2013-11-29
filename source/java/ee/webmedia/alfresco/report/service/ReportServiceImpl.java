package ee.webmedia.alfresco.report.service;

import static ee.webmedia.alfresco.report.service.ExcelUtil.setCellValueTruncateIfNeeded;
import static ee.webmedia.alfresco.report.service.ReportHelper.getReportHeaderMsgKeys;
import static ee.webmedia.alfresco.utils.TextUtil.formatDateOrEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.web.bean.repository.MapNode;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.archivals.model.ArchivalsStoreVO;
import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.casefile.service.CaseFileService;
import ee.webmedia.alfresco.classificator.enums.TemplateReportOutputType;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.classificator.enums.VolumeType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.docadmin.service.FieldDefinition;
import ee.webmedia.alfresco.docconfig.generator.systematic.DocumentLocationGenerator;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.functions.service.FunctionsService;
import ee.webmedia.alfresco.report.model.ReportDataCollector;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.report.model.ReportStatus;
import ee.webmedia.alfresco.series.service.SeriesService;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.TextUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.LinkedReviewTask;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowService;
import ee.webmedia.alfresco.workflow.service.type.WorkflowType;

/**
 * @author Riina Tens
 */
public class ReportServiceImpl implements ReportService {
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ReportServiceImpl.class);
    private static final int EXCEL_SHEET_MAX_ROWS = 1048576;
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy");
    private static int STATUS_CHECK_INTERVAL = 30000; // check after half minute

    private DocumentSearchService documentSearchService;
    private DocumentTemplateService documentTemplateService;
    private FileFolderService fileFolderService;
    private FunctionsService functionsService;
    private SeriesService seriesService;
    private NodeService nodeService;
    private GeneralService generalService;
    private UserService userService;
    private WorkflowService workflowService;
    private DocumentAdminService documentAdminService;
    private MimetypeService mimetypeService;
    private FileService fileService;
    private CaseFileService caseFileService;
    private TransactionService transactionService;
    private SendOutService sendOutService;
    /**
     * NB! Kui rakendus jookseb klastris, siis praegu eeldatakse, et aruannete genereerimine jookseb ainult ühes klastri õlas
     * ja ka genereerimise peatamine on võimalik ainult selles õlas.
     */
    private boolean reportGenerationEnabled;
    private boolean reportGenerationPaused;

    private boolean usableByAdminDocManagerOnly;

    @Override
    public NodeRef createReportResult(Node filter, TemplateReportType reportType, QName parentToChildAssoc) {
        Assert.isTrue(reportType != null && parentToChildAssoc != null, "reportType and parentToChildAssoc cannot be null.");
        Map<QName, Serializable> reportResultProps = new HashMap<QName, Serializable>();
        reportResultProps.put(ReportModel.Props.USERNAME, userService.getCurrentUserName());
        Map<String, Object> filterProps = filter.getProperties();
        String filterName = (String) filterProps.get(ReportHelper.getFilterNameProp(reportType).toString());
        String templateName = (String) filterProps.get(ReportHelper.getTemplateNameProp(reportType).toString());
        String reportName = StringUtils.isNotBlank(filterName) ? filterName : FilenameUtils.removeExtension(templateName) + "_result";
        reportResultProps.put(ReportModel.Props.REPORT_NAME, reportName);
        reportResultProps.put(ReportModel.Props.REPORT_TYPE, reportType.toString());
        reportResultProps.put(ReportModel.Props.USER_START_DATE_TIME, new Date());
        reportResultProps.put(ReportModel.Props.STATUS, ReportStatus.IN_QUEUE.toString());
        ReportHelper.setReportResultOutputType(reportType, filterProps, reportResultProps);

        NodeRef reportResultRef = nodeService.createNode(getReportsSpaceRef(), ReportModel.Assocs.REPORT_RESULT,
                ReportModel.Assocs.REPORT_RESULT, ReportModel.Types.REPORT_RESULT, reportResultProps).getChildRef();
        Map<QName, Serializable> props = generalService.getPropertiesIgnoringSystem(filter.getProperties());
        // filter
        nodeService.createNode(reportResultRef, parentToChildAssoc, parentToChildAssoc, filter.getType(), props).getChildRef();
        // template
        NodeRef templateRef = documentTemplateService.getReportTemplateByName(templateName, reportType);
        if (templateRef == null) {
            throw new UnableToPerformException("report_error_cannot_find_template");
        }
        try {
            fileFolderService.copy(templateRef, reportResultRef, null);
        } catch (FileNotFoundException e) {
            throw new UnableToPerformException("report_error_cannot_find_template");
        }
        return reportResultRef;
    }

    @Override
    public NodeRef createCsvReportResult(NodeRef nodeRef) {
        Map<QName, Serializable> reportResultProps = new HashMap<QName, Serializable>();
        String reportName = MessageUtil.getMessage("report_consolidated_list_default_title");
        reportResultProps.put(ReportModel.Props.USERNAME, userService.getCurrentUserName());
        reportResultProps.put(ReportModel.Props.REPORT_NAME, reportName);
        reportResultProps.put(ReportModel.Props.REPORT_TYPE, TemplateReportType.CONSOLIDATED_LIST.toString());
        reportResultProps.put(ReportModel.Props.USER_START_DATE_TIME, new Date());
        reportResultProps.put(ReportModel.Props.STATUS, ReportStatus.IN_QUEUE.toString());
        reportResultProps.put(ReportModel.Props.CSV_FUNCTION_STORE_NODE_REF, nodeRef);

        NodeRef reportResultRef = nodeService.createNode(getReportsSpaceRef(), ReportModel.Assocs.REPORT_RESULT,
                ReportModel.Assocs.REPORT_RESULT, ReportModel.Types.REPORT_RESULT, reportResultProps).getChildRef();
        return reportResultRef;
    }

    @Override
    public ReportDataCollector getReportFileInMemory(ReportDataCollector reportDataCollector) {
        NodeRef reportResultRef = reportDataCollector.getReportResultNodeRef();
        Map<QName, Serializable> reportResultProps = reportDataCollector.getReportResultProps();
        String reportTypeStr = (String) reportResultProps.get(ReportModel.Props.REPORT_TYPE);
        TemplateReportType reportType = TemplateReportType.valueOf(reportTypeStr);
        if (TemplateReportType.CONSOLIDATED_LIST == reportType) {
            NodeRef functionsRoot = (NodeRef) reportResultProps.get(ReportModel.Props.CSV_FUNCTION_STORE_NODE_REF);
            return createCsvFileInMemory(reportDataCollector, functionsRoot);
        }
        List<ChildAssociationRef> filters = nodeService.getChildAssocs(reportResultRef, Collections.singleton(ReportHelper.getFilterAssoc(reportType)));
        Assert.isTrue(filters != null && filters.size() == 1, "reportResult must have exactly one taskReportFilter child node!");
        Node filter = new Node(filters.get(0).getChildRef());

        NodeRef templateRef = getReportResultTemplate(reportResultRef, reportTypeStr);
        if (templateRef == null) {
            throw new UnableToPerformException("report_error_cannot_find_template");
        }
        ContentReader templateReader = fileFolderService.getReader(templateRef);
        Map<String, String> types = documentAdminService.getDocumentTypeNames(null);
        if (TemplateReportType.TASKS_REPORT == reportType) {
            List<NodeRef> taskRefs = documentSearchService.searchTasksForReport(filter);
            Map<QName, String> taskNames = getTaskNames();
            return createReportFileInMemory(taskRefs, templateReader, reportDataCollector,
                    new FillExcelTaskRowCallback(types, documentTemplateService.getCompoundWorkflowServerUrlPrefix(), documentTemplateService.getDocumentServerUrlPrefix(),
                            taskNames));
        } else if (TemplateReportType.VOLUMES_REPORT == reportType) {
            List<NodeRef> volRefs = documentSearchService.searchVolumesForReport(filter);
            return createReportFileInMemory(volRefs, templateReader, reportDataCollector, new FillExcelVolumeRowCallback(types));
        } else if (TemplateReportType.DOCUMENTS_REPORT == reportType) {
            List<NodeRef> documentRefs = new ArrayList<NodeRef>();
            long lastStatusCheckTime = System.currentTimeMillis();
            for (StoreRef storeRef : documentSearchService.getStoresFromDocumentReportFilter(filter.getProperties())) {
                doPauseReportGeneration();
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastStatusCheckTime > STATUS_CHECK_INTERVAL) {
                    if (isReportStopped(reportDataCollector, reportResultRef)) {
                        return reportDataCollector;
                    }
                    lastStatusCheckTime = currentTime;
                }
                documentRefs.addAll(documentSearchService.searchDocumentsForReport(filter, storeRef,
                        (String) reportDataCollector.getReportResultProps().get(ReportModel.Props.USERNAME)));
            }
            TemplateReportOutputType outputType = TemplateReportOutputType.valueOf((String) reportDataCollector.getReportResultProps().get(ReportModel.Props.REPORT_OUTPUT_TYPE));
            FillExcelRowCallback fillDocumentRowCallback = null;
            // Map for heading generation
            Map<String, Boolean> fieldsToShow = new HashMap<String, Boolean>();
            List<String> documentReportNotMandatoryFieldsInOrder = ReportHelper.getDocumentReportNotMandatoryFieldsInOrder();
            int fieldsSize = documentReportNotMandatoryFieldsInOrder.size();
            // Array for row generation
            boolean[] fieldsToShowArray = new boolean[fieldsSize];
            for (int i = 0; i < fieldsSize; i++) {
                String fieldId = documentReportNotMandatoryFieldsInOrder.get(i);
                Boolean used = documentAdminService.isFieldDefintionUsed(fieldId);
                fieldsToShow.put(fieldId, used);
                fieldsToShowArray[i] = used;
            }
            if (TemplateReportOutputType.DOCS_WITH_SUBNODES == outputType) {
                fillDocumentRowCallback = new FillExcelDocumentWithSubnodesRowCallback(types, fieldsToShow, fieldsToShowArray);
            } else {
                fillDocumentRowCallback = new FillExcelDocumentOnlyRowCallback(types, fieldsToShow, fieldsToShowArray);

            }
            return createReportFileInMemory(documentRefs, templateReader, reportDataCollector, fillDocumentRowCallback);
        } else {
            // report of unknown type or creating report file not implemented
            reportDataCollector.setResultStatus(ReportStatus.FAILED);
            return reportDataCollector;
        }
    }

    private ReportDataCollector createCsvFileInMemory(ReportDataCollector reportDataCollector, NodeRef rootRef) {
        Map<QName, Serializable> resultProps = BeanHelper.getDocumentListService().exportCsv(rootRef, reportDataCollector.getReportResultNodeRef());
        reportDataCollector.addReportResultProps(resultProps);
        reportDataCollector.setEncoding("UTF-8");
        reportDataCollector.setResultStatus(ReportStatus.FINISHED);
        return reportDataCollector;
    }

    @Override
    public void doPauseReportGeneration() {
        while (reportGenerationPaused) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
        }
    }

    private NodeRef getReportResultTemplate(NodeRef reportResultRef, String reportTypeStr) {
        List<FileInfo> fileInfos = fileFolderService.listFiles(reportResultRef);
        for (FileInfo fileInfo : fileInfos) {
            NodeRef fileRef = fileInfo.getNodeRef();
            if (nodeService.hasAspect(fileRef, DocumentTemplateModel.Aspects.TEMPLATE_REPORT)
                    && StringUtils.equals(reportTypeStr, (String) nodeService.getProperty(fileRef, DocumentTemplateModel.Prop.REPORT_TYPE))) {
                return fileRef;
            }
        }
        return null;
    }

    private NodeRef getReportsSpaceRef() {
        return generalService.getNodeRef(ReportModel.Repo.REPORTS_SPACE);
    }

    private ReportDataCollector createReportFileInMemory(List<NodeRef> nodeRefs, ContentReader templateReader, ReportDataCollector reportDataCollector,
            FillExcelRowCallback fillRowCallback) {
        if (nodeRefs == null) {
            nodeRefs = new ArrayList<NodeRef>();
        }
        InputStream templateInputStream = null;
        NodeRef reportResultNodeRef = reportDataCollector.getReportResultNodeRef();
        long lastStatusCheckTime = System.currentTimeMillis();
        try {
            templateInputStream = templateReader.getContentInputStream();
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(templateInputStream);
            Workbook wb = new org.apache.poi.xssf.streaming.SXSSFWorkbook(xssfWorkbook, 100);

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                sheet = wb.createSheet();
            } else {
                String sheetName = sheet.getSheetName();
                wb.removeSheetAt(0);
                sheet = wb.createSheet(sheetName);
                wb.setSheetOrder(sheetName, 0);
            }
            Row row = sheet.getRow(0);
            if (row == null) {
                row = sheet.createRow(0);
            }
            fillRowCallback.createHeadings(row);
            int rowNr = 1;
            RowProvider rowProvider = new RowProvider(sheet, rowNr);
            ReportStatus resultStatus = ReportStatus.FINISHED;
            for (NodeRef nodeRef : nodeRefs) {
                doPauseReportGeneration();
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastStatusCheckTime > STATUS_CHECK_INTERVAL) {
                    if (isReportStopped(reportDataCollector, reportResultNodeRef)) {
                        return reportDataCollector;
                    }
                    lastStatusCheckTime = currentTime;
                }
                if (rowNr >= EXCEL_SHEET_MAX_ROWS) {
                    resultStatus = ReportStatus.EXCEL_FULL;
                    break;
                }

                int rowsNeeded = fillRowCallback.initAndCalculateRows(nodeRef);

                rowNr += rowsNeeded;
                if (rowNr - 1 >= EXCEL_SHEET_MAX_ROWS) {
                    // don't start writing if all of document's rows cannot be written
                    resultStatus = ReportStatus.EXCEL_FULL;
                    break;
                }
                fillRowCallback.execute(rowProvider, nodeRef);

            }
            reportDataCollector.setWorkbook(wb);
            reportDataCollector.setEncoding(templateReader.getEncoding());
            reportDataCollector.setResultStatus(resultStatus);
            return reportDataCollector;
        } catch (Exception e) {
            LOG.error("Error generating file in memory", e);
            throw new UnableToPerformException("Failed to write to file", e);
        } finally {
            IOUtils.closeQuietly(templateInputStream);
        }
    }

    private boolean isReportStopped(ReportDataCollector reportDataCollector, NodeRef reportResultNodeRef) {
        ReportStatus repoStatus = ReportStatus.valueOf((String) nodeService.getProperty(reportResultNodeRef, ReportModel.Props.STATUS));
        if (ReportStatus.CANCELLING_REQUESTED == repoStatus) {
            reportDataCollector.setResultStatus(ReportStatus.CANCELLED);
            return true;
        }
        if (ReportStatus.DELETING_REQUESTED == repoStatus) {
            reportDataCollector.setResultStatus(ReportStatus.DELETED);
            return true;
        }
        return false;
    }

    private Map<QName, String> getTaskNames() {
        Map<QName, WorkflowType> workflowTypesByTask = workflowService.getWorkflowTypesByTask();
        Map<QName, String> taskNamesByType = new HashMap<QName, String>();
        for (Map.Entry<QName, WorkflowType> entry : workflowTypesByTask.entrySet()) {
            QName taskType = entry.getKey();
            taskNamesByType.put(taskType, MessageUtil.getTypeName(taskType));
        }
        return taskNamesByType;
    }

    @Override
    public NodeRef completeReportResult(ReportDataCollector reportDataProvider) {
        NodeRef reportResultNodeRef = reportDataProvider.getReportResultNodeRef();
        ReportStatus resultStatus = reportDataProvider.getResultStatus();
        if (ReportStatus.DELETED == resultStatus) {
            deleteReportResult(reportResultNodeRef);
            LOG.info("Deleted report nodeRef=" + reportResultNodeRef);
            return null;
        }
        Map<QName, Serializable> reportResultProps = new HashMap<QName, Serializable>();
        Workbook workbook = reportDataProvider.getWorkbook();
        // workbook may be null if creating report failed or was cancelled by user. Status check is performed in ExecuteReportsJob.
        Date completeDate = new Date();
        if (workbook != null) {
            String reportName = (String) nodeService.getProperty(reportResultNodeRef, ReportModel.Props.REPORT_NAME);
            if (StringUtils.isBlank(reportName)) {
                reportName = "Aruanne";
            }
            // Don't change file extension to xlsx, because Excel shall complain about it (although is able to open the file correctly)
            String fileName = FilenameUtil.buildFileName(reportName, "xltx");
            FileInfo createdFile = fileFolderService.create(reportResultNodeRef, fileName, ContentModel.TYPE_CONTENT);
            ContentWriter writer = fileFolderService.getWriter(createdFile.getNodeRef());
            writer.setMimetype(mimetypeService.guessMimetype(fileName));
            writer.setEncoding(reportDataProvider.getEncoding());
            try {
                workbook.write(writer.getContentOutputStream());
            } catch (IOException e) {
                throw new RuntimeException("Failed to write to result file.", e);
            }
            reportResultProps.put(ReportModel.Props.RUN_FINISH_START_TIME, completeDate);
        }
        else if (TemplateReportType.CONSOLIDATED_LIST.name().equals(reportDataProvider.getReportResultProps().get(ReportModel.Props.REPORT_TYPE))) {
            reportResultProps.put(ReportModel.Props.RUN_FINISH_START_TIME, completeDate);
        }
        reportResultProps.put(ReportModel.Props.STATUS, resultStatus.toString());
        if (ReportStatus.CANCELLED.equals(resultStatus)) {
            reportResultProps.put(ReportModel.Props.CANCEL_DATE_TIME, completeDate);
        }
        nodeService.addProperties(reportResultNodeRef, reportResultProps);
        NodeRef userReportFolderRef = userService.retrieveUserReportsFolderRef((String) reportDataProvider.getReportResultProps().get(ReportModel.Props.USERNAME));
        if (userReportFolderRef != null) {
            nodeService.moveNode(reportResultNodeRef, userReportFolderRef, ReportModel.Assocs.REPORT_RESULT, ReportModel.Assocs.REPORT_RESULT);
        } else {
            LOG.error("Couldn't retrieve reports folder for user " + AuthenticationUtil.getRunAsUser() + ", not moving report.");
        }
        return null;
    }

    private boolean isAfterDate(Date completedDateTime, Date dueDate) {
        if (completedDateTime != null && dueDate != null) {
            return completedDateTime.after(dueDate) && !DateUtils.isSameDay(completedDateTime, dueDate);
        }
        return false;
    }

    @Override
    public List<NodeRef> getAllRunningReports() {
        return getReportsInStatus(ReportStatus.RUNNING);
    }

    @Override
    public List<Node> getAllInQueueReports() {
        List<NodeRef> reportRefs = getReportsInStatus(ReportStatus.IN_QUEUE, ReportStatus.CANCELLING_REQUESTED, ReportStatus.DELETING_REQUESTED);
        List<Node> reports = new ArrayList<Node>();
        for (NodeRef reportRef : reportRefs) {
            Node report = new Node(reportRef);
            report.getProperties();
            reports.add(report);
        }
        return reports;
    }

    private List<NodeRef> getReportsInStatus(ReportStatus... requiredStatuses) {
        Assert.notNull(requiredStatuses, "requiredStatuses cannot be null.");
        List<ChildAssociationRef> childAssocs = getReportResultsFromFolder(getReportsSpaceRef());
        List<NodeRef> reportRefs = new ArrayList<NodeRef>();
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef reportRef = childAssoc.getChildRef();
            String statusStr = (String) nodeService.getProperty(reportRef, ReportModel.Props.STATUS);
            if (StringUtils.isNotBlank(statusStr)) {
                ReportStatus reportStatus = ReportStatus.valueOf(statusStr);
                for (ReportStatus requiredStatus : requiredStatuses) {
                    if (requiredStatus == reportStatus) {
                        reportRefs.add(reportRef);
                        break;
                    }
                }
            }
        }
        return reportRefs;
    }

    @Override
    public List<ReportResult> getReportResultsForUser(String username) {
        Assert.isTrue(StringUtils.isNotBlank(username));
        List<ChildAssociationRef> childAssocs = getReportResultsFromFolder(getReportsSpaceRef());
        List<ReportResult> userReports = new ArrayList<ReportResult>();
        for (ChildAssociationRef childAssoc : childAssocs) {
            WmNode reportNode = new WmNode(childAssoc.getChildRef(), ReportModel.Types.REPORT_RESULT);
            Map<String, Object> reportProps = reportNode.getProperties();
            if (StringUtils.equals((String) reportProps.get(ReportModel.Props.USERNAME), username)) {
                userReports.add(populateReportResult(reportNode, reportProps));
            }
        }
        childAssocs = getReportResultsFromFolder(userService.retrieveUserReportsFolderRef(username));
        for (ChildAssociationRef childAssoc : childAssocs) {
            WmNode reportNode = new WmNode(childAssoc.getChildRef(), ReportModel.Types.REPORT_RESULT);
            userReports.add(populateReportResult(reportNode, reportNode.getProperties()));
        }
        return userReports;
    }

    private ReportResult populateReportResult(WmNode reportNode, Map<String, Object> reportProps) {
        ReportResult reportResult = new ReportResult(reportNode);
        NodeRef reportResultRef = reportNode.getNodeRef();
        String reportTypeStr = (String) reportProps.get(ReportModel.Props.REPORT_TYPE);
        TemplateReportType reportType = TemplateReportType.valueOf(reportTypeStr);
        if (!TemplateReportType.CONSOLIDATED_LIST.name().equals(reportTypeStr)) {
            List<ChildAssociationRef> filters = nodeService.getChildAssocs(reportResultRef, Collections.singleton(ReportHelper.getFilterType(reportType)));
            if (filters != null && !filters.isEmpty()) {
                reportResult.setTemplateName((String) nodeService.getProperty(filters.get(0).getChildRef(), ReportHelper.getTemplateNameProp(reportType)));
            }
        }
        NodeRef reportResultFileRef = getReportResultFile(reportResultRef);
        if (reportResultFileRef != null) {
            reportResult.setDownloadUrl(fileService.generateURL(reportResultFileRef));
        }
        return reportResult;
    }

    private NodeRef getReportResultFile(NodeRef reportResultRef) {
        List<FileInfo> fileInfos = fileFolderService.listFiles(reportResultRef);
        for (FileInfo fileInfo : fileInfos) {
            NodeRef fileRef = fileInfo.getNodeRef();
            if (!nodeService.hasAspect(fileRef, DocumentTemplateModel.Aspects.TEMPLATE_REPORT)) {
                return fileRef;
            }
        }
        return null;
    }

    private List<ChildAssociationRef> getReportResultsFromFolder(NodeRef folderRef) {
        return nodeService.getChildAssocs(folderRef, Collections.singleton(ReportModel.Types.REPORT_RESULT));
    }

    @Override
    public void markReportRunning(NodeRef reportRef) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(ReportModel.Props.RUN_START_DATE_TIME, new Date());
        props.put(ReportModel.Props.STATUS, ReportStatus.RUNNING.toString());
        nodeService.addProperties(reportRef, props);
    }

    @Override
    public void enqueueReportForCancelling(final NodeRef reportRef) {
        if (!nodeService.exists(reportRef)) {
            // this functionality is called from report list; executeReportsJob may have deleted the node meanwhile
            return;
        }
        Node parent = generalService.getAncestorWithType(reportRef, ContentModel.TYPE_PERSON);
        if (parent != null) {
            if (ReportStatus.CANCELLING_REQUESTED == ReportStatus.valueOf((String) nodeService.getProperty(reportRef, ReportModel.Props.STATUS))) {
                nodeService.setProperty(reportRef, ReportModel.Props.STATUS, ReportStatus.CANCELLED.toString());
            }
        } else {
            transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {

                @Override
                public Void execute() throws Throwable {
                    nodeService.setProperty(reportRef, ReportModel.Props.STATUS, ReportStatus.CANCELLING_REQUESTED.toString());
                    return null;
                }
            }, false, true);
        }
    }

    @Override
    public void enqueueReportForDeleting(final NodeRef reportRef) {
        if (!nodeService.exists(reportRef)) {
            // this functionality is called from report list; executeReportsJob may have deleted the node meanwhile
            return;
        }
        Node parent = generalService.getAncestorWithType(reportRef, ContentModel.TYPE_PERSON);
        if (parent != null) {
            deleteReportResult(reportRef);
        } else {
            transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {

                @Override
                public Void execute() throws Throwable {
                    nodeService.setProperty(reportRef, ReportModel.Props.STATUS, ReportStatus.DELETING_REQUESTED.toString());
                    return null;
                }
            }, false, true);
        }
    }

    @Override
    public void deleteReportResult(NodeRef reportResultRef) {
        if (nodeService.exists(reportResultRef)) {
            nodeService.deleteNode(reportResultRef);
        }
    }

    @Override
    public void markReportDownloaded(NodeRef reportRef) {
        if (reportRef == null || !nodeService.exists(reportRef)) {
            return;
        }
        ReportStatus currentStatus = ReportStatus.valueOf((String) nodeService.getProperty(reportRef, ReportModel.Props.STATUS));
        if (ReportStatus.EXCEL_FULL == currentStatus) {
            nodeService.setProperty(reportRef, ReportModel.Props.STATUS, ReportStatus.EXCEL_FULL_DOWNLOADED.toString());
        } else if (ReportStatus.FINISHED == currentStatus) {
            nodeService.setProperty(reportRef, ReportModel.Props.STATUS, ReportStatus.FINISHED_DOWNLOADED.toString());
        }
    }

    private String formatBoolean(Boolean value) {
        return value == null ? "" : (value ? "jah" : "ei");
    }

    private String formatList(List value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean isString = false;
        boolean isAllBlank = true;
        boolean isCollection = false;
        for (Object listElement : value) {
            if (listElement != null && isAllBlank) {
                isAllBlank = false;
            }
            if (listElement instanceof String) {
                isString = true;
                break;
            }
            if (listElement instanceof List) {
                isCollection = true;
                break;
            }
        }
        if (isString || isAllBlank) {
            return TextUtil.joinNonBlankStringsWithComma(value);
        } else if (isCollection) {
            StringBuffer sb = new StringBuffer("(");
            int i = 0;
            for (Object listElement : value) {
                List list = (List) listElement;
                if (i++ > 0) {
                    sb.append(", ");
                }
                sb.append(formatList(list));
            }
            sb.append(")");
            return sb.toString();
        } else {
            return value.toString();
        }
    }

    private static class RowProvider {
        private final Sheet sheet;
        private int rowNum;

        public RowProvider(Sheet sheet, int startRow) {
            Assert.notNull(sheet);
            Assert.isTrue(startRow >= 0);
            this.sheet = sheet;
            rowNum = startRow;
        }

        public Row getRow() {
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                row = sheet.createRow(rowNum);
            }
            rowNum++;
            return row;
        }
    }

    // siia

    private abstract class FillExcelRowCallback {

        protected Map<String, String> types;

        public abstract int initAndCalculateRows(NodeRef taskRef);

        public abstract void execute(RowProvider rowProvider, NodeRef taskRef);

        public abstract void createHeadings(Row row);

    }

    private class FillExcelTaskRowCallback extends FillExcelRowCallback {
        private final Map<QName, String> taskNames;
        private final String compoundWorkflowServerUrlPrefix;
        private final String documentServerUrlPrefix;
        private final String caseFileServerUrlPrefix;

        public FillExcelTaskRowCallback(Map<String, String> types, String compoundWorkflowServerUrlPrefix, String documentServerUrlPrefix, Map<QName, String> taskNames) {
            Assert.notNull(types, "Types cannot be null.");
            Assert.notNull(taskNames, "Task names cannot be null.");
            this.types = types;
            this.taskNames = taskNames;
            this.compoundWorkflowServerUrlPrefix = compoundWorkflowServerUrlPrefix;
            this.documentServerUrlPrefix = documentServerUrlPrefix;
            caseFileServerUrlPrefix = documentTemplateService.getCaseFileUrl(null);
        }

        @Override
        public int initAndCalculateRows(NodeRef taskRef) {
            return 1;
        }

        @Override
        public void createHeadings(Row row) {
            List<String> msgKeys = getReportHeaderMsgKeys(TemplateReportType.TASKS_REPORT);
            if (msgKeys == null) {
                return;
            }
            if (!workflowService.isDocumentWorkflowEnabled()) {
                msgKeys = msgKeys.subList(4, msgKeys.size());
            }
            int cellNum = 0;
            for (String msgKey : msgKeys) {
                setCellValueTruncateIfNeeded(row.createCell(cellNum++), MessageUtil.getMessage(msgKey), LOG);
            }
        }

        @Override
        public void execute(RowProvider rowProvider, NodeRef taskRef) {
            Task task = workflowService.getTaskWithoutParentAndChildren(taskRef, null, false);
            Document document = null;
            CaseFile caseFile = null;
            CompoundWorkflow compoundWorkflow = null;
            NodeRef workflowNodeRef = task.getWorkflowNodeRef();
            if (workflowNodeRef != null && !nodeService.exists(workflowNodeRef)) {
                LOG.error("Not existing parent workflow nodeRef=" + workflowNodeRef + " for task=\n" + task);
                workflowNodeRef = null;
            }
            if (workflowNodeRef != null) {
                NodeRef documentRef = generalService.getAncestorNodeRefWithType(workflowNodeRef, DocumentCommonModel.Types.DOCUMENT);
                NodeRef caseFileRef = generalService.getAncestorNodeRefWithType(workflowNodeRef, CaseFileModel.Types.CASE_FILE);
                NodeRef compoundWorkflowRef = generalService.getAncestorNodeRefWithType(workflowNodeRef, WorkflowCommonModel.Types.COMPOUND_WORKFLOW);
                compoundWorkflow = compoundWorkflowRef != null ? workflowService.getCompoundWorkflow(compoundWorkflowRef) : null;
                if (documentRef != null) {
                    document = new Document(documentRef);
                }
                if (caseFileRef != null) {
                    caseFile = caseFileService.getCaseFile(caseFileRef);
                }
            }
            Row row = rowProvider.getRow();
            int cellIndex = 0;
            if (workflowService.isDocumentWorkflowEnabled()) {
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document != null ? document.getRegNumber() : "", LOG);
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document != null ? formatDateOrEmpty(DATE_FORMAT, document.getRegDateTime()) : "", LOG);
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document != null ? formatDateOrEmpty(DATE_FORMAT, document.getCreated()) : "", LOG);
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document != null ? types.get(document.objectTypeId()) : "", LOG);
            }
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), getTitle(compoundWorkflow, document, caseFile), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), task.getCreatorName(), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, task.getStartedDateTime()), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), task.getOwnerName(), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), task.getOwnerOrgStructUnit(), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), task.getOwnerJobTitle(), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), taskNames.get(task.getType()), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, task.getDueDate()), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, task.getCompletedDateTime()), LOG);
            if (task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK, WorkflowSpecificModel.Types.REVIEW_TASK, WorkflowSpecificModel.Types.OPINION_TASK)) {
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), task.getOutcome(), LOG);
            } else {
                String outcome = task.getOutcome();
                String comment = task.getComment();
                boolean notBlankComment = StringUtils.isNotBlank(comment);
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++),
                        ((StringUtils.isNotBlank(outcome) || notBlankComment) ? (outcome + (notBlankComment ? (": " + comment) : "")) : null), LOG);
            }
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatBoolean(task.isResponsible()), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, task.getStoppedDateTime()), LOG);
            String taskResolution;
            if (task.isType(WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK)) {
                taskResolution = MessageUtil.getMessage("task_search_due_date_extension_task_resolution", task.getProposedDueDateStr(), task.getWorkflowResolution());
            } else {
                taskResolution = task.getResolution();
            }
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), taskResolution, LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), isAfterDate(task.getCompletedDateTime(), task.getDueDate()) ? "jah" : "ei", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), task.getStatus(), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document != null ? document.getFunctionLabel() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document != null ? document.getSeriesLabel() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document != null ? document.getVolumeLabel() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document != null ? document.getCaseLabel() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), getUrl(document, compoundWorkflow, caseFile, task), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), compoundWorkflow != null ? compoundWorkflow.getWorkflowTypeString() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), compoundWorkflow != null ? compoundWorkflow.getTitle() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), compoundWorkflow != null ? compoundWorkflow.getOwnerName() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), compoundWorkflow != null ? compoundWorkflow.getOwnerStructUnit() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), compoundWorkflow != null ? compoundWorkflow.getOwnerJobTitle() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), compoundWorkflow != null ? compoundWorkflow.getCreatedDateStr() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), compoundWorkflow != null ? compoundWorkflow.getStartedDateStr() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), compoundWorkflow != null ? compoundWorkflow.getStoppedDateStr() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), compoundWorkflow != null ? compoundWorkflow.getEndedDateStr() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), compoundWorkflow != null ? compoundWorkflow.getStatus() : "", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), compoundWorkflow != null ? compoundWorkflow.getNumberOfDocumentsStr() : "", LOG);
        }

        private String getUrl(Document document, CompoundWorkflow compoundWorkflow, CaseFile caseFile, Task task) {
            String url = "";
            if ((task instanceof LinkedReviewTask) || compoundWorkflow == null) {
                return task.getOriginalTaskObjectUrl();
            }
            if (compoundWorkflow.isIndependentWorkflow()) {
                url = compoundWorkflowServerUrlPrefix + compoundWorkflow.getNodeRef().getId();
            } else if (compoundWorkflow.isCaseFileWorkflow()) {
                url = caseFile != null ? caseFileServerUrlPrefix + caseFile.getNodeRef().getId() : "";
            } else {
                url = document != null ? documentServerUrlPrefix + document.getNodeRef().getId() : "";
            }
            return url;
        }

        private String getTitle(CompoundWorkflow compoundWorkflow, Document document, CaseFile caseFile) {
            String title = "";
            if (compoundWorkflow == null) {
                return "";
            }
            if (compoundWorkflow.isCaseFileWorkflow()) {
                title = caseFile != null ? caseFile.getTitle() : "";
            } else if (compoundWorkflow.isIndependentWorkflow()) {
                title = compoundWorkflow.getTitle();
            } else {
                title = document != null ? document.getDocName() : "";
            }
            return title;
        }
    }

    private class FillExcelVolumeRowCallback extends FillExcelRowCallback {

        private final List<FieldDefinition> volFields;
        private Map<VolumeType, String> volTypeNames = new HashMap<VolumeType, String>();
        private final List<String> excludedFields = Arrays.asList(
                DocumentCommonModel.Props.FUNCTION.getLocalName()
                , DocumentCommonModel.Props.SERIES.getLocalName()
                , VolumeModel.Props.VOLUME_TYPE.getLocalName()
                , DocumentAdminModel.Props.OBJECT_TYPE_ID.getLocalName()
                , VolumeModel.Props.VOLUME_MARK.getLocalName()
                , DocumentDynamicModel.Props.TITLE.getLocalName()
                , VolumeModel.Props.DESCRIPTION.getLocalName()
                , VolumeModel.Props.VALID_FROM.getLocalName()
                , VolumeModel.Props.VALID_TO.getLocalName()
                , VolumeModel.Props.STATUS.getLocalName()
                , DocumentCommonModel.Props.OWNER_NAME.getLocalName()
                , DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT.getLocalName()
                , DocumentCommonModel.Props.OWNER_JOB_TITLE.getLocalName()
                , DocumentDynamicModel.Props.OWNER_SERVICE_RANK.getLocalName()
                , DocumentDynamicModel.Props.OWNER_WORK_ADDRESS.getLocalName()
                , DocumentCommonModel.Props.OWNER_EMAIL.getLocalName()
                , DocumentCommonModel.Props.OWNER_PHONE.getLocalName()
                );

        public FillExcelVolumeRowCallback(Map<String, String> types) {
            Assert.notNull(types, "Types cannot be null.");
            this.types = types;
            volFields = documentAdminService.getVolumeFieldDefinitions();
            removePreDefinedFields();
        }

        @Override
        public int initAndCalculateRows(NodeRef nodeRef) {
            return excludedFields.size() + volFields.size() + 1;
        }

        private String getVolType(VolumeType volType) {
            if (volTypeNames == null) {
                volTypeNames = new HashMap<VolumeType, String>();
            }
            String string = volTypeNames.get(volType);
            if (StringUtils.isNotBlank(string)) {
                return string;
            }
            string = MessageUtil.getMessage(volType);
            volTypeNames.put(volType, string);
            return string;

        }

        private void removePreDefinedFields() {
            List<FieldDefinition> toBeRemoved = new ArrayList<FieldDefinition>();
            for (FieldDefinition field : volFields) {
                if (excludedFields.contains(field.getFieldId())) {
                    toBeRemoved.add(field);
                }
            }
            volFields.removeAll(toBeRemoved);
        }

        @Override
        public void execute(RowProvider rowProvider, NodeRef nodeRef) {
            int cellIndex = 0;
            Node node = new MapNode(nodeRef);
            Map<String, Object> props = node.getProperties();
            Row row = rowProvider.getRow();

            LinkedHashSet<ArchivalsStoreVO> stores = generalService.getArchivalsStoreVOs();
            ArchivalsStoreVO store = null;
            for (ArchivalsStoreVO archivalsStoreVO : stores) {
                if (node.getNodeRef().getStoreRef().equals(archivalsStoreVO.getStoreRef())) {
                    store = archivalsStoreVO;
                    break;
                }
            }
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), (store == null ? MessageUtil.getMessage("functions_title") : store.getTitle()), LOG); // toimiku asukoht

            NodeRef functionRef = (NodeRef) props.get(DocumentCommonModel.Props.FUNCTION);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++),
                    functionRef == null ? "" : DocumentLocationGenerator.getFunctionLabel(functionsService.getFunctionByNodeRef(functionRef)), LOG); // funktsioon
            NodeRef seriesRef = (NodeRef) props.get(DocumentCommonModel.Props.SERIES);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++),
                    seriesRef == null ? "" : DocumentLocationGenerator.getSeriesLabel(seriesService.getSeriesByNodeRef(seriesRef)), LOG); // Sari
            if (node.getType().equals(CaseFileModel.Types.CASE_FILE)) {
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), getVolType(VolumeType.CASE_FILE), LOG);// toimiku tüüp
            } else {
                String volType = (String) props.get(VolumeModel.Props.VOLUME_TYPE);
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), getVolType(VolumeType.valueOf(volType)), LOG);// toimiku tüüp
            }
            String typeId = (String) props.get(DocumentAdminModel.Props.OBJECT_TYPE_ID);

            String type = types.get(typeId);
            if (StringUtils.isBlank(type) && StringUtils.isNotBlank(typeId)) {
                type = documentAdminService.getCaseFileTypeName(new MapNode(nodeRef));
                types.put(typeId, type);
            }
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), type, LOG);// asjatoimiku liik
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), (String) props.get(VolumeModel.Props.VOLUME_MARK), LOG); // tähis
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), (String) props.get(DocumentDynamicModel.Props.TITLE), LOG); // pealkiri
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), (String) props.get(VolumeModel.Props.DESCRIPTION), LOG); // kirjeldus
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, (Date) props.get(VolumeModel.Props.VALID_FROM)), LOG); // kehtiv alates
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, (Date) props.get(VolumeModel.Props.VALID_TO)), LOG); // kehtiv kuni
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), (String) props.get(VolumeModel.Props.STATUS), LOG); // staatus

            // caseFile only
            if (node.getType().equals(CaseFileModel.Types.CASE_FILE)) {
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), (String) props.get(DocumentCommonModel.Props.OWNER_NAME), LOG); // vastutaja
                @SuppressWarnings("unchecked")
                List<String> ownerOrgStructUnit = (List<String>) props.get(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT);
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), UserUtil.getDisplayUnit(ownerOrgStructUnit), LOG);
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), (String) props.get(DocumentCommonModel.Props.OWNER_JOB_TITLE), LOG);
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), (String) props.get(DocumentDynamicModel.Props.OWNER_SERVICE_RANK), LOG);
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), (String) props.get(DocumentDynamicModel.Props.OWNER_WORK_ADDRESS), LOG);
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), (String) props.get(DocumentCommonModel.Props.OWNER_EMAIL), LOG);
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), (String) props.get(DocumentCommonModel.Props.OWNER_PHONE), LOG);
            } else {
                cellIndex = addEmptyCells(row, cellIndex, 7);
            }

            for (FieldDefinition field : volFields) {
                Object prop = props.get(field.getQName());
                String string = "";
                if (prop instanceof String) {
                    string = (String) prop;
                } else if (prop instanceof Date) {
                    string = formatDateOrEmpty(DATE_FORMAT, (Date) prop);
                } else if (prop instanceof Boolean) {
                    string = formatBoolean((Boolean) prop);
                } else if (prop instanceof List) {
                    string = formatList((List) prop);
                } else if (prop != null) {
                    string = prop.toString();
                }
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), string, LOG);
            }
            if (node.getType().equals(CaseFileModel.Types.CASE_FILE)) {
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), documentTemplateService.getCaseFileUrl(nodeRef), LOG);
            } else {
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), documentTemplateService.getVolumeUrl(nodeRef), LOG);
            }

        }

        @Override
        public void createHeadings(Row row) {
            List<String> msgKeys = getReportHeaderMsgKeys(TemplateReportType.VOLUMES_REPORT);
            if (msgKeys == null) {
                return;
            }
            int cellNum = 0;
            for (String msgKey : msgKeys) {
                setCellValueTruncateIfNeeded(row.createCell(cellNum++), MessageUtil.getMessage(msgKey), LOG);
            }
            for (FieldDefinition field : volFields) {
                setCellValueTruncateIfNeeded(row.createCell(cellNum++), field.getName(), LOG);
            }
            setCellValueTruncateIfNeeded(row.createCell(cellNum++), MessageUtil.getMessage("link_to"), LOG);
        }

    }

    private class FillExcelDocumentOnlyRowCallback extends FillExcelRowCallback {
        protected final Map<String, Boolean> fieldsToShow;
        protected final boolean[] fieldsToShowArray;

        public FillExcelDocumentOnlyRowCallback(Map<String, String> types, Map<String, Boolean> fieldsToShow, boolean[] fieldsToShowArray) {
            Assert.notNull(types, "Types cannot be null.");
            Assert.notNull(fieldsToShow, "fieldsToShow cannot be null.");
            Assert.notNull(fieldsToShowArray, "fieldsToShowArray cannot be null.");
            this.types = types;
            this.fieldsToShow = fieldsToShow;
            this.fieldsToShowArray = fieldsToShowArray;
        }

        @Override
        public int initAndCalculateRows(NodeRef taskRef) {
            return 1;
        }

        @Override
        public void execute(RowProvider rowProvider, NodeRef documentRef) {
            generateDocReportMainFields(new Document(documentRef), rowProvider.getRow(), 0, 0, types, fieldsToShowArray);
        }

        @Override
        public void createHeadings(Row row) {
            generateHeadings(row, 0, getReportHeaderMsgKeys(TemplateReportType.DOCUMENTS_REPORT), fieldsToShow);
        }

    }

    private class FillExcelDocumentWithSubnodesRowCallback extends FillExcelDocumentOnlyRowCallback {
        private List<SendInfo> sendInfos;
        private Document document;
        private int rowsNeeded;

        public FillExcelDocumentWithSubnodesRowCallback(Map<String, String> types, Map<String, Boolean> fieldsToShow, boolean[] fieldsToShowArray) {
            super(types, fieldsToShow, fieldsToShowArray);
        }

        @Override
        public int initAndCalculateRows(NodeRef documentRef) {
            document = new Document(documentRef);
            List<String> searchableSendMode = document.getSearchableSendModeFromGeneralProps();
            if (searchableSendMode != null && !searchableSendMode.isEmpty()) {
                sendInfos = sendOutService.getDocumentSendInfos(documentRef);
            } else {
                sendInfos = new ArrayList<SendInfo>();
            }
            rowsNeeded = 1 + sendInfos.size() + getListSize(document.getPartyNames()) + getListSize(document.getApplicantNames());
            return rowsNeeded;
        }

        private int getListSize(List<String> list) {
            return list != null ? list.size() : 0;
        }

        @Override
        /**
         * Fills given Excel rows as follows:
         * 1) One row with document main data (always present)
         * 2) One row for each contract party child node, add data from 1) + contract party specific data
         * 3) One row for each send info child node, add data from 1) + send info specific data
         * 4) One row for each errand applicant child node, add data from 1) + errand applicant specific data
         *  */
        public void execute(RowProvider rowProvider, NodeRef documentRef) {
            Assert.notNull(document); // document has to be loaded
            int rowIndex = 0;

            int partyRowCounter = 0;
            int partyListSize = getListSize(document.getPartyNames()); // partyNames must be present if party group is added

            int applicantRowCounter = 0;
            int applicantListSize = getListSize(document.getApplicantNames()); // applicantNames must be present if applicant group is added

            Iterator<SendInfo> sendInfoIterator = sendInfos.iterator();

            boolean isContractPartyRow = false;
            boolean isSendInfoRow = false;
            boolean isApplicantRow = false;
            boolean hasCostManagers = isNotEmptyList(document.getCostManagers());
            boolean hasCountries = isNotEmptyList(document.getCountries());
            boolean hasCounties = isNotEmptyList(document.getCounties());
            boolean hasCities = isNotEmptyList(document.getCities());
            while (rowIndex < rowsNeeded) {
                Row row = rowProvider.getRow();
                if (rowIndex > 0) {
                    isContractPartyRow = partyRowCounter < partyListSize;
                    isSendInfoRow = !isContractPartyRow && sendInfoIterator.hasNext();
                    isApplicantRow = !isContractPartyRow && !isSendInfoRow && (applicantRowCounter < applicantListSize);
                }
                Pair<Integer, Integer> cellIndexes = generateDocReportMainFields(document, row, 0, 0, types, fieldsToShowArray);

                Integer cellIndex = cellIndexes.getFirst();
                Integer notMandatoryCellIndex = cellIndexes.getSecond();
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getAllRecipients(), LOG);
                if (isContractPartyRow) {
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getPartyNames().get(partyRowCounter), LOG);
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getPartyContactPersons().get(partyRowCounter), LOG);
                    partyRowCounter++;
                } else {
                    cellIndex = addEmptyCells(row, cellIndex, 2);
                }
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getFirstPartyContactPerson(), LOG);

                if (isSendInfoRow) {
                    SendInfo sendInfo = sendInfoIterator.next();
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), sendInfo.getRecipient(), LOG);
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, sendInfo.getSendDateTime()), LOG);
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), sendInfo.getSendMode(), LOG);
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), sendInfo.getSendStatus(), LOG);
                } else {
                    cellIndex = addEmptyCells(row, cellIndex, 4);
                }

                if (isApplicantRow) {
                    if (fieldsToShowArray[notMandatoryCellIndex++]) {
                        if (hasCostManagers) {
                            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getCostManagers().get(applicantRowCounter), LOG);
                        } else {
                            row.createCell(cellIndex++);
                        }
                    }
                    if (fieldsToShowArray[notMandatoryCellIndex++]) {
                        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getApplicantNames().get(applicantRowCounter), LOG);
                    }
                    if (fieldsToShowArray[notMandatoryCellIndex++]) {
                        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getErrandBeginDates().get(applicantRowCounter)), LOG);
                    }
                    if (fieldsToShowArray[notMandatoryCellIndex++]) {
                        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getErrandEndDates().get(applicantRowCounter)), LOG);
                    }
                    if (fieldsToShowArray[notMandatoryCellIndex++] && hasCountries) {
                        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getCountries().get(applicantRowCounter), LOG); // removable
                    }
                    if (fieldsToShowArray[notMandatoryCellIndex++] && hasCounties) {
                        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getCounties().get(applicantRowCounter), LOG); // removable
                    }
                    if (fieldsToShowArray[notMandatoryCellIndex++] && hasCities) {
                        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getCities().get(applicantRowCounter), LOG); // removable
                    }
                    applicantRowCounter++;
                } else {
                    int emptyCellsToAdd = 0;
                    for (int i = 0; i < 7; i++) {
                        emptyCellsToAdd += fieldsToShowArray[notMandatoryCellIndex++] ? 1 : 0;
                    }
                    cellIndex = addEmptyCells(row, cellIndex, emptyCellsToAdd);
                }

                if (fieldsToShowArray[notMandatoryCellIndex++]) {
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getDelivererName(), LOG);
                }
                if (fieldsToShowArray[notMandatoryCellIndex++]) {
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getProcurementType(), LOG);
                }
                if (fieldsToShowArray[notMandatoryCellIndex++]) {
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getInvoiceNumber(), LOG);
                }
                if (fieldsToShowArray[notMandatoryCellIndex++]) {
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getInvoiceDate()), LOG);
                }
                if (fieldsToShowArray[notMandatoryCellIndex++]) {
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getSellerPartyName(), LOG);
                }
                if (fieldsToShowArray[notMandatoryCellIndex++]) {
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getSellerPartyRegNumber(), LOG);
                }
                if (fieldsToShowArray[notMandatoryCellIndex++]) {
                    setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getTotalSum(), LOG);
                }
                rowIndex++;
            }
        }

        private boolean isNotEmptyList(List<String> costManagers) {
            return costManagers != null && !document.getCostManagers().isEmpty();
        }

        @Override
        public void createHeadings(Row row) {
            int cellIndex = generateHeadings(row, 0, getReportHeaderMsgKeys(TemplateReportType.DOCUMENTS_REPORT), fieldsToShow);
            generateHeadings(row, cellIndex, ReportHelper.getDocumentReportHeaderAdditionalMsgKeys(), fieldsToShow);
        }

    }

    private static int addEmptyCells(Row row, int startIndex, int numRows) {
        for (int i = 0; i < numRows; i++) {
            row.createCell(startIndex++);
        }
        return startIndex;
    }

    private int generateHeadings(Row row, int cellNum, List<String> msgKeys, Map<String, Boolean> showFields) {
        for (String msgKey : msgKeys) {
            String fieldId = ReportHelper.getCheckableFieldByMsgKey(msgKey);
            if (fieldId != null && showFields.containsKey(fieldId) && !showFields.get(fieldId)) {
                continue;
            }
            setCellValueTruncateIfNeeded(row.createCell(cellNum++), MessageUtil.getMessage(msgKey), LOG);
        }
        return cellNum;
    }

    private Pair<Integer, Integer> generateDocReportMainFields(Document document, Row row, int cellIndex, int notMandatoryCellIndex, Map<String, String> types,
            boolean[] fieldsToShowArray) {
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getRegNumber(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getRegDateTime()), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), types.get(document.objectTypeId()), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getFunctionLabel(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getSeriesLabel(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getVolumeLabel(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getCaseLabel(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getDocName(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getAccessRestriction(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getAccessRestrictionReason(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getAccessRestrictionBeginDate()), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getAccessRestrictionEndDate()), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getAccessRestrictionEndDesc(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getOwnerName(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getOwnerOrgStructUnit(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getOwnerJobTitle(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getDocStatus(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getSender(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getSenderRegDate()), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getSenderRegNumber(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getTransmittalMode(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getDueDate()), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getComplienceDate()), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getSignerName(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getSignerJobTitle(), LOG);
        if (fieldsToShowArray[notMandatoryCellIndex++]) {
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getKeywords(), LOG);
        }
        if (fieldsToShowArray[notMandatoryCellIndex++]) {
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getHierarchicalKeywords(), LOG);
        }
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getStorageType(), LOG);
        setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getCreated()), LOG);
        return new Pair<Integer, Integer>(cellIndex, notMandatoryCellIndex);
    }

    @Override
    public boolean isReportGenerationEnabled() {
        return reportGenerationEnabled;
    }

    @Override
    public boolean isReportGenerationPaused() {
        return reportGenerationPaused;
    }

    @Override
    public boolean isUsableByAdminDocManagerOnly() {
        return usableByAdminDocManagerOnly;
    }

    @Override
    public void setReportGenerationPaused(boolean reportGenerationPaused) {
        this.reportGenerationPaused = reportGenerationPaused;
    }

    public void setDocumentSearchService(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    public void setDocumentTemplateService(DocumentTemplateService documentTemplateService) {
        this.documentTemplateService = documentTemplateService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setGeneralService(GeneralService generalService) {
        this.generalService = generalService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setDocumentAdminService(DocumentAdminService documentAdminService) {
        this.documentAdminService = documentAdminService;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }

    public void setFileService(FileService fileService) {
        this.fileService = fileService;
    }

    public void setCaseFileService(CaseFileService caseFileService) {
        this.caseFileService = caseFileService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setSendOutService(SendOutService sendOutService) {
        this.sendOutService = sendOutService;
    }

    public void setReportGenerationEnabled(boolean reportGenerationEnabled) {
        this.reportGenerationEnabled = reportGenerationEnabled;
    }

    public void setFunctionsService(FunctionsService functionsService) {
        this.functionsService = functionsService;
    }

    public void setSeriesService(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    public void setUsableByAdminDocManagerOnly(boolean usableByAdminDocManagerOnly) {
        this.usableByAdminDocManagerOnly = usableByAdminDocManagerOnly;
    }
}