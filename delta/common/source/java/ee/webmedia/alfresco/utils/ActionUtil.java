package ee.webmedia.alfresco.utils;

import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.apache.commons.lang.StringUtils;

/**
 * @author Keit Tehvan
 */
public class ActionUtil {

    public static String PARAM_PARENT_NODEREF = "parentNodeRef";

    public static String getParam(ActionEvent event, String key, String defaultValue) {
        return getParamInternal(event, key, defaultValue);
    }

    public static String getParam(ActionEvent event, String key) {
        return getParamInternal(event, key, null);
    }

    public static <T> T getParam(ActionEvent event, String key, Class<T> class1) {
        String value = getParam(event, key);
        return DefaultTypeConverter.INSTANCE.convert(class1, value);
    }

    private static String getParamInternal(ActionEvent event, String key, String defaultValue) {
        UIComponent c = event.getComponent();
        Map<String, String> params = null;

        if (c instanceof UIActionLink) {
            UIActionLink link = (UIActionLink) c;
            params = link.getParameterMap();
        } else if (c.getChildCount() != 0) { // CommandButton or something else
            for (Object child : c.getChildren()) {
                if (child instanceof UIParameter && ((UIParameter) child).getName().equals(key)) {
                    Object value = ((UIParameter) child).getValue();
                    return value == null ? null : value.toString();
                }
            }
        }
        if (params != null && !params.containsKey(key) && defaultValue == null) {
            throw new RuntimeException("UIActionLink parameterMap does not contain key: " + key);
        }
        final String paramValue;
        if (params != null) {
            if (params.containsKey(key)) {
                paramValue = params.get(key);
            } else {
                paramValue = defaultValue;
            }
        } else {
            paramValue = defaultValue;
        }
        return paramValue;
    }

    public static boolean hasParam(ActionEvent event, String key) {
        UIComponent component = event.getComponent();

        if (component instanceof UIActionLink) {
            return ((UIActionLink) component).getParameterMap().containsKey(key);
        } else if (component.getChildCount() != 0) { // CommandButton or something else
            for (Object child : component.getChildren()) {
                if (child instanceof UIParameter && ((UIParameter) child).getName().equals(key)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Map<String, String> getParams(ActionEvent event) {
        Map<String, String> params = null;
        UIComponent c = event.getComponent();
        if (c instanceof UIActionLink) {
            UIActionLink link = (UIActionLink) c;
            params = link.getParameterMap();
        } else if (c.getChildCount() != 0) { // CommandButton or something else
            params = new HashMap<String, String>();
            for (Object child : c.getChildren()) {
                if (child instanceof UIParameter) {
                    UIParameter uiParameter = (UIParameter) child;
                    String key = uiParameter.getName();
                    params.put(key, uiParameter.getValue().toString());
                }
            }
        }
        return params;
    }

    public static NodeRef getParentNodeRefParam(ActionEvent event) {
        NodeRef folderRef = null;
        if (hasParam(event, PARAM_PARENT_NODEREF)) {
            String folderRefStr = getParam(event, PARAM_PARENT_NODEREF);
            if (StringUtils.isNotBlank(folderRefStr)) {
                folderRef = new NodeRef(folderRefStr);
            }
        }
        return folderRef;
    }

}
