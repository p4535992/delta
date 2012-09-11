package ee.webmedia.alfresco.common.propertysheet.modalLayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.alfresco.web.ui.common.Utils;

import ee.webmedia.alfresco.utils.ComponentUtil;
import ee.webmedia.alfresco.utils.MessageUtil;
import flexjson.JSONSerializer;

/**
 * Modal popup to display input fields and submit data.
 * If needed, can be extended to use propertysheet to display input fields.
 * 
 * @author Riina Tens (refactored and generalized from TaskListCommentComponent and SendManuallyToSapModalComponent)
 */
public class ValidatingModalLayerComponent extends ModalLayerComponent {
    private static final long serialVersionUID = 1L;

    public static final String ATTR_LABEL_KEY = "labelKey";
    public static final String ATTR_IS_DATE = "isDate";
    /** Submit button is not activated until all mandatory fields (marked by this attribute) are filled */
    public static final String ATTR_MANDATORY = "mandatory";
    public static final String ATTR_IS_HIDDEN = "isHidden";
    public static final String ATTR_PRESERVE_VALUES = "attrPreserveValues";

    @Override
    protected void writeModalContent(FacesContext context, ResponseWriter out, JSONSerializer serializer) throws IOException {

        out.write("<table><tbody>");
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
            if (child instanceof UIInput && !isHidden && !attrIsTrue(attributes, ATTR_PRESERVE_VALUES)) {
                ((UIInput) child).setValue(null);
            }
            Utils.encodeRecursive(context, child);
            out.write("</td></tr>");
            if (addValidation && !isHidden && isValidatedControl(attributes)) {
                out.write("<script type=\"text/javascript\">$jQ(document).ready(function(){");
                out.write("$jQ('#' + escapeId4JQ(" + serializer.serialize(child.getClientId(context)) + ") ).keyup(function(){" + validationJs + "});");
                out.write("$jQ('#' + escapeId4JQ(" + serializer.serialize(child.getClientId(context)) + ") ).change(function(){" + validationJs + "});");
                out.write(validationJs.toString());
                out.write("});</script>");
            }
        }
        out.write("<tr><td colspan='2'>");
        writeSubmitButton(context, out, serializer, " disabled=" + serializer.serialize(addValidation ? "true" : "false"));
        out.write("</td></tr>");
        out.write("</tbody></table>");
    }

    public boolean isValidatedControl(Map<String, Object> attributes) {
        return (attrIsTrue(attributes, ATTR_MANDATORY) || attrIsTrue(attributes, ATTR_IS_DATE));
    }

    private boolean attrIsTrue(Map<String, Object> attributes, String attrIsHidden) {
        return Boolean.TRUE.equals(attributes.get(attrIsHidden));
    }

}
