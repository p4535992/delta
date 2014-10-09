package ee.webmedia.alfresco.workflow.search.model;

import static ee.webmedia.alfresco.common.web.BeanHelper.getApplicationConstantsBean;
import static ee.webmedia.alfresco.common.web.BeanHelper.getDocumentAdminService;
import static ee.webmedia.alfresco.common.web.BeanHelper.getWorkflowConstantsBean;
import static ee.webmedia.alfresco.workflow.service.Task.dateFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.common.web.CssStylable;
import ee.webmedia.alfresco.docadmin.model.DocumentAdminModel;
import ee.webmedia.alfresco.document.model.CreatedAndRegistered;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.utils.UserUtil;
import ee.webmedia.alfresco.workflow.model.CompoundWorkflowType;
import ee.webmedia.alfresco.workflow.model.TaskAndDocument;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;
import ee.webmedia.alfresco.workflow.service.CompoundWorkflow;
import ee.webmedia.alfresco.workflow.service.Task;
import ee.webmedia.alfresco.workflow.service.WorkflowConstantsBean;

public class TaskInfo implements Serializable, Comparable<TaskInfo>, CssStylable, CreatedAndRegistered {

    private static final long serialVersionUID = 1L;

    private Node task;
    private Node document;
    private CompoundWorkflow compoundWorkflow;
    private boolean documentWorkflow;
    private boolean independentWorkflow;
    private boolean caseFileWorkflow;
    private boolean linkedReviewTask;
    private String cssStyleClass;
    private String taskTypeText;
    private String documentTypeName;
    private String docName;
    private String overdue;
    private String responsible;
    private String ownerOrganizationName;

    private Date documentCreated;
    private String comment;
    private String workflowResolution;
    private String compoundWorkflowTypeStr;
    private String compoundWorkflowOwnerOrganizationPath;

    private String compoundWorkflowDocumentsCount;

    public TaskInfo() {
    }

    public TaskInfo(Node task, String workflowResolution, CompoundWorkflow compoundWorkflow, Node taskDocument, Integer compoundWorkflowDocumentsCount) {
        this.task = task;
        linkedReviewTask = WorkflowSpecificModel.Types.LINKED_REVIEW_TASK.equals(task.getType());
        setResolution(workflowResolution);
        this.compoundWorkflow = compoundWorkflow;
        this.compoundWorkflowDocumentsCount = compoundWorkflowDocumentsCount != null ? compoundWorkflowDocumentsCount.toString() : "0";
        if (compoundWorkflow != null) {
            CompoundWorkflowType type = compoundWorkflow.getTypeEnum();
            if (type != null) {
                documentWorkflow = CompoundWorkflowType.DOCUMENT_WORKFLOW == type;
                independentWorkflow = CompoundWorkflowType.INDEPENDENT_WORKFLOW == type;
                caseFileWorkflow = CompoundWorkflowType.CASE_FILE_WORKFLOW == type;
                compoundWorkflowTypeStr = BeanHelper.getWorkflowConstantsBean().getCompoundWorkflowTypeMessage(type);
            }
        }
        document = taskDocument;
        if (document != null) {
            setDocType(document);
            Map<String, Object> docProps = document.getProperties();
            setDocName(document, task, (independentWorkflow || caseFileWorkflow), documentWorkflow, linkedReviewTask);
            documentCreated = (Date) docProps.get(ContentModel.PROP_CREATED);
        }
        setCssStyleClass(document);
    }

    public Node getTask() {
        return task;
    }

    public void setTask(Node task) {
        this.task = task;
    }

    public NodeRef getDocumentNodeRef() {
        return document.getNodeRef();
    }

    // LIST FIELD GETTERS:

    private void setDocType(Node document) {
        String documentTypeId = (String) document.getProperties().get(DocumentAdminModel.Props.OBJECT_TYPE_ID);
        documentTypeName = getDocumentAdminService().getDocumentTypeName(documentTypeId);
        if (StringUtils.isBlank(documentTypeName)) {
            documentTypeName = "";
        }
    }

    public String getDocType() {
        return documentTypeName;
    }

    public void setDocType(String documentTypeName) {
        this.documentTypeName = documentTypeName;
    }

    public String getRegNum() {
        return document != null ? (String) document.getProperties().get(DocumentCommonModel.Props.REG_NUMBER) : "";
    }

    public Date getRegDate() {
        return document != null ? (Date) document.getProperties().get(DocumentCommonModel.Props.REG_DATE_TIME) : null;
    }

    private void setDocName(Node document, Node task, boolean independentOrCaseFileWorkflow, boolean documentWorkflow, boolean linkedReviewTask) {
        if (!linkedReviewTask && documentWorkflow) {
            docName = (String) document.getProperties().get(DocumentCommonModel.Props.DOC_NAME);
        } else if (linkedReviewTask) {
            docName = (String) task.getProperties().get(WorkflowSpecificModel.Props.COMPOUND_WORKFLOW_TITLE);
        } else if (independentOrCaseFileWorkflow) {
            docName = compoundWorkflow.getTitle();
        } else {
            docName = ""; // If we reach here, then bean/JSP usage must be revised.
        }
    }

    public String getDocName() {
        return docName;
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
        if (ownerOrganizationName == null) {
            ownerOrganizationName = UserUtil.getDisplayUnit((List<String>) task.getProperties().get(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME));
        }
        return ownerOrganizationName;
    }

    public void setOwnerOrganizationName(String ownerOrganizationName) {
        this.ownerOrganizationName = ownerOrganizationName;
    }

    public String getOwnerJobTitle() {
        return (String) task.getProperties().get(WorkflowCommonModel.Props.OWNER_JOB_TITLE);
    }

    public String getTaskTypeText() {
        if (taskTypeText == null) {
            WorkflowConstantsBean workflowConstantsBean = getWorkflowConstantsBean();
            taskTypeText = workflowConstantsBean.getWorkflowTypeNameByTaskType(task.getType());
            if (taskTypeText == null) {
                taskTypeText = workflowConstantsBean.getTaskTypeName(task.getType());
            }
        }
        return taskTypeText;
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
        return documentCreated;
    }

    public String getComment() {
        if (comment == null) {
            if (task.getType().equals(WorkflowSpecificModel.Types.REVIEW_TASK) || task.getType().equals(WorkflowSpecificModel.Types.OPINION_TASK)
                    || linkedReviewTask) {
                return (String) task.getProperties().get(WorkflowCommonModel.Props.OUTCOME);
            }

            String outcome = (String) task.getProperties().get(WorkflowCommonModel.Props.OUTCOME);
            if (StringUtils.isBlank(outcome)) {
                outcome = "";
            }
            comment = (String) task.getProperties().get(WorkflowSpecificModel.Props.COMMENT);
            if (StringUtils.isBlank(comment)) {
                comment = "";
            }
            if (StringUtils.isBlank(outcome) && StringUtils.isBlank(comment)) {
                comment = null;
            } else {
                comment = outcome + ": " + comment;
            }
        }
        return comment;
    }

    public String getResponsible() {
        if (responsible == null) {
            responsible = task.hasAspect(WorkflowSpecificModel.Aspects.RESPONSIBLE) ? getApplicationConstantsBean().getMessageYes() : getApplicationConstantsBean()
                    .getMessageNo();
        }
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public Date getStoppedDate() {
        return (Date) task.getProperties().get(WorkflowCommonModel.Props.STOPPED_DATE_TIME);
    }

    private void setResolution(String workflowResolution) {
        if (WorkflowSpecificModel.Types.DUE_DATE_EXTENSION_TASK.equals(task.getType())) {
            Date proposedDueDate = (Date) task.getProperties().get(WorkflowSpecificModel.Props.PROPOSED_DUE_DATE);
            String proposedDueDateStr = proposedDueDate != null ? Task.dateFormat.format(proposedDueDate) : "";
            this.workflowResolution = MessageUtil.getMessage("task_search_due_date_extension_task_resolution", proposedDueDateStr, workflowResolution);
        } else {
            String taskResolution = (String) task.getProperties().get(WorkflowSpecificModel.Props.RESOLUTION);
            if (StringUtils.isNotBlank(taskResolution)) {
                this.workflowResolution = taskResolution;
            } else {
                this.workflowResolution = "";
            }
        }
    }

    public String getResolution() {
        return workflowResolution;
    }

    public void setOverdue(String overdue) {
        this.overdue = overdue;
    }

    public String getOverdue() {
        if (overdue == null) {
            Object completedDate = getCompletedDate();
            Object dueDate = getDueDate();
            if (completedDate != null && dueDate != null) {
                overdue = ((Date) completedDate).after((Date) dueDate) ? getApplicationConstantsBean().getMessageYes() : getApplicationConstantsBean().getMessageNo();
            }
            overdue = getApplicationConstantsBean().getMessageNo();
        }
        return overdue;
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
            return AppConstants.getNewCollatorInstance().compare(ownerName, taskInfo.getOwnerName());
        }
        return 0;
    }

    private void setCssStyleClass(Node document) {
        final Date dueDate = (Date) task.getProperties().get(WorkflowSpecificModel.Props.DUE_DATE);
        final Date completedDate = (Date) task.getProperties().get(WorkflowCommonModel.Props.COMPLETED_DATE_TIME);
        final String docStyleClass = document == null ? null : document.getType().getLocalName();
        cssStyleClass = TaskAndDocument.getCssStyleClass(docStyleClass, completedDate, dueDate);
    }

    @Override
    public String getCssStyleClass() {
        return cssStyleClass;
    }

    public NodeRef getCompoundWorkflowNodeRef() {
        return compoundWorkflow.getNodeRef();
    }

    public String getCompoundWorkflowType() {
        return getWorkflowTypeString();
    }

    public String getWorkflowTypeString() {
        return compoundWorkflowTypeStr;
    }

    public String getCompoundWorkflowTitle() {
        return hasCompoundWorkflow() ? (String) compoundWorkflow.getTitle() : "";
    }

    public String getCompoundWorkflowOwnerName() {
        return hasCompoundWorkflow() ? (String) compoundWorkflow.getOwnerName() : "";
    }

    public String getCompoundWorkflowOwnerOrganizationPath() {
        if (compoundWorkflowOwnerOrganizationPath == null) {
            compoundWorkflowOwnerOrganizationPath = hasCompoundWorkflow() ? compoundWorkflow.getOwnerStructUnit() : "";
        }
        return compoundWorkflowOwnerOrganizationPath;
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

    public String getCompoundWorkflowStatus() {
        return hasCompoundWorkflow() ? compoundWorkflow.getStatus() : "";
    }

    public String getCompoundWorkflowDocumentCount() {
        return compoundWorkflowDocumentsCount;
    }

    private boolean hasCompoundWorkflow() {
        return compoundWorkflow != null;
    }

    public String getOriginalTaskObjectUrl() {
        return (String) task.getProperties().get(WorkflowSpecificModel.Props.ORIGINAL_TASK_OBJECT_URL);
    }

    public boolean isDocumentWorkflow() {
        return documentWorkflow;
    }

    public boolean isIndependentWorkflow() {
        return independentWorkflow;
    }

    public boolean isCaseFileWorkflow() {
        return caseFileWorkflow;
    }

    public boolean isLinkedReviewTask() {
        return linkedReviewTask;
    }

}
