package ee.webmedia.alfresco.register.model;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = RegisterModel.URI)
public class Register implements Serializable {

    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private String prefix;
    private String suffix;
    private String comment;
    private boolean active;

    @AlfrescoModelProperty(isMappable = false)
    private int counter;
    @AlfrescoModelProperty(isMappable = false)
    private NodeRef nodeRef;

    @Override
    public String toString() {
        return new StringBuilder()
        .append("\nName (id) = " + name + "(" + id + ")\n")
        .append("counter = " + counter + "\n")
        .append("prefix | suffix = " + prefix + " | " + suffix + "\n")
        .append("Active = " + active + "\n").toString();
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

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

}
