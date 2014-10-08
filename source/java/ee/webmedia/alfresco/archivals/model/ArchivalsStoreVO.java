package ee.webmedia.alfresco.archivals.model;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

public class ArchivalsStoreVO {

    private final String storeSuffixAndPrimaryPathSuffix;
    private final String title;
    private final StoreRef storeRef;
    private final String primaryPath;
    private NodeRef nodeRef;

    public static ArchivalsStoreVO newInstance(String storeSuffixAndPrimaryPathSuffixAndtitle) {
        int i = storeSuffixAndPrimaryPathSuffixAndtitle.lastIndexOf('/');
        String first = storeSuffixAndPrimaryPathSuffixAndtitle.substring(0, i);
        String last = storeSuffixAndPrimaryPathSuffixAndtitle.substring(i + 1);
        return new ArchivalsStoreVO(first, last);
    }

    public ArchivalsStoreVO(String storeSuffixAndPrimaryPathSuffix, String title) {
        this.storeSuffixAndPrimaryPathSuffix = StringUtils.trimToEmpty(storeSuffixAndPrimaryPathSuffix);
        this.title = StringUtils.trimToEmpty(title);
        Assert.isTrue(StringUtils.isNotBlank(this.title));
        String[] parts = StringUtils.split(this.storeSuffixAndPrimaryPathSuffix, '/');
        Assert.isTrue(parts.length <= 2);
        String storeSuffix = "";
        String primaryPathSuffix = "";
        if (parts.length == 2) {
            storeSuffix = parts[0];
            primaryPathSuffix = parts[1];
        } else if (parts.length == 1) {
            storeSuffix = parts[0];
        }
        Assert.isTrue(storeSuffix.matches("^[A-Za-z0-9]*$"));
        Assert.isTrue(primaryPathSuffix.matches("^[A-Za-z0-9]*$"));
        storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "ArchivalsStore" + storeSuffix);
        primaryPath = "/fn:documentList" + primaryPathSuffix;
    }

    public String getStoreSuffixAndPrimaryPathSuffix() {
        return storeSuffixAndPrimaryPathSuffix;
    }

    public String getTitle() {
        return title;
    }

    public StoreRef getStoreRef() {
        return storeRef;
    }

    public String getPrimaryPath() {
        return primaryPath;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    @Override
    public String toString() {
        return "ArchivalsStoreVO[storeRef=" + storeRef + ", primaryPath=" + primaryPath + ", nodeRef=" + nodeRef + "]";
    }

    // hashCode and equals generated, based on storeRef and primaryPath

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((primaryPath == null) ? 0 : primaryPath.hashCode());
        result = prime * result + ((storeRef == null) ? 0 : storeRef.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ArchivalsStoreVO other = (ArchivalsStoreVO) obj;
        return ObjectUtils.equals(storeRef, other.storeRef) && ObjectUtils.equals(primaryPath, other.primaryPath);
    }

}
