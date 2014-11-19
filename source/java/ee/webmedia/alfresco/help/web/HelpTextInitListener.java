package ee.webmedia.alfresco.help.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import ee.webmedia.alfresco.common.web.BeanHelper;

/**
 * Loads help texts into servlet context during servlet context initialization.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class HelpTextInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().setAttribute("helpText", BeanHelper.getHelpTextService().getHelpTextKeys());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute("helpText");
    }
}
