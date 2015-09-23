package ee.webmedia.alfresco.document.file.model;

import org.alfresco.service.cmr.repository.NodeRef;

public class SimpleFileWithOrder extends SimpleFile {

    private static final long serialVersionUID = 1L;

    private final Long fileOrderInList;
    private final NodeRef fileRef;
    private final boolean generated;
    private boolean active;

    public SimpleFileWithOrder(String displayName, String readOnlyUrl, Long fileOrderInList, NodeRef fileRef, boolean generated) {
        super(displayName, readOnlyUrl);
        this.fileOrderInList = fileOrderInList;
        this.fileRef = fileRef;
        this.generated = generated;
    }

    public Long getFileOrderInList() {
        return fileOrderInList;
    }

    public NodeRef getFileRef() {
        return fileRef;
    }

    public boolean isGenerated() {
        return generated;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

}
