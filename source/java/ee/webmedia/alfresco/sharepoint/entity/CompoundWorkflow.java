package ee.webmedia.alfresco.sharepoint.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.casefile.service.CaseFile;
import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.sharepoint.entity.mapper.CompoundWorkflowMapper;
import ee.webmedia.alfresco.user.service.UserService;
import ee.webmedia.alfresco.workflow.model.WorkflowCommonModel;

public class CompoundWorkflow {

    public static final CompoundWorkflowMapper MAPPER = new CompoundWorkflowMapper();

    private Integer procedureId;
    private String type;
    private String title;
    private String comment;
    private String status;
    private String ownerName;
    private String ownerId;
    private String ownerEmail;
    private String creatorName;
    private Date createdDateTime;
    private Date startedDateTime;
    private Date finishedDateTime;
    private Integer parentId;
    private Integer originalProcedureId;

    public boolean isCaseFileWorkflow() {
        return "case_file_workflow".equals(type);
    }

    public boolean isStatusWorkInProgress() {
        return "teostamisel".equals(status);
    }

    public void writePropsTo(Map<QName, Serializable> props) {
        props.clear();
        props.put(WorkflowCommonModel.Props.PROCEDURE_ID, procedureId);
        props.put(WorkflowCommonModel.Props.TYPE, StringUtils.upperCase(type));
        props.put(WorkflowCommonModel.Props.TITLE, title);
        props.put(WorkflowCommonModel.Props.COMMENT, comment);
        props.put(WorkflowCommonModel.Props.STATUS, status);
        props.put(WorkflowCommonModel.Props.OWNER_NAME, ownerName);
        props.put(WorkflowCommonModel.Props.OWNER_ID, ownerId);
        props.put(WorkflowCommonModel.Props.OWNER_EMAIL, ownerEmail); // XXX missing?
        props.put(WorkflowCommonModel.Props.CREATOR_NAME, creatorName);
        props.put(WorkflowCommonModel.Props.CREATED_DATE_TIME, createdDateTime);
        props.put(WorkflowCommonModel.Props.STARTED_DATE_TIME, startedDateTime);
        props.put(WorkflowCommonModel.Props.FINISHED_DATE_TIME, finishedDateTime);
    }

    public void writeCaseFileOwnerProps(CaseFile caseFile, UserService userService) {
        if (StringUtils.isNotBlank(ownerId)) {
            caseFile.setProp(DocumentCommonModel.Props.OWNER_ID, ownerId);
            caseFile.setProp(DocumentCommonModel.Props.OWNER_NAME, ownerName);
            caseFile.setProp(DocumentCommonModel.Props.OWNER_EMAIL, ownerEmail);

            Map<QName, Serializable> userProps = userService.getUserProperties(ownerId);
            if (userProps != null) {
                caseFile.setProp(DocumentCommonModel.Props.OWNER_PHONE, userProps.get(ContentModel.PROP_TELEPHONE));
                caseFile.setProp(DocumentCommonModel.Props.OWNER_JOB_TITLE, userProps.get(ContentModel.PROP_JOBTITLE));
                caseFile.setProp(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT, userProps.get(ContentModel.PROP_ORGANIZATION_PATH));
                caseFile.setProp(DocumentDynamicModel.Props.OWNER_SERVICE_RANK, userProps.get(ContentModel.PROP_SERVICE_RANK));
                caseFile.setProp(DocumentDynamicModel.Props.OWNER_WORK_ADDRESS, userProps.get(ContentModel.PROP_STREET_HOUSE));
            } else {
                caseFile.setProp(DocumentCommonModel.Props.OWNER_PHONE, null);
                caseFile.setProp(DocumentCommonModel.Props.OWNER_JOB_TITLE, null);
                caseFile.setProp(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT, null);
                caseFile.setProp(DocumentDynamicModel.Props.OWNER_SERVICE_RANK, null);
                caseFile.setProp(DocumentDynamicModel.Props.OWNER_WORK_ADDRESS, null);
            }
        }
    }

    public Integer getProcedureId() {
        return procedureId;
    }

    public void setProcedureId(Integer procedureId) {
        this.procedureId = procedureId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public void setStartedDateTime(Date startedDateTime) {
        this.startedDateTime = startedDateTime;
    }

    public void setFinishedDateTime(Date finishedDateTime) {
        this.finishedDateTime = finishedDateTime;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Integer getOriginalProcedureId() {
        return originalProcedureId;
    }

    public void setOriginalProcedureId(Integer originalProcedureId) {
        this.originalProcedureId = originalProcedureId;
    }

}
