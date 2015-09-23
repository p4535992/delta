package ee.webmedia.alfresco.workflow.model;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getCompoundWorkflowDocMsg;

import java.io.Serializable;
import java.util.Date;

import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.time.FastDateFormat;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.volume.model.VolumeModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * Methods in this class must be kept in sync with the following files: </br>
 * * task-list-dialog-columns.jsp </br>
 * * task-list-dialog-min-columns.jsp </br>
 * </br>
 * Otherwise sorting of columns can have surprising effects
 */
public class TaskAndDocument implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String MSG_CASE_FILE_TITLE = MessageUtil.getMessage("compoundWorkflow_caseFile_title");
    private static final String MSG_CASE_FILE_WORKFLOW = MessageUtil.getMessage("casefile_workflow_type_name");
    private static final String MSG_INDEPENDENT_CWF_NO_DOCS = MessageUtil.getMessage("compound_workflow_independent_no_documents");
    private static final FastDateFormat SIMPLE_DATE_FORMAT = FastDateFormat.getInstance("dd.MM.yyyy");

    public static final String STYLECLASS_TASK_OVERDUE = "taskOverdue";

    private final Task task;
    private final Document document;
    private final CompoundWorkflow compoundWorkflow;
    private String title;
    private String dueDateStr;
    private String regNrOrVolumeMark;
    private String typeName;
    private NodeRef actionNodeRef;

    public TaskAndDocument(Task task, Document document, CompoundWorkflow compoundWorkflow) {
        this.task = task;
        this.document = document;
        this.compoundWorkflow = compoundWorkflow;
        task.setCssStyleClass(getCssStyleClass(document != null ? document.getCssStyleClass() : Document.GENERIC_DOCUMENT_STYLECLASS,
                task.getCompletedDateTime(),
                task.getDueDate()));
        if (compoundWorkflow != null) {
            if (compoundWorkflow.isDocumentWorkflow()) {
                actionNodeRef = document.getNodeRef();
            } else if (compoundWorkflow.isIndependentWorkflow()) {
                actionNodeRef = compoundWorkflow.getNodeRef();
            } else if (compoundWorkflow.isCaseFileWorkflow()) {
                actionNodeRef = compoundWorkflow.getParent();
            }
        }
    }

    public static String getCssStyleClass(String docStyleClass, final Date completedDate, final Date dueDate) {
        final String cssStyleClass;
        if (completedDate == null) {
            final Date now = new Date();
            if (dueDate != null && dueDate.before(now)) {
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
        return hasDocument() ? document.getDocName() : ((isIndependentWorkflow()) ? compoundWorkflow.getTitle() : "");
    }

    public String getTitle() {
        if (title == null && hasCompoundWorkflow()) {
            if (isDocumentWorkflow()) {
                title = hasDocument() ? document.getDocName() : "";
            } else if (isIndependentWorkflow()) {
                title = compoundWorkflow.getTitle();
            } else if (isCaseFileWorkflow()) {
                title = hasDocument() ? (String) document.getProperties().get(DocumentDynamicModel.Props.DOC_TITLE) : "";
            }
        }
        return title;
    }

    public String getResolution() {
        return task.getResolution();
    }

    public String getCreatorName() {
        return task.getCreatorName();
    }

    public String getRegNrOrVolumeMark() {
        if (regNrOrVolumeMark == null && hasDocument()) {
            if (isDocumentWorkflow()) {
                regNrOrVolumeMark = document.getRegNumber();
            } else if (isCaseFileWorkflow()) {
                regNrOrVolumeMark = (String) document.getProperties().get(VolumeModel.Props.VOLUME_MARK);
            } else {
                regNrOrVolumeMark = "";
            }
        }
        return regNrOrVolumeMark;
    }

    public Date getRegDateTime() {
        if (isDocumentWorkflow()) {
            return hasDocument() ? document.getRegDateTime() : null;
        }
        return null;
    }

    public String getRegDateTimeStr() {
        if (isDocumentWorkflow()) {
            return hasDocument() ? document.getRegDateTimeStr() : "";
        }
        return "";
    }

    public String getSender() {
        if (isDocumentWorkflow()) {
            return hasDocument() ? document.getSenderOrOwner() : "";
        }
        return "";
    }

    public Date getDocumentDueDate() {
        if (isDocumentWorkflow()) {
            return hasDocument() ? document.getDueDate() : null;
        } else if (isCaseFileWorkflow()) {
            return (Date) document.getProperties().get(DocumentDynamicModel.Props.WORKFLOW_DUE_DATE);
        }
        return null;
    }

    public String getDueDateStr() {
        if (dueDateStr == null && hasDocument() && hasCompoundWorkflow()) {
            if (isDocumentWorkflow()) {
                dueDateStr = document.getDueDateStr();
            } else if (isCaseFileWorkflow()) {
                Date dueDate = (Date) document.getProperties().get(DocumentDynamicModel.Props.WORKFLOW_DUE_DATE);
                if (dueDate != null) {
                    dueDateStr = SIMPLE_DATE_FORMAT.format(dueDate);
                }
            } else {
                dueDateStr = "";
            }
        }
        return dueDateStr;
    }

    public Date getTaskDueDate() {
        return task.getDueDate();
    }

    private boolean hasDocument() {
        return document != null;
    }

    private boolean hasCompoundWorkflow() {
        return compoundWorkflow != null;
    }

    private boolean isDocumentWorkflow() {
        return hasCompoundWorkflow() && compoundWorkflow.isDocumentWorkflow();
    }

    private boolean isCaseFileWorkflow() {
        return hasCompoundWorkflow() && compoundWorkflow.isCaseFileWorkflow();
    }

    private boolean isIndependentWorkflow() {
        return hasCompoundWorkflow() && compoundWorkflow.isIndependentWorkflow();
    }

    public CompoundWorkflow getCompoundWorkflow() {
        return compoundWorkflow;
    }

    public String getTypeName() {
        if (typeName == null) {
            if (hasDocument()) {
                typeName = isCaseFileWorkflow() ? MSG_CASE_FILE_WORKFLOW : document.getDocumentTypeName();
            } else if (isIndependentWorkflow()) {
                typeName = getCompoundWorkflowDocMsg(compoundWorkflow.getNumberOfDocuments(), MSG_INDEPENDENT_CWF_NO_DOCS);
            } else {
                typeName = MSG_CASE_FILE_TITLE;
            }
        }
        return typeName;
    }

    public String action() {
        if (!hasCompoundWorkflow()) {
            return "";
        }
        if (isDocumentWorkflow()) {
            return BeanHelper.getDocumentDialog().action();
        } else if (isIndependentWorkflow()) {
            return "dialog:compoundWorkflowDialog";
        } else {
            return "";
        }

    }

    public void actionListener(ActionEvent event) {
        if (!hasCompoundWorkflow()) {
            return;
        }
        if (compoundWorkflow.isDocumentWorkflow()) {
            BeanHelper.getDocumentDialog().open(event);
        } else if (compoundWorkflow.isIndependentWorkflow()) {
            BeanHelper.getCompoundWorkflowDialog().setupWorkflowFromList(event);
        } else if (compoundWorkflow.isCaseFileWorkflow()) {
            BeanHelper.getCaseFileDialog().openFromDocumentList(event);
        }
    }

    @Override
    public String toString() {
        return "TaskAndDocument [document=" + document + ", task=" + task + ", compoundWorkflow=" + compoundWorkflow + "]";
    }

    public NodeRef getActionNodeRef() {
        return actionNodeRef;
    }

}
