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
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
<<<<<<< HEAD

/**
 * @author Erko Hansar
 */
=======
import ee.webmedia.alfresco.workflow.service.Task;

>>>>>>> develop-5.1
public class TaskInfo implements Serializable, Comparable<TaskInfo>, CssStylable, CreatedAndRegistered {

    private static final long serialVersionUID = 1L;

    private Node task;
    private Node workflow;
    private Node document;
    private CompoundWorkflow compoundWorkflow;
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
        if (document == null) {
            return null;
        }
        DocumentTypeConverter docTypeConverter = new DocumentTypeConverter();
        String docTypeId = (String) document.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
        return docTypeConverter.convertSelectedValueToString(docTypeId);
    }

    public String getRegNum() {
        return document == null ? null : (String) document.getProperties().get(DocumentCommonModel.Props.REG_NUMBER);
    }

    public Date getRegDate() {
        return (document == null) ? null : (Date) document.getProperties().get(DocumentCommonModel.Props.REG_DATE_TIME);
    }

    /**
     * NB! This method is used for JSP sorting and returns values from different objects.
<<<<<<< HEAD
=======
     * 
>>>>>>> develop-5.1
     * @return document title or compound workflow title or empty string
     */
    public String getDocName() {
        if (!isLinkedReviewTask() && getCompoundWorkflow().isDocumentWorkflow()) {
            return (String) document.getProperties().get(DocumentCommonModel.Props.DOC_NAME);
        } else if (isLinkedReviewTask()) {
            return (String) task.getProperties().get(WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_TITLE);
        } else if (hasCompoundWorkflow() && (getCompoundWorkflow().isIndependentWorkflow() || getCompoundWorkflow().isCaseFileWorkflow())) {
            return getCompoundWorkflowTitle();
        }

        return ""; // If we reach here, then bean/JSP usage must be revised.
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
        return workflow == null ? MessageUtil.getMessage(task.getType().getLocalName()) : MessageUtil.getMessage(workflow.getType().getLocalName());
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
        return (getRegDateTime() != null) ? dateFormat.format(getRegDateTime()) : "";
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
        return document == null ? null : (Date) document.getProperties().get(ContentModel.PROP_CREATED);
    }

    public String getComment() {
        if (task.getType().equals(WorkflowSpecificModel.Types.REVIEW_TASK) || task.getType().equals(WorkflowSpecificModel.Types.OPINION_TASK)
                || isLinkedReviewTask()) {
            return (String) task.getProperties().get(WorkflowCommonModel.Props.OUTCOME);
        }

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

    public String getResponsible() {
        return MessageUtil.getMessage(task.hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE) ? "yes" : "no");
    }

    public Date getStoppedDate() {
        return (Date) task.getProperties().get(WorkflowCommonModel.Props.STOPPED_DATE_TIME);
    }

    public String getResolution() {
<<<<<<< HEAD
        if (WorkflowSpecificModel.Types.ASSIGNMENT_TASK.equals(task.getType())) {
            return (String) task.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
        } else if (isLinkedReviewTask()) {
            return (String) task.getProperties().get(WorkflowSpecificModel.Props.WORKFLOW_RESOLUTION);
        }
        
        return (String) workflow.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
=======
        String workflowResolution = workflow != null ? (String) workflow.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION) : "";
        if (WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK.equals(task.getType())) {
            Date proposedDueDate = (Date) task.getProperties().get(WorkflowSpecificModel.Props.PROPOSED_DUE_DATE);
            String proposedDueDateStr = proposedDueDate != null ? Task.dateFormat.format(proposedDueDate) : "";
            return MessageUtil.getMessage("task_search_due_date_extension_task_resolution", proposedDueDateStr, workflowResolution);
        }
        String taskResolution = (String) task.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
        if (StringUtils.isNotBlank(taskResolution)) {
            return taskResolution;
        }
        return workflowResolution;
>>>>>>> develop-5.1
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
            final String docStyleClass = document == null ? null : document.getType().getLocalName();
            cssStyleClass = TaskAndDocument.getCssStyleClass(docStyleClass, completedDate, dueDate);
        }
        return cssStyleClass;
    }

    public void setCompoundWorkflow(CompoundWorkflow compoundWorkflow) {
        this.compoundWorkflow = compoundWorkflow;
    }

    public CompoundWorkflow getCompoundWorkflow() {
        return compoundWorkflow;
    }

    public String getCompoundWorkflowType() {
        return hasCompoundWorkflow() ? compoundWorkflow.getWorkflowTypeString() : "";
    }

    public String getCompoundWorkflowTitle() {
        return hasCompoundWorkflow() ? compoundWorkflow.getTitle() : "";
    }

    public String getCompoundWorkflowOwnerName() {
        return hasCompoundWorkflow() ? compoundWorkflow.getOwnerName() : "";
    }

    public String getCompoundWorkflowOwnerOrganizationPath() {
        return hasCompoundWorkflow() ? compoundWorkflow.getOwnerStructUnit() : "";
    }

    public String getCompoundWorkflowOwnerJobTitle() {
        return hasCompoundWorkflow() ? compoundWorkflow.getOwnerJobTitle() : "";
    }

    public String getCompoundWorkflowCreatedDateTime() {
        return hasCompoundWorkflow() ? compoundWorkflow.getCreatedDateStr() : "";
    }

    public String getCompoundWorkflowStartedDateTime() {
        return hasCompoundWorkflow() ? compoundWorkflow.getStartedDateStr() : "";
    }

    public String getCompoundWorkflowStoppedDateTime() {
        return hasCompoundWorkflow() ? compoundWorkflow.getStoppedDateStr() : "";
    }

    public String getCompoundWorkflowFinishedDateTime() {
        return hasCompoundWorkflow() ? compoundWorkflow.getEndedDateStr() : "";
    }

<<<<<<< HEAD
    public String getCompoundWorkflowComment() {
        String linkedReviewTaskComment = isLinkedReviewTask() ? (String) task.getProperties().get(WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_COMMENT) : "";
        return hasCompoundWorkflow() ? compoundWorkflow.getComment() : StringUtils.defaultString(linkedReviewTaskComment, "");
    }

=======
>>>>>>> develop-5.1
    public String getCompoundWorkflowStatus() {
        return hasCompoundWorkflow() ? compoundWorkflow.getStatus() : "";
    }

    public String getCompoundWorkflowDocumentCount() {
        return hasCompoundWorkflow() ? compoundWorkflow.getNumberOfDocumentsStr() : "";
    }

    private boolean hasCompoundWorkflow() {
        return compoundWorkflow != null;
    }

    public String getOriginalTaskObjectUrl() {
        return (String) task.getProperties().get(WorkflowSpecificModel.Props.ORIGINAL_TASK_OBJECT_URL);
    }

    public boolean isLinkedReviewTask() {
        return WorkflowSpecificModel.Types.LINKED_REVIEW_TASK.equals(task.getType());
    }

}
