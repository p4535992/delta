package ee.webmedia.alfresco.utils;

import java.text.MessageFormat;
import java.util.Collection;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.alfresco.web.app.Application;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.StringEscapeUtils;

import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * Util class that helps to I18N messages.
 */
public class MessageUtil {

    /**
     * @param context
     * @param messageId - message id to be used
     * @param messageValuesForHolders - values for the placeHolders(could also contain MessageData objects that will be recursively translated
     *            or collection of elements - that each element is recursively translated if needed and and joined using space)
     * @return message that has given <code>messageId</code> with placeHolders replaced with given <code>messageValuesForHolders</code>
     */
    public static String getMessage(FacesContext context, String messageId, Object... messageValuesForHolders) {
        String message = Application.getMessage(context, messageId);
        final Object[] msgValuesForHolders;
        if (messageValuesForHolders != null && messageValuesForHolders.length > 0) {
            msgValuesForHolders = new Object[messageValuesForHolders.length];
            for (int i = 0; i < messageValuesForHolders.length; i++) {
                Object messageValueForHolder = messageValuesForHolders[i];
                if (messageValueForHolder instanceof MessageData) {
                    MessageData messageData = (MessageData) messageValueForHolder;
                    msgValuesForHolders[i] = localizeMessage(context, messageData);
                } else if (messageValueForHolder instanceof Collection) {
                    @SuppressWarnings("rawtypes")
                    Collection msgParameterArray = (Collection) messageValueForHolder;
                    if (!msgParameterArray.isEmpty() && msgParameterArray.iterator().next() instanceof MessageData) {
                        final StringBuilder sb = new StringBuilder();
                        @SuppressWarnings("unchecked")
                        Collection<MessageData> messageDataArray = (Collection<MessageData>) messageValueForHolder;
                        for (MessageData messageDataItem : messageDataArray) {
                            sb.append(localizeMessage(context, messageDataItem)).append(" ");
                        }
                        msgValuesForHolders[i] = sb.toString();
                    }
                } else {
                    msgValuesForHolders[i] = messageValueForHolder;
                }
            }
        } else {
            msgValuesForHolders = messageValuesForHolders;
        }
        if (msgValuesForHolders != null) {
            message = MessageFormat.format(message, msgValuesForHolders);
        }
        return message;
    }

    private static String localizeMessage(FacesContext context, MessageData messageData) {
        final String translationWithPlaceholders = Application.getMessage(context, messageData.getMessageKey());
        return MessageFormat.format(translationWithPlaceholders, getTranslatedMessageParameters(context, messageData));
    }

    /**
     * Deeply localizes message parameters that might contain MessageData objects, that need to be localized as well
     * 
     * @param context
     * @param messageData
     * @return
     */
    private static Object[] getTranslatedMessageParameters(FacesContext context, MessageData messageData) {
        final Object[] messageParameters = messageData.getMessageValuesForHolders();
        for (int i = 0; i < messageParameters.length; i++) {
            Object msgParameter = messageParameters[i];
            if (msgParameter instanceof MessageData) {
                MessageData msgData = (MessageData) msgParameter;
                final String localizedMessageParameter = localizeMessage(context, msgData);
                messageParameters[i] = localizedMessageParameter;
            } else if (msgParameter instanceof Collection) {
                @SuppressWarnings("rawtypes")
                Collection msgParameterArray = (Collection) msgParameter;
                if (!msgParameterArray.isEmpty() && msgParameterArray.iterator().next() instanceof MessageData) {
                    final StringBuilder sb = new StringBuilder();
                    @SuppressWarnings("unchecked")
                    Collection<MessageData> messageDataArray = (Collection<MessageData>) msgParameter;
                    for (MessageData messageDataItem : messageDataArray) {
                        sb.append(localizeMessage(context, messageDataItem)).append(" ");
                    }
                    messageParameters[i] = sb.toString();
                }
            }
        }
        return messageParameters;
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

    public static String getMessage(MessageData messageData) {
        return getMessage(messageData.getMessageKey(), messageData.getMessageValuesForHolders());
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

    public static void addErrorMessage(String msgKey, Object... messageValuesForHolders) {
        addStatusMessage(FacesContext.getCurrentInstance(), msgKey, FacesMessage.SEVERITY_ERROR, messageValuesForHolders);
    }

    public static void addInfoMessage(FacesContext currentInstance, String msgKey, Object... messageValuesForHolders) {
        addStatusMessage(currentInstance, msgKey, FacesMessage.SEVERITY_INFO, messageValuesForHolders);
    }

    public static boolean addStatusMessages(FacesContext facesContext, MessageDataWrapper feedbackWrapper) {
        boolean isErrorAdded = false;
        for (MessageData messageData : feedbackWrapper) {
            isErrorAdded |= addStatusMessage(facesContext, messageData);
        }
        return isErrorAdded;
    }

    /**
     * @param facesContext
     * @param messageData
     * @return true if added message with error or fatal severity
     */
    public static boolean addStatusMessage(FacesContext facesContext, MessageData messageData) {
        final MessageSeverity severity = messageData.getSeverity();
        addStatusMessage(facesContext, severity, messageData.getMessageKey(), messageData.getMessageValuesForHolders());
        return severity == MessageSeverity.ERROR || severity == MessageSeverity.FATAL;
    }

    /**
     * Add statusMessage to the faces context(to be shown to the user). Message text is retrieved from message bundle based on key <code>messageData.getMessageKey()</code> and
     * possible values could be set using <code>messageData.getMessageValuesForHolders()</code>. Severity of message is determined by <code>messageData.getSeverity()</code>
     * 
     * @param facesContext
     * @param messageData - messageData object used to create message
     * @return true if added message with error or fatal severity
     */
    public static boolean addStatusMessage(MessageData messageData) {
        return addStatusMessage(FacesContext.getCurrentInstance(), messageData);
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
            if (maybeUntranslated instanceof UnableToPerformException.UntransaltedMessageValueHolder) {
                final String messageKey = ((UnableToPerformException.UntransaltedMessageValueHolder) maybeUntranslated).getMessageKey();
                translatedMessageValuesForHolders[i] = getMessage(facesContext, messageKey);
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

    public static String getMessageAndEscapeJS(String messageId, Object... messageValuesForHolders) {
        return StringEscapeUtils.escapeJavaScript(getMessage(messageId, messageValuesForHolders));
    }
}
