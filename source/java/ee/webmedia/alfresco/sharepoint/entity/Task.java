package ee.webmedia.alfresco.sharepoint.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.sharepoint.entity.mapper.TaskMapper;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

public class Task implements Comparable<Task> {

    public static final TaskMapper MAPPER = new TaskMapper();

    private Integer orderNo;
    private String type;
    private boolean responsible;
    private String ownerName;
    private String ownerId;
    private String ownerEmail;
    private String creatorName;
    private String creatorId;
    private String creatorEmail;
    private String resolution;
    private Date startedDateTime;
    private Date dueDate;
    private Date completedDateTime;
    private String status;
    private String outcome;
    private String comment;
    private String ownerJobTitle;
    private String ownerOrganization;
    private Integer workflowOrder;

    /**
     * Compare orders tasks by type and then by order number. Type may be <code>null</code> but order number not.
     */
    @Override
    public int compareTo(Task o) {
        int r = 0;

        if ((type == null) != (o.type == null)) {
            r = type == null ? 1 : -1;
        } else {
            r = type.compareTo(o.type);
            if (r == 0) {
                r = orderNo.compareTo(o.orderNo);
            }
        }

        return r;
    }

    public void writePropsTo(Map<QName, Serializable> props) {
        props.clear();
        props.put(WorkflowCommonModel.Props.OWNER_NAME, ownerName);
        props.put(WorkflowCommonModel.Props.OWNER_ID, ownerId);
        props.put(WorkflowCommonModel.Props.OWNER_EMAIL, ownerEmail);
        props.put(WorkflowCommonModel.Props.CREATOR_NAME, creatorName);
        props.put(WorkflowSpecificModel.Props.CREATOR_ID, creatorId);
        props.put(WorkflowSpecificModel.Props.CREATOR_EMAIL, creatorEmail);
        props.put(WorkflowSpecificModel.Props.RESOLUTION, resolution);
        props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, startedDateTime);
        props.put(WorkflowSpecificModel.Props.DUE_DATE, dueDate);
        props.put(WorkflowCommonModel.Props.COMPLETED_DATE_TIME, completedDateTime);
        props.put(WorkflowCommonModel.Props.STATUS, status);
        props.put(WorkflowCommonModel.Props.OUTCOME, outcome);
        props.put(WorkflowSpecificModel.Props.COMMENT, comment);
        props.put(WorkflowCommonModel.Props.OWNER_JOB_TITLE, ownerJobTitle);
        props.put(WorkflowCommonModel.Props.OWNER_ORGANIZATION_NAME, ownerOrganization);
    }

    public boolean isAssignment() {
        return "assignment_task".equals(type);
    }

    public boolean isConfirmation() {
        return "confirmation_task".equals(type);
    }

    public QName getNodeType() {
        if (isAssignment()) {
            return WorkflowSpecificModel.Types.ASSIGNMENT_TASK;
        } else if (isConfirmation()) {
            return WorkflowSpecificModel.Types.CONFIRMATION_TASK;
        }
        throw new RuntimeException("Cannot return node type for type: [" + type + "].");
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isResponsible() {
        return responsible;
    }

    public void setResponsible(boolean responsible) {
        this.responsible = responsible;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public void setCreatorEmail(String creatorEmail) {
        this.creatorEmail = creatorEmail;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public void setStartedDateTime(Date startedDateTime) {
        this.startedDateTime = startedDateTime;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public void setCompletedDateTime(Date completedDateTime) {
        this.completedDateTime = completedDateTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setOwnerJobTitle(String ownerJobTitle) {
        this.ownerJobTitle = ownerJobTitle;
    }

    public void setOwnerOrganization(String ownerOrganization) {
        this.ownerOrganization = ownerOrganization;
    }

    public Integer getWorkflowOrder() {
        return workflowOrder;
    }

    public void setWorkflowOrder(Integer workflowOrder) {
        this.workflowOrder = workflowOrder;
    }
}
