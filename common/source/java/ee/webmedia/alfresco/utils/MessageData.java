<<<<<<< HEAD
package ee.webmedia.alfresco.utils;

import java.io.Serializable;

import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * The contract for storing message data. The message is not just one string. It can have parameters that will be
 * inserted into right places in the message text.
 * 
 * @author Ats Uiboupin
 */
public interface MessageData extends Serializable {

    String getMessageKey();

    Object[] getMessageValuesForHolders();

    MessageSeverity getSeverity();

    /**
     * @return message that should be used, if failed to translate message based on {@link #getMessageKey()}
     */
    MessageData getFallbackMessage();

=======
package ee.webmedia.alfresco.utils;

import java.io.Serializable;

import ee.webmedia.alfresco.utils.UnableToPerformException.MessageSeverity;

/**
 * The contract for storing message data. The message is not just one string. It can have parameters that will be
 * inserted into right places in the message text.
 */
public interface MessageData extends Serializable {

    String getMessageKey();

    Object[] getMessageValuesForHolders();

    MessageSeverity getSeverity();

    /**
     * @return message that should be used, if failed to translate message based on {@link #getMessageKey()}
     */
    MessageData getFallbackMessage();

>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
}