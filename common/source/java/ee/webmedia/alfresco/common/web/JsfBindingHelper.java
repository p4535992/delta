package ee.webmedia.alfresco.common.web;

import static ee.webmedia.alfresco.common.web.BeanHelper.getJsfBindingHelper;

import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;

/**
 * Helper class to hold references to jsf tree components.
 */
public class JsfBindingHelper {

    public static final String BEAN_NAME = "JsfBindingHelper";

    private final Map<String, UIComponent> jsfBindings = new HashMap<>();

    public void addBinding(String name, UIComponent component) {
        jsfBindings.put(name, component);
    }

    public UIComponent getComponentBinding(String name) {
        return jsfBindings.get(name);
    }

    public void removeBinding(String name) {
        jsfBindings.remove(name);
    }

    public static HtmlPanelGroup getOrCreateHtmlPanelGroup(String bindingName) {
        JsfBindingHelper jsfBindingHelper = getJsfBindingHelper();
        HtmlPanelGroup panelGroup = (HtmlPanelGroup) jsfBindingHelper.getComponentBinding(bindingName);
        if (panelGroup == null) {
            panelGroup = (HtmlPanelGroup) FacesContext.getCurrentInstance().getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            jsfBindingHelper.addBinding(bindingName, panelGroup);
        }
        return panelGroup;
    }

}
