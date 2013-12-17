package ee.webmedia.alfresco.utils;

import org.apache.commons.lang.StringUtils;

import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * Helps to create error messages for web components that might need to customize its error messages
 * 
 * @author Ats Uiboupin
 */
public class MessageCreatorHelper implements MessageData {
    private static final long serialVersionUID = 1L;
    private String msgCustomPrefix;
    private ErrorMsgFormat msgCustomFormat;
    private String createdMsgKey;
    private Object[] messageValuesForHolders;
    private MessageData fallbackMessage;

    public enum ErrorMsgFormat {
        /** Uses message, that contains placeholders: {0}: panelLabel, {1}: fieldName, {2}: invalidLocalName */
        FULL,
        /** Uses message, that contains placeholders: {0}: panelLabel, {1}: fieldName */
        PANEL_AND_FIELD_NAME,
        /** Uses message, that contains placeholders: {1}: fieldName */
        FIELD_NAME_ONLY
    }

    public MessageCreatorHelper(String msgCustomPrefix, ErrorMsgFormat msgCustomFormat) {
        this.msgCustomPrefix = msgCustomPrefix;
        this.msgCustomFormat = msgCustomFormat;
    }

    public void createMessage(String defaultErrorMsgPrefix, String msgInfix, ErrorMsgFormat defaultErrorMsgFormat) {
        String msgPrefix = msgCustomPrefix;
        if (StringUtils.isBlank(msgPrefix)) {
            msgPrefix = defaultErrorMsgPrefix;
        }
        ErrorMsgFormat msgFormat = msgCustomFormat;
        if (msgFormat == null) {
            msgFormat = defaultErrorMsgFormat;
        }
        createdMsgKey = msgPrefix + msgInfix + "_" + msgFormat.name();
    }

    public boolean isMessageSet() {
        return StringUtils.isNotBlank(createdMsgKey);
    }

    public void setMsgCustomPrefix(String msgCustomPrefix) {
        this.msgCustomPrefix = msgCustomPrefix;
    }

    public void setMsgCustomFormat(ErrorMsgFormat msgCustomFormat) {
        this.msgCustomFormat = msgCustomFormat;
    }

    public void setMessageValuesForHolders(Object... messageValuesForHolders) {
        this.messageValuesForHolders = messageValuesForHolders;
    }

    @Override
    public String getMessageKey() {
        return createdMsgKey;
    }

    @Override
    public Object[] getMessageValuesForHolders() {
        return messageValuesForHolders;
    }

    @Override
    public MessageSeverity getSeverity() {
        return MessageSeverity.ERROR;
    }

    @Override
    public MessageData getFallbackMessage() {
        return fallbackMessage;
    }

    public void setFallbackMessage(MessageData fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
    }
}