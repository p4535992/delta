package ee.webmedia.alfresco.docadmin.web;

import ee.webmedia.alfresco.docadmin.service.DocumentType;

/**
 * Dialog used to import {@link DocumentType}s
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> develop-5.1
 */
public class DocumentTypesImportDialog extends DynamicTypesImportDialog<DocumentType> {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "DocumentTypesImportDialog";

    protected DocumentTypesImportDialog() {
        super(DocumentType.class);
    }

}
