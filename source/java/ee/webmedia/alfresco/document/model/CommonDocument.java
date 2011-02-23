package ee.webmedia.alfresco.document.model;

import java.util.Date;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.dvk.model.AbstractDocument;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * @author Ats Uiboupin
 */
@AlfrescoModelType(uri = DocumentCommonModel.URI)
abstract public class CommonDocument extends AbstractDocument {

    @AlfrescoModelProperty(isMappable = false)
    private QName documentTypeId;

    private String function;
    private String series;
    private String volume;
    //
    private String regNumber;
    private String docName;
    private Date regDateTime;
    private String docStatus;
    //
    private String comment;
    private String storageType;
    private String keywords;

    // START: doccom:accessRights properties
    private String accessRestriction;
    private Date accessRestrictionBeginDate;
    private Date accessRestrictionEndDate;
    private String accessRestrictionEndDesc;
    private String accessRestrictionReason;
    // END: doccom:accessRights properties

    // START: doccom:owner properties
    private String ownerName;

    // END: doccom:owner properties

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public String getDocName() {
        return docName;
    }

    public String getRegNumber() {
        return regNumber;
    }

    public void setRegNumber(String regNumber) {
        this.regNumber = regNumber;
    }

    public Date getRegDateTime() {
        return regDateTime;
    }

    public void setRegDateTime(Date regDateTime) {
        this.regDateTime = regDateTime;
    }

    public String getDocStatus() {
        return docStatus;
    }

    public void setDocStatus(String docStatus) {
        this.docStatus = docStatus;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    // START: AccessRights

    public void setAccessRestriction(String accessRestriction) {
        this.accessRestriction = accessRestriction;
    }

    public String getAccessRestriction() {
        return accessRestriction;
    }

    public String getAccessRestrictionEndDesc() {
        return accessRestrictionEndDesc;
    }

    public void setAccessRestrictionEndDesc(String accessRestrictionEndDesc) {
        this.accessRestrictionEndDesc = accessRestrictionEndDesc;
    }

    public Date getAccessRestrictionEndDate() {
        return accessRestrictionEndDate;
    }

    public void setAccessRestrictionEndDate(Date accessRestrictionEndDate) {
        this.accessRestrictionEndDate = accessRestrictionEndDate;
    }

    public void setAccessRestrictionBeginDate(Date accessRestrictionBeginDate) {
        this.accessRestrictionBeginDate = accessRestrictionBeginDate;
    }

    public Date getAccessRestrictionBeginDate() {
        return accessRestrictionBeginDate;
    }

    public void setAccessRestrictionReason(String accessRestrictionReason) {
        this.accessRestrictionReason = accessRestrictionReason;
    }

    public String getAccessRestrictionReason() {
        return accessRestrictionReason;
    }

    public void setDocumentTypeId(QName documentTypeId) {
        this.documentTypeId = documentTypeId;
    }

    public QName getDocumentTypeId() {
        return documentTypeId;
    }

    // END: AccessRights

    // START: Owner aspect
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerName() {
        return ownerName;
    }
    // END: Owner aspect
}
