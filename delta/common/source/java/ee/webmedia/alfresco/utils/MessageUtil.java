package ee.webmedia.alfresco.utils;

import java.text.MessageFormat;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.Utils;

import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

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
     * Get message from faces context.
     * 
     * @param messageId Id of the message
     * @return message
     */
    public static String getMessage(String messageId, Object... messageValuesForHolders) {
        return getMessage(FacesContext.getCurrentInstance(), messageId, messageValuesForHolders);
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
        final String msg = getMessage(context, messageId, messageValuesForHolders);
        Utils.addErrorMessage(msg);
    }

    /**
     * @param context
     * @param messageId
     * @param severity
     * @param messageValuesForHolders
     */
    public static void addStatusMessage(FacesContext context, String messageId, FacesMessage.Severity severity, Object... messageValuesForHolders) {
        if (severity == FacesMessage.SEVERITY_ERROR) {
            addErrorMessage(context, messageId, messageValuesForHolders);
            return;
        }
        final String msg = getMessage(context, messageId, messageValuesForHolders);
        context.addMessage(null, new FacesMessage(severity, msg, msg));
    }

    public static void addInfoMessage(String msgKey, Object... messageValuesForHolders) {
        addStatusMessage(FacesContext.getCurrentInstance(), msgKey, FacesMessage.SEVERITY_INFO, messageValuesForHolders);
    }
    public static void addInfoMessage(FacesContext currentInstance, String msgKey, Object... messageValuesForHolders) {
        addStatusMessage(currentInstance, msgKey, FacesMessage.SEVERITY_INFO, messageValuesForHolders);
    }

    public static boolean addStatusMessages(FacesContext facesContext, FeedbackWrapper feedbackWrapper) {
        boolean isErrorAdded = false;
        for (FeedbackVO feedbackVO : feedbackWrapper) {
            isErrorAdded |= addStatusMessage(facesContext, feedbackVO);
        }
        return isErrorAdded;
    }

    /**
     * @param facesContext
     * @param feedbackVO
     * @return true if added message with error or fatal severity
     */
    private static boolean addStatusMessage(FacesContext facesContext, FeedbackVO feedbackVO) {
        final MessageSeverity severity = feedbackVO.getSeverity();
        addStatusMessage(facesContext, severity, feedbackVO.getMessageKey(), feedbackVO.getMessageValuesForHolders());
        return severity == MessageSeverity.ERROR || severity == MessageSeverity.FATAL;
    }

    /**
     * Add statusMessage to the faces context(to be shown to the user). Message text is retrieved from message bundle based on key <code>e.getMessage()</code>
     * and possible valuces could be set using <code>e.getMessageValuesForHolders()</code>. Severity of message is determined by <code>e.getSeverity()</code>
     * 
     * @param facesContext
     * @param e - exception object used to create message
     */
    public static void addStatusMessage(FacesContext facesContext, UnableToPerformException e) {
        addStatusMessage(facesContext, e.getSeverity(), e.getMessageKey(), e.getMessageValuesForHolders());
    }

    private static void addStatusMessage(FacesContext facesContext, final MessageSeverity severity, final String message,
            final Object... maybeUntransaltedMessageValuesForHolders) {
        final FacesMessage.Severity facesSeverity;
        if (severity == MessageSeverity.INFO) {
            facesSeverity = FacesMessage.SEVERITY_INFO;
        } else if (severity == MessageSeverity.WARN) {
            facesSeverity = FacesMessage.SEVERITY_WARN;
        } else if (severity == MessageSeverity.ERROR) {
            facesSeverity = FacesMessage.SEVERITY_ERROR;
        } else if (severity == MessageSeverity.FATAL) {
            facesSeverity = FacesMessage.SEVERITY_FATAL;
        } else {
            throw new RuntimeException("Unexpected severity: " + severity);
        }
        Object[] translatedMessageValuesForHolders = new Object[maybeUntransaltedMessageValuesForHolders.length];
        for (int i = 0; i < maybeUntransaltedMessageValuesForHolders.length; i++) {
            Object maybeUntranslated = maybeUntransaltedMessageValuesForHolders[i];
            if(maybeUntranslated instanceof UnableToPerformException.UntransaltedMessageValueHolder) {
                final String messageKey = ((UnableToPerformException.UntransaltedMessageValueHolder)maybeUntranslated).getMessageKey();
                translatedMessageValuesForHolders[i] = getMessage(messageKey);
            } else {
                translatedMessageValuesForHolders[i] = maybeUntranslated;
            }
        }
        addStatusMessage(facesContext, message, facesSeverity, translatedMessageValuesForHolders);
    }

    /**
     * Add error message by concatenating messages retrieved from the messageKeys
     * 
     * @param currentInstance
     * @param messageKeys
     */
    public static void addErrorMessage(FacesContext currentInstance, String[] messageKeys) {
        final StringBuilder sb = new StringBuilder();
        for (String messageId : messageKeys) {
            sb.append(getMessage(currentInstance, messageId)).append(" ");
        }
        Utils.addErrorMessage(sb.toString());
    }

}
