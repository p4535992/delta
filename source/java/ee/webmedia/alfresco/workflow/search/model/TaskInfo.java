package ee.webmedia.alfresco.workflow.search.model;

import static ee.webmedia.alfresco.workflow.service.Task.dateFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.CssStylable;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.CreatedAndRegistered;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.type.web.DocumentTypeConverter;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.model.TaskAndDocument;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

/**
 * @author Erko Hansar
 */
public class TaskInfo implements Serializable, Comparable<TaskInfo>, CssStylable, CreatedAndRegistered {

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

    public String getDocType() {
        DocumentTypeConverter docTypeConverter = new DocumentTypeConverter();
        String docTypeId = (String) document.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
        return docTypeConverter.convertSelectedValueToString(docTypeId);
    }

    public String getRegNum() {
        return (String) document.getProperties().get(DocumentCommonModel.Props.REG_NUMBER);
    }

    public Date getRegDate() {
        return (Date) document.getProperties().get(DocumentCommonModel.Props.REG_DATE_TIME);
    }

    public String getDocName() {
        return (String) document.getProperties().get(DocumentCommonModel.Props.DOC_NAME);
    }

    public String getCreatorName() {
        return (String) task.getProperties().get(WorkflowCommonModel.Props.CREATOR_NAME);
    }

    public Date getStartedDate() {
        return (Date) task.getProperties().get(WorkflowCommonModel.Props.STARTED_DATE_TIME);
    }

    public String getOwnerName() {
        return (String) task.getProperties().get(WorkflowCommonModel.Props.OWNER_NAME);
    }

    public String getOwnerOrganizationName() {
        return UserUtil.getDisplayUnit((List<String>) task.getProperties().get(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME));
    }

    public String getOwnerJobTitle() {
        return (String) task.getProperties().get(WorkflowCommonModel.Props.OWNER_JOB_TITLE);
    }

    public String getTaskTypeText() {
        return MessageUtil.getMessage(workflow.getType().getLocalName());
    }

    public Date getDueDate() {
        return (Date) task.getProperties().get(WorkflowSpecificModel.Props.DUE_DATE);
    }

    public Date getCompletedDate() {
        return (Date) task.getProperties().get(WorkflowCommonModel.Props.COMPLETED_DATE_TIME);
    }

    @Override
    public Date getRegDateTime() {
        return getRegDate();
    }

    public String getRegDateStr() {
        return getRegDateTime() != null ? dateFormat.format(getRegDateTime()) : "";
    }

    public String getStartedDateStr() {
        return getStartedDate() != null ? dateFormat.format(getStartedDate()) : "";
    }

    public String getDueDateStr() {
        return getDueDate() != null ? dateFormat.format(getDueDate()) : "";
    }

    public String getCompletedDateStr() {
        return getCompletedDate() != null ? dateFormat.format(getCompletedDate()) : "";
    }

    public String getStoppedDateStr() {
        return getStoppedDate() != null ? dateFormat.format(getStoppedDate()) : "";
    }

    @Override
    public Date getCreated() {
        return (Date) document.getProperties().get(ContentModel.PROP_CREATED);
    }

    public String getComment() {
        if (task.getType().equals(WorkflowSpecificModel.Types.REVIEW_TASK) || task.getType().equals(WorkflowSpecificModel.Types.OPINION_TASK)) {
            return (String) task.getProperties().get(WorkflowCommonModel.Props.OUTCOME);
        } else {
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

    public Date getStoppedDate() {
        return (Date) task.getProperties().get(WorkflowCommonModel.Props.STOPPED_DATE_TIME);
    }

    public String getResolution() {
        if (task.getType().equals(WorkflowSpecificModel.Types.ASSIGNMENT_TASK)) {
            return (String) task.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
        } else {
            return (String) workflow.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
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

    public String getStatus() {
        return (String) task.getProperties().get(WorkflowCommonModel.Props.STATUS);
    }

    @Override
    public int compareTo(TaskInfo taskInfo) {
        String ownerName = getOwnerName();
        if (StringUtils.isNotBlank(ownerName)) {
            if (StringUtils.isBlank(taskInfo.getOwnerName())) {
                return -1;
            }
            return AppConstants.DEFAULT_COLLATOR.compare(ownerName, taskInfo.getOwnerName());
        }
        return 0;
    }

    @Override
    public String getCssStyleClass() {
        if (cssStyleClass == null) {
            final Date dueDate = (Date) task.getProperties().get(WorkflowSpecificModel.Props.DUE_DATE);
            final Date completedDate = (Date) task.getProperties().get(WorkflowCommonModel.Props.COMPLETED_DATE_TIME);
            final String docStyleClass = document.getType().getLocalName();
            cssStyleClass = TaskAndDocument.getCssStyleClass(docStyleClass, completedDate, dueDate);
        }
        return cssStyleClass;
    }

}