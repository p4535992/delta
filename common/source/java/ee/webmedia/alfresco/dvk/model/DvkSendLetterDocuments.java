<<<<<<< HEAD
package ee.webmedia.alfresco.dvk.model;

import java.util.Collection;

/**
 * @author Ats Uiboupin
 */
public interface DvkSendLetterDocuments extends ILetterDocument, LetterCompilator {

    String getDocType();

    void setDocType(String docType);// TODO: enum

    // TODO: separate interface for recipientsRegNrs? also used in DvkSendWorkflowDocuments,
    // but not in DvkRecievedLetterDocuments
    Collection<String> getRecipientsRegNrs();

    void setRecipientsRegNrs(Collection<String> recipientsRegNrs);

    /**
     * Verify, that all the compulsory fields are filled that are needed for the outgoing message.
     */
    void validateOutGoing();

}
=======
package ee.webmedia.alfresco.dvk.model;

import java.util.Collection;

public interface DvkSendLetterDocuments extends ILetterDocument, LetterCompilator {

    String getDocType();

    void setDocType(String docType);// TODO: enum

    // TODO: separate interface for recipientsRegNrs? also used in DvkSendWorkflowDocuments,
    // but not in DvkRecievedLetterDocuments
    Collection<String> getRecipientsRegNrs();

    void setRecipientsRegNrs(Collection<String> recipientsRegNrs);

    /**
     * Verify, that all the compulsory fields are filled that are needed for the outgoing message.
     */
    void validateOutGoing();

}
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
