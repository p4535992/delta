package ee.webmedia.alfresco.report.service;

import static ee.webmedia.alfresco.report.service.ExcelUtil.setCellValueTruncateIfNeeded;
import static ee.webmedia.alfresco.report.service.ReportHelper.getReportHeaderMsgKeys;
import static ee.webmedia.alfresco.utils.TextUtil.formatDateOrEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.classificator.enums.TemplateReportOutputType;
import ee.webmedia.alfresco.classificator.enums.TemplateReportType;
import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.common.web.WmNode;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.document.file.service.FileService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.document.sendout.model.SendInfo;
import ee.webmedia.alfresco.document.sendout.service.SendOutService;
import ee.webmedia.alfresco.report.model.ReportDataCollector;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.report.model.ReportStatus;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
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
    private NodeService nodeService;
    private GeneralService generalService;
    private UserService userService;
    private WorkflowService workflowService;
    private DocumentAdminService documentAdminService;
    private MimetypeService mimetypeService;
    private FileService fileService;
    private TransactionService transactionService;
    private SendOutService sendOutService;
    /**
     * NB! Kui rakendus jookseb klastris, siis praegu eeldatakse, et aruannete genereerimine jookseb ainult ühes klastri õlas
     * ja ka genereerimise peatamine on võimalik ainult selles õlas.
     */
    private boolean reportGenerationEnabled;
    private boolean reportGenerationPaused;

    @Override
    public NodeRef createReportResult(Node filter, TemplateReportType reportType, QName parentToChildAssoc) {
        Assert.isTrue(reportType != null && parentToChildAssoc != null, "reportType and parentToChildAssoc cannot be null.");
        Map<QName, Serializable> reportResultProps = new HashMap<QName, Serializable>();
        reportResultProps.put(ReportModel.Props.USERNAME, userService.getCurrentUserName());
        Map<String, Object> filterProps = filter.getProperties();
        String filterName = (String) filterProps.get(ReportHelper.getFilterNameProp(reportType).toString());
        String templateName = (String) filterProps.get(ReportHelper.getTemplateNameProp(reportType).toString());
        String reportName = StringUtils.isNotBlank(filterName) ? filterName : FilenameUtil.getFilenameWithoutExtension(templateName) + "_result";
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
    public ReportDataCollector getReportFileInMemory(ReportDataCollector reportDataCollector) {
        NodeRef reportResultRef = reportDataCollector.getReportResultNodeRef();
        Map<QName, Serializable> reportResultProps = reportDataCollector.getReportResultProps();
        String reportTypeStr = (String) reportResultProps.get(ReportModel.Props.REPORT_TYPE);
        TemplateReportType reportType = TemplateReportType.valueOf(reportTypeStr);
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
            return createReportFileInMemory(taskRefs, templateReader, reportDataCollector, new FillExcelTaskRowCallback(types, taskNames));
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
                documentRefs.addAll(documentSearchService.searchDocumentsForReport(filter, storeRef));
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
        List<ChildAssociationRef> filters = nodeService.getChildAssocs(reportResultRef, Collections.singleton(ReportHelper.getFilterType(reportType)));
        if (filters != null && !filters.isEmpty()) {
            reportResult.setTemplateName((String) nodeService.getProperty(filters.get(0).getChildRef(), ReportHelper.getTemplateNameProp(reportType)));
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

    private abstract class FillExcelRowCallback {

        protected Map<String, String> types;

        public abstract int initAndCalculateRows(NodeRef taskRef);

        public abstract void execute(RowProvider rowProvider, NodeRef taskRef);

        public abstract void createHeadings(Row row);

    }

    private class FillExcelTaskRowCallback extends FillExcelRowCallback {
        private final Map<QName, String> taskNames;

        public FillExcelTaskRowCallback(Map<String, String> types, Map<QName, String> taskNames) {
            Assert.notNull(types, "Types cannot be null.");
            Assert.notNull(taskNames, "Task names cannot be null.");
            this.types = types;
            this.taskNames = taskNames;
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
            int cellNum = 0;
            for (String msgKey : msgKeys) {
                setCellValueTruncateIfNeeded(row.createCell(cellNum++), MessageUtil.getMessage(msgKey), LOG);
            }
        }

        @Override
        public void execute(RowProvider rowProvider, NodeRef taskRef) {
            Task task = workflowService.getTaskWithoutParentAndChildren(taskRef, null, false);
            NodeRef documentRef = generalService.getAncestorNodeRefWithType(taskRef, DocumentCommonModel.Types.DOCUMENT);
            Document document = null;
            if (documentRef != null) {
                document = new Document(documentRef);
            }
            Row row = rowProvider.getRow();
            int cellIndex = 0;
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getRegNumber(), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getRegDateTime()), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, document.getCreated()), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), types.get(document.objectTypeId()), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getDocName(), LOG);
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
                setCellValueTruncateIfNeeded(row.createCell(cellIndex++), task.getOutcome() + ": " + task.getComment(), LOG);
            }
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), task.isResponsible() ? "jah" : "ei", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), formatDateOrEmpty(DATE_FORMAT, task.getStoppedDateTime()), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), task.getResolution(), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), isAfterDate(task.getCompletedDateTime(), task.getDueDate()) ? "jah" : "ei", LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), task.getStatus(), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getFunctionLabel(), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getSeriesLabel(), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getVolumeLabel(), LOG);
            setCellValueTruncateIfNeeded(row.createCell(cellIndex++), document.getCaseLabel(), LOG);
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
                    notMandatoryCellIndex += emptyCellsToAdd;
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

        private int addEmptyCells(Row row, int startIndex, int numRows) {
            for (int i = 0; i < numRows; i++) {
                row.createCell(startIndex++);
            }
            return startIndex;
        }

        @Override
        public void createHeadings(Row row) {
            int cellIndex = generateHeadings(row, 0, getReportHeaderMsgKeys(TemplateReportType.DOCUMENTS_REPORT), fieldsToShow);
            generateHeadings(row, cellIndex, ReportHelper.getDocumentReportHeaderAdditionalMsgKeys(), fieldsToShow);
        }

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

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setSendOutService(SendOutService sendOutService) {
        this.sendOutService = sendOutService;
    }

    public void setReportGenerationEnabled(boolean reportGenerationEnabled) {
        this.reportGenerationEnabled = reportGenerationEnabled;
    }

}
