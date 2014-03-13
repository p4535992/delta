package ee.webmedia.alfresco.workflow.model;

import java.io.Serializable;
import java.util.Date;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.workflow.service.Task;

public class TaskAndDocument implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String STYLECLASS_TASK_OVERDUE = "taskOverdue";

    private final Task task;
    private final Document document;

    public TaskAndDocument(Task task, Document document) {
        this.task = task;
        this.document = document;
        task.setCssStyleClass(getCssStyleClass(document.getCssStyleClass(), task.getCompletedDateTime(), task.getDueDate()));
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
        return document.getDocName();
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
        return document.getRegNumber();
    }

    public Date getRegDateTime() {
        return document.getRegDateTime();
    }

    public String getSender() {
        return document.getSender();
    }

    public Date getDocumentDueDate() {
        return document.getDueDate();
    }

    public String getDocumentTypeName() {
        return document.getDocumentTypeName();
    }

    @Override
    public String toString() {
        return "TaskAndDocument [document=" + document + ", task=" + task + "]";
    }

}
