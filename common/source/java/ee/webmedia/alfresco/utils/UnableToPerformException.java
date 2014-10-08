<<<<<<< HEAD
package ee.webmedia.alfresco.utils;

import java.util.Arrays;

/**
 * This class could be used in service layer to notify view layer, that some message(key defined with constructor argument, placehoders with <code>messageValuesForHolders</code> )
 * could/should be shown to the user. <br>
 * If message should be shown to the user, use {@link MessageUtil#addStatusMessage(javax.faces.context.FacesContext, UnableToPerformException)}. <br>
 * Severity of the message is determined using <code>severity</code>
 * 
 * @author Ats Uiboupin
 */
public class UnableToPerformException extends RuntimeException implements MessageData {
    private static final long serialVersionUID = 1L;
    private final MessageSeverity severity;
    private Object[] messageValuesForHolders;
    private MessageData fallbackMessage;

    public enum MessageSeverity {
        INFO, WARN, ERROR, FATAL
    }

    public UnableToPerformException(String messageKey, Object... messageValuesForHolders) {
        this(MessageSeverity.ERROR, messageKey);
        setMessageValuesForHolders(messageValuesForHolders);
    }

    public UnableToPerformException(MessageSeverity severity, String messageKey) {
        this(severity, messageKey, null);
    }

    public UnableToPerformException(MessageSeverity severity, String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.severity = severity;
        messageValuesForHolders = new Object[0];
    }

    @Override
    public MessageSeverity getSeverity() {
        return severity;
    }

    @Override
    public Object[] getMessageValuesForHolders() {
        return messageValuesForHolders;
    }

    public void setMessageValuesForHolders(Object... messageValuesForHolders) {
        this.messageValuesForHolders = messageValuesForHolders;
    }

    @Override
    public String getMessageKey() {
        return super.getMessage();
    }

    @Override
    public MessageData getFallbackMessage() {
        return fallbackMessage;
    }

    public void setFallbackMessage(MessageData fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + Arrays.asList(messageValuesForHolders);
    }

}
=======
package ee.webmedia.alfresco.utils;

import java.util.Arrays;

/**
 * This class could be used in service layer to notify view layer, that some message(key defined with constructor argument, placehoders with <code>messageValuesForHolders</code> )
 * could/should be shown to the user. <br>
 * If message should be shown to the user, use {@link MessageUtil#addStatusMessage(javax.faces.context.FacesContext, UnableToPerformException)}. <br>
 * Severity of the message is determined using <code>severity</code>
 */
public class UnableToPerformException extends RuntimeException implements MessageData {
    private static final long serialVersionUID = 1L;
    private final MessageSeverity severity;
    private Object[] messageValuesForHolders;
    private MessageData fallbackMessage;

    public enum MessageSeverity {
        INFO, WARN, ERROR, FATAL
    }

    public UnableToPerformException(String messageKey, Object... messageValuesForHolders) {
        this(MessageSeverity.ERROR, messageKey);
        setMessageValuesForHolders(messageValuesForHolders);
    }

    public UnableToPerformException(MessageSeverity severity, String messageKey) {
        this(severity, messageKey, null);
    }

    public UnableToPerformException(MessageSeverity severity, String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.severity = severity;
        messageValuesForHolders = new Object[0];
    }

    @Override
    public MessageSeverity getSeverity() {
        return severity;
    }

    @Override
    public Object[] getMessageValuesForHolders() {
        return messageValuesForHolders;
    }

    public void setMessageValuesForHolders(Object... messageValuesForHolders) {
        this.messageValuesForHolders = messageValuesForHolders;
    }

    @Override
    public String getMessageKey() {
        return super.getMessage();
    }

    @Override
    public MessageData getFallbackMessage() {
        return fallbackMessage;
    }

    public void setFallbackMessage(MessageData fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + Arrays.asList(messageValuesForHolders);
    }

}
>>>>>>> develop-5.1
