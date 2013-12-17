package ee.webmedia.alfresco.help.web;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.ui.common.component.UIActionLink;
import org.alfresco.web.ui.repo.component.UIActions;

import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Globally useful methods and constants for working with help texts.
 * 
 * @author Martti Tamm
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
    }
}
