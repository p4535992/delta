package ee.webmedia.alfresco.common.propertysheet.customchildrencontainer;

import java.io.IOException;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.alfresco.web.ui.common.Utils;

/**
 * NB! In RichList this class cannot be used for submitting data, as it clears previous row children when next row is rendered.
 * Currently it is suitable for outputting text and links with external urls or javascript calls.
 */
public class CustomChildrenContainer extends UIComponentBase {

    public static final String ATTR_CHILD_GENERATOR = "childGenerator";
    public static final String ATTR_PARAMETERS = "parameterList";
    public static final String ATTR_CHILDREN_RENDERED = "childrenRendered";
    public static final String CUSTOM_CHILDREN_CONTAINER_FAMILY = CustomChildrenContainer.class.getCanonicalName();

    private int rowCounter = 0;
    private String customChildCreatorBinding;
    private String parametersValueBinding;

    public void setParametersValueBinding(String parametersValueBinding) {
        this.parametersValueBinding = parametersValueBinding;
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        Object parameters = getParametersValue(context);
        CustomChildrenCreator childGenerator = getCustomChildCreator(context);
        ValueBinding valueBinding = getValueBinding(ATTR_CHILDREN_RENDERED);
        Object rendered = valueBinding == null ? null : valueBinding.getValue(context);
        boolean childrenRendered = rendered == null || Boolean.parseBoolean(rendered.toString());

        if (childrenRendered) {
            List<UIComponent> createdChildren = childGenerator.createChildren(parameters, rowCounter++);
            if (createdChildren == null) {
                return;
            }
            for (UIComponent child : createdChildren) {
                if (child.isRendered()) {
                    // If we set parent instead of adding to ChildrenList, we can still create form submit links but the child won't appear in CustomChildrenContainer children
                    // list. This "hack" solves UIRichList problem as this component is used under UIColumn and setting parent here hides the children from the RichList renderer.
                    child.setParent(this);
                    Utils.encodeRecursive(context, child);
                }
            }
        }

        // If user repeatedly visits the same dialog, then this does not reset and keeps incrementing.
        // Too long values are not needed, so let's keep HTML size smaller.
        if (rowCounter > 99999) {
            rowCounter = 0;
        }
    }

    private Object getParametersValue(FacesContext context) {
        if (parametersValueBinding != null) {
            return context.getApplication().createValueBinding(parametersValueBinding).getValue(context);
        }
        ;
        return getValueBinding(ATTR_PARAMETERS).getValue(context);
    }

    public void setCustomChildCreator(String customChildCreatorBinding) {
        this.customChildCreatorBinding = customChildCreatorBinding;
    }

    private CustomChildrenCreator getCustomChildCreator(FacesContext context) {
        if (customChildCreatorBinding != null) {
            return (CustomChildrenCreator) context.getApplication().createValueBinding(customChildCreatorBinding).getValue(context);
        }
        return (CustomChildrenCreator) getValueBinding(ATTR_CHILD_GENERATOR).getValue(context);
    }

    @Override
    public String getFamily() {
        return CUSTOM_CHILDREN_CONTAINER_FAMILY;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[4];
        values[0] = super.saveState(context);
        values[1] = rowCounter;
        values[2] = parametersValueBinding;
        values[3] = customChildCreatorBinding;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
        rowCounter = (Integer) values[1];
        parametersValueBinding = (String) values[2];
        customChildCreatorBinding = (String) values[3];
    }
}
