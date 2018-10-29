package ee.smit.tera.model;

import java.io.Serializable;
import java.util.Date;

public class TeraFilesEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private Long id;
    private Date createdDateTime;
    private String filename;
    private String fileype;
    private String nodeRef;
    private String crypt;
    private boolean asics;
    private boolean checked;
    private String statusInfo;

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileype() {
        return fileype;
    }

    public void setFileype(String fileype) {
        this.fileype = fileype;
    }

    public String getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(String nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getCrypt() {
        return crypt;
    }

    public void setCrypt(String crypt) {
        this.crypt = crypt;
    }

    public boolean isAsics() {
        return asics;
    }

    public void setAsics(boolean asics) {
        this.asics = asics;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getStatusInfo() {
        return statusInfo;
    }

    public void setStatusInfo(String statusInfo) {
        this.statusInfo = statusInfo;
    }

    public TeraFilesEntry(){}


}
