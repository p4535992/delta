package ee.webmedia.alfresco.utils;

import java.util.Map;

import javax.faces.event.ActionEvent;

import org.alfresco.web.ui.common.component.UIActionLink;

/**
 * @author Keit Tehvan
 */
public class ActionUtil {

    public static String getParam(ActionEvent event, String key) {
        UIActionLink link = (UIActionLink) event.getComponent();
        Map<String, String> params = link.getParameterMap();
        if (!params.containsKey(key)) {
            throw new RuntimeException("UIActionLink parameterMap does not contain key: " + key);
        }
        return params.get(key);
    }

}
