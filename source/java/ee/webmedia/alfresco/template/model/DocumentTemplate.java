package ee.webmedia.alfresco.template.model;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * @author Kaarel Jõgeva
 */
@AlfrescoModelType(uri = DocumentTemplateModel.URI)
public class DocumentTemplate implements Serializable {

    private static final long serialVersionUID = 1L;
    private String name;
    private String comment;
    private String docTypeId;
    private String reportType;

    @AlfrescoModelProperty(isMappable = false)
    private NodeRef nodeRef;
    @AlfrescoModelProperty(isMappable = false)
    private String downloadUrl;
    @AlfrescoModelProperty(isMappable = false)
    private String docTypeName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDocTypeId() {
        return docTypeId;
    }

    public void setDocTypeId(String docTypeId) {
        this.docTypeId = docTypeId;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDocTypeName() {
        return docTypeName;
    }

    public void setDocTypeName(String docTypeName) {
        this.docTypeName = docTypeName;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getReportType() {
        return reportType;
    }
}
