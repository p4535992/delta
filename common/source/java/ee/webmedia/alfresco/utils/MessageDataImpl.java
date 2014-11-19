<<<<<<< HEAD
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

    private MessageData fallbackMessage;

    public MessageDataImpl(MessageSeverity severity, String messageKey, Object... messageValuesForHolders) {
        this.severity = severity;
        this.messageValuesForHolders = messageValuesForHolders;
        this.messageKey = messageKey;
    }

    public MessageDataImpl(String messageKey, Object... messageValuesForHolders) {
        this(MessageSeverity.ERROR, messageKey, messageValuesForHolders);
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
    public MessageData getFallbackMessage() {
        return fallbackMessage;
    }

    public void setFallbackMessage(MessageData fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
    }

    @Override
    public String toString() {
        return severity + ": " + messageKey + ", " + Arrays.asList(messageValuesForHolders);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fallbackMessage == null) ? 0 : fallbackMessage.hashCode());
        result = prime * result + ((messageKey == null) ? 0 : messageKey.hashCode());
        result = prime * result + Arrays.hashCode(messageValuesForHolders);
        result = prime * result + ((severity == null) ? 0 : severity.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MessageDataImpl other = (MessageDataImpl) obj;
        if (fallbackMessage == null) {
            if (other.fallbackMessage != null) {
                return false;
            }
        } else if (!fallbackMessage.equals(other.fallbackMessage)) {
            return false;
        }
        if (messageKey == null) {
            if (other.messageKey != null) {
                return false;
            }
        } else if (!messageKey.equals(other.messageKey)) {
            return false;
        }
        if (!Arrays.equals(messageValuesForHolders, other.messageValuesForHolders)) {
            return false;
        }
        if (severity != other.severity) {
            return false;
        }
        return true;
    }

}
=======
package ee.webmedia.alfresco.utils;

import java.util.Arrays;

import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * This class can be used to give feedback about actions done service layer to web layer (where content could be formated into faces message using {@link MessageUtil})
 */
public class MessageDataImpl implements MessageData {
    private static final long serialVersionUID = 1L;

    private final MessageSeverity severity;
    private final String messageKey;
    private final Object[] messageValuesForHolders;

    private MessageData fallbackMessage;

    public MessageDataImpl(MessageSeverity severity, String messageKey, Object... messageValuesForHolders) {
        this.severity = severity;
        this.messageValuesForHolders = messageValuesForHolders;
        this.messageKey = messageKey;
    }

    public MessageDataImpl(String messageKey, Object... messageValuesForHolders) {
        this(MessageSeverity.ERROR, messageKey, messageValuesForHolders);
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
    public MessageData getFallbackMessage() {
        return fallbackMessage;
    }

    public void setFallbackMessage(MessageData fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
    }

    @Override
    public String toString() {
        return severity + ": " + messageKey + ", " + Arrays.asList(messageValuesForHolders);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fallbackMessage == null) ? 0 : fallbackMessage.hashCode());
        result = prime * result + ((messageKey == null) ? 0 : messageKey.hashCode());
        result = prime * result + Arrays.hashCode(messageValuesForHolders);
        result = prime * result + ((severity == null) ? 0 : severity.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MessageDataImpl other = (MessageDataImpl) obj;
        if (fallbackMessage == null) {
            if (other.fallbackMessage != null) {
                return false;
            }
        } else if (!fallbackMessage.equals(other.fallbackMessage)) {
            return false;
        }
        if (messageKey == null) {
            if (other.messageKey != null) {
                return false;
            }
        } else if (!messageKey.equals(other.messageKey)) {
            return false;
        }
        if (!Arrays.equals(messageValuesForHolders, other.messageValuesForHolders)) {
            return false;
        }
        if (severity != other.severity) {
            return false;
        }
        return true;
    }

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
