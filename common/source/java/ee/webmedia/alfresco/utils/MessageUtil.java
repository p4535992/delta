package ee.webmedia.alfresco.utils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.web.ui.common.Utils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.springframework.util.Assert;

import ee.webmedia.alfresco.app.AppConstants;
import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * Util class that helps to I18N messages.
 */
public class MessageUtil {
    private static final Log LOG = org.apache.commons.logging.LogFactory.getLog(MessageUtil.class);

    /**
     * @param context
     * @param messageId - message id to be used
     * @param messageValuesForHolders - values for the placeHolders(could also contain MessageData objects that will be recursively translated
     *            or collection of elements - that each element is recursively translated if needed and and joined using space)
     * @return message that has given <code>messageId</code> with placeHolders replaced with given <code>messageValuesForHolders</code>
     */
    public static String getMessage(@SuppressWarnings("UnusedParameters") FacesContext context, String messageId, Object... messageValuesForHolders) {
        Assert.notNull(messageId, "no messageId given for translation");

        final Object[] translatedValuesForHolders = getTranslatedMessageValueHolders(messageValuesForHolders);
        String message = I18NUtil.getMessage(messageId, AppConstants.DEFAULT_LOCALE, translatedValuesForHolders);
        if (isMessageTranslatedByI18nUtil(message)) {
            return message;
        }

        return "$$" + messageId + "$$";
    }

    private static String format(String message, final Object[] translatedValuesForHolders) {
        try {
            return MessageFormat.format(message, translatedValuesForHolders);
        } catch (IllegalArgumentException e) {
            // if e.getMessage() is "can't parse argument number SOME_STRING" then message contains "{SOME_STRING}" - expected that message argument number is between "{" and "}"
            throw new UnableToPerformException(e.getMessage() + "\nProbably there is/are invalid character(s) in translation message.\nmessage=" + message, e);
        }
    }

    private static Object[] getTranslatedMessageValueHolders(Object... messageValuesForHolders) {
        if (ArrayUtils.isEmpty(messageValuesForHolders)) {
            // If messageValuesForHolders = null then we should also return null because otherwise we can't use {...} in text.
            // For example, see key fieldGroup_details_separator_infoText_documentType
            return messageValuesForHolders;
        }

        final Object[] msgValuesForHolders = new Object[messageValuesForHolders.length];
        for (int i = 0; i < messageValuesForHolders.length; i++) {
            Object messageValueForHolder = messageValuesForHolders[i];
            if (messageValueForHolder instanceof MessageData) {
                MessageData messageData = (MessageData) messageValueForHolder;
                msgValuesForHolders[i] = localizeMessage(messageData);
            } else if (messageValueForHolder instanceof Collection) {
                @SuppressWarnings("rawtypes")
                Collection msgParameterArray = (Collection) messageValueForHolder;
                if (!msgParameterArray.isEmpty() && msgParameterArray.iterator().next() instanceof MessageData) {
                    final StringBuilder sb = new StringBuilder();
                    @SuppressWarnings("unchecked")
                    Collection<MessageData> messageDataArray = (Collection<MessageData>) messageValueForHolder;
                    for (MessageData messageDataItem : messageDataArray) {
                        sb.append(localizeMessage(messageDataItem)).append(" ");
                    }
                    msgValuesForHolders[i] = sb.toString();
                }
            } else {
                msgValuesForHolders[i] = messageValueForHolder;
            }
        }

        return msgValuesForHolders;
    }

    private static String localizeMessage(MessageData messageData) {
        final String translationWithPlaceholders = getMessage(messageData.getMessageKey());
        return format(translationWithPlaceholders, getTranslatedMessageParameters(messageData));
    }

    /**
     * Deeply localizes message parameters that might contain MessageData objects, that need to be localized as well
     * 
     * @param messageData
     * @return
     */
    private static Object[] getTranslatedMessageParameters(MessageData messageData) {
        final Object[] messageParameters = messageData.getMessageValuesForHolders();
        for (int i = 0; i < messageParameters.length; i++) {
            Object msgParameter = messageParameters[i];
            if (msgParameter instanceof MessageData) {
                MessageData msgData = (MessageData) msgParameter;
                final String localizedMessageParameter = localizeMessage(msgData);
                messageParameters[i] = localizedMessageParameter;
            } else if (msgParameter instanceof Collection) {
                @SuppressWarnings("rawtypes")
                Collection msgParameterArray = (Collection) msgParameter;
                if (!msgParameterArray.isEmpty() && msgParameterArray.iterator().next() instanceof MessageData) {
                    final StringBuilder sb = new StringBuilder();
                    @SuppressWarnings("unchecked")
                    Collection<MessageData> messageDataArray = (Collection<MessageData>) msgParameter;
                    for (MessageData messageDataItem : messageDataArray) {
                        sb.append(localizeMessage(messageDataItem)).append(" ");
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
        return getMessage(null, messageId, messageValuesForHolders);
    }

    /**
     * @param messageData
     * @return message translated based on {@link MessageData#getMessageKey()} or if such message doesn't exist and {@link MessageData#getFallbackMessage()} is provided, then
     *         returned message is created based on it
     */
    public static String getMessage(MessageData messageData) {
        String messageKey = messageData.getMessageKey();
        String message = getMessage(messageKey, messageData.getMessageValuesForHolders());
        if (!isMessageTranslated(messageKey, message)) {
            MessageData fallbackMessage = messageData.getFallbackMessage();
            if (fallbackMessage != null) {
                return getMessage(fallbackMessage);
            }
        }
        return message;
    }

    public static boolean isMessageTranslated(String messageKey, String message) {
        return !StringUtils.equals(message, "$$" + messageKey + "$$");
    }

    private static boolean isMessageTranslatedByI18nUtil(String message) {
        return message != null;// i18nUtil returns null when message is not found
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
        Utils.addErrorMessage(getMessage(context, messageId, messageValuesForHolders));
    }

    /**
     * @param context
     * @param messageData
     */
    private static void addStatusMessageInternal(FacesContext context, MessageData messageData) {
        MessageSeverity severity = messageData.getSeverity();
        if (context == null) {
            context = FacesContext.getCurrentInstance(); // don't know if FacesContext was not given or FacesContext doesn't exist
            if (context == null) {
                logMessage(messageData, LOG); // FacesContext doesn't exist
                return;
            }
        }
        if (severity == MessageSeverity.ERROR) {
            addErrorMessage(context, messageData.getMessageKey(), messageData.getMessageValuesForHolders());
            return;
        }
        context.addMessage(null, getFacesMessage(messageData));
    }

    private static void logMessage(MessageData messageData, Log logger) {
        String msg = getMessage(messageData);
        MessageSeverity severity = messageData.getSeverity();
        if (severity == MessageSeverity.FATAL) {
            logger.fatal(msg);
        } else if (severity == MessageSeverity.ERROR) {
            logger.error(msg);
        } else if (severity == MessageSeverity.WARN) {
            logger.warn(msg);
        } else if (severity == MessageSeverity.INFO) {
            logger.info(msg);
        } else {
            throw new RuntimeException("Unknown MessageSeverity constant");
        }
    }

    public static void logMessage(MessageDataWrapper messageDataWrapper, Log logger) {
        for (MessageData messageData : messageDataWrapper) {
            logMessage(messageData, logger);
        }
    }
 
    public static FacesMessage getFacesMessage(MessageData msgData) {
        String msg = getMessage(msgData);
        return new FacesMessage(getFacesSeverity(msgData.getSeverity()), msg, msg);
    }

    public static void addWarningMessage(String msgKey, Object... messageValuesForHolders) {
        addStatusMessageInternal(FacesContext.getCurrentInstance(), new MessageDataImpl(MessageSeverity.WARN, msgKey, messageValuesForHolders));
    }

    public static void addInfoMessage(String msgKey, Object... messageValuesForHolders) {
        addStatusMessageInternal(FacesContext.getCurrentInstance(), new MessageDataImpl(MessageSeverity.INFO, msgKey, messageValuesForHolders));
    }

    public static void addErrorMessage(String msgKey, Object... messageValuesForHolders) {
        addStatusMessageInternal(FacesContext.getCurrentInstance(), new MessageDataImpl(MessageSeverity.ERROR, msgKey, messageValuesForHolders));
    }

    public static void addInfoMessage(FacesContext currentInstance, String msgKey, Object... messageValuesForHolders) {
        addStatusMessageInternal(currentInstance, new MessageDataImpl(MessageSeverity.INFO, msgKey, messageValuesForHolders));
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
        addStatusMessageInternal(facesContext, messageData);
        final MessageSeverity severity = messageData.getSeverity();
        return severity == MessageSeverity.ERROR || severity == MessageSeverity.FATAL;
    }

    /**
     * Add statusMessage to the faces context(to be shown to the user). Message text is retrieved from message bundle based on key <code>messageData.getMessageKey()</code> and
     * possible values could be set using <code>messageData.getMessageValuesForHolders()</code>. Severity of message is determined by <code>messageData.getSeverity()</code>
     * 
     * @param messageData - messageData object used to create message
     * @return true if added message with error or fatal severity
     */
    public static boolean addStatusMessage(MessageData messageData) {
        return addStatusMessage(FacesContext.getCurrentInstance(), messageData);
    }

    private static FacesMessage.Severity getFacesSeverity(final MessageSeverity severity) {
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
        return facesSeverity;
    }

    /**
     * Add error message by concatenating messages retrieved from the messageKeys
     * 
     * @param currentInstance
     * @param messageKeysWithValueObjects
     */
    public static void addErrorMessage(FacesContext currentInstance, Pair<String, Object[]>[] messageKeysWithValueObjects) {
        final StringBuilder sb = new StringBuilder();
        for (Pair<String, Object[]> messageAndValuePair : messageKeysWithValueObjects) {
            sb.append(getMessage(currentInstance, messageAndValuePair.getFirst(), messageAndValuePair.getSecond())).append(" ");
        }
        Utils.addErrorMessage(sb.toString());
    }

    /**
     * Resolves translation and escapes it for JavaScript. Since it is mostly used from JSP, values support special notation.
     * When message placeholder value starts with "msg." prefix, then this prefix is stripped and rest of the value is processed with getMessage().
     */
    public static String getMessageAndEscapeJS(String messageId, Object... messageValuesForHolders) {
        if (messageValuesForHolders == null) {
            return StringEscapeUtils.escapeJavaScript(getMessage(messageId, messageValuesForHolders));
        }

        List<Object> resolvedValues = new ArrayList<Object>(messageValuesForHolders.length);
        for (Object value : messageValuesForHolders) {
            if (value instanceof String && StringUtils.startsWith((String) value, "msg.")) {
                resolvedValues.add(getMessage(StringUtils.removeStart((String) value, "msg.")));
                continue;
            }
            resolvedValues.add(value);
        }

        return StringEscapeUtils.escapeJavaScript(getMessage(messageId, resolvedValues.toArray()));
    }

    public static String getMessage(Enum<?> c) {
        return getMessage("constant_" + c.getClass().getCanonicalName() + "_" + c.name());
    }

    public static String getTypeName(QName objectTypeQName) {
        TypeDefinition typeDef = BeanHelper.getDictionaryService().getType(objectTypeQName);
        String translatedTypeName = typeDef.getTitle();
        if (StringUtils.isBlank(translatedTypeName)) {
            translatedTypeName = getMessage(getTranslationKeyForType(objectTypeQName, typeDef));
        }
        if (StringUtils.isBlank(translatedTypeName)) {
            throw new IllegalStateException("there should be translation for type " + typeDef
                    + " in model properties file with key '" + getTranslationKeyForType(objectTypeQName, typeDef) + "'");
        }
        return translatedTypeName;
    }

    public static String getPropertyName(QName propertyQName) {
        PropertyDefinition propertyDef = BeanHelper.getDictionaryService().getProperty(propertyQName);
        String translatedTypeName = propertyDef.getTitle();
        if (StringUtils.isBlank(translatedTypeName)) {
            return propertyQName.toString();
        }
        return translatedTypeName;
    }

    public static String getTranslationKeyForType(QName objectTypeQName, TypeDefinition typeDef) {
        NamespaceService namespaceService = BeanHelper.getNamespaceService();
        String model = typeDef.getModel().getName().toPrefixString(namespaceService).replace(":", "_");
        String type = objectTypeQName.toPrefixString(namespaceService).replace(":", "_");
        return model + ".type." + type + ".title";
    }

}
