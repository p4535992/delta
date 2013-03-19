package ee.webmedia.alfresco.classificator.web;

import java.util.List;

import ee.webmedia.alfresco.classificator.model.ClassificatorValue;
import ee.webmedia.alfresco.docadmin.web.ListReorderHelper;
import ee.webmedia.alfresco.docadmin.web.ListReorderHelper.OrderModifier;
import ee.webmedia.alfresco.utils.Transformer;

/**
 * @author Priit Pikk
 */
public class ClassificatorOrderModifier<B extends ClassificatorValue> implements OrderModifier<B, Integer> {

    @Override
    public Integer getOrder(B object) {
        return object.getOrder();
    }

    @Override
    public void setOrder(B object, Integer previousMaxField) {
        object.setOrder(INT_INCREMENT_STRATEGY.tr(previousMaxField));
    }

    @Override
    public Integer getOriginalOrder(B object) {
        return object.getOriginalOrder();
    }

    public void markBaseState(List<B> objects) {
        for (B object : objects) {
            Integer order = getOrder(object);
            if (order == null) {
                order = Integer.MAX_VALUE;
            }
            object.setOriginalOrder(order);
        }
    }

    public static final Transformer<Integer, Integer> INT_INCREMENT_STRATEGY = new Transformer<Integer, Integer>() {
        @Override
        public Integer tr(Integer previousMaxField) {
            return previousMaxField == null ? 1 : previousMaxField + 1;
        }
    };

}
