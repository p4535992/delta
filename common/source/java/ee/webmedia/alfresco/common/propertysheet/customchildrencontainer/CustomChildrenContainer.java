package ee.webmedia.alfresco.common.propertysheet.customchildrencontainer;

import java.io.IOException;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

import ee.webmedia.alfresco.utils.ComponentUtil;

public class CustomChildrenContainer extends UIComponentBase {

    public static final String ATTR_CHILD_GENERATOR = "childGenerator";
    public static final String ATTR_PARAM_LIST = "parameterList";
    public static final String CUSTOM_CHILDREN_CONTAINER_FAMILY = CustomChildrenContainer.class.getCanonicalName();

    private int rowCounter = 0;

    @Override
    @SuppressWarnings("unchecked")
    public void encodeBegin(FacesContext context) throws IOException {
        List<Object> parameterList = (List<Object>) getValueBinding(ATTR_PARAM_LIST).getValue(context);
        CustomChildrenCreator childGenerator = (CustomChildrenCreator) getValueBinding(ATTR_CHILD_GENERATOR).getValue(context);
        List<UIComponent> children = ComponentUtil.getChildren(this);
        children.clear();
        children.addAll(childGenerator.createChildren(parameterList, rowCounter++));
        // If user repeatedly visits the same dialog, then this does not reset and keeps incrementing.
        // Too long values are not needed, so let's keep HTML size smaller.
        if (rowCounter > 99999) {
            rowCounter = 0;
        }
    }

    @Override
    public String getFamily() {
        return CUSTOM_CHILDREN_CONTAINER_FAMILY;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[2];
        values[0] = super.saveState(context);
        values[1] = rowCounter;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
        rowCounter = (Integer) values[1];
    }

}
