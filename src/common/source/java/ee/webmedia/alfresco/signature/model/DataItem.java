package ee.webmedia.alfresco.signature.model;

import java.io.Serializable;

import org.alfresco.repo.web.scripts.FileTypeImageUtils;
import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.signature.servlet.DownloadDigiDocContentServlet;

public class DataItem implements Serializable {

    private static final long serialVersionUID = 1L;

    protected int id;
    protected String name;
    protected String mimeType;
    protected String encoding;
    protected long size;
    protected byte[] data;
    protected String downloadUrl;

    public DataItem(NodeRef nodeRef, int id, String name, String mimeType, String encoding, long size, byte[] data) {
        if (id < 0) {
            throw new IllegalArgumentException("DataItem id must not be negative");
        }
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.size = size;
        this.data = data;
        if (nodeRef != null && name != null) {
            this.downloadUrl = DownloadDigiDocContentServlet.generateUrl(nodeRef, id, name);
        }
    }

    public DataItem(NodeRef nodeRef, int id, String name, String mimeType, String encoding, long size) {
        this(nodeRef, id, name, mimeType, encoding, size, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * Used in JSP to determine the file icon. 
     */
    public String getFileType16() {
        return FileTypeImageUtils.getFileTypeImage(getName(), true);
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean includeData) {
        StringBuilder sb = new StringBuilder("DataItem:");
        sb.append(" id=").append(id);
        sb.append(" name=").append(name);
        sb.append(" mimeType=").append(mimeType);
        sb.append(" encoding=").append(encoding);
        sb.append(" size=").append(size);
        sb.append(" downloadUrl=").append(downloadUrl);
        sb.append("\n");
        if (includeData) {
            sb.append(new String(data));
            sb.append("\n");
        }
        return sb.toString();
    }

}
