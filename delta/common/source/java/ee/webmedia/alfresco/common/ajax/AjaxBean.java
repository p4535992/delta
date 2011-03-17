package ee.webmedia.alfresco.common.ajax;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.faces.application.Application;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;
import javax.faces.event.PhaseId;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.web.app.servlet.ajax.InvokeCommand.ResponseMimetype;
import org.alfresco.web.ui.common.Utils;
import org.apache.myfaces.shared_impl.util.RestoreStateUtils;
import org.apache.myfaces.shared_impl.util.StateUtils;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * @author Alar Kvell
 * @author Romet Aidla
 */
public class AjaxBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String COMPONENT_ID_PARAM = "componentId";
    public static final String COMPONENT_CLIENT_ID_PARAM = "componentClientId";
    private static final String VIEW_NAME_PARAM = "viewName";

    // ------------------------------------------------------------------------------
    // AJAX handler methods
    
    @ResponseMimetype(MimetypeMap.MIMETYPE_HTML)
    public void submit() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        @SuppressWarnings("unchecked")
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String componentId = params.get(COMPONENT_ID_PARAM);
        Assert.hasLength(componentId, "componentId was not found in request");
        String componentClientId = params.get(COMPONENT_CLIENT_ID_PARAM);
        Assert.hasLength(componentClientId, "componentClientId was not found in request");
        String viewName = params.get(VIEW_NAME_PARAM);
        Assert.hasLength(viewName, "viewName was not found in request");

        Utils.setRequestValidationDisabled(context);

        // Phase 1: Restore view
        UIViewRoot viewRoot = restoreViewRoot(context, viewName);

        UIComponent component = ComponentUtil.findChildComponentById(context, viewRoot, componentId, componentClientId);
        Assert.notNull(component, String.format("Component with id=%s was not found", componentId));
        System.out.println("Found component\n    id=" + component.getId() + "\n    clientId=" + component.getClientId(context));

        // The following is copied from RestoreStateUtils#recursivelyHandleComponentReferencesAndSetValid
        ValueBinding binding = component.getValueBinding("binding");
        if (binding != null && !binding.isReadOnly(context)) {
            binding.setValue(context, component);
        }
        if (component instanceof UIInput) {
            ((UIInput) component).setValid(true);
        }
        RestoreStateUtils.recursivelyHandleComponentReferencesAndSetValid(context, component);

        // Phase 2: Apply request values; process events
        execute(context, viewRoot, component, new PhaseExecutor() {
            @Override
            public boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component) {
                component.processDecodes(context);
                viewRoot._broadcastForPhase(PhaseId.APPLY_REQUEST_VALUES);
                if (context.getRenderResponse() || context.getResponseComplete()) {
                    viewRoot.clearEvents();
                }
                return false;
            }
        });

        // Phase 3: Process validations; process events
        execute(context, viewRoot, component, new PhaseExecutor() {
            @Override
            public boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component) {
                component.processValidators(context);
                viewRoot._broadcastForPhase(PhaseId.PROCESS_VALIDATIONS);
                if (context.getRenderResponse() || context.getResponseComplete()) {
                    viewRoot.clearEvents();
                }
                return false;
            }
        });

        // Phase 4: Update model values; process events
        execute(context, viewRoot, component, new PhaseExecutor() {
            @Override
            public boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component) {
                component.processUpdates(context);
                viewRoot._broadcastForPhase(PhaseId.UPDATE_MODEL_VALUES);
                if (context.getRenderResponse() || context.getResponseComplete()) {
                    viewRoot.clearEvents();
                }
                return false;
            }
        });

        // Phase 5: Invoke application; process events
        execute(context, viewRoot, component, new PhaseExecutor() {
            @Override
            public boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component) {
                viewRoot._broadcastForPhase(PhaseId.INVOKE_APPLICATION);
                if (context.getRenderResponse() || context.getResponseComplete()) {
                    viewRoot.clearEvents();
                }
                return false;
            }
        });

        // Phase 6: Render response
        Utils.encodeRecursive(context, component);

        String viewState = saveView(context, viewRoot);
        ResponseWriter out = context.getResponseWriter();
        out.write("VIEWSTATE:" + viewState);
    }

    private boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component, PhaseExecutor executor) {
        boolean skipFurtherProcessing = false;

        if (context.getResponseComplete()) {
            return true;
        }
        if (context.getRenderResponse()) {
            skipFurtherProcessing = true;
        }

        if (executor.execute(context, viewRoot, component)) {
            return true;
        }

        if (context.getResponseComplete() || context.getRenderResponse()) {
            skipFurtherProcessing = true;
        }
        return skipFurtherProcessing;
    }

    private interface PhaseExecutor {
        boolean execute(FacesContext context, UIViewRoot viewRoot, UIComponent component);
    }

    private String saveView(FacesContext fc, UIViewRoot viewRoot) {
        StateManager stateManager = fc.getApplication().getStateManager();
        stateManager.saveSerializedView(fc);
        return createViewState(fc, viewRoot);
    }

    private UIViewRoot restoreViewRoot(FacesContext fc, String viewName) {
        Application application = fc.getApplication();
        ViewHandler viewHandler = application.getViewHandler();

        // Because there is no javax.faces.ViewState request parameter, last view state is restored.
        UIViewRoot viewRoot = viewHandler.restoreView(fc, viewName);
        fc.setViewRoot(viewRoot);
        return viewRoot;
    }

    private String createViewState(FacesContext fc, UIViewRoot viewRoot) {
        // See HtmlResponseStateManager#writeState for meaning of different objects in saved state array.
        // TREE_PARAM is set to null, so that last view state will be restored during next form submit
        // (not the view state that was used to generate the form).
        // It creates dependency to implementation of HtmlResponseStateManager, but no better solution was found.
        Object[] savedState = new Object[3];
        savedState[2] = viewRoot.getViewId();
        return StateUtils.construct(savedState, fc.getExternalContext());
    }
}
