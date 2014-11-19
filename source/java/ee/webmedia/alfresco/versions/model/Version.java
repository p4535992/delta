package ee.webmedia.alfresco.versions.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.repo.web.scripts.FileTypeImageUtils;
<<<<<<< HEAD
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

public class Version implements Serializable {

    private static final long serialVersionUID = 1L;

    private String version;
    private String author;
    private Date modified;
    private String downloadUrl;
<<<<<<< HEAD
    private String comment;
    private NodeRef nodeRef;
    private Node node;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5

    public String getVersion() {
        return version;
    }

<<<<<<< HEAD
=======
    public Float getVersionAsFloat() {
        return version == null ? null : Float.valueOf(version);
    }

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

<<<<<<< HEAD
    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    /**
     * Used to specify icon.
     */
    public String getFileType16() {
        return FileTypeImageUtils.getFileTypeImage(getDownloadUrl(), true);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nversion = " + version + "\n");
        sb.append("author = " + author + "\n");
        sb.append("modified = " + modified + "\n");
        return sb.toString();
    }

<<<<<<< HEAD
    public VersionNr getVersionNr() {
        return new Version.VersionNr(version);
    }

    public class VersionNr implements Comparable<VersionNr> {
        private final String versionNr;

        public VersionNr(String versionNr) {
            this.versionNr = versionNr;
        }

        public String getVersionNr() {
            return versionNr;
        }

        @Override
        public int compareTo(VersionNr compareVersionNr) {
            if (compareVersionNr == null || StringUtils.isBlank(compareVersionNr.getVersionNr())) {
                return 1;
            }

            if (versionNr == null) {
                return -1;
            }
            
            String numbers[] = StringUtils.split(version, ".");
            String compNumbers[] = StringUtils.split(compareVersionNr.getVersionNr(), ".");
            int parts = Math.min(numbers.length, compNumbers.length);
            for (int i = 0; i < parts; i++) {
                int result = Integer.valueOf(numbers[i]).compareTo(Integer.valueOf(compNumbers[i]));
                if (result != 0) {
                    return result;
                }
            }

            return numbers.length > compNumbers.length ? -1 : 1;
        }
    }
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}
