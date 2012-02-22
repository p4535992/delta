package ee.webmedia.alfresco.report.service;

import static ee.webmedia.alfresco.report.service.ExcelUtil.setCellValueTruncateIfNeeded;
import static ee.webmedia.alfresco.utils.TextUtil.formatDateOrEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
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

import ee.webmedia.alfresco.common.service.GeneralService;
import ee.webmedia.alfresco.docadmin.service.DocumentAdminService;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.search.service.DocumentSearchService;
import ee.webmedia.alfresco.report.model.ReportDataCollector;
import ee.webmedia.alfresco.report.model.ReportModel;
import ee.webmedia.alfresco.report.model.ReportStatus;
import ee.webmedia.alfresco.report.model.ReportType;
import ee.webmedia.alfresco.template.model.DocumentTemplateModel;
import ee.webmedia.alfresco.template.service.DocumentTemplateService;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.utils.FilenameUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UnableToPerformException;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.search.model.TaskReportModel;
import ee.webmedia.alfresco.workflow.search.model.TaskSearchModel;
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
    private static Map<ReportType, List<String>> reportHeaderMsgKeys = new HashMap<ReportType, List<String>>();

    private DocumentSearchService documentSearchService;
    private DocumentTemplateService documentTemplateService;
    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private GeneralService generalService;
    private UserService userService;
    private WorkflowService workflowService;
    private DocumentAdminService documentAdminService;
    private MimetypeService mimetypeService;

    static {
        reportHeaderMsgKeys.put(ReportType.TASKS_REPORT,
                Arrays.asList("task_search_result_regNum", "task_search_result_regDate", "task_search_result_createdDate", "task_search_result_docType",
                        "task_search_result_docName", "task_search_result_creatorName", "task_search_result_startedDate", "task_search_result_ownerName",
                        "task_search_result_ownerOrganizationName", "task_search_result_ownerJobTitle", "task_search_result_taskType", "task_search_result_dueDate",
                        "task_search_result_completedDate", "task_search_result_comment", "task_search_result_responsible", "task_search_result_stoppedDate",
                        "task_search_result_resolution", "task_search_result_overdue", "task_search_result_status", "task_search_result_function", "task_search_result_series",
                        "task_search_result_volume", "task_search_result_case"));

    }

    @Override
    public NodeRef createReportResult(Node filter, ReportType reportType, QName parentToChildAssoc) {
        Assert.isTrue(reportType != null && parentToChildAssoc != null, "reportType and parentToChildAssoc cannot be null.");
        Map<QName, Serializable> reportResultProps = new HashMap<QName, Serializable>();
        reportResultProps.put(ReportModel.Props.USERNAME, userService.getCurrentUserName());
        Map<String, Object> filterProps = filter.getProperties();
        String filterName = (String) filterProps.get(TaskSearchModel.Props.NAME);
        String templateName = (String) filterProps.get(TaskReportModel.Props.REPORT_TEMPLATE);
        String reportName = StringUtils.isNotBlank(filterName) ? filterName : FilenameUtil.getFilenameWithoutExtension(templateName) + "_result";
        reportResultProps.put(ReportModel.Props.REPORT_NAME, reportName);
        reportResultProps.put(ReportModel.Props.REPORT_TYPE, reportType.toString());
        reportResultProps.put(ReportModel.Props.USER_START_DATE_TIME, new Date());
        reportResultProps.put(ReportModel.Props.STATUS, ReportStatus.IN_QUEUE.toString());

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
        Map<QName, Serializable> reportResultProps = nodeService.getProperties(reportResultRef);
        reportDataCollector.setReportResultProps(reportResultProps);
        List<ChildAssociationRef> filters = nodeService.getChildAssocs(reportResultRef, Collections.singleton(TaskReportModel.Assocs.FILTER));
        Assert.isTrue(filters != null && filters.size() == 1, "reportResult must have exactly one taskReportFilter child node!");
        Node filter = new Node(filters.get(0).getChildRef());

        List<FileInfo> fileInfos = fileFolderService.listFiles(reportResultRef);
        NodeRef templateRef = null;
        String reportTypeStr = (String) reportResultProps.get(ReportModel.Props.REPORT_TYPE);
        for (FileInfo fileInfo : fileInfos) {
            NodeRef fileRef = fileInfo.getNodeRef();
            if (nodeService.hasAspect(fileRef, DocumentTemplateModel.Aspects.TEMPLATE_REPORT)
                    && StringUtils.equals(reportTypeStr, (String) nodeService.getProperty(fileRef, DocumentTemplateModel.Prop.REPORT_TYPE))) {
                templateRef = fileRef;
                break;
            }
        }
        if (templateRef == null) {
            throw new UnableToPerformException("report_error_cannot_find_template");
        }
        ContentReader templateReader = fileFolderService.getReader(templateRef);
        List<NodeRef> tasks = documentSearchService.searchTasksForReport(filter);
        return createReportFileInMemory(tasks, templateReader, ReportType.valueOf(reportTypeStr), reportDataCollector);
    }

    private NodeRef getReportsSpaceRef() {
        return generalService.getNodeRef(ReportModel.Repo.REPORTS_SPACE);
    }

    private ReportDataCollector createReportFileInMemory(List<NodeRef> taskRefs, ContentReader templateReader, ReportType reportType, ReportDataCollector reportDataCollector) {
        if (taskRefs == null || taskRefs.isEmpty()) {
            return null;
        }
        InputStream templateInputStream = null;
        try {
            templateInputStream = templateReader.getContentInputStream();
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(templateInputStream);
            Workbook wb = new org.apache.poi.xssf.streaming.SXSSFWorkbook(xssfWorkbook, 100);

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                sheet = wb.createSheet();
            }
            Row row = sheet.getRow(0);
            if (row == null) {
                row = sheet.createRow(0);
            }
            createHeadings(row, reportType);
            int rowNr = 1;
            Map<String, String> types = documentAdminService.getDocumentTypeNames(null);
            Map<QName, String> taskNames = getTaskNames();
            ReportStatus resultStatus = ReportStatus.FINISHED;
            for (NodeRef taskRef : taskRefs) {
                if (rowNr >= EXCEL_SHEET_MAX_ROWS) {
                    resultStatus = ReportStatus.EXCEL_FULL;
                    break;
                }
                row = sheet.getRow(rowNr);
                if (row == null) {
                    row = sheet.createRow(rowNr);
                }
                Task task = workflowService.getTaskWithoutParentAndChildren(taskRef, null, false);
                NodeRef documentRef = generalService.getAncestorNodeRefWithType(taskRef, DocumentCommonModel.Types.DOCUMENT);
                Document document = null;
                if (documentRef != null) {
                    document = new Document(documentRef);
                }
                setCellValueTruncateIfNeeded(row.createCell(0), document.getRegNumber(), LOG);
                setCellValueTruncateIfNeeded(row.createCell(1), formatDateOrEmpty(DATE_FORMAT, document.getRegDateTime()), LOG);
                setCellValueTruncateIfNeeded(row.createCell(2), formatDateOrEmpty(DATE_FORMAT, document.getCreated()), LOG);
                setCellValueTruncateIfNeeded(row.createCell(3), types.get(document.objectTypeId()), LOG);
                setCellValueTruncateIfNeeded(row.createCell(4), document.getDocName(), LOG);
                setCellValueTruncateIfNeeded(row.createCell(5), task.getCreatorName(), LOG);
                setCellValueTruncateIfNeeded(row.createCell(6), formatDateOrEmpty(DATE_FORMAT, task.getStartedDateTime()), LOG);
                setCellValueTruncateIfNeeded(row.createCell(7), task.getOwnerName(), LOG);
                setCellValueTruncateIfNeeded(row.createCell(8), task.getOwnerOrgStructUnit(), LOG);
                setCellValueTruncateIfNeeded(row.createCell(9), task.getOwnerJobTitle(), LOG);
                setCellValueTruncateIfNeeded(row.createCell(10), taskNames.get(task.getType()), LOG);
                setCellValueTruncateIfNeeded(row.createCell(11), formatDateOrEmpty(DATE_FORMAT, task.getDueDate()), LOG);
                setCellValueTruncateIfNeeded(row.createCell(12), formatDateOrEmpty(DATE_FORMAT, task.getCompletedDateTime()), LOG);
                if (task.isType(WorkflowSpecificModel.Types.EXTERNAL_REVIEW_TASK, WorkflowSpecificModel.Types.REVIEW_TASK, WorkflowSpecificModel.Types.OPINION_TASK)) {
                    setCellValueTruncateIfNeeded(row.createCell(13), task.getOutcome(), LOG);
                } else {
                    setCellValueTruncateIfNeeded(row.createCell(13), task.getOutcome() + ": " + task.getComment(), LOG);
                }
                setCellValueTruncateIfNeeded(row.createCell(14), task.isResponsible() ? "jah" : "ei", LOG);
                setCellValueTruncateIfNeeded(row.createCell(15), formatDateOrEmpty(DATE_FORMAT, task.getStoppedDateTime()), LOG);
                // TODO: check if this is correct
                setCellValueTruncateIfNeeded(row.createCell(16), task.getResolution(), LOG);
                setCellValueTruncateIfNeeded(row.createCell(17), isAfterDate(task.getCompletedDateTime(), task.getDueDate()) ? "jah" : "ei", LOG);
                setCellValueTruncateIfNeeded(row.createCell(18), task.getStatus(), LOG);
                setCellValueTruncateIfNeeded(row.createCell(19), document.getFunctionLabel(), LOG);
                setCellValueTruncateIfNeeded(row.createCell(20), document.getSeriesLabel(), LOG);
                setCellValueTruncateIfNeeded(row.createCell(21), document.getVolumeLabel(), LOG);
                setCellValueTruncateIfNeeded(row.createCell(22), document.getCaseLabel(), LOG);

                rowNr++;
            }
            reportDataCollector.setWorkbook(wb);
            reportDataCollector.setEncoding(templateReader.getEncoding());
            reportDataCollector.setResultStatus(resultStatus);
            return reportDataCollector;
        } catch (Exception e) {
            throw new UnableToPerformException("Failed to write to file", e);
        } finally {
            IOUtils.closeQuietly(templateInputStream);
        }
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

    private void createHeadings(Row row, ReportType reportType) {
        List<String> msgKeys = reportHeaderMsgKeys.get(reportType);
        if (msgKeys == null) {
            return;
        }
        int cellNum = 0;
        for (String msgKey : msgKeys) {
            setCellValueTruncateIfNeeded(row.createCell(cellNum), MessageUtil.getMessage(msgKey), LOG);
            cellNum++;
        }
    }

    @Override
    public NodeRef completeReportResult(ReportDataCollector reportDataProvider) {
        NodeRef reportResultNodeRef = reportDataProvider.getReportResultNodeRef();
        Workbook workbook = reportDataProvider.getWorkbook();
        // workbook may be null if creating report failed or was cancelled by user. Status check is performed in ExecuteReportsJob.
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
        }
        Map<QName, Serializable> reportResultProps = new HashMap<QName, Serializable>();
        reportResultProps.put(ReportModel.Props.STATUS, reportDataProvider.getResultStatus().toString());
        reportResultProps.put(ReportModel.Props.RUN_FINISH_START_TIME, new Date());
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
        List<NodeRef> reportRefs = getReportsInStatus(ReportStatus.IN_QUEUE);
        List<Node> reports = new ArrayList<Node>();
        for (NodeRef reportRef : reportRefs) {
            Node report = new Node(reportRef);
            report.getProperties();
            reports.add(report);
        }
        return reports;
    }

    private List<NodeRef> getReportsInStatus(ReportStatus requiredStatus) {
        Assert.notNull(requiredStatus, "Status cannot be null.");
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(getReportsSpaceRef(), Collections.singleton(ReportModel.Types.REPORT_RESULT));
        List<NodeRef> reportRefs = new ArrayList<NodeRef>();
        for (ChildAssociationRef childAssoc : childAssocs) {
            NodeRef reportRef = childAssoc.getChildRef();
            String statusStr = (String) nodeService.getProperty(reportRef, ReportModel.Props.STATUS);
            if (StringUtils.isNotBlank(statusStr) && requiredStatus.equals(ReportStatus.valueOf(statusStr))) {
                reportRefs.add(reportRef);
            }
        }
        return reportRefs;
    }

    @Override
    public void markReportRunning(NodeRef reportRef) {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(ReportModel.Props.RUN_START_DATE_TIME, new Date());
        props.put(ReportModel.Props.STATUS, ReportStatus.RUNNING.toString());
        nodeService.addProperties(reportRef, props);
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

}
