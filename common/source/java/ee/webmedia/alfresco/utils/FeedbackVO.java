package ee.webmedia.alfresco.utils;

import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * This class can be used to give feedback about actions done service layer to web layer (where content could be formated into faces message using
 * {@link MessageUtil})
 * 
 * @author Ats Uiboupin
 */
public class FeedbackVO {
    private MessageSeverity severity;
    private String messageKey;
    private Object[] messageValuesForHolders;

    public FeedbackVO(MessageSeverity severity, String messageKey, Object... messageValuesForHolders) {
        this.severity = severity;
        this.messageValuesForHolders = messageValuesForHolders;
        this.messageKey = messageKey;
    }

    public MessageSeverity getSeverity() {
        return severity;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageValuesForHolders() {
        return messageValuesForHolders;
    }
}
