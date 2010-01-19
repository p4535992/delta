package ee.webmedia.alfresco.menu.ui.component;

import java.util.Map;
import java.util.Stack;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.component.UIActionLink;

import ee.webmedia.alfresco.menu.ui.MenuBean;

/**
 * @author Kaarel Jõgeva
 */

public class UIMenuComponent extends UIComponentBase {

    public static final String MENU_FAMILY = UIMenuComponent.class.getCanonicalName();
    private boolean primary;
    private String activeItemId;
    private String id;

    public static final String PRIMARY_ATTRIBUTE_KEY = "primary";
    public static final String ACTIVE_ITEM_ID_ATTRIBUTE_KEY = "activeItemId";
    public static final String ID_ATTRIBUTE_KEY = "id";
    public static final String VALUE_SEPARATOR = "_";
    public final static String VIEW_STACK = "_alfViewStack";

    @Override
    public void queueEvent(FacesEvent event) {

        if (event instanceof ActionEvent) {
            FacesContext context = FacesContext.getCurrentInstance();
            UIActionLink link = (UIActionLink) event.getComponent();
            String activeId = link.getClientId(context).replaceAll("^[^0-9]*", "");
            @SuppressWarnings("unchecked")
            Map<String, Object> attr = link.getParent().getParent().getAttributes();

            Object isPrimary = attr.get(UIMenuComponent.PRIMARY_ATTRIBUTE_KEY);
            if (isPrimary != null && Boolean.parseBoolean(isPrimary.toString())) {
                MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME);
                menuBean.setActiveItemId(activeId);
            }
            
            // Clear the view stack, otherwise it would grow too big as the cancel button is hidden in some views
            // Later in the life-cycle the view where this action came from is added to the stack, so visible cancel buttons will function properly 
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(VIEW_STACK, new Stack<String>());
        }
        super.queueEvent(event);
    }

    // TODO - Currently saveState and restoreState aren't working properly. I.e. the active item id isn't returned correctly.
    // At the moment a workaround is used, MenuRenderer gets the active menu item id from MenuBean.
    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[4];
        values[0] = super.saveState(context);
        values[1] = this.getAttributes().get(UIMenuComponent.PRIMARY_ATTRIBUTE_KEY);
        values[2] = this.getAttributes().get(UIMenuComponent.ACTIVE_ITEM_ID_ATTRIBUTE_KEY);
        values[3] = this.getAttributes().get(UIMenuComponent.ID_ATTRIBUTE_KEY);
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
        setPrimary((Boolean) values[1]);
        this.activeItemId = (String) values[2];
        this.id = (String) values[3];
    }

    @Override
    public String getFamily() {
        return MENU_FAMILY;
    }

    public String getActiveItemId() {
        return activeItemId;
    }

    public void setActiveItemId(String activeItemId) {
        this.activeItemId = activeItemId;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isPrimary() {
        return primary;
    }
}