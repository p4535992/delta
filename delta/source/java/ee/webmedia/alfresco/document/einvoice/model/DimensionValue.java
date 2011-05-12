package ee.webmedia.alfresco.document.einvoice.model;

import static org.alfresco.util.EqualsHelper.nullSafeEquals;

import java.util.Date;

import org.alfresco.web.bean.repository.Node;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.propertysheet.classificatorselector.ClassificatorSelectorValueProvider;

public class DimensionValue implements Comparable<DimensionValue>, ClassificatorSelectorValueProvider {

    private final Node node;

    public DimensionValue(Node node) {
        Assert.notNull(node);
        this.node = node;
    }

    public void setValueName(String valueName) {
        node.getProperties().put(DimensionModel.Props.VALUE_NAME.toString(), valueName);
    }

    public String getValueName() {
        return (String) node.getProperties().get(DimensionModel.Props.VALUE_NAME.toString());
    }

    public void setValue(String value) {
        node.getProperties().put(DimensionModel.Props.VALUE.toString(), value);
    }

    public String getValue() {
        return (String) node.getProperties().get(DimensionModel.Props.VALUE.toString());
    }

    public void setValueComment(String valueComment) {
        node.getProperties().put(DimensionModel.Props.VALUE_COMMENT.toString(), valueComment);
    }

    public String getValueComment() {
        return (String) node.getProperties().get(DimensionModel.Props.VALUE_COMMENT.toString());
    }

    public void setBeginDateTime(Date beginDateTime) {
        node.getProperties().put(DimensionModel.Props.BEGIN_DATE.toString(), beginDateTime);
    }

    public Date getBeginDateTime() {
        return (Date) node.getProperties().get(DimensionModel.Props.BEGIN_DATE.toString());
    }

    public void setEndDateTime(Date endDateTime) {
        node.getProperties().put(DimensionModel.Props.END_DATE.toString(), endDateTime);
    }

    public Date getEndDateTime() {
        return (Date) node.getProperties().get(DimensionModel.Props.END_DATE.toString());
    }

    public void setActive(Boolean active) {
        node.getProperties().put(DimensionModel.Props.ACTIVE.toString(), active);
    }

    public Boolean getActive() {
        return (Boolean) node.getProperties().get(DimensionModel.Props.ACTIVE.toString());
    }

    public void setDefaultValue(Boolean defaultValue) {
        node.getProperties().put(DimensionModel.Props.DEFAULT_VALUE.toString(), defaultValue);
    }

    public Boolean getDefaultValue() {
        return (Boolean) node.getProperties().get(DimensionModel.Props.DEFAULT_VALUE.toString());
    }

    public Node getNode() {
        return node;
    }

    public boolean equals(DimensionValue otherDimensionValue) {
        return nullSafeEquals(getActive(), otherDimensionValue.getActive())
                && nullSafeEquals(getBeginDateTime(), otherDimensionValue.getBeginDateTime())
                && nullSafeEquals(getEndDateTime(), otherDimensionValue.getEndDateTime())
                && StringUtils.equals(getValue(), otherDimensionValue.getValue())
                && StringUtils.equals(getValueComment(), otherDimensionValue.getValueComment())
                && StringUtils.equals(getValueName(), otherDimensionValue.getValueName())
                && nullSafeEquals(getDefaultValue(), otherDimensionValue.getDefaultValue());
    }

    @Override
    public boolean isByDefault() {
        return getDefaultValue();
    }

    @Override
    public String getClassificatorDescription() {
        return getValueComment();
    }

    @Override
    public int compareTo(DimensionValue o) {
        if (getValueName() == null) {
            return -1;
        }
        return getValueName().compareToIgnoreCase(o.getValueName());
    }

    @Override
    public String getSelectorValueName() {
        return getValueName();
    }
}
