package ee.webmedia.alfresco.docadmin.web;

import ee.webmedia.alfresco.docadmin.service.CaseFileType;

/**
 * Dialog used to import {@link CaseFileType}s
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> 29c20c3e1588186b14bdc3b5fa90cae04ea61fc5
 */
public class CaseFileTypesImportDialog extends DynamicTypesImportDialog<CaseFileType> {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CaseFileTypesImportDialog";

    protected CaseFileTypesImportDialog() {
        super(CaseFileType.class);
    }

}
