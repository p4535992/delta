package ee.webmedia.alfresco.classificator.model;

import java.io.Serializable;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorValueProvider;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelProperty;
import ee.webmedia.alfresco.utils.beanmapper.AlfrescoModelType;

@AlfrescoModelType(uri = ClassificatorModel.URI)
@XStreamAlias("classificatorValue")
public class ClassificatorValue implements Serializable, Comparable<ClassificatorValue>, ClassificatorSelectorValueProvider {

    private static final long serialVersionUID = 1L;

    private String valueName;
    private String classificatorDescription;
    private String valueData;
    private int order;
    @AlfrescoModelProperty(isMappable = false)
    private int originalOrder;
    private boolean byDefault;
    private boolean readOnly;
    private boolean active;

    @AlfrescoModelProperty(isMappable = false)
    @XStreamOmitField
    private NodeRef nodeRef;

    @AlfrescoModelProperty(isMappable = false)
    @XStreamOmitField
    private String lastOrderValidationMsg;

    @AlfrescoModelProperty(isMappable = false)
    @XStreamOmitField
    private String lastNameValidationMsg;

    public ClassificatorValue() {
        // default
    }

    public ClassificatorValue(ClassificatorValue classificatorValue) {
        valueName = classificatorValue.getValueName();
        order = classificatorValue.getOrder();
        byDefault = classificatorValue.isByDefault();
        readOnly = classificatorValue.isReadOnly();
        active = classificatorValue.isActive();
        valueData = classificatorValue.getValueData();
        nodeRef = classificatorValue.getNodeRef();
    }

    public String getValueName() {
        return valueName;
    }

    public void setValueName(String valueName) {
        if (StringUtils.isBlank(valueName)) {
            lastNameValidationMsg = "classificator_value_validation_name";
        } else {
            lastNameValidationMsg = null;
            this.valueName = valueName;
        }
    }

    public String getOrderText() {
        return getOrder() + "";
    }

    public void setOrderText(String orderText) {
        if (StringUtils.isBlank(orderText)) {
            lastOrderValidationMsg = "classificator_value_validation_order_null";
        } else {
            try {
                int order = Integer.parseInt(orderText);
                if (order < 1) {
                    throw new NumberFormatException();
                }
                setOrder(order);
                lastOrderValidationMsg = null;
            } catch (NumberFormatException nfe) {
                lastOrderValidationMsg = "classificator_value_validation_order";
            }
        }
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOriginalOrder() {
        return originalOrder;
    }

    public void setOriginalOrder(int originalOrder) {
        this.originalOrder = originalOrder;
    }

    @Override
    public boolean isByDefault() {
        return byDefault;
    }

    public void setByDefault(boolean byDefault) {
        this.byDefault = byDefault;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
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

    public boolean isLastNameValidationSuccess() {
        return lastNameValidationMsg == null;
    }

    public boolean isLastOrderValidationSuccess() {
        return lastOrderValidationMsg == null;
    }

    public String validate() {
        if (lastNameValidationMsg != null) {
            return lastNameValidationMsg;
        }
        if (lastOrderValidationMsg != null) {
            return lastOrderValidationMsg;
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nvalueName = " + valueName + "\n");
        sb.append("order = " + order + "\n");
        sb.append("byDefault = " + byDefault + "\n");
        sb.append("readOnly = " + readOnly + "\n");
        sb.append("active = " + active + "\n");
        sb.append("nodeRef = " + nodeRef + "\n");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (active ? 1231 : 1237);
        result = prime * result + (byDefault ? 1231 : 1237);
        result = prime * result + ((classificatorDescription == null) ? 0 : classificatorDescription.hashCode());
        result = prime * result + ((nodeRef == null) ? 0 : nodeRef.hashCode());
        result = prime * result + order;
        result = prime * result + (readOnly ? 1231 : 1237);
        result = prime * result + ((valueName == null) ? 0 : valueName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        ClassificatorValue other = (ClassificatorValue) obj;
        if (active != other.active) {
            return false;
        }
        if (byDefault != other.byDefault) {
            return false;
        }
        if (nodeRef == null) {
            if (other.nodeRef != null) {
                return false;
            }
        } else if (!nodeRef.equals(other.nodeRef)) {
            return false;
        }
        if (order != other.order) {
            return false;
        }
        if (readOnly != other.readOnly) {
            return false;
        }
        if (valueName == null) {
            if (other.valueName != null) {
                return false;
            }
        } else if (!valueName.equals(other.valueName)) {
            return false;
        }
        if (classificatorDescription == null) {
            if (other.classificatorDescription != null) {
                return false;
            }
        } else if (!classificatorDescription.equals(other.classificatorDescription)) {
            return false;
        }
        return true;
    }

    @Override
    // Default comparing is according to value order
    public int compareTo(ClassificatorValue o) {
        if (getOrder() < o.getOrder()) {
            return -1;
        } else if (getOrder() > o.getOrder()) {
            return 1;
        }
        return 0;
    }

    public void setClassificatorDescription(String classificatorDescription) {
        this.classificatorDescription = classificatorDescription;
    }

    @Override
    public String getClassificatorDescription() {
        return classificatorDescription;
    }

    @Override
    public String getSelectorValueName() {
        return getValueName();
    }

    public void setValueData(String valueData) {
        this.valueData = valueData;
    }

    public String getValueData() {
        return valueData;
    }

}
