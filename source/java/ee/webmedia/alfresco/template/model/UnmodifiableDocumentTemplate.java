package ee.webmedia.alfresco.template.model;

import java.io.Serializable;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class UnmodifiableDocumentTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String comment;
    private final String docTypeId;
    private final String reportType;

    private final NodeRef nodeRef;
    private final String downloadUrl;
    private final String docTypeName;
    private final Set<QName> aspects;

    public UnmodifiableDocumentTemplate(DocumentTemplate template, Set<QName> aspects) {
        name = template.getName();
        comment = template.getComment();
        docTypeId = template.getDocTypeId();
        reportType = template.getReportType();
        nodeRef = template.getNodeRef();
        downloadUrl = template.getDownloadUrl();
        docTypeName = template.getDocTypeName();
        this.aspects = aspects;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public String getDocTypeId() {
        return docTypeId;
    }

    public String getReportType() {
        return reportType;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getDocTypeName() {
        return docTypeName;
    }

    public boolean hasAspect(QName aspect) {
        return aspects != null && aspects.contains(aspect);
    }

}
