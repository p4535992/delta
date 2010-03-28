package ee.webmedia.alfresco.workflow.search.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import ee.webmedia.alfresco.common.web.CssStylable;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.model.TaskAndDocument;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * @author Erko Hansar
 */
public class TaskInfo implements Serializable, Comparable<TaskInfo>, CssStylable {

    private static final long serialVersionUID = 1L;
    
    private Node task;
    private Node workflow;
    private Node document;
    private String cssStyleClass;
    
    public TaskInfo() {
    }

    public TaskInfo(Node task, Node workflow, Node document) {
        this.task = task;
        this.workflow = workflow;
        this.document = document;
    }

    public Node getTask() {
        return task;
    }

    public void setTask(Node task) {
        this.task = task;
    }

    public Node getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Node workflow) {
        this.workflow = workflow;
    }

    public Node getDocument() {
        return document;
    }

    public void setDocument(Node document) {
        this.document = document;
    }

    // LIST FIELD GETTERS:
    
    public Object getRegNum() {
        return document.getProperties().get(DocumentCommonModel.Props.REG_NUMBER);
    }

    public Object getRegDate() {
        return document.getProperties().get(DocumentCommonModel.Props.REG_DATE_TIME);
    }

    public Object getDocName() {
        return document.getProperties().get(DocumentCommonModel.Props.DOC_NAME);
    }

    public Object getCreatorName() {
        return task.getProperties().get(WorkflowCommonModel.Props.CREATOR_NAME);
    }

    public Object getStartedDate() {
        return task.getProperties().get(WorkflowCommonModel.Props.STARTED_DATE_TIME);
    }

    public Object getOwnerName() {
        return task.getProperties().get(WorkflowCommonModel.Props.OWNER_NAME);
    }

    public Object getOwnerOrganizationName() {
        return task.getProperties().get(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME);
    }

    public Object getOwnerJobTitle() {
        return task.getProperties().get(WorkflowCommonModel.Props.OWNER_JOB_TITLE);
    }

    public String getTaskTypeText() {
        return MessageUtil.getMessage(workflow.getType().getLocalName());
    }
    
    public Object getDueDate() {
        return task.getProperties().get(WorkflowSpecificModel.Props.DUE_DATE);
    }

    public Object getCompletedDate() {
        return task.getProperties().get(WorkflowCommonModel.Props.COMPLETED_DATE_TIME);
    }

    public Object getComment() {
        if (task.getType().equals(WorkflowSpecificModel.Types.REVIEW_TASK) || task.getType().equals(WorkflowSpecificModel.Types.OPINION_TASK)) {
            return task.getProperties().get(WorkflowCommonModel.Props.OUTCOME);
        }
        else {
            String outcome = (String) task.getProperties().get(WorkflowCommonModel.Props.OUTCOME);
            if (StringUtils.isBlank(outcome)) {
                outcome = "";
            }
            String comment = (String) task.getProperties().get(WorkflowSpecificModel.Props.COMMENT);
            if (StringUtils.isBlank(comment)) {
                comment = "";
            }
            if (StringUtils.isBlank(outcome) && StringUtils.isBlank(comment)) {
                return null;
            }
            return outcome + ": " + comment;
        }
    }

    public String getResponsible() {
        return MessageUtil.getMessage(task.hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE) ? "yes" : "no");
    }

    public Object getStoppedDate() {
        return task.getProperties().get(WorkflowCommonModel.Props.STOPPED_DATE_TIME);
    }
    
    public Object getResolution() {
        if (task.getType().equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
            return task.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
        }
        else {
            return workflow.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
        }
    }
    
    public String getOverdue() {
        Object completedDate = getCompletedDate();
        Object dueDate = getDueDate();
        if (completedDate != null && dueDate != null) {
            return MessageUtil.getMessage(((Date) completedDate).after((Date) dueDate) ? "yes" : "no");
        }
        return MessageUtil.getMessage("no");
    }
    
    public Object getStatus() {
        return task.getProperties().get(WorkflowCommonModel.Props.STATUS);
    }

    @Override
    public int compareTo(TaskInfo taskInfo) {
        String ownerName = (String) getOwnerName();
        if (StringUtils.isNotBlank(ownerName)) {
            if (StringUtils.isBlank((String) taskInfo.getOwnerName())) {
                return -1;
            }
            return ownerName.compareTo((String) taskInfo.getOwnerName());
        }
        return 0;
    }

    @Override
    public String getCssStyleClass() {
        if(cssStyleClass == null) {
            final Date dueDate = (Date) task.getProperties().get(WorkflowSpecificModel.Props.DUE_DATE);
            final Date completedDate = (Date) task.getProperties().get(WorkflowCommonModel.Props.COMPLETED_DATE_TIME);
            final String docStyleClass = document.getType().getLocalName();
            cssStyleClass = TaskAndDocument.getCssStyleClass(docStyleClass, completedDate, dueDate);
        }
        return cssStyleClass;
    }
    
}
