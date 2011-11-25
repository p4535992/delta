package ee.webmedia.alfresco.docadmin.web;

import ee.webmedia.alfresco.docadmin.service.CaseFileType;

/**
 * @author Ats Uiboupin
 */
public class CaseFileTypeListDialog extends DynamicTypeListDialog<CaseFileType> {
    private static final long serialVersionUID = 1L;

    protected CaseFileTypeListDialog() {
        super(CaseFileType.class);
    }

    @Override
    protected String getExportFileName() {
        return "caseFileTypes.xml";
    }

}
