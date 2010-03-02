package ee.webmedia.alfresco.menu.ui.component;

import java.util.Map;
import java.util.Stack;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;

import ee.webmedia.alfresco.common.web.ClearStateNotificationHandler;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.component.UIActionLink;

import ee.webmedia.alfresco.menu.ui.MenuBean;

/**
 * @author Kaarel Jõgeva
 */

public class UIMenuComponent extends UIComponentBase {

    public static final String MENU_FAMILY = UIMenuComponent.class.getCanonicalName();
    private boolean primary;
    private String activeItemId; // TODO - could be refactored to int
    private String id;

    public static final String PRIMARY_ATTRIBUTE_KEY = "primary";
    public static final String ACTIVE_ITEM_ID_ATTRIBUTE_KEY = "activeItemId";
    public static final String ID_ATTRIBUTE_KEY = "id";
    public static final String VALUE_SEPARATOR = "_";
    public static final String VIEW_STACK = "_alfViewStack";

    @Override
    public void queueEvent(FacesEvent event) {

        boolean forceReset = false;
        if (event instanceof ActionEvent) {
            FacesContext context = FacesContext.getCurrentInstance();
            UIActionLink link = (UIActionLink) event.getComponent();
            String clientId = link.getClientId(context);
            String activeId = clientId.replaceAll("^[^0-9]*", "");
            @SuppressWarnings("unchecked")
            Map<String, Object> attr = link.getParent().getParent().getAttributes();

            MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME);
            Object isPrimary = attr.get(UIMenuComponent.PRIMARY_ATTRIBUTE_KEY);
            if (isPrimary != null && Boolean.parseBoolean(isPrimary.toString())) {
                menuBean.setActiveItemId(activeId);
                if(Integer.parseInt(activeId) == MenuBean.MY_TASKS_AND_DOCUMENTS_ID) {
                    menuBean.processTaskItems();
                }
                if(Integer.parseInt(activeId) == MenuBean.DOCUMENT_REGISTER_ID) {
                    menuBean.collapseMenuItems(null);
                }
            }
            
            if(Integer.parseInt(menuBean.getActiveItemId()) == MenuBean.DOCUMENT_REGISTER_ID && link.getClientId(context).endsWith(MenuRenderer.SECONDARY_MENU_PREFIX + 0)) {
                forceReset = true;
            }

            if(!(link.getActionListener() != null && link.getActionListener().getExpressionString() != null && link.getActionListener().getExpressionString().equals(MenuBean.UPDATE_TREE_ACTTIONLISTENER)) || forceReset) {
                menuBean.resetStateList(); // don't reset browse items, but reset on documentsList first
                forceReset = false;
            }

            
            // Clear the view stack, otherwise it would grow too big as the cancel button is hidden in some views
            // Later in the life-cycle the view where this action came from is added to the stack, so visible cancel buttons will function properly 
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
            sessionMap.put(VIEW_STACK, new Stack<String>());

            menuBean.setClickedId(clientId);

            // let the ClearStateNotificationHandler notify all the interested listeners
            ClearStateNotificationHandler clearStateNotificationHandler = (ClearStateNotificationHandler) FacesHelper.getManagedBean(context, ClearStateNotificationHandler.BEAN_NAME);
            clearStateNotificationHandler.notifyClearStateListeners();
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