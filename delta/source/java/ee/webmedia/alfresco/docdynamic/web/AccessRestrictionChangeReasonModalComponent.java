package ee.webmedia.alfresco.docdynamic.web;

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

/**
 * @author Kaarel Jõgeva
 */
public class AccessRestrictionChangeReasonModalComponent extends UICommand {

    public static final String ACCESS_RESTRICTION_CHANGE_REASON_MODAL_ID = "accessRestrictionChangeReason_popup";

    public AccessRestrictionChangeReasonModalComponent() {
        setRendererType(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void decode(FacesContext context) {
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String actionValue = requestMap.get(getClientId(context));
        if (StringUtils.isNotBlank(actionValue)) {
            String reason = requestMap.get(getClientId(context) + "_reason");
            AccessRestrictionChangeReasonEvent event = new AccessRestrictionChangeReasonEvent(this, reason);
            queueEvent(event);
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (!isRendered()) {
            return;
        }

        ResponseWriter out = context.getResponseWriter();

        // modal popup code
        ComponentUtil.writeModalHeader(out, ACCESS_RESTRICTION_CHANGE_REASON_MODAL_ID, MessageUtil.getMessage("docdyn_accesRestrictionChangeReason_modal_header"), null);

        // popup content
        out.write("<table><tbody>");
        out.write("<tr><td>" + MessageUtil.getMessage("docdyn_accesRestrictionChangeReason") + ":</td></tr>");
        out.write("<tr><td>");
        out.write("<textarea class=\"expand19-200\" id=\"" + getClientId(context) + "_reason\" name=\"" + getClientId(context) + "_reason\" onkeyup=\"");
        out.write("document.getElementById('" + getClientId(context) + "_reason_btn').disabled = (document.getElementById('" + getClientId(context)
                + "_reason').value == null);");
        out.write("\" ></textarea>");
        out.write("</td></tr>");
        out.write("<tr><td>");
        out.write("<input id=\"" + getClientId(context) + "_reason_btn\" type=\"submit\" value=\"" + MessageUtil.getMessage("save") + "\" disabled=\"true\" onclick=\""
                + Utils.generateFormSubmit(context, this, getClientId(context), "SAVE") + "\" />");
        out.write("</td></tr>");
        out.write("</tbody></table>");

        ComponentUtil.writeModalFooter(out);
    }

    /**
     * @author Kaarel Jõgeva
     */
    public static class AccessRestrictionChangeReasonEvent extends ActionEvent {

        private static final long serialVersionUID = 1L;
        private final String reason;

        public AccessRestrictionChangeReasonEvent(UIComponent component, String reason) {
            super(component);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
}
