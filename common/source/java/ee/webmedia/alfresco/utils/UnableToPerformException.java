package ee.webmedia.alfresco.utils;

import java.util.Arrays;

/**
 * This class could be used in service layer to notify view layer, that some message(key defined with constructor argument, placehoders with
 * <code>messageValuesForHolders</code> ) could/should be shown to the user. <br>
 * If message should be shown to the user, use {@link MessageUtil#addStatusMessage(javax.faces.context.FacesContext, UnableToPerformException)}. <br>
 * Severity of the message is determined using <code>severity</code>
 * 
 * @author Ats Uiboupin
 */
public class UnableToPerformException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private MessageSeverity severity;
    private Object[] messageValuesForHolders;

    public enum MessageSeverity {
        INFO, WARN, ERROR, FATAL
    }

    public UnableToPerformException(MessageSeverity severity, String messageKey) {
        this(severity, messageKey, null);
    }

    public UnableToPerformException(MessageSeverity severity, String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.severity = severity;
        this.messageValuesForHolders = new Object[0];
    }

    public MessageSeverity getSeverity() {
        return severity;
    }

    public Object[] getMessageValuesForHolders() {
        return messageValuesForHolders;
    }

    public void setMessageValuesForHolders(Object... messageValuesForHolders) {
        this.messageValuesForHolders = messageValuesForHolders;
    }

    public String getMessageKey() {
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        return super.getMessage() + Arrays.asList(messageValuesForHolders);
    }

    /**
     * Can be used to add translation keys to be used in message value holders.<br>
     * Useful if you need to pass message from service layer(where you shouldn't use web layer translation mechanisms to loosely couple components) to web layer
     * where message key and values could be translated (most likely by {@link MessageUtil})
     * 
     * @author Ats Uiboupin
     */
    public static class UntransaltedMessageValueHolder {
        private String messageKey;

        public UntransaltedMessageValueHolder(String messageKey) {
            this.messageKey = messageKey;
        }

        public String getMessageKey() {
            return messageKey;
        }

        @Override
        public String toString() {
            return "$$" + messageKey + "$$";
        }

    }
}
