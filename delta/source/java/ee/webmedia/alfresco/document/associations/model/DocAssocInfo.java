package ee.webmedia.alfresco.document.associations.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.service.DocumentService.AssocType;

/**
 * @author Ats Uiboupin
 */
public class DocAssocInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String typeId;
    private String title;
    private AssocType assocType;
    private boolean source;
    private String regNumber;
    private Date regDateTime;

    private NodeRef nodeRef;

    private NodeRef caseNodeRef;

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

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public boolean isCase() {
        return caseNodeRef != null;
    }

    public void setCaseNodeRef(NodeRef caseNodeRef) {
        this.caseNodeRef = caseNodeRef;
    }

    public NodeRef getCaseNodeRef() {
        return caseNodeRef;
    }

    @Override
    public String toString() {
        return "DocAssocInfo [assocType=" + assocType + ", caseNodeRef=" + caseNodeRef + ", nodeRef=" + nodeRef
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
        return nodeRef == null ? caseNodeRef : nodeRef;
    }

}
