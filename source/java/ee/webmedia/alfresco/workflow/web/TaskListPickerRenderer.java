package ee.webmedia.alfresco.workflow.web;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.UIGenericPicker;
import org.alfresco.web.ui.common.renderer.BaseRenderer;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.search.Search;
import ee.webmedia.alfresco.common.propertysheet.search.SearchRenderer;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * This custom renderer must be set to an HtmlPanelGroup that wraps a UIGenericPicker as the only child.
 * It writes the HTML to open the picker component in a modal dialog which is hidden by default.
 * 
 * @author Erko Hansar
 */
public class TaskListPickerRenderer extends BaseRenderer {

    @SuppressWarnings("unchecked")
    @Override
    public void decode(FacesContext context, UIComponent component) {
        assertParmeters(context, component);
        UIGenericPicker picker = (UIGenericPicker) component.getChildren().get(0);

        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String value = requestMap.get(TaskListGenerator.getActionId(context, picker));
        if (StringUtils.isBlank(value)) {
            return;
        }

        if (value.startsWith(SearchRenderer.OPEN_DIALOG_ACTION + ";")) {
            picker.getAttributes().put(Search.OPEN_DIALOG_KEY, value.substring((SearchRenderer.OPEN_DIALOG_ACTION + ";").length()));
            Utils.setRequestValidationDisabled(context);
        } else if (value.startsWith(SearchRenderer.CLOSE_DIALOG_ACTION)) {
            picker.getAttributes().remove(Search.OPEN_DIALOG_KEY);
            Utils.setRequestValidationDisabled(context);
        } else {
            throw new RuntimeException("Unknown action: " + value);
        }
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        assertParmeters(context, component);
        UIGenericPicker picker = (UIGenericPicker) component.getChildren().get(0);

        ResponseWriter out = context.getResponseWriter();
        String openDialog = (String) picker.getAttributes().get(Search.OPEN_DIALOG_KEY);
        if (openDialog != null) {
            out.write("<div id=\"overlay\" style=\"display: block;\"></div>");
        }
        out.write("<div id=\"" + TaskListGenerator.getDialogId(context, picker) + "\" class=\"modalpopup modalwrap\"");
        if (openDialog != null) {
            out.write(" style=\"display: block;\"");
        }
        out.write("><div class=\"modalpopup-header clear\"><h1>");
        out.write(MessageUtil.getMessage(SearchRenderer.SEARCH_MSG));
        out.write("</h1><p class=\"close\"><a href=\"#\" onclick=\"");
        out.write(ComponentUtil.generateFieldSetter(context, picker, TaskListGenerator.getActionId(context, picker), SearchRenderer.CLOSE_DIALOG_ACTION));
        out.write("hideModal();");
        out.write(ComponentUtil.generateAjaxFormSubmit(context, picker, picker.getClientId(context), "1" /* ACTION_CLEAR */));
        out.write("\">");
        out.write(MessageUtil.getMessage(SearchRenderer.CLOSE_WINDOW_MSG));
        out.write("</a></p></div><div class=\"modalpopup-content\"><div class=\"modalpopup-content-inner modalpopup-filter\">");
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        assertParmeters(context, component);

        ResponseWriter out = context.getResponseWriter();
        out.write("</div></div></div>");

        UIGenericPicker picker = (UIGenericPicker) component.getChildren().get(0);
        String openDialog = (String) picker.getAttributes().get(Search.OPEN_DIALOG_KEY);
        if (openDialog != null) {
            picker.getAttributes().remove(Search.OPEN_DIALOG_KEY); // Used when full submit is done, but AJAX deprecates it
            out.write("<script type=\"text/javascript\">$jQ(document).ready(function(){");
            out.write(ComponentUtil.generateFieldSetter(context, picker, TaskListGenerator.getActionId(context, picker), SearchRenderer.OPEN_DIALOG_ACTION + ";" + openDialog));
            out.write("showModal('");
            out.write(TaskListGenerator.getDialogId(context, picker));
            out.write("');");
            out.write("});</script>");
        }

    }

}
