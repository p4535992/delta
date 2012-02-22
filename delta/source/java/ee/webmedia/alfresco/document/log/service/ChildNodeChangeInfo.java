package ee.webmedia.alfresco.document.log.service;

import org.alfresco.service.namespace.QName;

/**
 * Information holding object for composing history log message about a child node being added or removed. This class is used internally by {@link DocumentPropertiesChangeHolder}.
 * 
 * @see DocumentPropertiesChangeHolder
 * @author Martti Tamm
 */
public class ChildNodeChangeInfo {

    private final QName msgParamProp;

    private final String addMessageKey;

    private final String removeMessageKey;

    /**
     * Creates a new information holder for logging a child node being added or removed.
     * <p>
     * The message key for localizing the description must be in standard format:
     * <ul>
     * <li><code>document_log_</code><i>&lt;messagePart&gt;</i><code>_add</code> - when child node is being added;
     * <li><code>document_log_</code><i>&lt;messagePart&gt;</i><code>_rem</code> - when child node is being added.
     * </ul>
     * Therefore, the message part information is required for successful localization.
     * <p>
     * Localized history messages may contain property values from the child node. The property to include at position 0 is defined by <code>msgParamProp</code>. Currently, the
     * property may contain only <code>String</code> values. When the property has no value or the property has not been specified, an empty value place-holder will be provided
     * instead for the message.
     * <p>
     * Currently only one child node property value is possible to use in the message, but technically it is possible to extend this solution for multiple property values.
     * 
     * @param messagePart A unique message part to use for composing child node add or remove messages.
     * @param msgParamProp Optional node property name for injecting its value to the history message.
     */
    public ChildNodeChangeInfo(String messagePart, QName msgParamProp) {
        addMessageKey = new StringBuilder("document_log_").append(messagePart).append("_add").toString();
        removeMessageKey = new StringBuilder("document_log_").append(messagePart).append("_rem").toString();
        this.msgParamProp = msgParamProp;
    }

    public QName getMsgParamProp() {
        return msgParamProp;
    }

    public String getAddMessageKey() {
        return addMessageKey;
    }

    public String getRemoveMessageKey() {
        return removeMessageKey;
    }
}
