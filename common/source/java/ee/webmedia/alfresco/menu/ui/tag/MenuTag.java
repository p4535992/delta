<<<<<<< HEAD
package ee.webmedia.alfresco.menu.ui.tag;

import javax.faces.component.UIComponent;

import org.apache.myfaces.shared_impl.taglib.UIComponentTagBase;

import ee.webmedia.alfresco.menu.ui.component.MenuRenderer;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;

public class MenuTag extends UIComponentTagBase {

    private static final String MENU_COMPONENT_TYPE = UIMenuComponent.class.getCanonicalName();
    private static final String MENU_RENDERER_TYPE = MenuRenderer.class.getCanonicalName();
    private String primary;
    private String tooltip;

    @Override
    public String getComponentType() {
        return MENU_COMPONENT_TYPE;
    }

    @Override
    public String getRendererType() {
        return MENU_RENDERER_TYPE;
    }

    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);
        setBooleanProperty(component, UIMenuComponent.PRIMARY_ATTRIBUTE_KEY, primary);
        setStringProperty(component, UIMenuComponent.TOOLTIP_ATTRIBUTE_KEY, tooltip);
    }

    @Override
    public void release() {
        super.release();
        primary = null;
        tooltip = null;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

}
=======
package ee.webmedia.alfresco.menu.ui.tag;

import javax.faces.component.UIComponent;

import org.apache.myfaces.shared_impl.taglib.UIComponentTagBase;

import ee.webmedia.alfresco.menu.ui.component.MenuRenderer;
import ee.webmedia.alfresco.menu.ui.component.UIMenuComponent;

public class MenuTag extends UIComponentTagBase {

    private static final String MENU_COMPONENT_TYPE = UIMenuComponent.class.getCanonicalName();
    private static final String MENU_RENDERER_TYPE = MenuRenderer.class.getCanonicalName();
    private String primary;

    @Override
    public String getComponentType() {
        return MENU_COMPONENT_TYPE;
    }

    @Override
    public String getRendererType() {
        return MENU_RENDERER_TYPE;
    }

    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);
        setBooleanProperty(component, UIMenuComponent.PRIMARY_ATTRIBUTE_KEY, primary);
    }

    @Override
    public void release() {
        super.release();
        primary = null;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
