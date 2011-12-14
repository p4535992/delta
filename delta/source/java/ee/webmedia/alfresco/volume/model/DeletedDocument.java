package ee.webmedia.alfresco.volume.model;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.Serializable;
import java.util.Date;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * Represents a deleted document entry under volume node
 * 
 * @author Kaarel Jõgeva
 */
@AlfrescoModelType(uri = VolumeModel.URI)
public class DeletedDocument implements Serializable, Comparable<DeletedDocument> {
    private static final long serialVersionUID = 1L;

    private String actor;
    private Date deletedDateTime;
    private String documentData;
    private String comment;

    public DeletedDocument() {
        deletedDateTime = new Date();
    }

    public void createDocumentData(String regNr, String docName) {
        documentData = isBlank(regNr) ? docName : regNr + " " + docName;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Date getDeletedDateTime() {
        return deletedDateTime;
    }

    public void setDeletedDateTime(Date deletedDateTime) {
        this.deletedDateTime = deletedDateTime;
    }

    public String getDocumentData() {
        return documentData;
    }

    public void setDocumentData(String documentData) {
        this.documentData = documentData;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public int compareTo(DeletedDocument o) {
        return getDeletedDateTime().compareTo(o.getDeletedDateTime());
    }
}