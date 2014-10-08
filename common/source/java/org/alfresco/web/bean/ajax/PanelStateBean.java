package org.alfresco.web.bean.ajax;

<<<<<<< HEAD
import ee.webmedia.alfresco.utils.ComponentUtil;
import org.alfresco.web.ui.common.component.UIPanel;
import org.apache.myfaces.shared_impl.util.StateUtils;
import org.springframework.util.Assert;

import javax.faces.application.Application;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
=======
>>>>>>> develop-5.1
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

<<<<<<< HEAD
/**
 * Handles saving panel state in JSF view model. State changes are received using AJAX asynchronous call.
 *
 * Always last view state is restored and view state hidden input (with id="javax.faces.ViewState") must be
 * changed to value that is returned in response, otherwise old view state is restored with the next form submit. 
 *
 * This implementation works only when the view state is stored in server side (http session).
 *
 * @author Romet Aidla
=======
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.alfresco.web.ui.common.component.UIPanel;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.common.ajax.AjaxBean;
import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Handles saving panel state in JSF view model. State changes are received using AJAX asynchronous call.
 * Always last view state is restored and view state hidden input (with id="javax.faces.ViewState") must be
 * changed to value that is returned in response, otherwise old view state is restored with the next form submit.
 * This implementation works only when the view state is stored in server side (http session).
>>>>>>> develop-5.1
 */
public class PanelStateBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String PANEL_ID_PARAM = "panelId";
    public static final String PANEL_STATE_PARAM = "panelState";
    private static final String VIEW_NAME_PARAM = "viewName";

    // ------------------------------------------------------------------------------
    // AJAX handler methods

    public void updatePanelState() throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();

        Map params = fc.getExternalContext().getRequestParameterMap();
        String panelId = (String) params.get(PANEL_ID_PARAM);
        Assert.hasLength(panelId, "panelId was not found in request");
        String panelState = (String) params.get(PANEL_STATE_PARAM);
        Assert.hasLength(panelState, "panelState was not found in request");
        String viewName = (String) params.get(VIEW_NAME_PARAM);
        Assert.hasLength(viewName, "viewName was not found in request");

<<<<<<< HEAD
        UIViewRoot viewRoot = restoreViewRoot(fc, viewName);
=======
        UIViewRoot viewRoot = AjaxBean.restoreViewRoot(fc, viewName);
        if (viewRoot == null) {
            return;
        }
>>>>>>> develop-5.1

        UIPanel panel = (UIPanel) ComponentUtil.findComponentById(fc, viewRoot, panelId);
        Assert.notNull(panel, String.format("Panel with id=%s was not found", panelId));
        panel.setExpanded(Boolean.valueOf(panelState));

<<<<<<< HEAD
        String viewState = saveView(fc, viewRoot);
        writeResponse(fc, viewState);
    }

    private void writeResponse(FacesContext fc, String viewState) throws IOException {
        ResponseWriter out = fc.getResponseWriter();
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>");
        xml.append(String.format("<panel-state-save view-state=\"%s\"/>", viewState));
        out.write(xml.toString());
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
=======
        String viewState = AjaxBean.saveView(fc, viewRoot);
        AjaxBean.writeViewState(fc.getResponseWriter(), viewState);
>>>>>>> develop-5.1
    }
}
