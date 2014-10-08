package ee.webmedia.alfresco.workflow.model;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getCompoundWorkflowDocMsg;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;

<<<<<<< HEAD
/**
 * @author Alar Kvell
 */
=======
>>>>>>> develop-5.1
public class TaskAndDocument implements Serializable {
    private static final String MSG_CASE_FILE_TITLE = MessageUtil.getMessage("compoundWorkflow_caseFile_title");
    private static final long serialVersionUID = 1L;
    public static final String STYLECLASS_TASK_OVERDUE = "taskOverdue";
    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd.MM.yyyy");

    private final Task task;
    private final Document document;
    private final CompoundWorkflow compoundWorkflow;
    private String title;
    private Date workflowDueDate;
    private String workflowDueDateStr;
    private String volumeMark;

    public TaskAndDocument(Task task, Document document, CompoundWorkflow compoundWorkflow) {
        this.task = task;
        this.document = document;
        this.compoundWorkflow = compoundWorkflow;
        task.setCssStyleClass(getCssStyleClass(document != null ? document.getCssStyleClass() : Document.GENERIC_DOCUMENT_STYLECLASS,
                task.getCompletedDateTime(),
                task.getDueDate()));
    }

    public static String getCssStyleClass(String docStyleClass, final Date completedDate, final Date dueDate) {
        final String cssStyleClass;
        if (completedDate == null) {
            final Date now = new Date();
<<<<<<< HEAD
            if (dueDate != null && dueDate.before(now) && !DateUtils.isSameDay(dueDate, now)) {
=======
            if (dueDate != null && dueDate.before(now)) {
>>>>>>> develop-5.1
                cssStyleClass = STYLECLASS_TASK_OVERDUE;
            } else {
                cssStyleClass = docStyleClass;
            }
        } else {
            cssStyleClass = docStyleClass;
        }
        return cssStyleClass;
    }

    public Task getTask() {
        return task;
    }

    public Document getDocument() {
        return document;
    }

    // methods for sortLink tag in jsp
    public String getDocName() {
        return hasDocument() ? document.getDocName() : ((compoundWorkflow != null && compoundWorkflow.isIndependentWorkflow()) ? compoundWorkflow.getTitle() : "");
    }

    public String getTitle() {
        if (title == null && hasDocument()) {
            title = (String) document.getProperties().get(DocumentDynamicModel.Props.DOC_TITLE);
        }
        return title;
    }

    public Date getTaskDueDate() {
        return task.getDueDate();
    }

    public String getResolution() {
        return task.getResolution();
    }

    public String getCreatorName() {
        return task.getCreatorName();
    }

    public String getRegNumber() {
        return hasDocument() ? document.getRegNumber() : "";
    }

    public String getVolumeMark() {
        if (hasDocument() && volumeMark == null) {
            volumeMark = (String) document.getProperties().get(VolumeModel.Props.VOLUME_MARK);
        }
        return volumeMark;
    }

    public Date getRegDateTime() {
        return hasDocument() ? document.getRegDateTime() : null;
    }

    public String getRegDateTimeStr() {
        return hasDocument() ? document.getRegDateTimeStr() : "";
    }

    public String getSender() {
<<<<<<< HEAD
        return hasDocument() ? document.getSender() : "";
=======
        return hasDocument() ? document.getSenderOrOwner() : "";
>>>>>>> develop-5.1
    }

    public Date getDocumentDueDate() {
        return hasDocument() ? document.getDueDate() : null;
    }

    public String getDocumentDueDateStr() {
        return hasDocument() ? document.getDueDateStr() : "";
    }

    public Date getWorkflowDueDate() {
        if (hasDocument() && workflowDueDate == null) {
            workflowDueDate = (Date) document.getProperties().get(DocumentDynamicModel.Props.WORKFLOW_DUE_DATE);
        }
        return workflowDueDate;
    }

    public String getWorkflowDueDateStr() {
        if (hasDocument() && workflowDueDateStr == null) {
            Date dueDate = getWorkflowDueDate();
            if (dueDate != null) {
                workflowDueDateStr = dateFormat.format(getWorkflowDueDate());
            }
        }
        return workflowDueDateStr;
    }

    private boolean hasDocument() {
        return document != null;
    }

    public CompoundWorkflow getCompoundWorkflow() {
        return compoundWorkflow;
    }

    public String getDocumentTypeName() {
        String typeName;
        if (hasDocument()) {
            if (compoundWorkflow.isCaseFileWorkflow()) {
                typeName = MessageUtil.getMessage("casefile_workflow_type_name");
            } else {
                typeName = document.getDocumentTypeName();
            }
        } else if (compoundWorkflow != null && compoundWorkflow.isIndependentWorkflow()) {
            typeName = getCompoundWorkflowDocMsg(compoundWorkflow.getNumberOfDocuments(), MessageUtil.getMessage("compound_workflow_independent_no_documents"));
        } else {
            typeName = MSG_CASE_FILE_TITLE;
        }
        return typeName;
    }

    @Override
    public String toString() {
        return "TaskAndDocument [document=" + document + ", task=" + task + ", compoundWorkflow=" + compoundWorkflow + "]";
    }

}
