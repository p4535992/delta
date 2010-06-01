package ee.webmedia.alfresco.workflow.model;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * @author Alar Kvell
 */
public class TaskAndDocument implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String STYLECLASS_TASK_OVERDUE = "taskOverdue";

    private Task task;
    private Document document;

    public TaskAndDocument(Task task, Document document) {
        this.task = task;
        this.document = document;
        task.setCssStyleClass(getCssStyleClass(document.getCssStyleClass(), task.getCompletedDateTime(), task.getDueDate()));
    }

    public static String getCssStyleClass(String docStyleClass, final Date completedDate, final Date dueDate) {
        final String cssStyleClass;
        if (completedDate == null) {
            final Date now = new Date();
            if (dueDate != null && dueDate.before(now) && !DateUtils.isSameDay(dueDate, now)) {
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

    @Override
    public String toString() {
        return "TaskAndDocument [document=" + document + ", task=" + task + "]";
    }

}
