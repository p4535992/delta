package ee.webmedia.alfresco.common.listener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import ee.webmedia.alfresco.common.web.BeanHelper;

public class SessionListener implements HttpSessionListener {

    public static Set<String> loggedInUsers = Collections.synchronizedSet(new HashSet<String>());

    @Override
    public void sessionCreated(HttpSessionEvent arg0) {
        // nothing to do here
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent arg0) {
        String name = BeanHelper.getUserService().getCurrentUserName();
        loggedInUsers.remove(name);
    }

}
