package ee.webmedia.alfresco.docadmin.web;

import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * Dialog used to import {@link DocumentType}s
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class DocumentTypesImportDialog extends DynamicTypesImportDialog<DocumentType> {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "DocumentTypesImportDialog";

    protected DocumentTypesImportDialog() {
        super(DocumentType.class);
    }

}
