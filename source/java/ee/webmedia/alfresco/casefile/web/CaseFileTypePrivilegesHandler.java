package ee.webmedia.alfresco.casefile.web;

import ee.webmedia.alfresco.casefile.model.CaseFileModel;
import ee.webmedia.alfresco.docdynamic.web.DocumentTypePrivilegesHandler;
import ee.webmedia.alfresco.privilege.web.PrivilegesHandler;

/**
 * {@link PrivilegesHandler} for nodes of type {@link CaseFileModel.Types#CASE_FILE}
 */
public class CaseFileTypePrivilegesHandler extends DocumentTypePrivilegesHandler {

    private static final long serialVersionUID = 1L;

    protected CaseFileTypePrivilegesHandler() {
        super(CaseFileModel.Types.CASE_FILE, getDocumentCaseFilePrivs());
    }

    @Override
    protected void addDynamicPrivileges() {
        // Override to do nothing
    }

}