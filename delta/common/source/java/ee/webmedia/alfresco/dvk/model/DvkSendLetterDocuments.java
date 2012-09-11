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
