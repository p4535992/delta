package ee.webmedia.alfresco.menu.ui.component;

import java.util.Map;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.FacesEvent;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIActionLink;

import ee.webmedia.alfresco.menu.model.DropdownMenuItem;
import ee.webmedia.alfresco.menu.ui.MenuBean;


public class UIMenuComponent extends UIComponentBase {

    public static final String MENU_FAMILY = UIMenuComponent.class.getCanonicalName();
    private boolean primary;

    public static final String PRIMARY_ATTRIBUTE_KEY = "primary";
    public static final String TOOLTIP_ATTRIBUTE_KEY = "tooltip";
    public static final String VALUE_SEPARATOR = "_";
    public static final String VIEW_STACK = "_alfViewStack";

    public static class ClearViewStackActionListener implements ActionListener {

        @Override
        public void processAction(ActionEvent event) throws AbortProcessingException {
            FacesContext context = FacesContext.getCurrentInstance();
            UIActionLink link = (UIActionLink) event.getComponent();
            String clientId = link.getClientId(context);
            String activeId = clientId.replaceAll("^[^0-9]*", "");
            String linkId = link.getId();

            // Links defined in menu-structure.xml have XPath
            boolean forceReset = link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_XPATH) != null;

            // When creating new document or new object, don't reset
            boolean createNewDocument = activeId.startsWith(MenuBean.CREATE_NEW_DOCUMENT + VALUE_SEPARATOR);
            boolean createNew = activeId.startsWith(MenuBean.CREATE_NEW + VALUE_SEPARATOR);
            boolean outcomeShortcut = linkId != null && linkId.startsWith(MenuBean.SHORTCUT_OUTCOME_MENU_ITEM_PREFIX);

            // Clear the view stack, otherwise it would grow too big as the cancel button is hidden in some views
            // Later in the life-cycle the view where this action came from is added to the stack, so visible cancel buttons will function properly
            // We mustn't clear the stack and therefore reset breadcrumb for browse MenuItems
            if ((!isUpdateTreeActionListener(link) && !createNewDocument && !createNew && !outcomeShortcut) || forceReset) {
                MenuBean menuBean = (MenuBean) FacesHelper.getManagedBean(context, MenuBean.BEAN_NAME);
                MenuBean.clearViewStack(menuBean.getActiveItemId(), clientId);
            }
        }
    }

    @Override
    public void queueEvent(FacesEvent event) {

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
                if (Integer.parseInt(activeId) == MenuBean.DOCUMENT_REGISTER_ID) {
                    menuBean.collapseMenuItems(null);
                }
            }

            if (Integer.parseInt(menuBean.getActiveItemId()) == MenuBean.MY_TASKS_AND_DOCUMENTS_ID) {
                menuBean.processTaskItems(); // When user registers a doc, changes must reflect in admin session.
            }

            boolean forceReset = link.getAttributes().get(DropdownMenuItem.ATTRIBUTE_XPATH) != null;
            if (!isUpdateTreeActionListener(link) || forceReset) {
                Utils.setRequestValidationDisabled(context); // Disable validation if user is navigating away
            }

        }
        super.queueEvent(event);
    }

    private static boolean isUpdateTreeActionListener(UIActionLink link) {
        MethodBinding actionListener = link.getActionListener();
        return actionListener != null && MenuBean.UPDATE_TREE_ACTTIONLISTENER.equals(actionListener.getExpressionString());
    }

    // TODO - Currently saveState and restoreState aren't working properly. I.e. the active item id isn't returned correctly.
    // At the moment a workaround is used, MenuRenderer gets the active menu item id from MenuBean.
    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[2];
        values[0] = super.saveState(context);
        values[1] = getAttributes().get(UIMenuComponent.PRIMARY_ATTRIBUTE_KEY);
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
        setPrimary((Boolean) values[1]);
    }

    @Override
    public String getFamily() {
        return MENU_FAMILY;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isPrimary() {
        return primary;
    }

}
