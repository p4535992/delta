package ee.webmedia.alfresco.utils;

import java.text.MessageFormat;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.Utils;

/**
 * Util class that helps to I18N messages.
 * 
 * @author Ats Uiboupin
 */
public class MessageUtil {

    /**
     * @param context
     * @param messageId - message id to be used
     * @param messageValuesForHolders - values for the placeHolders
     * @return message that has given <code>messageId</code> with placeHolders replaced with given <code>messageValuesForHolders</code>
     */
    public static String getMessage(FacesContext context, String messageId, Object... messageValuesForHolders) {
        String message = Application.getMessage(context, messageId);
        if (messageValuesForHolders != null) {
            message = MessageFormat.format(message, messageValuesForHolders);
        }
        return message;
    }

    /**
     * Translates <code>messageId</code> using {@link MessageUtil#getMessage(FacesContext, String, Object...)} <br>
     * and adds it to Alfreco using {@link Utils#addErrorMessage(String)}
     * 
     * @param context
     * @param messageId
     * @param messageValuesForHolders
     */
    public static void addErrorMessage(FacesContext context, String messageId, Object... messageValuesForHolders) {
        final String msg = MessageUtil.getMessage(context, "document_validationMsg_mandatory_functionSeriesVolume");
        Utils.addErrorMessage(msg);
    }
}
