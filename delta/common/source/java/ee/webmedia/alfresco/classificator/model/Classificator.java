package ee.webmedia.alfresco.classificator.model;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = ClassificatorModel.URI)
public class Classificator implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    @XStreamOmitField
    private boolean addRemoveValues;
    private boolean deleteEnabled;

    @AlfrescoModelProperty(isMappable = false)
    @XStreamOmitField
    private NodeRef nodeRef;

    private String description;
    private Boolean alfabeticOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAddRemoveValues() {
        return addRemoveValues;
    }

    public void setAddRemoveValues(boolean addRemoveValues) {
        this.addRemoveValues = addRemoveValues;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getAlfabeticOrder() {
        return alfabeticOrder;
    }

    public void setAlfabeticOrder(Boolean alfabeticOrder) {
        this.alfabeticOrder = alfabeticOrder;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nName = " + name + "\n");
        sb.append("addRemoveValues = " + addRemoveValues + "\n");
        sb.append("nodeRef = " + nodeRef + "\n");
        return sb.toString();
    }

    public void setDeleteEnabled(boolean deleteEnabled) {
        this.deleteEnabled = deleteEnabled;
    }

    public boolean isDeleteEnabled() {
        return deleteEnabled;
    }
}
