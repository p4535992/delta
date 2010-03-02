package ee.webmedia.alfresco.document.associations.model;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.service.DocumentService.AssocType;

/**
 * @author Ats Uiboupin
 */
public class DocAssocInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String title;
    private AssocType assocType;
    //
    private NodeRef nodeRef;

    private NodeRef caseNodeRef;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
        return "DocAssocInfo [assocType=" + assocType + ", caseNodeRef=" + caseNodeRef + ", nodeRef=" + nodeRef + ", title=" + title + ", type=" + type + "]";
    }

}
