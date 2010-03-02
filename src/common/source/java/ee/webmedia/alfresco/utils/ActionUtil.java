package ee.webmedia.alfresco.utils;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.event.ActionEvent;

import org.alfresco.web.ui.common.component.UIActionLink;

/**
 * @author Keit Tehvan
 */
public class ActionUtil {

    public static String getParam(ActionEvent event, String key) {
        UIComponent c = event.getComponent();
        Map<String, String> params = null;
        
        if(c instanceof UIActionLink) {
            UIActionLink link = (UIActionLink) c;
            params = link.getParameterMap();
        } else if (c.getChildCount() != 0) { // CommandButton or something else
            for(Object child : c.getChildren()) {
                if(child instanceof UIParameter && ((UIParameter) child).getName().equals(key)) {
                    return ((UIParameter) child).getValue().toString();
                }
            }
        }
        
        if (params != null && !params.containsKey(key)) {
            throw new RuntimeException("UIActionLink parameterMap does not contain key: " + key);
        }
        return params.get(key);
    }

    public static boolean hasParam(ActionEvent event, String key) {
        UIActionLink link = (UIActionLink) event.getComponent();
        Map<String, String> params = link.getParameterMap();
        return params.containsKey(key);
    }

}
