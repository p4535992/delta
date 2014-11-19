package ee.webmedia.alfresco.common.propertysheet.modalLayer;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getActionId;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;

import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import ee.webmedia.alfresco.workflow.service.WorkflowUtil;
import flexjson.JSONSerializer;

/**
 * Modal popup to display child components and one submit button.
 */
public class ModalLayerComponent extends UICommand implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ATTR_HEADER_KEY = "headerKey";
    public static final String ATTR_SUBMIT_BUTTON_MSG_KEY = "submitButtonMsgKey";
    public static final String ATTR_SUBMIT_BUTTON_HIDDEN = "submitButtonHidden";
    public static final String ATTR_SET_RENDERED_FALSE_ON_CLOSE = "setRenderedFalseOnClose";
    public static final String ATTR_AUTO_SHOW = "autoShow";
    public static final String ACTION_INDEX = "actionIndex";
    public static final String ATTR_STYLE_CLASS = "modalStyleClass";

    public final static int ACTION_CLEAR = 1;
    public final static int ACTION_SUBMIT = 2;

    public ModalLayerComponent() {
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
                if (Boolean.TRUE.equals(ComponentUtil.getAttributes(this).get(ATTR_SET_RENDERED_FALSE_ON_CLOSE))) {
                    setRendered(false);
                }
            } else if (action == ACTION_SUBMIT) {
                String actionIndexStr = requestMap.get(getActionId(context, this));
                Integer actionIndex = null;
                try {
                    actionIndex = Integer.parseInt(actionIndexStr.substring((ACTION_INDEX + ";").length()));
                } catch (Exception e) {
                    // ignore missing value
                }
                ModalLayerSubmitEvent event = new ModalLayerSubmitEvent(this, actionIndex);
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
        JSONSerializer serializer = new JSONSerializer();
        String modalStyleClass = (String) getAttributes().get(ATTR_STYLE_CLASS);
        ComponentUtil.writeModalHeader(
                out,
                getModalHtmlId(context),
                MessageUtil.getMessage((String) getAttributes().get(ATTR_HEADER_KEY)),
                StringUtils.isBlank(modalStyleClass) ? "" : modalStyleClass,
                ComponentUtil.generateFieldSetter(context, this, getActionId(context, this), "")
                        + generateCloseOnClick(context));

        writeModalContent(context, out, serializer);

        ComponentUtil.writeModalFooter(out);

        if (Boolean.TRUE.equals(ComponentUtil.getAttributes(this).get(ATTR_AUTO_SHOW))) {
            out.write("<script type=\"text/javascript\">$jQ(document).ready(function(){");
            out.write("showModal('");
            out.write(getClientId(context) + "_popup");
            out.write("');");
            out.write("});</script>");
        }
    }

    protected String getModalHtmlId(FacesContext context) {
        return WorkflowUtil.getDialogId(context, this);
    }

    protected String generateCloseOnClick(FacesContext context) {
        return Utils.generateFormSubmit(context, this, getClientId(context), Integer.toString(ACTION_CLEAR));
    }

    protected void writeModalContent(FacesContext context, ResponseWriter out, JSONSerializer serializer) throws IOException {
        for (UIComponent child : ComponentUtil.getChildren(this)) {
            Utils.encodeRecursive(context, child);
        }
        out.write("<br />");
        writeSubmitButton(context, out, serializer, "");
    }

    protected void writeSubmitButton(FacesContext context, ResponseWriter out, JSONSerializer serializer, String extraAttrs) throws IOException {
        String submitButtonMessageKey = (String) getAttributes().get(ATTR_SUBMIT_BUTTON_MSG_KEY);
        if (StringUtils.isBlank(submitButtonMessageKey)) {
            submitButtonMessageKey = "save";
        }
        out.write("<input id=" + serializer.serialize(getSubmitButtonId(context))
                + " type=\"submit\" value=" + serializer.serialize(MessageUtil.getMessage(submitButtonMessageKey))
                + " class=\"specificAction\""
                + " onclick="
                + serializer.serialize(generateSubmitOnClick(context))
                + (Boolean.TRUE.equals(ComponentUtil.getAttributes(this).get(ATTR_SUBMIT_BUTTON_HIDDEN)) ? " style=\"display: none;\"" : "")
                + extraAttrs + " />");
    }

    protected String generateSubmitOnClick(FacesContext context) {
        return Utils.generateFormSubmit(context, this, getClientId(context), Integer.toString(ACTION_SUBMIT));
    }

    protected String getSubmitButtonId(FacesContext context) {
        return getClientId(context) + "_submit_btn";
    }

    public static class ModalLayerSubmitEvent extends ActionEvent {

        private static final long serialVersionUID = 1L;
        private final Integer actionIndex;

        public ModalLayerSubmitEvent(UIComponent component, Integer actionIndex) {
            super(component);
            this.actionIndex = actionIndex;
        }

        public Object getSubmittedValue(String componentId) {
            UIComponent component = getComponent();
            for (UIComponent child : ComponentUtil.getChildren(component)) {
                if (child.getId().equals(componentId) && child instanceof UIInput) {
                    return ((UIInput) child).getValue();
                }
            }
            // TODO: maybe just log error and return null?
            throw new RuntimeException("No input with id='" + componentId + "' was found on layer='" + component.getId() + "'");
        }

        public Integer getActionIndex() {
            return actionIndex;
        }
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

}
