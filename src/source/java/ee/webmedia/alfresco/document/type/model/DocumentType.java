package ee.webmedia.alfresco.document.type.model;

import java.io.Serializable;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

/**
 * @author Alar Kvell
 */
@AlfrescoModelType(uri = DocumentTypeModel.URI)
public class DocumentType implements Serializable {
    private static final long serialVersionUID = 1L;

    @AlfrescoModelProperty(isMappable = false)
    private QName id;

    private String name;
    private boolean used;
    private String comment;

    // START: getters / setters
    public QName getId() {
        return id;
    }

    public void setId(QName id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
    // END: getters / setters
}
