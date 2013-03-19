package ee.webmedia.alfresco.sharepoint.entity;

import java.util.Date;

import ee.webmedia.alfresco.sharepoint.entity.mapper.AssociationMapper;

public class Association {

    public static final AssociationMapper MAPPER = new AssociationMapper();

    private String fromNode;
    private String type;
    private String creator;
    private Date createdDateTime;
    private boolean mainDocument;

    public boolean isDocument() {
        return "Seotud dokument".equals(type);
    }

    public boolean isWorkflow() {
        return "Seotud menetlus".equals(type);
    }

    public boolean isRelatedUrl() {
        return "Seotud url".equals(type);
    }

    public String getFromNode() {
        return fromNode;
    }

    public void setFromNode(String fromNode) {
        this.fromNode = fromNode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public boolean isMainDocument() {
        return mainDocument;
    }

    public void setMainDocument(boolean mainDocument) {
        this.mainDocument = mainDocument;
    }
}
