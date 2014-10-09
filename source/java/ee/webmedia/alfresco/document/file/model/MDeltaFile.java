package ee.webmedia.alfresco.document.file.model;

import org.alfresco.service.cmr.repository.NodeRef;

public class MDeltaFile extends SimpleFile {

    private static final long serialVersionUID = 1L;
    private final long size;
    private final NodeRef parentRef;

    public MDeltaFile(String displayName, String readOnlyUrl, long size, NodeRef parentRef) {
        super(displayName, readOnlyUrl);
        this.size = size;
        this.parentRef = parentRef;
    }

    public long getSize() {
        return size;
    }

    public NodeRef getParentRef() {
        return parentRef;
    }

}
