<<<<<<< HEAD
package ee.webmedia.alfresco.notification.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = NotificationModel.URI)
public class GeneralNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    private String creatorName;
    private Date createdDateTime;
    private String message;
    private boolean active;

    @AlfrescoModelProperty(isMappable = false)
    private NodeRef nodeRef;

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GeneralNotification [");
        if (nodeRef != null) {
            builder.append("nodeRef=").append(nodeRef).append(", ");
        }
        builder.append("active=").append(active).append(", ");
        if (createdDateTime != null) {
            builder.append("createdDateTime=").append(createdDateTime).append(", ");
        }
        if (creatorName != null) {
            builder.append("creatorName=").append(creatorName).append(", ");
        }
        if (message != null) {
            builder.append("message=").append(message);
        }
        builder.append("]");
        return builder.toString();
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

}
=======
package ee.webmedia.alfresco.notification.model;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = NotificationModel.URI)
public class GeneralNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    private String creatorName;
    private Date createdDateTime;
    private String message;
    private boolean active;

    @AlfrescoModelProperty(isMappable = false)
    private NodeRef nodeRef;

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GeneralNotification [");
        if (nodeRef != null) {
            builder.append("nodeRef=").append(nodeRef).append(", ");
        }
        builder.append("active=").append(active).append(", ");
        if (createdDateTime != null) {
            builder.append("createdDateTime=").append(createdDateTime).append(", ");
        }
        if (creatorName != null) {
            builder.append("creatorName=").append(creatorName).append(", ");
        }
        if (message != null) {
            builder.append("message=").append(message);
        }
        builder.append("]");
        return builder.toString();
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

}
>>>>>>> develop-5.1
