package ee.webmedia.alfresco.utils;

import ee.webmedia.alfresco.docdynamic.service.DocumentDynamic;

/**
 * Exception that wraps MessageDataWrapper that could contain multiple error/information messages useful to the user
 * 
 * @author Ats Uiboupin
 */
public class UnableToPerformMultiReasonException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final MessageDataWrapper messageDataWrapper;
    private final DocumentDynamic document;

    public UnableToPerformMultiReasonException(MessageDataWrapper messageDataWrapper, DocumentDynamic document) {
        this.messageDataWrapper = messageDataWrapper;
        this.document = document;
    }

    public UnableToPerformMultiReasonException(MessageDataWrapper messageDataWrapper) {
        this(messageDataWrapper, null);
    }

    public MessageDataWrapper getMessageDataWrapper() {
        return messageDataWrapper;
    }

    public DocumentDynamic getDocument() {
        return document;
    }

}
