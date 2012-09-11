package ee.webmedia.alfresco.help.web;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

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

    public static void writeHelpTextLink(Writer out, FacesContext context, String type, String code) throws IOException {
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        out.write(String.format("<img src=\"%1$s/images/icons/Help.gif\" alt=\"%2$s\" title=\"%2$s\" onclick=\"help('%1$s/help/%3$s/%4$s')\" style=\"cursor:pointer\"/>",
                servletContext.getContextPath(), MessageUtil.getMessage("helptext"), type, code));
    }
}
