package ee.webmedia.alfresco.utils;

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

}
