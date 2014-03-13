package ee.webmedia.alfresco.adddocument;

/**
 * Marker class to distinguish known errors that may occur during document import by addDocument web service.
 */
public class AddDocumentException extends RuntimeException {

    public AddDocumentException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 1L;

}
