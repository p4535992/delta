package ee.webmedia.alfresco.help.web;

<<<<<<< HEAD
import java.io.IOException;
import java.io.Writer;
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

<<<<<<< HEAD
=======
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Globally useful methods and constants for working with help texts.
<<<<<<< HEAD
 * 
 * @author Martti Tamm
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class HelpTextUtil {

    public static final String TYPE_DIALOG = "dialog";

    public static final String TYPE_DOCUMENT_TYPE = "documentType";

    public static final String TYPE_FIELD = "field";

    public static boolean hasHelpText(FacesContext context, String type, String code) {
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
        Map<String, Map<String, Boolean>> helpTexts = (Map<String, Map<String, Boolean>>) servletContext.getAttribute("helpText");
        return helpTexts != null && helpTexts.get(type) != null && Boolean.TRUE.equals(helpTexts.get(type).get(code));
    }

<<<<<<< HEAD
    public static void writeHelpTextLink(Writer out, FacesContext context, String type, String code) throws IOException {
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        out.write(String.format("<img src=\"%1$s/images/icons/Help.gif\" alt=\"%2$s\" title=\"%2$s\" onclick=\"help('%1$s/help/%3$s/%4$s')\" style=\"cursor:pointer\"/>",
                servletContext.getContextPath(), MessageUtil.getMessage("helptext"), type, code));
=======
    @SuppressWarnings("unchecked")
    public static UIActionLink createHelpTextLink(FacesContext context, String type, String code) {
        javax.faces.application.Application application = context.getApplication();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        UIActionLink link = (UIActionLink) application.createComponent(UIActions.COMPONENT_ACTIONLINK);
        link.setRendererType(UIActions.RENDERER_ACTIONLINK);
        FacesHelper.setupComponentId(context, link, null);

        String title = MessageUtil.getMessage("helptext");
        link.setTooltip(title);

        link.setValue(title);
        link.setShowLink(false);

        String contextPath = servletContext.getContextPath();
        link.setOnclick("help('" + contextPath + "/help/" + type + "/" + code + "'); return false;");
        link.setImage("/images/icons/Help.gif");
        link.getAttributes().put("styleClass", "icon-link");
        link.getAttributes().put("style", "background-position: center;");

        return link;
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
    }
}
