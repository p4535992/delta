package ee.webmedia.alfresco.docadmin.web;

import java.util.List;

import org.alfresco.service.namespace.QName;

import ee.webmedia.alfresco.base.BaseObject;
import ee.webmedia.alfresco.docadmin.web.ListReorderHelper.OrderModifier;
import ee.webmedia.alfresco.utils.RepoUtil;
import ee.webmedia.alfresco.utils.Transformer;

/**
 * Accessor and mutator for order property of the {@link BaseObject} that should be used to (re)order
 */
public class BaseObjectOrderModifier<B extends BaseObject> implements OrderModifier<B, Integer> {
    /** qName of the order property */
    private final QName orderProperty;
    /** temp property used to store previous value of order property */
    private final QName orderPropertyOriginalValue;

    public BaseObjectOrderModifier(QName orderProp) {
        orderProperty = orderProp;
        orderPropertyOriginalValue = RepoUtil.createTransientProp(orderProp.getLocalName() + "_BaseObjectReorderHelper");
    }

    @Override
    public Integer getOrder(B object) {
        return object.getProp(orderProperty);
    }

    @Override
    public void setOrder(B object, Integer previousMaxField) {
        object.setProp(orderProperty, INT_INCREMENT_STRATEGY.tr(previousMaxField));
    }

    @Override
    public Integer getOriginalOrder(B object) {
        return object.getProp(orderPropertyOriginalValue);
    }

    public void markBaseState(List<B> objects) {
        for (B object : objects) {
            Integer order = getOrder(object);
            Integer origOrder = getOriginalOrder(object);
            if (order == null) {
                // Other code should theoretically ensure that order is not null
                // But this is just in case (e.g. some older version did not ensure this)
                order = Integer.MAX_VALUE;
                object.setProp(orderProperty, order);
            }
            if (origOrder == null) {
            	object.setProp(orderPropertyOriginalValue, order);
            }
        }
    }

    public static final Transformer<Integer, Integer> INT_INCREMENT_STRATEGY = new Transformer<Integer, Integer>() {
        @Override
        public Integer tr(Integer previousMaxField) {
            return previousMaxField == null ? 1 : previousMaxField + 1;
        }
    };

}
