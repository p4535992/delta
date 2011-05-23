package ee.webmedia.alfresco.document.einvoice.web;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;

import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

public class SendManuallyToSapModalComponent extends UICommand {

    private final static int ACTION_CLEAR = 1;
    private final static int ACTION_ADD = 2;
    private static final String ENTRY_SAP_NUMBER_MODAL_ID = "entrySapNumber_popup";

    public SendManuallyToSapModalComponent() {
        setRendererType(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void decode(FacesContext context) {
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String actionValue = requestMap.get(getClientId(context));
        if (StringUtils.isNotBlank(actionValue)) {
            int action = Integer.parseInt(actionValue);
            if (action == ACTION_CLEAR) {
                // do nothing;
            } else if (action == ACTION_ADD) {
                String entrySapNumber = requestMap.get(getClientId(context) + "_entrySapNumber");
                SendToSapManuallyEvent event = new SendToSapManuallyEvent(this, entrySapNumber);
                queueEvent(event);
            } else {
                throw new RuntimeException("Unknown action: " + actionValue);
            }
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (isRendered() == false) {
            return;
        }

        ResponseWriter out = context.getResponseWriter();

        // modal popup code
        out.write("<div id=\"" + ENTRY_SAP_NUMBER_MODAL_ID + "\" class=\"modalpopup modalwrap\">");
        out.write("<div class=\"modalpopup-header clear\"><h1>");
        out.write(MessageUtil.getMessage("document_invoiceEntrySapNumber_insert"));
        out.write("</h1><p class=\"close\"><a href=\"#\" onclick=\"");
        out.write(ComponentUtil.generateFieldSetter(context, this, getActionId(context, this), ""));
        out.write(Utils.generateFormSubmit(context, this, getClientId(context), Integer.toString(ACTION_CLEAR)));
        out.write("\">");
        out.write(MessageUtil.getMessage("close_window"));
        out.write("</a></p></div><div class=\"modalpopup-content\"><div class=\"modalpopup-content-inner modalpopup-filter\">");

        // popup content
        out.write("<table><tbody>");
        out.write("<tr><td>" + MessageUtil.getMessage("document_invoiceEntrySapNumber") + ":</td></tr>");
        out.write("<tr><td>");
        out.write("<input id=\"" + getClientId(context) + "_entrySapNumber\" name=\"" + getClientId(context) + "_entrySapNumber\" onkeyup=\"");
        out.write("document.getElementById('" + getClientId(context) + "_entrySapNumber_btn').disabled = (document.getElementById('" + getClientId(context)
                + "_entrySapNumber').value == null || document.getElementById('" + getClientId(context) + "_entrySapNumber').value.replace(/^\\s+|\\s+$/g, '').length == 0);");
        out.write("\" ></input>");
        out.write("</td></tr>");
        out.write("<tr><td>");
        out.write("<input id=\"" + getClientId(context) + "_entrySapNumber_btn\" type=\"submit\" value=\"" + MessageUtil.getMessage("save") + "\" disabled=\"true\" onclick=\""
                + Utils.generateFormSubmit(context, this, getClientId(context), Integer.toString(ACTION_ADD)) + "\" />");
        out.write("</td></tr>");
        out.write("</tbody></table>");

        // close modal popup
        out.write("</div></div></div>");
    }

    private static String getActionId(FacesContext context, UIComponent component) {
        return component.getParent().getClientId(context) + "_sap_action";
    }

    public static class SendToSapManuallyEvent extends ActionEvent {

        private static final long serialVersionUID = 1L;

        public String entrySapNumber;

        public SendToSapManuallyEvent(UIComponent component, String entrySapNumber) {
            super(component);
            this.entrySapNumber = entrySapNumber;
        }

    }

}
