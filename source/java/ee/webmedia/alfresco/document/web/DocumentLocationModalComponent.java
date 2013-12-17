package ee.webmedia.alfresco.document.web;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getActionId;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent;
import ee.webmedia.alfresco.common.propertysheet.modalLayer.ModalLayerComponent.ModalLayerSubmitEvent;
import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * @author Keit Tehvan
 */
public class DocumentLocationModalComponent extends UICommand {

    private static final String FIELD_VALUE_SAVE = "SAVE";
    private static final String FIELD_VALUE_CANCEL = "CANCEL";
    public static final String MODAL_ID = "documentLocation_popup";

    public DocumentLocationModalComponent() {
        setRendererType(null);
    }

    @Override
    public void decode(FacesContext context) {
        @SuppressWarnings("unchecked")
        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        String actionValue = requestMap.get(getClientId(context));
        if (StringUtils.isNotBlank(actionValue)) {
            if (StringUtils.equals(FIELD_VALUE_SAVE, actionValue)) {
                ModalLayerSubmitEvent event = new ModalLayerSubmitEvent(this, ModalLayerComponent.ACTION_SUBMIT);
                queueEvent(event);
            } else if (StringUtils.equals(FIELD_VALUE_CANCEL, actionValue)) {
                ModalLayerSubmitEvent event = new ModalLayerSubmitEvent(this, ModalLayerComponent.ACTION_CLEAR);
                queueEvent(event);
            } else {
                throw new RuntimeException("Unknown action: " + actionValue);
            }
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        if (!isRendered()) {
            return;
        }
        ResponseWriter out = context.getResponseWriter();
        String onClose = ComponentUtil.generateFieldSetter(context, this, getActionId(context, this), "")
                + Utils.generateFormSubmit(context, this, getClientId(context), FIELD_VALUE_CANCEL);
        ComponentUtil.writeModalHeader(out, MODAL_ID, MessageUtil.getMessage("document_move"), onClose);
        out.write("<table><tbody><tr><td>");

    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        if (!isRendered()) {
            return;
        }
        List<UIComponent> children = ComponentUtil.getChildren(this);
        for (UIComponent uiComponent : children) {
            Utils.encodeRecursive(context, uiComponent);
        }
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        if (!isRendered()) {
            return;
        }
        ResponseWriter out = context.getResponseWriter();
        out.write(
                "</td></tr><tr><td><input id=\"" + getClientId(context) + "_doc_loc_btn\" type=\"submit\" value=\"" + MessageUtil.getMessage("save") + "\" onclick=\""
                        + Utils.generateFormSubmit(context, this, getClientId(context), FIELD_VALUE_SAVE) + "\" />");
        out.write("</td></tr></tbody></table>");
        ComponentUtil.writeModalFooter(out);
        super.encodeEnd(context);
    }

}
