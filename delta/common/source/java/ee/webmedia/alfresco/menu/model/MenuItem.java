package ee.webmedia.alfresco.menu.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;

import org.alfresco.config.Config;
import org.alfresco.i18n.I18NUtil;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.config.ActionsConfigElement;
import org.alfresco.web.config.ActionsConfigElement.ActionDefinition;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.ConstantMethodBinding;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.alfresco.web.ui.repo.component.evaluator.ActionInstanceEvaluator;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

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
    @XStreamAlias("title-id")
    private String titleId;
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
    private String processor;
    @XStreamOmitField
    private List<String> styleClass;

    @XStreamOmitField
    private static final String ACTION_CONTEXT = "actionContext";
    @XStreamOmitField
    private static final String ATTR_VALUE = "value";
    @XStreamOmitField
    public static final String ATTR_PLAIN_MENU_ITEM = "plainMenuItem";

    public MenuItem() {}

    public UIComponent createComponent(FacesContext context, String id, UserService userService) {
        return createComponent(context, id, false, userService, false);
    }
    
    public UIComponent createComponent(FacesContext context, String id, UserService userService, boolean createChildren) {
        return createComponent(context, id, false, userService, false);
    }
    
    public UIComponent createComponent(FacesContext context, String id, UserService userService, boolean createChildren, boolean plainLink) {
        return createComponent(context, id, false, userService, true);
    }

    /**
     * Return ActionLink, based on xml configuration. Returns null, if user doesn't have permissions.
     * 
     * @param context
     * @param application Faces Application
     * @return
     */
    public UIComponent createComponent(FacesContext context, String id, boolean active, UserService userService, boolean plainLink) {

        if (isRestricted() && !hasPermissions(userService)) {
            return null;
        }

        javax.faces.application.Application application = context.getApplication();
        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);

        UIComponent component = null;
        ResourceBundle messages = Application.getBundle(context);

        link.setRendererType(UIActions.RENDERER_ACTIONLINK);
        FacesHelper.setupComponentId(context, link, id);

        if(getTitle() == null) {
            setTitle(I18NUtil.getMessage(getTitleId()));
        }
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
                FacesHelper.setupComponentId(context, evaluator, id + "-evaluator");
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
        if (plainLink) {
            link.getAttributes().put(ATTR_PLAIN_MENU_ITEM, Boolean.TRUE);
            return link;
        }

        MenuItemWrapper wrap = (MenuItemWrapper) application.createComponent(MenuItemWrapper.class.getCanonicalName());
        FacesHelper.setupComponentId(context, wrap, id + "-wrapper");
        wrap.setPlain(true);
        if (getId() != null) {
            wrap.setMenuId(getId());
        }
        wrap.getAttributes().put("styleClass", StringUtils.join(getStyleClass(), " "));
        @SuppressWarnings("unchecked")
        List<UIComponent> children = wrap.getChildren();
        children.add(link);

        return wrap;
    }

    protected void addParameter(FacesContext context, UIActionLink link, String name, String value) {
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
                "Subitems: " + (subItems == null ? null : subItems.size()) + "; ";
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
    
    public String getTitleId() {
        return titleId;
    }

    public void setTitleId(String titleId) {
        this.titleId = titleId;
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
        if(params == null) {
            setParams(new HashMap<String, String>());
        }
        return params;
    }
    
    public void setParams(Map<String, String> params) {
        this.params = params;
    }
    public String getProcessor() {
        return processor;
    }
    public void setProcessor(String processor) {
        this.processor = processor;
    }

    public List<String> getStyleClass() {
        if (styleClass == null) {
            styleClass = new ArrayList<String>();
        }
        return styleClass;
    }

    public void setStyleClass(List<String> styleClass) {
        this.styleClass = styleClass;
    }

}