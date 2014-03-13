package ee.webmedia.alfresco.document.associations.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.document.assocsdyn.service.DocumentAssociationsServiceImpl;
import ee.webmedia.alfresco.document.model.DocumentCommonModel;
import ee.webmedia.alfresco.document.service.DocumentService.AssocType;

public class DocAssocInfo implements Serializable, Comparable<DocAssocInfo> {

    private static final long serialVersionUID = 1L;

    private String type;
    private String typeId;
    private String title;
    private AssocType assocType;
    private QName assocTypeQName;
    private boolean source;
    private String regNumber;
    private Date regDateTime;

    private NodeRef thisNodeRef;
    private NodeRef otherNodeRef;
    private NodeRef caseNodeRef;
    private NodeRef volumeNodeRef;
    private boolean isCaseFileVolume;
    private boolean allowDelete;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public AssocType getAssocType() {
        return assocType;
    }

    public void setAssocType(AssocType assocType) {
        this.assocType = assocType;
    }

    public boolean isSource() {
        return source;
    }

    public void setSource(boolean source) {
        this.source = source;
    }

    public NodeRef getOtherNodeRef() {
        return otherNodeRef;
    }

    public void setOtherNodeRef(NodeRef otherNodeRef) {
        this.otherNodeRef = otherNodeRef;
    }

    public boolean isDocument() {
        return !isCase() && !isWorkflow() && !isVolume();
    }

    public boolean isCase() {
        return caseNodeRef != null;
    }

    public boolean isWorkflow() {
        return AssocType.WORKFLOW == assocType;
    }

    public boolean isVolume() {
        return volumeNodeRef != null;
    }

    public boolean isCaseFileVolume() {
        return isVolume() && isCaseFileVolume;
    }

    public boolean isDocumentToDocumentAssoc() {
        return DocumentCommonModel.Assocs.DOCUMENT_2_DOCUMENT.equals(assocTypeQName);
    }

    public boolean isFollowUpOrReplyAssoc() {
        return DocumentCommonModel.Assocs.DOCUMENT_FOLLOW_UP.equals(assocTypeQName) || DocumentCommonModel.Assocs.DOCUMENT_REPLY.equals(assocTypeQName);
    }

    public void setCaseFileVolume(boolean isCaseFileVolume) {
        this.isCaseFileVolume = isCaseFileVolume;
    }

    public void setCaseNodeRef(NodeRef caseNodeRef) {
        this.caseNodeRef = caseNodeRef;
    }

    public NodeRef getCaseNodeRef() {
        return caseNodeRef;
    }

    public void setVolumeNodeRef(NodeRef volumeNodeRef) {
        this.volumeNodeRef = volumeNodeRef;
    }

    public NodeRef getVolumeNodeRef() {
        return volumeNodeRef;
    }

    @Override
    public String toString() {
        return "DocAssocInfo [assocType=" + assocType + ", caseNodeRef=" + caseNodeRef + ", nodeRef=" + otherNodeRef
                + ", title=" + title + ", type=" + type + ", source=" + source + "]";
    }

    public void setRegNumber(String regNumber) {
        this.regNumber = regNumber;
    }

    public String getRegNumber() {
        return regNumber;
    }

    public void setRegDateTime(Date regDateTime) {
        this.regDateTime = regDateTime;
    }

    public Date getRegDateTime() {
        return regDateTime;
    }

    public NodeRef getEffectiveNodeRef() {
        return otherNodeRef == null ? caseNodeRef : otherNodeRef;
    }

    @Override
    public int compareTo(DocAssocInfo otherDocAssocInfo) {
        if (otherDocAssocInfo == null) {
            return (assocType == AssocType.WORKFLOW || isCase()) ? -1 : 0;
        }
        QName otherAssocTypeQName = otherDocAssocInfo.getAssocTypeQName();
        if (otherAssocTypeQName.equals(assocTypeQName)) {
            return AppConstants.DEFAULT_COLLATOR.compare(StringUtils.defaultString(getTitle()), StringUtils.defaultString(otherDocAssocInfo.getTitle()));
        } else if (assocType == AssocType.WORKFLOW) {
            return -1;
        } else if (otherDocAssocInfo.getAssocType() == AssocType.WORKFLOW) {
            return 1;
        } else {
            return Integer.valueOf(DocumentAssociationsServiceImpl.ASSOCS_BETWEEN_DOC_LIST_UNIT_ITEMS.indexOf(assocTypeQName)).compareTo(
                    DocumentAssociationsServiceImpl.ASSOCS_BETWEEN_DOC_LIST_UNIT_ITEMS.indexOf(otherAssocTypeQName));
        }
    }

    public NodeRef getThisNodeRef() {
        return thisNodeRef;
    }

    public void setThisNodeRef(NodeRef thisNodeRef) {
        this.thisNodeRef = thisNodeRef;
    }

    public NodeRef getSourceNodeRef() {
        return source ? otherNodeRef : thisNodeRef;
    }

    public NodeRef getTargetNodeRef() {
        return source ? thisNodeRef : otherNodeRef;
    }

    public QName getAssocTypeQName() {
        return assocTypeQName;
    }

    public void setAssocTypeQName(QName assocTypeQName) {
        this.assocTypeQName = assocTypeQName;
    }

    public boolean isAllowDelete() {
        return allowDelete;
    }

    public void setAllowDelete(boolean allowDelete) {
        this.allowDelete = allowDelete;
    }

}
