package org.alfresco.web.bean.ajax;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

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
 * 
 * @author Romet Aidla
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

        UIViewRoot viewRoot = AjaxBean.restoreViewRoot(fc, viewName);
        if (viewRoot == null) {
            return;
        }

        UIPanel panel = (UIPanel) ComponentUtil.findComponentById(fc, viewRoot, panelId);
        Assert.notNull(panel, String.format("Panel with id=%s was not found", panelId));
        panel.setExpanded(Boolean.valueOf(panelState));

        String viewState = AjaxBean.saveView(fc, viewRoot);
        AjaxBean.writeViewState(fc.getResponseWriter(), viewState);
    }
}
