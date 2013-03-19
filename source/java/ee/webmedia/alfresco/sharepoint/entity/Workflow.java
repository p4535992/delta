package ee.webmedia.alfresco.sharepoint.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.sharepoint.entity.mapper.WorkflowMapper;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;
import ee.webmedia.alfresco.workflow.model.WorkflowSpecificModel;

public class Workflow implements Comparable<Workflow> {

    public static final WorkflowMapper MAPPER = new WorkflowMapper();

    private Integer orderNo;
    private String type;
    private Date startedDateTime;
    private String creatorName;
    private String status;

    @Override
    public int compareTo(Workflow o) {
        return orderNo.compareTo(o.orderNo);
    }

    public boolean isAssignment() {
        return "assignment_workflow".equals(type);
    }

    public boolean isConfirmation() {
        return "confirmation_workflow".equals(type);
    }

    public QName getNodeType() {
        if (isAssignment()) {
            return WorkflowSpecificModel.Types.ASSIGNMENT_WORKFLOW;
        } else if (isConfirmation()) {
            return WorkflowSpecificModel.Types.CONFIRMATION_WORKFLOW;
        }
        throw new RuntimeException("Cannot return node type for type: [" + type + "].");
    }

    public void writePropsTo(Map<QName, Serializable> props) {
        props.clear();
        props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, startedDateTime);
        props.put(WorkflowCommonModel.Props.CREATOR_NAME, creatorName);
        props.put(WorkflowCommonModel.Props.STATUS, status);
        props.put(WorkflowCommonModel.Props.MANDATORY, false);
        props.put(WorkflowCommonModel.Props.STOP_ON_FINISH, false);

        if (isAssignment()) {
            props.put(WorkflowCommonModel.Props.PARALLEL_TASKS, true);
        } else if (isConfirmation()) {
            props.put(WorkflowCommonModel.Props.PARALLEL_TASKS, false);
        }
    }

    public Integer getOrderNo() {
        return orderNo;
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

    public void setStartedDateTime(Date startedDateTime) {
        this.startedDateTime = startedDateTime;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
