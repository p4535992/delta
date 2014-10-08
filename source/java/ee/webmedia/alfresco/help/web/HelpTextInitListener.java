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
>>>>>>> develop-5.1
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
