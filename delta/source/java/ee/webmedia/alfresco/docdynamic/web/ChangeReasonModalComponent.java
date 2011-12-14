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
public class ChangeReasonModalComponent extends UICommand {

    public static final String ACCESS_RESTRICTION_CHANGE_REASON_MODAL_ID = "accessRestrictionChangeReason_popup";
    public static final String DELETE_DOCUMENT_REASON_MODAL_ID = "deleteDocumentReason_popup";
    private String modalId;
    private String modalTitle;
    private String reasonLabel;
    private String finishButtonLabelId = "save";

    public ChangeReasonModalComponent() {
        setRendererType(null);
    }

    public ChangeReasonModalComponent(String modalId, String modalTitle, String reasonLabel) {
        this();
        this.modalId = modalId;
        this.modalTitle = modalTitle;
        this.reasonLabel = reasonLabel;
    }

    @Override
    public void decode(FacesContext context) {
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String actionValue = requestMap.get(getClientId(context));
        if (StringUtils.isNotBlank(actionValue)) {
            String reason = requestMap.get(getClientId(context) + "_reason");
            ChangeReasonEvent event = new ChangeReasonEvent(this, reason);
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
        ComponentUtil.writeModalHeader(out, modalId, MessageUtil.getMessage(modalTitle), null);

        // popup content
        out.write("<table><tbody>");
        out.write("<tr><td>" + MessageUtil.getMessage(reasonLabel) + ":</td></tr>");
        out.write("<tr><td>");
        out.write("<textarea class=\"expand19-200\" id=\"" + getClientId(context) + "_reason\" name=\"" + getClientId(context) + "_reason\" onkeyup=\"");
        out.write("document.getElementById('" + getClientId(context) + "_reason_btn').disabled = (document.getElementById('" + getClientId(context)
                + "_reason').value == null);");
        out.write("\" ></textarea>");
        out.write("</td></tr>");
        out.write("<tr><td>");
        out.write("<input id=\"" + getClientId(context) + "_reason_btn\" type=\"submit\" value=\"" + MessageUtil.getMessage(finishButtonLabelId)
                + "\" disabled=\"true\" onclick=\""
                + Utils.generateFormSubmit(context, this, getClientId(context), "SAVE") + "\" />");
        out.write("</td></tr>");
        out.write("</tbody></table>");

        ComponentUtil.writeModalFooter(out);
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] state = new Object[5];
        state[0] = super.saveState(context);
        state[1] = modalId;
        state[2] = modalTitle;
        state[3] = reasonLabel;
        state[4] = finishButtonLabelId;
        return state;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object states[] = (Object[]) state;
        super.restoreState(context, states[0]);
        modalId = (String) states[1];
        modalTitle = (String) states[2];
        reasonLabel = (String) states[3];
        finishButtonLabelId = (String) states[4];
    }

    // START: getters/setters

    public String getModalTitle() {
        return modalTitle;
    }

    public String getModalId() {
        return modalId;
    }

    public void setModalId(String modalId) {
        this.modalId = modalId;
    }

    public void setModalTitle(String modalTitle) {
        this.modalTitle = modalTitle;
    }

    public String getReasonLabel() {
        return reasonLabel;
    }

    public void setReasonLabel(String reasonLabel) {
        this.reasonLabel = reasonLabel;
    }

    public String getFinishButtonLabelId() {
        return finishButtonLabelId;
    }

    public void setFinishButtonLabelId(String finishButtonLabelId) {
        this.finishButtonLabelId = finishButtonLabelId;
    }

    // END: getters/setters

    /**
     * @author Kaarel Jõgeva
     */
    public static class ChangeReasonEvent extends ActionEvent {

        private static final long serialVersionUID = 1L;
        private final String reason;

        public ChangeReasonEvent(UIComponent component, String reason) {
            super(component);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
}
