<<<<<<< HEAD
package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.docadmin.web.BaseObjectOrderModifier.INT_INCREMENT_STRATEGY;

import java.util.List;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.docadmin.web.ListReorderHelper.OrderModifier;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Implementation of {@link OrderModifier} that knows how to change order property held in {@link Node}
 * 
 * @author Ats Uiboupin
 */
public class NodeOrderModifier implements OrderModifier<Node, Integer> {
    /** qName of the order property */
    private final QName orderProperty;
    /** temp property used to store previous value of order property */
    private final QName orderPropertyOriginalValue;

    public NodeOrderModifier(QName orderProp) {
        orderProperty = orderProp;
        orderPropertyOriginalValue = RepoUtil.createTransientProp(orderProp.getLocalName() + "_BaseObjectReorderHelper");
    }

    @Override
    public Integer getOrder(Node object) {
        return getProp(object, orderProperty);
    }

    @Override
    public void setOrder(Node object, Integer previousMaxField) {
        setProp(object, orderProperty, INT_INCREMENT_STRATEGY.tr(previousMaxField));
    }

    @Override
    public Integer getOriginalOrder(Node object) {
        return getProp(object, orderPropertyOriginalValue);
    }

    public void markBaseState(List<Node> objects) {
        for (Node object : objects) {
            Integer order = getOrder(object);
            setPreviousOrder(object, order);
        }
    }

    public void setPreviousOrder(Node object, Integer order) {
        if (order == null) {
            // Other code should theoretically ensure that order is not null
            // But this is just in case (e.g. some older version did not ensure this)
            order = Integer.MAX_VALUE;
        }
        setProp(object, orderPropertyOriginalValue, order);
    }

    private void setProp(Node object, QName prop, int value) {
        object.getProperties().put(prop.toString(), value);
    }

    private Integer getProp(Node object, QName property) {
        return (Integer) object.getProperties().get(property.toString());
    }

}
=======
package ee.webmedia.alfresco.docadmin.web;

import static ee.webmedia.alfresco.docadmin.web.BaseObjectOrderModifier.INT_INCREMENT_STRATEGY;

import java.util.List;

import org.alfresco.service.namespace.QName;
import org.alfresco.web.bean.repository.Node;

import ee.webmedia.alfresco.docadmin.web.ListReorderHelper.OrderModifier;
import ee.webmedia.alfresco.utils.RepoUtil;

/**
 * Implementation of {@link OrderModifier} that knows how to change order property held in {@link Node}
 */
public class NodeOrderModifier implements OrderModifier<Node, Integer> {
    /** qName of the order property */
    private final QName orderProperty;
    /** temp property used to store previous value of order property */
    private final QName orderPropertyOriginalValue;

    public NodeOrderModifier(QName orderProp) {
        orderProperty = orderProp;
        orderPropertyOriginalValue = RepoUtil.createTransientProp(orderProp.getLocalName() + "_BaseObjectReorderHelper");
    }

    @Override
    public Integer getOrder(Node object) {
        return getProp(object, orderProperty);
    }

    @Override
    public void setOrder(Node object, Integer previousMaxField) {
        setProp(object, orderProperty, INT_INCREMENT_STRATEGY.tr(previousMaxField));
    }

    @Override
    public Integer getOriginalOrder(Node object) {
        return getProp(object, orderPropertyOriginalValue);
    }

    public void markBaseState(List<Node> objects) {
        for (Node object : objects) {
            Integer order = getOrder(object);
            setPreviousOrder(object, order);
        }
    }

    public void setPreviousOrder(Node object, Integer order) {
        if (order == null) {
            // Other code should theoretically ensure that order is not null
            // But this is just in case (e.g. some older version did not ensure this)
            order = Integer.MAX_VALUE;
        }
        setProp(object, orderPropertyOriginalValue, order);
    }

    private void setProp(Node object, QName prop, int value) {
        object.getProperties().put(prop.toString(), value);
    }

    private Integer getProp(Node object, QName property) {
        return (Integer) object.getProperties().get(property.toString());
    }

}
>>>>>>> develop-5.1
