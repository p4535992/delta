package ee.webmedia.alfresco.utils;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.event.ActionEvent;

import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.web.ui.common.component.UIActionLink;

public class ActionUtil {

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
                    return ((UIParameter) child).getValue().toString();
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
        UIActionLink link = (UIActionLink) event.getComponent();
        Map<String, String> params = link.getParameterMap();
        return params.containsKey(key);
    }

}
