package ee.webmedia.alfresco.template.service;

/**
 * @author Alar Kvell
 */
public class DocumentTemplateNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DocumentTemplateNotFoundException(String message) {
        super(message);
    }

}
