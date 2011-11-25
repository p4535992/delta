package ee.webmedia.alfresco.common.propertysheet.customchildrencontainer;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.webapp.UIComponentTag;

/**
 * Tag for generating custom children based on attribute values
 * 
 * @author Riina Tens
 */
public class CustomChildrenContainerTag extends UIComponentTag {

    @Override
    public String getComponentType() {
        return CustomChildrenContainer.CUSTOM_CHILDREN_CONTAINER_FAMILY;
    }

    @Override
    public String getRendererType() {
        return null;
    }

    private String parameterList;
    private String childGenerator;

    @Override
    public void release() {
        super.release();
        setParameterList(null);
        childGenerator = null;
    }

    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);
        Application application = FacesContext.getCurrentInstance().getApplication();
        component.setValueBinding(CustomChildrenContainer.ATTR_PARAM_LIST, application.createValueBinding(parameterList));
        component.setValueBinding(CustomChildrenContainer.ATTR_CHILD_GENERATOR, application.createValueBinding(childGenerator));
    }

    public void setChildGenerator(String childGenerator) {
        this.childGenerator = childGenerator;
    }

    public String getChildGenerator() {
        return childGenerator;
    }

    public void setParameterList(String parameterList) {
        this.parameterList = parameterList;
    }

    public String getParameterList() {
        return parameterList;
    }

}
