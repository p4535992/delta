package ee.webmedia.alfresco.common.propertysheet.modalLayer;

import static ee.webmedia.alfresco.workflow.service.WorkflowUtil.getActionId;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
 * Modal popup to display input fields and submit data.
 * If needed, can be extended to use propertysheet to display input fields.
 * 
 * @author Riina Tens (refactored and generalized from TaskListCommentComponent and SendManuallyToSapModalComponent)
 */
public class ModalLayerComponent extends UICommand implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String ATTR_LABEL_KEY = "labelKey";
    public static final String ATTR_HEADER_KEY = "headerKey";
    public static final String ATTR_IS_DATE = "isDate";
    /** Submit button is not activated until all mandatory fields (marked by this attribute) are filled */
    public static final String ATTR_MANDATORY = "mandatory";
    public static final String ATTR_IS_HIDDEN = "isHidden";
    public static final String ATTR_SUBMIT_BUTTON_MSG_KEY = "submitButtonMsgKey";
    public static final String ACTION_INDEX = "actionIndex";

    private final static int ACTION_CLEAR = 1;
    private final static int ACTION_SUBMIT = 2;

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
                // do nothing;
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

        // modal popup code
        ComponentUtil.writeModalHeader(
                out,
                WorkflowUtil.getDialogId(context, this),
                MessageUtil.getMessage((String) getAttributes().get(ATTR_HEADER_KEY)),
                ComponentUtil.generateFieldSetter(context, this, getActionId(context, this), "")
                        + Utils.generateFormSubmit(context, this, getClientId(context), Integer.toString(ACTION_CLEAR)));

        // popup content
        out.write("<table><tbody>");
        JSONSerializer serializer = new JSONSerializer();
        List<UIComponent> checkedChildren = new ArrayList<UIComponent>();
        for (UIComponent child : ComponentUtil.getChildren(this)) {
            Map<String, Object> attributes = ComponentUtil.getAttributes(child);
            if (!attrIsTrue(attributes, ATTR_IS_HIDDEN) && isValidatedControl(attributes)) {
                checkedChildren.add(child);
            }
        }
        boolean addValidation = !checkedChildren.isEmpty();
        StringBuilder validationJs = new StringBuilder("");
        if (addValidation) {
            validationJs.append("document.getElementById(" + serializer.serialize(getSubmitButtonId(context)) + ").disabled = ");
            int childCounter = 0;
            for (UIComponent validatedChild : checkedChildren) {
                Map<String, Object> attributes = ComponentUtil.getAttributes(validatedChild);
                boolean mandatory = attrIsTrue(attributes, ATTR_MANDATORY);
                if (mandatory) {
                    validationJs.append("isEmptyInput(" + serializer.serialize(validatedChild.getClientId(context)) + ")");
                }
                if (attrIsTrue(attributes, ATTR_IS_DATE)) {
                    if (mandatory) {
                        validationJs.append(" || ");
                    }
                    validationJs.append("!validateDateInput(" + serializer.serialize(validatedChild.getClientId(context)) + ")");
                }
                if (childCounter < checkedChildren.size() - 1) {
                    validationJs.append(" || ");
                }
                childCounter++;
            }
        }
        for (UIComponent child : ComponentUtil.getChildren(this)) {
            Map<String, Object> attributes = ComponentUtil.getAttributes(child);
            boolean isHidden = attrIsTrue(attributes, ATTR_IS_HIDDEN);
            out.write("<tr><td class=\"propertiesLabel" + (isHidden ? " hidden" : "") + "\">");
            out.write(MessageUtil.getMessage((String) attributes.get(ATTR_LABEL_KEY)) + ":</td>");
            out.write("<td>");
            if (child instanceof UIInput) {
                ((UIInput) child).setValue(null);
            }
            Utils.encodeRecursive(context, child);
            out.write("</td></tr>");
            if (addValidation && !isHidden && isValidatedControl(attributes)) {
                out.write("<script type=\"text/javascript\">$jQ(document).ready(function(){");
                out.write("$jQ('#' + escapeId4JQ(" + serializer.serialize(child.getClientId(context)) + ") ).keyup(function(){" + validationJs + "});");
                out.write("$jQ('#' + escapeId4JQ(" + serializer.serialize(child.getClientId(context)) + ") ).change(function(){" + validationJs + "});");
                out.write("});</script>");
            }
        }

        String submitButtonMessageKey = (String) getAttributes().get(ATTR_SUBMIT_BUTTON_MSG_KEY);
        if (StringUtils.isBlank(submitButtonMessageKey)) {
            submitButtonMessageKey = "save";
        }
        out.write("<tr><td colspan='2'>");
        out.write("<input id=" + serializer.serialize(getSubmitButtonId(context))
                + " type=\"submit\" value=" + serializer.serialize(MessageUtil.getMessage(submitButtonMessageKey))
                + " disabled=" + serializer.serialize(addValidation ? "true" : "false")
                + " onclick="
                + serializer.serialize(Utils.generateFormSubmit(context, this, getClientId(context), Integer.toString(ACTION_SUBMIT))) + " />");
        out.write("</td></tr>");
        out.write("</tbody></table>");

        ComponentUtil.writeModalFooter(out);
    }

    public boolean isValidatedControl(Map<String, Object> attributes) {
        return (attrIsTrue(attributes, ATTR_MANDATORY) || attrIsTrue(attributes, ATTR_IS_DATE));
    }

    private String getSubmitButtonId(FacesContext context) {
        return getClientId(context) + "_submit_btn";
    }

    private boolean attrIsTrue(Map<String, Object> attributes, String attrIsHidden) {
        return Boolean.TRUE.equals(attributes.get(attrIsHidden));
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
