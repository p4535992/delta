package ee.webmedia.alfresco.sharepoint.entity;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.docdynamic.model.DocumentDynamicModel;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.sharepoint.entity.mapper.CaseFileMapper;
import ee.webmedia.alfresco.volume.model.VolumeModel;

public class CaseFile {

    public static final CaseFileMapper MAPPER = new CaseFileMapper();

    private String volumeMark;
    private String ownerName;
    private String title;
    private String userName;
    private Date validFrom;
    private Date validTo;
    private String status;
    private String contactName;
    private String applicantType;
    private String applicantArea;
    private String applicationLanguage;
    private String caseResult;
    private boolean supervisionVisit;
    private boolean opcat;
    private String comment;
    private String keywordLevel1;
    private String keywordLevel2;
    private String legislation;
    private boolean generalRightToEquality;
    private boolean discrimination;
    private boolean goodAdministration;
    private boolean childRights;
    private boolean childApplicant;
    private boolean toSurvey;
    private String procedureStatus;
    private boolean equalityOfTreatment;
    private Date workflowDueDate;
    private String keywordsString;

    public void updateProps(ee.webmedia.alfresco.casefile.service.CaseFile cf) {
        if (updateProp(cf, DocumentDynamicModel.Props.OWNER_NAME, ownerName)) {
            cf.setProp(DocumentCommonModel.Props.OWNER_ID, null);
            cf.setProp(DocumentCommonModel.Props.OWNER_EMAIL, null);
            cf.setProp(DocumentCommonModel.Props.OWNER_PHONE, null);
            cf.setProp(DocumentCommonModel.Props.OWNER_JOB_TITLE, null);
            cf.setProp(DocumentCommonModel.Props.OWNER_ORG_STRUCT_UNIT, null);
            cf.setProp(DocumentDynamicModel.Props.OWNER_SERVICE_RANK, null);
            cf.setProp(DocumentDynamicModel.Props.OWNER_WORK_ADDRESS, null);
        }
        updateProp(cf, VolumeModel.Props.TITLE, title);
        updateProp(cf, DocumentDynamicModel.Props.USER_NAME, userName);
        updateProp(cf, VolumeModel.Props.VALID_FROM, validFrom);
        updateProp(cf, VolumeModel.Props.VALID_TO, validTo);
        updateProp(cf, VolumeModel.Props.STATUS, status);
        updateProp(cf, DocumentDynamicModel.Props.CONTACT_NAME, contactName);
        updateProp(cf, DocumentDynamicModel.Props.APPLICANT_TYPE, applicantType);
        updateProp(cf, DocumentDynamicModel.Props.APPLICANT_AREA, applicantArea);
        updateProp(cf, DocumentDynamicModel.Props.APPLICATION_LANGUAGE, applicationLanguage);
        updateProp(cf, DocumentDynamicModel.Props.CASE_RESULT, caseResult);
        updateProp(cf, DocumentDynamicModel.Props.SUPERVISION_VISIT, supervisionVisit);
        updateProp(cf, DocumentDynamicModel.Props.OPCAT, opcat);
        updateProp(cf, DocumentDynamicModel.Props.COMMENT, comment);
        updateProp(cf, DocumentDynamicModel.Props.FIRST_LEVEL_RESPONDENT, keywordLevel1 != null ? (Serializable) Arrays.asList(keywordLevel1) : null);
        updateProp(cf, DocumentDynamicModel.Props.SECOND_LEVEL_RESPONDENT, keywordLevel2 != null ? (Serializable) Arrays.asList(keywordLevel2) : null);
        updateProp(cf, DocumentDynamicModel.Props.LEGISLATION, legislation);
        updateProp(cf, DocumentDynamicModel.Props.GENERAL_RIGHT_TO_EQUALITY, generalRightToEquality);
        updateProp(cf, DocumentDynamicModel.Props.DISCRIMINATION, discrimination);
        updateProp(cf, DocumentDynamicModel.Props.GOOD_ADMINISTRATION, goodAdministration);
        updateProp(cf, DocumentDynamicModel.Props.CHILD_RIGHTS, childRights);
        updateProp(cf, DocumentDynamicModel.Props.CHILD_APPLICANT, childApplicant);
        updateProp(cf, DocumentDynamicModel.Props.TO_SURVEY, toSurvey);
        updateProp(cf, DocumentDynamicModel.Props.PROCEDURE_STATUS, procedureStatus);
        updateProp(cf, DocumentDynamicModel.Props.EQUALITY_OF_TREATMENT, equalityOfTreatment);
        updateProp(cf, DocumentDynamicModel.Props.WORKFLOW_DUE_DATE, workflowDueDate);
        updateProp(cf, QName.createQName(DocumentDynamicModel.URI, "keywordsString"), keywordsString);
    }

    private boolean updateProp(ee.webmedia.alfresco.casefile.service.CaseFile cf, QName prop, Serializable value) {
        if (value != null && (!(value instanceof String) || StringUtils.isNotBlank((String) value))) {
            cf.setProp(prop, value);
            return true;
        }
        return false;
    }

    public String getVolumeMark() {
        return volumeMark;
    }

    public void setVolumeMark(String volumeMark) {
        this.volumeMark = volumeMark;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Timestamp validFrom) {
        this.validFrom = validFrom != null ? new Date(validFrom.getTime()) : null;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo != null ? new Date(validTo.getTime()) : null;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public void setApplicantType(String applicantType) {
        this.applicantType = applicantType;
    }

    public void setApplicantArea(String applicantArea) {
        this.applicantArea = applicantArea;
    }

    public void setApplicationLanguage(String applicationLanguage) {
        this.applicationLanguage = applicationLanguage;
    }

    public void setCaseResult(String caseResult) {
        this.caseResult = caseResult;
    }

    public void setSupervisionVisit(boolean supervisionVisit) {
        this.supervisionVisit = supervisionVisit;
    }

    public void setOpcat(boolean opcat) {
        this.opcat = opcat;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setKeywordLevel1(String keywordLevel1) {
        this.keywordLevel1 = keywordLevel1;
    }

    public void setKeywordLevel2(String keywordLevel2) {
        this.keywordLevel2 = keywordLevel2;
    }

    public void setLegislation(String legislation) {
        this.legislation = legislation;
    }

    public void setGeneralRightToEquality(boolean generalRightToEquality) {
        this.generalRightToEquality = generalRightToEquality;
    }

    public void setDiscrimination(boolean discrimination) {
        this.discrimination = discrimination;
    }

    public void setGoodAdministration(boolean goodAdministration) {
        this.goodAdministration = goodAdministration;
    }

    public void setChildRights(boolean childRights) {
        this.childRights = childRights;
    }

    public void setChildApplicant(boolean childApplicant) {
        this.childApplicant = childApplicant;
    }

    public void setToSurvey(boolean toSurvey) {
        this.toSurvey = toSurvey;
    }

    public void setProcedureStatus(String procedureStatus) {
        this.procedureStatus = procedureStatus;
    }

    public void setEqualityOfTreatment(boolean equalityOfTreatment) {
        this.equalityOfTreatment = equalityOfTreatment;
    }

    public void setWorkflowDueDate(Date workflowDueDate) {
        this.workflowDueDate = workflowDueDate;
    }

    public void setKeywordsString(String keywordsString) {
        this.keywordsString = keywordsString;
    }
}
