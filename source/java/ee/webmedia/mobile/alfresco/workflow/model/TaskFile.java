package ee.webmedia.mobile.alfresco.workflow.model;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.document.file.model.File;

public class TaskFile {

    private String name;
    private NodeRef nodeRef;
    private boolean deleted;
    private String deleteUrl;
    private long size;
    private String displayName;
    private String readOnlyUrl;
    private Boolean viewDocumentFilesPermission;

    public TaskFile() {
    }

    public TaskFile(String name, NodeRef nodeRef) {
        this.name = name;
        this.nodeRef = nodeRef;
    }

    public TaskFile(File file) {
        nodeRef = file.getNodeRef();
        size = file.getSize();
        displayName = file.getDisplayName();
        readOnlyUrl = file.getReadOnlyUrl();
        viewDocumentFilesPermission = file.isViewDocumentFilesPermission();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDeleteUrl(String deleteUrl) {
        this.deleteUrl = deleteUrl;
    }

    public String getDeleteUrl() {
        return deleteUrl;
    }

    public long getSize() {
        return size;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getReadOnlyUrl() {
        return readOnlyUrl;
    }

    public Boolean getViewDocumentFilesPermission() {
        return viewDocumentFilesPermission;
    }

}
