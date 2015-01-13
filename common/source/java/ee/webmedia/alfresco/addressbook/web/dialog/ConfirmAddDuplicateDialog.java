package ee.webmedia.alfresco.addressbook.web.dialog;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;

import ee.webmedia.alfresco.common.web.BeanHelper;
import ee.webmedia.alfresco.utils.MessageUtil;

/**
 * Dialog that shows confirmation message before adding duplicate
 */
public class ConfirmAddDuplicateDialog extends BaseDialogBean {
    private static final long serialVersionUID = 1L;
    public static final String BEAN_NAME = "ConfirmAddDuplicateDialog";
    private String confirmMessage;

    @Override
    protected String finishImpl(FacesContext context, String outcome) throws Exception {
        AddressbookAddEditDialog bean = BeanHelper.getAddressbookAddEditDialog();
        bean.persistEntry();
        return getCloseOutcome(2);
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
        return MessageUtil.getMessage("yes");
    }

    @Override
    public String getCancelButtonLabel() {
        return MessageUtil.getMessage("no");
    }

}
