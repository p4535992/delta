package ee.webmedia.alfresco.document.lock.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Model object for representing locked documents and files
 */
public class Lock implements Serializable, Comparable<Lock> {

    private static final long serialVersionUID = 1L;

    private NodeRef object;
    private NodeRef docNodeRef;
    private String docRegNr;
    private Date docRegDate;
    private String docName;
    private String fileName;
    private String fileUrl;
    private String lockedBy;

    public Lock() {
        // Default constructor
    }

    public Lock(NodeRef object) {
        this.object = object;
    }

    public NodeRef getObject() {
        return object;
    }

    public void setObject(NodeRef object) {
        this.object = object;
    }

    public NodeRef getDocNodeRef() {
        return docNodeRef;
    }

    public void setDocNodeRef(NodeRef docNodeRef) {
        this.docNodeRef = docNodeRef;
    }

    public String getDocRegNr() {
        return docRegNr;
    }

    public void setDocRegNr(String docRegNr) {
        this.docRegNr = docRegNr;
    }

    public Date getDocRegDate() {
        return docRegDate;
    }

    public void setDocRegDate(Date docRegDate) {
        this.docRegDate = docRegDate;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public boolean isFile() {
        return fileName != null;
    }

    public boolean isDocument() {
        return !isFile();
    }

    @Override
    public int compareTo(Lock o) {
        if (docName != null && o.docName != null) {
            int docNameCompare = docName.compareTo(o.docName);
            if (docNameCompare != 0) {
                return docNameCompare;
            }
            if (fileName != null && o.fileName != null) {
                return fileName.compareTo(o.fileName);
            }
        }
        return 0;
    }

    /**
     * Generated
     * Fields: object
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((object == null) ? 0 : object.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return object != null && obj instanceof Lock && ((Lock) obj).object != null && ((Lock) obj).object.equals(object);
    }
}
