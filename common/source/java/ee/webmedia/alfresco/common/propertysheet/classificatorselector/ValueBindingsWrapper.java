package ee.webmedia.alfresco.common.propertysheet.classificatorselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import junit.framework.Assert;

import org.apache.myfaces.el.ValueBindingImpl;

/**
 * Wrapper class to bind multiple properties to one control
 */
public class ValueBindingsWrapper extends ValueBindingImpl {

    private List<ValueBinding> valueBindings;

    public ValueBindingsWrapper(ValueBinding... valueBinding) {
        Assert.assertTrue(valueBinding != null && valueBinding.length > 0);
        this.valueBindings = Arrays.asList(valueBinding);
    }

    public ValueBindingsWrapper() {
        valueBindings = null;
    }

    @Override
    public Object getValue(FacesContext facesContext) {
        List<Object> values = new ArrayList<Object>();
        for (ValueBinding valueBinding : valueBindings) {
            values.add(valueBinding.getValue(facesContext));
        }
        return values;
    }

    @Override
    public void setValue(FacesContext facesContext, Object newValue) {
        Assert.assertTrue(newValue == null || newValue instanceof List);
        if (newValue == null) {
            for (ValueBinding valueBinding : valueBindings) {
                valueBinding.setValue(facesContext, null);
            }
            return;
        }
        @SuppressWarnings("unchecked")
        List<Object> newValues = (List<Object>) newValue;
        Assert.assertTrue(newValues.size() == valueBindings.size());
        int valueIndex = 0;
        for (Object object : newValues) {
            valueBindings.get(valueIndex).setValue(facesContext, object);
            valueIndex++;
        }
    }

    @Override
    public Object saveState(final FacesContext facesContext) {
        List<Object> values = new ArrayList<Object>();
        for (ValueBinding valueBinding : valueBindings) {
            values.add(((StateHolder) valueBinding).saveState(facesContext));
        }
        return values.toArray();
    }

    @Override
    public void restoreState(FacesContext facesContext, Object obj) {
        Object[] values = (Object[]) obj;
        valueBindings = new ArrayList<ValueBinding>();
        for (Object value : values) {
            ValueBindingImpl valueBinding = new ValueBindingImpl();
            valueBinding.restoreState(facesContext, value);
            valueBindings.add(valueBinding);
        }
    }
}
