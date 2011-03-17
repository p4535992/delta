package ee.webmedia.alfresco.signature.model;

import java.io.InputStream;
import java.io.Serializable;

import org.alfresco.repo.web.scripts.FileTypeImageUtils;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.ObjectUtils;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.webmedia.alfresco.signature.exception.SignatureException;
import ee.webmedia.alfresco.signature.servlet.DownloadDigiDocContentServlet;

public class DataItem implements Serializable {

    private static final long serialVersionUID = 1L;

    protected int id;
    protected String name;
    protected String mimeType;
    protected String encoding;
    protected long size;
    protected DataFile dataFile;
    protected String downloadUrl;

    public DataItem(NodeRef nodeRef, int id, String name, String mimeType, String encoding, long size, DataFile dataFile) {
        if (id < 0) {
            throw new IllegalArgumentException("DataItem id must not be negative");
        }
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.size = size;
        this.dataFile = dataFile;
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

    public String getName() {
        return name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getEncoding() {
        return encoding;
    }

    public long getSize() {
        return size;
    }

    /**
     * Returns a new stream each time. Caller must close the stream!
     */
    public InputStream getData() throws SignatureException {
        InputStream inputStream = null;
        try {
            inputStream = dataFile.getBodyAsStream();
        } catch (DigiDocException e) {
            throw new SignatureException("Error getting data, " + toString(), e);
        }
        if (inputStream == null) {
            throw new SignatureException("Error getting data, " + toString() + ": inputStream is null");
        }
        return inputStream;
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
        StringBuilder sb = new StringBuilder("DataItem[");
        sb.append("id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", mimeType=").append(mimeType);
        sb.append(", encoding=").append(encoding);
        sb.append(", size=").append(size);
        sb.append(", downloadUrl=").append(downloadUrl);
        sb.append(", dataFile=").append(ObjectUtils.identityToString(dataFile));
        sb.append("]");
        return sb.toString();
    }

}