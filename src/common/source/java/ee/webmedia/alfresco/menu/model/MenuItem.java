package ee.webmedia.alfresco.menu.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Map.Entry;

import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;

import org.alfresco.config.Config;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.config.ActionsConfigElement;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.ConstantMethodBinding;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.evaluator.ActionInstanceEvaluator;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import ee.webmedia.alfresco.document.type.service.DocumentTypeService;
import ee.webmedia.alfresco.menu.ui.component.MenuItemWrapper;
import ee.webmedia.alfresco.user.service.UserService;

/**
 * Base class for menu items.
 * 
 * @author Kaarel Jõgeva
 */
@XStreamAlias("item")
public class MenuItem implements Serializable {
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(MenuItem.class);

    @XStreamOmitField
    private static final long serialVersionUID = 0L;
    @XStreamAsAttribute
    private String id;
    @XStreamAsAttribute
    private String title;
    @XStreamAsAttribute
    private boolean admin;
    @XStreamAsAttribute
    @XStreamAlias("document-manager")
    private boolean docManager;
    private String outcome;
    @XStreamAlias("subitems")
    private List<MenuItem> subItems;
    @XStreamAlias("action-listener")
    private String actionListener;
    @XStreamOmitField
    private Map<String, String> params;

    @XStreamOmitField
    private static final String ACTION_CONTEXT = "actionContext";
    @XStreamOmitField
    private static final String ATTR_VALUE = "value";

    /**
     * Constructor for {@link MenuItem}
     * 
     * @param title
     * @param outcome
     */
    public MenuItem(String title, String outcome) {
        this.title = title;
        this.outcome = outcome;
    }

    /**
     * Constructor for {@link MenuItem}
     * 
     * @param title
     * @param outcome
     * @param children
     */
    public MenuItem(String title, String outcome, List<MenuItem> children) {
        this.title = title;
        this.outcome = outcome;
        this.subItems = children;
    }

    /**
     * Return ActionLink, based on xml configuration. Returns null, if user doesn't have permissions.
     * 
     * @param context
     * @param application Faces Application
     * @return
     */
    public UIComponent createComponent(FacesContext context, String id, UserService userService, DocumentTypeService docTypeService) {
        return createComponent(context, id, false, userService, docTypeService);
    }

    public UIComponent createComponent(FacesContext context, String id, boolean active, UserService userService) {
        return createComponent(context, id, active, userService, null);
    }

    public UIComponent createComponent(FacesContext context, String id, UserService userService) {
        return createComponent(context, id, false, userService, null);
    }

    public UIComponent createComponent(FacesContext context, String id, boolean active, UserService userService, DocumentTypeService docTypeService) {

        if (isRestricted() && !hasPermissions(userService)) {
            return null;
        }

        javax.faces.application.Application application = context.getApplication();
        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);

        UIComponent component = null;
        ResourceBundle messages = Application.getBundle(context);

        link.setRendererType(UIActions.RENDERER_ACTIONLINK);
        FacesHelper.setupComponentId(context, link, id);
        link.setValue(getTitle());
        link.setTooltip(getTitle());
        link.setAction(new ConstantMethodBinding(getOutcome()));
        if (!getActionListener().equals("")) {
            link.setActionListener(application.createMethodBinding(getActionListener(), new Class[] { javax.faces.event.ActionEvent.class }));
        }

        if (getParams() != null) {
            for (Entry<String, String> entry : getParams().entrySet()) {
                addParameter(context, link, entry.getKey(), entry.getValue());
            }
        }

        if (active) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = link.getAttributes();
            attributes.put("styleClass", "active");
        }

        // We need to apply styleClass in apply request values phase
        link.setImmediate(true);

        Config config = Application.getConfigService(context).getGlobalConfig();
        ActionDefinition actionDef = ((ActionsConfigElement) config.getConfigElement(ActionsConfigElement.CONFIG_ELEMENT_ID))
                .getActionDefinition(getOutcome());

        // Check, if there is config for this action and overwrite properties if available
        if (actionDef != null) {
            // prepare any code based evaluators that may be present
            if (actionDef.Evaluator != null) {
                ActionInstanceEvaluator evaluator =
                        (ActionInstanceEvaluator) application.createComponent(UIActions.COMPONENT_ACTIONEVAL);
                FacesHelper.setupComponentId(context, evaluator, null);
                evaluator.setEvaluator(actionDef.Evaluator);
                evaluator.setValueBinding(ATTR_VALUE, application.createValueBinding("#{" + ACTION_CONTEXT + "}"));

                // add the action evaluator component and walk down the hiearchy
                if (component != null) {
                    @SuppressWarnings("unchecked")
                    List<UIComponent> children = component.getChildren();
                    children.add(evaluator);
                }
                component = evaluator;
            }

            if (actionDef.TooltipMsg != null) {
                link.setTooltip(messages.getString(actionDef.TooltipMsg));
            } else if (actionDef.Tooltip != null) {
                if (UIComponentTagUtils.isValueReference(actionDef.Tooltip)) {
                    link.setValueBinding("tooltip", application.createValueBinding(actionDef.Tooltip));
                } else {
                    link.setValue(actionDef.Tooltip);
                }
            }

            if (actionDef.Label != null) {
                link.setValue(actionDef.Label);
            } else if (actionDef.LabelMsg != null) {
                link.setValue(messages.getString(actionDef.LabelMsg));
            }

            if (actionDef.Action != null) {
                if (UIComponentTagUtils.isValueReference(actionDef.Action)) {
                    link.setAction(application.createMethodBinding(actionDef.Action, null));
                } else {
                    link.setAction(new ConstantMethodBinding(actionDef.Action));
                }
            }
            if (actionDef.ActionListener != null) {
                link.setActionListener(application.createMethodBinding(actionDef.ActionListener, new Class[] { javax.faces.event.ActionEvent.class }));
            }

            // build any child params <f:param> components that are needed.
            Map<String, String> params = actionDef.getParams();
            if (params != null) {
                for (String name : params.keySet()) {
                    addParameter(context, link, name, params.get(name));
                }
            }
        }

        MenuItemWrapper wrap = (MenuItemWrapper) application.createComponent(MenuItemWrapper.class.getCanonicalName());
        FacesHelper.setupComponentId(context, wrap, null);
        wrap.setPlain(true);
        @SuppressWarnings("unchecked")
        List<UIComponent> children = wrap.getChildren();
        children.add(link);

        return wrap;
    }

    private void addParameter(FacesContext context, UIActionLink link, String name, String value) {
        UIParameter param = (UIParameter) context.getApplication().createComponent(ComponentConstants.JAVAX_FACES_PARAMETER);
        FacesHelper.setupComponentId(context, param, null);
        param.setName(name);
        if (UIComponentTagUtils.isValueReference(value)) {
            param.setValueBinding(ATTR_VALUE, context.getApplication().createValueBinding(value));
        } else {
            param.setValue(value);
        }
        @SuppressWarnings("unchecked")
        List<UIComponent> children = link.getChildren();
        children.add(param);
    }

    public boolean hasPermissions(UserService userService) {
        if (isAdmin()) {
            return userService.isAdministrator();
        } else if (isDocManager()) {
            return userService.isDocumentManager();
        }
        return false;

    }

    public boolean isRestricted() {
        return isAdmin() || isDocManager();
    }

    public String getActionListener() {
        return (this.actionListener != null) ? this.actionListener : "";
    }

    public void setActionListener(String actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public String toString() {
        return "Title: " + title +
                "Outcome: " + outcome + ", " +
                "Admin: " + admin + ", " +
                "Subitems: " + subItems.size() + "; ";
    }

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public boolean hasSubItems() {
        return (subItems != null && subItems.size() > 0);
    }

    public List<MenuItem> getSubItems() {
        return subItems;
    }

    public void setSubItems(List<MenuItem> subItems) {
        this.subItems = subItems;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isDocManager() {
        return docManager;
    }

    public void setDocManager(boolean docManager) {
        this.docManager = docManager;
    }

    public Map<String, String> getParams() {
        return params;
    }
    
    public void setParams(Map<String, String> params) {
        this.params = params;
    }

}
