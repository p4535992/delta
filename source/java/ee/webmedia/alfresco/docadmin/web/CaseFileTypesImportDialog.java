package ee.webmedia.alfresco.docadmin.web;

import ee.webmedia.alfresco.docadmin.service.CaseFileType;

/**
 * Dialog used to import {@link CaseFileType}s
 * 
 * @author Ats Uiboupin
 */
public class CaseFileTypesImportDialog extends DynamicTypesImportDialog<CaseFileType> {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CaseFileTypesImportDialog";

    protected CaseFileTypesImportDialog() {
        super(CaseFileType.class);
    }

}
