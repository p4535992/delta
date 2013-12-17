package ee.webmedia.alfresco.versions.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.repo.web.scripts.FileTypeImageUtils;

public class Version implements Serializable {

    private static final long serialVersionUID = 1L;

    private String version;
    private String author;
    private Date modified;
    private String downloadUrl;

    public String getVersion() {
        return version;
    }

    public Float getVersionAsFloat() {
        return version == null ? null : Float.valueOf(version);
    }

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

}
