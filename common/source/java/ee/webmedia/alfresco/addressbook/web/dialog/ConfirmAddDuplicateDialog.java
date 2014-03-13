package ee.webmedia.alfresco.addressbook.web.dialog;

import javax.faces.context.FacesContext;

import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.dialog.BaseDialogBean;

/**
 * Dialog that shows confirmation message before adding duplicate
 */
public class ConfirmAddDuplicateDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "ConfirmAddDuplicateDialog";
    private String confirmMessage;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        AddressbookAddEditDialog bean = (AddressbookAddEditDialog) FacesHelper.getManagedBean( //
                FacesContext.getCurrentInstance(), AddressbookAddEditDialog.BEAN_NAME);
        bean.persistEntry();
        return outcome + "[2]";
    }

    public void setConfirmMessage(String confirmMessage) {
        this.confirmMessage = confirmMessage;
    }

    /** @return confirm message to be shown by JSP page */
    public String getConfirmMessage() {
        return confirmMessage;
    }

    @Override
    public String cancel() {
        return getDefaultCancelOutcome();
    }

    @Override
    public boolean getFinishButtonDisabled() {
        return false;
    }

    @Override
    public String getFinishButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), "yes");
    }

    @Override
    public String getCancelButtonLabel() {
        return Application.getMessage(FacesContext.getCurrentInstance(), "no");
    }

}
