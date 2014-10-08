package ee.webmedia.alfresco.docadmin.web;

import org.alfresco.web.app.AlfrescoNavigationHandler;

import ee.webmedia.alfresco.docadmin.service.CaseFileType;

/**
 * Details of {@link CaseFileType}.
 * To open this dialog just call actionListener. You must not set action attribute on actionLink that opens this dialog nor any other way perform navigation, as actionListener
 * handles navigation
<<<<<<< HEAD
 * 
 * @author Ats Uiboupin
=======
>>>>>>> develop-5.1
 */
public class CaseFileTypeDetailsDialog extends DynamicTypeDetailsDialog<CaseFileType, CaseFileTypeDetailsDialog.CaseFileTypeDialogSnapshot> {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "CaseFileTypeDetailsDialog";

    // START: Block beans

    public CaseFileTypeDetailsDialog() {
        super(CaseFileType.class);
    }

    /**
     * Contains fields that contain state to be used when restoring dialog
<<<<<<< HEAD
     * 
     * @author Ats Uiboupin
=======
>>>>>>> develop-5.1
     */
    static class CaseFileTypeDialogSnapshot extends DynamicTypeDetailsDialog.DynTypeDialogSnapshot<CaseFileType> {
        private static final long serialVersionUID = 1L;

        @Override
        public String getOpenDialogNavigationOutcome() {
            return AlfrescoNavigationHandler.DIALOG_PREFIX + "caseFileTypeDetailsDialog";
        }
    }

    @Override
    protected CaseFileTypeDialogSnapshot newSnapshot() {
        return new CaseFileTypeDialogSnapshot();
    }

    // START: jsf actions/accessors

    /** JSP */
    @Override
    public boolean isAddFieldVisible() {
        return true;
    }

    // END: jsf actions/accessors
}
