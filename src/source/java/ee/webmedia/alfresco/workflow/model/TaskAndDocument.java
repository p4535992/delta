package ee.webmedia.alfresco.workflow.model;

import java.io.Serializable;

import ee.webmedia.alfresco.document.model.Document;
import ee.webmedia.alfresco.workflow.service.Task;

/**
 * @author Alar Kvell
 */
public class TaskAndDocument implements Serializable {
    private static final long serialVersionUID = 1L;

    private Task task;
    private Document document;

    public TaskAndDocument(Task task, Document document) {
        this.task = task;
        this.document = document;
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
