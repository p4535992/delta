package ee.webmedia.alfresco.common.propertysheet.generator;

import java.util.List;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.common.ConstantMethodBinding;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.shared_impl.taglib.UIComponentTagUtils;

import ee.webmedia.alfresco.common.propertysheet.inlinepropertygroup.HandlesViewMode;

/**
 * @author Alar Kvell
 */
public class ActionLinkGenerator extends BaseComponentGenerator implements HandlesViewMode {

    public static final String ACTION_KEY = "action";
    public static final String ACTION_LISTENER_KEY = "actionListener";
    public static final String ACTION_LISTENER_PARAMS_KEY = "params";

    @Override
    public UIComponent generate(FacesContext context, String id) {
        Application application = context.getApplication();
        UIActionLink component = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
        component.setRendererType(UIActions.RENDERER_ACTIONLINK);
        FacesHelper.setupComponentId(context, component, id);

        // component.setValueBinding("value", ... is called by BaseComponentGenerator

        String action = getCustomAttributes().get(ACTION_KEY);
        if (StringUtils.isNotBlank(action)) {
            component.setAction(new ConstantMethodBinding(action));
        }

        String actionListener = getCustomAttributes().get(ACTION_LISTENER_KEY);
        if (StringUtils.isNotBlank(actionListener)) {
            component.setActionListener(application.createMethodBinding(actionListener, new Class[] { javax.faces.event.ActionEvent.class }));
        }

        @SuppressWarnings("unchecked")
        List<UIComponent> children = component.getChildren();

        String params = getCustomAttributes().get(ACTION_LISTENER_PARAMS_KEY);
        if (StringUtils.isNotBlank(params)) {
            for (String param : StringUtils.split(params, '¤')) {
                String[] paramNameAndValue = StringUtils.split(param, "=", 2);
                UIParameter paramComponent = (UIParameter) application.createComponent(UIParameter.COMPONENT_TYPE);
                paramComponent.setName(paramNameAndValue[0]);
                String value = paramNameAndValue[1];
                if (UIComponentTagUtils.isValueReference(value)) {
                    paramComponent.setValueBinding("value", application.createValueBinding(value));
                } else {
                    paramComponent.setValue(value);
                }
                children.add(paramComponent);
            }
        }

        return component;
    }
}
