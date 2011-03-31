package ee.webmedia.alfresco.utils;

import java.util.Arrays;

import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * This class can be used to give feedback about actions done service layer to web layer (where content could be formated into faces message using {@link MessageUtil})
 * 
 * @author Ats Uiboupin
 */
public class MessageDataImpl implements MessageData {
    private static final long serialVersionUID = 1L;

    private final MessageSeverity severity;
    private final String messageKey;
    private final Object[] messageValuesForHolders;

    public MessageDataImpl(MessageSeverity severity, String messageKey, Object... messageValuesForHolders) {
        this.severity = severity;
        this.messageValuesForHolders = messageValuesForHolders;
        this.messageKey = messageKey;
    }

    @Override
    public MessageSeverity getSeverity() {
        return severity;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }

    @Override
    public Object[] getMessageValuesForHolders() {
        return messageValuesForHolders;
    }

    @Override
    public String toString() {
        return severity + ": " + messageKey + ", " + Arrays.asList(messageValuesForHolders);
    }

}
