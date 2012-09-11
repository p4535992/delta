package ee.webmedia.alfresco.utils;

/**
 * Exception that wraps MessageDataWrapper that could contain multiple error/information messages useful to the user
 * 
 * @author Ats Uiboupin
 */
public class UnableToPerformMultiReasonException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final MessageDataWrapper messageDataWrapper;

    public UnableToPerformMultiReasonException(MessageDataWrapper messageDataWrapper) {
        this.messageDataWrapper = messageDataWrapper;
    }

    public MessageDataWrapper getMessageDataWrapper() {
        return messageDataWrapper;
    }

}
